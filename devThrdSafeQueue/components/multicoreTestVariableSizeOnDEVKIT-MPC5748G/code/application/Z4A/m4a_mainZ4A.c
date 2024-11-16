/**
 * @file m4a_mainZ4A.c
 *
 * Copyright (C) 2019-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
/* Module interface
 *   m4a_startSecondaryCore
 *   main
 * Local functions
 *   isrPit1
 *   isrPit2
 *   isrPit3
 *   osInstallInterruptServiceRoutines
 *   taskInitProcess
 *   task1ms
 *   taskReporting
 */

/*
 * Include files
 */

#include "m4a_mainZ4A.h"

#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <assert.h>

#include "ccl_configureClocks.h"
#include "xbs_crossbarSwitch.h"
#include "stm_systemTimer.h"
#include "lbd_ledAndButtonDriver.h"
#include "sio_serialIO.h"
#include "rtos.h"
#include "gsl_systemLoad.h"
#include "std_decoratedStorage.h"
#include "icn_interCoreNotification.h"
#include "vsq_threadSafeQueueVariableSize.h"
#include "pid_processID.h"
#include "crc_checksum.h"
#include "mtx_mutex.h"
#include "rnd_random.h"
#include "mz2_mainZ2.h"
#include "m4b_mainZ4B.h"

/* This application proves correctness of data transmission with E2E protection. However,
   the checksum and cycle counter evaluation is done by assertion in DEBUG compilation
   only. */
#ifndef DEBUG
# warning Full error evaluation is done in DEBUG compilation only!
#endif

/*
 * Defines
 */

/** The demo can be compiled with a ground load. Most tasks produce some CPU load if this
    switch is set to 1. */
#define TASKS_PRODUCE_GROUND_LOAD   0

/** The maximum capacity of the queue objects in terms of number of stored elements of
    maximum size. */
#define MAX_QUEUE_LENGTH            50



/*
 * Local type definitions
 */

/** The enumeration of all events IDs used on this core. Actually, these IDs are provided
    by the RTOS at runtime, when creating the event. However, it is guaranteed that the
    IDs, which are dealt out by rtos_osCreateEvent() form the series 0, 1, 2, .... So we
    don't need to have a dynamic storage of the IDs; we define them as constants and
    double-check by assertion that we got the correct, expected IDs from
    rtos_osCreateEvent(). Note, this requires that the order of creating the events follows
    the order here in the enumeration. */
enum evId_t
{
    /** Regular timer event. Used for the regular 1ms worker task. */
    idEv1ms = 0,

    /** Regular timer event. Used for the regular reporting task. */
    idEvReporting,

    /** The number of events to register. */
    noRegisteredEvents
};


/** The RTOS uses constant priorities for its events, which are defined here.\n
      Note, the priority is a property of an event rather than of a task. A task implicitly
    inherits the priority of the event it is associated with. */
enum prioEv_t
{
    prioTaskIdle = 0,            /** Prio 0 is implicit, cannot be chosen explicitly */
    prioEvReporting,
    prioEv1ms,
};



/*
 * Local prototypes
 */


/*
 * Data definitions
 */

/** A synchronization object shared between the three cores. It is used to ensure that the
    initialization tasks of process P1 on the three cores are executed in a well-defined
    order. */
/// @todo Evaluate risk of using SHARED for mutexes
struct m4a_mutexStartup_t SHARED(m4a_mutexStartup) =
{ 
    .mtx = MTX_MUTEX_INITIALLY_RELEASED,
    .stZ4AInitialization = m4a_stInit_start,
    .stZ4BInitialization = m4a_stInit_start,
    .stZ2Initialization = m4a_stInit_start,
};

/** The current, averaged CPU load in tens of percent. */
static unsigned int SDATA_OS(m4a_cpuLoadZ4A) = 1000;

/** Counter of cyclic 1ms user task. */
static unsigned long SBSS_P1(m4a_cntTask1ms) = 0;

/** Counter of cyclic reporting user task. */
static unsigned long SBSS_P1(m4a_cntTaskReporting) = 0;

/** Counter of cycles of infinite main loop. */
static unsigned long SBSS_OS(m4a_cntTaskIdle) = 0;

/** Counter of queued elements, i.e. elements sent to core Z4B. */
static unsigned long SBSS_P1(m4a_noQueuedElements) = 0;

/** Counter of total queued bytes, i.e. bytes sent to core Z4B. */
static unsigned long SBSS_P1(m4a_sizeOfQueuedData) = 0;

/** The number of times, core Z4A saw an full Tx queue. */
static unsigned long SBSS_P1(m4a_noEvTxQueueFull) = 0;

/** Counter of elements received from core Z2. */
static unsigned long SBSS_P1(m4a_noRxElements) = 0;

/** Counter of total received bytes, i.e. bytes got from core Z2. */
static unsigned long SBSS_P1(m4a_sizeOfRxData) = 0;

/** The number of times, core Z4A found an empty Rx queue. */
static unsigned long SBSS_P1(m4a_noEvRxQueueEmpty) = 0;

/** A counter of the invocations of the otherwise useless PIT1 ISR. */
static unsigned long long SBSS_OS(m4a_cntISRPit1) = 0;

/** A counter of the invocations of the otherwise useless PIT2 ISR. */
static unsigned long long SBSS_OS(m4a_cntISRPit2) = 0;

/** A counter of the invocations of the otherwise useless PIT3 ISR. */
static unsigned long long SBSS_OS(m4a_cntISRPit3) = 0;

/** The queue object under test. It connects core Z4A with Z2. */
vsq_queueHead_t * UNCACHED_P1(m4a_pQHead_z2ToZ4A) = NULL;

/** The queue object under test. It connects core Z4A with Z4B. */
vsq_queueTail_t * UNCACHED_P1(m4a_pQTail_z4AToZ4B) = NULL;

/** Stack reserve of process p1 on the second core. */
static unsigned int SBSS_OS(m4a_stackReserveP1) = 0;

/** Stack reserve of kernel process on the second core. */
static unsigned int SBSS_OS(m4a_stackReserveOS) = 0;

/** Interface with assembly code: Here's a variable in the assembly startup code, which
    takes the addresses of the C main function to be invoked on core Z4B. It needs to be
    initialized prior to starting the core. Z4B and Z2 may use the same function.
      @todo Why can't we place the declaration locally into the function where it is used?
    The compiler rejects it due to the section attribute. */
/// @todo Bad section. Don't we require cache inhibitted? And surely not in P1!
extern void (*volatile SECTION(.bss.startup) sup_main_Z4B)(signed int, const char **);

/** Interface with assembly code: Here's a variable in the assembly startup code, which
    takes the addresses of the C main function to be invoked on core Z2. It needs to be
    initialized prior to starting the core. Z4B and Z2 may use the same function. */
extern void (*volatile SECTION(.bss.startup) sup_main_Z2)(signed int, const char **);


/*
 * Function implementation
 */

/**
 * Compute a random number in the range 0..#RAND_MAX.
 *   @return
 * Get the random number.
 */
static int random(void)
{
    /* This simple implementation of rand() taken from
       https://code.woboq.org/userspace/glibc/stdlib/random_r.c.html, downloaded on Mar 18,
       2019. */
    static uint32_t SDATA_P1(state_) = 1u;
    state_ = ((state_ * 1103515245U) + 12345U) & 0x7fffffffU;

    return (int)state_;

} /* End of random */


/**
 * A regularly triggered interrupt handler for the timer PIT3. The interrupt does nothing
 * but counting a variable. It is triggered at high frequency and asynchronously to the
 * kernel's clock tick to prove the system stability and properness of the context switches.
 *   @remark
 * This is a normal interrupt running in the kernel context (supervisor mode, no MPU
 * restrictions).
 */
static void isrPit1(void)
{
    ++ m4a_cntISRPit1;

    /* RM 51.4.11, p. 2738f: Acknowledge the timer interrupt in the causing HW device. Can
       be done as this is "trusted code" that is running in supervisor mode. */
    PIT->TIMER[1].TFLG = PIT_TFLG_TIF(1);

} /* End of isrPit1 */



/**
 * A regularly triggered interrupt handler for the timer PIT3. The interrupt does nothing
 * but counting a variable. It is triggered at high frequency and asynchronously to the
 * kernel's clock tick to prove the system stability and properness of the context switches.
 *   @remark
 * This is a normal interrupt running in the kernel context (supervisor mode, no MPU
 * restrictions).
 */
static void isrPit2(void)
{
    ++ m4a_cntISRPit2;

    /* RM 51.4.11, p. 2738f: Acknowledge the timer interrupt in the causing HW device. Can
       be done as this is "trusted code" that is running in supervisor mode. */
    PIT->TIMER[2].TFLG = PIT_TFLG_TIF(1);

} /* End of isrPit2 */



/**
 * A regularly triggered interrupt handler for the timer PIT3. The interrupt does nothing
 * but counting a variable. It is triggered at high frequency and asynchronously to the
 * kernel's clock tick to prove the system stability and properness of the context switches.
 *   @remark
 * This is a normal interrupt running in the kernel context (supervisor mode, no MPU
 * restrictions).
 */
static void isrPit3(void)
{
    ++ m4a_cntISRPit3;

    /* RM 51.4.11, p. 2738f: Acknowledge the timer interrupt in the causing HW device. Can
       be done as this is "trusted code" that is running in supervisor mode. */
    PIT->TIMER[3].TFLG = PIT_TFLG_TIF(1);

} /* End of isrPit3 */



/**
 * This demonstration software uses a number of fast interrupts to produce system load and
 * prove stability. The interrupts are timer controlled (for simplicity) but the
 * activations are chosen as asynchronous to the operating system clock as possible to
 * provoke a most variable preemption pattern.
 */
static void osInstallInterruptServiceRoutines(void)
{
    /* Disable timers during configuration. RM, 51.4.1, p. 2731.
         Disable all PIT timers during configuration. Note, this is a global setting for
       all sixteen timers. Accessing the bits makes this rountine have race conditions with
       the RTOS initialization that uses timer #RTOS_CORE_0_IDX_OF_PID_TIMER. Both routines
       must not be called in concurrency. */
    PIT->MCR = PIT_MCR_MDIS(1) | PIT_MCR_FRZ(1);

    /* Install the ISRs now that all timers are stopped.
         Vector numbers: See MCU reference manual, section 28.7, table 28-4. */
    const unsigned int processorID = rtos_osGetIdxCore();
    rtos_osRegisterInterruptHandler( &isrPit1
                                   , processorID
                                   , /* vectorNum */ PIT_Ch1_IRQn
                                   , /* psrPriority */ 1
                                   , /* isPreemptable */ true
                                   );
    rtos_osRegisterInterruptHandler( &isrPit2
                                   , processorID
                                   , /* vectorNum */ PIT_Ch2_IRQn
                                   , /* psrPriority */ 8
                                   , /* isPreemptable */ true
                                   );
    rtos_osRegisterInterruptHandler( &isrPit3
                                   , processorID
                                   , /* vectorNum */ PIT_Ch3_IRQn
                                   , /* psrPriority */ 14
                                   , /* isPreemptable */ true
                                   );

    /* Peripheral clock has been initialized to 40 MHz. The timers count at this rate. To
       get a 1ms interrupt tick we need to count till 40000.
         The RTOS operates in ticks of 1ms. We use prime numbers to get good asynchronity
       with the RTOS clock.
         -1: See RM, 51.6 */
    PIT->TIMER[1].LDVAL = 39989-1;/* Interrupt rate approx. 1kHz */
    PIT->TIMER[2].LDVAL = 4001-1; /* Interrupt rate approx. 10kHz */
    PIT->TIMER[3].LDVAL = 1327-1; /* Interrupt rate approx. 30kHz */

    /* Enable timer operation. This operation affects all timer channels.
         PIT_MCR_FRZ: For this multi-core MCU it is not so easy to decide whether or not to
       let the timers be stopped on debugger entry: Any stopped core will halt the timers,
       regardless whether that core is related to the timer or not (and how should the
       debugger know...). Both possibilities can be annoying or advantageous, depending on
       the situation. */
    PIT->MCR = PIT_MCR_MDIS(0) | PIT_MCR_FRZ(1);

    /* Clear possibly pending interrupt flags. */
    PIT->TIMER[1].TFLG = PIT_TFLG_TIF(1);
    PIT->TIMER[2].TFLG = PIT_TFLG_TIF(1);
    PIT->TIMER[3].TFLG = PIT_TFLG_TIF(1);

    /* Enable interrupts by the timers and start them. See RM 51.4.10. */
    PIT->TIMER[1].TCTRL = PIT_TCTRL_CHN(0) | PIT_TCTRL_TIE(1) | PIT_TCTRL_TEN(1);
    PIT->TIMER[2].TCTRL = PIT_TCTRL_CHN(0) | PIT_TCTRL_TIE(1) | PIT_TCTRL_TEN(1);
    PIT->TIMER[3].TCTRL = PIT_TCTRL_CHN(0) | PIT_TCTRL_TIE(1) | PIT_TCTRL_TEN(1);

} /* End of osInstallInterruptServiceRoutines */



/**
 * Initialization task of process \a PID.
 *   @return
 * The function returns the Boolean descision, whether the initialization was alright and
 * the system can start up. "Not alright" is expressed by a negative number, which hinders
 * the RTOS to startup.
 *   @param PID
 * The ID of the process, the task function is executed in.
 *   @remark
 * In this sample, we demonstrate that different processes' tasks can share the same task
 * function implementation. This is meant a demonstration of the technical feasibility but
 * not of good practice; the implementation needs to use shared memory, which may break a
 * safety constraint, and it needs to consider the different privileges of the processes.
 */
static int32_t taskInitProcess(uint32_t PID)
{
    static unsigned int SHARED(cnt_) = 0;
    ++ cnt_;
    bool success = cnt_ == PID;

    if(success &&  PID == 1)
    {
#ifdef DEBUG
        mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
        assert(m4a_mutexStartup.stZ4AInitialization == m4a_stInit_start);
        mtx_releaseMutex(&m4a_mutexStartup.mtx);
#endif
        /* Note: Only process 1 has access to the C lib (more precise: to those functions
           of the C lib, which write to lib owned data objects) and can write a status
           message. */
        SIO_PUTS("Core Z4A: Initialize P1");

        /* Create our head and our tail object of the two queues, which connect this core
           with the others.
             The macros denote an estimation of the actual object sizes. The estimation
           needs to be not lower and should be not much greater than the true value. */
        #define SIZE_OF_QUEUE_HEAD (8u)
        #define SIZE_OF_QUEUE_TAIL ((MAX_QUEUE_LENGTH)*(M4A_MAX_NO_PAYLOAD_BYTES+8u)+40u)
        static _Alignas(uintptr_t) uint8_t UNCACHED_P1(memPoolHead)[SIZE_OF_QUEUE_HEAD];
        static _Alignas(uintptr_t) uint8_t UNCACHED_P1(memPoolTail)[SIZE_OF_QUEUE_TAIL];
        const unsigned int sizeOfQueueHead = vsq_getSizeOfQueueHead()
                         , sizeOfQueueTail = vsq_getSizeOfQueueTail( MAX_QUEUE_LENGTH
                                                                   , M4A_MAX_NO_PAYLOAD_BYTES
                                                                   );
        if(sizeOfQueueHead <= sizeof(memPoolHead)  &&  sizeOfQueueTail <= sizeof(memPoolTail))
        {
            m4a_pQHead_z2ToZ4A = vsq_createQueueHead(&memPoolHead[0]);
            m4a_pQTail_z4AToZ4B = vsq_createQueueTail( &memPoolTail[0]
                                                     , MAX_QUEUE_LENGTH
                                                     , M4A_MAX_NO_PAYLOAD_BYTES
                                                     );
        }
        else
            success = false;
            
        /* Signal the other cores that we've created our queue objects. */
        mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
        m4a_mutexStartup.stZ4AInitialization = m4a_stInit_objectsCreated;
        mtx_releaseMutex(&m4a_mutexStartup.mtx);

        /* Now wait for completion of the other cores' object creation. */
        bool goAhead;
        do
        {
            mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
            goAhead = m4a_mutexStartup.stZ4BInitialization == m4a_stInit_objectsCreated
                      &&  m4a_mutexStartup.stZ2Initialization == m4a_stInit_objectsCreated;
            success = m4a_mutexStartup.stZ4BInitialization != m4a_stInit_aborted
                      &&  m4a_mutexStartup.stZ2Initialization != m4a_stInit_aborted;
            mtx_releaseMutex(&m4a_mutexStartup.mtx);
        }
        while(success && !goAhead);

        if(success)
        {
            SIO_PUTS("Core Z4A: All cores have created their queue objects");
            assert(m4a_pQHead_z2ToZ4A != NULL  &&  m4a_pQTail_z4AToZ4B != NULL
                   &&  m4b_pQHead_z4AToZ4B != NULL  &&  m4b_pQTail_z4BToZ2 != NULL
                   &&  mz2_pQHead_z4BToZ2 != NULL  &&  mz2_pQTail_z2ToZ4A != NULL
                  );

            /* Now link our head and tail with their counterparts owned by the other cores
               (and residing in a different process' memory). */
            vsq_linkQueueHeadWithTail(m4a_pQHead_z2ToZ4A, mz2_pQTail_z2ToZ4A);
            vsq_linkQueueTailWithHead(m4a_pQTail_z4AToZ4B, m4b_pQHead_z4AToZ4B);

            /* Signal the other cores that we've completed our queue initialization. */
            mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
            m4a_mutexStartup.stZ4AInitialization = m4a_stInit_objectsLinked;
            mtx_releaseMutex(&m4a_mutexStartup.mtx);

            do
            {
                mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
                goAhead = m4a_mutexStartup.stZ4BInitialization == m4a_stInit_objectsLinked
                          &&  m4a_mutexStartup.stZ2Initialization == m4a_stInit_objectsLinked;
                success = m4a_mutexStartup.stZ4BInitialization != m4a_stInit_aborted
                          &&  m4a_mutexStartup.stZ2Initialization != m4a_stInit_aborted;
                mtx_releaseMutex(&m4a_mutexStartup.mtx);
            }
            while(success && !goAhead);
        }            
        
        if(success)
            SIO_PUTS("Core Z4A: All cores have completed initialization");
        else
            SIO_PUTS("Core Z4A: Initialization failed at least on one core");
    }

    return success? 0: -1;

} /* End of taskInitProcess */



/**
 * Task function, cyclically activated every Millisecond. The LED D4 is switched on and off.
 *   @return
 * If the task function returns a negative value then the task execution is counted as
 * error in the process.
 *   @param PID
 * A user task function gets the process ID as first argument.
 *   @param taskParam
 * A variable task parameter. Here just used for testing.
 */
static int32_t task1ms(uint32_t PID ATTRIB_UNUSED, uintptr_t taskParam ATTRIB_DBG_ONLY)
{
    assert(taskParam == 0);

    /* Make spinning of the task observable in the debugger. */
    ++ m4a_cntTask1ms;

#if TASKS_PRODUCE_GROUND_LOAD == 1
    /* Produce a bit of CPU load. This call simulates some true application software. */
    del_delayMicroseconds(/* fullLoadThisNoMicroseconds */ 50 /* approx. 5% load */);
#endif
    
    /* Read from the queue from core Z2. The three queues in test are cyclically arranged.
       The queue from Z2 contains the same elements, which we had queued before towards
       core Z4B. */
    const unsigned int maxNoReadCycles = RND_URAND(0, 10);
    unsigned int u = 0;
    while(u < maxNoReadCycles)
    {
        unsigned int sizeOfPayload;
        const uint8_t * const pData = vsq_readFromHead(m4a_pQHead_z2ToZ4A, &sizeOfPayload);

        assert(sizeOfPayload <= M4A_MAX_NO_PAYLOAD_BYTES);
        static uint8_t SBSS_P1(sqc_) = 0;

        if(pData != NULL)
        {
            /* If we have at least one byte of payload then we can validate the
               checksum. */
            if(sizeOfPayload >= 1)
            {
                const uint8_t crc ATTRIB_DBG_ONLY = crc_checksumSAEJ1850_8Bit
                                                                        ( &pData[1]
                                                                        , sizeOfPayload-1
                                                                        );
                assert(crc == pData[0]);
                assert(sizeOfPayload <= 1  || pData[1] == sqc_);

                m4a_sizeOfRxData += sizeOfPayload;
            }
            ++ m4a_noRxElements;
            ++ u;

            /* The sequence counter relates to all queued elements, including those,
               which are too short to contain it. */
            ++ sqc_;
        }
        else
        {   
            ++ m4a_noEvRxQueueEmpty;
            break;
        }
    } /* while(All attempts to read) */


    /* Write a number of elements into the queue towards core Z4B. */
    const unsigned int noNewElements = RND_URAND(0, 10);
    for(u=0; u<noNewElements; ++u)
    {
        static uint8_t SBSS_P1(sqc_) = 0;

        const unsigned int sizeOfElement = RND_URAND(0, M4A_MAX_NO_PAYLOAD_BYTES);

        uint8_t element[sizeOfElement]
              , *pPayload;
              
        /* We have two APIs to write to the queue. We use both, randomly selected. */
        const bool useAPIAlloc = RND_BRAND(0.5f);
        if(useAPIAlloc)
            pPayload = vsq_allocTailElement(m4a_pQTail_z4AToZ4B, sizeOfElement);
        else
            pPayload = &element[0];
        
        bool isQueued = pPayload != NULL;
        if(isQueued)
        {
            /* We can add a checksum, if there's room enough. */
            if(sizeOfElement >= 1)
            {
                /* We can add the sequence counter only, if there's room enough. */
                if(sizeOfElement >= 2)
                {
                    pPayload[1] = sqc_;

                    /* The element is filled with random data. The first byte is however
                       reserved for the checksum and the second one for the sequence
                       counter. */
                    unsigned int u;
                    for(u=2; u<sizeOfElement; ++u)
                        pPayload[u] = RND_URAND(0, 255);
                }

                pPayload[0] = crc_checksumSAEJ1850_8Bit(&pPayload[1], sizeOfElement-1);
            }
        }
        
        /* Try to append the new element. This can fail if the queue is already full. We
           don't need to end our loop if not: In the next cycle we could produce a smaller
           element or something has meanwhile been consumed from the other end of the
           queue. */
        if(useAPIAlloc)
        {
            if(isQueued)
                vsq_postTailElement(m4a_pQTail_z4AToZ4B);
        }
        else
            isQueued = vsq_writeToTail(m4a_pQTail_z4AToZ4B, &pPayload[0], sizeOfElement);
        
        if(isQueued)
        {
            ++ m4a_noQueuedElements;
            m4a_sizeOfQueuedData += sizeOfElement;

            /* The sequence counter relates to all queued elements, including those, which
               are too short to contain it. */
            ++ sqc_;
        }
        else
            ++ m4a_noEvTxQueueFull;

    } /* End for(All newly queued elements) */

    /* Let the LED blink as long as we see any progress. */
    static unsigned long SBSS_P1(noQueuedElements_last_) = 0;
    if(m4a_noQueuedElements - noQueuedElements_last_ >= 2500)
    {
        static bool SBSS_P1(isOn_) = false;
        lbd_setLED(lbd_led_0_DS11, isOn_ = !isOn_);
        noQueuedElements_last_ = m4a_noQueuedElements;
    }

    return 0;

} /* End of task1ms */



/**
 * Task function, cyclically activated every Millisecond. The LED D4 is switched on and
 * off.few seconds to report the status via serial out.
 *   @return
 * If the task function returns a negative value then the task execution is counted as
 * error in the process.
 *   @param PID
 * A user task function gets the process ID as first argument.
 *   @param taskParam
 * A variable task parameter. Here just used for testing.
 */
static int32_t taskReporting(uint32_t PID ATTRIB_UNUSED, uintptr_t taskParam ATTRIB_DBG_ONLY)
{
    assert(taskParam == 0);

    /* Make spinning of the task observable in the debugger. */
    ++ m4a_cntTaskReporting;

#if TASKS_PRODUCE_GROUND_LOAD == 1
    /* Produce a bit of CPU load. This call simulates some true application software. */
    del_delayMicroseconds(/* fullLoadThisNoMicroseconds */ 125000 /* approx. 5% load */);
#endif

    iprintf( "taskReporting:\r\n"
             "  No. queued elements: %lu (%lu/s)\r\n"
             "  Delta Rx elements at core Z4B: %lu\r\n"
             "  Delta Rx elements at core Z2: %lu\r\n"
             "  Delta Rx elements at core Z4A: %lu\r\n"
             "  No. queued bytes: %lu\r\n"
             "  Delta Rx bytes at core Z4B: %lu\r\n"
             "  Delta Rx bytes at core Z2: %lu\r\n"
             "  Delta Rx bytes at core Z4A: %lu\r\n"
             "  No Tx full events on core Z4A: %lu\r\n"
             "  No Tx full events on core Z4B: %lu\r\n"
             "  No Tx full events on core Z2: %lu\r\n"
             "  CPU load [%%]: %u, %u, %u\r\n"
           , m4a_noQueuedElements
           , (unsigned long)(1000ull * m4a_noQueuedElements / m4a_cntTask1ms)
           , m4a_noQueuedElements - m4b_noRxElements
           , m4a_noQueuedElements - mz2_noRxElements
           , m4a_noQueuedElements - m4a_noRxElements
           , m4a_sizeOfQueuedData
           , m4a_sizeOfQueuedData - m4b_sizeOfRxData
           , m4a_sizeOfQueuedData - mz2_sizeOfRxData
           , m4a_sizeOfQueuedData - m4a_sizeOfRxData
           , m4a_noEvTxQueueFull
           , m4b_noEvTxQueueFull
           , mz2_noEvTxQueueFull
           , m4a_cpuLoadZ4A/10, m4b_cpuLoadZ4B/10, mz2_cpuLoadZ2/10
           );

    return 0;

} /* End of taskReporting */



/**
 * Start a secondary core. The initial core is already started and it is the one, which will
 * typically execute this function.
 *   @param idxCore
 * Which core to start? Permitted indexes are 1 for the Z4B core and 2 for the Z2 core.
 *   @param main
 * The main function to be used for the secondary core. Although it might be tempting to
 * have one and the same main function for all cores, the normal function main must not be
 * passed: The compiler generates special code on entry into C's main() to initialize the C
 * runtime. If C's main() is called for more than a single core then this initializtaion
 * happens repeatedly and maybe even coincidentally; either of this can make the runtime
 * fail.
 */
void m4a_startSecondaryCore( unsigned int idxCore
                           , void (*main)(signed int, const char *[])
                           )
{
    /* The entry point into the C code cannot be the normal main function for the other
       cores. The C compiler unconditionally generates the call of the C run time
       initialization as part of the machine code that implements the entry into main. We
       must of course not execute this initialization three times. Instead, we pass the
       main function for the other cores explicitly to the startup code. Although this
       effectively is cross-core communication we don't have any issues with it; the other
       core is not yet running, has not filled any data into its cache and the output write
       buffers of the Z4A core, which executes this code here will be written through its
       cache into external memory long before the other core actually reaches the location
       in the startup code where the function pointer is read. */

    /* Interface with assembly startup code. (There's no header file for the assembly
       startup code.): The entry point into code execution after reset. Common for all
       three cores. */
    extern void _Noreturn sup_startUp(void);

    /* Prepare core start: Enter code start address and set allowed run modes. */
    switch(idxCore)
    {
    case 1 /* Z4B */:
        /* Pass the pointer to the C code entry to the assembly startup code being executed
           on the other core. Note, all core use the same startup code. This code is
           conditionally in that it reads a core dependent address of the C main function
           to use. */
        sup_main_Z4B = main;

        /* Ensure that the value has been written into main memory before we do the mode
           change below. */
        std_fullMemoryBarrier();

        /* RM 38.3.91, p. 1209: The core is enabled in all run modes. Caution, the RM names
           the three cores 1, 2 and 3, while in SW (as the argument of this function does)
           we normally count from zero. */
        MC_ME->CCTL2 = 0x00fc;

        /* RM, 38.3.94, p. 1214: Reset vector for core 1. Why do we need to set this
           vector? It had already been entered in the boot sector, the BAF. */
        MC_ME->CADDR2 = (uint32_t)sup_startUp | MC_ME_CADDR2_RMC(1);

        break;

    case 2 /* Z2 */:
        /* Pass the pointer to the C code entry to the assembly startup code being executed
           on the other core. See case Z4B for details. */
        sup_main_Z2 = main;

        /* Ensure that the value has been written into main memory before we do the mode
           change below. */
        std_fullMemoryBarrier();

        /* RM 38.3.92, p. 1211: The core is enabled in all run modes. */
        MC_ME->CCTL3 = 0x00fc;

        /* RM, 38.3.95, p. 1215: Reset vector for core 2. */
        MC_ME->CADDR3 = (uint32_t)sup_startUp | MC_ME_CADDR3_RMC(1);

        break;

    default:
        /* Core 0 is always running and there are no other cores on this hardware. */
        assert(false);
    }

    /* RM 38.3.94: Another core is started on the next run mode change. We trigger a
       transition from DRUN to DRUN. On return from this function call, the other core is
       running in parallel. */
    ccl_triggerTransitionToModeDRUN();

} /* End of m4a_startSecondaryCore */



/**
 * Entry point into C code. The C main function is entered without arguments and despite
 * of its usual return code definition it must never be left in this environment.
 * (Returning from main would enter an infinite loop in the calling assembler startup
 * code.)
 */
int /* _Noreturn */ main(int noArgs ATTRIB_DBG_ONLY, const char *argAry[] ATTRIB_DBG_ONLY)
{
    assert(noArgs == 1  && strcmp(argAry[0], "Z4A") == 0);

    /* Complete the core HW initialization - as far as not yet done by the assembly startup
       code. */

    /* All clocks run at full speed, including all peripheral clocks. */
    ccl_configureClocks();

    /* Interrupts become usable and configurable by SW. */
    rtos_osInitINTCInterruptController();

    /* Configuration of cross bars: All three cores need efficient access to ROM and RAM.
       By default, the cores generally have strictly prioritized access to all memory slave
       ports in order Z4A, I-Bus, Z4A, D-Bus, Z4B, I-Bus, Z4B, D-Bus, Z2, I-Bus, Z2, D-Bus.
       While not optimal it is at least acceptable for most ports - with the exception of
       slave port 3 of cross bar 0: This port is the only path for the D buses of Z4B and
       Z2 (see RM, 16.1.1, table 16-1, p. 409) and Z2 suffers from starvation if Z4B is
       heavily transferring data. This can happen in our example, where all cores are
       spinning in a tiny loop to increment some counters in RAM. The minimum action to
       take is setting this port to round robin arbitration. */
    xbs_configCrossbarSwitch(/* isZ2IOCore */ true);

    /* The core is now running in the desired state. I/O driver initialization follows to
       the extend required by this simple sample. */

    /* Start the system timers for execution time measurement.
         Caution: On the MPC5748G, this is not an opton but an essential prerequisite for
       running safe-RTOS. The MPC5748G has a simplified z4 core without the timebase
       feature. The system timer is used as substitute. The driver needs to be started and
       it must be neither changed nor re-configured without carefully double-checking the
       side-effects on the kernel! */
    stm_osInitSystemTimers();

    /* Initialize the button and LED driver for the eval board. */
    lbd_osInitLEDAndButtonDriver( /* onButtonChangeCallback_core0 */ NULL
                                , /* PID_core0 */                    0
                                , /* onButtonChangeCallback_core1 */ NULL
                                , /* PID_core1 */                    0
                                , /* onButtonChangeCallback_core2 */ NULL
                                , /* PID_core2 */                    0
                                , /* tiMaxTimeInUs */                1000
                                );

    /* Initialize the serial output channel as prerequisite of using printf. */
    sio_osInitSerialInterface(/* baudRate */ 115200);

    bool initOk = true;

#if ICN_NO_NOTIFICATIONS > 0
    /* Initialize the inter-core notification service. */
    if(icn_osInitInterCoreNotificationDriver() != icn_err_noError)
        initOk = false;
#endif

    /* Register the process initialization tasks. */
    if(rtos_osRegisterInitTask( taskInitProcess
                              , pid_pidTestQueueZ4A
#ifdef DEBUG
                              , /* tiTaskMaxInUs */ 0u
#else
                              , /* tiTaskMaxInUs */ 10000u
#endif
                              )
       != rtos_err_noError
      )
    {
        initOk = false;
    }

    /* Create the events that trigger application tasks at the RTOS. Note, we do not really
       respect the ID, which is assigned to the event by the RTOS API rtos_osCreateEvent().
       The returned value is redundant. This technique requires that we create the events
       in the right order and this requires in practice a double-check by assertion - later
       maintenance errors are unavoidable otherwise. */
    unsigned int idEvent;
    if(rtos_osCreateEvent
                    ( &idEvent
                    , /* tiCycleInMs */              1
                    , /* tiFirstActivationInMs */    0
                    , /* priority */                 prioEv1ms
                    , /* minPIDToTriggerThisEvent */ RTOS_EVENT_NOT_USER_TRIGGERABLE
                    , /* taskParam */                0
                    )
       != rtos_err_noError
      )
    {
        initOk = false;
    }
    else
        assert(idEvent == idEv1ms);

    if(rtos_osRegisterUserTask( idEv1ms
                              , task1ms
                              , pid_pidTestQueueZ4A
                              , /* tiTaskMaxInUs */ 0
                              )
       != rtos_err_noError
      )
    {
        initOk = false;
    }

    if(rtos_osCreateEvent
                    ( &idEvent
                    , /* tiCycleInMs */              2500
                    , /* tiFirstActivationInMs */    1000
                    , /* priority */                 prioEvReporting
                    , /* minPIDToTriggerThisEvent */ RTOS_EVENT_NOT_USER_TRIGGERABLE
                    , /* taskParam */                0
                    )
       != rtos_err_noError
      )
    {
        initOk = false;
    }
    else
        assert(idEvent == idEvReporting);

    if(rtos_osRegisterUserTask( idEvReporting
                              , taskReporting
                              , pid_pidTestQueueZ4A
                              , /* tiTaskMaxInUs */ 0
                              )
       != rtos_err_noError
      )
    {
        initOk = false;
    }

    /* The last check ensures that we didn't forget to register a task. */
    assert(idEvent == noRegisteredEvents-1);

    /* We start the other cores. */
    /// @todo Sort out the race conditions in the implementation of the function to install ISRs
    m4a_startSecondaryCore(/* idxCore */ 1 /* Z4B */, m4b_mainZ4B);
    m4a_startSecondaryCore(/* idxCore */ 2 /* Z2 */, mz2_mainZ2);

    /* Initialize the RTOS kernel. The global interrupt processing is resumed if it
       succeeds. The step involves a configuration check. We must not startup the SW if the
       check fails. */
    if(!initOk ||  rtos_osInitKernel() != rtos_err_noError)
        while(true)
            ;

    /* Installing more interrupts should be possible while the system is already running. */
    osInstallInterruptServiceRoutines();

    /* The code down here becomes our idle task. It is executed when and only when no
       application task or ISR is running. */

    while(true)
    {
        /* Compute the average CPU load. Note, this operation lasts about 1.5s and has a
           significant impact on the cycling speed of this infinite loop. Furthermore, it
           measures only the load produced by the tasks and system interrupts but not that
           of the rest of the code in the idle loop. */
        m4a_cpuLoadZ4A = gsl_osGetSystemLoad();

        /* Communicate some status information to the reporting task running on the boot
           core. */
        const unsigned int stackReserveP2 = rtos_getStackReserve(pid_pidTestQueueZ4B)
                         , stackReserveP3 = rtos_getStackReserve(pid_pidTestQueueZ2);
        m4a_stackReserveP1 = rtos_getStackReserve(pid_pidTestQueueZ4A);
        m4a_stackReserveOS = rtos_getStackReserve(pid_pidOs);
        
        static bool SDATA_OS(isOn_) = true;
        lbd_osSetLED( lbd_led_1_DS10
                    , isOn_ 
                      ||  stackReserveP2 < 512u
                      ||  stackReserveP3 < 512u
                      ||  m4a_stackReserveP1 < 2048u
                      ||  m4a_stackReserveOS < 2048u
                      ||  rtos_getNoTotalTaskFailure(pid_pidTestQueueZ4A) > 0u
                    );
        isOn_ = !isOn_;

        /* Make spinning of the idle task observable in the debugger. */
        ++ m4a_cntTaskIdle;
    }
} /* End of main */

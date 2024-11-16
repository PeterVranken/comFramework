/**
 * @file mz2_mainZ2.c
 * C entry function for the third core, Z2. It runs safe-RTOS with the same simple sample
 * application as core Z4B, "initial". It drives the last available user LED on the
 * evaluation board.
 *
 * Copyright (C) 2020-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
 *   mz2_mainZ2
 * Local functions
 *   isrPit4
 *   isrPit5
 *   isrPit6
 *   osInstallInterruptServiceRoutines
 *   taskInitProcess
 *   task1ms
 *   taskOs1ms
 */

/*
 * Include files
 */

#include "mz2_mainZ2.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <assert.h>

#include "typ_types.h"
#include "crc_checksum.h"
#include "rtos.h"
#include "gsl_systemLoad.h"
#include "lbd_ledAndButtonDriver.h"
#include "sio_serialIO.h"
#include "del_delay.h"
#include "stm_systemTimer.h"
#include "rnd_random.h"
#include "pid_processID.h"
#include "m4a_mainZ4A.h"
#include "m4b_mainZ4B.h"


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
enum
{
    /** Regular timer event. Used for the regular 1ms worker task. */
    idEv1ms = 0,

    /** The number of tasks to register. */
    noRegisteredEvents
};


/** The RTOS uses constant priorities for its events, which are defined here.\n
      Note, the priority is a property of an event rather than of a task. A task implicitly
    inherits the priority of the event it is associated with. */
enum
{
    prioTaskIdle = 0,            /** Prio 0 is implicit, cannot be chosen explicitly */
    prioEv1ms = 1,
};


/*
 * Local prototypes
 */


/*
 * Data definitions
 */

/** Counter of regular 1ms user isr. */
volatile unsigned long UNCACHED_OS(mz2_cntIsr1ms) = 0;

/** Counter of regular 100us user isr. */
volatile unsigned long UNCACHED_OS(mz2_cntIsr100us) = 0;

/** Counter of regular 33us user isr. */
volatile unsigned long UNCACHED_OS(mz2_cntIsr33us) = 0;

/** Counter of cyclic 1ms user task. */
volatile unsigned long SECTION(.uncached.P3.mz2_cntTask1ms) mz2_cntTask1ms = 0;

/** Counter of cyclic 1ms OS task. */
volatile unsigned long SECTION(.uncached.OS.mz2_cntTaskOs1ms) mz2_cntTaskOs1ms = 0;

/** Counter of cycles of infinite main loop. */
volatile unsigned long SECTION(.uncached.OS.mz2_cntTaskIdle) mz2_cntTaskIdle = 0;

/** Counter of notifications , which could not be delivered from Z2 to Z4B because the
    preceding notification had not been fully processed yet. */
volatile unsigned int UNCACHED_OS(mz2_noNotificationsLoss) = 0;

/** Stack reserve of process P3 on the second core. */
volatile unsigned int UNCACHED_OS(mz2_stackReserveP3) = 0;

/** Stack reserve of kernel process on the second core. */
volatile unsigned int UNCACHED_OS(mz2_stackReserveOS) = 0;

/** The average CPU load produced by all ISRs in tens of percent. */
volatile unsigned int UNCACHED_OS(mz2_cpuLoadZ2) = 1000;

/** The queue object under test. It connects core Z2 with Z4B. */
vsq_queueHead_t * UNCACHED_P3(mz2_pQHead_z4BToZ2) = NULL;

/** The queue object under test. It connects core Z2 with Z4A. */
vsq_queueTail_t * UNCACHED_P3(mz2_pQTail_z2ToZ4A) = NULL;

/** Counter of queued elements, i.e. elements sent to core Z4A. */
unsigned long UNCACHED_P3(mz2_noQueuedElements) = 0;

/** Counter of total queued bytes, i.e. bytes sent to core Z4A. */
unsigned long UNCACHED_P3(mz2_sizeOfQueuedData) = 0;

/** The number of times, core Z2 saw an full Tx queue. */
unsigned long UNCACHED_P3(mz2_noEvTxQueueFull) = 0;

/** Counter of elements received from core Z4B. */
unsigned long UNCACHED_P3(mz2_noRxElements) = 0;

/** Counter of total received bytes, i.e. bytes got from core Z4B. */
unsigned long UNCACHED_P3(mz2_sizeOfRxData) = 0;

/** The number of times, core Z2 found an empty Rx queue. */
unsigned long UNCACHED_P3(mz2_noEvRxQueueEmpty) = 0;


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
    static uint32_t SDATA_P3(state_) = 1u;
    state_ = ((state_ * 1103515245U) + 12345U) & 0x7fffffffU;

    return (int)state_;

} /* End of random */


/**
 * A regularly triggered interrupt handler for the timer PIT4. The interrupt does nothing
 * but counting a variable. It is triggered at high frequency and asynchronously to the
 * other ISRs and clocks on the other cores.
 */
static void isrPit4(void)
{
    ++ mz2_cntIsr1ms;

    /* RM 51.4.11, p. 2738f: Acknowledge the timer interrupt in the causing HW device. Can
       be done as this is "trusted code" that is running in supervisor mode. */
    PIT->TIMER[4].TFLG = PIT_TFLG_TIF(1);

} /* End of isrPit4 */



/**
 * A regularly triggered interrupt handler for the timer PIT5. The interrupt does nothing
 * but counting a variable. It is triggered at high frequency and asynchronously to the
 * other ISRs and clocks on the other cores.
 */
static void isrPit5(void)
{
    ++ mz2_cntIsr100us;

    /* RM 51.4.11, p. 2738f: Acknowledge the timer interrupt in the causing HW device. Can
       be done as this is "trusted code" that is running in supervisor mode. */
     PIT->TIMER[5].TFLG = PIT_TFLG_TIF(1);

} /* End of isrPit5 */



/**
 * A regularly triggered interrupt handler for the timer PIT6. The interrupt does nothing
 * but counting a variable. It is triggered at high frequency and asynchronously to the
 * other ISRs and clocks on the other cores.
 */
static void isrPit6(void)
{
    ++ mz2_cntIsr33us;

    /* RM 51.4.11, p. 2738f: Acknowledge the timer interrupt in the causing HW device. Can
       be done as this is "trusted code" that is running in supervisor mode. */
    PIT->TIMER[6].TFLG = PIT_TFLG_TIF(1);

} /* End of isrPit6 */



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
    rtos_osRegisterInterruptHandler( &isrPit4
                                   , processorID
                                   , /* vectorNum */ PIT_Ch4_IRQn
                                   , /* psrPriority */ 1
                                   , /* isPreemptable */ true
                                   );
    rtos_osRegisterInterruptHandler( &isrPit5
                                   , processorID
                                   , /* vectorNum */ PIT_Ch5_IRQn
                                   , /* psrPriority */ 2
                                   , /* isPreemptable */ true
                                   );
    rtos_osRegisterInterruptHandler( &isrPit6
                                   , processorID
                                   , /* vectorNum */ PIT_Ch6_IRQn
                                   , /* psrPriority */ 3
                                   , /* isPreemptable */ true
                                   );

    /* Peripheral clock has been initialized to 40 MHz. The timers count at this rate. To
       get a 1ms interrupt tick we need to count till 40000.
         The RTOS on the other cores operates in ticks of 1ms. We use prime numbers to get
       good asynchronity with that activity.
         -1: See RM, 51.6 */
    PIT->TIMER[4].LDVAL = 39979-1;/* Interrupt rate approx. 1kHz */
    PIT->TIMER[5].LDVAL = 3989-1; /* Interrupt rate approx. 10kHz */
    PIT->TIMER[6].LDVAL = 1321-1; /* Interrupt rate approx. 30kHz */

    /* Enable timer operation. This operation affects all timer channels.
         PIT_MCR_FRZ: For this multi-core MCU it is not so easy to decide whether or not to
       let the timers be stopped on debugger entry: Any stopped core will halt the timers,
       regardless whether that core is related to the timer or not (and how should the
       debugger know...). Both possibilities can be annoying or advantageous, depending on
       the situation. */
    PIT->MCR = PIT_MCR_MDIS(0) | PIT_MCR_FRZ(1);

    /* Clear possibly pending interrupt flags. */
    PIT->TIMER[4].TFLG = PIT_TFLG_TIF(1);
    PIT->TIMER[5].TFLG = PIT_TFLG_TIF(1);
    PIT->TIMER[6].TFLG = PIT_TFLG_TIF(1);

    /* Enable interrupts by the timers and start them. See RM 51.4.10. */
    PIT->TIMER[4].TCTRL = PIT_TCTRL_CHN(0) | PIT_TCTRL_TIE(1) | PIT_TCTRL_TEN(1);
    PIT->TIMER[5].TCTRL = PIT_TCTRL_CHN(0) | PIT_TCTRL_TIE(1) | PIT_TCTRL_TEN(1);
    PIT->TIMER[6].TCTRL = PIT_TCTRL_CHN(0) | PIT_TCTRL_TIE(1) | PIT_TCTRL_TEN(1);

} /* End of osInstallInterruptServiceRoutines */



/**
 * Initialization task of process \a PID.
 *   @return
 * The function returns the Boolean decision, whether the initialization was alright and
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
    bool success = PID == pid_pidTestQueueZ2;
    if(success)
    {
#ifdef DEBUG
        mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
        assert(m4a_mutexStartup.stZ2Initialization == m4a_stInit_start);
        mtx_releaseMutex(&m4a_mutexStartup.mtx);
#endif
        /* Note: Only process 1 has access to the C lib (more precise: to those functions
           of the C lib, which write to lib owned data objects) and can write a status
           message. */

        /* Create our head and our tail object of the two queues, which connect this core
           with the others.
             The macros denote an estimation of the actual object sizes. The estimation
           needs to be not lower and should be not much greater than the true value. */
        #define SIZE_OF_QUEUE_HEAD (8u)
        #define SIZE_OF_QUEUE_TAIL ((MAX_QUEUE_LENGTH)*(M4A_MAX_NO_PAYLOAD_BYTES+8u)+40u)
        static _Alignas(uintptr_t) uint8_t UNCACHED_P3(memPoolHead)[SIZE_OF_QUEUE_HEAD];
        static _Alignas(uintptr_t) uint8_t UNCACHED_P3(memPoolTail)[SIZE_OF_QUEUE_TAIL];
        const unsigned int sizeOfQueueHead = vsq_getSizeOfQueueHead()
                         , sizeOfQueueTail = vsq_getSizeOfQueueTail( MAX_QUEUE_LENGTH
                                                                   , M4A_MAX_NO_PAYLOAD_BYTES
                                                                   );
        if(sizeOfQueueHead <= sizeof(memPoolHead)  &&  sizeOfQueueTail <= sizeof(memPoolTail))
        {
            mz2_pQHead_z4BToZ2 = vsq_createQueueHead(&memPoolHead[0]);
            mz2_pQTail_z2ToZ4A = vsq_createQueueTail( &memPoolTail[0]
                                                    , MAX_QUEUE_LENGTH
                                                    , M4A_MAX_NO_PAYLOAD_BYTES
                                                    );
        }
        else
            success = false;

        /* Signal the other cores that we've created our queue objects. */
        mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
        m4a_mutexStartup.stZ2Initialization = m4a_stInit_objectsCreated;
        mtx_releaseMutex(&m4a_mutexStartup.mtx);

        /* Now wait for completion of all cores' object creation. */
        bool goAhead;
        do
        {
            mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
            goAhead = m4a_mutexStartup.stZ4AInitialization == m4a_stInit_objectsLinked;
            success = m4a_mutexStartup.stZ4AInitialization != m4a_stInit_aborted
                      &&  m4a_mutexStartup.stZ4BInitialization != m4a_stInit_aborted;
            mtx_releaseMutex(&m4a_mutexStartup.mtx);
        }
        while(success && !goAhead);

        if(success)
        {
            assert(m4a_pQHead_z2ToZ4A != NULL  &&  m4a_pQTail_z4AToZ4B != NULL
                   &&  m4b_pQHead_z4AToZ4B != NULL  &&  m4b_pQTail_z4BToZ2 != NULL
                   &&  mz2_pQHead_z4BToZ2 != NULL  &&  mz2_pQTail_z2ToZ4A != NULL
                  );

            /* Now link our head and tail with their counterparts owned by the other cores
               (and residing in a different process' memory). */
            vsq_linkQueueHeadWithTail(mz2_pQHead_z4BToZ2, m4b_pQTail_z4BToZ2);
            vsq_linkQueueTailWithHead(mz2_pQTail_z2ToZ4A, m4a_pQHead_z2ToZ4A);

            /* Signal the other cores that we've completed our queue initialization. */
            mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
            m4a_mutexStartup.stZ2Initialization = m4a_stInit_objectsLinked;
            mtx_releaseMutex(&m4a_mutexStartup.mtx);

            do
            {
                mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
                goAhead = m4a_mutexStartup.stZ4AInitialization == m4a_stInit_objectsLinked
                          &&  m4a_mutexStartup.stZ4BInitialization == m4a_stInit_objectsLinked;
                success = m4a_mutexStartup.stZ4AInitialization != m4a_stInit_aborted
                          &&  m4a_mutexStartup.stZ4BInitialization != m4a_stInit_aborted;
                mtx_releaseMutex(&m4a_mutexStartup.mtx);
            }
            while(success && !goAhead);
        }
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
    ++ mz2_cntTask1ms;

#if TASKS_PRODUCE_GROUND_LOAD == 1
    /* Produce a bit of CPU load. This call simulates some true application software. */
    del_delayMicroseconds(/* fullLoadThisNoMicroseconds */ 50 /* approx. 5% load */);
#endif

    /* Try a number of times to read an element from the queue. Sometimes, but not always,
       we will entirely empty the queue. */
    const unsigned int maxNoReadCycles = RND_URAND(0, 10);
    unsigned int u = 0;
    while(u < maxNoReadCycles)
    {
        static unsigned int SBSS_P3(sizeOfPayload_) = 0;
        static const uint8_t * SBSS_P3(pData_) = NULL;

        /* We can propagate a received element from this core to the next one (the Z4A) only
           if there's room enough in the second queue. If there were no room in the
           previous task invocation then pData_ is still set and we need to push this
           element first. */
        if(pData_ != NULL)
        {
            assert(sizeOfPayload_ <= M4A_MAX_NO_PAYLOAD_BYTES);
            const bool isQueued = vsq_writeToTail(mz2_pQTail_z2ToZ4A, pData_, sizeOfPayload_);
            if(isQueued)
            {
                /* The element from the Z4B has been advanced to the Z4A and we can
                   acknowledge the element by resetting the pointer. */
                pData_ = NULL;

                ++ mz2_noQueuedElements;
                mz2_sizeOfQueuedData += sizeOfPayload_;
            }
            else
            {
                /* Queue to Z4A is full: We do not reset the pointer to the pending element
                   and we won't proceed with reading more elements from the Rx queue from
                   the Z4B. */
                ++ mz2_noEvTxQueueFull;
                break;
            }
        }
        else
        {
            static uint8_t SBSS_P3(sqc_) = 0;
            pData_ = vsq_readFromHead(mz2_pQHead_z4BToZ2, &sizeOfPayload_);
            assert(sizeOfPayload_ <= M4A_MAX_NO_PAYLOAD_BYTES);

            if(pData_ != NULL)
            {
                /* If we have at least one byte of payload then we can validate the
                   checksum. */
                if(sizeOfPayload_ >= 1)
                {
                    const uint8_t crc ATTRIB_DBG_ONLY = crc_checksumSAEJ1850_8Bit
                                                                            ( &pData_[1]
                                                                            , sizeOfPayload_-1
                                                                            );
                    assert(crc == pData_[0]);
                    assert(sizeOfPayload_ <= 1  || pData_[1] == sqc_);

                    mz2_sizeOfRxData += sizeOfPayload_;
                }
                ++ mz2_noRxElements;
                ++ u;

                /* The sequence counter relates to all queued elements, including those,
                   which are too short to contain it. */
                ++ sqc_;
            }
            else
            {
                ++ mz2_noEvRxQueueEmpty;
                break;
            }
        } /* End if(Send already received element or receive next one?) */
    } /* while(All attempts to read) */

    /* Let the LED blink as long as we see any progress. */
    static unsigned long SBSS_P3(noRxElements_last_) = 0;
    if(mz2_noRxElements - noRxElements_last_ >= 2500)
    {
        static bool SBSS_P3(isOn_) = false;
        lbd_setLED(lbd_led_4_DS7, isOn_ = !isOn_);
        noRxElements_last_ = mz2_noRxElements;
    }

    return 0;

} /* End of task1ms */



//    /* Try the inter-core notification driver: We send an event to activate a task on core
//       Z4B. The notification parameter is a simple sequence 0, 1, 2, ..., which is
//       double-checked at the receiver side for validation that all notifications were
//       delivered. */
//    if(icn_osIsNotificationPending(ICN_ID_NOTIFICATION_Z2_TO_Z4B))
//        ++ mz2_noNotificationsLoss;
//    icn_osSendNotification( ICN_ID_NOTIFICATION_Z2_TO_Z4B
//                          , /* notificationParam */ mz2_cntTaskOs1ms-1
//                          );



/**
 * C entry function for the third core, Z2. It runs safe-RTOS with the same simple sample
 * application as core Z4B, "initial". It drives the last available user LED on the
 * evaluation board.
 *   @param noArgs
 * Number of arguments in \a argAry. Is actually always equal to one.
 *   @param argAry
 * Array of string arguments to the function. Actually, always a single string which equals
 * the name of the core, which is started.
 *   @remark
 * Actually, the function is a _Noreturn. We don't declare it as such in order to avoid a
 * compiler warning.
 */
void /* _Noreturn */ mz2_mainZ2( int noArgs ATTRIB_DBG_ONLY
                               , const char *argAry[] ATTRIB_DBG_ONLY
                               )
{
    assert(noArgs == 1  &&  strcmp(argAry[0], "Z2") == 0);

#if 0 /* Here, on the third core, we must not make use of the serial output. It is
         basically alright to make use of the sio API but blocking by busy wait is involved
         with hard to predict impact on the RTOS timing. Moreover, the use of the C library
         is strongly deprecated - it is not proven that it has been configured and compiled
         for multi-core use. Simple functions without static data will properly work, but
         nothing can be said about the printf and stream function families, which make use
         of buffers and heap. */
    #define GREETING "Hello World\r\n"
    sio_osWriteSerial(GREETING, /* noBytes */ sizeof(GREETING)-1);
    puts("puts saying " GREETING);
    printf("printf saying %s", GREETING);
    #undef GREETING
#endif

    /* Register the process initialization tasks. */
    bool initOk = true;
    if(rtos_osRegisterInitTask( taskInitProcess
                              , pid_pidTestQueueZ2
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
    if(initOk
       &&  rtos_osCreateEvent( &idEvent
                             , /* tiCycleInMs */              1
                             , /* tiFirstActivationInMs */    0
                             , /* priority */                 prioEv1ms
                             , /* minPIDToTriggerThisEvent */ RTOS_EVENT_NOT_USER_TRIGGERABLE
                             , /* taskParam */                0
                             )
           == rtos_err_noError
      )
    {
        assert(idEvent == idEv1ms);
    }
    else
        initOk = false;

    if(initOk
       && rtos_osRegisterUserTask( idEv1ms
                                 , task1ms
                                 , pid_pidTestQueueZ2
                                 , /* tiTaskMaxInUs */ 0
                                 )
          != rtos_err_noError
      )
    {
        initOk = false;
    }

    /* The last check ensures that we didn't forget to register a task. */
    assert(idEvent == noRegisteredEvents-1);

    /* Configure the interrupts, which we have just to produce some load and disturbance. */
    osInstallInterruptServiceRoutines();

    /* Initialize the RTOS kernel. The global interrupt processing is resumed if it
       succeeds. The step involves a configuration check. We must not startup the SW if the
       check fails. */
    if(!initOk ||  rtos_osInitKernel() != rtos_err_noError)
        while(true)
            ;

    /* The rest of the code is placed in an infinite loop; it becomes the RTOS' idle task. */
    while(true)
    {
        /* Compute the average CPU load. Note, this operation lasts about 1.5s and has a
           significant impact on the cycling speed of this infinite loop. Furthermore, it
           measures only the load produced by the interrupts but not that of the rest of
           the code in the idle loop. */
        mz2_cpuLoadZ2 = gsl_osGetSystemLoad();

        /* Communicate some status information to the reporting task running on the boot
           core. */
        const unsigned int stackReserveP1 = rtos_getStackReserve(pid_pidTestQueueZ4A)
                         , stackReserveP2 = rtos_getStackReserve(pid_pidTestQueueZ4B);
        mz2_stackReserveP3 = rtos_getStackReserve(pid_pidTestQueueZ2);
        mz2_stackReserveOS = rtos_getStackReserve(pid_pidOs);

        static bool SDATA_OS(isOn_) = true;
        lbd_osSetLED( lbd_led_5_DS6
                    , isOn_
                      ||  stackReserveP1 < 512u
                      ||  stackReserveP2 < 512u
                      ||  mz2_stackReserveP3 < 2048u
                      ||  mz2_stackReserveOS < 2048u
                      ||  rtos_getNoTotalTaskFailure(pid_pidTestQueueZ2) > 0u
                    );
        isOn_ = !isOn_;

        /* Make spinning of the idle task observable in the debugger. */
        ++ mz2_cntTaskIdle;

    } /* End of inifinite idle loop of bare metal application. */

} /* End of mz2_mainZ2 */

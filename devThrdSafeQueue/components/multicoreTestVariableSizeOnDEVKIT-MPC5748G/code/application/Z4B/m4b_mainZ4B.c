/**
 * @file m4b_mainZ4B.c
 * C entry function for the second core running safe-RTOS. By compile-time configuration,
 * this can be either the Z4B or the Z2. The main function starts the safe-RTOS kernel on
 * the chosen core.\n
 *   Two regular tasks are spinning and driving an LED each. A third LED is commanded by
 * the idle task. Only if three LEDs are blinking everything is alright.\n
 *   Progress information is permanently written into the serial output channel. A terminal
 * on the development host needs to use these settings: 115000 Bd, 8 Bit data word, no
 * parity, 1 stop bit.
 *
 * Copyright (C) 2018-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
 *   m4b_onButtonChangeCallback
 *   m4b_mainZ4B
 * Local functions
 *   injectError
 *   taskInitProcess
 *   taskNotificationNewData
 *   task1ms
 *   taskOs1ms
 */

/*
 * Include files
 */

#include "m4b_mainZ4B.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <assert.h>

#include "MPC5748G.h"
#include "typ_types.h"
#include "crc_checksum.h"
#include "rtos.h"
#include "sio_serialIO.h"
#include "gsl_systemLoad.h"
#include "lbd_ledAndButtonDriver.h"
#include "del_delay.h"
#include "pid_processID.h"
#include "rnd_random.h"
#include "mz2_mainZ2.h"
#include "m4a_mainZ4A.h"


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

/** The enumeration of all events, tasks and priorities, to have them as symbols in the
    source code. Most relevant are the event IDs. Actually, these IDs are provided by the
    RTOS at runtime, when creating the event. However, it is guaranteed that the IDs, which
    are dealt out by rtos_osCreateEvent() form the series 0, 1, 2, .... So we don't need
    to have a dynamic storage of the IDs; we define them as constants and double-check by
    assertion that we got the correct, expected IDs from rtos_osCreateEvent(). Note, this
    requires that the order of creating the events follows the order here in the
    enumeration.\n
      Here, we have the IDs of the created events. They occupy the index range starting
    from zero. */
enum
{
    /** Regular timer event. */
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
    prioEv1ms,
};


/*
 * Local prototypes
 */


/*
 * Data definitions
 */

/** Counter of cyclic 1ms user task. */
unsigned long UNCACHED_P2(m4b_cntTask1ms) = 0;

/** Counter of cycles of infinite main loop. */
unsigned long UNCACHED_OS(m4b_cntTaskIdle) = 0;

/** The queue object under test. It connects core Z4B with Z4A. */
vsq_queueHead_t * UNCACHED_P2(m4b_pQHead_z4AToZ4B) = NULL;

/** The queue object under test. It connects core Z4B with Z2. */
vsq_queueTail_t * UNCACHED_P2(m4b_pQTail_z4BToZ2) = NULL;

/** Counter of queued elements, i.e. elements sent to core Z2. */
unsigned long UNCACHED_P2(m4b_noQueuedElements) = 0;

/** Counter of total queued bytes, i.e. bytes sent to core Z2. */
unsigned long UNCACHED_P2(m4b_sizeOfQueuedData) = 0;

/** The number of times, core Z4B saw an full Tx queue. */
unsigned long UNCACHED_P2(m4b_noEvTxQueueFull) = 0;

/** Counter of elements received from core Z4A. */
unsigned long UNCACHED_P2(m4b_noRxElements) = 0;

/** Counter of total received bytes, i.e. bytes got from core Z4A. */
unsigned long UNCACHED_P2(m4b_sizeOfRxData) = 0;

/** The number of times, core Z4B found an empty queue. */
unsigned long UNCACHED_P2(m4b_noEvRxQueueEmpty) = 0;

/** Stack reserve of process P2 on the second core. */
unsigned int UNCACHED_OS(m4b_stackReserveP2) = 0;

/** Stack reserve of kernel process on the second core. */
unsigned int UNCACHED_OS(m4b_stackReserveOS) = 0;

/** The average CPU load produced by all tasks and interrupts in tens of percent. */
unsigned int SECTION(.uncached.OS.m4b_cpuLoadZ4B) m4b_cpuLoadZ4B = 1000;


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
    static uint32_t SDATA_P2(state_) = 1u;
    state_ = ((state_ * 1103515245U) + 12345U) & 0x7fffffffU;

    return (int)state_;

} /* End of random */


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
    bool success = PID == pid_pidTestQueueZ4B;
    if(success)
    {
#ifdef DEBUG
        mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
        assert(m4a_mutexStartup.stZ4BInitialization == m4a_stInit_start);
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
        static _Alignas(uintptr_t) uint8_t UNCACHED_P2(memPoolHead)[SIZE_OF_QUEUE_HEAD];
        static _Alignas(uintptr_t) uint8_t UNCACHED_P2(memPoolTail)[SIZE_OF_QUEUE_TAIL];
        const unsigned int sizeOfQueueHead = vsq_getSizeOfQueueHead()
                         , sizeOfQueueTail = vsq_getSizeOfQueueTail( MAX_QUEUE_LENGTH
                                                                   , M4A_MAX_NO_PAYLOAD_BYTES
                                                                   );
        if(sizeOfQueueHead <= sizeof(memPoolHead)  &&  sizeOfQueueTail <= sizeof(memPoolTail))
        {
            m4b_pQHead_z4AToZ4B = vsq_createQueueHead(&memPoolHead[0]);
            m4b_pQTail_z4BToZ2 = vsq_createQueueTail( &memPoolTail[0]
                                                    , MAX_QUEUE_LENGTH
                                                    , M4A_MAX_NO_PAYLOAD_BYTES
                                                    );
        }
        else
            success = false;

        /* Signal the other cores that we've created our queue objects. */
        mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
        m4a_mutexStartup.stZ4BInitialization = m4a_stInit_objectsCreated;
        mtx_releaseMutex(&m4a_mutexStartup.mtx);

        /* Now wait for completion of all cores' object creation. */
        bool goAhead;
        do
        {
            mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
            goAhead = m4a_mutexStartup.stZ4AInitialization == m4a_stInit_objectsLinked;
            success = m4a_mutexStartup.stZ4AInitialization != m4a_stInit_aborted
                      &&  m4a_mutexStartup.stZ2Initialization != m4a_stInit_aborted;
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
            vsq_linkQueueHeadWithTail(m4b_pQHead_z4AToZ4B, m4a_pQTail_z4AToZ4B);
            vsq_linkQueueTailWithHead(m4b_pQTail_z4BToZ2, mz2_pQHead_z4BToZ2);

            /* Signal the other cores that we've completed our queue initialization. */
            mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
            m4a_mutexStartup.stZ4BInitialization = m4a_stInit_objectsLinked;
            mtx_releaseMutex(&m4a_mutexStartup.mtx);

            do
            {
                mtx_acquireMutex(&m4a_mutexStartup.mtx, /* wait */ true);
                goAhead = m4a_mutexStartup.stZ4AInitialization == m4a_stInit_objectsLinked
                          &&  m4a_mutexStartup.stZ2Initialization == m4a_stInit_objectsLinked;
                success = m4a_mutexStartup.stZ4AInitialization != m4a_stInit_aborted
                          &&  m4a_mutexStartup.stZ2Initialization != m4a_stInit_aborted;
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
    ++ m4b_cntTask1ms;

#if TASKS_PRODUCE_GROUND_LOAD == 1
    /* Produce a bit of CPU load. This call simulates some true application software. */
    del_delayMicroseconds(/* fullLoadThisNoMicroseconds */ 50 /* approx. 5% load */);
#endif

    /* Try a number of times to read an element from the queue. Sometimes, but not always,
       we will entirely empty the queue. The range of the random number is a bit larger
       than in the corresponding Tx loop on core Z4A so that we expect to see a higher rate
       of empty queue events than full events. */
    const unsigned int maxNoReadCycles = RND_URAND(0, 11);
    unsigned int u = 0;
    while(u < maxNoReadCycles)
    {
        static unsigned int SBSS_P2(sizeOfPayload_) = 0;
        static const uint8_t * SBSS_P2(pData_) = NULL;

        /* We can propagate a received element from this core to the next one (the Z2) only
           if there's room enough in the second queue. If there were no room in the
           previous task invocation then pData_ is still set and we need to push this
           element first. */
        if(pData_ != NULL)
        {
            assert(sizeOfPayload_ <= M4A_MAX_NO_PAYLOAD_BYTES);
            const bool isQueued = vsq_writeToTail(m4b_pQTail_z4BToZ2, pData_, sizeOfPayload_);
            if(isQueued)
            {
                /* The element from the Z4A has been advanced to the Z2 and we can
                   acknowledge the element by resetting the pointer. */
                pData_ = NULL;

                ++ m4b_noQueuedElements;
                m4b_sizeOfQueuedData += sizeOfPayload_;
            }
            else
            {
                /* Queue to Z2 is full: We do not reset the pointer to the pending element
                   and we won't proceed with reading more elements from the Rx queue from
                   the Z4A. */
                ++ m4b_noEvTxQueueFull;
                break;
            }
        }
        else
        {
            static uint8_t SBSS_P2(sqc_) = 0;
            pData_ = vsq_readFromHead(m4b_pQHead_z4AToZ4B, &sizeOfPayload_);
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

                    m4b_sizeOfRxData += sizeOfPayload_;
                }
                ++ m4b_noRxElements;
                ++ u;

                /* The sequence counter relates to all queued elements, including those,
                   which are too short to contain it. */
                ++ sqc_;
            }
            else
            {
                ++ m4b_noEvRxQueueEmpty;
                break;
            }
        } /* End if(Send already received element or receive next one?) */
    } /* while(All attempts to read) */

    /* Let the LED blink as long as we see any progress. */
    static unsigned long SBSS_P2(noRxElements_last_) = 0;
    if(m4b_noRxElements - noRxElements_last_ >= 2500)
    {
        static bool SBSS_P2(isOn_) = false;
        lbd_setLED(lbd_led_2_DS9, isOn_ = !isOn_);
        noRxElements_last_ = m4b_noRxElements;
    }

    return 0;

} /* End of task1ms */



/**
 * C entry function main. Is used for the second core running safe-RTOS. It depends on
 * configuration macros #RTOS_RUN_SAFE_RTOS_ON_CORE_1 and #RTOS_RUN_SAFE_RTOS_ON_CORE_2,
 * which one that is.
 *   @param noArgs
 * Number of arguments in \a argAry. Is actually always equal to one.
 *   @param argAry
 * Array of string arguments to the function. Actually, always a single string which equals
 * the name of the core, which is started.
 *   @remark
 * Actually, the function is a _Noreturn. We don't declare it as such in order to avoid a
 * compiler warning.
 */
void /* _Noreturn */ m4b_mainZ4B( int noArgs ATTRIB_DBG_ONLY
                                , const char *argAry[] ATTRIB_DBG_ONLY
                                )
{
    assert(noArgs == 1  &&  strcmp(argAry[0], "Z4B") == 0);

#if 0 /* Here, on the second core, we must not make use of the serial output. It is
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
                              , pid_pidTestQueueZ4B
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
       &&  rtos_osRegisterUserTask( idEv1ms
                                  , task1ms
                                  , pid_pidTestQueueZ4B
                                  , /* tiTaskMaxInUs */ 0
                                  )
           != rtos_err_noError
      )
    {
        initOk = false;
    }

    /* The last check ensures that we didn't forget to register a task. */
    assert(idEvent == noRegisteredEvents-1);

    /* Initialize the RTOS kernel. The global interrupt processing is resumed if it
       succeeds. The step involves a configuration check. We must not startup the SW if the
       check fails. */
    if(!initOk ||  rtos_osInitKernel() != rtos_err_noError)
        while(true)
            ;

    /* The code down here becomes the idle task of the RTOS. We enter an infinite loop,
       where some background can be placed. */
    while(true)
    {
        /* Compute the average CPU load. Note, this operation lasts about 1s and has a
           significant impact on the cycling speed of this infinite loop. Furthermore, it
           measures only the load produced by the tasks and system interrupts but not that
           of the rest of the code in the idle loop. */
        m4b_cpuLoadZ4B = gsl_osGetSystemLoad();

        /* Communicate some status information to the reporting task running on the boot
           core. */
        const unsigned int stackReserveP1 = rtos_getStackReserve(pid_pidTestQueueZ4A)
                         , stackReserveP3 = rtos_getStackReserve(pid_pidTestQueueZ2);
        m4b_stackReserveP2 = rtos_getStackReserve(pid_pidTestQueueZ4B);
        m4b_stackReserveOS = rtos_getStackReserve(pid_pidOs);

        static bool SDATA_OS(isOn_) = true;
        lbd_osSetLED( lbd_led_3_DS8
                    , isOn_
                      ||  stackReserveP1 < 512u
                      ||  stackReserveP3 < 512u
                      ||  m4b_stackReserveP2 < 2048u
                      ||  m4b_stackReserveOS < 2048u
                      ||  rtos_getNoTotalTaskFailure(pid_pidTestQueueZ4B) > 0u
                    );
        isOn_ = !isOn_;

        /* Make spinning of the idle task observable in the debugger. */
        ++ m4b_cntTaskIdle;

    } /* End of inifinite idle loop of RTOS. */

} /* End of m4b_mainZ4B */

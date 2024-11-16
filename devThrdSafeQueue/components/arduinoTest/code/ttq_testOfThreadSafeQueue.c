/**
 * @file ttq_testOfThreadSafeQueue.c
 *
 * This is the main entry point into the application. The application implements a
 * test of the thread-safe queue. Three pairs of communication partners use a queue object
 * each and send and validate their individual telegrams. The three pairs implement all
 * possible priority relationships of prducer and consumer, low to high, vice versa and
 * same priorities (with preemption due to Round Robin scheduling).\n
 *   The test is running infinitely and the feedback is a simple cycle counter and error
 * counters shown on the Arduino LCD and written into the serial port.\n
 *   Initialization of the RTOS, root task functions and the idle loop of the RTOS are
 * implemented here.
 *
 * Copyright (C) 2015-2016 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
 *
 * Module interface
 *   setup
 *   loop
 * Local functions
 *   blink
 *   onDataError
 *   producerH
 *   relayL
 *   consumerH
 *   producerM
 *   consumerM
 *   taskProducerH
 *   taskRelayL
 *   taskConsumerH
 *   taskProducerM
 *   taskConsumerM
 *   taskDisplay
 */

/*
 * Include files
 */

#include <stdlib.h>
#include <Arduino.h>
#include "types.h"
#include "rtos.h"
#include "rtos_assert.h"
#include "gsl_systemLoad.h"
#include "stdout.h"
#include "dpy_display.h"
#include "aev_applEvents.h"
#include "crc_checksum.h"
#include "tsq_threadSafeQueue.h"


/*
 * Defines
 */

/** Debugging support: Normally all results are printed from the idle task. The use of
    printf for this reporting can be inhibited in order to make the stdout stream and the
    Serial object behind printf (temporarily) available to other tasks for sake of program
    debugging. */
#define ENABLE_PRINTF_FOR_IDLE_TASK     1

/** Common stack size of all the tasks. */
#define STACK_SIZE_TASK 300

/** Pin 13 has an LED connected on most Arduino boards. */
#define LED 13

/** The index to the task objects as needed for requesting the overrun counter or the stack
    usage.
      @remark The order of enumeration values matters: The task indexes are ordered as the
    initialization calls in \a setup. */
enum { idxTaskProducerH = 0
     , idxTaskRelayL
     , idxTaskConsumerH
     , idxTaskProducerM
     , idxTaskConsumerM
     , idxTaskDisplay
     , noTasks
     };


/** The length of the queue * \a _pQueueH2L. */
#define MAX_LENGTH_QUEUE_H2L 8

/** The length of the queue * \a _pQueueL2H. */
#define MAX_LENGTH_QUEUE_L2H 5

/** The length of the queue * \a _pQueueM2M. */
#define MAX_LENGTH_QUEUE_M2M 7


/*
 * Local type definitions
 */

/** This is the data structure, which is passed from producer to consumer. It's the same
    kind of object in all queues in use. */
typedef struct telegram_t
{
    /** The checkum protected data contents of the telegram. */
    struct payload_t
    {
        /** An cyclic counter. Incremented with each sent telegram and used to detect
            sequence errors in the test. */
        uint16_t sqc;

        /** Some random data. */
        uint8_t rndAry[10];

    } payload;

    /** The checksum of the complete telegram. Used to detect unwanted data changed
        during telegram delivery. */
    uint8_t checksum;

} telegram_t;


/*
 * Local prototypes
 */


/*
 * Data definitions
 */

/* Results of the idle task. */
volatile uint8_t _cpuLoad = 200;

/* Count the loops of the tasks. */
volatile unsigned long _cntProducerH = 0
                     , _cntConsumerH = 0
                     , _cntProducerM = 0
                     , _cntConsumerM = 0
                     , _cntRelayL = 0;

/* Count the data processing related events. */
volatile unsigned long _noWrittenElementsQueueH2L = 0
                     , _noEventsQueueH2LFull = 0
                     , _noReadElementsQueueL2H = 0
                     , _noEventsQueueL2HFull = 0
                     , _noWrittenElementsQueueM2M = 0
                     , _noEventsQueueM2MFull = 0
                     , _noReadElementsQueueM2M = 0;

/** Count the data errors seen during run of test. We use a signed type since we start with
    a negative value. We purposely inject a few errors at the beginning to validate the
    error recognition itself. After a while the test should permanently show zero errors.\n
      A short type suffices, we use a saturating counter. */
volatile int8_t _noDataErrors = -20;

/** The queue object that connects the producer task at high priority with the relay
    task at low priority. */
static tsq_queue_t *_pQueueH2L = NULL;

/** The queue object that connects the relay task at low priority with the consumer
    task at high priority. */
static tsq_queue_t *_pQueueL2H = NULL;

/** The queue object that connects the two round robin tasks of same priority. */
static tsq_queue_t *_pQueueM2M = NULL;


/*
 * Function implementation
 */

/**
 * Trivial routine that flashes the LED a number of times to give simple feedback. The
 * routine is blocking.
 *   @param noFlashes
 * The number of times the LED is lit.
 */
static void blink(uint8_t noFlashes)
{
#define TI_FLASH 150

    while(noFlashes-- > 0)
    {
        digitalWrite(LED, HIGH);  /* Turn the LED on. (HIGH is the voltage level.) */
        delay(TI_FLASH);          /* The flash time. */
        digitalWrite(LED, LOW);   /* Turn the LED off by making the voltage LOW. */
        delay(TI_FLASH);          /* Time between flashes. */
    }
    delay(1000-TI_FLASH);         /* Wait for a second after the last flash - this command
                                     could easily be invoked immediately again and the
                                     bursts need to be separated. */
#undef TI_FLASH
}



/** 
 * Report a data error.
 */

static inline void onDataError()
{
    cli();
    if(_noDataErrors < 127)
        ++ _noDataErrors;
    sei();
    
} /* End of onDataError */



/**
 * The high priority data producer. This function produces the initial input to the data
 * flow from the high priority producer via the low priority relay to the high priority
 * consumer.\n
 *   If the queue has space left a data element is filled randomly and tagged with sequence
 * counter and checksum to enable later validation at the final receiver.
 *   @return
 * \a true, if the queue had space to produce a data element, \a false otherwise.
 */

bool producerH()
{
    /* We keep the data telegram static to have the last recent sequence counter value. */
    static telegram_t t = {.payload = {.sqc = (typeof(t.payload.sqc))-1}};
    static bool telegramHasSent = true;

#ifdef TSQ_ENABLE_API_QUEUE_DIAGNOSTICS
    ASSERT(tsq_getMaximumQueueUsage(_pQueueH2L) <= MAX_LENGTH_QUEUE_H2L);
    ASSERT(_noEventsQueueH2LFull == 0
           ||  tsq_getMaximumQueueUsage(_pQueueH2L) == MAX_LENGTH_QUEUE_H2L
          );
#endif

    /* Queue write API 1: Prepare object in a local buffer and write it at once to the tail
       of the queue. */

    if(telegramHasSent)
    {
        /* Last recently prepared data has been queued and we need a new one.
             Fill the random part of the telegram and increment sequence counter
           cyclically. */
        unsigned int u;
        for(u=0; u<sizeof(t.payload.rndAry)/sizeof(t.payload.rndAry[0]); ++u)
            t.payload.rndAry[u] = rand();
        ++ t.payload.sqc;

        /* Compute checksum on payload of telegram. */
        t.checksum = crc_checksumSAEJ1850_8Bit(&t.payload, sizeof(t.payload));
        
        /* At the beginning, we inject a few errors to validate the test procedure itself. */
        static uint16_t cntErrInjection = /* noInjectedErrors */ 10
                                          * 0x400 - 1;
        if(cntErrInjection > 0)
        {
            /* Inject an error every 1024 cycles. */
            if((cntErrInjection & 0x3ff) == 0x001)
                ++ t.checksum;    

            -- cntErrInjection;
        }
    }

    /* Try to queue the telegram. Due to the race conditions between producer and consumer
       it's useless to query first if there is space enough. Just try. If the queue is full
       we keep the telegram unchanged as a static element and will retry queuing it in
       the next cycle. */
    telegramHasSent = tsq_writeToTail(_pQueueH2L, /* pData */ &t, sizeof(telegram_t));

    return telegramHasSent;

} /* End of producerH */




/** 
 * The relay operation: The outbound queue is checked for space. If available then the
 * inbound queue is queried for received data telegrams. If any then it is copied into the
 * outbound queue. This operation is repeated until either the outbound queue is full or
 * the inbound doesn't have any more received elements.\n
 *   The relay doesn't do any data validation itself, it just propagates the telegrams from
 * the high priority producer to the high priority (final) consumer.
 *   @return
 * \a true if the function returns because all inbound elements could be propagated into
 * the outbound queue and \a false if the function was left since there was no space in the
 * outbound queue. (\a is even returned if the outbounmd queue was full but there was no
 * inbound element to be propagated.)
 */

static bool relayL()
{
    /* Queue write API 2 is applied for accessing the outbound queue: Allocate room in the
       queue and produce the data in place. Save copying the element data. This means that
       we inspect the inbound queue only if the outbound has space. If the outbound has
       space but there's nothing received in the inbound, then the reservation in the
       outbound queue is kept till the next tick of the relay operation. (The space
       reservation of a queue has no time limit.) */
    while(true)
    {
        static telegram_t *pTOut = NULL;
        
        /* If our static pointer is not NULL then we have a still unused free element in
           the outbound queue and we must not try to allocate one. */
        if(pTOut == NULL)
            pTOut = (telegram_t*)tsq_allocTailElement(_pQueueL2H);

        if(pTOut == NULL)
        {
            /* There's no room in the outbound queue, nothing can be propagated. Return and
               wait for the next tick. */
            return false;
        }
            
        /* Get the next element from the inbound queue if any is available. The trick:
           Since we first allocated a free element in the outbound queue we can now let the
           queue-read operation write directly into this element. No further local buffer
           is required and one copy operation can be safed. */
        const telegram_t * const pTIn = tsq_readFromHead(_pQueueH2L);
        if(pTIn == NULL)
        {
            /* There's no further element in the inbound queue, nothing can be propagated
               for now. Return and wait for the next tick. pTOut remains valid. */
            return true;
        }
        
        /* Copy the data directly from the inbound into the outbound queue. */
        memcpy(pTOut, pTIn, sizeof(telegram_t));

        /* Submit the element in the outbound queue. Reset pTOut to acknowldege this for
           our next loop. */
        tsq_postTailElement(_pQueueL2H);
        pTOut = NULL;
        
    } /* End while there are still elements, which can be propagated. */
    
} /* End of relayL */




/**
 * The consumer of the data at the end of the chained flow. This function validates the
 * data elements from the high priority producer and transmitted via the relay at low
 * priority.\n
 *   All recognized data errors are counted using \a onDataError.
 */

void consumerH()
{
    static unsigned int sqcRef = 0;
    
    while(true)
    {
        const telegram_t * const pT = tsq_readFromHead(_pQueueL2H);
        if(pT == NULL)
            break;
        
        /* Code as critical as possible for this test: read checksum, which is written last
           by the producer, first. */
        const uint8_t checksumRef = pT->checksum;
        
        /* Count reception event. */
        cli();
        ++ _noReadElementsQueueL2H;
        sei();
        
        if(checksumRef == crc_checksumSAEJ1850_8Bit(&pT->payload, sizeof(pT->payload)))
        {
            if(sqcRef == pT->payload.sqc)
            {
                /* Next reception: We need to see the cyclically next value for the sequence
                   counter. */
                ++ sqcRef;
            }
            else
            {
                onDataError();
                
                /* Next reception: We need to see the cyclic successor of the received
                   value for the sequence counter. */
                sqcRef = pT->payload.sqc+1;
            }             
        }
        else
        {
            onDataError();

            /* Next reception: We expect to see the cyclically next value for the sequence
               counter. */
            ++ sqcRef;
        }
    }
} /* End of consumerH */



/**
 * The medium priority data producer. This function produces the input to the data
 * flow between the two medium priority producers. This connection is a test of both ends
 * of the queue being served by contexts of identical priority. Mutual exclusion is still
 * an issue since the two tasks are scheduled in round robin pattern; they can preempt
 * one another.\n
 *   If the queue has space left a data element is filled randomly and tagged with sequence
 * counter and checksum to enable validation at the receiver.
 *   @return
 * \a true, if the queue had space to produce a data element, \a false otherwise.
 */

bool producerM()
{
    /* We keep the data telegram static to have the last recent sequence counter value.
       Using an unexpected initial value for the sequence counter is the first injected
       error. */
    static telegram_t t = {.payload = {.sqc = 22 /* instead of (typeof(t.payload.sqc))-1 */}};
    static bool telegramHasSent = true;

#ifdef TSQ_ENABLE_API_QUEUE_DIAGNOSTICS
    ASSERT(tsq_getMaximumQueueUsage(_pQueueM2M) <= MAX_LENGTH_QUEUE_M2M);
    ASSERT(_noEventsQueueM2MFull == 0
           ||  tsq_getMaximumQueueUsage(_pQueueM2M) == MAX_LENGTH_QUEUE_M2M
          );
#endif

    /* Queue write API 1: Prepare object in a local buffer and write it at once to the tail
       of the queue. */

    if(telegramHasSent)
    {
        /* The last recently prepared data has been queued and we need a new telegram.
             Fill the random part of the telegram and increment sequence counter
           cyclically. */
        unsigned int u;
        for(u=0; u<sizeof(t.payload.rndAry)/sizeof(t.payload.rndAry[0]); ++u)
            t.payload.rndAry[u] = rand();
        ++ t.payload.sqc;

        /* Compute checksum on payload of telegram. */
        t.checksum = crc_checksumSAEJ1850_8Bit(&t.payload, sizeof(t.payload));
        
        /* At the beginning, we inject a few errors to validate the test procedure itself. */
        static uint16_t cntErrInjection = /* noInjectedErrors */ 9
                                          * 0x400 - 1;
        if(cntErrInjection > 0)
        {
            /* Inject an error every 1024 cycles. */
            if((cntErrInjection & 0x3ff) == 0x001)
                ++ t.checksum;    

            -- cntErrInjection;
        }
    }

    /* Try to queue the telegram. Due to the race conditions between producer and consumer
       it's useless to query first if there is space enough. Just try. If the queue is full
       we keep the telegram unchanged as a static element and will retry queuing it in
       the next cycle. */
    telegramHasSent = tsq_writeToTail(_pQueueM2M, /* pData */ &t, sizeof(telegram_t));

    return telegramHasSent;

} /* End of producerM */




/**
 * The consumer of the same-priority connection at medium priority. This function validates
 * the data elements from the preempting producer of identical priority.\n
 *   All recognized data errors are counted using \a onDataError.
 */

void consumerM()
{
    static unsigned int sqcRef = 0;
    
    while(true)
    {
        const telegram_t * const pT = tsq_readFromHead(_pQueueM2M);
        if(pT == NULL)
            break;
        
        /* Code as critical as possible for this test: read checksum, which is written last
           by the producer, first. */
        const uint8_t checksumRef = pT->checksum;
        
        /* Count reception event. */
        cli();
        ++ _noReadElementsQueueM2M;
        sei();
        
        if(checksumRef == crc_checksumSAEJ1850_8Bit(&pT->payload, sizeof(pT->payload)))
        {
            if(sqcRef == pT->payload.sqc)
            {
                /* Next reception: We need to see the cyclically next value for the sequence
                   counter. */
                ++ sqcRef;
            }
            else
            {
                onDataError();
                
                /* Next reception: We need to see the cyclic successor of the received
                   value for the sequence counter. */
                sqcRef = pT->payload.sqc+1;
            }             
        }
        else
        {
            onDataError();

            /* Next reception: We expect to see the cyclically next value for the sequence
               counter. */
            ++ sqcRef;
        }
    }
} /* End of consumerM */



/**
 * The root function of the producer task of high priority. Irregular timing is
 * implemented using the random function.
 *   @param initialResumeCondition
 * The vector of events which made the task due the very first time.
 */

static void taskProducerH(uint16_t initialResumeCondition)
{
    ASSERT(initialResumeCondition == RTOS_EVT_DELAY_TIMER);

    /* The producer is in charge of creating the queue object. After creation he awakes the
       communication peer, which will then assume the existance and operability of the same
       object. */
    const unsigned int sizeOfQueue = tsq_getSizeOfQueue
                                            ( MAX_LENGTH_QUEUE_H2L
                                            , /* maxElementSize */ sizeof(telegram_t)
                                            , /* alignOfElement */ 1
                                            );
    uint8_t memChunk[sizeOfQueue];
    _pQueueH2L = tsq_createQueue( memChunk
                                , MAX_LENGTH_QUEUE_H2L
                                , /* maxElementSize */ sizeof(telegram_t)
                                , /* alignOfElement */ 1
                                );

    /* Notify the communication peer that the connecting queue object is ready to use. */
    rtos_sendEvent(EVT_QUEUE_H2L_READY);

    uintTime_t noTicksTillNextProduction;
    do
    {
        /* Apply atomic read-modify-write operation to safely increment the task counter. */
        cli();
        ++ _cntProducerH;
        sei();

        /* Do data production if queue is not full or count the queue full event otherwise.
           The result is written into the queue, which is connected to the relay task at
           low priority. */
        if(producerH())
        {
            cli();
            ++ _noWrittenElementsQueueH2L;
            sei();
        }
        else
        {
            cli();
            ++ _noEventsQueueH2LFull;
            sei();
        }

        /* The time till next production is chosen randomly. */
        noTicksTillNextProduction = 1 + rand()%10;
    }
    while(rtos_waitForEvent(RTOS_EVT_DELAY_TIMER, /* all */ false, noTicksTillNextProduction));
    ASSERT(false);

} /* End of taskProducerH */





/**
 * The root of the task, which implements the regular low priority task that serves the
 * relay between the inbound and outbound queue. It has an infinite loop, always waiting
 * for the next clock tick at cycle time and then invoking the task function.
 *   @param initialResumeCondition
 * The vector of events which made the task due the very first time.
 */

static void taskRelayL(uint16_t initialResumeCondition)
{
    /* This task is started by an event after the connecting queue objects have been
       created. Double-check these conditions. */
    ASSERT((initialResumeCondition & EVT_QUEUE_H2L_READY) != 0  &&  _pQueueH2L != NULL);

    /* The relay is in charge of creating the queue object to the high priority consumer.
       After creation it awakes the communication peer, which will then assume the
       existance and operability of the same object. */
    const unsigned int sizeOfQueue = tsq_getSizeOfQueue
                                            ( MAX_LENGTH_QUEUE_L2H
                                            , /* maxElementSize */ sizeof(telegram_t)
                                            , /* alignOfElement */ 1
                                            );
    uint8_t memChunk[sizeOfQueue];
    _pQueueL2H = tsq_createQueue( memChunk
                                , MAX_LENGTH_QUEUE_L2H
                                , /* maxElementSize */ sizeof(telegram_t)
                                , /* alignOfElement */ 1
                                );

    /* Notify the communication peer that the connecting queue object is ready to use. */
    rtos_sendEvent(EVT_QUEUE_L2H_READY);

#define TI_CYCLE_IN_TICKS   (uintTime_t)(35.0e-3/RTOS_TICK + 0.5)

    ASSERT(__builtin_constant_p(TI_CYCLE_IN_TICKS)  &&  TI_CYCLE_IN_TICKS <= 127);

    do
    {
        /* Apply atomic read-modify-write operation to safely increment the task counter. */
        cli();
        ++ _cntRelayL;
        sei();

        /* Exceute the relay operation and count event if it fails due to full queue. */
        if(!relayL())
        {
            cli();
            ++ _noEventsQueueL2HFull;
            sei();
        }
    }
    while(rtos_suspendTaskTillTime(TI_CYCLE_IN_TICKS));
    ASSERT(false);

#undef TI_CYCLE_IN_TICKS
} /* End of taskRelayL */





/**
 * The root function of the consumer task of high priority. Irregular timing is
 * implemented using the random function.
 *   @param initialResumeCondition
 * The vector of events which made the task due the very first time.
 */

static void taskConsumerH(uint16_t initialResumeCondition)
{
    /* This task is started by an event after the connecting queue objects have been
       created. Double-check these conditions. */
    ASSERT((initialResumeCondition & EVT_QUEUE_L2H_READY) != 0
           &&  _pQueueL2H != NULL
          );

    uintTime_t noTicksTillNextProduction;
    do
    {
        /* Apply atomic read-modify-write operation to safely increment the task counter. */
        cli();
        ++ _cntConsumerH;
        sei();

        /* Read elements from the end of the chain of queues and validate the contents.
           Count errors in case. */
        consumerH();

        /* The time till next production is chosen randomly. */
        noTicksTillNextProduction = 1 + rand()%14;
    }
    while(rtos_waitForEvent(RTOS_EVT_DELAY_TIMER, /* all */ false, noTicksTillNextProduction));
    ASSERT(false);

} /* End of taskConsumerH */





/**
 * The root function of the producer task of medium priority. Fast, regular timing and
 * the shortest available round robin time slice is implemented for this task.
 *   @param initialResumeCondition
 * The vector of events which made the task due the very first time.
 */

static void taskProducerM(uint16_t initialResumeCondition)
{
    ASSERT(initialResumeCondition == RTOS_EVT_DELAY_TIMER);

    /* The producer is in charge of creating the queue object. After creation he awakes the
       communication peer, which will then assume the existance and operability of the same
       object. */
    const unsigned int sizeOfQueue = tsq_getSizeOfQueue
                                            ( MAX_LENGTH_QUEUE_M2M
                                            , /* maxElementSize */ sizeof(telegram_t)
                                            , /* alignOfElement */ 1
                                            );
    uint8_t memChunk[sizeOfQueue];
    _pQueueM2M = tsq_createQueue( memChunk
                                , MAX_LENGTH_QUEUE_M2M
                                , /* maxElementSize */ sizeof(telegram_t)
                                , /* alignOfElement */ 1
                                );

    /* Notify the communication peer that the connecting queue object is ready to use. */
    rtos_sendEvent(EVT_QUEUE_M2M_READY);

    do
    {
        /* Apply atomic read-modify-write operation to safely increment the task counter. */
        cli();
        ++ _cntProducerM;
        sei();

        /* Do data production if queue is not full or count the queue full event otherwise.
           The result is written into the queue, which is connected to the relay task at
           low priority. */
        if(producerM())
        {
            cli();
            ++ _noWrittenElementsQueueM2M;
            sei();
        }
        else
        {
            cli();
            ++ _noEventsQueueM2MFull;
            sei();
        }
        
        /* The cycle time is chosen very short. We can even use the shortest possible time
           of a single RTOS tick with the danger of a system overload (statement made by
           testing). Despite of using the shortest possible time interval can Round Robin
           scheduling lead to preemptions: The Round Robin time is set to one tick and the
           time slice will surely end at the next system clock tick - and we don't know
           how late in the running clock cycle this task became active (there are competing
           tasks of higher priority) and the execution time of the consumer will be longer
           (it handles several elements in one activation), maybe longer than one system
           timer tick. */
    }
    while(rtos_waitForEvent( RTOS_EVT_DELAY_TIMER
                           , /* all */ false
                           , /* noTicksTillNextProduction */ 1
                           )
         );
    ASSERT(false);

} /* End of taskProducerM */





/**
 * The root function of the consumer task of medium priority. Irregular timing is
 * implemented using the random function.
 *   @param initialResumeCondition
 * The vector of events which made the task due the very first time.
 */

static void taskConsumerM(uint16_t initialResumeCondition)
{
    /* This task is started by an event after the connecting queue objects have been
       created. Double-check these conditions. */
    ASSERT((initialResumeCondition & EVT_QUEUE_M2M_READY) != 0
           &&  _pQueueM2M != NULL
          );

    uintTime_t noTicksTillNextProduction;
    do
    {
        /* Apply atomic read-modify-write operation to safely increment the task counter. */
        cli();
        ++ _cntConsumerM;
        sei();

        /* Read elements from the queue and validate the contents. Count errors in case. */
        consumerM();

        /* The time till next production is chosen randomly. */
        noTicksTillNextProduction = 1 + rand()%15;
    }
    while(rtos_waitForEvent(RTOS_EVT_DELAY_TIMER, /* all */ false, noTicksTillNextProduction));
    ASSERT(false);

} /* End of taskConsumerM */





/**
 * The root of the task that implements the regular 200 ms task, which updates the display
 * with the test status information. It has an infinite loop, always waiting for the next
 * 200 ms clock tick and then invoking the task function.
 *   @param initialResumeCondition
 * The vector of events which made the task due the very first time.
 */

static void taskDisplayL(uint16_t initialResumeCondition)
{
#define TI_CYCLE_IN_TICKS   (uintTime_t)(200.0e-3/RTOS_TICK + 0.5)

    ASSERT(__builtin_constant_p(TI_CYCLE_IN_TICKS));
    ASSERT(initialResumeCondition == RTOS_EVT_DELAY_TIMER  &&  TI_CYCLE_IN_TICKS <= 127);

    do
    {
        /* Display the test statistics on the LCDisplay. */
        cli();
        uint32_t cycles = _cntConsumerH + _cntConsumerM;
        sei();
        dpy_printNoCycles(cycles);
        
        /* The 1 Byte values don't need mutual exclusion code, they can be read
           atomically. */
        dpy_printNoErrors(_noDataErrors);
        dpy_printCpuLoad(_cpuLoad);
    }
    while(rtos_suspendTaskTillTime(TI_CYCLE_IN_TICKS));
    ASSERT(false);

#undef TI_CYCLE_IN_TICKS
} /* End of taskDisplayL */





/**
 * The initalization of the RTOS tasks and general board initialization.
 */

void setup()
{
#if defined(DEBUG)  || ENABLE_PRINTF_FOR_IDLE_TASK
    /* Redirect stdout into Serial at 9600 bps. */
    init_stdout(9600);

    /* Print greeting to the console window. */
    puts_progmem(rtos_rtuinosStartupMsg);
#endif

    /* Print greeting on the LCD. */
    dpy_printGreeting();

    /* Initialize the digital pin as an output. The LED is used for most basic feedback about
       operability of code. */
    pinMode(LED, OUTPUT);

    /* Write the invariant parts of the display once. This needs to be done here, before
       multitasking begins. */
    dpy_printBackground();

    /* Configure all tasks. */
    ASSERT(noTasks == RTOS_NO_TASKS);

    /* Configure the tasks. By experience, setting up this kind of table is error prone and
       any error leads to dead software. To make this less prone to typical copy & paste
       errors we decouple the tasks by compound statements and use assertions. The
       assertions require that the initialization of the tasks is done in the order of the
       task index enumeration. */
    uint8_t idxTask = 0;
    {
        ASSERT(idxTask == idxTaskProducerH);
        ASSERT(idxTask < RTOS_NO_TASKS);
        static uint8_t stackTaskProducerH_[STACK_SIZE_TASK];
        rtos_initializeTask( idxTask
                           , /* taskFunction */     taskProducerH
                           , /* priority */         RTOS_NO_PRIO_CLASSES-1 /* Highest */
                           , /* timeRoundRobin */   0 /* 0=off */
                           , /* pStackArea */       &stackTaskProducerH_[0]
                           , /* stackSize */        sizeof(stackTaskProducerH_)
                           , /* startEventMask */   RTOS_EVT_DELAY_TIMER
                           , /* startByAllEvents */ false
                           , /* startTimeout */     0
                           );
        ++ idxTask;
    }
    {
        ASSERT(idxTask == idxTaskRelayL);
        ASSERT(idxTask < RTOS_NO_TASKS);
        static uint8_t stackTaskRelayL_[STACK_SIZE_TASK];
        rtos_initializeTask( idxTask
                           , /* taskFunction */     taskRelayL
                           , /* priority */         0 /* Lowest */
                           , /* timeRoundRobin */   0 /* 0=off */
                           , /* pStackArea */       &stackTaskRelayL_[0]
                           , /* stackSize */        sizeof(stackTaskRelayL_)
                           , /* startEventMask */   EVT_QUEUE_H2L_READY
                           , /* startByAllEvents */ false
                           , /* startTimeout */     0
                           );
        ++ idxTask;
    }
    {
        ASSERT(idxTask == idxTaskConsumerH);
        ASSERT(idxTask < RTOS_NO_TASKS);
        static uint8_t stackTaskConsumerH_[STACK_SIZE_TASK];
        rtos_initializeTask( idxTask
                           , /* taskFunction */     taskConsumerH
                           , /* priority */         RTOS_NO_PRIO_CLASSES-1 /* Highest */
                           , /* timeRoundRobin */   0 /* 0=off */
                           , /* pStackArea */       &stackTaskConsumerH_[0]
                           , /* stackSize */        sizeof(stackTaskConsumerH_)
                           , /* startEventMask */   EVT_QUEUE_L2H_READY
                           , /* startByAllEvents */ false
                           , /* startTimeout */     0
                           );
        ++ idxTask;
    }
    {
        ASSERT(idxTask == idxTaskProducerM);
        ASSERT(idxTask < RTOS_NO_TASKS);
        static uint8_t stackTaskProducerM_[STACK_SIZE_TASK];
        rtos_initializeTask( idxTask
                           , /* taskFunction */     taskProducerM
                           , /* priority */         1 /* Medium of 0..2 */
                           , /* timeRoundRobin */   1 /* 0=off */
                           , /* pStackArea */       &stackTaskProducerM_[0]
                           , /* stackSize */        sizeof(stackTaskProducerM_)
                           , /* startEventMask */   RTOS_EVT_DELAY_TIMER
                           , /* startByAllEvents */ false
                           , /* startTimeout */     3
                           );
        ++ idxTask;
    }
    {
        ASSERT(idxTask == idxTaskConsumerM);
        ASSERT(idxTask < RTOS_NO_TASKS);
        static uint8_t stackTaskConsumerM_[STACK_SIZE_TASK];
        rtos_initializeTask( idxTask
                           , /* taskFunction */     taskConsumerM
                           , /* priority */         1 /* Medium of 0..2 */
                           , /* timeRoundRobin */   1 /* 0=off */
                           , /* pStackArea */       &stackTaskConsumerM_[0]
                           , /* stackSize */        sizeof(stackTaskConsumerM_)
                           , /* startEventMask */   EVT_QUEUE_M2M_READY
                           , /* startByAllEvents */ false
                           , /* startTimeout */     0
                           );
        ++ idxTask;
    }
    {
        ASSERT(idxTask == idxTaskDisplay);
        ASSERT(idxTask < RTOS_NO_TASKS);
        static uint8_t stackTaskDisplay_[STACK_SIZE_TASK];
        rtos_initializeTask( idxTask
                           , /* taskFunction */     taskDisplayL
                           , /* priority */         0 /* Lowest */
                           , /* timeRoundRobin */   0 /* 0=off */
                           , /* pStackArea */       &stackTaskDisplay_[0]
                           , /* stackSize */        sizeof(stackTaskDisplay_)
                           , /* startEventMask */   RTOS_EVT_DELAY_TIMER
                           , /* startByAllEvents */ false
                           , /* startTimeout */     10
                           );
        ++ idxTask;
    }

    /* All tasks 0..RTOS_NO_TASKS need to be initialized. */
    ASSERT(idxTask == RTOS_NO_TASKS);

} /* End of setup */




/**
 * The application owned part of the idle task. This routine is repeatedly called whenever
 * there's some execution time left. It's interrupted by any other task when it becomes
 * due.
 *   @remark
 * Different to all other tasks, the idle task routine may and should return. (The task as
 * such doesn't terminate). This has been designed in accordance with the meaning of the
 * original Arduino loop function.
 */

void loop()
{
    /* Give an alive sign. */
    blink(3);

#if ENABLE_PRINTF_FOR_IDLE_TASK
    printf("\nRTuinOS is idle\n");
#endif

    /* Compute the CPU load and write the result into a global variable. No
       access synchronization is needed here since writing and reading a uint8 is atomic.
         We do this repeately to get less data traffic (and more time to read) on the COM
       connection with the host computer. */
    _cpuLoad = gsl_getSystemLoad();
    _cpuLoad = gsl_getSystemLoad();
    _cpuLoad = gsl_getSystemLoad();

#if ENABLE_PRINTF_FOR_IDLE_TASK
    printf("CPU load: %u %%\n", (_cpuLoad+1)/2);

    cli();
    unsigned long cntProducerH = _cntProducerH
                , cntConsumerH = _cntConsumerH
                , cntProducerM = _cntProducerM
                , cntConsumerM = _cntConsumerM
                , cntRelayL   = _cntRelayL;
    unsigned long noWrittenElementsQueueH2L = _noWrittenElementsQueueH2L
                , noEventsQueueH2LFull      = _noEventsQueueH2LFull
                , noReadElementsQueueL2H    = _noReadElementsQueueL2H
                , noEventsQueueL2HFull      = _noEventsQueueL2HFull
                , noWrittenElementsQueueM2M = _noWrittenElementsQueueM2M
                , noEventsQueueM2MFull      = _noEventsQueueM2MFull
                , noReadElementsQueueM2M    = _noReadElementsQueueM2M;
    sei();
    printf("Task counts:\n"
           "  Producer High: %lu\n"
           "  Relay:         %lu\n"
           "  Consumer High: %lu\n"
           "  Producer Med:  %lu\n"
           "  Consumer Med:  %lu\n"
          , cntProducerH
          , cntRelayL
          , cntConsumerH
          , cntProducerM
          , cntConsumerM
          );
    printf( "Events:\n"
            "  Data errors:    %i\n"
            "  Written H2L:    %lu\n"
            "  Queue full H2L: %lu\n"
            "  Read L2H:       %lu\n"
            "  Queue full L2H: %lu\n"
            "  Written M2M:    %lu\n"
            "  Queue full M2M: %lu\n"
            "  Read M2M:       %lu\n"
          , (int)_noDataErrors
          , noWrittenElementsQueueH2L
          , noEventsQueueH2LFull
          , noReadElementsQueueL2H
          , noEventsQueueL2HFull
          , noWrittenElementsQueueM2M
          , noEventsQueueM2MFull
          , noReadElementsQueueM2M
          );

    uint8_t u;
    for(u=0; u<RTOS_NO_TASKS; ++u)
    {
        printf( "Task %u: Unused stack area %u Byte, overrun counter: %u\n"
              , u
              , rtos_getStackReserve(u)
              , rtos_getTaskOverrunCounter(/* idxTask */ u, /* doReset */ false)
              );
    }
//    cli();
//    sei();

#endif // ENABLE_PRINTF_FOR_IDLE_TASK

} /* End of loop */





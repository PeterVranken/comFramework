/**
 * @file tst_testCases.c
 * Test case implementation for thread safe queue with variable element size.
 *
 * Copyright (C) 2016-2021 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
 *   tst_initModule
 *   tst_shutdownModule
 *   tst_taskProducer
 *   tst_taskConsumer
 * Local functions
 */

/*
 * Include files
 */

#include <stdint.h>
#include <stdbool.h>
#include <stdatomic.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include "rtos_rtosEmulation.h"
#include "crc_checksum.h"
#include "vsq_threadSafeQueueVariableSize.h"
#include "tst_testCases.h"


/*
 * Defines
 */

/** The number of queue objects run in parallel. */
#define NO_QUEUE_OBJS 11

/** The maximum capacity of the queue objects used for the communication from the fast to
    the slow task. */
#define MAX_QUEUE_LENGTH_FAST_TO_SLOW 25

/** The maximum capacity of the queue objects used for the communication from the slow to
    the fast task. */
#define MAX_QUEUE_LENGTH_SLOW_TO_FAST 50

/** The maximum number of random payload bytes of a telegram used in the test. */
#define MAX_NO_PAYLOAD_BYTES          23u

/** The maximum capacity of the queue objects depending on the index of the queue. */
#define MAX_QUEUE_LENGTH(idxQ) (isFastToSlow(idxQ)? (MAX_QUEUE_LENGTH_FAST_TO_SLOW)     \
                                                  : (MAX_QUEUE_LENGTH_SLOW_TO_FAST)     \
                               )

/** The maximum size, a telegram can have. */
#define MAX_SIZEOF_TELEGRAM_T   (sizeof(telegram_t)+(MAX_NO_PAYLOAD_BYTES))

/** A signed integer in the range [i, j]. */
#define IRAND(i,j) (rand()%((j)-(i)+1)+(i))

/** Floating point random number with more than 15 Bit resolution; taken fron
    http://www.azillionmonkeys.com/qed/random.html on Jan 23, 2017. */
#define DRAND() ({                                                                  \
    double d;                                                                       \
    do {                                                                            \
       d = (((rand() * RS_SCALE) + rand()) * RS_SCALE + rand()) * RS_SCALE;         \
    } while(d >= 1); /* Round off */                                                \
    d;                                                                              \
})

/** Helper for #DRAND. */
#define RS_SCALE (1.0 / (1.0 + RAND_MAX))

/** A scaled floating point random number in the range [a, b). */
#define RAND(a,b) ((double)((b)-(a))*DRAND()+(double)(a))

/** A Boolean random number with given probability p of getting a true. */
#define BOOL_RAND(p) ((DRAND()<(p))? true: false)


/** The alignment required for the queue content object. */
#ifdef _STDC_VERSION_C17_C11
# define ALIGN_OF_TELEGRAM_T _Alignof(telegram_t)
#elif defined(__GNUC__) && defined(__WIN64__)
# define ALIGN_OF_TELEGRAM_T 8
#elif defined(__GNUC__) && defined(__WIN32__)
# define ALIGN_OF_TELEGRAM_T 4
#else
# error Define the alignment of struct telegram_t for your compiler/target
#endif


#ifdef _STDC_VERSION_C17_C11
/* This code is valid regardless of having GCC - but how to recognize the C language
   revision independent of the compiler? */
# define MEMORY_BARRIER_FULL() {atomic_thread_fence(memory_order_seq_cst);}
#elif __GNUC__
# define MEMORY_BARRIER_FULL() {__sync_synchronize();}
#else
# error Macro MEMORY_BARRIER_FULL() needs to be defined for your target
#endif


#ifdef __GNUC__
/** Intentionally unused objects in the C code can be marked using this define. A compiler
    warning is avoided. */
#define ATTRIB_UNUSED __attribute__((unused))

#ifdef NDEBUG
/** Objects in the C code intentionally unused in the production compilation can be marked
    using this define. A compiler warning is avoided. */
# define ATTRIB_DBG_ONLY ATTRIB_UNUSED
#else
/** Objects in the C code intentionally unused in the production compilation can be marked
    using this define. A compiler warning is avoided. */
# define ATTRIB_DBG_ONLY
#endif

#else
# define ATTRIB_UNUSED
# define ATTRIB_DBG_ONLY
#endif


/*
 * Local type definitions
 */

/** This is the data structure, which is passed from producer to consumer. */
typedef struct telegram_t
{
    /** The checksum of the complete telegram. Used to detect unwanted data changed
        during telegram delivery. */
    uint8_t checksum;

    /** The checkum protected data contents of the telegram. */
    struct payload_t
    {
        /** An infinite counter. Incremented with each sent telegram and used to detect
            sequence errors in the test. */
        unsigned long sqc;

        /** The number of bytes actualls stored in rndAry. */
        unsigned int sizeOfRndAry;

        /** Some random data as variable size array. */
        uint8_t rndAry[0];

    } payload;

} telegram_t;


/*
 * Local prototypes
 */


/*
 * Data definitions
 */

/** The queue objects under test by reference. */
static vsq_queue_t *_pQueueAry[NO_QUEUE_OBJS] = {[0] = NULL};

/** The data required to produce a defined, checkable sequence of telegrams. */
static unsigned long _sqcProdAry[NO_QUEUE_OBJS] = {[0] = 0};

/** The data required to validation of a defined, checkable sequence of telegrams. */
static unsigned long _sqcConsAry[NO_QUEUE_OBJS] = {[0] = 0};

/** Counter to indicate how many repetitions ahve been made. */
_Atomic volatile unsigned long tst_noWrittenElements = 0
                             , tst_noEventsQueueFull = 0
                             , tst_noReadElements = 0;

/*
 * Function implementation
 */

/**
 * The queue objects are partly used for the communication from task A to task B and partly
 * in the opposite direction. This method tells the direction for a given queue.
 *   @param idxQueue
 * The index of the queue in the global array of such \a _pQueueAry.
 */
static inline bool isFastToSlow(unsigned int idxQueue)
{
    return (idxQueue & 1) == 0;

} /* End of isFastToSlow */



/**
 * The queue objects are partly used for the communication from task A to task B and partly
 * in the opposite direction. This method tell a given task the communication direction for
 * a given queue.
 *   @param isFastTask
 * The two tasks are identified/distinguished by this parameter.
 *   @param idxQueue
 * The index of the queue in the global array of such \a _pQueueAry.
 */
static inline bool isOutbound(bool isFastTask, unsigned int idxQueue)
{
    return isFastTask? isFastToSlow(idxQueue)
                     : !isFastToSlow(idxQueue);

} /* End of isOutbound */



/**
 * The initialization of a single queue.
 *   @param idxQueue
 * The index of the queue in the global array of such \a _pQueueAry.
 */
static void initQueue(const unsigned int idxQueue)
{
    /* Create a queue object for this test.
         +1: The queue should properly operate with structs using the flexible array member.
       Such struct objects can easily have a size, which is not a multiple of the required
       alignment. We simulate this for testing purpose by the +1, while sizeof(telegram_t)
       needs to be a multiple of the alignment. */
    assert(sizeof(telegram_t) % ALIGN_OF_TELEGRAM_T == 0
           &&  (sizeof(telegram_t)+1) % ALIGN_OF_TELEGRAM_T != 0
          );
    const unsigned int sizeOfQueue = vsq_getSizeOfQueue
                                            ( MAX_QUEUE_LENGTH(idxQueue)
                                            , /* maxElementSize */ MAX_SIZEOF_TELEGRAM_T
                                            );
    assert(sizeOfQueue > 0);
    printf("Size of queue %u: %u Byte\n", idxQueue, sizeOfQueue);

    /* Are there caching effects? Don't have the objects to close to one another in the
       address space. Shape a gap on the heap by allocating more memory than required.
       Don't do this for all the objects in order to see possible out-of-boundary errors by
       harmful interferences of still close subsequent objects. */
    unsigned int sizeOfChunk = sizeOfQueue;
    if((idxQueue % 3) == 1)
        sizeOfChunk += IRAND(0,8191) + 0x33219;
    char * const pMemChunk = malloc(sizeOfChunk);
    assert(pMemChunk != NULL);
    _pQueueAry[idxQueue] = vsq_createQueue( pMemChunk
                                          , /* maxNoStdElements */ MAX_QUEUE_LENGTH(idxQueue)
                                          , /* sizeOfStdElement */ MAX_SIZEOF_TELEGRAM_T
                                          );
    assert((void*)_pQueueAry[idxQueue] == (void*)pMemChunk);

    unsigned int sizeOfPayload ATTRIB_DBG_ONLY;
    const telegram_t *pT_received = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfPayload);
    assert(pT_received == NULL  &&  sizeOfPayload == 0);

    /* First test can be done without multithreading. */
    telegram_t t = {.checksum = 87, .payload={.sqc = 2764324, .sizeOfRndAry = 0}};
    bool success = vsq_writeToTail(_pQueueAry[idxQueue], /* pData */ &t, sizeof(telegram_t));
    assert(success);
    ++ t.payload.sqc;
    -- t.checksum;

    /* A valid queue element can have size zero. */
    success = vsq_writeToTail(_pQueueAry[idxQueue], /* pData */ NULL, 0);
    assert(success);

#if 0
    success = vsq_writeToTail(_pQueueAry[idxQueue], /* pData */ &t, sizeof(telegram_t));
    assert(success);
#else
    telegram_t *pAllocatedElement = (telegram_t*)
                                    vsq_allocTailElement( _pQueueAry[idxQueue]
                                                        , sizeof(telegram_t)
                                                        );
    assert(pAllocatedElement != NULL);
    pAllocatedElement->payload.sqc = t.payload.sqc;
    pAllocatedElement->checksum = t.checksum;
    vsq_postTailElement(_pQueueAry[idxQueue]);
#endif
    ++ t.payload.sqc;
    -- t.checksum;

    pT_received = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfPayload);
    assert(sizeOfPayload == sizeof(telegram_t));
    assert(pT_received != NULL);
    assert(pT_received->payload.sqc == 2764324  &&  pT_received->checksum == 87);
    success = vsq_writeToTail(_pQueueAry[idxQueue], /* pData */ &t, sizeof(telegram_t));
    assert(success);
    pT_received = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfPayload);
    assert(pT_received != NULL  &&  sizeOfPayload == 0);
    pT_received = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfPayload);
    assert(pT_received != NULL  &&  sizeOfPayload == sizeof(telegram_t));
    assert(pT_received->payload.sqc == 2764325  &&  pT_received->checksum == 86);
    pT_received = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfPayload);
    assert(pT_received != NULL  &&  sizeOfPayload == sizeof(telegram_t));
    assert(pT_received->payload.sqc == 2764326  &&  pT_received->checksum == 85);
    pT_received = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfPayload);
    assert(pT_received == NULL  &&  sizeOfPayload == 0);
#ifdef VSQ_ENABLE_API_QUEUE_DIAGNOSTICS
    assert(vsq_getMaximumQueueUsage(_pQueueAry[idxQueue]) == 3);
#endif

    unsigned int u;
    union
    {
        telegram_t t;
        uint8_t ensureSizeAry[MAX_SIZEOF_TELEGRAM_T];
    } tv = {.t = {.checksum = 87, .payload={.sqc = 2764324, .sizeOfRndAry = 0}}};

    /* Push telegrams until queue full indication. */
    unsigned int noMsgsInQueue = 0;
    do
    {
        /* Fill the random payload area. */
        tv.t.payload.sizeOfRndAry = noMsgsInQueue % (MAX_NO_PAYLOAD_BYTES+1);
        unsigned v;
        for(v=0; v<tv.t.payload.sizeOfRndAry; ++v)
            tv.t.payload.rndAry[v] = 3*v;
        success = vsq_writeToTail( _pQueueAry[idxQueue]
                                 , /* pData */ &tv
                                 , sizeof(telegram_t) + tv.t.payload.sizeOfRndAry
                                 );
        if(success)
        {
            ++ noMsgsInQueue;
            ++ tv.t.payload.sqc;
            -- tv.t.checksum;
        }
    }
    while(success);
    printf("initQueue: %u telegrams fit into queue[%u]\n", noMsgsInQueue, idxQueue);

    success = vsq_writeToTail(_pQueueAry[idxQueue], /* pData */ &tv, MAX_SIZEOF_TELEGRAM_T);
    assert(!success);

    for(u=0; u<noMsgsInQueue; ++u)
    {
        pT_received = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfPayload);
        assert(pT_received != NULL);
        assert(sizeOfPayload == sizeof(telegram_t) + u % (MAX_NO_PAYLOAD_BYTES+1));
        assert(sizeOfPayload == sizeof(telegram_t) + pT_received->payload.sizeOfRndAry);
        assert(pT_received->payload.sqc == 2764324+u  &&  pT_received->checksum == 87-u);

#ifdef VSQ_ENABLE_API_QUEUE_DIAGNOSTICS
        assert(vsq_getMaximumQueueUsage(_pQueueAry[idxQueue]) == noMsgsInQueue);
#endif
    }
    pT_received = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfPayload);
    assert(pT_received == NULL  &&  sizeOfPayload == 0);

#ifdef VSQ_ENABLE_API_QUEUE_DIAGNOSTICS
    assert(vsq_getMaximumQueueUsage(_pQueueAry[idxQueue]) == noMsgsInQueue);
#endif
} /* End of initQueue */



/**
 * The initialization of the test module. This function needs to be called prior to begin
 * of multitasking; there must be no race conditions.
 */
void tst_initModule()
{
    unsigned int u;
    for(u=0; u<NO_QUEUE_OBJS; ++u)
    {
        /* Create the wanted queue object. */
        initQueue(u);
        printf("_pQueueAry[%u]: 0x%p\n", u, _pQueueAry[u]);

        /* Initialize the sequence counters to a negative value in order to have the
           overrun in the simulation time. */
        _sqcProdAry[u] = (unsigned long)-1000;
        _sqcConsAry[u] = (unsigned long)-1000;

    } /* for(All queues under test) */

} /* End of tst_initModule */




/**
 * Shutdown of the test module. This function needs to be called after multitasking has
 * ended; there must be no race conditions.
 */
void tst_shutdownModule()
{
    unsigned int idxQueue;
    for(idxQueue=0; idxQueue<NO_QUEUE_OBJS; ++idxQueue)
    {
        free(_pQueueAry[idxQueue]);

    } /* for(All queues under test) */

} /* End of tst_shutdownModule */




/**
 * Put a radnom element in one of the available queues.
 *   @param idxQueue
 * The index of the queue in the global array of such \a _pQueueAry.
 */
static void produce(unsigned int idxQueue)
{
    bool successOfPost;
    static unsigned int noAPI1=0, noAPI2=0;

    /* Randomly choose the API to write to the queue. */
    if(BOOL_RAND(0.5))
    {
        /* API: Prepare object in a local buffer and write it at once to the tail of the
           queue. */
        struct
        {
            telegram_t t;
            uint8_t varDataArea[MAX_NO_PAYLOAD_BYTES];

        } tv = {.t = {.checksum = 87, .payload={.sqc = 2764324, .sizeOfRndAry = 0}}};

        /* Set the sequence counter to the next value to use. */
        tv.t.payload.sqc = _sqcProdAry[idxQueue];
#if 0
        /* Self test of test application: This should lead to a reported E2E validation
           error after a while. */
        if(BOOL_RAND(1e-5))
        {
            printf("%s, %u: Inject error\n", __func__, __LINE__);
            fflush(stdout);
            ++ tv.t.payload.sqc;
        }
#endif

        /* Fill the random part of the telegram. */
        tv.t.payload.sizeOfRndAry = IRAND(0,MAX_NO_PAYLOAD_BYTES);
        assert(sizeof(telegram_t) + tv.t.payload.sizeOfRndAry <= sizeof(tv));
        unsigned int u;
        for(u=0; u<tv.t.payload.sizeOfRndAry; ++u)
            tv.t.payload.rndAry[u] = IRAND(0, 255);

        tv.t.checksum = crc_checksumSAEJ1850_8Bit
                                    ( &tv.t.payload
                                    , sizeof(tv.t.payload) + tv.t.payload.sizeOfRndAry
                                    );
#if 0
        /* Self test of test application: This should lead to a reported E2E validation error
           after a while. */
        if(tv.t.payload.sizeOfRndAry > 0  && BOOL_RAND(1e-5))
        {
            printf("%s, %u: Inject error\n", __func__, __LINE__);
            fflush(stdout);
            ++ tv.t.payload.rndAry[0];
        }
#endif
        successOfPost = vsq_writeToTail( _pQueueAry[idxQueue]
                                       , /* pData */ &tv
                                       , sizeof(telegram_t) + tv.t.payload.sizeOfRndAry
                                       );
        ++ noAPI1;
    }
    else
    {
        /* API: Allocate room in the queue and produce the data in place. Save copying the
           local buffer. */
        const unsigned int sizeOfRndAry = IRAND(0,MAX_NO_PAYLOAD_BYTES);
        telegram_t * const pT = (telegram_t*)
                                vsq_allocTailElement( _pQueueAry[idxQueue]
                                                    , sizeof(telegram_t) + sizeOfRndAry
                                                    );
        if(pT != NULL)
        {
            assert(((uintptr_t)pT & (ALIGN_OF_TELEGRAM_T-1)) == 0);
            successOfPost = true;
            pT->payload.sqc = _sqcProdAry[idxQueue];
#if 0
            /* Self test of test application: This should lead to a reported E2E validation
               error after a while. */
            if(BOOL_RAND(1e-5))
            {
                printf("%s, %u: Inject error\n", __func__, __LINE__);
                fflush(stdout);
                ++ pT->payload.sqc;
            }
#endif

            /* Fill the random part of the telegram. */
            unsigned int u;
            for(u=0; u<sizeOfRndAry; ++u)
                pT->payload.rndAry[u] = IRAND(0, 255);
            pT->payload.sizeOfRndAry = sizeOfRndAry;
            pT->checksum = crc_checksumSAEJ1850_8Bit( &pT->payload
                                                    , sizeof(pT->payload) + sizeOfRndAry
                                                    );
#if 0
            /* Self test of test application: This should lead to a reported E2E validation
               error after a while. */
            if(sizeOfRndAry > 0  && BOOL_RAND(1e-5))
            {
                printf("%s, %u: Inject error\n", __func__, __LINE__);
                fflush(stdout);
                ++ pT->payload.rndAry[0];
            }
#endif
            /* Submit the element without copying the data any more. */
            vsq_postTailElement(_pQueueAry[idxQueue]);

            ++ noAPI2;
        }
        else
            successOfPost = false;
    }
    //assert(noAPI1 < 10000  ||  noAPI2 < 10000);

    if(successOfPost)
    {
        /* Count sent event. */
        atomic_fetch_add(&tst_noWrittenElements, 1);

        /* Update sequence counter only if this telegram could be posted. */
        ++ _sqcProdAry[idxQueue];
    }
    else
    {
        /* Count queue full event. */
        atomic_fetch_add(&tst_noEventsQueueFull, 1);
    }
} /* End of produce */





/**
 * Read all meanwhile appended elements from a queue and double-check newly received elements.
 *   @param idxQueue
 * The index of the queue in the global array \a _pQueueAry of such.
 */
static void consume(unsigned int idxQueue)
{
    const telegram_t *pT;
    while(true)
    {
        unsigned int sizeOfQueueElement;
        pT = vsq_readFromHead(_pQueueAry[idxQueue], &sizeOfQueueElement);
        assert(pT == NULL  &&  sizeOfQueueElement == 0
               ||  pT != NULL
                   &&  sizeOfQueueElement == sizeof(telegram_t) + pT->payload.sizeOfRndAry
              );
        if(pT == NULL)
            break;

        /* Code as critical as possible for this test: read checksum, which is written last
           by the producer, first. */
        const uint8_t checksumRef = pT->checksum;

        /* Count reception event. */
        atomic_fetch_add(&tst_noReadElements, 1);

        assert(_sqcConsAry[idxQueue] == pT->payload.sqc);
        ++ _sqcConsAry[idxQueue];

        const uint8_t checksum = crc_checksumSAEJ1850_8Bit( &pT->payload
                                                          , sizeOfQueueElement
                                                            - sizeof(telegram_t)
                                                            + sizeof(pT->payload)
                                                          );
        assert(checksum == checksumRef);
    }
} /* End of consume */





/**
 * The common code of producer and consumer task, which behave widely symmetric.
 *   @param isFastTask
 * The two tasks are identified/distinguished by this parameter.
 */
static void communicate(bool isFastTask)
{
    unsigned int idxQ;
    for(idxQ=0; idxQ<NO_QUEUE_OBJS; idxQ+=2)
    {
        if(isOutbound(isFastTask, idxQ))
        {
            signed intNoTelegramsMin, intNoTelegramsMax;
            if(isFastTask)
            {
                intNoTelegramsMin = 0;
                intNoTelegramsMax = 6;
            }
            else
            {
                intNoTelegramsMin = 15;
                intNoTelegramsMax = 100;
            }

            signed int i;
            for(i=IRAND(intNoTelegramsMin,intNoTelegramsMax); i>0; --i)
                produce(idxQ);
        }
        else
            consume(idxQ);
    }
} /* End of communicate */




/**
 * The task, which is cycling at 1ms.
 */
void tst_task1ms()
{
    communicate(/* isFastTask */ true);

} /* End of tst_taskProducer */




/**
 * The task, which is cycling at 10ms.
 */
void tst_task10ms()
{
    communicate(/* isFastTask */ false);

} /* End of tst_taskConsumer */

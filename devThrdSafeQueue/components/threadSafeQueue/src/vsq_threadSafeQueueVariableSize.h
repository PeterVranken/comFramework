#ifndef VSQ_THREADSAFEQUEUEVARIABLESIZE_INCLUDED
#define VSQ_THREADSAFEQUEUEVARIABLESIZE_INCLUDED
/**
 * @file vsq_threadSafeQueueVariableSize.h
 * Definition of global interface of module vsq_threadSafeQueueVariableSize.c
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

/*
 * Include files
 */

#include <stdbool.h>
#include <assert.h>


/*
 * Defines
 */

/** A diagnostic API reporting the usage of the queue can be useful for supporting the
    development of an application (dimensioning the queue sizes) but won't normally be
    required by the ready application. Therefore the compilation of this API can be
    configured using this switch. */
#ifndef VSQ_ENABLE_API_QUEUE_DIAGNOSTICS
# define VSQ_ENABLE_API_QUEUE_DIAGNOSTICS   1
#endif


/** Many error conditions, which are static in the sense that they can only appear due to
    errors in the implementation code are checked by assertions. This relates to the
    implementation of the queue itself, but to the implementation of the client code, too.
    The most typical errors will be caught the first time the code is executed. This
    concept of static error checks makes it inevitable to have an assertion mechanism. Most
    platforms will offer an assertion. #VSQ_ASSERT(boolean_t) needs to expand to the
    assertion on your platform. */
#define VSQ_ASSERT(booleanInvariant)        assert(booleanInvariant)


/* The software is written as portable as possible. This requires the awareness of the C
   language standard it is compiled with - particularly the new stuff from the C11 standard
   strongly supports portability. (Effectively, not using C11 means that GCC will be
   required for a lower degree of portability.)
     If your environment supports C11 then you should use it and make this #define on the
   compiler's command line.
     With respect to the language feature C11 and C17 are identical. We combine the in one
   switch. */
#if defined(__STDC_VERSION__)
# if (__STDC_VERSION__)/100 == 2017
#  define _STDC_VERSION_C17
#  define _STDC_VERSION_C17_C11
# elif (__STDC_VERSION__)/100 == 2011
#  define _STDC_VERSION_C11
#  define _STDC_VERSION_C17_C11
# elif (__STDC_VERSION__)/100 == 1999
#  define _STDC_VERSION_C99
# endif
#endif


/*
 * Global type definitions
 */

/** The queue object as an unknown struct. */
struct vsq_queue_t;

/** The queue object as an unknown type. The API operates with pointers to such objects. */
typedef struct vsq_queue_t vsq_queue_t;


/*
 * Global data declarations
 */


/*
 * Global prototypes
 */

/** Prior to queue creation: Query the size of a queue object. */
unsigned int vsq_getSizeOfQueue(unsigned int maxQueueLength, unsigned int maxElementSize);

/** Create a new queue object. */
struct vsq_queue_t *vsq_createQueue( void *pMemoryChunk
                                   , unsigned int maxQueueLength
                                   , unsigned int maxElementSize
                                   );

/** Append a new element to the tail of the queue. */
bool vsq_writeToTail(vsq_queue_t *pQueue, const void *pData, unsigned int noBytes);

/** Check if the queue has currently room to append another element to the tail and return
    the available element in case. */
void *vsq_allocTailElement(vsq_queue_t * const pQueue, unsigned int sizeOfPayload);

/** Submit a queue element, which had been allocated with \a vsq_allocTailElement. */
void vsq_postTailElement(vsq_queue_t *pQueue);

/** Read a meanwhile receivced new element from the head of the queue. */
const void *vsq_readFromHead(vsq_queue_t *pQueue, unsigned int *pSizeOfPayload);

#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
/** Get the maximum number of queued elements, which has ever been seen. */
unsigned int vsq_getMaximumQueueUsage(const vsq_queue_t *pQueue);
#endif

#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
/** Get the maximum number of queued elements, which has ever been seen. */
unsigned int vsq_getMaximumQueueUsageInByte(const vsq_queue_t *pQueue);
#endif

#endif  /* VSQ_THREADSAFEQUEUEVARIABLESIZE_INCLUDED */

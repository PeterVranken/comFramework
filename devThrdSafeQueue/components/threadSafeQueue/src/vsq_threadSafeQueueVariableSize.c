/**
 * @file vsq_threadSafeQueueVariableSize.c
 * Thread-safe implementation of a queue. Filling and reading the queue may be done from
 * different threads. It doesn't matter whether these threads have same or different
 * priorities and whether they are running on the same or on different cores.\n
 *   The implementation is lock-free. Mutual exclusion of threads is based on spatial
 * separation rather then on serialization (ordering in time). No two threads will ever
 * read or write to the same address space. The implementation is based on shared memory,
 * which can be read and written by both affected threads. In practice, this will almost
 * always mean some uncached memory area.
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
 *   vsq_getSizeOfQueue
 *   vsq_createQueue
 *   vsq_getMaxSizeOfElement
 *   vsq_writeToTail
 *   vsq_allocTailElement
 *   vsq_postTailElement
 *   vsq_readFromHead
 *   vsq_getMaximumQueueUsage
 *   vsq_getMaximumQueueUsageInByte
 * Local functions
 *   alignedSizeOfObject
 *   isaligned
 *   calculateSizeOfRingBuffer
 *   getElementAt
 *   getLinkPtrOfElementAt
 *   setLinkPtrOfElementAt
 *   setHdrOfElementAt
 *   allocTailElement
 *   postTailElement
 *   byteOffsetOfRingBuffer
 */

/*
 * Include files
 */

#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <limits.h>
#if defined(__STDC_VERSION__) &&  (__STDC_VERSION__)/100 >= 2011
# include <stdatomic.h>
#endif

#include "vsq_threadSafeQueueVariableSize.h"


/*
 * Defines
 */


/** The thread-safe implementation of the dispatcher queue builds on defined memory
    ordering; machine operations are expected to happen in the order of C code statements.
    This is not easy to achieve in C as the language semantics doesn't have awareness of
    concurrency (before C11). The solution will always be target and compiler dependent.
    Here, we offer a macro, which is meant to implement a full memory barrier on the
    target.\n
      Fortunately, when using GCC there is a platform independent solution. See
    http://gcc.gnu.org/onlinedocs/gcc-4.4.3/gcc/Atomic-Builtins.html for details. */
#ifdef _STDC_VERSION_C17_C11
# define MEMORY_BARRIER_FULL() {atomic_thread_fence(memory_order_seq_cst);}
#elif __GNUC__
# define MEMORY_BARRIER_FULL() {__sync_synchronize();}
#else
# error Macro MEMORY_BARRIER_FULL() needs to be defined for your target
#endif


/** The alignment of type queueElement_t. The queue element can make use of a configurable
    integer size for storage of indexes. We need to know the aignment for this type and
    compilers not supporting C11 can't figure it out. You need to configure the alignment
    in accordance with the chosen index type, see uintidx_t.\n
      TriCore: The alignment of an unsigned int is not identical to its size. The alignment
    of the reduced to 2 Byte even if unsigned int is configured as index type. */
#if defined(_STDC_VERSION_C17_C11)
# define ALIGN_OF_HDR      (_Alignof(queueElement_t))
#elif defined(__AVR__)
# define ALIGN_OF_HDR      (1u)
#else
# define ALIGN_OF_HDR      (sizeof(((const queueElement_t*)NULL)->idxNext))
#endif


/** The alignment of the payload of the queued elements in Byte. The payload of an element
    read from the queue is returned by reference. The got address will have at least this
    alignment.\n
      The value will normally be the largest natural alignment for the given architecture,
    i.e. 4 Byte for 32 Bit systems and 8 Byte on 64 Bit systems. However, special
    architectures like TriCore can deviate. A 32 Bit TriCore could use 2 Byte if the payload
    doesn't contain double values.\n
      The specified value must not be less than #ALIGN_OF_HDR. This is checked by
    assertion. */
#if defined(_STDC_VERSION_C17_C11)
# define ALIGN_OF_PAYLOAD   (_Alignof(void*))
#elif defined(__AVR__)
# define ALIGN_OF_PAYLOAD   (1u)
#else
# define ALIGN_OF_PAYLOAD   (sizeof(void*))
#endif


/** The alignment of the chosen atomic integer type. Mostly, it'll be
    sizeof(uintatomic_t), but because of some specialities on particular platforms,
    we make this explicit.
      TriCore: The alignment of an unsigned int is not identical to the alignment, we
    require, which is the alignment that ensures atomic read and write operations. Taking
    the maximum of alignment and size should be alright in nearly all environments. */
#if defined(_STDC_VERSION_C17_C11)
# define ALIGN_OF_UINTATOMIC (MAX(_Alignof(uintatomic_t), sizeof(uintatomic_t)))
#else
# define ALIGN_OF_UINTATOMIC (sizeof(uintatomic_t))
#endif


/** The maximum of two numbers as a preprocessor expression. */
#define MAX(a,b) (((a) > (b)) ? (a) : (b))

/** The minimum of two numbers as a preprocessor expression. */
#define MIN(a,b) (((a) < (b)) ? (a) : (b))

/* We are about to declare the unsigned int type as an integral type with atomic access.
   For GCC and more recent language versions it is possible to check at compile time if
   this is really granted. All other compilers or C revisions will require
   double-checking. */
#if defined(_STDC_VERSION_C17_C11)
_Static_assert( ATOMIC_INT_LOCK_FREE == 2
              , "Require integral type with atomic read or write access"
              );
#elif defined(__GNUC__) && defined(_STDC_VERSION_C99)
# ifndef __GCC_ATOMIC_INT_LOCK_FREE
#  error Require integral type with atomic read or write access
# endif
#elif defined(__AVR__)
    /* AVR is supported by an explicitly different typedef of the atomic integer type, see
       below. */
#else
# error Check atomicity of integral data type for your compiler/target CPU
#endif



/*
 * Local type definitions
 */

/** The thread-safe implementation of the queue builds on atomic read and write
    of a numeric type. Basically any atomic unsigned integer available on the target
    platform can be typedef'ed here; however, the maximum length of the queue is given by
    the range of this type - thus don't use a too short integer type if you have the
    choice. unsigned int will be appropriate on most 32 and 16 Bit platforms and unsigned
    char on a typical 8 Bit platform.\n
      Negligible problems arise on platforms like AVR, where the atomic integer type is
    shorter than unsigned int. Due to the typedef made here, two integer types are mixed in
    the implementation without making this transparent by explicit type casts. The implicit
    type casts still ensure code correctness with one documented exception: the optional
    diagnostic API functions are now restricted to be used solely in the producer context.
    However, these functions are not essential and will be switched off in most
    integrations. We decided to not clean this up by using typedef'ed integers throughout
    the complete implementation -- this would degrade the readability and maintainability
    of the code and such systems are anyway not the targeted platforms. */
#if defined(__AVR__)
typedef uint8_t uintatomic_t;
#else
typedef unsigned int uintatomic_t;
#endif

/** This type is used to store index and size information for queue elements. 16, 32 and
    even 64 Bit types can be specified. (Where 64 Bit should have no practical relevance.)
    The appropriate choice is not a really a matter of having a 16 or 32 Bit architecture.
    Even on 32 Bit embedded platforms, the RAM consumption is an important aspect and the
    choice can save a significant amount of RAM: The size of header information for each
    queued element is 4 Byte if a 16 Bit type is configured and 8 Byte for a 32 Bit type.
    If the alignment for the payload is no more than 4 Byte (almost certain for 32 Bit
    systems) then this means a RAM reduction of 4 Byte per queued element. This can sum up
    to several hundreds of Byte for a typical embedded application.}\n
      Caution, the chosen type must not be larger than unsigned int on the given platform. */
typedef unsigned short int uintidx_t;

/** The queue. A ring buffer implementation has been chosen. */
struct vsq_queue_t
{
    /** The ring buffer elements of the queue.\n
          This member is written by producer and consumer but both will never touch it at
        the same array entry at a time. This spatial mutual exclusion is controlled by the
        other members \a idxHead and \a idxTail. */
    void *ringBuffer;

    /** The size in Byte of the ring buffer.\n
          This member is constant and read-only (after initialization) and out of scope of
        race conditions between producer and consumer. */
    unsigned int sizeOfRingBuffer;

    /** The read position into the ring buffer of the queue. The element, this index refers
        to, is owned by the consumer for race condition free data processing.\n
          The name of this member results from the idea of using the ring buffer to
        implement a queue; new elements are appended to the tail of the queue and elder,
        consumed ones are removed from the (stale) head.\n
          This member is read-only to the data producer and updated by the consumer. It is
        important that this member can be read or written in a single atomic operation;
        this explains the type, which has to be configured platform dependent. */
    volatile
        #ifdef _STDC_VERSION_C17_C11
        _Alignas(ALIGN_OF_UINTATOMIC) /* Required for TriCore */
        #endif
        uintatomic_t idxHead;

#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
    /** The position of the current head of the list of data elements currently in the ring
        buffer. The index denotes the perspective of the producer. The consumer has his own
        index \a idxRead. The distinction between producer and consumer index avoids
        forbidden read-modify-write operations.\n
          This member is read and updated solely by the producer. There are no race
        conditions with the consumer code. */
    unsigned int idxHeadCopy;
#endif

    /** The position of the current tail of the list of data elements currently in the ring
        buffer. The tail element always exists. Strictly spoken, we never have an empty
        queue. The consumer always has the ownership of one element and after creation of
        the queue this is the (empty) tail element. (Although the consumer doesn't have
        awareness of this ownership.)\n
          This member is read-only to the data consumer and updated by the producer. It is
        essential that this member can be read or written in a single atomic operation;
        this explains the type, which has to be configured platform dependent. */
    volatile
        #ifdef _STDC_VERSION_C17_C11
        _Alignas(ALIGN_OF_UINTATOMIC) /* Required for TriCore */
        #endif
        uintatomic_t idxTail;

    /** The use of the API vsq_allocTailElement/vsq_postTailElement requires temporary
        storage of some intermediate data. Since the reentrance of this function pair is
        anyway not given by principle, we can use the queue object itself to store this
        data. idxNewAllocatedTail is set by vsq_allocTailElement() for later use by
        vsq_postTailElement().\n
          This member is accessed only by the producer code and is not subject to race
        conditions. */
    unsigned int idxNewAllocatedTail;

#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
    /** The current number of elements in the ring buffer.\n
          This member is updated and read solely by the producer. There are no race
        conditions. */
    unsigned int usage;

    /** The maximum number of elements, which had ever been stored in the ring buffer.\n
          This member is read-only to the data consumer and updated by the producer. It is
        essential that this member can be read or written in a single atomic operation;
        this explains the type, which has to be configured platform dependent. */
    volatile
        #ifdef _STDC_VERSION_C17_C11
        _Alignas(ALIGN_OF_UINTATOMIC) /* Required for TriCore */
        #endif
        uintatomic_t maxUsage;

    /** The maximum number of bytes so far, which had been used for queuing elements. The
        number of bytes includes all overhead for data organization and can be related to
        the size of the ring buffer.\n
          This number can be used at application validation time for optimization of the
        required queue size.\n
          This member is read-only to the data consumer and updated by the producer. It is
        essential that this member can be read or written in a single atomic operation;
        this explains the type, which has to be configured platform dependent. */
    volatile
        #ifdef _STDC_VERSION_C17_C11
        _Alignas(ALIGN_OF_UINTATOMIC) /* Required for TriCore */
        #endif
        uintatomic_t maxUsageInByte;
#endif
};



/** Formally, this is the type of a queued element. We use a flexible array member to model
    the payload data. Effectively, this is the heaer of the element, since the payload data
    is not really inside the struct. The combination of both characteristics is
    advantageous, we can access the payload by the normal dot operator. */
typedef struct queueElement_t
{
    /** A union is applied just to force a certain size of the struct. The size needs to be
        an integral multiple of the alignment, which is specified for the payload data. The
        fulfillment of the specification is double-checked by several depending assertions.
        If such an assertion fires then it may easily be that this type definition is
        wrong.
          The union is implemented anonymously, as the alternatives are meaningless to the
        functional code and effectively not used in the source code. */
    union
    {
        /** The use of an union to ensure the object size requires an anonymous struct to
            bundle all true struct members. */
        struct
        {
            /** The link to the successor element in the queue. It is the Byte index into
                the ring buffer at which the first byte of the header of the successor is
                found. */
            uintidx_t idxNext;

            /** The size of the payload of the queued element in Byte. */
            uintidx_t sizeOfPayload;
        };

        /** A member of the union, which is not used but which enforces the required size
            of the queue element header. The size needs to be an integral multiple of
            #ALIGN_OF_PAYLOAD. */
        uint8_t dummyToForceAlignment[ALIGN_OF_PAYLOAD];
    };

#if defined(_STDC_VERSION_C17_C11) || defined(_STDC_VERSION_C99)
    uint8_t payload[];
#elif defined(__GNUC__)
    uint8_t payload[0];
#else
# error Flexible array members are not supported by your compiler
#endif
} queueElement_t;


/*
 * Local prototypes
 */


/*
 * Data definitions
 */


/*
 * Function implementation
 */

/**
 * We are creating an array of elements. The array as a whole must be correct aligned (using
 * method byteOffsetOfRingBuffer()) but the element size must be a multiple of the element
 * alignment, too - otherwise would latest the second element in the array be misaligned.
 * This method computes the element size to be reserved in the array based on the user
 * demanded element size.
 *   @return
 * Get the number of Bytes, which need to be reserved for each element in the ring buffer
 * array that implements the queue.
 *   @param sizeOfElement
 * The user specified size of an element in the queue.
 *   @param alignOfElement
 * The alignment required for an element in the queue.
 */

static inline unsigned int alignedSizeOfObject( unsigned int sizeOfElement
                                              , unsigned int alignOfElement
                                              )
{
    const unsigned int mask = alignOfElement-1;
    return ((sizeOfElement + mask) & ~mask);

} /* End of alignedSizeOfObject */




#ifdef DEBUG
/**
 * Test function, mainly intended for code self-tests in assertions: Check proper alignment
 * of an address or Byte index.
 *   @return
 * \a true, if address/index has required alignment, else \a false.
 *   @param addressOrIndex
 * The address or Byte index to check.
 *   @param alignment
 * The required alignment. Is a power of two, most likely 1, 2, 4 or 8.
 */
static inline bool isaligned(uintptr_t address, unsigned int alignment)
{
    return (address & ~(alignment-1)) == address;

} /* End of isaligned */
#endif


/**
 * Figure out, how many ring buffer space we need at minimum to store a given number of
 * elements of given size.
 *   @return
 * Get the number of bytes required for the ring buffer.
 *   @param maxNoStdElements
 * This number of elements, each having a size of \a sizeOfStdElement Byte, needs to fit
 * into the queue.
 *   @param sizeOfStdElement
 * See \a maxNoStdElements.
 */
static unsigned int calculateSizeOfRingBuffer( unsigned int maxNoStdElements
                                             , unsigned int sizeOfStdElement
                                             )
{
    /* (maxQueueLength+1): One element of the ring buffer is always owned by the consumer
       for reading and not available to filling the queue. To fulfill the user
       expectations, we need to allocate one element more than the demanded maximum length
       of the queue.
         sizeof(queueElement_t) is guaranteed to be a multiple of ALIGN_OF_PAYLOAD, so that
       the payload beginns at the correct aligned address. */
    assert(alignedSizeOfObject(sizeof(queueElement_t), ALIGN_OF_PAYLOAD) % ALIGN_OF_PAYLOAD
           == 0
           && offsetof(queueElement_t, payload) == sizeof(queueElement_t)
          );
    return (maxNoStdElements+1)
           * (sizeof(queueElement_t)                                    /* Element header */
              + alignedSizeOfObject(sizeOfStdElement, ALIGN_OF_PAYLOAD) /* Payload */
             );
} /* End of calculateSizeOfRingBuffer */






/**
 * Retrieve a queued element from the ring buffer, whichis identified by its index in that
 * linear array.
 *   @return
 * Get the element by reference. The returned pointer permits read and write access.
 *   @param idxStartOfElement
 * The Byte offset into the ring buffer. The first byte of an element will be assumed at
 * this position.
 */
static inline queueElement_t *getElementAt( const vsq_queue_t * const pQueue
                                          , unsigned int idxStartOfElement
                                          )
{
    VSQ_ASSERT(isaligned(idxStartOfElement, ALIGN_OF_PAYLOAD)
               &&  idxStartOfElement < pQueue->sizeOfRingBuffer
              );
    return (queueElement_t*)((char*)pQueue->ringBuffer + idxStartOfElement);

} /* End of getElementAt */



/**
 * Get the link index of the element in the ring buffer, which \a idxStartOfElement points
 * to. The link index is the index in the linear ring buffer of the first byte of the
 * header of the successor element in the queue.
 *   @return
 * The link is implemented as the Byte index into the ring buffer of the first header byte
 * of the next element. Get this index.
 *   @param idxStartOfElement
 * The Byte offset into the ring buffer. The first byte of an element will be assumed at
 * this position.
 */
static inline unsigned int getLinkPtrOfElementAt( const vsq_queue_t * const pQueue
                                                , unsigned int idxStartOfElement
                                                )
{
    const queueElement_t * const pElem = getElementAt(pQueue, idxStartOfElement);
    const unsigned int idxStartOfNextElement = pElem->idxNext;
    VSQ_ASSERT(idxStartOfNextElement < pQueue->sizeOfRingBuffer);
    return idxStartOfNextElement;

} /* End of getLinkPtrOfElementAt */



/**
 * Set the link index of the element in the ring buffer, which \a idxStartOfElement points
 * to. The link index is the index in the linear ring buffer of the first byte of the
 * header of the successor element in the queue.
 *   @param idxStartOfElement
 * The Byte offset into the ring buffer. The first byte of an element will be assumed at
 * this position.
 *   @param idxStartOfSuccessor
 * The Byte offset into the ring buffer. The first byte of the successor element will be
 * assumed at this position.
 */
static inline void setLinkPtrOfElementAt( vsq_queue_t * const pQueue
                                        , unsigned int idxStartOfElement
                                        , unsigned int idxStartOfSuccessor
                                        )
{
    queueElement_t * const pElem = getElementAt(pQueue, idxStartOfElement);

    VSQ_ASSERT(isaligned(idxStartOfSuccessor, ALIGN_OF_PAYLOAD)
               &&  idxStartOfSuccessor < pQueue->sizeOfRingBuffer
              );
    pElem->idxNext = idxStartOfSuccessor;

} /* End of setLinkPtrOfElementAt */



/**
 * Set the complete header information of the element in the ring buffer, which \a
 * idxStartOfElement points to. The header consists of the payload size and the link index,
 * which is the index in the linear ring buffer of the first byte of the header of the
 * successor element in the queue.
 *   @param idxStartOfElement
 * The Byte offset into the ring buffer. The first byte of an element will be assumed at
 * this position.
 *   @param idxStartOfSuccessor
 * The Byte offset into the ring buffer. The first byte of the successor element will be
 * assumed at this position.
 *   @param sizeOfPayload
 * The size of the payload of the element addressed by idxStartOfElement.
 */
static inline void setHdrOfElementAt( vsq_queue_t * const pQueue
                                    , unsigned int idxStartOfElement
                                    , unsigned int idxStartOfSuccessor
                                    , unsigned int sizeOfPayload
                                    )
{
    queueElement_t * const pElem = getElementAt(pQueue, idxStartOfElement);

    VSQ_ASSERT(isaligned(idxStartOfSuccessor, ALIGN_OF_PAYLOAD)
               &&  idxStartOfSuccessor < pQueue->sizeOfRingBuffer
               &&  idxStartOfSuccessor == 0
                   ||  idxStartOfElement + sizeof(queueElement_t) + sizeOfPayload
                       <= idxStartOfSuccessor
              );
    pElem->idxNext = idxStartOfSuccessor;
    pElem->sizeOfPayload = sizeOfPayload;

} /* End of setLinkPtrOfElementAt */



/**
 * Check if the queue has currently room to append a new element to the tail. Return the
 * available element in case.
 *   @return
 * Either get the pointer to the location where the payload of the new element can be
 * copied to or NULL if the queue is currently too full to store the new element.
 *   @param pQueue
 * The queue object by reference.
 *   @param pIdxNewTail
 * Part of processing is the computation of the index of the element in the linear array,
 * which implements the ring buffer. This index is returned by reference if the function
 * doesn't return NULL. It points to the header of the returned element.
 *   @param sizeOfPayload
 * The payload of the new element will have a size of this number of bytes.
 *   @remark
 * The function is an inline implementation of the common part of both APIs to access the
 * tail of the queue.
 */
static inline void *allocTailElement( vsq_queue_t * const pQueue
                                    , unsigned int *pIdxNewTail
                                    , unsigned int sizeOfPayload
                                    )
{
    const unsigned int idxHead = pQueue->idxHead;

#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
    /* Keep track with the movement of the read index by the consumer. This way, we can
       maintain the number of currently stored elements without the need of
       read-modify-write operation (which we don't have). */
    unsigned int idxHeadCopy = pQueue->idxHeadCopy
               , noElements = pQueue->usage;
    if(idxHeadCopy != idxHead)
    {
        do
        {
            /* Acknowledge the considered element and check for underflow. */
            -- noElements;
            VSQ_ASSERT(noElements < pQueue->sizeOfRingBuffer);

            /* Advance to successor of visited element. */
            idxHeadCopy = getLinkPtrOfElementAt(pQueue, idxHeadCopy);
        }
        while(idxHeadCopy != idxHead);

        /* Write back the updated read-acknowledge pointer. */
        pQueue->idxHeadCopy = idxHeadCopy;
    }
#endif

    /* We use a ring buffer but don't want to offer a kind of wrapped memory space to the
       caller. Either the rest of ring buffer space behind the tail suffices or the
       beginning of the ring buffer up to the head. Otherwise we reject the new element.
         Before writing, the tail index points to the last queued element (the one, which
       is currently owned by the consumer for reading). */

    const unsigned int idxTail = pQueue->idxTail
                     , sizeOfRingBuffer = pQueue->sizeOfRingBuffer;

    /* Initial candidate for placing the new element is just behind the current tail. */
    const unsigned int idxNew = getLinkPtrOfElementAt(pQueue, idxTail);

    const unsigned int sizeOfElem = sizeof(queueElement_t)
                                    + alignedSizeOfObject(sizeOfPayload, ALIGN_OF_PAYLOAD);
    unsigned int idxNewTail = UINT_MAX;
    if(idxNew > idxHead)
    {
        /* We can place the new element behind the tail or - if this doesn't fit - we skip
           the rest of the buffer and start over at the beginning. */
        if(sizeOfRingBuffer >= idxNew + sizeOfElem)
        {
            /* The rest of the ring buffer has still enough space. idxNew is confirmed. */
            idxNewTail = idxNew;
        }
        else if(idxHead >= sizeOfElem)
        {
            /* The end of the ring buffer was too small, but our second choice, the
               beginning of the ring buffer, is large enough. Index 0 is confirmed. */
            idxNewTail = 0;
        }
    }
    else if(idxNew < idxHead  &&  idxHead >= idxNew + sizeOfElem)
    {
        /* The ring buffer has enough space between tail and head. idxNew is confirmed. */
        idxNewTail = idxNew;
    }

    if(idxNewTail != UINT_MAX)
    {
        /* The link to the found new element needs to be written into the tail element so
           far.\n
             We can safely touch the tail so far although it may already be owned by the
           consumer: The link won't ever be evaluated by the consumer before it decides to
           advance to the next element and this decision is triggered only by statements
           down below the memory barrier in postTailElement(). */
        setLinkPtrOfElementAt( pQueue
                             , /* idxStartOfElement */ idxTail
                             , /* idxStartOfSuccessor */ idxNewTail
                             );

        /* The link to the successor of our new element can't be made yet - we can't
           anticipate its size, which would be needed for its placement. However, we set a
           preliminary link, which points directly behind the new element. This preliminary
           value is evaluated by allocTailElement(), when it is called for the successor.
           The position directly behind will be the initial candidate for placing the
           successor. */
        unsigned int idxStartOfSuccessor = idxNewTail + sizeOfElem;
        if(idxStartOfSuccessor >= sizeOfRingBuffer)
        {
            /* "Directly behind" is meant in a cyclic manner. It is the beginning of the buffer
               if we reach its end. */
            VSQ_ASSERT(idxStartOfSuccessor == sizeOfRingBuffer);
            idxStartOfSuccessor = 0;
        }
        setHdrOfElementAt( pQueue
                         , /* idxStartOfElement */ idxNewTail
                         , idxStartOfSuccessor
                         , sizeOfPayload
                         );

#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
        /* Write back the updated element count. +1: This function actually means adding a
           new element. */
        pQueue->usage = noElements + 1;
        if(noElements >= pQueue->maxUsage)
            pQueue->maxUsage = noElements + 1;

        /* Compute the currently allocated number of bytes. It is the cyclic difference
           from head to the end of the upcoming element, which beginns a the new tail.
             Note, at storage of the first element, the calculation incorporates the empty
           dummy element. This is not a fault, as it belongs to the overhead, which is
           documented to belong into the reported usage value. */
        unsigned int usageInByte = idxStartOfSuccessor - idxHead;
        if((int)usageInByte < 0)
            usageInByte += pQueue->sizeOfRingBuffer;
        if(usageInByte > pQueue->maxUsageInByte)
            pQueue->maxUsageInByte = usageInByte;
#endif

        /* The function returns the position of the new element in the ring buffer (needed
           in postTailElement() for announcing the new element to the consumer) and the
           pointer to where to put the payload data. */
        *pIdxNewTail = idxNewTail;
        return &getElementAt(pQueue, idxNewTail)->payload[0];
    }
    else
    {
#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
        /* Write back the updated element count. */
        pQueue->usage = noElements;
#endif
        /* The function returns NULL - no space. */
        return NULL;
    }
} /* End of allocTailElement */



/**
 * Counterpart to void *allocTailElement(vsq_queue_t *, unsigned int *): Finalize appending
 * the previously allocated element to the queue, i.e. notify a consumer the availability
 * of this element. The ownership of the reserved element, which was gained with
 * allocTailElement(), ends with entry into this method; the producer must not touch the
 * element any more.
 *   @param pQueue
 * The queue object by reference.
 *   @param idxNewTail
 * The index of the first header byte of the appended element in the linear array, which
 * implements the ring buffer. This index is entirely meaningless to the caller but had
 * been returned by the preceding, corresponding call of \a allocTailElement. The producer
 * code is responsible of storing and returning this value.
 *   @remark
 * The function is an inline implementation of the common part of both APIs to access the
 * tail of the queue.
 */
static inline void postTailElement( vsq_queue_t *pQueue
                                  , unsigned int idxNewTail
                                  )
{
    /* We put a full memory barrier between filling the new element and updating the link
       in the tail so far on the one hand and notifying the availablity of the new element
       to the consumer. All instructions for element filling will have completed before the
       index change of the tail gets visible to the consumer. */
    MEMORY_BARRIER_FULL();

    /* Notify the new element by updating the shared tail index in an atomic write. */
    VSQ_ASSERT(idxNewTail != pQueue->idxHead);
    pQueue->idxTail = idxNewTail;

} /* End of postTailElement */




/**
 * Compute the byte offset of the memory for the ringbuffer if putting both, the object and
 * the ringbuffer into a single memory chunk. The two elements can't simply be concatenated,
 * we need to consider a gap to fulfill all alignment requirements.
 *   @return
 * Get the offset from the beginning of the queue object to the beginning of the ring
 * buffer in Byte.
 *   @remark
 * This simple function holds the common code of object creation and object size query.
 */
static inline unsigned int byteOffsetOfRingBuffer(void)
{
    /* Consider the maybe different alignment of the queue element when computing the
       beginning of the ring buffer memory inside the chunk. */
    return alignedSizeOfObject(sizeof(vsq_queue_t), ALIGN_OF_PAYLOAD);

} /* End of byteOffsetOfRingBuffer */



/**
 * Calculate the size of a queue object.\n
 *   This function is meant to be called prior to creation of a queue object. The caller of
 * the constructor is in charge of allocating the memory for the object - the intended use
 * case of the queue implementation is the embedded environment, which doesn't permit to
 * allocate memory dynamically using \a malloc.\n
 *   This function needs to be called with the same parameters as later the constructor. The
 * constructor will silently assume that the memory chunk it receives from the caller will
 * have the size computed by this method. The typical use case avoids the use of \a malloc:
 * @code
 *  struct qElement_t;
 *  #define MAX_Q_LEN 10
 *  const unsigned int sizeOfQ = vsq_getSizeOfQueue( MAX_Q_LEN
 *                                                 , sizeof(struct qElement_t)
 *                                                 );
 *  VSQ_ASSERT(_Alignof(struct qElement_t) <= _Alignof(unsigned int));
 *  _Alignas(unsigned int) char memoryChunk[sizeOfQ];
 *  vsq_queue_t myNewQ = vsq_createQueue( memoryChunk
 *                                      , MAX_Q_LEN
 *                                      , sizeof(struct qElement_t)
 *                                      );
 * @endcode
 *   Alternatively, the embedded environment may offer a simple, uncritical memory
 * allocation or partitioning API, most likely without a free function to avoid
 * fragmentation and indeterministic timing behavior.
 *   @return
 * The number of bytes required to construct a queue object with the passed parameters.
 * Only this number is computed, nothing else happens, in particular no queue object is
 * constructed.\n
 *   If a 16 Bit integer type is used to implement the link indexes for the queue elements,
 * it can happen that the the specified capacity of the queue is not realizable. The
 * function returns zero in this case.
 *   @param maxNoStdElements
 * The queue implementation imposes a fixed maximum size of the queue. The size is
 * specified in terms of how many elements of given size \a would fit into the queue. At
 * run-time, stored elements can have arbitrary sizes so that no statement is possible
 * about the storable number.\n
 *   Rationale: Using a typical standard element size plus a number of those elements as
 * size specification supports the still most relevant use cases with elements of identical
 * size.\n
 *   A queue size of zero is considered an error in the client code. This is caught by
 * assertion and NULL is returned. \a maxNoStdElements * \a sizeOfStdElement needs to be
 * greater than zero.
 *   @param sizeOfStdElement
 * The size of a standard element, which should be storable \a maxNoStdElements times in
 * the queue. This size is just used for specification of the queue's capacity but has no
 * meaning at run-time any more.
 *   @remark
 * Caution, the implementation is not made safe against overflows on systems, where type
 * unsigned int is less than 32 Bit. On such systems, you must not rely on the function
 * return value but carefully double-check that the size of the ring buffer doesn't exceed
 * the 64 kByte limit. Actually, this is bit theoretic as such systems won't ever have so
 * much RAM that this constraint would be relevant.
 *   @remark
 * The function operates without type information but needs to form a list of
 * elements. To safely do so it needs to anticipate the alignment required for such an
 * element. This alignment will later be applied to all contained elements, regardless of
 * their individual size.\n
 *   Usually this is the alignment of the largest element type if it are primitive types or
 * the alignment of the largest field in any of the elements if it are structs.\n
 *   The required alignment usually is a platform dependent constant and not an application
 * dependent run-time argument. Therefore, it is supplied as macro #ALIGN_OF_PAYLOAD, which
 * is part of the compile-time configuration data of this module. You should double-check
 * the setting for your particular platform.
 *   @remark
 * The memory chunk required by the constructor does not only need to have the right
 * minimum size but, secondary, it needs to have the right alignment, which is specified at
 * compile-time using macro #ALIGN_OF_PAYLOAD.
 *   @see vsq_createQueue()
 */
unsigned int vsq_getSizeOfQueue( unsigned int maxNoStdElements
                               , unsigned int sizeOfStdElement
                               )
{
    unsigned int size = 0;
    if(maxNoStdElements > 0  &&  sizeOfStdElement > 0)
    {
        size = byteOffsetOfRingBuffer()
               + calculateSizeOfRingBuffer(maxNoStdElements, sizeOfStdElement);

        /* For 16 Bit index values: Check if the ring buffer fits into an array of maximum
           64 kByte.
             Note, we need to disable the warning "type-limits" for the next statement.
           Justification: If we configure with our special integer types (atomic and for
           array indexes) as unsigned int then the condition reduce to an always true
           expression, which the compiler warns about. However, the true is what we want to
           see, so no worries behind the warning. */
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wtype-limits"
        if(sizeof(((const queueElement_t*)NULL)->idxNext) < sizeof(unsigned int)
           &&  size >= (1 << sizeof(((const queueElement_t*)NULL)->idxNext)*8u)
           ||  sizeof(uintatomic_t) < sizeof(unsigned int)
               &&  size >= (1 << sizeof(uintatomic_t)*8u)
          )
        {
            size = 0;
        }
#pragma GCC diagnostic pop
    }
    VSQ_ASSERT(size > 0);
    return size;

} /* End of vsq_getSizeOfQueue */




/**
 * Create a new queue object.
 *   @return
 * Get the pointer to the ready-to-use queue object. Effectively, this is the same pointer
 * as \a pMemoryChunk but type casted for use as queue. If the caller uses dynamic memory
 * management then he may free either his memory chunk or the returned pointer after use of
 * the queue.
 *   @param pMemoryChunk
 * The caller is in charge of allocating memory for the new queue object. A memory chunk of
 * required size or bigger is passed in by reference. The required size needs to queried
 * with the other method vsq_getSizeOfQueue() prior to the call of this constructor.\n
 *    Besides the right size, the memory chunk needs to have the right alignment, which is
 * specified at compile-time unsing macro #ALIGN_OF_PAYLOAD. If you use \a malloc to
 * allocate the memory chunk than this should normally be granted.\n
 *   The correct alignment is double-checked by assertion.
 *   @param maxNoStdElements
 * The queue implementation imposes a fixed maximum size of the queue. The size is
 * specified in terms of how many elements of given size \a would fit into the queue. At
 * run-time, stored elements can have arbitrary sizes so that no statement is possible
 * about the storable number.\n
 *   Rationale: Using a typical standard element size plus a number of those elements as
 * size specification supports the still most relevant use cases with elements of identical
 * size.\n
 *   A queue size of zero is considered an error in the client code. This is caught by
 * assertion and NULL is returned. \a maxNoStdElements * \a sizeOfStdElement needs to be
 * greater than zero.
 *   @param sizeOfStdElement
 * The size of a standard element, which should be storable \a maxNoStdElements times in
 * the queue. This size is just used for specification of the queue's capacity but has no
 * meaning at run-time any more.
 *   @remark
 * For alignment considerations and constarints, please refer to vsq_getSizeOfQueue().
 *   @remark
 * There's no destructor for a queue object. The caller is responsible for providing the
 * memory for the object and freeing this memory -- if applicable -- would be the only
 * operation to delete a queue object after use.
 *   @see vsq_getSizeOfQueue()
 */
struct vsq_queue_t *vsq_createQueue( void *pMemoryChunk
                                   , unsigned int maxNoStdElements
                                   , unsigned int sizeOfStdElement
                                   )
{
#define CHECK_THIS                                                                          \
            ((ALIGN_OF_UINTATOMIC | (ALIGN_OF_UINTATOMIC-1))+1 == 2*ALIGN_OF_UINTATOMIC     \
             &&  (ALIGN_OF_HDR | (ALIGN_OF_HDR-1))+1 == 2*ALIGN_OF_HDR                      \
             &&  (ALIGN_OF_PAYLOAD | (ALIGN_OF_PAYLOAD-1))+1 == 2*ALIGN_OF_PAYLOAD          \
             &&  ALIGN_OF_PAYLOAD >= ALIGN_OF_HDR                                           \
             &&  sizeof(queueElement_t) % ALIGN_OF_PAYLOAD == 0                             \
             &&  offsetof(queueElement_t, payload) % ALIGN_OF_PAYLOAD == 0                  \
             &&  sizeof(unsigned int) >= sizeof(uintidx_t)                                  \
            )
#if defined(_STDC_VERSION_C17_C11)
    _Static_assert(CHECK_THIS, "Check configuration of module");
    _Static_assert(_Alignof(queueElement_t) == ALIGN_OF_HDR, "Check configuration of module");
#else
    assert(CHECK_THIS);
#endif
#undef CHECK_THIS

    /* A queue with zero capacity is useless and surely a mistake in the client code. */
    if(maxNoStdElements == 0  ||  sizeOfStdElement == 0)
    {
        VSQ_ASSERT(false);
        return NULL;
    }

    vsq_queue_t * const pQueue = (vsq_queue_t*)pMemoryChunk;

    /* Check the alignment of the memory chunk for the queue object. */
    #ifdef _STDC_VERSION_C17_C11
    # define ALIGN_OF_QUEUE_T _Alignof(vsq_queue_t)
    #elif defined(__GNUC__) && defined(__WIN64__)
    # define ALIGN_OF_QUEUE_T 8
    #elif defined(__GNUC__) && defined(__WIN32__)
    # define ALIGN_OF_QUEUE_T 4
    #elif defined(__AVR__)
    # define ALIGN_OF_QUEUE_T 1
    #else
    # error Define the alignment of struct queue_t for your compiler/target
    #endif
    VSQ_ASSERT(isaligned((uintptr_t)pQueue, ALIGN_OF_QUEUE_T));
    #undef ALIGN_OF_QUEUE_T

    /* An element consists of a header, which makes the link to the next element, and the
       user provided payload. The alignment specified for the payload is guaranteed
       ensuring that the header size is a multiple of the demanded alignment. */
    pQueue->sizeOfRingBuffer = calculateSizeOfRingBuffer(maxNoStdElements, sizeOfStdElement);
    pQueue->ringBuffer = (void*)((char*)pMemoryChunk + byteOffsetOfRingBuffer());
    VSQ_ASSERT(isaligned((uintptr_t)pQueue->ringBuffer, ALIGN_OF_PAYLOAD));
    VSQ_ASSERT((uintptr_t)pQueue
               + vsq_getSizeOfQueue(maxNoStdElements, sizeOfStdElement)
               == (uintptr_t)pQueue->ringBuffer + pQueue->sizeOfRingBuffer
              );
    //memset(pQueue->ringBuffer, /* char */ 0, /* len */ pQueue->sizeOfRingBuffer);

    /* The head and tail of the queue and the read position into it all point initially to
       the same element.
         Where to place it matters with respect to a common use case of queues. If the
       queues is solely used with elements of always same size (and this size had been
       specified at queue creation time) then the initial element should be put at the very
       end of the ring buffer; only then it will be possible to fill the buffer entirely
       without fragmentation and as many elements will fit as had been specified at
       creation time. */
    pQueue->idxTail = pQueue->sizeOfRingBuffer - sizeof(queueElement_t);
    pQueue->idxHead = pQueue->idxTail;

    /* The element, which is initially owned by the consumer, is empty. Its link points to
       first element behind the one, which will be written by the first producer activity. */
    setLinkPtrOfElementAt( pQueue
                         , /* idxStartOfElement */ pQueue->idxTail
                         , /* idxStartOfSuccessor */ 0
                         );

    pQueue->idxNewAllocatedTail = UINT_MAX;
#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
    pQueue->idxHeadCopy = pQueue->idxHead;
    pQueue->usage = 0;
    pQueue->maxUsage = 0;
    pQueue->maxUsageInByte = 0;
#endif

    return pQueue;

} /* End of vsq_createQueue */




/**
 * Append a new element to the tail of the queue.
 *   @return
 * The operation can fail; the queue is implemented with a pre-determined maximum size and
 * it can be currently full. Get \a true if the operation succeeds and \a false in case of
 * a currently full queue.\n
 *   The elements themselves have a flexible size. The function will return \a false if the
 * caller tries to write more bytes than currently fit into the queue.
 *   @param pQueue
 * The queue object to write to by reference.
 *   @param pData
 * The pointer to the payload data of the appended element. This is an anonymous byte
 * sequence to this method. memcpy is used to copy the data into the queue, which imposes
 * no alignment requirements on the data.
 *   @param noBytes
 * The number of bytes to write.\n
 *   No data will be copied to the queue if the function returns \a false.\n
 *   Note, it is valid to append an empty element. The related later call of
 * vsq_readFromHead() will return a non-NULL data pointer in combination with a number of
 * zero bytes.
 *   @see vsq_allocTailElement()
 *   @see vsq_readFromHead()
 */
bool vsq_writeToTail(vsq_queue_t *pQueue, const void *pData, unsigned int noBytes)
{
    unsigned int idxNewTail;
    void *pFreeElem = allocTailElement(pQueue, &idxNewTail, noBytes);

    if(pFreeElem != NULL)
    {
        memcpy(pFreeElem, pData, noBytes);

        /* Notify the new element to the consumer and update the statistics of used
           elements. */
        postTailElement(pQueue, idxNewTail);

        return true;
    }
    else
        return false;

} /* End of vsq_writeToTail */



/**
 * Check if the queue has currently room to append another element to the tail and return the
 * available element in case.\n
 *   This method, together with the other method vsq_postTailElement(), is an alternative
 * API to write to the end of the queue. If vsq_allocTailElement() returns a non NULL
 * pointer then the caller can take any time to fill the queue element the return value
 * points to without fearing any race conditions. After having the element filled he will
 * use \a vsq_postTailElement to submit the element. From now on the element will be
 * visible to the consumer at the end of the queue.\n
 *   Using this API in contrast to vsq_writeToTail() can save a local copy of the produced
 * data in the producers implementation.\n
 *   The producer APIs to access the tail of the queue are not race condition free. A call
 * of \a vsq_writeToTail must either\n
 *   - return before the next call of the same method or\n
 *   - return before the invocation of \a vsq_allocTailElement or\n
 *   - be initiated after return from \a vsq_postTailElement.
 *
 * This means for a single producer context, that it can alternatingly use \a
 * vsq_writeToTail and the pair of \a vsq_allocTailElement and \a vsq_postTailElement. For
 * concurrent producer contexts it imposes the need for the implementation of mutual
 * exclusion code at the caller's side.
 *   @return
 * Get either the next available ring buffer element by reference or NULL if the queue is
 * currently full.\n
 *   The returned pointer is aligned as had been specified at compile-time using macro
 * #ALIGN_OF_PAYLOAD. The returned pointer can be safely casted to the element type and
 * access to the element (or its fields in case of a struct) can be done through this
 * pointer. There is no time limit in keeping the pointer (i.e. until data submission with
 * \a vsq_postTailElement) and using the pointer can avoid the need for an additional local
 * copy of the data during data production time.
 *   @param pQueue
 * The queue object by reference.
 *   @param sizeOfPayload
 * The size of the payload of the appended element in Byte.
 *   @remark
 * The calls of \a vsq_allocTailElement and \a vsq_postTailElement need to be done strictly
 * alternatingly. It is not possible to reserve several elements by multiple calls of the
 * former method and to submit them later by the same number of calls of the latter method.
 *   @remark
 * It is possible to use this method prior to \a vsq_writeToTail to query if the queue is
 * currently full and to avoid a negative return value of that function. It's however
 * disencouraged to do so. There is a little useless computation overhead in doing so and,
 * more important, the pair of \a vsq_allocTailElement and \a vsq_writeToTail is not race
 * condition free; a queue element could become free between the two calls -- the strategy
 * would be too conservative.
 */
void *vsq_allocTailElement(vsq_queue_t * const pQueue, unsigned int sizeOfPayload)
{
    VSQ_ASSERT(pQueue->idxNewAllocatedTail == UINT_MAX);
    return allocTailElement(pQueue, &pQueue->idxNewAllocatedTail, sizeOfPayload);

} /* End of vsq_allocTailElement */




/**
 * Submit a queue element, which had been allocated with vsq_allocTailElement().\n
 *   From now on, the element is in the queue and visible to the consumer. The pointer,
 * which had been got from \a vsq_allocTailElement is invalid and must no longer be used.\n
 *   Please, find more details of using this API in the description of the counterpart
 * method \a vsq_allocTailElement.
 *   @param pQueue
 * The queue object, where the submitted element had been allocated.
 *   @see void *vsq_allocTailElement(vsq_queue_t *)
 */
void vsq_postTailElement(vsq_queue_t *pQueue)
{
    VSQ_ASSERT(pQueue->idxNewAllocatedTail != UINT_MAX);
    postTailElement(pQueue, pQueue->idxNewAllocatedTail);
#ifdef DEBUG
    pQueue->idxNewAllocatedTail = UINT_MAX;
#endif
} /* End of vsq_postTailElement */




/**
 * Read a meanwhile receivced new element from the head of the queue.
 *   @return
 * Get the pointer to the newly received element if a new element has arrived. \a NULL is
 * returned if no new element has been received since the previous invocation of this
 * method.\n
 *   The element, which is returned by reference is from now on owned by the data consumer,
 * i.e. the caller of this method. It may use the pointer to read the data. The ownership
 * only ends by getting another pointer to another element with a future invocation of this
 * method. In particular, it does not end when a future call of this method returns \a
 * NULL. The access to the owned element is race condition free for the owner of the
 * pointer.\n
 *   The returned pointer is aligned as had been specified at compile-time using macro
 * #ALIGN_OF_PAYLOAD. The returned pointer can be safely casted to the element type and
 * access to the element (or its fields in case of a struct) can be done through this
 * pointer without the need for first copying the data.
 *   @param pQueue
 * The queue object to be read from by reference.
 *   @param pSizeOfPayload
 * The number of bytes, which are conveyed with the received element, is returned by
 * reference. It is the same value as had been provided to the related call of either
 * vsq_writeToTail() or vsq_allocTailElement().\n
 *   The value is set to zero if the function returns NULL.\n
 *   Note, queued elements of size zero are allowed and possible. In which case, the
 * value is set to zero, too; howeber, now the function won't return NULL.\n 
 *   @remark
 * The consumer API is not reentrant. It is not possible to let concurrent consumer
 * contexts read from the head of the queue. This holds even if the consumer code
 * implements synchronization code, which ensures mutual exclusion from this method. This
 * is because the method's effect persists after return from the method; the returned
 * element is reserved to the caller until the next method invocation.
 *   @see bool vsq_writeToTail(vsq_queue_t *, const void *, unsigned int)
 */
const void *vsq_readFromHead(vsq_queue_t *pQueue, unsigned int *pSizeOfPayload)
{
    const void *pReceivedElementsPayload;
    unsigned int idxRead = pQueue->idxHead;
    if(idxRead != pQueue->idxTail)
    {
        const unsigned int newIdxRead = getLinkPtrOfElementAt(pQueue, idxRead);

        /* Here we are between the code that used the data from the element owned by the
           consumer so far and the notification that it is no longer used by the consumer.
           We put a full memory barrier to ensure that the execution of the data consuming
           code is surely completed before the notification can become visible to the
           producer. */
        MEMORY_BARRIER_FULL();
        pQueue->idxHead = newIdxRead;

        const queueElement_t * const pElem = getElementAt(pQueue, newIdxRead);
        pReceivedElementsPayload = &pElem->payload[0];
        *pSizeOfPayload = pElem->sizeOfPayload;
    }
    else
    {
        pReceivedElementsPayload = NULL;
        *pSizeOfPayload = 0;
    }
    return pReceivedElementsPayload;

} /* End of vsq_readFromHead */




#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
/**
 * Get the maximum number of queued elements, which has been seen since creation of the
 * queue object.
 *   @return
 * Get the number of elements.
 *   @param pQueue
 * The queue object by reference, to which the query relates.
 *   @remark
 * The compilation of this API can be turned on/off by configuration switch
 * #VSQ_ENABLE_API_QUEUE_DIAGNOSTICS.
 *   @remark
 * This method my be called at any time from producer or conumer context.
 */
unsigned int vsq_getMaximumQueueUsage(const vsq_queue_t *pQueue)
{
    const unsigned int maxNoQueued = pQueue->maxUsage;
    VSQ_ASSERT(maxNoQueued*sizeof(queueElement_t) <= pQueue->sizeOfRingBuffer);
    return maxNoQueued;

} /* End of vsq_getMaximumQueueUsage */
#endif



#if VSQ_ENABLE_API_QUEUE_DIAGNOSTICS == 1
/**
 * Get the maximum use of buffer memory, which has been seen since creation of the
 * queue object.\n
 *   Note, the variable element size implies, that the queue potentially can't make use of
 * all memory. This is even normal and not an extraordinary situation. Consequently, if the
 * reported memory consumption is less than the configured memory, it doesn't necessarily
 * mean the the queue was never full.
 *   @return
 * Get the number in Byte.
 *   @param pQueue
 * The queue object by reference, to which the query relates.
 *   @remark
 * The compilation of this API can be turned on/off by configuration switch
 * #VSQ_ENABLE_API_QUEUE_DIAGNOSTICS.
 *   @remark
 * This method my be called at any time from producer or conumer context.
 */
unsigned int vsq_getMaximumQueueUsageInByte(const vsq_queue_t *pQueue)
{
    const unsigned int maxUsageInByte = pQueue->maxUsageInByte;
    VSQ_ASSERT(maxUsageInByte <= pQueue->sizeOfRingBuffer);
    return maxUsageInByte;

} /* End of vsq_getMaximumQueueUsageInByte */
#endif
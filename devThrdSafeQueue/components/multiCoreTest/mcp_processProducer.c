/**
 * @file mcp_processProducer.c
 * Multi-Core test of thread-safe queue: This source compiles to a stand-alone Windows
 * application, that implements a data producer. The producer writes random data into the
 * queue object. The queue will be immdiately full if the consumer process is not started
 * as well.\n
 *   The data elements in the queue use sequence counters and CRC checksums to enable data
 * damage detection at the consumer side.\n
 *   The queue object is installed in named shared memory, which can be addressed to by the
 * consumer code, too.\n
 *   The code of this test is not portable. It makes use of several Windows specific
 * operations, particularly the access to the shared memory.\n
 *   Effectively, this test implements a multi-process application of the queue. It bcomes
 * a multi-core because of the Windows script, which is applied to start producer and
 * consumer thread on different cores of a Windows box.
 *
 * Copyright (C) 2016-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
 * Local functions
 */

/*
 * Include files
 */

#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <conio.h>
#include <Windows.h>

#include "cpd_commonProcessDefinitions.h"
#include "crc_checksum.h"
#include "tsq_threadSafeQueue.h"
//#include "mcp_processProducer.h"


/*
 * Defines
 */
 

/*
 * Local type definitions
 */
 
 
/*
 * Local prototypes
 */
 
 
/*
 * Data definitions
 */
 
 
/*
 * Function implementation
 */




 
/*
 * Local type definitions
 */

 
/**
 * Main entry point into the producer application. The application runs in a loop until a
 * key hit event is signalled to the process. In every cycle one data element is written
 * into the queue. The maximum cycling speed is limited by some Windows timing operations.\n
 *   All unexpected situations are reported by assertion. It is useless to compile this
 * code other than in debug configuration. The define DEBUG need to be made on the compiler
 * command line.
 *   @return
 * Always 0, no errors are reported.
 *   @param noArgs
 * The number of command line arguments.
 *   @param argAry
 * The list of \a noArgs command line arguments.
 */

int main(int noArgs, char *argAry[])
{
    const unsigned int sizeOfQueue = tsq_getSizeOfQueue
                                            ( CPD_MAX_QUEUE_LENGTH
                                            , /* maxElementSize */ CPD_SIZE_OF_QUEUE_ELEMENT
                                            , _Alignof(cpd_telegram_t)
                                            )
                     , sizeOfMagic = sizeof(CPD_MAGIC_ID_OF_SHARED_MEM)
                     , sizeOfSharedMem = sizeOfQueue + sizeOfMagic;


    /* https://docs.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-createfilemappinga */
    /// @todo Do we need PAGE_READWRITE + SEC_NOCACHE?
    const HANDLE hSharedMem = CreateFileMapping( INVALID_HANDLE_VALUE
                                               , NULL
                                               , PAGE_READWRITE
                                               , 0
                                               , sizeOfSharedMem
                                               , CPD_NAME_OF_SHARED_MEMORY_FOR_QUEUE
                                               );
    assert(hSharedMem != INVALID_HANDLE_VALUE);
        
    /* See https://docs.microsoft.com/en-us/windows/win32/api/sysinfoapi/nf-sysinfoapi-getsysteminfo. */
    SYSTEM_INFO lpSystemInfo;
    GetSystemInfo(&lpSystemInfo);
    printf( "System info:\n"
            "  dwPageSize = %lu = 0x%lX\n"
            "  lpMinimumApplicationAddress = 0x%p\n"
            "  lpMaximumApplicationAddress = 0x%p\n"
            "  dwAllocationGranularity = %lu = 0x%lX\n"
          , lpSystemInfo.dwPageSize, lpSystemInfo.dwPageSize
          , lpSystemInfo.lpMinimumApplicationAddress
          , lpSystemInfo.lpMaximumApplicationAddress
          , lpSystemInfo.dwAllocationGranularity, lpSystemInfo.dwAllocationGranularity
          );

    /* See https://docs.microsoft.com/en-us/windows/win32/api/memoryapi/nf-memoryapi-mapviewoffileex */
    const uintptr_t alignOfSharedMem = lpSystemInfo.dwAllocationGranularity;
    assert(alignOfSharedMem > 0u
           &&  ((alignOfSharedMem - 1u) | alignOfSharedMem) + 1u == 2u*alignOfSharedMem
          );
    LPVOID const pDesiredViewAddress = (void*)
                                       (((uintptr_t)lpSystemInfo.lpMaximumApplicationAddress
                                         - sizeOfSharedMem
                                        ) & ~(alignOfSharedMem - 1u)
                                       );
    void * const pMemChunk = (void*)MapViewOfFileEx( hSharedMem
                                                   , FILE_MAP_ALL_ACCESS
                                                   , 0
                                                   , 0
                                                   , sizeOfSharedMem
                                                   , pDesiredViewAddress
                                                   );
    assert(pMemChunk != NULL);

    /* Most likely the Windows shared memory function will only return memory chunks with
       the most restrictive alignment. We didn't double check for according documentation
       and simply rely on this. A run time check is anyway done by the queue
       implementation, which would immediately report an alignment problem by assertion. */
    
    /* Write the magic to the beginning of the shared memory to prove at consumer side that
       we really see the same memory. */
    *(unsigned long long*)pMemChunk = CPD_MAGIC_ID_OF_SHARED_MEM;
    
    printf( "MapViewOfFile = 0x%p (desired: 0x%p), magic = 0x%llX\n"
          , pMemChunk
          , pDesiredViewAddress
          , *(unsigned long long*)pMemChunk
          );
    
    if(pMemChunk != pDesiredViewAddress)
    {
        printf("Producer process aborts due to undesired shared memory address\n");
        return 1;
    }
    
    /* The producer is responsible for object creation. The consumer relies on us; he is
       going to type cast the memory address to a queue object. This requires that we are
       started earlier than the consumer process. A real application would use a mutex or
       similar to signal object creation to the consumer. */
    tsq_queue_t *pQueue = tsq_createQueue( pMemChunk + sizeOfMagic
                                         , CPD_MAX_QUEUE_LENGTH
                                         , /* maxElementSize */ CPD_SIZE_OF_QUEUE_ELEMENT
                                         , _Alignof(cpd_telegram_t)
                                         );
#if 0
    /* Windows: The task scheduler normally uses a clock tick of about 15ms. There's
       however an OS call to lower this value. We can achieve a much better timing accuracy
       by setting the value to 1ms.
         Caution: This impact Windows behavior globally! All applications get the new
       timing. */
    const UINT tiOsTick = 1;
    MMRESULT mmRes = timeBeginPeriod(/* uPeriodMs */ tiOsTick);
    printf( "Windows time quantum set to %u ms%s"
            , (unsigned)tiOsTick
            , mmRes == TIMERR_NOERROR? "": " failed"
            );
#endif

    static cpd_telegram_t t = { .payload = {.cnt = (unsigned long)-1000}
                              , .checksum = 0
                              };
    unsigned long noCycles= 0
                , noWrittenElements = 0
                , noEventsQueueFull = 0;
    do
    {
        bool successOfPost;

#ifdef TSQ_ENABLE_API_QUEUE_DIAGNOSTICS
        assert(tsq_getMaximumQueueUsage(pQueue) <= CPD_MAX_QUEUE_LENGTH);
        assert(noEventsQueueFull == 0
               ||  tsq_getMaximumQueueUsage(pQueue) == CPD_MAX_QUEUE_LENGTH
              );
#endif
        //static unsigned int noAPI1=0, noAPI2=0;

        /* Randomly choose the API to write to the queue. */
        if(rand() <= RAND_MAX/2)
        {
            /* API: Prepare object in a local buffer and write it at once to the tail of the
               queue. */

            /* Fill the random part of the telegram. */
            unsigned int u;
            for(u=0; u<sizeof(t.payload.rndAry)/sizeof(t.payload.rndAry[0]); ++u)
                t.payload.rndAry[u] = rand();

            t.checksum = crc_checksumSAEJ1850_8Bit(&t.payload, sizeof(t.payload));

            successOfPost = tsq_writeToTail(pQueue, /* pData */ &t, sizeof(cpd_telegram_t));

            //++ noAPI1;
        }
        else
        {
            /* API: Allocate room in the queue and produce the data in place. Save copying
               the local buffer. */
            cpd_telegram_t * const pT = (cpd_telegram_t*)tsq_allocTailElement(pQueue);
            if(pT != NULL)
            {
                successOfPost = true;
                pT->payload.cnt = t.payload.cnt;

                /* Fill the random part of the telegram. */
                unsigned int u;
                for(u=0; u<sizeof(pT->payload.rndAry)/sizeof(pT->payload.rndAry[0]); ++u)
                    pT->payload.rndAry[u] = rand();

                pT->checksum = crc_checksumSAEJ1850_8Bit(&pT->payload, sizeof(pT->payload));

                /* Submit the element without copying the data any more. */
                tsq_postTailElement(pQueue);

                //++ noAPI2;
            }
            else
                successOfPost = false;
        }
        //assert(noAPI1 < 10000  ||  noAPI2 < 10000);

        if(successOfPost)
        {
            /* Count sent event. */
            ++ noWrittenElements;

            /* Update sequence counter only if this telegram could be posted. */
            ++ t.payload.cnt;
        }    
        else
        {
            /* Count queuefull event. */
            ++ noEventsQueueFull;
        }
        
        ++ noCycles;
        if((noCycles % 1000) == 0)
        {
            printf( "Producer: %8lu cycles, noWrittenElements: %8lu, noEventsQueueFull: %8lu\n"
                  , noCycles
                  , noWrittenElements
                  , noEventsQueueFull
                  );
            fflush(stdout);
        }
        
        Sleep(1);
    }
    while(_kbhit() == 0);

#if 0
    /* Windows: Reset the clock tick of the task scheduler. */
    mmRes = timeEndPeriod(/* uPeriodMs */ tiOsTick);
    if(mmRes != TIMERR_NOERROR)
        printf("Resetting Windows time quantum failed");
#endif

    // release
    UnmapViewOfFile(pMemChunk);
    CloseHandle(hSharedMem);

    return 0;
    
} /* End of main */

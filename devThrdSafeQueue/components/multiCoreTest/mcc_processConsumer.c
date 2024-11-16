/**
 * @file mcc_processConsumer.c
 * Multi-Core test of thread-safe queue: This source compiles to a stand-alone Windows
 * application, that implements a data consumer. The consumer permanently checks the queue
 * object for new data.\n
 *   Note, producer process is the owner of the queue object. It is responsible for
 * creation of the object. This requires starting the producer process a safe time span
 * before the consumer process. Normally, this would require some inter-process
 * communication, e.g. by a mutex. This is not the focus of this test; we apply a simple
 * Windows script to first start the producer and then the consumer with a little delay.\n
 *   The data elements in the queue use sequence counters and CRC checksums to make data
 * damage detection possible here at the consumer side.\n
 *   The queue object is installed in named shared memory, which can be addressed to by the
 * producer code, too.\n
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
//#include "mcc_processConsumer.h"


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
 * Main entry point into the consumer application. The application runs in a loop until a
 * key hit event is signalled to the process. In every cycle the queue is checked for new
 * data elements. If any are receioved than they are read and validated. Any data damage
 * would be reported by assertion. The maximum cycling speed is limited by some Windows
 * timing operations.\n
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
    /* Start of processes: The consumer uses the object created by the producer. This is
       not safely implemented in the application code. We ensure availability of the created
       object only by a little delay here. */
    printf("Consumer: Wait 1s to let the producer process create the queue object\n");
    fflush(stdout);
    Sleep(1000);

    const unsigned int sizeOfQueue = tsq_getSizeOfQueue
                                            ( CPD_MAX_QUEUE_LENGTH
                                            , /* maxElementSize */ CPD_SIZE_OF_QUEUE_ELEMENT
                                            , _Alignof(cpd_telegram_t)
                                            )
                     , sizeOfMagic = sizeof(CPD_MAGIC_ID_OF_SHARED_MEM)
                     , sizeOfSharedMem = sizeOfQueue + sizeOfMagic;

    /* https://docs.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-createfilemappinga */
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
    
    /* Check the magic at the beginning of the shared memory to prove that we really see
       the same memory as written by the producer. */
    const unsigned long long magic = *(unsigned long long*)pMemChunk;
    const bool isMagicOk = magic == CPD_MAGIC_ID_OF_SHARED_MEM;
    printf( "MapViewOfFile = 0x%p (desired: 0x%p), magic = 0x%llX (%s)\n"
          , pMemChunk
          , pDesiredViewAddress
          , *(unsigned long long*)pMemChunk
          , isMagicOk? "ok": "invalid"
          );
    
    if(!isMagicOk ||  pMemChunk != pDesiredViewAddress)
    {
        printf("Consumer process aborts due to bad magic or undesired shared memory address\n");
        return 1;
    }

    /* Most likely the Windows shared memory function will only return memory chunks with
       the most restrictive alignment. We didn't double check for according documentation
       and simply rely on this. A run time check is anyway done by the queue
       implementation, which would immediately report an alignment problem by assertion. */
    
    /* The producer is responsible for object creation. We simply expect to find the object
       at the agreed location, which is implemented by a Windows named shared memory
       object. All we need to do is a type cast of the pointer. A real application would
       surely validate the memory contents but this is just a simple test and any problem
       would be reported by assertion or abnormal program termination. */
    tsq_queue_t *pQueue = (tsq_queue_t*)((uintptr_t)pMemChunk
                                         + sizeof(CPD_MAGIC_ID_OF_SHARED_MEM)
                                        );
    unsigned long noCycles = 0
                , sqc = (unsigned long)-1000
                , noReadElements = 0;
    do
    {
        const cpd_telegram_t *pT;
        while(true)
        {
            pT = tsq_readFromHead(pQueue);
            if(pT == NULL)
                break;

            assert(((uintptr_t)pT & (_Alignof(cpd_telegram_t)-1)) == 0);
            
            /* Count reception event. */
            ++ noReadElements;

            assert(sqc == pT->payload.cnt);
            ++ sqc;

            uint8_t checksum = crc_checksumSAEJ1850_8Bit(&pT->payload, sizeof(pT->payload));
            assert(checksum == pT->checksum);
        }

        ++ noCycles;
        if((noCycles % 50000) == 0)
        {
            printf( "Consumer: %8lu cycles, noReadElements: %11lu\n"
                  , noCycles
                  , noReadElements
                  );
            fflush(stdout);
        }
        
        Sleep(0);
    }
    while(_kbhit() == 0);

    // release
    UnmapViewOfFile(pMemChunk);
    CloseHandle(hSharedMem);

    return 0;

} /* End of main */

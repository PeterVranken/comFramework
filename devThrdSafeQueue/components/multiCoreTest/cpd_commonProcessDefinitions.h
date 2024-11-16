#ifndef CPD_COMMONPROCESSDEFINITIONS_INCLUDED
#define CPD_COMMONPROCESSDEFINITIONS_INCLUDED
/**
 * @file cpd_commonProcessDefinitions.h
 * Definition of common elements of both processes.
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

/*
 * Include files
 */

#include <stdint.h>


/*
 * Defines
 */

/** The maximum capacity of the queue objects used in these tests. */
#define CPD_MAX_QUEUE_LENGTH 10

/** The name under which the shared memory is accessible for both of the processes. */
#define CPD_NAME_OF_SHARED_MEMORY_FOR_QUEUE "Global-threadSafeQueue"

/** A magic number to prove taht both processes really see the same, shared memory
    contents. */
#define CPD_MAGIC_ID_OF_SHARED_MEM  0x12ADDFCFA29E867Eull

/*
 * Global type definitions
 */

/** This is the data structure, which is passed from producer to consumer. */
typedef struct cpd_telegram_t
{
    /** The checkum protected data contents of the telegram. */
    struct payload_t
    {
        /** An infinite counter. Incremented with each sent telegram and used to detect
            sequence errors in the test. */
        unsigned long cnt;
        
        /** Some random data. */
        int rndAry[1000];

    } payload;
    
    /** The checksum of the complete telegram. Used to detect unwanted data changed
        during telegram delivery. */
    uint8_t checksum;
    
} cpd_telegram_t;


/** The queue needs to operate well with elements, which have a size that is not a multiple
    of the alignment of the element. This situation can appear with structs with flexible
    array member. We test it by using this define with virtual increased object size
    instead of sizeof(cpd_telegram_t). */
#define CPD_SIZE_OF_QUEUE_ELEMENT (sizeof(cpd_telegram_t)+7)


/*
 * Global data declarations
 */


/*
 * Global prototypes
 */



#endif  /* CPD_COMMONPROCESSDEFINITIONS_INCLUDED */

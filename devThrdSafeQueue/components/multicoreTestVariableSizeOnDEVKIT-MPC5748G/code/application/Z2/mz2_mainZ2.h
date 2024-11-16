#ifndef MZ2_MAINZ2_INCLUDED
#define MZ2_MAINZ2_INCLUDED
/**
 * @file mz2_main_Z2.h
 * Definition of global interface of module mz2_mainZ2.c
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

/*
 * Include files
 */

#include <stdint.h>
#include <stdbool.h>

#include "typ_types.h"
#include "vsq_threadSafeQueueVariableSize.h"

/*
 * Defines
 */


/*
 * Global type definitions
 */


/*
 * Global data declarations
 */

/** Counter of cyclic 1ms user isr. */
extern volatile unsigned long mz2_cntIsr1ms;  

/** Counter of cyclic 100us user isr. */
extern volatile unsigned long mz2_cntIsr100us;  

/** Counter of cyclic 33us user isr. */
extern volatile unsigned long mz2_cntIsr33us;  

/** Counter of cyclic 1ms user task. */
extern volatile unsigned long mz2_cntTask1ms;  

/** Counter of cyclic 1ms OS task. */
extern volatile unsigned long mz2_cntTaskOs1ms;

/** Counter of cycles of infinite main loop. */
extern volatile unsigned long mz2_cntTaskIdle;

/** Stack reserve of process P3 on the second core. */
extern volatile unsigned int mz2_stackReserveP3;

/** Stack reserve of kernel process on the second core. */
extern volatile unsigned int mz2_stackReserveOS;

/** The average CPU load produced by all tasks and interrupts on core Z4B in tens of
    percent. */ 
extern volatile unsigned int mz2_cpuLoadZ2;

/** The queue object under test. It connects core Z2 with Z4B. */
extern vsq_queueHead_t *mz2_pQHead_z4BToZ2;

/** The queue object under test. It connects core Z2 with Z4A. */
extern vsq_queueTail_t *mz2_pQTail_z2ToZ4A;

/** Counter of queued elements, i.e. elements sent to core Z4A. */
extern unsigned long mz2_noQueuedElements;

/** Counter of total queued bytes, i.e. bytes sent to core Z4A. */
extern unsigned long mz2_sizeOfQueuedData;

/** The number of times, core Z2 saw an full Tx queue. */
extern unsigned long mz2_noEvTxQueueFull;

/** Counter of elements received from core Z4B. */
extern unsigned long mz2_noRxElements;

/** Counter of total received bytes, i.e. bytes got from core Z4B. */
extern unsigned long mz2_sizeOfRxData;

/** The number of times, core Z2 found an empty Rx queue. */
extern unsigned long mz2_noEvRxQueueEmpty;


/*
 * Global prototypes
 */

/** Callback for LED and button I/O driver. */
int32_t mz2_onButtonChangeCallback(uint32_t PID ATTRIB_UNUSED, uint8_t buttonState);

/** Main entry point of code execution for core Z2. */
void /* _Noreturn */ mz2_mainZ2(int noArgs, const char *argAry[]);

#endif  /* MZ2_MAINZ2_INCLUDED */

#ifndef M4B_MAINZ4B_INCLUDED
#define M4B_MAINZ4B_INCLUDED
/**
 * @file m4b_mainZ4B.h
 * Definition of global interface of module m4b_mainZ4B.c
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

/** Counter of cyclic 1ms user task. */
extern unsigned long m4b_cntTask1ms;  

/** Counter of cycles of infinite main loop. */
extern unsigned long m4b_cntTaskIdle;

/** The queue object under test. It connects core Z4B with Z4A. */
extern vsq_queueHead_t *m4b_pQHead_z4AToZ4B;

/** The queue object under test. It connects core Z4B with Z2. */
extern vsq_queueTail_t *m4b_pQTail_z4BToZ2;

/** Counter of queued elements, i.e. elements sent to core Z2. */
extern unsigned long m4b_noQueuedElements;

/** Counter of total queued bytes, i.e. bytes sent to core Z2. */
extern unsigned long m4b_sizeOfQueuedData;

/** The number of times, core Z4B saw an full Tx queue. */
extern unsigned long m4b_noEvTxQueueFull;

/** Counter of elements received from core Z4A. */
extern unsigned long m4b_noRxElements;

/** Counter of total received bytes, i.e. bytes got from core Z4A. */
extern unsigned long m4b_sizeOfRxData;

/** The number of times, core Z4B found an empty queue. */
extern unsigned long m4b_noEvRxQueueEmpty;

/** Stack reserve of process P2 on the second core. */
extern unsigned int m4b_stackReserveP2;

/** Stack reserve of kernel process on the second core. */
extern unsigned int m4b_stackReserveOS;

/** The average CPU load produced by all tasks and interrupts on core Z4B in tens of
    percent. */ 
extern unsigned int m4b_cpuLoadZ4B;


/*
 * Global prototypes
 */

/** Callback for LED and button I/O driver. */
int32_t m4b_onButtonChangeCallback(uint32_t PID ATTRIB_UNUSED, uint8_t buttonState);

/** Main entry point of code execution for core Z4B. */
void /* _Noreturn */ m4b_mainZ4B(int noArgs, const char *argAry[]);

#endif  /* M4B_MAINZ4B_INCLUDED */

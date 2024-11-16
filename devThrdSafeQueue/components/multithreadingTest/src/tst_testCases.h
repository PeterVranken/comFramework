#ifndef TST_TESTCASES_INCLUDED
#define TST_TESTCASES_INCLUDED
/**
 * @file tst_testCases.h
 * Definition of global interface of module tst_testCases.c
 *
 * Copyright (C) 2016 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
#include <stdatomic.h>


/*
 * Defines
 */


/*
 * Global type definitions
 */


/*
 * Global data declarations
 */

/** Counter to indicate how many repetitions ahve been made. */
extern _Atomic volatile unsigned long tst_noWrittenElements
                                    , tst_noEventsQueueFull
                                    , tst_noReadElements;

/*
 * Global prototypes
 */

/** Initialization of the module. */
void tst_initModule();

/** Shutdown of the test module. */
void tst_shutdownModule();

/** The task, which implements one communication peer. */
void tst_task1ms();

/** The task, which implements the counterpart communication peer. */
void tst_task10ms();

#endif  /* TST_TESTCASES_INCLUDED */

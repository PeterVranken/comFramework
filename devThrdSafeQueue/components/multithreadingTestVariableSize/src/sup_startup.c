/**
 * @file sup_startup.c
 * Simple application of RTOS emulation. Three threads are defined, which regularly invoke
 * their individual task functions at different rates.
 *
 * Copyright (C) 2017 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
 *   sup_taskSlow
 *   sup_taskMedium
 *   sup_taskFast
 *   main
 * Local functions
 */

/*
 * Include files
 */

#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdio.h>
#ifdef __WINNT__
# include <conio.h>
#endif
#include <assert.h>

#include "tst_testCases.h"
#include "rtos_rtosEmulation.h"



/*
 * Defines
 */


/*
 * Local type definitions
 */


/*
 * Local prototypes
 */

/* The prototypes of the task functions, which can't be static. The prototypes don't need
   to be made public. */
rtos_taskFctResult_t sup_taskSlow();
rtos_taskFctResult_t sup_taskMedium();
rtos_taskFctResult_t sup_taskFast();


/*
 * Data definitions
 */


/*
 * Function implementation
 */

/**
 * Task function, which is regularly executed.
 *   @return
 * \a rtos_taskFctResult_continueScheduling until the termination condition of the
 * application is reached (Windows only: Any key hit). Then it returns once \a
 * rtos_taskFctResult_continueScheduling_endOfScheduling.
 */

rtos_taskFctResult_t sup_taskSlow()
{
    //static volatile unsigned long cnt = 0;
    //printf("%s: Invocation %lu\n", __func__, cnt);
    //fflush(stdout);
    //++ cnt;
    
#if 0
    /* Simulate some load. */
    const double tiFullLoad = 2e-3
               , now = rtos_getTime();
    while(rtos_getTime() < now+tiFullLoad)
        ;
#endif

#ifdef __WINNT__
    return _kbhit()!=0? rtos_taskFctResult_endOfScheduling
                      : rtos_taskFctResult_continueScheduling;
#else           
    return rtos_taskFctResult_continueScheduling;
#endif

} /* End of sup_taskSlow */



/**
 * Task function, which is regularly executed.
 *   @return
 * rtos_taskFctResult_continueScheduling, the cyclic repetition should never end.
 */
 
rtos_taskFctResult_t sup_taskMedium()
{
    //static volatile unsigned long cnt = 0;
    //printf("%s: Invocation %lu\n", __func__, cnt);
    //fflush(stdout);
    //++ cnt;

#if 0
    /* Simulate some load. */
    const double tiFullLoad = 5e-3
               , now = rtos_getTime();
    while(rtos_getTime() < now+tiFullLoad)
        ;
#endif
    tst_task10ms();

    return rtos_taskFctResult_continueScheduling;

} /* End of sup_taskMedium */



/**
 * Task function, which is regularly executed.
 *   @return
 * rtos_taskFctResult_continueScheduling, the cyclic repetition should never end.
 */
 
rtos_taskFctResult_t sup_taskFast()
{
    //static volatile unsigned long cnt = 0;
    //printf("%s: Invocation %lu\n", __func__, cnt);
    //fflush(stdout);
    //++ cnt;
    
#if 0
    /* Simulate some load. */
    const double tiFullLoad = 2e-3
               , now = rtos_getTime();
    while(rtos_getTime() < now+tiFullLoad)
        ;
#endif
    tst_task1ms();
    
    return rtos_taskFctResult_continueScheduling;

} /* End of sup_taskFast */



/**
 * The application entry point.
 *   @return
 * Always 0, no error is reported to the shell.
 *   @param noArgs
 * The number of command line arguments.
 *   @param argAry
 * The array of command line arguments.
 */

int main(int noArgs __attribute__((unused)), char *argAry[] __attribute__((unused)))
{
    /* Prior to begin of multitasking do the application initialization. This can still be
       done race condition free. */
    rtos_initModule();
    tst_initModule();

    /* Define the user threads, which are to be started. */
    const rtos_threadSpecification_t userThreadAry[] =
        { [0] = { .name = "taskFast"
                , .fctTask = sup_taskFast
                , .tiCycleMs = 1
                }
        , [1] = { .name = "taskMedium"
                , .fctTask = sup_taskMedium
                , .tiCycleMs = 10
                }
        , [2] = { .name = "taskSlow"
                , .fctTask = sup_taskSlow
                , .tiCycleMs = 50
                }
        };

        
    /* Offer a way to terminate the basically infinitly spinning application. */
#ifdef __WINNT__
    printf("%s: Hit any key to terminate ...\n", __func__);
#else
    printf("%s: Type Ctrl-C to abort the infinite run of the application ...\n", __func__);
#endif

    /* Run the scheduler. The next call returns only when all started threads have
       terminated again. This will last until one of the task functions demands the end of
       scheduling. */
    rtos_runScheduler( /* noThreads */    sizeof(userThreadAry)/sizeof(userThreadAry[0])
                     , /* threadDefAry */ userThreadAry
                     , /* threadAry */    NULL
                     );
    
    /* After multhreading has ended we can safely shutdwon the modules without any race
       conditions. */
    tst_shutdownModule();
    
    printf("%s: exiting\n", __func__);
    return 0;

} /* End of main */


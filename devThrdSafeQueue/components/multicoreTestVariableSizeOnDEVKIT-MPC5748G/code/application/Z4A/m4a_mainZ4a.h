#ifndef M4A_MAIN_Z4A_INCLUDED
#define M4A_MAIN_Z4A_INCLUDED
/**
 * @file syc_systemConfiguration.h
 * Definition of global interface of module m4a_mainZ4A.c
 *
 * Copyright (C) 2019-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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

#include "typ_types.h"
#include "vsq_threadSafeQueueVariableSize.h"
#include "mtx_mutex.h"


/*
 * Defines
 */

/** The maximum number of random payload bytes of a telegram used in the test. */
#define M4A_MAX_NO_PAYLOAD_BYTES          23u


/*
 * Global type definitions
 */


/*
 * Global data declarations
 */

/** A synchronization object shared between the three cores. It is used to ensure that the
    initialization tasks of process P1 on the three cores are executed in a well-defined
    order. */
extern struct m4a_mutexStartup_t
{
    /** The mutex opbject to protect the state variable. */
    mtx_mutex_t mtx;
    
    /** The state variables: Has core Z4A already created the shared objects? */
    enum m4a_stInitialization_t
    {
        m4a_stInit_start,
        m4a_stInit_objectsCreated,
        m4a_stInit_objectsLinked,
        m4a_stInit_done,
        m4a_stInit_aborted,
        
    } stZ4AInitialization;
    
    /** The state variables: Has core Z4B already created the shared objects? */
    enum m4a_stInitialization_t stZ4BInitialization;
    
    /** The state variables: Has core Z2 already created the shared objects? */
    enum m4a_stInitialization_t stZ2Initialization;

} m4a_mutexStartup;

/** The queue object under test. It connects core Z4A with Z2. */
extern vsq_queueHead_t *m4a_pQHead_z2ToZ4A;

/** The queue object under test. It connects core Z4A with Z4B. */
extern vsq_queueTail_t *m4a_pQTail_z4AToZ4B;



/*
 * Global prototypes
 */

/** Start a secondary core. */
void m4a_startSecondaryCore(unsigned int idxCore, void (*main)(signed int, const char *[]));

/** Entry point into C code. */
int /* _Noreturn */ main(int noArgs ATTRIB_DBG_ONLY, const char *argAry[] ATTRIB_DBG_ONLY);

#endif  /* M4A_MAIN_Z4A_INCLUDED */

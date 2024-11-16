#ifndef PID_PROCESS_ID_INCLUDED
#define PID_PROCESS_ID_INCLUDED
/**
 * @file pid_processID.h
 * Definition of process IDs. Processes are generally shared by all cores and so we have a
 * common file with global definition of their PIDs.
 *
 * Copyright (C) 2021-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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


/*
 * Defines
 */


/*
 * Global type definitions
 */

/** The IDs of the processes in use. Processes are defined across all cores and therefore,
    we place the definition here in the global space.\n
      Note, the chosen number is the priviledge level at the same time; the higher the
    number the higher the privileges. The permitted range is 1 ... #RTOS_NO_PROCESSES. */
enum
{
    pid_pidOs = 0,              /// Kernel always and implicitly has PID 0
    pid_pidTestQueueZ4A = 1,    /// Main process on core Z4A, used for testing of queue
    pid_pidTestQueueZ4B = 2,    /// Main process on core Z4B, used for testing of queue
    pid_pidTestQueueZ2 = 3,     /// Main process on core Z2, used for testing of queue

    tmpNoPrc,
    pid_noProcessesInUse = tmpNoPrc-1
};
 
 

/*
 * Global data declarations
 */


/*
 * Global prototypes
 */

#endif  /* PID_PROCESS_ID_INCLUDED */

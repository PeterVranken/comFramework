#ifndef AEV_APPLEVENTS_INCLUDED
#define AEV_APPLEVENTS_INCLUDED
/**
 * @file tc12/aev_applEvents.h
 * Definition of application events. The application events are managed in a
 * central file to avoid inconsistencies and accidental double usage.
 *
 * Copyright (C) 2013-2016 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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

/** This event is sent by the high priority producer task to notify the consumer in the
    relais task that the connecting queue object is created and ready for use. */
#define EVT_QUEUE_H2L_READY         (RTOS_EVT_EVENT_00)

/** This event is sent by the high priority consumer task to notify the producer in the
    relais task that the connecting queue object is created and ready for use. */
#define EVT_QUEUE_L2H_READY         (RTOS_EVT_EVENT_01)

/** This event is sent by the medium priority producer task to notify the consumer of same
    priority that the connecting queue object is created and ready for use. */
#define EVT_QUEUE_M2M_READY         (RTOS_EVT_EVENT_02)


/*
 * Global type definitions
 */


/*
 * Global data declarations
 */


/*
 * Global prototypes
 */



#endif  /* AEV_APPLEVENTS_INCLUDED */

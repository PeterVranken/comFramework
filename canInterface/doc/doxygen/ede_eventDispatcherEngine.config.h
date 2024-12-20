#ifndef EDE_EVENTDISPATCHERENGINE_CONFIG_INCLUDED
#define EDE_EVENTDISPATCHERENGINE_CONFIG_INCLUDED
/**
 * @file ede_eventDispatcherEngine.config.h
 * The integration environment dependent, static configuration of the CAN interface.
 *
 * Copyright (C) 2015-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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

#include <assert.h>


/*
 * Configuration.
 *   The defines and typedefs found in this section are the configuration of the CAN
 * interface for the integration into a specific platform.
 */

/*
 * The list of required include files as far as they are configuration: In most embedded
 * platforms the basic types are addressed to under names like uint8, sint16, int16_t, etc.
 *   The implementation of the CAN interface tries to avoid these types in order to benefit
 * from the C type concept, which leaves it open to the platform which widths are suitable
 * for the basic types. Particularly on small 8 or 16 systems this will save a significant
 * amount of expensive RAM.\n
 *   However, at the interface to the platform environment the use of such types is likely.
 * If a type definition is needed then you would place an according include statement
 * here.\n
 *   Moreover, down here there are references to the types of some operating system
 * elements like handles and indexes. To resolve these references it's likely that you need
 * to include some related headers. The include statements should be placed here.
 */
//#include "os_types.h"


/** Many error conditions, which are static in the sense that they can only appear due to
    real errors in the implementation code (as opposed to errors caused by run-time data)
    are checked by assertions. This relates to the implementation of the dispatcher engine
    itself, but - much more important - to the implementation of the integration code,
    mainly the event callbacks, too. Here, the most typical errors will be caught the first
    time the code is executed. This concept of static error checks makes it inevitable to
    have an assertion mechanism. Most platforms will offer an assertion.
    #EDE_ASSERT(boolean_t) needs to expand to the assertion on your platform. */
#define EDE_ASSERT(booleanInvariant)    assert(booleanInvariant)

/** The data type of the kind of processed external events. The meaning of the different
    kinds is transparent to the implementation of the dispatcher engine; it'll just deliver
    the events together with the sender provided kind. The only exception is the slightly
    limited range of the chosen integral type: The implementation of the dispatcher
    reserves a few values from the implementation range for its own purposes. These are the
    values between (#EDE_EV_KIND_LAST+1) and ((ede_kindOfEvent_t)-2), both including.\n
      Any basic integer type can be used, signed or unsigned. This will normally include
    the C enumeration types, too. */
typedef unsigned int ede_kindOfEvent_t;

/** The data type of an event handle. The handle is needed to distinguish all events in the
    context of a dispatcher system. Use case CAN: The CAN API of the operating system
    mostly uses some kind of handle for messages, e.g. the index of a MTO (message transfer
    object), and this handle is used by the OS' notification callback (e.g. ISR) in order
    to identify the notified message.
      @todo Use the typedef to make our internally used \a ede_senderHandleEvent_t
    identical to the operating system's given handle or identifier type. */
typedef unsigned int ede_senderHandleEvent_t;


/** Several interface specifications, e.g. for memory allocation and event queue, depend on
    the correct alignment of data objects. (As these low level functions typically operate
    on void* but don't have compile-time decided datatypes). Therefore, they need to know
    the worst case alignment requirement and assume that for all their operations.
      @todo Specify the alignment, which suits to all basic machine words, e.g. 4 for most
    32 Bit architectures. The define needs to expand to a integer literal, which designates
    a power of two, mostly one out of 1, 2, 4 or 8. */
#define EDE_COMMON_MACHINE_ALIGNMENT 4

/** Some code builds on atomic read and atomic write of an integer word. An example would
    be an error counter, which is incremented in the context, where the error potentially
    occurs but which can be read and evaluated from all other contexts. This typedef
    specifies the largest machine word on the given platform, which is still guaranteed to
    be atomic for both, read and write operations. (This must not be mixed up with a C11
    atomic type, which even atomic read-modify-write operations are defined for.) On most
    platforms, the largest atomic type is at the same type the native machine word, i.e.
    the word, with the same size as the internal data buses and registers. However, this
    depends, on a 32 Bit Infineon AURIX for example a uint32_t is not generally atomic and
    a uint16_t would be the right setting.
      @note The chosen word limits the functionality building on it, e.g. the maximum range
    of countable errors or the maximum number of queueable events. */
typedef unsigned int ede_atomicUnsignedInt_t;


/** Any event source is owned by one particular dispatcher, the very one, which had
    registered it at the system. (See ede_registerExternalEventSource() for details). Only
    this dispatcher will be able to properly decode the event data. The integration code
    can easily contain programming errors, which make a sender post its event to the wrong
    dispatcher, which would lead to servere run-time errors. The implementation of the
    dispatcher can do a run-time check if a sender posts its events always to the right
    dispatcher, but on cost of a significant portion of additional RAM. (Each registered
    source now has an ID of the registering dispatcher, which can sum up to a kByte or even
    more for real systems with many CAN messages.)
      Faults, which lead to wrong event delivery will nearly always be static programming
    errors, i.e., they won't depend on run-time input data of the system. In all of these
    cases, it'll be appropriate to do the check only in DEBUG compilation and to save the
    high expense of RAM in the product compilation. */
#ifdef DEBUG
# define EDE_CHECK_FOR_EVENT_DELIVERY_TO_ASSOCIATED_DISPATCHER 1u
#else
# define EDE_CHECK_FOR_EVENT_DELIVERY_TO_ASSOCIATED_DISPATCHER 0u
#endif

/** Tailoring of the API: The timer context data is not always required and can be switched
    off at compile time. This will safe \a sizeof(uintptr_t) Byte per created timer. An
    alternative to applying user provided data can be quering the timer handle to identify
    it, please refer to ede_getHandleTimer().\n
      The define is set to either 1 or 0. */
#define EDE_ENABLE_TIMER_CONTEXT_DATA               1

#endif  /* EDE_EVENTDISPATCHERENGINE_CONFIG_INCLUDED */

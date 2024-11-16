/**
 * @file dpy_display.cpp
 *   A class around the LCD display object grants properly synchronized access to the LCD
 * from different RTuinOS tasks. The display becomes a shared resource.\n
 *   The class only offers some application specific, formatted print functions. No other
 * information than anticpated by these functions can be written to the display. With other
 * words, the entire layout design of the application output is controlled by this module.
 *
 * Copyright (C) 2013-2015 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
 *   dpy_display_t::dpy_display_t
 *   dpy_display_t::printBackground
 *   dpy_display_t::printValueAsBar
 *   dpy_display_t::printNoCycles
 *   dpy_display_t::printNoErrors
 *   dpy_display_t::printTime
 *   dpy_display_t::printCpuLoad
 *   dpy_printGreeting
 *   dpy_printBackground
 *   dpy_printValueAsBar
 *   dpy_printNoCycles
 *   dpy_printNoErrors
 *   dpy_printCpuLoad
 * Local functions
 *   dpy_display_t::acquireMutex
 *   dpy_display_t::releaseMutex
 */

/*
 * Include files
 */

#include <Arduino.h>
#include <LiquidCrystal.h>

#include "rtos.h"
#include "rtos_assert.h"

#if DPY_MULTI_TASKING_SUPPORT
# include "aev_applEvents.h"
#endif
#include "dpy_display.h"


/*
 * Defines
 */


/*
 * Local type definitions
 */

/** Some special characters from the build-in character table of the display get a
    meaingful name. See http://www.sparkfun.com/datasheets/LCD/HD44780.pdf. */
enum enumSpecialChars_t { cBar = 0xff };


/*
 * Local prototypes
 */


/*
 * Data definitions
 */

/** The one and only display object. */
dpy_display_t dpy_display;


/*
 * Function implementation
 */


/**
 * Class construtor. Only a single instance of the class must exist in an application,
 * therefore this constructor must never be used.
 *   @remark
 *   The configuration of the underlaying class LiquidCrystal, particularly the supported
 * pins, is done hardcoded in the implementation of this constructor.
 */

dpy_display_t::dpy_display_t()
    : LiquidCrystal(8, 9, 4, 5, 6, 7)
{
    /* Initialize LCD shield. */
    begin(16, 2);
    noBlink();
    noCursor();
    
} /* End of dpy_display_t::dpy_display_t */



/**
 * Print a greeting message. This function should be called immediately after reset.\n
 *   The function must not be called at run time, when concurrent tasks try to access the
 * display. This function will not acquire the mutex for safe access of the display.
 */

void dpy_display_t::printGreeting()
{
    /* Define the greeting. The first line is printed left aligned. The second one will
       move once from the left to the right. Both lines must not contain more than 16
       characters. Note, the effect of the moving text fragment is lost if the second line
       is longer than a few characters only. */
#define TXT_FIRST_LINE  "ThreadSafeQueue"
#define TXT_SECOND_LINE "***"

    ASSERT((int)sizeof(TXT_FIRST_LINE)-1 <= 16  &&  (int)sizeof(TXT_SECOND_LINE)-1 <= 16);

    char lcdLine[16+1];
#ifdef DEBUG
    int noChars =
#endif
    sprintf(lcdLine, "%-16s", TXT_FIRST_LINE);
    ASSERT(noChars < (int)sizeof(lcdLine));
    setCursor(/* col */ 0, /* row */ 0);
    print(lcdLine);

    lcdLine[sizeof(lcdLine)-1] = '\0';
    uint8_t u;
    for(u=0; u<16+2-sizeof(TXT_SECOND_LINE); ++u)
    {
        memset(lcdLine, ' ', sizeof(lcdLine)-1);
        memcpy(&lcdLine[u], TXT_SECOND_LINE, sizeof(TXT_SECOND_LINE)-1);
        setCursor(/* col */ 0, /* row */ 1);
        print(lcdLine);

        /* rtos_delay() must not be used here: A prerequiste of this function is that the
           RTOS kernel is not yet running. */
        delay(333 /* ms */);
    }
    memset(lcdLine, ' ', sizeof(lcdLine)-1);
    setCursor(/* col */ 0, /* row */ 1);
    print(lcdLine);

#undef TXT_FIRST_LINE
#undef TXT_SECOND_LINE
} /* End of dpy_display_t::printGreeting */




/**
 * Print the invariant parts of the display layout. This function must be called once at
 * the beginning, prior to the call of any of the other formatted print commands.\n
 *   The function must not be called at run time, when concurrent tasks try to access the
 * display. This function will not acquire the mutex for safe access of the display.
 */

void dpy_display_t::printBackground()
{
    char lcdLine[16+1];
#ifdef DEBUG
    int noChars =
#endif
    sprintf(lcdLine, "Cyc:           0");
    ASSERT(noChars < (int)sizeof(lcdLine));
    setCursor(/* col */ 0, /* row */ 0);
    print(lcdLine);

    sprintf(lcdLine, "Err:    0 C:  0%%");
    ASSERT(noChars < (int)sizeof(lcdLine));
    setCursor(/* col */ 0, /* row */ 1);
    print(lcdLine);

} /* End of dpy_display_t::printBackground */



#if 0
/**
 * Display a value as bar. The function implements a pseudo-analog display including a
 * peak hold. A convenient animation is achieved only if this function is called regularly
 * every few hundred milli seconds.\n
 *   The function can be called only at run time from an \b RTuinOS task other than the
 * idle task. (It acquires the mutex for safe access of the display, which is forbidden for
 * the idle task.)
 *   @param value
 * The (positive) value to print. The value is displayed as a bar of variable length. Null
 * means 1 bar element and a value of \a fullScale means all bar elements.
 *   @param fullScale
 * Full scale value.
 */

void dpy_display_t::printValueAsBar(uint8_t value, uint8_t fullScale)
{
#define TI_PEAK_HOLD 40 /* Unit is clock tick of invocation of this method. */

    /* 30/2 = 15, for 15 variable bar elements. 15/1 is not used as we can otherwise add
       fullScale to the numerator to get symmetric round off. */
    uint8_t noElems = 1 + (30u*value+fullScale)/(2u*fullScale);
    if(noElems > 16)
        noElems = 16;
        
    static uint8_t cntPeakHold_ = 0
                 , peakValue_ = 0;
    if(cntPeakHold_ == 0  ||  noElems >= peakValue_)
    {
        peakValue_ = noElems;
        cntPeakHold_ = TI_PEAK_HOLD;
    }
    else
        -- cntPeakHold_;
        
    /* Get access to the display, or wait until anybody else has finished respectively. A
       timeout has been defined which should never elapse, but who knows. In case it
       should, we simply deny printing. */
    if(acquireMutex())
    {
        setCursor(/* col */ 0, /* row */ 0);
        uint8_t u=0;
        while(u < noElems)
        {
            write(cBar);
            ++ u;
        }
        if(noElems < peakValue_)
        {
            ++ u;
            while(u < peakValue_)
            {
                write(' ');
                ++ u;
            }
            write(cBar);
        }
        while(u < 16)
        {
            write(' ');
            ++ u;
        }

        /* And release the mutex as soon as possible after writing to the underlaying class has
           been done. */
        releaseMutex();
    }
#undef TI_PEAK_HOLD
} /* End of dpy_display_t::printValueAsBar */
#endif



/**
 * Print the current number of test cycles.\n
 *   The function can be called only at run time from an \b RTuinOS task other than the
 * idle task. (It acquires the mutex for safe access of the display, which is forbidden for
 * the idle task.)
 *   @param noCycles
 * The current number of test cycles.
 */

void dpy_display_t::printNoCycles(uint32_t noCycles)
{
    char lcdString[10+1];
#ifdef DEBUG
    int noChars =
#endif
    sprintf(lcdString, "%10lu", noCycles);
    ASSERT(noChars < (int)sizeof(lcdString));

    /* Get access to the display, or wait until anybody else has finished respectively. A
       timeout has been defined which should never elapse, but who knows. In case it
       should, we simply deny printing. */
    if(acquireMutex())
    {
        setCursor(/* col */ 6, /* row */ 0);
        print(lcdString);
        
        /* And release the mutex as soon as possible after writing to the underlaying class has
           been done. */
        releaseMutex();
    }
} /* End of dpy_display_t::printNoCycles */




/**
 * Print the current number of seen errors.\n
 *   The function can be called only at run time from an \b RTuinOS task other than the
 * idle task. (It acquires the mutex for safe access of the display, which is forbidden for
 * the idle task.)
 *   @param noErrors
 * The current number of test errors. The number may be signed in order to anticipate the
 * expected, injected errors but the negativ value is limited to -9999, otherwise an
 * assertion fires.
 */

void dpy_display_t::printNoErrors(int16_t noErrors)
{
    char lcdString[5+1];
#ifdef DEBUG
    int noChars =
#endif
    sprintf(lcdString, "%5i", noErrors);
    ASSERT(noChars < (int)sizeof(lcdString));

    /* Get access to the display, or wait until anybody else has finished respectively. A
       timeout has been defined which should never elapse, but who knows. In case it
       should, we simply deny printing. */
    if(acquireMutex())
    {
        setCursor(/* col */ 4, /* row */ 1);
        print(lcdString);
        
        /* And release the mutex as soon as possible after writing to the underlaying class has
           been done. */
        releaseMutex();
    }
} /* End of dpy_display_t::printNoErrors */




#if 0
/**
 * Print the current time.\n
 *   The function can be called only at run time from an \b RTuinOS task other than the
 * idle task. (It acquires the mutex for safe access of the display, which is forbidden for
 * the idle task.)
 *   @param hour
 * The hours of the current time, range 0..23. Exceeding the range will lead to a runtime
 * error!
 *   @param min
 * The minutes of the current time, range 0..59. Exceeding the range will lead to a runtime
 * error!
 *   @param sec
 * The seconds of the current time, range 0..59. Exceeding the range will lead to a runtime
 * error!
 */

void dpy_display_t::printTime(uint8_t hour, uint8_t min, uint8_t sec)
{
    char lcdString[8+1];
#ifdef DEBUG
    int noChars =
#endif
    sprintf(lcdString, "%02u:%02u:%02u", hour, min, sec);
    ASSERT(noChars < (int)sizeof(lcdString));

    /* Get access to the display, or wait until anybody else has finished respectively. A
       timeout has been defined which should never elapse, but who knows. In case it
       should, we simply deny printing. */
    if(acquireMutex())
    {
        /* "16-sizeof" means to display right aligned. */
        setCursor(/* col */ 16-(sizeof(lcdString)-1), /* row */ 0);
        print(lcdString);

        /* And release the mutex as soon as possible after writing to the underlaying class has
           been done. */
        releaseMutex();
    }
} /* End of dpy_display_t::printTime */
#endif



#if 1
/**
 * Print the current CPU load.\n
 *   The function can be called only at run time from an \b RTuinOS task other than the
 * idle task. (It acquires the mutex for safe access of the display, which is forbidden for
 * the idle task.)
 *   @param cpuLoad
 * The value to print. Scaling is 0.5%, a passed value of 200 will result in a displayed
 * value of 100%. The range is [0..200].
 */

void dpy_display_t::printCpuLoad(uint8_t cpuLoad)
{
    char lcdString[3+1];
#ifdef DEBUG
    int noChars =
#endif
    sprintf(lcdString, "%3u", (cpuLoad+1)/2);
    ASSERT(noChars < (int)sizeof(lcdString));

    /* Get access to the display, or wait until anybody else has finished respectively. A
       timeout has been defined which should never elapse, but who knows. In case it
       should, we simply deny printing. */
    if(acquireMutex())
    {
        setCursor(/* col */ 12, /* row */ 1);
        print(lcdString);

        /* And release the mutex as soon as possible after writing to the underlaying class
           has been done. */
        releaseMutex();
    }
} /* End of dpy_display_t::printCpuLoad */
#endif




/**
 * At runtime, when the \b RTuinOS tasks compete for the display, strict synchronization is
 * required. All requests to write to the display are serialized by an \b RTuinOS mutex.
 * This private method is called by all the public print methods immediately before the
 * first access to the underlaying class LiquidCrystal.\n
 *   This method blocks until the mutex is available or the wait timeout elapses.
 *   @return
 * The function uses a timeout when waiting for the mutex. If the mutex was got in the
 * defined time span the function returns true and the calling task owns the display. If it
 * returns false, the calling task must not access the display.\n
 *   @remark
 * If and only if this function returns true the mutex must be released again as soon as
 * possible.
 *   @see void dpy_display_t::releaseMutex(void)
 */

inline boolean dpy_display_t::acquireMutex()
{
#if DPY_MULTI_TASKING_SUPPORT
    uint16_t gotEvtVec = rtos_waitForEvent( EVT_MUTEX_LCD | RTOS_EVT_DELAY_TIMER
                                          , /* all */ false
                                          , 1 /* unit is 2 ms */
                                          );

    /* Normally, no task will block the display longer than 2ms. This can however happen
       due to interruptions by tasks, which then own the CPU for a long while. The client
       code can nonetheless be implemented safe; in case it can simply skip display
       operation. */
    return (gotEvtVec & EVT_MUTEX_LCD) != 0;
#else
    return true;
#endif
} /* End of dpy_display_t::acquireMutex */



/**
 * Release the display; return the mutex to the \b RTuinOS system that had been acquired
 * shortly before by a successful call of boolean dpy_display_t::acquireMutex(void).\n
 *   @see boolean dpy_display_t::acquireMutex(void)
 */

inline void dpy_display_t::releaseMutex()
{
#if DPY_MULTI_TASKING_SUPPORT

    /* Release the mutex. */
    rtos_sendEvent(EVT_MUTEX_LCD);

#endif
} /* End of dpy_display_t::releaseMutex */



/**
 * C API to call method printGreeting of the one and only (global) display object \a
 * dpy_display.\n
 *   Print a greeting after reset.
 *   @see dpy_display_t::printGreeting()
 *   @remark C++ clients should not use this API but access the global object \a
 * dpy_display instead.
 */
DPY_C_API void dpy_printGreeting()
{
    dpy_display.printGreeting();
}



/**
 * C API to call method printBackground of the one and only (global) display object \a
 * dpy_display.\n
 *   To be called once first and typically only once at all: The invariant background
 * parts of the layout are printed. This print may overwrite any of the others.
 *   @see dpy_display_t::printBackground()
 *   @remark C++ clients should not use this API but access the global object \a
 * dpy_display instead.
 */
DPY_C_API void dpy_printBackground()
{
    dpy_display.printBackground();
}


#if 0
/**
 * C API to call method \a printValueAsBar of the one and only (global) display object \a
 * dpy_display.\n
 *   Pseudo-analog display of a numeric value as bar of variable length.
 *   @see dpy_display_t::printValueAsBar(uint8_t, uint8_t)
 *   @remark C++ clients should not use this API but access the global object \a
 * dpy_display instead.
 */
DPY_C_API void dpy_printValueAsBar(uint8_t value, uint8_t fullScale)
{
    dpy_display.printValueAsBar(value, fullScale);
}
#endif


/**
 * C API to call method \a printNoCycles of the one and only (global) display object \a
 * dpy_display.\n
 *   Print the number of test cycles done so far.
 *   @param noCycles The number to display.
 *   @see dpy_display_t::printNoCycles(uint16_t)
 *   @remark C++ clients should not use this API but access the global object \a
 * dpy_display instead.
 */
DPY_C_API void dpy_printNoCycles(uint32_t noCycles)
{
    dpy_display.printNoCycles(noCycles);
}



/**
 * C API to call method \a printNoErrors of the one and only (global) display object \a
 * dpy_display.\n
 *   Print the number of test errors seen so far.
 *   @param noErrors The number to display.
 *   @see dpy_display_t::printNoErrors(int16_t)
 *   @remark C++ clients should not use this API but access the global object \a
 * dpy_display instead.
 */
DPY_C_API void dpy_printNoErrors(int16_t noErrors)
{
    dpy_display.printNoErrors(noErrors);
}



/**
 * C API to call method \a printCpuLoad of the one and only (global) display object \a
 * dpy_display.\n
 *   Print the current CPU load in percent.
 *   @param cpuLoad The current CPU load in units of 0.5%.
 *   @see dpy_display_t::printCpuLoad(uint8_t)
 *   @remark C++ clients should not use this API but access the global object \a
 * dpy_display instead.
 */
DPY_C_API void dpy_printCpuLoad(uint8_t cpuLoad)
{
    dpy_display.printCpuLoad(cpuLoad);
}



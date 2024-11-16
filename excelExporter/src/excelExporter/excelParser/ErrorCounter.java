/**
 * @file ErrorCounter.java
 * A simpler error counter helper class. Actually, it is the replacement for a global
 * variable, which can be incremented by different participants of the parsing process.
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
/* Interface of class ErrorCounter
 *   ErrorCounter
 *   setNoWarnings
 *   getNoWarnings
 *   setNoErrors
 *   getNoErrors
 */

package excelExporter.excelParser;

import java.util.*;


/**
 * Count errors and warnings.
 *   Errors and warnings can be counted, the counter values can be queried and reset.
 */

public class ErrorCounter
{
    /** Counter of reported errors. */
    private int noErrors_ = 0;
    
    /** Counter of reported warnings. */
    private int noWarnings_ = 0;
    
    /**
     * Set the value of ErrorCounter.noErrors_.
     *   @param newValue
     * The new value of noErrors_.
     *   @see #getNoErrors
     */
    public void setNoErrors(int newValue)
        { noErrors_ = newValue; }


    /**
     * Get the current value of ErrorCounter.noErrors_.
     *   @return
     * The member's value is returned.
     *   @see #setNoErrors
     */
    public int getNoErrors()
        { return noErrors_; }


    /**
     * Set the value of ErrorCounter.noWarnings_.
     *   @param newValue
     * The new value of noWarnings_.
     *   @see #getNoWarnings
     */
    public void setNoWarnings(int newValue)
        { noWarnings_ = newValue; }


    /**
     * Get the current value of ErrorCounter.noWarnings_.
     *   @return
     * The member's value is returned.
     *   @see #setNoWarnings
     */
    public int getNoWarnings()
        { return noWarnings_; }


    
    /**
     * Increment the current value of ErrorCounter.noErrors_ by one.
     *   @return
     * The member's new value is returned.
     *   @see #error
     */
    public int error()
        { return ++noErrors_; }


    
    /**
     * Increment the current value of ErrorCounter.noWarnings_ by one.
     *   @return
     * The member's new value is returned.
     *   @see #error
     */
    public int warning()
        { return ++noWarnings_; }


    
    /**
     * Reset the number of errors and warnings.
     */
    public void reset()
    {
        noErrors_ = 0;
        noWarnings_ = 0;
    }
    
    
    /**
     * Add the number of issues collected in another counter object to those of this
     * counter.
     *   @param theOtherCounter The other object, whose issues are added to this.
     */
    public void add(ErrorCounter theOtherCounter)
    {
        noErrors_ += theOtherCounter.getNoErrors();
        noWarnings_ += theOtherCounter.getNoWarnings();

    } /* End of add */
    
} /* End of class ErrorCounter definition. */






/**
 * @file Frame.java
 * Part of the output data model for information rendering by StringTemplate: An exchanged
 * frame.
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
/* Interface of class Frame
 *   Frame
 */

package codeGenerator.dataModelListener;

import java.util.*;
import codeGenerator.dbcParser.*;
import org.antlr.v4.runtime.*;


/**
 * The data structure describing a communication frame on the bus.
 *   Objects of this class are part of the nested data structure passed to the template
 * engine for rendering the information in the wanted format.<p>
 *   The basic design principle is the use of public members because such members can be
 * directly accessed from the output templates. Boolean decisions are either expressed as
 * explicit public Boolean members or by references to nested member objects, which are
 * null if an optional member is not used or applicable: StringTemplate can query public
 * Boolean members or check for null references.
 */

public class Frame extends NetObject implements Comparable<Frame>
{
    /** The selectable sort orders. */
    protected enum SortOrder
    { 
        /** Sort a collection of frames by name, ascending. */
        byName,

        /** Sort a collection of frames by name, descending. */
        byNameInverse,

        /** Sort a collection of frames by CAN ID, ascending. */
        byId,

        /** Sort a collection of frames by CAN ID, descending. */
        byIdInverse,

        /** Sort a collection of frames standard before extended CAN ID. */
        byIsExtId,

        /** Sort a collection of frames extended before standard CAN ID. */
        byIsExtIdInverse,

        /** Sort a collection of frames by number of payload bytes. */
        bySize,

        /** Sort a collection of frames by number of payload bytes. */
        bySizeInverse,

        /** Sort a collection of frames Tx before Rx. */
        sentFirst,

        /** Sort a collection of frames Rx before Tx. */
        receivedFirst
    }
    
    /** The currently selected sort order. */
    static private SortOrder sortOrder = SortOrder.byName;

    /** The ID of the frame. */
    public int id = -1;
    
    /** The ID could be an extended one, which has a 29 Bit encoding on the bus. */
    public boolean isExtId = false;

    /** The node, which sends this PDU. */
    public String sender = null;
    
    /** Is the frame sent by the given node. */
    public boolean isSent = false;
    
    /** Is at least one signal received by the given node. */
    public boolean isReceived = false;
    
    /** The length in Byte of the frame. */
    public int size = 0;
    
    /** Data Length Code (DLC): The length of the frame encoded in a 4 Bit value as used in
        the CAN (FD) specification.<p>
          The DLC can encode only the 16 frame lengths, which are possible in CAN FD. Other
        applications of CAN, particularly the J1939 transport protocol, allow different
        lengths. If a frame size doesn't match any of the 16 CAN FD sizes then this field
        is null. A template could use a construct like:<p>
          {@code <if(frame.DLC)>CAN message with DLC <frame.DLC> and<else>J1939 PGN<endif>
        with <frame.size> Byte of payload} */
    public Integer DLC = null;

    /** A frames consists of PDUs. For CAN communication a one by one relation is modeled
        for frames and PDUs, but to keep this data structure open for LIN and FlexRay we
        have an implementation that permits any number of PDUs in a frame. */
    public Pdu[] pduAry = null;

    /** Default constructor. */
    public Frame()
    {}
    
    /** The number of PDUs. For CAN communication always one. From a StringTemplate V4
        template this member is accessed as {@code <frame.noPdus>}.
          @return Get the number of PDUs. */
    public int getNoPdus()
        { return pduAry != null? pduAry.length: 0; }


    /** The comparison method, which yields the currently selected sort-order.
          @return {@code <0, 0, >0} depending on relation of this to the other object.
          @param otherFrame The other object. */
    @Override public int compareTo(Frame otherFrame)
    {
        switch(sortOrder)
        {
        case byName:
            return this.name.compareToIgnoreCase(otherFrame.name);
        
        case byNameInverse:
            return otherFrame.name.compareToIgnoreCase(this.name);

        case byId:
            {
                final int deltaId = this.id - otherFrame.id;
                if(deltaId != 0)
                    return deltaId;
                else
                {
                    /* If they have same ID then an extended ID is considered the larger
                       one. */
                    if(this.isExtId && !otherFrame.isExtId)
                        return 1;
                    else if(!this.isExtId && otherFrame.isExtId)
                        return -1;
                    else
                        return 0;
                }
            }   
        case byIdInverse:
            {
                final int deltaId = otherFrame.id - this.id;
                if(deltaId != 0)
                    return deltaId;
                else
                {
                    /* If they have same ID then an extended ID is considered the larger one
                       and it comes first. */
                    if(!this.isExtId && otherFrame.isExtId)
                        return 1;
                    else if(this.isExtId && !otherFrame.isExtId)
                        return -1;
                    else
                        return 0;
                }
            }
        case byIsExtId:
            if(this.isExtId && !otherFrame.isExtId)
                return 1;
            else if(!this.isExtId && otherFrame.isExtId)
                return -1;
            else
                return 0;
        
        case byIsExtIdInverse:
            if(!this.isExtId && otherFrame.isExtId)
                return 1;
            else if(this.isExtId && !otherFrame.isExtId)
                return -1;
            else
                return 0;
        
        case bySize:
            return this.size - otherFrame.size;

        case bySizeInverse:
            return otherFrame.size - this.size;

        case sentFirst:
            if(this.isSent && !otherFrame.isSent)
                return 1;
            if(otherFrame.isSent && !this.isSent)
                return -1;
            if(this.isReceived && (!otherFrame.isSent && !otherFrame.isReceived))
                return 1;
            if(otherFrame.isReceived && (!this.isSent && !this.isReceived))
                return -1;
            return 0;

        case receivedFirst:
            if(this.isReceived && !otherFrame.isReceived)
                return 1;
            if(otherFrame.isReceived && !this.isReceived)
                return -1;
            if(this.isSent && (!otherFrame.isReceived && !otherFrame.isSent))
                return 1;
            if(otherFrame.isSent && (!this.isReceived && !this.isSent))
                return -1;
            return 0;
        
        default:
            assert false: "Bad sort order selected";
            return 0;
        }
    } /* End of compareTo */


    /** Select the wanted order. This method should be used prior to running a sort method
        on a collection of Frame objects. The setting made with this method holds for
        the whole class of Frame objects.
          @param newSortOrder The selected new sort order, applied to future collection
        sorts. */
    static protected void setSortOrder(SortOrder newSortOrder)
        { sortOrder = newSortOrder; }
    
} /* End of class Frame definition. */





/**
 * @file FrameRef.java
 * Part of the output data model for information rendering by StringTemplate: The reference
 * to an object of class Frame.
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
/* Interface of class FrameRef
 *   FrameRef
 */

package codeGenerator.dataModelListener;

import java.util.*;
import codeGenerator.dbcParser.*;
import org.antlr.v4.runtime.*;


/**
 * The data structure holding the reference to the decription of a communication frame on
 * the bus. At the same time the frame is associated with a CAN bus object. Collections of
 * FrameRef objects are used to provide one and the same set of Frame objects in different,
 * particular orders.<p>
 *   Objects of this class are part of the nested data structure passed to the template
 * engine for rendering the information in the wanted format.<p>
 *   The basic design principle is the use of public members because such members can be
 * directly accessed from the output templates. Boolean decisions are either expressed as
 * explicit public Boolean members or by references to nested member objects, which are
 * null if an optional member is not used or applicable: StringTemplate can query public
 * Boolean members or check for null references.
 */

public class FrameRef implements Comparable<FrameRef>
{
    /** The selectable sort orders. */
    protected enum SortOrder
    {
        /** Sort a collection of frame references by name, ascending. */
        sortOrderByName,
        
        /** Sort a collection of frame references in file order. */
        sortOrderNatural,
        
        /** Sort a collection of frame references by CAN ID, ascending. */
        sortOrderById,
        
        /** Sort a collection of frame standard before extended CAN ID. */
        sortOrderByIsExtId,
        
        /** Sort a collection of frame references by payload size, ascending. */
        sortOrderSize      
    }

    /** The currently selected sort order. */
    static private SortOrder sortOrder = SortOrder.sortOrderNatural;

    /** The reference to the Frame object. */
    public Frame frame = null;

    /** The reference to the Bus object. The frame is transmitted on this bus.<p>
          This reference is needed as Frame objects are owned by the Cluster object not by
        a Bus object. When iterating through the data structure in the StringTemplate V4
        template then a Bus object won't be accessible as usual by scoping. */
    public Bus bus = null;

    /** A map with a single key, value pair serves as filter to query whether the frame
        belongs to a specific bus. During iteration along the collection of FrameRef
        objects one can select only those frames, which belong to the bus of interest. The
        map's only key is the name of the bus the frame is transmitted on, the value is a
        Boolean true. All other keys (i.e. not matching bus names) will yield a Boolean
        false due to the behavior of Java Map objects in StringTemplate V4. A filter
        expression in a template can look like ("CAN_RT" would be a bus name):<p>
          {@code <if(frameRef.busIs.CAN_RT)><processFrame(frameRef.frame)><endif>} */
    public Map<String,Boolean> busIs = null;

    /** Create a new frame reference.
          @param busObj The bus the frame is transmitted on.
          @param frameObj The frame. */
    FrameRef(Bus busObj, Frame frameObj)
    {
        bus = busObj;
        frame = frameObj;
        busIs = new HashMap<String,Boolean>();
        busIs.put(bus.name, Boolean.valueOf(true));
    }

    /** The comparison method, which yields the currently selected sort-order.
          @return -1, 0, 1 depending on relation of this to the other object.
          @param otherFrameRef The other object. */
    @Override public int compareTo(FrameRef otherFrameRef)
    {
        switch(sortOrder)
        {
        case sortOrderByName:
            return this.frame.name.compareToIgnoreCase(otherFrameRef.frame.name);

        case sortOrderNatural:
            assert(this.frame.objId - otherFrameRef.frame.objId != 0);
            return this.frame.objId - otherFrameRef.frame.objId;

        case sortOrderById:
            {
                final int deltaId = this.frame.id - otherFrameRef.frame.id;
                if(deltaId != 0)
                    return deltaId;
                else
                {
                    /* If they have same ID then an extended ID is considered the larger
                       one. */
                    if(this.frame.isExtId && !otherFrameRef.frame.isExtId)
                        return 1;
                    else if(!this.frame.isExtId && otherFrameRef.frame.isExtId)
                        return -1;
                    else
                        return 0;
                }
            }
        case sortOrderByIsExtId:
            if(this.frame.isExtId && !otherFrameRef.frame.isExtId)
                return 1;
            else if(!this.frame.isExtId && otherFrameRef.frame.isExtId)
                return -1;
            else
                return 0;

        case sortOrderSize:
            return this.frame.size - otherFrameRef.frame.size;

        default:
            assert(false);
            return 0;
        }
    } /* End of compareTo */

    /** Select the wanted order. This method should be used prior to running a sort method
        on a collection of FrameRef objects. The setting made with this method holds for
        the whole class of FrameRef objects.
          @param newSortOrder The selected new sort order, applied to future collection
        sorts. */
    static protected void setSortOrder(SortOrder newSortOrder)
        { sortOrder = newSortOrder; }

} /* End of class FrameRef definition. */





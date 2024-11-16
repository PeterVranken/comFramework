/**
 * @file Cluster.java
 * This file defines the principal data structure: the structure of the object that is
 * passed to the StringTemplate engine for rendering the information in the different
 * output files. The design of this data structure is determined by the capabilities and
 * limitations of the StringTemplate engine.
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
/* Interface of class Cluster
 */

package codeGenerator.dataModelListener;

import java.util.*;
import java.text.*;
import codeGenerator.dbcParser.*;
import org.antlr.v4.runtime.*;


/**
 * The data structure describing the complete network cluster.
 *   An object of this class contains the results of parsing the input files, completed by
 * some configuration data. The object is passed to the template engine for rendering the
 * information in the wanted format.<p>
 *   The basic design principle is the use of public members because such members can be
 * directly accessed from the output templates. Boolean decisions are either expressed as
 * explicit public Boolean members or by references to nested member objects, which are
 * null if an optional member is not used or applicable: StringTemplate can query public
 * Boolean members or check for null references.
 */
public class Cluster
{
    /** The name of the cluster. */
    public String name = "";
    
    /** All code generation is related to one specific node in the cluster. The name of
        this node as used in the network description files. */
    public String nodeName = "";

    /** A cluster consists of buses, where in case of CAN buses one bus is described by one
        DBC file. */
    public ArrayList<Bus> busAry = null;

    /** The name of the collection in the map that holds all frames in lexical order of
        their names. Used as key into the map of frame reference collections. */
    private final String sortOrderLexical = "byName";
    
    /** The name of the collection in the map that holds all frames in raising order of
        their CAN ID. Used as key into the map of frame reference collections.
          @remark If an 11 and a 29 Bit frame have the same ID then the extended CAN ID is
        considered the greater one. */
    private final String sortOrderId = "byId";
    
    /** The name of the collection in the map that holds all frames in parsing order. Used
        as key into the map of frame reference collections. */
    private final String sortOrderUnsorted = "file";

    /** Basically, frames are a collection owned by the buses but to become able to offer
        all frames in a clusterwide sorted order, the cluster object also owns sorted
        collections of all frames of all buses.<p>
          This collection holds the frames sorted according to their names. Sorting is
        done based on the Java method String.compareToIgnoreCase, which determines the
        details of the yielded order.<p>
          A template could get the lexical list of all frames (regardless which bus it
        belongs to) by some code like:<p>
          {@code All frames in lexical order:
                 <cluster.frameByNameAry:{frameRef|<frameRef.frame><\n>}>}<p>
          The inverse sort order can be got with the StringTemplate V4 operator
        reverse: {@code <reverse(cluster.frameByNameAry):{frameRef|<frameRef.frame><\n>}>}.<p>
          Please note that the elements of the collection are not of type Frame but it are
        FrameRef objects. These form an association of the frame and the bus it is
        transmitted on. */
    public ArrayList<FrameRef> frameByNameAry = null;

    /** Basically, frames are a collection owned by the buses but to become able to offer
        all frames in a clusterwide sorted order, the cluster object also owns sorted
        collections of all frames of all buses. See {@link #frameByNameAry} for an example of
        how to apply the collection in a template.<p>
          This collection holds the frames sorted by ascending CAN ID.<p>
          Please note that the elements of the collection are not of type Frame but it are
        FrameRef objects. These form an association of the frame and the bus it is
        transmitted on. */
    public ArrayList<FrameRef> frameByIdAry = null;
    
    /** Basically, frames are a collection owned by the buses but to become able to offer
        all frames in a clusterwide sorted order, the cluster object also owns sorted
        collections of all frames of all buses. See {@link #frameByNameAry} for an example of
        how to apply the collection in a template.<p>
          This collection holds the frames sorted in the order of appearance in the network
        databases. This is the order of database files, followed by the appearance of frame
        definitions in the files. This collection has been added for completeness only;
        it's actually redundant: The same iteration along all frames is yielded with a
        normal, hierarchical iteration along cluster, buses, frames.<p>
          Please note that the elements of the collection are not of type Frame but it are
        FrameRef objects. These form an association of the frame and the bus it is
        transmitted on. */
    public ArrayList<FrameRef> frameInFileOrderAry = null;

    /** For template diagnosis: Are there (probably unsupported) floating point signals
        in use?
          Theoretically a signal may have a floating point data type. Since this is very
        rare in practice any normal application of the code generator will likely not
        support this case. To enable safe code generation templates here's a flag which
        tells if there is at least one floating point signal in use in the cluster. If so,
        the template could generate an error. */
    public boolean hasFloatingPointSignals = false;
    
    /** For template diagnosis: Are there (probably unsupported) floating point signals
        in use, which have a non trivial scaling?
          Theoretically a floating point signal in a network database file can have a
        linear scaling. However, this is completely useless and won't probably be ever
        supported by any code generation template. To enable safe code generation templates
        here's a flag which tells if there is at least one scaled floating point signal in
        use in the cluster. If so, the template could generate an error. */
    public boolean hasScaledFloatingPointSignals = false;
    
    /** Default constructor. */
    public Cluster()
    {}
    
    /**
     * Add a bus object after parsing to this cluster.
     *   @param bus
     * The bus object to add to the cluster.
     */
    public void addBus(Bus bus)
    {
        assert(bus != null);
        if(busAry == null)
            busAry = new ArrayList<>();
        busAry.add(bus);
        
        if(bus.frameAry != null)
        {
            assert(bus.frameAry.size() > 0);

            /* We make a list of new frame reference objects. The frame collection of the
               bus object can't be used just like that as the knowledge about the bus could
               not be accessed from the template code. */
            ArrayList<FrameRef> frameRefAry = new ArrayList<FrameRef>();
            for(Frame frame: bus.frameAry)
                frameRefAry.add(new FrameRef(bus, frame));

            /* Update the cluster-owned collection of all frames (as references into the bus).
               This additional collection is used to offer the frame in different sort order to
               the user. */
            if(frameByNameAry == null)
            {
                assert(frameByIdAry == null  &&  frameInFileOrderAry == null);
                frameByNameAry = new ArrayList<FrameRef>();
                frameByIdAry = new ArrayList<FrameRef>();
                frameInFileOrderAry = new ArrayList<FrameRef>();
            }

            /* The unsorted collection is updated by appending the new frame references to
               the collection so far. */
            frameInFileOrderAry.addAll(frameRefAry);
            
            /* The sorted collections are updated by first adding the new elements and then
               re-sorting them. This means an inefficient strategy; sorting once after
               having added all buses would be better. However, this would mean a two-step
               mechanism for the client code and we only have a few buses and a few hundred
               frames in practice so efficiency doesn't matter here. */
            frameByNameAry.addAll(frameRefAry);
            FrameRef.setSortOrder(FrameRef.SortOrder.sortOrderByName);
            Collections.sort(frameByNameAry);

            frameByIdAry.addAll(frameRefAry);
            FrameRef.setSortOrder(FrameRef.SortOrder.sortOrderById);
            Collections.sort(frameByIdAry);
            
        } /* End if(Bus contains at least one frame) */
    } /* End of Cluster.addBus. */
    
    /** The number of buses in {@link #busAry}. From a StringTemplate V4 template
        this member is accessed as {@code <cluster.noBuses>}.
          @return Get the number of buses. */
    public int getNoBuses()
        { return busAry != null? busAry.size(): 0; }

    /** The number of frames in the sorted collections {@link #frameByNameAry},
        {@link #frameByIdAry} and {@link #frameInFileOrderAry}. From a StringTemplate V4
        template this member is accessed as {@code <cluster.noFrames>}.
          @return Get the number of frames. */
    public int getNoFrames()
    {
        if(frameByNameAry != null)
        {
            assert(frameByNameAry.size() == frameByIdAry.size()
                   &&  frameByNameAry.size() == frameInFileOrderAry.size()
                  );
            return frameByNameAry.size();
        }
        else
        {
            assert(frameByIdAry == null  &&  frameInFileOrderAry == null);
            return 0;
        }
    } /* End of Cluster.getNoFrames */
    
    
    /**
     * Get the string representation of the cluster; it's its name. From a
     * StringTemplate V4 template this representation of the object is accessed as
     * {@code <cluster>}.
     *   @return The string value
     */
    @Override public String toString()
    {
        return name;

    } /* End of Cluster.toString */
    
} /* End of class Cluster definition. */





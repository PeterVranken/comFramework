/**
 * @file Pdu.java
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
/* Interface of class Pdu
 */

package codeGenerator.dataModelListener;

import java.util.*;


/**
 * The data structure describing a PDU in a frame.
 *   Objects of this class are part of the nested data structure passed to the template
 * engine for rendering the information in the wanted format.<p>
 *   The basic design principle is the use of public members because such members can be
 * directly accessed from the output templates. Boolean decisions are either expressed as
 * explicit public Boolean members or by references to nested member objects, which are
 * null if an optional member is not used or applicable: StringTemplate can query public
 * Boolean members or check for null references. Convenience functions, which permit to
 * query information that can easily and on the fly be derived from the other public
 * members is implemented as Getter.
 */
public class Pdu extends NetObject
{
    /** CAN communication: A multiplexed signal set is a set of signals that is selected by
        the integer value transmitted as the multiplex signal. */
    public static class MultiplexedSignalSet implements Comparable<MultiplexedSignalSet>
    {
        /** The null based index of the object in a collection, e.g. the collection of
            signal sets in a PDU. The index order is related to the appearance of the
            signal sets in the list {@link Pdu#muxSignalSetAry}. Although this order is
            meaningless the index can be used to support the implementation of arrays of
            objects or enumerations by a list of #define's in the generated code.<p>
              The value is -1 if the object is not element of a collection. */
        public int i0 = -1;

        /** The one based index of the multiplexed signal sets in a PDU. The value is
            <b>i0</b>+1.<p>
              The value is -1 if the object is not element of a collection. */
        public int i = -1;

        /** The selector value of this set of multiplexed signals. */
        public int muxValue = -1;
        
        /** The list of signals, which belong to the set. */
        public List<Signal> signalAry = null;

        /** Default constructor. */
        public MultiplexedSignalSet()
        {}
    
        /** The number of multiplexed signals in this set. From a StringTemplate V4
            template this member is accessed as {@code <muxSignalSet.noSignals>}.
          @return Get the number of signals in the set. */
        public int getNoSignals()
            {return signalAry != null? signalAry.size(): 0;}
        
        /** Add a signal to the set.
              @param s The signal object. */
        void addSignal(Signal s)
        {
            if(signalAry == null)
                signalAry = new ArrayList<Signal>();
            s.i0 = signalAry.size();
            s.i = s.i0 + 1;
            signalAry.add(s);
        }
        
        /** The comparison method, which yields the predefined sort-order.
              @return {@code >0, 0, <0} depending on relation of this to the other object.
              @param otherSignalSet The other object. */
        @Override public int compareTo(MultiplexedSignalSet otherSignalSet)
        {
            /* The default order is according to the multiplex selector value. */
            return this.muxValue - otherSignalSet.muxValue;

        } /* End of compareTo */

    } /* End of class Pdu.MultiplexedSignalSet */
    
    
    /** The ID of the PDU if applicable. Can be a global unique identifier or local with
        respect to the frame. */
    public int id = -1;
    
    /** The ID could be an extended one, which has a 29 Bit encoding on the bus. */
    public boolean isExtId = false;

    /** The node, which sends this PDU. */
    public String sender = null;
    
    /** Is the PDU sent by the given node. */
    public boolean isSent = false;
    
    /** Is at least one signal received by the given node. */
    public boolean isReceived = false;
    
    /** The maximum supported PDU size in Byte. The value is 1785, which is the maximum
        size a parameter (or PGN) can have in J1939 communication. Although such a PGN is
        not a CAN frame, it is handled as such in the DBC files used with the J1939
        protocol. */
    static public final int maxSize = 1785;

    /** The length in Byte of the PDU. */
    public int size = 0;
    
    /** An array of integers. Each contained integer is the index of a byte in the PDU,
        which needs to be initialized to 0 prior to writing the signal values into the
        PDU (i.e. prior to running the pack operation).<p>
          Signals might occupy only fractions of a byte in which case they will implement
        the write operation by an or-assignment (|=) rather than by a true assignment (=).
        These bytes will need to be zeroized at the beginning. Furthermore, there might be
        bytes in the PDU not occupied by any signal. These should not stay undefined.
          @remark If a PDU uses the signal multiplex concept then the array will list at
        least all the bytes, which contribute to a multiplexed signal with at least one
        bit. A sophisticated algorithm which determines the optimal, minimal byte list has
        not been implemented for this case. Note, the missing "sophisticated algorithm"
        doesn't lead to a failure but only to a few uselessy initialized bytes in rare
        cases. */
    public int[] idxByteInitialNullAry = null;

    /** The PDU has a byte position inside the frame. Here is the byte offset relative to
        the beginning of the frame. */
    public int offsetInFrame = 0;
    
    /** A PDU consists of signals. All normal, not multiplexed signals of the PDU are held
        in this list. If there are no such signals then this field is null.<p>
          For CAN communication a multiplexing is defined. Several sub-sets of signals
        share the same bit positions in the PDU and can appear only one at a time,
        controlled by a multiplex signal. This bunch of signals, selector and selected
        signals, is not contained in the array. */
    public List<Signal> signalAry = null;

    /** Default constructor. */
    public Pdu()
    {}
    
    /** The number of normal, not multiplexed signals. Neither the multiplex selector nor
        the multiplexed signals are included in the count.<p>
          From a StringTemplate V4 template this member is accessed as {@code
        <pdu.noSignals>}.
          @return Get the number of signals. */
    public int getNoSignals()
        {return signalAry != null? signalAry.size(): 0;}

    /** The total number of signals. This is the count of all signals; normal and
        multiplexed signals and the multiplex selector.<p>
          From a StringTemplate V4 template this member is accessed as {@code
        <pdu.totalNoSignals>}.
          @return Get the total number of signals. */
    public int getTotalNoSignals()
    {
        int noSig = getNoSignals()
                    + (muxSelector != null? 1: 0);
        if(muxSignalSetAry != null)
        {
            for(MultiplexedSignalSet muxSignalSet: muxSignalSetAry)
                noSig += muxSignalSet.getNoSignals();
        }
        return noSig;
    }

    /** If (and only if) there is a multiplex signal then the this signal is not null. It
        is the selecting signal. */
    public Signal muxSelector = null;
    
    /** If there is at least one multiplexed signal then this list is the list of sets of
        multiplexed signals, which all have the same multiplex value. Otherwise the list is
        null. */
    public List<MultiplexedSignalSet> muxSignalSetAry = null;

    /** The number of multiplexed signal sets. The multiplex selector is no such signal
        (set) and is not included in the count. From a StringTemplate V4 template this
        member is accessed as {@code <pdu.noMuxSignalSets>}.
          @return Get the number of multiplexed signal sets. */
    public int getNoMuxSignalSets()
        {return muxSignalSetAry != null? muxSignalSetAry.size(): 0;}

    /** The map of those signals, which were identified by name as signals of special
        interest, e.g. for checksum or sequence counter evaluation.<p>
          Special signals are identified by name. The key into the map is the name of the
        special signal as given on the tool's command line; this holds regardless of the
        signal name in the network database that matched against the regular expression
        that is given on the command line, too. The map is null if a special signal is
        neither requested on the command line nor found in this particular PDU.<p>
          The value retrieved from the map is a reference to the {@link Signal}
        object. It's the same object as held in the linear lists {@link #signalAry} or
        {@link MultiplexedSignalSet#signalAry}. The indexes {@link #i} and {@link #i0} of the
        retrieved {@link Signal} object relate to those linear lists and don't form a
        contiguous index space in this map at the same time.<p>
          In a StringTemplate V4 template the special signal {@code checksum} of a PDU is
        retrieved by an expression like {@code <pdu.specialSignalMap.checksum>}. The size
        of this signal in Bit could e.g. be rendered by {@code
        <pdu.specialSignalMap.checksum.length>}. This example requires the command line
        argument {@code --special-signal-name checksum} to agree on the symbolic name
        {@code checksum} for this special signal. */
    public HashMap<String,Signal> specialSignalMap = null;

} /* End of class Pdu definition. */





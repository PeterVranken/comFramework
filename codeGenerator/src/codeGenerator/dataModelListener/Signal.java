/**
 * @file Signal.java
 * Part of the output data model for information rendering by StringTemplate: An exchange
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
/* Interface of class Signal
 */

package codeGenerator.dataModelListener;

import java.util.*;


/**
 * The data structure describing a single signal.
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
public class Signal extends NetObject implements Comparable<Signal>
{
    /** For inbound PDUs: Not all signals need necessarily be received by the given node. */
    public boolean isReceived = false;

    /** This flag is {@code true} if and only if the signal is the selector value for
        multiplexed signals in the frame. */
    public boolean isMuxSelector = false;
    
    /** This flag is {@code true} for and only for multiplexed signals. */
    public boolean isMuxedSignal = false;
    
    /** The multiplex selector value to activate this signal if it is a multiplexed signal,
        i.e. if {@link #isMuxedSignal} is {@code true}. The value is -1 for normal, non
        multiplexed signals. */
    public int muxValue = -1;

    /** Query function for special signals: Is this signal a particular special signal?<p>
          The field is a Java Map object if the signal has been recognized as a special
        signal, otherwise <b>is</b> is null. (Special signals -- e.g. a checksum or a
        sequence counter -- and how to recognize them is user-specified by according
        command line arguments.) The unusual name of this member has been chosen for sake
        of an intuitive design of the aimed template code, see below.<p>
          The map has a single key, value pair for each special signal it matches with
        (normally only one). The key is the symbolic name of the special signal as it
        has been specified on the command line and the value is a Boolean {@code true}.
        Using a Map object in StringTemplate implicitly leads to a Boolean {@code false} if
        a key is used, which is not stored in the map. Consequently, if asking for the
        name of a special signal you will always get the correct answer whether the signal
        is or is not this special signal.<p>
          Example: Be {@code --special-signal-name checksum --re-special-signal crc.*} part
        of the command line. A signal, whose name matches the regular expression {@code
        crc.*} is recognized as special signal {@code checksum} and a StringTemplate V4
        template could use a construct as follows to emit specific code for those checksum
        signals:
        <pre>{@code
          <pdu.signalAry:{s|<if(s.is.checksum)><\\>
          printf("The checksum of PDU " <pdu> " is %u\n", <cif_signalVarName(s)>);
          <endif>}>}
        </pre> */
    public Map<String,Boolean> is = null;
     
    /** The length of the signal in Bit. */
    public int length = 0;

    /** The bit position of the signal in the PDU. LSB of first byte is bit position 0, MSB
        of this byte is 7, LSB of second byte is 8, etc. startBit is defined as in the
        database file; it relates to the LSB for signals in Intel byte order and to the MSB
        for signals in Motorola byte order. */
    public int startBit = 0;

    /** The position of the LSB of the signal in the PDU. This is the byte index in the
        range 0 .. pdu.size-1. The value does not depend on the byte order of the
        signal. */ 
    public int idxByteLSB = 0;
    
    /** The position of the LSB of the signal in the PDU. This is the bit index in the
        byte designated by idxByteLSB. The LSB of the byte has index 0, the range is 0
        .. 7. The value does not depend on the byte order of the signal. */
    public int idxBitInByteLSB = 0;
    
    /** Extended data type information: Is it a (scaled) integer signal? */
    public boolean isInteger = true;

    /** Extended data type information: Is it a float signal? Can become true only for 32
        Bit signals. */
    public boolean isFloat = false;

    /** Extended data type information: Is it a double signal? Can become true only for 64
        Bit signals. */
    public boolean isDouble = false;

    /** The byte-order. */
    public boolean isMotorola = true;

    /** The minmum (world) value of the signal. */
    public double min = 0.0;

    /** The maximum (world) value of the signal. */
    public double max = 0.0;

    /** The scaling factor. World value is binary value*{@link #factor}+{@link #offset}
        [unit]. */
    public double factor = 0.0;

    /** The offset value of the scaling. World value is binary value*{@link #factor}+{@link
        #offset} [unit]. */
    public double offset = 0.0;

    /** True integer quantities usually don't need a scaling operation. This flag indicates
        if a signal has the specific scaling {@link #factor}=1 and {@link #offset}=0, thus
        the identity or one-by-one scaling. */
    public boolean isVoidScaling = false;
    
    /** Boolean indication if the scaling factor is void, meaning {@link #factor}=1. */
    public boolean isVoidFactor = false;
    
    /** Boolean indication if the offset is void, meaning {@link #offset}=0. */
    public boolean isVoidOffset = false;
        
    /** The unit of the signal's world value or null if no unit is specified. */
    public String unit = "";

    /** The definition of a single named signal value. */
    public static class ValueDesc implements Comparable<ValueDesc>
    {
        /** The textual representation of the signal value. */
        public String name = null;

        /** The numeric representation of the named signal value, which needs to be an
            integer.<p>
              Note, i is not the index in the collection of named signal values and,
            consequently, there is no counterpart i0. */
        public long i = 0;

        /**
         * Create a new object.
         *   @param sigName The name of the signal value.
         *   @param sigNumVal The numeric integer value.
         */
        ValueDesc(String sigName, long sigNumVal)
            { name = sigName; i = sigNumVal; }

        /** The comparison method, which yields the predefined sort-order.
              @return -1, 0, 1 depending on relation of this to the other object.
              @param otherValue The other object. */
        @Override public int compareTo(ValueDesc otherValue)
        {
            /* The default order is according to the numeric value of the named value. */
            if(this.i > otherValue.i)
                return 1;
            else if(this.i < otherValue.i)
                return -1;
            else
                return 0;

        } /* End of compareTo */

    } /* End class Signal.ValueDesc */

    /** The list of named signal values. */
    public ArrayList<ValueDesc> valueDescAry = null;

    /** The number of named signal values. From a StringTemplate V4 template this member is
        accessed as {@code <pdu.noValueDescs>}.
          @return Get the number of value descriptions. */
    public int getNoValueDescs()
        { return valueDescAry != null? valueDescAry.size(): 0; }

    /** Signed or unsigned data type? */
    public boolean isSigned = false;

    /** The basic data type in the target language, which is used to represent the signal. */
    public String type = null;

    /** The position of the bytes in the packed PDU, which contain at least one bit
        of the signal. The byte order is MSByte first, which means that we will see
        a sequence of decreasing indexes for Intel signals. */
    public int[] byteAry = null;

    /** Mask of used bits per byte.<p>
          This array relates to the other array byteAry. It has the same size and the
        elements of same index correspond. The elements in this array hold the information
        about the bits in a byte, which are occupied by the signal. These bits are set in
        the mask byte found here.<p>
          If the signal occupies all bits of the byte then the mask would be 0xff. However,
        the generated access code doesn't need to do a masking operation at all (assuming
        the access is done bytewise). To indicate this situation and to permit having
        "optimizing" conditional template code such a mask is not represented by an Integer
        of value 255 but by a Boolean of value false; the itended use case is like:<p>
          {@code <if(maskByte)> & <maskByte><endif>} */
    public Object[] maskAry = null;

    /** Due to the possibly shifted location of a signal value it might be necessary
        to use an intermediate type to access the data that has more bytes than the final
        signal value has. Furthermore this intermediate type will always be unsigned. */
    public String accType = null;

    /** Optional left shift operation: Needed only for sign bit propagation of signed
        signals, otherwise null. */
    public Integer shiftLeft = null;

    /** Optional right shift operation to make final result right aligned. May be null if no
        such shift is required. */
    public Integer shiftRight = null;

    /** The list of names of receiver network nodes of this signal. */
    public String[] receiverAry = null;

    /** Default constructor. */
    public Signal()
    {}
    
    /** Give acces to the number of receivers. From a StringTemplate V4 template this
        member is accessed as {@code <pdu.noReceivers>}. 
          @return Get the number of (known) receivers of this signal. */
    public int getNoReceivers()
        { return receiverAry != null? receiverAry.length: 0; }

    /** The comparison method, which yields the predefined sort-order.
          @return {@code >0, 0, <0} depending on the relation of this to the other object.
          @param otherSignal The other object. */
    @Override public int compareTo(Signal otherSignal)
    {
        /* The default order is by location inside the PDU; we refer to the start bit. */
        return this.startBit - otherSignal.startBit;

    } /* End of compareTo */

} /* End of class Signal definition. */





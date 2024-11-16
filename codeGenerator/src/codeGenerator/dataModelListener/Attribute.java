/**
 * @file Attribute.java
 * Part of the output data model for information rendering by StringTemplate: An attribute's
 * value.
 *
 * Copyright (C) 2015-2024 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class Attribute
 */

package codeGenerator.dataModelListener;

import java.util.*;


/**
 * The data structure describing an attribute value definition.
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
public class Attribute
{
    /** The name of the attribute. */
    public String name = null;
    
    /** The null based index of the object in a collection, e.g. the collection of signals
        in a PDU. The index order is related to the appearance of the object definition in
        the network database. Although this order is meaningless the index can be used to
        support the implementation of arrays of objects or enumerations by a list of
        #define's in the generated code.<p>
          The value is -1 if the object is not element of a collection. */
    public int i0 = -1;
    
    /** The one based index of the attribute in a collection, e.g. the attribMap of a
        frame. The value is i0+1.<p>
          The value is -1 if the object is not element of a collection. */
    public int i = -1;
      
    /** The numeric value of the attribute. It is of Java type Long for an attribute of
        type int, hex or of type Integer for an enumeration or of Java type Double for a
        floating point attribute.<p>
          The numeric value for an enumeration attribute is its 0 based index, where the
        index relates to the list of value names specified in the attribute definition.
          @remark The full value range of Java's Long is not supported for attributes of
        type int and hex. This Java type has been chosen to safely capture the intended
        range of unsigned 32 Bit integers. Attribute value handling involves floating point
        operations and (far) outside the 32 Bit integer range there might be numeric
        deviations because of the limited resolution of type double. */
    public Number n = null;
    
    /** If the attribute is not of kind string, then the value of field n is rendered as
        floating point value. (For string attributes the result is NaN, not a number.).<p>
          Real existing DBC files tend to used improper typing. Using this method can help
        to get the adequate rendering of an integer attribute, which actually represents a
        continuously valued quantity.<p>
          Note, ST4 accesses getters from a template by capitalizing the first character and
        adding prefix "get" to what is written in the template. To access this getter you
        need to use a template construct like this one:
        <pre>{@code 
          Minimum distance time: <frame.attribMap.tiMsgMinDist.doubleValue; format="%.3f">}
        </pre>
          @return The method returns the value of n as a Java double value. */
    public double getDoubleValue()
        {return n != null? n.doubleValue(): Double.NaN;}
     
    /** If the attribute is not of kind string, then the value of field n is rendered as
        integral number. (For string attributes the result is 0.)<p>
          Real existing DBC files tend to used improper typing. Using this method can help
        to get the adequate rendering of an continuously valued quantity, which is modelled
        as an integer attribute.<p>
          Note, ST4 accesses getters from a template by capitalizing the first character and
        adding prefix "get" to what is written in the template. To access this getter you
        need to use a template construct like this one:
        <pre>{@code
          Maximum number of messages per second: <frame.attribMap.maxNoMsgsPerSec.intValue; format="%i">}
        </pre>
          @return The method returns the value of n as a Java int value. */
    public long getIntValue()
        {return n != null? n.longValue(): 0;}
     
    /** If the attribute is not of kind string, then the comparison of the value of field n
        with zero is rendered as Boolean. (For string attributes the result is false.) Use
        case are integer attributes, which effectively model Boolean properties.<p>
          Note, ST4 accesses getters from a template by capitalizing the first character and
        adding prefix "get" to what is written in the template. To access this getter you
        need to use a template construct like this one:
        <pre>{@code
          <if(!bus.attribMap.isCanFd.isZero)>
            // The CAN driver is initialized in CAN FD mode.
          <else>
            // The CAN driver is initialized in standard mode.
          <endif>
          initCanDriver(!<bus.attribMap.isCanFd.isZero>);}
        </pre>
          @return Get true if n is defined and has a non-zero numeric value. */
    public boolean getIsZero() 
        {return n != null  &&  n.doubleValue() != 0.0;}
    
    /** The string value of the attribute if it is of type string or enumeration. */
    public String str = null;
    
    /** An attribute's value can either be set explicitly for an object or the object can
        have the attribute's default value (but it can't have no value). Whether this
        attribute has the default value is indicated by this Boolean. */
    public boolean isDefVal = false;
    
    /** Query function for enumeration type attributes: Does the attribute have a specific
        enumeration value?<p>
          The query is implemented as a Java Map if the attribute is of type enumeration,
        otherwise <b>is</b> is null. The unusual name of this member has been chosen for
        sake of an intuitive design of the aimed template code.<p>
          The map has a single key, value pair. The key is the name of the value the
        enumeration attribute has and the value is a Boolean true. Using a map object in
        StringTemplate implicitly leads to a Boolean false if a key is used, which is not
        stored in the map. Consequently, just asking for an enumeration value name you will
        always get the correct answer whether the attribute has this value.<p>
          Example: Given, the attribute definition specifies an enumeration for the send
        characteristics of a frame with the possible values "undefined", "regular",
        "occasional", then your StringTemplate V4 template could look like:
        <pre>{@code 
          <if(frame.attribMap.sendCharacteristics.is.regular)>
            // <frame.name> is a regularly sent frame.
            // ... Put your C code generation to handle regular frames here
          <elseif(frame.attribMap.sendCharacteristics.is.occasional)>
            // <frame.name> is an event triggered frame.
            // ... Put your C code generation to handle event triggered frames here
          <else>
            #error Unexpected type of frame found for <frame.name>. Please, double <\\>
            check your network database file <bus.networkFile>
          <endif>}
        </pre> */
    public HashMap<String,Boolean> is = null;
     
    /** Default constructor. */
    public Attribute()
    {}
    
    /**
     * Get the string representation of the attribute value. From a StringTemplate V4
     * template this representation of the object is accessed as e.g. {@code <attrib>}.
     *   @return The string value
     */
    @Override public String toString()
    {
        /* A safe type recognition is not possible; a numeric type is assumed if the string
           is empty. Normally this will be okay and the function is anyway used only for
           debug output. */
        if(str != null  &&  str.compareTo("") != 0)
            return str;
        else if(n != null)
            return n.toString();
        else
            return "";

    } /* End of Attribute.toString */
}

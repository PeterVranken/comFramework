/**
 * @file AttributeDef.java
 * Part of the output data model for information rendering by StringTemplate: An attribute
 * definition.
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
/* Interface of class AttributeDef
 */

package codeGenerator.dataModelListener;

import java.util.*;


/**
 * The data structure describing an attribute definition.
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
public class AttributeDef
{
    /** The name of the attribute. */
    public String name;

    /** Attributes are related to either the bus, a node, a frame or a signal. This is
        called the object type. */
    public enum ObjectType 
    {
        /** Unused, initial, undefined value. */
        undefined,

        /** Attribute relates to a CAN bus. */
        bus,

        /** Attribute relates to a CAN network node. */
        node,

        /** Attribute relates to a CAN frame. */
        frame,

        /** Attribute relates to a CAN signal. */
        signal,

        /** Attribute relates to an evironment variable. */
        envVar
    };

    /** Attributes are related to either the bus, a node, a frame or a signal. The object
        type this attribute relates to. */
    public ObjectType objType = ObjectType.undefined;

    /** To support conditional code in the templates the object type the attribute relates
        to is represented as a set of Booleans, too. One and only one of these will be set. */
    public boolean isBus = false
                 , isNode = false
                 , isFrame = false
                 , isSignal = false
                 , isEnvVar = false;

    /** The zero based index of the object in a collection, e.g. the collection of signals
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
      
    /** The data type of the attribute.<p>
          Hex is a special form of integer. It's probably meant that the principal
        representation of the integer value should be hexadecimal. Consequently, if isHex is
        true then isInt is also true. */
    public boolean isString = false
                 , isEnum = false
                 , isInt = false
                 , isHex = false
                 , isFloat = false;

    /** Min and max applicable for int, hex and float. For enum min is always 0
        and max is number of enum values minus one. Otherwise both 0.
          @remark In nearly all use cases min and max hold integer values. Using the data
        type double is nonetheless safe as all 32 Bit integers can be represented error
        free as double and as StringTemplate renders double values of integers by default
        without a fractional part. */
    public double min = 0
                , max = 0;

    /** The definition of a single named value from an enumeration type atttribute. */
    public static class EnumValueDef
    {
        /** The textual representation of the enumeration value. */
        public String name = null;

        /** The numeric representation of the enumeration value as a zero based index.<p>
              This number is the value of an actual attribute of this type and which can be
            retrieved from an object of type {@link Attribute} through its field {@link
            Attribute#n}, e.g. by a template expression like {@code
            <frame.attribMap.myParticularAttributeName.n>}. */
        public int i0 = 0;
        
        /** The numeric representation of the enumeration value as a one based index. */
        public int i = 0;
    
        /** Default constructor. */
        public EnumValueDef()
        {}
    }

    /** The list of all enumeration values of an enumeration type attribute. */
    public EnumValueDef[] enumValAry = null;

    /** The default value for this attribute or null if no such value is defined.<p>
          Even if an attribute doesn't have a default value all affected objects (frame,
        signal, etc.) can still have got an individual, actual value. In which case the
        parser has emitted a warning only about the missing default value. Code generation
        templates need to anticipate a possible null value here but can count on an actual
        value of the attribute when visiting the affected objects. */
    public Attribute defVal = null;
    
    /** Default constructor. */
    public AttributeDef()
    {}
    
    /**
     * Get the string representation of the attribute definition; it's the name of the
     * attribute. From a StringTemplate V4 template this representation of the object is
     * accessed as e.g. {@code <attribDef>}.
     *   @return The string value
     */
    @Override public String toString()
    {
        return name;

    } /* End of AttributeDef.toString */
}

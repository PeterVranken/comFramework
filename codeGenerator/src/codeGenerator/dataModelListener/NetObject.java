/**
 * @file NetObject.java
 * Part of the output data model for information rendering by StringTemplate: The common
 * parts of all network objects.
 *
 * Copyright (C) 2015-2023 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class NetObject
 *   storeAttribValue
 */

package codeGenerator.dataModelListener;

import java.util.*;


/**
 * The data structure describing the common parts of all network objects.
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

public class NetObject
{
    /** The ID generator. */
    private static int _nextObjId = 1;

    /** The ID of this object. Each instance of a NetObject has its unique ID, which can be
        useful for having related data objects in the generated code with individual names. */
    public int objId = _nextObjId++;

    /** The name of the object. */
    public String name = "";
    
    /** The null based index of the object in a collection, e.g., the collection of signals
        in a PDU. The index order is related to the appearance of the object definition in
        the network database. Although this order is meaningless the index can be used to
        support the implementation of arrays of objects or enumerations by a list of
        #define's in the generated code.<p>
          If the reference to an object is held in more than one collection, then the index
        relates to the most important collection.<p>
          The value is -1 if the object is not element of a collection. */
    public int i0 = -1;
    
    /** The one based index of the attribute in a collection, e.g., the attribMap of a
        frame. The value is i0+1.<p>
          If the reference to an object is held in more than one collection, then the index
        relates to the most important collection.<p>
          The value is -1 if the object is not element of a collection. */
    public int i = -1;
      
    /** The description of the object. It is taken from the comment in the network data
        base file. If several comments are defined for the same object in the *.dbc file
        then they are concatenated in this string.
          @remark The description of an object is useless in the code generation process if
        it injects undesired characters into the generated output. If the aimed output is
        e.g., a C source file than the description must not contain non ASCII characters.
        But even more characters are unsafe: If the description is used as string literal
        in generated C source code then a backslash or double quote would invalidate the
        generated code. Other target languages prohibit the presence of line feeds, e.g.,
        GNU Octave. Therefore potentially unsafe characters are filtered out and
        replaced by a dot. If the DBC file contains natural language comments then they
        will likely be somewhat deformed in the generated output. */
    public String desc = null;
    
    /** Network objects can have attributes. All attributes, which belong to this kind of
        network object are held in a map. An attribute is referenced by name in this map.<p>
          Please note, the term attribute relates to the attributes defined in the network
        database - as opposed to the StringTemplate V4 template attributes and the user
        attributes. */
    public Map<String,Attribute> attribMap = null;
    
    /** Default constructor. */
    public NetObject()
    {}
    
    /**
     * The number of elements in the attribute map as a get function. Access it from a
     * template as {@literal <myNetObj.noAttribs}.
     *   @return The number of attributes, which relate to this network object.
     */
    public int getNoAttribs()
    {
        return attribMap != null? attribMap.size(): 0;

    } /* End of NetObject.getNoAttribs */
    
    
    /**
     * Get the string representation of the NetObject; it's its name by default. From a
     * StringTemplate V4 template this default representation of the object is accessed as
     * {@code <obj>}, where <b>obj</b> is a place holder for the specific network obejct,
     * e.g., a node, a frame, a signal, etc.
     *   @return The string value
     */
    @Override public String toString()
    {
        return name;

    } /* End of NetObject.toString */
    
    
    /**
     * Store an attribute value in a NetObject.<p>
     *   The indexes in the collection, <b>i0</b> and <b>i</b>, are set by side-effect.
     * They enumerate all stored attribs in the order of storage.
     *   @return Should normally be null. Get the reference to the overridden, discarded
     * attribute if an attribute of given name had already been stored before.
     *   @param name The name of the attribute.
     *   @param attrib The Attribute object.
     */
    protected Attribute storeAttribValue(String name, Attribute attrib)
    {
        if(attribMap == null)
            attribMap = new HashMap<String,Attribute>();
        Attribute overwrittenAttrib = attribMap.put(name, attrib);
        
        /* Normally we increment the index of the attribute in the collection but in case
           we replace another one then we inherit the index in order to avoid a gap in the
           sequence of indexes. */
        if(overwrittenAttrib == null)
            attrib.i = attribMap.size();
        else
            attrib.i = overwrittenAttrib.i;
        assert attrib.i > 0;
        attrib.i0 = attrib.i - 1;
        
        return overwrittenAttrib;

    } /* End of NetObject.storeValue */
    
} /* End class NetObject */

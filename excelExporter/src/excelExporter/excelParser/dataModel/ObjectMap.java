/**
 * @file ObjectMap.java
 * This is the base class of most elements in the data model, a map of objects, which is
 * compliant with the interface of the StringTemplate V4 engine. Such a map offers
 * arbitrary data elements from the parsed input next to fixed, pre-defined elements, which
 * support the data model.
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
/* Interface of class ObjectMap
 *   ObjectMap
 *   setName
 *   setLogContext
 *   setIndexInCollection
 *   isPseudoFieldName
 *   getPseudoField
 *   entrySet
 *   containsKey
 *   get
 *   put
 *   putItem
 *   getItem
 *   createComparator
 *   toString
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.*;


/**
 * This is the base class of most elements in the data model, a map of objects, which is
 * compliant with the interface of the StringTemplate V4 engine. Such a map offers
 * arbitrary data elements from the parsed input next to fixed, predefined elements, which
 * support the data model.<p>
 *   The class extends the Java Map. The map values are the data items from the parsed
 * input which can thus be accessed directly by name from a StringTemplate V4 template, by
 * using separating dots similar to the notation of fields in Java objects.<p>
 *   Most objects of the data model are instances of this kind of container. The idea is to
 * access the data items in the container under their natural name. As an example, if the
 * container is the representation of an Excel workbook then the principal data items are
 * the worksheets of that book and they could be accessed by name. Be the workbook "myBook"
 * and have it two worksheets, named "mySheetA" and "mySheetB". From a StringTemplate V4
 * template one could access the parsed data like {@code <myBook.mySheetA>} or {@code
 * <myBook.mySheetB>}.<p>
 *   This way to access dynamic data (the names of the "fields" of {@code myBook} are
 * determined by the application input) is convenient and in the template syntactically
 * very close to the conventional StringTemplate data model, which is a predefined Java
 * data structure consisting of nested Java classes having public fields of predefined
 * names. In either case the dot notation is used in the template.<p>
 *   The major difference between this container class and the conventional data model is
 * the inaccessibility of additional public fields. For map objects the StringTemplate V4
 * template engine only supports the map lookup but not the "normal" introspection for
 * public fields of requested name. If we extend the container with public fields, e.g. we
 * want to add the field "noSheets" to our workbook, then this field would not be visible
 * from a template. We support such fields by mapping them into the map's contents. The
 * Java Map implementation is overridden to do so. Now all access from the template engine
 * is done via the map interface, be it access of predefined fields like our example
 * "noSheets" or be it access of true, dynamic contents, i.e. data items like "mySheetA" or
 * "mySheetB". We call the predefined fields "pseudo-fields" since from a template they
 * look like and behave like true public fields of the Java object but in fact they are
 * implemented as map content items.<p>
 *   This class implements a map, which supports true map behavior (dynamic data items
 * from the application input) and having predefined pseudo-fields (predefined by our
 * application design), that are merged into the map contents. Some generic pseudo-fields
 * are already defined by this base class (e.g. the name of the container) and it provides
 * the mechanisms to let a derived class define its own, further pseudo-fields.<p>
 *   Two problems arise.<p> 
 *   Evidently, pseudo-fields and true, dynamic data contents share the map key space. All
 * keys are Java String objects with the meaning name-of-object. The name under which a
 * pseudo-field is stored and accessed cannot be used for a dynamic data item at the same
 * time. Since we have the pseudo-field "name_" there can't be a worksheet called {@code
 * name_} in the workbook {@code myBook} in our example above. From a template {@code
 * <myBook.name_>} refers to the name of {@code myBook} but not to a worksheet of that
 * name.<p>
 *   The implementation of this map class checks for name clashes of this kind at parse
 * time, when the map is filled with the information from the application input.<p>
 *   It is impossible to complete parsing if there's such a name clash. Since naming of
 * data items can be controlled via command line options this will not mean that it would
 * be impossible to successfully read the input, only some fine tuning of the application
 * configuration will be required.<p>
 *   In practice, name clashes will occur only exceptional. Typical names of pseudo-fields
 * in the data model will not likely appear in natural Excel input. Where this is not the
 * case we decided to rename our pseudo-fields, even if this leads to less neat template
 * code. The most prominent example is the name of an object. In the conventional data
 * model this is the public field {@code name}. Since "name" can easily appear in natural
 * input we called the pseudo-field "name_". The same happens for the index-in-collection,
 * our conventional "i" and "i0" became "i_" and "i0_", respectively.<p>
 *   The second problem is the documentation. Conventional data models, which are
 * predefined, nested Java classes are appropriately documented with Javadoc. All public
 * fields are listed and explained in meaningful HTML representation and their nesting is
 * easily understandable. Using this container class the pseudo-fields are implemented by
 * run-time code and won't appear as such in the Javadoc documentation.<p>
 *   At several points the design of the implementation of this map class was driven by
 * considerations how to make information important to StringTemplate V4 template writers
 * visible in the Javadoc documentation. First of all, the pseudo-fields are held in true
 * fields. These are inaccessible via the template engine and not required by surrounding
 * Java code. Under normal circumstances these fields would be implemented private. We made
 * them public so that the application user will find them in Javadoc. The user sees these
 * fields as public - and can indeed access them (if not as public fields but via the map
 * interface as pseudo-field). Secondary, all pseudo-fields are listed as a Java
 * enumeration. For this base class and for each derived container class you will find the
 * related enumeration and each named value will be documented with a short explanation of
 * the pseudo-field it relates to. See e.g. {@link Cluster.PseudoFieldName} or {@link
 * ObjectMap.PseudoFieldName}.
 */
public class ObjectMap<T> extends AbstractMap<Object,Object>
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(ObjectMap.class);

    /** The error counter for the parsing and/or data rendering process. */
    protected final ErrorCounter errCnt_;

    /** A formatted string used to precede all logging statements of this module. */
    protected String logCtx_ = null;

    /** This is the list of pseudo-fields of class {@link ObjectMap} that can be accessed
        from a StringTemplate V4 template. The enumerated fields can be accessed like
        ordinary public fields from a StringTemplate V4 template. The dot notation is used
        to do so. Taking the first named value {@link PseudoFieldName#name_} as example,
        one would write {@code <obj.name_>} to access the related pseudo-field.<p>
          A detailed discussion of pseudo-fields and the enumeration of these can be found
        in the class description of {@link ObjectMap}. */
    public static enum PseudoFieldName
    {
        /** The name {@link ObjectMap#name_} of the container of data items.<p>
              From a StringTemplate V4 template the object name would be accessed  with an
            expression like {@code <object.name_>}.
              @remark Careful, this yields an {@link Identifier} object, not a Java String.
            Rending the name as {@code <object.name_>} in a StringTemplate V4 template will
            evaluate to the name as C identifier. {@code <object.name_.givenName>} rather
            is what you may intend. */
        name_,
        
        /** The unique ID {@link ObjectMap#objId} of this object in the data model.<p>
              From a StringTemplate V4 template the ID would be accessed with an expression
            like {@code <object.objId>}. */
        objId,
        
        /** The null based index-in-collection {@link ObjectMap#i0_} of the object.<p>
              From a StringTemplate V4 template the index would be accessed with an
            expression like {@code <object.i0_>}. */
        i0_,
                
        /** The one based index-in-collection {@link ObjectMap#i_} of the object.<p>
              From a StringTemplate V4 template the index would be accessed with an
            expression like {@code <object.i_>}. */
        i_,
        
        /** The sorted list {@link ObjectMap#itemAry} of real data items in the
            container. (Pseudo-fields are not element of this list.)<p>
              From a StringTemplate V4 template the list would be accessed with an
            expression like {@code <object.itemAry>}. */
        itemAry,
        
        /** The map {@link ObjectMap#itemMap} of real data items in the
            container. (Pseudo-fields are not element of this map.)<p>
              From a StringTemplate V4 template the map would be accessed with an
            expression like {@code <object.itemMap>}. The map is sorted in case-sensitive
            lexical order of key strings (i.e. names of the data items). */
        itemMap,
        
        /** The number of real data items in map {@link ObjectMap#itemMap} and list {@link
            ObjectMap#itemAry}, as returned by {@link ObjectMap#getNoItems}.<p>
              From a StringTemplate V4 template the number of items would be accessed with
            an expression like {@code <object.noItems>}. */
        noItems,
        
        /** Flag {@link ObjectMap#exists}, the presence of the container in the data
            model.<p>
              From a StringTemplate V4 template the flag would be queried with an
            expression like {@code <if(container.exists)>Can evaluate container
            contents<endif>}. */
        exists,
    };

    
    /** The ID of this object. Each object in the data model gets a unique ID, which can be
        useful for having related data objects in the generated code with individual
        names. */
    public final int objId;

    /** The name of the object.
          @remark The name is stored as an object of type {@link Identifier}. If is is
        rendered like {@code <obj.name_>} in a StringTemplate V4 template you will not get
        the given name but the C identifier, which is most similar to the given name - this
        can even be identical to the name. If you really want to get the given name then
        you'd rather put {@code <obj.name_.givenName>} into your template.<p>
          For consistency, this field would be better named "name" (compare to other
        elements of the data model). However, all public field names of this class are
        reserved key words at the same time and can't be used as designations in the
        application input. Therefore, we can't apply such a common designation as "name". */
    public Identifier name_ = null;

    /** The null based index of the object in a collection of those. If the object is held
        in more than one collection then the index relates to the most important one. The
        index can be used to support the implementation of arrays or enumerations of
        objects in the generated output.
          @remark This field should be named {@code i0} for consistency with our usual data
        model naming scheme. However, all public field names of this class are
        reserved key words at the same time and can't be used as designations in the
        application input. Therefore, we can't apply such a common designation as "i0". */
    public int i0_ = -1;

    /** The one based index of the object in a collection of those. If the object is held
        in more than one collection then the index relates to the most important one. The
        index can be used to support the implementation of arrays or enumerations of
        objects in the generated output.<p>
          The value of this index is {@code i0_ + 1}.
          @remark This field should be named {@code i} for consistency with our usual data
        model naming scheme. However, all public field names of this class are
        reserved key words at the same time and can't be used as designations in the
        application input. Therefore, we can't apply such a common designation as "i" */
    public int i_ = -1;

    /** This is an embedded map, which stores all real data items (in contrast to the
        pseudo-fields). Which are the real data items depends on the actual container. This
        is the base class implementation and different derived conatiner classes will
        typically contain different kinds of data items:
        <ul>
        <li>{@link Cluster}: Data items are {@link ExcelWorkbook} objects
        <li>{@link ExcelWorkbook}: Data items are {@link ExcelWorksheet} objects
        <li>{@link ExcelWorksheet}: Data items are {@link RowObjectContainer} objects, the
          groups of row objects
        <li>{@link RowObjectContainer}: Recursively contains data items of same type. This
        model the recursive grouping of row objects
        <li>{@link RowObject}: Data items are {@link CellObject} objects</ul> */
    public final Map<String,T> itemMap;

    /** All real data items (in contrast to the pseudo-fields), which are held in the map
        {@link #itemMap} are stored a second time in this list.
          @remark The list is sorted; which order is determined by the application
        configuration and is kept for reference in {@link ObjectList#sortOrder}. From a
        StringTemplate V4 template the sort order can be queried with an expression like
        {@code <obj.itemAry.sortOrder>}. */
    public ObjectList<T> itemAry = null;

    /** The number of data items in {@link #itemAry} and {@link #itemMap}. From a
        StringTemplate V4 template this member is accessed as {@code <obj.noItems>}.
          @return Get the number of items. */
    public int getNoItems()
        {return itemMap.size();}

    /** The presence of a container object as Boolean. From a StringTemplate V4 template it
        is a bit tricky to query the existance of a container of this class. While the
        existance of an object in the data model is normally queried by a template
        expression like<p>
          {@code <if(cluster.object)>"object" is present<endif>}<p>
        will this fail for Java Map objects. {@code <if(cluster.mapObject)>} will return
        {@code false} even if the map object exists but if it is empty. Empty maps are
        however quite common in our data model. Let's take for example a flat worksheet
        "mySheet" of workbook "myBook" that owns all row objects in the root level and
        doesn't have nested sub-groups of row objects. The template expression<p>
          {@code <if(myBook.mySheet)><myBook.mySheet.rowAry:renderRow()><endif>}<p>
        would fail and not render any row object. (Since the <i>map</i> {@code mySheet} is
        empty.) Using the flag {@code exists} the template can be formulated meaningful and
        safe:<p>
          {@code <if(myBook.mySheet.exists)><myBook.mySheet.rowAry:renderRow()><endif>} */
    public final boolean exists = true;
        
    /** A set of all the names of pseudo-fields supports the implementation of the lookup
        operation. The names from this base class are contained as well as those from all
        derived classes. */
    private final Set<String> pseudoFieldNameSet_;
    

    /**
     * Create a new map object.
     *   @param errCnt
     * A client supplied error counter. The use case is to permit consecutive error
     * counting across different phases of parsing and rendering.
     *   @param logContext
     * A string used to precede all logging statements of this module. Pass null if not
     * needed.
     *   @param name
     * The name of the new object as an Identifier.
     *   @param listOfPseudoFields
     * A list of String objects, which designate the pseudo field names from all derived
     * classes. If the map is queried for an element with a name found in this list then the
     * derived will have to return the according value from its overloaded method {@link
     * #getPseudoField}.
     */
    protected ObjectMap( ErrorCounter errCnt
                       , String logContext
                       , Identifier name
                       , List<String> listOfPseudoFields
                       )
    {
        assert errCnt != null  &&  name != null  &&  listOfPseudoFields != null;

        /* Add all base class' pseudo field enumeration values as String to the set. */
        pseudoFieldNameSet_ = new HashSet<String>();
        for(PseudoFieldName fName: PseudoFieldName.values())
            pseudoFieldNameSet_.add(fName.name());
        /* Add all pseudo fields from the derived classes. */
        final int noBaseClassPseudoFields = pseudoFieldNameSet_.size();
        pseudoFieldNameSet_.addAll(listOfPseudoFields);
        
        /* The next statement can be uncommented for development support. The concatenated
           list of pseudo-fields of all sub-classes is printed. */
        //_logger.debug( "ObjectMap<{}>: listOfPseudoFields={}"
        //             , getClass().getName()
        //             , pseudoFieldNameSet_
        //             );

        /* We check by assertion that all fields have been added. In case of name clashes
           it could not be the case. This is a static implementation error and an assertion
           is appropriate to report. */
        assert pseudoFieldNameSet_.size()
               == noBaseClassPseudoFields + listOfPseudoFields.size()
               : "Doubly defined pseudo field names found";
        
        assert errCnt != null  &&  name != null;
        errCnt_ = errCnt;
        objId = Identifier.getUniqueId();
        logCtx_ = logContext != null? logContext: "";
        itemMap = new TreeMap<String,T>();
        name_ = name;

    } /* End of ObjectMap<T> */



    /**
     * Set the name of the object.
     *   @param name
     * The (new) name of the object.
     */
    protected void setName(Identifier name)
        {name_ = name;}
        
    
    /**
     * Set the logging context string for future logging.
     *   @param logContext
     * The (new) context string, which prepends future log messages. Maybe null if not such
     * context string is wanted.
     */
    protected void setLogContext(String logContext)
        {logCtx_ = logContext != null? logContext: "";}

    

    /**
     * Set the index-in-collection.
     *   @param i0
     * The new value for the null based index of this object in an embedding collection.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void setIndexInCollection(int i0)
    {
        assert i0 >= 0;
        i0_ = i0;
        i_ = i0_+1;

    } /* End of setIndexInCollection */



    /**
     * Check a string if it designates a pseudo field of the map.
     *   @return Get the Boolean response.
     *   @param name The tested string.
     */
    private boolean isPseudoFieldName(String name)
    {
        return pseudoFieldNameSet_.contains(name);

    } /* End of isPseudoFieldName */




    /**
     * Get the value of a pseudo-field by name.
     *   @return
     * The value of the pseudo-field is returned typeless as Object.
     *   @param pseudoFieldName 
     * The value is refered to by its name. {@code pseudoFieldName} designates a valid
     * pseudo-field, either from this base class or from one of the derived classes.
     *   @remark
     * The base class implementation proves by assertion that the pseudo filed name is
     * valid. The overloaded implementation of the derived classes must not do so. They
     * should either return the queried value or call their super class and delegate the
     * query if they don't know the pseudo field name.
     */
    protected Object getPseudoField(String pseudoFieldName)
    {
        final Object value;
        switch(PseudoFieldName.valueOf(pseudoFieldName))
        {
        case name_:
            value = name_;
            break;

        case objId:
            value = Integer.valueOf(objId);
            break;

        case i0_:
            value = Integer.valueOf(i0_);
            break;

        case i_:
            value = Integer.valueOf(i_);
            break;

        case itemAry:
            value = itemAry;
            break;
            
        case itemMap:
            /* In the StringTemplate V4 context it's advantageous and common to handle
               empty sets rather by null than by an empty collection. */
            value = itemMap.size()>0? itemMap: null;
            break;
            
        case noItems:
            value = Integer.valueOf(getNoItems());
            break;

        case exists:
            value = Boolean.valueOf(exists);
            break;

        default:
            assert false
                 : "Inconsistency between switch here and method isPseudoFieldName."
                   + " Pseudo field name: " + pseudoFieldName;
            value = null;
        }
        
        return value;
        
    } /* End of getPseudoField */
    
    
    
    /**
     * Deriving a new Map class from AbstractMap requires at minimum overloading the
     * entrySet function. The StringTemplate V4 engine will call this method if the
     * template iterates through the map.<p>
     *   We overload this method to filter out the pseudo fields, we want to hide when an
     * iteration along all map entries takes place. Filtering the pseudo fields here is a
     * contradictory behavior with the overloaded get method, which will return the pseudo
     * fields. Theoretically, this contradiction could be recognized by the StringTemplate
     * V4 engine but by experience it doesn't complain about.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    @Override public Set<Map.Entry<Object,Object>> entrySet()
    {
        /* Return a copy of the entry set from the embedded map, which only contains the
           real data items. We use a linked HashSet for the copy in order to retain the
           sort order of the TreeMap itemMap.
             The copy is required to adapt the different types of the values.
             The returned set is a fresh copy, which breaks the Map interface in that it
           doesn't implement the synchronization with the map contents. However, this
           doesn't harm as the StringTemplate V4 engine will use the set as read only. It's
           not going to modify the map and no other party is going to do so while the
           engine has and uses the set. */
        final Set<Map.Entry<Object,Object>> objectSet =
                                                new LinkedHashSet<Map.Entry<Object,Object>>();
        for(Map.Entry<String,T> entry: itemMap.entrySet())
        {
            objectSet.add(new AbstractMap.SimpleEntry<Object,Object>( entry.getKey()
                                                                    , entry.getValue()
                                                                    )
                         );
        }

        _logger.debug( "{}ObjectMap.entrySet(): {} elements"
                     , logCtx_
                     , objectSet.size()
                     );

        return objectSet;

    } /* End entrySet */



    /**
     * Check for presence of a given key,value pair in the map.<p>
     *   The StringTemplate V4 engine will use this Map method prior to the query for the
     * value. We say "is available" to both, contained real data items and pseudo fields.
     * This is a contradictory behavior with the behavior of method entrySet, which won't
     * list the pseudo fields. Theoretically, the contradiction could be recognized by the
     * StringTemplate V4 engine but by experience it doesn't complain about.<p>
     *   There's a pit-fall: The template engine has a two step approach to find the map
     * entries. First, it tries to identify the entry by passing in the template attribute,
     * which is used in the template map operator {@code .()}. The attribute is represented
     * in the StringTemplate defined Java class; different template constructs will yield
     * different Java classes. If the engine gets a negative response then it tries again,
     * this time with the attribute rendered as text, as a Java String object. (If the
     * internal attribute representation already is a Java String then no second attempt is
     * made.)<p>
     *   For us, it's impossible to operate on the internal StringTemplate classes.
     * Therefore we reject all queries with key objects, which are not of Java class String
     * and wait for the second attempt.
     *   @return
     * {@code true} for contained real data items and pseudo fields if referenced to by
     * Java String keys, {@code false} otherwise.
     *   @param key
     * The key attribute from the template.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    @Override public boolean containsKey(Object key)
    {
        final boolean containsIt;

        /* if instanceof: The delegation of the query to the embedded map would not require
           the type condition String (the map only contains String objects and will reject
           all other stuff) but we need the type condition anyway for entry in method
           isPseudoFieldName. */
        if(key instanceof String)
        {
            final String keyString = (String)key;

            containsIt = isPseudoFieldName(keyString)
                         ||  itemMap.containsKey(keyString);
        }
        else
            containsIt = false;

        _logger.debug( "{}ObjectMap.containsKey({}) = {}"
                     , logCtx_
                     , key
                     , containsIt
                     );
        return containsIt;

    } /* End of containsKey */



    /**
     * Get an object from the Map as it is seen by the StringTemplate V4 engine. The object
     * can either be a pseudo-field or a real data item.
     *   @return Get the object or null if no such object exists.
     *   @param key
     * The key, which the demanded object is associated with. In practice and due to the
     * behavior of the StringTemplate V4 engine this will always be a Java String. This
     * type is ensured by assertion. See method {@link #containsKey}, too.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    @Override public Object get(Object key)
    {
        final Object value;
        if(key instanceof String)
        {
            final String keyString = (String)key;
            _logger.debug("{}ObjectMap.get({})", logCtx_, keyString);

            if(isPseudoFieldName(keyString))
                value = getPseudoField(keyString);
            else
                value = itemMap.get(keyString);

            /* Uncomment the next logging line only temporarily for debugging purpose. It'll
               produce much too much output in general. */
            //_logger.debug("{}ObjectMap.get({}) = {}", logCtx_, keyString, value!=null? value.toString(): "null");
        }
        else
        {
            assert false: "Expect solely keys of type String";
            value = null;
        }

        return value;

    } /* End of get */



    /**
     * Adding a real data item to this map object must only be done using {@link
     * #putItem}. Java's {@link Map#put} is overridden only to double-check this by
     * assertion.
     *   @return Always returns null. But will always fire an assertion before.
     *   @param key
     * The name of the object.
     *   @param value
     * The added object of template type {@code <T>}.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    @Override public Object put(Object key, Object value)
    {
        /* This method must never be called. */
        assert false;
        return null;

    } /* End of put */




    /**
     * Add a real data item to the map.
     *   @return Get the Boolean information whether the object could be added to the
     * map. Adding can fail if the object's name clashes with a reserved keyword or an
     * already contained object.
     *   @param objectName
     * This is the name under which the object is stored in the map. Must not be null.
     *   @param object
     * The object to add. Must not be null.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public boolean putItem(String objectName, T object)
    {
        assert object != null  &&  objectName != null;

        /* Check if the object has the name of a reserved keyword (the name of a pseudo
           field). */
        if(isPseudoFieldName(objectName))
        {
            errCnt_.error();
            _logger.error( "{}Object name {} conflicts with a reserved"
                           + " keyword. Please use another object name"
                         , logCtx_
                         , objectName
                         );
            return false;
        }

        /* Check if value is already contained. */
        if(itemMap.containsKey(objectName))
        {
            errCnt_.error();
            _logger.error( "{}Object name {} is ambiguous. Objects with same parent object"
                           + " must not share the same name"
                         , logCtx_
                         , objectName
                         );
            return false;
        }

        _logger.debug( "{}Object {} is added to ObjectMap {}"
                     , logCtx_
                     , objectName
                     , name_
                     );

        /* Add the sub-container/object to the map to enable retrieval by name. */
        itemMap.put(objectName, object);

        /* Add the sub-container/item to the list for retrieval by iteration. */
        if(itemAry == null)
            itemAry = new ObjectList<T>(name_);
        itemAry.add(object);

        return true;

    } /* End of putItem */



    /**
     * Get a real data item from the map. The operation basically is the get method of the
     * superclass but it makes the distinction between the true container objects and the
     * pseudo-fields.
     *   @return Get either the requested data item of template type T or null if no such
     * object exists in the map. This variant of the get method will never return a pseudo
     * fields of this.
     *   @param name
     * The name of the wanted real data item. An error is reported if {@code name}
     * designates a pseudo-field.
     */
    protected T getItem(String name)
    {
        /* Query the embedded map for the object name assuming that it is a true data item
           but not a pseudo field. */
        T object = itemMap.get(name);
        _logger.debug( "{}ObjectMap.getItem({}): itemMap.get returned {}"
                     , logCtx_
                     , name
                     , object != null? object.toString(): "null"
                     );

        /* Do error reporting if a pseudo field should have been referenced. */
        if(object == null  &&  isPseudoFieldName(name))
        {
            errCnt_.error();
            _logger.error( "{}Object {} has been referenced. This object name conflicts"
                           + " with a reserved keyword. Mostly, object names are taken"
                           + " from the column titles or from the cells in the columns,"
                           + " which are designated as path elements. Please check these"
                           + " elements in your input"
                         , logCtx_
                         , name
                         );
        }
        else assert !isPseudoFieldName(name);

        return object;

    } /* End of getItem */



    /**
     * A comparator for sorting containers in a Java List is created and returned.
     *   @return Get the temporary comparator object as usable with the Java Collections
     * class.
     *   @param <T>
     * Type of the container elements, which are to be compared.
     *   @param sortOrder
     * The wanted sort order. Sorting is done with respect to the name {@link #name_} of
     * the container. Sorting is done based on {@link SortOrder#createComparatorString};
     * refer to this method to get details about the behavior of sorting.
     */
    public static <T> SortOrder.Comparator<ObjectMap<T>>
                                         createComparator(final SortOrder.Order sortOrder)
    {
        return new SortOrder.Comparator<ObjectMap<T>>()
                    {
                        private final Comparator<String> comparator =
                                               SortOrder.createComparatorString(sortOrder);

                        public int compare(ObjectMap<T> a, ObjectMap<T> b)
                        {
                            final String sortNameA = a.name_.givenName
                                       , sortNameB = b.name_.givenName;
                            return comparator.compare(sortNameA, sortNameB);
                            
                        } /* End of compare */

                        public SortOrder.Order getSortOrder()
                            {return sortOrder;}
                        
                    }; /* End anonymous comparator class */
                    
    } /* End of createComparator */
    
    
    
    
    /**
     * Get the string representation of the map object; it's its name as an identifier.
     * From a StringTemplate V4 template this representation of the object is not
     * accessible. Here you need to reference the object's name, e.g. {@code <obj.name_>}.
     *   @return
     * Get the string value.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    @Override public String toString()
    {
        return name_.toString();

    } /* End of toString */


} /* End of class ObjectMap definition. */





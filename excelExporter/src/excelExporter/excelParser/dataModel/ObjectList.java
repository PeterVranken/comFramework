/**
 * @file ObjectList.java
 * This class is a sorted ArrayList of data model objects, e.g. applied in a container of
 * row objects.
 *
 * Copyright (C) 2015-2016 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class ObjectList
 *   ObjectList
 *   add
 *   sort
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.SortOrder;
import excelExporter.excelParser.ErrorCounter;


/**
 * A list of objects in the data model. Applied to the storage of row objects and groups of
 * such in containers and of the containers themselves.
 *   @param <T>
 * The class of the objects in the list or container.
 */

public class ObjectList<T> extends ArrayList<T>
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(ObjectList.class);

    /** The ID of this object. Each object in the data model gets a unique ID, which can be
        useful for having related data objects in the generated code with individual
        names. */
    public final int objId;

    /** The parent of the list of data model objects is the name of the the list containing
        group. Where group can refer to an Excel input defined group of row objects or to a
        global group of worksheets. If the list is the root collection, then the name is
        the name of the Excel worksheet. */
    public final Identifier parent;

    /** The chosen sort order of the list. */
    public SortOrder.Order sortOrder = SortOrder.Order.undefined;


    /**
     * Constructor for a new list.
     *   @param parent The name of the containing group.
     */
    public ObjectList(Identifier parent)
    {
        objId = Identifier.getUniqueId();
        this.parent = parent;

    } /* End of ObjectList<T> */



    /** 
     * The number of contained data items. The number is accessible from a StringTemplate
     * V4 template with an expression like {@code <list.noItems>}.
     *   @return Get the number if items in the list. Same as {@link ArrayList#size}.
     */
    public int getNoItems()
    {
        return size();

    } /* End of getNoItems */
    
    

    /**
     * Add an element to the list. This method behaves like the superclass method besides
     * that it invalidates the sort order recorded for this list.
     * {@inheritDoc}
     */
    @Override public boolean add(T obj)
    {
        sortOrder = SortOrder.Order.undefined;
        return super.add(obj);

    } /* End of add */



    /**
     * Add an element to the list. This method behaves like the superclass method besides
     * that it invalidates the sort order recorded for this list.
     * {@inheritDoc}
     */
    @Override public void add(int idx, T obj)
    {
        sortOrder = SortOrder.Order.undefined;
        super.add(idx, obj);

    } /* End of add */



    /**
     * Sort the list with help of an external comparator.
     *   @param comparator The externally provided comparator, suitable to compare two list
     * elements.
     */
    public void sort(SortOrder.Comparator<? super T> comparator)
    {
        Collections.sort(this, comparator);
        this.sortOrder = comparator.getSortOrder();
        
/// @todo Update of the index-in-collection could be implemented here if we define an
// interface for setIndexInCollection. However, does this make sense? We don't want to
// always do it, since the index relates to the "most important" collection only?
//        /* The index in the list needs to be reset according to the new order. */
//        int i = 0;
//        for(T listElem: this)
//            listElem.setIndexInCollection(i++);
            
    } /* End of sort */
    
    
    /**
     * Get the string representation of the list; it's its parent's name. From a
     * StringTemplate V4 template this representation of the object is accessed as
     * {@code <objectList>}.
     *   @return The string value
     */
    @Override public String toString()
    {
        return parent.toString();

    } /* End of toString */


} /* End of class ObjectList definition. */





/**
 * @file RowObjectContainer.java
 * This is the recursive data structure, which holds the tree structure of row objects. An
 * Excel worksheet basically is one such container.
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
/* Interface of class RowObjectContainer
 *   addLists
 *   RowObjectContainer
 *   getPseudoField
 *   putGroup
 *   addRow
 *   setSortOrder
 *   addRowWithPath
 *   getSortOrderSubGroups
 *   createComparatorSubGroups
 *   sort
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.*;



/**
 * This is the recursive data structure, which holds the tree structure of groups or
 * containers of row objects. An Excel worksheet is one such container.<p>
 *   Formally, a {@link RowObjectContainer} object is a Java Map, which contains other
 * {@link RowObjectContainer} objects, looked up by name. The map entries are the nested
 * groups of row objects, which can thus be accessed directly by name from a StringTemplate
 * V4 template and by means of the dot notation. If the worksheet {@code ws} defines the
 * groups {@code A} and {@code B} on root level then the template can address to these
 * groups by {@code <ws.A>} and {@code <ws.B>}. If {@code C} was a sub-group of  {@code A}
 * then then the template can address to {@code C} by an expression like {@code
 * <ws.A.C>}<p>
 *   The map entries are sorted in case-sensitive lexical order. If in a StringTemplate V4
 * template the map iteration operator is applied to a group object then it delivers the
 * keys (i.e. group names) in lexical order. This order is hard coded and can't be
 * influenced by command line arguments. Map iteration could look as follows in a template:<p>
 *   {@code All sub groups of worksheet: <ws:{key|<\n> group name: <key>}><\n>}<p>
 * In the example before, the produced output should list {@code A} and {@code B}, maybe
 * among more sub-groups of the worksheet.<p>
 *   The {@link RowObjectContainer} object provides more information to the rendering
 * process. The map of row object groups is extended by some so-called "pseudo-fields",
 * which can be accessed from a template by means of the same dot notation. As an example,
 * the group/container has a name and this name can be accessed by the template expression
 * {@code <group.name_>}; where {@code name_} denotes a pseudo-field. (It's impossible to
 * name a group of rows "name_", this is a reserved keyword.)<p>
 *   The list of all supported pseudo-fields is documented as enumeration {@link
 * RowObjectContainer.PseudoFieldName}.<p>
 *   {@link RowObjectContainer} is derived from its base class and the base class'
 * pseudo-fields are inherited and can be accessed in the same way. They are documented as
 * enumeration {@link ObjectMap.PseudoFieldName}.<p>
 *   There are two (sorted) list objects which give access to the row objects of the given
 * group (the leafs of the data tree) and to its nested sub-groups. The sort order is
 * controlled from the application command line. The first pseudo-field {@link
 * RowObjectContainer.PseudoFieldName#rowAry} permits an iteration of the row objects owned
 * by the given group.<p>
 *   Extending the previous example, the root level row objects of the worksheet would be
 * visisted by {@code <ws.rowAry:{row|<renderRowObject(row)>}>}. The iteration of all row
 * objects owned by group {@code A} would look like {@code
 * <ws.A.rowAry:{row|<renderRowObject(row)>}>}.<p>
 *   The second additional field {@link RowObjectContainer.PseudoFieldName#groupAry}
 * permits an iteration of the sub-groups of the group in controlled sort order. Different
 * to the example above, which applied the map iteration of the StringTemplate V4 engine,
 * will the iteration of this list object directly visit the {@link RowObjectContainer}
 * objects that represent the sub-groups. Putting it all together will we get a complete
 * recursive iteration of the worksheet by a template fragment like:<p>
 *   {@code <renderGroup(workSheet)>}<p>
 *   {@code renderGroup(g) ::= "<g.rowAry:{row|<renderRowObject(row)>}>
 * <g.groupAry:{subGroup|<renderGroup(subGroup)>}>"}
 */
public class RowObjectContainer extends ObjectMap<RowObjectContainer>
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(RowObjectContainer.class);

    /** This is the list of pseudo-fields of class {@link RowObjectContainer} that can be
        accessed from a StringTemplate V4 template. The enumerated fields can be accessed
        like ordinary public fields from a StringTemplate V4 template. The dot notation is
        used to do so. Taking the first named value {@link PseudoFieldName#rowAry} as an
        example, one would write {@code <container.rowAry>} to access the related
        pseudo-field.<p>
          A short explanation of each pseudo-field and a reference to more in-depth
        information can be found as description of each named value of the enumeration.<p>
          Please note, that the pseudo-fields of the base class {@link ObjectMap} are
        inherited and can be accessed, too. See {@link ObjectMap.PseudoFieldName}.<p>
          A detailed discussion of pseudo-fields and the enumeration of these can be found
        in the class description of {@link ObjectMap}. */
    public static enum PseudoFieldName
    {
        /** The list {@link RowObjectContainer#rowAry} of row objects in the
            group/container.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <container.rowAry>}. */
        rowAry, 
        
        /** The number {@link RowObjectContainer#getNoRows} of row objects in the
            group/container.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <container.noRows>}. */
        noRows,
        
        /** If {@link RowObjectContainer#getNoRows} returns one then this pseudo-field
            {@link RowObjectContainer#prop} holds a reference to the one and only row
            object in {@link RowObjectContainer#rowAry}. If the container has any other
            number of row objects then the reference {@link RowObjectContainer#prop} is
            null.<p>
              From a StringTemplate V4 template the only row object would be accessed with
            an expression like {@code <container.prop>}. */
        prop,

        /** The list {@link ObjectMap#itemAry} of sub-group/container objects of the given
            container. This pseudo-field is an alias to access the pseudo-field {@link
            ObjectMap.PseudoFieldName#itemAry} of the base class.<p>
              From a StringTemplate V4 template the list would be accessed with an
            expression like {@code <container.groupAry>}. */
        groupAry,
        
        /** The number of sub-groups/containers in the given group/container. This
            pseudo-field is an alias to access the pseudo-field {@link
            ObjectMap.PseudoFieldName#noItems}.<p>
              From a StringTemplate V4 template the number would be accessed with an
            expression like {@code <container.noGroups>}. */
        noGroups,
    };

    /** A list of all the names of pseudo-fields supports the implementation of the lookup
        operation. */
    private final static List<String> _pseudoFieldNameList;
    static 
    {
        /* Add all enumeration values as String to the list. */
        _pseudoFieldNameList = new ArrayList<String>();
        for(PseudoFieldName name: PseudoFieldName.values())
            _pseudoFieldNameList.add(name.name());
    }
    
    
    /** The list of leaf objects of the tree of row objects and groups of such. Each row of
        the Excel worksheet is considered one data object, mostly refered to as a "row
        object". It is placed in the root container (the worksheet object) or in one of its
        groups or nested sub-groups. The collection {@link #rowAry} holds all row objects,
        which belong into the group represented by this {@link RowObjectContainer}
        object.<p>
          The order of row objects in the list is controlled by the application command
        line arguments {@code sort-order-of-column} and {@code sort-priority-of-column}.
        Please note, that several instances of these arguments may all affect the sort
        order. */
    public ObjectList<RowObject> rowAry = null;

    /** The number of row objects in {@link #rowAry}. From a StringTemplate V4 template
        this member is accessed as {@code <group.noRows>}.
          @return Get the number of rows. */
    public int getNoRows()
        {return rowAry != null? rowAry.size(): 0;}

    /** A reference to the one and only row object in {@link #rowAry}.<p>
          A typical use case of grouping is the implementation of direct lookup of row
        objects by name. The group has the name of the row object and the group will
        contain one and only one row object. In this situation some typical template code
        could look like:<p>
          {@code <first(mySheet.(myRowObjsName).rowAry).myRowObjsProp>}<p>
        This example assumes that {@code mySheet} is a worksheet object, which has a group
        for each row object, which direct look up is implemented for. The name of the
        particular row object of interest is held in attribute {@code myRowObjsName} and the
        requested property of that row object is (literally) {@code myRowObjsProp}. Using
        this field here, {@code prop}, the same result would be achieved with:<p>
          {@code <mySheet.(myRowObjsName).prop.myRowObjsProp>}<p>
        which is much more to the point and a bit shorter.<p>
          The name of this field has been chosen to support the use-case: From the
        perspective of the Excel spreadsheet maintenance the group actually isn't a group
        but rather a row object (that can be accessed by name). Now {@code .prop}
        references its properties and {@code .myRowObjsProp} selects a particular
        one out of them.<p>
          {@code prop} is null if there are no or more than one row objects in the group.
        (This field is designed only to support the outline use-case.) Having null for
        {@code prop} implicitly makes the template code safer: The former but not the
        latter template example would require additional explicit code for error reporting
        if the input doesn't matches the assumed format, i.e. if there were more than a
        single row object in the group.<p>
          Note, the StringTemplate V4 engine will not make a difference between not having
        a single row object (and {@code prop} being null) or having a single row object
        ({@code prop} not being null) without any property; {@code
        <if(mySheet.(myRowObjsName).prop)>} will evaluate to the unmet condition in either
        case. For most template applications will this however be irrelevant as either
        situation will be considered an error. */
    public RowObject prop = null;

    /** The user-demanded sort order for this group with respect to its siblings in the
        same parent group. */
    private SortOrder.Order sortOrder_ = SortOrder.Order.undefined;

    /** The null based index of the column of the worksheet, which had initially let to the
        creation of this group and which had initially defined its properties. Or -1 if
        this is the root level group, which represents the whole worksheet.<p>
          The index is needed for feedback in case references to this group from different
        columns make differing property specifications. */
    private int idxColWorksheet_ = -1;


    /**
     * A helper method: The first statement of a constructor needs to be the super call but
     * to satisfy this call we need to the concatenated lists of pseudo-fields. If we have
     * this concatenation as an expression it can be done in the argument list of super(),
     * which can thus stay the first statement. This method implements a list
     * concatenation. A local lambda expression is likely an alternative.
     *   @return
     * Get the concatenated lists. A shallow copy of the two operands is returned, i.e. a
     * new list object with identical element objects.
     *   @param a
     * The first list.
     *   @param b
     * The second list.
     */     
    private static List<String> addLists(List<String> a, List<String> b)
    {
        final ArrayList<String> c = new ArrayList<>(a);
        c.addAll(b);
        return c;
    }
    
    
    
    /**
     * Create a new container object.
     *   @param errCnt
     * A client supplied error counter. The use case is to permit consecutive error
     * counting across different phases of parsing.
     *   @param logContext
     * A string used to precede all logging statements of this module. Pass null if not
     * needed.
     *   @param name
     * The container represents a group of row objects. This is the name of the group.
     *   @param idxColWorksheet
     * The null based index of the column of the input worksheet, which initially specifies
     * this group and its properties.<p>
     *    Pass pseudo index -1 for the root container, which represents the whole
     * worksheet.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public RowObjectContainer( ErrorCounter errCnt
                             , String logContext
                             , Identifier name
                             , int idxColWorksheet
                             )
    {
        super( errCnt
             , logContext
             , name
             , _pseudoFieldNameList
             );
        idxColWorksheet_ = idxColWorksheet;

    } /* End of RowObjectContainer */


        
    /**
     * Create a new container object. This variant is required by sub-classes: An
     * additional argument permits the sub-class to define additional pseudo-fields.
     *   @param errCnt
     * A client supplied error counter. The use case is to permit consecutive error
     * counting across different phases of parsing.
     *   @param logContext
     * A string used to precede all logging statements of this module. Pass null if not
     * needed.
     *   @param name
     * The container represents a group of row objects. This is the name of the group.
     *   @param idxColWorksheet
     * The null based index of the column of the input worksheet, which initially specifies
     * this group and its properties.<p>
     *    Pass pseudo index -1 for the root container, which represents the whole
     * worksheet.
     *   @param listOfSubClassPseudoFields
     * A list of names of pseudo-fields. The constructor of a sub-class can pass the
     * additional pseudo-fields of the sub-class.
     */
    protected RowObjectContainer( ErrorCounter errCnt
                                , String logContext
                                , Identifier name
                                , int idxColWorksheet
                                , List<String> listOfSubClassPseudoFields
                                )
    {
        super( errCnt
             , logContext
             , name
             , addLists(_pseudoFieldNameList, listOfSubClassPseudoFields)
             );
        idxColWorksheet_ = idxColWorksheet;

    } /* End of RowObjectContainer */



    /**
     * Get the value of a pseudo-field by name.
     *   @return The value of the pseudo-field is returned typeless as Object.
     *   @param pseudoFieldName The value is refered to by its name.
     */
    @Override protected Object getPseudoField(String pseudoFieldName)
    {
        final Object value;
        switch(pseudoFieldName)
        {
        case "rowAry":
            value = rowAry;
            break;

        case "noRows":
            value = Integer.valueOf(getNoRows());
            break;

        case "prop":
            assert prop == null || rowAry.get(0) == prop; 
            value = prop;
            break;

        case "groupAry":
            value = itemAry;
            break;

        case "noGroups":
            value = Integer.valueOf(getNoItems());
            break;

        default:
            value = super.getPseudoField(pseudoFieldName);
        }
        
        return value;

    } /* End of getPseudoField */
    
    

    
    /**
     * Add a group object (i.e. a sub-container) to the map.
     *   @return
     * Get the Boolean information whether the group object could be added to the
     * map. Adding can fail if the group name clashes with a reserved keyword or an
     * already existing group.
     *   @param group
     * The object to add.
     *   @param idxRow
     * The null based row index; used for error reporting.
     *   @param idxCol
     * The null based column index; used for error reporting.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public boolean putGroup(RowObjectContainer group, int idxRow, int idxCol)
    {
        assert group != null  &&  group.name_ != null;
        final String groupName = group.name_.givenName;
        assert groupName != null;

        /* Add the group object to map and list. */
        if(!putItem(groupName, group))
            return false;
        group.setIndexInCollection(itemAry.size()-1);

        _logger.debug( "{}Row {}, column {}: Group {} is added to RowObjectContainer {}"
                     , logCtx_
                     , idxRow+1, idxCol+1
                     , groupName
                     , name_
                     );

        return true;

    } /* End of putGroup */



    /**
     * Add a row object to the list of those.
     *   @param row
     * The object to add.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void addRow(RowObject row)
    {
        assert row != null;

        /* If this is the first row then create the array. (An empty array is expressed by
           null for sake of easy template writing.) */
        if(rowAry == null)
        {
            rowAry = new ObjectList<RowObject>(name_);
            
            /* This is the first added row object thus the only one (so far). We hold it
               not only in the list but also in field prop. */
            assert prop == null;
            prop = row;
        }
        else
        {   
            /* Field prop must only support the special (but common) case of a row object
               container having an only row object. Discard the reference to the only row
               object if there are more. */
            prop = null;
        }

        /* Add the row object to the list for retrieval by iteration. Update the index into
           the collection. It relates to the list view of the row objects in this
           container. */
        row.setIndexInCollection(rowAry.size());
        rowAry.add(row);

    } /* End of addRow */



    /**
     * Set the sort order for this group. Sorting relates to this group among its siblings
     * inside their common parent.<p>
     *   The first call of this method will set the sort order. All later calls will
     * double-check if the sort order passed in is identical to the one set in the first
     * call. The intention is to call this method on all references to the group and to do
     * the validation that all references use a consistent sort specification. (Differing
     * specifications can appear if references origin from different columns of the
     * worksheet; sort specifications are made per column.) An inconsistency is reported as
     * error.
     *   @param sortOrder
     * The demanded sort order.
     *   @param idxCol
     * The null based index of the column of the input worksheet, which the sort
     * specification results from. Needed for consistency check only: A group is normally
     * created by a cell entry in a certain column and the sorting properties to be applied
     * to a group are specified in relation to a column. However, the same group can be
     * referenced from a cell located in another column and sorting can be done only if the
     * sorting specification of this column is not contradictory.
     *   @remark
     * This method must not be called for the root container, which represents the whole
     * worksheet and which doesn't have any siblings.
     */
    private void setSortOrder(SortOrder.Order sortOrder, int idxCol)
    {
        assert idxCol >= 0;
        if(idxCol == idxColWorksheet_)
        {
            /* The sort specification origins from the column the group was created from.
               There's implicitly no inconsistency and we take the settings. */
            sortOrder_ = sortOrder;
        }
        else
        {
            /* This time the sort demand origins from another column; this happens in the
               context of a reference to an already created group. We need to double-check
               for consistent settings. */
            if(sortOrder_ != sortOrder)
            {
                errCnt_.error();
                _logger.error( "{}Group {} has been referenced from worksheet column {}."
                               + " This column specifies sort order {}. However, the group"
                               + " had been created with sort order {} in worksheet column"
                               + " {}. If the same group is referenced from different columns"
                               + " then both columns need to have consistent settings"
                             , logCtx_
                             , toString()
                             , idxCol+1
                             , sortOrder
                             , sortOrder_
                             , idxColWorksheet_+1
                             );
            }
        }
    } /* End of setSortOrder */



    /**
     * Add a row object to the list of those in the right group. The right group is
     * addressed by a path of groups and child-groups. A sub-set of the row object's
     * properties is specified to be path elements in a particular order. The values of
     * these properties specify groups and child groups until the targeted parent of the
     * row object is found.<p>
     *   The path matching starts with container this. The path therefore is a relative
     * path in general. Only if this is the root container (i.e. the container associated
     * with the worksheet) then the path is interpreted as an absolute path.<p>
     *   If the groups of the path don't exist yet then they are created as side effect of
     * the operation.
     *   @param row
     * The object to add.
     *   @param colTitleMgr
     * The column title manager by reference. Used to retrieve the information about the
     * grouping path scheme.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void addRowWithPath(RowObject row, ColumnTitleMgr colTitleMgr)
    {
        List<ColumnTitleMgr.ColAttribs> pathPropertyAry = colTitleMgr.getGroupingPathScheme();
        RowObjectContainer parent = this;
        if(pathPropertyAry != null)
        {
            for(ColumnTitleMgr.ColAttribs colAttribs: pathPropertyAry)
            {
                CellObject cell = row.getItem(colAttribs.title_);

                /* It's permitted to not use the path specifying columns in consecutive
                   order. There may be empty cells in between. These behave as if not part
                   of the path at all. */
                if(cell == null  ||  cell.type == CellObject.CellType.blank)
                    continue; // for(colAttribs)

                /* It's basically possible to use a type-casted Boolean or numeric cell as
                   path element designation, but it's likely a kind of problem. Emit a
                   warning. Or an error if type casting to text was not possible. */
                final String groupName = cell.text;
                if(cell.type != CellObject.CellType.text)
                {
                    if(groupName != null)
                    {
                        final Level level;
                        if(cell.type == CellObject.CellType.integer)
                        {
                            /* We have a lot of uses cases, where integers play the role of
                               enumerations and in this cases, it's not suspicious to use
                               an integer type column for grouping. We emit the message on
                               very low level only. */
                            level = Level.DEBUG;
                        }
                        else
                        {
                            level = Level.WARN;
                            errCnt_.warning();
                        }
                        _logger.log( level
                                   , "{}Cell ({},{}) is part of the group path but is of"
                                     + " unexpected type {}. Expect a cell of type {}"
                                   , logCtx_
                                   , cell.iRow, cell.iCol
                                   , cell.type
                                   , CellObject.CellType.text
                                   );
                    }
                    else
                    {
                        errCnt_.error();
                        _logger.error( "{}Cell ({},{}) is part of the group path but is of"
                                       + " unexpected type {} and can't be used"
                                       + " as path element. Please reformat the cell to"
                                       + " type {}"
                                     , logCtx_
                                     , cell.iRow, cell.iCol
                                     , cell.type
                                     , CellObject.CellType.text
                                     );
                    }
                }
                else assert groupName != null;

                /* This is an error but for now we continue as for the empty cell. */
                if(groupName == null)
                    continue; // for(colAttribs)

                /* Check if our current container has the required group. Shape it
                   otherwise. */
                RowObjectContainer group = parent.getItem(groupName);
                if(group == null)
                {
                    /* Create the sub-group object. */
                    RowObjectContainer newGroup = new RowObjectContainer
                                                            ( errCnt_
                                                            , logCtx_
                                                            , new Identifier(groupName)
                                                            , colAttribs.idx_
                                                            );

                    /* If we got null due to a clash with a reserved keyword then the
                       attempt to create the group will now double the error message. This
                       is acceptable as the message generated here will be more specific,
                       with better localization of the problem. */
                    if(parent.putGroup(newGroup, cell.i0Row, cell.i0Col))
                    {
                        _logger.debug( "{}Group {} has been created as child of parent"
                                       + " group {}"
                                     , logCtx_
                                     , newGroup.name_
                                     , parent.name_
                                     );
                        group = newGroup;
                    }
                }

                if(group != null)
                {
                    /* Set the desired sort order if it had been created now. Double check
                       for consistent sort settings otherwise, if the same group is visited
                       again. (Sorting itself will be done later, when the complete nested
                       data structure is built up.) */
                    group.setSortOrder(colAttribs.sortOrder_, colAttribs.idx_);

                    /* Do the recursion with the found or newly created and configured
                       group. */
                    parent = group;
                }
                else
                {
                    /* If group is a rejected new group object, then the path element is
                       ignored and the row object will be added a level upwards. This is
                       harmless insofar as all further parsing is anyway done only to give
                       more feedback on errors in the input. */
                }
            } /* End for(All path elements) */
        } /* End if(Grouping path specified?) */

        /* Now add the row object to the found or shaped parent group. */
        parent.addRow(row);

    } /* End of addRowWithPath */



    /**
     * Get the user-demanded sort order for the sub-groups of this container.<p>
     *   The sub-groups are usually defined in the same column of the worksheet but not
     * necessarily. Therefore, they may have differing sort settings. By side-effect this
     * method double-checks that all groups demand the same order. An error is reported if
     * not.
     *   @return
     * Get the sort order, which is to be applied to the set of sub-groups.
     *   @remark
     * This function must not be called for groups without any sub-groups.
     */
    private SortOrder.Order getSortOrderSubGroups()
    {
        assert itemAry != null  &&  itemAry.size() > 0;
        final SortOrder.Order sortOrder = itemAry.get(0).sortOrder_;
        for(RowObjectContainer subGroup: itemAry)
        {
            if(subGroup.sortOrder_ != sortOrder)
            {
                errCnt_.error();
                _logger.error( "{}The sub-groups {} and {} of group {} specify the deviating"
                               + " sort orders {} and {}, respectively. Sorting of"
                               + " sub-groups is undefined. The sort"
                               + " specification is taken from the column attribute"
                               + " specification. Please check if the involved sub-groups"
                               + " are defined in different columns of the worksheet. If so"
                               + " adjust the sort properties of these columns"
                             , logCtx_
                             , itemAry.get(0).toString()
                             , subGroup.toString()
                             , itemAry.get(0).sortOrder_
                             , subGroup.sortOrder_
                             , toString()
                             );
                return SortOrder.Order.undefined;
            }
        }

        return sortOrder;

    } /* End of getSortOrderSubGroups */



    /**
     * A comparator for sorting the sub-groups of this container is created and returned.
     *   @return Get the temporary comparator object as usable with the Java Collections
     * class.
     */
    private SortOrder.Comparator<RowObjectContainer> createComparatorSubGroups()
    {
        return new SortOrder.Comparator<RowObjectContainer>()
                    {
                        private final SortOrder.Comparator<String> comparator =
                                    SortOrder.createComparatorString(getSortOrderSubGroups());

                        public int compare( RowObjectContainer a
                                          , RowObjectContainer b
                                          )
                        {
                            final String sortNameA = a.name_.givenName
                                       , sortNameB = b.name_.givenName;
                            return comparator.compare(sortNameA, sortNameB);

                        } /* End of compare */

                        public SortOrder.Order getSortOrder()
                            {return comparator.getSortOrder();}

                    }; /* End anonymous comparator class */

    } /* End of createComparatorSubGroups */




    /**
     * Sort the contents of this container, sort the row objects and the sub-groups in this
     * container. This is done recursively for all sub-groups and their row-objects and
     * sub-groups.<p>
     *   This method should be called after complete build-up of the container.
     *   @param colTitleMgr
     * The column title manager by reference. Used to retrieve the information about the
     * column sort attributes.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void sort(ColumnTitleMgr colTitleMgr)
    {
        /* Sort the list of row objects of this container. */
        if(rowAry != null)
        {
            /* The column manager provides an ordered list of names of properties. The row
               objects need to be sorted according to the values of the listed properties.
               By repeatedly sorting the array of row objects we implement the priority of
               sort columns. The column/property of highest priority is used as last and
               determines the principal sort order. */
            List<ColumnTitleMgr.ColAttribs> sortedColAry =
                                                        colTitleMgr.getPropertySortingScheme();
            if(sortedColAry!= null)
            {
                for(ColumnTitleMgr.ColAttribs sortedCol: sortedColAry)
                {
                    _logger.debug( "{}Sort row objects of group {}; apply sort order {} to"
                                   + " property {}"
                                 , logCtx_
                                 , toString()
                                 , sortedCol.sortOrder_
                                 , sortedCol.title_
                                 );
                    rowAry.sort( RowObject.createComparator
                                               ( sortedCol.sortOrder_
                                               , /* propName */ sortedCol.title_
                                               )
                               );
                }
            }

            /* The index in the list needs to be reset according to the new, final order. */
            int i = 0;
            for(RowObject rowObj: rowAry)
                rowObj.setIndexInCollection(i++);
        }

        /* Sort the sub-groups of this container in the linked list of those. */
        if(itemAry != null)
        {
            final SortOrder.Order sortOrder = getSortOrderSubGroups();
            if(sortOrder != SortOrder.Order.undefined)
            {
                _logger.debug( "{}Apply sort order {} to sub-groups of group {}"
                             , logCtx_
                             , sortOrder
                             , toString()
                             );
                itemAry.sort(createComparatorSubGroups());

                /* The index in the list needs to be reset according to the new order. */
                int i = 0;
                for(RowObjectContainer group: itemAry)
                    group.setIndexInCollection(i++);
            }

            /* Do the recursion for all sub-groups of this container. */
            for(RowObjectContainer group: itemAry)
                group.sort(colTitleMgr);
        }
    } /* End of sort */

} /* End of class RowObjectContainer definition. */





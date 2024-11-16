/**
 * @file Cluster.java
 * This file defines the principal data structure: The structure of the object that is
 * passed to the StringTemplate V4 engine for rendering the information in the different
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
 *   Cluster
 *   getPseudoField
 *   putBook
 *   reportDirectDataAccess
 *   sort
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.ErrorCounter;
import excelExporter.excelParser.SortOrder;


/**
 * The root of all parsed and rendered information.<p>
 *   The "data model" is the Java representation of the Excel input, the parse tree with
 * other words. The "Cluster" is the root of the data model and information rendering in a
 * StringTemplate V4 template will always begin with the one and only object of this
 * class.<p>
 *   An object of this class contains the results of parsing the input files, completed by
 * some configuration data. The object is passed to the template engine for rendering the
 * information in the wanted format.<p>
 *   Formally, a {@link Cluster} object is a Java Map, which contains Excel workbooks,
 * looked up by name. The name of a workbook is subject to the application configuration.
 * It might be the Excel file name but this is not a must and not even common practice. If
 * the application has parsed two Excel workbooks "myBookA" and "myBookB", then these
 * workbooks would be accessed from a StringTemplate V4 template with an expression like
 * {@code <cluster.myBookA>} or {@code <cluster.myBookB>}.<p>
 *   The cluster provides more information to the rendering process. The map of workbooks
 * is extended by some so-called "pseudo-fields", which can be accessed from a template by
 * means of the dot notation. As an example, the cluster has a name and this name can be
 * accessed by the template expression {@code <cluster.name_>}; where {@code name_} denotes
 * a pseudo-field. (It's impossible to name a workbook "name_", this is a reserved
 * keyword.)<p>
 *   The list of all supported pseudo-fields is documented as enumeration  {@link
 * Cluster.PseudoFieldName}.<p>
 *   {@link Cluster} is derived from its base class and the base class' pseudo-fields are
 * inherited and can be accessed in the same way. They are documented as enumeration  {@link
 * ObjectMap.PseudoFieldName}.
 */
public class Cluster extends ObjectMap<ExcelWorkbook>
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(Cluster.class);

    /** This is the list of pseudo-fields of class {@link Cluster} that can be accessed
        from a StringTemplate V4 template. The enumerated fields can be accessed like
        ordinary public fields from a StringTemplate V4 template. The dot notation is used
        to do so. Taking the first named value {@link PseudoFieldName#theOnlyWorkbook} as
        an example, one would write {@code <cluster.theOnlyWorkbook>} to access the related
        pseudo-field.<p>
          A short explanation of each pseudo-field and a reference to more in-depth
        information can be found as description of each named value of the enumeration.<p>
          Please note, that the pseudo-fields of the base class {@link ObjectMap} are
        inherited and can be accessed, too. See {@link ObjectMap.PseudoFieldName}.<p>
          A detailed discussion of pseudo-fields and the enumeration of these can be found
        in the class description of {@link ObjectMap}. */
    public static enum PseudoFieldName
    {
        /** The only parsed workbook {@link Cluster#theOnlyWorkbook}, if unambiguously
            possible.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <cluster.theOnlyWorkbook>}. */
        theOnlyWorkbook,

        /** The only parsed worksheet {@link Cluster#theOnlyWorksheet}, if unambiguously
            possible.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <cluster.theOnlyWorksheet>}. */
        theOnlyWorksheet,

        /** The list of all parsed workbooks. This pseudo-field is an alias to access the
            pseudo-field {@link ObjectMap.PseudoFieldName#itemAry} of the base class.<p>
              From a StringTemplate V4 template the list would be accessed with an expression
            like {@code <cluster.bookAry>}. */
        bookAry,

        /** The number of parsed workbooks in the cluster. This pseudo-field is an alias to
            access the pseudo-field {@link ObjectMap.PseudoFieldName#noItems}.<p>
              The number of books relates to both collections, {@link ObjectMap#itemAry}
            and {@link ObjectMap#itemMap}.<p>
              From a StringTemplate V4 template the number would be accessed with an
            expression like {@code <cluster.noBooks>}. */
        noBooks,

        /** The parsed worksheets can be kept in global groups of those (and regardless of
            their relationship to workbooks). Here's the map {@link Cluster#sheetGroupMap}
            of these groups and lookup is done by the name of the group.<p>
              From a StringTemplate V4 template the map of groups would be accessed with an
            expression like {@code <cluster.sheetGroupMap>}. */
        sheetGroupMap,

        /** The number {@link Cluster#getNoSheetGroups} of named worksheet groups in
            {@link Cluster#sheetGroupMap}.<p>
              From a StringTemplate V4 template the number of groups would be accessed with
            an expression like {@code <cluster.noSheetGroups>}. */
        noSheetGroups,

        /** The total number {@link Cluster#noSheets} of parsed worksheets.<p>
              From a StringTemplate V4 template the number of sheets would be accessed with
            an expression like {@code <cluster.noSheets>}. */
        noSheets,
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
    

    /** To simplify template writing in a special but quite common case the data model
        offers a direct reference to the workbook. This will work only if only a single
        workbook is parsed in total. If no or more than one workbook is parsed this
        reference is set to null in order to avoid unrecognized problems due to
        ambiguities.<p>
          How this field is set, how it relates to parsed workbooks is written to the
        application log on logging level INFO and more in detail on logging level DEBUG.<p>
          This field can be available even if the other field {@link #theOnlyWorksheet} is
        null. */
    public ExcelWorkbook theOnlyWorkbook = null;
    
    /** To simplify template writing in a special but quite common case the data model
        offers a direct reference to the worksheet. This will work only if a single
        worksheet is parsed in total. In all other cases this reference is set to null in
        order to avoid unrecognized problems due to ambiguities.<p>
          How this field is set, how it relates to parsed worksheets is written to the
        application log on logging level INFO and more in detail on logging level DEBUG.<p>
          If this field is available (not null) then the other field {@link
        #theOnlyWorkbook} will be available, too. */
    public ExcelWorksheet theOnlyWorksheet = null;
    
    /** The worksheets of all the workbooks are held a second time in a map of global
        worksheet groups. The names of the groups and the assignment of worksheets into
        these groups is user-specified by means of application configuration.<p>
          The use case is to order the worksheets of inhomogenous workbooks into homogenous
        groups of identically structured worksheets. Using the map of groups one can iterate
        along all worksheets of same structure.<p>
          This field is null if no global sheet groups are defined. */
    public Map< /* groupName */ String
              , /* group */     ObjectList<ExcelWorksheet>
              > sheetGroupMap = null;
           
    /** The number of global groups of worksheets in {@link #sheetGroupMap}. From a
        StringTemplate V4 template this member is accessed as {@code
        <cluster.noSheetGroups>}.
          @return Get the number of groups of worksheets. */
    public int getNoSheetGroups()
        { return sheetGroupMap != null? sheetGroupMap.size(): 0; }

    /** The total number of parsed sheets, regardless of how this relates to different
        books and sheet groups. Please note, the number of references to parsed sheets will
        in general be greater since one and the same sheet can be held in more than one
        collection. */
    public int noSheets = 0;


    /**
     * Create a new cluster object.
     *   @param errCnt
     * A client supplied error counter. The use case is to permit consecutive error
     * counting across different phases of processing.
     *   @param name
     * The user chosen name of the data cluster.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public Cluster(ErrorCounter errCnt, String name)
    {
        super( errCnt
             , /* logContext */ "Cluster" + (name != null? " "+name: "") + ": "
             , new Identifier(name != null? name: "Cluster")
             , _pseudoFieldNameList
             );
        noSheets = 0;

    } /* End of Cluster */
    

    /**
     * Get the value of a pseudo-field by name.
     *   @return
     * The value of the pseudo-field is returned typeless as Object.
     *   @param pseudoFieldName 
     * The value is refered to by its name. {@code pseudoFieldName} designates a valid
     * pseudo-field, either from this base class or from one of the derived classes.
     *   @remark
     * The base class implementation proves by assertion that the pseudo-field name is
     * valid. The overloaded implementation of the derived classes must not do so. They
     * should either return the queried value or call their super class and delegate the
     * query if they don't recognize the pseudo field name.
     */
    @Override protected Object getPseudoField(String pseudoFieldName)
    {
        final Object value;
        switch(pseudoFieldName)
        {
        case "theOnlyWorkbook":
            value = theOnlyWorkbook;
            break;

        case "theOnlyWorksheet":
            value = theOnlyWorksheet;
            break;

        case "bookAry":
            value = itemAry;
            break;

        case "noBooks":
            value = Integer.valueOf(getNoItems());
            break;

        case "sheetGroupMap":
            value = sheetGroupMap;
            break;

        case "noSheetGroups":
            value = Integer.valueOf(getNoSheetGroups());
            break;

        case "noSheets":
            value = Integer.valueOf(noSheets);
            break;

        default:
            value = super.getPseudoField(pseudoFieldName);
        }
        
        return value;
        
    } /* End of getPseudoField */
    
    
    
    /**
     * Add a new workbook. From a StringTemplate V4 template this member is inaccessible.
     *   @param book
     * The added workbook object. Its name must not be empty; it is the key into the map to
     * fetch the book.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void putBook(ExcelWorkbook book)
    {
        /* Add the workbook object to map and list. */
        assert book.name_ != null && !book.name_.givenName.isEmpty();
        if(!putItem(book.name_.givenName, book))
            return;
        book.setIndexInCollection(itemAry.size()-1);
        
        _logger.debug("{}Add new workbook {}", logCtx_, book.toString());
        
        /* We offer references to the only parsed worksheet/workbook in the whole cluster
           if this is unambiguous. This is a special but very common case and template
           writing is significantly simplyfied in this case. */
        if(itemAry.size() == 1)
        {
            assert theOnlyWorkbook == null;
            theOnlyWorkbook = book;
            _logger.debug( "{}New workbook {} is made \"theOnlyWorkbook\" of this cluster"
                         , logCtx_
                         , book.toString()
                         );
        }
        else if(theOnlyWorkbook != null)
        {
            theOnlyWorkbook = null;
            _logger.debug( "{}\"theOnlyWorkbook\" is no longer unambiguous in this cluster and"
                           + " is set null"
                         , logCtx_
                         );
        }
        if(noSheets == 0  &&  book.getNoItems() == 1)
        {
            assert theOnlyWorksheet == null;
            theOnlyWorksheet = book.itemAry.get(0);
            _logger.debug( "{}Worksheet {}.{} is made \"theOnlyWorksheet\" of this cluster"
                         , logCtx_
                         , book.toString()
                         , theOnlyWorksheet.toString()
                         );
        }
        
        /* Count parsed sheets. */
        noSheets += book.getNoItems();
        
        if(theOnlyWorksheet != null  &&  noSheets > 1)
        {
            theOnlyWorksheet = null;
            _logger.debug( "{}\"theOnlyWorksheet\" is no longer unambiguous in this cluster"
                           + " and is set null"
                         , logCtx_
                         );
        }
    } /* End of putBook */


    
    /** 
     * User feedback: This method reports if the direct access to a single
     * workbook/worksheet is available.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void reportDirectDataAccess()
    {
        if(theOnlyWorkbook != null)
        {
            _logger.info( "{}\"theOnlyWorkbook\" references Excel workbook {} ({})"
                        , logCtx_
                        , theOnlyWorkbook.toString()
                        , theOnlyWorkbook.excelFile
                        );
        }
        else
        {
            _logger.info( "{}{} Excel workbooks have been parsed."
                          + " \"theOnlyWorkbook\" is set to null as an unambiguous reference"
                          + " to a single workbook is impossible"
                        , logCtx_
                        , getNoItems()
                        );
        }
        
        if(theOnlyWorksheet != null)
        {
            assert theOnlyWorkbook != null;
            _logger.info( "{}\"theOnlyWorksheet\" references Excel worksheet {}.{}"
                          + " ({}::{})"
                        , logCtx_
                        , theOnlyWorkbook.toString()
                        , theOnlyWorksheet.toString()
                        , theOnlyWorksheet.excelFile
                        , theOnlyWorksheet.tabName
                        );
        }
        else
        {
            _logger.info( "{}\"theOnlyWorksheet\" is set to null as an"
                          + " unambiguous reference to a single worksheet is impossible"
                        , logCtx_
                        );
        }
        
        /// @todo Consider to iterate all workbooks and report their theOnlyWorksheet
        
    } /* End of reportDirectDataAccess */
    
    


    /**
     * Sort the workbooks of this cluster and the worksheets in the global groups, which
     * are managed by the cluster.<p>
     *   This method should be called at the end of parsing all the workbooks.
     *   @param sortOrderBooks
     * The wanted sort order for the workbooks. Sorting is done with respect to the names
     * of the workbooks. Sorting is done based on {@link SortOrder#createComparatorString};
     * refer to this method to get details about the behavior of sorting.
     *   @param sortOrderSheets
     * The wanted sort order for the worksheets. Sorting is done with respect to the names
     * of the worksheets. Sorting is done based on {@link
     * SortOrder#createComparatorString}; refer to this method to get details about the
     * behavior of sorting.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void sort(SortOrder.Order sortOrderBooks, SortOrder.Order sortOrderSheets)
    {
        /* Sort the list of workbooks in this cluster. */
        if(itemAry != null  &&  sortOrderBooks != SortOrder.Order.undefined)
        {
            final SortOrder.Comparator<? super ExcelWorkbook> comparator =
                                               ExcelWorkbook.createComparator(sortOrderBooks);
            itemAry.sort(comparator);

            /* The index in the list needs to be reset according to the new order. */
            int i = 0;
            for(ExcelWorkbook book: itemAry)
                book.setIndexInCollection(i++);
        }

        /* The sorting of the worksheets has to be done in the global groups. */
        if(sheetGroupMap != null  &&  sortOrderSheets != SortOrder.Order.undefined)
        {
            /* Sort all global groups in the map of those. */
            final SortOrder.Comparator<? super RowObjectContainer> comparator =
                                         RowObjectContainer.createComparator(sortOrderSheets);
            for(ObjectList<ExcelWorksheet> wsGroup: sheetGroupMap.values())
                wsGroup.sort(comparator);
            
            /* The field index-in-collection of the sorted elements is not reset: The
               global worksheet groups are not the principal collections for worksheets and
               the index doesn't relate to this subordinated collections. */
        }
    } /* End of sort */
    
} /* End of class Cluster definition. */





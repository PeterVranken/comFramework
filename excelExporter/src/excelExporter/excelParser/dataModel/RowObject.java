/**
 * @file RowObject.java
 * An object of this class represents a single row from an Excel worksheet.
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
/* Interface of class RowObject
 *   RowObject
 *   getPseudoField
 *   putCell
 *   compare
 *   createComparator
 */

package excelExporter.excelParser.dataModel;

import java.util.*;

import org.apache.logging.log4j.*;
import excelExporter.excelParser.SortOrder;
import excelExporter.excelParser.ErrorCounter;


/**
 * The row of an Excel worksheet as map of properties. Each cell of the row is such a
 * property and the key to access it is the name of the column it is found in.<p>
 *   The row obect has additional fields, which represent predefined properties (e.g. the
 * coordinates of the cell in the worksheet) or support the iteration along all cell based
 * properties.<p>
 *   An object of this class contains the results of parsing one row of a worksheet in an
 * Excel input file. The object is passed as part of the data model to the template engine
 * for rendering the information in the wanted format.<p>
 *   Formally, an {@link RowObject} object is a Java Map, which contains Excel cells,
 * looked up by (property) name. The name of a property is subject to the application
 * configuration. It might be the contents of the top most cell of the column in the Excel
 * worksheet but this is not a must and not even common practice. If the application has
 * parsed an Excel worksheet with two columns "A" and "B", then the related properties of
 * the row object would be accessed from a StringTemplate V4 template with an expression
 * like {@code <row.A>} or {@code <row.B>}.<p>
 *   Note, empty cells in the Excel worksheet are not added to the {@code RowObject}
 * object. They are not represented by {@code CellObject} objects. If a template expression
 * like {@code <row.A>} hits an empty cell then the expression evaluates to null.
 * Consequently, commonly available cell information like row and column index is not
 * available for empty cells. Refer to {@link CellObject#isNotBlank} for a detailed
 * consideration how to handle the distinction between empty and non empty cells from a
 * template.<p>
 *   The row object provides more information to the rendering process. The map of cells or
 * properties is extended by some so-called "pseudo-fields", which can be accessed from a
 * template by means of the same dot notation. As an example, the row object has a row
 * number and this number can be accessed by the template expression {@code <row.iRow>};
 * where {@code iRow} denotes a pseudo-field. (It's impossible to name a property (i.e. to
 * title a column) "iRow", this is a reserved keyword.)<p>
 *   The list of all supported pseudo-fields is documented as enumeration {@link
 * RowObject.PseudoFieldName}.<p>
 *   {@link RowObject} is derived from its base class and the base class' pseudo-fields are
 * inherited and can be accessed in the same way. They are documented as enumeration {@link
 * ObjectMap.PseudoFieldName}.
 */

public class RowObject extends ObjectMap<CellObject>
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(RowObject.class);

    /** This is the list of pseudo-fields of class {@link RowObject} that can be
        accessed from a StringTemplate V4 template. The enumerated fields can be accessed
        like ordinary public fields from a StringTemplate V4 template. The dot notation is
        used to do so. Taking the first named value {@link PseudoFieldName#i0Row} as an
        example, one would write {@code <row.i0Row>} to access the related
        pseudo-field.<p>
          A short explanation of each pseudo-field and a reference to more in-depth
        information can be found as description of each named value of the enumeration.<p>
          Please note, that the pseudo-fields of the base class {@link ObjectMap} are
        inherited and can be accessed, too. See {@link ObjectMap.PseudoFieldName}.<p>
          A detailed discussion of pseudo-fields and the enumeration of these can be found
        in the class description of {@link ObjectMap}. */
    public static enum PseudoFieldName
    {
        /** Access the null based row coordinate {@link RowObject#i0Row} in the Excel
            input.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <row.i0Row>}. */
        i0Row,
        
        /** Access the one based row coordinate {@link RowObject#iRow} in the Excel input.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <row.iRow>}. */
        iRow,
        
        /** The list {@link ObjectMap#itemAry} of all properties of the row object. This
            pseudo-field is an alias to access the pseudo-field {@link
            ObjectMap.PseudoFieldName#itemAry} of the base class.<p>
              From a StringTemplate V4 template the list would be accessed with an
            expression like {@code <row.cellAry>}. */
        cellAry,
        
        /** The number of properties of the row object. This pseudo-field is an alias to
            access the pseudo-field {@link ObjectMap.PseudoFieldName#noItems} of the base
            class.<p>
              From a StringTemplate V4 template the number would be accessed with an
            expression like {@code <row.noCells>}. */
        noCells,
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
    
    /** The null based index of the row in the worksheet. */
    public final int i0Row;

    /** The one based index of the row in the worksheet. */
    public final int iRow;

    

    /**
     * Create a new row object.
     *   @param errCnt
     * A client supplied error counter. The use case is to permit consecutive error
     * counting across different phases of parsing.
     *   @param logContext
     * A string used to precede all logging statements of this module. Pass null if not
     * needed.
     *   @param idxRow
     * The null based index of the row in the worksheet.
     */
    public RowObject(ErrorCounter errCnt, String logContext, int idxRow)
    {
        super( errCnt
             , logContext
             , /* name */ new Identifier("Row" + idxRow)
             , _pseudoFieldNameList
             );
             
        i0Row = idxRow;
        iRow = i0Row + 1;

    } /* End of RowObject */



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
        case "i0Row":
            value = Integer.valueOf(i0Row);
            break;

        case "iRow":
            value = Integer.valueOf(iRow);
            break;

        case "cellAry":
            value = itemAry;
            break;

        case "noCells":
            value = Integer.valueOf(getNoItems());
            break;

        default:
            value = super.getPseudoField(pseudoFieldName);
        }
        
        return value;

    } /* End of getPseudoField */
    
    

    
    /**
     * Add a cell object to the map.
     *   @return Get the Boolean information whether the cell object could be added to the
     * map. Adding can fail if the property name clashes with a reserved keyword or an
     * already existing property.
     *   @param propName A cell represents a property of the row object. This is the name
     * of the property; the name is the key into the map to later access it.
     *   @param cellObj The object to add.
     */
    public boolean putCell(String propName, CellObject cellObj)
    {
        assert cellObj != null;

        /* Add the cell object to map and list. */
        if(!putItem(propName, cellObj))
        {
            errCnt_.error();
            _logger.error( "{}Row {}, column {}: Property name {} conflicts either with a"
                           + " reserved keyword or with another property. Please use"
                           + " another property name as column title."
                           + " The intended value {} is ignored"
                         , logCtx_
                         , cellObj.iRow, cellObj.iCol
                         , propName
                         , cellObj
                         );
            return false;
        }
        
        _logger.debug( "{}Row {}, column {}: Property {}={} is added to RowObject"
                     , logCtx_
                     , cellObj.iRow, cellObj.iCol
                     , propName
                     , cellObj
                     );

        /* Set the values of some predefined properties. */
        cellObj.name = new Identifier(propName);
        cellObj.setIndexInCollection(itemAry.size()-1);

        return true;

    } /* End of putCell */



    /**
     * Compare two row objects. This function supports the sorting algorithms from Java's
     * Collections. Row objects can be compared according to different sort orders and with
     * respect to a single of their properties.
     *   @return Get -1 if a should precede b, 0 if they are equal in the sense of the sort
     * order and 1 if a should follow b.
     *   @param a
     * The first operand.
     *   @param b
     * The second operand.
     *   @param sortOrder
     * The sorder, which should be yielded by this comparison.
     *   @param propName
     * The name of the property of the row objects, which should be compared to one
     * another.
     */
    private static int compare( RowObject a
                              , RowObject b
                              , SortOrder.Order sortOrder
                              , String propName
                              )
    {
        /* Check for the presence of the properties. Properties are represented as cell
           objects. If we get two, then we can delegate the comparison to that class.
           Otherwise we need additional rules to sort it out.
             Objects, which don't have the property at all will come behind those having
           the property.
             If both don't have the property then they are considered equal. */
        final CellObject propA = a.getItem(propName)
                       , propB = b.getItem(propName);
        if(propA != null  &&  propB != null)
            return CellObject.compare(propA, propB, sortOrder);
        else if(propA == null  &&  propB == null)
            return 0;
        else if(propA == null)
            return 1;
        else
            return -1;

    } /* End of compare */



    /**
     * A comparator for sorting row objects in a collection is created and returned.
     *   @return Get the temporary comparator object as usable with the Java Collections
     * class.
     *   @param sortOrder
     * The desired sort order.
     *   @param propName
     * The row objects can be sorted according to the values of a single property. The name
     * of the property is passed in. Objects, which don't have the property at all will
     * come behind those having the property.
     */
    public static SortOrder.Comparator<RowObject> createComparator
                                                        ( final SortOrder.Order sortOrder
                                                        , final String propName
                                                        )
    {
        return new SortOrder.Comparator<RowObject>()
                    {
                        public int compare(RowObject a, RowObject b)
                            {return RowObject.compare(a, b, sortOrder, propName);}
                        
                        public SortOrder.Order getSortOrder()
                            {return sortOrder;}

                    }; /* End anonymous comparator class */

    } /* End of createComparator */

} /* End of class RowObject definition. */





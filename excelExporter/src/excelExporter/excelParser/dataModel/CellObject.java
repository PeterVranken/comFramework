/**
 * @file CellObject.java
 * The representation of a single cell of the Excel worksheet.
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
/* Interface of class CellObject
 *   CellObject
 *   setIndexInCollection
 *   compareNumerically
 *   compare
 *   toString
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.SortOrder;


/**
 * The representation of a single cell of the Excel worksheet.<p>
 *   Note, empty cells are not added to the data model at all. The access from a template
 * to such a cell will always evaluate to null. Commonly available cell information, like
 * row and column index, is not available for blank cells. Refer to {@link
 * CellObject#isNotBlank} for a detailed consideration how to handle the distinction from a
 * template.
 */

public class CellObject
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(CellObject.class);

    /** The ID of this object. Each object in the data model gets a unique ID, which can be
        useful for having related data objects in the generated code with individual
        names. */
    public final int objId;

    /** The null based index of the object in a collection. The index can be used to
        support the implementation of arrays of objects or enumerations by a list of
        #define's in the generated code.<p>
          If the reference to an object is held in more than one collection, then the index
        relates to the most important collection.<p>
          The value is -1 if the object is not element of a collection. */
    public int i0 = -1;

    /** The one based index of the attribute in a collection. The value is {@link #i0}+1.<p>
          If the reference to an object is held in more than one collection, then the index
        relates to the most important collection.<p>
          The value is -1 if the object is not element of a collection. */
    public int i = -1;

    /** The usual name of an object's null based index-in-collection in the data model is
        {@link #i0}. However, for some of the objects and to avoid name clashes with Excel
        input data, we can't permit such a common designation and need to append an
        underscore. This makes the data model unavoidably inconsistent. To improve the
        situation we offer the object's index for all unaffected objects under the second
        designation, too. For this object, {@code <obj.i0_>} would mean just the same as
        {@code <obj.i0>}. This makes that for any kind of object a template can refer to
        the index by the former expression.
          @return Get the null based index. */
    public int getI0_()
        {return i0;}

    /** The usual name of an object's one based index-in-collection in the data model is
        {@link #i0}. However, for some of the objects and to avoid name clashes with Excel
        input data, we can't permit such a common designation and need to append an
        underscore. This makes the data model unavoidably inconsistent. To improve the
        situation we offer the object's index for all unaffected objects under the second
        designation, too. For this object, {@code <obj.i_>} would mean just the same as
        {@code <obj.i>}. This makes that for any kind of object a template can refer to
        the index by the former expression.
          @return Get the one based index. */
    public int getI_()
        {return i;}

    /** The name of the cell object is the name of the property of the row object, which is
        represented by this cell. Normally, this is the title of the column the cell was
        found in.
          @remark The name is stored as an object of type {@link Identifier}. If is is
        rendered like {@code <cell.name>} in a StringTemplate V4 template you will not get
        the given name but the C identifier, which is most similar to the given name - this
        can even be identical to the name. If you really want to get the given name then
        you'd rather put {@code <cell.name.givenName>} into your template. */
    public Identifier name = null;

    /** The usual name of an object's name in the data model is {@link #name}. However, for
        some of the objects and to avoid name clashes with Excel input data, we can't
        permit such a common designation and need to append an underscore. This makes the
        data model unavoidably inconsistent. To improve the situation we offer the object
        name for all unaffected objects under the second designation, too. For this object,
        {@code <obj.name_>} would mean just the same as {@code <obj.name>}. This makes
        that for any kind of named object a template can refer to the name by the former
        expression.
          @return Get the object name. */
    public Identifier getName_()
        {return name;}

    /** An enumeration describing the possible data types of the cell contents.<p>
          Note, empty cells are not added to the data model. The access from a template
        to such a cell will evaluate to null. Therefore, the enumeration values {@code
        blank} and {@code undefined} will never be seen from a template. See {@link
        #isNotBlank} for a more detailed consideration. */
    public enum CellType 
    {
        /** No information about cell type (yet). */
        undefined, 
        
        /** Empty cell. */
        blank, 
        
        /** Cell with integral numeric value. */
        integer, 
        
        /** Cell with real numeric value. */
        real, 
        
        /** Cell with calendar date information. */
        date, 
        
        /** Cell with text contents. */
        text, 
        
        /** Cell with Boolean contents. */
        bool, 
        
        /** Cell with error information. */
        error
    };

    /** The data type of the cell contents.
          @remark Empty cells are not added to the data model. The access from a template
        to such a cell will evaluate to null. Therefore, the enumeration value {@code
        blank} will never be seen from a template. See {@link #isNotBlank} for a more
        detailed consideration. */
    public CellType type = CellType.blank;

    /** The data type of the cell contents as Boolean flag.<p>
          To support conditional code in the templates the cell type is represented as a
        set of Booleans, too. (See e.g. {@link #isInt} or {@link #isDate}.) Since empty
        cells are not added to the data model at all, it is difficult to have an according
        flag, e.g. {@code isBlank} for empty cells. The logically inverted information has
        been added instead. It enables conditional template expressions in a readable
        manner, like {@code <if(!myCell.isNotBlank)>} although it is - technically spoken -
        not really working: For any cell object in the data model the field is {@code
        true}. For empty cells there is no CellObject object in the data model and the
        expression {@code myCell.isNotBlank} evaluates to null, which is interpreted as
        {@code false}. The behavior is as expected in either case. */
    public boolean isNotBlank = false;

    /** The data type of the cell contents as Boolean flag.<p>
          To support conditional code in the templates the cell type is represented as a
        set of Booleans, too. One and only one of these will be set. See enumerated value
        {@link #type}, too. */
    public boolean isInt = false
                 , isReal = false
                 , isDate = false
                 , isText = false
                 , isBool = false
                 , isError = false;

    /** The text contents of a cell, mainly used if it is of type {@link CellType#text}. For
        other types this field will contain a suitable textual representation of the cell
        contents. It is null for empty/blank/error cells. */
    public String text = null;

    /** Query function for cells with text or most simple numeric contents: Does the cell
        contain a specific (expected) string?<p>
          The query is implemented as a Java Map if the text representation of cell is not
        empty otherwise <b>is</b> is null. The unusual name of this member has been chosen for
        sake of an intuitive design of the aimed template code.<p>
          The map has a single key, value pair. The key is the text representation of the
        cell contents and the value is a Boolean true. Using a map object in StringTemplate
        implicitly leads to a Boolean false if a key is used, which is not stored in the
        map. Consequently, just asking for a certain text you will always get the correct
        answer whether or not the cell contains this text.<p>
          Example: Given, the Excel worksheet column Mode is used to hold a pre-defined,
        enumerated selector like "undefined", "modeA" or "modeB", then your StringTemplate
        V4 template could look like:
        <pre>{@code 
          <if(row.Mode.is.modeA)>
            // We are in mode A.
            // ... Put your C code generation to handle mode A here
          <elseif(row.Mode.is.modeB)>
            // We are in mode B.
            // ... Put your C code generation to handle mode B here
          <else>
            #error Unexpected of undefined mode found. Please, double check your<\\>
            Excel specification
            <info.error.({Unexpected or undefined mode <row.mode.text> found})>
          <endif>}
        </pre> */
    public HashMap<String,Boolean> is = null;
     
    /** The text contents modified such that it becomes an identifier as defined in many
        programming languages, e.g. C. The first character may be a letter or the
        underscore, all others may be the same or a decimal digit.<p>
          All groups of unpermitted characters in {@link #text} are replaced by a single
        character x and an underscore prepends the resut if it should begin with a
        digit.<p>
          This field is available only for text cells, i.e., if {@link #isText} is {@code
        true}, otherwise it's null. */
    public String ident = null;

    /** The text contents modified such that it becomes a more restricted identifier as for
        {@link #ident}. The first character may be a letter, all others may be the same or
        a decimal digit.<p>
          All groups of unpermitted characters are replaced by a single character x and a
        further x prepends the result if it should begin with a digit.<p>
          This field is available only for text cells, i.e., if {@link #isText} is {@code
        true}, otherwise it's null. */
    public String identStrict = null;

    /** This flag indicates whether the text contents of the cell and the modification into
        identifier {@link #ident} are identical. It'll always be {@code false} if {@link
        #isText} is {@code false}. */
    public boolean identEquals = false;

    /** This flag indicates whether the text contents of the cell and the modification into
        identifier {@link #identStrict} are identical. It'll always be {@code false} if {@link
        #isText} is {@code false}. */
    public boolean identStrictEquals = false;

    /** The text contents in the form of a valid JSON String. All non-printable characters
        are escaped. (This representation is most useful for generated C code, too, but not
        fully correct. If it comes to numerical escapes - which is quite unlikely for
        normal text sources - then C differs from JSON: The field provides, e.g., \u0009,
        where C would expect \x09.)<p>
          This field is available only for text cells, i.e., if {@link #isText} is 
        {@code true}, otherwise it's null. */
    public String jsonString = null;

    /** The integer value of a numeric cell if it is of type {@link CellType#integer} or
        {@link CellType#bool}. For {@link CellType#real} (and including date
        representations) this will contain the rounded off numeric value. null for non
        numeric cell types. */
    public Long d = null;

    /** The floating point value of a numeric cell if it is of type {@link CellType#real},
        {@link CellType#integer}, {@link CellType#bool} or {@link CellType#date}. {@code
        null} for non numeric cell types. */
    public Double n = null;

    /** The value of a numeric cell if it is of type {@link CellType#date} or {@code
        null} for non dates. The use of this field enables applying the StringTemplate V4
        date renderer to render a date in the variants of any locale, e.g.:<p>
          {@code <cell.date; format="MMM dd, yyyy HH:mm">}<p>
          Note, if a cell contains a date then you can also put {@code <cell>} into your
        template. This expression will use the default representation of the {@link
        CellObject} object, which makes use of the Excel format information stored in and
        retrieved from the worksheet. Ideally, this representation should be identical to
        the cell representation visible in the Excel application but in fact it only
        resembles it; the format string handling of Excel and Java's POI are not fully
        compatible.<p>
          Note, if a cell contains a date then you can also put {@code <cell.date>} into your
        template. This expression will use the default representation of the {@link
        java.util.Calendar} object, which is a short format with date and time
        information.
          @remark It's hard to find explicit documentation on how the format string needs
        to look like. Most likely, it's an extended definition of Java's {@link
        java.text.SimpleDateFormat} (see e.g. <a
        href="https://docs.oracle.com/javase/tutorial/i18n/format/simpleDateFormat.html">
        https://docs.oracle.com/javase/tutorial/i18n/format/simpleDateFormat.html</a>).
        Besides the SimpleDateFormat formatting characters you may use some predefined
        format strings. (This information is not for granted):<p>
        {@code "short" "medium" "long" "full"}<p>
        {@code "date:short" "date:medium" "date:long" "date:full"}<p>
        {@code "time:short" "time:medium" "time:long" "time:full"} */
    public Calendar date = null;

    /** The Boolean value of a cell of {@link CellType#bool}.<p>
          For numeric cell types it is {@code true} if the numeric value is not equal to
        null and {@code false} otherwise.<p>
          For text cells it is {@code true} if the cell contents are equal to one out of
        "true", "yes", "okay" or "ok" and {@code false} otherwise. String comparison is
        done case insensitive.<p>
          It is {@code false} for empty/blank/error cells. */
    public boolean bool = false;

    /** The comment, which is attached to the cell or null if no such comment exists. */
    public String comment = null;

    /** The author of comment {@link #comment} if there is one, otherwise null. */
    public String authorOfComment = null;

    /** The one based index of the row, which the cell is located in. */
    public int iRow = 1;

    /** The null based index of the row, which the cell is located in. */
    public int i0Row = 0;

    /** The one based index of the column, which the cell is located in. */
    public int iCol = 1;

    /** The null based index of the column, which the cell is located in. */
    public int i0Col = 0;

    /**
     * Create a cell object, which represents a blank Excel cell.
     */
    public CellObject()
    {
        objId = Identifier.getUniqueId();

    } /* End of CellObject. */



    /**
     * Set the index-in-collection.
     *   @param i0 The new value for the null based index of this object in an embedding
     * collection.
     */
    public void setIndexInCollection(int i0)
    {
        assert i0 >= 0;
        i0 = i0;
        i = i0+1;

    } /* End of setIndexInCollection */



    /**
     * Compare two cell objects numerically. This function supports the sorting algorithms
     * from Java's Collections.
     *   @return Get -1 if a should precede b, 0 if they are equal in the sense of the sort
     * order and 1 if a should follow b.
     *   @param a
     * The first operand.
     *   @param b
     * The second operand.
     *   @param inverse
     * The inverse numerical sorting is not achieved by exchanging the operands; instead,
     * this Boolean flag needs to be set to {@code true}. In inverse order we still keep
     * all numeric operands before all non numerics. The numeric block is sorted in
     * decreasing number value, the non numbers are sorted in descending lexical order.
     */
    private static int compareNumerically(CellObject a, CellObject b, boolean inverse)
    {
        /* If both operands are numeric than we compare the values. Only looking at the
           definition of this class we would have to do this type dependent, since the
           (long) integer values can exceed the maximum double value that is still accurate
           for integers. However, we don't do so as all integers have be derived from
           original floating point information from the POI interface. We can always do a
           simple floating point comparison.
             If one value is not numeric then the number precedes the non number.
             If both aren't numbers then we apply the lexical sort order. */
        if(a.n != null  &&  b.n != null)
        {
            final double dblA = a.n.doubleValue()
                       , dblB = b.n.doubleValue();
            if(dblA < dblB)
                return inverse? 1: -1;
            else if(dblA > dblB)
                return inverse? -1: 1;
            else
                return 0;
        }
        else if(a.n == null  &&  b.n == null)
        {
            return compare( a
                          , b
                          , inverse? SortOrder.Order.inverseLexical: SortOrder.Order.lexical
                          );
        }
        else if(a.n == null)
            return 1;
        else
            return -1;

    } /* End of compareNumerically */



    /**
     * Compare two cell objects. This function supports the sorting algorithms from Java's
     * Collections. Cell objects can be compare according to different sort orders.
     *   @return Get -1 if a should precede b, 0 if they are equal in the sense of the sort
     * order and 1 if a should follow b.
     *   @param a
     * The first operand.
     *   @param b
     * The second operand.
     *   @param sortOrder
     * The sorder, which should be yielded by this comparison. Details about the different
     * supported sort order can be faound in the explanation of the named values of the
     * enumeration {@link excelExporter.excelParser.SortOrder.Order}.
     */
    public static int compare( CellObject a
                             , CellObject b
                             , SortOrder.Order sortOrder
                             )
    {
        switch(sortOrder)
        {
        case lexical:
        case inverseLexical:
        case ASCII:
        case inverseASCII:
            /* The lexical comparisons are done regardless of the type of the property. We had
               anyway decided for an appropriate textual representation of the information
               and can now rely on the implementation of the string compare. */
            return SortOrder.compare(a.toString(), b.toString(), sortOrder);

        case numerical:
            return compareNumerically(a, b, /* inverse */ false);

        case inverseNumerical:
            return compareNumerically(a, b, /* inverse */ true);

        default:
            assert false: "Unknown sort-order chosen";
        case undefined:
            /* We consider both objects equal and sorting won't change their order. */
            return 0;

        } /* End of switch(sortOrder) */

    } /* End of compare */



    /**
     * Get the string representation of the cell. From a StringTemplate V4 template this
     * representation of the object is accessed as {@code <cell>}.
     *   @return The string value.
     */
    @Override public String toString()
    {
        return text==null? "": text;

    } /* End of toString */

} /* End of class CellObject definition. */





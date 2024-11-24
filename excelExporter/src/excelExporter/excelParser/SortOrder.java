/**
 * @file SortOrder.java
 * The sort orders, which can be applied to the elements of the data model as an enumeration.
 *
 * Copyright (C) 2016-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class SortOrder
 *   compareNumerically
 *   createComparatorString
 */

package excelExporter.excelParser;

import java.util.*;
import org.apache.logging.log4j.*;

/**
 * The sort orders, which can be applied to the elements of the data model as an
 * enumeration.
 */
public class SortOrder
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(SortOrder.class);

    /** An enumeration describing the supported sorting of objects in the data model. */
    public static enum Order
    {
        /** Illegal value, no sort order. */
        undefined,
        
        /** Lexical order. The case insensitive Java String comparison is applied. It'll do
            a natural alphabetic sorting but will not necessarily handle language specific
            characters as expected. See Java documentation of {@link
            String#compareToIgnoreCase} for details. */
        lexical,
        
        /** Similar to {@link #lexical} but the natural Java compare of class String is
           applied. Comparison is done case sensitive. See Java documentation of {@link
           String#compareTo} for details. */
        ASCII,
        
        /** The numerical order tries to interpret both compared character strings as real
            numbers. It uses the Java method {@link Double#parseDouble} to do so. If both
            strings are found to be numbers then the string representing the smaller number
            will precede the string representing the greater number.<p>
              If only one string represents a number then this string will precede the non
            number.<p>
              If both strings don't represent numbers then they are sorted in ascending
            lexical order.<p>
              The final sort order is a sequence of raising numbers followed by a block of
            alphabetically ascending non number strings. */
        numerical,
        
        /** This sorting order leads to the inverse sequence as {@link #lexical} does. */
        inverseLexical,
        
        /** This sorting order leads to the inverse sequence as {@link #ASCII} does. */
        inverseASCII,
        
        /** The inverse numerically sort order leads to a sequence, where number
           representing strings precede non number strings. The number strings are sorted
           in order of falling number values and the non numbers in descending lexical
           order.<p>
             Please note, this sort order does <b>not</b> lead to the inverse sequence as
           {@link #numerical} does. */
        inverseNumerical,
    };

    

    /**
     * Compare two strings numerically for sorting them.
     *   @return Get the comparison result as compatible with Java's {@link
     * Collections#sort}:<ul>
     *   <li> If both strings represent (floating point) numbers, be it a and b, then
     *        the method returns a-b
     *   <li> If one string represents a number then the function returns +/-1 such that
     *        sorting will place the number representing string before the other one
     *   <li> If none of the strings represents a number than the functions returns
     *        the result of the lexical String comparison; these strings will be sorted
     *        lexically.
     *   </ul>
     *   @param sa
     * The first operand.
     *   @param sb
     * The second operand.
     *   @param inverse
     * The inverse numerical sorting is not achieved by exchanging the operands; instead,
     * this Boolean flag needs to be set to {@code true}. In inverse order we still keep
     * all numeric operands before all non numerics. The numeric block is sorted in
     * decreasing number value, the non numbers are sorted in descending lexical order.
     */
    private static int compareNumerically(String sa, String sb, boolean inverse)
    {
        boolean isNumA, isNumB;
        double a, b;
        try
        {
            a = Double.parseDouble(sa);
            isNumA = true;
        }
        catch(NumberFormatException ex)
        {
            isNumA = false;
            a = 0.0;
        }
        try
        {
            b = Double.parseDouble(sb);
            isNumB = true;
        }
        catch(NumberFormatException ex)
        {
            isNumB = false;
            b = 0.0;
        }
        if(isNumA && isNumB)
        {
            if(a < b)
                return inverse? 1: -1;
            else if(a > b)
                return inverse? -1: 1;
            else
                return 0;
        }
        else if(isNumA)
            return -1;
        else if(isNumB)
            return 1;
        else
        {
            if(inverse)
                return sb.compareToIgnoreCase(sa);
            else
            return sa.compareToIgnoreCase(sb);
        }        
    } /* End of compareNumerically */
    
    
    
    /**
     * Compare two String objects with given sort order.
     *   @return Get the comparison result as compatible with Java's {@link
     * Collections#sort}.
     *   @param a
     * The first operand.
     *   @param b
     * The second operand.
     *   @param sortOrder
     * The sort order to apply.
     */
    public static int compare(String a, String b, Order sortOrder)
    {
        switch(sortOrder)
        {
        case lexical:
            return a.compareToIgnoreCase(b);

        case inverseLexical:
            return b.compareToIgnoreCase(a);

        case numerical:
            return compareNumerically(a, b, /* inverse */ false);

        case inverseNumerical:
            return compareNumerically(a, b, /* inverse */ true);

        case ASCII:
            return a.compareTo(b);

        case inverseASCII:
            return b.compareTo(a);

        /* Undefined sorting: Don't change the order of objects at
           all. */
        default:
            assert false;
        case undefined:
            return 0;
        }
    } /* End of compare */
    
    
    
    /**
     * This is an extension of the Java class {@link java.util.Comparator}, which provides
     * access to the sort order, which is implemented by the comparator object.
     *   @param <T>
     * The class of the compared objects.
     */
    public interface Comparator<T> extends java.util.Comparator<T>
    {
        /**
         * Query the sort order implemented by the given Comparator object.
         *   @return Get the sort order.
         */
        Order getSortOrder();

    } /* End of interface Comparator. */
    

    
    /**
     * A comparator for sorting String objects is created and returned.
     *   @return Get a temporary comparator object as usable with the Java Collections
     * class.
     *   @param sortOrder
     * The comparator will support the given sort order. The following sort orders are
     * supported:<ul>
     *   <li>lexical: The case insensitive sort order of Java String
     *   <li>ASCII: The natural sort order of Java String
     *   <li>numerical: The String contents are interpreted as numbers and the numbers'
     * values are compared. See {@link Order} for additional details.
     *   <li>undefined: All compared elements will be treated as equal. Sorting will not
     * change the order of elements</ul>
     */
    public static Comparator<String> createComparatorString(final Order sortOrder)
    {
        return new Comparator<String>()
                    {
                        public int compare(String a, String b)
                            {return SortOrder.compare(a, b, sortOrder);}
                        
                        public Order getSortOrder()
                            {return sortOrder;}

                    }; /* End anonymous comparator class */
                                        
    } /* End of createComparatorString */
    
    
    
    
} /* End of class SortOrder definition. */






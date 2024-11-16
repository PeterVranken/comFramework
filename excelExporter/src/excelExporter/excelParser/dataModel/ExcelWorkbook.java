/**
 * @file ExcelWorkbook.java
 * This file defines the element of the StringTemplate V4 data model, which models a Excel
 * workbook. The design of this data structure is determined by the capabilities and
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
/* Interface of class ExcelWorkbook
 *   ExcelWorkbook
 *   setName
 *   setExcelFile
 *   getPseudoField
 *   putSheet
 *   sort
 *   toString
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.*;


/**
 * The data structure describing the complete workbook.<p>
 *   An object of this class contains the results of parsing an Excel input file. The
 * object is passed as part of the data model to the template engine for rendering the
 * information in the wanted format.<p>
 *   Formally, an {@link ExcelWorkbook} object is a Java Map, which contains Excel
 * worksheets, looked up by name. The name of a worksheet is subject to the application
 * configuration. It might be the tab of the Excel worksheet but this is not a must and not
 * even common practice. If the application has parsed two Excel worksheets "mySheetA" and
 * "mySheetB", then these worksheets would be accessed from a StringTemplate V4 template
 * with an expression like {@code <book.mySheetA>} or {@code <book.mySheetB>}.<p>
 *   The workbook provides more information to the rendering process. The map of worksheets
 * is extended by some so-called "pseudo-fields", which can be accessed from a template by
 * means of the dot notation. As an example, the workbook has a name and this name can be
 * accessed by the template expression {@code <book.name_>}; where {@code name_} denotes a
 * pseudo-field. (It's impossible to name a worksheet "name_", this is a reserved
 * keyword.)<p>
 *   The list of all supported pseudo-fields is documented as enumeration {@link
 * ExcelWorkbook.PseudoFieldName}.<p>
 *   {@link ExcelWorkbook} is derived from its base class and the base class' pseudo-fields
 * are inherited and can be accessed in the same way. They are documented as enumeration
 * {@link ObjectMap.PseudoFieldName}.
 */
public class ExcelWorkbook extends ObjectMap<ExcelWorksheet>
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(ExcelWorkbook.class);

    /** This is the list of pseudo-fields of class {@link ExcelWorkbook} that can be
        accessed from a StringTemplate V4 template. The enumerated fields can be accessed
        like ordinary public fields from a StringTemplate V4 template. The dot notation is
        used to do so. Taking the first named value {@link PseudoFieldName#excelFile} as an
        example, one would write {@code <cluster.excelFile>} to access the related
        pseudo-field.<p>
          A short explanation of each pseudo-field and a reference to more in-depth
        information can be found as description of each named value of the enumeration.<p>
          Please note, that the pseudo-fields of the base class {@link ObjectMap} are
        inherited and can be accessed, too. See {@link ObjectMap.PseudoFieldName}.<p>
          A detailed discussion of pseudo-fields and the enumeration of these can be found
        in the class description of {@link ObjectMap}. */
    public static enum PseudoFieldName
    {
        /** The file designation {@link ExcelWorkbook#excelFile} of the Excel source.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <book.excelFile>}. */
        excelFile,
    
        /** The list of parsed worksheets in the book. This pseudo-field is an alias to
            access the pseudo-field {@link ObjectMap.PseudoFieldName#itemAry} of the base
            class.<p>
              From a StringTemplate V4 template the list would be accessed with an
            expression like {@code <book.sheetAry>}. */
        sheetAry, 
        
        /** The number of parsed worksheets in the workbook. This pseudo-field is an alias to
            access the pseudo-field {@link ObjectMap.PseudoFieldName#noItems}.<p>
              The number of sheets relates to both collections, {@link ObjectMap#itemAry}
            and {@link ObjectMap#itemMap}.<p>
              From a StringTemplate V4 template the number would be accessed with an
            expression like {@code <book.noSheets>}. */
        noSheets,
        
        /** The only parsed worksheet {@link ExcelWorkbook#theOnlyWorksheet} of the
            workbook, if unambiguously possible. {@code null} otherwise.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <book.theOnlyWorksheet>}. */
        theOnlyWorksheet,
        
        /** The map {@link ExcelWorkbook#optionMap} of user specified options.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <book.optionMap>}. */
        optionMap,
        
        /** The number {@link ExcelWorkbook#getNoOptions} of user specified options in {@link
            ExcelWorkbook#optionMap}.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <book.noOptions>}. */
        noOptions,
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
    
    /** The file designation of the Excel source. */
    public FileExt excelFile = null;
    
    /** To simplify template writing in a special but quite common case the data model
        offers a direct reference from the workbook to its worksheet. This will work only
        if a single worksheet is parsed in this book. Otherwise this reference is set to
        null in order to avoid unrecognized problems due to ambiguities.<p>
          How this field is set, how it relates to parsed worksheets is logged on logging
        level DEBUG. */
    public ExcelWorksheet theOnlyWorksheet = null;
    
    /** A map of user specified options or template attributes. User options are template
        attributes, which are simply passed through from the application's command line to
        the StringTemplate V4 template and can there be used to control the code
        generation. The use case are optional constructs in the generated code, which are
        controlled from the command line.<p>
          User options/attributes can be given in the context of an in- or output file.
        This map holds all options given in the context of the currently processed input
        file. (Refer to {@link Info#optionMap} for the output related options.)<p>
          The name of an option or template attribute is the key into the map and the
        attribute's value is the value of the map entry. The value object's Java type is
        one out of String, Boolean, Integer or Double, depending on which fits best to the
        original command line argument; a command line argument <i>cmdLineArg</i> with
        value {@code true} would obviously be passed as Boolean into the template and would
        permit conditional code by using a construct like: {@code
        <if(wBook.optionMap.cmdLineArg)>Command line argument is TRUE!<endif>}<p>
          A sorted map is used for the implementation. The map retains the order of
        appearance of options on the command line. If an option is set repeatedly then the
        first appearance on the command line counts. (Typical scenario: The option is set
        first in the global context as default value then in the context of a specific in-
        or output file with a value dedicated to that file.) A map iteration in the
        StringTemplate V4 template will yield this order. Example:<p>
          {@code <wBook.optionMap:{name|<name>=<wBook.optionMap.(name)><\n>}>} */
    public LinkedHashMap<String,Object> optionMap = null;
    
    /** The number of user options in {@link #optionMap}. From a StringTemplate V4
        template this member is accessed like {@code <wBook.noOptions>}.
          @return Get the number of options. */
    public int getNoOptions()
        { return optionMap != null? optionMap.size(): 0; }
    
    /** A global counter used for unambiguous, generic naming of workbook objects if no
        other name is known. The id is incremented on every use. */
    private static int _idAnonymousWorkbook = 0;
    

    /**
     * Create a new Excel workbook object.
     *   @param errCnt
     * A client supplied error counter. The use case is to permit consecutive error
     * counting across different phases of processing.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public ExcelWorkbook(ErrorCounter errCnt)
    {
        super( errCnt
             , /* logContext */ null
             , new Identifier("")
             , _pseudoFieldNameList
             );
        setName((String)null);

    } /* End of ExcelWorkbook */
    
    
    /**
     * Set the name of the object and adjust the logging context accordingly.
     *   @param givenName
     * The name of the object or empty string or null if the name should be set by rules.
     * First the name is tried to be derived from the file name. If this is impossble then
     * a generic name is taken.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void setName(String givenName)
    {
        final String tmpName;
        
        if(givenName != null)
            tmpName = givenName;
        else if(excelFile != null)
            tmpName = excelFile.nameStem;
        else
            tmpName = null;
        
        if(tmpName != null && !tmpName.trim().isEmpty())
            setName(new Identifier(tmpName));
        else
            setName(new Identifier("Workbook_" + ++_idAnonymousWorkbook));

        setLogContext("Workbook " + name_ + ": ");
        
    } /* End of setName */
    
    
    
    /**
     * Set the name of the represented Excel file.    
     *   @param excelFile
     * The designation of the parsed Excel file.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */   
    public void setExcelFile(FileExt excelFile)
        {this.excelFile = excelFile;}



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
        case "excelFile":
            value = excelFile;
            break;

        case "sheetAry":
            value = itemAry;
            break;

        case "noSheets":
            value = Integer.valueOf(getNoItems());
            break;

        case "theOnlyWorksheet":
            value = theOnlyWorksheet;
            break;

        case "optionMap":
            value = optionMap;
            break;

        case "noOptions":
            value = Integer.valueOf(getNoOptions());
            break;

        default:
            value = super.getPseudoField(pseudoFieldName);
        }
        
        return value;
        
    } /* End of getPseudoField */
    
    
    
    /**
     * Add a worksheet object to the workbook.
     *   @return
     * Get the Boolean information whether the worksheet could be put into the map. Adding
     * can fail if the worksheet name clashes with a reserved keyword or an already
     * existing worksheet.
     *   @param
     * worksheet The object to add.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public boolean putSheet(ExcelWorksheet worksheet)
    {
        assert worksheet != null  &&  worksheet.name_ != null
               && !worksheet.name_.givenName.isEmpty();
        final String worksheetName = worksheet.name_.givenName;

        /* Add the worksheet object to map and list. */
        if(!putItem(worksheetName, worksheet))
            return false;
        worksheet.setIndexInCollection(itemAry.size()-1);

        _logger.debug( "{}Worksheet {} is added to ExcelWorkbook {}"
                     , logCtx_
                     , worksheetName
                     , toString()
                     );

        /* We offer a reference to the only parsed worksheet in this workbook if
           this is unambiguous. This is a special but very common case and template
           writing can be simpler in this case. */
        if(itemAry.size() == 1)
        {
            assert theOnlyWorksheet == null;
            theOnlyWorksheet = worksheet;
            _logger.debug( "{}New worksheet {} is made \"theOnlyWorksheet\" of this"
                           + " workbook"
                         , logCtx_
                         , worksheetName
                         );
        }
        else if(theOnlyWorksheet != null)
        {
            theOnlyWorksheet = null;
            _logger.debug( "{}\"theOnlyWorksheet\" is no longer unambiguous in this"
                           + " workbook and is set null"
                         , logCtx_
                         );
        }

        return true;

    } /* End of putSheet */



    /**
     * Sort the worksheets in this workbook.<p>
     *   This method should be called at the end of parsing the workbook.
     *   @param sortOrder
     * The wanted sort order. Sorting is done with respect to the name of the worksheet.
     * Sorting is done based on {@link SortOrder#createComparatorString}; refer to this
     * method to get details about the behavior of sorting.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void sort(SortOrder.Order sortOrder)
    {
        if(itemAry != null)
        {
            final SortOrder.Comparator<? super ExcelWorksheet> comparator =
                                                  ExcelWorksheet.createComparator(sortOrder);
            itemAry.sort(comparator);

            /* The index in the list needs to be reset according to the new order. */
            int i = 0;
            for(ExcelWorksheet sheet: itemAry)
                sheet.setIndexInCollection(i++);
        }
    } /* End of sort */
    
} /* End of class ExcelWorkbook definition. */





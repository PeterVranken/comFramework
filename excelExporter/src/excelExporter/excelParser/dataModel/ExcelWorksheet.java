/**
 * @file ExcelWorksheet.java
 * This file defines the element of the StringTemplate V4 data model, which models a Excel
 * worksheet. The design of this data structure is determined by the capabilities and
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
/* Interface of class ExcelWorksheet
 *   ExcelWorksheet
 *   getPseudoField
 *   setParent
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.*;


/**
 * The data structure describing a worksheet in the workbook.<p>
 *   An object of this class contains the results of parsing a worksheet of an Excel input
 * file. The object is passed as part of the data model to the template engine for
 * rendering the information in the wanted format.<p>
 *   Formally, an {@link ExcelWorksheet} object is a Java Map, which contains groups of
 * Excel rows, looked up by name. Such a group of rows is represented by a {@link
 * RowObjectContainer} object. The name of a group is taken from the cells in the grouping
 * column(s) of the worksheet. If the worksheet defines two groups "myGroupA" and
 * "myGroupB", then these groups would be accessed from a StringTemplate V4 template with
 * an expression like {@code <sheet.myGroupA>} or {@code <sheet.myGroupB>}.<p>
 *   The worksheet provides more information to the rendering process. The map of row
 * object groups is extended by some so-called "pseudo-fields", which can be accessed from
 * a template by means of the dot notation. As an example, the worksheet has a name and
 * this name can be accessed by the template expression {@code <sheet.name_>}; where {@code
 * name_} denotes a pseudo-field. (It's impossible to name a group of rows "name_", this is
 * a reserved keyword.)<p>
 *   The list of all supported pseudo-fields is documented as enumeration {@link
 * ExcelWorksheet.PseudoFieldName}.<p>
 *   {@link ExcelWorksheet} is derived from its base classes and the base classes'
 * pseudo-fields are inherited and can be accessed in the same way. They are documented as
 * enumerations {@link RowObjectContainer.PseudoFieldName} and {@link
 * ObjectMap.PseudoFieldName}.
 */
public class ExcelWorksheet extends RowObjectContainer
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(ExcelWorksheet.class);

    /** This is the list of pseudo-fields of class {@link ExcelWorksheet} that can be
        accessed from a StringTemplate V4 template. The enumerated fields can be accessed
        like ordinary public fields from a StringTemplate V4 template. The dot notation is
        used to do so. Taking the first named value {@link PseudoFieldName#excelFile} as an
        example, one would write {@code <sheet.excelFile>} to access the related
        pseudo-field.<p>
          A short explanation of each pseudo-field and a reference to more in-depth
        information can be found as description of each named value of the enumeration.<p>
          Please note, that the pseudo-fields of the base classes {@link
        RowObjectContainer} and {@link ObjectMap} are inherited and can be accessed, too.
        See {@link RowObjectContainer.PseudoFieldName} and {@link
        ObjectMap.PseudoFieldName}, respectively.<p>
          A detailed discussion of pseudo-fields and the enumeration of these can be found
        in the class description of {@link ObjectMap}. */
    public static enum PseudoFieldName 
    {
        /** The file designation {@link ExcelWorksheet#excelFile} of the Excel source.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <sheet.excelFile>}. */
        excelFile,
        
        /** The tab name {@link ExcelWorksheet#tabName} of the worksheet in the Excel
            source.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <sheet.tabName>}. */
        tabName,
        
        /** The work book object {@link ExcelWorksheet#parentBook}, this sheet belongs to.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <sheet.parentBook>}. */
        parentBook, 
        
        /** The flag {@link ExcelWorksheet#isRoot}, whether this container is a complete
            worksheet and not just a nested sub-group of row objects.<p>
              From a StringTemplate V4 template it would be accessed with an expression
            like {@code <if(container.isRoot)>This is the complete worksheet<else>This is a
            sub-group of a worksheet<endif>}. */
        isRoot,
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

    /** The file designation of the Excel source. File name, extension, path etc. can be
        accessed from a StringTemplate V4 template through the public members of class
        {@link FileExt}. */
    public FileExt excelFile = null;
    
    /** The title string of the tab as found in the Excel file. */
    public String tabName = null;

    /** The workbook by reference, which contains this worksheet. */
    public ExcelWorkbook parentBook = null;
    
    /** This always true flag indicates that we are a Worksheet. The Java class
        ExcelWorksheet is a RowObjectContainer but the super class doesn't have this flag.
        When queried from a StringTemplate V4 template the flag will evaluate to {@code
        true} if and only if the queried container is the root container or with other
        words the complete Worksheet object. The disticntion between a true Worksheet and
        sub-groups of row objects can be useful when writing generic templates. Example:<p>
          {@code <if(container.isRoot)>This is a worksheet object<else>This is a sub-group
        of the worksheet<endif>} */
    public final boolean isRoot = true;
    
    /** A global counter used for unambiguous, generic naming of worksheet objects if no
        other name is known. The id is incremented on every use. */
    private static int _idAnonymousWorksheet = 0;
    
    
    /** 
     * Create a new Excel worksheet object.
     *   @param errCnt
     * A client supplied error counter. The use case is to permit consecutive error
     * counting across different phases of parsing.
     *   @param logContext
     * A string used to precede all logging statements of this module. Pass null if not
     * needed.
     *   @param name
     * The name of the object, which is the name to retrieve the object from the data
     * model. The name should not be empty. If so a generic name is chosen instead.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public ExcelWorksheet(ErrorCounter errCnt, String logContext, String name)
    {
        super( errCnt
             , logContext
             , name != null  && !name.trim().isEmpty()
               ? new Identifier(name.trim())
               : new Identifier("Worksheet_" + ++_idAnonymousWorksheet)
             , /* idxColWorksheet */ -1
             , _pseudoFieldNameList
             );
    } /* End of ExcelWorksheet */
    
    

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
        case "excelFile":
            value = excelFile;
            break;

        case "tabName":
            value = tabName;
            break;

        case "parentBook":
            value = parentBook;
            break;

        case "isRoot":
            value = Boolean.valueOf(isRoot);
            break;

        default:
            value = super.getPseudoField(pseudoFieldName);
        }
        
        return value;
        
    } /* End of getPseudoField */
    
    
    
    /**
     * Set parent of the worksheet.
     *   @param parent
     * A reference to the workbook, which contains this worksheet.
     *   @remark
     * This method is irrelevant and meaningless to a StringTemplate V4 template.
     */
    public void setParent(ExcelWorkbook parent)
    {
        parentBook = parent;
        
    } /* End of setParent */
    
} /* End of class ExcelWorksheet definition. */





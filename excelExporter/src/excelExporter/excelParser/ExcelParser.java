/**
 * @file ExcelParser.java
 * Read the contents of an Excel file into the StringTemplate V4 data model.
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
/* Interface of class ExcelParser
 *   ExcelParser
 *   jsonStringify
 *   parseCell
 *   putRowIntoGroup
 *   readXlsFile
 *   getSheetNameAsIdent
 *   parseXlsSheet
 *   parseXlsFile
 *   errorAmbiguousTemplateMatch
 *   getWorksheetDefaultTemplate
 *   getWorksheetTemplate (3 variants)
 *   compileListOfWorksheets
 */

package excelExporter.excelParser;

import excelExporter.main.ParameterSet;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;
import org.apache.logging.log4j.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.format.*;
import org.apache.poi.ss.formula.eval.NotImplementedException;
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.hssf.util.*;
//import org.apache.poi.xssf.usermodel.*;
import excelExporter.excelParser.dataModel.*;


/**
 * Read the contents of an Excel file into the StringTemplate V4 data model.
 */
public class ExcelParser
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(ExcelParser.class);

    /** The error counter for the running parsing process. */
    private final ErrorCounter errCnt_;

    /** The set of user parameters, which say everything about what and how to parse. */
    private final ParameterSet p_;

    /** The name/path of the currently parsed Excel file. Mainly used for progress
        reporting. */
    private FileExt file_ = null;

    /** The index of the workbook under progress as found as POI object {@link #wb_}. The
        index relates to the user specified list of workbooks inside {@link #p_}. */
    private int idxWb_ = -1;

    /** The POI workbook object under progress. */
    private Workbook wb_ = null;

    /** The index of the default worksheet template inside of {@link #p_}. Or -1 if no such
        default template is defined. Or -2 if the search for the default template didn't
        take place yet. */
    private int idxWorksheetDefaultTemplate_ = -2;

    /** A cache of "identifierfied" worksheet names. We use a cahce rather than doing the
        operation on the fly in order to have better control of user feedback. The
        make-name-identifier operation should be reported but only once per name. The map
        needs to be discarded when a workbook is read and prior to reading the next one. */
    private Map<Integer,String> mapOfSheetNameByIdx_ = null;


    /** A sub-class implements the iterative search of all worksheets matching given
        cirteria. */
    private class WorksheetIteration implements Iterator<Integer>
    {
        /** The iteration is implemented by linear walk through all worksheets of the book.
            Here's the next worksheet to test by index. */
        private int idxSheet_;

        /** The regular expression used for iteration. */
        final private Pattern reTabName_;

        /**
         * Create an iteration, which visits all worksheets of the workbook, whose name
         * match a given regular expression.
         *   @throws PatternSyntaxException
         * The syntax of {@code reWorksheetName} is not checked. An exception is thrown for
         * invalid expressions.
         *   @param reWorksheetName
         * The regular expression, which needs to match against the whole name of the
         * worksheet.
         */
        public WorksheetIteration(String reWorksheetName)
            throws PatternSyntaxException
        {
            /* Comparison should be case insensitive. Excel doesn't permit to have two
               sheets of same name with only differing case. */
            reTabName_ = Pattern.compile(reWorksheetName, Pattern.CASE_INSENSITIVE);
            idxSheet_ = 0;

        } /* End of WorksheetIteration.WorksheetIteration */


        /**
         * Look for the next matching worksheet and store a reference for later retrieval
         * with {@link #next}.
         *   @return Get the Boolean answer whether at least one more matching worksheet
         * exists in the workbook.
         */
        @Override public boolean hasNext()
        {
            /* How many sheets are there in the workbook? */
            final int noSheets = wb_.getNumberOfSheets();

            while(idxSheet_ < noSheets)
            {
                /* Not unconditionally matching against the read tab title but first making
                   it an identifier is justified only by consistency considerations. A
                   similar mechanism is used when column attributes are applied to columns
                   by regexp match against the column titles. Here too, the match is made
                   against the already "identifierfied" titles. */
                String nameOfSheet = getSheetNameAsIdent(idxSheet_);

                Matcher m = reTabName_.matcher(nameOfSheet);
                if(m.matches())
                {
                    _logger.debug( "WorksheetIteration.hasNext: Worksheet {} at index {}"
                                   + " matches regular expression {}"
                                 , nameOfSheet
                                 , idxSheet_
                                 , reTabName_
                                 );
                    return true;
                }
                else
                    ++ idxSheet_;
            }

            return false;

        } /* End WorksheetIteration.hasNext */


        /**
         * Get the next matching worksheet.
         *   @return The worksheet is returned by index. The index relates to the POI
         * workbook object {@link #wb_}.
         *   @throws NoSuchElementException
         * Throws this exception if the method is called too often.
         */
        @Override public Integer next()
            throws NoSuchElementException
        {
            if(idxSheet_ < wb_.getNumberOfSheets())
                return Integer.valueOf(idxSheet_++);
            else
                throw new NoSuchElementException();

        } /* End WorksheetIteration.next */


        /**
         * Unsupported method of the interface.
         *   @throws UnsupportedOperationException
         * Always throws this exception. Never call this method.
         */
        @Override public void remove()
            throws UnsupportedOperationException, IllegalStateException
        {
            throw new UnsupportedOperationException();

        } /* End WorksheetIteration.remove */


    } /* End of class WorksheetIteration */


    /**
     * Create an Excel parser. The object can be reused to parse several Excel files.
     *   @param userParams
     * The user specified application parameter set. Contains all details of what and how
     * to parse.
     *   @param errCnt
     * A client supplied error counter. The use case is to permit consecutive error
     * counting across different phases of parsing and different input files.
     */
    public ExcelParser(ParameterSet userParams, ErrorCounter errCnt)
    {
        /* Set user parameters and invalidate the search for the default template in the
           parameter set. */
        p_ = userParams;
        idxWorksheetDefaultTemplate_ = -2;

        errCnt_ = errCnt;
    }


    /**
     * Basic string operation: Make some text compatible with the JSON format as defined
     * for String objects. In fact, the non-printable characters are replaced by escape
     * sequences.
     */
    private static String jsonStringify(String anyText)
    {
        String jsonStr = anyText;
        int idxSrc
          , idxDest = 0;
        final int len = anyText.length();
        for(idxSrc=0; idxSrc<len; ++idxSrc, ++idxDest)
        {
            int c;
            String subst = "";
            int lenSubst = 0;
            switch(anyText.charAt(idxSrc))
            {
                /* "case"-conditions: We use the best-readable escape notation for the
                   commonly used special characters.
                     This table of substitutions can be found, e.g., at
                   https://learn.microsoft.com/en-us/sql/relational-databases/json/how-for-json-escapes-special-characters-and-control-characters-sql-server?view=sql-server-ver16,
                   visited Sep 5, 2024. */
                case '\"': subst = "\""; lenSubst = 1; break;
                case '\\': subst = "\\"; lenSubst = 1; break;
                case '/': subst = "/"; lenSubst = 1; break;
                case '\b': subst = "b";  lenSubst = 1; break;
                case '\f': subst = "f";  lenSubst = 1; break;
                case '\n': subst = "n";  lenSubst = 1; break;
                case '\r': subst = "r";  lenSubst = 1; break;
                case '\t': subst = "t";  lenSubst = 1; break;
                
                /* Otherwise: Check for remaining non-printable characters and substitute by
                   numerical escape. */
                default:
                    c =  anyText.charAt(idxSrc); 
                    if(c < 0x20  ||  c >= 0x7F)
                    {
                        subst = String.format("u%04x", c);
                        lenSubst = subst.length();
                    }
            }
            
            if(lenSubst > 0)
            {
                jsonStr = jsonStr.substring(0, idxDest)
                          + "\\" + subst
                          + jsonStr.substring(idxDest + 1);
                idxDest += lenSubst;
            }
        } /* for(All charaters of input string) */

        return jsonStr;
        
    } /* jsonStringify */
    
    
    
    /**
     * Parse a cell.<p>
     *   The POI cell object is represented as a cell object of the data model.
     *   @return Get the new CellObject.
     *   @param poiCell The POI cell object.
     *   @param evaluator The formula evaluator to be applied of formula cells.
     */
    private CellObject parseCell(Cell poiCell, FormulaEvaluator evaluator)
    {
        /* Create a blank cell. */
        CellObject cell = new CellObject();

        /* Make some general cell properties accessible from the data model. */
        cell.i0Row = poiCell.getRowIndex();
        cell.iRow = cell.i0Row+1;
        cell.i0Col = poiCell.getColumnIndex();
        cell.iCol = cell.i0Col+1;
        Comment comment = poiCell.getCellComment();
        if(comment != null)
        {
            /// @todo Check: From some samples it looks as if comment.getString() would
            // return a string starting with the initials of the author and followed by a
            // colon and the comment text and if comment.getAuthor() would only return the
            // word Author.
            cell.comment = comment.getString().getString().trim();
            cell.authorOfComment = comment.getAuthor().trim();
        }
        /// @todo Access to color unclear. Using ColorColor() we don't know how to proceed
        // to get a meaningful representation, e.g. name. Using Color(), we don't see the
        // returned value resonably representing the visible color in the Excel file: For
        // foreground color we only saw either 0 or 64 regardless of the colors. 64 seems
        // to say: automatic, not set at all. For background color we only saw 64.
        //   Going from color to either XSSFColor or HSSFColor and then to getIndex(), seems
        // to yield same numbers as directly using Color(); this not completely tried but
        // it's reasonable anyway.
        //Color color = poiCell.getCellStyle().getFillBackgroundColorColor();
        //if(color != null)
        //{
        //    _logger.info("" + color);
        //    // There's HSSF and XSSF
        //    XSSFColor xssfColor = XSSFColor.toXSSFColor(color);
        //    short idxColor = poiCell.getCellStyle().getFillBackgroundColor();
        //
        //    String backgroundColor = "" + color.toString() + ", " + idxColor + ", "
        //                             + xssfColor.getIndex();
        //    _logger.info("Cell color: " + backgroundColor);
        //}

        /* The next level of if can be avoided by using policy RETURN_BLANK_AS_NULL; the
           distinction between empty and missing cells will be irrelevant for almost all
           applications. */
        if(poiCell.getCellType() != Cell.CELL_TYPE_BLANK)
        {
            /* FormulaEvaluator.evaluate may throw an exception in case of complex Excel
               worksheets with unsupported constructs. In these cases we replace the cell
               by a blank cell. In the given context this is considered just a warning;
               there are other (properly handled) data errors which also lead to ignoring
               the cell contents as column title without emitting an error. */
            CellValue cellValue;
            try
            {
                cellValue = evaluator.evaluate(poiCell);
                assert cellValue != null: "evaluator.evaluate returned null";
            }
            catch(java.lang.RuntimeException ex)
            {
                errCnt_.warning();
                _logger.warn( "Cell ({},{}) can't be evaluated by Apache POI. The cell"
                              + " is handled like a cell with Excel data error. Caught"
                              + " error: {}"
                            , poiCell.getRowIndex()+1
                            , poiCell.getColumnIndex()+1
                            , ex.getMessage()
                            );
                cellValue = CellValue.getError(1);
                assert cellValue.getCellType() == Cell.CELL_TYPE_ERROR;
            }

            switch(cellValue.getCellType())
            {
            case Cell.CELL_TYPE_BOOLEAN:
                cell.type = CellObject.CellType.bool;
                cell.bool = cellValue.getBooleanValue();
                cell.text = "" + cell.bool;
                cell.n = Double.valueOf(cell.bool? 1.0: 0.0);
                break;

            case Cell.CELL_TYPE_NUMERIC:
                double cellAsNum = cellValue.getNumberValue();
                cell.n = Double.valueOf(cellAsNum);
                if(DateUtil.isCellDateFormatted(poiCell))
                {
                    cell.type = CellObject.CellType.date;
                    Date date = DateUtil.getJavaDate(cellAsNum);
                    String dateFmt = poiCell.getCellStyle().getDataFormatString();
                    /// @todo Here we need some heuristic filtering: dateFmt tends to contain leading characters like [bla bla] and the trailing character sequence ;@
                    cell.text = new CellDateFormatter(dateFmt).format(date);
                    //cell.text = new SimpleDateFormat("dd.MM.yyyy").format(date);
                    _logger.debug( "Cell ({},{}): Date format is {}, formatted date is {}"
                                 , cell.iRow, cell.iCol
                                 , dateFmt
                                 , cell.text
                                 );
                    cell.date = Calendar.getInstance();
                    cell.date.setTime(date);
                }
                else
                {
                    cell.type = CellObject.CellType.real;
                    cell.text = "" + cellAsNum;
                }

                /* Set Boolean interpretation of numeric value. */
                cell.bool = cellAsNum != 0.0;

                break;

            case Cell.CELL_TYPE_STRING:
                String text = cellValue.getStringValue().trim();
                if(text.length() > 0)
                {
                    cell.type = CellObject.CellType.text;
                    cell.text = text;
                    cell.bool = text.equalsIgnoreCase("true")
                                ||  text.equalsIgnoreCase("yes")
                                ||  text.equalsIgnoreCase("okay")
                                ||  text.equalsIgnoreCase("ok");

                    /// @todo We should try a number conversion here. If n is set, then d will be derived below
                }
                else
                {
                    /* Defaults of class CellObject should be approriate. */
                    assert cell.type == CellObject.CellType.blank && !cell.isNotBlank
                           &&  cell.text == null  && !cell.bool;
                }
                break;

            case Cell.CELL_TYPE_ERROR:
                cell.type = CellObject.CellType.error;
                cell.text = "#error in cell";
                break;

            case Cell.CELL_TYPE_BLANK:
            /* CELL_TYPE_FORMULA will never happen as we did the formula evaluation. */
            case Cell.CELL_TYPE_FORMULA:
            default:
                assert false;
                cell.type = CellObject.CellType.error;
                cell.text = "#internal error";
                errCnt_.error();
                _logger.fatal("parseCell: Internal error, unexpected cell format received");
                break;
            }

            /* Find the best fitting integer representation for a real number and refine
               the numeric type. */
            if(cell.n != null)
            {
                assert cell.d == null;

                if(cell.n.compareTo(Double.valueOf(Long.MIN_VALUE)) >= 0
                   &&  cell.n.compareTo(Double.valueOf(Long.MAX_VALUE)) <= 0
                  )
                {
                    long d = Long.valueOf(cell.n.longValue());
                    cell.d = d;

                    /* Make type definition more precise by distiguishing real from integer
                       numbers. */
                    if(cell.type == CellObject.CellType.real
                       &&  Double.valueOf(d).compareTo(cell.n) == 0
                      )
                    {
                        cell.type = CellObject.CellType.integer;
                        cell.text = "" + d;
                    }
                } /* End if(Real number can be rounded to integer number?) */

            } /* End if(Cell has a numeric value?) */

            /* Set the Boolean type flag for support of conditional template code. */
            assert !cell.isNotBlank;
            cell.isNotBlank = true;
            switch(cell.type)
            {
            case text:
                cell.isText = true;
                break;
            case integer:
                cell.isInt = true;
                break;
            case real:
                cell.isReal = true;
                break;
            case date:
                cell.isDate = true;
                break;
            case bool:
                cell.isBool = true;
                break;
            case error:
                cell.isError = true;
                break;
            case blank:
                cell.isNotBlank = false;
                break;

            case undefined:
            default:
                assert false;
            }

            /* Support of code generation: The text contents of a cell are converted to
               identifiers. */
            if(cell.isText)
            {
                assert cell.text != null  &&  !cell.text.trim().isEmpty();
                cell.ident = Identifier.identifierfy(cell.text, /* isStrict */ false);
                cell.identEquals = cell.text.equals(cell.ident);
                cell.identStrict = Identifier.identifierfy(cell.text, /* isStrict */ true);
                cell.identStrictEquals = cell.text.equals(cell.identStrict);
                cell.jsonString = jsonStringify(cell.text);
                
                // @todo Should we try a conversion text -> number? Many existing Excel sheets suffer from bad formatting; as number literal is typed but stored as text
            }
            else
            {
                assert cell.ident == null  &&  cell.identStrict == null
                       && !cell.identEquals && !cell.identStrictEquals;
            }
        }
        else
        {
            /* Defaults of class CellObject should be approriate. */
            assert cell.type == CellObject.CellType.blank && !cell.isNotBlank
                   &&  cell.text == null  && !cell.bool;

        } /* End if(Can the cell be formula evaluated?) */

        /* Add the cell contents in form of a map that serves as Boolean query for a
           articular text content of the cell. */
        String mapKey = "";
        if(cell.text != null)
            mapKey = cell.text.trim();
        else
            mapKey = "";
        if(!mapKey.isEmpty())
        {
            /* Add the pair (cell content text, true) to the querying map "is". The
               template can use a construct like:
                 <if(row.ColTitle.is.someString)>// Cell contains "someString" <endif> */
            cell.is = new HashMap<String,Boolean>(1);
            cell.is.put(mapKey, Boolean.valueOf(true));
        }
            
        _logger.debug("parseCell: Found {}", cell);
        return cell;

    } /* End of parseCell */



    /**
     * Get the name of a worksheet by index into the currently open workbook. If this is
     * user-demanded the it'll be modified to be an identifier.
     *   @return
     * Get the name as String.
     *   @param idxSheet
     * The index of the sheet in {@link #wb_}, which must be an open, read workbook.
     */
    private String getSheetNameAsIdent(int idxSheet)
    {
        assert wb_ != null  &&  idxSheet < wb_.getNumberOfSheets();

        if(mapOfSheetNameByIdx_ == null)
            mapOfSheetNameByIdx_ = new HashMap<Integer,String>();

        final Integer integerIdxSheet = Integer.valueOf(idxSheet);
        String sheetName = mapOfSheetNameByIdx_.get(integerIdxSheet);
        if(sheetName != null)
        {
            /* The name is in the cache, just return it. */
            return sheetName;
        }
        else
        {
            sheetName = wb_.getSheetName(idxSheet);
            if(p_.workbookAry.get(idxWb_).worksheetNamesAreIdentifiers)
            {
                final String sheetNameIdent = Identifier.identifierfy( sheetName
                                                                     , /* isStrict */ false
                                                                     );
                if(!sheetNameIdent.equals(sheetName))
                {
                    _logger.info( "Worksheet name read from Excel tab is modified"
                                  + " from {} to {} to make it an identifier"
                                , sheetName, sheetNameIdent
                                );
                    sheetName = sheetNameIdent;
                }
            }

            /* Put the name in the cache for later reuse without issueing the message
               again. */
            mapOfSheetNameByIdx_.put(integerIdxSheet, sheetName);
        }

        return sheetName;

    } /* End of getSheetNameAsIdent */



    /**
     * Read a single worksheet from the POI stream into the data model.
     *   @return The parse result or null if parsing completely failed.
     *   @param idxSheet
     * The index of the sheet in {@link #wb_}. It needs to be validated by the caller.
     *   @param idxWorksheetTemplate
     * The worksheet template to be applied by index into {@code p_.worksheetTemplateAry}
     * or -1 if basic default parsing settings should be applied.
     *   @param givenName
     * The user specified a name to be used in the data model for the parsed worksheet.
     * null if no given name was specified.
     *   @param readNameIsIdent
     * If the name is read from from the Excel input (i.e. if {@code givenName} is null)
     * then it can be made an identifier.
     */
    private ExcelWorksheet parseXlsSheet( int idxSheet
                                        , int idxWorksheetTemplate
                                        , String givenName
                                        , boolean readNameIsIdent
                                        )
    {
        /* Retrieve the worksheet template. */
        final ParameterSet.WorksheetTemplate wshTmpl;
        if(idxWorksheetTemplate >= 0)
            wshTmpl = p_.worksheetTemplateAry.get(idxWorksheetTemplate);
        else
            wshTmpl = null;

        FormulaEvaluator evaluator = wb_.getCreationHelper().createFormulaEvaluator();

        /* Get a reference to a sheet. The sheet needs to exist, the index has been checked
           before. */
        final Sheet sheet = wb_.getSheetAt(idxSheet);
        assert sheet != null;
        final String sheetName = getSheetNameAsIdent(idxSheet)
                   , logContext = file_.getName() + ", " + sheet.getSheetName() + ": ";

        _logger.debug("{}Parsing worksheet {}, {}", logContext, idxSheet, sheetName);
        ExcelWorksheet worksheet = null;

        /* See if the sheet contains any rows. This code has been taken from a POI sample
           and is somewhat unclear. Why can't we simply iterate according to the getRowNum
           methods? */
        if(sheet.getPhysicalNumberOfRows() > 0)
        {
            /* Create a map of column titles. */
            ColumnTitleMgr colTitleMgr = new ColumnTitleMgr( p_
                                                           , errCnt_
                                                           , logContext
                                                           , wb_
                                                           , idxSheet
                                                           , idxWorksheetTemplate
                                                           );

            /* The name of the worksheet object can be given or read from the Excel file
               and it can be made an identifier. */
            worksheet = new ExcelWorksheet( errCnt_
                                          , logContext
                                          , givenName != null? givenName: sheetName
                                          );
            worksheet.tabName = sheetName;
            assert file_ != null;
            worksheet.excelFile = file_;

            /* Special case: A worksheet doesn't have a collection of row object containers
               but one and only one. The index is always set fix to first. */
            worksheet.setIndexInCollection(0);

            /* Iterate along all rows in the user specified area. The user specified
               boundaries of the parsed area are all null based and inculding indexes.
               getLastRowNum() is inclusive. */
            final int idxEndRow = sheet.getLastRowNum() + 1;
            _logger.debug("Worksheet has " + sheet.getPhysicalNumberOfRows()
                          + " physical rows and " + idxEndRow + " actual rows"
                         );
            for(int idxRow=0; idxRow<idxEndRow; ++idxRow)
            {
                /* Skip the row, which had been used for parsing the column titles. */
                if(idxRow == colTitleMgr.getIdxColumnTitleRow())
                    continue /* for(idxRow: All parsed data rows) */;

                /* Skip the rows, which are not in the included set of those. */
                if(wshTmpl != null  &&  !wshTmpl.isRowSupported(idxRow+1))
                    continue /* for(idxRow: All parsed data rows) */;

                final Row row = sheet.getRow(idxRow);
                if(row != null)
                {
                    final int idxFirstCell = row.getFirstCellNum()
                            , idxLastCell = row.getLastCellNum(); /* Exclusive! */
                    assert idxFirstCell >= 0  || idxLastCell <= 0;
                    assert idxFirstCell == -1  ||  idxFirstCell < idxLastCell;

                    _logger.debug( "Parsing row {}/{} with {} cells"
                                 , idxRow+1
                                 , idxEndRow
                                 , idxLastCell-idxFirstCell
                                 );

                    RowObject rowObj = null;
                    for(int idxCell=idxFirstCell; idxCell<idxLastCell; ++idxCell)
                    {
                        Cell poiCell = row.getCell(idxCell, Row.RETURN_NULL_AND_BLANK);
                        if(poiCell != null)
                        {
                            /* We need to extract the coordinates as getCell always uses a 0
                               based index of cell objects but the returned cell may relate
                               to another column. */
                            final int idxCol = poiCell.getColumnIndex();
                            final Integer idxColInt = Integer.valueOf(idxCol);

                            /* Consider the user specified area. It's not stated in the POI
                               API documentation if the cells are ordered in raising column
                               index, so it's not safe to break the loop when we reach the
                               upper index of the area. (And due to the chosen
                               representation of the area we don't easily know this upper
                               index.) Solution: We process all but skip not included
                               columns. */
                            if(wshTmpl != null  &&  !wshTmpl.isColSupported(idxCol+1))
                                continue; /* for(idxCell: All cells in the row) */

                            /* The column title is the name of the property of the new row
                               object. If no such title has been defined then we need to
                               take a generic name. */
                            String propName = colTitleMgr.getColumnTitle(idxColInt);
                            assert propName != null;

                            /* Read the cell and add it under the column title in the row
                               object.
                                 Empty cells must not be added. Using Excel it's quite
                               intransparent if we have no cell or a cell with no content,
                               this can e.g. depend on the history of editing. If we add
                               empty cells than our data model inherits this intransparency
                               and template writing becomes more error prone (more than it
                               anyway is); one would permanently have to query the cell
                               property isBlank to take the right decision.
                                 Not adding blank cells has the disadvantage that some
                               common fields for cells like row and column index are not
                               available for blank cells. */
                            final CellObject cellObj = parseCell(poiCell, evaluator);
                            if(cellObj.type != CellObject.CellType.blank)
                            {
                                if(rowObj == null)
                                    rowObj = new RowObject(errCnt_, logContext, idxRow);
                                rowObj.putCell(propName, cellObj);
                            }
                            else
                            {
                                _logger.debug( "Row {}, column {}: Blank cell is not added"
                                               + " to the data model"
                                             , idxRow+1, idxColInt+1
                                             );
                            } /* End if(Cell not empty?) */
                        }
                        else
                        {
                            /* The spreadsheet cell is missing in the worksheet. This is
                               not an error, just an empty cell. */
                            _logger.debug("Cell {} is missing", idxCell+1);

                        } /* End if(Cell not empty?) */

                    } /* End for(All cells in the row) */

                    if(rowObj != null)
                        worksheet.addRowWithPath(rowObj, colTitleMgr);
                    else
                    {
                        _logger.debug( "Row {}/{} is empty in the specified column range"
                                     , idxRow+1, idxEndRow
                                     );
                    }
                }
                else
                {
                    _logger.debug("Row {}/{} is empty", idxRow+1, idxEndRow);

                } /* End if(Row object exists?) */
            } /* End for(All rows) */

            /* The sorting of the data elements in the model is done now if reasonably
               possible, i.e. if no error happened so far. */
            if(errCnt_.getNoErrors() == 0)
                worksheet.sort(colTitleMgr);
        }
        else
        {
            errCnt_.warning();
            _logger.warn( "{}: Worksheet {}, {}, has no physical rows. This worksheet is not"
                          + " added to the data model for rendering"
                        , logContext
                        , idxSheet, sheetName
                        );

        } /* End if(Worksheet not empty?) */

        return worksheet;

    } /* End of parseXlsSheet */



    /**
     * Open and read an Excel workbook into the local POI workbook object for further
     * processing.
     *   @return
     * The Boolean result. Only if the method returns true the POI workbook representation
     * {@link #wb_} can be used for further evaluation. Errors have been reported and
     * counted in case of false.
     *   @param idxWorkbook
     * The index of the Excel file in the array of input file specifications in {@link #p_}.
     */
    private boolean readXlsFile(int idxWorkbook)
    {
        assert idxWorkbook < p_.workbookAry.size();
        ParameterSet.WorkbookDesc excelFileDesc = p_.workbookAry.get(idxWorkbook);

        file_ = new FileExt(excelFileDesc.fileName);
        _logger.debug("Reading Excel workbook file {}", file_.getAbsolutePath());

        /* Open the file as a POI stream. */
        FileInputStream inputStream = null;
        boolean success;
        try
        {
            inputStream = new FileInputStream(file_.getAbsolutePath());

            /* The WorkbookFactory decides, which of the supported Excel formats the file has
               and creates a Workbook object of the appropriate class. If it doesn't find a
               supported format it'll throw an InvalidFormatException exception. */
            assert wb_ == null: "Previously opened POI workbook had not been closed";
            wb_ = WorkbookFactory.create(inputStream);
            idxWb_ = idxWorkbook;
            success = true;
        }
        catch(IOException ex)
        {
            success = false;
            errCnt_.error();
            _logger.error( "{}: Can't open input file. {}"
                         , file_.getName()
                         , ex.getMessage()
                         );
        }
        catch(InvalidFormatException ex)
        {
            success = false;
            errCnt_.error();
            _logger.error( "{}: Can't read input file. {}"
                         , file_.getName()
                         , ex.getMessage()
                         );
        }

        if(inputStream != null)
        {
            try{inputStream.close();}
            catch(IOException e){}
        }

        assert mapOfSheetNameByIdx_ == null;
        return success;

    } /* End readXlsFile */


    /**
     * Report an ambiguous association of a worksheet with a worksheet template.
     *   @param nameOfSheet The name of the worksheet.
     *   @param template The second matching worksheet template.
     */
    private void errorAmbiguousTemplateMatch( String nameOfSheet
                                            , ParameterSet.WorksheetTemplate template
                                            )
    {
        errCnt_.error();
        _logger.error( "The association of worksheet {} with a worksheet"
                       + " template is ambiguous. Template {} would match,"
                       + " too"
                     , nameOfSheet
                     , template
                     );
    } /* End errorAmbiguousTemplateMatch */



    /**
     * Find the default worksheet template if any is defined by the user.
     *   @return
     * The worksheet template is returned as index into array {@code
     * p_.worksheetTemplateAry}. -1 is returned if no defualt worksheet template exists.
     */
    private int getWorksheetDefaultTemplate()
    {
        /* The result of a former search is cached. This is simple as p_ is final from
           object creation and the default template never changes. */
        if(idxWorksheetDefaultTemplate_ >= -1)
            return idxWorksheetDefaultTemplate_;

        /* Look for the default template: It is anonymous and doesn't refer to a worksheet
           by either tab or index. */
        assert idxWorksheetDefaultTemplate_ == -2;
        if(p_.worksheetTemplateAry != null)
        {
            for(int idxTmpl=0; idxTmpl<p_.worksheetTemplateAry.size(); ++idxTmpl)
            {
                final ParameterSet.WorksheetTemplate tmpl =
                                                        p_.worksheetTemplateAry.get(idxTmpl);
                if(tmpl.name == null  &&  tmpl.reTabName == null  &&  tmpl.index < 0)
                {
                    if(idxWorksheetDefaultTemplate_ == -2)
                    {
                        idxWorksheetDefaultTemplate_ = idxTmpl;
                        _logger.debug("{}. worksheet template has been identified as"
                                      + " default worksheet template"
                                     , idxTmpl+1
                                     );
                    }
                    else
                    {
                        errCnt_.error();
                        _logger.error("The default worksheet template is ambiguous. The {}."
                                      + " and the {}. specified template would match"
                                     , idxWorksheetDefaultTemplate_+1
                                     , idxTmpl+1
                                     );
                    }
                }
            }
        } /* End if(At least one worksheet template is defined on the command line?) */

        /* If there's no default template specified we at least record that we'd already
           looked for the default template. */
        if(idxWorksheetDefaultTemplate_ == -2)
            idxWorksheetDefaultTemplate_ = -1;

        assert idxWorksheetDefaultTemplate_ >= -1;
        return idxWorksheetDefaultTemplate_;

    } /* End getWorksheetDefaultTemplate */


    /**
     * Find the worksheet template, with given name.
     *   @return
     * The worksheet template is returned as index into array {@code
     * p_.worksheetTemplateAry}. An error is reported and -1 is returned if no worksheet
     * template with given name exists.
     *   @param templateName
     * The name of the template.
     */
    private int getWorksheetTemplate(String templateName)
    {
        for(int idxTmpl=0; idxTmpl<p_.worksheetTemplateAry.size(); ++idxTmpl)
        {
            final ParameterSet.WorksheetTemplate tmpl = p_.worksheetTemplateAry.get(idxTmpl);
            if(tmpl.name != null  && tmpl.name.equals(templateName))
            {
                /* Double check consistency of user input. It's probably a mistake if the
                   found and directlc addressed template is configured for rule rule based
                   association with whatever other worksheets. */
                if(tmpl.reTabName != null  ||  tmpl.index != -1)
                {
                    errCnt_.warning();
                    _logger.warn("Worksheet template {} is directly addressed to by name but"
                                 + " it is configured for rule based association; either"
                                 + " the tab or the index of the targeted worksheet is"
                                 + " set"
                                , tmpl
                                );
                }
                return idxTmpl;
            }
        }

        /* No such template has been found. In case of direct template addressing no
           default template is considered. Report an error. */
        errCnt_.error();
        _logger.error("Worksheet template {} is demanded for worksheet parsing but no"
                      + " such template is defined"
                     , templateName
                     );

        return -1;

    } /* End getWorksheetTemplate(String templateName) */



    /**
     * Find the worksheet template, which is associated with a given worksheet by implicit
     * rule. This includes the association with a default template if any is defined.
     *   @return
     * The worksheet template is returned as index into array {@code
     * p_.worksheetTemplateAry}. -1 is returned if an error ocurred (invalid, contradictory
     * or incomplete user input in {@link #p_}) or if no worksheet template is associated,
     * so that parsing should be done with simple default settings.
     *   @param idxSheet
     * The index of a worksheet in the open workbook {@link #wb_}. The template is searched,
     * which belongs to this worksheet.
     */
    private int getWorksheetTemplate(int idxSheet)
    {
        assert idxSheet < wb_.getNumberOfSheets();

        /* The template, which fits to the given worksheet is determined rule based. This
           means we look for a template, which refers to the given worksheet by regular
           expression match of the worksheet tab or by index. Both at a time is considered
           an error. */

        /* Get the name of the worksheet.
             Not unconditionally matching against the read tab title but first making
           it an identifier is justified only by consistency considerations. A
           similar mechanism is used when column attributes are applied to columns
           by regexp match against the column titles. Here too, the match is made
           against the already "identifierfied" titles. */
        String nameOfSheet = wb_.getSheetName(idxSheet);
        if(p_.workbookAry.get(idxWb_).worksheetNamesAreIdentifiers)
            nameOfSheet = Identifier.identifierfy(nameOfSheet, /* isStrict */ false);

        /* Iterate along all user defined worksheet templates. Take first matching. Report if
           there are more, ambiguous matches. */
        int idxTmpl, idxFound = -1;
        if(p_.worksheetTemplateAry != null)
        {
            for(idxTmpl=0; idxTmpl<p_.worksheetTemplateAry.size(); ++idxTmpl)
            {
                final ParameterSet.WorksheetTemplate tmpl =
                                                        p_.worksheetTemplateAry.get(idxTmpl);

                /* Optimization by caching precompiled regular expressions is not an issue
                   here, we iterate through a few dozen of user specified objects at
                   maximum. */
                if(tmpl.reTabName != null)
                {
                    assert tmpl.index == -1;

                    /* Comparison should be case insensitive. Excel doesn't permit to have two
                       sheets of same name with only differing case. */
                    Pattern reTabName;
                    try
                    {
                        reTabName = Pattern.compile(tmpl.reTabName, Pattern.CASE_INSENSITIVE);
                        Matcher m = reTabName.matcher(nameOfSheet);
                        if(m.matches())
                        {
                            if(idxFound == -1)
                            {
                                idxFound = idxTmpl;
                                _logger.info("Worksheet {} is associated with worksheet"
                                             + " template {} by regular expression match"
                                             + " with {}"
                                            , nameOfSheet
                                            , tmpl
                                            , tmpl.reTabName
                                            );
                            }
                            else
                                errorAmbiguousTemplateMatch(nameOfSheet, tmpl);
                        }
                    }
                    catch(PatternSyntaxException ex)
                    {
                        /* A bad regular expression can easily happen as it is direct user
                           input. It's considered a parsing error since this happens surely
                           not by intention. */
                        errCnt_.error();
                        _logger.error("Worksheet template {} refers to a worksheet tab by bad"
                                      + " regular expression {}. {}"
                                     , tmpl
                                     , tmpl.reTabName
                                     , ex.getMessage()
                                     );

                        /* No further action, this template is skipped. */
                    }
                }
                else if(tmpl.index != -1)
                {
                    assert tmpl.reTabName == null;

                    /* The user specified index is one based. */
                    if(tmpl.index == idxSheet+1)
                    {
                        if(idxFound == -1)
                        {
                            idxFound = idxTmpl;
                            _logger.info("Worksheet {} is associated with worksheet"
                                         + " template {} by index {}"
                                        , nameOfSheet
                                        , tmpl
                                        , tmpl.index
                                        );
                        }
                        else
                            errorAmbiguousTemplateMatch(nameOfSheet, tmpl);
                    }
                }
                else
                {
                    /* Either this is the default template or it is a template properly
                       specified for direct reference by name. */
                }
            } /* End for(All user defined templates) */
        } /* End if(At least one worksheet template is defined on the command line?) */

        /* Look for a possibly defined default template if we have no match so far. */
        if(idxFound == -1)
        {
            idxFound = getWorksheetDefaultTemplate();
            if(idxFound != -1)
            {
                _logger.info("Worksheet {} is associated with the default worksheet"
                             + " template"
                            , nameOfSheet
                            );
            }
        }

        return idxFound;

    } /* End getWorksheetTemplate(int idxSheet) */



    /**
     * Find the worksheet template, which is associated with a given worksheet. This
     * includes the association with a default template if any is defined.
     *   @return
     * The worksheet template is returned as index into array {@code
     * p_.worksheetTemplateAry}. -1 is returned if an error ocurred (invalid, contradictory
     * or incomplete user input in {@link #p_}) or if no worksheet template is associated,
     * so that parsing should be done with simple default settings.
     *   @param idxSheet
     * The index of a worksheet in the open workbook {@link #wb_}. The template is searched,
     * which belongs to this worksheet.
     *   @param templateName
     * The right template can be directly adressed to by name. If this parameter is not
     * null then only a template with this name will be returned. It is considered an error
     * if no such template exists.
     */
    private int getWorksheetTemplate(int idxSheet, String templateName)
    {
        assert wb_ != null;
        if(templateName != null)
        {
            /* The user explicitly stated which template to use. */
            return getWorksheetTemplate(templateName);
        }
        else
        {
            /* The template, which fits to the given worksheet is determined rule based. */
            return getWorksheetTemplate(idxSheet);
        }
    } /* End getWorksheetTemplate(int idxSheet, String templateName) */



    /**
     * Reading a workbook requires to know a list of worksheets to be parsed. The list of
     * sheets is derived from the user input by matching actual sheet names against user
     * specified regular expressions and by looking for sheets by user specified index.
     * This method resolves the user input into a list of sheet indexes.<p>
     *   Parsing a worksheet requires the association with a worksheet template. Each of
     * the sheets in the list can have another template so this method returns the
     * associated template together with the worksheet index.<p>
     *   The worksheet reference of the workbook object, that let to pair of sheet to parse
     * and template to apply is a third return value.
     *   The POI workbook object {@link #wb_} needs to be existent. All user input is taken
     * from {@link #p_}.
     *   @return Get the list as tripple of integers. Each first value is the index of the
     * worksheet in the workbook, each second one is the index of the worksheet template in
     * {@code p_.worksheetTemplateAry} and each third one is the index of the worksheet
     * refernce in {@code p_.workbookAry.worksheetRefAry}.<p>
     *   The list can be empty if there are not matching worksheets. If so then this method
     * will report it as an error.
     */
    private ArrayList<int[]> compileListOfWorksheets()
    {
        ParameterSet.WorkbookDesc excelFileDesc = p_.workbookAry.get(idxWb_);

        ArrayList<int[]> list = new ArrayList<>();

        /* For each workbook on the command line any number of references to (specific)
           worksheets can have been made. For the given workbook, loop along all of these
           references. */
        if(excelFileDesc.worksheetRefAry != null  &&  excelFileDesc.worksheetRefAry.size() > 0)
        {
            for(int idxWsRef=0; idxWsRef<excelFileDesc.worksheetRefAry.size(); ++idxWsRef)
            {
                ParameterSet.WorkbookDesc.WorksheetRef wsRef =
                                                excelFileDesc.worksheetRefAry.get(idxWsRef);

                /* The worksheet can be referenced by name or by index. */
                if(wsRef.reTabName != null)
                {
                    /* The worksheet(s) is selected by regular expression, which is matched
                       against the tabs in the workbook. Actually, this can yield a set of
                       worksheets. */
                    try
                    {
                        WorksheetIteration itByRE = new WorksheetIteration(wsRef.reTabName);

                        /* There should be at least one matching worksheet; this seems to
                           be the user's expectation. */
                        if(itByRE.hasNext())
                        {
                            /* Loop along all worksheets with matching name. */
                            do
                            {
                                int[] listElement = new int[3];
                                listElement[0] = itByRE.next().intValue();
                                assert listElement[0] >= 0;

                                /* Now look for the associated worksheet template. This
                                   yields either the index of the template in p_ or -1 if
                                   default settings should be used or an error appeared. */
                                listElement[1] = getWorksheetTemplate
                                                            ( listElement[0]
                                                            , wsRef.worksheetTemplateName
                                                            );
                                listElement[2] = idxWsRef;

                                list.add(listElement);
                            }
                            while(itByRE.hasNext());
                        }
                        else
                        {
                            /* No matching worksheet. */
                            errCnt_.error();
                            _logger.error( "{}: No worksheet matching regular expression"
                                           + " {} found in workbook"
                                         , file_.getName()
                                         , wsRef.reTabName
                                         );
                        }
                    }
                    catch(PatternSyntaxException ex)
                    {
                        /* A bad regular expression can easily happen as it is direct user
                           input. It's considered a parsing error since this happens surely
                           not by intention. */
                        errCnt_.error();
                        _logger.error("Worksheet reference {} contains the bad"
                                      + " regular expression {}. {}. No worksheet is selected"
                                     , wsRef
                                     , wsRef.reTabName
                                     , ex.getMessage()
                                     );
                    }
                }
                else
                {
                    /* The worksheet is selected by index. */

                    /* Either by name or by index is mandatory. */
                    assert wsRef.index >= 1;

                    /* Validate if the index is in range. The user specified index is one
                       based. */
                    if(wsRef.index <= wb_.getNumberOfSheets())
                    {
                        int[] listElement = new int[3];
                        listElement[0] = wsRef.index-1;
                        assert listElement[0] >= 0;
                        listElement[1] = getWorksheetTemplate( listElement[0]
                                                             , wsRef.worksheetTemplateName
                                                             );
                        listElement[2] = idxWsRef;
                        list.add(listElement);
                    }
                    else
                    {
                        errCnt_.error();
                        _logger.error("Worksheet reference {} contains the bad worksheet"
                                      + " index {}. The workbook only contains {} sheets"
                                     , wsRef
                                     , wsRef.index
                                     , wb_.getNumberOfSheets()
                                     );
                    }
                } /* End if(worksheet referenced by name or by index?) */
            } /* End for(All worksheet references for the given workbook) */
        }
        else
        {
            /* Add all contained sheets to the list. */
            final int noSheets = wb_.getNumberOfSheets();
            for(int idx=0; idx<noSheets; ++idx)
            {
                int[] listElement = new int[3];
                listElement[0] = idx;
                assert listElement[0] >= 0;
                listElement[1] = getWorksheetTemplate( listElement[0]
                                                     , /* worksheetTemplateName */ null
                                                     );
                listElement[2] = -1;
                list.add(listElement);
            }
        }

        if(list.size() == 0)
        {
            /* No matching worksheet at all, this is surely not the user's expectation. */
            errCnt_.error();
            _logger.error( "{}: No worksheet in the workbook matches the selection"
                           + " criteria"
                         , file_.getName()
                         );
        }

        return list;

    } /* End of compileListOfWorksheets */




    /**
     * Read an Excel workbook based on the complex user parameters.
     *   @return
     * The parsing result is returned as a workbook object of the StringTemplate V4 data
     * model. If an error occurs, then null is may be returned instead. If no error is
     * reported through the agreed {@link ErrorCounter} object {@link #errCnt_} then the
     * returned object is not null and it contains at least one parsed worksheet.
     *   @param mapOfWorksheetGroupsByName
     * The parsed workbook contains the parsed worksheets. These can be held a second time
     * in the global worksheet groups. The parser updates the passed in map accordingly.
     *   @param idxFile
     * The user specified application parameter set {@link #p_} contains an array of user
     * demanded Excel input files. This is the index of the parsed file into that array.
     */
    public ExcelWorkbook parseXlsFile( Map< /* groupName */ String
                                          , /* group */     ObjectList<ExcelWorksheet>
                                          > mapOfWorksheetGroupsByName
                                     , int idxFile
                                     )
    {
        /* Read the Excel file into memory. From now we can directly access the POI object
           wb_. */
        if(!readXlsFile(idxFile))
            return null;

        assert idxFile == idxWb_  &&  idxFile >= 0  &&  idxFile < p_.workbookAry.size();
        ParameterSet.WorkbookDesc excelFileDesc = p_.workbookAry.get(idxFile);

        /* The bad case no input file specified is filtered by the command line
           evaluation. */
        assert excelFileDesc.fileName != null;

        /* Create a still empty workbook object. The parsed worksheets will be added to
           this object. */
        ExcelWorkbook workbook = new ExcelWorkbook(errCnt_);
        workbook.setExcelFile(new FileExt(excelFileDesc.fileName));

        /* The name of the workbook in the data model is normally set by the user but
           can also be derived from the file name. This is handled inside setName. */
        workbook.setName(excelFileDesc.name);

        /* Compile the list of worksheets to be read from the user input and associate the
           appropriate worksheet templates. This list can then be linearly processed one by
           one. */
        ArrayList<int[]> worksheetList = compileListOfWorksheets();

        for(int[] idxAry: worksheetList)
        {
            final int idxSheet = idxAry[0]
                    , idxTemplate = idxAry[1]
                    , idxWsRef = idxAry[2];

            /* Via the (optional) worksheet reference the user may explicitly give a name to
               the parsed worksheet. Can be null otherwise. */
            final String wsName = idxWsRef >= 0
                                  ? excelFileDesc.worksheetRefAry.get(idxWsRef).name
                                  : null;

            /* Should a worksheet name read from the tab in the Excel file be made an
               identifier? */
            final boolean wsNameIsIdent = excelFileDesc.worksheetNamesAreIdentifiers;

            /* Run the worksheet parser. It might return null in case of errors. */
            ExcelWorksheet worksheet = parseXlsSheet( idxSheet
                                                    , idxTemplate
                                                    , wsName
                                                    , wsNameIsIdent
                                                    );

            /* In case of errors or if the sheet is empty we can get null and there's
               nothing left to do. */
            if(worksheet == null)
                continue; // for(int[] idxAry: worksheetList)

            worksheet.setParent(workbook);

            /* Add a parsed worksheet to the parse result so far. This might fail if we
               have a name conflict. */
            workbook.putSheet(worksheet);

            /* Retrieve the worksheet template. */
            final ParameterSet.WorksheetTemplate wshTmpl =
                                             idxTemplate >= 0
                                             ? p_.worksheetTemplateAry.get(idxTemplate)
                                             : null;

            /* On user demand the worksheet is also added to one of the global groups. */
            String groupName = p_.defaultWorksheetGroup;
            if(wshTmpl != null  &&  wshTmpl.worksheetGroup != null)
                groupName = wshTmpl.worksheetGroup;
            if(groupName != null)
            {
                ObjectList<ExcelWorksheet> groupList = mapOfWorksheetGroupsByName
                                                      .get(groupName);
                if(groupList == null)
                {
                    groupList = new ObjectList<ExcelWorksheet>(new Identifier(groupName));
                    mapOfWorksheetGroupsByName.put(groupName, groupList);
                }
                _logger.debug( "Add worksheet {} of workbook {} to global group {}"
                             , worksheet.toString()
                             , workbook
                             , groupName
                             );
                groupList.add(worksheet);
            }

        } /* for(All worksheets to be parsed) */

        /* Destroy POI representation of Excel workbook, which is no longer used. */
        idxWb_ = -1;
        wb_ = null;

        /* The cache of sheet names must not be reused with future workbooks. */
        mapOfSheetNameByIdx_ = null;

        /* It is impossible to get an error free parse result being null. */
        assert errCnt_.getNoErrors() > 0  ||  workbook != null;

        /* The sorting of the worksheets in the workbook is done now if wanted and
           reasonably possible, i.e. if no error happened so far. */
        if(p_.sortOrderWorksheets != SortOrder.Order.undefined  &&  errCnt_.getNoErrors() == 0)
        {
            /* Sort the linear list of sheets in this workbook. */
            workbook.sort(p_.sortOrderWorksheets);
        }

        return workbook;

    } /* End of parseXlsFile */


} /* End of class ExcelParser definition. */






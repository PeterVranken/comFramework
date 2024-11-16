/**
 * @file ColumnTitleMgr.java
 * This class manages the titles of the colums, which are the names of the properties of the
 * row objects in the data model at the same time.
 *   - Column titles are read from the worksheet
 *   - Column titles are set explicitly
 *   - Anonymous columns get generic names
 *   - Column titles can be queried
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
/* Interface of class ColumnTitleMgr
 *   ColumnTitleMgr
 *   cellToString
 *   getRow
 *   readColumnTitlesFromWorksheet
 *   putColTitle
 *   recordGroupingCol
 *   recordSortedPropertyCol
 *   applyColumnAttribs
 *   createGenericColumnTitle
 *   getColumnTitle
 *   getGroupingPathScheme
 *   getPropertySortingScheme
 */

package excelExporter.excelParser;

import excelExporter.main.ParameterSet;
import java.util.*;
import java.util.regex.*;
import org.apache.logging.log4j.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.format.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.*;
import org.apache.poi.xssf.usermodel.*;
import excelExporter.main.Pair;
import excelExporter.excelParser.dataModel.Identifier;



/**
 * This class manages the titles of the colums, which are the names of the properties of the
 * row objects in the data model at the same time.
 * - Column titles are read from the worksheet
 * - Column titles are set explicitly
 * - Anonymous columns get generic names
 * - Column titles can be queried
 */

public class ColumnTitleMgr
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(ColumnTitleMgr.class);

    /** The set of user parameters, which say everything about what and how to parse. */
    private final ParameterSet p_;

    /** A counter for errors and warnings in title management. */
    private final ErrorCounter errCnt_;

    /** A formatted string used to precede all logging statements of this module. */
    private final String logCtx_;

    /** The POI object representing the workbook under progress. */
    private final Workbook wb_;

    /** The POI object representing the worksheet in {@link #wb_} under progress. */
    private final Sheet wsh_;

    /** The refernce to the applied worksheet template in {@link #p_} or null if default
        settings should be applied. */
    private final ParameterSet.WorksheetTemplate wshTmpl_;

    /** The row number of the selected column title row. Or -1 if no such row is selected
        or if no such row exists. */
    private int idxColTitleRow_ = -1;

    /** The map of associations between a column index and the title/name of this column. */
    private final Map<Integer,String> mapOfColTitles_ = new HashMap<Integer,String>();

    /** The relevant attributes of grouping columns as user-specified on the command line. */
    public class ColAttribs
    {
        /** The null based index of the column in the worksheet. */
        final public int idx_;

        /** The title of the column as found in the worksheet or as given by the user. */
        public String title_ = null;

        /** The sort order of the groups, which are made by the differing entries found in
            this column of the worksheet. */
        final public SortOrder.Order sortOrder_;

        /** The sort priority of the column in comparison to other sorting columns. Only
            relevant for property columns. */
        final int sortPriority_;

        /**
         * Constructor for incomplete object as required during first gathering of
         * information. The title of the column doesn't need to be finalized when using
         * this constructor. It's set to null instead.
         *   @param idxCol
         * The null based index of the column in the Excel worksheet.
         *   @param sortOrder
         * The user-demanded sort order either for a group or row object with respect to
         * its relationship to its siblings inside their common container.
         *   @param sortPrio
         * The sort priority of this column in comparison to other sorting columns. Only
         * relevant for property columns.
         */
        public ColAttribs(int idxCol, SortOrder.Order sortOrder, int sortPrio)
        {
            idx_ = idxCol;
            sortOrder_ = sortOrder;
            sortPriority_ = sortPrio;
            
        } /* End of ColAttribs */

    } /* End of class ColAttribs */


    /** The handling of nested groups of row objects requires an ordered list of row object
        properties (equivalent to column titles), which have grouping characteristics. The
        list of these forms the grouping scheme or path.<p>
          This is the list of grouping columns and their revelant attributes. Method {@link
        #getGroupingPathScheme} will use this list to return the grouping scheme. */
    private List<ColAttribs> groupingColAry_ = null;

    /** Sorting row objects is done according to the properties, which are related to the
        columns. Sorting is supported by comparators provided by the column manager. The
        manager keeps track of all sorted columns/properties in this list. */
    private LinkedList<ColAttribs> sortedPropColAry_ = null;

    /**
     * A new instance of ColumnTitleMgr is created.
     *   @param userParams
     * The user specified application parameter set. Contains all details of what and how
     * to parse.
     *   @param errCnt
     * Later errors and warnings will be counted in this error counter.
     *   @param logContext
     * A string used to precede all logging statements of this module. Pass null if not
     * needed.
     *   @param wb
     * The POI workbook object representing the Excel workbook to whom this column manager
     * relates.
     *   @param idxSheet
     * The index of the sheet in {@code wb}. It needs to be validated by the caller.
     *   @param idxWorksheetTemplate
     * The worksheet template to be applied by index into {@code
     * userParams.worksheetTemplateAry} or -1 if basic default parsing settings should be
     * applied.
     */
    public ColumnTitleMgr( ParameterSet userParams
                         , ErrorCounter errCnt
                         , String logContext
                         , Workbook wb
                         , int idxSheet
                         , int idxWorksheetTemplate
                         )
    {
        p_ = userParams;
        errCnt_ = errCnt;
        logCtx_ = logContext != null? logContext: "";
        wb_ = wb;

        /* Get a reference to the sheet in progress. The sheet needs to exist, the index
           has to be checked by the caller. */
        wsh_ = wb.getSheetAt(idxSheet);
        assert wsh_ != null;

        /* Get the reference to the applied worksheet template in the user parameters. */
        wshTmpl_ = idxWorksheetTemplate >= 0
                        ? userParams.worksheetTemplateAry.get(idxWorksheetTemplate)
                        : null;

        /* Scan workbook for first shot of how to name the columns. */
        readColumnTitlesFromWorksheet();

        /* Update the index/title mapping by possible user-specified aliases. Record
           grouping and sorting schemes. */
        applyColumnAttribs();

    } /* End of ColumnTitleMgr.ColumnTitleMgr. */



    /**
     * Return a cell of the worksheet as String.
     *   @return Get the cell contents as String or null if an evaluation was not possible.
     * This will happen in case of Excel formula errors.
     *   @param cell The POI cell object.
     *   @param evaluator The formula evaluator to be applied of formula cells.
     */
    private String cellToString(Cell cell, FormulaEvaluator evaluator)
    {
        String contents;

        /* FormulaEvaluator.evaluate may throw an exception in case of complex Excel
           worksheets with unsupported constructs. In these cases we replace the cell by a
           blank cell. In the given context this is concidered just a warning; there are
           other (properly handled) data errors which also lead to ignoring the cell
           contents as column title without emitting an error. */
        CellValue cellValue;
        try
        {
            /* Policy RETURN_BLANK_AS_NULL (as opposed to RETURN_NULL_AND_BLANK) is
               required here for unconditional application of a formula evaluator to the
               cell. */
            cellValue = evaluator.evaluate(cell);
            assert cellValue != null: "evaluator.evaluate returned null";
        }
        catch(NotImplementedException ex)
        {
            errCnt_.warning();
            _logger.warn( "{}Cell ({},{}) is specified to contain"
                          + " the column title but it can't be evaluated. A generic column"
                          + " name will be used instead. Caught error: {}"
                        , logCtx_
                        , cell.getRowIndex()+1
                        , cell.getColumnIndex()+1
                        , ex.getMessage()
                        );
            cellValue = new CellValue("");
            assert cellValue.getCellType() == Cell.CELL_TYPE_STRING;
        }

        switch(cellValue.getCellType())
        {
        case Cell.CELL_TYPE_BOOLEAN:
            errCnt_.warning();
            _logger.warn( "{}Cell ({},{}) is specified to contain"
                          + " the column title but is of type boolean. A generic column"
                          + " name will be used instead"
                        , logCtx_
                        , cell.getRowIndex()+1
                        , cell.getColumnIndex()+1
                        );
            contents = null;
            break;

        case Cell.CELL_TYPE_NUMERIC:
            errCnt_.warning();
            _logger.warn( "{}Cell ({},{}) is specified to contain"
                          + " the column title but is of numeric type. A generic column"
                          + " name will be used instead"
                        , logCtx_
                        , cell.getRowIndex()+1
                        , cell.getColumnIndex()+1
                        );
            contents = null;
            break;

        case Cell.CELL_TYPE_STRING:
            contents = cellValue.getStringValue().trim();
            if(contents.length() <= 0)
                contents = null;
            break;

        case Cell.CELL_TYPE_ERROR:
            contents = null;
            break;

        case Cell.CELL_TYPE_BLANK:
        case Cell.CELL_TYPE_FORMULA:
            /* CELL_TYPE_BLANK will never occur as we used policy RETURN_BLANK_AS_NULL;
               CELL_TYPE_FORMULA will never happen as we did the formula evaluation. */
        default:
            assert false;
            contents = null;
            errCnt_.error();
            _logger.fatal( "{}Cell ({},{}): Unexpected cell format received"
                         , logCtx_
                         , cell.getRowIndex()+1
                         , cell.getColumnIndex()+1
                         );
            break;
        }

        _logger.debug( "{}Cell ({},{}): Found \"{}\" as column title"
                     , logCtx_
                     , cell.getRowIndex()+1
                     , cell.getColumnIndex()+1
                     , contents
                     );
        return contents;

    } /* End of cellToString */



    /**
     * Find and return a row of the worksheet {@link #wsh_} under progress by index. Take
     * the first non-empty row of a worksheet, if the index is unknown.
     *   @return Get the wanted row or null if no such row exists.
     *   @param idxRow The index of the row. May be {@code <0} to say that the first
     * non-empty row is meant.
     */
    private Row getRow(int idxRow)
    {
        assert idxColTitleRow_ == -1;
        Row row = null;
        if(wsh_.getPhysicalNumberOfRows() > 0)
        {
            if(idxRow >= 0)
            {
                /* Directly access the row by index and see if it exists. */
                row = wsh_.getRow(idxRow);
                if(row != null)
                    idxColTitleRow_ = idxRow;
            }
            else
            {
                /* Iterate along all rows but stop at the first none empty one. */
                final int lastRowNum = wsh_.getLastRowNum();
                for(idxRow=0; idxRow<=lastRowNum; ++idxRow)
                {
                    row = wsh_.getRow(idxRow);
                    if(row != null)
                    {
                        idxColTitleRow_ = idxRow;
                        _logger.info( "{}Row {} is used as column title row"
                                    , logCtx_
                                    , idxColTitleRow_+1
                                    );
                        break;
                    }
                }
            }
        }

        assert row == null  &&  idxColTitleRow_ == -1
               ||  row != null  &&  idxColTitleRow_ >= 0;
        assert row == null  ||  idxRow < 0  ||  idxRow == row.getRowNum()
               : "Understanding of row numbers is invalid";
        assert idxColTitleRow_ == -1  ||  idxColTitleRow_ == row.getRowNum()
               : "Understanding of row numbers is invalid";
        return row;

    } /* End of getRow */



    /**
     * The column titles are taken from the row of a work sheet.
     */
    private void readColumnTitlesFromWorksheet()
    {
        final boolean readTitlesFromWorksheet;
        final int idxTitleRow;
        if(wshTmpl_ != null)
        {
            assert wshTmpl_.idxTitleRow >= -1;

            readTitlesFromWorksheet = wshTmpl_.idxTitleRow >= 0;
            
            /* idxTitleRow can become -1 to say "take first non empty row". It can also
               become -2 here but then it'll never be used as readTitlesFromWorksheet is
               now false. */
            idxTitleRow = wshTmpl_.idxTitleRow - 1;
        }
        else
        {
            readTitlesFromWorksheet = true;

            /* -1 means "take first non empty row". */
            idxTitleRow = -1;

        } /* End if(Worksheet template available?) */


        /* Not to read any column titles from the workbook data is a user option. */
        if(readTitlesFromWorksheet)
        {
            boolean titleRowFound = false;
            int noTitlesFound = 0;
            assert idxTitleRow >= -1;
            final String rowDesignation = idxTitleRow >= 0
                                          ? ""+(idxTitleRow+1)
                                          : "(first non-empty)";

            /* Look for the line with colunm titles. It might not exist and we get a null. */
            Row row = getRow(idxTitleRow);
            if(row != null)
            {
                assert idxColTitleRow_ >= 0;
                final int idxFirstCell = row.getFirstCellNum()
                        , idxEndCell = row.getLastCellNum(); /* Exclusive. */
                assert idxFirstCell >= 0  || idxEndCell == 0;
                assert idxFirstCell == -1  ||  idxFirstCell < idxEndCell;

                _logger.debug( "{}Parsing row {} as title row with {} cells"
                             , logCtx_
                             , rowDesignation
                             , idxEndCell-idxFirstCell
                             );

                titleRowFound = true;
                FormulaEvaluator evaluator = wb_.getCreationHelper().createFormulaEvaluator();
                for(int idxCell=idxFirstCell; idxCell<idxEndCell; ++idxCell)
                {
                    /* RETURN_BLANK_AS_NULL (as opposed to RETURN_NULL_AND_BLANK) is
                       required for safe and unconditional application of a formula
                       evaluator to the returned cell. */
                    Cell cell = row.getCell(idxCell, Row.RETURN_BLANK_AS_NULL);
                    if(cell != null)
                    {
                        final int idxCol = cell.getColumnIndex();
                        assert idxCol >= 0: "Bad working assumption about POI column indexes";
                        final Integer idxColInt = Integer.valueOf(idxCol);

                        /* Consider the user specified area. It's not stated in the POI
                           API documentation if the cells are ordered in raising column
                           index, so it's not safe to break the loop when we reach the
                           upper index of the area. (And due to the chosen
                           representation of the area we don't easily know this upper
                           index.) Solution: We process all but skip not included
                           columns. */
                        if(wshTmpl_ != null  && !wshTmpl_.isColSupported(idxCell+1))
                            continue; /* for(idxCell: All cells in the row) */

                        /* Extract and add the found column title. */
                        String colTitle = cellToString(cell, evaluator);
                        if(colTitle != null)
                        {
                            ++ noTitlesFound;
                            if(wshTmpl_ != null  && wshTmpl_.columnTitlesAreIdentifiers)
                            {
                                final String colTitleIdent = Identifier.identifierfy
                                                                        ( colTitle
                                                                        , /* isStrict */ false
                                                                        );
                                if(!colTitleIdent.equals(colTitle))
                                {
                                    _logger.info( "{}Column title read from cell ({},{}) is"
                                                  + " modified from {} to {} to make it an"
                                                  + " identifier"
                                                , logCtx_
                                                , idxColTitleRow_+1, idxCol+1
                                                , colTitle, colTitleIdent
                                                );
                                    colTitle = colTitleIdent;
                                }
                            }
                            mapOfColTitles_.put(idxColInt, colTitle);
                        }
                        else
                        {
                            /* Nothing to be done here. cellToString has already emitted an
                               appropriate warning. */
                        }
                    }
                    else
                    {
                        /* Don't comment on this; this is not necessarily a problem: we
                           have other sources for column titles. Emitting a warning/error
                           here is anyway impossible as we can't safely judge, which
                           visible column is affected. Therefore, we can't double check if
                           we have an explicitly set column title. All we can do is
                           emitting a debug level information. */
                        _logger.debug( "{}Row {}, which is expected to contain"
                                       + " a column title, has an empty cell. Data found"
                                       + " in the according column will get a generic name"
                                       + " derived from the column index if this column is"
                                       + " not excluded from processing and if no column"
                                       + " title has been set explicitly"
                                     , logCtx_
                                     , idxColTitleRow_+1
                                     );
                    }
                }
            } /* End if(Title row exists in the sheet) */

            if(!titleRowFound)
            {
                errCnt_.error();
                _logger.error( "{}Row {}, which is specified to hold the column titles,"
                               + " doesn't exist in the worksheet"
                             , logCtx_
                             , rowDesignation
                             );
            }
            else if(noTitlesFound == 0)
            {
                errCnt_.error();
                _logger.error( "{}Row {}, which is specified to hold the column titles,"
                               + " doesn't contain valid cells"
                             , logCtx_
                             , rowDesignation
                             );
            }
        }
        else
        {
            _logger.info( "{}No title row is read from the worksheet", logCtx_);

        } /* End if(Any column titles to be read from the worksheet?) */

    } /* End of readColumnTitlesFromWorksheet */



    /**
     * Add a pair column index, column title to the map of such. If the mapping supersedes
     * another, already made association then an informative message is made as feedback.
     *   @param idxCol The null based column index.
     *   @param title The (new) column title.
     */
    private void putColTitle(int idxCol, String title)
    {
        String aliasedTitle = mapOfColTitles_.put(Integer.valueOf(idxCol), title);
        if(aliasedTitle != null  &&  !aliasedTitle.equals(title))
        {
            _logger.info( "{}User specified column title {} aliases column title {}, which"
                          + " had been read from the worksheet"
                        , logCtx_
                        , title, aliasedTitle
                        );
        }
    } /* End of putColTitle */


    /**
     * Keep track of columns with grouping characteristics. The built-up record determines
     * the later path or grouping scheme. Recording is done in order of calling this
     * method.
     *   @param idxCol
     * The grouping column, identified by null based index.
     *   @param sortOrder
     * The user demanded sort order for the group, i.e. the order which applies to this
     * group in the relationship to its siblings inside the common parent group.
     */
    private void recordGroupingCol(int idxCol, SortOrder.Order sortOrder)
    {
        if(groupingColAry_ == null)
            groupingColAry_ = new LinkedList<ColAttribs>();

        groupingColAry_.add(new ColAttribs(idxCol, sortOrder, /* priority */ -1));

    } /* End of recordGroupingCol */



    /**
     * Keep track of columns without grouping characteristics, which are sorted. The
     * built-up record is the basis for the comparator object applied to the sorting of row
     * objects.
     *   @param idxCol
     * The property column, identified by null based index.
     *   @param sortOrder
     * The user demanded sort order for the column.
     *   @param priority
     * The user specified priority for the property related to the column.
     */
    private void recordSortedPropertyCol(int idxCol, SortOrder.Order sortOrder, int priority)
    {
        assert sortOrder != SortOrder.Order.undefined;
        
        if(sortedPropColAry_ == null)
            sortedPropColAry_ = new LinkedList<ColAttribs>();
            
        /* The columns are collected in the order of sort priority. This sorting is done
           here immediately and not using the algorithms of Collections; the reason is the
           pseudo priority -1, which means the priority is determined by the order of
           appearance of the column specification on the command line. (Earlier appearance
           means higher priority.)
             Sorting of higher priority (lower value) needs to be done later, i.e. larger
           values of priority should come first in the list and columns with pseudo prio
           are added as next at the beginning of the list. */
        if(priority <= 0)
        {
            /* Pseudo priority. */             
            sortedPropColAry_.add( /* index */ 0
                                 , new ColAttribs(idxCol, sortOrder, /* priority */ -1)
                                 );
        }
        else
        {
            /* Explicit priority requires list iteration to find the right position.
               Elements with same priority must not be overtaken (the order of appearances
               decides) and nor must pseudo priorities be overtaken if not required to get
               beyond a larger real priority. */
            ListIterator<ColAttribs> it = sortedPropColAry_.listIterator(/* index */ 0);
            while(it.hasNext())
            {
                /* Look ahead: Find the first real property from here towards the tail.
                   This object doesn't necessarily exist. */
                final ListIterator<ColAttribs> itLookAhead = 
                                                sortedPropColAry_.listIterator(it.nextIndex());
                assert it.nextIndex() == itLookAhead.nextIndex();
                
                int nextRealPrio = -1;
                while(nextRealPrio <= 0  && itLookAhead.hasNext())
                    nextRealPrio = itLookAhead.next().sortPriority_;
                    
                assert nextRealPrio <= 0 || it.nextIndex() < itLookAhead.nextIndex();
                    
                if(nextRealPrio <= 0  ||  nextRealPrio <= priority)
                {
                    /* Position found, insert new object here, i.e. at position of it. */
                    break;
                }
                else
                {
                    /* nextRealPrio is not a pseudo prio && nextRealPrio<priority: Here is
                       not yet the right position, we need to advance. Advancing can take
                       advantage from the look ahead iteration: it points behind the object
                       with the real priority. */
                    it = itLookAhead;
                }
            }
            it.add(new ColAttribs(idxCol, sortOrder, priority));
            
        } /* End if(Pseudo prio or prio by numeric value?) */

    } /* End of recordSortedPropertyCol */



    /**
     * Apply the user-specified column attributes to the column information held in this
     * manager.<p>
     *   Update the set of mappings between column index and column name by applying the
     * user specified aliases (aliasing the column names taken from the cells in the title
     * row in method {@link #readColumnTitlesFromWorksheet}). The mapping is taken from the
     * user input (i.e. application configuration) {@link #p_}.<p>
     *   Build the path scheme for grouping of rows, i.e. collect all columns with grouping
     * characteristics.
     */
    private void applyColumnAttribs()
    {
        /* Don't do anything if parsing is done without applied worksheet template or if
           the template doesn't say anything about columns. */
        if(wshTmpl_ == null  ||  wshTmpl_.columnDescAry == null)
            return;

        /* We make a copy of the index,name map as it is after reading column titles from
           the worksheet and prior to applying the first user alias. The copy is used to
           match user specified column designations by regular expression. Without a copy
           the regular expressions of later handled aliases could match and re-alias
           earlier made aliases. Recursive aliasing is surely unwanted in the user
           interface. A shallow copy is appropriate. */
        final Map<Integer,String> mapOfColTitlesFromSheet =
                                                new HashMap<Integer,String>(mapOfColTitles_);

        /* Recursive aliasing is still possible by having different column attribute
           specifications, all addressing to the same column. We keep track by a temporary
           set of sheet indexes. */
        final Set<Integer> setOfVisitedCols = new HashSet<Integer>();

        /* Iterate along all column attribute specifications in the worksheet template. */
        for(ParameterSet.WorksheetTemplate.ColumnAttributes colAttribs: wshTmpl_.columnDescAry)
        {
            int idxCol = -1;
            if(colAttribs.reTitle != null)
            {
                /* The regular expression is matched against all column titles read
                   from the worksheet. The match needs to be unambiguous. */
                try
                {
                    Pattern reTitle = Pattern.compile(colAttribs.reTitle);

                    int noMatches = 0;
                    for(Map.Entry<Integer,String> entry: mapOfColTitlesFromSheet.entrySet())
                    {
                        Matcher m = reTitle.matcher(entry.getValue());
                        if(m.matches())
                        {
                            /* The meant column is identified. */
                            idxCol = entry.getKey().intValue();
                            assert idxCol >= 0;

                            ++ noMatches;

                        } /* End if(RegExp matches col title found in workbook) */

                    } /* for(All pairs in the map) */

                    if(noMatches != 1)
                    {
                        errCnt_.error();
                        _logger.error("Column attribute specification {} refers to"
                                      + " a column title by regular expression {}. An"
                                      + " unambiguous match is required but {}"
                                      + " matches were found"
                                     , colAttribs
                                     , colAttribs.reTitle
                                     , noMatches
                                     );
                    }
                }
                catch(PatternSyntaxException ex)
                {
                    /* A bad regular expression can easily happen as it is direct user
                       input. It's considered an error since this happens surely
                       not by intention. */
                    errCnt_.error();
                    _logger.error("{}Column attribute specification {} refers to a column"
                                  + " title by bad regular expression {}. {}"
                                 , logCtx_
                                 , colAttribs
                                 , colAttribs.reTitle
                                 , ex.getMessage()
                                 );

                    /* No further action, this column attribute specification is
                       skipped. */
                }
            }
            else
            {
                /* The attributes object needs to address to a column either by regular
                   expression or by index. */
                assert colAttribs.index >= 1;

                /* The association is made by index. */
                idxCol = colAttribs.index-1;

            } /* End if(Association made by title or by index?) */

            if(idxCol >= 0)
            {
                /* set.add: Keep track of the handled column. */
                if(setOfVisitedCols.add(Integer.valueOf(idxCol)))
                {
                    /* The new element was not yet contained in the set. */

                    /* Does this attribute specification define an alias for the column title?
                       If so, update the pair in the map. */
                    if(colAttribs.name != null)
                        putColTitle(idxCol, colAttribs.name);

                    /* Record if this column has grouping or property sorting
                       characteristics. */
                    if(colAttribs.isGroupingColumn)
                    {
                        assert colAttribs.sortPriority == -1;
                        recordGroupingCol(idxCol, colAttribs.sortOrder);
                    }
                    else if(colAttribs.sortOrder != SortOrder.Order.undefined)
                    {
                        assert colAttribs.sortPriority == -1  ||  colAttribs.sortPriority > 0;
                        recordSortedPropertyCol( idxCol
                                               , colAttribs.sortOrder
                                               , colAttribs.sortPriority
                                               );
                    }
                }
                else
                {
                    /* Do not permit applying several instances of a column attribute
                       specification to one and the same column. This is probably undesired
                       and just a mistake in the application configuration. */
                    errCnt_.error();
                    _logger.error( "{}Column attribute specification {} refers"
                                   + " to column {} in the parsed worksheet."
                                   + " However, the attributes of this column had already"
                                   + " been specified before, either by column title"
                                   + " (see --column-title) or by index (see"
                                   + " --column-index)"
                                 , logCtx_
                                 , colAttribs
                                 , idxCol+1
                                 );
                }
            } /* End if(This attribute specification applies to an existing column) */

        } /* End for(All column attribute specifications) */

    } /* End of applyColumnAttribs */


    /**
     * Generate a generic and unambiguous column title and store it in the map.
     *   @return Get the title.
     *   @param idxCol The column index.
     */
    private String createGenericColumnTitle(int idxCol)
    {
        /* Configure the number of tried column names until we give up with error. */
        final int maxAttemptsToDisambiguate = 10000;
        
        final Integer idxColInt = Integer.valueOf(idxCol);
        assert !mapOfColTitles_.containsKey(idxColInt);

        assert idxCol >= 0;
        final String title = "Col" + (idxCol+1);

        /* Disambiguate the title. */
        final Collection<String> setOfNamesSoFar = mapOfColTitles_.values();
        int iDisambiguate = 0;
        String checkedTitle = title;
        while(setOfNamesSoFar.contains(checkedTitle))
        {
            /* Limit the effort in pathologic cases. */
            if(iDisambiguate > maxAttemptsToDisambiguate)
            {
                errCnt_.error();
                _logger.error( "{}No unambiguous, generic title could be found for column {}"
                               + " The name clash with another column disables safe data"
                               + " access from the data model"
                             , logCtx_
                             , idxCol+1
                             );
                break;
            }

            /* Modify title and try again. */
            ++ iDisambiguate;
            checkedTitle = title + "_" + iDisambiguate;
        }

        /* Add the generated and validated title to the map so far. */
        String nullStr = mapOfColTitles_.put(idxColInt, checkedTitle);
        assert nullStr == null;

        return checkedTitle;

    } /* End of createGenericColumnTitle */



    /**
     * Get the row number, which is used as column title row.
     *   @return The row number as returned by POI's Row.getRowNum() or -1 if no row is
     * evaluated for column titles
     */
    public int getIdxColumnTitleRow()
    {
        return idxColTitleRow_;

    } /* End of getIdxColumnTitleRow */



    /**
     * Get the title of a given column by index.
     *   @return Get the title.
     *   @param idxCol The null based column index.
     */
    public String getColumnTitle(int idxCol)
    {
        assert idxCol >= 0: "Bad column index";
        String title = mapOfColTitles_.get(Integer.valueOf(idxCol));
        if(title == null)
        {
            /* Generate a generic column title. */
            title = createGenericColumnTitle(idxCol);

            errCnt_.warning();
            _logger.warn( "{}No title has been found or specified for column {}."
                          + " The generic name {} is used instead"
                        , logCtx_
                        , idxCol+1
                        , title
                        );
        }

        assert title != null;
        return title;

    } /* End of getColumnTitle */


    /**
     * Get the grouping path scheme, i.e. the sorted list of columns, which have grouping
     * characteristics.
     *   @return Get the scheme as an ordered list of column titles or null if no grouping
     * columns are specified.
     */
    public List<ColAttribs> getGroupingPathScheme()
    {
        /* The list has been recorded by column index only; the column title has left
           open. The finalization of the list is done by this function at first
           invokation. */
        if(groupingColAry_ != null  &&  groupingColAry_.get(0).title_ == null)
        {
            _logger.debug("{}getGroupingPathScheme:", logCtx_);
            for(ColAttribs colAttribs: groupingColAry_)
            {
                /* Add the meanwhile finalized column title to the list element. */
                assert colAttribs.title_ == null  &&  colAttribs.idx_ >= 0
                       &&  colAttribs.sortPriority_ == -1;
                colAttribs.title_ = getColumnTitle(colAttribs.idx_);
                
                _logger.debug( "  idx: {}, title: {}, sort order: {}"
                             , colAttribs.idx_
                             , colAttribs.title_
                             , colAttribs.sortOrder_
                             );
            }
        }
        
        return groupingColAry_;

    } /* End of getGroupingPathScheme */



    /**
     * Get the row object sorting scheme, i.e. the sorted list of properties (i.e. column
     * titles), which the object have to be sorted to.
     *   @return Get the scheme as an ordered list of column titles or null if no sorting
     * property columns are specified.
     */
    public List<ColAttribs> getPropertySortingScheme()
    {
        /* The list has been recorded by column index only; the column title has left
           open. The finalization of the list is done by this function at first
           invokation. */
        if(sortedPropColAry_ != null  &&  sortedPropColAry_.get(0).title_ == null)
        {
            _logger.debug("{}getPropertySortingScheme:", logCtx_);
            for(ColAttribs colAttribs: sortedPropColAry_)
            {
                /* Add the meanwhile finalized column title to the list element. */
                assert colAttribs.title_ == null  &&  colAttribs.idx_ >= 0;
                colAttribs.title_ = getColumnTitle(colAttribs.idx_);
                
                _logger.debug( "  idx: {}, title: {}, sort order: {}, priority: {}"
                             , colAttribs.idx_
                             , colAttribs.title_
                             , colAttribs.sortOrder_
                             , colAttribs.sortPriority_
                             );
            }
        }
        
        return sortedPropColAry_;

    } /* End of getPropertySortingScheme */

} /* End of class ColumnTitleMgr definition. */





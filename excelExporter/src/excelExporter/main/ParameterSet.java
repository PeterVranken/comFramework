/**
 * @file ParameterSet.java
 * The application parameter set.
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
/* Interface of class ParameterSet
 *   WorksheetTemplate.getDesignation
 *   defineArguments
 *   isIdent
 *   isIndex
 *   isInIntSet
 *   isRowSupported
 *   isColSupported
 *   parseGetNextArg
 *   parseIntRange
 *   parseStateWorksheetRef
 *   parseStateColumnAttributes
 *   parseStateUserOption
 *   cloneLinkedHashMap
 *   strToSortOrder
 *   parseCmdLine
 *   loadLog4jLogger
 *   toString
 */

package excelExporter.main;

import java.util.*;
import org.apache.logging.log4j.*;
import org.stringtemplate.v4.*;
import applicationInterface.cmdLineParser.CmdLineParser;
import excelExporter.excelParser.ErrorCounter;
import excelExporter.excelParser.SortOrder;


/**
 * The application parameter set.<p>
 *   <b>Remark:</b> The naming convention of indicating local and static members with a
 * trailing or leading underscore is not applied as this class is subject to rendering with
 * StringTemplate. For the same reasons most members are held public.
 */

public class ParameterSet
{
    /** The global logger object for all progress and error reporting. */
    private static Logger _logger = null;

    /** The error counter for parameter parsing. */
    private ErrorCounter errCnt_ = null;

    /** The name of the data cluster. */
    public String clusterName = null;

    /** The Boolean flag, whether the application logger uses the standard file based
        configuration of the Apache log4j 2 package or the programatic configuration
        through the command line arguments of this application.
          @remark This field is not used by {@link ParameterSet} besides that it reports
        the value in toString. The client code of this class is responsible of writing a
        reasonable value into this field. */
    public boolean useStdLog4j2Config = false;
    
    /** The logging level used for this run of the application.
          @remark This field is not used by {@link ParameterSet} besides that it reports
        the value in toString. The client code of this class is responsible of writing a
        reasonable value into this field. */
    public String logLevel = "INFO";
    
    /** The file name of the application log or null if no log file is written.
          @remark This field is not used by {@link ParameterSet} besides that it reports
        the value in toString. The client code of this class is responsible of writing a
        reasonable value into this field. */
    public String logFileName = null;
    
    /** The pattern, according to which the Apache logger will format the log entries
          @remark This field is not used by {@link ParameterSet} besides that it reports
        the value in toString. The client code of this class is responsible of writing a
        reasonable value into this field. */
    public String log4j2Pattern = "%d %-5p - %m%n";

    /** Use verbose mode for template loading. */
    public boolean stringTemplateVerbose = false;

    /** The name of the default group of work sheets. null means there is no such default
        group. */
    public String defaultWorksheetGroup = null;

    /** Sort order of workbooks in the array of such in the data model. */
    public SortOrder.Order sortOrderWorkbooks = SortOrder.Order.undefined;

    /** Sort order of worksheets in all global groups of such in the data model. */
    public SortOrder.Order sortOrderWorksheets = SortOrder.Order.undefined;

    /** A map of user specified template attributes, which appear as code generation
        options in the application's user interface. These attributes of Java type
        String, Boolean, Integer or Double are simply passed through from the
        application's command line to the StringTemplate V4 template and can there be
        used to control the code generation. The use case are optional constructs in
        the generated code, which are controlled from the command line.<p>
          The name of an option/attribute is the key into the map and the
        option/attribute's value is the value of the map entry.<p>
          A LinkedHashMap is applied in order to retain the order of appearance of options
        on the command line in the map.<p>
          This is the global, context free option map. */
    public LinkedHashMap<String,Object> optionMap = new LinkedHashMap<>();

    /** The description of an input Excel workbook. */
    public class WorkbookDesc
    {
        /** The name of the workbook under which the data read from the input file is
            stored in the data model. This name can be used to look for the workbook in a
            map of such. Can be null then the name is derived from the file name. */
        public String name = null;

        /** The file name. */
        public String fileName = null;

        /** If the name of a parsed worksheet is not given explicitly but read from the tab
            in the Excel file then it can be useful to make the read name an identifier
            without blanks and special characters. We have a switch, which relates to all
            worksheets of a book. Explicitly given names are not affected. */
        public boolean worksheetNamesAreIdentifiers = false;

        /** Parsing of the workbook means to make a selection by defining a sub-set of
            worksheets. One referenced worksheet, one element of the sub-set, is selected
            by an instance of this class. */
        public class WorksheetRef
        {
            /** A reference to the owning workbook specification. */
            private final WorkbookDesc owner_;

            /** After parsing, the worksheet will be stored into one or more groups in the
                data model. This is the name under which it is stored. The name is derived
                from the tab's name in the workbook if null is stated. */
            public String name = null;

            /** The worksheet can be addressed to by name or index. It is an error to
                specify none or both. Here we have the reference by name, the regular
                expression needs to match. State null if the reference by index should be
                applied. */
            public String reTabName = null;

            /** The worksheet can be addressed to by name or index. It is an error to
                specify none or both. Here we have the reference by one-based index. State
                -1 if the reference by name should be applied. */
            public int index = -1;

            /** Parsing of a worksheet is controlled by a worksheet template. Here, a
                template can be referenced by name. If null is stated then an implicit,
                rule-based association from a template to this worksheet is made. */
            public String worksheetTemplateName = null;


            /**
             * Create a new worksheet reference in the given book.
             *   @param owner
             * The workbook reference this worksheet reference belongs to.
             */
            protected WorksheetRef(WorkbookDesc owner)
                {owner_ = owner;}


            /**
             * Validate the user specified settings after command line parsing. The first
             * found problem is reported by exception.
             *   @throws CmdLineParser.InvalidArgException
             * The function operates silently. As long as we don't have a problem it will
             * just return. In case of an error it throws an exception.
             */
            protected void validate()
                throws CmdLineParser.InvalidArgException
            {
                /* A context string for feedback from this routine. */
                final String ctx = "Workbook " + owner_ + ", command line context worksheet"
                                   + " selection " + toString() + ": ";
                if(name != null)
                    isIdent(name, ctx);
                if(reTabName == null  &&  index == -1
                   ||  reTabName != null  &&  index != -1
                  )
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx
                               + "A worksheet selection is made either by tab or by index. One"
                               + " of both but not both at a time needs to be specified"
                              );
                }
                else if(index != -1)
                    isIndex(index, ctx);

            } /* End of validate */


            /**
             * Get a suitable textual representation of this worksheet reference.
             *   @return Get a meaning designation as useful for reporting purpose.
             */
            public String toString()
            {
                if(name != null)
                    return name;
                if(reTabName != null)
                    return "tab:" + reTabName;
                if(index != -1)
                    return "index:" + index;
                return "(anonymous)";

            } /* End of WorkbookDesc.WorksheetRef.toString */

        } /* End class WorkbookDesc.WorksheetRef */


        /** Each worksheet to be parsed from the workbook is specified by an entry in this
            array. The worksheets are parsed in the order of this array. If null is stated
            then all worksheets of the workbook are read with help of implicitly associated
            worksheet templates. */
        public ArrayList<WorksheetRef> worksheetRefAry = null;


        /** A map of user specified template attributes, which appear as code generation
            options in the application's user interface. These attributes of Java type
            String, Boolean, Integer or Double are simply passed through from the
            application's command line to the StringTemplate V4 template and can there be
            used to control the code generation. The use case are optional constructs in
            the generated code, which are controlled from the command line.<p>
              The name of an option/attribute is the key into the map and the
            option/attribute's value is the value of the map entry.
              This is the option map in the context of an Excel input file. */
        public LinkedHashMap<String,Object> optionMap = new LinkedHashMap<>();


        /**
         * Create a new worksheet reference, which is owned by the embedding workbook
         * specification.
         *   @return Get the worksheet reference.
         */
        WorksheetRef createWorksheetRef()
        {
            final WorksheetRef wsRef = new WorksheetRef(this);

            /* Add the new worksheet reference object to the collection in the workbook
               specification. */
            if(worksheetRefAry == null)
                worksheetRefAry = new ArrayList<WorksheetRef>();
            worksheetRefAry.add(wsRef);

            return wsRef;

        } /* End of createWorksheetRef */


        /**
         * Validate the user specified settings after command line parsing. The first found
         * problem is reported by exception.
         *   @throws CmdLineParser.InvalidArgException
         * The function operates silently. As long as we don't have a problem it will
         * just return. In case of an error it throws an exception.
         */
        protected void validate()
            throws CmdLineParser.InvalidArgException
        {
            if(fileName == null || fileName.trim().isEmpty())
            {
                throw new CmdLineParser.InvalidArgException("Workbook specification "
                                                            + toString()
                                                            + " contains an empty file name"
                                                           );
            }

            /* Recursively run the validation of the nested worksheet references. */
            if(worksheetRefAry != null)
                for(WorksheetRef wsRef: worksheetRefAry)
                    wsRef.validate();

        } /* End of validate */


        /**
         * Get a suitable textual representation of this workbook specification.
         *   @return Get a meaning designation as useful for reporting purpose.
         */
        public String toString()
        {
            if(name != null)
                return name;
            if(fileName != null)
                return "file:" + fileName;
            return "(anonymous)";

        } /* End of WorkbookDesc.toString */

    } /* End of class WorkbookDesc */


    /** There is a list of worksbook specifications. There will at least a single workbook
        be specified. */
    public ArrayList<WorkbookDesc> workbookAry = new ArrayList<>();


    /** A worksheet template is a collection of parameters, which are required to
        suceessfully parse a single worksheet. It furthermore has a few parameters, which
        permit to associate the template with worksheets actually found in read workbooks. */
    public class WorksheetTemplate
    {
        /** The name of the worksheet template. A worksheet reference from the workbook
            description can select this particular template and avoid the implicit, rule
            based association by refering to this name.<p>
              May be null if this template should be used for implicit association only. */
        public String name;

        /** Implicit, rule based association: If the name of a parsed worksheet matches
            this regular expression then the template is applied.<p>
              Can be null if the worksheet is directly addressed to by name or if implicit
            association should be made by index. */
        public String reTabName = null;

        /** Implicit, rule based association: The one based index of worksheets, which this
            template should be applied to.<p>
              Can be -1 if the worksheet is directly addressed to by name or if implicit
            association should be made by tab name. */
        public int index = -1;

        /** The worksheet can be assigned to a group of such, which is irrelated to the
            workbook files. This way, homogenous workbooks with an inhomogenous set of
            worksheets can by handled: Each worksheet of same kind in any of the
            workbooks will be assigned to the same group. A StringTemplate template can
            then iterate along this group.<p>
              If null and if there is a default group then the worksheet is placed into the
            default group. */
        public String worksheetGroup = null;

        /** A particular row of the worksheet can contain the titles of the columns. These
            can be used as keys, when storing a row's cell contents into a map. And the
            associating of column templates can be done by reference to these titles.<p>
              This is the one based index of the row of column titles. The index may but
            doesn't need to point into the parsed area specified with {@link
            #inclRowIdxAry} and {@link #exclRowIdxAry}.<p>
              If 0 is stated then the first non empty row used.<p>
              If -1 is stated then generic default titles are used. */
        public int idxTitleRow = 0;

        /** If column titles are read from the Excel input then they can be transformed to
            become C identifiers. This switch relates to all column-titles of a worksheet
            and affects only the first value of the titles. Each title can then be aliased
            by a column related attributes specification, which would override the effect of
            the switch here. */
        public boolean columnTitlesAreIdentifiers = false;

        /** The explicitly included rows by one based index. Each array element is a range
            of indexes; a pair of from and to, both including. The array is empty if no
            particular rows should be included by index. Now all rows are included
            implicitly. */
        public ArrayList<Pair<Integer,Integer>> inclRowIdxAry = new ArrayList<>();

        /** The explicitly excluded rows by one based indexes. Each array element is a
            range of indexes; a pair of from and to, both including.<p>
              The array is null if no frames should be excluded by index. If indexes are
            listed as both, ex- and included, then the exclude condition overrules the
            include condition. This is in particular relevant if all rows are implicitly
            included. */
        public ArrayList<Pair<Integer,Integer>> exclRowIdxAry = new ArrayList<>();

        /** The explicitly included columns by one based index. Each array element is a
            range of indexes; a pair of from and to, both including. The array is empty if
            no particular columns should be included by index. Now all columns are included
            implicitly. */
        public ArrayList<Pair<Integer,Integer>> inclColIdxAry = new ArrayList<>();

        /** The explicitly excluded columns by one based indexes. Each array element is a
            range of indexes; a pair of from and to, both including.<p>
              The array is null if no frames should be excluded by index. If indexes are
            listed as both, ex- and included, then the exclude condition overrules the
            include condition. This is in particular relevant if all columns are implicitly
            included. */
        public ArrayList<Pair<Integer,Integer>> exclColIdxAry = new ArrayList<>();


        /** A worksheet template can have a set of column templates of this class. */
        public class ColumnAttributes
        {
            /** The worksheet template by reference, which this column specification
                belongs to. */
            final WorksheetTemplate owner_;

            /** The column title as used in the data model. The data held in the matching
                column can be referenced from a StringTemplate V4 template under this name.
                The use case is to hide natural language titles using blanks and special
                characters, which can be found in typical Excel worksheets.<p>
                  If null is stated then the name is derived from the column title - either
                the one found in the file or the generic one if no title row is specified. */
            public String name = null;

            /** The column template is associated to columns by title or by index. Parsing
                of a column, which matches this regular expression is controlled by this
                column template.<p>
                  Please note, if the other switch {@link #columnTitlesAreIdentifiers} is
                set then the regular expression is matched against the identifier, i.e. the
                title read from the Excel input after modification.<p>
                  State null if the association should be made by index. */
            public String reTitle = null;

            /** The column template is associated to columns by title or by index. Parsing
                of a column is controlled by this column template if the column has this
                one-based index.<p>
                  Can be -1 if the association should be made by column title. */
            public int index = -1;

            /** The column may have grouping characteristics, i.e. the cell contents
                can have the meaning of a path element. */
            public boolean isGroupingColumn = false;

            /** The elements inside a group in the data model can be sorted according to
                the criterion stated here.<p>
                  If the template describes parsing of a grouping column then the group in
                the data model will contain sub-groups. These can be sorted directly.<p>
                  If the template describes parsing of a normal column, then the group in
                the data model will contain rows, which contain data from more than a
                single column. The other involved columns can of course specify a sort
                order, too. This is sorted out by priority, see {@link #sortPriority}. */
            public SortOrder.Order sortOrder = SortOrder.Order.undefined;

            /** For groups in the data model containg row objects: If several columns,
                which relate to different properties of the row object specify a sort order
                then this is the priority. Sorting is first done according to the columns
                with larger priority values then repeated in order of falling priority
                value. Finally, the sort-order with lowest priority value will have
                determined the prinicpal sort-order, i.e. lower value means higher
                priority.
                  A value of -1 means that the priority is derived from the position of the
                column specification on the command line. Earlier specifications denote
                higher priority. */
            public int sortPriority = -1;


            /**
             * Create a new column template in the given worksheet template.
             *   @param owner
             * The worksheet template this column template belongs to.
             */
            protected ColumnAttributes(WorksheetTemplate owner)
                {owner_ = owner;}


            /**
             * Validate the user specified settings after command line parsing. The first
             * found problem is reported by exception.
             *   @throws CmdLineParser.InvalidArgException
             * The function operates silently. As long as we don't have a problem it will
             * just return. In case of an error it throws an exception.
             */
            protected void validate()
                throws CmdLineParser.InvalidArgException
            {
                /* A context string for feedback from this routine. */
                final String ctx = "Worksheet template " + owner_
                                   + ", command line context column attributes "
                                   + toString() + ": ";
                if(name != null  &&  name.trim().isEmpty())
                    throw new CmdLineParser.InvalidArgException(ctx + "Column name is empty");
                if(reTitle == null  &&  index == -1
                   ||  reTitle != null  &&  index != -1
                  )
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx
                               + "Column attributes are associated with actual columns"
                               + " either by column title or by index. One of both but"
                               + " not both at a time needs to be specified"
                              );
                }
                else if(index != -1)
                    isIndex(index, ctx);

                if(isGroupingColumn && sortPriority != -1)
                {
                    throw new CmdLineParser.InvalidArgException
                                    (ctx + "A grouping column can't have a sort priority");
                }
            } /* End of validate */



            /**
             * Get a suitable textual representation of this column template.
             *   @return Get a meaning designation as useful for reporting purpose.
             */
            public String toString()
            {
                if(name != null)
                    return name;
                if(reTitle != null)
                    return "title:" + reTitle;
                if(index != -1)
                    return "index:" + index;
                return "(anonymous)";

            } /* End of WorksheetTemplate.ColumnAttributes.toString */

        } /* End of class ColumnAttributes */

        /** The list of column descriptions. The elements of the list relate to columns
            existing in the worksheet and they are used to attach some additional
            properties to these columns. However, the list of descriptions is not a
            selection of columns for parsing; unmentioned columns will be parsed, too,
            using default settings. */
        public ArrayList<ColumnAttributes> columnDescAry = null;


        /**
         * Create a new column template, which is owned by the embedding worksheet
         * template.
         *   @return Get the template.
         */
        ColumnAttributes createColumnAttributes()
        {
            ColumnAttributes colTmpl = new ColumnAttributes(this);

            /* Add the new column attributes object to the collection in the worksheet
               template. */
            if(columnDescAry == null)
                columnDescAry = new ArrayList<ColumnAttributes>();
            columnDescAry.add(colTmpl);

            return colTmpl;

        } /* End of createColumnAttributes */



        /**
         * Validate the user specified settings after command line parsing. The first found
         * problem is reported by exception.
         *   @throws CmdLineParser.InvalidArgException
         * The function operates silently. As long as we don't have a problem it will just
         * return. In case of an error it throws an exception.
         */
        protected void validate()
            throws CmdLineParser.InvalidArgException
        {
            if(reTabName != null  &&  index != -1)
            {
                throw new CmdLineParser.InvalidArgException
                            ("Bad worksheet template definition " + toString()
                             + " found. Template is"
                             + " configured for rule based association but refers to"
                             + " the targeted worksheet by tab and index at the same"
                             + " time. Only one out of both is permitted"
                            );
            }

            /* Recursively run the validation of the nested column attribute
               specifications. */
            if(columnDescAry != null)
                for(ColumnAttributes colTmpl: columnDescAry)
                    colTmpl.validate();

        } /* End of validate */


        /**
         * Test if a given integer value is in an integer set.
         *   @return Get the Boolean result.
         *   @param s The set as a list of value or ranges of values.
         *   @param i The integer number.
         */
        private boolean isInIntSet(ArrayList<Pair<Integer,Integer>> s, int i)
        {
            for(Pair<Integer,Integer> p: s)
            {
                if(p.second != null)
                {
                    if(p.first.intValue() <= i  &&  i <= p.second.intValue())
                        return true;
                }
                else if(p.first.intValue() == i)
                    return true;
            }

            return false;

        } /* End of WorksheetTemplate.isInIntSet */



        /**
         * Test if a given row is in the set of parsed rows.
         *   @return Get the Boolean result.
         *   @param idxRow The one based index of the row.
         */
        public boolean isRowSupported(int idxRow)
        {
            /* A row is supported if it is not in the excluded set but either in the included
               set or if the included set is not specified. Begin with exclusion. */
            if(isInIntSet(exclRowIdxAry, idxRow))
                return false;

            if(inclRowIdxAry.size() == 0)
                return true;

            return isInIntSet(inclRowIdxAry, idxRow);

        } /* End of WorksheetTemplate.isRowSupported */



        /**
         * Test if a given column is in the set of parsed columns.
         *   @return Get the Boolean result.
         *   @param idxCol The one based index of the column.
         */
        public boolean isColSupported(int idxCol)
        {
            /* A column is supported if it is not in the excluded set but either in the
               included set or if the included set is not specified. Begin with
               exclusion. */
            if(isInIntSet(exclColIdxAry, idxCol))
                return false;

            if(inclColIdxAry.size() == 0)
                return true;

            return isInIntSet(inclColIdxAry, idxCol);

        } /* End of WorksheetTemplate.isColSupported */



        /**
         * Get a suitable textual representation of this worksheet template.
         *   @return Get a meaning designation as useful for reporting purpose.
         */
        public String toString()
        {
            if(name != null)
                return name;
            if(reTabName != null)
                return "tab:" + reTabName;
            if(index != -1)
                return "index:" + index;
            return "(anonymous)";

        } /* End of WorksheetTemplate.toString */

    } /* End of class WorksheetTemplate */


    /** There is a list of worksheet templates. If it is null then all worksheets are
        parsed using default settings. */
    public ArrayList<WorksheetTemplate> worksheetTemplateAry = null;


    /** The pair of template and output file to be rendered on basis of this template. */
    static public class TemplateOutputPair
    {
        /** The template group file to be used. */
        public String templateFileName = null;

        /** The root template in the template group file. */
        public String templateName = null;

        /** The default value for command line argument template-name. */
        private static final String _defaultTemplateName = "renderCluster";

        /** The name of the template argument, holding the network cluster information. */
        public String templateArgNameCluster = null;

        /** The default value for command line argument template-arg-name-cluster. */
        private static final String _defaultTemplateArgNameCluster = "cluster";

        /** The name of the template argument, holding the general information. */
        public String templateArgNameInfo = null;

        /** The default value for command line argument template-arg-name-info. */
        private static final String _defaultTemplateArgNameInfo = "info";

        /** The wrap column for output of iterations in the template. Pass a none positive
            value to have no wrapping at all. */
        public int templateWrapCol = 0;

        /** The file name of the generated name. */
        public String outputFileName = null;

        /** A map of user specified template attributes, which appear as code generation
            options in the application's user interface. These attributes of Java type
            String, Boolean, Integer or Double are simply passed through from the
            application's command line to the StringTemplate V4 template and can there be
            used to control the code generation. The use case are optional constructs in
            the generated code, which are controlled from the command line.<p>
              The name of an option/attribute is the key into the map and the
            option/attribute's value is the value of the map entry.
              This is the option map in the context of a generated output file. */
        public LinkedHashMap<String,Object> optionMap = new LinkedHashMap<>();

    } /* End of class ParameterSet.TemplateOutputPair */


    /** The list of output files plus the template to generate them. */
    public ArrayList<TemplateOutputPair> templateOutputPairAry = new ArrayList<>();


    /**
     * Define all command line arguments.
     *   Define the command line arguments, which are required to fill the application's
     * parameter set.
     *   @param clp
     * The command line parser object.
     */
    static public void defineArguments(CmdLineParser clp)
    {
        clp.defineArgument( "c", "cluster-name"
                          , /* cntMin, cntMax */ 0, 1
                          , /* defaultValue */ "cluster"
                          , "The name of the complete data cluster. Optional, may be"
                            + " given once in the global context. Default is"
                            + " \"cluster\""
                          );
//        clp.defineArgument( "$(point)", ""
//                          , /* cntMin, cntMax */ 0, -1
//                          , /* defaultValue */ null
//                          , ""
//                            + " "
//                            + " "
//                            + " "
//                            + ".\nOptional, default is"
//                            + " "
//                            + ".\nThis parameter is Mandatory"
//                          );
        clp.defineArgument( "vt", "string-template-verbose"
                          , /* cntMax */ 1
                          , "The template engine integrated for output generation has a debug"
                            + " mode to report details of searching addressed templates."
                            + " Use this"
                            + " Boolean switch to enable the verbose mode of StringTemplate."
                            + " See http://www.stringtemplate.org/ for more. Must be given in"
                            + " the global context only. Optional, default is false"
                          );
        clp.defineArgument( "dg", "default-worksheet-group"
                          , /* cntMin, cntMax */ 0, 1
                          , /* defaultValue */ null
                          , "The name of the default group for worksheets. In the data model"
                            + " all parsed worksheets are stored in the context of their"
                            + " workbook but they can additionally be stored in global"
                            + " groups. If no particular group is specified for a worksheet"
                            + " it'll be put into the default group"
                            + ".\nOptional, default is not to have a default group"
                          );
        clp.defineArgument( "sob", "sort-order-of-workbooks"
                          , /* cntMin, cntMax */ 0, 1
                          , /* defaultValue */ SortOrder.Order.undefined.toString()
                          , "The order of appearance of parsed workbooks in the ordered"
                            + " collections in the data model. Ordering relates to the"
                            + " given name of the workbook, which is not necessarily the"
                            + " file name"
                            + ".\nPossible values are:"
                            + "\n- " + SortOrder.Order.lexical.toString()
                            + "\n- " + SortOrder.Order.ASCII.toString()
                            + "\n- " + SortOrder.Order.numerical.toString()
                            + "\n- " + SortOrder.Order.inverseLexical.toString()
                            + "\n- " + SortOrder.Order.inverseASCII.toString()
                            + "\n- " + SortOrder.Order.inverseNumerical.toString()
                            + ".\nOptional, default is not to sort workbooks. The parsing"
                            + " order will be retained"
                          );
        clp.defineArgument( "sos", "sort-order-of-worksheets"
                          , /* cntMin, cntMax */ 0, 1
                          , /* defaultValue */ SortOrder.Order.undefined.toString()
                          , "The order of appearance of parsed worksheets in the ordered"
                            + " collections in the data model. Ordering relates to the"
                            + " given name of the worksheet, which is not necessarily the"
                            + " title string of the Excel tab"
                            + ".\nPossible values are:"
                            + "\n- " + SortOrder.Order.lexical.toString()
                            + "\n- " + SortOrder.Order.ASCII.toString()
                            + "\n- " + SortOrder.Order.numerical.toString()
                            + "\n- " + SortOrder.Order.inverseLexical.toString()
                            + "\n- " + SortOrder.Order.inverseASCII.toString()
                            + "\n- " + SortOrder.Order.inverseNumerical.toString()
                            + ".\nOptional, default is not to sort worksheets. The parsing"
                            + " order will be retained"
                          );

        /* Arguments to specify a workbook. */
        clp.defineArgument( "i", "input-file-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The name of the next Excel workbook file to be parsed. This"
                            + " argument opens the command line context of a new"
                            + " workbook specification. It can be used repeatedly to"
                            + " define several input files"
                            + ".\nThis parameter is optional. By default no Excel file is"
                            + " opened. Not reading an Excel file can be useful with"
                            + " self-contained StringTemplate V4 templates or if they only"
                            + " depend on user options"
                          );
        clp.defineArgument( "b", "workbook-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The given name of a workbook. The parsed workbook will be"
                            + " stored in the data model under this name"
                            + ".\nOptional. If not given then the workbook name is derived"
                            + " from the Excel file name. Must not be given repeatedly in"
                            + " the same workbook context"
                            + ".\nThe use case of this argument is to have a kind of"
                            + " clean alias of the file name, which will easily contain"
                            + " blanks and special characters, which makes the design of a"
                            + " StringTemplate V4 template cumbersome"
                            + ".\nIt's recommended to always use this argument; not using it"
                            + " can lead to StringTemplate V4 templates that depend"
                            + " on the Excel input file name"
                          );
        clp.defineArgument( "si", "worksheet-names-are-identifiers"
                          , /* cntMax */ -1
                          , "If worksheet names are derived from the tab in the Excel file"
                            + " then they can be modified to become identifiers, i.e."
                            + " they only contain letters, digits and the underscore and"
                            + " don't begin with a digit. This mainly supports the common"
                            + " use case where tab titles contain blanks to improve"
                            + " readability of the Excel workbook. This mechanisms is an"
                            + " alternative to the other argument worksheet-name but it has"
                            + " the added value of operating on all parsed sheets of the"
                            + " workbook"
                            + ".\nPlease note, this argument only relates to tab titles"
                            + " read from the input. Aliasing of worksheet names with"
                            + " argument worksheet-name can overrule the identifier-making"
                            + " here"
                            + ".\nOptional, default is to leave worksheet names unchanged."
                            + " The argument can be used once in the command line context"
                            + " of a workbook context"
                          );

        /* Arguments to specify the worksheet selection of a workbook. */
        clp.defineArgument( "ss", "open-worksheet-selection"
                          , /* cntMax */ -1
                          , "This argument opens the command line context of a worksheet"
                            + " selection. It may be given repeatedly in the context of a"
                            + " workbook specification. The new sub-context specifies"
                            + " a single worksheet of the workbook to be parsed"
                            + ".\nIf this context is used at least once for a given workbook"
                            + " then only the specified worksheets are parsed and all others"
                            + " are ignored"
                            + ".\nOptional, if this argument is not used in the context of"
                            + " a workbook specification then all worksheets of the book"
                            + " are parsed"
                          );
        clp.defineArgument( "s", "worksheet-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The given name of a worksheet. The parsed worksheet will be"
                            + " stored in the data model under this name"
                            + ".\nOptional. If not given then the worksheet name is derived"
                            + " from the tab in the Excel file. Must not be given"
                            + " repeatedly in the same worksheet selection context"
                            + ".\nThe use case of this argument is to have a kind of"
                            + " clean alias of the tab's title, which will easily contain"
                            + " blanks and special characters, which makes the design of a"
                            + " StringTemplate V4 template cumbersome"
                          );
        clp.defineArgument( "tab", "worksheet-by-tab"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The worksheet is selected by reference to a tab in the"
                            + " workbook. A regular expression is expected; all worksheets"
                            + " with matching tab title are selected for parsing. The"
                            + " argument can be used once in the context of a worksheet"
                            + " selection"
                            + ".\nPlease note, if the other argument"
                            + " worksheet-names-are-identifiers is given then the regular"
                            + " expression is matched against the forced identifier, i.e."
                            + " the tab title read from the Excel input after modification"
                            + ".\nOptional, the worksheet is selected by index if this"
                            + " argument is not given. One out of this argument or"
                            + " worksheet-by-index is mandatory in the worksheet selection"
                            + " context"
                          );
        clp.defineArgument( "idx", "worksheet-by-index"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ -1
                          , "The worksheet is selected in the workbook by one-based index."
                            + " The argument can be used only in the context of a worksheet"
                            + " selection"
                            + ".\nOptional, the worksheet is selected by tab title if this"
                            + " argument is not given. One out of this argument or"
                            + " worksheet-by-tab is mandatory in the worksheet selection"
                            + " context"
                          );
        clp.defineArgument( "ast", "applied-worksheet-template"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "Parsing of a worksheet is normally done under control of a"
                            + " worksheet template. (A worksheet template is a sub-set of"
                            + " arguments, which form another command line context.) This"
                            + " is the name of the worksheet template, which is to be"
                            + " applied"
                            + ".\nOptional, by default will a suitable worksheet template"
                            + " be associated by rules"
                          );

        /* Arguments to specify the worksheet template. */
        clp.defineArgument( "st", "open-worksheet-template"
                          , /* cntMax */ -1
                          , "This argument opens the command line context of a worksheet"
                            + " template. Any number of worksheet templates can be"
                            + " specified by repeated use of this argument. If no template"
                            + " is specified then all worksheets will be parsed with most"
                            + " simple default settings."
                          );
        clp.defineArgument( "stn", "worksheet-template-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The given name of a worksheet template. A worksheet, which is"
                            + " selected in the command line context of a workbook"
                            + " specification can refer to this particular worksheet"
                            + " template by this name. See other argument"
                            + " applied-worksheet-template, too"
                            + ".\nOptional. If not given then the worksheet template can't"
                            + " be associated explicitly with a selected worksheet. The"
                            + " association can still be made by rules"
                          );
        clp.defineArgument( "atb", "association-by-tab"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "This is the regular expression specifying the title of a"
                            + " possibly existing tab in the workbook. If a matching tab"
                            + " exists and if the worksheet belonging to that tab doesn't"
                            + " explicitly refer to another worksheet template by name then"
                            + " this worksheet template will be associated and control the"
                            + " parsing of that worksheet"
                            + ".\nPlease note, if the other argument"
                            + " worksheet-names-are-identifiers is given then the regular"
                            + " expression is matched against the forced identifier, i.e."
                            + " the tab title read from the Excel input after modification"
                            + ".\nThis argument can be used once in the context of a"
                            + " worksheet template. It must not be given if"
                            + " association-by-index is given in the same context. The"
                            + " argument is optional, default is not to try a"
                            + " rule based association of a template"
                          );
        clp.defineArgument( "ai", "association-by-index"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ -1
                          , "This is the one-based index of a possibly existing tab in the"
                            + " workbook."
                            + " If a worksheet with this index exists and if it doesn't"
                            + " explicitly refer to another worksheet template by"
                            + " name then this worksheet template will be associated and"
                            + " control the parsing of that worksheet"
                            + ".\nThis argument can be used once in the context of a"
                            + " worksheet template. It must not be given if"
                            + " association-by-tab is given in the same context. The"
                            + " argument is optional, default is not to try a"
                            + " rule based association of a template"
                          );
        clp.defineArgument( "g", "group"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The name of a global worksheet group. If this argument is"
                            + " given then the parsed worksheet will not only be found in"
                            + " the workbook but also in the referenced group. Use case is"
                            + " grouping of structural identical worksheets across"
                            + " different input files, which may each contain inhomogeneous"
                            + " sets of structurally differing worksheets"
                            + ".\nOptional, default is not to put the associated worksheet"
                            + " in a global worksheet group. The argument can be used once"
                            + " in the command line context of a worksheet template"
                          );
        clp.defineArgument( "rt", "index-title-row"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ 0
                          , "This is the one based index of a row in the associated, parsed"
                            + " worksheet, which contains the column titles. It doesn't"
                            + " matter if the title row belongs to the set of included rows"
                            + " (see other arguments include-range-of-rows and"
                            + " exclude-range-of-rows)"
                            + ".\nSpecify -1 rather than an index to not read column titles"
                            + " from a row in the worksheet but to use generic column"
                            + " titles instead"
                            + ".\nOptional, default is to read column titles from the first"
                            + " non-empty row. The argument can be used once in the command"
                            + " line context of a worksheet template"
                          );
        clp.defineArgument( "id", "column-titles-are-identifiers"
                          , /* cntMax */ -1
                          , "The column titles read from the title row of the worksheet"
                            + " can be modified to become identifiers, i.e. they only"
                            + " contain letters, digits and the underscore and don't begin"
                            + " with a digit. This mainly supports the common use case"
                            + " where column titles contain blanks to improve"
                            + " maintainability of the Excel table. This mechanisms is an"
                            + " alternative to the other argument column-name but it has"
                            + " the added value of operating on all titles of the worksheet"
                            + ".\nPlease note, this argument only relates to titles read"
                            + " from the input. Aliasing of column names with argument"
                            + " column-name can overrule the identifier-making here"
                            + ".\nOptional, default is to leave column titles unchanged."
                            + " The argument can be used once in the command line context"
                            + " of a worksheet template"
                          );
        clp.defineArgument( "inc", "include-range-of-rows"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "A one-based index of a row or a range (a colon separated pair)"
                            + " of those. The rows with the given index or with an index in"
                            + " the given range (both boundaries are including) are added"
                            + " to the set of included rows. All rows with an index"
                            + " not belonging to this set will be ignored by the parser"
                            + ".\nIf no include condition is given then all rows in the"
                            + " associated, parsed worksheet form the set of included"
                            + " rows"
                            + ".\nThis argument can be used any number of times in the"
                            + " context of a worksheet template specification"
                          );
        clp.defineArgument( "ex", "exclude-range-of-rows"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "A one-based index of a row or a range (a colon separated pair)"
                            + " of those. The rows with the given index or with an index in"
                            + " the given range (both boundaries are including) are added"
                            + " to the set of excluded rows. All rows from the set of"
                            + " included frames with an index not belonging to the set of"
                            + " excluded rows form the set of parsed rows. All other rows"
                            + " in the associated, parsed worksheet will be ignored by the"
                            + " parser"
                            + ".\nThis argument can be used any number of times in the"
                            + " command line context of a worksheet template specification"
                          );
        clp.defineArgument( "icc", "include-range-of-columns"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "A one-based index of a column or a range (a colon separated"
                            + " pair) of those. The columns with the given index or with an"
                            + " index in the given range (both boundaries are including)"
                            + " are added to the set of included columns. All columns with"
                            + " an index not belonging to this set will be ignored by the"
                            + " parser"
                            + ".\nIf no include condition is given then all columns in the"
                            + " associated, parsed worksheet form the set of included"
                            + " columns"
                            + "\n.This argument can be used any number of times in the"
                            + " context of a worksheet template specification"
                          );
        clp.defineArgument( "exc", "exclude-range-of-columns"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "A one-based index of a column or a range (a colon separated"
                            + " pair) of those. The columns with the given index or with an"
                            + " index in the given range (both boundaries are including)"
                            + " are added to the set of excluded columns. All columns from"
                            + " the set of included frames with an index not belonging to"
                            + " the set of excluded columns form the set of parsed columns."
                            + " All other columns in the associated, parsed worksheet will"
                            + " be ignored by the parser"
                            + ".\nThis argument can be used any number of times in the"
                            + " command line context of a worksheet template specification"
                          );

        /* The arguments to specify the column attributes follow up. */
        clp.defineArgument( "ca", "open-column-attributes"
                          , /* cntMax */ -1
                          , "This argument opens the command line context of a column"
                            + " attributes specification. It may be given repeatedly in the"
                            + " context of a worksheet template specification. The new"
                            + " sub-context specifies some properties of a single column in"
                            + " the parsed worksheet"
                            + ".\nThis context can be used any number of times in the"
                            + " context of the worksheet template but it doesn't have an"
                            + " impact on the column selection. A column, which no"
                            + " attributes are specified for will be parsed using default"
                            + " assumptions"
                          );
        clp.defineArgument( "cn", "column-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The given name of a column. The cell value found in the"
                            + " affected column forms a property of the parsed row and this"
                            + " property will be stored in the data model under the name"
                            + " given here"
                            + ".\nThe argument is optional, by default will the column or"
                            + " property name be derived from the column title. The use"
                            + " case of this argument is providing a kind of clean alias"
                            + " for the column title, which will easily contain blanks and"
                            + " special characters, which makes the design of a"
                            + " StringTemplate V4 template cumbersome"
                            + ".\nThis argument can be used once in the command line"
                            + " context of a column attributes specification"
                          );
        clp.defineArgument( "ct", "column-title"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "A regular expression describing the title of a possible column"
                            + " in the parsed worksheet. If a matching column exists then"
                            + " the attributes specified in this context relate to that"
                            + " column"
                            + ".\nPlease note, if the other argument"
                            + " column-titles-are-identifiers is given then the regular"
                            + " expression is matched against the forced identifier, i.e."
                            + " the title read from the Excel input after modification. This"
                            + " argument must not be used if the titles are not read from"
                            + " the Excel input. Matching isn't done against the generic"
                            + " column titles; use column-index instead"
                            + ".\nThe argument is optional, by default will the association"
                            + " of the attributes specification with a particular column be"
                            + " made by index, see other argument column-index"
                            + ".\nThis argument can be used once in the command line"
                            + " context of a column attributes specification. It must not be"
                            + " used together with the other argument column-index in the"
                            + " same context"
                          );
        clp.defineArgument( "ci", "column-index"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ -1
                          , "The index of the associated column in the parsed worksheet. If"
                            + " a column with this index exists then the specified"
                            + " attributes relate to this column"
                            + ".\nThe argument is optional, by default will the association"
                            + " of the attributes specification with a particular column be"
                            + " made by title, see other argument column-title. However,"
                            + " one out of column-title or column-index is mandatory in"
                            + " the command line context of a column attributes specification"
                            + ".\nThis argument can be used once in the command line"
                            + " context of a column attributes specification. It must not be"
                            + " used together with the other argument column-title in the"
                            + " same context"
                          );
        clp.defineArgument( "ig", "is-grouping-column"
                          , /* cntMax */ -1
                          , "The associated column is a grouping column. The cell contents"
                            + " are understood as names of groups of row objects in the"
                            + " data model"
                            + ".\nOptional, by default is a column understood as a property"
                            + " of the row objects"
                            + ".\nThis argument can be used once in the command line"
                            + " context of a column attributes specification"
                          );
        clp.defineArgument( "soc", "sort-order-of-column"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The column related sort order. If it is a grouping column then"
                            + " sorting relates to the groups specified by the column. If"
                            + " it is a normal column then sorting of the row objects in a"
                            + " group will be done according to the row"
                            + " object's property, which is specified by this column"
                            + ".\nPossible values are:"
                            + "\n- " + SortOrder.Order.lexical.toString()
                            + "\n- " + SortOrder.Order.ASCII.toString()
                            + "\n- " + SortOrder.Order.numerical.toString()
                            + "\n- " + SortOrder.Order.inverseLexical.toString()
                            + "\n- " + SortOrder.Order.inverseASCII.toString()
                            + "\n- " + SortOrder.Order.inverseNumerical.toString()
                            + ".\nOptional, default is not to sort row objects"
                            + ".\nThis argument can be used once in the command line"
                            + " context of a column attributes specification"
                          );
        clp.defineArgument( "spc", "sort-priority-of-column"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ -1
                          , "A row object will have several properties. All according"
                            + " columns can use the other argument sort-order-of-column to"
                            + " demand sorting according to the property value. If more"
                            + " than a single column do so then the sorting of row objects is"
                            + " done repeatedly in falling values of this argument, i.e. the"
                            + " lower value means the higher priority. Please state a positive"
                            + " value as priority"
                            + ".\nOptional. All sorted columns, which this argument is not"
                            + " given for, get a sort"
                            + " priority in inverse order of appearance on the command line;"
                            + " earlier appearance means higher priority"
                            + ".\nThis argument must not be used for grouping columns"
                            + ".\nThis argument can be used once in the command line"
                            + " context of a column attributes specification"
                          );

        clp.defineArgument( "op", "user-option-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The name of a user option, which is passed into the"
                            + " StringTemplate V4 template as additional attribute. The"
                            + " value of this attribute can be used in the template to"
                            + " conditionally control code generation from the"
                            + " application command line"
                            + ".\nThe value of the option (or template attribute) is"
                            + " specified with the other command line argument"
                            + " user-option-value"
                            + ".\nThis argument opens the local context of an option"
                            + " specification. It can be a sub-context of the global"
                            + " context, of the workbook specification or of the output"
                            + " generation block. It can be"
                            + " used any number of times in each of these contexts. All"
                            + " options specified in the global context will become"
                            + " default values for all workbooks and output generation"
                            + " blocks, but they may be redefined or overridden in those"
                            + " contexts"
                          );
        clp.defineArgument( "ov", "user-option-value"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "Specify the value of a user option or attribute (see"
                            + " user-option-name)"
                            + ".\nThis is the only other (and optional) command line"
                            + " argument in the sub-context of a user option. Consequently,"
                            + " if given it needs to immediately follow"
                            + " user-option-name"
                            + ".\nThe value is passed into the StringTemplate V4 template"
                            + " as an object of best suiting Java type; numbers are passed"
                            + " in as either Integer or Double and the literals true/false"
                            + " as Boolean. If none of these fits then the template"
                            + " receives a String with the literal text from the command"
                            + " line"
                            + ".\nThis argument is optional. If omitted then the template"
                            + " will receive the value Boolean(true). This means that"
                            + " Boolean code generation control switches can be passed in"
                            + " by simply putting -op <nameOfSwitch>"
                          );


        /* Arguments to specify an output generation block (file and template) */
        clp.defineArgument( "o", "output-file-name"
                          , /* cntMin, cntMax */ 1, -1
                          , /* defaultValue */ null
                          , "The name of a generated output file. This argument opens the"
                            + " context of an output generation block. This block of"
                            + " arguments describes how a single output file is generated"
                            + " from the data model under use of a template file. Any"
                            + " number of output files can be generated if the output"
                            + " generation block is repeatedly given"
                            + ".\nNote, directories missing in the path to the designated"
                            + " file will be created by the application"
                            + ".\nThe generated output can be written into a standard"
                            + " console stream, too. Two file names are reserved to do so:"
                            + "\n- stdout"
                            + "\n- stderr"
                            + "\nIf the output of the information rendering process is"
                            + " redirected to the console then you should raise the logging"
                            + " level to WARN at minimum; if rendering is successful then"
                            + " the rendered data model won't be intermingled with"
                            + " application progress information"
                          );
        clp.defineArgument( "t", "template-file-name"
                          , /* cntMin, cntMax */ 1, -1
                          , /* defaultValue */ null
                          , "The name of the StringTemplate V4 template file, which is"
                            + " applied to render the contents of the network database"
                            + " files. Only group template files are supported. Please,"
                            + " refer to http://www.stringtemplate.org/ for"
                            + " a manual how to write a valid template.\n"
                            + "  The argument must be used once and only once in the context"
                            + " of an output generation block"
                          );
        clp.defineArgument( "tn", "template-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "A StringTemplate V4 template file typically contains a set of"
                            + " (nested) templates. This argument names the root template,"
                            + " which is used by the application to render the contents of"
                            + " the network database files.\n"
                            + "  The argument can be used once in the context"
                            + " of an output generation block.\n"
                            + "  The argument can be used once in the global"
                            + " context, too, then the global values becomes the"
                            + " default for all output generation blocks that do not"
                            + " specify the value themselves. Optional, the default is"
                            + " \"renderCluster\" if this argument is not used at all"
                          );
        clp.defineArgument( "tc", "template-arg-name-cluster"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The contents of the network database files are passed to"
                            + " the selected template as a template argument. This"
                            + " command line argument names the template argument.\n"
                            + "  The argument can be used once in the context"
                            + " of an output generation block.\n"
                            + "  The argument can be used once in the global"
                            + " context, too, then the global values becomes the"
                            + " default for all output generation blocks that do not"
                            + " specify the value themselves. Optional, the default is"
                            + " \"cluster\" if this argument is not used at all"
                          );
        clp.defineArgument( "ti", "template-arg-name-info"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "Some general information (date and time, file names, etc.)"
                            + " is passed to the selected template as a"
                            + " template argument. This command line argument names"
                            + " the template argument.\n"
                            + "  The argument can be used once in the context"
                            + " of an output generation block.\n"
                            + "  The argument can be used once in the global"
                            + " context, too, then the global values becomes the"
                            + " default for all output generation blocks that do not"
                            + " specify the value themselves. Optional, the default is"
                            + " \"info\" if this argument is not used at all"
                          );
        clp.defineArgument( "w", "template-wrap-column"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ -1
                          , "This integer argument is passed to the render function of"
                            + " StringTemplate V4. The template can be written such that"
                            + " the generated lines of output are wrapped after this column;"
                            + " please, refer to http://www.stringtemplate.org/"
                            + " for details.\n"
                            + "  The argument can be used once in the context"
                            + " of an output generation block.\n"
                            + "  The argument can be used once in the global"
                            + " context, too, then the global values becomes the"
                            + " default for all output generation blocks that do not"
                            + " specify the value themselves. Optional, the default is"
                            + " not to wrap lines if this argument is not used at all"
                          );
    } /* End of ParameterSet.defineArguments */


    /**
     * Check if a string is a valid identifier.
     *   @throws CmdLineParser.InvalidArgException
     * The function operates silently. As long as we don't have a syntactic problem it will
     * just return. In case of an unexpected string it throws an exception.
     *   @param validatedName The checked name.
     *   @param context An documentary string for error reporting: In which context has the
     * check been made?
     */
    public static void isIdent(String validatedName, String context)
        throws CmdLineParser.InvalidArgException
    {
        if(!validatedName.matches("(?i)[a-z][a-z_0-9]*"))
        {
            throw new CmdLineParser.InvalidArgException
                      (context + "An identifier is expected as command line"
                       + " argument but got " + validatedName
                       + ". Any characters other than letters, digits or the"
                       + " underscore are not permitted"
                      );
        }
    } /* End isIdent */



    /**
     * Check if a number is a valid row/column index.
     *   @throws CmdLineParser.InvalidArgException
     * The function operates silently. As long as we don't have a problem it will
     * just return. In case of an unpermitted pair of numbers it throws an exception.
     *   @param validatedNum
     * The checked number.
     *   @param context
     * An documentary string for error reporting: In which context has the check been made?
     */
    public static void isIndex(int validatedNum, String context)
        throws CmdLineParser.InvalidArgException
    {
        if(validatedNum < 1)
        {
            throw new CmdLineParser.InvalidArgException
                      (context + "A one-based index >= 1  is expected as command line"
                       + " argument but got " + validatedNum
                      );
        }
    } /* End isIndex */



    /**
     * Check if a number is a valid row/column index range.
     *   @throws CmdLineParser.InvalidArgException
     * The function operates silently. As long as we don't have a problem it will
     * just return. In case of an unpermitted number it throws an exception.
     *   @param validatedFrom The checked number, which designates the beginning of the
     * range (including).
     *   @param validatedTo The checked number, which designates the end of the
     * range (including). Or -1 if the range should be open ended.
     *   @param context An documentary string for error reporting: In which context has the
     * check been made?
     */
    public static void isIndexRange(int validatedFrom, int validatedTo, String context)
        throws CmdLineParser.InvalidArgException
    {
        if(validatedFrom < 1)
        {
            throw new CmdLineParser.InvalidArgException
                      (context + "A one-based index range is expected as command line"
                       + " argument but got [" + validatedFrom + ", " + validatedTo
                       + "]. The first index should be >= 1"
                      );
        }
        if(validatedTo < 1  &&  validatedTo != -1)
        {
            throw new CmdLineParser.InvalidArgException
                      (context + "A one-based index range is expected as command line"
                       + " argument but got [" + validatedFrom + ", " + validatedTo
                       + "]. The second index should be >= 1 or -1 to indicate an open"
                       + " ended range"
                      );
        }
        if(validatedFrom > validatedTo  &&  validatedTo != -1)
        {
            throw new CmdLineParser.InvalidArgException
                      (context + "A one-based index range is expected as command line"
                       + " argument but got [" + validatedFrom + ", " + validatedTo
                       + "]. The second index should be greater or equal than the first number"
                      );
        }
    } /* End isIndexRange */



    /**
     * Filter arguments, which relate to this class.
     *   @return Next relevant argument or null if there's no one left.
     *   @param argStream
     * The stream object, which delivers all arguments.
     */
    private String parseGetNextArg(Iterator<String> argStream)
    {
        while(argStream.hasNext())
        {
            String arg = argStream.next();
            switch(arg)
            {
            case "cluster-name":
            case "string-template-verbose":
            case "default-worksheet-group":
            case "sort-order-of-workbooks":
            case "sort-order-of-worksheets":
            case "input-file-name":
            case "workbook-name":
            case "worksheet-names-are-identifiers":
            case "open-worksheet-selection":
            case "worksheet-name":
            case "worksheet-by-tab":
            case "worksheet-by-index":
            case "applied-worksheet-template":
            case "open-worksheet-template":
            case "worksheet-template-name":
            case "association-by-tab":
            case "association-by-index":
            case "group":
            case "index-title-row":
            case "column-titles-are-identifiers":
            case "include-range-of-rows":
            case "exclude-range-of-rows":
            case "include-range-of-columns":
            case "exclude-range-of-columns":
            case "open-column-attributes":
            case "column-name":
            case "column-title":
            case "column-index":
            case "is-grouping-column":
            case "sort-order-of-column":
            case "sort-priority-of-column":
            case "user-option-name":
            case "user-option-value":
            case "output-file-name":
            case "template-file-name":
            case "template-name":
            case "template-arg-name-cluster":
            case "template-arg-name-info":
            case "template-wrap-column":

                return arg;
                
            default:
            }
        }

        return null;

    } /* End ParameterSet.parseGetNextArg */



    /**
     * Extract an integer range from the value of an according command line argument. Both
     * possible numbers are  assumed to designate one-based indexes, any value {@code <1} is
     * considered an error.
     *   @return The function either returns the pair of integers that designates the range
     * or it throws a CmdLineParser.InvalidArgException exception with contained error
     * message.
     *   @throws CmdLineParser.InvalidArgException Thrown on any kind of error.
     *   @param argName
     * The name of the argument (i.e. the command line switch), used for reporting only.
     *   @param argValue
     * The string value of the command line argument, which is decoded to a range of
     * integers.
     */
    private Pair<Integer,Integer> parseIntRange(String argName, String argValue)
        throws CmdLineParser.InvalidArgException
    {
        if(argValue.matches("\\s*\\p{Digit}+(:\\p{Digit}+)?\\s*"))
        {
            String[] numAry = argValue.split(":");

            try
            {
                assert numAry.length >= 1  &&  numAry.length <= 2;
                Integer from = Integer.valueOf(numAry[0])
                      , to;
                if(numAry.length == 2)
                {
                    to = Integer.valueOf(numAry[1]);
                    if(to.intValue() < from.intValue())
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("The value "+ argValue + " of argument " + argName
                                   + " designates an empty range"
                                  );
                    }
                }
                else
                    to = null;

                if(from < 1  ||  to != null  &&  to < 1)
                {
                    throw new CmdLineParser.InvalidArgException
                              ("The value "+ argValue + " of argument " + argName
                               + " designates an invalid index range. Indexes need to be"
                               + " >= 1"
                              );
                }

                return new Pair<Integer,Integer>(from, to);
            }
            catch(NumberFormatException e)
            {
                /* Because of the regular expression check at the beginning this can happen
                   only due to a range overflow. We can emit a specific error message. */
                throw new CmdLineParser.InvalidArgException
                      ("Value " + argValue + " of argument " + argName
                       + " is out of range. A positive integer is expected in the range"
                       + " [0, 2^31-1]"
                      );
            }
        }
        else
        {
            throw new CmdLineParser.InvalidArgException
                  ("The value of argument " + argName + " is either a positive decimal"
                   + " integer or a colon separated pair of those but found: \""
                   + argValue + "\""
                  );
        }
    } /* End of ParameterSet.parseIntRange */




    /**
     * Parsing state/context worksheet reference.
     *   @throws CmdLineParser.InvalidArgException
     * A CmdLineParser.InvalidArgException exception with contained error message is thrown
     * in case of failures.
     *   @return The function returns the next token from the stream of command line
     * arguments. (It needs to read it from the stream as look-ahead for parsing of
     * optional context elements.) This is the first token, which has to be evaluated in
     * the calling super state.
     *   @param clp
     * The command line parser object, which contains the stream of arguments, which is
     * input to the parser.
     *   @param argStream
     * The iterator of this argument stream. The context opening argument
     * open-worksheet-selection is the argument last recently read from the stream.
     *   @param workbook
     * The workbook specification in whose context the worksheet reference is parsed.
     */
    private String parseStateWorksheetRef( CmdLineParser clp
                                         , Iterator<String> argStream
                                         , WorkbookDesc workbook
                                         )
        throws CmdLineParser.InvalidArgException
    {
        /* This state/context is entered on its opening argument. */
        final String argName = "open-worksheet-selection";

        /* Retrieve the value of this command line argument. */
        // This argument doesn't have a real value. No need to access clp.
        //final Boolean value = clp.getBoolean(argName);

        /* A context string for feedback into the application log from this routine. */
        String ctx = "Workbook " + workbook + ", command line context worksheet"
                     + " selection ";
        final String dbgCtx = "ParameterSet.parseStateWorksheetRef, Workbook "
                              + workbook + ": ";

        /* Collect the information from the arguments in a new worksheet reference object.
           It's created inside the workbook specification. */
        WorkbookDesc.WorksheetRef wsRef = workbook.createWorksheetRef();

        /* Handle the arguments until we recognize one, which forces us to leave this
           sub-state. */
        String arg = null;
        boolean closeState = false;
        do
        {
            arg = parseGetNextArg(argStream);
            if(arg == null)
                break; /* while(More unconsumed arguments left?) */

            _logger.debug("{}Found argument {}", dbgCtx, arg);

            /* Handle the argument if it belongs to this sub-state or leave the sub-state
               without consuming the argument. */
            switch(arg)
            {
            case "worksheet-name":
                if(wsRef.name != null)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + wsRef + ": "
                               + "worksheet-name repeatedly set. Was "
                               + wsRef.name + " and should become " + clp.getString(arg)
                              );
                }
                wsRef.name = clp.getString(arg);
                break;

            case "worksheet-by-tab":
                if(wsRef.reTabName != null)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + wsRef + ": "
                               + "worksheet-by-tab repeatedly set. Was " + wsRef.reTabName
                               + " and should become " + clp.getString(arg)
                              );
                }
                wsRef.reTabName = clp.getString(arg);
                break;

            case "worksheet-by-index":
                if(wsRef.index != -1)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + wsRef + ": "
                               + "worksheet-by-index repeatedly set. Was " + wsRef.index
                               + " and should become " + clp.getInteger(arg)
                              );
                }
                wsRef.index = clp.getInteger(arg);
                break;

            case "applied-worksheet-template":
                if(wsRef.worksheetTemplateName != null)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + wsRef + ": "
                               + "applied-worksheet-template repeatedly set. Was "
                               + wsRef.worksheetTemplateName
                               + " and should become " + clp.getString(arg)
                              );
                }
                wsRef.worksheetTemplateName = clp.getString(arg);
                break;

            default:
                closeState = true;
                _logger.debug(dbgCtx + "Argument " + arg + " ends command line context"
                              + " worksheet selection " + wsRef
                             );

            } /* End switch(Which argument?) */
        }
        while(!closeState);

        /* Validation of got input is done later, when the workbook specification has been
           completely parsed. */

        /* The principle of implementing a sub-state implies that we get one argument too
           much from the command line parser. This argument (can be null if we reached the
           end of the command line) is returned to the calling super state for consumption. */
        return arg;

    } /* End ParameterSet.parseStateWorksheetRef */



    /**
     * Parsing state/context column attributes.
     *   @throws CmdLineParser.InvalidArgException
     * A CmdLineParser.InvalidArgException exception with contained error message is thrown
     * in case of failures.
     *   @return The function returns the next token from the stream of command line
     * arguments. (It needs to read it from the stream as look-ahead for parsing of
     * optional context elements.) This is the first token, which has to be evaluated in
     * the calling super state.
     *   @param clp
     * The command line parser object, which contains the stream of arguments, which is
     * input to the parser.
     *   @param argStream
     * The iterator of this argument stream. The context opening argument
     * open-column-attributes is the argument last recently read from the stream.
     *   @param worksheetTmpl
     * The worksheet template in whose context the column attributes are parsed.
     */
    private String parseStateColumnAttributes( CmdLineParser clp
                                             , Iterator<String> argStream
                                             , WorksheetTemplate worksheetTmpl
                                             )
        throws CmdLineParser.InvalidArgException
    {
        /* This state/context is entered on its opening argument. */
        final String argName = "open-column-attributes";

        /* Retrieve the value of this command line argument. */
        // This argument doesn't have a real value. No need to access clp.
        //final Boolean value = clp.getBoolean(argName);

        /* A context string for feedback into the application log from this routine. */
        final String ctx = "Worksheet template " + worksheetTmpl
                           + ", command line context column attributes "
                   , dbgCtx = "ParameterSet.parseStateColumnAttributes, Worksheet template "
                              + worksheetTmpl + ": ";

        /* Collect the information from the arguments in this new object. Later we will add
           it to our collection so far. */
        final WorksheetTemplate.ColumnAttributes colTmpl =
                                                    worksheetTmpl.createColumnAttributes();

        /* Handle the arguments until we recognize one, which forces us to leave this
           sub-state. */
        String arg = null;
        boolean closeState = false;
        do
        {
            arg = parseGetNextArg(argStream);
            if(arg == null)
                break; /* while(More unconsumed arguments left?) */

            _logger.debug("{}Found argument {}", dbgCtx, arg);

            /* Handle the argument if it belongs to this sub-state or leave the sub-state
               without consuming the argument. */
            switch(arg)
            {
            case "column-name":
                if(colTmpl.name != null)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + colTmpl + ": "
                               + "column-name repeatedly set. Was "
                               + colTmpl.name + " and should become " + clp.getString(arg)
                              );
                }
                colTmpl.name = clp.getString(arg);
                break;

            case "column-title":
                if(colTmpl.reTitle != null)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + colTmpl + ": "
                               + "column-title repeatedly set. Was " + colTmpl.reTitle
                               + " and should become " + clp.getString(arg)
                              );
                }
                colTmpl.reTitle = clp.getString(arg);
                break;

            case "column-index":
                if(colTmpl.index != -1)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + colTmpl + ": "
                               + "column-index repeatedly set. Was " + colTmpl.index
                               + " and should become " + clp.getInteger(arg)
                              );
                }
                colTmpl.index = clp.getInteger(arg);
                break;

            case "is-grouping-column":
                if(colTmpl.isGroupingColumn)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + colTmpl + ": "
                               + "is-grouping-column repeatedly set"
                              );
                }
                colTmpl.isGroupingColumn = clp.getBoolean(arg);

                /* It's impossible to set a Boolean argument to false on the command line.
                   One can only not use the argument. */
                assert(colTmpl.isGroupingColumn);

                break;

            case "sort-order-of-column":
                if(colTmpl.sortOrder != SortOrder.Order.undefined)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + colTmpl + ": "
                               + "sort-order-of-column repeatedly set. Was "
                               + colTmpl.sortOrder + " and should become "
                               + clp.getString(arg)
                              );
                }
                colTmpl.sortOrder = strToSortOrder(clp.getString(arg));
                break;

            case "sort-priority-of-column":
                if(colTmpl.sortPriority != -1)
                {
                    throw new CmdLineParser.InvalidArgException
                              (ctx + colTmpl + ": "
                               + "column-sortPriority repeatedly set. Was "
                               + colTmpl.sortPriority + " and should become "
                               + clp.getInteger(arg)
                              );
                }
                colTmpl.sortPriority = clp.getInteger(arg);
                if(colTmpl.sortPriority < 1)
                {
                    throw new CmdLineParser.InvalidArgException
                                (ctx + colTmpl + ": "
                                 + "A valid sort priority is an integer number >= 1 but got "
                                 + colTmpl.sortPriority
                                );
                }
                break;

            default:
                closeState = true;
                _logger.debug(dbgCtx + "Argument " + arg + " ends command line context"
                              + " column attributes " + colTmpl
                             );

            } /* End switch(Which argument?) */
        }
        while(!closeState);

        /* Validation of got input is done later, when the worksheet template specification
           has been completely parsed. */

        /* The principle of implementing a sub-state implies that we get one argument too
           much from the command line parser. This argument (can be null if we reached the
           end of the command line) is returned to the calling super state for consumption. */
        return arg;

    } /* End ParameterSet.parseStateColumnAttributes */



    /**
     * Parsing state/context user code generation option.
     *   @throws CmdLineParser.InvalidArgException
     * The function either processes the command line arguments successfully or it throws a
     * CmdLineParser.InvalidArgException exception with contained error message.
     *   @return The function returns the next token from the stream of command line
     * arguments. (It needs to read it from the stream as look-ahead for parsing of
     * optional context elements.)
     *   @param optionMap
     * In case of successful parsing the user option (i.e. user specified template
     * attribute) is added to this map. The option/attribute name is the key, the value is
     * the value in the map, too. If the map already contains an entry then this entry will
     * silently be replaced - this is how default values given in the global context are
     * handled.
     *   @param clp
     * The command line parser object, which contains the stream of arguments, which is
     * input to the parser.
     *   @param argStream
     * The iterator of this argument stream. The name of the user option/attribute is the
     * argument last recently read from the stream. The subsequent argument is also
     * consumed.
     */
    private String parseStateUserOption( LinkedHashMap<String,Object> optionMap
                                       , CmdLineParser clp
                                       , Iterator<String> argStream
                                       )
        throws CmdLineParser.InvalidArgException
    {
        /* This state/context is entered on its opening argument. */
        final String argName = "user-option-name";

        /* Retrieve the value of this command line argument. */
        final String name = clp.getString(argName);

        /* The user option/attribute specification only has two elements and both are
           mandatory. So there's no variability, the next one needs to be the attribute's
           value and needs to be the last one of this context. */
        String argValue = parseGetNextArg(argStream);
        Object value = null;
        if(argValue != null  &&  argValue.compareTo("user-option-value") == 0)
        {
            final String valueAsString = clp.getString(argValue);

            /* The value is added to the map as an object of the best fitting Java type.
               This will maximize the usability in the later template expansion. */
            try
            {
                value = Integer.parseInt(valueAsString);
            }
            catch(NumberFormatException e)
            {
                value=null;
            }
            if(value == null)
            {
                try
                {
                    value = Double.parseDouble(valueAsString);
                }
                catch(NumberFormatException e)
                {
                    value=null;
                }
            }
            if(value == null)
            {
                if(valueAsString.equalsIgnoreCase("true"))
                    value = Boolean.valueOf(true);
                else if(valueAsString.equalsIgnoreCase("false"))
                    value = Boolean.valueOf(false);
                else
                    value = valueAsString;
            }
            assert value != null;
            _logger.debug("User option " + name + " is of Java type "
                          + value.getClass().getName() + " and has the value " + value
                         );

            /* To unify both cases for the caller, optional argument given or not given, we
               need to comsume a further argument now. */
            argValue = parseGetNextArg(argStream);
        }
        else
        {
            /* By default a user option is a Boolean true. The use case are code generation
               controlling flags, which can be added just like that on the command line,
               e.g. -op generateDebugCode on the command line could be a way to make
               templates conditionally generate additional code for debugging. */
            value = Boolean.valueOf(true);
        }

        /* Add to or replace the request in the map. Silent replacing is the way default
           values are handled: They are first added in the global context and can then be
           overwritten in the specific context. */
        assert optionMap != null;
        optionMap.put(name, value);

        return argValue;

    } /* End ParameterSet.parseStateUserOption */



    /**
     * Call the method LinkedHashMap.clone() on a {@code LinkedHashMap<String,Object>}
     * without getting compiler warnings because of unsafe type casts.
     *   @return
     * Get the cloned map (shallow copy).
     *   @param <T>
     * The type of the map value.
     *   @param map
     * The map to clone.
     */
    @SuppressWarnings("unchecked")
    private static <T> LinkedHashMap<String,T> cloneLinkedHashMap(LinkedHashMap<String,T> map)
    {
        return (LinkedHashMap<String,T>)map.clone();

    } /* End of ParameterSet.cloneLinkedHashMap */




    /** The internal states of the argument parsing algorithm. */
    private enum ParseState { /** Outside a command line context */           global           
                            , /** Command line context workbook */            workbook         
                            , /** Command line context worksheet reference */ worksheetRef     
                            , /** Command line context worksheet template*/   worksheetTemplate
                            , /** Command line context column attributes */   columnAttributes 
                            , /** Command line context output file */         output           
                            , /** Parsing of command line done */             terminated       
                            };

    /**
     * Conversion command line argument string to enumeration for SortOrder.
     *   @return Get the enumeration value.
     *   @throws CmdLineParser.InvalidArgException
     * Any bad input string is reported by exception with meaningful message.
     *   @param sortOrder
     * The sort order by name.
     */
    public SortOrder.Order strToSortOrder(String sortOrder)
        throws CmdLineParser.InvalidArgException
    {
        try
        {
            return SortOrder.Order.valueOf(sortOrder);
        }
        catch(IllegalArgumentException ex)
        {
            throw new CmdLineParser.InvalidArgException
                      ("Invalid enumeration value " + sortOrder + " found for sort-order."
                       + " Permitted values are " + SortOrder.Order.lexical.toString()
                       + ", " + SortOrder.Order.ASCII.toString()
                       + ", " + SortOrder.Order.numerical.toString()
                       + ", " + SortOrder.Order.inverseLexical.toString()
                       + ", " + SortOrder.Order.inverseASCII.toString()
                       + " and " + SortOrder.Order.inverseNumerical.toString()
                      );
        }
    } /* End of strToSortOrder */


    /**
     * Fill the parameter object with actual values.
     *   After successful command line parsing this function iterates along the parse
     * result to fill all fields of this parameter object.
     *   @throws CmdLineParser.InvalidArgException
     * The parser can't find all structural problems of the given set of command line
     * arguments. This function returns only if all arguments were given in an accepted
     * order and if all mandatory arguments were found. Otherwise the exception is thrown
     * and the application can't continue to work. An according error message is part of
     * the exception.
     *   @param clp
     * The command line parser object after succesful run of CmdLineParser.parseArgs.
     */
    public void parseCmdLine(CmdLineParser clp)
        throws CmdLineParser.InvalidArgException
    {
        /* This object is filled in the global command line context and serves as default
           value for all later specified template output pairs. */
        TemplateOutputPair defaultTemplateOutputPair = new TemplateOutputPair();

        /* The next obejetcs are temporarily used when the related command line context is
           entered. Being in the context they are filled. On leaving the context they are
           copied into the related collection in this parameter set. */
        TemplateOutputPair templateOutputPair = null;
        WorkbookDesc workbookDesc = null;
        WorksheetTemplate worksheetTmpl = null;

        /* Some context information for logging. */
        final String dbgCtx = "ParameterSet.parseCmdLine: ";

        /* Most arguments are context dependent. The context is derived from the order of
           arguments. A new context is openend by some principal arguments, like bus-name,
           which opens the context of a bus definition. From now on the arguments will be
           related to this new context. We need a small state machine and we need to
           process the parsed arguments in their order of appearance on the command line. */
        Iterator<String> it = clp.iterator();

        /* The state machine is simplified by reading state changing arguments twice. The
           first time they trigger the leave-state action, and only the next time the
           enter-state action, already being in the new state. Both actions are now needed
           only once at the beginning and the end of the related state. */
        boolean argHasBeenConsumed = true;
        String arg = null;

        ParseState state = ParseState.global;
        while(state != ParseState.terminated)
        {
            /* Some arguments are out of scope of the ParameterSet. We skip them. Get next
               relevant argument. */
            if(argHasBeenConsumed)
                arg = parseGetNextArg(it);
            if(arg != null)
                _logger.debug("{}Found argument {}", dbgCtx, arg);
            else
            {
                /* Having reached the end of the command line we run once again through the
                   state machine to give it the change to trigger the state exit actions
                   needed to properly close any open context. The response on this event
                   needs to by state terminated under all circumstances. */
                arg = "";
            }

            /* Most cases will consume the argument so take this as default. */
            argHasBeenConsumed = true;

            /* The argument values have common types. These variable are intended to
               temporarily hold such values: The values must not be accessed repeatedly in
               the comand line parser object - this would move and invalidate its internal
               read pointer. */
            String argVal;
            Pair<Integer,Integer> range;

            /* Handle the relevant arguments state dependently. */
            switch(state)
            {
            case global:
                switch(arg)
                {
                /* The initial cases handle the transitions to other states. We never come
                   back to the global context; check for completeness, add default
                   values. */
                case "input-file-name":
                    /* This argument opens a workbook specification context. */
                case "open-worksheet-template":
                    /* This argument opens a worksheet template context. */
                case "output-file-name":
                    /* This argument opens an output generation context. */
                case "":
                    /* This case is used after the very last argument to close any open
                       context. */
                    _logger.debug("{}Global context is closed forever", dbgCtx);

                    if(clusterName == null)
                        clusterName = "cluster";

                    /* Select subsequent state. */
                    if(arg.equals("input-file-name"))
                        state = ParseState.workbook;
                    else if(arg.equals("open-worksheet-template"))
                        state = ParseState.worksheetTemplate;
                    else if(arg.equals("output-file-name"))
                        state = ParseState.output;
                    else
                        state = ParseState.terminated;

                    /* Evaluate the same argument again in order to enter the aimed state
                       in the next loop cycle. */
                    argHasBeenConsumed = false;

                    break;

                case "cluster-name":
                    /* Repeated appearance is already filtered by the parser. */
                    assert clusterName == null;
                    clusterName = clp.getString(arg);
                    break;

                case "string-template-verbose":
                    assert stringTemplateVerbose == false;
                    stringTemplateVerbose = true;
                    break;

                case "default-worksheet-group":
                    if(defaultWorksheetGroup != null)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("default-worksheet-group repeatedly set in the global"
                                   + " command line context. Was "
                                   + defaultWorksheetGroup + " and should become "
                                   + clp.getString(arg)
                                  );
                    }
                    defaultWorksheetGroup = clp.getString(arg);
                    break;

                case "sort-order-of-workbooks":
                    if(sortOrderWorkbooks != SortOrder.Order.undefined)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("sort-order-of-workbooks repeatedly set in the global"
                                   + " command line context. Was " + sortOrderWorkbooks
                                   + " and should become " + clp.getString(arg)
                                  );
                    }
                    sortOrderWorkbooks = strToSortOrder(clp.getString(arg));
                    break;

                case "sort-order-of-worksheets":
                    if(sortOrderWorksheets != SortOrder.Order.undefined)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("sort-order-of-worksheets repeatedly set in the global"
                                   + " command line context. Was " + sortOrderWorksheets
                                   + " and should become " + clp.getString(arg)
                                  );
                    }
                    sortOrderWorksheets = strToSortOrder(clp.getString(arg));
                    break;

                case "user-option-name":
                    /* The sub-parse-functions needs one argument look-ahead to handle
                       optional arguments. The last recently read token has therefore not
                       yet been consumed. */
                    arg = parseStateUserOption(optionMap, clp, it);
                    argHasBeenConsumed = false;
                    break;

                /* Here we have some cases for input specification and output generation
                   context arguments, which have reasonable common default values. The
                   values the user passes in the global context are stored locally and used
                   as default value for all later instances of that contexts. */
                /// @todo Double-check on design level if this concept really makes sense. It mainly relates to template properties, which are typically specific to the generated files. Why should we have a common default value for all of them?
                // First answer: Yes true, this mechnism doesn't make sense. We will leave
                // it in the code as it is not doocumented and invisble to the user anyway
                // and since it implements as basically valuable mechanism, which could be
                // exploited in the future for other arguments.
                case "template-name":
                    if(defaultTemplateOutputPair.templateName != null)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("Default template name repeatedly set in the"
                                   + " global context. Was "
                                   + defaultTemplateOutputPair.templateName
                                   + " and should become " + clp.getString(arg)
                                  );
                    }
                    defaultTemplateOutputPair.templateName = clp.getString(arg);
                    break;

                case "template-arg-name-cluster":
                    if(defaultTemplateOutputPair.templateArgNameCluster != null)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("Default template argument name repeatedly set in the"
                                   + " global context. Was "
                                   + defaultTemplateOutputPair.templateArgNameCluster
                                   + " and should become " + clp.getString(arg)
                                  );
                    }
                    defaultTemplateOutputPair.templateArgNameCluster = clp.getString(arg);
                    break;

                case "template-arg-name-info":
                    if(defaultTemplateOutputPair.templateArgNameInfo != null)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("Default template argument name repeatedly set in the"
                                   + " global context. Was "
                                   + defaultTemplateOutputPair.templateArgNameInfo
                                   + " and should become " + clp.getString(arg)
                                  );
                    }
                    defaultTemplateOutputPair.templateArgNameInfo = clp.getString(arg);
                    break;

                case "template-wrap-column":
                    if(defaultTemplateOutputPair.templateWrapCol > 0
                       || defaultTemplateOutputPair.templateWrapCol == -1
                      )
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("Default wrap column repeatedly set in the"
                                   + " global context. Was "
                                   + defaultTemplateOutputPair.templateWrapCol
                                   + " and should become " + clp.getInteger(arg)
                                  );
                    }
                    defaultTemplateOutputPair.templateWrapCol = clp.getInteger(arg);
                    if(defaultTemplateOutputPair.templateWrapCol <= 0)
                        defaultTemplateOutputPair.templateWrapCol = -1;
                    break;

                default:
                    throw new CmdLineParser.InvalidArgException
                              ("Command line argument " + arg
                               + " must not be used in the global context"
                              );
                }
                break;

            case workbook:
                /* Not having a (half-way) completed temporary workbook object means
                   that we are about to enter this state. In contrast, we could already be
                   in the same state and the argument demands to leave it and re-enter it
                   again for another instance of a workbook description. */
                if(workbookDesc == null)
                {
                    assert arg.equals("input-file-name");
                    workbookDesc = new WorkbookDesc();
                    workbookDesc.fileName = clp.getString(arg);
                    workbookDesc.optionMap = cloneLinkedHashMap(optionMap);
                    _logger.debug( "{}Workbook context {} is opened"
                                 , dbgCtx
                                 , workbookDesc.fileName
                                 );
                }
                else
                {
                    switch(arg)
                    {
                    /* The initial cases initiate closing the current context. */
                    case "":
                        /* This case is used after the very last argument to close any open
                           context. */
                    case "input-file-name":
                        /* If we see this argument the second time while in this state then
                           it opens a new instance of the workbook specification context. */
                    case "open-worksheet-template":
                        /* This argument opens a worksheet template context. */
                    case "output-file-name":
                        /* This opens an ouput file context. */

                        /* The current workbook specification context is closed. */
                        _logger.debug( "{}Workbook specification context {} is closed"
                                     , dbgCtx
                                     , workbookDesc.fileName
                                     );

                        /* The current context is checked for completeness and finalized
                           with default values. */
                        // No mandatory fields here

                        /* Check user input for completeness and consistency. */
                        workbookDesc.validate();

                        /* Add the finalized object to the parameter set. */
                        workbookAry.add(workbookDesc);
                        workbookDesc = null;

                        if(arg.equals(""))
                            state = ParseState.terminated;
                        else if(arg.equals("open-worksheet-template"))
                            state = ParseState.worksheetTemplate;
                        else if(arg.equals("output-file-name"))
                            state = ParseState.output;
                        else
                            assert arg.equals("input-file-name");

                        /* Let this argument be reparsed again for opening the new
                           context. */
                        argHasBeenConsumed = false;

                        break;

                    case "open-worksheet-selection":
                        /* This argument opens the sub-context of a worksheet selection.
                           The workbook specification context is kept open here in the loop
                           and switch environment and the sub-state is completely handled
                           in the called subroutine. It operates on our current, temporary
                           workbook object.
                             The implementation of the sub-context returns when it saw an
                           argument in the stream, which doesn't belong into the
                           sub-context. It is inserted into the stream evaluated here in
                           its super state by overwriting arg and setting the state to "not
                           yet consumed". */
                        arg = parseStateWorksheetRef(clp, /*argStream*/ it, workbookDesc);
                        argHasBeenConsumed = false;
                        break;

                    case "workbook-name":
                        if(workbookDesc.name != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Workbook name repeatedly set in the context of"
                                       + " workbook " + workbookDesc.fileName
                                       + ". Was " + workbookDesc.name
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        workbookDesc.name = clp.getString(arg);
                        break;

                    case "worksheet-names-are-identifiers":
                        if(workbookDesc.worksheetNamesAreIdentifiers)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Argument worksheet-names-are-identifiers repeatedly"
                                       + " given in the context of workbook "
                                       + workbookDesc.fileName
                                      );
                        }
                        workbookDesc.worksheetNamesAreIdentifiers = clp.getBoolean(arg);

                        /* It's impossible to set a Boolean argument to false on the
                           command line. One can only not use the argument. */
                        assert(workbookDesc.worksheetNamesAreIdentifiers);

                        break;


                    case "user-option-name":
                        /* The sub-parse-functions needs one argument look-ahead to handle
                           optional arguments. The last recently read token has therefore not
                           yet been consumed. */
                        arg = parseStateUserOption(workbookDesc.optionMap, clp, it);
                        argHasBeenConsumed = false;
                        break;

                    default:
                        throw new CmdLineParser.InvalidArgException
                                  ("The use of argument " + arg + " is invalid in the context"
                                   + " of workbook definition " + workbookDesc.fileName
                                  );

                    } /* switch(Which argument to be consumed in the opened context
                                workbook specification?) */

                } /* if(Do we open the context workbook specification or is it already
                        opened?) */
                break;

            case worksheetTemplate:
                /* Not having a (half-way) completed temporary worksheet template object
                   means that we are about to enter this state. In contrast, we could
                   already be in the same state and the argument demands to leave it and
                   re-enter it again for another instance of a worksheet template. */
                if(worksheetTmpl == null)
                {
                    assert arg.equals("open-worksheet-template");
                    worksheetTmpl = new WorksheetTemplate();
                    _logger.debug("{}Worksheet template context is opened", dbgCtx);
                }
                else
                {
                    switch(arg)
                    {
                    /* The initial cases initiate closing the current context. */
                    case "":
                        /* This case is used after the very last argument to close any open
                           context. */
                    case "input-file-name":
                        /* This argument opens a workbook specification context. */
                    case "open-worksheet-template":
                        /* If we see this argument the second time while in this state then
                           it opens a new instance of the worksheet template context. */
                    case "output-file-name":
                        /* This opens an ouput file context. */

                        /* The current worksheet template context is closed. */
                        _logger.debug( "{}Worksheet template context {} is closed"
                                     , dbgCtx
                                     , worksheetTmpl
                                     );

                        /* Check user input for completeness and consistency. */
                        worksheetTmpl.validate();

                        /* Add the finalized object to the parameter set. */
                        if(worksheetTemplateAry == null)
                            worksheetTemplateAry = new ArrayList<WorksheetTemplate>();
                        worksheetTemplateAry.add(worksheetTmpl);
                        worksheetTmpl = null;

                        if(arg.equals(""))
                            state = ParseState.terminated;
                        else if(arg.equals("input-file-name"))
                            state = ParseState.workbook;
                        else if(arg.equals("output-file-name"))
                            state = ParseState.output;
                        else assert arg.equals("open-worksheet-template");

                        /* Let this argument be reparsed again for opening the new
                           context. */
                        argHasBeenConsumed = false;

                        break;

                    case "worksheet-template-name":
                        if(worksheetTmpl.name != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Worksheet template name repeatedly set in the"
                                       + " context of worksheet template "
                                       + worksheetTmpl
                                       + ". Was " + worksheetTmpl.name
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        worksheetTmpl.name = clp.getString(arg);
                        break;

                    case "association-by-tab":
                        if(worksheetTmpl.reTabName != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Tab title repeatedly set in the"
                                       + " context of worksheet template "
                                       + worksheetTmpl
                                       + ". Was " + worksheetTmpl.reTabName
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        worksheetTmpl.reTabName = clp.getString(arg);
                        break;

                    case "association-by-index":
                        if(worksheetTmpl.index != -1)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Worksheet index repeatedly set in the"
                                       + " context of worksheet template "
                                       + worksheetTmpl
                                       + ". Was " + worksheetTmpl.index
                                       + " and should become " + clp.getInteger(arg)
                                      );
                        }
                        worksheetTmpl.index = clp.getInteger(arg);
                        if(worksheetTmpl.index < 1)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Invalid worksheet index " + worksheetTmpl.index
                                       + " found in the context of worksheet template "
                                       + worksheetTmpl
                                       + ". Only values >= 1 are permitted"
                                      );
                        }
                        break;

                    case "group":
                        if(worksheetTmpl.worksheetGroup != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Worksheet group repeatedly set in the"
                                       + " context of worksheet template " + worksheetTmpl
                                       + ". Was " + worksheetTmpl.worksheetGroup
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        worksheetTmpl.worksheetGroup = clp.getString(arg);
                        break;

                    case "index-title-row":
                        if(worksheetTmpl.idxTitleRow != 0)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Title row index repeatedly set in the"
                                       + " context of worksheet template "
                                       + worksheetTmpl
                                       + ". Was " + worksheetTmpl.idxTitleRow
                                       + " and should become " + clp.getInteger(arg)
                                      );
                        }
                        worksheetTmpl.idxTitleRow = clp.getInteger(arg);
                        if(worksheetTmpl.idxTitleRow < 1  &&  worksheetTmpl.idxTitleRow != -1)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Invalid title row index " + worksheetTmpl.idxTitleRow
                                       + " found in the context of worksheet template "
                                       + worksheetTmpl
                                       + ". Only values >= 1 are permitted, or -1"
                                      );
                        }
                        break;

                    case "column-titles-are-identifiers":
                        if(worksheetTmpl.columnTitlesAreIdentifiers)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Argument column-titles-are-identifiers repeatedly"
                                       + " given in the context of worksheet template "
                                       + worksheetTmpl
                                      );
                        }
                        worksheetTmpl.columnTitlesAreIdentifiers = clp.getBoolean(arg);

                        /* It's impossible to set a Boolean argument to false on the
                           command line. One can only not use the argument. */
                        assert(worksheetTmpl.columnTitlesAreIdentifiers);

                        break;

                    case "include-range-of-rows":
                        argVal = clp.getString(arg);
                        worksheetTmpl.inclRowIdxAry.add(parseIntRange(arg, argVal));
                        break;

                    case "exclude-range-of-rows":
                        argVal = clp.getString(arg);
                        worksheetTmpl.exclRowIdxAry.add(parseIntRange(arg, argVal));
                        break;

                    case "include-range-of-columns":
                        argVal = clp.getString(arg);
                        worksheetTmpl.inclColIdxAry.add(parseIntRange(arg, argVal));
                        break;

                    case "exclude-range-of-columns":
                        argVal = clp.getString(arg);
                        worksheetTmpl.exclColIdxAry.add(parseIntRange(arg, argVal));
                        break;

                    case "open-column-attributes":
                        /* This argument opens the sub-context of a column attribute
                           specification. The worksheet template specification context is
                           kept open here in the loop and switch environment and the
                           sub-state is completely handled in the called subroutine. It
                           operates on our current, temporary worksheet template object.
                             The implementation of the sub-context returns when it saw an
                           argument in the stream, which doesn't belong into the
                           sub-context. It is inserted into the stream evaluated here in
                           its super state by overwriting arg and setting the state to "not
                           yet consumed". */
                        arg = parseStateColumnAttributes(clp, /*argStream*/ it, worksheetTmpl);
                        argHasBeenConsumed = false;
                        break;

                    default:
                        throw new CmdLineParser.InvalidArgException
                                  ("The use of argument " + arg + " is invalid in the context"
                                   + " of worksheet template "
                                   + worksheetTmpl
                                  );

                    } /* switch(Which argument to be consumed in the opened context
                                worksheet specification?) */

                } /* if(Do we open the context worksheet template specification or is it
                        already opened?) */
                break;

            case output:
                /* Not having a (half-way) completed temporary output pair object means
                   that we are about to enter this state. In contrast, we could already be
                   in the same state and the argument demands to leave it and re-enter it
                   again for another instance of an output pair. */
                if(templateOutputPair == null)
                {
                    assert arg.equals("output-file-name");
                    templateOutputPair = new TemplateOutputPair();
                    templateOutputPair.outputFileName = clp.getString(arg);
                    templateOutputPair.optionMap = cloneLinkedHashMap(optionMap);
                    _logger.debug( "{}Output generation context for file {} is opened"
                                 , dbgCtx
                                 , templateOutputPair.outputFileName
                                 );
                }
                else
                {
                    switch(arg)
                    {
                    /* The initial cases initiate closing the current context. */
                    case "":
                        /* This case is used after the very last argument to close any open
                           context. */
                    case "input-file-name":
                        /* This argument opens a workbook specification context. */
                    case "open-worksheet-template":
                        /* This argument opens a worksheet template context. */
                    case "output-file-name":
                        /* If we see this argument the second time while in this state then
                           it opens a new instance of the output generation context. */

                        /* The current output generation context is closed. */
                        _logger.debug( "{}Output generation context for file {} is closed"
                                     , dbgCtx
                                     , templateOutputPair.outputFileName
                                     );

                        /* The current context is checked for completeness and finalized
                           with default values. */
                        assert templateOutputPair.outputFileName != null;
                        if(templateOutputPair.templateFileName == null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Mandatory argument template-file-name not found in"
                                       + " output generation context for file "
                                       + templateOutputPair.outputFileName + ". It needs"
                                       + " to be given prior to opening any new context,"
                                       + " including another output generation context"
                                      );
                        }
                        /* The user may have set default values in the global context for
                           the next parameters. If so and if they were not set in this
                           output generation context then use those default values. */
                        if(templateOutputPair.templateName == null)
                        {
                            if(defaultTemplateOutputPair.templateName != null)
                            {
                                templateOutputPair.templateName = defaultTemplateOutputPair
                                                                  .templateName;
                            }
                            else
                            {
                                templateOutputPair.templateName =
                                                      TemplateOutputPair._defaultTemplateName;
                            }
                        }
                        if(templateOutputPair.templateArgNameCluster == null)
                        {
                            if(defaultTemplateOutputPair.templateArgNameCluster != null)
                            {
                                templateOutputPair.templateArgNameCluster
                                           = defaultTemplateOutputPair.templateArgNameCluster;
                            }
                            else
                            {
                                templateOutputPair.templateArgNameCluster =
                                            TemplateOutputPair._defaultTemplateArgNameCluster;
                            }
                        }
                        if(templateOutputPair.templateArgNameInfo == null)
                        {
                            if(defaultTemplateOutputPair.templateArgNameInfo != null)
                            {
                                templateOutputPair.templateArgNameInfo
                                              = defaultTemplateOutputPair.templateArgNameInfo;
                            }
                            else
                            {
                                templateOutputPair.templateArgNameInfo =
                                               TemplateOutputPair._defaultTemplateArgNameInfo;
                            }
                        }
                        if(templateOutputPair.templateWrapCol == 0)
                        {
                            if(defaultTemplateOutputPair.templateWrapCol != 0)
                            {
                                templateOutputPair.templateWrapCol = defaultTemplateOutputPair
                                                                     .templateWrapCol;
                            }
                            else
                                templateOutputPair.templateWrapCol = -1;
                        }

                        /* Add the finalized object to the parameter set. */
                        templateOutputPairAry.add(templateOutputPair);
                        templateOutputPair = null;

                        if(arg.equals("input-file-name"))
                            state = ParseState.workbook;
                        else if(arg.equals("open-worksheet-template"))
                            state = ParseState.worksheetTemplate;
                        else if(arg.equals(""))
                            state = ParseState.terminated;
                        else
                            assert arg.equals("output-file-name");

                        /* Let this argument be reparsed again for opening the new
                           context. */
                        argHasBeenConsumed = false;

                        break;

                    case "template-file-name":
                        if(templateOutputPair.templateFileName != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Template group file name repeatedly set in the"
                                       + " output generation context for file"
                                       + templateOutputPair.outputFileName + ". Was "
                                       + templateOutputPair.templateFileName
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        templateOutputPair.templateFileName = clp.getString(arg);
                        break;

                    case "template-name":
                        if(templateOutputPair.templateName != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Template name repeatedly set in the"
                                       + " output generation context for file"
                                       + templateOutputPair.outputFileName + ". Was "
                                       + templateOutputPair.templateName
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        templateOutputPair.templateName = clp.getString(arg);
                        break;

                    case "template-arg-name-cluster":
                        if(templateOutputPair.templateArgNameCluster != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Template argument name repeatedly set in the"
                                       + " output generation context for file"
                                       + templateOutputPair.outputFileName + ". Was "
                                       + templateOutputPair.templateArgNameCluster
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        templateOutputPair.templateArgNameCluster = clp.getString(arg);
                        break;

                    case "template-arg-name-info":
                        if(templateOutputPair.templateArgNameInfo != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Template argument name repeatedly set in the"
                                       + " output generation context for file"
                                       + templateOutputPair.outputFileName + ". Was "
                                       + templateOutputPair.templateArgNameInfo
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        templateOutputPair.templateArgNameInfo = clp.getString(arg);
                        break;

                    case "template-wrap-column":
                        if(templateOutputPair.templateWrapCol != 0)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Wrap column repeatedly set in the"
                                       + " output generation context for file "
                                       + templateOutputPair.outputFileName + ". Was "
                                       + templateOutputPair.templateWrapCol
                                       + " and should become " + clp.getInteger(arg)
                                      );
                        }
                        templateOutputPair.templateWrapCol = clp.getInteger(arg);
                        if(templateOutputPair.templateWrapCol <= 0)
                            templateOutputPair.templateWrapCol = -1;
                        break;

                    case "user-option-name":
                        /* The sub-parse-functions needs one argument look-ahead to handle
                           optional arguments. The last recently read token has therefore not
                           yet been consumed. */
                        arg = parseStateUserOption(templateOutputPair.optionMap, clp, it);
                        argHasBeenConsumed = false;
                        break;

                    default:
                        throw new CmdLineParser.InvalidArgException
                                  ("The use of argument " + arg + " is invalid in the"
                                   + " context of output file definition "
                                   + templateOutputPair.outputFileName
                                  );
                    } /* End switch(Which arg in the open output generation context?) */
                } /* if(Do we open the output generation context or is it already opened?) */
                break;

            default: assert false;

            } /* End switch(Current state) */

            assert arg == null  || !arg.equals("") ||  state == ParseState.terminated
                   : "Final, terminating transition of state machine failed"
                   ;

        } /* End while(For all command line arguments) */

    } /* End of ParameterSet.parseCmdLine */



    /**
     * log4j logging is initialized for this class. Different to usual this can't be done as
     * a static expression at class initialization time. The reason is that this class is
     * involved in the preparatory log4j configuration work, which must be completed prior
     * to the class loading of log4j. Call this method once after having done all log4j
     * configuration and before the further activity of this class wants to make use of the
     * log4j logger.
     */
    public static void loadLog4jLogger()
        {_logger = LogManager.getLogger(ParameterSet.class);}


    /**
     * Render the current settings as a string.
     */
    public String toString()
    {
        final String templateFileName = "excelExporter/main/ParameterSet_toString.stg";
        STGroup stg = null;
        try
        {
            stg = new STGroupFile(templateFileName);
            stg.verbose = false;
            stg.registerRenderer(Number.class, new NumberRenderer());
            stg.registerRenderer(String.class, new StringRenderer());
            ST template = stg.getInstanceOf("parameterSet");
            template.add("p", this);
            return template.render(/* optional wrapCol */ 72);
        }
        catch(RuntimeException e)
        {
            _logger.error("Error in rendering of application parameters. " + e.getMessage());
            return "?";
        }
    } /* End of ParameterSet.toString() */

} /* End of class ParameterSet definition. */

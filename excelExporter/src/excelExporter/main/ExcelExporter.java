/**
 * @file ExcelExporter.java
 * Main entry point into the Excel exporter of the COM framework.
 *
 * Copyright (C) 2015-2025 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class ExcelExporter
 *   ExcelExporter
 *   createDir
 *   defineArguments
 *   parseCmdLine
 *   run
 *   main
 */

package excelExporter.main;

import java.util.*;
import java.io.*;
import java.text.*;

//import excelExporter.excelParser.*;
import org.stringtemplate.v4.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.logging.log4j.*;
import applicationInterface.cmdLineParser.CmdLineParser;
import applicationInterface.loggerConfiguration.Log4j2Configurator;
import excelExporter.excelParser.*;
import excelExporter.excelParser.dataModel.*;


/**
 * This class has a main function, which implements the excel exporter application.
 *   @author Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
 *   @see ExcelExporter#main
 */

public class ExcelExporter
{
    /** The name of this Java application. */
    public static final String _applicationName = "excelExporter";

    /** Version designation as four numeric parts. */
    private static int[] _versionAry = {1, 4, 0, GitRevision.getProjectRevision()};

    /** The first three parts of the version of the tool, which relate to functional
        changes of the application.
          @todo This version designation needs to be kept in sync with the version of the
        data model so that writing of safe templates (with respect to unexpected tool
        changes) becomes possible. Any change of the data model needs to be reflected in
        the major parts of this version designation and the other version designation
        _versionDataModel needs to be synchronized then. Tool changes without a change of
        the data model - be it in the minor or major parts of the tool's version - won't
        lead to an update of _versionDataModel. */
    public static final String _version = "" + _versionAry[0]
                                          + "." + _versionAry[1]
                                          + "." + _versionAry[2];

    /** The full version of the tool, including the forth part, the build number. */
    public static final String _versionFull = _version + "." + _versionAry[3];
    
    /** The version of the data model, which is input to the templates. The integer number
        is composed as M*1000+m if M.m.f is the version designation of the tool when the data
        model was changed the last time.
          @remark Any change made here needs to be done identically in the data model, file
        Info.java, field Info.versionDataModel. */
    public static final int _versionDataModel = 1004;

    /** The global logger object for all progress and error reporting. It is initialized to
        null in order to give time to the other class {@link Log4j2Configurator} to first
        configure the loggers according to the command line settings. */
    private static Logger _logger = null;

    /** A help text, which is printed together with the usage message derived from the
        command line arguments. */
    private static final String _applicationHelp =
        // (setq fill-column 89 fill-prefix "        + \"")
        "The command line interface of excelExporter has the following concept: The\n"
        + "arguments form groups. A group of successive arguments can specify an input\n"
        + "file, another group can specify another input file or an output file and so\n"
        + "on. The beginning of a group of arguments is recognized by a specific\n"
        + "argument, the principal argument of the group. The description of the command\n"
        + "line arguments typically says \"this argument opens the context of ...\".\n"
        + "Naturally, the same command line switches can be repeatedly used, once in\n"
        + "each group of same kind.\n"
        + "  Such a group of command line arguments or a \"context\" actually is the\n"
        + "representation of an object in the parameter tree of the application. This is\n"
        + "the model behind the parameter tree:\n"
        + "\n"
        + "- Root elements are either Excel input file specifications, specifications of\n"
        + "  generated output files or worksheet templates\n"
        + "- The input file specification contains the Excel file name and it has any\n"
        + "  number of worksheet selection objects as children\n"
        + "    - A worksheet selection specifies one or more worksheets for parsing. All\n"
        + "      sheets or any sub-set of sheets of a workbook can be parsed. Selection\n"
        + "      can be made by name or by index\n"
        + "- An output file specification contains the file name and information about\n"
        + "  the StringTemplate V4 template to be applied\n"
        + "- A worksheet template is a set of rules how to interpret one or more\n"
        + "  worksheets. It can be applied to a particular worksheet or to several of\n"
        + "  those, from either one or several input files. It describes how the\n"
        + "  data of a worksheet is organized in terms of groups and sub-groups. It has\n"
        + "  any number of column attribute objects as children\n"
        + "    - A column attributes object specifies properties of a column, like name\n"
        + "      and sort order\n"
        + "\n"
        + "Besides the command line arguments from a group or context, there are\n"
        + "conventional command line arguments, which relate to the run of the\n"
        + "application as a whole, like logging and verbosity settings. It's said that\n"
        + "they belong to the global context.\n"
        + "  Please note, different to the common GNU command line interface this\n"
        + "application demands a blank between the switch and its value. For example\n"
        + "-oMyOutputFile.c would be rejected, whereas -o MyOutputFile.c would be the\n"
        + "correct specification of a generated output file.";

    /** The structure that holds all command line parameters. */
    private CmdLineParser cmdLineParser_ = null;

    /** The global structure that holds all runtime parameters. Here, the field is
        initialized to null. True initialization is done at run time to avoid early loading
        of the log4j class (which may be in use by the field's class). Early loading of the
        log4j class would shortcut the command line controlled initialization of the
        logging. */
    private ParameterSet parameterSet_ = null;

    /** The correct EOL in abbreviated form. */
    private static final String NL = System.lineSeparator();

    /** The log4j configurator provides access to the logging settings of this application
        run. */
    private Log4j2Configurator log4j2Configurator = null;


    /**
     * The nested directories required for file creation are created.
     * The method extracts the path from the given file name and creates all directories
     * required to make this path existing.
     *   @return
     * true, if method succeeded, else false.
     *   @param fileName
     * The file name of file to be created. May be relative or absolute.
     */
    static private boolean createDir(String fileName)
    {
        return createDir(new File(fileName));

    } /* End of ExcelExporter.createDir. */




    /**
     * The nested directories required for file creation are created.
     * The method extracts the path from the given file name and creates all directories
     * required to make this path existing.
     *   @return
     * true, if method succeeded, else false.
     *   @param fileNameObj
     * The file name of a file to be created as a File object. May be relative or absolute.
     */
    static private boolean createDir(File fileNameObj)
    {
        boolean success = true;

        /* The following operation will only fail if a root directory has been specified.
           This is caught with the if. */
        if(!fileNameObj.isDirectory())
            fileNameObj.getAbsoluteFile().getParentFile().mkdirs();
        else
            success = false;

        return success;

    } /* End of ExcelExporter.createDir. */




    /**
     * Create a command line parser and define all command line arguments. This method
     * defines the arguments owned by the application main class and it calls the argument
     * definition functions of all other modules that have their own arguments.
     *   @remark The command line processor will detect unwanted redefinitions of command
     * line arguments and reports them as runtime exception. This strongly supports safe
     * development of different modules each having ist individual command line arguments.
     */
    private void defineArguments()
    {
        assert cmdLineParser_ == null: "Don't parse the command line twice";
        cmdLineParser_ = new CmdLineParser();

        /* Define all expected arguments ... */
        cmdLineParser_.defineArgument( "h"
                                     , "help"
                                     , /* cntMax */ 1
                                     , "Demand this help."
                                     );

        /* Let the logger configurator define its command line arguments. */
        Log4j2Configurator.defineArguments(cmdLineParser_);

        /* Let the parameter module define its further command line arguments. */
        ParameterSet.defineArguments(cmdLineParser_);

        /* No unnamed arguments are expected. */
        //cmdLineParser_.defineArgument( /* cntMin, cntMax */ 0, -1
        //                            , /* defaultValue */ null
        //                            , ""
        //                            );

    } /* End of defineArguments */



    /**
     * Read and check the command line arguments.
     *   The command line is evaluated. If an error is found the usage is displayed.
     *   @return true is returned if the method succeeds. Then retrieve all command line
     * options from member cmdLineParser_. If the function fails it returns false. The main
     * program should end silently.
     *   @param argAry
     * The command line arguments of the application.
     */
    private boolean parseCmdLine(String[] argAry)
    {
        assert cmdLineParser_ != null;
        try
        {
            cmdLineParser_.parseArgs(argAry);

            if(cmdLineParser_.getBoolean("h"))
            {
                /* ... and explain them. */
                greeting();
                System.out.print(cmdLineParser_.getUsageInfo( _applicationName
                                                            , /* argumentsTabularOnly */ true
                                                            )
                                 + NL + _applicationHelp + NL
                                );
                return false;
            }
        }
        catch(CmdLineParser.InvalidArgException e)
        {
            greeting();
            System.err.print(cmdLineParser_.getUsageInfo( _applicationName
                                                        , /* argumentsTabularOnly */ true
                                                        )
                             + NL + _applicationHelp + NL
                             + NL + "Invalid command line. " + e.getMessage() + NL
                            );
            return false;
        }

        return true;

    } /* End of ExcelExporter.parseCmdLine. */



    /**
     * This method implements the application behovior. Call it once from the main function
     * run is synchronous and does not fork another task or process.
     *   @return
     * <b>true</b>, if method succeeded, else <b>false</b>.
     */
    public boolean run()
    {
        /* Now we can instantiate a logger for the classes, which are involved in the log4j
           configuration, which normally means that they offer a static method
           defineArguments. This method needs to be necessarily called prior to the log4j
           configuration (as command line evaluation has do be done to get the arguments of
           the wanted configuration) but the constructor of those classes must not: this
           would make the class loader load also log4j and configuration would not take
           place timely. log4j would be initialized with the default settings. */
        /// @todo The whole initialization process is error prone and hard to understand.
        // Would it be an idea to have two times command line parsing: One only with the
        // arguments defined for log4j configuration, then doing this configuration, then
        // involving all other modules and parsing again. It would require to make the
        // command line parser tolerant against unknown (since in pass one not yet defined)
        // arguments.
        ParameterSet.loadLog4jLogger();

        /* Only now - after initialization of the log4j 2 class - we instantiate all
           required objects. This has not been done before as their classes could depend on
           log4j code already at class load time; and early loading of the log4j class
           would shortcut the log4j initialization as implemented by the application main
           function. */
        parameterSet_ = new ParameterSet();

        /* The logging related command line argument are not managed or parsed by the class
           ParameterSet. However it permits to store them in order to include them in
           reporting the current application parameter set. Here, we just copy the
           arguments as parsed by the log4j configurator into the parameter object. */
        parameterSet_.useStdLog4j2Config = log4j2Configurator.getUseStdConfigSequence();
        parameterSet_.logFileName = log4j2Configurator.getLogFileName();
        parameterSet_.logLevel = log4j2Configurator.getLogLevel();
        parameterSet_.log4j2Pattern = log4j2Configurator.getLogPattern();

        try
        {
            /* Parse the context dependent command line arguments, which may appear
               repeatedly in different contexts. */
            parameterSet_.parseCmdLine(cmdLineParser_);

            /* Echo the (complex structured) command line information in a structured way,
               which reflects how it has been understood. */
            _logger.info("Command line arguments:" + NL + parameterSet_);
        }
        catch(CmdLineParser.InvalidArgException e)
        {
            /* The log4j logger is not applied here in order to get the same look and feel
               as in case of the more basic command line errors, which had been trapped and
               reported in parseCmdLine. */
            System.err.print(cmdLineParser_.getUsageInfo( _applicationName
                                                        , /* argumentsTabularOnly */ true
                                                        )
                             + NL + "Invalid command line. " + e.getMessage() + NL
                            );
            return false;
        }

        boolean success = true;
        int successfullyParsedFiles = 0;

        /* A single error counter is used for all operations. Its life cycle ends with the
           run of the application. However, it'll be repeatedly reset to null. A reference
           to this error counter is passed to involved modules and objects.
             A second counter is used to report the total number of issues at the end of
           the application run. */
        final ErrorCounter errCnt = new ErrorCounter()
                         , totalErrCnt = new ErrorCounter();

        Identifier.setErrorContext(errCnt, /* context */ "Name disambiguation: ");

        /* A single parser object is used for all workbooks. */
        final ExcelParser parser = new ExcelParser(parameterSet_, errCnt);

        /* Prepare the still empty set of global worksheet groups.
             Remark: ExcelExporter 0.16 switches from a HashMap to a TreeMap. The reason is
           the undefined sort order of the implementation of HashMap. This sort order has
           an impact on the rendered information if the StringTemplate V4 engine's Map
           iteration is applied. Although actually irrelevant let this to irritation as the
           Java implementation changed from Java 1.7 to 1.8. The same application using the
           same StringTemplate V4 templates produces different output with these two Java
           runtime revisions. The difference can be seen with sample treeView. Now using
           TreeMap do the differences between the Java revisions disappear. (Although we
           still have a difference to the elder HashMap implementation with Java 1.7.) */
        Map< /* groupName */ String
           , /* group */     ObjectList<ExcelWorksheet>
           > mapOfWorksheetGroupsByName = new TreeMap<String,ObjectList<ExcelWorksheet>>();

        Cluster cluster = new Cluster(errCnt, parameterSet_.clusterName);

        /* Loop along all Excel workbooks demanded on the command line. */
        if(parameterSet_.workbookAry.size() > 0)
        {
            for(int idxFile=0; idxFile<parameterSet_.workbookAry.size(); ++idxFile)
            {
                final ParameterSet.WorkbookDesc excelFileDesc = parameterSet_
                                                                .workbookAry.get(idxFile);

                final FileExt excelFile = new FileExt(excelFileDesc.fileName);

                /* This will output the full path where the file is read from. */
                _logger.info("Excel input file: {}", excelFile.getAbsolutePath());

                /* Parse the workbook. If the returned object is not null then it can still
                   be invalid in the sense that it doesn't contain any sheet. null can
                   happen because of parsing errors. */
                final ExcelWorkbook workbook = parser.parseXlsFile( mapOfWorksheetGroupsByName
                                                                  , idxFile
                                                                  );
                assert workbook != null  ||  errCnt.getNoErrors() > 0;

                if(errCnt.getNoErrors() == 0)
                {
                    ++ successfullyParsedFiles;

                    /* Add the user options just like that without any data modification or
                       handling. */
                    assert workbook != null;
                    workbook.optionMap = excelFileDesc.optionMap;

                    _logger.log( errCnt.getNoWarnings() > 0? Level.WARN: Level.INFO
                               , "Parsing done with {} errors and {} warnings"
                               , errCnt.getNoErrors()
                               , errCnt.getNoWarnings()
                               );

                    /* Add the completed workbook object to the data model if there are no
                       errors. */
                    cluster.putBook(workbook);
                }
                else
                {
                    success = false;
                    _logger.error( "Parsing done with {} errors and {} warnings"
                                 , errCnt.getNoErrors()
                                 , errCnt.getNoWarnings()
                                 );
                    _logger.error( "Parse result from Excel input file {}"
                                   + " is rejected due to previous errors. The rendered"
                                   + " data model won't contain information from this"
                                   + " Excel workbook"
                                 , excelFile.getPath()
                                 );
                }

                /* Error counting and reporting is done separately for all parsed
                   worksbook. We collect all errors for a final overall result. */
                totalErrCnt.add(errCnt);
                errCnt.reset();

            } /* while(All command line demanded Excel input files) */
        }
        else
        {
            /* Not defining any input file is not considered an error. There are numerous
               useful applications of StringTemplate V4 templates, which are self-contained
               and don't need any input data or which depend only on a few data items
               passed in directly on the command line as user options. It's worth a
               consideration if the next message is informative or a warning. */
            _logger.info("No Excel input file is defined");
        }

        assert errCnt.getNoErrors() == 0  &&  errCnt.getNoWarnings() == 0;

        /* Add the set of global worksheet groups to the data model. */
        if(mapOfWorksheetGroupsByName.size() > 0)
            cluster.sheetGroupMap = mapOfWorksheetGroupsByName;

        /* The data model of the network is complete. Pass it to the template engine. Data
           rendering is done if there's either one successfully parsed file or if there's
           no file to parse at all - there are useful applications of StringTemplate V4
           templates, which don't need input data. */
        if(successfullyParsedFiles > 0  ||  parameterSet_.workbookAry.size() == 0)
        {
            /* Sort the workbooks and sheets in the cluster's collections with respect to
               their names. */
            cluster.sort(parameterSet_.sortOrderWorkbooks, parameterSet_.sortOrderWorksheets);

            /* Give feedback to the user about usability of the direct access data
               references. */
            if(successfullyParsedFiles > 0)
                cluster.reportDirectDataAccess();

            /* Run the template engine repeatedly - different templates will render the
               parse information into different output files. */
            Iterator<ParameterSet.TemplateOutputPair> itOFile =
                                                parameterSet_.templateOutputPairAry.iterator();
            while(itOFile.hasNext())
            {
                assert errCnt.getNoErrors() == 0  &&  errCnt.getNoWarnings() == 0;

                Info info = new Info(errCnt);
                info.setApplicationInfo(_applicationName
                                       , _versionAry
                                       , _versionDataModel
                                       );

                ParameterSet.TemplateOutputPair templateOutputPair = itOFile.next();
                info.setTemplateInfo( templateOutputPair.templateFileName
                                    , templateOutputPair.templateName
                                    , templateOutputPair.templateArgNameCluster
                                    , templateOutputPair.templateArgNameInfo
                                    , templateOutputPair.templateWrapCol
                                    );
                info.setOutputInfo(templateOutputPair.outputFileName);

                /* Pass the output related user attributes from the command line into the
                   template. */
                info.setUserOptions(templateOutputPair.optionMap);

                STGroup stg = null;
                try
                {
                    stg = new STGroupFile(templateOutputPair.templateFileName);
                }
                catch(Exception e)
                {
                    errCnt.error();
                    _logger.error("Error reading template group file. {}", e.getMessage());
                    success = false;
                }

                if(stg != null)
                {
                    /* By experience, the first true use of the template group object
                       starts the template compilation - this is not only done at the
                       obvious locations getInstanceOf or render. Since we use runtime
                       exceptions in our error listener to abort the template expansion all
                       of these actions need to be try/catch protected, regadless whether
                       the ST4 APIs declare a throw or not. */
                    String generatedCode = null;
                    try
                    {
                        /* Install our listener to get the ST4 messages into our
                           application log and to count internal ST4 errors, too. */
                        stg.setListener(new ST4ErrorListener(errCnt));

                        stg.verbose = parameterSet_.stringTemplateVerbose;
                        stg.registerRenderer(Number.class, new NumberRenderer());
                        stg.registerRenderer(String.class, new StringRenderer());
                        stg.registerRenderer(Calendar.class, new DateRenderer());
                        ST template = stg.getInstanceOf(templateOutputPair.templateName);
                        if(template != null)
                        {
                            if(errCnt.getNoErrors() == 0)
                            {
                                _logger.info( "Network information is rendered according to"
                                              + " template {}:{}({},{})"
                                            , templateOutputPair.templateFileName
                                            , templateOutputPair.templateName
                                            , templateOutputPair.templateArgNameCluster
                                            , templateOutputPair.templateArgNameInfo
                                            );
                                template.add( templateOutputPair.templateArgNameCluster
                                            , cluster
                                            );
                                template.add(templateOutputPair.templateArgNameInfo, info);
                                if(templateOutputPair.templateWrapCol > 0)
                                {
                                    generatedCode = template.render(templateOutputPair
                                                                    .templateWrapCol
                                                                   );
                                }
                                else
                                    generatedCode = template.render();

                                /* The error counter had been passed to the data model and
                                   there it collects template emitted errors and warnings.
                                   Code generation can have failed even if the template
                                   expansion succeeded. */
                            }
                            else
                            {
                                errCnt.error();
                                _logger.error("Template group file "
                                              + templateOutputPair.templateFileName
                                              + " is not usable. See previous error messages"
                                             );
                                success = false;
                            }
                        }
                        else
                        {
                            errCnt.error();
                            _logger.error("Template {}:{} not found. Please,"
                                          + " double check file name, CLASSPATH (the search"
                                          + " path for template files) and the name of"
                                          + " the template. See command line options"
                                          + " template-file-name and template-name, too"
                                         , templateOutputPair.templateFileName
                                         , templateOutputPair.templateName
                                         );
                            success = false;
                        }
                    }
                    catch(Exception e)
                    {
                        errCnt.error();
                        _logger.error( "Error rendering the information. Template"
                                       + " expansion failed: {}"
                                     , e.getMessage()
                                     );
                        success = false;
                    }

                    _logger.info( "Template expansion done with {} errors and {} warnings"
                                , errCnt.getNoErrors()
                                , errCnt.getNoWarnings()
                                );

                    if(errCnt.getNoErrors() == 0)
                    {
                        final PrintStream out;
                        if("stdout".equalsIgnoreCase(templateOutputPair.outputFileName))
                            out = System.out;
                        else if("stderr".equalsIgnoreCase(templateOutputPair.outputFileName))
                            out = System.err;
                        else
                            out = null;

                        if(out != null)
                        {
                            /* Write generated code into a standard console stream. */
                            out.print(generatedCode);
                        }
                        else
                        {
                            /* Write generated code into output file. */
                            File outputFile = new File(templateOutputPair.outputFileName);

                            BufferedWriter writer = null;
                            try
                            {
                                /* This will output the full path where the file is written
                                   to. */
                                _logger.info( "The rendered input is written into file {}"
                                            , outputFile /*.getCanonicalPath()*/
                                            );
                                /* Ensure that all needed parents exist for the file. */
                                createDir(outputFile);

//                                FileWriter fileWriter = new FileWriter(outputFile);
//                                assert fileWriter != null: "fileWriter is null";
//                                writer = new BufferedWriter(fileWriter);
                                FileOutputStream outputFileStream = new FileOutputStream(outputFile);
                                writer = new BufferedWriter
                                                (new OutputStreamWriter( outputFileStream
                                                                       , "UTF-8"
                                                                       //, "ISO-8859-1"
                                                                       //, "UTF-16"
                                                                       )
                                                );
                                writer.write(generatedCode);
                            }
                            catch(IOException e)
                            {
                                success = false;
                                errCnt.error();
                                _logger.error( "Error writing generated file. {}"
                                             , e.getMessage()
                                             );
                            }

                            /* Close the writer regardless of what happened. */
                            try
                            {
                                if(writer != null)
                                    writer.close();
                            }
                            catch(IOException e)
                            {
                                success = false;
                                errCnt.error();
                                _logger.error( "Error closing generated file. {}"
                                             , e.getMessage()
                                             );
                            }
                        }
                    }
                    else
                    {
                        success = false;
                        _logger.info( "Output file {} is not generated due to previous errors"
                                    , templateOutputPair.outputFileName
                                    );
                    }
                } /* End if(Template file successfully read) */

                /* Error counting and reporting is done separately for all generated output
                   files. We collect all errors for a final overall result. */
                totalErrCnt.add(errCnt);
                errCnt.reset();

            } /* End while(All pairs (template, output file)) */

        } /* End if(Do we have to render at least one successfully parsed File?) */

        final String logMsg = _applicationName + " terminating with {} errors and {} warnings";
        final Level level;
        if(totalErrCnt.getNoErrors() > 0)
            level = Level.ERROR;
        else if(totalErrCnt.getNoWarnings() > 0)
            level = Level.WARN;
        else
            level = Level.INFO;
        _logger.log(level, logMsg, totalErrCnt.getNoErrors(), totalErrCnt.getNoWarnings());

        return success;

    } /* End of ExcelExporter.run. */





    /**
     * Print the application's title to stdout.
     */
    private static void greeting()
    {
        /* Printing the applied version of ANTLR and StringTemplate is useful but unsafe.
           By experiment, it turned out that the printed values do not depend on the
           run-time jars. Instead the values are frozen at compilation time, i.e., when
           excelExporter-*.jar is made. If the application is run with another classpath
           than the compiler then the printed versions may not match the actually used,
           actually running libraries. */
        final String greeting = _applicationName + " " + _versionFull
                                + " Copyright (C) 2015-2025, Peter Vranken"
                                + " (mailto:Peter_Vranken@Yahoo.de)"
                                + "\n"
                                + "including: StringTemplate "
                                + org.stringtemplate.v4.ST.VERSION;
        System.out.println(greeting);

    } /* End of greeting */



    /**
     * Main entry point when run via command line.
     *   @throws java.lang.Exception
     * General errors are reported by exception.
     *   @param argAry
     * The command line.
     */
    public static void main(String[] argAry) throws Exception
    {
        /* Create the one and only object of this class. It implements the application's
           behavior. */
        ExcelExporter This = new ExcelExporter();

        /* Create a command line parser and define all command line arguments. Then parse
           the actual command line. This is only pass one of command line parsing, which
           double-checks the static constraints as made in the definitions. */
        This.defineArguments();
        if(This.parseCmdLine(argAry))
        {
            /* Print the application greeting. This is not done at higher log levels: If
               information rendering is done to stdout then the log level will surely be
               set to WARN at minimum - in which case a greeting will definitely distort the
               intended (automation) idea.
                 A better condition to suppress the greeting would be to ask if there's at
               least one information rendering command that goes into stdout or stderr.
               However, this can't be implemented: We'd need the full command line
               evaluation to figure this out and the command line evaluation can already
               produce a lot of logging information - our greeting came much too late. */
            if(Log4j2Configurator
               .getLogLevel(This.cmdLineParser_).isLessSpecificThan(Level.INFO)
              )
            {
                greeting();
            }

            /* Configure log4j2 prior to first use. This is done by side-effect of a
               constructor call. The object is kept only to have access to the configured
               logging settings; they are reported into the application log later. */
            This.log4j2Configurator = new Log4j2Configurator(This.cmdLineParser_);

            /* Get the class' logger instance only after completing the log4j2
               configuration. */
            _logger = LogManager.getLogger(ExcelExporter.class);

            boolean success = This.run();
            _logger.debug( "{} terminating {}"
                         , _applicationName
                         , success? "successfully": "with errors"
                         );
            System.exit(success? 0: 1);
        }
    } /* End of ExcelExporter.main. */

} /* End of class ExcelExporter definition. */

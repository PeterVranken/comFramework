/**
 * @file CodeGenerator.java
 * Main entry point into the C code generator of the COM framework.
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
/* Interface of class CodeGenerator
 *   CodeGenerator
 *   createDir
 *   parseCmdLine
 *   initLog4j
 *   run
 *   main
 */

package codeGenerator.main;

import java.util.*;
import java.io.*;

import cmdLineParser.*;
import codeGenerator.dbcParser.*;
import codeGenerator.dataModelListener.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.stringtemplate.v4.*;
import org.apache.log4j.*;
//import org.apache.logging.log4j.*;


/**
 * This class has a main function, which implements the code generator application.
 *   @author Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
 *   @see CodeGenerator#main
 */

public class CodeGenerator
{
    /** The name of this Java application. */
    public static final String _applicationName = "comFramework - codeGenerator";

    /** The parts of the version designation: major, minor, fix, build. */
    private static int[] _versionAry = {1, 12, 2, GitRevision.getProjectRevision()};

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
        model was changed the last time. */
    public static final int _versionDataModel = 1012;

    /** The global logger object for all progress and error reporting. */
    private static Logger _logger = Logger.getLogger(CodeGenerator.class.getName());

    /** The structure that holds all command line parameters. */
    private CmdLineParser cmdLineParser_ = null;

    /** The global structure that holds all runtime parameters. */
    private ParameterSet parameterSet_ = null;

    /** The correct EOL in abbreviated form. */
    private final String NL = System.lineSeparator();

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

    } /* End of CodeGenerator.createDir. */




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

    } /* End of CodeGenerator.createDir. */




    /**
     * Read and check the command line arguments.
     *   The command line is evaluated. If an error is found the usage is displayed.
     * @return true is returned if the method succeeds. Then retrieve all command line
     * options from member cmdLineParser_. If the function fails it returns false. The main
     * program should end.
     * @param argAry
     *   The command line arguments of the program.
     */
    private boolean parseCmdLine(String[] argAry)
    {
        assert cmdLineParser_ == null: "Don't parse the command line twice";
        cmdLineParser_ = new CmdLineParser();

        /* Define all expected arguments ... */
        cmdLineParser_.defineArgument( "h"
                                     , "help"
                                     , /* cntMax */ 1
                                     , "Demand this help."
                                     );
        cmdLineParser_.defineArgument( "v"
                                     , "verbosity"
                                     , /* cntMin, cntMax */ 0, 1
                                     , /* defaultValue */ "INFO"
                                     , "Verbosity of all logging. Specify one out of OFF, "
                                       + "FATAL, ERROR, WARN, or INFO. Default is INFO."
                                     );
        cmdLineParser_.defineArgument( "l"
                                     , "log-file"
                                     , /* cntMin, cntMax */ 0, 1
                                     , /* defaultValue */ null
                                     , "If given, a log file is written containing general "
                                       + "program flow messages."
                                     );
        cmdLineParser_.defineArgument( "p"
                                     , "log4j-pattern"
                                     , /* cntMin, cntMax */ 0, 1
                                     , /* defaultValue */ null
                                     , "A pattern for the log file entries may be specified, "
                                       + "e.g. \"%d %p: [%t]: %m%n\". See "
                                       + "Log4j.PatternLayout for details. The default will "
                                       + "be most often sufficient."
                                     );

        /* No unnamed arguments are expected. */
        //cmdLineParser_.defineArgument( /* cntMin, cntMax */ 0, -1
        //                            , /* defaultValue */ null
        //                            , ""
        //                            );

        /* Let the parameter module define its further command line arguments. */
        ParameterSet.defineArguments(cmdLineParser_);

        parameterSet_ = new ParameterSet();
        try
        {
            cmdLineParser_.parseArgs(argAry);

            /* The normal one-global-value (i.e. context free) arguments are immediately
               accessible after parsing. This suffices to initalize the Apache logger,
               which is required to report the progress of the further parsing of the
               context dependent arguments. */
            initLog4j();

            /* Now parse the context dependent arguments, which may appear repeatly in
               different contexts. */
            parameterSet_.parseCmdLine(cmdLineParser_);

            if(cmdLineParser_.getBoolean("h"))
            {
                /* ... and explain them. */
                System.out.print(cmdLineParser_.getUsageInfo(_applicationName));
            }
            else
            {
                /* Echo the (complex structured) command line information as parameter struct
                   as it has been understood. */
                _logger.info("Command line arguments:" + NL + parameterSet_);
            }
        }
        catch(CmdLineParser.InvalidArgException e)
        {
            parameterSet_ = null;
            System.out.print(cmdLineParser_.getUsageInfo(_applicationName)
                             + NL + "Invalid command line. " + e.getMessage() + NL
                            );
            return false;
        }

        return !cmdLineParser_.getBoolean("h");

    } /* End of CodeGenerator.parseCmdLine. */




    /**
     * Initialize the log4j logger module. The basic settings are taken from the members
     * that reflect the respective command line options -v, -l, -p.
     */
    private void initLog4j()
    {
        String verbosity   = cmdLineParser_.getString("v")
             , logFileName = cmdLineParser_.getString("l")
             , pattern     = cmdLineParser_.getString("p");

        /* Create the logging directories. */
        if( logFileName != null )
            createDir(logFileName);

        final Level wantedLogLevel = Level.toLevel(verbosity, Level.INFO);
        if(pattern == null  ||  pattern.length() == 0)
            pattern = "%d %-5p %m%n";

        /* Set up a simple configuration that logs on the console. */
        BasicConfigurator.configure(); // See http://logging.apache.org/log4j/docs/index.html

        /* Configure the root logger. All packages' loggers will inherit the settings. */
        Logger rootLogger = Logger.getRootLogger();
        if(logFileName != null)
        {
            try
            {
                rootLogger.addAppender(new FileAppender( new PatternLayout(pattern)
                                                       , logFileName
                                                       )
                                      );
            }
            catch(IOException e)
            {
                rootLogger.error("Can't create log file " + logFileName + ": "
                                 + e.getMessage()
                                );
                //e.printStackTrace();
            }
        } /* End if(Log file wanted?) */

        rootLogger.setLevel(wantedLogLevel);

        _logger.info("Log started");

    } /* End of CodeGenerator.initLog4j. */





    /**
     * After setting all parameters (by constructor) call this method to perform the
     * operation. run is synchronous and does not fork another task or process.
     *   @return
     * <b>true</b>, if method succeeded, else <b>false</b>.
     */
    public boolean run()
    {
        /* A single error counter is used for all operations. Its life cycle ends with the
           run of the application. However, it'll be repeatedly reset to null. A reference
           to this error counter is passed to involved modules and objects.
             A second counter is used to report the total number of issues at the end of
           the application run. */
        final ErrorCounter errCnt = new ErrorCounter()
                         , totalErrCnt = new ErrorCounter();

        /* A single parser object is used for all network specification files. */
        final DbcParserMain parser = new DbcParserMain();

        /* The cluster object basically is the list of buses, which are got from parsing a
           single network databse file. We start here with an still emtpy cluster. */
        Cluster cluster = new Cluster();
        cluster.name = parameterSet_.clusterName;
        cluster.nodeName = parameterSet_.nodeName;

        Iterator<ParameterSet.BusDescription> itBus =
                                                parameterSet_.busDescriptionAry.iterator();
        int successfullyParsedBuses = 0;
        boolean success = true;
        while(itBus.hasNext())
        {
            assert errCnt.getNoErrors() == 0  &&  errCnt.getNoWarnings() == 0;

            ParameterSet.BusDescription busDescription = itBus.next();
            File networkDbFile = new File(busDescription.networkFileName);

            /* This will output the full path where the file is read from. */
            _logger.info("Next network database file: " + networkDbFile.getAbsolutePath());

            errCnt.reset();
            ParseTree parseTree = parser.parse(busDescription.networkFileName, errCnt);
            Bus bus;
            if(parseTree != null)
            {
                /* Walk trough the parse tree using a listener and pick out the relevant
                   information. Get the transformed data back. */
                bus = DataModelListener.walk(parseTree, busDescription, errCnt);
            }
            else
            {
                assert errCnt.getNoErrors() > 0: "Unreported error";
                bus = null;
                success = false;
            }

            final Level logLevel =
                    errCnt.getNoErrors() > 0
                        ? Level.ERROR
                        : (errCnt.getNoWarnings() > 0? Level.WARN: Level.INFO);
            _logger.log( logLevel
                       , "Parsing done with " + errCnt.getNoErrors() + " errors and "
                         + errCnt.getNoWarnings() + " warnings"
                       );

            /* Add a successfully parsed bus to the cluster object. */
            if(bus != null)
            {
                ++ successfullyParsedBuses;

                /* Convenience in templates: enumerate all buses. */
                bus.i = successfullyParsedBuses;
                bus.i0 = bus.i - 1;

                /* Add the bus to the bus list. */
                cluster.addBus(bus);

                /* Propagate the warnings from the bus to the cluster. */
                if(bus.hasFloatingPointSignals)
                    cluster.hasFloatingPointSignals = true;
                if(bus.hasScaledFloatingPointSignals)
                    cluster.hasScaledFloatingPointSignals = true;
            }
            else
            {
                assert errCnt.getNoErrors() > 0: "Unreported error";
                success = false;
                _logger.error("Parse result from network database "
                              + networkDbFile.getPath()
                              + " is rejected due to previous errors. The rendered"
                              + " data model won't contain information from this"
                              + " network database file"
                             );
            }

            /* Error counting and reporting is done separately for all parsed network
               specification files. We collect all errors for a final overall result. */
            totalErrCnt.add(errCnt);
            errCnt.reset();

        } /* End while(All buses definitions in parameter set) */

        /* The data model of the network is complete. Pass it to the template engine. Do
           this repeatedly - different templates will render the information into different
           output files. */
        if(successfullyParsedBuses > 0)
        {
            Iterator<ParameterSet.TemplateOutputPair> itOFile =
                                                parameterSet_.templateOutputPairAry.iterator();
            while(itOFile.hasNext())
            {
                assert errCnt.getNoErrors() == 0  &&  errCnt.getNoWarnings() == 0;

                Info info = new Info(errCnt);
                info.setApplicationInfo( _applicationName
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
                    _logger.error("Error reading template group file. " + e.getMessage());
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
                        ST template = stg.getInstanceOf(templateOutputPair.templateName);
                        if(template != null)
                        {
                            if(errCnt.getNoErrors() == 0)
                            {
                                _logger.info("Network information is rendered according to"
                                             + " template "
                                             + templateOutputPair.templateFileName + ":"
                                             + templateOutputPair.templateName + "("
                                             + templateOutputPair.templateArgNameCluster + ", "
                                             + templateOutputPair.templateArgNameInfo + ")"
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
                            _logger.error("Template " + templateOutputPair.templateFileName
                                          + ":" + templateOutputPair.templateName
                                          + " not found. Please, double check file name,"
                                          + " CLASSPATH (the search path for template files)"
                                          + " and the name of the template. See command"
                                          + " line options template-file-name and"
                                          + " template-name, too"
                                         );
                            success = false;
                        }
                    }
                    catch(Exception e)
                    {
                        errCnt.error();
                        _logger.error("Error rendering the information. Template"
                                      + " expansion failed: " + e.getMessage()
                                     );
                        success = false;
                    }

                    final Level logLevel =
                            errCnt.getNoErrors() > 0
                                ? Level.ERROR
                                : (errCnt.getNoWarnings() > 0? Level.WARN: Level.INFO);
                    _logger.log( logLevel
                               , "Template expansion done with " + errCnt.getNoErrors()
                                 + " errors and " + errCnt.getNoWarnings() + " warnings"
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
                                _logger.info( "Generated code is written into file "
                                              + outputFile /*.getCanonicalPath()*/
                                            );

                                /* Ensure that all needed parents exist for the file. */
                                createDir(outputFile);

                                FileWriter fileWriter = new FileWriter(outputFile);
                                assert fileWriter != null: "fileWriter is null";
                                writer = new BufferedWriter(fileWriter);

                                // @todo Consider generation of utf-8 on command line demand. Investigate BOM issue
                                // http://stackoverflow.com/questions/1001540/how-to-write-a-utf-8-file-with-java
                                //writer = new BufferedWriter
                                //                ( new OutputStreamWriter
                                //                        ( new FileOutputStream(outputFile)
                                //                        , "UTF-8"
                                //                        )
                                //                );

                                writer.write(generatedCode);
                            }
                            catch(IOException e)
                            {
                                success = false;
                                _logger.error("Error writing generated file. "
                                              + e.getMessage()
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
                                _logger.error( "Error closing generated file. "
                                               + e.getMessage()
                                             );
                            }
                        }
                    }
                    else
                    {
                        success = false;
                        _logger.info("Output file " + templateOutputPair.outputFileName
                                     + " is not generated due to previous errors"
                                    );
                    }
                } /* End if(Template file successfully read) */

                /* Error counting and reporting is done separately for all generated output
                   files. We collect all errors for a final overall result. */
                totalErrCnt.add(errCnt);
                errCnt.reset();

            } /* End while(All pairs (template, output file)) */
        } /* End if(Do we have to render at least one successfully parsed bus?) */

        final String logMsg = _applicationName + " terminating with "
                              + totalErrCnt.getNoErrors() + " errors and "
                              + totalErrCnt.getNoWarnings() + " warnings";
        final Level level;
        if(totalErrCnt.getNoErrors() > 0)
            level = Level.ERROR;
        else if(totalErrCnt.getNoWarnings() > 0)
            level = Level.WARN;
        else
            level = Level.INFO;
        _logger.log(level, logMsg);

        return success;

    } /* End of CodeGenerator.run. */





    /**
     * Main entry point when run via command line.
     *   @param argAry
     * The command line.
     */
    public static void main(String[] argAry)
    {
        /* Printing the applied version of ANTLR and StringTemplate is useful but unsafe.
           By experiment, it turned out that the printed values do not depend on the
           run-time jars. Instead the values are frozen at compilation time, i.e., when
           comFrameworkCodeGenerator-*.jar is made. If the application is run with another
           classpath than the compiler the printed versions may not match the actually
           used, actually running libraries.
             This is particularly critical since ANTLR.jar contains its own copy of
           StringTemplate, but the contained version of ST used to be not always the latest
           available one. As of writing, Feb 2023, the last recent version of ANTLR,
           4.12.0, contains the last recent version of ST, which is 4.3.4.
             A way out of these uncertainties would be deleting ST from the ANTLR jar
           (proven to work well) but we don't won't to publish our own, manipulated ANTLR
           jar.
             The similar way of doing fails: ANTLR is offered as antlr-runtime-4.8.jar,
           too. This jar doesn't contain the grammar compiler but promises to support the
           parser once generated. However, our application doesn't run with this jar, a
           missing class is reported.
             The only other way out is to use the same class paths at build and at run-time
           and trust on the Java compiler and Java runtime making identical decisions on
           which classes to load from which jars.
             By placing the ST jar always before the ANTLR jar, we can benefit from recent
           maintenance releases of ST. */
        final String greeting = _applicationName + " " + _versionFull
                                + " Copyright (C) 2015-2024, Peter Vranken"
                                + " (mailto:Peter_Vranken@Yahoo.de)"
                                + "\n"
                                + "including: ANTLR "
                                + org.antlr.v4.runtime.RuntimeMetaData.VERSION
                                + " and StringTemplate "
                                + org.stringtemplate.v4.ST.VERSION;
        System.out.println(greeting);

        CodeGenerator This = new CodeGenerator();

        if(This.parseCmdLine(argAry))
        {
            final boolean success = This.run();
            _logger.debug( _applicationName + " terminating "
                           + (success? "successfully": "with errors")
                         );
            System.exit(success? 0: 1);
        }
    } /* End of CodeGenerator.main. */

} /* End of class CodeGenerator definition. */

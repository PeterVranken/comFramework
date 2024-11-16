/**
 * @file Log4j2Configurator.java
 * The configuration of the log4j 2 logger is done in this class.
 *
 * Copyright (C) 2015 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class Log4j2Configurator
 *   Log4j2Configurator
 *   defineArguments
 *   renderConfiguration
 */

package applicationInterface.loggerConfiguration;

import java.util.*;
import java.io.*;
import org.stringtemplate.v4.*;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.message.*;
import applicationInterface.cmdLineParser.CmdLineParser;
import excelExporter.main.ExcelExporter;


/**
 * Initialize the application logging with an appropriate configuration.
 *   This class is a bit special in use. It has a constructor but the actual operation is
 * done as side effect of the intended one and only call of the constructor. The new
 * object created by this call is of no interest and can be discarded immediately.\n
 *   The class has static members to prepare the command line evaluation as far as logging
 * is concerned. After defining the related command line arguments the command line can be
 * parsed (externally) and the parse result is passed to this class' constructor, which
 * generates a temporary object with the according settings. The temporary object is passed
 * to the render process. A temporary XML file with the command line demanded configuration
 * is written and log4j 2 is initialized with this configuration. The temporary object and
 * XML file become immediately obsolete.\n
 *   The log4j 2 initialization sequence is performed in the instance of the first logger
 * creation. Consequently, the mechanism implemented in the constructor of this class will
 * succeed only if there is no use of a log4j 2 logger prior to its call. The normal use
 * case is to call this class in the main function as part of the application
 * initialization. If so, then the typical code pattern of using log4j must not be applied
 * to the main class: It's common practice to have a final static logger object in each
 * class, which is initialized with the class' name. This must hence not be done in the
 * main class; loading of the main class requires initialization of its static objects and
 * the log4j class would be initialized before the program flow reaches the code
 * implemented here. The main class needs to use a none final static logger with initial
 * value null. Only after running the log4j initialization with this class it'll fetch a
 * logger object and assign it to its static logger variable.\n
 *   For the same reasons, there must be no static field in the application main class,
 * which is initialized to a value other than null if the field's class has a statically
 * initialized log4j logger. This would again mean to load the log4j class at load time of
 * the main class.<p>
 *   Remark: The use of StringTemplate V4 for generation of the XML code requires the
 * existence of a (temporary) object although the operation of this class actually is of
 * static nature. An alternative could be a private sub-class and an object of this class,
 * which is created in a static member of the visible class. This would avoid the unusual
 * call of the class constructor.
 */

public class Log4j2Configurator
{
    /** The logger instance for this class. It is only initialized after we have prepared
        the configuration. By default it is set to null. */
    private static Logger _logger = null;

    /** Boolean Flag: Use unbiased log4j2 configuration sequence. */
    private boolean useStdConfigSequence_ = true;

    /** The name of the logFile. Used from the StringTemplate V4 template when generating
        the XML configuration code. null means not to write a log file at all. This is the
        default behavior. */
    public String logFileName = null;

    /** The log level as a String. Used from the StringTemplate V4 template when generating
        the XML configuration code. */
    public String logLevel = null;

    /** The default log level in case of unspecified or wrong specified value. */
    private static final Level _logLevelDefault = Level.INFO;

    /** The log4j2 message pattern used for the file appender. Used from the StringTemplate
        V4 template when generating the XML configuration code. */
    public String logPattern = null;

    /** The default log pattern for the log file in case of unspecified or wrong specified
        value. */
    private static final String _logPatternDefault = "%d %-5p - %m%n";


    /**
     * Do the command line evaluation for the log level.<p>
     *   If an invalid log level is given as command line argument and if a logger object
     * is already available then a warning is issued.
     *   @return
     * Get the desired logging level.
     *   @param clp
     * The command line parser object, which is queried for the actual values of the
     * involved command line arguments.
     */
    public static Level getLogLevel(CmdLineParser clp)
    {
        final String logLevel = clp.getString("v");
        if(logLevel != null)
        {
            /* Validate the user input. */
            final Level level = Level.getLevel(logLevel.toUpperCase());
            if(level != null)
                return level;
            else
            {
                if(_logger != null)
                {
                    _logger.warn( "Log level {} is invalid. Log level {} is used instead."
                                , logLevel
                                , _logLevelDefault
                                );
                }
                return _logLevelDefault;
            }
         }
         else
         {
            /* Use default, the user didn't say anything about the log level. */
            return _logLevelDefault;
         }
    } /* End of getLogLevel */



    /**
     * The one and only temporarily required instance of Log4j2Configurator is created. The
     * creation process configures the logger as a kind of side effect and the created
     * object is actually no longer used once the constructor has successfully made it.
     *   @param clp
     * The command line parser object, which has already successfully parsed the
     * application command line. This implicitly means, that the other method {@link
     * #defineArguments} has been called before.
     */
    public Log4j2Configurator(CmdLineParser clp)
    {
        String configXml = null;
        useStdConfigSequence_ = clp.getBoolean("V");
        if(!useStdConfigSequence_)
        {
            logFileName = clp.getString("l");
            logPattern  = clp.getString("p");

            /* Validate the user input. */
            logLevel = getLogLevel(clp).toString();
            if(logPattern == null  ||  logPattern.length() == 0)
                logPattern = _logPatternDefault;

            /* Compile the XML configuration. */
            configXml = renderConfiguration();
            if(configXml == null)
                useStdConfigSequence_ = true;
        }
        File xmlFileDesignation = null;
        if(!useStdConfigSequence_)
        {
            PrintWriter writer = null;
            try
            {
                /* Create a temporary file for the configuration. */
                xmlFileDesignation = File.createTempFile( /* prefix */ "log4j2config_"
                                                        , /* suffix */ ".xml"
                                                        , /* directory */ null /* TMP */
                                                        );

                /* Reopen and use the new file for writing the configuration. */
                writer = new PrintWriter(xmlFileDesignation, "UTF-8");
                assert configXml != null;
                writer.print(configXml);

                /* Config file is successfully written. Direct log4j2 to use it. We need a
                   URL instead of the file name to do so. */
                Properties props = System.getProperties();
                props.setProperty( "log4j.configurationFile"
                                 , xmlFileDesignation.toURI().toURL().toString()
                                 );
            }
            catch(IOException e)
            {
                useStdConfigSequence_ = true;
                System.out.println( "Caught file I/O exception when writing config file. "
                                    + e.getMessage()
                                  );
            }
            if(writer != null)
                writer.close();
        }

        /* Now we get a logger instance for this class. This must be the first use of
           log4j2 in this Java application - only then it'll cause the initialization with
           the prepared settings. This condition can not be tested here. To meet this
           demand one will run this class early in the main class and will not initialize a
           static logger variable in that class. */
        assert _logger == null;
        _logger = LogManager.getLogger(Log4j2Configurator.class);

        if(useStdConfigSequence_)
        {
            if(clp.getBoolean("V"))
            {
                /* The standard configuration is wanted - just confirm it. The user should
                   know, what to do. */
                _logger.debug( "No user specified log settings are applied. The log4j 2"
                               + " standard configuration process is used"
                             );
            }
            else
            {
                /* The standard configuration is a fallback and there might be no
                   configuration. Give feedback. */
                System.err.println
                           ( "Previous errors in the log4j2 configuration cause the use"
                             + " of the log4j2 standard configuration sequence. See"
                             + " http://logging.apache.org/log4j/2.x/manual/configuration.html"
                             + " for details about properly configuring log4j 2."
                           );
            }
        }

        /* Configuration file is no longer required. */
        if(xmlFileDesignation != null)
        {
            if(xmlFileDesignation.delete())
            {
                _logger.debug( "Temporary configuration file {} successfully deleted"
                             , xmlFileDesignation
                             );
            }
            else
            {
                _logger.warn( "Error deleting temporary configuration file {}"
                            , xmlFileDesignation
                            );
            }
        }

        /* We call the method again, which evaluates the log level command line argument.
           Since we now have a valid logger object will it emit a visible warning in case
           of invalid user input. */
        getLogLevel(clp);

    } /* End of Log4j2Configurator.Log4j2Configurator. */



    /**
     * Define all command line arguments, which relate to the logger configuration.\n
     *   This static method needs to be called prior to the constructor of the one and only
     * temporarily required object.
     *   @param clp
     * The command line parser object.
     */
    static public void defineArguments(CmdLineParser clp)
    {
        clp.defineArgument( "V"
                          , "use-standard-log4j2-configuration"
                          , /* cntMax */ 1
                          , "Use the standard configuration sequence of log4j2. See"
                            + " http://logging.apache.org/log4j/2.x/manual/configuration.html"
                            + " for details."
                            + " If given then no programmatic configuration of"
                            + " logging is done and the other arguments log-level, log-file"
                            + " and log4j2-pattern are ignored"
                            + ".\nOptional. By default the programmatic configuration takes"
                            + " place"
                          );
        clp.defineArgument( "v"
                          , "log-level"
                          , /* cntMin, cntMax */ 0, 1
                          , /* defaultValue */ null
                          , "Verbosity of all logging. Specify one out of OFF,"
                            + " FATAL, ERROR, WARN, or INFO"
                            + ".\nOptional, default is INFO"
                          );
        clp.defineArgument( "l"
                          , "log-file"
                          , /* cntMin, cntMax */ 0, 1
                          , /* defaultValue */ null
                          , "If given, a log file is written containing general"
                            + " program flow messages."
                            + ".\nOptional. By default logging output only goes to the"
                            + " console"
                          );
        clp.defineArgument( "p"
                          , "log4j2-pattern"
                          , /* cntMin, cntMax */ 0, 1
                          , /* defaultValue */ null
                          , "A pattern for the log file entries may be specified,"
                            + " e.g. \"%d %C %p: %m%n\". See"
                            + " http://logging.apache.org/log4j/2.x/manual/layouts.html"
                                                                         + "#PatternLayout"
                            + " for details"
                            + ".\nOptional; the default will be most often sufficient"
                            + ".\nPlease note, the console output is not affected"
                          );
    } /* End of defineArguments */



    /**
     * Render the current configuration as a XML code.
     *   @return Get the XML configuration as String. In case of errors (due to bad
     * application installation) no XML configuration is available and the function returns
     * null.
     */
    private String renderConfiguration()
    {
        final String templateFileName = "applicationInterface/loggerConfiguration/"
                                        + "Log4j2Configurator_renderConfiguration.stg";
        STGroup stg = null;
        try
        {
            stg = new STGroupFile(templateFileName);
            stg.verbose = false;
            stg.registerRenderer(Number.class, new NumberRenderer());
            stg.registerRenderer(String.class, new StringRenderer());
            ST template = stg.getInstanceOf("log4j2Configuration");
            template.add("config", this);
            return template.render();
        }
        catch(RuntimeException e)
        {
            System.err.println( "Error in rendering of log4j2 configuration. "
                                + e.getMessage()
                              );
            return null;
        }
    } /* End of ParameterSet.toString() */


    /**
     * Get the Boolean flag if the unbiased log4j2 configuration sequence should be used
     * rather than programatic configuration via command line arguments.
     *   @return Getthe Boolean flag.
     */
    public boolean getUseStdConfigSequence()
        {return useStdConfigSequence_;}

    /**
     * Get the configured name of the logFile.
     *   @return Get the file name. null means not to write a log file at all.
     */
    public String getLogFileName()
        {return logFileName;}

    /**
     * Get the configured log level as a String.
     *   @return Get a string like WARN or INFO.
     */
    public String getLogLevel()
        {return logLevel;}

    /**
     * Get the log4j2 message pattern, which is used for the file appender.
     *   @return Get a string like %d %C %p: %m%n
     */
    public String getLogPattern()
        {return logPattern;}

} /* End of class Log4j2Configurator definition. */






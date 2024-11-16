/**
 * @file CmdLineParser.java
 * Command line parser for Java applications.
 *
 * Copyright (C) 2004, 2015-2022 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class CmdLineParser
 *   Argument.Argument (2 variants)
 *   Argument.init
 *   Argument.setPrefixLengths
 *   Argument.getName
 *   CmdLineParser
 *   setArgumentPrefixes
 *   addArgDefinition
 *   defineArgument (5 variants)
 *   parseArgs
 *   wrapText
 *   getUsageInfo
 *   getArgument
 *   setReadIndex
 *   getNoValues
 *   getBoolean
 *   getInteger
 *   getDouble
 *   getString
 *   iterator
 */

package applicationInterface.cmdLineParser;

import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;
import java.security.InvalidParameterException;


/**
 * This class provides a general mechanism to evaluate the program's command line
 * arguments.
 *   A CmdLineParser object is provided with the definitions of all allowed program
 * arguments, i.e. their names, their types and default values (see e.g. {@link
 * #defineArgument(String, String, int, int, String, String)}). Furthermore, a common
 * property of all arguments are the leading character sequences, distinguished for short
 * and long argument names (see {@link #setArgumentPrefixes}). By default single or double
 * dashes are used to indicate named arguments.<p>
 *   The value of an argument is taken from the next command line token. The convention
 * that values directly stick to the short switch or directly follow the long switch,
 * separated by an equal character, are not supported: It needs to be -a ValueOfA or
 * --asLongName ValueOfA; -aValueOfA and --asLongName=ValueOfA are not recognized.<p>
 *   Command line tokens, which are not preceded by a named switch are considered values
 * of the unnamed argument. Normally, they are the input file operands of an application.
 * The unnamed argument is defined with {@link #defineArgument(int, int, String, String)})
 * and besides this handled like named arguments.<p>
 *   The leading character sequence of long argument names, the double dash be default, can
 * be given once on the command without a name behind. This command line token is ignored
 * as an argument but changes the behavior of the command line parser: All further tokens
 * on the command line are taken as such as values of the unnamed argument.
 *   The definition of any argument contains the count, minimum and maximum. The maximum
 * can be infinite. A minimum count of null would be specified for optional arguments.<p>
 *   As the next step, the command line is parsed according to the finalized argument
 * specification phase ({@link #parseArgs}). The internal map is extended with the actual
 * values. An error is returned if the command line contains unknown arguments or too many
 * or to few occurrences of an argument.<p>
 *   After parsing, the actual values of any argument can be fetched with one of the get
 * methods, e.g. {@link #getString}. The get methods are type related. One must not use
 * another type then the one which was defined for that argument. The get methods can be
 * invoked repeatedly to fetch all actual values of an argument. This iteration is infinite
 * and cycles around the available values. The actual number can be queried with {@link
 * #getNoValues(String)}.<p>
 *   The cyclic iteration has been chosen to support a common use case: Most arguments will
 * be optional or mandatory but have a maximum count of one. For those arguments the parser
 * object can serve as (global) database of application settings after parsing. From any
 * location in the software one can invoke the get method the query the value of one of the
 * settings and all queries will yield the same value, be it the actual value from the
 * command line or the default value.<p>
 *   The method {@link #iterator} returns an Iterator along all argument values found on
 * the command line, this includes the values of the unnamed argument. This iteration is
 * not cyclic, one can walk once along all argument values. An application can call the
 * (appropriate) get method for all arguments returned by the iteration and will this way
 * process all values in the order of appearance on the command line. The use case are
 * applications where the order of arguments matters, arguments probably, which may have
 * several values or occurrences on the command line.
 */
public class CmdLineParser implements Iterable<String>
{
    /**
     * Non compile time errors from this module are reported with an exception of this
     * type.
     */
    public static class InvalidArgException extends Exception
    {
        /**
         * An exception is created.
         * @param strMsg
         *   A specific message can be set.
         */
        public InvalidArgException(String strMsg)
            { super(strMsg); }

    } /* End of class CmdLineParser.InvalidArgException definition. */



    /**
     * Internally used data structure to store the argument definitions and values.
     */
    private static class Argument
    {
        /** Enumeration of types of arguments. */
        public enum Type 
        {
            /** Boolean type */
            Boolean, 
            
            /** Integral numeric type */
            Integer, 
            
            /** Real numeric type */
            Double, 
            
            /** Text */
            String, 
            
            /** Other types */
            UnnamedArgument
        };
        
        /** The data type of the argument. A file argument actually means an undefined
            string. */
        private Type type_;
        
        /** The range of the appearance count (on the command line). 0 would mean optional. */
        private int cntMin_, cntMax_;
        
        /** The default value as a typeless object. */
        private Object defaultVal_;
        
        /** The values as a list of typeless objects. */
        private LinkedList<Object> valList_;
        
        /** The control of reading the (next) actual value of the argument. */
        private Iterator<Object> itVal_ = null;
        
        /** Argument names: The switches used on the command line which preceed the value
            of this argument. The common prefix like "-" or "--" are included. */
        private String shortName_, longName_;
        
        /** The user-specified explanation of this argument. */
        private String description_;

        /** The lengths of the switch name prefixes.
              @remark Caution, the inital value of theses members needs to be maintained in
            sync with the inital value of members shortArgPrefix_ and longArgPrefix_ of the
            surrounding class. */
        private static int _lengthOfShortArgumentPrefix = "-".length()
                         , _lengthOfLongArgumentPrefix = "--".length();
        
        /**
         * A new instance of CmdLineParser.Argument is created.
         *   @param type
         * One out of Argument.type.Integer, Argument.type.Double, Argument.type.Boolean,
         * Argument.type.String.
         *   @param cntMin
         * Specify how often this command line option needs to be present on the command
         * line at minimum. Specify 0 to say that it is optional.
         *   @param cntMax
         * Specify how often this command line option needs to be present on the command
         * line at maximum. Specify 0 or less to say arbitrarily often.
         *   @param defaultVal
         * The default value of matching type: Boolean, Integer, Double or String. A type
         * check is not made. A later exception in the client code is probale in case of a
         * mismatch.
         *   @param description
         * This string is used for compiling a usage message. See {@link
         * #getUsageInfo(String,boolean)}.
         */
        public Argument( Type type
                       , int cntMin
                       , int cntMax
                       , Object defaultVal
                       , String description
                       )
        {
            type_        = type;
            
            if(cntMin < 0  ||  (cntMax > 0  &&  cntMax < cntMin))
                throw new InvalidParameterException("Invalid count specification for argument");
            cntMin_      = cntMin;
            cntMax_      = cntMax;
            defaultVal_  = defaultVal;
            valList_     = new LinkedList<Object>();
            itVal_       = null;
            description_ = description;

            /* The names are added before the arguments are stored in the vector, see
               addArgDefinition. */
            shortName_ = null;
            longName_  = null;

        } /* End of Argument.Argument */

    
         
        /**
         * Name handling and stripping the prefixes requires knowing the lengths of the
         * prefixes.
         *   Call this function always when the prefixes are redefined.
         *   @param lengthOfShortArgumentPrefix
         * The length of the prefix for short arguments.
         *   @param lengthOfLongArgumentPrefix
         * The length of the prefix for long arguments.
         */
        public static void setPrefixLengths( int lengthOfShortArgumentPrefix
                                           , int lengthOfLongArgumentPrefix
                                           )
        {
            assert lengthOfShortArgumentPrefix > 0  &&  lengthOfLongArgumentPrefix > 0;
            _lengthOfShortArgumentPrefix = lengthOfShortArgumentPrefix;
            _lengthOfLongArgumentPrefix  = lengthOfLongArgumentPrefix;
            
        } /* End of Argument.setPrefixLengths */
        
        
        
        /**
         * Get a human friendly naming for this argument.
         *   Arguments have long and short names, its optional which or both to use. This
         * function returns the most meaningful name for the argument.
         *   @return Get the name without the leading switch indicator like - or --. The
         * empty string is returned for unnamed arguments.
         */
        public String getName()
        {
            String name;
            if(longName_ != null)
            {
                assert longName_.length() >= _lengthOfLongArgumentPrefix;
                name = longName_.substring(_lengthOfLongArgumentPrefix);
            }
            else
            {   
                assert shortName_ != null
                       &&  shortName_.length() > _lengthOfShortArgumentPrefix;
                name = shortName_.substring(_lengthOfShortArgumentPrefix);
            }                
            return name;
            
        } /* End of Argument.getName */

    } /* End of class CmdLineParser.Argument definition. */


    /** A definition used for proper formatting of the usage message. */
    static final String TEXT_WRAP_INDENT_STRING           = "          "
                      , TEXT_WRAP_INDENT_PARAGRAPH_STRING = "  ";

    /** A definition used for proper formatting of the usage message. */
    static final int TEXT_WRAP_COLUMN         = 70 // Must be larger then indent depth'.
                   , TEXT_TAB_CMD_DESCRIPTION = 0  // Use 0 to disable tabulator.
                   , TEXT_WRAP_INDENT_DEPTH   = TEXT_WRAP_INDENT_STRING.length()
                   , TEXT_WRAP_INDENT_PARAGRAPH_DEPTH =
                                                    TEXT_WRAP_INDENT_PARAGRAPH_STRING.length();

    /** The vector holds the descriptions of all defined and thus allowed, named command line
        arguments. After parsing it may also contain their actual values. */
    private Vector<Argument> vectorOfArgs_ = new Vector<>();

    /** The list of all arguments on the command line in the order of appearance. */
    private LinkedList<String> listOfArgsInCmdLine_ = new LinkedList<String>();
    
    /** The map associates the name of an argument with the argument specification object
        in the vector of those. */
    private HashMap<String, Argument> mapArgByName_ = new HashMap<>();

    /** A character that denotes arguments in their brief form. Most often a dash (the
        default) or a slash.
          @remark Caution, the initial value of this member needs to be maintained in
        sync with the initial value of member _lengthOfShortArgumentPrefix of the inner
        class Argument. */
    private String shortArgPrefix_ = "-";

    /** A character that denotes arguments in the long form. Most often a double dash
        (the default).
          @remark Caution, the initial value of this member needs to be maintained in
        sync with the initial value of member _lengthOfLongArgumentPrefix of the inner
        class Argument. */
    private String longArgPrefix_ = "--";

    /** A helper string that contains a part of the usage message. Compiled during the
        argument definition phase. */
    private String helpCmdLine_     = ""
                 , helpArgs_        = "";

    
    /**
     * Define the heading character(s) of each argument.
     *   By default each argument is recognized with a leading dash or a double dash
     * for long names.<p>
     *   However, these characters can be redefined. All command line arguments that do not
     * start with the agreed symbols and that are neither values of such arguments, are
     * called unnamed arguments. Most often, it'll be the names of files to be processed.
     * After parsing of the command line, the client of the parser can access all these
     * arguments as an array or via an iterator.<p>
     *   All named arguments can be addressed in short and long fashion, e.g. -h or --help.
     * The long variant of the argument's name is prefixed with a different symbol,
     * although it's not forbidden to use the same one.
     *   @param shortArgumentPrefix
     * The string that denotes the short form of named command line arguments. Should be a
     * one character string. Must not be null or the empty string.
     * Must not be the empty string.
     *   @param longArgumentPrefix
     * The string that denotes the long form of named command line arguments. May be the
     * same as <b>strShortArgumentPrefix</b>. Must not be null or the empty string.
     */
    public void setArgumentPrefixes( String shortArgumentPrefix
                                   , String longArgumentPrefix
                                   )
    {
        if(shortArgumentPrefix == null  ||  shortArgumentPrefix.length() == 0
           ||  longArgumentPrefix == null  ||  longArgumentPrefix.length() == 0
          )
        {
            throw new InvalidParameterException("An argument's name prefix must not be"
                                                + " null or empty"
                                               );
        }

        Argument.setPrefixLengths(shortArgumentPrefix.length(), longArgumentPrefix.length());
        shortArgPrefix_ = shortArgumentPrefix;
        longArgPrefix_  = longArgumentPrefix;

    } /* End of CmdLineParser.setArgumentPrefixes. */






    
    /**
     * Add a new argument definition. Subroutine of the defineArgument methods.
     *   @param shortName
     * The short name of the argument without the prefix character. Most often a string
     * like "h" or "v".
     *   @param longName
     * The long name of the argument without the prefix character. Most often a string like
     * "help" or "verbose".<p>
     *   Normally, one out of short and long name is optional and may be null. Only if the
     * unnamed arguments are defined then both names will be null.
     *   @param newArg
     * The argument definition object to be added under the given names.
     *   @throws InvalidParameterException
     * If an argument of the specified name already exists an InvalidParameterException is
     * thrown.
     */
    private void addArgDefinition(String shortName, String longName, Argument newArg)
    {
        /* If no name is given then we define the unnamed arguments (mostly the names of
           files to be processed. This is internally expressed by the empty name. */
        boolean isUnnamedArg = shortName == null  &&  longName == null;
        if(isUnnamedArg)
        {
            longName = "";
            shortName = null;
        }

        if(shortName != null)
        {
            /* Check: The same argument name must neither be used twice nor as a long and a
               short name. */
            if(mapArgByName_.containsKey(shortArgPrefix_ + shortName)
               ||  mapArgByName_.containsKey(longArgPrefix_ + shortName)
              )
            {
                /* This is a coding error in the class' client, throw a run time exception. */
                throw new InvalidParameterException("Argument " + shortName + " is"
                                                    + " already defined"
                                                   );
            }

            shortName = shortArgPrefix_ + shortName;
            newArg.shortName_ = shortName;
        }

        if(longName != null)
        {
            /* Check: The same argument name must neither be used twice nor as a long and a
               short name. */
            if(mapArgByName_.containsKey(shortArgPrefix_ + longName)
               || mapArgByName_.containsKey(longArgPrefix_ + longName)
              )
            {
                /* This is a coding error in the class' client, throw a run time exception. */
                throw new InvalidParameterException((isUnnamedArg
                                                     ? "Unnamed arguments are"
                                                     : "Argument " + longName + " is"
                                                    )
                                                    + " already defined"
                                                   );
            }

            longName = longArgPrefix_ + longName;
            newArg.longName_ = longName;
        }    

        /* Add a new description object to the vector and associate its index with the two
           names. */
        if(shortName != null)
            mapArgByName_.put(shortName, newArg);

        if(longName != null)
            mapArgByName_.put(longName, newArg);

        vectorOfArgs_.add(newArg);

        /* Update the help string. */
        helpCmdLine_ += ' ';

        if(newArg.cntMin_ == 0)
            helpCmdLine_ += "[";
        if(newArg.cntMax_ > 1  ||  newArg.cntMax_ <= 0)
            helpCmdLine_ += "{";

        StringBuffer sbCmdHelp = new StringBuffer();
        if(isUnnamedArg)
        {
            sbCmdHelp.append("Operand"
                             + (newArg.cntMax_>1 || newArg.cntMax_<=0? "s": "")
                            );
        }
        else
        {
            if(shortName != null  &&  longName != null)
            {
                helpCmdLine_ += "(" + shortName + "|" + longName + ")";
                sbCmdHelp.append(shortName + ", " + longName);
            }
            else if(shortName != null)
            {
                helpCmdLine_ += shortName;
                sbCmdHelp.append(shortName);
            }
            else
            {
                helpCmdLine_ += longName;
                sbCmdHelp.append(longName);
            }
        }        
        sbCmdHelp.append(':');
        do
        {
            sbCmdHelp.append(' ');
        }
        while(sbCmdHelp.length() < TEXT_TAB_CMD_DESCRIPTION);
        sbCmdHelp.append(newArg.description_);
        helpArgs_ += '\n' + wrapText(sbCmdHelp.toString(), /* doIndentFirstLine */ false);

        switch(newArg.type_)
        {
        case Integer:
            helpCmdLine_ += " <IntValue>";
            break;

        case Double:
            helpCmdLine_ += " <DoubleValue>";
            break;

        case String:
            helpCmdLine_ += " <StringValue>";
            break;

        case Boolean:
            /* Booleans have no argument. */
            break;
        
        case UnnamedArgument:
            helpCmdLine_ += "<Operand>";
            break;

        default: assert false;
        }

        if(newArg.cntMax_ > 1  ||  newArg.cntMax_ <= 0)
            helpCmdLine_ += "}";
        if(newArg.cntMin_ == 0)
            helpCmdLine_ += "]";

    } /* End of CmdLineParser.addArgDefinition */



    
    /**
     * The passed names must be valid; at least one needs to be none empty. Do a check.
     *   @throws InvalidParameterException
     * A failure is a coding error and reported by exception.
     *   @param shortName
     * The short name of the argument without the prefix character. Most often a string
     * like "h" or "v".
     *   @param longName
     * The long name of the argument without the prefix character. Most often a string like
     * "help" or "verbose".<p>
     *   One and only one out of short and long name is optional and may be null. Empty or
     * blank strings must not be used at all.
     */
    private void checkArgNames(String shortName, String longName)
    {
        if(shortName == null  &&  longName == null
           ||  shortName != null  &&  shortName.trim().length() == 0
           ||  longName != null  &&  longName.trim().length() == 0
          )
        {
            throw new InvalidParameterException
                        ("One out of argument's short or long name must not be null."
                         + " Both must not be an empty or blank string"
                        );
        }
    } /* End of CmdLineParser.checkArgNames */
                              
                              

    
    /**
     * Define an expected and allowed boolean argument before parsing.
     *   Only those arguments which have been defined using these methods will later be
     * accepted, when actual parsing is performed.<p>
     *   The default value of boolean arguments is always false (i.e. not present on the
     * command line). In this sense they are always optional and the minimum count is
     * implicitly 0.
     *   @param shortName
     * The short name of the argument without the prefix character. Most often a string
     * like "h" or "v".
     *   @param longName
     * The long name of the argument without the prefix character. Most often a string like
     * "help" or "verbose".<p>
     *   One out of short and long name is optional and may be null.
     *   @param cntMax
     * The maximum number of occurances of this argument on the command line.
     *   @param description
     * This descriptive string will be used when compiling a usage message. See {@link
     * #getUsageInfo}. The description should be verbose; it may be of any length as a
     * simple word wrap algorithm is applied that ensures a proper formatting. No other
     * formatting characters than newlines for paragraph formatting should be used in the
     * description text.
     *   @throws InvalidParameterException
     * If an argument of the specified name already exists an InvalidParameterException is
     * thrown.
     */
    public void defineArgument( String shortName
                              , String longName
                              , int cntMax
                              , String description
                              )
        throws InvalidParameterException
    {
        checkArgNames(shortName, longName);
        Argument newArg = new Argument( Argument.Type.Boolean
                                      , /* cntMin */ 0
                                      , cntMax
                                      , Boolean.valueOf(false)
                                      , description
                                      );
        addArgDefinition(shortName, longName, newArg);
        
    } /* End of CmdLineParser.defineArgument. */




    
    /**
     * Define an expected and allowed integer argument before parsing.
     *   Only those arguments which have been defined using these methods will later be
     * accepted, when actual parsing is performed.
     *   @param shortName
     * The short name of the argument without the prefix character. Most often a string
     * like "h" or "v".
     *   @param longName
     * The long name of the argument without the prefix character. Most often a string like
     * "help" or "verbose".<p>
     *   One out of short and long name is optional and may be null.
     *   @param cntMin
     * The minimum number of occurances of this argument on the command line. Pass 0 for
     * optional arguments.
     *   @param cntMax
     * The maximum number of occurances of this argument on the command line. A value less
     * or equal to 0 means infinite.
     *   @param defaultVal
     * The default value of type int. Ignored if <b>bOptional</b> is false.
     *   @param description
     * This descriptive string will be used when compiling a usage message. See {@link
     * #getUsageInfo}. The description should be verbose; it may be of any length as a
     * simple word wrap algorithm is applied that ensures a proper formatting. No other
     * formatting characters than newlines for paragraph formatting should be used in the
     * description text.
     *   @throws InvalidParameterException
     * If an argument of the specified name already exists an InvalidParameterException is
     * thrown.
     */
    public void defineArgument( String shortName
                              , String longName
                              , int cntMin
                              , int cntMax
                              , int defaultVal
                              , String description
                              )
        throws InvalidParameterException
    {
        checkArgNames(shortName, longName);
        Argument newArg = new Argument( Argument.Type.Integer
                                      , cntMin
                                      , cntMax
                                      , Integer.valueOf(defaultVal)
                                      , description
                                      );
        addArgDefinition(shortName, longName, newArg);

    } /* End of CmdLineParser.defineArgument. */




    
    /**
     * Define an expected and allowed floating point argument before parsing.
     *   Only those arguments which have been defined using these methods will later be
     * accepted, when actual parsing is performed.
     *   @param shortName
     * The short name of the argument without the prefix character. Most often a string
     * like "h" or "v".
     *   @param longName
     * The long name of the argument without the prefix character. Most often a string like
     * "help" or "verbose".<p>
     *   One out of short and long name is optional and may be null.
     *   @param cntMin
     * The minimum number of occurances of this argument on the command line. Pass 0 for
     * optional arguments.
     *   @param cntMax
     * The maximum number of occurances of this argument on the command line. A value less
     * or equal to 0 means infinite.
     *   @param defaultVal
     * The default value of type double.
     *   @param description
     * This descriptive string will be used when compiling a usage message. See {@link
     * #getUsageInfo}. The description should be verbose; it may be of any length as a
     * simple word wrap algorithm is applied that ensures a proper formatting. No other
     * formatting characters than newlines for paragraph formatting should be used in the
     * description text.
     *   @throws InvalidParameterException
     * If an argument of the specified name already exists an InvalidParameterException is
     * thrown.
     */
    public void defineArgument( String shortName
                              , String longName
                              , int cntMin
                              , int cntMax
                              , double defaultVal
                              , String description
                              )
        throws InvalidParameterException
    {
        checkArgNames(shortName, longName);
        Argument newArg = new Argument( Argument.Type.Double
                                      , cntMin
                                      , cntMax
                                      , Double.valueOf(defaultVal)
                                      , description
                                      );
        addArgDefinition(shortName, longName, newArg);

    } /* End of CmdLineParser.defineArgument. */




    
    /**
     * Define an expected and allowed string argument before parsing.
     *   Only those arguments which have been defined using these methods will later be
     * accepted, when actual parsing is performed.
     *   @param shortName
     * The short name of the argument without the prefix character. Most often a string
     * like "h" or "v".
     *   @param longName
     * The long name of the argument without the prefix character. Most often a string like
     * "help" or "verbose".<p>
     *   One out of short and long name is optional and may be null.
     *   @param cntMin
     * The minimum number of occurances of this argument on the command line. Pass 0 for
     * optional arguments.
     *   @param cntMax
     * The maximum number of occurances of this argument on the command line. A value less
     * or equal to 0 means infinite.
     *   @param defaultVal
     * The default value of type String.
     *   @param description
     * This descriptive string will be used when compiling a usage message. See {@link
     * #getUsageInfo}. The description should be verbose; it may be of any length as a
     * simple word wrap algorithm is applied that ensures a proper formatting. No other
     * formatting characters than newlines for paragraph formatting should be used in the
     * description text.
     *   @throws InvalidParameterException
     * If an argument of the specified name already exists an InvalidParameterException is
     * thrown.
     */
    public void defineArgument( String shortName
                              , String longName
                              , int cntMin
                              , int cntMax
                              , String defaultVal
                              , String description
                              )
        throws InvalidParameterException
    {
        checkArgNames(shortName, longName);
        Argument newArg = new Argument( Argument.Type.String
                                      , cntMin
                                      , cntMax
                                      , defaultVal
                                      , description
                                      );
        addArgDefinition(shortName, longName, newArg);

    } /* End of CmdLineParser.defineArgument. */





    
    /**
     * Define expected or allowed unnamed arguments.
     *   Unnamed arguments mostly have the meaning of input files to be processed. They are
     * not preceeded by a switch of anticipated and specified (short or long) name but they
     * must not start with the common prefix of the switches. (In order to become able to
     * recognize misspelled switches on the command line.) As an exception, one may pass
     * the switch prefix of the long argument form (usually the double dash) once on the
     * command line -- all remaining arguments will then be taken as unnamed arguments
     * regardless of their appearance.<p>
     *   Only those arguments which have been defined using the defineArgument methods will
     * later be accepted, when actual parsing is performed.
     *   @param cntMin
     * The minimum number of occurances of this argument on the command line. Pass 0 if
     * unnamed arguments are optional.
     *   @param cntMax
     * The maximum number of occurances of this argument on the command line. A value less
     * or equal to 0 means infinite.
     *   @param defaultVal
     * The default value of type String.
     *   @param description
     * This descriptive string will be used when compiling a usage message. See {@link
     * #getUsageInfo}. The description should be verbose; it may be of any length as a
     * simple word wrap algorithm is applied that ensures a proper formatting. No other
     * formatting characters than newlines for paragraph formatting should be used in the
     * description text.
     *   @throws InvalidParameterException
     * If an argument of the specified name already exists an InvalidParameterException is
     * thrown.
     */
    public void defineArgument( int cntMin
                              , int cntMax
                              , String defaultVal
                              , String description
                              )
        throws InvalidParameterException
    {
        Argument newArg = new Argument( Argument.Type.UnnamedArgument
                                      , cntMin
                                      , cntMax
                                      , defaultVal
                                      , description
                                      );
        addArgDefinition(/* shortName */ null, /* longName */ null, newArg);

    } /* End of CmdLineParser.defineArgument. */





    
    /**
     * Parse the program parameters.
     *   The string array passed to main is inspected by this method. All contained values
     * are compared with the allowed arguments defined in this object. The values are
     * figured out and stored in the map for later use with the get-methods.
     *   @param argAry
     * The command line arguments as passed to the main function.
     *   @throws InvalidArgException
     * If the method fails, the main program should be aborted printing the usage message.
     *   @see CmdLineParser#getBoolean
     *   @see CmdLineParser#getInteger
     *   @see CmdLineParser#getDouble
     *   @see CmdLineParser#getString
     *   @see CmdLineParser#iterator
     */
    public void parseArgs(String[] argAry)
        throws InvalidArgException
    {
        final int nStateArg         = 0;
        final int nStateUnnamedArgs = 1;

        int nStatus = nStateArg;
        boolean noNamedArgsAnyMore = false;
        int i = 0;
        while(i < argAry.length)
        {
            /* We adopt the GNU concept of pseudo argument --: if found the first time
               then consider all remaining arguments unnamed arguments regardless of
               their appearance. Particularly, they may beginn with a dash or double
               dash. */
            if(!noNamedArgsAnyMore 
               &&  argAry[i].compareTo(longArgPrefix_) == 0
              )
            {
                /* Indicate the state change. No argument parsing any more. */
                noNamedArgsAnyMore = true;
                nStatus = nStateUnnamedArgs;

                /* Ignore this argument which just separated parsed arguments from all
                   the rest. */
                ++ i;
                continue;
            }
            
            switch(nStatus)
            {
            case nStateArg:

                if(mapArgByName_.containsKey(argAry[i]))
                {
                    /* Get the argument specification from the map to see which type and
                       default value are defined. */
                    Argument arg = mapArgByName_.get(argAry[i]);

                    /* A value must not appear more often then allowed. */
                    if(arg.cntMax_ > 0  &&  arg.valList_.size() >= arg.cntMax_)
                    {
                        throw new InvalidArgException("Only " + arg.cntMax_ + " value"
                                                      + (arg.cntMax_>1? "s are": " is")
                                                      + " allowed for command line"
                                                      + " argument " + arg.getName()
                                                      + " but more have been found"
                                                     );
                    }

                    /* Is a value specified? Boolean arguments are handled differently. They
                       can't be set to false; if they are found it implicitly means a true. */
                    if(arg.type_ != Argument.Type.Boolean  &&  i+1 >= argAry.length)
                    {
                        throw new InvalidArgException
                                    ("Missing value for command line argument " + argAry[i]);
                    }

                    /* Access the value. */
                    Object val = null;
                    switch(arg.type_)
                    {
                    case Boolean:
                        val = Boolean.valueOf(true);
                        break;

                    case Integer:
                        try{ val = Integer.valueOf(argAry[i+1]); }
                        catch(NumberFormatException e)
                        {
                            InvalidArgException invalidArgException =
                                        new InvalidArgException
                                            ("Invalid value " + argAry[i+1]
                                             + " for integer type argument "
                                             + argAry[i] + " specified"
                                            );
                            invalidArgException.initCause(e);
                            throw invalidArgException;
                        }
                        ++ i;
                        break;

                    case Double:
                        try{ val = Double.valueOf(argAry[i+1]); }
                        catch(NumberFormatException e)
                        {
                            InvalidArgException invalidArgException =
                                        new InvalidArgException
                                            ("Invalid value " + argAry[i+1]
                                              + " for floating point argument "
                                              + argAry[i] + " specified"
                                            );
                            invalidArgException.initCause(e);
                            throw invalidArgException;
                        }
                        ++ i;
                        break;

                    case String:
                        val = argAry[++i];
                        break;
                    
                    default:
                        /* It must not be an unamed argument as this is associated with the
                           empty string that can't be found as command line argument. */
                        assert false;

                    } /* End switch(Which type of argument?) */
                    
                    /* Add the new value to the list of those. */
                    arg.valList_.addLast(val);

                    /* Store the argument in the history list. The reference is made by
                       name, which is most useful for the client of this class; most call
                       functions use the name to address to certain arguments. */
                    listOfArgsInCmdLine_.addLast(arg.getName());

                    /* Ready, next argument. */
                    ++ i;
                }
                else
                {
                    /* Regard it as an unnamed argument. */
                    nStatus = nStateUnnamedArgs;
                }
                break;

            case nStateUnnamedArgs:

                /* Add the string to the unnamed arguments. It are any strings that are
                   found somewhere in the command line. Typically an application
                   expects some files to be processed. */

                /* Get the argument specification from the map to see which type and
                   default value are defined. Unnamed arguments use the empty string as
                   pseudo name. */
                String nameOfUnamedArgSpecInMap = longArgPrefix_ + "";
                Argument arg = mapArgByName_.get(nameOfUnamedArgSpecInMap);

                /* If in the parsing phase an unnamed argument begins with one of the
                   switch indicators then this is regarded an error (but not an unnamed
                   argument). It's highly probable the we just see a typo on the command
                   line.
                     Last condition term: If no unnamed arguments are specified then
                   it's an error in general. */
                if(!noNamedArgsAnyMore
                   &&  (argAry[i].startsWith(shortArgPrefix_)
                        ||  argAry[i].startsWith(longArgPrefix_)
                       )
                   ||  arg == null
                  )
                {
                    throw new InvalidArgException("Unknown command line argument "
                                                  + argAry[i]
                                                 );
                }

                /* An unnamed argument must not appear more often then allowed. */
                if(arg.cntMax_ > 0  &&  arg.valList_.size() >= arg.cntMax_)
                {
                    throw new InvalidArgException("Only " + arg.cntMax_ 
                                                  + " unnamed command line argument"
                                                  + (arg.cntMax_>1? "s are": " is")
                                                  + " allowed but more have been found"
                                                 );
                }

                /* For unnamed arguments the argument is the value itself. */
                arg.valList_.addLast(argAry[i]);

                /* Store the argument in the history list. The reference is made by
                   name, which is most useful for the client of this class; most call
                   functions use the name to address to certain arguments. */
                listOfArgsInCmdLine_.addLast(arg.getName());
                
                /* Normally assume a switch as next argument by default. */
                if(!noNamedArgsAnyMore)
                    nStatus = nStateArg;

                /* Next argument. */
                ++ i;
                
                break;

            } /* End switch(Which operation to perform?) */

        } /* End while(All command line arguments.) */

        /* Check, if all mandatory arguments were given. */
        StringBuffer sbNames = new StringBuffer();
        int noMissingArguments = 0;
        for(Argument arg: vectorOfArgs_)
        {
            if(arg.valList_.size() < arg.cntMin_)
            {
                String name = arg.longName_;
                if(name == null)
                    name = arg.shortName_;
                assert name != null;
                
                if(noMissingArguments > 0)
                    sbNames.append(",");
                sbNames.append(" ");
                sbNames.append(name);
                
                ++ noMissingArguments;
            }
        }
            
        if(noMissingArguments > 0)
        {
            throw new InvalidArgException("Missing mandatory command line argument"
                                          + (noMissingArguments > 1? "s:": "") + sbNames
                                         );
        }
    } /* End of CmdLineParser.parseArgs. */





    
    /**
     * Reformat a text, using a simple line break algorithm, that controls the line length.
     *   @return
     * Get the reformatted text as String.
     *   @param strText
     * The text to reformat. Must not end with a line feed and must not be empty.
     *   @param doIndentFirstLine
     * The wrapped lines are indented. If doIndentFirstLine is false the first line
     * exclusively isn't.
     */
    private String wrapText(String strText, boolean doIndentFirstLine)
    {
        int l = 0;
        StringBuffer sbResult = new StringBuffer();

        if(doIndentFirstLine)
        {
            sbResult.append(TEXT_WRAP_INDENT_STRING);
            l = TEXT_WRAP_INDENT_DEPTH;
        }

        final char LF = '\n';
        boolean atLStart = true;
        for(int i=0; i<strText.length(); ++i)
        {
            char c = strText.charAt(i);

            /* Suppress white space at the beginning of a new line but allow empty lines. */
            if(atLStart  &&  c != LF)
            {
                if(c > ' ')
                {
                   sbResult.append(c);
                   ++ l;
                   atLStart = false;
                }
            }

            /* Break when necessary. */
            else if(l > TEXT_WRAP_COLUMN  &&  c <= ' ')
            {
                /* White space at the end of the line may already be appended and is
                   tolerated. */
                sbResult.append(LF);
                sbResult.append(TEXT_WRAP_INDENT_STRING);
                l = TEXT_WRAP_INDENT_DEPTH;
                if(c == LF)
                {
                    sbResult.append(TEXT_WRAP_INDENT_PARAGRAPH_STRING);
                    l += TEXT_WRAP_INDENT_PARAGRAPH_DEPTH;
                }
                atLStart = true;
            }

            /* Preserve line breaks in the original text. */
            else if(c == LF)
            {
                sbResult.append(LF);
                sbResult.append(TEXT_WRAP_INDENT_STRING);
                l = TEXT_WRAP_INDENT_DEPTH;
                sbResult.append(TEXT_WRAP_INDENT_PARAGRAPH_STRING);
                l += TEXT_WRAP_INDENT_PARAGRAPH_DEPTH;
                atLStart = true;
            }

            /* Nomal case, just copy the character. */
            else
            {
                sbResult.append(c);
                ++ l;
                atLStart = false;
            }
        }

        return sbResult.toString();

    } /* End of CmdLineParser.wrapText. */





    
    /**
     * Get a string listing all defined arguments.
     *   Use this method to get a brief help message in case {@link #parseArgs} fails.
     * Print this message after printing the error message which was part of the caught
     * exception.
     *   @return
     * The help message is returned.
     *   @param applicationName
     * To complete the help string, the method needs to know the name of the application
     * that uses CmdLineParser.
     *   @param argumentsTabularOnly
     * Usually, this will be set to {@code true} and the usage string shows the list of
     * possible arguments in a kind of BNF. However, this piece of text is made in a
     * generic way, only most basic characteristics of arguments like optional or mandatory
     * can be reflected.<p>
     *   The BNF promises more than it can keep: An argument could for example be available
     * only, if another one is given. In contrast to this, the generic rendering of the
     * information handles each argument independent of all others. Furthermore, the list
     * can be become such long, that it is anyway no longer use- or meaningful.<p>
     *   Whenever you consider the generated information either incomplete or missleading
     * or meaningless then you can pass {@code true} to suppress the generation. Now, only
     * an ellipsis is placed into the output if there's at least a single argument. The
     * rest will be described by the still available tabular description of all arguments.
     * In the table, the description of dependencies or arguments is under full control of
     * the client of this module.
     */
    public String getUsageInfo(String applicationName, boolean argumentsTabularOnly)
    {
        StringBuffer sb = new StringBuffer();

        /* Present the command line syntax. */
        sb.append("usage: ");
        sb.append(applicationName);
        
        if(argumentsTabularOnly)
        {
            /* Don't describe the arguments as BNF. Add an ellipsis if we leave out a
               portion of text. */
            if(!helpCmdLine_.isEmpty())
                sb.append(" ... (Arguments see below)");
        }
        else
        {
            /* Append the BNF description of the arguments. This can lead to very long
               lines and we do a wrapping. */
            sb.append(helpCmdLine_);
            sb = new StringBuffer(wrapText(sb.toString(), /* doIndentFirstLine */ false));
        }
        
        /* The descriptions of the distinct arguments are already wrapped. */
        sb.append(helpArgs_);

        sb.append("\n");
        return sb.toString();

    } /* End of CmdLineParser.getUsageInfo. */





    
    /**
     * Get the reference to an argument specification by name.
     *   @return
     * The argument is returned.
     *   @param name
     * The short or long name of the argument without the prefix.
     *   @throws InvalidParameterException
     * The exception is thrown if <b>name</b> is not the name ofd any named argument. A
     * runtime exception is chosen because the class' client defines and thus knows all
     * arguments. It's a coding error in the client.
     */
    private Argument getArgument(String name)
    {
        String possibleArgName = shortArgPrefix_ + name;

        if(!mapArgByName_.containsKey(possibleArgName))
            possibleArgName = longArgPrefix_ + name;

        if(!mapArgByName_.containsKey(possibleArgName))
        {
            /* This is a coding error in the class' client, throw a run time exception. */
            throw new InvalidParameterException("Undefined argument " + name + " used");
        }

        return mapArgByName_.get(possibleArgName);

    } /* End of CmdLineParser.getArgument. */




    
    /**
     * Get the number next of values of a given argument.
     *   Use this function prior to calling {@link #setReadIndex}.
     *   @return Get the number of vaues parsed in the command line.
     *   @param argumentName
     * The short or long name of the argument without the prefix. Pass the empty string
     * (not null) to get the number of unnamed arguments.
     *   @throws InvalidParameterException
     * The exception is thrown if <b>name</b> is not the name of any named argument. A
     * runtime exception is chosen because the class' client defines and thus knows all
     * arguments. It's a coding error in the client.
     */
    public int getNoValues(String argumentName)
    {
        Argument arg = getArgument(argumentName);
        return arg.valList_.size();

    } /* End of CmdLineParser.getNoValues */




    
    /**
     * Set the index of the next value to be read.
     *   Normally, all values of an argument are read in the order of appearance on the
     * command lone. This function can manipulate the iteration. The read position can be
     * changed to any valid index. The future invokations of the argument related get-value
     * function are affected.
     *   @param argumentName
     * The short or long name of the argument without the prefix. Pass the empty string
     * (not null) to manipulate the iteration of the unnamed arguments.
     *   @param idxVal
     * The read position as null based index over all found values of the given argument.
     * The passed index needs ti be in its bounds; call {@link #getNoValues(String)} to get
     * the number of accessible values.
     *   @throws InvalidParameterException
     * The exception is thrown if <b>name</b> is not the name of any named argument or if
     * the index is out of bounds. A runtime exception is chosen because the class' client
     * defines and thus knows all arguments. It's a coding error in the client.
     */
    public void setReadIndex(String argumentName, int idxVal)
    {
        Argument arg = getArgument(argumentName);
        if(idxVal < 0  ||  idxVal >= arg.valList_.size())
        {
            /* This is a coding error in the class' client, throw a run time exception. */
            throw new InvalidParameterException("Read index out of bounds. Argument "
                                                + argumentName + " only has "
                                                + arg.valList_.size() + " values"
                                               );
        }

        /* Get a fresh iterator and advance it to the desired position. */
        arg.itVal_ = arg.valList_.iterator();
        for(int i=0; i<idxVal; ++i)
            arg.itVal_.next();

    } /* End of CmdLineParser.setReadIndex */




    
    /**
     * Retrieve a boolean argument value.
     *   After parsing, the actual value of a boolean command line argument can be
     * retrieved. The client can't see whether the value has been explicitely set or
     * whether it is the default value.<p>
     *   For arguments with a multiplicity of more than one this function behaves iterative
     * and cyclic. Each invokation will yield the value of the next appearance of the
     * argument on the command line. If all value were queried the iteration starts over
     * with the value of the first appearance.
     *   @return The boolean value is returned.
     *   @param argumentName
     * The name of the command line argument. An exception is thrown if the argument
     * doesn't exist or if it is not of type Argument.type.Boolean.
     *   @see CmdLineParser#defineArgument(String, String, int, String)
     */
    public boolean getBoolean(String argumentName)
    {
        Argument arg = getArgument(argumentName);
        if(arg.type_ != Argument.Type.Boolean)
        {
            /* This is a coding error in the class' client, throw a run time exception. */
            throw new InvalidParameterException("Argument " + argumentName
                                                + " is not of type boolean"
                                               );
        }
        
        /** @todo Redundant code: Boolean can only have default value false and parsed,
            found value true. It should be sufficient to return false if the list is empty
            and true otherwise. TBC. */
        if(arg.valList_.size() == 0)
        {
            assert !((Boolean)arg.defaultVal_).booleanValue();
            return ((Boolean)arg.defaultVal_).booleanValue();
        }
        else
        {
            if(arg.itVal_ == null  ||  !arg.itVal_.hasNext())
               arg.itVal_ = arg.valList_.iterator();
            assert arg.itVal_.hasNext();
            Boolean argVal = ((Boolean)arg.itVal_.next()).booleanValue();
            assert argVal;
            return argVal;
        }
    } /* End of CmdLineParser.getBoolean. */





    
    /**
     * Retrieve a integer argument value.
     *   After parsing, the actual value of a integer command line argument get be
     * retrieved. The client can't see whether the value has been explicitely set or
     * whether it is the default value.
     *   @return The integer value is returned.
     *   @param argumentName
     * The name of the command line argument. An exception is thrown if the argument
     * doesn't exist or if it is not of type Argument.type.Integer.
     *   @see CmdLineParser#defineArgument(String, String, int, int, int, String)
     */
    public int getInteger(String argumentName)
    {
        Argument arg = getArgument(argumentName);
        if(arg.type_ != Argument.Type.Integer)
        {
            /* This is a coding error in the class' client, throw a run time exception. */
            throw new InvalidParameterException("Argument " + argumentName
                                                + " is not of type integer"
                                               );
        }

        if(arg.valList_.size() == 0)
            return ((Integer)arg.defaultVal_).intValue();
        else
        {
            if(arg.itVal_ == null  ||  !arg.itVal_.hasNext())
               arg.itVal_ = arg.valList_.iterator();
            assert arg.itVal_.hasNext();
            return ((Integer)arg.itVal_.next()).intValue();
        }
    } /* End of CmdLineParser.getInteger. */





    
    /**
     * Retrieve a double argument value.
     *   After parsing, the actual value of a floating point command line argument get be
     * retrieved. The client can't see whether the value has been explicitely set or
     * whether it is the default value.
     * <pre>
     *    CmdLineParser cmdLineParser = new CmdLineParser();
     *    cmdLineParser.defineArgument("x", "eXtension", 1, 1, 1.234, "a float value");
     *    cmdLineParser.parseArgs(args); // String[] args as passed to main.
     *    double x = cmdLineParser.getDouble("x")
     *         , alsoX = cmdLineParser.getDouble("eXtension");
     * </pre>
     *   @return The double value is returned.
     *   @param argumentName
     * The name of the command line argument. An exception is thrown if the argument
     * doesn't exist or if it is not of type Argument.type.Double.
     *   @see CmdLineParser#defineArgument(String, String, int, int, double, String)
     */
    public double getDouble(String argumentName)
    {
        Argument arg = getArgument(argumentName);
        if(arg.type_ != Argument.Type.Double)
        {
            /* This is a coding error in the class' client, throw a run time exception. */
            throw new InvalidParameterException("Argument " + argumentName
                                                + " is not of type floating point"
                                               );
        }

        if(arg.valList_.size() == 0)
            return ((Double)arg.defaultVal_).doubleValue();
        else
        {
            if(arg.itVal_ == null  ||  !arg.itVal_.hasNext())
               arg.itVal_ = arg.valList_.iterator();
            assert arg.itVal_.hasNext();
            return ((Double)arg.itVal_.next()).doubleValue();
        }
    } /* End of CmdLineParser.getDouble. */





    
    /**
     * Retrieve a string argument value; this includes the value of unnamed arguments.
     *   After parsing, the actual value of a string command line argument get be
     * retrieved. The client can't see whether the value has been explicitely set or
     * whether it is the default value.
     *   @return The string value is returned.
     *   @param argumentName
     * The name of the command line argument. For unnamed arguments the empty String (not
     * null) is passed. An exception is thrown if the argument doesn't exist or if it is
     * neither of type Argument.type.String nor Argument.Type.UnnamedArgument.
     *   @see CmdLineParser#defineArgument(String, String, int, int, String, String)
     *   @see CmdLineParser#defineArgument(int, int, String, String)
     */
    public String getString(String argumentName)
    {
        Argument arg = getArgument(argumentName);
        if(arg.type_ != Argument.Type.String  &&  arg.type_ != Argument.Type.UnnamedArgument)
        {
            /* This is a coding error in the class' client, throw a run time exception. */
            throw new InvalidParameterException("Argument " + argumentName
                                                + " is not of type string"
                                               );
        }

        if(arg.valList_.size() == 0)
            return (String)arg.defaultVal_;
        else
        {
            if(arg.itVal_ == null  ||  !arg.itVal_.hasNext())
               arg.itVal_ = arg.valList_.iterator();
            assert arg.itVal_.hasNext();
            return (String)arg.itVal_.next();
        }
    } /* End of CmdLineParser.getString. */




    
    /**
     * Get an iterator over all named and unnamed arguments as they are found in the
     * command line.
     *   All arguments that were found in the command line are stored in a LinkedList
     * object. Get an iterator along this list. The iteration is particularly useful in
     * applications, where the order of arguments matters, i.e. if the meaning of switches
     * depend on the presence or absence of predecessors. The iteration permits to retrieve
     * all argument values in the order of appearance on the command line.<p>
     *   If <b>it</b> is the returned iterator then <b>it.next()</b> delivers a String
     * object with the next argument's name. Then use the appropriate get-value method to
     * access the value of this argument.<p>
     *   For unnamed arguments the empty string is returned. This string is accepted by
     * the {@link #getString} method.
     *   @return
     * The iterator pointing to the beginning of the list is returned.
     */
    public Iterator<String> iterator()
    {
        return listOfArgsInCmdLine_.iterator();

    } /* End of CmdLineParser.iterator. */

} /* End of class CmdLineParser definition. */

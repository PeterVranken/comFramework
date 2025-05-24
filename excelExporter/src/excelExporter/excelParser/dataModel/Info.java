/**
 * @file Info.java
 * This file implements a data container for general information needed during code
 * generation: Date and time, the names of involved files, etc.
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
/* Interface of class Info
 *   setApplicationInfo
 *   setTemplateInfo
 *   setOutputInfo
 *   setUserOptions
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import java.text.*;
//import org.stringtemplate.v4.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.ErrorCounter;


/**
 * The data structure holding general information for code generation.<p>
 *   The basic design principle is the use of public members because such members can be
 * directly accessed from the output templates.
 */
public class Info implements IST4CmdListener</* TContext */ Integer, /* TCmdResult */ Object>
{
    /** The global logger object for all progress and error reporting. */
    private static Logger _logger = LogManager.getLogger(Info.class);

    /** The error counter, which counts the template emitted errors and warnings. */
    private ErrorCounter errCnt_ = null;

    /** The name of the code generating application. */
    public static String application = "CodeGenerator";
    
    /** The first three parts of the version of the tool, which relate to the functional
        state of the application. */
    public static String version = "1.0.0";
    
    /** Major part of version designation of the application. This part relates to major
        releases. */
    public static int versionMajor = 1;
    
    /** Minor part of version designation of the application. This part relates to
        functional changes; a change of the data model will always cause an increment of
        this part. */
    public static int versionMinor = 0;
    
    /** Third part of version designation of the application. This part relates to fixes
        or insignificant functional changes. */
    public static int versionFix = 0;
    
    /** Forth part of version designation of the application. This part is updated with
        every change of the any of the files of the application - regardless whether it is
        a functional change, a documentation change or a change of the samples. */
    public static int versionBuild = 0;
    
    /** The version of the data model supported by the code generator. This version is
        synchronized with the major parts of the tool version when but only when the data
        model is changed. Any change of the data model will lead to a change of at least
        one of the tool's two major version parts. A template should make a check of the
        data model version and create an error message if the data model version it is
        designed for differs from the one passed into the template by the tool.<p>
          To make conditional code for error generation easier this version designation is
        an integer number. The number is composed as M*1000+m if M.m.f used to be the
        version designation of the tool when the data model was changed the last time.<p>
          Please refer to <b>isVersionDataModel</b> for testing the data model version from
        a template. */
    public static int versionDataModel = 1004;
    
    /** The version designation <b>versionDataModel</b> for the data model, which is
        essential for relating templates to permitted, compatible tool revisions, is
        testable through this data element. It is a map with a single key, value pair. The
        value is Boolean true, the key is the String "v"+versionDataModel, e.g., v2012 for
        data model revision 2.12.<p>
          A template can use an expressing like
        <pre>
          &lt;if(!isVersionDataModel.v2012)&gt;#error Wrong tool revision, need data model 2.12&lt;endif&gt;
        </pre>
        to validate that it is running on a revision of the code generator tool, which
        supports the data model the template was designed for. (Any none matching key
        evaluates to a Boolean false.) */
    public static Map<String,Boolean> isVersionDataModel = null;

    /** Time of code generation. */
    public final String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                                   .format(Calendar.getInstance().getTime());

    /** Year of date (as applicable for copyright notices). */
    public final String year = new SimpleDateFormat("yyyy")
                                   .format(Calendar.getInstance().getTime());
    
    /** The StringTemplate V4 group template file as an extended Java File object. This
        object extends the Java class File by adding some information about parts of the
        file name (like its extension). Please refer to class FileExt.<p>
          The information from the Java class File is partly accessible: All methods
        starting with get or is can be used by placing the method name without the syllable
        get/is into the template, e.g., {@code <templateFile.name>} would yield the return
        value of {@code File.getName()}.<p>
          Please refer to the online help of Java class File for details. */
    public FileExt templateFile = null;
    
    /** The name of the StringTemplate V4 template used to render the information. */
    public String templateName = null;
    
    /** The StringTemplate V4 template, which is used to render the information has an
        argument, that contains all network related information. This is the name of this
        argument. */
    public String templateArgNameCluster = null;
    
    /** The StringTemplate V4 template, which is used to render the information has an
        argument, that contains general purpose information (e.g., data, time, file names).
        This is the name of this argument.<p>
          The object passed in into the template under this argument name is an instance of
        this class codeGenerator.dataModelListener.Info. */
    public String templateArgNameInfo = null;
    
    /** Information rendering can optionally apply line wrapping. If so then
        templateWrapCol is an Integer with the wrap column. Normally it is null and no
        wrapping takes place. */
    public Integer templateWrapCol = null;

    /** The generated output file as an extended Java File object. This object extends the
        Java class File by adding some information about parts of the file name (like its
        extension). Please refer to class FileExt.<p>
          The information from the Java class File is partly accessible: All methods
        starting with get or is can be used by placing the method name without the syllable
        get/is into the template, e.g., {@code <output.name>} would yield the return value of
        File.getName().<p>
          Please refer to the online help of Java class File for details. */
    public FileExt output = null;
    
    /** The value of the environment variable USERNAME. */
    public static final String envVarUSERNAME = System.getenv("USERNAME");
    
    /** The value of the environment variable HOME. */
    public static final String envVarHOME = System.getenv("HOME");
    
    /** The value of the environment variable TMP. */
    public static final String envVarTMP = System.getenv("TMP");
    
    /** The value of the environment variable OS. */
    public static final String envVarOS = System.getenv("OS");

    /** This is a Java Map object, which can hold custom number objects. The normal
        operation of a number is to start at 0 and to count up each time it is referenced.
        These counters can be used to generate null based linear indexes in the generated
        code. However, special operations permit to use more general arithmetic and binary
        operations on the numbers. Furthermore, they can be compared against boundary
        values.<p>
          Please note, unlike the other fields of {@link Info} does {@code calc} not bring
        any information into the template expansion process. It's an empty scratch pad for
        some simple arithmetics on numeric value (which need to come from other elements of
        the data model). The main use case it the support of linear counters (numeric
        enumerations) in the generated code and the possibility to have conditional code on
        numeric data elements like integer message attributes.<p>
          All numbers are 64 Bit integers, all operations are 64 Bit integer operations.<p>
          The reading of a number is got by simply querying it by name in the map. All of
        the more complex operations are submitted by using pseudo map element names, which
        encode number name, operation and operand at the same time. Reading the number and
        doing the comparisons are the only operations, which affect the template expansion:
        They return a numeric or Boolean object to engine and template. All other operations
        only have their side-effect on the addressed number in the map but return null to
        the template engine - no text will be generated from those.<p>
          The pseudo element names have the pattern {@code
        <numberName>[_<operation>[_<operand>]]}. The operand either is a literal number or
        the name of an other number, which must already exist in the map.<p>
          Literal numbers are either signed decimal numbers, which may have the suffix n or
        positive hexadecimal numbers with prefix 0x or one of the Boolean constants true and
        false. The suffix n means a negation of the decimal number literal. Examples are
        12, -123, 123n or 0xffff.<p>
          The negation suffix n has been added as an alternative to the common dash for
        sake of simple template writing. Using this syntax pattern enables using the
        simpler StringTemplate V4 map access operator {@code map.key}, where {@code map}
        and {@code key} need to be identifiers. This holds for literals. If the operand is
        a signed integer attribute from the data model one will anyway have to use the more
        complex access operator {@code map.(<sub-template>)} and here the usual rendering
        of negative values with the preceding dash would be applied. (Examples below.)<p>
          The default for the optional operand depends on the operation. Most typical use
        cases are supported like linear counters and comparison with null.<p>
          The destination number of the operation should already exist in the map. If it
        doesn't it is created by side effect. The initial value depends on the operation.
        This is only possible for a few operations like set, sadd, ssub and smul and
        referencing a number for read. Except for set, linear or logarithmic counters are
        assumed.<p>
          The operations are set, get, add, sub, mul, div, sadd, ssub, smul, and, or, xor,
        not, sr, asr, sl, isGE, isLE, isG, isL, isE and isNE.<p>
          set is the assignment. Only the value is assigned; if the operand is another
        number than its possible sticky operation (see below) is not inherited.<p>
          get means reading a number. It is a unary operator and must not have an operand.
        The difference to normal reading by just stating the number's name is that a sticky
        post operation is not executed after reading. This is particular useful for
        debugging of the template code; values can be printed without altering the behavior
        of the code generation.<p>
          add, sub, mul, div are the basic numeric operations.<p>
          sadd, ssub, smul do the same as add, sub, mul but they are sticky, meaning that
        the operation is repeated every time after reading the number. This stickiness is
        how the counters are implemented. The sticky operation is cleared by applying any
        of the non sticky operations with the exception of set. Keeping the sticky
        operation for operation set further supports the use of counters as main purpose of
        the scratch pad: A counter can be adjusted to a new value but keeps counting as
        before.<p>
          and, or, xor, not, sr, asr, sl are the binary operations, the latter three mean
        shifting. Note that not is a unary operator and must not have an operand.<p>
          isGE, isLE, isG, isL, isE, isNE are the comparisons {@code >=, <=, >, <, ==, !=},
        respectively. They yield a Boolean result, which is returned to the template engine
        and which can be used in a StringTemplate V4 conditional expression. Please note,
        the Boolean result is not assigned to the destination number; all of these
        operations do not alter the map contents, they just affect the code generation.<p>
          The default operand of all comparisons is null. You can check if number x is
        negative by a simple {@code <info.calc.x_isL>}.<p>
          Examples:<p>
          {@code <info.calc.x>}: Read number x, i.e., return the numeric value to the
        template and expand it there into text. Create it as linear null based counter, if
        this should be the first reference to x. (x gets the initial value 0 and the sticky
        operation +1.)<p>
          {@code <info.calc.x_mul_23n>}: Multiply x with -23.<p>
          {@code <info.calc.y_set_0xff>}: Create y and set it to 255. No sticky operation
        is defined for y.<p>
          {@code <info.calc.y_ssub_x>}: Assign the difference y-x to y. Next reading of y
        will yield this value but subtract the same value again as internal post operation;
        subtracting x's value is the sticky operation. Note, due to the post-processing y's
        internal value always differs from the one got from reading y. Note, the sticky
        operation subtracts x's value as it was at the evaluation time of this command, not
        x's current value meanwhile.<p>
          {@code <info.calc.({noFrames_set_<cluster.noFrames>})>}: A typical way to get
        "real" numeric information into the initially empty scratch pad {@link #calc} is
        the use of the StringTemplate V4 map operator {@code .()}, which builds the key to
        the wanted value by template expansion prior to querying the map.<p>
          {@code <if(info.calc.noFrames_isG_0)>uint16 noFrames =
        <info.calc.noFrames>;<else><info.error.("No frames are defined!")><endif>}: Use of
        comparison operation to generate conditional output from a template.<p>
          Since Boolean results are not stored back into the map just like that can Boolean
        expressions be computed only with the work around of using conditional template
        code. If we express the Boolean true as -1 and false as 0 then the available binary
        operators behave like Boolean operators; this is the meaning of the Boolean literals
        true and false:<p>
          {@code <if(info.calc.x_isL_0)><info.calc.xBad_set_false><else><info.calc.xBad_set_true><endif><\\>}<p>
          {@code <if(info.calc.x_isG_100)><info.calc.xTooLarge_set_true><else><info.calc.xTooLarge_set_false><endif><\\>}<p>
          {@code <info.calc.xBad_or_xTooLarge><\\>}<p>
          {@code <if(info.calc.xBad_isNE)>#error x (<info.calc.x>) is out of range [0; 100]<endif>} */
    public Map<String,Object> calc = null;

    /** The number of user stored numbers in the scratch pad {@link #calc}. From a
        StringTemplate V4 template this member is accessed as {@code
        <info.noCalcNumbers>}.
          @return Get the number of stored numbers. */
    public int getNoCalcNumbers()
        { assert calc != null; return calc.size(); }

    /** A map of user specified options or template attributes. User options are template
        attributes, which are simply passed through from the application's command line to
        the StringTemplate V4 template and can there be used to control the code
        generation. The use case are optional constructs in the generated code, which are
        controlled from the command line.<p>
          User options/attributes can be given in the context of an in- or output file.
        This map holds all options given in the context of the currently generated output
        file. (Refer to {@link ExcelWorkbook#optionMap} for the input related options.)<p>
          The name of an option or template attribute is the key into the map and the
        attribute's value is the value of the map entry. The value object's Java type is
        one out of String, Boolean, Integer or Double, depending on which fits best to the
        original command line argument; a command line argument <i>cmdLineArg</i> with
        value {@code true} would obviously be passed as Boolean into the template and would
        permit conditional code by using a construct like: {@code
        <if(info.optionMap.cmdLineArg)>Command line argument is TRUE!<endif>}<p>
          A sorted map is used for the implementation. The map retains the order of
        appearance of options on the command line. A map iteration in the StringTemplate V4
        template will yield this order. Example:<p>
          {@code <info.optionMap:{name|<name>=<info.optionMap.(name)><\n>}>} */
     public LinkedHashMap<String,Object> optionMap = null;
    
    /** The number of user options in {@link #optionMap}. From a StringTemplate V4
        template this member is accessed as {@code <info.noOptions>}.
          @return Get the number of options. */
    public int getNoOptions()
        { return optionMap != null? optionMap.size(): 0; }
    
    
    /** This is a pseudo field of the StringTemplate V4 data model. If it is used from a
        template then it expands to nothing. It operates only by side-effect on its
        argument: The argument is treated as error message during code generation. It is
        printed to the code generator's application log and the code generation fails. It
        should be used if the conditional template code detects some unwanted state, like
        an empty list of frames for example:<p>
          {@code <if(!bus.frameAry)><info.error.("There are no frames defined!")><endif>} */
    public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object> error;
    
    /** This is a pseudo field of the StringTemplate V4 data model. If it is used from a
        template then it expands to nothing. It operates only by side-effect on its
        argument: The argument is treated as a warning during code generation. It is
        printed to the code generator's application log but the code generation continues.
        It should be used if the conditional template code detects some unwanted state,
        which can be tolerated. The example reports if there's only a single frame
        defined:<p>
          {@code <if(!rest(bus.frameAry))><info.warn.("There's only one frame defined!")><endif>} */
    public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object> warn;
    
    /** This is a pseudo field of the StringTemplate V4 data model. If it is used from a
        template then it expands to nothing. It operates only by side-effect on its
        argument: The argument is treated as informative output during code generation.
        It is printed to the code generator's application log and the code generation
        continues. It can be used to report some status information:<p>
          {@code <info.info.(["There are ", length(bus.frameAry), " frames defined"])>} */
    public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object> info;

    /** This is a pseudo field of the StringTemplate V4 data model. If it is used from a
        template then it expands to nothing. It operates only by side-effect on its
        argument: The argument is treated as informative output during code generation.
        It is printed to the code generator's application log and the code generation
        continues. It can be used to report verbose progress information:<p>
          {@code <bus.frameAry:{f|<info.debug.({Process frame <f.name>})>}>} */
    public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object> debug;


    /** This class bundles the command interpreters, which implement the string operations.
        The class has been shaped only to structure the template expressions for string
        operations; in an ST4 template, we can write, e.g.,
          {@code <info.src.cmpRegExp("abccbca == .+c+.+$")>} */
    public class StringSupport
    {
        /** This pseudo field of the StringTemplate V4 data model allows redefining
            the argument delimiter, which is used by all the string comparison
            operations.<p>
              The ST4 template expressions of all the provided string operations get only
            a single argument from the template. (It uses the map syntax, e.g., {@code
            <info.str.cmp.(...)>}, where ... stands for the argument of the string
            operation.) However, string comparison requires two arguments and replacement
            operations would even require more. The concept is splitting the only argument
            provided by the template in parts and taking the parts as separate arguments of
            the aimed operation. If an operation requires n operands than the first n-1
            occurances of the delimiter split the test input into the n arguments. After
            splitting, all surrounding white space is removed from the parts, the result
            are eventually the arguments.<p>
              The delimiter is a regular expression. The default is "==|#", which means that
            either a double = or a single # can be used to split the input text into
            arguments of the operation.<p>
              Example. The argument delimiter is changed into the hyphen:<p>
              {@code <info.str.setArgumentDelimiter.("-")>}<p>
              {@code isCRC = <info.str.cmp.([signal.name,"- CRC"])>;} */
        public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object> 
                                                                        setArgumentDelimiter;

        /** This pseudo field of the StringTemplate V4 data model provides ordinary
            string comparison to the template expansion process.<p>
              The operation takes an argument string, which is split into two arguments.
            (See setArgumentDelimiter for more details about retrieving arguments.) The two
            arguments are compared for string equality. The result is a Boolean
            StringTemplate attribute. The expression can be used directly - it'll expand to
            either "true" or "false" - and it can be used as argument of conditional
            expressions. See example:<p>
              {@code 
                <if(info.str.cmp.({<signal.name> == CRC}))>
                  Signal name is "CRC"
                <else>
                  Signal is not the checksum!
                <endif>
              } */
        public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object> cmp;

        /** This pseudo field of the StringTemplate V4 data model provides ordinary,
            case insensitive string comparison to the template expansion process.<p>
              Please refer to other attribute cmp, which behaves exactly identical except for
            the different comparison; cmpI is case insensitive.<p>
              {@code 
                <if(info.str.cmpI.({<signal.name> == crc}))>
                  Signal name is "CRC", "crc" or "Crc", etc.
                <else>
                  Signal is not the checksum!
                <endif>
              } */
        public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object> cmpI;

        /** This pseudo field of the StringTemplate V4 data model provides regular
            expression based string comparison to the template expansion process.<p>
              Please refer to other attribute cmp, which behaves exactly identical except
            for the different comparison of the arguments; cmpRegExp is regular expression
            based.<p>
              The first argument is the string and the second argument is the regular
            expression. The regular expression is compiled using Java class
            java.util.regex.Pattern and a description of the expression syntax can be found
            at the
            <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html">Oracle Javadoc pages</a>.
            Syntax errors in the regular expression are reported during template expansion
            and will inhibit code generation.<p>
              The operation returns "string matches to regular expression" as a Booelan
            StringTemplate attribute, which can be used as argument of a conditional
            template expression:<p>
              {@code 
                <if(info.str.cmpRegExp.({<signal.name> == ^.*CRC.*$}))>
                  Signal name contains "CRC".
                <else>
                  Signal is not the checksum!
                <endif>
              } */
        public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                                                                    cmpRegExp;

        /** This pseudo field of the StringTemplate V4 data model provides regular
            expression based, case insensitive string comparison to the template expansion
            process.<p>
              Please refer to other attribute cmpRegExp, which behaves exactly identical
            except for the different comparison; cmpRegExpI is case insensitive:<p>
              {@code 
                <if(info.str.cmpRegExpI.({<signal.name> == ^.*CRC.*$}))>
                  Signal name contains "CRC", "crc" or "Crc", etc.
                <else>
                  Signal is not the checksum!
                <endif>
              } */
        public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object> 
                                                                                    cmpRegExpI;

        /** This pseudo field of the StringTemplate V4 data model provides regular
            expression based string replacement to the template expansion process.<p>
              The operation takes an argument string, which is split into three arguments:
            Input string, regular expression and replacement string.<p>
              The input string is matched against the regular expression. All matches are
            then substituted by the replacement string. The normal Java regular expression
            replacement is applied, i.e., the replacement string may use $&lt;n&gt; to refer to
            the n-th capture group in the regular expression.<p>
              This pseudo-field expands to the resulting string. If the resulting string
            would be empty, then null is returned; this enables if-clauses, which can check
            if, e.g., a capture group captures something or if it remains empty. (The
            resulting template expansion text is not affected; it's the same for null and
            the empty string.)<p>
              {@code 
                <if(info.str.cmpRegExpI.({<signal.name> == ^(.*)CRC$}))>
                  Raw signal name of checksum signal is <\\>
                  <info.str.replRegExp.({<signal.name>#(.*?)_?CRC#$1})>;
                <else>
                  Signal is not the checksum!
                <endif>
              }<p>
              CAUTION: Argument splitting is handled a bit different to the string
            comparison functions and different to what is documented for
            setArgumentDelimiter about retrieving arguments. No white space is removed
            after splitting the arguments. This has been decided in order to enable easy
            processing of blanks:<p>
              {@code 
                const char *url = <info.str.replRegExp.({<someURL()># #%20})>;
                // A URL must not contain blanks!
              } */
        public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                                                                    replRegExp;
        /** This pseudo field of the StringTemplate V4 data model provides case insensitive
            regular expression based string replacement to the template expansion
            process.<p>
              The operation takes an argument string, which is split into three arguments:
            Input string, regular expression and replacement string.<p>
              The input string is matched case insensitively against the regular
            expression. All matches are then substituted by the replacement string. The
            normal Java regular expression replacement is applied, i.e., the replacement
            string may use $&lt;n&gt; to refer to the n-th capture group in the regular
            expression.<p>
              This pseudo-field expands to the resulting string. If the resulting string
            would be empty, then null is returned; this enables if-clauses, which can check
            if, e.g., a capture group captures something or if it remains empty. (The
            resulting template expansion text is not affected; it's the same for null and
            the empty string.)
            <pre>
                {@literal <if(info.str.cmpRegExpI.({<signal.name> == ^(.*)Crc$}))>}
                  Raw signal name of checksum signal is {@literal <\\>}
                  {@literal <info.str.replRegExpI.({<signal.name>#_?CRC#})>;}
                {@literal <else>}
                  Signal is not the checksum!
                {@literal <endif>}
            </pre>
              CAUTION: Argument splitting is handled a bit different to the string
            comparison functions and different to what is documented for
            setArgumentDelimiter about retrieving arguments. No white space is removed
            after splitting the arguments. This has been decided in order to enable easy
            processing of blanks:<p>
              {@code 
                const char *url = <info.str.replRegExpI.({<someURL()># #%20})>;
                // A URL must not contain blanks!
              } */
        public final ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                                                                   replRegExpI;

        /**
         * Create the StringSupport object.
         *   @param errCnt String operation caused errors are counted in this object.
         */
        public StringSupport(ErrorCounter errCnt)
        {
            assert errCnt != null: "Don't pass null as error counter";

            /* Create the command interpreter for string comparison. We have an instance per
               operation mode, e.g., normal vs. regular expression. */
            String logContext = "<Info.str.setArgumentDelimiter>: ";
            setArgumentDelimiter =
                new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                  ( /* context */ StrCmdListener.operationModeSetDelimiter
                                  , new StrCmdListener(errCnt, logContext)
                                  , errCnt
                                  , logContext
                                  );
            logContext = "<Info.str.cmp>: ";
            cmp = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                  ( /* context */ StrCmdListener.operationModeSimple
                                  , new StrCmdListener(errCnt, logContext)
                                  , errCnt
                                  , logContext
                                  );
            logContext = "<Info.str.cmpI>: ";
            cmpI = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                  ( /* context */ StrCmdListener.operationModeSimpleIgnCase
                                  , new StrCmdListener(errCnt, logContext)
                                  , errCnt
                                  , logContext
                                  );
            logContext = "<Info.str.cmpRegExp>: ";
            cmpRegExp = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                  ( /* context */ StrCmdListener.operationModeRegExp
                                  , new StrCmdListener(errCnt, logContext)
                                  , errCnt
                                  , logContext
                                  );
            logContext = "<Info.str.cmpRegExpI>: ";
            cmpRegExpI = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                  ( /* context */ StrCmdListener.operationModeRegExpIgnCase
                                  , new StrCmdListener(errCnt, logContext)
                                  , errCnt
                                  , logContext
                                  );
            logContext = "<Info.str.replRegExp>: ";
            replRegExp = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                                  ( /* context */ StrCmdListener.operationModeRegExpRepl
                                  , new StrCmdListener(errCnt, logContext)
                                  , errCnt
                                  , logContext
                                  );
            logContext = "<Info.str.replRegExpI>: ";
            replRegExpI = new ST4CmdInterpreter</*TContext*/ Integer, /*TCmdResult*/ Object>
                                  ( /* context */ StrCmdListener.operationModeRegExpReplIgnCase
                                  , new StrCmdListener(errCnt, logContext)
                                  , errCnt
                                  , logContext
                                  );
        } /* End of StringSupport */

    } /* End class Info.StringSupport */

    /** This is a container for string support operations, mostly comparison of strings in
        several modes. Click at data type Info.StringSupport for a detailed description of
        the available string operations. */
    public StringSupport str;

    /**
     * Create the Info object.
     *   @param errCnt Template emitted and caused errors are counted in this object.
     */
    public Info(ErrorCounter errCnt)
    {
        assert errCnt != null: "Don't pass null as error counter";
        errCnt_ = errCnt;
        calc = new NumberMap(errCnt, /* logContext */ "<info.calc>: ");
        error = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                            ( /* context */ Integer.valueOf(Level.ERROR.intLevel())
                            , /* cmdListener */ this
                            , errCnt_
                            , /* debugLogContext */ "<Info.error>: "
                            );
        warn = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                            ( /* context */ Integer.valueOf(Level.WARN.intLevel())
                            , /* cmdListener */ this
                            , errCnt_
                            , /* debugLogContext */ "<Info.warn>: "
                            );
        info = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                            ( /* context */ Integer.valueOf(Level.INFO.intLevel())
                            , /* cmdListener */ this
                            , errCnt_
                            , /* debugLogContext */ "<Info.info>: "
                            );
        debug = new ST4CmdInterpreter</* TContext */ Integer, /* TCmdResult */ Object>
                              ( /* context */ Integer.valueOf(Level.DEBUG.intLevel())
                              , /* cmdListener */ this
                              , errCnt_
                              , /* debugLogContext */ "<Info.debug>: "
                              );

        /* Create the command interpreter for string comparison. We have an instance per
           operation mode, e.g., normal vs. regular expression. */
        str = new StringSupport(errCnt);

    } /* End of Info */



    /**
     * Get the string representation of the Info object; it's composed from some of some of
     * its general fields.
     *   @return The string value
     */
    @Override public String toString()
    {
        return application + ", version " + version + ", " + time;

    } /* End of Info.toString */


    /**
     * Set the information about this application.
     *   @param appName The application name.
     *   @param versionAry The four parts of the version designation of the application
     *   @param verDataModel The version of the data model for the templates.
     */
    public static void setApplicationInfo( String appName
                                         , int[] versionAry
                                         , int verDataModel
                                         )
    {
        application = appName;
        versionMajor = versionAry[0];
        versionMinor = versionAry[1];
        versionFix = versionAry[2];
        versionBuild = versionAry[3];
        version = "" + versionAry[0] + "." + versionAry[1] + "." + versionAry[2];
        versionDataModel = verDataModel;

        /* A map is applied to make the version test available as a <if()>  condition in
           the template. */
        isVersionDataModel = new HashMap<>(1);
        isVersionDataModel.put("v"+versionDataModel, Boolean.valueOf(true));

    } /* End of setApplicationInfo */


    /**
     * Fill the object with information about the template in use.
     *   @param tFileName The name of the template file.
     *   @param tName The name of the template.
     *   @param tArgCluster The name of the template argument with network information.
     *   @param tArgInfo The name of the template argument with general information.
     *   @param tWrapCol The wrap column or a value {@code <=} 0 if no wrapping takes place.
     */
    public void setTemplateInfo( String tFileName
                               , String tName
                               , String tArgCluster
                               , String tArgInfo
                               , int tWrapCol
                               )
    {
        templateFile = new FileExt(tFileName);
        templateName = tName;
        templateArgNameCluster = tArgCluster;
        templateArgNameInfo = tArgInfo;

        if(tWrapCol > 0)
            templateWrapCol = Integer.valueOf(tWrapCol);
        else
            templateWrapCol = null;

    } /* End of setTemplateInfo */



    /**
     * Fill the information concerning the generated output file.
     *   @param fileName The name of the generated output file.
     */
    public void setOutputInfo(String fileName)
    {
        output = new FileExt(fileName);

    } /* End of setOutputFileInfo */


    /**
     * Copy a map with user attributes by reference into the Info object.
     *   @param map
     * The map with pairs of attribute names (key) and attribute values.
     */
    public void setUserOptions(LinkedHashMap<String,Object> map)
    {
        optionMap = map;

    } /* End of setUserOptions */


    /**
     * This method implements the command listener, which is used to emit a message to the
     * application log under control of the currently expanded StringTemplate V4 template.
     *   @return The function will always return null: The template engine must not
     * generate any ouput because of the log command.
     *   @param logLevel
     * The listener is connected to different command interpreters. The interpreters relate
     * to the different logging severity levels and this information is passed in as
     * context. The context is the integer value of the static Log4j Level objects, e.g.
     * {@link org.apache.logging.log4j.Level}.
     *   @param message
     * The command string, which is interpreted as message to be logged.
     */
    public Object interpret(Integer logLevel, String message)
    {
        final int level = logLevel.intValue();
        
        if(level == Level.FATAL.intLevel())
        {
            errCnt_.error();
            _logger.fatal(message);
        }
        else if(level == Level.ERROR.intLevel())
        {
            errCnt_.error();
            _logger.error(message);
        }
        else if(level == Level.WARN.intLevel())
        {
            errCnt_.warning();
            _logger.warn(message);
        }
        else if(level == Level.INFO.intLevel())
            _logger.info(message);
        else if(level == Level.DEBUG.intLevel())
            _logger.debug(message);
        else
        {
            errCnt_.error();
            assert false: "Invalid log4j severity level used";
        }

        /* Writing to the application log should never produce any output in the template
           expansion. */
        return null;

    } /* End of IST4CmdListener.interpret */

} /* End class Info */
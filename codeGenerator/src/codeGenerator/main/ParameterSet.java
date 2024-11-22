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
 *   BusDescription.isInIntSet
 *   BusDescription.isFrameSupported
 *   defineArguments
 *   parseGetNextArg
 *   parseIntRange
 *   parseStateUserOption
 *   parseStateSpecialSignal
 *   cloneLinkedHashMap
 *   parseCmdLine
 *   toString
 */

package codeGenerator.main;

import java.util.*;
import org.apache.log4j.*;
import org.stringtemplate.v4.*;
import cmdLineParser.CmdLineParser;

/**
 * The application parameter set.<p>
 *   <b>Remark:</b> The naming convention of indicating local and static members with a
 * trailing or leading underscore is not applied as this class is subject to rendering with
 * StringTemplate. For the same reasons most members are held public.
 */

public class ParameterSet
{
    /** The global logger object for all progress and error reporting. */
    private static Logger _logger = Logger.getLogger(ParameterSet.class.getName());

    /** The name of the network cluster. */
    public String clusterName = null;

    /** The name of the network node the code is generated for. */
    public String nodeName = null;

    /** Use verbose mode for template loading. */
    public boolean stringTemplateVerbose = false;

    /** Specific signals can be made directly accessible by name. The user selects these
        signals by name. The use case is the support of alive or sequence counters or
        checksum validation in the generated code. An object of this class holds all
        user-specified information required to identify one special signal. */
    public static class SpecialSignalRequest
    {
        /** The name of the special signal in the data model. The special signal is stored
            in a map, which is owned by the PDU. This string is the key to look up the
            special signal.<p>
              The use case for this name is the harmonization of inhomogeneous network
            databases. Regardless how the signal is actually named in a PDU, it can always
            be addressed to from within the template by this fixed and known identifier. */
        public String name = null;

        /** A special signal is identified by name. Each signal of a PDU, whose name
            matches entirely against this regular expression is considered the requested
            special signal. (An error is raised if the number of matching signals in any
            PDU is other than 0 or 1.) The reason for applying a regular expression is the
            inhomogeneous naming found in real existing network databases. If e.g. the
            checksum is always stored in a signal "CRC" then the regular could degenerate
            to this name. In less homogeneous cases it might e.g. be useful to specify
            ".*(crc|checksum).*" */
        public String reSpecialSignal = null;

        /**
         * Simple constructor.
         * @param signalName The name of the special signal.
         * @param regExp The regular expression to filter the special signal. Or null if a
         * simple string match should be done.
         */
        public SpecialSignalRequest( String signalName
                                   , String regExp
                                   )
        {
            name = signalName;
            reSpecialSignal = regExp;
        }
    } /* End of class SpecialSignalRequest */



    /** The input of the code generation process is described in terms of buses that make
        up the cluster. */
    public static class BusDescription
    {
        /** The (short) name of the bus. Used inside the templates to disambiguate symbols,
            i.e. permit having same frame, PDU and signal names in different buses. */
        public String name = null;

        /** The name of the network database file specifying the nodes, frames and signals
            on this bus. */
        public String networkFileName = null;

        /** The direction of transmission of a frame is derived from the name of the
            network node the code is generated for. This setting is made bus specific as
            network database files from different sources might use different names for one
            and the same node. Should normally be the same for all buses and identical to
            nodeName on cluster level. */
        public String me = null;

        /** The transmission direction of frames (sent, received, none of these or both) is
            encoded in the network database. However, on command line demand can this
            information be put inverted into the data model in order to support code
            generation for residual bus simulation. Actually sent frames are reported to
            the StringTemplate V4 templates as being received and vice versa.<p>
              This field reflects the value of the command line argument
            invert-transmission-direction. */
        public boolean invertTransmissionDirection = false;

        /** The explicitely included frames by ID. Each array element is a range of IDs; a
            pair of from and to, both including. The array is null if no frames should be
            included by ID. */
        public ArrayList<Pair<Integer,Integer>> inclFrameIdAry = new ArrayList<>();

        /** Explicitly included frames by name. All frame matching against the regular
            expression are included.<p>
              The set of included frames is the union of these frames and those having an
            ID in <b>inclFrameIdAry</b>. If the union should be empty then all frames form
            the set of included frames. */
        public String reInclFrameName = null;

        /** The explicitely excluded frame IDs. Each array element is a range of IDs; a
            pair of from and to, both including.<p>
              The array is null if no frames should be excluded by ID. If ex- and included
            IDs are listed then the exclude condition overrules the include condition. */
        public ArrayList<Pair<Integer,Integer>> exclFrameIdAry = new ArrayList<>();

        /** Explicitely excluded frames by name.<p>
              The union of the set of matching frames and the set of frames with IDs in
            <b>exclFrameIdAry</b> are excluded. If a frame is in both the set of included
            and the set of excluded frames then it is excluded. */
        public String reExclFrameName = null;

        /** A list of filters for those special signals, the generated code should have
            access to. Can be used to implement checksum or alive counter handling in the
            code templates.<p>
              Signals of a frame, which unambiguously match one of the given regular
            expressions will be added to the rendered data model in a map of signal
            descriptors under the given name. */
        public LinkedHashMap<String,SpecialSignalRequest> specialSignalMap = new LinkedHashMap<>();

        /** A map of user specified template attributes, which appear as code generation
            options in the application's user interface. These attributes of Java type
            String, Boolean, Integer or Double are simply passed through from the
            application's command line to the StringTemplate V4 template and can there be
            used to control the code generation. The use case are optional constructs in
            the generated code, which are controlled from the command line.<p>
              The name of an option/attribute is the key into the map and the
            option/attribute's value is the value of the map entry. */
        public LinkedHashMap<String,Object> optionMap = new LinkedHashMap<>();


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

        } /* End of ParameterSet.BusDescription.isInIntSet */



        /**
         * Test if a given frame is in the set of supported frames.
         *   @return Get the Boolean result.
         *   @param name The name of the frame.
         *   @param id The frame ID.
         *   @todo The frame ID doesn't distinguish between 11 and 29 Bit value although
         * both kinds of IDs share the number range 0..0x7ff. It'll be impossible to
         * include such a number as standard CAN ID and exclude the same number as
         * extended CAN ID at the same time - or vice versa.
         */
        public boolean isFrameSupported(String name, int id)
        {
            /* A frame is supported if it is not in the excluded set but either in the included
               set or if the included set is not specified. Begin with exclusion. */
            if(reExclFrameName != null  &&  name.matches(reExclFrameName)
               || isInIntSet(exclFrameIdAry, id)
              )
            {
                return false;
            }

            if(reInclFrameName == null  &&  inclFrameIdAry.size() == 0)
                return true;

            return reInclFrameName != null  &&  name.matches(reInclFrameName)
                   || isInIntSet(inclFrameIdAry, id);

        } /* End of ParameterSet.BusDescription.isFrameSupported */

    } /* End of class ParameterSet.BusDescription */

    /** One bus description for each input network database file. */
    public ArrayList<BusDescription> busDescriptionAry = new ArrayList<>();

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
            option/attribute's value is the value of the map entry. */
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
                          , "The name of the complete network cluster. Optional, may be"
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
//                            + ". Optional, default is"
//                            + " "
//                            + ". This parameter is Mandatory"
//                          );
        clp.defineArgument( "me", "node-name"
                          , /* cntMin, cntMax */ 1, -1
                          , /* defaultValue */ null
                          , "The name of the network node code is generated for. The name"
                            + " needs to be passed in in the global context, this is the name"
                            + " principally used in the generated code.\n"
                            + "  The name can also be passed in in the local context of a"
                            + " bus definition. There it is"
                            + " optional and used only in case the network database uses a"
                            + " deviating name for the same node"
                          );
        clp.defineArgument( "vt", "string-template-verbose"
                          , /* cntMax */ 1
                          , "The template engine integrated for output generation has a debug"
                            + " mode to report details of searching addressed files"
                            + " and templates. Use this Boolean argument"
                            + " to enable the verbose mode of StringTemplate."
                            + " See http://www.stringtemplate.org/ for more. Must be given in"
                            + " the global context only. Optional, default is false"
                          );

        /* Arguments to specify a bus. */
        clp.defineArgument( "b", "bus-name"
                          , /* cntMin, cntMax */ 1, -1
                          , /* defaultValue */ null
                          , "The name of a bus of the cluster. This argument opens the"
                            + " local context of a new"
                            + " bus specification. It can be used repeatedly to define several"
                            + " buses. This parameter is mandatory, at least one bus must be"
                            + " defined in the cluster"
                          );
        clp.defineArgument( "dbc", "network-file-name"
                          , /* cntMin, cntMax */ 1, -1
                          , /* defaultValue */ null
                          , "The name of the network database file, which specifies the"
                            + " currently defined bus. Must be given in the context of a"
                            + " bus definition only. This parameter is mandatory for each"
                            + " bus specification"
                          );
        clp.defineArgument( "inv", "invert-transmission-direction"
                          , /* cntMax */ -1
                          , "The data model entries related to the transmission direction"
                            + " are all inverted if this Boolean argument is set. Use case is"
                            + " code generation for residual bus simulation. The name"
                            + " of the device under test is specified with the other"
                            + " command line argument node-name. Frames, which are received"
                            + " by that node will be put in the data model with field isSent"
                            + " set to true and frames, which are sent by that node will"
                            + " have the flag"
                            + " isReceived set. Code generated from the data model will"
                            + " expose appropriate transmission behavior for the test"
                            + " device, which is intended to talk to that node."
                            + "\n  This argument must be given in the bus context only."
                            + " Optional, default is false"
                          );
        clp.defineArgument( "id", "include-frame-id"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "A frame ID or a range (a colon separated pair) of those. The"
                            + " frames with the given ID or with"
                            + " an ID in the given range (both boundaries are including) are"
                            + " added to the set of included frames. All frames"
                            + " not belonging to this set will be ignored during processing"
                            + " of the network database file.\n"
                            + "  Note, this option doesn't discriminate between 11 Bit"
                            + " standard and 29 Bit extended IDs. It'll not be possible to"
                            + " selectively include either standard or extended IDs in the"
                            + " number range 0..2047.\n"
                            + "  If no include condition is given (neither by this argument"
                            + " nor by re-include-frame-name) then all frames in the input"
                            + " file form the set of included frames.\n"
                            + "  This argument can be used any number of times in the"
                            + " context of a bus specification. It must not be used in the"
                            + " global context; there's no common default for all buses or"
                            + " network database files"
                          );
        clp.defineArgument( "fr", "re-include-frame-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "Add frames by name to the set of included frames. All frames"
                            + " whose names match with this regular expression are elements"
                            + " of the set. See argument include-frame-id for more details.\n"
                            + "  Optional. This argument can be used once in the context of"
                            + " a bus specification. It can be set once in the global"
                            + " context, too, then it becomes the default value for all bus"
                            + " specifications that do not set the value themselves"
                          );
        clp.defineArgument( "ex", "exclude-frame-id"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "A frame ID or a range (a colon separated pair) of those. The"
                            + " frames with the given ID or with an ID in the given range"
                            + " (both boundaries are including) are added to the set of"
                            + " excluded frames.\n"
                            + "  Note, this option doesn't discriminate between 11 Bit"
                            + " standard and 29 Bit extended IDs. It'll not be possible to"
                            + " selectively exclude either standard or extended IDs in the"
                            + " number range 0..2047.\n"
                            + "  All frames from the set of included frames,"
                            + " which do not belong to the set of excluded frames,"
                            + " form the set of supported frames."
                            + " Only the supported frames will be considered during"
                            + " processing of the network database file.\n"
                            + "  This argument can be used any number of times in the"
                            + " context of a bus specification. It must not be used in the"
                            + " global context; there's no common default for all buses or"
                            + " network database files"
                          );
        clp.defineArgument( "exfr", "re-exclude-frame-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "Add frames by name to the set of excluded frames. All frames"
                            + " whose names match with this regular expression are elements"
                            + " of the set. See argument exclude-frame-id for more details.\n"
                            + "  Optional. This argument can be used once in the context of"
                            + " a bus specification. It can be set once in the global"
                            + " context, too, then it becomes the default value for all bus"
                            + " specifications that do not set the value themselves"
                          );

        /* Arguments to specify a user option (name and value). */
        clp.defineArgument( "op", "user-option-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The name of a user option, which is passed into the"
                            + " StringTemplate V4 template as additional attribute. The"
                            + " value of this attribute can be used in the template to"
                            + " conditionally control the code generation.\n"
                            + "  The value of the option (or template attribute) is"
                            + " specified with the other command line argument"
                            + " user-option-value.\n"
                            + "  This argument opens the local context of an option"
                            + " specification. It can be a sub-context of the global"
                            + " context or of the bus or output generation block. It can be"
                            + " used any number of times in each of these contexts. All"
                            + " options specified in the global context will become default"
                            + " values for all buses and output generation blocks but they"
                            + " may be redefined or overridden in those contexts"
                          );
        clp.defineArgument( "ov", "user-option-value"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "Specify the value of a user option or attribute (see"
                            + " user-option-name).\n"
                            + "  This is the only other (and optional) command line"
                            + " argument in the sub-context of a user option. Consequently,"
                            + " if given it needs to immediately follow"
                            + " user-option-name.\n"
                            + "  The value is passed into the StringTemplate V4 template"
                            + " as an object of best suiting Java type; numbers are passed"
                            + " in as either Integer or Double and the literals true/false"
                            + " as Boolean. If none of these fits then the template"
                            + " receives a String with the literal text from the command"
                            + " line.\n"
                            + "  This argument is optional. If omitted then the template"
                            + " will receive the value Boolean(true). This means that"
                            + " Boolean code generation control switches can be passed in"
                            + " by simply putting -op <nameOfSwitch>"
                          );

        /* Arguments to specify a special signal request (name and regular expression),
           which is part of the bus specify block */
        clp.defineArgument( "s", "special-signal-name"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The name of a special signal. If a frame is found to have a"
                            + " signal with matching name then a description of this"
                            + " signal is added under this name to the data model for"
                            + " rendering.\n"
                            + "  It is considered an error if several signals of a"
                            + " single frame would match - then"
                            + " the regular expression re-special-signal should be"
                            + " refined.\n"
                            + "  This argument is used in the context of a bus definition"
                            + " and it opens the sub-context of a special signal request."
                            + " It can be used repeatedly to select several different"
                            + " signals of interest.\n"
                            + "  The argument can be used in the global context, too, then"
                            + " it becomes the default value for all bus specifications"
                            + " that do not set the value themselves (together with the"
                            + " related re-special-signal).\n"
                            + "  Optional, no special signal is requested if not used at"
                            + " all"
                          );
        clp.defineArgument( "re", "re-special-signal"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ null
                          , "The special signal is the one of a frame, which matches"
                            + " against this regular expression; the entire name must"
                            + " match. In simple cases the regular expression can be"
                            + " identical with the name special-signal-name of the special"
                            + " signal but if the signal has differing names in different"
                            + " frames than this regular expression can be applied to"
                            + " filter all of them.\n"
                            + "  This argument must be used only in the context of a"
                            + " special signal request. It can be used once for each"
                            + " special signal request; if used it needs to immediately"
                            + " follow the name of the special signal.\n"
                            + "  Optional, by default the regular expression is identical"
                            + " to the special-signal-name"
                          );

        /* Arguments to specify an output generation block (file and template) */
        clp.defineArgument( "o", "output-file-name"
                          , /* cntMin, cntMax */ 1, -1
                          , /* defaultValue */ null
                          , "The name of a generated output file. This argument opens the"
                            + " context of an output generation block. This block of"
                            + " arguments describes how a single output file is generated"
                            + " from the network database files under control of a template"
                            + " file. Any number of output files can be generated if the"
                            + " output generation block is repeatedly given.\n"
                            + "  Note, directories missing in the path to the designated"
                            + " file will be created by the application.\n"
                            + "  The generated output can be written into a standard"
                            + " console stream, too. Two file names are reserved to do so:\n"
                            + "  - stdout\n"
                            + "  - stderr\n"
                            + "  If the output of the information rendering process is"
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
                            + "  Note, the template engine locates template files through"
                            + " the Java class path. This is the class path defined when"
                            + " starting the code generator application. The launch scripts"
                            + " in the diverse samples demonstrate how to set the class"
                            + " path appropriately. Consider using argument"
                            + " string-template-verbose if locating files causes trouble.\n"
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
                            + " context, too, then the global value becomes the"
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
                            + "  The command line argument can be used once in the context"
                            + " of an output generation block.\n"
                            + "  The argument can be used once in the global"
                            + " context, too, then the global value becomes the"
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
                            + "  The command line argument can be used once in the context"
                            + " of an output generation block.\n"
                            + "  The argument can be used once in the global"
                            + " context, too, then the global value becomes the"
                            + " default for all output generation blocks that do not"
                            + " specify the value themselves. Optional, the default is"
                            + " \"info\" if this argument is not used at all"
                          );
        clp.defineArgument( "w", "template-wrap-column"
                          , /* cntMin, cntMax */ 0, -1
                          , /* defaultValue */ -1
                          , "This integer argument is passed to the render function of"
                            + " StringTemplate V4. The templates can be written such that"
                            + " the generated lines of output are wrapped after this column;"
                            + " please, refer to http://www.stringtemplate.org/"
                            + " for details.\n"
                            + "  The argument can be used once in the context"
                            + " of an output generation block.\n"
                            + "  The argument can be used once in the global"
                            + " context, too, then the global value becomes the"
                            + " default for all output generation blocks that do not"
                            + " specify the value themselves. Optional, the default is"
                            + " not to wrap lines if this argument is not used at all"
                          );
    } /* End of ParameterSet.defineArguments */


    /**
     * Filter arguments, which relate to this class.
     *   @return Next relevant argument or null if there's no one left.
     */
    private String parseGetNextArg(Iterator<String> argStream)
    {
        while(argStream.hasNext())
        {
            String arg = argStream.next();
            switch(arg)
            {
            case "cluster-name":
            case "node-name":
            case "string-template-verbose":
            case "bus-name":
            case "network-file-name":
            case "invert-transmission-direction":
            case "include-frame-id":
            case "re-include-frame-name":
            case "exclude-frame-id":
            case "re-exclude-frame-name":
            case "user-option-name":
            case "user-option-value":
            case "special-signal-name":
            case "re-special-signal":
            case "template-file-name":
            case "template-name":
            case "template-arg-name-cluster":
            case "template-arg-name-info":
            case "template-wrap-column":
            case "output-file-name":

                return arg;

            default:
            }
        }

        return null;

    } /* End ParameterSet.parseGetNextArg */


    /**
     * Extract an integer range from the value of an according command line argument.
     *   @return The function either returns the pair of integers that designates the range or
     * it throws a CmdLineParser.InvalidArgException exception with contained error
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
               controlling flags, which can be added jaut like that on the command line,
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
     * Parsing state/context special signal request.
     *   @throws CmdLineParser.InvalidArgException
     * A CmdLineParser.InvalidArgException exception with contained error message is thrown
     * in case of failures.
     *   @return The function returns the next token from the stream of command line
     * arguments. (It needs to read it from the stream as look-ahead for parsing of
     * optional context elements.)
     *   @param requestMap
     * If the function succeeds it adds the successfully parsed special signal request to
     * this Map under the name of the special signal. If the map already contains an entry
     * then this entry will silently be replaced - this is how default values given in the
     * global context are handled.
     *   @param clp
     * The command line parser object, which contains the stream of arguments, which is
     * input to the parser.
     *   @param argStream
     * The iterator of this argument stream. The name of the requested special signal is
     * the argument last recently read from the stream.
     */
    private String parseStateSpecialSignal( Map<String,SpecialSignalRequest> requestMap
                                          , CmdLineParser clp
                                          , Iterator<String> argStream
                                          )
        throws CmdLineParser.InvalidArgException
    {
        /* This state/context is entered on its opening argument. */
        final String argName = "special-signal-name";

        /* Retrieve the value of this command line argument. */
        final String name = clp.getString(argName);

        /* The name will become a map key in STringTemplate V4 and needs to be a valid
           identifier there. */
        if(!name.matches("(?i)[a-z][a-z_0-9]*"))
        {
            throw new CmdLineParser.InvalidArgException
                      ("Command line option special-signal-name expects an identifier as"
                       + " argument but got " + name
                       + ". Any characters other than letters, digits or the"
                       + " underscore are not permitted"
                      );
        }

        /* The special signal request only has two elements and the other one (the regular
           expression) is optional. */
        String argNext = parseGetNextArg(argStream)
             , reSpecialSignal;
        if(argNext != null  &&  argNext.compareTo("re-special-signal") == 0)
        {
            reSpecialSignal = clp.getString(argNext);

            /* To unify both cases for the caller, optional argument given or not given, we
               need to comsume a further argument now. */
            argNext = parseGetNextArg(argStream);
        }
        else
        {
            /* By default, no regular expression match is done but a simple compare with
               the name. */
            reSpecialSignal = null;
        }

        /* Add to or replace the request in the map. */
        assert requestMap != null;
        requestMap.put(name, new SpecialSignalRequest(name, reSpecialSignal));

        return argNext;

    } /* End ParameterSet.parseStateSpecialSignal */



    /**
     * Call the method LinkedHashMap.clone() on a LinkedHashMap&lt;String,Object&gt; without
     * getting compiler warnings because of unsafe type casts.
     *   @return
     * Get the cloned map (shallow copy).
     *   @param map
     * The map to clone.
     */
    @SuppressWarnings("unchecked")
    private static <T> LinkedHashMap<String,T> cloneLinkedHashMap(LinkedHashMap<String,T> map)
    {
        return (LinkedHashMap<String,T>)map.clone();

    } /* End of ParameterSet.cloneLinkedHashMap */




    /** The internal states of the argument parsing algorithm. */
    private enum ParseState {global, bus, output, terminated};


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
        BusDescription defaultBus = new BusDescription()
                     , bus = null;
        TemplateOutputPair defaultTemplateOutputPair = new TemplateOutputPair()
                         , templateOutputPair = null;

        /* Some context information for logging. */
        final String ctx = "ParameterSet.parseCmdLine: ";

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
                _logger.debug(ctx + "Found argument " + arg);
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

            /* Most arguments have a string value. This is a temporary variable to hold
               such a value. */
            String argVal;

            /* Handle the relevant arguments state dependently. */
            switch(state)
            {
            case global:
                switch(arg)
                {
                /* The initial cases handle the transitions to other states. We never come
                   back to the global context; check for completeness, add default
                   values. */
                case "bus-name":
                    /* This argument opens a bus definition context. */
                case "output-file-name":
                    /* This argument opens an output generation context. */
                case "":
                    /* This case is used after the very last argument to close any open
                       context. */
                    _logger.debug(ctx + "Global context is closed forever");

                    if(clusterName == null)
                        clusterName = "cluster";
                    if(nodeName == null)
                        throw new CmdLineParser.InvalidArgException
                                  ("Mandatory argument node-name not found. It needs to"
                                   + " be given prior to opening any other context"
                                  );

                    if(arg.compareTo("bus-name") == 0)
                        state = ParseState.bus;
                    else if(arg.compareTo("output-file-name") == 0)
                        state = ParseState.output;
                    else
                        state = ParseState.terminated;

                    argHasBeenConsumed = false;
                    break;

                case "cluster-name":
                    /* Repeated appearance is already filtered by the parser. */
                    assert clusterName == null;
                    clusterName = clp.getString(arg);
                    break;

                case "node-name":
                    if(nodeName != null)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("Node name repeatedly set in the global context. Was "
                                   + nodeName + " and should become " + clp.getString(arg)
                                  );
                    }
                    nodeName = clp.getString(arg);
                    break;

                case "string-template-verbose":
                    assert stringTemplateVerbose == false;
                    stringTemplateVerbose = true;
                    break;

                /* Here we have some cases for bus and output generation context arguments,
                   which have reasonable common default values. The values the user passes
                   in the global context are stored locally and used as default value for
                   all later instances of that contexts. */

                case "re-include-frame-name":
                    if(defaultBus.reInclFrameName != null)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("Regular expression for inclusion of frames by name"
                                   + " repeatedly set in the global context. Was "
                                   + defaultBus.reInclFrameName
                                   + " and should become " + clp.getString(arg)
                                  );
                    }
                    defaultBus.reInclFrameName = clp.getString(arg);
                    break;

                case "re-exclude-frame-name":
                    if(defaultBus.reExclFrameName != null)
                    {
                        throw new CmdLineParser.InvalidArgException
                                  ("Regular expression for exclusion of frames by name"
                                   + " repeatedly set in the global context. Was "
                                   + defaultBus.reExclFrameName
                                   + " and should become " + clp.getString(arg)
                                  );
                    }
                    defaultBus.reExclFrameName = clp.getString(arg);
                    break;

                case "special-signal-name":
                    /* The sub-parse-functions needs one argument look-ahead to handle
                       optional arguments. The last recently read token has therefore not
                       yet been consumed. */
                    arg = parseStateSpecialSignal(defaultBus.specialSignalMap, clp, it);
                    argHasBeenConsumed = false;
                    break;

                case "user-option-name":
                    /* The sub-parse-functions needs one argument look-ahead to handle
                       optional arguments. The last recently read token has therefore not
                       yet been consumed. */
                    arg = parseStateUserOption(defaultBus.optionMap, clp, it);
                    argHasBeenConsumed = false;
                    break;

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

            case bus:
                if(bus == null)
                {
                    assert arg.compareTo("bus-name") == 0;
                    bus = new BusDescription();
                    bus.name = clp.getString(arg);
                    bus.specialSignalMap = cloneLinkedHashMap(defaultBus.specialSignalMap);
                    bus.optionMap = cloneLinkedHashMap(defaultBus.optionMap);
                    _logger.debug(ctx + "Bus context " + bus.name + " is opened");
                }
                else
                {
                    switch(arg)
                    {
                    /* The initial cases initiate closing the current context. */
                    case "":
                        /* This case is used after the very last argument to close any open
                           context. */
                    case "bus-name":
                        /* If we see this argument the second time while in this state then
                           it openes a new instance of the bus context. */
                    case "output-file-name":
                        /* This openes an ouput file context. The current bus context is
                           closed. */
                        _logger.debug(ctx + "Bus context " + bus.name + " is closed");

                        /* The current context is checked for completeness and finalized
                           with default values. */
                        if(bus.networkFileName == null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Mandatory argument network-file-name not found for"
                                       + " bus " + bus.name + ". It needs"
                                       + " to be given prior to opening any new context,"
                                       + " including another bus definition context"
                                      );
                        }
                        if(bus.me == null)
                        {
                            bus.me = nodeName;
                            _logger.debug(ctx + "Use default value " + nodeName + " for "
                                          + bus.name + ".me"
                                         );
                        }
                        /* The user may have set default values in the global context for
                           the next parameters. If so and if they were not set in this
                           bus specification context then uses those default values. */
                        if(bus.reInclFrameName == null)
                            bus.reInclFrameName = defaultBus.reInclFrameName;
                        if(bus.reExclFrameName == null)
                            bus.reExclFrameName = defaultBus.reExclFrameName;

                        /* Add the finalized object to the parameter set. */
                        busDescriptionAry.add(bus);
                        bus = null;

                        if(arg.compareTo("output-file-name") == 0)
                            state = ParseState.output;
                        else if(arg.compareTo("") == 0)
                            state = ParseState.terminated;
                        else assert arg.compareTo("bus-name") == 0;

                        /* Let this argument be reparsed again for opening the new context. */
                        argHasBeenConsumed = false;
                        break;

                    case "node-name":
                        if(bus.me != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Node name repeatedly set in the context of bus "
                                       + bus.name + ". Was " + bus.me
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        bus.me = clp.getString(arg);
                        break;

                    case "network-file-name":
                        if(bus.networkFileName != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Network file name repeatedly set in the context"
                                       + " of bus " + bus.name + ". Was " + bus.networkFileName
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        bus.networkFileName = clp.getString(arg);
                        break;

                    case "invert-transmission-direction":
                        if(bus.invertTransmissionDirection)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Transmission direction inversion repeatedly demanded"
                                       + " in the context of bus " + bus.name
                                      );
                        }
                        bus.invertTransmissionDirection = true;
                        break;

                    case "include-frame-id":
                        argVal = clp.getString(arg);
                        _logger.debug(ctx + "Included frame ID range " + argVal + " found");
                        bus.inclFrameIdAry.add(parseIntRange(arg, argVal));
                        break;

                    case "re-include-frame-name":
                        if(bus.reInclFrameName != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Regular expression for inclusion of frames by name"
                                       + " repeatedly set in the context of bus " + bus.name
                                       + ". Was " + bus.reInclFrameName
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        bus.reInclFrameName = clp.getString(arg);
                        break;

                    case "exclude-frame-id":
                        argVal = clp.getString(arg);
                        _logger.debug(ctx + "Excluded frame ID range " + argVal + " found");
                        bus.exclFrameIdAry.add(parseIntRange(arg, argVal));
                        break;

                    case "re-exclude-frame-name":
                        if(bus.reExclFrameName != null)
                        {
                            throw new CmdLineParser.InvalidArgException
                                      ("Regular expression for exclusion of frames by name"
                                       + " repeatedly set in the context of bus " + bus.name
                                       + ". Was " + bus.reExclFrameName
                                       + " and should become " + clp.getString(arg)
                                      );
                        }
                        bus.reExclFrameName = clp.getString(arg);
                        break;

                    case "special-signal-name":
                        /* The sub-parse-functions needs one argument look-ahead to handle
                           optional arguments. The last recently read token has therefore not
                           yet been consumed. */
                        arg = parseStateSpecialSignal(bus.specialSignalMap, clp, it);
                        argHasBeenConsumed = false;
                        break;

                    case "user-option-name":
                        /* The sub-parse-functions needs one argument look-ahead to handle
                           optional arguments. The last recently read token has therefore not
                           yet been consumed. */
                        arg = parseStateUserOption(bus.optionMap, clp, it);
                        argHasBeenConsumed = false;
                        break;

                    default:
                        throw new CmdLineParser.InvalidArgException
                                  ("The use of argument " + arg + " is invalid in the context"
                                   + " of bus definition " + bus.name
                                  );
                    } /* switch(Which argument to be consumed in the opened context bus?) */
                } /* if(Do we open the context bus or is it already opened?) */
                break;

            case output:
                if(templateOutputPair == null)
                {
                    assert arg.compareTo("output-file-name") == 0;
                    templateOutputPair = new TemplateOutputPair();
                    templateOutputPair.outputFileName = clp.getString(arg);
                    templateOutputPair.optionMap = cloneLinkedHashMap(defaultBus.optionMap);
                    _logger.debug(ctx + "Output generation context for file "
                                  + templateOutputPair.outputFileName + " is opened"
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
                    case "output-file-name":
                        /* If we see this argument the second time while in this state then
                           it openes a new instance of the output generation context. */
                    case "bus-name":
                        /* This openes a bus specification context. The current output
                           generation context is closed. */
                        _logger.debug(ctx + "Output generation context for file "
                                      + templateOutputPair.outputFileName + " is closed"
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
                           output generation context then uses those default values. */
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

                        if(arg.compareTo("bus-name") == 0)
                            state = ParseState.bus;
                        else if(arg.compareTo("") == 0)
                            state = ParseState.terminated;
                        else assert arg.compareTo("output-file-name") == 0;

                        /* Let this argument be reparsed again for opening the new context. */
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
                                  ("The use of argument " + arg + " is invalid in the context"
                                   + " of output file definition "
                                   + templateOutputPair.outputFileName
                                  );
                    } /* End switch(Which arg in the open output generation context?) */
                } /* if(Do we open the output generation context or is it already opened?) */
                break;

            default: assert false;

            } /* End switch(Current state) */

            assert arg == null  ||  arg.compareTo("") != 0  ||  state == ParseState.terminated
                   : "Final, terminating transition of state machine failed"
                   ;

        } /* End while(For all command line arguments) */

    } /* End of ParameterSet.parseCmdLine */



    /**
     * Render the current settings as a string.
     */
    public String toString()
    {
        final String templateFileName = "codeGenerator/main/ParameterSet_toString.stg";
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

/**
 * @file Bus.java
 * Part of the output data model for information rendering by StringTemplate: The single
 * communication bus as described by a DBC file.
 *
 * Copyright (C) 2015-2023 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class Bus
 */

package codeGenerator.dataModelListener;

import java.util.*;
import org.apache.log4j.*;
import codeGenerator.dbcParser.*;


/**
 * The data structure describing a communication bus in the cluster.
 *   An object of this class contains the results of parsing a single DBC input file. The
 * object is part of the nested data structure passed to the template engine for rendering
 * the information in the wanted format.<p>
 *   The basic design principle is the use of public members because such members can be
 * directly accessed from the output templates. Boolean decisions are either expressed as
 * explicit public Boolean members or by references to nested member objects, which are
 * null if an optional member is not used or applicable: StringTemplate can query public
 * Boolean members or check for null references.
 */

public class Bus extends NetObject implements IST4CmdListener<Object,Object>
{
    /** The global logger object for all progress and error reporting. */
    private static Logger _logger = Logger.getLogger(Bus.class.getName());

    /** The error counter, which counts the template emitted errors and warnings. */
    private final ErrorCounter errCnt_;

    /** The name of the network description file, which has been parsed into this object.
          The network file is described with an extended Java File object. This
        object extends the Java class File by adding some information about parts of the
        file name (like its extension). Please refer to class FileExt.<p>
          The information from the Java class File is partly accessible: All methods
        starting with get or is can be used by placing the method name without the syllable
        get/is into the template, e.g. {@code <networkFile.name>} would yield the return
        value of File.getName().<p>
          Please refer to the online help of Java class File for details. */
    public FileExt networkFile = null;
    
    /** A map of attribute definitions, including the default values. An attribute is
        referenced by its name. The list contains all user-requested attributes, regardless
        whether it refers to the bus, to a node, a frame or a signal. This is safely
        possible as all attribute share the same namespace. */
    public HashMap<String,AttributeDef> attribDefMap = null;
    
    /** The same attribute definition objects, which are stored in the map attribDefMap can
        also be referenced from this array. The array structure supports the implementation
        of overview tables of attributes in a template. */
    public ArrayList<AttributeDef> attribDefAry = null;
    
    /** The number of attribute definitions in <b>attribDefAry</b> and <b>attribDefMap</b>.
        From a StringTemplate V4 template this member is accessed as {@code
        <bus.noAttribDefs>}.
          @return Get the number of attribute definitions. */
    public int getNoAttribDefs()
    {
        assert attribDefAry == null  ||  attribDefAry.size() == attribDefMap.size();
        return attribDefAry != null? attribDefAry.size(): 0;
    }

    /** A bus has network nodes. */
    public Node[] nodeAry = null;
    
    /** A bus knows frames, which are exchanged with the given network node on this bus.
        Initially, the frames in the array have the order of appearance in the parsed
        network database. The array can be sorted using the command {@link #sortFrameAry}. */
    public ArrayList<Frame> frameAry = null;

    /** The number of frames in <b>frameAry</b>. From a StringTemplate V4
        template this member is accessed as {@code <bus.noFrames>}.
          @return Get the number of frames. */
    public int getNoFrames()
        { return frameAry != null? frameAry.size(): 0; }

    /** This field can be used to demand a certain sort order of the frame list {@link
        #frameAry}. The order of frames is changed by passing the wanted sort order in a
        StringTemplate V4 template expression like {@code <bus.sortFrameAry.byId>}, where
        {@code byId} is meant an example of a supported sort order. All subsequently
        evaluated template expressions will find the field {@code <bus.frameAry>} in the
        new sort order. Supported sort orders are:<br><ul>
          <li> {@code byName}: Ascending lexical order of names 
          <li> {@code byNameInverse}: Descending lexical order of names 
          <li> {@code byId}: Ascending CAN ID. Note, if an 11 and a 29 Bit frame have the
        same ID then the extended CAN ID is considered the greater one.
          <li> {@code byIdInverse}: Descending CAN ID. Note, if an 11 and a 29 Bit frame
        have the same ID then the extended CAN ID is considered the greater one.
          <li> {@code byIsExtId}: Extended 29 Bit IDs come after 11 Bit standard IDs.
          <li> {@code byIsExtIdInverse}: Extended 29 Bit IDs come before 11 Bit standard IDs.
          <li> {@code bySize}: Ascending size of payload
          <li> {@code bySizeInverse}: Descending size of payload
          <li> {@code sentFirst}: Sent frames before received frames
          <li> {@code receivedFirst}: Received frames before sent frames
        </ul><br>
          Some sort orders, e.g. sorting by size of payload, are undecided for several
        frames. A secondary, sub-ordinated sort criterion can be implemented by sorting
        twice. Begin with the sub-ordinated criterion (e.g. sorting by CAN ID) followed by
        the principal sort order (e.g. by size of payload). In this example would larger
        frames follow smaller ones and all equally sized would be ordered by ascending CAN
        ID.<p>
          Please note, once you've sorted the array of frames the initial order is lost and
        can't be restored again. This is the order of appearance in the network file.
        However, a frame iteration in this order is still possible via field {@link
        Cluster#frameInFileOrderAry}. Reordering a bus' frame list won't affect the frame
        lists owned by {@link Cluster}; {@link Cluster#frameByNameAry}, {@link
        Cluster#frameByIdAry} and {@link Cluster#frameInFileOrderAry} are constant
        throughout the complete template expansion process. */
    public final ST4CmdInterpreter<Object,Object> sortFrameAry;

    /** The transmission direction of frames (sent, received, none of these or both) is
        encoded in the network database and can be found in according fields in {@link
        Frame}, {@link Pdu} and {@link Signal}. However, on command line demand can this
        information be inverted in order to support code generation for residual bus
        simulation. Actually sent frames are reported as being received and 
        vice versa. (See command line argument invert-transmission-direction.)<p>
          This field reflects the value of the command line argument. A StringTemplate V4
        template can be made aware of transmission direction inversion.
          @remark
        Caution, the inversion of the transmission direction only relates to the fields
        {@code isSent} and {@code isReceived} of frames, PDUs and signals. The textual
        information about senders and receivers is not affected. In this sense is the
        resulting data model inconsistent if inversion is applied. */
    public boolean isTransmissionDirectionInverted = false;
    
    /** Theoretically a signal may have a floating point data type. Since this is very rare
        in practice any normal application of the code generator will likely not support
        this case. To enable safe code generation templates here's a flag which tells if
        there is at least one floating point signal in use in the bus. If so, the template
        could generate an error. */
    public boolean hasFloatingPointSignals = false;
    
    /** Theoretically a floating point signal in a network database file can have a linear
        scaling. However, this is completely useless and won't probably be ever supported
        by any code generation template. To enable safe code generation templates here's a
        flag which tells if there is at least one scaled floating point signal in use in
        the bus. If so, the template could generate an error. */
    public boolean hasScaledFloatingPointSignals = false;
    
    /** A map of user specified options or template attributes. User options are template
        attributes, which are simply passed through from the application's command line to
        the StringTemplate V4 template and can there be used to control the code
        generation. The use case are optional constructs in the generated code, which are
        controlled from the command line.<p>
          User options/attributes can be given in the global context of the command line or
        in the context of a bus or an output file. This map holds all options given in the
        context of the bus. (Refer to {@link Info#optionMap} for the output file related
        options.)<p>
          User options, which are set in the global context of the command line are adopted
        by all buses. Nonetheless, a particular bus can re-specify the same option with
        deviating value.<p>
          The name of an option or template attribute is the key into the map and the
        attribute's value is the value of the map entry. The value object's Java type is
        one out of String, Boolean, Integer or Double, depending on which fits best to the
        original command line argument; a command line argument <i>cmdLineArg</i> with
        value {@code true} would obviously be passed as Boolean into the template and would
        permit conditional code by using a construct like: {@code
        <if(bus.optionMap.cmdLineArg)>Command line argument is TRUE!<endif>} */
    public HashMap<String,Object> optionMap = null;
    
    /** The number of user options in <b>optionMap</b>. From a StringTemplate V4
        template this member is accessed as {@code <bus.noOptions>}.
          @return Get the number of options. */
    public int getNoOptions()
        { return optionMap != null? optionMap.size(): 0; }
        
        
    /** 
     * Create the Bus object.
     *   @param errCnt Template emitted and caused errors are counted in this object.
     */
    public Bus(ErrorCounter errCnt)
    {
        assert errCnt != null: "Don't pass null as error counter";
        errCnt_ = errCnt;
        sortFrameAry = new ST4CmdInterpreter<>( /* context */ null
                                              , /* cmdListener */ this
                                              , errCnt_
                                              , /* logContext */ "<Bus.sortFrameAry>: "
                                              );
    } /* End of Bus */
    
    
    /**
     * This method implements the command listener, which is used to apply the sorting of
     * the frame array.
     *   @return The function will always return null: The template engine must not
     * generate any output because of the sort command.
     *   @param context
     * The context is not used; the listener is connected only to a single command
     * interpreter. It expects null.
     *   @param cmd
     * The command string, which is interpreted as a sort command.
     */
    public Object interpret(Object context, String cmd)
    {
        assert context == null;
        
        try
        {
            /* Set the wanted sort order statically for the class of frames. Then run the
               collection's sort function.
                 valueOf parses the string and will report an error by exception. */
            Frame.setSortOrder(Frame.SortOrder.valueOf(cmd));
            if(frameAry != null)
                Collections.sort(frameAry);
        }
        catch(IllegalArgumentException ex)    
        {
            /* Normal runtime situation: An invalid string has been passed to the command
               interpreter. We don't do anything, just report the problem. */
            errCnt_.error();
            _logger.error("Error interpreting sort order " + cmd + " for frameAry of bus "
                          + name + ". No such sort order is supported"
                         );
        }
        
        /* Sorting the frames should never produce any output in the template expansion. */
        return null;
        
    } /* End if IST4CmdListener.interpret */
    
} /* End of class Bus definition. */





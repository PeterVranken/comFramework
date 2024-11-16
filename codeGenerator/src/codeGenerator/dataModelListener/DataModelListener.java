/**
 * @file DataModelListener.java
 * This class implements a antlr parser listener that shapes the data model from the parse
 * tree.
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
/* Interface of class DataModelListener
 *   DataModelListener
 *   getBusData
 *   errorContext (2 variants)
 *   getNumberFromContext
 *   getIntegerFromText
 *   getIntegerFromContext
 *   getIntegerFromToken
 *   getLongIntegerFromText
 *   getLongIntegerFromContext
 *   getStringFromToken
 *   getAttribNameFromToken
 *   getImplType
 *   exitNodes
 *   parseCanId
 *   enterMsg
 *   invertPduTransmissionDirection
 *   exitMsg
 *   transformGrid
 *   checkSignalForSpecialSignal
 *   testBitsInSet
 *   markSignalBitsAsOccupied
 *   exitSignal
 *   exitGlobalComment
 *   exitNodeComment
 *   exitMsgComment
 *   exitSignalComment
 *   exitValueDescription
 *   exitAttributeDefinition
 *   exitUnrecognizedStatement
 *   getAttribute
 *   exitAttributeDefault
 *   exitAttributeValue
 *   exitSignalExtendedValueTypeList
 *   pduInheritsFrameAttributes
 *   checkForActualAttribValues
 *   walk
 */

package codeGenerator.dataModelListener;

import java.util.*;
import java.io.*;
import org.apache.log4j.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import codeGenerator.dbcParser.*;
import codeGenerator.main.ParameterSet;
import codeGenerator.main.Pair;


/**
 * This class implements a listener for a walk through the parse tree. The antlr parser
 * listener shapes the data model from the parse tree.
 */

public class DataModelListener extends DbcBaseListener
{
    /** Access the Apache logger object. */
    private static Logger _logger = Logger.getLogger(DataModelListener.class.getName());

    /** Counter for all errors and messages, which occur during semantic listening. */
    private ErrorCounter errCnt_;

    /** A copy of the bus related application parameters is needed to interpret parts of the
        parsed information. We need e.g. to know the name of the node to generate code for
        in order to determine the transmission direction of a frame. */
    ParameterSet.BusDescription busDescription_ = null;

    /** The listener collects all data in this bus object. */
    private Bus bus_ = null;

    /** Temporarily used working context for the signal listeners: The PDU object, which found
        signals are added to. */
    private Pdu pdu_ = null;

    /** Temporarily used map of set of bits of signal sets. Needed for detection of
        overlapping signals. Distinct signals sets are the alternative, multiplexed signals
        sets. The normal signals form another set. Each bit set is represented by an array
        of bytes. */
    private HashMap<Integer,int[]> pdu_mapOfBitSetsOfMuxSigs_ = new HashMap<>();

    /** Temporarily used array to collect the bytes of the PDU data area that are
        completely occupied by a single unmultiplexed signal. Needed to determine the set
        of bytes, which need initialization in the pack functions. */
    private boolean[] pdu_setOfSigBytes_ = null;

    /** Temporarily used array to collect data bytes of PDU that are occupied by at least
        one multiplexed signal. Needed to determine the set of bytes, which need
        initialization in the pack functions. */
    private boolean[] pdu_setOfMuxedSigBytes_ = null;

    /** Filling the list of sets of multiplexed signals is supported by this temporarily
        used map, which relates each list entry to the multiplex switch value. */
    private HashMap<Integer,Pdu.MultiplexedSignalSet> pdu_mapMuxSignalSetByMuxValue_ =
                                                                            new HashMap<>();
    /** Temporarily set variable to state the context we are currently in. Used as intro
        for error and progress messages. */
    private String errorContext_ = "";

    /** A list of names of floating point signals. The list is implemented as a String,
        which is used for reporting. */
    private StringBuffer nameListOfFloatSignals_ = null;

    /** A set of all bad CAN IDs, which have been reported as either error or warning. Used
        to suppress repeated messages on the same ID. */
    private HashSet<Long> setOfBadCanIDs_ = new HashSet<>();

    /** A counter of warnings "BA_REL_ not supported". If used in a DBC then this warning
        would typically appear very often. We stop after some repetitions. */
    private int noWarningsBaRel_ = 0;

    /** A map of all network nodes that are declared in the header part of the network
        database file. */
    private HashMap<String,Node> mapNodeByName_ = new HashMap<>();

    /** Helper class: The CAN ID. */
    static private class CanId
    {
        /** The numeric value of the CAN ID. */
        int id_;

        /** The Boolean distinction between 11 and 29 Bit CAN IDs. */
        boolean isExtId_;

        /** Constructor for a new CAN ID object. */
        public CanId(int canId, boolean isExtId)
        {
            id_ = canId;
            isExtId_ = isExtId;
        }


        /**
         * Comparison of this with another object of type CanId. The intention of this
         * method is enabling the use of objects of this class as key value in collection
         * classes.
         *   @return
         * Get true is "this" has same values for all its fields as the other CanId
         * object.
         *   @param otherObj
         * The other CanId object. An assertion fires if otherObj is not of class CanId.
         */
        @Override public boolean equals(Object otherObj)
        {
            /* Same object reference means identity and identity implies equalality. */
            if (this == otherObj)
                return true;

            /* A full implementation of equals requires some code to make it safely
               available to objects of any class. We don't support it, since we won't ever
               apply it to foreign objects. We catch this situation just by assertion. */
            //if(otherObj == null || getClass() != otherObj.getClass())
            //      return false;
            assert otherObj instanceof CanId
                   : "CanId.equals is implemented only for objects of same type";

            CanId otherCanId = (CanId)otherObj;
            return id_ == otherCanId.id_  &&  isExtId_ == otherCanId.isExtId_;

        } /* End of CanId.equals */


        /**
         * Provide a hash value of this object. The returned value as such is meaningless
         * but it'll always be the same if called from objects, which are equal. The
         * intention of this method is enabling the use of objects of this class as key
         * value in collection classes.
         *   @return
         * Get the hash value as an integer.
         */
        @Override public int hashCode()
        {
            /* From practice, we know that the co-presence of standard and extended IDs of
               same numeric value is very unlikely to happen. Therefore, making the hash
               code not maximally unique by disregarding the discimination between standrad
               and extended will normally speedup the code: The frequently used hashCode()
               becomes faster on (the little) cost of very rare additional calls of
               equals(). */
            return id_ /* << 1 | (isExtId_? 1: 0) */;
        }

        /**
         * Short form of textual representation of CAN ID: Decimal and with postfix 'x' for
         * extended IDs.
         */
        @Override public String toString()
            { return "" + id_ + (isExtId_? "x": ""); }

        /**
         * Long form of textual representation of CAN ID: Decmal plus hexadecimal
         * representation.
         */
        public String toStringLong()
            { return toString() + " (0x" + Integer.toHexString(id_) + ")"; }

    } /* End class CanId */


    /** A map of all frames. Lookup by ID. */
    private HashMap<CanId,Frame> mapFrameById_ = new HashMap<>();

    /** A map of all signals. Lookup by frame ID and name. */
    private HashMap<Pair<CanId,String>, Signal> mapSignalByIdAndName_ = new HashMap<>();


    /**
     * A new instance of DataModelListener is created.
     *   An error counter is passed in to let this listener continue counting parse errors
     * after the parser itself and other listeners.
     *   @param errCnt
     * The future semantic messages of the listener are counted in this object. The counter
     * is taken as it is and not reset or initialized.<p>
     *   Optional, pass null to use a new error counter.
     */
    private DataModelListener(ErrorCounter errCnt)
    {
        if(errCnt != null)
            errCnt_ = errCnt;
        else
            errCnt_ = new ErrorCounter();

        /* Create a result object. */
        bus_ = new Bus(errCnt_);

    } /* End of DataModelListener.DataModelListener. */



    /**
     * Get the data model after (successful) walk through the parse tree.
     *   @return
     * The network data for a single DBC file or null if parsing failed.
     *   @param resetListener
     * If true than all data this listener has collected so far is discarded after return so
     * that the listener can be reused for another parse tree walk.
     */
    private Bus getBusData(boolean resetListener)
    {
        Bus result;

        if(errCnt_.getNoErrors() == 0)
            result = bus_;
        else
            result = null;

        if(resetListener)
            bus_ = new Bus(errCnt_);

        return result;

    } /* End of  getBusData */



    /**
     * Get some context information for error messages.
     *   @return Get a string indicating filename and line number.
     *   @param ctx The information is retrieved from the start token of a rule context.
     *   @remark Preferrably use the other method if you have a particular token.
     */
    private String errorContext(ParserRuleContext ctx)
        { return errorContext(ctx.start); }



    /**
     * Get some context information for error messages.
     *   @return Get a string indicating filename and line number.
     *   @param token The information is retrieved from the token of interest.
     *   @remark Preferrably use this method if you have a particular token.
     */
    private String errorContext(Token token)
    {
        return bus_.networkFile.getName() + ":" + token.getLine() + ": ";
    }



    /**
     * Get the numeric value from the parser's NumberContext object.
     *   @return
     * The value as a double.
     *   @param ctx
     * The context object as returned by the parser.
     */
    private double getNumberFromContext(DbcParser.NumberContext ctx)
    {
        /* The exceptions from the string to number conversions are purposily now caught: The
           got text has been validated by the parser; if we run into a conversion problem
           than this is a true internal implementation error, worth an exception. */
        final double n = Double.parseDouble(ctx.getText());

        /* We won't see a format conversion error by exception but too large numbers would
           result in +/- inf. We can catch this by explicitly asking and produce an error
           in case. */
        if(n > Double.MAX_VALUE  ||  n < -Double.MAX_VALUE)
        {
            errCnt_.error();
            _logger.error(errorContext(ctx)
                          + "Invalid floating point value found. The supported range of"
                          + " [-" + Double.MAX_VALUE + ", " + Double.MAX_VALUE + "]"
                          + " is exceeded"
                         );

            /* No need to have a replacement value as for integers: Infinite is a valid
               number representation. */
        }

        return n;

    } /* End of DataModelListener.getNumberFromContext */





    /**
     * Get the numeric value from a char string returned by the parser and do the
     * appropriate error handling.\n
     *   The parser reads an integer as a sequence of digits of any length. This listener
     * uses the Java type int for data modelling, which means a range limitation. If the
     * network database file should contain an integer outside of this range than an error
     * is raised by this method although it is syntactically correct.
     *   @return
     * The value as an int or -1 in case of errors.\n
     *   Error reporting is done by logging the message and incrementing the global error
     * counter.\n
     *   Caution, the return value -1 can in most situation be seen as an error indication
     * (as most occurances of integers in the grammar are unsigned integers) but not in
     * general. Where the grammar permits signed values the -1 can be the legal numeric
     * value. In this case the client of the method doesn't get a direct error
     * notification. He could compare the global error counter prior and after the call of
     * this method.
     *   @param errorContext
     * A string prepending a possible error message. Should indicate where in the input the
     * problematic number conversion appeared.
     *   @param numAsText
     * The character string, e.g. fetched from the parse tree.
     */
    private int getIntegerFromText(String errorContext, String numAsText)
    {
        int intVal = -1;
        try
        {
            intVal = Integer.parseInt(numAsText);
        }
        catch(NumberFormatException e)
        {
            /* The only possible reason for this exception is a range violation. */
            errCnt_.error();
            _logger.error(errorContext
                          + "Invalid integer found. The supported range of [-2^31, 2^31-1]"
                          + " is exceeded"
                         );
            intVal = -1;
        }

        return intVal;

    } /* End of DataModelListener.getIntegerFromText */





    /**
     * Get the numeric value from the parser's Integer Token or SignedIntegerContext
     * object.\n
     *   The parser reads an integer as a sequence of digits of any length. This listener
     * uses the Java type int for data modelling, which means a range limitation. If the
     * network database file should contain an integer outside of this range than an error
     * is raised by this method although it is syntactically correct.
     *   @return
     * The value as an int or -1 in case of errors.\n
     *   Error reporting is done by logging the message and incrementing the global error
     * counter.\n
     *   Caution, the return value -1 can in most situation be seen as an error indication
     * (as most occurances of integers in the grammar are unsigned integers) but not in
     * general. Where the grammar permits signed values the -1 can be the legal numeric
     * value. In this case the client of the method doesn't get a direct error
     * notification. He could compare the global error counter prior and after the call of
     * this method.
     *   @param ctx
     * The context object as returned by the parser.
     */
    private int getIntegerFromContext(DbcParser.SignedIntegerContext ctx)
    {
        return getIntegerFromText(errorContext(ctx), ctx.getText());

    } /* End of DataModelListener.getIntegerFromContext */





    /**
     * Get the numeric value from the parser's Integer Token object.\n
     *   The parser reads an integer as a sequence of digits of any length. This listener
     * uses the Java type int for data modelling, which means a range limitation. If the
     * network database file should contain an integer outside of this range than an error
     * is raised by this method although it is syntactically correct.
     *   @return
     * The value as an int or -1 in case of errors.\n
     *   Error reporting is done by logging the message and incrementing the global error
     * counter.\n
     *   Caution, the return value -1 can in most situation be seen as an error indication
     * (as most occurances of integers in the grammar are unsigned integers) but not in
     * general. Where the grammar permits signed values the -1 can be the legal numeric
     * value. In this case the client of the method doesn't get a direct error
     * notification. He could compare the global error counter prior and after the call of
     * this method.
     *   @param token
     * The Token object as returned by the parser.
     */
    private int getIntegerFromToken(Token token)
    {
        return getIntegerFromText(errorContext(token), token.getText());

    } /* End of DataModelListener.getIntegerFromToken */





    /**
     * Get the numeric value from a char string returned by the parser and do the
     * appropriate error handling.\n
     *   The parser reads an integer as a sequence of digits of any length. This listener
     * uses the Java type long for data modelling, which means a range limitation. If the
     * network database file should contain an integer outside of this range than an error
     * is raised by this method although it is syntactically correct.
     *   @return
     * The value as a long or -1 in case of errors.\n
     *   Error reporting is done by logging the message and incrementing the global error
     * counter.\n
     *   Caution, the return value -1 can in most situation be seen as an error indication
     * (as most occurances of integers in the grammar are unsigned integers) but not in
     * general. Where the grammar permits signed values the -1 can be the legal numeric
     * value. In this case the client of the method doesn't get a direct error
     * notification. He could compare the global error counter prior and after the call of
     * this method.
     *   @param errorContext
     * A string prepending a possible error message. Should indicate where in the input the
     * problematic number conversion appeared.
     *   @param numAsText
     * The character string, e.g. fetched from the parse tree.
     */
    private long getLongIntegerFromText(String errorContext, String numAsText)
    {
        long longVal = -1;
        try
        {
            longVal = Long.parseLong(numAsText);
        }
        catch(NumberFormatException e)
        {
            /* The only possible reason for this exception is a range violation. */
            errCnt_.error();
            _logger.error(errorContext
                          + "Invalid integer found. The supported range of [-2^63, 2^63-1]"
                          + " is exceeded"
                         );
            longVal = -1;
        }

        return longVal;

    } /* End of DataModelListener.getLongIntegerFromText */





    /**
     * Get the numeric value from the parser's Integer Token or SignedIntegerContext
     * object.\n
     *   The parser reads an integer as a sequence of digits of any length. This listener
     * uses the Java type long for data modelling, which means a range limitation. If the
     * network database file should contain an integer outside of this range than an error
     * is raised by this method although it is syntactically correct.
     *   @return
     * The value as an int or -1 in case of errors.\n
     *   Error reporting is done by logging the message and incrementing the global error
     * counter.\n
     *   Caution, the return value -1 can in most situation be seen as an error indication
     * (as most occurances of integers in the grammar are unsigned integers) but not in
     * general. Where the grammar permits signed values the -1 can be the legal numeric
     * value. In this case the client of the method doesn't get a direct error
     * notification. He could compare the global error counter prior and after the call of
     * this method.
     *   @param ctx
     * The context object as returned by the parser.
     */
    private long getLongIntegerFromContext(DbcParser.SignedIntegerContext ctx)
    {
        return getLongIntegerFromText(errorContext(ctx), ctx.getText());

    } /* End of DataModelListener.getLongIntegerFromContext */





    /**
     * Get a string from a grammar token String.
     *   The grammar token String is a lexer token with no further, inner structure. In
     * particular, the string contents can't be distinguished from the string's contents.
     * This function strips the quotes.<p>
     *   The string contents after stripping the quotes is "trim"-ed, white space at both
     * ends is removed.
     *   @return
     * Get the string in the selected format.
     *   @param token
     * The parsing result, a token.
     *   @param asJavaString
     * If the string is needed as Java String then the returned string contents are
     * searched for non printable characters. Sequences of such are replaced by a single
     * dot.
     */
    private String getStringFromToken(Token token, boolean asJavaString)
    {
        String text = token.getText();
        assert text.length() >= 2: "Unexpected token length for String";
        text = text.substring(1, text.length()-1).trim();
        if(asJavaString)
        {
            /// @todo Complete function. Replace dot by \ooo representation. Consider
            // different target languages. C needs other escaping than HTML or *.m. Can
            // become a user option and the default could be derived from the extension of
            // the generated output file. If an option then individual to each output file

            /* The function removes non printable characters to make generated code safe.
               Each omission is indicated by an inserted dot. (C code must not contain non
               ASCII codes). */
            text = text.replaceAll("[^\\p{Print}||['\\\"]]+", ".");
        }
        return text;

    } /* End of DataModelListener.getStringFromToken */





    /**
     * Get an attribute name from a grammar token String.
     *   The grammar token String is a lexer token with no further, inner structure. This
     * token is sometimes used to hold an attribute name. This function extracts the
     * attribute name.<p>
     *   Getting the attribute name is basically stripping the quotes from the string
     * token. However, due to tolerating typically syntax errors in a DBC file we do some
     * corrective actions, too.
     *   @return
     * Get the attribute name, (after possible corrections) it is a valid identifier.
     *   @param token
     * The parsing result, a token.
     */
    private String getAttribNameFromToken(Token token)
    {
        String attribName = getStringFromToken(token, /* asJavaString */ false);

        /* Many real existing DBC files use the hyphen as character in attribute names.
           This is not correct but we tolerate it by correcting the name. A warning is not
           emitted here, this has already been done during the semantic check walk. */
        attribName = attribName.replaceAll("-", "_");
        return attribName;

    } /* End of DataModelListener.getAttribNameFromToken */





    /**
     * {@inheritDoc}<p>
     * The list of node names has been parsed and can be stored in our data model.
     */
    @Override public void exitNodes(DbcParser.NodesContext ctx)
    {
        if(ctx.nodeList.size() > 0)
        {
            assert bus_.nodeAry == null;
            bus_.nodeAry = new Node[ctx.nodeList.size()];
            int i = 0;
            for(Token tok: ctx.nodeList)
            {
                Node n = new Node();
                n.name = tok.getText();
                n.i0 = i;
                n.i = i + 1;
                bus_.nodeAry[i++] = n;

                /* We need to update a map of all nodes: The comments and attribute values
                   are parsed later and they will be associated by name. */
                if(mapNodeByName_.put(n.name, n) != null)
                {
                    errCnt_.warning();
                    _logger.warn( errorContext(tok) + "Node name " + n.name
                                  + " is doubly defined"
                                );
                }
            }
        }
    } /* End of exitNodes */



    /**
     * Parse the (extended) CAN identifier.
     *   Although basically an integer is the extended identifier of a CAN message
     * different. It is distinguished from normal IDs by a set bit 31 but the value of this
     * bit is not part of the ID. This method decodes the ID in both cases.
     *   @return Get the CAN ID. The standard ID 0 is returned in case of errors.
     *   @param numberText The ID as got as text from the parser. Actually a decimal number.
     */
    private CanId parseCanId(String numberText)
    {
        /* We use 64 Bit numbers for parsing the string representation: Extended IDs use
           unsigned integers up to 0x81fffff for their representtaion in the DBC file and
           these numbers can't be parsed as Java int. */
        long extId;
        try
        {
            extId = Long.parseLong(numberText);
        }
        catch(NumberFormatException e)
        {
            /* The only possible reason for this exception is a range violation. */
            errCnt_.error();
            _logger.error(errorContext_
                          + "Invalid frame ID found. The supported range of [-2^63, 2^63-1]"
                          + " is exceeded"
                         );
            extId = 0;
        }

        long id = extId & ~0x0000000080000000l;
        final boolean isExtId = (extId & 0x0000000080000000l) != 0;

        boolean isWarning = false;
        if(isExtId && (id & 0xffffffffe0000000l) != 0
           ||  !isExtId && (id & 0xfffffffffffff800l) != 0
          )
        {
            /* Invalid IDs do appear in real network databases - we encounter them for
               unused messages -, so we should be tolerant. If the further processing is
               possible with the found (but invalid) value then we rate the problem a
               warning only. Further processing means that the result fits into an int. */
            isWarning = (extId & 0xffffffff00000000l) == 0;

            /* Emit a message only once for each bad ID. */
            if(!setOfBadCanIDs_.contains(Long.valueOf(extId)))
            {
                final String errMsg = errorContext_ + "Invalid frame ID " + numberText
                                      + " (" + String.format("0x%x", extId)
                                      + ") found. The encoded ID " + id + " ("
                                      + String.format("0x%x", id)
                                      + ") is out of range of "
                                      + (isExtId? "a 29": "an eleven")
                                      + " Bit unsigned integer";
                if(isWarning)
                {
                    errCnt_.warning();
                    _logger.warn(errMsg);
                }
                else
                {
                    errCnt_.error();
                    _logger.error(errMsg);
                }
                setOfBadCanIDs_.add(Long.valueOf(extId));
            }

            if(!isWarning)
                id = 0;
        }

        assert(!isWarning &&  (int)id >= 0  &&  id <= 0x1fffffff
               ||  isWarning
                   &&  id >= (long)Integer.MIN_VALUE  &&  id <= (long)Integer.MAX_VALUE
              );
        return new CanId((int)id, isExtId);

    } /* End of parseCanId */



    /**
     * {@inheritDoc}<p>
     * Found next frame: Create a frame object. This object is the new context for
     * subsequent invokations of the signal listeners.<p>
     *   Specific to the CAN network database: Frame and PDU are considered the same. Therefore
     * we actually shape a PDU object.
     */
    @Override public void enterMsg(DbcParser.MsgContext ctx)
    {
        assert(pdu_ == null);

        /// @todo Cleanup error context handling: local to routine only?
        //String oldErrorContext = errorContext_;
        errorContext_ = "Bus " + bus_.name + ", " + ctx.name.getText() + ": ";
        _logger.debug(errorContext_ + "Enter frame");

        /* The exceptions from the string conversion are purposily now caught: The got text has
           been validated as string by the parser; if we run into a conversion problem than
           this is a true internal implementation error, worth an exception. */
        assert(ctx.id != null);
        assert(ctx.length != null);

        final String name = ctx.name.getText();
        final CanId canId = parseCanId(ctx.id.getText());

        if(busDescription_.isFrameSupported(name, canId.id_))
        {
            /* For CAN PDU and frame are identical. */
            pdu_ = new Pdu();
            pdu_.name = ctx.name.getText();
            pdu_.id = canId.id_;
            pdu_.isExtId = canId.isExtId_;
            pdu_.size = getIntegerFromToken(ctx.length);
            pdu_.offsetInFrame = 0;

            /* The implementation of the parser permits any size of PDU. The only
               limitation are memory consumption and computation time. Regardless, we
               restrict the size of a PDU to the maximum CAN frame size of 64 Byte. This
               means valuable feedback to the user and it makes the code less vulnerable
               against heavily corrupted input data. */
            if(pdu_.size > Pdu.maxSize)
            {
                errCnt_.error();
                _logger.error(errorContext_ + "PDU size " + pdu_.size
                              + " Byte exceeds the maximum supported limit of "
                              + Pdu.maxSize + " Byte. Note, the size will be reduced to the"
                              + " maximum, which can evoke subsequent error messages"
                             );
                pdu_.size = Pdu.maxSize;
            }

            /* Two arrays are initialized, which are used to determine, which bytes and
               bits of the PDU are occupied by its signals. */
            assert pdu_mapOfBitSetsOfMuxSigs_.size() == 0;
            pdu_setOfSigBytes_ = new boolean[pdu_.size];
            pdu_setOfMuxedSigBytes_ = new boolean[pdu_.size];

            /* Sender can be not set in the network file. */
            if(ctx.sender != null)
            {
                pdu_.sender = ctx.sender.getText();
                pdu_.isSent = pdu_.sender.compareTo(busDescription_.me) == 0;
            }
            else
            {
                pdu_.sender = null;
                pdu_.isSent = false;
            }
            /// @todo Should we have the union of all signal receiver lists as receiver list in the PDU?
        }
        else
        {
            _logger.info(errorContext_ + "Frame " + canId.toStringLong()
                         + " is excluded from processing"
                        );

            /** @todo We should add a list of excluded frames to the data model, this is
                useful for reporting purposes. Just let the frame be processed as normal
                but put it in exitMsg into the other list. Error msg in exitSignal on
                pdu_==null needs to be changed and attribute/comment handling needs to
                consider the second frame list for addressing to these frames. (Maybe this
                is not needed because of these frames also being in the maps, TBC) */
        }
    } /* End of enterMsg */



    /**
     * Support of residual bus simulation: The transmission direction of a PDU can
     * be inverted. The generated code shall send a frame, which is actually
     * specified a received one in the network database. Such code is required for
     * testing the node in question.
     *   @remark
     * The inversion applies to the currently parsed PDU, found in {@link #pdu_}. Parsing of
     * this PDU needs to be completely done.
     *   @remark
     * The inversion only relates to the fields isSent and isReceived of PDU and signals.
     * The textual information about senders and receivers is not affected. In this sense
     * is the resulting data model inconsistent.
     */
    private void invertPduTransmissionDirection()
    {
        if(pdu_.isSent || pdu_.isReceived)
        {
            final boolean isSigReceivedNew = pdu_.isSent;
            if(!pdu_.isSent || !pdu_.isReceived)
            {
                /* If the transmission direction is simple and undirectional we can
                   exchange the values of both flags. */
                final boolean isSent = pdu_.isReceived;
                pdu_.isReceived = pdu_.isSent;
                pdu_.isSent = isSent;

                /* The signal flags strictly follows the found PDU flags. If the real node
                   receives the PDU (i.e. a sub-set of its signals) then it is now sent
                   (i.e. by the testing node) and no signal are received by the testing
                   node.
                     If the real node sends the PDU then the testing node now needs to
                   receive all signals in order to do an all-embracing residual bus
                   simulation. */

                _logger.debug(errorContext_ + "Inverted transmission direction is applied to"
                              + " this frame"
                             );
            }
            else
            {
                assert pdu_.isSent && pdu_.isReceived;

                /* Loop back frames, which are sent by a node and immediately read back,
                   differ. They just need to be received by the testing node. While the
                   original information may specify a sub-set of signals for reception will
                   the testing node need to receive all signals (as they are all sent by
                   the node under test and my affect the residual bus simulation). */
                pdu_.isSent = false;

                _logger.debug(errorContext_ + "Inversion of transmission direction can't"
                              + " be applied to this loopback frame. It is marked for"
                              + " reception"
                             );
            }

            /* Overwrite the signal reception flag. All cases resolve into the same: all
               signals are treated identical and the flag's value is the negated send flag
               of the real node. */
            if(pdu_.signalAry != null)
            {
                for(Signal s: pdu_.signalAry)
                    s.isReceived = isSigReceivedNew;
            }
            if(pdu_.muxSelector != null)
                pdu_.muxSelector.isReceived = isSigReceivedNew;
            if(pdu_.muxSignalSetAry != null)
            {
                for(Pdu.MultiplexedSignalSet sigSet: pdu_.muxSignalSetAry)
                {
                    if(sigSet.signalAry != null)
                    {
                        for(Signal s: sigSet.signalAry)
                            s.isReceived = isSigReceivedNew;
                    }
                    else
                        assert(false);
                }
            }
        }
        else
        {
            /* Frame, which do not at all affect the node in question are not changed. They
               stay unaffected. */
            _logger.debug(errorContext_ + "Frame is not processed by node "
                          + busDescription_.me + ". Inversion of transmission direction"
                          + " doesn't apply"
                         );
        }
    } /* End of invertPduTransmissionDirection */



    /**
     * {@inheritDoc}<p>
     * Frame/PDU parsing complete: Create a frame object, embedd the completed PDU object and
     * add it to the bus.
     */
    @Override public void exitMsg(DbcParser.MsgContext ctx)
    {
        assert(ctx.name != null);

        _logger.debug(errorContext_ + "Exit frame");

        /* If this frame is not supported then pdu_ is null. */
        if(pdu_ != null)
        {
            /* Bring the signals in a defined order. By default we sort according to the
               location in the PDU. */
            if(pdu_.signalAry != null)
            {
                Collections.sort(pdu_.signalAry);

                /* Reassign the linear index of all signals in the list. */
                int i = 0;
                for(Signal s: pdu_.signalAry)
                {
                    s.i0 = i;
                    s.i  = i + 1;
                    i    = s.i;
                }
            }

            /* Do the same for all sets of multiplexed signals. */
            if(pdu_.muxSignalSetAry != null)
            {
                /* Sort the signals in the set according to the location in the PDU. */
                for(Pdu.MultiplexedSignalSet muxSignalSet: pdu_.muxSignalSetAry)
                {
                    if(muxSignalSet.signalAry != null)
                    {
                        Collections.sort(muxSignalSet.signalAry);

                        /* Reassign the linear index of all signals in the list. */
                        int i = 0;
                        for(Signal s: muxSignalSet.signalAry)
                        {
                            s.i0 = i;
                            s.i  = i + 1;
                            i    = s.i;
                        }
                    }
                    else
                        assert(false);
                }

                /* Sort the signal sets. The sort criterion is defined in the method {@link
                   Pdu.MultiplexedSignalSet#compareTo}. By default it is the order of
                   ascending multiplex selector values. */
                Collections.sort(pdu_.muxSignalSetAry);
            }


            /* Evaluate the information about bits and bytes of the PDU's data area in
               use. The result is the set of bytes, which need to be initialized to 0 in
               the generated pack operation. */
            int i
              , j = 0;
            int[] setOfBytesToInit = new int[pdu_.size];
            for(i=0; i<pdu_.size; ++i)
            {
                if(pdu_setOfMuxedSigBytes_[i] || !pdu_setOfSigBytes_[i])
                    setOfBytesToInit[j++] = i;
            }
            assert pdu_.idxByteInitialNullAry == null;
            if(j > 0)
            {
                pdu_.idxByteInitialNullAry = new int[j];
                for(i=0; i<j; ++i)
                    pdu_.idxByteInitialNullAry[i] = setOfBytesToInit[i];
            }
            pdu_mapOfBitSetsOfMuxSigs_.clear();
            pdu_setOfSigBytes_ = null;
            pdu_setOfMuxedSigBytes_ = null;

            /* Do some basic multiplexing related validation. */
            if(pdu_.muxSelector == null  &&  pdu_.muxSignalSetAry != null)
            {
                errCnt_.error();
                _logger.error(errorContext(ctx)
                              + "Message " + pdu_.name
                              + " contains alternative, multiplexed signals"
                              + " but no other signal is specified as multiplex selector"
                             );
            }
            else if(pdu_.muxSelector != null  &&  pdu_.muxSignalSetAry == null)
            {
                errCnt_.warning();
                _logger.warn(errorContext(ctx)
                             + "Message " + pdu_.name
                             + " has a signal, which is specified a multiplex"
                             + " selector but there are no alternative, multiplexed signals"
                            );
            }
            pdu_mapMuxSignalSetByMuxValue_.clear();

            /* Support of residual bus simulation: The transmission direction of a PDU can
               be inverted. The generated code shall send a frame, which is actually
               specified a received one in the network database. Such code is required for
               testing the node in question. */
            if(busDescription_.invertTransmissionDirection)
                invertPduTransmissionDirection();

            Frame frame = new Frame();

            /* For CAN PDU and frame are identical. */
            frame.name = pdu_.name;
            frame.id = pdu_.id;
            frame.isExtId = pdu_.isExtId;
            frame.size = pdu_.size;
            frame.sender = pdu_.sender;
            frame.isSent = pdu_.isSent;
            frame.isReceived = pdu_.isReceived;

            /* A CAN frame is restricted to a set of 16 allowed sizes. Which one out of the
               16 is the Data Length Code. */
            boolean invalidSize;
            int DLC = -1;
            if(frame.size >= 0  &&  frame.size <= 8)
            {
                DLC = frame.size;
                invalidSize = false;
            }
            else if(frame.size <= 24)
            {
                DLC = 8 + (frame.size-8)/4;
                invalidSize = (frame.size-8) % 4 != 0;
            }
            else if(frame.size <= 32)
            {
                DLC = 13;
                invalidSize = frame.size != 32;
            }
            else if(frame.size <= 64)
            {
                DLC = 13 + (frame.size-32)/16;
                invalidSize = (frame.size-32) % 16 != 0;
            }
            else
                invalidSize = true;

            if(invalidSize)
            {
                assert frame.DLC == null: "Initialization error";
                _logger.info(errorContext(ctx)
                             + "Message " + frame.name
                             + " has a size of " + frame.size + " Byte. This is not a valid"
                             + " CAN frame size and could point to an error unless the"
                             + " message belongs to a transport protocol like in J1939."
                             + " The CAN specification allows message sizes of 0..8 Byte for"
                             + " ordinary frames and also 12, 16, 20, 24, 32, 48 and 64 Byte"
                             + " for CAN FD frames"
                            );
            }
            else
                frame.DLC = Integer.valueOf(DLC);

            assert frame.DLC == null  ||  frame.DLC >= 0  &&  frame.DLC <= 15
                   : "Invalid DLC computed";

            /* The frame contains a single PDU (CAN). */
            pdu_.i0 = 0;
            pdu_.i = 1;
            frame.pduAry = new Pdu[1];
            frame.pduAry[0] = pdu_;
            pdu_ = null;

            if(bus_.frameAry == null)
                bus_.frameAry = new ArrayList<>();
            frame.i0 = bus_.frameAry.size();
            frame.i = frame.i0 + 1;
            bus_.frameAry.add(frame);
            mapFrameById_.put(new CanId(frame.id, frame.isExtId), frame);

            /* Let's check if the class CanId and its method equals work appropriately. */
            assert frame == mapFrameById_.get(new CanId(frame.id, frame.isExtId))
                   : "Map of frames doesn't work";
        }
        else
        {
            assert pdu_mapMuxSignalSetByMuxValue_.size() == 0;
        }

        errorContext_ = "Bus " + busDescription_.name + ": ";

    } /* End of exitMsg */




    /**
     * Determine the best suiting implementation type for a given signal.
     *   @return
     * Get the type name as string, like uint8_t.
     *   @param isSigned
     * Boolean statement if the signal is signed.
     *   @param length
     * The length of the signal in Bit.
     */
    private String getImplType(boolean isSigned, int length)
    {
        /* It doesn't matter that we use hard coded names for the basic types. The concept
           of using StringTemplate for rendering the information has the concept of
           translation maps to make any wanted set of type strings from the set imposed
           here. */
        if(length <= 0  ||  length > 64  || (length == 1  && isSigned))
        {
            errCnt_.error();
            _logger.error(errorContext_ + "Invalid signal type or signal length ("
                          + length + " Bit, " + (isSigned? "signed": "unsigned") + ")."
                          + (isSigned? " A valid signed signal has 2 .. 64 Bit"
                                     : " A valid unsigned signal has 1 .. 64 Bit"
                            )
                         );

            /* This might be used by the information rendering process to produce a compile
               time error in the generated output. */
            return "invalidNumericBaseType_t";
        }

        if(isSigned)
        {
            if(length <= 8)
                return "sint8_t";
            else if(length <= 16)
                return "sint16_t";
            else if(length <= 32)
                return "sint32_t";
            else
            {
                assert length <= 64: "Invalid case > 64";
                return "sint64_t";
            }
        }
        else
        {
            if(length <= 1)
                return "bool_t";
            else if(length <= 8)
                return "uint8_t";
            else if(length <= 16)
                return "uint16_t";
            else if(length <= 32)
                return "uint32_t";
            else
            {
                assert length <= 64: "Invalid case > 64";
                return "uint64_t";
            }
        }
    } /* End of DataModelListener.getImplType */





    /**
     * Transform a bit position from the standard grid into the other grid (linear
     * bit positions for Motorola signals) and vice versa.
     *   @return
     * Get the same bit position but expressed in the other grid.
     *   @param bitIdx
     * The bit position to transform.
     */
    private int transformGrid(int bitIdx)
    {
        /* The size in Bit of a byte. */
        final int L = 8;

        return L*(bitIdx/L) + (L-1)-(bitIdx%L);

    } /* End of DataModelListener.transformGrid */





    /**
     * Check if a signal is a signal of special interest to the user.
     *   A list of special signals can be specified as application parameter. If such a
     * signal is found it is directly accessible from within the template. Intended to
     * support checksum handling etc.
     *   @param s
     * The parsed, to be tested signal.
     */
    private void checkSignalForSpecialSignal(Signal s)
    {
        /* Check if the signal is a signal of special interest. */
        for(ParameterSet.SpecialSignalRequest req: busDescription_.specialSignalMap.values())
        {
            /* The request can be based on an exact signal name match or on a regular
               expression matching. */
            if(req.reSpecialSignal == null  &&  s.name.compareTo(req.name) == 0
               ||  req.reSpecialSignal != null  &&  s.name.matches(req.reSpecialSignal)
              )
            {
                /* The signal is a demanded special signal. */
                _logger.debug(errorContext_ + " The filter (" + req.name + ", "
                              + (req.reSpecialSignal != null? req.reSpecialSignal
                                                            : "<no regexp given>"
                                )
                              + ") matches signal " + s.name
                             );

                /* Add the pair (specialSignalName, true) to the querying map "is". The
                   template can use a construct like
                   <if(s.is.checkSum)>// Checksum evaluation on signal<endif> */
                if(s.is == null)
                    s.is = new HashMap<String,Boolean>(1);
                s.is.put(req.name, Boolean.valueOf(true));

                /* Add the signal to the map but double-check that there's no other
                   matching signal. Ambiguities can't be handled from a template and need
                   to be rated as blocking error. The user is in charge to refine his
                   filter criteria. */
                if(pdu_.specialSignalMap == null)
                    pdu_.specialSignalMap = new HashMap<String,Signal>();
                Signal firstMatchingSignal = pdu_.specialSignalMap.put(req.name, s);
                if(firstMatchingSignal != null)
                {
                    errCnt_.error();
                    _logger.error(errorContext_
                                  + " The filter (" + req.name + ", "
                                  + (req.reSpecialSignal != null? req.reSpecialSignal
                                                                : "<no regexp given>"
                                    )
                                  + ") is ambiguous. The previous match was "
                                  + firstMatchingSignal.name + ". Please refine your filter"
                                  + " in order to unambiguously select up to one signal only"
                                 );
                }
            } /* End if(This signal matches against one of the user provded filters.) */

            /* Don't have a break here. It might be useless but it's not a problem if the
               user has several filters characterizing the same signal as special signal. A
               single signal might be special for the user because of different aspects. */

        } /* End for(All user demands for special signals) */

    } /* End of DataModelListener.checkSignalForSpecialSignal */




    /**
     * Test if some of the bits of a byte are in a given set of bits. Used to detect
     * overlap of signals in a PDU. An error message is written if an overlap is detected.
     *   @return
     * <b>true</b>, if all the passed bits don't belong to the set, i.e. if there's no
     * overlap of signals, else <b>false</b>.
     *   @param setOfBits
     * A byte array which represents the bit set. Or null, in which case nothing is done at
     * all.
     *   @param idxByte
     * The set of bits is represented by an array byte. The tested bits correspond with
     * byte of this index.
     *   @param mask
     * A byte of bits. These bits are tested and should not be in the bit set so far.
     */
    private boolean testBitsInSet(int[] setOfBits, int idxByte, int mask)
    {
        if(setOfBits != null  &&  (setOfBits[idxByte] & mask) != 0)
        {
            errCnt_.error();
            _logger.error(errorContext_
                          + "This signal overlaps with another one. The two signals"
                          + " share at least one bit. Safe signal en- and decoding"
                          + " is impossible"
                         );
            return false;
        }
        else
            return true;

    } /* End of DataModelListener.testBitsInSet */





    /**
     * Set some bits of the PDU's data area as occupied.
     *   Invoke this function with all signal masks of all bytes of any signal of a PDU.
     * It'll collect this information to find out, whether there are overlapping signals
     * and which bytes are completely written so that they don't need to be initialized by
     * the PDU pack function. A parse error is raised in case of overlapping signals.
     *   @param idxByte
     * The index of the byte in the PDU's data area.
     *   @param mask
     * The mask of bits in use from a signal.
     *   @param muxValue
     * The multiplex selector value for this signal if it would be a multiplexed signal or
     * -1 for a normal signal.
     *   @remark
     * The analysis is not fully done for multiplexed signals. Here, it's evident that
     * there will be overlapping signals. The normal analysis needed to be done for each
     * multiplexed signal and the need-initialization would become the worst case result.
     * However, the additional effort for this full scale analysis won't pay off as
     * multiplexed signals are barely used and the optimization benefit is little for the
     * generated code.
     */
    private void markSignalBitsAsOccupied(int idxByte, int mask, int muxValue)
    {
        assert pdu_ != null  &&  mask <= 255;

        if(idxByte < pdu_.size)
        {
            if(muxValue < 0)
            {
                /* It's a normal signal. */

                /* Which bytes need initialization (in the pack functions): Bytes which are
                   entirely inside a single signal will be written by assignment and don't
                   require initialization. Remind those bytes. */
                if(mask == 0xff)
                    pdu_setOfSigBytes_[idxByte] = true;

                /* Detect signal overlap: Check new bits against all bit sets, i.e. the set
                   of bits of normal signals and such a set for each alternative,
                   multiplexed signal set. */
                for(int[] setOfBits: pdu_mapOfBitSetsOfMuxSigs_.values())
                {
                    /* if-break: Basically we could always complete the loop in order to
                       find multiple overlap problems, but the error message is too
                       unspecific for this purpose. It doesn't make any sense to get this
                       message repeatedly. */
                    if(!testBitsInSet(setOfBits, idxByte, mask))
                        break;
                }
            }
            else
            {
                /* Which bytes need initialization (in the pack function): Multiplexed
                   signals are not checked on bit granularity to find the bytes that need
                   initialization. Instead, these bytes are just recorded for "need
                   initialization" as they might be written by some but not all of the
                   multiplexed signals. */
                pdu_setOfMuxedSigBytes_[idxByte] = true;

                /* Detect signal overlap: Multiplexed signals need to be checked against
                   normal signals and against their own multiplexed set of signals. (They
                   will probably overlap with many of the signals from other alternative
                   signal sets, which is not considered an error.)
                     Both bit sets to check against don't need to exist yet, this could be
                   the first byte of the first signal of the signal set. */
                testBitsInSet(pdu_mapOfBitSetsOfMuxSigs_.get(-1), idxByte, mask);
                testBitsInSet(pdu_mapOfBitSetsOfMuxSigs_.get(muxValue), idxByte, mask);
            }

            /* The new bits are now added to the related bit set for future tests of other
               signals and their bytes. */
            int[] setOfBits = pdu_mapOfBitSetsOfMuxSigs_.get(muxValue);
            if(setOfBits != null)
            {
                assert idxByte < setOfBits.length;
                setOfBits[idxByte] |= mask;
            }
            else
            {
                /* This is the first byte of the first signal from the set. Start a new set
                   of occupied bits. */
                setOfBits = new int[pdu_.size];
                setOfBits[idxByte] = mask;
                pdu_mapOfBitSetsOfMuxSigs_.put(muxValue, setOfBits);
            }
        }
        else
        {
            /* This case can be entered due to errors in the network database. These
               problems have already been reported before. No error message needed here
               again. */
            _logger.debug(errorContext_
                          + "The signal bytes are out of range of the containing PDU and"
                          + " can't be considered for overlap detection"
                         );
        }
    } /* End of DataModelListener.markSignalBitsAsOccupied */



    /**
     * {@inheritDoc}<p>
     * Signal parsing complete.
     */
    @Override public void exitSignal(DbcParser.SignalContext ctx)
    {
        assert ctx.name != null: "Context not available";

        String oldErrorContext = errorContext_;
        errorContext_ = "Bus " + bus_.name + ", " + ctx.name.getText() + ": ";
        _logger.debug(errorContext_ + "Exit signal");

        /* The grammar rule signal matches for valid frames and for pseudo-messages
           (VECTOR__INDEPENDENT_SIG_MSG). We ignore the parsing result in the latter case. */
        if(pdu_ == null)
        {
            _logger.debug(errorContext_
                          + "Signal belongs either to an excluded frame or to a"
                          + " VECTOR__INDEPENDENT_SIG_MSG pseudo frame;"
                          + " it is ignored"
                         );
            errorContext_ =  oldErrorContext;
            return;
        }

        assert pdu_ != null: "PDU object not available";
        errorContext_ = "Bus " + bus_.name + ", " + pdu_.name + ", " + ctx.name.getText()
                        + ": ";

        /* The exceptions from the string to int conversions are purposely now caught: The
           got text has been validated by the parser; if we run into a conversion problem
           than this is a true internal implementation error, worth an exception. */

        boolean isMuxSelector = false
              , isMuxedSignal = false;
        int muxValue = -1;
        if(ctx.mpxIndicator != null)
        {
            assert ctx.mpxIndicator.getText().length() >= 1: "Invalid mpx indicator token";
            if(ctx.mpxIndicator.getText().length() == 1)
            {
                assert ctx.mpxIndicator.getText().compareTo("M") == 0: "Expected M";
                isMuxSelector = true;
            }
            else
            {
                isMuxedSignal = true;
                muxValue = getIntegerFromText( errorContext(ctx.mpxIndicator)
                                             , ctx.mpxIndicator.getText().substring(1)
                                             );
            }
        }

        Signal s = new Signal();
        s.name = ctx.name.getText();
        s.desc = null;
        s.length = getIntegerFromToken(ctx.length);
        s.startBit = getIntegerFromToken(ctx.startBit);
        assert ctx.byteOrder.getText().length() == 1: "Invalid byte order token";
        s.isMotorola = ctx.byteOrder.getText().compareTo("0") == 0;
        s.isSigned = ctx.signed.getText().compareTo("-") == 0;
        s.isMuxSelector = isMuxSelector;
        s.isMuxedSignal = isMuxedSignal;
        s.muxValue = muxValue;

        final int L = 8;
        final int N = L*pdu_.size;

        /* The validation of the found signal position information and the derived
           information can be done in different bit index grids. Depending on which piece
           of information and which byte order we have the one or the other grid is
           advantageous. transformGrid transforms from one to the other grid.
             The "normal" grid as used for the start bit index in the network database file
           counts from lower to higher byte addresses and from the LSB to the MSB of
           the bytes. The other grid counts from lower to higher byte addresses and from
           the MSB to the LSB of the bytes. */
        final int sb = s.startBit
                , sb_T = transformGrid(s.startBit);

        /* Validation of found signal position information.
             Motorola format requires transformation of start bit into other grid so
           that signal bits occupy successive indexes as naturally without
           transformation for Intel format. */
        if((s.isMotorola? sb_T: sb)+s.length > N)
        {
            _logger.error(errorContext_
                          + "Invalid signal position, signal is out of range of PDU"
                         );
            errCnt_.error();
        }

        /* Add the list of bytes, which the signal is located in. The order of the list is
           always from most to least significant byte. */
        if(s.length > 0)
        {
            int firstByteIdx;
            int lastByteIdx;
            int i;
            if(s.isMotorola)
            {
                firstByteIdx = sb_T / L;
                lastByteIdx = (sb_T+s.length-1) / L;
                s.idxByteLSB = lastByteIdx;
                s.idxBitInByteLSB = transformGrid(sb_T+s.length-1) - L*lastByteIdx;
                assert lastByteIdx >= firstByteIdx;
                s.byteAry = new int[lastByteIdx - firstByteIdx + 1];
                for(i=firstByteIdx; i<=lastByteIdx; ++i)
                    s.byteAry[i-firstByteIdx] = i;
            }
            else
            {
                firstByteIdx = sb / L;
                s.idxByteLSB = firstByteIdx;
                s.idxBitInByteLSB = sb - L*firstByteIdx;
                lastByteIdx = (sb+s.length-1) / L;
                assert lastByteIdx >= firstByteIdx;
                s.byteAry = new int[lastByteIdx - firstByteIdx + 1];
                for(i=lastByteIdx; i>=firstByteIdx; --i)
                    s.byteAry[lastByteIdx-i] = i;
            }

            /* Access type: Type of intermediate result, which can hold all the bytes,
               which are touched by the signal (prior to masking and shifting). */
            final int noAccBytes = lastByteIdx - firstByteIdx + 1;
            s.accType = getImplType(/* isSigned */ false, L*noAccBytes);

            /* Where are MSB and LSB of the signal in both grids? */
            int msb, msb_T, lsb, lsb_T;
            if(s.isMotorola)
            {
                /* Motorola: The start bit designates the MSB. */
                msb = sb;
                msb_T = sb_T;

                /* The LSB is the last bit of the sequence of bits. This sequence is linear
                   in the transformed grid. */
                lsb_T = msb_T + s.length - 1;
                lsb = transformGrid(lsb_T);
            }
            else
            {
                /* Intel: The start bit designates the LSB of the signal. */
                lsb = sb;
                lsb_T = sb_T;

                /* Intel: The MSB is the last bit of the sequence, which begins with the
                   start bit. */
                msb = sb + s.length - 1;
                assert msb < N  ||  errCnt_.getNoErrors() > 0;
                msb_T = transformGrid(msb);
            }

            /* Signed signals are first left shifted till they are left aligned and only
               then shifted back to the final right aligned state in order to get the
               needed sign propagation. */
            int shiftLeft;
            if(s.isSigned)
            {
                /* Number of bits to become left aligned in the byte. */
                shiftLeft = (L-1) - msb%L;

                /* The intermediate expression has a power of two size in Byte, not the
                   exactly needed number of bytes. The left shift must go across the
                   (unneeded but unavoidable) additional bytes.
                     The second condition of the while is needed to avoid an infinite loop
                   for heavily corrupted input data. Error reporting is not needed as
                   explicit size check has already been done before. */
                int sizeOfAccType = 1;
                while(sizeOfAccType < noAccBytes && sizeOfAccType != 0)
                    sizeOfAccType <<= 1;
                assert sizeOfAccType <= 8  ||  errCnt_.getNoErrors() > 0
                       : "Insufficient error reporting";
                shiftLeft += L*(sizeOfAccType - noAccBytes);

                if(shiftLeft > 0)
                    s.shiftLeft = Integer.valueOf(shiftLeft);
            }
            else
                shiftLeft = 0;

            /* The number of bits to shift right to get the signal right aligned. */
            int shiftRight = shiftLeft + lsb%L;
            if(shiftRight > 0)
                s.shiftRight = Integer.valueOf(shiftRight);

            s.maskAry = new Object[lastByteIdx - firstByteIdx + 1];
            for(i=firstByteIdx; i<=lastByteIdx; ++i)
            {
                /* Algorithm:
                     if MSB is in this byte
                         Set all right hand side bits incl MSB
                     else
                         Set all bits
                     if LSB is in this byte
                         Reset all right hand side bits excl LSB
                     if isMotorola
                         Write mask at array index i-firstByteIdx
                     else
                         Write mask at array index lastByteIdx-i
                   Constants:
                     1 << n: 2^n
                     (1 << L) - 1: 255 */

                final int firstBitOfByte = L*i
                        , lastBitOfByte = firstBitOfByte + (L-1);
                int mask;
                if(msb >= firstBitOfByte  &&  msb <= lastBitOfByte)
                {
                    /* mask = 2^(msb%8+1) - 1 */
                    mask = (1 << (msb%L+1)) - 1;
                }
                else
                    mask = (1 << L) - 1;

                if(lsb >= firstBitOfByte  &&  lsb <= lastBitOfByte)
                    mask -= (1 << lsb%L) - 1;

                /* Is masking of this byte required? Otherwise pass false to the template
                   to indicate "no". */
                Object maskInteger;
                if(mask < (1 << L) - 1)
                    maskInteger = Integer.valueOf(mask);
                else
                {
                    /* Here, inside the array we tend to not use the null object as usual
                       indication for false: The iteration in a template file along the
                       array elements using the colon purposely skips null elements. If we
                       place a Boolean false then the template can have conditional code on
                       relevant masks. */
                    maskInteger = Boolean.valueOf(false);
                }

                if(s.isMotorola)
                    s.maskAry[i-firstByteIdx] = maskInteger;
                else
                    s.maskAry[lastByteIdx-i] = maskInteger;

                /* Mark byte i in the PDU array of used bytes as no-init-needed if the mask
                   is 0xff. Run some tests on overlapping signals (which can raise a parse
                   error). */
                markSignalBitsAsOccupied(i, mask, isMuxedSignal? muxValue: -1);

            } /* for(All PDU bytes touched by the signal with at least one bit) */
        }
        else
        {
            assert s.length == 0: "Unexpected parse result";
            _logger.warn(errorContext_ + "Signal length null encountered");
            errCnt_.warning();
            s.accType = getImplType(/* isSigned */ false, L);
        }

        /// @todo Add fields factor_n, factor_d, offset_n and offset_d and provide rational approximation by sint16. See http://en.wikipedia.org/wiki/Continued_fraction for details
        s.factor = getNumberFromContext(ctx.factor);
        s.offset = getNumberFromContext(ctx.offset);
        s.isVoidFactor = s.factor == 1.0;
        s.isVoidOffset = s.offset == 0.0;
        s.isVoidScaling = s.isVoidFactor && s.isVoidOffset;
        s.min = getNumberFromContext(ctx.min);
        s.max = getNumberFromContext(ctx.max);
        s.unit = getStringFromToken(ctx.unit, /* asJavaString */ true);
        if(s.unit.length() == 0)
            s.unit = null;

        /* Only later during the walk through the parse tree we will find the extended type
           information on signals. For 32 or 64 Bit signals we might now decide on
           according integers but will learn later that it are floating point numbers. If
           so, the type will then be replaced accordingly. (The access type set before is
           not affected.) */
        s.type = getImplType(s.isSigned, s.length);

        if(ctx.recList.size() > 0)
        {
            s.isReceived = false;
            s.receiverAry = new String[ctx.recList.size()];
            Iterator<Token> it = ctx.recList.iterator();
            int i = 0;
            while(it.hasNext())
            {
                Token token = it.next();
                s.receiverAry[i++] = token.getText();
                if(token.getText().compareTo(busDescription_.me) == 0)
                    s.isReceived = true;
            }
        }
        else
        {
            /** @todo Should we assume that the signal is received if the receiver list is
                not set at all in the network database? Is this rather "still undefined" or
                rather "no, not received by anybody"? A heuristics could be the look at all
                signals: If none of them are specified then it's rather "still undefined"
                and we would assume "signal is received". */
            s.isReceived = false;
            s.receiverAry = null;
        }

        /* Add the signal for later retrieval to the map of all signals: Later a comment or
           an attribute value could be attached. */
        mapSignalByIdAndName_.put(Pair.create(new CanId(pdu_.id, pdu_.isExtId), s.name), s);

        /* Let's check if the class Pair and its method equals work appropriately. */
        assert s == mapSignalByIdAndName_.get(Pair.create( new CanId(pdu_.id, pdu_.isExtId)
                                                         , s.name
                                                         )
                                             )
               : "Map of signals doesn't work";

        /* Check if this signal is a signal of special interest. If so add it as such to
           the currently parsed PDU. */
        checkSignalForSpecialSignal(s);

        /* Add the new signal at the right context into the PDU object. */
        if(isMuxSelector)
        {
            if(pdu_.muxSelector == null)
            {
                s.i0 = 0;
                s.i = 1;
                pdu_.muxSelector = s;
            }
            else
            {
                _logger.error(errorContext_
                              + " Multiple signals are specified to be the multiplex"
                              + " selector. This is not supported by the code generator"
                             );
                errCnt_.error();
            }
        }
        else if(isMuxedSignal)
        {
            /* It is a multiplexed signal. We need to put it into the set of multiplexed
               signals related to the given switch value. Find out if there's already such
               a set. */
            Pdu.MultiplexedSignalSet set = pdu_mapMuxSignalSetByMuxValue_.get(muxValue);
            if(set == null)
            {
                set = new Pdu.MultiplexedSignalSet();
                set.muxValue = muxValue;
                set.signalAry = new ArrayList<Signal>();

                /* Add the new map to the lookup map. */
                pdu_mapMuxSignalSetByMuxValue_.put(muxValue, set);

                /* ... and to the data model. */
                if(pdu_.muxSignalSetAry == null)
                    pdu_.muxSignalSetAry = new ArrayList<Pdu.MultiplexedSignalSet>();
                set.i0 = pdu_.muxSignalSetAry.size();
                set.i = set.i0 + 1;
                pdu_.muxSignalSetAry.add(set);
            }
            assert set.muxValue == muxValue;
            set.addSignal(s);
        }
        else
        {
            /* Normal case: It is an ordinary signal. */
            if(pdu_.signalAry == null)
                pdu_.signalAry = new ArrayList<Signal>();
            s.i0 = pdu_.signalAry.size();
            s.i = s.i0 + 1;
            pdu_.signalAry.add(s);
        }

        if(s.isReceived)
            pdu_.isReceived = true;

        errorContext_ =  oldErrorContext;

    } /* End of exitSignal */



    /**
     * {@inheritDoc}<p>
     * A bus related comment has been parsed and can be stored in our data model.
     */
    @Override public void exitGlobalComment(DbcParser.GlobalCommentContext ctx)
    {
        /* If several comments address to the same bus then they are concatenated
           separated by a blank. */
        if(bus_.desc == null)
            bus_.desc = "";
        else
        {
            /** @todo Don't use a newline to separate the comments until we have a concept
                for safe text handling in the rendered output. A newline would e.g.
                invalidate generated Octave code, where a newline must not appear in a
                string literal. */
            bus_.desc += " ";
        }

        bus_.desc += getStringFromToken(ctx.text, /* asJavaString */ true);
        _logger.debug(errorContext(ctx.text) + "CM: Bus: " + bus_.name + ", comment: "
                      + bus_.desc
                     );
    } /* End of exitGlobalComment */



    /**
     * {@inheritDoc}<p>
     * A node related comment has been parsed and can be stored in our data model.
     */
    @Override public void exitNodeComment(DbcParser.NodeCommentContext ctx)
    {
        Node n = mapNodeByName_.get(ctx.nodeName.getText());
        if(n == null)
        {
            errCnt_.warning();
            _logger.warn(errorContext(ctx.nodeName) + "Comment relates to undefined node "
                         + ctx.nodeName.getText() + ". It is ignored"
                        );
            return;
        }

        /* If several comments address to the same node then they are concatenated
           separated by a blank. */
        if(n.desc == null)
            n.desc = "";
        else
        {
            /** @todo Don't use a newline to separate the comments until we have a
                concept for safe text handling in the rendered output. A newline would e.g.
                invalidate generated Octave code, where a newline must not appear in a
                string literal. */
            n.desc += " ";
        }
        n.desc += getStringFromToken(ctx.text, /* asJavaString */ true);
        _logger.debug(errorContext(ctx.nodeName) + "CM: Node: " + n.name + ", comment: "
                      + n.desc
                     );
    } /* End of exitNodeComment */



    /**
     * {@inheritDoc}<p>
     * A frame related comment has been parsed and can be stored in our data model.
     */
    @Override public void exitMsgComment(DbcParser.MsgCommentContext ctx)
    {
        Frame f = mapFrameById_.get(parseCanId(ctx.msgId.getText()));
        if(f == null)
        {
            /* No error message, we can have filtered out the addressed frame. */
            return;
        }

        /* If several comments address to the same frame then they are concatenated
           separated by a blank. */
        if(f.desc == null)
            f.desc = "";
        else
        {
            /** @todo Don't use a newline to separate the comments until we have a concept
                for safe text handling in the rendered output. A newline would e.g.
                invalidate generated Octave code, where a newline must not appear in a
                string literal. */
            f.desc += " ";
        }
        f.desc += getStringFromToken(ctx.text, /* asJavaString */ true);
        _logger.debug(errorContext(ctx.msgId) + "CM: Frame: " + f.name + ", comment: "
                      + f.desc
                     );
    } /* End of exitMsgComment */



    /**
     * {@inheritDoc}<p>
     * A signal related comment has been parsed and can be stored in our data model.
     */
    @Override public void exitSignalComment(DbcParser.SignalCommentContext ctx)
    {
        Signal s = mapSignalByIdAndName_.get(Pair.create( parseCanId(ctx.msgId.getText())
                                                        , ctx.signalName.getText()
                                                        )
                                            );
        if(s == null)
        {
            /* No error message, we can have filtered out the frame holding the addressed
               signal. */
            return;
        }

        /* If several comments address to the same signal then they are concatenated
           separated by a blank. */
        if(s.desc == null)
            s.desc = "";
        else
        {
            /** @todo Don't use a newline to separate the comments until we have a concept
                for safe text handling in the rendered output. A newline would e.g.
                invalidate generated Octave code, where a newline must not appear in a
                string literal. */
            s.desc += " ";
        }
        s.desc += getStringFromToken(ctx.text, /* asJavaString */ true);
        _logger.debug(errorContext(ctx.signalName) + "CM: Signal: " + s.name + ", comment: "
                      + s.desc
                     );
    } /* End of exitSignalComment */




    /**
     * {@inheritDoc}
     * Parse the enumeration of named signal values.
     */
    @Override public void exitValueDescription(DbcParser.ValueDescriptionContext ctx)
    {
        if(ctx.msgId != null)
        {
            assert ctx.signalName != null  &&  ctx.envVarName == null;

            /* Look for the signal the named values refer to. All signals are already known
               when named signal values appear the first time. */
            Signal s = mapSignalByIdAndName_.get(Pair.create( parseCanId(ctx.msgId.getText())
                                                            , ctx.signalName.getText()
                                                            )
                                                );
            if(s == null)
            {
                /* No error message, we can have filtered out the frame holding the addressed
                   signal. */
                return;
            }

            for(DbcParser.SingleValueDescriptionContext ctxSingleValDesc
                                                             : ctx.singleValueDescription()
               )
            {
                if(s.valueDescAry == null)
                    s.valueDescAry = new ArrayList<Signal.ValueDesc>();

                assert ctxSingleValDesc.value != null
                       &&  ctxSingleValDesc.description != null;

                String description = ctxSingleValDesc.description.getText();
                assert description.length() >= 2  &&  description.startsWith("\"")
                       &&  description.endsWith("\"");
                description = description.substring(1, description.length()-1);
                s.valueDescAry.add
                               (new Signal.ValueDesc
                                           ( description
                                           , getLongIntegerFromContext(ctxSingleValDesc.value)
                                           )
                               );
            }

            /* Sort the values according to ascending order of numeric value. */
            if(s.valueDescAry == null)
                Collections.sort(s.valueDescAry);
        }
        else
        {
            /* Environment variables are silently ignored. An according messages has already
               been emitted by the parser. */
        }
    } /* End of exitValueDescription */



    /**
     * {@inheritDoc}<p>
     * Evaluate the definition of an attribute.
     */
    @Override public void exitAttributeDefinition(DbcParser.AttributeDefinitionContext ctx)
    {
        AttributeDef attribDef = new AttributeDef();
        attribDef.name = getAttribNameFromToken(ctx.attribName);

        /* What kind of object does the attribute refer to? */
        if(ctx.objectType != null)
        {
            switch(ctx.objectType.getText())
            {
            case "BU_":
                attribDef.objType = AttributeDef.ObjectType.node;
                attribDef.isNode = true;
                break;

            case "BO_":
                attribDef.objType = AttributeDef.ObjectType.frame;
                attribDef.isFrame = true;
                break;

            case "SG_":
                attribDef.objType = AttributeDef.ObjectType.signal;
                attribDef.isSignal = true;
                break;

            case "EV_":
                errCnt_.warning();
                _logger.warn(errorContext(ctx) + "Attribute " + attribDef.name
                             + " is related to an evironment variable. Environment"
                             + " variables are however not supported by this application"
                            );
                attribDef.objType = AttributeDef.ObjectType.envVar;
                attribDef.isEnvVar = true;
                break;

            default:
                /* If we get here there would be an error in the parser. */
                assert false: "Invalid kind of attribute parsed";
            }
        }
        else
        {
            /* No explicit statement, it's a global, bus related attribute. */
            attribDef.objType = AttributeDef.ObjectType.bus;
            attribDef.isBus = true;
        }

        /* The grammar rule has an alternative for each possible data type of an attribute.
           Here we have an according if/elseif switch. */
        DbcParser.AttribTypeIntContext ctxTypeInt = ctx.attribTypeInt();
        DbcParser.AttribTypeStringContext ctxTypeString = ctx.attribTypeString();
        DbcParser.AttribTypeEnumContext ctxTypeEnum = ctx.attribTypeEnum();
        DbcParser.AttribTypeFloatContext ctxTypeFloat = ctx.attribTypeFloat();
        if(ctxTypeInt != null)
        {
            /* The grammar matched the definition of an integer attribute. */
            attribDef.isInt = true;
            attribDef.isHex = ctxTypeInt.type.getText().compareTo("HEX") == 0;
            attribDef.min = (double)getLongIntegerFromContext(ctxTypeInt.min);
            attribDef.max = (double)getLongIntegerFromContext(ctxTypeInt.max);
        }
        else if(ctxTypeString != null)
        {
            /* The grammar matched the definition of a string attribute. */
            attribDef.isString = true;
        }
        else if(ctxTypeEnum != null)
        {
            /* The grammar matched the definition of an enumeration attribute. */
            attribDef.isEnum = true;
            final int noVals = ctxTypeEnum.enumValList.size();
            assert noVals >= 1;
            attribDef.max = (double)(noVals-1);
            attribDef.enumValAry = new AttributeDef.EnumValueDef[noVals];
            int enumNum = 0;
            for(Token enumValToken: ctxTypeEnum.enumValList)
            {
                AttributeDef.EnumValueDef enumValDef = new AttributeDef.EnumValueDef();

                /* The textual representation of the enum value. */
                enumValDef.name = getStringFromToken(enumValToken, /* asJavaString */ false);

                /* The numeric representation of the enum value is implicitly defined by
                   order of appearance. */
                enumValDef.i0 = enumNum;
                enumValDef.i = enumNum+1;

                /* Store new enum value in the attribute definition. */
                attribDef.enumValAry[enumNum] = enumValDef;

                ++ enumNum;

            } /* for(All specified named values of the enum) */

            /// @todo Actual DBC files tend to have several times the same value
            // designation for different values, like "undefined" or "no_value". Shall
            // we have a name disambiguation here in order to avoid problems in one by
            // one generated C enumerations? Will this be possible locally here without
            // a conflict at where the attributes are referenced? Likely not, since
            // attribute default values of enums are made by name not value.
            //   We can store the original and the disambiguated name and use the
            // former for lookup and store the latter in the actual attribute as string
            // representation of the enum.
            //   For now, we just emit a warning.
            for(int idxTestedVal=0; idxTestedVal<enumNum; ++idxTestedVal)
            {
                final String testedVal = attribDef.enumValAry[idxTestedVal].name;

                for(int idxEnum=0; idxEnum<enumNum; ++idxEnum)
                {
                    if(idxEnum == idxTestedVal)
                        continue;

                    if(testedVal.equals(attribDef.enumValAry[idxEnum].name))
                    {
                        /* If value is visible for lower indexes then we have already
                           warned in a earlier cycle of the outer loop. */
                        if(idxEnum < idxTestedVal)
                            break;

                        errCnt_.warning();
                        _logger.warn(errorContext(ctx) + "Ambiguous definition of"
                                     + " attribute " + attribDef.name + ". The enumeration"
                                     + " value name " + testedVal
                                     + " is used repeatedly in the definition of the"
                                     + " enumeration"
                                    );

                        /* Don't warn again, if there should be even more repetitions of
                           the same value name. */
                        break;
                    }
                }
            } /* for(All double-checked named values of the enum) */
        }
        else
        {
            /* The grammar matched the definition of a floating point attribute. */
            assert ctxTypeFloat != null;
            attribDef.isFloat = true;
            attribDef.min = getNumberFromContext(ctxTypeFloat.min);
            attribDef.max = getNumberFromContext(ctxTypeFloat.max);
        }

        if(bus_.attribDefMap == null)
            bus_.attribDefMap = new HashMap<String,AttributeDef>();
        if(bus_.attribDefAry == null)
            bus_.attribDefAry = new ArrayList<AttributeDef>();

        /* Check for double definition. */
        AttributeDef otherAttribDef = bus_.attribDefMap.get(attribDef.name);
        if(otherAttribDef == null)
        {
            /* Maintain the order of objects in the collection; useful for making lists in
               a template. */
            attribDef.i0 = bus_.attribDefAry.size();
            attribDef.i = attribDef.i0+1;
            bus_.attribDefMap.put(attribDef.name, attribDef);
            bus_.attribDefAry.add(attribDef);
        }
        else
        {
            errCnt_.error();
            _logger.error(errorContext(ctx) + "Attribute " + attribDef.name
                          + " of object type " + attribDef.objType
                          + " is already defined"
                          + (attribDef.objType != otherAttribDef.objType
                             ? " as object type " + otherAttribDef.objType
                               + ". Attribute names have a global name space and"
                               + " must not be reused for different object types"
                             : ""
                            )
                         );
        }

    } /* End of exitAttributeDefinition */




    /**
     * {@inheritDoc}<p>
     * The definition of a node specific attribute or environment variable is not
     * supported. However, we try to parse it and want to emit a warning that we ignore it
     * (for now).
     */
    @Override public void exitUnrecognizedStatement(DbcParser.UnrecognizedStatementContext ctx)
    {
        String unrecognizedStatement = ctx.statement.getText();

        String msg = errorContext(ctx) + "Statement " + unrecognizedStatement
                     + " is ignored. Node specific attributes and environment variables"
                     + " are not supported by this application";

        boolean doWarn;
        if(unrecognizedStatement.equals("BA_REL_"))
        {
            final int maxNoPrintedWarnings = 10;

            ++ noWarningsBaRel_;
            doWarn = noWarningsBaRel_ <= maxNoPrintedWarnings;
            if(noWarningsBaRel_ == maxNoPrintedWarnings)
            {
                msg += ". Maximum " + maxNoPrintedWarnings + " of warnings reached. No"
                       + " more warnings about unrecognized use of statement BA_REL_ will"
                       + " be raised";
            }
        }
        else
            doWarn = true;

        if(doWarn)
        {
            errCnt_.warning();
            _logger.warn(msg);
        }
    } /* End of exitUnrecognizedStatement */




    /**
     * Get the value of an attribute from the parse tree.
     *   @return Get the value of an attribute as an {@link Attribute} object. Or null in
     * case of (already reported) errors.
     *   @param name The attribute is identified by its name, which has been parsed before.
     *   @param ctx The rule context from the parser as for the attribute value.
     *   @param isDefVal The Boolean information whether we parse an attribute value or a
     * default value. Although both use the same grammar rule the evaluation differs.
     */
    private Attribute getAttribute( String name
                                  , DbcParser.AttribValContext ctx
                                  , boolean isDefVal
                                  )
    {
        /* Retrieve the related attribute definition. */
        AttributeDef attribDef = bus_.attribDefMap.get(name);
        if(attribDef == null)
        {
            errCnt_.error();
            _logger.error(errorContext(ctx) + "Invalid reference to attribute " + name
                          + ". No such attribute has been defined before"
                         );
            return null;
        }

        Attribute attrib = new Attribute();
        attrib.name = name;

        /* Get the value from the parse tree but consider the expected data type. */
        double numVal = 0.0;
        if(attribDef.isString ||  attribDef.isEnum && isDefVal)
        {
            /* A string value is needed for string type attributes and for the default
               value of enums. */
            if(ctx.stringVal != null)
                attrib.str = getStringFromToken(ctx.stringVal, /* asJavaString */ true);
            else
            {
                errCnt_.error();
                _logger.error(errorContext(ctx) + "Bad attribute value for attribute "
                              + name + ": Expect a string as value"
                             );
                return null;
            }
        }
        else
        {
            /* A numeric value is needed for enum, int, hex and float. */
            if(ctx.numVal != null)
                numVal = getNumberFromContext(ctx.numVal);
            else
            {
                errCnt_.error();
                _logger.error(errorContext(ctx) + "Bad attribute value for attribute "
                              + name + ": Expect a numeric value"
                             );
                return null;
            }
        }

        /* Translate the parsed value into the representation of the data model. */
        if(attribDef.isEnum)
        {
            /* Design error of the DBC format: The default value is expressed as a string
               holding the meant enumeration value, while an actual object value uses an
               unsigned integer as null based index into the list of enumeration values to
               do the same. */
            if(isDefVal)
            {
                /* Look for the string in the saved list of enum value strings. The
                   position is the numeric value of the attribute. */
                boolean found = false;
                for(AttributeDef.EnumValueDef enumValDef: attribDef.enumValAry)
                {
                    /// @todo If we want to disambiguate the enumeration value designations
                    // then we would compare here to the original name from the DBC file but
                    // - if found - store the disambiguated in the new Attribute object.
                    if(enumValDef.name.compareTo(attrib.str) == 0)
                    {
                        /* There are network database files, which reuse the same enum
                           value name for different enum values. This would be worth a
                           warning in general. It even becomes an error here since the
                           default value is defined by value name and is ambiguous (whereas
                           the actual values are defined as an unambiguous number). */
                        if(found)
                        {
                            /* Although actually an error, we rate it as a warning and use
                               the first appearance of the value name as default value.
                               This has been decided since real DBC files easily show this
                               problem. */
                            errCnt_.warning();
                            _logger.warn(errorContext(ctx) + "Ambiguous definition of"
                                         + " attribute default value. The enumeration"
                                         + " value name " + attrib.str
                                         + " is used repeatedly in the"
                                         + " definition of the enumeration and can't be"
                                         + " resolved to the aimed enumeration value. The"
                                         + " first match is used, the supported default"
                                         + " value is " + attrib.n
                                        );
                        }
                        else
                        {
                            found = true;

                            /* Numeric index value is found in the attribute definition. */
                            attrib.n = Integer.valueOf(enumValDef.i0);
                        }
                    } /* End if(Parsed enum value found in definition list?) */
                } /* for(All enum value definitions) */

                if(!found)
                {
                    errCnt_.error();
                    _logger.error(errorContext(ctx) + "Value \"" + attrib.str + "\""
                                  + " is not a valid value specified for attribute " + name
                                 );
                    return null;
                }
            }
            else
            {
                /* In Java, the type cast double to int saturates to the range of int. It
                   doesn't throw an exception. */
                int idx = (int)numVal;
                if((double)idx == numVal)
                {
                    if(idx >= 0  &&  idx < attribDef.enumValAry.length)
                        attrib.n = Integer.valueOf(idx);
                    else
                    {
                        /* DBC files have been found where a negative integer is used as
                           value. This is likely meant to be the default value. It is
                           however not clear if this is compliant with the standard,
                           normally the default value would be addressed to by not
                           specifying a value at all. We emit a warning in this case. */
                        errCnt_.warning();
                        _logger.warn(errorContext(ctx)
                                     + "Invalid enumeration value index " + idx + " found."
                                     + " Expect an unsigned integer in the range 0.."
                                     + (attribDef.enumValAry.length-1)
                                     + ". The default enumeration value will be used instead"
                                    );
                        if(attribDef.defVal != null)
                            attrib.n = attribDef.defVal.n;
                        else
                        {
                            errCnt_.error();
                            _logger.error(errorContext(ctx)
                                          + "Default enumeration value requested but no such"
                                          + " default value had been defined before"
                                         );
                            return null;
                        }
                    }
                }
                else
                {
                    errCnt_.error();
                    _logger.error(errorContext(ctx) + "Invalid enumeration value index"
                                  + numVal + " found."
                                  + " Expect an unsigned integer in the range 0.."
                                  + (attribDef.enumValAry.length-1)
                                 );
                    return null;
                }
            } /* End if(Default or individual attribute value?) */

            assert (attrib.n instanceof Integer) &&  attrib.n.intValue() >= 0
                   &&  attrib.n.intValue() < attribDef.enumValAry.length;

            /* Provide numeric and textual information. */
            /// @todo If we want to disambiguate the enumeration values we should here take
            // the disambiguated one but we can offer the original in a further, new field
            // - or vice versa?
            final String enumValName = attribDef.enumValAry[attrib.n.intValue()].name;
            attrib.str = enumValName;

            /* Put the found value into the easy-query map. */
            /// @todo If we want to disambiguate the enumeration values: which one should be
            // used for easy-query? Hopefully, the answer is not so relevant. Code
            // generation is anyway not safely possible if ambiguities occur in actually
            // used enumeration values. Mostly they appear in placeholders like "not used".
            attrib.is = new HashMap<String,Boolean>(1);
            attrib.is.put(enumValName, Boolean.valueOf(true));
        }
        else if(attribDef.isInt /* isInt includes isHex. */)
        {
            attrib.n = Long.valueOf((long)numVal);

            /* The parser may have accepted a none integer even for this attribute.
               Double-check. */
            if((double)attrib.n.longValue() != numVal)
            {
                errCnt_.warning();
                _logger.warn(errorContext(ctx) + "The none integer or out of range value "
                             + numVal
                             + " has been assigned to integer attribute " + name
                             + ". The rounded value " + attrib.n + " is used instead"
                             );
            }
        }
        else if(attribDef.isFloat)
            attrib.n = Double.valueOf(numVal);
        else
        {
            /* Tpye String: Nothing else to do. */
            assert attribDef.isString;
        }

        attrib.isDefVal = isDefVal;

        return attrib;

    } /* End of DataModelListener.getAttribute */




    /**
     * {@inheritDoc}<p>
     * Evaluate the definition of an attribute's default value.
     */
    @Override public void exitAttributeDefault(DbcParser.AttributeDefaultContext ctx)
    {
        final String attribName = getAttribNameFromToken(ctx.attribName);
        Attribute attrib = getAttribute(attribName, ctx.attribVal(), /* isDefVal */ true);
        if(attrib == null)
            return;

        /* Write the value into the definition; this should be the first time (null
           check). */
        AttributeDef attribDef = bus_.attribDefMap.get(attribName);
        assert attribDef != null;
        if(attribDef.defVal == null)
        {
            attribDef.defVal = attrib;

            /* Now broadcast the value to all objects of the related type. Find the
               appropriate collection that holds all the related objects. */
            Collection<? extends NetObject> collectionNetObjs = null;
            switch(attribDef.objType)
            {
            case bus:
                ArrayList<Bus> list = new ArrayList<>(1);
                list.add(bus_);
                collectionNetObjs = list;
                break;

            case node:
                collectionNetObjs = mapNodeByName_.values();
                break;

            case frame:
                collectionNetObjs = mapFrameById_.values();
                break;

            case signal:
                collectionNetObjs = mapSignalByIdAndName_.values();
                break;

            case envVar:
                /* Environment variable objects are not supported. No need to give an
                   according message here again. */
                break;

            default: assert false;
            }

            /* Iterate across the found collection. */
            if(collectionNetObjs != null)
            {
                for(NetObject netObj: collectionNetObjs)
                {
                    _logger.debug("Attach default value " + attrib + " of attribute "
                                  + attribName + " to " + attribDef.objType + " "
                                  + netObj.name
                                 );
                    assert attribName.compareTo(attrib.name) == 0;
                    Attribute previousValue = netObj.storeAttribValue(attribName, attrib);
                    if(previousValue != null)
                    {
                        errCnt_.warning();
                        _logger.warn(errorContext(ctx.attribName)
                                     + "Default value for attribute " + attribName
                                     + " is doubly defined. The previous value "
                                     + previousValue + " is ignored"
                                    );
                    }
                } /* End for(All related network objects) */
            } /* End if(Supported type of network object?) */
        }
        else
        {
            errCnt_.warning();
            _logger.warn(errorContext(ctx.attribName) + "Default value of attribute "
                         + attribName + " is repeatedly defined. This value is ignored"
                        );
        }
    } /* End of exitAttributeDefault */



    /**
     * {@inheritDoc}<p>
     * Pick the individual attribute values from the parse tree and put them into the
     * addressed network objects.
     */
    @Override public void exitAttributeValue(DbcParser.AttributeValueContext ctx)
    {
        final String attribName = getAttribNameFromToken(ctx.attribName);

        /* Read the value of the attribute by comparing it with the information from the
           attribute definition. Any inconsistencies are reported. If any, we have to
           ignore the value. */
        Attribute attrib = getAttribute(attribName, ctx.attribVal(), /* isDefVal */ false);
        if(attrib == null)
            return;

        /* Access the definition of the attribute. */
        AttributeDef attribDef = bus_.attribDefMap.get(attribName);
        assert attribDef != null;

        /* Find the addressee of the attribute value. Different syntax elements are used to
           address to either the bus, a node, a frame or a signal.
             A bug of the Kvaser Database Editor (Version 3.13.317, Sep 9, 2020) disclosed
           a bug in our DBC parser: The Kvaser editor had written bad attribute lines, like
           this:
             BA_ "GenSigStartValue" BO_ 1793 100;
           although the attribute GenSigStartValue had been properly defined as signal
           related. For another signal from the same message it had correctly written:
             BA_ "GenSigStartValue" SG_ 1793 ALIV 100;
           In the code below, our parser goes into the switch statement and selects the
           case not because of what had actually been parsed but because of its already
           available knowledge about the attribute: The BA_DEF_ statement has been parsed
           and it had declared "GenSigStartValue" to be a signal related attribute and now
           it goes into the "case signal". However, the ANTLR parse result doesn't contain
           the fields, which are normally set for a signal attribute.
             The same can happen for all cases. We need to inspect all variable grammar
           rule elements before use. These are: nodeName, msgId, signalName and envVarName. */
        NetObject netObj = null;
        boolean syntaxErrBadType = false;
        switch(attribDef.objType)
        {
        case bus:
            syntaxErrBadType = ctx.nodeName != null  ||  ctx.msgId != null
                               ||  ctx.signalName != null  ||  ctx.envVarName != null;
            if(!syntaxErrBadType)
                netObj = bus_;

            break;

        case node:
            syntaxErrBadType = ctx.nodeName == null  ||  ctx.msgId != null
                               ||  ctx.signalName != null  ||  ctx.envVarName != null;
            if(!syntaxErrBadType)
            {
                netObj = mapNodeByName_.get(ctx.nodeName.getText());
                if(netObj == null)
                {
                    errCnt_.warning();
                    _logger.warn(errorContext(ctx.attribName) + "Attribute " + attribName
                                 + " refers to unknown network node " + ctx.nodeName.getText()
                                 + ". It is ignored"
                                );
                }
            }
            break;

        case frame:
            syntaxErrBadType = ctx.nodeName != null  ||  ctx.msgId == null
                               ||  ctx.signalName != null  ||  ctx.envVarName != null;
            if(!syntaxErrBadType)
            {
                netObj = mapFrameById_.get(parseCanId(ctx.msgId.getText()));

                /* No error message must be emitted if the addressed frame object is not
                   found: By configuration the user can select any sub-set of frames for
                   processing. */
            }
            break;

        case signal:
            syntaxErrBadType = ctx.nodeName != null  ||  ctx.msgId == null
                               ||  ctx.signalName == null  ||  ctx.envVarName != null;
            if(!syntaxErrBadType)
            {
                netObj = mapSignalByIdAndName_.get
                                              (Pair.create( parseCanId(ctx.msgId.getText())
                                                          , ctx.signalName.getText()
                                                          )
                                              );
                /* No error message must be emitted if the addressed signal object is not
                   found: It could belong to a not supported frame. */
            }
            break;

        case envVar:
            syntaxErrBadType = ctx.nodeName != null  ||  ctx.msgId != null
                               ||  ctx.signalName != null  ||  ctx.envVarName == null;
            if(!syntaxErrBadType)
            {
                /* Environment variables are not supported but related warnings have
                   already been emitted. Ignore the attribute value silently. */
            }
            break;

        default: assert false;
        }

        if(syntaxErrBadType)
        {
            /* The attribute value statement doesn't fit to a bus attribute. */
            errCnt_.error();
            _logger.error(errorContext(ctx.attribName) + "Syntax error in value"
                          + " definition of attribute " + attribName
                          + ". The attribute value definition relates to another kind of"
                          + " object than the attribute had been defined for"
                         );
            assert netObj == null;
        }

        /* Attach the attribute to the addressee's network object. */
        if(netObj != null)
        {
            _logger.debug("Attach value " + attrib + " of attribute " + attribName
                          + " to " + attribDef.objType + " " + netObj.name
                         );
            assert attribName.compareTo(attribDef.name) == 0;
            Attribute previousValue = netObj.storeAttribValue(attribName, attrib);
            if(previousValue != null  &&  !previousValue.isDefVal)
            {
                errCnt_.warning();
                _logger.warn(errorContext(ctx.attribName) + "Attribute " + attribName
                             + " is doubly defined. The previous value "
                             + previousValue + " is ignored"
                            );
            }
        }
    } /* End of exitAttributeValue */




    /**
     * {@inheritDoc}
     * Check for floating point signal data type and validate. Must only be applied to
     * signals of according length.
     */
    @Override public void exitSignalExtendedValueTypeList
                                        (DbcParser.SignalExtendedValueTypeListContext ctx)
    {
        /* The enumeration value is parsed as any positive integer. We need to validate the
           range. */
        final String errLocation = errorContext(ctx.signalExtendedValueType);
        final int typeVal = getIntegerFromToken(ctx.signalExtendedValueType);

        /* Syntactically incorrect out of range values have already been caught and reported
           by the parser. Here we just act on floating point data types, which cause some
           inconvenience. */
        if(typeVal == 1 /* float */  ||  typeVal == 2 /* double */)
        {
            final boolean isDouble = typeVal == 2;

            /* The values 1 and 2 mean floating point numbers.
                 Find the signal the type information relates to. */
            final CanId canId = parseCanId(ctx.msgId.getText());
            Signal s = mapSignalByIdAndName_.get(Pair.create(canId, ctx.signalName.getText()));
            if(s == null)
            {
                /* No error message, we can have filtered out the frame holding the addressed
                   signal. */
                return;
            }

            final String signal = "Frame " + canId + ", " + s.name;

            /* Report the presence of floating point signals globally to the code
               generation templates. */
            bus_.hasFloatingPointSignals = true;

            /* Floating point signals are quite uncommon and likely not supported by the
               code templates of the application. Let's collect the signals and report the
               problem once at the end as a warning. */
            if(nameListOfFloatSignals_ == null)
                nameListOfFloatSignals_ = new StringBuffer(signal);
            else
            {
                nameListOfFloatSignals_.append(", ");
                nameListOfFloatSignals_.append(signal);
            }
            _logger.debug(errLocation
                          + signal +" has a floating point data type. Floating point"
                          + " signals are very uncommon"
                          );

            /* Floating point signals may theoretically be scaled although this is
               completely useless. It's very likely not supported by the code templates of
               the application. Report the situation as a warning. */
            if(s.factor != 1.0  ||  s.offset != 0.0)
            {
                /* Report this problem globally to the code generation templates. */
                bus_.hasScaledFloatingPointSignals = true;

                    errCnt_.warning();
                _logger.warn(errLocation + signal + " is scaled but has a"
                             + " floating point data type. Scaling is useless for"
                             + " floating point signals and is likely not supported by the"
                             + " code generation templates of this application"
                            );
            }

            /* Floating point data types are applicable only for signals of fixed length.
               Double-check. */
            if(!isDouble &&  s.length == 32  ||  isDouble &&  s.length == 64)
            {
                /* Attach the new, additional type information to the already existing
                   signal information. */
                s.isInteger = false;
                s.isFloat = !isDouble;
                s.isDouble = isDouble;
                s.type = isDouble? "float64_t": "float32_t";
            }
            else
            {
                errCnt_.error();
                _logger.error(errLocation
                              + signal + " is a floating point signal but the "
                              + " size of the signal doesn't fit. Need a "
                              + (isDouble? "64": "32") + " Bit signal"
                             );
            }
        }
        else
        {
            /* Otherwise it should be 0 for integer - no action needed, all signals are
               considered scaled integers by default - or -1 in the case of theoretically
               possible snytax errors in the number literal. */
            assert typeVal == 0  ||  typeVal == -1;
        }
    } /* End of DataModelListener.exitSignalExtendedValueTypeList */



    /**
     * Let all PDUs inherit their frames' attributes.
     *   For CAN, PDU and frame have a one by one relation but actually, the PDU is not
     * found in the network database. (We have introduced it only to approach a common data
     * model for different kinds of networks.) All parsed attributes relate to the frame
     * objects but for practical reasons when writing templates it's more convenient to
     * access the attributes via the PDU object. After parsing has completed, we copy the
     * attributes of all frames to their PDU object.
     */
    private void pduInheritsFrameAttributes()
    {
        /* Iterate across all frames. */
        for(Frame frame: mapFrameById_.values())
        {
            assert frame.pduAry.length == 1;
            Pdu pdu = frame.pduAry[0];

            /* The PDU gets a reference to the map of the frame object. */
            assert pdu.attribMap == null;
            pdu.attribMap = frame.attribMap;
        }
    } /* End of DataModelListener.pduInheritsFrameAttributes */



    /**
     * Real existing DBC files tend to not have a default value for all the defined
     * attributes. Tolerant parsing means that this is a warning only as long as the code
     * generation is not harmfully affected. We decide to tolerate it if all objects of
     * given kind have got an actual value of the incomplete attribute.<p>
     *   This method double checks the presence of actual values for a given attribute. It
     * must be called only after parsing has completed.
     */
    private void checkForActualAttribValues(AttributeDef attribDef)
    {
        final String attribName = attribDef.name;

        /* No need to call this for attributes, which have a default value. This value
           would have been broadcastet to all the affected objects. */
        assert attribDef.defVal == null;

        /* It can easily be the case that the value has been forgotten at all. This would
           to hundreds of error messages. Limit it by only emitting the first few. Consider
           that this is the check for only a single attribute. It can still become a long
           list if attribute default values are mssing in general. */
        final int maxErrReports = 10
                , noErrBefore = errCnt_.getNoErrors();

        /* Visit all net objects, which might have this attribute. Find the appropriate
           collection that holds all the related objects. */
        Collection<? extends NetObject> collectionNetObjs = null;
        switch(attribDef.objType)
        {
            case bus:
                ArrayList<Bus> list = new ArrayList<>(1);
                list.add(bus_);
                collectionNetObjs = list;
                break;

            case node:
                collectionNetObjs = mapNodeByName_.values();
                break;

            case frame:
                collectionNetObjs = mapFrameById_.values();
                break;

            case signal:
                collectionNetObjs = mapSignalByIdAndName_.values();
                break;

            case envVar:
                /* Environment variable objects are not supported. No need to give an
                   according message here again. */
                break;

            default: assert false;
        }

        /* Iterate across the found collection. */
        if(collectionNetObjs != null)
        {
            for(NetObject netObj: collectionNetObjs)
            {
                _logger.debug("Check presence of value of attribute " + attribName + " of "
                              + attribDef.objType + " " + netObj.name
                             );

                if(netObj.attribMap.get(attribName) == null)
                {
                    errCnt_.error();
                    _logger.error(errorContext_
                                  + "No actual value is defined for attribute " + attribName
                                  + " of " + attribDef.objType + " " + netObj.name
                                 );

                    /* Limit number of error messages. */
                    if(errCnt_.getNoErrors() - noErrBefore >= maxErrReports)
                    {
                        errCnt_.warning();
                        _logger.warn(errorContext_
                                     + "Too many error messages. The presence check of"
                                     + " attribute " + attribName + " is aborted."
                                     + " There might be more " + attribDef.objType
                                     + " objects with missing value"
                                    );
                        break; // for(netObj)
                    }
                }
            } /* End for(All related network objects) */
        } /* End if(Supported type of network object?) */
    } /* End of checkForActualAttribValues */



    /**
     * Do a walk through a parse tree using this listener.
     *   The walk picks all relevant information from the parse tree and puts it into the
     * aimed data model of class {@link Bus}. Some data validation, which was not yet
     * possible during parsing is done at the same time and can lead to not yet recognized
     * parse errors.
     *   @return
     * Get the bus object containing all bus related relevant parse results. Or get null if
     * an error occured during the walk through the parse tree.
     *   @param parseTree
     * The result of a successful parsing of the network database file. The result is
     * unpredictable if the output of a parse process is passed in that had reported an
     * error: The listener strongly depends on the checks, which are made during parsing.
     *   @param busDescription
     * A parameter set that describes the bus from the perspective of the user is passed
     * in. Most important detail is the name of the network node the code generation is
     * made for.
     *   @param errCnt
     * An error counter for reporting data validation problems encountered during the walk.
     * The returned bus object is valid only if no error is reported.<p>
     *   Optional. Pass null if no known error counter should be applied.
     */
    public static Bus walk( ParseTree parseTree
                          , final ParameterSet.BusDescription busDescription
                          , ErrorCounter errCnt
                          )
    {
        DataModelListener l = new DataModelListener(errCnt);

        l.errorContext_ = "Bus " + busDescription.name + ": ";
        l.busDescription_ = busDescription;

        /* Store the name of the bus: It is not part of the parse data, not part of the
           network database but user provided. */
        assert l.bus_ != null;
        l.bus_.name = busDescription.name;

        /* Store the name of the network database file for reference. */
        l.bus_.networkFile = new FileExt(busDescription.networkFileName);

        /* Pass the arguments and user options defined at the command line into the
           template. */
        l.bus_.isTransmissionDirectionInverted = busDescription.invertTransmissionDirection;
        if(busDescription.invertTransmissionDirection)
        {
            _logger.info(l.errorContext_ + "Inverted transmission direction is applied to"
                         + " the data model of this bus"
                        );
        }
        l.bus_.optionMap = busDescription.optionMap;

        _logger.debug(l.errorContext_ + "Starting walk through parse tree of network database "
                      + busDescription.networkFileName
                     );

        /* Create a walker for transformation of parse tree into data model needed
           for the template engine. */
        ParseTreeWalker walker = new ParseTreeWalker();

        /* Do the walk with this data-transformer-walker to shape the needed data
           model. */
        walker.walk(l, parseTree);

        // @todo Sort the signal value lists of all signals in all pdus. Sort order would
        // be the numeric value
        //for signal: all signals
        //{
        //    if(signal.valueDescAry != null)
        //        Collections.sort(signal.valueDescAry);
        //}

        /* Emit now collected warnings about uncommon use of floating point signals. */
        if(l.nameListOfFloatSignals_ != null)
        {
            l.errCnt_.warning();
            _logger.warn(l.errorContext_ + "The network database contains frames with"
                         + " floating point signals. Floating point"
                         + " signals are very uncommon and might not be supported by the"
                         + " code generation templates of the application. Consider to"
                         + " use logging level DEBUG to get more details. The affected"
                         + " signals are: " + l.nameListOfFloatSignals_
                        );
        }

        /* Check if all attributes have got default values. */
        if(l.bus_.attribDefAry != null)
        {
            for(AttributeDef attribDef: l.bus_.attribDefAry)
            {
                /* Check for default value. The only problem with a missing default value
                   are network objects not getting an individual value. This is double
                   checked in a second step. */
                if(attribDef.defVal == null)
                {
                    l.errCnt_.warning();
                    _logger.warn(l.errorContext_
                                 + "No default value is defined for attribute "
                                 + attribDef.name + " of object type " + attribDef.objType
                                 + ". This will lead to an error if at least one related"
                                 + " object doesn't get an individual value"
                                );
                    l.checkForActualAttribValues(attribDef);
                }
            } /* For(All attribute definitions) */
        }

        /* Copy the attributes of the frames to their PDU. */
        l.pduInheritsFrameAttributes();

        /* Get the transformed data. */
        Bus bus = l.getBusData(/* reset */ true);

        _logger.debug(l.errorContext_ + "Walk through parse tree "
                      + (bus!=null? "successfully done": "done with failures")
                     );

        return bus;

    } /* End of DataModelListener.walk */

} /* End of class DataModelListener definition. */

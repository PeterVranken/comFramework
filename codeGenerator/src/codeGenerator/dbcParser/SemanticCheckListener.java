/**
 * @file SemanticCheckListener.java
 * The parser's grammar is intentionally kept clean from too many specific evaluations,
 * which would require either embedded action code or at least crude syntax constructs. To
 * give an example, the multiplexer switch of a signal is defined as a character m followed
 * without whitespace by an integer. This is a subset of the pattern of an identifier and
 * stated in the grammar as such. After successful parsing we know that we saw any
 * identifier but we still need to double-check whether it is compliant with the more
 * specific pattern of the multiplexer switch. This and a number of similar tests are done
 * by walking through the parse tree after parsing. The walk is done using this listener.
 *
 * Copyright (C) 2015-2018 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class SemanticCheckListener
 *   parseInt
 *   SemanticCheckListener
 *   location
 *   exitValueTable
 *   enterMsg
 *   exitSignal
 *   exitEnvironmentVariable
 *   exitSignalExtendedValueTypeList
 *   exitSingleValueDescription
 *   exitDbc
 */

package codeGenerator.dbcParser;

import java.util.*;
import java.util.regex.*;
import org.apache.log4j.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;


/**
 * Semantic check of parse result.
 *   This class implements a listener for a walk through the parse tree. All syntax checks
 * not yet done during grammar matching are performed.
 */

public class SemanticCheckListener extends DbcBaseListener
{
    /// @todo Add error reporting by interface: Should be application dependent but not defined in the parser package
    
    /** Access the Apache logger object. */
    private static Logger _logger = Logger.getLogger(SemanticCheckListener.class.getName());
    
    /** Counter for all errors and messages, which occur during semantic listening. */
    private ErrorCounter errCnt_;

    /** Static regular expression for the check of attribute names. We expect an identifier
        enclosed in double quotes. */
    final private static Pattern _reAttribName = Pattern.compile("\"[a-zA-Z_][a-zA-Z_0-9]*\"");
    
    /** Static regular expression for the weak check of attribute names. Actually, the
        format specification requires an identifier enclosed in double quotes but typical
        DBC files tend to use the hyphen as further character. */
    final private static Pattern _reAttribNameWithFaults =
                                              Pattern.compile("\"[-a-zA-Z_][-a-zA-Z_0-9]*\"");
    
    /** Don't emit the same warning any time the same bad attribute name reappears in the
        DBC file: A set of already reported bad names. */
    private HashSet<String> setOfBadAttribNames_ = null;
            
    /** Static regular expression for the check of environment access types. */
    final private static Pattern _reEnvVarAccessType = 
                                            Pattern.compile("DUMMY_NODE_VECTOR(800)?[0-3]");
    
    /** A list of ignored value tables. The list is implemented as a String, which is used
        for reporting. */
    private StringBuffer nameListOfValTables_ = null;
    
    /** The number of illegal negative named signal values. Used for problem reporting. */
    private int noSignedSignalValueDescriptions_ = 0;
    
    /** A helper flag to detect unsupported multiplexing. */
    private boolean bMpxControlSignalFound_ = false;
    
    
    /**
     * At many locations the parser is guided to read an integer. Such a number is defiend
     * as a sequence of digits. At evaluation time of the parse information we will use the
     * normal string to integer conversion routines to get the numeric value. Here a range
     * check is involved, which is not anticipated by the parser. To avoid later exceptions
     * we provide a string to integer conversion with range check and implicit error
     * reporting.\n
     *   A public static method is provided, which can also be used from outside by other
     * listeners and related code.
     *   @return
     * Get the parsed integer number. The value will always be in the range [0, 2^31-1]. -1
     * is returned in case of errors.
     *   @param tokInteger
     * The integer token to be validated as returned by the parser.
     *   @param errCnt
     * The error counter object to be used for error reporting. May be null if a possible
     * error should not be counted.
     *   @param errMsgHeader
     * An error message is written to the global logger in case. It is preceeded by this
     * string, which will typically contain some informative text about the context and
     * location of the integer validation. The error report is added without blanks, full
     * stops or colons in between.\n
     *   May be null in which case no error reporting takes place.
     */
    public static int parseInt(Token tokInteger, ErrorCounter errCnt, String errMsgHeader)
    {
        int unsignedIntVal = -1;
        try
        {
            unsignedIntVal = Integer.parseInt(tokInteger.getText());
            
            /* The parser has validated that this is a signless number. */
            assert unsignedIntVal >= 0: "Unexpected negative value";
        }
        catch(NumberFormatException e)
        {
            /* Error reporting is conditionally. */
            if(errCnt != null)
                errCnt.error();
            if(errMsgHeader != null)
            {
                /* The only possible reason for this exception is a range violation. */
                _logger.error(errMsgHeader
                              + "Invalid integer found. The supported range of [0, 2^31-1]" 
                              + " is exceeded"
                             );
            }
            unsignedIntVal = -1;
        }
                        
        return unsignedIntVal;
        
    } /* End of SemanticCheckListener.parseInt */
    

     
    /**
     * Create an instance of this class.
     *   @param errCnt
     * The future semantic messages of the listener are counted in this object. The counter
     * is taken as it is and not reset or initialized.
     */
    public SemanticCheckListener(ErrorCounter errCnt)
    {
        errCnt_ = errCnt;
        
    } /* End of SemanticCheckListener.SemanticCheckListener */

    
    /**
     * Compose a string, which indicates the location of a parse problem in a uniquely used
     * way.
     *   @param tok
     * The location is extracted from a Token object.
     */   
    String location(Token tok)
    {
        return (new String()).format( "%s:%d:%d: "
                                    , tok.getInputStream().getSourceName()
                                    , tok.getLine()
                                    , tok.getCharPositionInLine()
                                    );
    } /* End of SemanticCheckListener.location */                                     

    
    /**
     * {@inheritDoc}<p>
     * Rule valueTable: This syntax element is deprecated, obsolete and not evaluated
     * although still often found in real DBC file. Therefore it's useful to get a warning
     * in case.
     */
    @Override public void exitValueTable(DbcParser.ValueTableContext ctx)
    {
        Token tokName = ctx.name;
        
        /* Collect a list of names and write a single warning at the end. */
        if(nameListOfValTables_ == null)
            nameListOfValTables_ = new StringBuffer(tokName.getText());
        else 
        {
            nameListOfValTables_.append(", ");
            nameListOfValTables_.append(tokName.getText());
        }
        
        _logger.debug( location(tokName) + "Value table " + tokName.getText()
                       + " found. Value tables are obsolete and will be ignored"
                     );
    } /* End of exitValueTable */
    
    
    
        
    /**
     * {@inheritDoc}
     */
    @Override public void enterMsg(DbcParser.MsgContext ctx)
    {
        /* Reset the flag, which is used to find multiple mux selector signals in one
           frame. */
        bMpxControlSignalFound_ = false;

    } /* End of enterMsg */



    /**
     * {@inheritDoc}<p>
     * Signal parsing complete.
     */
    @Override public void exitSignal(DbcParser.SignalContext ctx)
    {
        assert ctx.name != null: "Context not available";

        /* Predicate 1 or 0 for byteOrder in signal needed. Grammar only says Integer. Do
           check now. */
        String byteOrder = ctx.byteOrder.getText();
        if(byteOrder.compareTo("0") != 0  &&  byteOrder.compareTo("1") != 0)
        {
            errCnt_.error();
            _logger.error(location(ctx.byteOrder)
                          + "Invalid byte order, expect either 0 for big-endian or 1 for"
                          + " little-endian"
                         );
        }

        boolean isMuxSelector = false
              , isMuxedSignal = false;
        int muxValue = -1;
        /* The multiplx indicator is parsed as an optional "ID" but actually there are
           strict constraints on the identifier. As the parser doesn't check them we have
           to do here: It's either a character M or the character m immediately followed by
           an unsigned integer (see section 8.2). */
        if(ctx.mpxIndicator != null)
        {
            /* Length at least one character has been checked by the parser. */
            assert ctx.mpxIndicator.getText().length() >= 1: "Invalid mpx indicator token";
            
            boolean isMpxOkay = false;
            final String parsedID = ctx.mpxIndicator.getText();
            if(parsedID.length() > 1)
            {
                /* Multiplex selector value of switched signal.
                     Here, we need a try/catch for Integer.parseInt since the parser was not
                   guided to check for an integer value. */
                try
                {
                    isMpxOkay = parsedID.charAt(0) == 'm'
                                &&  Integer.parseInt(parsedID.substring(1)) >= 0;
                }
                catch(NumberFormatException e)
                {
                    isMpxOkay = false;
                }
            }
            else
            {
                /* Multiplex control signal. */
                isMpxOkay = parsedID.compareTo("M") == 0;
                
                /* It's not clear from the DBC specification wether several multiplex
                   groups in one frame are allowed or not. Anyway, since we can't support
                   it we will raise an error message. Better to stop parsing than ending up
                   with false generated code.
                     The check is implemented by a flag being reset on message entry. */
                if(bMpxControlSignalFound_)
                {
                    errCnt_.error();
                    _logger.error(location(ctx.mpxIndicator)
                                  + "More than one multiplex selector is specified in the"
                                  + " context of a frame. This is not supported by the"
                                  + " application"
                                 );
                }
                else
                    bMpxControlSignalFound_ = true;
            }
            
            if(!isMpxOkay)
            {
                errCnt_.error();
                _logger.error(location(ctx.mpxIndicator)
                              + "Syntax error in signal multiplex specification. Expect"
                              + " either a single character M or the character m"
                              + " immediately followed by an unsigned integer in the range"
                              + " [0, 2^31-1]"
                             );
            }
        }
    } /* End of SemanticCheckListener.exitSignal */



    /**
     * {@inheritDoc}
     * The access type of an environment variable definition is specified to be one out of
     * DUMMY_NODE_VECTOR0, DUMMY_NODE_VECTOR1, DUMMY_NODE_VECTOR2, DUMMY_NODE_VECTOR3,
     * DUMMY_NODE_VECTOR8000, DUMMY_NODE_VECTOR8001, DUMMY_NODE_VECTOR8002 or
     * DUMMY_NODE_VECTOR8003. However, this list is not explicitly modeled as such in the
     * grammar, e.g. like 'DUMMY_NODE_VECTOR0' | ... | 'DUMMY_NODE_VECTOR8003'. This would
     * disable parser to recognize the same words as identifier in other grammar rules,
     * e.g. as signal or message name. Instead, the words have been parsed as identifier
     * and we now have to validate that the parsed identifier is a word from the permitted
     * list.
     */
    @Override public void exitEnvironmentVariable(DbcParser.EnvironmentVariableContext ctx)
    {
        assert ctx.accessType != null;
        if(!_reEnvVarAccessType.matcher(ctx.accessType.getText()).matches())
        {
            errCnt_.error();
            _logger.error(location(ctx.accessType)
                          + "Invalid environment variable access type found. Expect"
                          + " DUMMY_NODE_VECTOR<n>, where <n> is one out of 0..3 or"
                          + " 8000..8003"
                         );
        }
    } /* End of SemanticCheckListener.exitEnvironmentVariable */



    /**
     * {@inheritDoc}
     * Check for unsupported signal data type. We only can integer.
     */
    @Override public void exitSignalExtendedValueTypeList
                                        (DbcParser.SignalExtendedValueTypeListContext ctx)
    {
        /* The enumeration value is parsed as any positive integer. We need to validate the
           range. */
        final String errLocation = location(ctx.signalExtendedValueType);
        final int typeVal = parseInt(ctx.signalExtendedValueType, errCnt_, errLocation);
        
        if(typeVal < 0  ||  typeVal >= 3)
        {
            errCnt_.error();
            _logger.error(errLocation
                          + "Invalid signal type found. Only the types 0, 1 and 2 are defined"
                          + " for integer, float and double, respectively"
                         );
        }
    } /* End of SemanticCheckListener.exitSignalExtendedValueTypeList */



    /**
     * {@inheritDoc}
     * The syntax definition of a named signal value (section 8.4) requires an unsigned
     * value. However, real existing DBC files tend to use negative values, too. If token
     * invalidSign of grammar rule singleValueDescription matches then we need to emit a
     * warning about improper use of the DBC format.
     */
    @Override public void exitSingleValueDescription
                                        (DbcParser.SingleValueDescriptionContext ctx)
    {
        assert ctx.value != null  &&  ctx.description != null;
        String numberText = ctx.value.getText();
        if(numberText.charAt(0) == '-')
        {
            /* To avoid being flooded with messages we count the number of problems and
               write a single warning at the end. For now we only emit a message at lowest
               priority. */
            ++ noSignedSignalValueDescriptions_;
            
            _logger.debug( location(ctx.start) + "Signed signal value " + numberText
                           + " (" + ctx.description.getText() + ")"
                           + " found. Only unsigned integers are allowed but the signed"
                           + " value is tolerated"
                         );
        }
    } /* End of SemanticCheckListener.exitSingleValueDescription */

    
    /**
     * The attribute handling uses the syntax token identifier for the name of the
     * attribute but the syntax demands to enclose the identifier in double quotes. The
     * parser's grammar uses the general rule string to match the attribute name. This
     * method validates that a string Token object really contains a valid attribute name.
     * Errors are reported to the global log and the global error counter is incremented in
     * case.
     */
    private void validateAttribName(Token tokStringIdent)
    {
        assert tokStringIdent != null;
        final String string = tokStringIdent.getText();
        assert string.length() >= 2;
        
        if(!_reAttribName.matcher(string).matches())
        {
            if(_reAttribNameWithFaults.matcher(string).matches())
            {
                /* Don't emit the same warning any time the name reappears in the DBC file. */
                if(setOfBadAttribNames_ == null)
                    setOfBadAttribNames_ = new HashSet<>();

                if(!setOfBadAttribNames_.contains(string))
                {
                    errCnt_.warning();
                    _logger.warn( location(tokStringIdent) + "Invalid attribute name "
                                  + string + " found. Expect an identifier enclosed in"
                                  + " double quotes. The name will be corrected by replacing"
                                  + " all invalid characters with underscores"
                                );
                    setOfBadAttribNames_.add(string);
                }
            }
            else
            {
                errCnt_.error();
                _logger.error( location(tokStringIdent) + "Invalid attribute name "
                               + string + " found. Expect"
                               + " an identifier enclosed in double quotes"
                             );
            }
        }
    } /* End of SemanticCheckListener.validateAttribName */
    
    
    
    /**
     * {@inheritDoc}
     * The attribute handling uses the syntax token identifier for the name of the
     * attribute but the syntax demands to enclose the identifier in double quotes. The
     * parser's grammar uses the general rule string to match the attribute name and here
     * we have to validate that the parsed string really contains a valid identifier only.
     */
    @Override public void exitAttributeDefinition(DbcParser.AttributeDefinitionContext ctx)
    {
        validateAttribName(ctx.attribName);
        
    } /* End of SemanticCheckListener.exitAttributeDefinition */


    /**
     * {@inheritDoc}
     * The attribute handling uses the syntax token identifier for the name of the
     * attribute but the syntax demands to enclose the identifier in double quotes. The
     * parser's grammar uses the general rule string to match the attribute name and here
     * we have to validate that the parsed string really contains a valid identifier only.
     */
    @Override public void exitAttributeDefault(DbcParser.AttributeDefaultContext ctx)
    {
        validateAttribName(ctx.attribName);
        
    } /* End of SemanticCheckListener.exitAttributeDefault */


    /**
     * {@inheritDoc}
     * The attribute handling uses the syntax token identifier for the name of the
     * attribute but the syntax demands to enclose the identifier in double quotes. The
     * parser's grammar uses the general rule string to match the attribute name and here
     * we have to validate that the parsed string really contains a valid identifier only.
     */
    @Override public void exitAttributeValue(DbcParser.AttributeValueContext ctx)
    {
        validateAttribName(ctx.attribName);
        
    } /* End of SemanticCheckListener.exitAttributeValue */



    /**
     * {@inheritDoc}
     * Do final cleanup operations at the end of the walk through the parse tree.
     */
    @Override public void exitDbc(DbcParser.DbcContext ctx)
    {
        /* Write a summarizing warning of ignored value tables. */
        if(nameListOfValTables_ != null)
        {
            final int maxOutputLength = 1000;
            final String ellipsis = " (...)";
            String nameListOfValTables;
            if(nameListOfValTables_.length() <= maxOutputLength-ellipsis.length())
                nameListOfValTables = nameListOfValTables_.toString();
            else
            {
                nameListOfValTables = nameListOfValTables_.substring( 0
                                                                    , maxOutputLength
                                                                      - ellipsis.length()
                                                                    )
                                      + ellipsis;
            }
            errCnt_.warning();
            _logger.warn( "The network database contains value tables. Value tables are"
                          + " obsolete and are ignored by the application. Consider to"
                          + " use logging level"
                          + " DEBUG to get more details. The ignored value tables are: "
                          + nameListOfValTables
                        );
            nameListOfValTables_ = null;
        }
        
        /* Write a summarizing warning of ignored value tables. */
        if(noSignedSignalValueDescriptions_ > 0)
        {
            errCnt_.warning();
            _logger.warn( "" + noSignedSignalValueDescriptions_
                          + " invalid signed integers were found as"
                          + " part of signal value descriptions. They are tolerated although"
                          + " only unsigned values are permitted. Consider to"
                          + " use logging level DEBUG to get the location of all"
                          + " occurrences"
                        );
            noSignedSignalValueDescriptions_ = 0;
        }
        
        setOfBadAttribNames_ = null;

    } /* End of exitDbc. */
}




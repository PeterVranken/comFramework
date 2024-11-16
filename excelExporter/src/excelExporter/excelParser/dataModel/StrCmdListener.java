/**
 * @file StrCmdListener.java
 * This file implements the listener for an ST4 command interpreter, which provides string
 * operations to the template expansion process, string comparison in the first place.
 *
 * Copyright (C) 2023-2024 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class StrCmdListener
 *   StrCmdListener
 *   interpret
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import java.util.regex.*;
import java.text.*;
import org.stringtemplate.v4.*;
import org.apache.logging.log4j.*;

import excelExporter.excelParser.ErrorCounter;

/**
 * This class implements the listener for an ST4 command interpreter, which provides string
 * operations to the template expanion process, string comparison in the first place.
 */
public class StrCmdListener implements IST4CmdListener< /* TContext */ Integer
                                                      , /* TCmdResult */ Boolean
                                                      >
{
    /** The global logger object for all progress and error reporting. */
    private static Logger _logger = LogManager.getLogger(Info.class);

    /** A string, which precedes all logged messages of the command interpreter. Set to ""
        if no such context string is needed. */
    private String logContext_ = "";
    
    /** The error counter, which counts the template caused errors and warnings. */
    private final ErrorCounter errCnt_;

    /** The offered operation allows only a single argument, while we require two operands.
        We will split the only string argument into operands using this separator. */
    private static String _reArgDelimiter = "==|#";
    
    /** Argument separator _reArgDelimiter as a ready to use regular expression pattern
        object. */
    private static Pattern _rePatternArgDelimiter = Pattern.compile(_reArgDelimiter);
    
    /** Operation mode: Configure the delimiter of operands; no comparison is involved. */
    public static final int operationModeSetDelimiter = 0;
    
    /** Operation mode: Normal, case sensitive string comparison. */
    public static final int operationModeSimple = 1;
    
    /** Operation mode: Normal, case seninsitive string comparison. */
    public static final int operationModeSimpleIgnCase = 2;
    
    /** Operation mode: Regular expression based string comparison. */
    public static final int operationModeRegExp = 3;
    
    /** Operation mode: Regular expression based, case insensitive string comparison. */
    public static final int operationModeRegExpIgnCase = 4;
    
    /**
     * Set the argument delimiter to be used from now on.<p>
     *   The string representation of the needed regular expression is syntax checked and
     * compiled into the internally used representation, a Java Pattern object. An error is
     * reported to the template engine in case of a syntax error.
     *   @param reDelimiter
     * The regular expression to be used as a String.
     */
    private void setDelimiter(final String reDelimiter)
    {
        try
        {
            _rePatternArgDelimiter = Pattern.compile(reDelimiter);
            _reArgDelimiter = reDelimiter;
            _logger.debug( logContext_ + "Argument delimiter changed to \""
                           + _reArgDelimiter + "\""
                         );
        }
        catch(PatternSyntaxException exc)
        {
            errCnt_.error();
            _logger.error( logContext_ + "\"" + reDelimiter + "\" is not a valid regular"
                           + " expression for the new argument"
                           + " delimiter. The value so far, \"" + _reArgDelimiter
                           + "\", is still used. " + exc.getMessage()
                         );
        }
    } /* End of setDelimiter */
    
    
    /**
     * Implementation of operation modes cmpRegExp(I): String matching against a regular
     * expression.
     *   @param string
     * The string, which is checked if it matches to the regular expression.
     *   @param regExp
     * The regular expression. The string is matched to this expression.
     *   @param ignoreCase
     * If true then the regular expression matcher operaterates in case insensitive mode.
     * Otherwise it is case sensitive.
     */
    private boolean cmpRegExp( final String string
                             , final String regExp
                             , final boolean ignoreCase
                             )
    {
        int flags = 0;  
        if(ignoreCase)
            flags |= Pattern.CASE_INSENSITIVE;

        boolean doesMatch = false;
        try
        {
            final Pattern p = Pattern.compile(regExp, flags);
            doesMatch = p.matcher(string).matches();
        }
        catch(PatternSyntaxException exc)
        {
            errCnt_.error();
            _logger.error( logContext_ + "\"" + regExp + "\" is not a valid regular"
                           + " expression to match string \"" + string
                           + "\" against. " + exc.getMessage()
                         );
        }
        return doesMatch;
        
    } /* End of cmpRegExp */
    
    

    /**
     * Create the StrCmdListener object.
     *   @param errCnt
     * Template caused string comparison errors are counted in this object.
     *   @param logContext
     * A string, which precedes all later logged messages. Pass "" if no such context
     * string is needed.
     */
    public StrCmdListener(ErrorCounter errCnt, String logContext)
    {
        errCnt_ = errCnt;
        logContext_ = logContext;

    } /* End of StrCmdListener */


    /**
     * This method implements the command listener.<p>
     *   The operand string is split into parts to extract the two true operands, the
     * strings to compare. Then these two strings are compared.
     *   @return The function returns true if both operand strings are equal (or match in
     * case of regular expression based comparisons) and false otherwise. The result is
     * returned as Java class Boolean, which makes the comparison result available to
     * conditional clauses in an ST4 template. (Whereas a C like integer result
     * wouldn't.)<p>
     *   The function returns null if a particular operation has no result or if the result
     * should not lead to any text emitted during tempalte expansion.
     *   @param modeOfOperation
     * The context information as specified at object creation time. For the string
     * comparison interpreter, it is applied to distiguish different operation modes, like
     * case sensitive versus case insensitive or simple versus regular expression based.
     *   @param stringWithOperands
     * The argument(s) of the operation as found during template expansion in the template.
     */
    public Boolean interpret(Integer modeOfOperation, String stringWithOperands)
    {
        /* Different instances of the interpreter can be operated in different comparison
           modes, e.g., case sensitive or not or normal versus regular expression based. */
        final int mode = modeOfOperation.intValue();
    
        Boolean isEqual;
        if(mode == operationModeSetDelimiter)
        {
            setDelimiter(/* reDelimiter */ stringWithOperands);
            
            /* If we return null instead of the actually promised Boolean then the template
               expression to set the delimiter expands to nothing - which is exactly what
               we need. */
            isEqual = null;
        }
        else
        {
            final String[] opAry = _rePatternArgDelimiter.split( stringWithOperands
                                                               , /* limit */ 2
                                                               );
            if(opAry.length == 2)
            {
                final String op1 = opAry[0].trim()
                           , op2 = opAry[1].trim();
                boolean isEq = false;
                switch(mode)
                {
                case operationModeSimple:
                    isEq = op1.equals(op2);
                    break;

                case operationModeSimpleIgnCase:
                    isEq = op1.equalsIgnoreCase(op2);
                    break;

                case operationModeRegExp:
                    isEq = cmpRegExp(/*string*/ op1, /*regExp*/ op2, /*ignoreCase*/ false);
                    break;
                    
                case operationModeRegExpIgnCase:
                    isEq = cmpRegExp(/*string*/ op1, /*regExp*/ op2, /*ignoreCase*/ true);
                    break;
                    
                default:
                    assert false;
                }
                _logger.debug( logContext_ + "Comparing \"" + op1 + "\" with \"" + op2 + "\": "
                               + isEq
                             );
                isEqual = Boolean.valueOf(isEq);
            }
            else
            {
                assert opAry.length == 1
                       : "Pattern.split is expected to always return at least one string"
                         + " and no more than the limit";

                errCnt_.error();
                _logger.error( logContext_ + "Received \"" + opAry[0].trim() 
                               + "\" as only operand, but need two operands."
                               + " Most likely, your comparison command doesn't "
                               + " contain the argument delimiter, which is " + _reArgDelimiter
                               + ". Please note that this is considered a regular expression"
                             );
                isEqual = Boolean.valueOf(false);
            }
        } /* if(Set delimiter or do comparison?) */
        
        return isEqual;

    } /* End of IST4CmdListener.interpret */

} /* End class StrCmdListener */
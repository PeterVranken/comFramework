/**
 * @file ParseErrorListener.java
 * Error listener as used during parsing process. The listener integrates the DBC parser
 * into the given application.
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
/* Interface of class ParseErrorListener
 *   ParseErrorListener
 *   setErrorCounter
 *   location (2 variants)
 *   syntaxError
 */

package codeGenerator.dbcParser;

import java.util.*;
import org.apache.log4j.*;
import org.antlr.v4.runtime.*;
import codeGenerator.dbcParser.*;


/**
 * Error listener for antlr4.
 *   The listener redirects the output of the antlr4 parser to the Apache logging used in
 * this application.
 */

public class ParseErrorListener extends BaseErrorListener
{
    /** Access the Apache logger object. */
    private static Logger _logger = Logger.getLogger(ParseErrorListener.class.getName());
    
    /** The listener counts all warnings and error in this counter object. */
    private ErrorCounter errCnt_ = null;
    
    /**
     * A new instance of ParseErrorListener is created.
     *   @param errorCounter
     * An error counter to be used is passed in. The counter is incremented on errors but
     * never reset.\n
     *   Pass null if problems should not be counted.
     */
    public ParseErrorListener(ErrorCounter errorCounter)
    {
        errCnt_ = errorCounter;

    } /* End of ParseErrorListener.ParseErrorListener. */


    /**
     * Pass an error counter object.
     *   From now on all reported problems are counted in the passed counter object. It is
     * never reset.
     *   @param errorCounter
     * An error counter to be used is passed in.\n
     *   Pass null if problems should not be counted.
     */
    public void setErrorCounter(ErrorCounter errorCounter)
    {
        errCnt_ = errorCounter;
        
    } /* End of ParseErrorListener.setErrorCounter */
    
    
    
    /**
     * Compose a string, which indicates the location of a parse problem in a uniquely used
     * way.
     *   @param tok
     * The location is extracted from a Token object.
     */   
    public static String location(Token tok)
    {
        assert tok != null;
        return tok.getInputStream().getSourceName() + ":" + tok.getLine() + ":"
               + tok.getCharPositionInLine() + ": ";
               
    } /* End of location */                                     

    
    /**
     * Compose a string, which indicates the location of a parse problem in a uniquely used
     * way.
     *   @param recognizer
     * The file name is extracted from a Recognizer object.
     *   @param line
     * The line number.
     *   @param charPositionInLine
     * The column number.
     */   
    public static String location( Recognizer<?,?> recognizer
                                 , int line
                                 , int charPositionInLine 
                                 )
    {
        assert recognizer != null;
        return recognizer.getInputStream().getSourceName() + ":" + line + ":"
               + charPositionInLine + ": ";
               
    } /* End of location */                                     

    
    /**
     * {@inheritDoc}
     * The main callback of the listener. Another error message is emitted.
     */
     
    @Override public void syntaxError( Recognizer<?,?> recognizer
                                     , Object offendingSymbol
                                     , int line
                                     , int charPositionInLine
                                     , String msg
                                     , RecognitionException e
                                     )
    {
        /* Count errors. */
        if(errCnt_ != null)
            errCnt_.error();

        assert msg != null;
        _logger.error(location(recognizer, line, charPositionInLine) + msg 
                      + (e != null  &&  e.getMessage() != null
                         ? " (" + e.getMessage() + ")"
                         : ""
                        )
                     );
    }

} /* End of class ParseErrorListener definition. */

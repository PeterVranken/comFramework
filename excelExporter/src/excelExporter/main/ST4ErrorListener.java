/**
 * @file ST4ErrorListener.java
 * Error listener as used during template expansion process. This class implements an error
 * listener for the StringTemplate engine, that connects its error feedback with the
 * application's logging and error counting mechanisms.
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
/* Interface of class ST4ErrorListener
 *   ST4ErrorListener
 *   setErrorCounter
 *   location
 *   filterST4ErrMsg
 *   logError
 *   compileTimeError
 *   runTimeError
 *   IOError
 *   internalError
 */

package excelExporter.main;

import java.util.*;
import java.util.regex.Pattern;
import org.apache.logging.log4j.*;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.STMessage;
import org.stringtemplate.v4.misc.ErrorType;
import excelExporter.excelParser.ErrorCounter;


/**
 * ST4 error listener as used during template expansion process.
 *   This class implements an error listener for the StringTemplate engine, that connects
 * its error feedback with the application's logging and error counting mechanisms.
 *   @author Peter Vranken <a href="mailto:Peter_Vranken@Yahoo.de">(Contact)</a>
 */

public class ST4ErrorListener implements STErrorListener
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(ST4ErrorListener.class);
    
    /** The listener counts all warnings and error in this counter object. */
    private ErrorCounter errCnt_ = null;
    
    /** The number of emitted error messages. It's most typical that one and the same error
        is repeated over and over during template expansion. It make much sense to abort
        the expansion after a limited number of errors instead of flooding the application
        log with redundant information. */
    private int noErrs_ = 0;
    
    /** The maximum number of emitted errors. If this number is exceeded, the template
        expansion is aborted. */
    static private final int _maxNoErrs = 10;

    /**
     * A new instance of ST4ErrorListener is created.
     *   @param errorCounter
     * An error counter to be used is passed in. The counter is incremented on errors but
     * never reset.\n
     *   Pass null if problems should not be counted.
     */
    public ST4ErrorListener(ErrorCounter errorCounter)
    {
        errCnt_ = errorCounter;

    } /* End of ST4ErrorListener.ST4ErrorListener. */


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
        
    } /* End of ST4ErrorListener.setErrorCounter */
    
    
    
    /**
     * Compose a string, which indicates the location of a template expansion problem in a
     * uniquely used way.
     *   @return Get the string.
     *   @remark For the template expansion process there's no specific location
     * information. This function rather is a dummy.
     */   
    public static String location()
    {
        return "ST4 template expansion: ";
               
    } /* End of location */                                     

    
    /** 
     * The reported errors from ST4 are ugly in that they contain a lot of information,
     * which is irrelevant in our application context. We try to remove the unwanted
     * information and filter the relevant parts.
     *   @return Get the filtered error message.
     *   @param msg The error message to be filtered.
     */
    private static String filterST4ErrMsg(String msg)
    {
        /* The ST4 error messages contain the stack trace as part of the string. The stack
           trace is meaningless in our context and we try to filter it out. By experience
           it is the trailing part of the message and looking for the first appearance of
           "at <class>" we can relatively safely identify it. */        
        Pattern p = Pattern.compile("(?i)\n\tat [a-z0-9.$(): \t]+\r?\n");
        assert p != null;
        String[] msgPartAry = p.split(/* input */ msg, /* limit */ 2);
        
        // @todo Remove trailing newline (if there's no stack trace one day)
        return msgPartAry[0];
        
    } /* End of filterST4ErrMsg */
    
    
    /**
     * Log a single error and abort template expansion if the maximum number of errors is
     * reached.
     *   @param msg
     * The reported ST4 error message.
     */
    private void logError(STMessage msg)
    {
        /* Count errors internally. */
        ++ noErrs_;
        
        /* The ST4 engine seems to use one or more try/catch blocks around the expansion
           (including this listener) and it seems to use this same error listener again
           inside the catch blocks. When we reach the numeric limit we throw an exception
           to abort the template expansion. The ST4 catch will put our own exception
           message into this listener again and we would log it again, followed by another
           exception thrown from here that make code execution leave the ST4 catch block.
           This may happen several times due to the nested try/catch blocks inside ST4 and
           until we finally reach our own try/catch around the ST4 rendering process. All
           in all we would report the same message several times.
             The way out: We throw the abort exception only once when reaching (==) the
           limit but not always when having exceeded the limit (>). There's no need to
           leave the innermost ST4 catch block again with exception. That catch block will
           properly initiate the abortion, regardless of the surrounding other catch
           blocks.
             The +1 is needed to still consider the logging of the message in the abort
           exception when this function is called again from the innermost ST4 catch block.
           This is a summary and should not be considered in the permitted counting range. */
        if(noErrs_ <= _maxNoErrs+1)
        {
            /* Count errors globally and visibly in the application log. */
            if(errCnt_ != null)
                errCnt_.error();

            assert msg != null;
            _logger.error(location() + filterST4ErrMsg(msg.toString()));
            
            /* Abort the template expansion when the permitted number of errors has been
               reported. Note, the outer if clause will still allow logging the message
               from the here thrown exception. */
            if(noErrs_ == _maxNoErrs)
            {
                throw new RuntimeException(location()
                                           + "Template expansion is aborted due to too many"
                                           + " errors. See previous error messages"
                                          );
            }
        }
    } /* End of logError */
    

    /**
     * {@inheritDoc}
     * The callback of the listener for compilation errors. The reported error is filtered
     * for relevant information and written to the log. The compilation is aborted by a
     * thrown runtime error if the maximum number of errors is reached.
     *   @remark We need to throw a runtime error since the signature of the function is
     * predefined by StringTemplate and unchangeable.
     */
    @Override public void compileTimeError(STMessage msg)
    {
        logError(msg);

    } /* End of compileTimeError */
  
  
    /**
     * {@inheritDoc}
     * The callback of the listener for errors during actual template expansion, i.e. the
     * use of the already compiled templates. The reported error is filtered for relevant
     * information and written to the log. The compilation is aborted by a thrown runtime
     * error if the maximum number of errors is reached.
     *   @remark We need to throw a runtime error since the signature of the function is
     * predefined by StringTemplate and unchangeable.
     */
    @Override public void runTimeError(STMessage msg)
    {
        /* Querying undefined attributes in templates may often point to true template
           errors but is normal template programming technique, too. We must not rate this
           as a counted error. Even a warning it is too strong. If it is purposely used,
           then it'll typically appear dozens of times due to repeated expansion of list
           elements. We offer writing the information on DEBUG level. */
        if(msg.error == ErrorType.NO_SUCH_PROPERTY )
            _logger.debug(location() + filterST4ErrMsg(msg.toString()));
        else
            logError(msg);

    } /* End of runTimeError */
  
  
    /**
     * {@inheritDoc}
     * The callback of the listener for I/O errors. The reported error is filtered for
     * relevant information and written to the log. The compilation is immediately aborted
     * by a thrown runtime exception.
     *   @remark We need to throw a runtime exception since the signature of the function
     * is predefined by StringTemplate and unchangeable.
     */
    @Override public void IOError(STMessage msg)
    {
        logError(msg);
        throw new RuntimeException(location()
                                   + "Template expansion is aborted because of an"
                                   + " unexpected I/O error. See previous error messages"
                                  );
    } /* End of IOError */
  
  
    /**
     * {@inheritDoc}
     * The callback of the listener for internal errors. The reported error is filtered for
     * relevant information and written to the log. The compilation is immediately aborted
     * by a thrown runtime exception.
     *   @remark We need to throw a runtime exception since the signature of the function
     * is predefined by StringTemplate and unchangeable.
     *   @remark It's not sure if this callback will ever be used in our application.
     * Where should the internal error result from?
     */
    @Override public void internalError(STMessage msg)
    {
        logError(msg);
        throw new RuntimeException(location()
                                   + "Template expansion is aborted because of an"
                                   + " unexpected internal error. See previous error messages"
                                  );
    } /* End of internalError */
  
   
} /* End of class ST4ErrorListener definition. */

/**
 * @file ST4CmdInterpreter.java
 * This class is a class for fields in a StringTemplate V4 data model, which behave as a
 * command interpreter. A single String argument got from the template is passed to the
 * interpreter.<p>
 *   The actual interpretation of the argument is not implemented; this class implements
 * the connection of the StringTemplate V4 engine with a listener, which will do the actual
 * command interpretation.<p>
 *   The listener is an interface only and the idea is that a data container in the
 * StringTemplate V4 data model implements this interface. Its implementation of the
 * listener can interpret the commands such that they operate on its data contents.
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
/* Interface of class ST4CmdInterpreter
 *   ST4CmdInterpreter
 *   entrySet
 *   containsKey
 *   get
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import java.text.*;
import java.util.regex.*;
//import codeGenerator.dbcParser.*;
//import org.antlr.v4.runtime.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.ErrorCounter;



/**
 * This class is a class for fields in a StringTemplate V4 data model, which behave as a
 * command interpreter. A single String argument got from the template is passed to the
 * interpreter.<p>
 *   The actual interpretation of the argument is not implemented; this class implements
 * the connection of the StringTemplate V4 engine with a listener, which will do the actual
 * command interpretation.<p>
 *   The listener is an interface only and the idea is that a data container in the
 * StringTemplate V4 data model implements this interface. Its implementation of the
 * listener can interpret the commands such that they operate on its data contents.<p>
 *   Parameter TContext:<p>
 * This is the type of the context object, which is passed to the command interpreter at
 * creation time and which it passes for reference to the connected listener, when doing a
 * command interpretation.<p>
 *   Parameter TCmdResult:<p>
 * The type of the data object, which is returned to the template engine as result of the
 * interpreted command. It needs to be a class, which is supported by the template engine.
 */
public class ST4CmdInterpreter<TContext,TCmdResult> extends AbstractMap<String,TCmdResult>
{
    /** The global logger object for all progress and error reporting. */
    private static Logger _logger = LogManager.getLogger(ST4CmdInterpreter.class);

    /** The error counter, which counts the template caused errors and warnings. */
    private final ErrorCounter errCnt_;

    /** A short string, which precedes all messages to the application log. Used for
        localizing the logged information. */
    private final String logContext_;

    /** The context this command interpreter is used in. The object is provided by the
        listener and meaningless to the command interpreter. */
    private final TContext context_;
    
    /** The connected listener, which does do the actual command interpretation. */
    IST4CmdListener<TContext,TCmdResult> cmdListener_;
    
    /**
     * Creation of a new command interpreter.
     *   @param context
     * This anonymous object is stored by the command interpreter and passed to the
     * connected listener any time it delegates a command to the listener. Having the
     * context information the listener can decide which command interpreter it is talking
     * to in case it should have connected to more than a single command interpreter.
     *   @param cmdListener
     * The connected listener, which does do the actual command interpretation.
     *   @param errCnt
     * Template caused errors are counted in this object.
     *   @param logContext
     * A string, which precedes all later logged messages (debugging only). Pass null if
     * no such context string is needed.
     */
    protected ST4CmdInterpreter( TContext context
                               , IST4CmdListener<TContext,TCmdResult> cmdListener
                               , ErrorCounter errCnt
                               , String logContext
                               )
    {
        context_ = context;
        cmdListener_ = cmdListener;
        assert errCnt != null: "Don't pass null as error counter";
        errCnt_ = errCnt;
        logContext_ = logContext != null? logContext: "";
        
    } /* End of ST4CmdInterpreter */


    
    /**
     * The listener should normally be attached once in the constructor. This fails if
     * interpreter and listener should be the same class. The class cannot construct its
     * super class with a pointer to itself as argument to the super class' constructor. In
     * this case, one will set the listener to null in the constructor and attach it later
     * using this method.
     *   @param cmdListener
     * The connected listener, which does do the actual command interpretation.
     */
    protected void attachListener(IST4CmdListener<TContext,TCmdResult> cmdListener)
    {
        cmdListener_ = cmdListener;
        
    } /* End of ST4CmdInterpreter.attachListener */
     


    /**
     * Deriving a new Map class from AbstractMap requires at minimum overloading the
     * entrySet function. The StringTemplate V4 engine will call this method if the
     * template iterates through the map. Since the command interpreter functionally seen
     * doesn't have map characteristics this can generally be considered a template design
     * error.<p>
     *   We report the problem as a warning only: The template engine normally keeps silent
     * in case of invalid access to fields of the data model and so does the implementation
     * here with respect to the further template expansion.
     *   @return By behavior, the command interpreter isn't a map and consequently, we
     * always return an empty set. This response is inconsistent with the "always true"
     * behavior of {@link #containsKey} and theoretically this contradiction could be
     * recognized by the StringTemplate engine but by experience it doesn't complain about.
     */
    @Override public Set<Map.Entry<String,TCmdResult>> entrySet()
    {
        errCnt_.warning();
        _logger.warn( "{}A map iteration has been tried on this field of the"
                      + " data model. This is possible as the field formally"
                      + " is a Java Map object. However, it acts as a command interpreter,"
                      + " doesn't behave as a map and must not be queried as such"
                    , logContext_
                    );

        /* We return an empty hash set. This should not produce a problem in the
           StringTemplate V4 engine and template expansion wil continue with not generated
           text output - which is the common behavior of StringTemplate in case of invalid
           access to fields of the data model. */
        return new HashSet<Map.Entry<String,TCmdResult>>();

    } /* End ST4CmdInterpreter.entrySet */



    /**
     * Check for presence of a given key value pair in the map.<p>
     *   The StringTemplate V4 engine will use this Map method prior to the query for
     * the value. We basically say "is available" to any key in order to encourage the
     * engine to do the actual query.<p>
     *   There's a pit-fall: The template engine has a two step approach to find the
     * map entries. First, it tries to identify the entry by passing in the template
     * attribute, which is used in the template map operator .(). The attribute is
     * represented in the StringTemplate defined Java class; different template
     * constructs will yield different Java classes. If the engine gets a negative
     * response then it tries again, this time with the attribute rendered as text, as
     * a Java String object. (If the internal attribute representation already is a
     * Java String then no second attempt is made.)<p>
     *   For us, it's impossible to operate on the internal StringTemplate classes.
     * Therefore we reject all queries with key objects, which are not of Java class
     * String and wait for the second attempt - which we always confirm.
     *   @return true for Java String keys, false otherwise.
     *   @param key The key attribute from the template.
     */
    @Override public boolean containsKey(Object key)
    {
        return key instanceof String;
    }


    /**
     * The Map method get is overloaded with the command interpretation. Actually, no map
     * lookup is done at all. Instead, the key is taken as a command string and delegated
     * to the connected listener, which is the actual interpreter.
     *   @return The method returns the result from the listener. This should either be an
     * object, which is renderable by the StringTemplate V4 engine or null.<p>
     *   Null is useful if the command operates only by side-effect: It changes some
     * contents of the data model but should not lead to any emitted text by the template
     * engine.
     *   @param cmd The key attribute from the template. Because of the filtering done
     * in containsKey we will always see a Java String. This is the command string.
     */
    @Override public TCmdResult get(Object cmd)
    {
        assert cmd instanceof String;

        TCmdResult response = cmdListener_.interpret(context_, (String)cmd);
        _logger.debug( "{}Got command string \"{}\", return {} to the template engine"
                     , logContext_
                     , cmd
                     , response
                     );
        return response;

    } /* End of ST4CmdInterpreter.get */

} /* End of class ST4CmdInterpreter */

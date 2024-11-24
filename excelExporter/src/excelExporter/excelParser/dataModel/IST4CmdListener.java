/**
 * @file IST4CmdListener.java
 * The specification of the interface to a command listener, which can connect to a
 * command interpreter and do the actual command interpretation.
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
/* Interface of class IST4CmdListener
 *   IST4CmdListener
 */

package excelExporter.excelParser.dataModel;


/**
 * The interface of a command listener, which can connect to a StringTemplate V4 command
 * interpreter.
 *   @param <TContext>
 * This is the type of the context object, which is passed to the command interpreter at
 * creation time and which it passes for reference to the connected listener, when doing a
 * command interpretation.
 *   @param <TCmdResult>
 * The type of the data object, which is returned by the listener and which is passed on to
 * the template engine as result of the interpreted command. It needs to be a class, which
 * is supported by the template engine.
 */
interface IST4CmdListener<TContext,TCmdResult>
{
    /** The listener gets the command as a String and responds to it by an object of agreed
        type.
          @return The response object is returned to the StringTemplate V4 engine. It'll
        render it as usual yielding the according text output in the generated output. If
        the command should be handled silently by the template engine, i.e., without
        generating any text output, then the listener should return null.
          @param context
        This is the object of agreed type, which had been passed to the command interpreter
        at its creation time. Any time the interpreter invokes this listener, it'll pass
        this object for reference. This way a listener can connect to different
        interpreters at a time.
          @param cmd
        The command string as received from the StringTemplate V4 template under progress
        of the template engine. */
    TCmdResult interpret(TContext context, String cmd);
    
} /* End of IST4CmdListener */

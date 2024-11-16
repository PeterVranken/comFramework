/**
 * @file Identifier.java
 * Names of the objects in the data model are represented by this class. The names are
 * offered in their original notation and as identifiers.
 *
 * Copyright (C) 2016 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class Identifier
 *   setErrorContext
 *   isIdentifier
 *   makeIdentifier
 *   isStrictIdentifier
 *   makeStrictIdentifier
 *   identifierfy
 *   Identifier
 */

package excelExporter.excelParser.dataModel;

import java.util.*;
import org.apache.logging.log4j.*;
import excelExporter.excelParser.ErrorCounter;
import java.util.Random;


/**
 * Names of the objects in the data model are represented by this class. The names are
 * offered in their original notation and as identifiers.<p>
 *   The names of objects are mostly taken from the Excel input data, beginning with the
 * name of the workbook, which may be derived from the file name and continuing with the
 * names of worksheets taken from the tabs in the Excel workbook. These names will
 * typically contain natural elements like blanks or special characters, which can be
 * disturbing, when rendering the data model as kind of program code (source code of
 * programming languages or scripting code). Therefore these names are stored as objects of
 * class Identifier, which behave basically like Strings in a StringTemplate V4 template
 * beside that their normal rendering incorporates the transformation of the natural
 * notation in one, which is compliant with the identifier of most programming languages.
 * The natural notation can still be addressed from a template through a dedicated field.
 */

public class Identifier
{
    /** The global logger object for all progress and error reporting. */
    private static final Logger _logger = LogManager.getLogger(Identifier.class);

    /** A counter for errors and warnings in title management.
          @remark The initial value of this field is null in order to detect by null
        pointer exception if the initialization of this module has not been made. This is
        considered an implementation error. See {@link #setErrorContext}. */
    private static ErrorCounter _errCnt = null;

    /** A formatted string used to precede all logging statements of this module. */
    private static String _logCtx = null;

    /** The ID generator. Each object in the data model gets a unique ID, which can be
        useful for having related data objects in the generated code with individual
        names. */
    private static int _nextObjId = 1;

    /** The natural, given name of the object. This name can be accessed only explicitly
        through this field. In a StringTemplate V4 template you would access the natural
        name of a group like {@code <group.name.givenName>}. */
    final public String givenName;

    /** The name of the object as an identifier as defined in many programming languages,
        e.g. C. The first character may be a letter or the underscore, all others may be
        the same or a decimal digit. In a code generation environment, {@link #ident} can
        be considered a kind of corrected version of {@link #givenName}.<p>
          All groups of unpermitted characters are replaced by a single character x and an
        underscore prepends the resut if it should begin with a digit.<p>
          This field can be accessed as any normal, public filed, but its value is the
        result of {@link #toString}, too. The usual way to refer to the value of this field
        from a StringTemplate V4 template will look like {@code <object.name>} but the more
        explicit {@code <object.name.ident>} would yield the same. */
    final public String ident;

    /** The name of the object as a more restricted identifier as for {@link #ident}. The
        first character may be a letter, all others may be the same or a decimal digit. In
        a code generation environment, {@link #identStrict} can be considered a kind of
        corrected version of {@link #givenName}.<p>
          All groups of unpermitted characters are replaced by a single character x and a
        further x prepends the result if it should begin with a digit. */
    final public String identStrict;

    /** This flag indicates whether the given name of the object and the name of the object
        as identifier {@link #ident} are identical. */
    final public boolean identEquals;
    
    /** This flag indicates whether the given name of the object and the name of the object
        as strict identifier {@link #identStrict} are identical. */
    final public boolean identStrictEquals;
    
    /** This map associates any found C-like identifier with the given names it had made
        from. Required to implement unique associations. */
    private static final Map<String,String> _mapNameByIdent = new HashMap<String,String>(100);
    
    /** This map associates any so far modified name with the found, associated C-like
        identifier. Required to implement unique associations. */
    private static final Map<String,String> _mapIdentByName = new HashMap<String,String>(100);
    
    /** This map associates any found strict identifier with the given names it had made
        from. Required to implement unique associations. */
    private static final Map<String,String> _mapNameByStrictIdent = 
                                                            new HashMap<String,String>(100);
    
    /** This map associates any so far modified name with the found, associated strict
        identifier. Required to implement unique associations. */
    private static final Map<String,String> _mapStrictIdentByName =
                                                            new HashMap<String,String>(100);
    
    /** A random sequence used for disambiguating names. */
    private static final Random randomSequence = new Random();

    /**
     * Set the error context. In rare situations, this module can fail to do what it is
     * expected to. Use this method to define an error reporting channel.
     *   @param errCnt
     * The error counter to be used from now. Must not be null.
     *   @param context
     * A text fragment, which will prepend all logging messages of this module. Can be null
     * if no such common fragment is required.
     */
    public static void setErrorContext(ErrorCounter errCnt, String context)
    {
        assert errCnt != null;
        _errCnt = errCnt;
        
        if(context != null)
            _logCtx = context;
        else
            _logCtx = "";
        
    } /* End of setErrorContext */
    
    
    
    /**
     * Get the next unique ID, useful for a new object of the data model.
     *   @return The ID.
     */
    public static int getUniqueId()
        {return _nextObjId++;}


    /**
     * Test if a name is a strict identifier at the same time.
     *   @return Get the Boolean answer.
     *   @param name The tested name.
     */
    private static boolean isStrictIdentifier(String name)
    {
        assert name != null;
        return name.matches("(?i)^[a-z][a-z0-9]*$");
        
    } /* End of isStrictIdentifier */
    
    
    /**
     * Shape a strict identifier from a given name.
     *   @return Get the identifier.
     *   @param name The given name, which should become an identifier.
     */
    private static String makeStrictIdentifier(String name)
    {
        String ident = name.trim();
        if(ident.length() == 0  ||  (ident.charAt(0) >= '0'  &&  ident.charAt(0) <= '9'))
            ident = "x" + ident;
        
        /* Transformation: White space is translated into nothing and other special
           characters are translated into 'x' */
        ident = ident.replaceAll("\\s+", "");
        ident = ident.replaceAll("[^a-zA-Z0-9]+", "x");
            
        assert isStrictIdentifier(ident);
        return ident;
        
    } /* End of makeStrictIdentifier */
    
    
    /**
     * Test if a name is a less restrictive C-like identifier at the same time.
     *   @return Get the Boolean answer.
     *   @param name The tested name.
     */
    private static boolean isIdentifier(String name)
    {
        assert name != null;
        return name.matches("(?i)^[a-z_][a-z_0-9]*$");
        
    } /* End of isIdentifier */
    
    
    /**
     * Shape a less restrictive C-like identifier from a given name.
     *   @return Get the identifier.
     *   @param name The given name, which should become an identifier.
     */
    private static String makeIdentifier(String name)
    {
        String ident = name.trim();
        if(ident.length() == 0  ||  (ident.charAt(0) >= '0'  &&  ident.charAt(0) <= '9'))
            ident = "_" + ident;
        
        /* Transformation: White space is translated into an underscore and other special
           characters are translated into 'x' */
        ident = ident.replaceAll("\\s+", "_");
        ident = ident.replaceAll("[^a-zA-Z0-9_]+", "x");
            
        assert isIdentifier(ident);
        return ident;
        
    } /* End of makeIdentifier */
    
    
    
    /**
     * Modify a character string carefully, such that it becomes an identifier.
     *   @return Get a string, which resembles name and which begins with a letter or maybe
     * underscore and continues with a series of such or decimal digits.
     *   @param name
     * The processed character string; usually the name of an object.
     *   @param isStrict
     * Boolean switch if the strict identifier or the less restrictive from the C syntax is
     * supported. If {@code true} then the more strict kind of identifier is supported,
     * which doesn't allow the underscore.
     */
    public static String identifierfy(String name, boolean isStrict)
    {
        /* Configuration: When to report an error in very pathologic cases? */
/// @todo Change limit to 10000. 1 is meant just for testing; 0 should be tested, too
        final int maxAttemptsToDisambiguate = 1;
        
        /* The maps to be used depend on the kind of identifier. Both kinds spawn
           independent namespaces. */
        final Map<String,String> mapNameByIdent, mapIdentByName;
        if(isStrict)
        {
            mapNameByIdent = _mapNameByStrictIdent;
            mapIdentByName = _mapStrictIdentByName;
        }
        else
        {
            mapNameByIdent = _mapNameByIdent;
            mapIdentByName = _mapIdentByName;
        }
            
        /* Do nothing if condition is already met. */
        if(isStrict && isStrictIdentifier(name)  ||  !isStrict && isIdentifier(name))
            return name;
        
        /* Look in the map if we'd already "identifierfied" the name. If so, return the found
           identifier. */
        String ident = mapIdentByName.get(name);
        if(ident != null)
            return ident;
        
        /* This is a new name to be "identifierfied". Start with the static solution, which
           still disregards possible ambiguities. */
        final String identStem = isStrict? makeStrictIdentifier(name): makeIdentifier(name);
        
        /* Disambiguation: Look into the map of natural names if there is any, which has
           the found identifier already in use. If so, modify the candidate and do all of
           this in a loop. */
        ident = identStem;
        int idx = 1;
        while(mapNameByIdent.get(ident) != null)
        {
            /* Avoid an infinite loop in pathologic cases. */
            if(idx > maxAttemptsToDisambiguate)
            {
                _errCnt.error();
                _logger.fatal( "{}No unambiguous identifier could be found for object name"
                               + " {}. Sorry no way out, you will need to modify the heavily"
                               + " ambiguous input data"
                             , _logCtx
                             , name
                             );
                return ident;
            }
            

            /* Shape next candidate. We use a two stage approach. First we try to find the
               next free number but after a while we try random appendixes in case there
               should be a systematic blocking. */
            if(idx <= maxAttemptsToDisambiguate/2)
                ident = identStem + (isStrict? "x": "_") + idx;
            else
                ident = identStem + (isStrict? "x": "_") + randomSequence.nextInt(1000000);
            
        } /* End of while(Still an in-use-name?) */
        
        /* Here we have a new, still unused identifier. Add it to the maps and return it. */
        if(_logger.isDebugEnabled()) 
        { 
            final int maxLenNames = 32;

            final String truncatedName = name.length() > maxLenNames
                                         ? name.substring(0, maxLenNames) + "[..]"
                                         : name
                       , truncatedIdent = ident.length() > maxLenNames
                                          ? ident.substring(0, maxLenNames) + "[..]"
                                          : ident;
            _logger.debug( "{}Associate \"{}\" with identifier {}"
                         , _logCtx
                         , truncatedName
                         , truncatedIdent
                         );
        }            

        String prevObj = mapNameByIdent.put(ident, name);
        assert prevObj == null;
        prevObj = mapIdentByName.put(name, ident);
        assert prevObj == null;
        
        return ident;
        
    } /* End of identifierfy */
    



    /**
     * A new instance of Identifier is created from a normal String.
     *   @param name
     * The natural name of the object designated by the new Identifier object.
     */
    public Identifier(String name)
    {
        givenName = name;
        ident = identifierfy(name, /* isStrict */ false);
        identStrict = identifierfy(name, /* isStrict */ true);
        identEquals = name.equals(ident);
        identStrictEquals = name.equals(identStrict);

    } /* End of Identifier.Identifier. */


    /**
     * Return the name of the object that is designated by this Identifier as an
     * identifier.
     */
    public String toString()
        {return ident;}

} /* End of class Identifier definition. */





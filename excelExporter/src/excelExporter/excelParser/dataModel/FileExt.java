/**
 * @file FileExt.java
 * 
 *
 * Copyright (C) 2015 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
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
/* Interface of class FileExt
 *   FileExt
 */

package excelExporter.excelParser.dataModel;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * The Java class File already brings some valuable information about the name of a
 * file and parts of it, which is accessible from a StringTemplate V4 template - but in
 * this derived class it is even extended.<p>
 *   See <a href="https://docs.oracle.com/javase/7/docs/api/java/io/File.html">java.io.File</a>
 *   The practice of code generation requires some more details on file names. Here's an
 * extended class, which adds these to the Java File class in order to make this
 * information easily accessible from a StringTemplate V4 template.<p>
 *   Note, to fetch the information from the Java base class
 * <a href="https://docs.oracle.com/javase/7/docs/api/java/io/File.html">java.io.File</a>
 * you need to be aware of the way, the StringTemplate V4 engine accesses public methods of
 * a class: The access is restricted to public methods with names starting with "get",
 * followed by a capitalized character. The function must not have any function argument.
 * From a template, such a function is accessed without the "get" and with lower case first
 * character, see these examples:<p>
 *   - {@code <info.output.canonicalFile>} to access {@code File.getCanonicalFile()}
 * for the currently generated file<p>
 *   - {@code <info.templateFile.absoluteFile>} to access {@code File.getAbsoluteFile()}
 * for the currently applied template group file
 */
public class FileExt extends File
{
    /** Static pattern for splitting a file name in stem and extension. */
    final private static Pattern _reRawNameExt = Pattern.compile("(.*)\\.([^.]+)");

    /** Static pattern for splitting a file name stem in mnemonic and name stem. */
    final private static Pattern _reFNameMnm =
                                            Pattern.compile("(?i)([a-z][a-z0-9]*)_(.+)");

    /** The file name without extension (and without the separating dot). null if
        the name stem is blank (as in ".ext"). */
    public String rawName = null;

    /** The file name stem, the name without path, menmonic and extension. Does not
        include the dot and underscore, which separate mnemonic and extension. null if
        the name stem is blank (as in ".ext"). */
    public String nameStem = null;

    /** The file name extension without the dot. null if there is no extension. */
    public String ext = null;

    /** The file name prefix or menmonic. It is defined as the matching characters of
        the first (true) group of the regular expression<p>
          "(?i)^([a-z][a-z_0-9]*)_.+$"<p>
        on the file name stem. If the regular expression doesn't match then this field
        is null.<p>
          The use case of this file name part is to shape a kind of name space for
        global objects in plain C code. Normally, a three character menemonic ("mnm")
        of the file or module name is used and this will then prepend the names of all
        global objects in the generated C code. */
    public String mnm = null;

    /**
     * Split a file name into parts and store all details about the file name and path
     * in public fields.
     *   @param fName The file name.
     */
    public FileExt(String fName)
    {
        super(fName);

        /* Separate the raw file name from the extension. */
        Matcher matcher = _reRawNameExt.matcher(getName());
        if(matcher.matches())
        {
            rawName = matcher.group(1);
            ext = matcher.group(2);
        }
        else
        {
            /* There's no separating dot, everything is the stem and the extension is
               blank. */
            rawName = getName();
            ext = null;
        } 

        matcher = _reFNameMnm.matcher(rawName);
        if(matcher.matches())
        {
            mnm = matcher.group(1);
            nameStem = matcher.group(2);
        }
        else
        {
            /* The pattern mnm_nameStem doesn't match, the mnemonic is undefined. */
            mnm = null;
            nameStem = rawName;
        } 
    } /* End of FileExt.FileExt */


    
    /**
     * The default representation of the file name in user feedback.
     *   @return Get the file name with extension but without path.
     */
    public String toString()
    {
        return getName();
        
    } /* End of toString */
    
    
} /* End of class FileExt */



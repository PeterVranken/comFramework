/**
 * @file Node.java
 * Part of the output data model for information rendering by StringTemplate: The network
 * communication node as described by a DBC file.
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
/* Interface of class Node
 *   Node
 */

package codeGenerator.dataModelListener;


/**
 * The data structure describing a communication node in the bus.
 *   An object of this class contains the results of parsing a single DBC input file. The
 * object is part of the nested data structure passed to the template engine for rendering
 * the information in the wanted format.<p>
 *   The basic design principle is the use of public members because such members can be
 * directly accessed from the output templates. Boolean decisions are either expressed as
 * explicit public Boolean members or by references to nested member objects, which are
 * null if an optional member is not used or applicable: StringTemplate can query public
 * Boolean members or check for null references.
 */

public class Node extends NetObject
{
    /** Default constructor. */
    public Node()
    {}

} /* End of class Bus definition. */





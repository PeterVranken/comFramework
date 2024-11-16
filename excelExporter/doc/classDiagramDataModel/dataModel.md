[TOC]

# The excelExporter Data Model #

## Introduction ##

Successful use of excelExporter depends on full transparency of its data
model. The data model is the internal representation of the parsed
information as it is passed on to the StringTemplate V4 template engine.
With other words: How do the Java objects *Cluster* and *Info* look like,
which contain the information parsed from all Excel input files?

The StringTemplate V4 engine itself is well described. The documentation
tells how to access fields and methods of the rendered Java objects, how
to iterate along fields that are collections classes, etc. All of this is
useless if there's no clear awareness which actual fields, methods and
collections of those the application uses.

Further reading of this text requires that you have read the
[StringTemplate V4 manual](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/doc/ST4-270115-0836-52.pdf) and understood the concepts of the template
expressions, i.e. how to access fields and methods of Java objects and how
to use the iterations along collections.

Basically, the documentation of the data model uses Javadoc. The filter is
set to public objects since the template engine is restricted to accessing
public fields and methods. For simple and straightforward data models
would this be sufficient and adequate. For excelExporter it isn't for some
reasons:

- The data model uses recursive structures. This is not a contradiction
  with Javadoc but degrades the transparency of the documentation
- excelExporter makes intensive use of the template engine's interface to
  Java *Map* object's. Unfortunately, this interface is barely mentioned in
  the StringTemplate V4 manual and some relevant aspects of the interface
  are not mentioned at all
- The description of Java *Map* objects in Javadoc leaves a lot open about
  the key-value relationship, where do the values come from, which keys
  to use
- There are semantic relations between the configuration of the
  application (i.e. the command line) and the data model. This relates to
  naming, sorting and grouping and can't reasonably be described in Javadoc

This Wiki page tries to close the gaps.

## Overview ##

The basic data concept of excelExporter is the row object. Any data row of
any of the parsed worksheets of any of the parsed workbooks forms one row
object. This object is a collection of cell objects, where each owned cell
object represents one non empty cell of that row in the Excel input.
Eventually, only the cell objects provide access to the actual Excel input
data.

The row objects are held in groups of such. Groups can be nested and form
a data tree this way. Each row object belongs into one and only one group.
The grouping structure and which row object goes into which group is
controlled by both, the application configuration and the Excel data.

The core of the data model is such a group, the collection of row objects.
This collection is modelled by Java class *RowObjectContainer*. The row
object container _has_ a sorted list of row objects and it _is_ a map of
other row object containers. The former are the row objects in that group
and the latter are the nested sub-groups of the group.

An Excel worksheet is a *RowObjectContainer*. (Actually it is a derived
class with some additional fields, like name and path of the Excel file.)
In the most simple case, if no grouping is applied then we have a single
*RowObjectContainer* (the worksheet itself), where the list of row objects
contains all rows of the sheet and where the map of groups is empty; all
rows are owned by the root group and no nested sub-groups exist. In more
complex situations there will be more than one worksheets and those will
have a tree of sub-groups and the row objects will be distributed among
these sub-groups.

This [inner part of the data model](#The-inner-part-of-the-data-model-worksheet-and-RowObjectContainer), the worksheet, is depicted as UML
diagram in section UML diagrams at the bottom of this page.

The [outer part of the data model](#The-outer-part-of-the-data-model-cluster-workbook-worksheet-groups), shown as UML in the same section,
relates to the cluster (the root of all), the workbooks, which consist of
worksheets and the worksheet groups, which can be used to agglomerate the
same set of worksheets in another, customized way: Instead of relating the
sheets to the them containing books can they be grouped according to their
meaning.

## The skeleton of the data model ##

This section describes the main path through the data model from top to
bottom, i.e. from the input attribute of the StringTemplate V4 template
expansion to the contents of the cells of the Excel input. Here, we
restrict the description to the main elements of the data model and do not
mention all their fields. We focus on statements like: "the workbook
contains sheets" but don not mention the other properties of the workbook
like name or file name of the Excel file. The details of all elements can
be found in the UML diagrams in section [UML diagrams](#uml-diagrams) at
the bottom of this page and in the [Javadoc](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/doc/dataModel/index.html?overview-summary.html) of the data model.

### From cluster to workbook ###

The root of the data model is the cluster. The cluster is a map of all the
parsed Excel workbooks. The cluster object is passed to the StringTemplate
V4 template as an attribute; having this attribute *cluster*, the workbook
object can be accessed with StringTemplate V4's dot operator as
*<cluster.myBooksName\>*. Please note, the field *myBooksName* in this
expression is the literal name of the workbook, where the name is
specified by the application configuration (the command line). The name
can be explicitly set but if nothing is said about the book's name then it
is derived from the file name (name without path and extension).

Example: Your command line says: *--input-file-name dataDictionary.xlsx
--workbook-name Interfaces*. The given name of the parsed workbook is
literally "Interfaces" and inside a StringTemplate V4 template you would
access this workbook as *<cluster.Interfaces\>*.

Note: By convention, all item names specified in the application input
(both by configuration and by Excel file contents), in particular object
names, should begin with a capital. The predefined elements of the data
model generally have names beginning with a lower case character.
Following this convention it's easy to see what's built-in functionality
and what are input data dictated structural elements. Furthermore, clashes
between input data and predefined symbol names are avoided.

The detailed information about cluster and workbook are found as Javadoc
description of the Java classes *Cluster* and *ExcelWorkbook*,
respectively.

### From workbook to worksheet ###

The workbook is a map of all worksheets, which have been parsed in the
given Excel workbook file. Inside a StringTemplate V4 template and having
a workbook attribute one can access the worksheet with the dot operator
with an expression like *<book.mySheetsName\>*. Please note, the field
*mySheetsName* in this expression is the literal name of the worksheet,
where the name is specified by the application configuration (the command
line). The name can be explicitly set but if nothing is said about the
worksheet's name then it is taken from the tab in the Excel file.

Example: Your command line says: *--open-worksheet-selection --worksheet-name
InterfaceA*. The given name of the parsed worksheet is
literally "InterfaceA" and inside a StringTemplate V4 template you would
access this worksheet as *<book.InterfaceA\>*.

If this example would belong into the context of the example from the
previous section, then an expression like
*<cluster.Interfaces.InterfaceA\>* would be valid.

The detailed information about the worksheet is found as Javadoc
description of the Java class *ExcelWorksheet*.

### From worksheet to row objects ###

In the data model all Excel input is generally organized in row objects. A
row object is a map of cells. It represents one row from a worksheet.

The worksheet is a so called row object container. The row object
container is *not* a map of row objects; this wouldn't work since row
objects don't have a name and the lookup operation would be difficult to
define. Instead, the worksheet (as any other row object container) has a
list of owned row objects and a template expression can be used to iterate
along the list to process all of them.

Example: Be there a template *renderRowObject()*, which describes how to
process a single row of Excel input. The list of row objects of a
worksheet attribute *sheet* would be rendered by a StringTemplate V4
expression like *<sheet.rowAry:renderRowObject()\>*.

Continuing the previous example the row objects of the worksheet
*InterfaceA* could be processed by an expression like
*<cluster.Interfaces.InterfaceA.rowAry:renderRowObject()\>*

The detailed information about the row object is found as Javadoc
description of the Java class *RowObject*.

### From worksheet to groups and sub-groups ###

The worksheet is a map of groups of row objects. (If excelExporter's
grouping facilities are not applied then the map won't contain any
entries. Please refer to command line argument *--is-grouping-column*.)
Here we have a recursive data structure: The groups in the map are
themselves row object containers having (sub-)groups.

The elements of a map are accessed from a StringTemplate V4 template with
the dot operator. The key is the name of the object, in this case the
given name of the group. "Given name" relates to that groups' names are
defined by cell contents in the Excel file, the cells in the column/s,
which is/are specified to be (a) grouping column/s in the application
configuration (see *--is-grouping-column*).

Example: Have the worksheet *sheet* two groups *GroupA* and *GroupB* and
have *GroupB* a sub-group *GroupC* then these groups could be accessed
from a StringTemplate V4 template with expressions like *<sheet.GroupA\>*,
*<sheet.GroupB\>* and *<sheet.GroupB.GroupC\>*. The last expression
demonstrates the recursive structure of row object containers.

Continuing the previous example the row objects of a sub-group *Signals*
of worksheet *InterfaceA* could be processed by an expression like
*<cluster.Interfaces.InterfaceA.Signals.rowAry:renderSignal()\>*. The
fictive name *renderSignal* of the invoked sub-template implies that the
row objects in the groups represent signals.

The worksheet as any other row object container has a list of its
sub-groups, too. While the map access to the sub-groups is well suited for
direct name based access to particular groups can the list of sub-groups
be applied to iterate all groups, regardless of how they are named. This
is useful if the processed data items are not described by a single row
of the Excel worksheet but by a bunch of those, which are grouped. The
name of the group would mean the name of the data item and could not be
anticipated in a template.

The detailed information about the row object containers is found as Javadoc
description of the Java class *RowObjectContainer* and its base class
*ObjectMap*.

### From row object to data contents ###

The last link in the chain to eventually access the data in the parsed
Excel file is the cell object. The cell object is the representation of a
single cell of the Excel worksheet. It is owned by the according row
object.

It is a basic assumption of our data model that all Excel input is row
oriented. (Column-wise defined data is still processable but with degraded
adequateness and thus readability of the template code, see sample
[columnWise](https://sourceforge.net/p/comframe/code/HEAD/tree/excelExporter/trunk/samples/columnWise/).) A row is considered describing one data item and each cell of
a row is considered describing one particular property of the item. The
name of this property is the name of the column. The name of the column
usually is the text content of the top most cell of the column but it can
be explicitly given, too. This is subject to the application configuration
(see command line arguments *--column-name* and *--index-title-row*).

A row object is a map of cell objects. The key to access a particular cell
is the name of the property the cell represents. The cell object itself is
an ordinary Java object with predefined fields, which give direct access
to the data contents of the represented Excel worksheet cell. There are
different fields to access the cell contents in different representations,
among more as simple text, as number or as Boolean. Using the number
representation would for example enable the use of the StringTemplate V4
features to render numeric information in a particular, controlled way.
The number could e.g. be rendered with a specific number of significant
digits, as hexadecimal or whatsoever. Please refer to the
[StringTemplate V4 manual](https://sourceforge.net/projects/comframe/files/ST4-270115-0836-52.pdf/download) for details.

Example: The Excel worksheet have columns titled "Name", "DataType" and
"Description". A sub-template rendering the row object from our previous
example could look as follows:

~~~~~~~~~~~~~~~~~
renderSignal(sig) ::= <<
/* Signal <sig.Name>: <sig.Description> */
extern <sig.DataType.ident> <sig.Name>;
>>
~~~~~~~~~~~~~~~~~

Note, in the template fragment are the cell objects mostly rendered
without addressing to a particular field of the cell object. By default, a
cell object is rendered by the contents of its field *text*. The template
expression *<sig.Name\>* from the example is the cell object *Name*
as a whole. It expands to the same as the more explicit
*<sig.Name.text\>* would. Only the expression
*<sig.DataType.ident\>* chooses a particular representation of the
contents of the cell object; *ident* means to render the text contents as
a valid C identifier. Blanks and special characters are replaced by
underscores.

The detailed information about the cell object is found as Javadoc
description of the Java class *CellObject*.

### The global worksheet groups ###

So far, the design of the data model followed the hierarchy of the input.
The cluster is the agglomeration of workbooks, a workbook has sheets, a
sheet has rows and groups of such, a row has cells. This path through the
data is appropriate if excelExporter is configured to implement a particular
input form using Excel as human-machine interface. The user is expected to
provide the input in an Excel file and this file is processed.

excelExporter can also be useful to process a (large) set of Excel files
at a time. If they all together constitute a large set of data items then
it won't be relevant how the worksheets relate to the distinct workbooks,
the task just is to render the data items in all the sheets. This
iteration is supported by the concept of global worksheet groups. A
configurable filter delegates a worksheet after parsing into a particular
group; the selection can be made by name pattern or by index in the book.
Any number of groups and related filters can be defined.

In any useful configuration will the filters be defined such that a group
only contains worksheets of same structure. Data processing becomes as
easy as iterating along the worksheets in the group.

Example: The workbooks are expected to have two kinds if worksheets, those
of the first kind called "Interface <Name\>" and the other one called
"Attributes". <Name\> stands for a particular interface's name. The
configuration would use a regular expression like "Interface .\*" as name
pattern to delegate the first kind of sheets into a group named
"Interfaces". (See command line arguments *--association-by-tab* and
*--group*.) All of these sheets will form the global worksheet group
*Interfaces*. The root data model object *Cluster* has a map of all global
worksheet groups; lookup is done by name of the group. Each worksheet group
is modeled as a list of [ExcelWorksheet](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/doc/dataModel/excelExporter/excelParser/dataModel/ExcelWorksheet.html) objects.

The processing of all interface sheets, regardless of how many
input files have been parsed and regardless of how many sheets "Interface
<Name\>" are found in each input file could look like:

*<cluster.sheetGroupMap.Interfaces:renderInterfaceSheet()\>*

Note, despite of delegating the worksheets "Interface <Name\>" into a
global group are they still found in the data model hierarchy inside their
parent workbook. It's of course possible to do the iteration along all
books and filter them for matching interface sheets and render those. This
will yield the same output but would require more template code. Moreover,
the required filter operation is not trivial in StringTemplate V4
expression syntax; it's much easier to express this by worksheet group
filters on the command line.

### Shortcut: The one and only worksheet ###

A specific but quite common use case of excelExporter is supported by some
shortcuts. If in your application exactly one worksheet is parsed or if
exactly one workbook is parsed or if a parsed workbook has exactly one
parsed worksheet then the fields *theOnlyWorkbook* and *theOnlyWorksheet*
become available, i.e. they are no longer *null* as otherwise. Both fields
are available in the context *Cluster* and *theOnlyWorksheet* is available
in the context of any workbook. In the easiest case, where you parse one
worksheet from a single Excel input file, your template could directly
access the parsed data by an expression like:

*<cluster.theOnlyWorksheet.rowAry:renderRowObject()\>*

## Lists of objects and sorting ##

The data model has a lot of lists of data objects. Most of these lists can
be sorted. The rendering process, which applies the StringTemplate V4
iteration operator ":" to these lists, naturally follows the sort order and
so does the generated output. Whether sorting should be applied and which
sort criteria to use is subject to the application configuration. The
default is to leave objects in the order they have in the input.

Sorting mostly applies to the lists *rowAry* of row objects, which are
owned by the groups, the *RowObjectContainers*. Row objects have a number
of properties, represented by the owned cell objects. Sorting the list of
row objects relates to these properties.

Example: If a row object has the properties *A* and *B* then the list of
row objects can be sorted in ascending lexical order of *A* and then - if
two row objects should have the same value of *A* - according to
descending numeric values of *B*. *A* is said to have the higher sort
priority than *B* in this example.

Row objects' properties relate to named columns in the Excel input.
Consequently is the configuration of sorting considered an attribute of
worksheet columns. Please refer to command line arguments
*--sort-order-of-column* and *--sort-priority-of-column*.

Groups are normally accessed by name through the map interface. However,
to support iterative processing of arbitrary group and sub-group
structures has the data model a list of groups, too. See [*RowObjectContainer*](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/doc/dataModel/excelExporter/excelParser/dataModel/RowObjectContainer.PseudoFieldName.html)'s
field *groupAry*, which is an alias of the base class' *itemAry*. Groups
are accessed by name and they don't have further properties; consequently,
groups can only be sorted by name and specifying a sort-priority is
useless and not allowed. Sorting of groups is configured by the same
command line argument *--sort-order-of-column*.

A cluster has the list *bookAry* of workbooks; it's an alias of the base
class' *itemAry*. Sorting of this list according to the name of the
workbook objects is controlled by command line argument
*--sort-order-of-workbooks*.

A workbook has the list *sheetAry* of workbooks; it's an alias of the base
class' *itemAry*. The global worksheet groups are implemented as lists of
worksheet objects. Sorting of these lists according to the name of the
worksheet objects is controlled by command line argument
*--sort-order-of-worksheets*. Note, this is a global setting; individual
sort configuration on a per workbook or per worksheet group base is not
supported.

Sorting data items is discussed in detail in the other Wiki page
[Grouping and sorting](https://sourceforge.net/p/excelexporter/wiki/Grouping%20and%20sorting/).

## The Map interface ##

The typical StringTemplate V4 application renders an object of predefined
data structure and of variable contents. The templates will use
expressions like *<object.fieldName\>* to access the data contents of the
data model. With excelExporter the data structure strongly depends on the
input data. In sample *firstSample*, template file [firstSample.stg](https://sourceforge.net/p/comframe/code/HEAD/tree/excelExporter/trunk/samples/firstSample/firstSample.stg), last lines,
you see that the data from the cells in the Excel worksheet columns
"Country" and "Capital" are accessed by the template expressions
*<row.Country\>* and *<row.Capital\>*, respectively. (*row* is the row
object in the given context.) The field names are not predetermined but
dictated by the contents of the Excel input. Evidently, there can't be a
Java class in the data model, which has the fields *Country* and
*Capital* - another Excel file would require another Java class. This is
solved by applying StringTemplate V4's interface to Java class *Map*. If
*row* is a Java *Map* then the dot operator in *<row.prop\>* no longer
means the access of field *prop* of object *row* but a map lookup
operation with key *prop*. We implement e.g. *RowObject* as a Java *Map*
and store all cells in the map. The cells are associated with their column
title (i.e. the title is the key) and so will *<row.Country\>* return the
cell object from column "Country".

The map based design has been chosen because of the syntactic identity of
ordinary field access and map lookup in StringTemplate V4's expression
syntax. This became already evident in the example before. It holds even
more if it comes to the worksheet and group relation. A worksheet is a map
of its groups and a group is a map of its sub-groups; getting to the list
of row objects of a deeply nested sub-group looks quite straightforward in
a template: *<sheet.group.subGroup.subSubGroup.rowAry\>*. Please note, in
real template code would "sheet", "group", "subGroup" and "subSubGroup" be
literal names of your Excel file's elements, the tab naming the sheet and
the cells naming the groups. Despite of using terms from your individual
Excel input does the StringTemplate V4 template still look as if there was
a dedicated Java class definition reflecting your Excel input.

### Pseudo-fields ###

A workbook is a map of parsed worksheets, which can thus be addressed to
by name. This leads to natural looking and adequate template expression
code. However, it's as natural that a workbook object has more properties
than the contained worksheets. In our data model it offers for example the
file name information about the parsed Excel input file. This information
can't be attached to the object as ordinary field; for objects recognized
as Java *Map* won't the StringTemplate V4 engine grant access to public
fields. The implementation of a *Map* object in our data model overcomes
this with its "pseudo-fields". These are predefined map values, which are
associated with the name of the pseudo field. Since the StringTemplate V4
map interface uses the same dot operator for map lookup and for field
access is the use of the pseudo fields in a template expression identical
to true fields. However, two important differences between true fields and
pseudo-fields remain.

#### Name clashes ####

The most evident implication are name clashes. Any pseudo-field is a
predefined map value, which occupies a particular key - the name under
which it can be retrieved from the map. No other object of this name can
be stored in the same map. Parsing would fail at that point. The object
*ExcelWorkbook* has e.g. a pseudo-field *exists* and it would be impossible to
parse and render the Excel input if there was a worksheet named "exists".
excelExporter would report an error and terminate.

In practice, this is not a problem. There are pseudo-fields with quite
common names, like "name". In these cases we decided to append the
underscore to the pseudo-field name in order to effectively avoid name
clashes. The name of a worksheet object would be accessed by an expression
like *<sheet.name_\>* not  *<sheet.name\>*.

Moreover, the convention is to let all input data dependently named
objects have names beginning with capitals while all symbols from the data
model begin with a lower case character. In the example before it would
mean to name the worksheet "Exists" rather than "exists". And even if the
tab in the Excel input would say "exists" then would it still be possible
to explicitly use a differing name for the worksheet object by means of
application configuration (see command line argument *--worksheet-name*).

#### Javadoc for pseudo-fields ####

The second implication of using pseudo-fields instead of true fields is
the documentation. Normally, it's quite adequate to document a
StringTemplate V4 data model with Javadoc. All public fields are
described and one knows how to use them from the template. For
pseudo-fields it depends. We designed the Java source code under the
consideration to still benefit from Javadoc. The internal implementation
of pseudo-fields uses true, public fields, even though public is not
required as visibility and even where methods would sometimes be more
adequate. This ensures that the actually inaccessible true fields appear
in the Javadoc; from a template it looks as if they were available but in
fact a template expression will get them by map lookup of the pseudo-field
of same name.

Furthermore, the implementation of pseudo-fields in the shared base class
*ObjectMap* builds on an enumeration of all pseudo-field names. This
enumeration is individual for each subclass and a Javadoc description is
available. The named values of the enumerations briefly summarize the
meaning of the pseudo-field and they have a reference to the more
detailed documentation of the internal, true field, which implements the
pseudo-field.

The name of the enumeration class of an object map *ParticularMapClass*
would be found in the Javadoc as class *ParticularMapClass.PseudoFieldName*.
Note, *ParticularMapClass* is meant a place holder for an existing
subclass of *ObjectMap* but *PseudoFieldName* is meant literally.

For example, you will find the description of the pseudo-fields of the
worksheet object as [ExcelWorksheet.PseudoFieldName](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/doc/dataModel/excelExporter/excelParser/dataModel/ExcelWorksheet.PseudoFieldName.html).

### Map iteration ###

Maps are iterable data collections. The StringTemplate V4 engine supports
the iteration operator ":" for Java *Map* objects. The visited data items
are the keys of the values stored in the map and the template at the right
hand of the operator gets the key as attribute.

Our Java *Map* class *ObjectMap* implements its pseudo-fields as
predefined map values. However, the implementation ensures that the names
of these pseudo-fields won't be returned by the StringTemplate V4 engine's
map iteration. If you apply the map iteration e.g. to a workbook object
then you will get the names of all the parsed worksheets but you won't see
the name of a pseudo-field like "excelFile".

Example: The properties of a row object can be rendered generically by
using the map iteration along all cell objects. Draw attention to the
indirection *(propName)* to retrieve the cell object by the name
got by the map iteration:

*<rowObj:{propName|Property <propName\> has the value <rowObj.(propName).text\><\n\>}\>*

The keys are iterated in ASCII order. This is implicit behavior of the
implementation of the applied Java *Map* class and is not subject to the
application configuration.

### Empty map, test on existence ###

In StringTemplate V4 data models it's quite common to represent
unavailable optional elements or empty sets by a Java *null*. In the
template we have typical conditional expressions like:

*<if(setOfObjects)\><setOfObjects:renderObject()\><else\>//
Set of objects is empty!<endif\>*.

Such a construct will fail if the set is implemented as Java *Map* as it
holds for many of our data model's items. The StringTemplate V4 engine
interprets the expression *<if(map)\>* as a "if(map is not null and
not empty)". The second part of the condition makes the difference. In our
data model play map objects an important role, which are existent as Java
object (thus not *null*) but which do not have any stored key, value pair.
The most prominent example is the group of row objects (Java class
*RowObjectContainer*, e.g. the worksheet object), which doesn't have a
sub-group. As a map it is empty (sub-groups would be the stored values)
but it still has its pseudo-fields, which - among more - give access to
the row objects in the group. The typical expression from above would fail
for maps:

*<if(group)\><group.rowAry:renderRowObject()\><endif\>*

*group* may exist and *group.rowAry* may be not empty but no row object
would be rendered. To overcome this we added the static Boolean field
*exists* to the set of pseudo fields of our *Map* objects. This field
always returns *true*. And even if a map *M* is *null* then *<M.exists\>*
is still a valid template expression; it evaluates to *false*. Therefore

*<if(group.exists)\><group.rowAry:renderRowObject()\><endif\>*

will exactly do, what it is expected to.

Note, not using the *.exists* belongs to the typical pitfalls when writing
StringTemplate V4 templates for excelExporter.

## The Info object ##

TODO

## UML diagrams ##

### The outer part of the data model: Cluster, workbook, worksheet, worksheet groups ###

![UML_RowObjectContainer](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/doc/classDiagramDataModel/dataModel-topLevel.jpg)

### The inner part of the data model: Worksheet and RowObjectContainer ###
![UML_RowObjectContainer](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/doc/classDiagramDataModel/dataModel.jpg)


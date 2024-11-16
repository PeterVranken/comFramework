[TOC]

# Grouping and sorting data

## Preamble

Reading of this text requires that you have read the
[StringTemplate V4 manual](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/doc/ST4-270115-0836-52.pdf) and understood the concepts of the template
expressions, i.e. how to access fields and methods of Java objects and how
to use the iterations along collections.

## Simple tables

The use of excelExporter is intuitive only as long as the data
organization is most simple: The Excel input contains an ordinary
table; a line represents a data item and the columns have the
meaning of a property of the item and all items are structurally
identical.

The data model is designed to support ordinary tables as intuitive as
possible, by accessing Excel workbook and worksheet and column/property by
name using the dot operator of StringTemplate V4. This makes template
writing straight forward. The rows - the data items - are iterated with a
template expression like:

~~~~~~~~~~~~~~~~
<cluster.myBook.mySheet.rowAry:renderRow()>
~~~~~~~~~~~~~~~~

In this example there will be a sub-template *renderRow(row)*, which
renders a single data item. The template can directly access the
properties of the data item by name, like

~~~~~~~~~~~~~~~~~
renderRow(row) = <<
The value of property A is <row.A> and the value of property B is <row.B>.
>>
~~~~~~~~~~~~~~~~~

Note, the template code fragment makes use of the default representation
of a data item's property. For numeric or Boolean properties this might be
insufficient and an expression like *<row.A.n\>* or *<row.A.bool\>* could be
more appropriate.

A lot of simple tasks can be solved with this pattern but in many
situations there will be relations between data items of same or different
kind and excelExporter's grouping facilities will be required to model
this.


## Grouping

### How grouping works

In general, excelExporter considers a row of the Excel input an integral
data item. These data items are referred to as row objects. A row object
has properties, which are stored as name, value pairs. The name of a
property is taken from the title of a column and the value is the cell
contents found in that row and that column.

This basic pattern holds under all circumstances, regardless of grouping
or not grouping data items. The only difference if grouping is applied is
the location in the data model, where the row object is stored. In the
case of simple tables there's only a single row object container, which
represents the complete worksheet. All row objects are owned by this
container; an iteration of the container's *rowAry* will visit all rows of
the Excel input. If grouping is applied then there are more, subordinated
row object containers. All containers form a tree; the worksheet is the
root. All row objects appear only once, in one of the nested containers.

It has to be understood that the specification of the targeted container
(or group) is part of the row object itself; a sub-set of its properties
designates the aimed container. In particular, there's no concept of
different kinds of rows like one kind specifying a group and the other one
specifying the data items in the group. When using groups it still holds
that any Excel row specifies a single row object.

Note, the terms container and group are interchangeable in our context.
Group is more abstract and relates to the logical data organization; data
items are organized in nested groups of such. Container rather relates to
the implementation of a group; there is a Java class *RowObjectContainer*,
which implements the group. The term group is advantageous, more natural
for the explanations given here but the term container can't be avoided
since the data model is documented using Javadoc, which is strictly
implementation related.

The location of a row object in the tree of nested groups is controlled by
a path concept. Columns in the Excel input relate to properties of the row
objects. Specific columns can be marked on the application command line as
being *grouping columns*. The list of grouping columns defines a path
schema. The actual values of the related properties form the path to the
location of the row object.

#### Example

Here is an example. The following table represents the complete Excel
worksheet. It has six columns and six data rows. (The first row holds the
column titles but not a data item.) On the command line, the columns B, D
and A have been specified in this order as grouping columns, i.e. the cell
contents in the 3rd, 5th and 2nd column form the path:

   i | A | B | C | D | E
  ---|---|---|---|---|---
   1 |   |   | t |   | 0
   2 | P |   | s |   | 34
   3 | u | v | x | y | 12
   4 | U | V | X | Y | -1
   5 |   | V | q | Y |
   6 | Y | V | q |   | 1

After parsing there are six row objects in five groups in the data model.
(The count of groups includes the root container, the worksheet.) Each row
object has up to six properties. (Empty cells mean an omitted property or
a *null* value during template expansion.) Here's the list of row objects
with their property values and the path to the container, which owns them:

  1. Path=/: i=1, C=t, E=0
  - Path=/P/: i=2, A=P, C=s, E=34
  - Path=/v/y/u/: i=3, A=u, B=v, C=x, D=y, E=12
  - Path=/V/Y/U/: i=4, A=U, B=V, C=X, D=Y, E=-1
  - Path=/V/Y/: i=5, B=V, C=q, D=Y
  - Path=/V/Y/: i=6, A=Y, B=V, C=q, E=1

Nomenclature: The designation /a/b/c/ in the list means that the row
object is stored in group "c", which is a sub-group of group "b", which is
a sub-group of "a", which is a sub-group of the root container, which is
the representation of the worksheet. The row objects in the nested group
"c" could be iterated with a template expression like:
*<sheet.a.b.c.rowAry:renderRow()\>*.

In our example, the template expression *<sheet.V.Y.rowAry:{r|i=<r.i\> }\>*
would visit the last two rows of the worksheet and render them as: "i=5 i=6 ".

Please note the following:

  - The root container, the worksheet itself, is owner or location of the
    row object if the cells in all grouping columns are left empty
  - Empty cells in grouping columns are generally ignored, a path like
    //P// is considered the same as /P/. This explains why the 5th and 6th
    row object end up in the same group
  - The order of grouping columns matters. B, D and A have been declared
    grouping columns in this order and consequently is the path of e.g.
    the 3rd row object /v/y/u/ but not /u/v/y/
  - The path forming properties are still normal properties of the row
    object, i.e. they are accessible as any other property, they aren't
    filtered or hidden. Rendering a row object can be done in full
    awareness of the path, where it is located.

### Data modeling is a must

The following note is essential when considering the use of excelExporter
for automation tasks: The successful use of excelExporter requires
explicit modeling of the data and its representation in the Excel input
file(s). excelExporter is not capable to operate on arbitrary existing
Excel files.

If your automation task needs to process structured data then you will
have to design the format of the Excel input file in a way that it
represents your data and that it can be rendered by excelExporter. Excel
input format and StringTemplate V4 templates for rendering the information
will be designed hand in hand. There's little chance that excelExporter
can render your existing Excel files - with the (frequent) exception of
[simple tables](#Simple-tables).

Another note relates to the complexity of the input. In theory is
excelExporter capable to model any tree of data items. In practice you
will experience problems when the nesting is more than three levels deep.
The StringTemplate V4 expressions to access the data in a structured way
will get too complex and development and maintenance of the templates will
become the limitation.

### Aimed use cases

#### Parent/Child relationships between data items of different kinds

The intended use case of the grouping is modeling data item hierarchies,
trees of items of same or different kind. The concepts are show in the
sample [xls2dbc](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/samples/xls2dbc/).

The sample models the communication on the CAN bus as used in automotive
engineering. The CAN bus has nodes, which exchange CAN messages. A message
consists of signals. Signals have certain pre-defined properties like name
and binary encoding and all elements can have application specific
attributes. These elements form a tree of inhomogeneous data items. The
sample shows how to model this tree in an Excel file and how to let
excelExporter generate the network database file (*.dbc) from the Excel
input.

The sample has three differing implementations, which all lead to the
output of the same network database file. They differ in using one
worksheet for holding all data items or different worksheets for data
items of different kind and in that they reference child items in
different ways. The implementations are held in the three sub-folders
*using...*. Please have a look at all three implementations. The
differences are outlined in the files *readMe.txt* in the three
sub-folders.


#### The group with a solitary row object

The group with a single contained row object is a special case but a quite
important design pattern. Above it had been said that groups barely have own
properties. They have a name and that's nearly all. The rest is the list of
owned row objects. However, if we have a one-by-one relation between
groups and row objects, it becomes possible to consider both together as
the representation of a single data item and the properties of
the single row object are the properties of that item. Now we have data
items which can have direct child items.

Remember, a group is not constituted by a special row or special command.
It is constituted by the cell content of a specific column in the row.
Take the following example of a worksheet:

  Name  | Gender | Age
  ------|--------|----
  Laura | female | 45
  John  | male   | 23
  George| male   | 34
  Tim   | male   | 78
  Sarah | female | 23

The worksheet be not a [simple table](#Simple-tables) since column *Name* be declared
a grouping column on the command line. The root container doesn't have any
own row object but it has five sub-groups. Since all cells in column
*Name* are different have all of these groups exactly one row object, the
row object from the row which constitutes the group at the same time. The
group is the model of a person and the columns *Gender* and *Age* hold
properties of the person.

Addressing to the usual fields of the data model we would have a
StringTemplate V4 expression like *<first(sheet.John.rowAry).Age\>* to get
John's age. To support the use case under consideration the data model has
an alias for the pattern *first(group.rowAry)*: The same result is yielded
with the expression *<group.prop\>* or *<sheet.John.prop.Age\>* in our
example. The age can be read out as if it were a property of the group
*John*. In fact *prop* is nothing else than a copy of the first element in
the group's list *rowAry* but the naming of this field makes the template
code much better readable in this use case.

Another consideration applies. Getting the age of *John* in a template
expression will barely by useful. Any real application will query the age
of *someone* and *someone* will be specified by the contents of some cell
in the Excel input, i.e. by a property of some related row object. In
practice will the construct above only appear in combination with
StringTemplate V4's indirection operator .(), e.g.

~~~~~~~~~~~~~~~~~~~~~
renderAge(name) ::= "<name> is <sheet.(name).prop.Age> years old"
~~~~~~~~~~~~~~~~~~~~~

The table in the example becomes a map of the data items described by the
rows. The key for the lookup operation is the content of the cell in the
grouping column; the person is looked up by her name in our example.
(Insofar is the chosen example inadequate; it requires unambiguous given
names.)

Note, this concept works just the same with two or more grouping columns.
The prerequisite for safe lookup becomes that the tuples are unambiguous.
Extending the example with another grouping column *Last Name* we could
write the template like:

~~~~~~~~~~~~~~~~~~~~~
renderAge(givenName,lastName) ::= <<
<givenName> <lastName> is <sheet.(lastName).(givenName).prop.Age> years old
>>
~~~~~~~~~~~~~~~~~~~~~

The expression *<sheet.(lastName).(givenName)\>* requires that column
*Last Name* has been declared a grouping column before column *Name*.

## Sorting groups and row objects

The elements in the lists in the data model can be sorted. By default the
elements will have the order of appearance at parsing time. Sorting can be
applied to lists of workbooks, lists of worksheets, lists of groups of row
objects and to lists of row objects. It is controlled by the command line
arguments named *--sort-...*.

### General hints

The sort operator supports three kinds of sorting, lexical, ASCII and
numerical. The main difference between the first two is the handling of
the character case. Lexical sorting will mostly do what you intend, ASCII
will place upper case names before lower case, regardless of the
characters; *Zorro* will come before *adam*. More differences can result
from language specific special characters.

Numeric sorting relates to the numeric (floating point) value of the
contents of the cells, which are compared. In lexical sort order a data
item with property value "10" would come before a "9" but in numerical
order you'd get 9 before 10. If the data type of the cell, which is
subject to a comparison, is text then a conversion to a floating point
number type is tried first. If the complete cell contents resolve to a
number then the conversion result is the input to the comparison. If the
conversion fails then the data item with the non number cell is placed
behind the one with the numeric cell. All data items with non number cells
are sorted lexically among each other.

All sort orders are offered in inverse direction, too. Note, the inverse
direction doesn't necessarily mean an inverted sequence of objects. For
numeric sorting it still holds that numbers come before non numbers; both
blocks will have the inverse order but the complete sequence obviously
doesn't have.

Here, in the context of grouping only the sorting of groups and row
objects is further discussed.

### Sorting groups

Groups barely have properties besides their name. This is why sorting
groups is restricted to sorting according to their name. (The special case
of [groups having a solitary row object](#The-group-with-a-solitary-row-object), where the only row object's
properties are considered the group's properties at the same time is not
supported by the sorting concept.) Numeric sorting is technically
supported but won't have noticeable significance since group names are true
names in most use cases.

Specifying that groups should be sorted is done in the command line
context of the column, which constitutes the groups. If
a column is specified to be a grouping column with command line argument
*--is-grouping-column* then the argument *--sort-order-of-column* will
enable sorting and select the sort order. This sort order is applied to
all groups, which are created by cell entries found in this column.

If the configuration specifies a single column as grouping column then all
created groups will have the same parent, the container object that
represents the complete worksheet. All groups are siblings in the same
list of groups owned by this container. In a StringTemplate V4 template
this list is accessed by an expression like *<sheet.groupAry\>* and this
is the list, which will be sorted. An iteration like
*<sheet.groupAry:{gr|Group: <gr.name_\><\n\>}\>* will display the sorted
list.

In general, sorting groups means to sort the field *groupAry* of the
parent container. If several columns are specified to be grouping columns
then we get nested groups. The groups created from the cell contents of
the second grouping column will no longer all have the same parent
container. Sorting is done among all siblings belonging to the same
parent.

#### Ambiguities in group sorting configuration

A specific complexity arises. In typical use cases all child groups of the
same parent group will be constituted by cell entries of the same column
but this is not necessarily the case. See section [Example](#Example)
above: The cells from the path scheme can be left empty.

Have a look at the following table, which depicts a hypothetic parsed
worksheet. Be columns *Path1* and *Path2* declared grouping columns in
this order:

   i | Path1 | Path2 | PropA | PropB
  ---|-------|-------|-------|------
   1 | G1    | GG1   | Hello | 3.14
   2 | G2    | GG1   | abc   | 1
   3 |       | G1    | World | 2.71
   4 | G3    | GG1   | foo   | -1
   5 | G1    |       | x     | 0.99
   6 | G3    | GG2   | bar   | 99

In this example row objects i=1, i=3 and i=5 reside in the same group *G1*
(i=1 in a nested subgroup *GG1* of *G1*), which is a child of the root
group, the worksheet object. Its siblings are *G2* and *G3*. Row objects
i=1 and i=5 make the reference to group *G1* in column *Path1*, while row
object i=3 makes the reference to the same group in column *Path2*. The
sort configuration for group *G1* and its siblings needs to be taken from
two different columns' attributes - and can easily be contradictory.

This is resolved by the simple rule, that successfully running
excelExporter requires unambiguous configuration of sorting of groups. The
application run is otherwise aborted with error message and no output
shall be generated.

For our example it would mean to either specify the same sorting for both
columns *Path1* and *Path2* on the command line or to move the cell
contents "G1" in row i=3 from column *Path2* to column *Path1*. Note,
while the latter has no impact on the grouping structure of the data model
will it alter the properties of row object i=3, which can be painful in a
real application with more meaningful column titles.

### Sorting row objects

The row objects, which are owned by a group (which is the root container
representing the entire worksheet in simple cases), can be sorted, too.
This relates to the order of elements in the data model fields
*<group.rowAry\>*.

The row objects are sorted with respect to the values of their properties.
Since properties are related to or constituted by columns is it natural to
find the sorting configuration again in the columns' attributes. Actually,
the same command line argument *--sort-order-of-column* is applied, this
time to one or more columns, which are *not* grouping columns.

If command line argument *--sort-order-of-column* is applied to a single
property column then the row objects will be sorted according to their
values of the property, which relates to that column.

Sorting of row objects is generally done in all the groups. It is
generally impossible to specify the sort order of row objects on a per
group base. All groups from a worksheet will always use the same sort
configuration. Different worksheets can have different column attributes
and can consequently use differing sort configurations.

#### Prioritized sorting

Row objects can have several properties constituted by several columns.
All of these can be specified to do sorting. All sorted columns get a
priority; the priority can either be set explicitly with command line
argument *--sort-priority-of-column* or it is determined by the order of
appearance of the column attributes specification on the command line:
columns specified earlier get the higher priority.

If sorting is done according to more than one property then the row
objects are basically sorted according to the values of the property with
highest priority; only those row objects, which have the same property
value will be decided according to the sorting property with next lower
priority.

Prioritized sorting of row objects is demonstrated in sample [sortedTable](https://svn.code.sf.net/p/comframe/code/excelExporter/trunk/samples/sortedTable/).

# excelExporter - Rendering Excel Spreadsheets as Text

## Introduction

excelExporter is a Java application that can translate the information
found in one or more Excel workbooks into manifold textual
representations. excelExporter is a general purpose text rendering
application for Excel spreadsheets.

excelExporter is distributed as part of
[comFramework](https://github.com/PeterVranken/comFramework/wiki) project.
Here, it is considered an auxiliary code generator targeting the handling
of data dictionaries and the generation of related interfaces.

However, the use of excelExporter is in no way restricted to the context
of CAN interface generation in automotive software development and,
indeed, excelExporter can be installed and used independently from the
rest of comFramework. Inside the downloaded comFramework archive, it has
its own folder and installation guide. Just cut it out. It'll be useful
for various automation tasks in other software development environments,
too.

You can render the information in the Excel input for example as:

-   HTML or LaTeX for reporting and documentation.
-   Various XML formats for interfacing with other applications.
-   C/C++ or any other textual programming languages.

In- and output are described separately in the configuration of the tool.
First you will specify a set of Excel input files. All of these are parsed
and merged to one large data structure, often referred to as data model.
Input files of same or of different format are supported. ("Format" refers
to the structure of the Excel files with respect to contained sheets and
columns.)

Secondary, you will define a list of output files. The specification of
each output file is associated with the specification of a StringTemplate
V4 template group file. The templates control the output generation and
this is how the same information is rendered, e.g., once as HTML code and
once as C program fragment.

## Documentation

### Command line interface of excelExporter

The usage of the tool is explained in the application's command line usage
text: Begin with running excelExporter with command line option `--help`.

The command line interface of the application has the following concept:

The arguments form groups. A group of successive arguments can specify an
input file, another group can specify another input file or an output file
and so on. The beginning of a group of arguments is recognized by a
specific argument, the principal argument of the group. The usage text
typically says "this argument opens the context of ...". Groups of
arguments are nested. A group can have child groups. A group of arguments
ends either at the end of the command line or when another group of same
or higher level (i.e., parent level) begins. Naturally, the same command
line switches can be repeatedly used, once in each group of same kind.

Such a group of command line arguments or a "context" actually is the
representation of an object in the parameter tree of the application. This
is the model behind the parameter tree:

- Root elements are either Excel input file specifications, specifications
  of generated output files or worksheet templates.
- The input file specification contains the Excel file name and it has any
  number of worksheet selection objects as children.
    - A worksheet selection specifies one or more worksheets for parsing.
      All sheets or any sub-set of sheets of a workbook can be parsed.
      Selection can be made by name or by index.
- An output file specification contains the file name and information
  about the StringTemplate V4 template to be applied.
- A worksheet template is a set of rules how to interpret one or more
  worksheets. It can be applied to a particular worksheet or to several of
  those, from either one or from several input files. It describes how the
  data of a worksheet is organized in terms of groups and sub-groups. It
  has any number of column attributes objects as children.
    - A column attributes object specifies properties of a column, like
      name and sort order.

Besides the command line arguments from a group or context there are
"traditional" command line arguments, which relate to the run of the
application as a whole, like logging and verbosity settings. The
application usage text says they belong to the global context. The global
context isinitially open and ends forever when one of the above mentioned,
specific contexts is opened. With other words, the "traditional" arguments
need to come first.

Please note, different to the common GNU command line interface this
application demands a blank between the switch and its value. For example
`-oMyOutputFile.c` would be rejected, whereas `-o MyOutputFile.c` would be the
correct specification of a generated output file.

The application's command line arguments relate to the definition of in-
and output files and how to parse the input. This should be understood
after reading the application usage text. To successfully use the
application one still needs to understand the internal representation of
the read input data (i.e., the data model) and the way it is rendered in
the output files. The next sections explain the available, related
documentation.


### The data model

The internal representation of the parsed input information, called "data
model", is explained in detail in Wiki page
[excelExporter's Data Model](https://github.com/PeterVranken/comFramework/wiki/The-excelExporter-Data-Model)
and it is documented as a
[Javadoc of the complete data structure](https://petervranken.github.io/comFramework/excelExporter/doc/dataModel/index.html?overview-summary.html "Data model for StringTemplate V4").
The same is found in your local installation, please click on file
`excelExporter/doc/dataModelForStringTemplateV4.html`.

In the Javadoc you find the documentation of all public elements of the
data structure that are accessible from the StringTemplate V4 templates.
The data structure is deeply nested, and actually, it are even two data
structures, which are passed to the rendering process:

-   The parsed information forms an object of class `Cluster`.
-   The information about output files plus some environmental information
    is put into an object of class `Info`.

You will study the Javadoc pages to see, which pieces of information to be
used from within a template.

Please note, the Javadoc describes the different elements (classes) of the
data model. Their nesting is not fully transparent from the Javadoc since
recursive structures are involved. The actual structure of the data
model will depend on (and reflect) the structure the input data is
organized in. Only in the most simple case it's a linear list of so called
[RowObjects](https://petervranken.github.io/comFramework/excelExporter/doc/dataModel/excelExporter/excelParser/dataModel/RowObject.html),
which represent a single row from the Excel input
file and which consist of so called
[CellObjects](https://petervranken.github.io/comFramework/excelExporter/doc/dataModel/excelExporter/excelParser/dataModel/CellObject.html).
In all other cases the actual data structure depends on your input data,
on the format of your Excel file and on your application configuration
(which all needs to be consistent with one another).

The explanation of the data model and how its structure depends on Excel
input and application configuration is given in the Wiki page mentioned
before.

Another source of knowledge about the data model and how to access its
elements is the investigation of the templates (`*.stg`) in our samples, see
files `excelExporter/samples/.../*.stg` in your local installation or
online at
<https://github.com/PeterVranken/comFramework/tree/main/excelExporter/samples>.

### The StringTemplate V4 templates

The technique of rendering the information held in a `Cluster` and an
`Info` object is well documented, see previous section. The two objects
are passed to the StringTemplate V4 template engine and this engine is
fully documented and needs to be understood. Please refer to
<https://github.com/antlr/stringtemplate4/blob/master/doc/index.md> or find a printable version of the
documentation either
[online](https://github.com/PeterVranken/comFramework/blob/main/excelExporter/doc/ST4-270115-0836-52.pdf)
or locally in your installation as `excelExporter/doc/ST4-270115-0836-52.pdf`.

Please note, as a matter of experience, you will have to read the
StringTemplate V4 documentation entirely before you can start to
successfully develop your first useful template. StringTemplate V4 is
powerful and convenient but not self-explaining.

Studying the template group files (*.stg) from our samples is another
important source of information, see
[excelExporter/samples/.../*.stg](https://github.com/PeterVranken/comFramework/tree/main/excelExporter/samples/).

### The Wiki pages

An additional source of documentation are the Wiki pages of the project.
The Wiki pages shade a light at some most relevant, selected issues; a
comprehensive, self-contained (printable) manual is not planned. As of
today, December 2024, we have the following discussions in the Wiki
pages, which directly relate to excelExporter:

-   [Overview on the comFramework project](https://github.com/PeterVranken/comFramework/wiki/Home/)
-   [excelExporter's Data Model](https://github.com/PeterVranken/comFramework/wiki/The-excelExporter-Data-Model/)
-   [Grouping and sorting](https://github.com/PeterVranken/comFramework/wiki/excelExporter,-Grouping-and-sorting/)

## Installation

excelExporter is a Java application. The installation is as simple as
unpacking an archive and optionally setting an environment variable. It is
described in detail either
[online](https://petervranken.github.io/comFramework/excelExporter/doc/installation.html)
or locally in your installation as file
`excelExporter/doc/installation.html`.

## What's new

### Release 1.4

Update of data model: The support of string comparison operations has
been extended. `<info.str>` now supports regular expression based search
and replacement.

### Release 1.3.1

Integration of the latest release of StringTemplate V4, which is 4.3.4.

### Release 1.3

This version increment was made to make a clean break when the project
moved from SourceForge to GitHub. In fact, there are no significant
changes to excelExporter since the last release, but there are a number of
smaller corrections, mostly to the documentation.

### Release 1.2

Update of data model:

The cell representation now offers a JSON and widely C/C++ compatible text
representation, so that string objects of these contexts can be created.

Update of data model: The support of string comparison operations has
been adopted from codeGenerator. `<info.str>` is a new attribute, which
offers some comparison operations, including matching to regular
expressions. Text fields of the data model (e.g., cell contents) can be
compared with one another or with expected contents.

### Release 1.1

Update of data model: The cell object now contains the field `is`, which
is a map with a single key/value pair. The key is the text contents of the
cell and the value is a Boolean true. This allows to do string compares
with the cell contents in a template. The use case is using cells as
selector for different, enumerated options; those cells must contain one
out of a limited set of predefined possible character strings and the
template can check, which one it is.

### Release 1.0.5

Integration of latest release of StringTemplate V4, which is 4.3.3.

### Release 1.0.4

Migration of source code to Java Open JDK 18.0.

### Release 1.0.3

Text output, which contains non ASCII characters is now saved to file as
UTF-8. (Used to be a not specified, default code page.)

Master sample `renderTable` re-designed. This actually is a small
convenience application, which strongly simplifies the use of
excelExporter for the most common use cases.

Fix: We found an Excel workbook, which let to still unexpected runtime
errors of POI when evaluating the cell contents. These Java
RuntimeExceptions had made the application immediately abort rather then
reporting the problem. Now, the evaluation of affected cells is skipped
with warning and no reasonable cell contents are accessible from the
templates.

excelExporter has been migrated to the recent release of StringTemplate
V4, which is 4.3.1. The only noticeable difference should be a fix of the
white space handling in StringTemplate. It used to be very difficult to
precisely control the generation of blank lines in the output. With this
fix, the newline characters found in the templates are accurately conveyed
into the generated output.

Caution, this will likely mean that this version of the code generator
will produce different output when using the same, existing templates. The
differences are however restricted to blank lines and should not matter in
C code generation environments.

### Release 1.0

After more than a year of hassle-free productive use of the tool we
decided to make it a release and change the major field of the revision
designation to one - even if there's only little new functionality.

New sample renderTable: This folder contains pre-configured files for the
most common use case of excelExporter. Copy these files to have a fully
operational starting point for your excelExporter application - out of the
box and without struggling with the complex and cumbersome command line of
the tool.

Error handling improved. Internal errors of StringTemplate V4 during
rendering are now streamlined with the application logging. Before, they
were printed in the log (together with the bulky but in the given context
meaningless Java stack frame) but were not recognized or counted by the
application.

Minor improvements of documentation.

Fix: The field `isBlank` of a CellObject in the data model was not
operational. Blank cells are not put into the data model at all; so any
access to this field would evaluate to either false (for normal, non blank
cells) or null (for blank cells). Now the data model offers the logically
inverse as isNotBlank: This evaluates to true for normal, non blank cells
or null (interpreted as false) for blank cells. This change is the only
reason for the increase of the version number of the data model.

There should be no functional change for existing templates, which make
use of the no longer existing field CellObject.isBlank: The behavior
should be as wrong as it used to be. However, using the new field
CellObject.isNotBlank, they can now be repaired.

### Release 0.18

New cell type `date` introduced for spreadsheet cell objects. Time and
date designations read from an Excel spreadsheet can now be rendered with
the StringTemplate V4 DateRenderer and using format strings, which are
similar to the Java class SimpleDateFormat.

A new sample has been added, timeAndDate, which demonstrates the new
capabilities.

### Release 0.17

Documentation extended and many corrections made on documentation.

The row object container got the new field prop to support the common use
case of groups with a single row object.

### Release 0.16

The initial release. Full functionality of the application but preliminary
state of samples and documentation.

# Download

After transition from SourceForge to GitHub, excelExporter is no longer
provided as separate package. You get it as an element of the comFramework
download, see [here](https://github.com/PeterVranken/comFramework/releases).

However, if really nothing from comFramework but excelExporter is relevant
to you, then you can easily cut it out of the comFramework archive. The
installation of excelExporter doesn't depend on the rest of comFramework.
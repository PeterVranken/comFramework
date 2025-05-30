# comFramework - Code Generator

## Introduction

This archive contains the binary distribution of the code generator from
the comFramework project. The code generator can translate the information
found in CAN network databases into nearly any textual representation and
in particular into C source code, which is perfectly tailored to the needs
of your specific (embedded) environment.

Interface implementations and particularly the often required pack and
unpack functions for message composition from signals and decomposition
into signals can be generated.

However, C source code is just one out of a long list of useful
representations. You can render the information also as:

-   HTML or LaTeX for documentation
-   Excel Spreadsheet (`*.csv` only)
-   Various XML formats like AUTOSAR arxml or TargetLink configuration files
-   M Scripts for the MathWorks Embedded Coder configuration
-   A2L files for support of calibration interfaces
-   Python, Perl or other interpreted languages for whatever purpose

In- and output description are decoupled in the parametrization of the
tool. First you will specify a set of CAN network databases (`*.dbc`
files). All of these are parsed and merged to one large data structure.
The use case of merging several databases is the code generation for
network nodes, which are connected to several buses. (Merging different
databases is of limited value if they are inhomogeneous with respect to
the use of attributes. If the evaluation of frame properties like send
period and transmission characteristics becomes quite different on
different buses then it might be better to run the code generator several
times and let it generate differently designed source files.)

Secondary, you will define a list of output files. The specification of
each output file is associated with the specification of a StringTemplate
V4 template file. The template controls the output generation and this is
how the same information is rendered once as C source code and once as
Embedded Coder interface description (just to put it into an example).


## Documentation

### Command line interface of code generator

The usage of the tool is explained in the usage text: Begin with running
the code generator with command line option `--help`. The same information
can be found on-line and a bit more in detail on the
[project Wiki](https://github.com/PeterVranken/comFramework/wiki/The-Command-Line-of-the-comFramework-DBC-Code-Generator "Usage of DBC code generator").

The command line interface of the code generator has the following
concept:

The arguments form groups. A group of successive arguments can specify an
input file, another group can specify another input file or an output file
and so on. The beginning of a group of arguments is recognized by a
specific argument, the principal argument of the group. The usage text
typically says "this argument opens the context of ...". Naturally, the
same command line switches can be repeatedly used, once in each group of
same kind.

Additionally to the use of command line switches in the context of a
group, one can put the same prior to opening the first group context: Now
they have the character of a default value for all later groups, which do
not themselves specify these particular arguments.

Finally, there are "traditional" command line arguments, which relate to
the run of the application as a whole, like logging and verbosity
settings.

Please note, different to the common GNU command line interface this
application demands a blank between the switch and its value. For example
`-oMyOutputFile.c` would be rejected, whereas `-o MyOutputFile.c` would be
the correct specification of a generated output file.

The application's command line options mainly relate to the definition of
in- and output files. The rest is the internal representation of the read
input data and the way it is rendered in the output files. The next
sections explain the available, related documentation.


### The data model

The internal representation of the parsed input information, called "data
model", is documented as a
[Javadoc of the complete data structure](https://petervranken.github.io/comFramework/codeGenerator/doc/dataModel/index.html?overview-summary.html "Data model for StringTemplate V4").
The same is found in your local installation, please click on file
`codeGenerator/doc/dataModelForStringTemplateV4.html`.

Here, you find the documentation of all public elements of the data
structure that are accessible from the StringTemplate V4 templates. The
data structure is deeply nested, and actually, it are even two data
structures, which are passed to the rendering process:

-   The parsed information forms an object of class Cluster.
-   The information about output files plus some environmental information
    is put into an object of class Info.

You will study the Javadoc pages to see, which pieces of information to be
used from within a template.

### The StringTemplate V4 templates

The technique of rendering the information held in a `Cluster` and an `Info`
object is well documented. The two objects are passed to the
StringTemplate V4 template engine and this engine is fully documented.
Please refer to
<https://github.com/antlr/stringtemplate4/blob/master/doc/index.md> or
find a printable version of the documentation online as
<https://github.com/PeterVranken/comFramework/blob/main/codeGenerator/doc/ST4-270115-0836-52.pdf>
or as `codeGenerator/doc/ST4-270115-0836-52.pdf` in your local
installation.

Please note, as a matter of experience, you will have to read the
StringTemplate V4 documentation entirely before you can start to
successfully develop your first useful template. StringTemplate V4 is
powerful and convenient but not self-explaining.

Another source for this knowledge is the investigation of the sample
templates: See files `codeGenerator/samples/.../*.stg` in your local
installation or online at
<https://github.com/PeterVranken/comFramework/tree/main/codeGenerator/samples>.

### The Wiki pages

An additional source of documentation are the Wiki pages of the project.
The Wiki pages shade a light at some most relevant, selected issues; a
comprehensive, self-contained (printable) manual is not planned. As of
today, Nov 2024, we have the following discussions in the Wiki pages,
which directly relate to the DBC code generator:

-   [Overview on the comFramework project](https://github.com/PeterVranken/comFramework/wiki/Home/ "comFramework - About this Project")
-   [The command line of the DBC code generator](https://github.com/PeterVranken/comFramework/wiki/The-Command-Line-of-the-comFramework-DBC-Code-Generator "Usage of DBC code generator")
-   [Prerequisites, limitations and pitfalls](https://github.com/PeterVranken/comFramework/wiki/Prerequisites,-Limitations-and-Pitfalls "Java version, known issues")
-   [Compatibility of the DBC parser with real *_.dbc_ files](https://github.com/PeterVranken/comFramework/wiki/Reusage-of-Code,-Standalone-Use-of-DBC-Parser-and-Compatibility#Compatibility)
-   [Reusability of the DBC file parser in other contexts/applications](https://github.com/PeterVranken/comFramework/wiki/Reusage-of-Code,-Standalone-Use-of-DBC-Parser-and-Compatibility "Reusage of code, standalone use of DBC parser and compatibility")
-   [Options for conditional code generation](https://github.com/PeterVranken/comFramework/wiki/Conditional-Code-Generation-vs-Generation-of-Conditional-Code "Conditional code generation versus generation of conditional code")
-   [The use of attributes](https://github.com/PeterVranken/comFramework/wiki/How-to-access-Attributes-in-the-Network-Database "How to access attributes in the network database?")
-   [A common pattern how to combine handwritten code with auto-generated
    code in a beneficial way](https://github.com/PeterVranken/comFramework/wiki/How-to-access-Attributes-in-the-Network-Database#typical-code-architecture "Typical code architecture")
-   [Sugar on top of inheritance or how to change the copyright notice](https://github.com/PeterVranken/comFramework/wiki/Sugar-on-Top-of-Inheritance-or-how-to-change-the-Copyright-Notice "Terence Parr: 'Sugar on top of inheritance'")

## Installation

The code generator is a Java application. The installation is as simple as
unpacking an archive and optionally setting an environment variable. It is
described in detail either
[online](https://petervranken.github.io/comFramework/codeGenerator/doc/installation.html)
or locally in your installation as file
`codeGenerator/doc/installation.html`.

## What's new

### Release 1.4

Update of data model: The support of string comparison operations has
been extended. `<info.str>` now supports regular expression based search
and replacement.

### Release 1.13.1

Integration of the latest releases of StringTemplate V4 and ANTLR, which
are 4.3.4 and 4.13.2, respectively.

### Release 1.13.0

This version increment was made to make a clean break when the project
moved from SourceForge to GitHub. In fact, there are no significant
changes to the DBC code generator since the last release, but there are a
number of smaller corrections, mostly to the documentation.

### Release 1.12.0

The data model has been extended with string comparison operations and a
related new sample has been added (stringCompare). Text fields of the data
model can be compared with one another or with expected contents or they
can be matched to a regular expression.

The template library for iterations has been extended. The filter
conditions, _which_ messages or _which_ signals, etc., to iterate are now
external to the new, additional iterations templates. The user can pass
a Boolean expression to instruct the iterations, which particular objects
to visit.

With these changes, it becomes much easier and straightforward to
implement specific iterations, e.g., all frames having a signal named
`.*CRC.*` for the generation of checksum validation code.

The template library has been extended. The new library file loops.stg
offers standard for- and while-loops.

The MathWorks Embedded Coder samples have been migrated to MATLAB 2021b.

The special file names `stdout` and `stderr` are now supported for
generated files. Generated output can directly go to the console.

### Release 1.11.6

Integration of the latest releases of StringTemplate V4 and ANTLR, which
are 4.3.4 and 4.12.0, respectively.

Support of transport protocols in that the message size is no longer
restricted to CAN frame sizes 0..8, 12, 16, 20, 24, 32, 48 and 64 Byte.
Other sizes used to be an error before. Now, the code generator just emits
an informative message about deviating message sizes and it sets the DLC,
which is only only the true frame sizes, to null.

The new semantics of field frame.DLC requires a change of the data model
version from formerly 1.10 to 1.11.

Fix: An empty DBC file without any message definition let to a (caught)
null pointer exception if a template made use of frame sorting.

Fix: The index i0 of bus objects inside the collection cluster of those
had been wrong calculated. This is an ages old bug; probably the template
expression `<bus.i0>` won't ever have worked correct in the past.

### Release 1.11.2

Integration of the latest releases of StringTemplate V4 and ANTLR, which
are 4.3.3 and 4.10.1, respectively.

### Release 1.11

Bug fix: Sorting frames by CAN ID had disregarded the property "is
extended ID". If two frames would have had the same IDs, one as 11 and the
other as 29 Bit ID, then the order of both is undefined in earlier
releases. Now a 29 Bit ID is considered greater than an 11 Bit ID of same
numeric value.

New sort orders for CAN IDs. The property "is extended ID" became a sort
criterion. Frames with extended ID can be put before or after those with
standard IDs. This is particularly useful if sorting by ID is used as
second subordinated criterion: Now we have two solid blocks of frames, one
for 11 Bit and one for 29 Bit IDs and each block is sorted by raising
numeric ID value. (Inverse orders are supported as well.)

Note, the data model revision has not been modified; it remains at 1010.

### Release 1.10.6

Bug fix: Bad lookup code had let the code generator mix up 11 and 29 Bit
CAN IDs if they would have the same numeric values. Parser aborted with
error message.

Bug fix: A specific syntax error in the DBC could make the code generator
abort with null pointer exception rather than with proper error message.
The problem appeared if an attribute value was specified for the wrong
type of object, e.g. if an attribute was set for a signal although it had
been defined before as message related attribute. Such a wrong DBC file
had been produced by the Kvaser Database Editor (Version 3.13.317, Sep 9,
2020) after editing some attribute values. Now the application simply
rejects the DBC file with precise hint what's wrong with the file.

### Release 1.10.4

The code generator has been migrated to the recently issued revisions of
StringTemplate and ANTLR, 4.3.1 and 4.8, respectively.

The only noticeable difference should be a fix of the white space handling
in StringTemplate. In all revisions so far, it used to be very difficult
to precisely control the generation of blank lines in the output. With
this fix, the handling of newline characters in templates became more
transparent. In particular, this relates to leading and trailing newlines
in sub-templates.

Caution, this means that this version of the code generator can produce
different output with existing templates. The differences are however
limited to blank lines and should not matter in C code generation
environments.

The support of CAN FD has been improved. The frame size of CAN messages is
checked against the allowed CAN FD frame sizes. The data model has been
extended by the data length code (DLC). The new data model has
revision 1010.

### Release 1.10

Error handling improved. Firstly, internal errors of StringTemplate V4
during rendering are now streamlined with the application logging. Before,
they were printed in the log (together with the bulky but in the given
context meaningless Java stack frame) but were not recognized or counted
by the application.

Secondly, error counting has been aligned with the other application
excelExporter. A summary note is printed at the end of the application
run, which reports the total number of errors and warnings in all parse
and output generation steps. Moreover, if the total number is not zero
then the application now ends with exit value one, which facilitates
proper integration of the code generator into an automated build
environment.

Note: There's no change of the data model. It remains at revision
designation 1009.

### Release 1.9

The data model class Signal gets a new field, which permits a template
to find out if the signal is a particular "special" signal, like checksum
or sequence counter.

### Release 1.8

New feature: A new command line argument, invert-transmission-direction,
supports code generation for a node, which implements residual bus
simulation. If the argument is given then the transmission direction of
frames and signals is inverted in the data model. A new field in the data
model, Bus.isTransmissionDirectionInverted, makes this behavior
transparent to the templates.

Inverse sort orders have been added for the bus's frame array and
documentation of sorting became much more accurate; it used to not mention
all supported sort orders.

Frame sort order sendFirst has been renamed to sentFirst, which means a not
backward compatible change of the data model. Existing templates may fail
to do expected frame sorting with this revision of the code generator!

The signal lists of the PDUs are now sorted; before the order of
appearance of signal specifications in the network database files had been
retained. This normalizations makes code generation more stable against
equivalent changes of the network database files. Normal signals and
signals in multiplexed sets of those are now sorted according to their
location in the PDU (ascending order of start bit). The multiplexed sets
of signals are sorted according to ascending order of selector value.

The named values of a signal are now sorted according to ascending numeric
values.

Bug fix: The named value lists of a signal had been wrong in that the
value names were put in quotes. Now the quotes are removed and the true
name of a signal value is put into the data model.

Bug fix: The Embedded Coder sample had generated the conversion macros
binary-value-to-world-value and vice versa only for signals, which are
either sent or received by the node. This was inconsistent with the
generation of signal initialization code, which had been generated for all
the signals of a frame. The initialization code may depend on the
conversion macros. Now, the templates generate the conversion macros for
all signals, even those, which are not transmitted by the node.

The interface descriptions for AUTOSAR, Embedded Coder, TargetLink and
ASAM 2-MC generated by the samples will now list all signals of a
transmitted frame. So far, only sent and received signals had been listed.
Although this is basically correct was it found to be hindering because
many real network databases don't specify senders and receivers properly
for all frames and signals.

Bug fix: A misplaced `<endif>` has been found in template cap_canApi.c.stg
of the Embedded Coder sample. The #define's that make min and max of a
signal public were only generated for scaled signals. This was not by
intention, min and max are as useful for unscaled integers. Now, the min
and max values are available as #define for all signals.

Bug fix: The same misplaced `<endif>` let to unwanted suppression of parts
of the Embedded Coder interface. For unscaled integers was the chosen
storage class mechanism not generated.

### Release 1.7

DBC Parser: User attributes and named signal values now support the full
signed and unsigned 32 Bit integer range from -2^31 till 2^32-1. Before,
the parser's implementation had used the Java type int, which limited the
positive range to 2^31-1.

Some more fields in the data model class Signal facilitate the handling of
multiplexed signals. Although a rather minor change demands the change of
the data model the increase of the second field of the revision number.

A new sample, EmbeddedCoderAPIWithBusStruct, demonstrates how to perfectly
integrate a comFramework based CAN interface with model based APSW design
and Embedded Coder based code generation from a Simulink model.

### Release 1.6

The DBC parser has been made more tolerant against typical mistakes:

-   Attribute names may use the actually forbidden hyphen. The parser
    replaces the hyphens with underscores, emits a warning only and
    continues
-   Missing attribute default values had been reported as an error. They
    are now reported as warning only if all affected objects got an
    individual value
-   Invalid CAN IDs are now reported only once, not at every occurrence

The environmental part of the data model, StringTemplate V4 attribute
info, has two new elements:

-   The StringTemplate V4 template engine has been connected to the Log4j
    application logger. A template can emit messages on debug, info, warn
    and error level. The latter two are counted and code generation won't
    take place if there are errors. This way, a template can report invalid
    states and hinder the template engine to regardless generate code
-   A numeric scratch pad is available. Numeric operations, including
    comparisons and binary operations, can be applied to numeric fields of
    the data model. This means a new level of conditional code generation
    and opens some new structural possibilities

A new sample, `scratchPad`, demonstrates the capabilities and the usage of
the new scratch pad. Please have a look at the [generated output](https://github.com/PeterVranken/comFramework/blob/main/codeGenerator/samples/scratchPad/output/demoScratchPad.txt) to get a
first impression.

### Release 1.5

The data model now offers sorted lists of frames. A template can iterate
along all frames in lexical order of their names or in numerical order of
ID or in parsing i.e. file order. In all cases the iteration can be done
in both directions.

### Release 1.4

A minor extension of the data model for sake of template design: The
description of a signal's bit pattern in the PDU is supported by
additional redundant but convenient fields. See [Javadoc](https://svn.code.sf.net/p/comframe/code/codeGenerator/trunk/doc/dataModel/index.html) of Java class
Signal.

Fix: The log file contained a few bad end of line characters, which let to
a mixture of UNIX and Windows style.

### Release 1.3

Our understanding of multiplexed signals turned out to be wrong. Release
1.2 and before only permitted alternative signals, where alternative sets
of signals is the true intention. The according change of the data model
required the increment of the minor version number.

#### Fix 1.3.4

Revision 1.3.3 had been released with a bug in one of its core sample
templates. This bug has been fixed in 1.3.4. The source code of the code
generator itself has not been modified but we changed the third field of
its revision designation because of the high impact of this issue: Most
people will use the sample templates as starting point for their
application and would suffer from the inherited bug in the template.

The bug was located and is fixed in the template [codeGenC.stg](https://github.com/PeterVranken/comFramework/blob/main/codeGenerator/samples/raceTechnology/templates/lib/codeGenC.stg "GitHub repository").

However, the complete set of templates should be exchanged for consistency
reasons. The introduction of the support template
[iterations.stg](https://github.com/PeterVranken/comFramework/blob/main/codeGenerator/samples/raceTechnology/templates/lib/iterations.stg "GitHub repository")
had established the attribute name `signal` for signals. Earlier templates
used to refer to `s` to address to the signal in scope. This has now been
aligned and all templates use the attribute name `signal`.


### Release 1.2

An unpleasant bug fix: The code itself was alright but there were two too
strict assertions in the code of release 1.1, which let the program abort
depending on the network database. One assertion would fire in case of a
specific error in the network file but the other one for each input file
not defining any frame related attribute - which is a quite common
situation. Release 1.1 would run if the Java machine is not started with
assertion support (-ea).

The release number has been incremented not because of the fixes but since
the data model has been polished. All objects now support the method
`toString` so that they can be rendered in a template by themselves, e.g.
`<frame>` will render a complete frame object. The default behavior is to
render an object as its name, `<frame>` would be identical to
`<frame.name>`.

Furthermore, the availability of get-number-of-element methods has been
harmonized; they should now be available for all the collections in the
data model. The StringTemplate V4 operator `<length()>` is probably no
longer required.


### Release 1.1

This release appeared already two weeks after the first release. By chance
we found a side note in  the StringTemplate V4 documentation mentioning
the support of Java Map objects. This led to a redesign of the handling of
CAN DBC attributes and the special signals. The data model has been
changed. The functionality is exactly the same but the usage (i.e. the
required template constructs) is much more intuitive. The sample template
file attributes.stg disappeared; it was a convenience file to compensate
for the somewhat cumbersome earlier approach.

Having Java Map available it became now possible to implement an earlier
idea, user specified options or, with other words, template attributes.
Arbitrary attributes of Boolean, numerical or string type can be defined
on the command line and are put into the rendered data model. The code
generation process can be controlled by user specified command line
options.

CAUTION: This revision will fail if run with assertion checking. See next
revision for details.

### Release 1.0

The initial release. Full functionality of code generation but preliminary
state of samples and documentation.

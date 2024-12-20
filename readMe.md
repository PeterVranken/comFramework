# comFramework 2.1

## About this project

This project presents a flexible, widely customizable CAN communication
interface for embedded applications. It binds signal based application
code to the frame based hardware layer. It covers the CAN stack from the
application layer down to the hardware driver layer (not including).

The interface implementation is code generator supported; the dedicated
API with the application software and the unpack/pack functions for
message (de)composition can be generated from information in the network
databases. A sample integration demonstrates, how to generate much more:
Setting initial signal values, DLC check, checksum generation/validation,
sequence counter generation/validation and the implementation of different
timing patterns is generated in a fully automated fashion. Attributes
defined in the DBC file(s) support the automation.

We call this project a framework since the interface should be considered
a suggestion only; the high flexibility of the code generation process
makes it easy to design different interface architectures, which can reach
a similar degree of automation with respect to changes of the network
database(s).

### The CAN interface

The operational core of the
[CAN interface](https://github.com/PeterVranken/comFramework/wiki/The-CAN-Interface)
is a dispatcher engine that decouples the one or more interrupts, which
are typically used by the CAN hardware driver for notification of send or
receive events, from the application task(s); this makes the complete data
processing at the application side safe, straightforward, race condition
free programming.

The dispatcher implements a generic concept of events and notifications by
callbacks. Among more, events can be interrupts from the hardware layer or
timer events. The callback functions use the timer and other events to
model the transmission timing patterns of the frames and to generate
timeout information.

The callbacks are external to the dispatcher engine. They are either
auto-coded from the DBC files or they can be hand-coded and would then
operate on data tables, which are auto-coded from the DBC files. Samples
demonstrate both techniques.

Any number of dispatcher objects can be instantiated in order to support
multi-threading architectures in a transparent and convenient way.

### The DBC code generator

The
[DBC code generator](https://petervranken.github.io/comFramework/codeGenerator/readMe.html)
consists of an open source parser for CAN network database files (*_.dbc_
or DBC files) with connected general purpose code generator. The idea is
most simple and most flexible:

The parser transforms the DBC files into an internal data representation,
which holds all information about the network. This data structure is a
special form of the parse tree. The structure has been chosen such that it
is compatible with the template engine StringTemplate V4 by Terence Parr,
see <http://www.stringtemplate.org/>. This template engine is capable to
render deeply nested data structures and it can therefore transform the
parse tree in nearly any kind of textual representation. This explains the
high flexibility/customizability of the whole system.

Just by configuring the templates, the code generator can produce
different useful representations of the information in the network files,
like:

- An HTML report with all frames, signals, attributes and all the
  properties of these.
- An Excel file with all the same (however, only as *_.csv_).
- C source code and related header files, which implement a CAN interface.
  The interface will contain the needed data structures, timing related
  frame processing, validation code and pack and unpack operations to
  transform signal sets to frames and vice versa. The implementation can
  be made specific to a particular platform's requirements.
- LaTeX source code for documentation of the interface.
- Interface definition files: If code from a model based code generation
  environment is linked to the CAN interface (e.g. MathWorks MATLAB
  with either their Embedded Coder or with dSPACE TargetLink) then a
  descripition of the signal interface is essential as these code
  generators need to be aware of the signal sets and their properties,
  data types and scaling in the first place. Our code generator can
  generate the required M scripts or XML files.
- ASAM MCD-2 MC interface description files (*_.a2l_) if the target
  platform shall be connected to a measurement and calibration tool like
  ETAS INCA or Vector Informatik CANape.
- AUTOSAR specification code (*_.arxml_). The DBC file contents can be
  rendered as an AUTOSAR software component, which connects to the
  ISignals of the COM stack, including all required application data types
  with scaling and more information.

### DBC parser

In most automation environments our code generator can be used as raw DBC
parser for whatever purpose, too. Typically, interpreted languages like
Perl, Python or Octave's M script are applied in software automation
environments. If you use any interpreted language then you can configure
the code generator to render the information in the syntax of your
scripting language. Run the code generator, run the generated script and
have the information in the context of your automation environment. The
configuration of the code generation can be tailored; you will just render
those parts of the information you really need. No need to develop the
most complex all embracing data structure. A simple [example for GNU Octave](https://github.com/PeterVranken/comFramework/wiki/Reusage-of-Code,-Standalone-Use-of-DBC-Parser-and-Compatibility/#example-the-code-generator-as-dbc-parser-for-gnu-octave-m)
is provided.

### excelExporter as auxiliary code generator

A second, auxiliary code generator is part of the framework. This is the
Java application excelExporter. The idea is nearly the same as for the
main code generator but the input is a set of Excel workbooks instead of
DBC files. The parse tree can be as simple as a linear list of rows from a
flat table or a as complex as a deeply nested tree of interrelated data
items. The concrete data structure depends on the definition of the input.
The parse tree is rendered by the StringTemplate V4 template engine,
identical to what has been said for the DBC code generator. The intented
use case of the auxiliary code generator excelExporter is to support the
handling of interfaces. Excel files serve as data dictionary of signals,
variables, diagnostic interface items, etc. and can be translated into
C/C++ interface implementations, documentation, ASAM MCD-2 MC or
AUTOSAR interface specifications, etc.

## Status of the project

The project is ready for productive use.

- The CAN interface with its dispatcher engine is distributed as source
  code; concept and how-to-use are documented in this [Wiki page](https://github.com/PeterVranken/comFramework/wiki/The-CAN-Interface).
- Several compilable and runnable sample integrations of the CAN interface
  are distributed with source code and makefiles together with the
  dispatcher engine. There are integrations for Windows, there's an
  Arduino ATmega 2560 real time integration and an integration with the
  MathWorks Embedded Coder. A complete runnable real-time application for
  MPC5748G with CAN driver and integrated CAN interface can be found at
  [GitHub](https://github.com/PeterVranken/DEVKIT-MPC5748G/tree/master/samples/CAN).
- The code generators are distributed as Java application. A number of
  samples for the [DBC code generator](https://github.com/PeterVranken/comFramework/tree/main/codeGenerator/samples) and for [excelExporter](https://github.com/PeterVranken/comFramework/tree/main/excelExporter/samples) demonstrate
  how templates can look like, which do the transformations mentioned above.
- [GitHub Releases](https://github.com/PeterVranken/comFramework/releases)
  provide ZIP archives for download, which bundle all needed tools, files
  and samples in a ready-to-use folder structure.
- More recent revisions of the software are distributed as source code in
  this repository and, as far as the code generation tools are concerned,
  as compiled Java \*_.jar_ files. Get for example the last recent DBC
  code generator by replacing the files _dist/_\* in your installation by
  the files from
  <https://github.com/PeterVranken/comFramework/tree/main/codeGenerator/dist/>.
  excelExporter files would be found
  [here](https://github.com/PeterVranken/comFramework/tree/main/excelExporter/dist),
  respectively.

Support of the project is appreciated to support more kinds of network
databases. For now, we are restricted to the DBC format. However, this
format looses importance. New formats like arxml or FIBEX will probably
supersede DBC in the future. Furthermore, different physical bus systems
have different network database files, like *_.ldf_ for LIN communication.
Parsers for these input formats are required and - what's more difficult -
a common data model for all of these buses and network files needs to be
developed so that the parser becomes a configurable choice but the
templates can be kept widely independent of the input format.

## Installation

comFramework is distributed as a ZIP archive. The installation means
extracting the archive, providing a Java JRE and setting a few environment
variables.

Please find the installation guide 
[online in GitHub](https://github.com/PeterVranken/comFramework/wiki/Installation/)
or as file [_comFramework/doc/installation.html_](doc/installation.html)
after extracting the archive.

## Documentation

### What's new

CAN Interface: An overview of all available documentation and of the
latest changes is given in the related
[`readMe`](https://petervranken.github.io/comFramework/canInterface/readMe.html)
file.

DBC Code Generator: An overview of the documentation and the latest
changes is given in the related
[`readMe`](https://petervranken.github.io/comFramework/codeGenerator/readMe.html)
file.

Excel Exporter: An overview of the documentation and the latest changes is
given in the related
[`readMe`](https://petervranken.github.io/comFramework/excelExporter/readMe.html)
file.

### Wiki pages

A growing source of documentation are the
[Wiki pages](https://github.com/PeterVranken/comFramework/wiki "comFramework - About this Project")
of the project. The Wiki pages shade a light at some most relevant,
selected issues; a comprehensive, self-contained (printable) manual is not
planned.

As of today, December 2024, we have the following discussions in the Wiki
pages:

- [Installation of distributed ZIP archive](https://github.com/PeterVranken/comFramework/wiki/Installation/)
- [The CAN interface - concept and usage](https://github.com/PeterVranken/comFramework/wiki/The-CAN-Interface)
- [The DBC code generator](https://github.com/PeterVranken/comFramework/wiki/The-DBC-Code-Generator)
- [The command line of the DBC code generator](https://github.com/PeterVranken/comFramework/wiki/The-Command-Line-of-the-comFramework-DBC-Code-Generator "Usage of DBC code generator")
- [Compatibility of the DBC parser with real *_.dbc_ files](https://github.com/PeterVranken/comFramework/wiki/Reusage-of-Code,-Standalone-Use-of-DBC-Parser-and-Compatibility#Compatibility)
- [Prerequisites, limitations and pitfalls](https://github.com/PeterVranken/comFramework/wiki/Prerequisites,-Limitations-and-Pitfalls "Java version, known issues")
- [Reusability of the DBC file parser in other contexts/applications](https://github.com/PeterVranken/comFramework/wiki/Reusage-of-Code,-Standalone-Use-of-DBC-Parser-and-Compatibility "Reusage of code, standalone use of DBC parser and compatibility")
- [Options for conditional code generation](https://github.com/PeterVranken/comFramework/wiki/Conditional-Code-Generation-vs-Generation-of-Conditional-Code "Conditional code generation versus generation of conditional code")
- [The use of attributes](https://github.com/PeterVranken/comFramework/wiki/How-to-access-Attributes-in-the-Network-Database "How to access attributes in the network database?")
- [A common pattern how to combine handwritten code with auto-generated code in a beneficial way](https://github.com/PeterVranken/comFramework/wiki/How-to-access-Attributes-in-the-Network-Database#typical-code-architecture "Typical code architecture")
- [Sugar on top of inheritance or how to change the copyright notice](https://github.com/PeterVranken/comFramework/wiki/Sugar-on-Top-of-Inheritance-or-how-to-change-the-Copyright-Notice "Terence Parr: 'Sugar on top of inheritance'")
- [Concept of excelExporter](https://github.com/PeterVranken/comFramework/wiki/excelExporter,-Rendering-Excel-Spreadsheets-as-Text "excelExporter's Wiki Home Page")
- [excelExporter's data model](https://github.com/PeterVranken/comFramework/wiki/The-excelExporter-Data-Model "Understanding excelExporter's data model")
- [excelExporter, grouping and sorting](https://github.com/PeterVranken/comFramework/wiki/excelExporter,-Grouping-and-Sorting "How to interrelate data objects, how to sort columns")
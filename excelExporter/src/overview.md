# excelExporter - Rendering Excel Spreadsheets as Text #

This Javadoc describes the data model of excelExporter, an
application that can translate the information found in one or more Excel
workbooks into manifold textual representations. excelExporter is a
general purpose text rendering application for Excel spreadsheets.

The principal aim of the tool is supporting automation tasks in software
development environments. In the context of the
[comFramework project](https://github.com/PeterVranken/comFramework/wiki/Home),
to which it belongs, this targets the handling of data dictionaries and
interfaces but excelExporter is by design independent from this intended
purpose and will be useful for various other automation tasks.

You can render the information in the Excel input for example as:

- HTML or LaTeX for reporting and documentation
- Various XML formats for interfacing with other applications
- C/C++ or any other textual programming languages

Rendering Excel files basically is a two step process. First you specify a
set of Excel input files. All of these are parsed and merged to one large
data structure, ofter referred to as _data model_. Input files of same or
of different format are supported. ("Format" refers to the structure of
the Excel files with respect to contained sheets and columns.)

Secondary, you define a list of the output files to be generated. The
specification of each output file is associated with the specification of a 
<a href="http://www.StringTemplate.org/" target="_blank">StringTemplate V4</a>
template file. The template controls the output generation; different
templates yield different output from the input. The template specifies
how to embed pieces of information taken from the data model into literal
text. In order to successfully design your templates you need to have a
detailed understanding of the structure of the data model.

The data model is a Java object with fields and methods. Many fields are
nested collections and data sub-structures. The "pieces of information",
which can be placed into the output, are the values of the public fields
and the return values of the public methods and all of these are well
described by this Javadoc.

This Javadoc describes the data model in terms of field and method
descriptions. The concept of the data model, an overview with UML diagrams
and an explanation with examples how to access it from StringTemplate V4
templates can be found online in the Wiki page
<a href="https://github.com/PeterVranken/comFramework/wiki/The-excelExporter-Data-Model" target="_blank">excelExporter's Data Model</a>
of the excelExporter project.

An overview on the documentation available for the application
excelExporter can be found online at
<a href="https://github.com/PeterVranken/comFramework/wiki/excelExporter,-Rendering-Excel-Spreadsheets-as-Text" target="_blank">GitHub</a>.

# Random Generation of CAN Network Databases #

This folder contains an Octave script, which can be used to generate a
valid CAN network database file for testing purpose. Most of the
parameters are purely random chosen and each run of the script will
therefore generate another database.

The generated databases contain messages with individual properties for
size, send patterns and timing. The timing properties are modeled by those
attributes, which are typically used by the comFramework samples.

The frames contain signals of randomly chosen size, scaling and endianess.

## Running the script ##

The script has been run with GNU Octave 4.0.3 for Windows. The script code
founds on a Java library; this requires proper setting of the Java class
path, when starting Octave. See `javaclasspath.txt` for more.

In Octave, `cd` to the directory of script `generateRandomDbc.m`. Now 
type 

    help generateRandomDbc
    
A valid command line to generate the DBC file myDatabase.dbc would be

    generateRandomDbc myDatabase.dbc myEcu

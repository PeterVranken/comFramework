# Prepare the environment for the run of one of the other build scripts.
#   See the TODO tags first. Some paths need to be initially set.

# TODO This is the only essential setting you need to make. Configure the path to the Java
# JDK to use. A Java JDK is not element of this Git repository. Our pre-compiled jar files
# have been made with OpenJDK jdk-23.0.1, see https://jdk.java.net/23/ (visited Nov 19th,
# 2024) to find a suitable download without license restrictions.
$env:JAVA_HOME = "C:\ProgramFiles\Java\jdk-23.0.1"

# TODO Configure the path to the Doxygen installation. This setting is not needed to build
# or run the comFramework tools but it is important if you want to make the API
# documentation of the CAN interface (e.g, after a source code modification). Making the
# API documentation has been tested with doxygen 1.9. The doxygen executable is expected in
# "$env:DOXYGEN_HOME\bin".
$env:DOXYGEN_HOME = "C:\ProgramFiles\doxygen-1.9.3"

# TODO Configure paths to MinGW and to a set of UNIX style command line tools (echo, cp,
# mv, etc. These settings are not needed to build or run the comFramework tools but they
# are important if you want to reproduce the builds of the sample integrations of
# comFramework's CAN interface. (And for compiling the C code, which is generated when
# running a few of the code generator and excelExporter samples.)
#   Caution: An installation path containing blanks is not supported as a makefile is
# involved in the builds.
$env:MINGW_HOME = "c:\ProgramFiles\mingw-w64-x86_64-8.1.0-posix-seh-rt_v6-rev0\mingw64"
$env:UNIX_TOOLS_BIN = "c:\Programme\Git\usr\bin"

# TODO Configure the path to the Arduino installation. This setting is not needed to build
# or run the comFramework tools but it is important if you want to reproduce the builds of
# the sample integrations of comFramework's CAN interface with the Arduino platform.
# avr-gcc.exe is expected to be found at "$env:ARDUINO_HOME\hardware\tools\avr\bin". The
# build has been tested with Arduino 1.8.19, including avr-gcc 7.3.0.
#   Caution: An installation path containing blanks is not supported as a makefile is
# involved in the builds.
$env:ARDUINO_HOME = "C:\ProgramFiles\arduino-1.8.19"

# The following paths relate to the repository itself and don't need particular care.
$env:ANT_HOME = "$PSScriptRoot\apache-ant-1.10.15"
$env:GITWCREV_HOME = "$PSScriptRoot\GitWCRev"

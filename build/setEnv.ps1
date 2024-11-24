# Prepare the environment for the run of one of the other build scripts.
#   See the TODO tags first. Some paths need to be initially set.

$env:ANT_HOME = "$PSScriptRoot\apache-ant-1.10.15"
$env:GITWCREV_HOME = "$PSScriptRoot\GitWCRev"

# TODO Configure the path to the Java JDK to use. A Java JDK is not element of this Git
# repository. Our pre-compiled jar files have been made with OpenJDK jdk-23.0.1, see
# https://jdk.java.net/23/ (visited Nov 19th, 2024) to find a suitable download without
# license restrictions.
$env:JAVA_HOME = "C:\ProgramFiles\Java\jdk-23.0.1"

# TODO Configure paths to MinGW and to a set of UNIX style command line tools (echo, cp,
# mv, etc. These settings are not needed to build or run the comFramework tools but they
# are important if you want to reproduce the builds of the sample integrations of
# comFramework's CAN interface. (And for compiling the C code, which is generated when
# running a few of the code generator and excelExporter samples.)
$env:MINGW_HOME = "c:\ProgramFiles\mingw-w64-x86_64-8.1.0-posix-seh-rt_v6-rev0\mingw64"
$env:UNIX_TOOLS_BIN = "c:\ProgramFiles\Git\usr\bin"

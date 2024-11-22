# Build Java application codeGenerator
#   See the TODO tags first. Some paths need to be set first.

$env:ANT_HOME = "$PSScriptRoot\apache-ant-1.10.15"
$env:GITWCREV_HOME = "$PSScriptRoot\GitWCRev"

# TODO Configure the path to the Java JDK to use. A Java JDK is not element of this Git
# repository. Our pre-compiled jar files have been made with OpenJDK jdk-23.0.1, see
# https://jdk.java.net/23/ (visited Nov 19th, 2024) to find a suitable download without
# license restritions.
$env:JAVA_HOME = "C:\ProgramFiles\Java\jdk-23.0.1"

$path = $env:PATH
$env:PATH = "$env:JAVA_HOME\bin;$env:ANT_HOME\bin;$env:GITWCREV_HOME"
pushd $PSScriptRoot\..\codeGenerator\src
ant -e jar $args
$env:PATH = $path
popd
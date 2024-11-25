# Build Java application codeGenerator.
#   See the TODO tags in sub-ordinated file setEnv.ps1 first. Some paths need to be
# initially set.

# Prepare the environment for the run of this script.
."$PSScriptRoot\setEnv.ps1"

$path = $env:PATH
$env:PATH = "$env:JAVA_HOME\bin;$env:ANT_HOME\bin;$env:GITWCREV_HOME" `
            + ";$env:MINGW_HOME\bin;$env:UNIX_TOOLS_BIN" `
            + ";$env:DOXYGEN_HOME\bin;$env:ARDUINO_HOME\hardware\tools\avr\bin"
#write-host "PATH: $env:PATH"
ant -f canInterface.xml -e $args
$env:PATH = $path

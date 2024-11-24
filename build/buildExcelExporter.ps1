# Build Java application excelExporter.
#   See the TODO tags in sub-ordinated file setEnv.ps1 first. Some paths need to be
# initially set.

# Prepare the environment for the run of this script.
."$PSScriptRoot\setEnv.ps1"

$path = $env:PATH
$env:PATH = "$env:JAVA_HOME\bin;$env:ANT_HOME\bin;$env:GITWCREV_HOME"
#write-host "PATH: $env:PATH"
pushd $PSScriptRoot\..\excelExporter\src
ant -e $args
$env:PATH = $path
popd
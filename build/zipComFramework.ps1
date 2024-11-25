# Bundle the file for a binary distribution of project comFramework
#   See the TODO tags in sub-ordinated file setEnv.ps1 first. Some paths need to be
# initially set.

# Prepare the environment for the run of this scripr.
."$PSScriptRoot\setEnv.ps1"

$path = $env:PATH
$env:PATH = "$env:JAVA_HOME\bin;$env:ANT_HOME\bin;$env:GITWCREV_HOME"
ant -e -f zipComFramework.xml $args
$env:PATH = $path

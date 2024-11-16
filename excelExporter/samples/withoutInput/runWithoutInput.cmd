@echo off
goto LStart
:LUsage
echo usage: runWithoutInput -h OR tabLen [--help]
echo Run excelExporter sample withoutInput. No Excel file is read as input. The applied
echo StringTemplate V4 template is self-contained with the exception of little input data,
echo which is passed in as user options.
echo   tabLen is the size of the generated multiplication table. The value is passed as
echo user option into the template expansion.
echo   CAUTION: The simple command line evaluation of this script requires that tabLen
echo appears as first command line argument.
echo   All command line arguments but -h are passed to the Java application excelExporter.
echo The first argument is unconditionally used as table length; passing other information
echo as first argument evokes misleading error messages! Use --help to get the usage
echo message of the Java application excelExporter.
echo   Normally this script needs to be started from the original location in the
echo distribution of excelExporter. However, you may set the environment variable
echo EXCELEXPORTER_HOME to make it independent of its location. Refer to the documentation
echo of excelExporter to get more details.
goto :eof

REM
REM Copyright (c) 2016-2024, Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
REM

:LStart
if /i "%1" == "-h" goto LUsage
if "%1" == "" goto LUsage

setlocal

if "%EXCELEXPORTER_HOME%" == "" (
    set EXCELEXPORTER_HOME=..\..
)

REM The Java classpath is used to localize the StringTemplate V4 template file. Here, it's
REM sufficient to assign those paths, which hold the templates. The launcher script
REM excelExporter.cmd takes care that the actual Java search path is set, too.
set CLASSPATH=templates

REM Find the launcher script.
set PATH=%PATH%;%EXCELEXPORTER_HOME%\dist

excelExporter ^
  --log-file withoutInput.log ^
  --log-level warn ^
  --cluster-name "MulTab" ^
  -op tabLen -ov %1 ^
  --output-file-name output/mulTab.txt ^
    --template-file-name mulTab.stg ^
  --output-file-name stdout ^
    --template-file-name mulTab.stg ^
  %2 %3 %4 %5 %6 %7 %8 %9

@echo off
goto LStart
:LUsage
echo usage: runExcelExporter
echo   Normally this script needs to be started from the original location in the
echo distribution of excelExporter. However, you may set the environment variable
echo EXCELEXPORTER_HOME to make it independent of its location. Refer to the documentation
echo of excelExporter to get more details.
goto :eof

:: 
:: Copyright (c) 2016, Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
:: 

:LStart
if /i "%1" == "-h" goto LUsage

setlocal

if "%EXCELEXPORTER_HOME%" == "" (
    set EXCELEXPORTER_HOME=..\..
)

:: The Java classpath is used to localize the StringTemplate V4 template file. Here, it's
:: sufficient to assigne those paths, wich hold the template. The launcher script
:: excelExporter.cmd takes care that the actual Java search path is set, too.
set CLASSPATH=templates

:: Find the launcher script.
set PATH=%PATH%;%EXCELEXPORTER_HOME%\dist

call excelExporter.cmd ^
  -c "Test Sorting" ^
  -i "data.xlsx" -b testData ^
    -ss -s groupedTowns -tab "Grouped Towns" -ast groupedTownsTempl ^
  -st -stn groupedTownsTempl -rt 1 -id ^
    -ca -ct Path_1 -ig -soc lexical ^
    -ca -ct A -soc lexical -spc 2 ^
    -ca -ct B -soc numerical -spc 1 ^
    -ca -ct C -soc lexical -spc 3 ^
    -ca -ct Path_2 -ig -soc lexical ^
  --output-file-name HTML/treeView.html ^
    --template-file-name treeView.stg ^
    --template-name treeView ^
  %*

::    -ca -ct A -soc lexical -spc 2 ^
::    -ca -ct B -soc inverseNumerical -spc 1 ^
::    -ca -ct C -soc lexical -spc 3 ^

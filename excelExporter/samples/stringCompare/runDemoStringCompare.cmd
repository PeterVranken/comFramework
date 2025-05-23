@echo off
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

REM Lauch the Java application excelExporter.
excelExporter ^
  --log-file stringCompare.log ^
  --log-level warn ^
  --cluster-name "StrCmp" ^
  --output-file-name stdout ^
    --template-file-name demoStringCompare.stg ^
    --template-name demoStringCompare ^
  --output-file-name output/demoStringCompare.txt ^
    --template-file-name demoStringCompare.stg ^
    --template-name demoStringCompare ^
  %*


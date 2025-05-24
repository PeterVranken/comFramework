@echo off
setlocal
set CLASSPATH=templates;..\raceTechnology\templates
set COMFRAMEWORK_CODEGENERATOR_HOME=..\..
set PATH=%PATH%;%COMFRAMEWORK_CODEGENERATOR_HOME%\dist
call codeGenerator ^
  --log-file stringCompare.log ^
  --cluster-name demoStringCompare ^
  --node-name ECU ^
  --bus-name RT_simplified ^
    -dbc ../dbcFiles/CAN_RT_simplified.dbc ^
  --output-file-name stdout ^
    --template-file-name demoStringCompare.stg ^
    --template-name demoStringCompare ^
  --output-file-name output/demoStringCompare.txt ^
    --template-file-name demoStringCompare.stg ^
    --template-name demoStringCompare ^
  %*

@echo off
setlocal
set CLASSPATH=
set EXCELEXPORTER_HOME=..
set PATH=%PATH%;%EXCELEXPORTER_HOME%\dist
excelExporter.cmd ^
  --log-file excelExporter.log ^
  --cluster-name test ^
  --sort-order-of-workbooks lexical ^
  --sort-order-of-worksheets numerical ^
  --input-file-name test.xlsx ^
    --workbook-name TEST ^
    --open-worksheet-selection ^
      --applied-worksheet-template TMPL ^
      --worksheet-by-tab .*DBC.* ^
    --open-worksheet-selection ^
      --worksheet-by-index 2 ^
      --worksheet-name MyWS ^
  --open-worksheet-template ^
    --worksheet-template-name TMPL ^
    --association-by-tab .*DBC.* ^
    --column-titles-are-identifiers ^
    --group groupCAN ^
    --include-range-of-rows 7 ^
    --include-range-of-rows 9:9 ^
    --include-range-of-rows 6 ^
    --include-range-of-rows 11:13 ^
    --include-range-of-columns 1 ^
    --include-range-of-columns 1:1 ^
    --include-range-of-columns 1:3 ^
    --include-range-of-columns 5:8 ^
    --include-range-of-columns 10 ^
    --include-range-of-columns 4:7 ^
    --open-column-attributes ^
      --is-grouping-column ^
      --column-title ^.*Message*$ ^
    --open-column-attributes ^
      --column-name SendType ^
      --column-index 5 ^
    --open-column-attributes ^
      --column-name SendTime ^
      --column-title "Send_Time" ^
    --open-column-attributes ^
      --column-name BaudRate ^
      --column-title "(?i).*baud.*" ^
  --output-file-name test.output.txt ^
    --template-file-name test.stg ^
    --template-name test ^
  %*

@echo off
setlocal
set PATH=c:\ProgramFiles\mingw-w64-x86_64-8.1.0-posix-seh-rt_v6-rev0\mingw64\bin;%PATH%;
REM set PATH=C:\ProgramFiles\mingw-i686-8.1.0-release-win32-dwarf-rt_v6-rev0\mingw32\bin;%PATH%;

REM c:/ProgramFiles/mingw-w64-x86_64-8.1.0-posix-seh-rt_v6-rev0/mingw64/bin/gcc.exe -Wl,--print-map,--cref,--warn-common -o bin/win/DEBUG/canInterfaceMTTest.exe @bin/win/DEBUG/obj/listOfObjFiles.txt -lwinmm -lm -latomic > bin/win/DEBUG/canInterfaceMTTest.map

gcc -std=c11                                        ^
    -g -Ofast -DDEBUG                               ^
    -o processConsumer.exe                          ^
    -I../threadSafeQueue/src                        ^
    -Wl,--print-map,--cref,--warn-common            ^
    -lwinmm -lm -latomic                            ^
    mcc_processConsumer.c                           ^
    crc_checksum.c                                  ^
    ../threadSafeQueue/src/tsq_threadSafeQueue.c    ^
    > processConsumer.map

if ERRORLEVEL 1 (
    echo Compilation error consumer
    goto :eof
) else (
    echo Compilation consumer okay
)

gcc -std=c17                                        ^
    -g -Ofast -DDEBUG                               ^
    -o processProducer.exe                          ^
    -I../threadSafeQueue/src                        ^
    -Wl,--print-map,--cref,--warn-common            ^
    -Wl,-lwinmm,-lm,-latomic                        ^
    mcp_processProducer.c                           ^
    crc_checksum.c                                  ^
    ../threadSafeQueue/src/tsq_threadSafeQueue.c    ^
    > processProducer.map
if ERRORLEVEL 1 (
    echo Compilation error producer
    goto :eof
) else (
    echo Compilation producer okay
)

REM goto :eof

start /AFFINITY 1 .\processProducer.exe
if ERRORLEVEL 1 (
    echo Run-time error producer. Consumer is not started
    goto :eof
)
start /B/Wait /AFFINITY 2 .\processConsumer.exe
if ERRORLEVEL 1 (
    echo Run-time error consumer. Please stop producer process
    goto :eof
)

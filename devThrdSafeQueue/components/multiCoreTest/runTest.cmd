@echo off
setlocal
REM set PATH=c:\ProgramFiles\mingw-w64-x86_64-8.1.0-posix-seh-rt_v6-rev0\mingw64\bin;%PATH%;
set PATH=C:\ProgramFiles\mingw-i686-8.1.0-release-win32-dwarf-rt_v6-rev0\mingw32\bin;%PATH%;

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

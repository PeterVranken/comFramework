REM Collect some power consumption related information, which includes statements about the
REM current Windows scheduler timing.
powercfg -energy duration 5
if ERRORLEVEL 1 (
    goto :eof
)
energy-report.html
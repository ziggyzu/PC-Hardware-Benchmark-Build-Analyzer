@echo off
set "JAVA_HOME=C:\Users\Zubaier Hossain\.jdks\openjdk-26.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ===================================================
echo Starting PC Hardware Benchmark & Build Analyzer...
echo ===================================================
call ".\mvnw.cmd" javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with an error. Press any key to close.
    pause >nul
)

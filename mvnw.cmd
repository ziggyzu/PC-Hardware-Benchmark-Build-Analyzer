@echo off
setlocal enabledelayedexpansion
set "ARGS="
for %%A in (%*) do (
    if "%%A"=="javafx:run" (
        set "ARGS=!ARGS! org.openjfx:javafx-maven-plugin:0.0.8:run"
    ) else (
        set "ARGS=!ARGS! %%A"
    )
)

if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
    call "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" %ARGS%
) else (
    call mvn %ARGS%
)

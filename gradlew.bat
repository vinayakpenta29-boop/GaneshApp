@echo off
setlocal

REM Determine the location of the gradle-wrapper.jar
set WRAPPER_JAR=%~dp0gradle\wrapper\gradle-wrapper.jar

REM Check if gradle-wrapper.jar exists
if not exist "%WRAPPER_JAR%" (
    echo Gradle wrapper JAR not found at %WRAPPER_JAR%
    exit /b 1
)

REM Execute the Gradle wrapper
java -jar "%WRAPPER_JAR%" %*

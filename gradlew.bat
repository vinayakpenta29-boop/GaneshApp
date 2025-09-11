@echo off
rem -----------------------------------------------------------------------------
rem Gradle startup script for Windows
rem -----------------------------------------------------------------------------

set DEFAULT_JVM_OPTS=

set APP_NAME=Gradle
set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0

setCLASSPATH=%DIRNAME%gradle-launcher.jar

if "%JAVA_OPTS%" == "" goto noJavaOpts
setJAVA_OPTS=%JAVA_OPTS%

:noJavaOpts

java %JAVA_OPTS% -classpath "%CLASSPATH%" org.gradle.launcher.GradleMain %*

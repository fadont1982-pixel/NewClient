@echo off
setlocal

set "APP_HOME=%~dp0"
set "GRADLE_LIB=%APP_HOME%gradle-tmp\gradle-9.2.1\lib"

if not exist "%GRADLE_LIB%" (
    echo Gradle distribution not found in %GRADLE_LIB%
    exit /b 1
)

java -cp "%GRADLE_LIB%\*" org.gradle.launcher.GradleMain %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%

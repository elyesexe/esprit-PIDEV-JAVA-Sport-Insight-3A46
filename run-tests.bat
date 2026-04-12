@echo off
setlocal

set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"

set "JAVA_HOME=C:\Users\gueni\.jdks\openjdk-26"
if exist "%JAVA_HOME%\bin\java.exe" (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
    echo Running tests with Maven from PATH...
    mvn test
    goto :end
)

set "INTELLIJ_MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"
if exist "%INTELLIJ_MVN%" (
    echo Running tests with IntelliJ Maven...
    call "%INTELLIJ_MVN%" test
    goto :end
)

echo Maven not found.
echo Install Maven or run from IntelliJ.
exit /b 1

:end
echo.
echo Test reports: target\surefire-reports
endlocal

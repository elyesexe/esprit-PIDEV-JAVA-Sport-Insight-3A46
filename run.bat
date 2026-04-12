@echo off
REM Script to run the Sport Insight Application

echo Compiling and running Sport Insight...
echo.

REM Clean and compile
echo Step 1: Cleaning and compiling...
call mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

REM Run the application using javafx-maven-plugin
echo.
echo Step 2: Running the application...
call mvn javafx:run

pause


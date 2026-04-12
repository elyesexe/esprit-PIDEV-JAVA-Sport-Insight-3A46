@echo off
REM Script de vérification du projet Sport Insight (Windows)

echo =========================================
echo   Sport Insight - Vérification du projet
echo =========================================
echo.

REM Vérifier Java
echo 1. Vérification de Java...
java -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr "version"') do set JAVA_VERSION=%%i
    echo    + Java trouvé: %JAVA_VERSION%
) else (
    echo    - Java non trouvé. Veuillez installer Java JDK 17+
    exit /b 1
)

REM Vérifier Maven
echo.
echo 2. Vérification de Maven...
mvn -v >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    + Maven trouvé
) else (
    echo    - Maven non trouvé. Veuillez installer Maven 3.8+
    exit /b 1
)

REM Vérifier la structure
echo.
echo 3. Vérification de la structure...
if exist "pom.xml" (
    echo    + pom.xml trouvé
) else (
    echo    - pom.xml non trouvé
    exit /b 1
)

if exist "src\main\java\module-info.java" (
    echo    + module-info.java trouvé
) else (
    echo    - module-info.java non trouvé
    exit /b 1
)

if exist "src\main\resources\views\login.fxml" (
    echo    + login.fxml trouvé
) else (
    echo    - login.fxml non trouvé
    exit /b 1
)

if exist "src\main\java\tn\esprit\mains\LoginApp.java" (
    echo    + LoginApp.java trouvé
) else (
    echo    - LoginApp.java non trouvé
    exit /b 1
)

REM Compiler
echo.
echo 4. Compilation du projet...
mvn clean compile

if %ERRORLEVEL% EQU 0 (
    echo    + Compilation réussie
) else (
    echo    - Erreur de compilation
    exit /b 1
)

echo.
echo =========================================
echo  + Toutes les vérifications sont passées!
echo =========================================
echo.
echo Pour lancer l'application:
echo   mvn javafx:run
echo.
pause


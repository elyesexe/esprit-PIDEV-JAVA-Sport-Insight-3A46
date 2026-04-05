@echo off
REM Script de compilation pour le projet Sport Insight
REM Utilise javac directement sans Maven

setlocal enabledelayedexpansion

REM Chemins des dépendances Maven
set MYSQL_JAR=C:\Users\hamou\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar
set PROTOBUF_JAR=C:\Users\hamou\.m2\repository\com\google\protobuf\protobuf-java\3.25.1\protobuf-java-3.25.1.jar

REM Chemin du répertoire de sortie
set OUTPUT_DIR=target\classes

REM Chemins sources
set ENTITIES_SRC=src\main\java\tn\esprit\entities
set TOOLS_SRC=src\main\java\tn\esprit\tools
set SERVICES_SRC=src\main\java\tn\esprit\services
set MAINS_SRC=src\main\java\tn\esprit\mains

echo.
echo ========================================
echo   Compilation du projet Sport Insight
echo ========================================
echo.

REM Compilation
echo [1/1] Compilation des fichiers Java...
javac -d %OUTPUT_DIR% -cp "%MYSQL_JAR%;%PROTOBUF_JAR%" ^
  %ENTITIES_SRC%\*.java ^
  %TOOLS_SRC%\*.java ^
  %SERVICES_SRC%\*.java ^
  %MAINS_SRC%\*.java

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo Compilation reussie!
    echo ========================================
    echo.
    echo Pour executer l'application :
    echo java -cp "%OUTPUT_DIR%;%MYSQL_JAR%;%PROTOBUF_JAR%" tn.esprit.mains.Main
    echo.
) else (
    echo.
    echo ========================================
    echo ERREUR : La compilation a echoue!
    echo ========================================
    echo.
)

endlocal
pause


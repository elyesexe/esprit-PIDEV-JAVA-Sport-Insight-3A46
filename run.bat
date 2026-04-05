@echo off
REM Script d'execution pour le projet Sport Insight

setlocal enabledelayedexpansion

REM Chemins des dependances Maven
set MYSQL_JAR=C:\Users\hamou\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar
set PROTOBUF_JAR=C:\Users\hamou\.m2\repository\com\google\protobuf\protobuf-java\3.25.1\protobuf-java-3.25.1.jar

REM Chemin du repertoire de classes compilees
set OUTPUT_DIR=target\classes

echo.
echo ========================================
echo   Execution - Sport Insight CRUD
echo ========================================
echo.

java -cp "%OUTPUT_DIR%;%MYSQL_JAR%;%PROTOBUF_JAR%" tn.esprit.mains.Main

endlocal
pause


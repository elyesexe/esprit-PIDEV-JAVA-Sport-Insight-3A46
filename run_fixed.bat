@echo off
REM Correction et relancement - Sport Insight

echo.
echo =========================================
echo   CORRECTION APPLIQUÉE - Spacer Fixed
echo =========================================
echo.
echo Fichiers corrigés:
echo ✅ annonce_view.fxml
echo ✅ commentaire_view.fxml
echo.
echo Erreur résolue: Spacer → Region
echo.

REM Vérifier Maven
echo Vérification de Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ❌ Maven n'est pas installé ou pas dans PATH
    echo.
    echo Solutions:
    echo 1. Installer Maven depuis https://maven.apache.org
    echo 2. Ou utiliser un IDE (IntelliJ ou Eclipse)
    echo 3. Ou utiliser Docker
    echo.
    pause
    exit /b 1
)

echo ✅ Maven détecté

echo.
echo =========================================
echo   ÉTAPE 1: Nettoyage
echo =========================================
echo.
cd /d "%~dp0"
mvn clean

if errorlevel 1 (
    echo.
    echo ❌ Erreur lors du nettoyage
    pause
    exit /b 1
)

echo.
echo =========================================
echo   ÉTAPE 2: Compilation
echo =========================================
echo.
mvn compile

if errorlevel 1 (
    echo.
    echo ❌ Erreur lors de la compilation
    pause
    exit /b 1
)

echo.
echo ✅ Compilation réussie!
echo.
echo =========================================
echo   ÉTAPE 3: Lancement de l'application
echo =========================================
echo.

mvn javafx:run

pause


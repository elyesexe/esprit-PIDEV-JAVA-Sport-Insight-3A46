@echo off
REM Script de démarrage pour Sport Insight JavaFX Application

echo.
echo =========================================
echo   🏆 Sport Insight - Application
echo =========================================
echo.

REM Vérifie si Java est installé
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERREUR: Java n'est pas installé ou non configuré dans PATH
    echo Veuillez installer Java JDK 17 ou supérieur
    pause
    exit /b 1
)

echo ✅ Java détecté

REM Affiche un message d'information
echo.
echo Instructions d'utilisation:
echo ==========================
echo.
echo 1. Assurez-vous que MySQL est en cours d'exécution
echo 2. Vérifiez les paramètres de connexion dans MyConnection.java
echo 3. Les tables doivent être créées dans la base de données
echo.
echo Configuration requise:
echo - Base de données: sport_insight
echo - Utilisateur: root
echo - Mot de passe: (configurez dans MyConnection.java)
echo.
echo ❓ Appuyez sur une touche pour continuer...
pause

echo.
echo 🚀 Démarrage de l'application...
echo.

REM Lance l'application en utilisant les fichiers compilés
cd /d "%~dp0"

REM Crée le répertoire de build s'il n'existe pas
if not exist "target\classes" (
    echo ❌ Les fichiers compilés n'existent pas
    echo Veuillez compiler le projet d'abord:
    echo   - Avec Maven: mvn clean install
    echo   - Ou importer dans un IDE (Eclipse, IntelliJ)
    pause
    exit /b 1
)

REM Lance l'application
java -cp "target\classes;target\lib\*" tn.esprit.javafx.SportInsightApplication

if errorlevel 1 (
    echo.
    echo ❌ Erreur lors du démarrage de l'application
    pause
    exit /b 1
)

pause


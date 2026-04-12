#!/bin/bash
# Script de vérification du projet Sport Insight

echo "========================================="
echo "  Sport Insight - Vérification du projet"
echo "========================================="
echo ""

# Vérifier Java
echo "1. Vérification de Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[^"]+')
    echo "   ✓ Java trouvé: $JAVA_VERSION"
else
    echo "   ✗ Java non trouvé. Veuillez installer Java JDK 17+"
    exit 1
fi

# Vérifier Maven
echo ""
echo "2. Vérification de Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -v | head -n 1)
    echo "   ✓ Maven trouvé: $MVN_VERSION"
else
    echo "   ✗ Maven non trouvé. Veuillez installer Maven 3.8+"
    exit 1
fi

# Vérifier la structure du projet
echo ""
echo "3. Vérification de la structure..."
if [ -f "pom.xml" ]; then
    echo "   ✓ pom.xml trouvé"
else
    echo "   ✗ pom.xml non trouvé"
    exit 1
fi

if [ -f "src/main/java/module-info.java" ]; then
    echo "   ✓ module-info.java trouvé"
else
    echo "   ✗ module-info.java non trouvé"
    exit 1
fi

if [ -f "src/main/resources/views/login.fxml" ]; then
    echo "   ✓ login.fxml trouvé"
else
    echo "   ✗ login.fxml non trouvé"
    exit 1
fi

if [ -f "src/main/java/tn/esprit/mains/LoginApp.java" ]; then
    echo "   ✓ LoginApp.java trouvé"
else
    echo "   ✗ LoginApp.java non trouvé"
    exit 1
fi

# Essayer de compiler
echo ""
echo "4. Compilation du projet..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "   ✓ Compilation réussie"
else
    echo "   ✗ Erreur de compilation"
    exit 1
fi

echo ""
echo "========================================="
echo "✓ Toutes les vérifications sont passées!"
echo "========================================="
echo ""
echo "Pour lancer l'application:"
echo "  mvn javafx:run"
echo ""


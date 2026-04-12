# Sport Insight - Guide de mise en place

## 🎯 Vue d'ensemble

Ce projet est une application JavaFX pour gérer les sports d'équipe (matchs, joueurs, etc.) avec une base de données MySQL.

## 📋 Prérequis

### 1. Java JDK 17 ou supérieur
- **Vérifier l'installation:**
  ```bash
  java -version
  ```
- **Si non installé:** Téléchargez depuis https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- **Configuration Windows:**
  - Ajouter le dossier `bin` du JDK au PATH système
  - Exemple: `C:\Program Files\Java\jdk-17\bin`

### 2. Maven 3.8+
- **Vérifier l'installation:**
  ```bash
  mvn -v
  ```
- **Si non installé:**
  1. Téléchargez Maven depuis https://maven.apache.org/download.cgi
  2. Extrayez le fichier ZIP dans un dossier (ex: `C:\Program Files\Apache\maven`)
  3. Ajouter le dossier `bin` au PATH système
  4. Redémarrez votre terminal

### 3. MySQL
- Vous pouvez utiliser XAMPP (qui inclut MySQL)
- Assurez-vous que le service MySQL est en cours d'exécution

## 🚀 Installation du projet

### Étape 1: Cloner ou extraire le projet
```bash
cd C:\xampp\htdocs\esprit-PIDEV-JAVA-Sport-Insight-3A46
```

### Étape 2: Vérifier Maven
```bash
mvn -v
```
Vous devez voir la version de Maven (ex: `Maven 3.9.x`)

### Étape 3: Télécharger les dépendances
```bash
mvn clean compile
```
Cela téléchargera tous les modules JavaFX et autres dépendances dans le dossier `.m2/repository`.

## 🎮 Exécution de l'application

### Option 1: Avec Maven (Recommandé)
```bash
mvn javafx:run
```

### Option 2: Avec le script batch (Windows)
```bash
run.bat
```

### Option 3: Depuis IntelliJ IDEA
1. Ouvrir le projet dans IntelliJ IDEA
2. Aller à **Run > Edit Configurations...**
3. Créer une nouvelle configuration pour `LoginApp`:
   - Main class: `tn.esprit.mains.LoginApp`
   - VM options: `--add-modules javafx.controls,javafx.fxml`
4. Cliquer sur **Run**

## 📁 Structure du projet

```
esprit-PIDEV-JAVA-Sport-Insight-3A46/
├── pom.xml                              # Configuration Maven
├── README.md                            # Documentaire principal
├── SETUP.md                             # Ce fichier
├── run.bat                              # Script d'exécution (Windows)
├── run.sh                               # Script d'exécution (Linux/Mac)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java         # Configuration des modules Java
│   │   │   └── tn/esprit/
│   │   │       ├── mains/
│   │   │       │   ├── LoginApp.java    # Point d'entrée (interface de connexion)
│   │   │       │   ├── Main.java        # Point d'entrée principal
│   │   │       │   └── UserMain.java    # Point d'entrée utilisateur
│   │   │       ├── controllers/
│   │   │       │   └── LoginController.java
│   │   │       ├── entities/            # Modèles JPA/ORM
│   │   │       ├── services/
│   │   │       │   ├── IService.java
│   │   │       │   ├── IUserService.java
│   │   │       │   └── UserService.java
│   │   │       └── tools/
│   │   │           └── MyConnection.java (Connexion à la DB)
│   │   └── resources/
│   │       ├── views/
│   │       │   ├── login.fxml
│   │       │   └── register.fxml
│   │       └── images/
│   │           └── sportinsight.png
│   └── test/                            # Tests unitaires
└── target/                              # Fichiers compilés (généré)
```

## 🔧 Configurations importantes

### module-info.java
Définit les modules Java requis et les packages exposés à JavaFX:
```java
module untitled {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;

    // Packages ouverts à JavaFX pour le chargement des FXML
    opens tn.esprit.views to javafx.fxml;
    opens tn.esprit.controllers to javafx.fxml;
    opens tn.esprit.mains to javafx.fxml;

    exports tn.esprit.mains;
}
```

### pom.xml
- **JavaFX version:** 21.0.1
- **Modules inclus:** controls, fxml, graphics, base
- **MySQL:** mysql-connector-j 8.4.0
- **Plugins:** maven-compiler-plugin, javafx-maven-plugin

## ⚙️ Dépannage

### Erreur: "JavaFX runtime components are missing"
**Cause:** Les modules JavaFX ne sont pas trouvés au runtime.

**Solutions:**
1. Vérifiez que Maven a bien téléchargé les dépendances:
   ```bash
   mvn dependency:resolve
   ```
2. Relancez la compilation:
   ```bash
   mvn clean compile
   ```
3. Si vous utilisez IntelliJ IDEA:
   - Allez à **File > Invalidate Caches > Invalidate and Restart**
   - Ré-ouvrez le projet

### Erreur: "Cannot find FXML file"
**Cause:** Le chemin du fichier FXML est incorrect.

**Vérifiez:**
- Les fichiers FXML doivent être dans `src/main/resources/views/`
- Les chemins dans le code Java doivent être: `/views/login.fxml`
- Les images doivent être dans `src/main/resources/images/`

### Erreur: "Cannot access class LoginController"
**Cause:** Le package n'est pas ouvert à JavaFX dans `module-info.java`.

**Vérifiez le fichier `module-info.java`:**
```java
opens tn.esprit.controllers to javafx.fxml;
```

### Maven command not found
**Cause:** Maven n'est pas installé ou n'est pas dans le PATH.

**Solution:**
1. Téléchargez Maven
2. Extrayez-le
3. Ajoutez le dossier `bin` au PATH Windows:
   - Clic droit sur "Ce PC" > Propriétés
   - Variables d'environnement
   - Sélectionnez Path > Modifier
   - Ajouter le chemin du dossier bin de Maven

## 📦 Dépendances principales

| Dépendance | Version | Utilisation |
|-----------|---------|-------------|
| javafx-controls | 21.0.1 | Composants UI JavaFX |
| javafx-fxml | 21.0.1 | Chargement des fichiers FXML |
| javafx-graphics | 21.0.1 | Rendu graphique |
| javafx-base | 21.0.1 | Base JavaFX |
| mysql-connector-j | 8.4.0 | Connexion MySQL |

## 🔐 Sécurité

- Les mots de passe sont stockés en clair dans la base de données (à améliorer avec du hachage)
- Assurez-vous que la base de données est sécurisée
- N'exposez pas les identifiants de connexion MySQL

## 📝 Prochaines étapes

1. Configurer la connexion MySQL dans `tools/MyConnection.java`
2. Importer ou créer la schéma de base de données
3. Implémenter les contrôleurs pour les autres écrans (dashboard, etc.)
4. Ajouter la validation des formulaires
5. Implémenter la gestion des rôles utilisateur

## 🆘 Support

Pour des problèmes spécifiques:
- Consultez la documentation JavaFX: https://openjfx.io/
- Documentation Maven: https://maven.apache.org/

## ✅ Checklist de validation

- [ ] Java JDK 17+ installé et configuré
- [ ] Maven 3.8+ installé et configuré
- [ ] MySQL en cours d'exécution
- [ ] `mvn -v` affiche la version
- [ ] `mvn clean compile` se termine sans erreur
- [ ] `mvn javafx:run` lance l'application
- [ ] L'écran de connexion s'affiche

---

**Date de création:** 2026-04-12
**Version:** 1.0


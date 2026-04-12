# Sport Insight - JavaFX Application

## Prérequis
- **Java JDK 17+** (installé et configuré dans le PATH)
- **Maven 3.8+** (installé et configuré dans le PATH)
- **MySQL** (pour la base de données)

## Installation

### 1. Vérifier Maven
```bash
mvn -v
```
Si Maven n'est pas trouvé, téléchargez-le depuis https://maven.apache.org/download.cgi et ajoutez le dossier `bin` au PATH.

### 2. Compiler le projet
```bash
mvn clean compile
```

### 3. Exécuter l'application

#### Option A: Avec Maven (recommandé)
```bash
mvn javafx:run
```

#### Option B: Double-cliquer sur `run.bat` (Windows)
```
run.bat
```

#### Option C: Depuis IntelliJ IDEA
1. Allez dans **Run > Edit Configurations...**
2. Sélectionnez la configuration pour `LoginApp`
3. Dans « VM options », ajoutez:
   ```
   --add-modules javafx.controls,javafx.fxml
   ```
4. Cliquez sur « Run »

## Structure du projet

```
src/
├── main/
│   ├── java/
│   │   ├── tn/esprit/
│   │   │   ├── controllers/     # Contrôleurs JavaFX
│   │   │   ├── entities/        # Modèles de base de données
│   │   │   ├── mains/           # Points d'entrée (LoginApp, Main, etc.)
│   │   │   ├── services/        # Services (UserService, etc.)
│   │   │   └── tools/           # Utilitaires (MyConnection, etc.)
│   │   └── module-info.java     # Configuration des modules Java
│   └── resources/
│       ├── views/               # Fichiers FXML (UI)
│       │   ├── login.fxml
│       │   └── register.fxml
│       └── images/              # Images et ressources
└── target/                       # Fichiers compilés
```

## Configuration

### module-info.java
Le fichier `module-info.java` configure les modules Java pour JavaFX :
- Ouvre le package `tn.esprit.views` à `javafx.fxml` (pour charger les fichiers FXML)
- Ouvre le package `tn.esprit.controllers` à `javafx.fxml` (pour accéder aux contrôleurs)
- Exporte le package `tn.esprit.mains` (point d'entrée)

### pom.xml
Le fichier `pom.xml` contient :
- Dépendances JavaFX (controls, fxml, graphics, base)
- Plugin Maven JavaFX pour l'exécution facile
- Configuration du compilateur Java 17

## Dépannage

### Erreur: "JavaFX runtime components are missing"
1. Vérifiez que Maven a bien téléchargé les dépendances:
   ```bash
   mvn dependency:resolve
   ```
2. Si vous utilisez IntelliJ IDEA, relancez `mvn clean compile` depuis le terminal
3. Dans IntelliJ, allez à **File > Invalidate Caches > Invalidate and Restart**

### Erreur: "Cannot find FXML file"
Vérifiez que le chemin dans le code Java correspond à la structure des ressources:
- Les chemins doivent commencer par `/views/` ou `/images/`
- Les fichiers FXML doivent être dans `src/main/resources/views/`

### Erreur: "Cannot access class LoginController"
Vérifiez le fichier `module-info.java` pour s'assurer que:
```java
opens tn.esprit.controllers to javafx.fxml;
opens tn.esprit.views to javafx.fxml;
```

## Déploiement

Pour créer un JAR exécutable avec toutes les dépendances :
```bash
mvn clean package
```

## Support
Pour plus d'aide, consultez la documentation JavaFX:
https://openjfx.io/



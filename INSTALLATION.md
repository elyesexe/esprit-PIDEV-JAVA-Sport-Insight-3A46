# 📚 Guide d'Installation et Configuration

## 1️⃣ Prérequis

### Logiciels obligatoires
- **Java JDK 17+** - [Télécharger](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6+** - [Télécharger](https://maven.apache.org/download.cgi)
- **MySQL Server 5.7+** - [Télécharger](https://www.mysql.com/downloads/)
- **Git** (optionnel) - [Télécharger](https://git-scm.com/)

### IDE recommandés (optionnel)
- **IntelliJ IDEA Community** - [Télécharger](https://www.jetbrains.com/idea/)
- **Eclipse IDE** - [Télécharger](https://www.eclipse.org/)
- **VS Code** avec extensions Java

## 2️⃣ Installation de Java

### Windows
1. Téléchargez l'installeur JDK depuis oracle.com
2. Exécutez l'installeur
3. Notez le chemin d'installation (ex: `C:\Program Files\Java\jdk-17`)
4. Ajoutez à la variable d'environnement PATH:
   - Appuyez sur `Win + X` → Paramètres
   - Recherchez "Variables d'environnement"
   - Ajoutez le chemin du JDK au PATH

### Vérifier l'installation
```bash
java -version
javac -version
```

## 3️⃣ Installation de Maven

### Windows
1. Téléchargez Maven
2. Décompressez dans un répertoire (ex: `C:\apache-maven-3.8.1`)
3. Ajoutez `C:\apache-maven-3.8.1\bin` au PATH

### Vérifier l'installation
```bash
mvn -version
```

## 4️⃣ Installation de MySQL

### Windows
1. Téléchargez MySQL Community Server
2. Exécutez l'installeur MySQL
3. Configuration pendant l'installation:
   - **Port**: 3306 (défaut)
   - **Root password**: Notez-le bien!
   - **Service name**: MySQL80 (ou autre version)

### Vérifier l'installation
```bash
mysql --version
```

## 5️⃣ Configuration de la Base de Données

### Créer la base de données

```bash
# Se connecter à MySQL en tant que root
mysql -u root -p

# Exécuter le script SQL
source schema.sql;
```

Ou importer le fichier `schema.sql`:

1. Ouvrez MySQL Workbench
2. File → Open SQL Script → Sélectionnez `schema.sql`
3. Execute (Ctrl + Shift + Enter)

### Vérifier la création
```sql
SHOW DATABASES;
USE sport_insight;
SHOW TABLES;
DESCRIBE annonce;
DESCRIBE commentaire;
```

## 6️⃣ Configuration du Projet

### Étape 1: Mettre à jour MyConnection.java

Ouvrez `src/main/java/tn/esprit/tools/MyConnection.java` et modifiez:

```java
private static final String URL = "jdbc:mysql://localhost:3306/sport_insight";
private static final String USER = "root";
private static final String PASSWORD = "votre_mot_de_passe_mysql";
```

**Exemple avec mot de passe "Sport123":**
```java
private static final String PASSWORD = "Sport123";
```

### Étape 2: Vérifier le pom.xml

Assurez-vous que les dépendances sont correctes:
```bash
mvn dependency:tree
```

## 7️⃣ Compilation du Projet

### Via Maven (Recommandé)

```bash
# Se placer dans le répertoire du projet
cd C:\esprit-PIDEV-JAVA-Sport-Insight-3A46

# Nettoyer et compiler
mvn clean install

# Ou directement compiler
mvn compile
```

### Via IDE

**IntelliJ IDEA:**
1. File → Open → Sélectionnez le dossier du projet
2. Attendez que Maven configure le projet
3. Build → Build Project (Ctrl + F9)

**Eclipse:**
1. File → Import → Existing Maven Projects
2. Sélectionnez le dossier du projet
3. Right-click → Maven → Update Project

## 8️⃣ Exécution de l'Application

### Via Maven

```bash
mvn javafx:run
```

### Via ligne de commande (après compilation)

```bash
java -cp "target/classes:target/lib/*" tn.esprit.javafx.SportInsightApplication
```

### Via le script batch (Windows)

```bash
run.bat
```

### Via IDE

**IntelliJ IDEA:**
1. Right-click sur `SportInsightApplication.java`
2. Run 'SportInsightApplication'

**Eclipse:**
1. Right-click sur le projet
2. Run As → Java Application
3. Sélectionnez `SportInsightApplication`

## 9️⃣ Dépannage

### Erreur: "java: command not found"
**Solution**: Java n'est pas dans le PATH
- Installez JDK correctement
- Ajoutez le chemin au PATH
- Redémarrez le terminal/CMD

### Erreur: "Unable to load FXML"
**Solution**: Les fichiers FXML ne sont pas trouvés
```bash
# Vérifiez que les fichiers existent
dir src\main\resources\*.fxml

# Recompiler le projet
mvn clean compile
```

### Erreur: "Connection refused to host: 127.0.0.1"
**Solution**: MySQL n'est pas en cours d'exécution
```bash
# Vérifier le service MySQL (Windows)
net start MySQL80

# Ou redémarrer MySQL
```

### Erreur: "Access denied for user 'root'"
**Solution**: Mot de passe incorrect dans MyConnection.java
1. Vérifiez votre mot de passe MySQL
2. Testez la connexion:
```bash
mysql -u root -p sport_insight
```

### Erreur: "Unknown database 'sport_insight'"
**Solution**: La base de données n'existe pas
```bash
mysql -u root -p < schema.sql
```

### Erreur de compilation Maven
**Solution**: Problème de dépendances
```bash
mvn clean
mvn install -DskipTests
```

## 🔟 Tests de Fonctionnement

### Test de connexion à la BD
```java
// Créez un petit programme de test:
public static void main(String[] args) {
    try {
        AnnonceService service = new AnnonceService();
        List<Annonce> annonces = service.getAll();
        System.out.println("✅ Connexion réussie! " + annonces.size() + " annonces trouvées.");
    } catch (Exception e) {
        System.out.println("❌ Erreur: " + e.getMessage());
    }
}
```

### Test de l'interface JavaFX
1. Lancez l'application
2. Naviguez vers l'onglet "Annonces"
3. Vous devriez voir 3 annonces de test
4. Testez l'ajout d'une nouvelle annonce

## 📋 Checklist Pré-Production

- [ ] Java 17+ installé et configuré
- [ ] Maven installé et configuré
- [ ] MySQL en cours d'exécution
- [ ] Base de données créée avec schema.sql
- [ ] MyConnection.java configuré correctement
- [ ] Projet compilé sans erreurs
- [ ] Application démarre sans erreur
- [ ] Les données de test sont visibles
- [ ] Les opérations CRUD fonctionnent

## 📞 Support

Si vous rencontrez des problèmes:

1. **Vérifiez les logs**: Consultez la sortie console
2. **Vérifiez MySQL**: `mysql -u root -p`
3. **Vérifiez Java**: `java -version`
4. **Vérifiez Maven**: `mvn -version`
5. **Effacez le cache Maven**: `mvn clean`

## 🎓 Ressources Utiles

- [Documentation Java](https://docs.oracle.com/en/java/)
- [Documentation Maven](https://maven.apache.org/guides/)
- [Documentation MySQL](https://dev.mysql.com/doc/)
- [Documentation JavaFX](https://openjfx.io/)

---

**Dernière mise à jour**: 11 Avril 2026  
**Version**: 1.0.0


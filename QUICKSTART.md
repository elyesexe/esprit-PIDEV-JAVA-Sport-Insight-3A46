# ⚡ Quick Start Guide - Sport Insight

## 🚀 Démarrage Rapide en 5 Minutes

### 1️⃣ Prérequis (Vérifier d'abord)
```bash
# Java
java -version
# Doit afficher: "Java 17" ou supérieur

# Maven  
mvn -version
# Doit afficher une version

# MySQL
mysql --version
# Doit afficher une version
```

### 2️⃣ Base de Données
```bash
# Ouvrir MySQL
mysql -u root -p

# Dans MySQL:
SOURCE C:\chemin\vers\schema.sql;
# Ou importer le fichier schema.sql dans MySQL Workbench
```

### 3️⃣ Configuration
Ouvrir: `src/main/java/tn/esprit/tools/MyConnection.java`

Modifier les 3 lignes:
```java
private static final String URL = "jdbc:mysql://localhost:3306/sport_insight";
private static final String USER = "root";
private static final String PASSWORD = "votre_mot_de_passe_ici";
```

### 4️⃣ Compiler et Lancer
```bash
# Option A: Maven
cd C:\esprit-PIDEV-JAVA-Sport-Insight-3A46
mvn clean install
mvn javafx:run

# Option B: Script
run.bat

# Option C: IDE
# IntelliJ/Eclipse → Right-click → Run
```

### 5️⃣ Utiliser l'Application
- **Onglet 1**: Gestion des Annonces
  - Tableau à gauche, formulaire à droite
  - Cliquez sur une annonce pour la modifier
  - Recherche par titre/date/poste

- **Onglet 2**: Gestion des Commentaires
  - Même interface que les annonces
  - Gestion modération incluse
  - Recherche par annonce/joueur

---

## 📌 Cas d'Utilisation Rapide

### Ajouter une Annonce
1. Remplissez tous les champs du formulaire
2. Cliquez **➕ Ajouter**
3. Message "✅ Annonce ajoutée avec succès!"

### Modifier une Annonce
1. Cliquez sur la ligne dans le tableau
2. Modifiez les champs
3. Cliquez **✏️ Modifier**

### Supprimer une Annonce
1. Cliquez sur la ligne
2. Cliquez **🗑️ Supprimer**
3. Confirmez

### Rechercher
- **Par titre**: Entrez un mot → Cliquez 🔍
- **Par date**: Sélectionnez → Cliquez 📅
- **Par poste**: Entrez → Cliquez 📝
- **Réinitialiser**: Cliquez 🔄

---

## ⚠️ Problèmes Courants

### ❌ "Connection refused"
```bash
# MySQL n'est pas lancé, démarrer MySQL:
net start MySQL80
# Ou le redémarrer
```

### ❌ "Access denied"
- Vérifier le mot de passe dans MyConnection.java
- Tester: `mysql -u root -p`

### ❌ "Unknown database"
```bash
# Réimporter schema.sql:
mysql -u root -p < schema.sql
```

### ❌ "Unable to load FXML"
```bash
# Recompiler:
mvn clean compile
```

---

## 📂 Fichiers Importants

```
.
├── src/main/java/tn/esprit/
│   ├── tools/MyConnection.java       ← Configurer ceci
│   ├── controllers/
│   │   ├── AnnonceController.java
│   │   └── CommentaireController.java
│   └── javafx/
│       └── SportInsightApplication.java
├── src/main/resources/
│   ├── annonce_view.fxml
│   ├── commentaire_view.fxml
│   └── styles.css
├── schema.sql                        ← Exécuter ceci
├── README.md                         ← Lire ceci
└── INSTALLATION.md                   ← En cas de problème
```

---

## 🎯 Checklist d'Installation

- [ ] Java 17+ installé et dans PATH
- [ ] Maven installé et dans PATH
- [ ] MySQL en cours d'exécution
- [ ] `schema.sql` importé
- [ ] `MyConnection.java` configuré
- [ ] Projet compilé sans erreur
- [ ] Application démarre
- [ ] Données de test visibles
- [ ] CRUD fonctionnel

---

## 📚 Ressources

- **Problème détaillé?** → Consultez `INSTALLATION.md`
- **Toutes les fonctionnalités?** → Consultez `README.md`
- **Changements apportés?** → Consultez `CHANGES.md`
- **Erreur BD?** → Vérifiez `schema.sql`

---

## 🎓 Raccourcis Clavier

- **F5**: Rafraîchir les données
- **Delete**: Supprimer sélection (sur TableView)
- **Ctrl+A**: Sélectionner tout (dans TextArea)
- **Tab**: Naviguer entre champs
- **Enter**: Valider formulaire (dépend du contexte)

---

## 💡 Conseils

1. **Toujours tester avec les données de test d'abord**
2. **Créer un utilisateur dans la table `user` avant d'ajouter des annonces**
3. **Les IDs d'annonce et joueur doivent exister**
4. **Consulter la console pour les messages d'erreur**
5. **Les recherches utilisent SQL LIKE (partiel pour titre)**

---

## ✅ Test de Fonctionnement

Après le démarrage, vérifier:
1. ✅ Application affiche 2 onglets
2. ✅ 3 annonces visibles dans le tableau
3. ✅ 3 commentaires visibles dans l'autre onglet
4. ✅ Formulaires réactifs
5. ✅ Boutons fonctionnent
6. ✅ Messages de succès/erreur s'affichent

---

**Durée estimée de configuration complète**: 5-10 minutes  
**Durée estimée de résolution d'un problème**: < 5 minutes avec ce guide

Bon développement! 🚀


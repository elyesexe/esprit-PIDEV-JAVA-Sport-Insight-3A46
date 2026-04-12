# 🏆 Sport Insight - Gestion des Annonces et Commentaires

## 📋 Vue d'ensemble

Sport Insight est une application JavaFX pour la gestion des annonces sportives et des commentaires. L'application offre une interface graphique intuitive pour les opérations CRUD (Create, Read, Update, Delete) sur deux entités principales: **Annonces** et **Commentaires**.

## 🎯 Fonctionnalités

### Gestion des Annonces
- ✅ **Ajouter** une nouvelle annonce
- ✅ **Modifier** une annonce existante
- ✅ **Supprimer** une annonce
- ✅ **Afficher** toutes les annonces dans un tableau
- ✅ **Rechercher** par titre (avec LIKE)
- ✅ **Rechercher** par date de publication
- ✅ **Rechercher** par poste recherché
- ✅ **Rechercher** par titre ET date

### Gestion des Commentaires
- ✅ **Ajouter** un nouveau commentaire
- ✅ **Modifier** un commentaire existant
- ✅ **Supprimer** un commentaire
- ✅ **Afficher** tous les commentaires dans un tableau
- ✅ **Rechercher** par ID annonce
- ✅ **Rechercher** par ID joueur
- ✅ **Gérer** la modération des commentaires

## 🏗️ Architecture

```
src/main/java/tn/esprit/
├── entities/
│   ├── Annonce.java           # Classe modèle pour les annonces
│   └── Commentaire.java       # Classe modèle pour les commentaires
├── services/
│   ├── AnnonceService.java    # Logique métier pour les annonces
│   ├── CommentaireService.java # Logique métier pour les commentaires
│   └── IService.java          # Interface générique
├── controllers/
│   ├── AnnonceController.java     # Contrôleur JavaFX pour les annonces
│   └── CommentaireController.java # Contrôleur JavaFX pour les commentaires
├── javafx/
│   └── SportInsightApplication.java # Application principale
├── tools/
│   └── MyConnection.java      # Gestionnaire de connexion BD
└── mains/
    └── AnnonceMain.java       # Ancien menu console (optionnel)

src/main/resources/
├── annonce_view.fxml      # Interface des annonces
└── commentaire_view.fxml  # Interface des commentaires
```

## 🗄️ Base de Données

### Structure des tables

#### Table `annonce`
```sql
CREATE TABLE annonce (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    poste_recherche VARCHAR(100),
    niveau_requis VARCHAR(100),
    date_publication DATE,
    statut VARCHAR(20),
    entraineur_id INT,
    FOREIGN KEY (entraineur_id) REFERENCES user(id)
);
```

#### Table `commentaire`
```sql
CREATE TABLE commentaire (
    id INT PRIMARY KEY AUTO_INCREMENT,
    contenu TEXT NOT NULL,
    date_commentaire DATE,
    joueur_id INT,
    annonce_id INT,
    auteur_anonyme VARCHAR(100),
    nb_likes INT DEFAULT 0,
    moderation_status VARCHAR(20),
    moderation_reason TEXT,
    FOREIGN KEY (joueur_id) REFERENCES user(id),
    FOREIGN KEY (annonce_id) REFERENCES annonce(id)
);
```

## 🛠️ Dépendances

### Maven Dependencies
```xml
<!-- MySQL Connector -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
</dependency>

<!-- JavaFX Controls -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21.0.1</version>
</dependency>

<!-- JavaFX FXML -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>21.0.1</version>
</dependency>
```

## 🚀 Installation et Exécution

### Prérequis
- **Java JDK 17+** (testé avec Java 17+)
- **Maven 3.6+** ou un IDE supportant Maven
- **MySQL 5.7+** avec la base de données `sport_insight` créée
- **Git** (optionnel)

### Étapes d'installation

#### 1. Cloner ou télécharger le projet
```bash
cd C:\esprit-PIDEV-JAVA-Sport-Insight-3A46
```

#### 2. Configurer la connexion à la base de données
Modifiez `src/main/java/tn/esprit/tools/MyConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/sport_insight";
private static final String USER = "root";
private static final String PASSWORD = "votre_mot_de_passe";
```

#### 3. Compiler le projet
```bash
mvn clean install
```

#### 4. Exécuter l'application
```bash
mvn javafx:run
```

Ou via le script fourni (Windows):
```bash
run.bat
```

## 📱 Guide d'utilisation

### Onglet Annonces

#### Ajouter une annonce:
1. Remplissez tous les champs du formulaire à droite
2. Cliquez sur le bouton **➕ Ajouter**
3. Un message de succès apparaîtra

#### Modifier une annonce:
1. Cliquez sur une annonce dans le tableau
2. Ses données apparaissent dans le formulaire
3. Modifiez les champs souhaités
4. Cliquez sur **✏️ Modifier**

#### Supprimer une annonce:
1. Sélectionnez une annonce dans le tableau
2. Cliquez sur **🗑️ Supprimer**
3. Confirmez la suppression

#### Rechercher des annonces:
- **Par titre**: Entrez un mot-clé et cliquez **🔍 Titre**
- **Par date**: Sélectionnez une date et cliquez **📅 Date**
- **Par poste**: Entrez le poste et cliquez **📝 Poste**
- **Réinitialiser**: Cliquez **🔄 Réinitialiser** pour voir toutes les annonces

### Onglet Commentaires

#### Ajouter un commentaire:
1. Remplissez tous les champs du formulaire
2. Cliquez sur **➕ Ajouter**

#### Modifier/Supprimer:
Procédure similaire aux annonces

#### Rechercher des commentaires:
- **Par joueur**: Entrez l'ID joueur et cliquez **👤 Joueur**
- **Par annonce**: Entrez l'ID annonce et cliquez **📢 Annonce**

## 🔍 Fonctionnalités de Recherche

### Recherche avancée (Annonces)
- **Recherche par titre** (LIKE %): Trouvez les annonces contenant le mot-clé
- **Recherche par date**: Trouvez les annonces d'une date spécifique
- **Recherche par titre ET date**: Combinaison de critères
- **Recherche par poste**: Filtrez les annonces pour un poste spécifique

## 📝 Formats attendus

### Annonce
| Champ | Format | Exemple |
|-------|--------|---------|
| Titre | Texte | "Gardien expérimenté recherché" |
| Description | Texte long | "Nous cherchons un gardien..." |
| Poste | Texte | "Gardien" |
| Niveau requis | Texte | "Professionnel" |
| Date publication | YYYY-MM-DD | 2026-04-11 |
| Statut | ACTIVE / CLOSED | ACTIVE |
| ID Entraîneur | Nombre entier | 1 |

### Commentaire
| Champ | Format | Exemple |
|-------|--------|---------|
| Contenu | Texte | "Excellent joueur..." |
| ID Joueur | Nombre entier | 5 |
| ID Annonce | Nombre entier | 3 |
| Auteur | Texte | "John Doe" |
| Likes | Nombre entier | 42 |
| Modération | APPROVED / PENDING / REJECTED | APPROVED |
| Date | YYYY-MM-DD | 2026-04-11 |

## 🐛 Résolution des problèmes

### "Unable to load FXML file"
- Assurez-vous que les fichiers `.fxml` sont dans `src/main/resources/`
- Vérifiez que le chemin dans le FXMLLoader correspond

### "Connection refused"
- Vérifiez que MySQL est en cours d'exécution
- Vérifiez les identifiants dans `MyConnection.java`
- Vérifiez que la base de données `sport_insight` existe

### "Foreign key constraint fails"
- Assurez-vous que l'ID entraîneur existe dans la table `user`
- Vérifiez que l'ID annonce existe avant d'ajouter un commentaire

### "TableView appears empty"
- Vérifiez les noms de colonnes dans le FXML
- Assurez-vous que les cellValueFactory sont correctement configurées

## 📚 API des Services

### AnnonceService
```java
// CRUD basique
void add(Annonce annonce)
void update(Annonce annonce)
void delete(int id)
List<Annonce> getAll()
Annonce getById(int id)

// Recherches spécialisées
List<Annonce> searchByTitre(String titre)
List<Annonce> searchByDatePublication(LocalDate date)
List<Annonce> searchByTitreAndDate(String titre, LocalDate date)
List<Annonce> getAnnoncesByPoste(String poste)
List<Annonce> getAnnoncesByEntraineur(int entraineurId)
List<Annonce> getAnnoncesActives()
```

### CommentaireService
```java
// CRUD basique
void add(Commentaire commentaire)
void update(Commentaire commentaire)
void delete(int id)
List<Commentaire> getAll()
Commentaire getById(int id)

// Recherches spécialisées
List<Commentaire> getCommentairesByAnnonce(int annonceId)
List<Commentaire> getCommentairesByJoueur(int joueurId)
```

## 🎨 Interface Utilisateur

### Thème
- **Couleur principale**: Bleu foncé (#2C3E50)
- **Accents**: Orange/Gris (#34495E, #ECF0F1)
- **Police**: Système par défaut
- **Icônes**: Emojis pour une meilleure UX

### Composants
- **Tables**: Affichage en temps réel des données
- **Formulaires**: Saisie structurée des données
- **Boutons**: Actions claires et intuitives
- **Messages**: Feedback utilisateur en couleurs

## 🔐 Sécurité

- **Prepared Statements**: Utilisés pour prévenir les injections SQL
- **Gestion des erreurs**: Try-catch avec messages d'erreur informatifs
- **Validation**: Vérification des champs avant insertion
- **Foreign Keys**: Appliquées au niveau de la base de données

## 🚄 Performance

- **Lazy Loading**: Les données sont chargées à la demande
- **Cache**: Les services utilisent une connexion singleton
- **Indexation**: Assurez-vous que les index sont créés sur les colonnes de recherche

```sql
CREATE INDEX idx_titre ON annonce(titre);
CREATE INDEX idx_date ON annonce(date_publication);
CREATE INDEX idx_poste ON annonce(poste_recherche);
CREATE INDEX idx_annonce_id ON commentaire(annonce_id);
CREATE INDEX idx_joueur_id ON commentaire(joueur_id);
```

## 📞 Support

Pour les problèmes ou les questions:
1. Vérifiez les logs de la console
2. Consultez le fichier d'erreur MySQL
3. Vérifiez les configurations dans `MyConnection.java`

## 🎓 Améliorations futures

- [ ] Système d'authentification utilisateur
- [ ] Pagination des tableaux
- [ ] Export des données (PDF, Excel)
- [ ] Graphiques statistiques
- [ ] Notifications en temps réel
- [ ] Mode sombre
- [ ] Internationalisation (i18n)
- [ ] Synchronisation avec API REST

## 📄 Licence

Projet éducatif - ESPRIT École d'Ingénierie

## 👨‍💻 Auteur

Créé pour le projet PIDEV JAVA - 3A46

---

**Dernière mise à jour**: 11 Avril 2026  
**Version**: 1.0.0 - Production Ready


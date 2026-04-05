etor# Sport Insight - Système de Gestion des Sponsors et Contrats

## Description
Application Java complète pour gérer les sponsors et les contrats sponsors avec fonctionnalités CRUD (Create, Read, Update, Delete) et recherche avancée.

## Prérequis
- Java JDK 17 ou supérieur
- MySQL/MariaDB installé et en cours d'exécution
- Maven 3.9.14+ (optionnel, le projet peut être compilé avec javac)

## Installation

### 1. Configuration de la base de données
Assurez-vous que votre base de données `sport_insight` existe et est accessible.

### 2. Dépendances Maven
Les dépendances suivantes sont installées dans `~/.m2/repository/`:
- `mysql-connector-j-8.4.0.jar`
- `protobuf-java-3.25.1.jar`

### 3. Compilation

**Option 1 : Utiliser le script batch (Windows)**
```bash
compile.bat
```

**Option 2 : Compilation manuelle avec javac**
```bash
javac -d target/classes -cp "C:\Users\hamou\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar;C:\Users\hamou\.m2\repository\com\google\protobuf\protobuf-java\3.25.1\protobuf-java-3.25.1.jar" src/main/java/tn/esprit/entities/*.java src/main/java/tn/esprit/tools/*.java src/main/java/tn/esprit/services/*.java src/main/java/tn/esprit/mains/*.java
```

**Option 3 : Utiliser Maven**
```bash
mvn clean compile
```

## Exécution

### Option 1 : Utiliser le script batch (Windows)
```bash
run.bat
```

### Option 2 : Exécution manuelle
```bash
java -cp "target/classes;C:\Users\hamou\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar;C:\Users\hamou\.m2\repository\com\google\protobuf\protobuf-java\3.25.1\protobuf-java-3.25.1.jar" tn.esprit.mains.Main
```

## Structure du Projet

```
esprit-PIDEV-JAVA-Sport-Insight-3A46/
├── src/
│   └── main/
│       └── java/
│           └── tn/
│               └── esprit/
│                   ├── entities/
│                   │   ├── Sponsor.java
│                   │   ├── ContratSponsor.java
│                   │   └── ... (autres entités)
│                   ├── services/
│                   │   ├── IService.java (interface générique)
│                   │   ├── SponsorService.java
│                   │   └── ContratSponsorService.java
│                   ├── tools/
│                   │   └── MyConnection.java (connexion BD)
│                   └── mains/
│                       └── Main.java (interface CLI)
├── target/
│   └── classes/ (fichiers compilés)
├── pom.xml
├── compile.bat
├── run.bat
├── CRUD_GUIDE_FR.md
└── README.md
```

## Fonctionnalités

### Gestion des Sponsors
- ✅ **Créer** un nouveau sponsor
- ✅ **Lire** tous les sponsors ou un sponsor spécifique
- ✅ **Mettre à jour** les informations d'un sponsor
- ✅ **Supprimer** un sponsor
- ✅ **Rechercher** par nom, email, téléphone ou adresse

**Champs d'un Sponsor:**
- ID (auto-généré)
- Nom
- Email
- Téléphone
- Budget
- Nom du logo
- Adresse
- Date de mise à jour

### Gestion des Contrats Sponsor
- ✅ **Créer** un nouveau contrat
- ✅ **Lire** tous les contrats ou un contrat spécifique
- ✅ **Mettre à jour** les informations d'un contrat
- ✅ **Supprimer** un contrat
- ✅ **Rechercher** par description, statut ou statut de paiement
- ✅ **Filtrer** par Sponsor ID

**Champs d'un Contrat Sponsor:**
- ID (auto-généré)
- Date de début
- Date de fin
- Montant
- Description
- Statut
- Statut de notificationn
- Statut de paiement
- ID du Sponsor (clé étrangère)
- ID de l'Équipe (clé étrangère)

## Utilisation

### Menu Principal
```
============================================
      SPONSOR & CONTRACT MANAGEMENT
============================================
1. Sponsor Management
2. Contract Sponsor Management
0. Exit
```

### Exemple d'Ajout de Sponsor
```
1. Sponsor Management
1. Add Sponsor
Name: Nike
Email: contact@nike.com
Telephone: +33123456789
Budget: 50000.00
Logo Name: nike_logo.png
Address: Paris, France
```

### Exemple de Recherche
```
1. Sponsor Management
4. Search Sponsor
Search keyword: nike
--- SEARCH RESULTS ---
Sponsor{id=1, nom='Nike', email='contact@nike.com', ...}
```

## Format des Données

### Dates
Format accepté : **YYYY-MM-DD** (2024-01-15)

### Nombres
- Budget et montant : Nombres décimaux (ex: 50000.50)
- IDs : Nombres entiers

### Chaînes de caractères
- Acceptent les espaces et caractères spéciaux
- Aucune limitation de longueur spécifiée

## Gestion des Erreurs

### Erreurs courantes et solutions

1. **"Field 'id_equipe' doesn't have a default value"**
   - Assurez-vous que la table `contrat_sponsor` a les bonnes contraintes
   - Vérifiez la colonne `equipe_id` dans la base de données

2. **"Cannot delete or update a parent row: a foreign key constraint fails"**
   - Supprimez d'abord tous les contrats associés au sponsor
   - Ensuite, supprimez le sponsor

3. **"Connection refused"**
   - Vérifiez que MySQL est en cours d'exécution
   - Vérifiez les identifiants de connexion dans `MyConnection.java`
   - Assurez-vous que la base de données `sport_insight` existe

4. **"mvn : Le terme 'mvn' n'est pas reconnu"**
   - Maven n'est pas configuré dans les variables d'environnement PATH
   - Utilisez les scripts `compile.bat` et `run.bat` ou compilez avec `javac` directement

## Architecture Logicielle

### Modèle Service
- **IService<T>** : Interface générique avec méthodes CRUD de base
- **SponsorService** : Implémentation pour Sponsor
- **ContratSponsorService** : Implémentation pour ContratSponsor

### Modèle de Données
- Entités avec constructeurs appropriés
- Getters et setters pour tous les champs
- Méthodes `toString()` pour l'affichage

### Interface Utilisateur
- Menu interactif basé sur la console
- Gestion des erreurs de saisie utilisateur
- Navigation entre les menus

## Sécurité

✅ **Prepared Statements** - Protection contre les injections SQL
✅ **Validation d'entrée** - Vérification des types numériques
✅ **Gestion des ressources** - Fermeture des connexions et requêtes

## Améliorations Futures
- [ ] Interface graphique (Swing/JavaFX)
- [ ] Pagination pour les listes de résultats
- [ ] Filtres multiples pour la recherche
- [ ] Export des données (CSV, PDF)
- [ ] Authentification et autorisations
- [ ] Tests unitaires
- [ ] Logging structuré

## Support
Pour toute question ou problème, veuillez vérifier :
1. Le format des entrées
2. La connexion à la base de données
3. Les fichiers JAR dépendances
4. Les permissions d'accès aux fichiers

## Licence
Projet académique - ESPRIT 3A

## Auteur
Développé pour le projet PIDEV

---

**Dernière mise à jour :** 2024


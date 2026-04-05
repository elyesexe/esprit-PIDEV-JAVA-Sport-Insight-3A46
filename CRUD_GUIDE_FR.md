# CRUD Sponsor et Contrat Sponsor avec Recherche

## Vue d'ensemble
Ce projet implémente un système de gestion complet (CRUD) pour les Sponsors et les Contrats Sponsor avec fonctionnalité de recherche.

## Fonctionnalités

### Sponsor Management (Gestion des Sponsors)
Les opérations suivantes sont disponibles :

1. **Ajouter un Sponsor** - Add Sponsor
   - Nom
   - Email
   - Téléphone
   - Budget
   - Nom du logo
   - Adresse

2. **Voir tous les Sponsors** - View All Sponsors
   - Liste complète de tous les sponsors

3. **Voir un Sponsor par ID** - View Sponsor by ID
   - Chercher un sponsor spécifique par son ID

4. **Rechercher des Sponsors** - Search Sponsor
   - Rechercher par : nom, email, téléphone ou adresse
   - Utilise des motifs de recherche (LIKE)

5. **Mettre à jour un Sponsor** - Update Sponsor
   - Modifier les informations d'un sponsor existant
   - Champs optionnels (appuyez sur Entrée pour ignorer)

6. **Supprimer un Sponsor** - Delete Sponsor
   - Supprimer un sponsor par son ID

### Contract Sponsor Management (Gestion des Contrats Sponsor)
Les opérations suivantes sont disponibles :

1. **Ajouter un Contrat** - Add Contract
   - Date de début (YYYY-MM-DD)
   - Date de fin (YYYY-MM-DD)
   - Montant
   - Description
   - Statut
   - Sponsor ID
   - Équipe ID

2. **Voir tous les Contrats** - View All Contracts
   - Liste complète de tous les contrats

3. **Voir un Contrat par ID** - View Contract by ID
   - Chercher un contrat spécifique par son ID

4. **Rechercher des Contrats** - Search Contract
   - Rechercher par : description, statut ou statut de paiement
   - Utilise des motifs de recherche (LIKE)

5. **Mettre à jour un Contrat** - Update Contract
   - Modifier le montant, le statut ou le statut de paiement
   - Champs optionnels (appuyez sur Entrée pour ignorer)

6. **Supprimer un Contrat** - Delete Contract
   - Supprimer un contrat par son ID

7. **Voir les Contrats par Sponsor ID** - View Contracts by Sponsor ID
   - Voir tous les contrats associés à un sponsor

## Architecture

### Classes principales
- **Sponsor.java** - Entité représentant un sponsor
- **ContratSponsor.java** - Entité représentant un contrat sponsor
- **SponsorService.java** - Service CRUD pour les sponsors avec recherche
- **ContratSponsorService.java** - Service CRUD pour les contrats avec recherche
- **IService.java** - Interface générique pour tous les services
- **Main.java** - Interface utilisateur interactive avec menu

### Technologie
- **Java 17+**
- **MySQL Connector/J 8.4.0**
- **JDBC pour l'accès à la base de données**

## Comment compiler

```bash
javac -d target/classes -cp "chemin/mysql-connector-j.jar" src/main/java/tn/esprit/entities/*.java src/main/java/tn/esprit/tools/*.java src/main/java/tn/esprit/services/*.java src/main/java/tn/esprit/mains/*.java
```

## Comment exécuter

```bash
java -cp "target/classes;chemin/mysql-connector-j.jar;chemin/protobuf-java.jar" tn.esprit.mains.Main
```

## Fonctionnalités de recherche

### Recherche de Sponsors
- **Mots-clés acceptés** : nom, email, téléphone, adresse
- **Type de recherche** : Partial matching (LIKE SQL)
- **Exemple** : Taper "tech" trouvera tous les sponsors avec "tech" dans le nom, email, téléphone ou adresse

### Recherche de Contrats
- **Mots-clés acceptés** : description, statut, statut de paiement
- **Type de recherche** : Partial matching (LIKE SQL)
- **Exemple** : Taper "PENDING" trouvera tous les contrats avec le statut "PENDING"

## Flux de travail typique

### Pour ajouter un sponsor :
1. Sélectionner "1. Sponsor Management"
2. Sélectionner "1. Add Sponsor"
3. Entrer les informations demandées
4. Confirmer l'ajout

### Pour chercher un sponsor :
1. Sélectionner "1. Sponsor Management"
2. Sélectionner "4. Search Sponsor"
3. Entrer le mot-clé de recherche
4. Affichage des résultats correspondants

### Pour mettre à jour un sponsor :
1. Sélectionner "1. Sponsor Management"
2. Sélectionner "5. Update Sponsor"
3. Entrer l'ID du sponsor
4. Modifier les champs souhaités (appuyez sur Entrée pour ignorer)
5. Confirmer la mise à jour

### Pour supprimer un sponsor :
1. Sélectionner "1. Sponsor Management"
2. Sélectionner "6. Delete Sponsor"
3. Entrer l'ID du sponsor
4. Confirmer la suppression

## Notes de sécurité
- Les requêtes SQL utilisent des Prepared Statements pour éviter les injections SQL
- Les dates doivent être au format YYYY-MM-DD
- Les champs optionnels permettent une mise à jour partielle

## Gestion des erreurs
- Les erreurs de connexion sont affichées lors du démarrage
- Les erreurs de base de données sont affichées lors des opérations
- Les erreurs de saisie (nombres invalides) sont gérées gracieusement

## Développement futur
- Ajout d'une interface graphique (Swing/JavaFX)
- Pagination pour les listes de résultats
- Filtres multiples pour la recherche
- Export des données (CSV, PDF)
- Authentification et autorisations


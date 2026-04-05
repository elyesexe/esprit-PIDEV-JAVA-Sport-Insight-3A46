u# Guide de Test Complet - CRUD Sponsor et Contrat Sponsor

## Scénarios de Test

### Test 1 : Ajouter et Visualiser des Sponsors

#### Étape 1 : Accéder au Menu des Sponsors
```
Options principales:
1 (Sponsor Management)
```

#### Étape 2 : Ajouter un premier sponsor
```
Choix: 1 (Add Sponsor)

Saisir les informations:
Name: Apple Inc
Email: contact@apple.com
Telephone: +33612345678
Budget: 100000.00
Logo Name: apple_logo.png
Address: Cupertino, California

Résultat attendu: "Sponsor added successfully!"
```

#### Étape 3 : Ajouter un deuxième sponsor
```
Choix: 1 (Add Sponsor)

Name: Microsoft Corporation
Email: info@microsoft.com
Telephone: +33687654321
Budget: 150000.00
Logo Name: microsoft_logo.png
Address: Redmond, Washington

Résultat attendu: "Sponsor added successfully!"
```

#### Étape 4 : Visualiser tous les sponsors
```
Choix: 2 (View All Sponsors)

Résultat attendu:
Sponsor{id=1, nom='Apple Inc', email='contact@apple.com', ...}
Sponsor{id=2, nom='Microsoft Corporation', email='info@microsoft.com', ...}
```

---

### Test 2 : Recherche de Sponsors

#### Étape 1 : Rechercher par nom
```
Choix: 4 (Search Sponsor)
Search keyword: Apple

Résultat attendu:
Sponsor{id=1, nom='Apple Inc', ...}
```

#### Étape 2 : Rechercher par email
```
Choix: 4 (Search Sponsor)
Search keyword: microsoft

Résultat attendu:
Sponsor{id=2, nom='Microsoft Corporation', email='info@microsoft.com', ...}
```

#### Étape 3 : Rechercher par téléphone
```
Choix: 4 (Search Sponsor)
Search keyword: +336

Résultat attendu:
Sponsor{id=2, nom='Microsoft Corporation', telephone='+33687654321', ...}
```

#### Étape 4 : Rechercher par adresse
```
Choix: 4 (Search Sponsor)
Search keyword: California

Résultat attendu:
Sponsor{id=1, nom='Apple Inc', adresse='Cupertino, California', ...}
```

---

### Test 3 : Mettre à Jour un Sponsor

#### Étape 1 : Afficher un sponsor spécifique
```
Choix: 3 (View Sponsor by ID)
Enter Sponsor ID: 1

Résultat attendu:
Sponsor{id=1, nom='Apple Inc', budget=100000.0, ...}
```

#### Étape 2 : Mettre à jour le sponsor
```
Choix: 5 (Update Sponsor)
Enter Sponsor ID to update: 1

Current data: Sponsor{id=1, nom='Apple Inc', ...}
New Name (press Enter to skip): Apple Computers
New Email (press Enter to skip): 
New Telephone (press Enter to skip): 
New Budget (press Enter to skip): 120000.00
New Address (press Enter to skip): 

Résultat attendu: "Sponsor updated successfully!"
```

#### Étape 3 : Vérifier la mise à jour
```
Choix: 3 (View Sponsor by ID)
Enter Sponsor ID: 1

Résultat attendu:
Sponsor{id=1, nom='Apple Computers', budget=120000.0, ...}
```

---

### Test 4 : Ajouter et Gérer des Contrats Sponsor

#### Étape 1 : Accéder au Menu des Contrats
```
Menu principal: 2 (Contract Sponsor Management)
```

#### Étape 2 : Ajouter un contrat
```
Choix: 1 (Add Contract)

Saisir les informations:
Start Date (YYYY-MM-DD): 2024-01-01
End Date (YYYY-MM-DD): 2024-12-31
Amount: 50000.00
Description: Sponsorship for football team
Status: ACTIVE
Sponsor ID: 1
Team ID: 5

Résultat attendu: "Contrat Sponsor added successfully!"
```

#### Étape 3 : Visualiser tous les contrats
```
Choix: 2 (View All Contracts)

Résultat attendu:
ContratSponsor{id=1, dateDebut=2024-01-01, dateFin=2024-12-31, montant=50000.0, ...}
```

#### Étape 4 : Ajouter un deuxième contrat
```
Choix: 1 (Add Contract)

Start Date (YYYY-MM-DD): 2024-03-15
End Date (YYYY-MM-DD): 2025-03-14
Amount: 75000.00
Description: Equipment sponsorship
Status: PENDING
Sponsor ID: 2
Team ID: 5

Résultat attendu: "Contrat Sponsor added successfully!"
```

---

### Test 5 : Recherche dans les Contrats

#### Étape 1 : Rechercher par description
```
Choix: 4 (Search Contract)
Search keyword: football

Résultat attendu:
ContratSponsor{..., description='Sponsorship for football team', ...}
```

#### Étape 2 : Rechercher par statut
```
Choix: 4 (Search Contract)
Search keyword: ACTIVE

Résultat attendu:
ContratSponsor{..., statut='ACTIVE', ...}
```

#### Étape 3 : Filtrer par Sponsor ID
```
Choix: 7 (View Contracts by Sponsor ID)
Enter Sponsor ID: 1

Résultat attendu:
ContratSponsor{..., sponsorId=1, description='Sponsorship for football team', ...}
```

---

### Test 6 : Mettre à Jour un Contrat

#### Étape 1 : Afficher un contrat spécifique
```
Choix: 3 (View Contract by ID)
Enter Contract ID: 1

Résultat attendu:
ContratSponsor{id=1, montant=50000.0, statut='ACTIVE', ...}
```

#### Étape 2 : Mettre à jour le contrat
```
Choix: 5 (Update Contract)
Enter Contract ID to update: 1

Current data: ContratSponsor{..., montant=50000.0, statut='ACTIVE', ...}
New Amount (press Enter to skip): 55000.00
New Status (press Enter to skip): 
New Payment Status (press Enter to skip): PAID

Résultat attendu: "Contrat Sponsor updated successfully!"
```

#### Étape 3 : Vérifier la mise à jour
```
Choix: 3 (View Contract by ID)
Enter Contract ID: 1

Résultat attendu:
ContratSponsor{id=1, montant=55000.0, statutPaiement='PAID', ...}
```

---

### Test 7 : Suppression (à effectuer avec précaution)

#### Étape 1 : Supprimer un contrat
```
Choix: 6 (Delete Contract)
Enter Contract ID to delete: 1

Résultat attendu: "Contrat Sponsor deleted successfully!"
```

#### Étape 2 : Vérifier la suppression
```
Choix: 3 (View Contract by ID)
Enter Contract ID: 1

Résultat attendu: "Contract not found!"
```

#### Étape 3 : Supprimer un sponsor (après avoir supprimé ses contrats)
```
Choix: 1 (Sponsor Management)
Choix: 6 (Delete Sponsor)
Enter Sponsor ID to delete: 2

Résultat attendu: "Sponsor deleted successfully!"
```

---

### Test 8 : Gestion des Erreurs

#### Erreur 1 : Entrée invalide (chaîne au lieu d'un nombre)
```
Choix: abc

Résultat attendu: "Invalid input! Please enter a number."
```

#### Erreur 2 : ID inexistant
```
Choix: 3 (View Sponsor by ID)
Enter Sponsor ID: 999

Résultat attendu: "Sponsor not found!"
```

#### Erreur 3 : Date invalide
```
Choix: 1 (Add Contract)
Start Date (YYYY-MM-DD): 2024-13-01 (mois invalide)

Résultat attendu: Exception ou erreur de parsing
```

---

## Cas d'Usage Avancés

### Cas 1 : Gestion complète d'un Sponsor
```
1. Créer un sponsor
2. Ajouter plusieurs contrats pour ce sponsor
3. Rechercher les contrats par Sponsor ID
4. Mettre à jour les contrats
5. Mettre à jour les informations du sponsor
6. Supprimer les contrats associés
7. Supprimer le sponsor
```

### Cas 2 : Recherche Multicritères
```
1. Ajouter plusieurs sponsors avec données similaires
2. Effectuer des recherches partielles
3. Vérifier que les résultats sont corrects
```

### Cas 3 : Gestion des Dates
```
1. Ajouter un contrat avec des dates
2. Ajouter un contrat avec des dates qui se chevauchent
3. Vérifier les restrictions de dates (le cas échéant)
```

---

## Checklist de Test Complète

- [ ] Tous les sponsors peuvent être ajoutés
- [ ] Tous les sponsors peuvent être visualisés
- [ ] La recherche de sponsors fonctionne par nom
- [ ] La recherche de sponsors fonctionne par email
- [ ] La recherche de sponsors fonctionne par téléphone
- [ ] La recherche de sponsors fonctionne par adresse
- [ ] Les sponsors peuvent être mis à jour
- [ ] Les sponsors peuvent être supprimés
- [ ] Les contrats peuvent être ajoutés
- [ ] Les contrats peuvent être visualisés
- [ ] La recherche de contrats fonctionne
- [ ] Les contrats peuvent être filtrés par Sponsor ID
- [ ] Les contrats peuvent être mis à jour
- [ ] Les contrats peuvent être supprimés
- [ ] La gestion des erreurs fonctionne correctement
- [ ] Les dates sont correctement formatées
- [ ] Les montants et budgets sont correctement manipulés

---

## Données de Test Recommandées

### Sponsors
```
1. Apple Inc | contact@apple.com | +33612345678 | 100000 | Cupertino, California
2. Microsoft | info@microsoft.com | +33687654321 | 150000 | Redmond, Washington
3. Google | support@google.com | +33645321098 | 120000 | Mountain View, California
4. Amazon | business@amazon.com | +33611223344 | 180000 | Seattle, Washington
5. Nike | marketing@nike.com | +33656789012 | 200000 | Beaverton, Oregon
```

### Contrats
```
1. Apple Inc | 2024-01-01 à 2024-12-31 | 50000 | Sponsorship for football
2. Microsoft | 2024-03-15 à 2025-03-14 | 75000 | Equipment sponsorship
3. Google | 2024-06-01 à 2025-05-31 | 60000 | Digital marketing
4. Amazon | 2024-02-01 à 2024-11-30 | 90000 | Infrastructure
5. Nike | 2024-04-01 à 2026-03-31 | 150000 | Major sponsorship
```

---

**Fin du Guide de Test**


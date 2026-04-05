# 🔍 Guide Avancé - Recherches Avancées et Filtrages

## Vue d'Ensemble

Les services CRUD ont été enrichis avec des méthodes de recherche avancée pour permettre des recherches plus précises et flexibles.

---

## 🎯 Recherches Avancées pour les SPONSORS

### 1. Recherche par Nom Exact
```java
List<Sponsor> results = sponsorService.searchByName("Apple");
```
**Utilité:** Trouver tous les sponsors contenant "Apple" dans le nom
**Exemple de résultats:** Apple Inc, Apple Computers, etc.

### 2. Recherche par Budget Minimum
```java
List<Sponsor> results = sponsorService.searchByMinBudget(100000);
```
**Utilité:** Trouver tous les sponsors avec un budget >= 100 000
**Tri:** Par budget décroissant
**Exemple:** Trouver les gros sponsors

### 3. Recherche par Budget Maximum
```java
List<Sponsor> results = sponsorService.searchByMaxBudget(50000);
```
**Utilité:** Trouver tous les sponsors avec un budget <= 50 000
**Tri:** Par budget décroissant
**Exemple:** Trouver les petits sponsors

### 4. Recherche par Plage de Budget
```java
List<Sponsor> results = sponsorService.searchByBudgetRange(50000, 150000);
```
**Utilité:** Trouver tous les sponsors avec un budget entre 50 000 et 150 000
**Tri:** Par budget décroissant
**Exemple:** Trouver les sponsors de catégorie moyenne

---

## 💰 Recherches Avancées pour les CONTRATS

### 1. Recherche par Montant Minimum
```java
List<ContratSponsor> results = contratService.searchByMinMontant(50000);
```
**Utilité:** Trouver tous les contrats avec montant >= 50 000
**Tri:** Par montant décroissant
**Exemple:** Trouver les gros contrats

### 2. Recherche par Montant Maximum
```java
List<ContratSponsor> results = contratService.searchByMaxMontant(100000);
```
**Utilité:** Trouver tous les contrats avec montant <= 100 000
**Tri:** Par montant décroissant
**Exemple:** Trouver les contrats limités en budget

### 3. Recherche par Plage de Montant
```java
List<ContratSponsor> results = contratService.searchByMontantRange(50000, 150000);
```
**Utilité:** Trouver tous les contrats dans une plage de montant
**Tri:** Par montant décroissant
**Exemple:** Trouver les contrats de catégorie moyenne

### 4. Recherche par Statut
```java
List<ContratSponsor> results = contratService.searchByStatut("ACTIVE");
```
**Utilité:** Trouver tous les contrats avec un statut spécifique
**Statuts possibles:** ACTIVE, PENDING, COMPLETED, CANCELLED, PAUSED

### 5. Recherche par Statut de Paiement
```java
List<ContratSponsor> results = contratService.searchByStatutPaiement("PAID");
```
**Utilité:** Trouver tous les contrats avec un statut de paiement spécifique
**Statuts possibles:** PENDING, PARTIAL, PAID, OVERDUE

---

## 📋 Cas d'Usage Pratiques

### Cas 1: Trouver tous les sponsors avec un budget >= 100 000
```
// Code Java
List<Sponsor> grosSponsors = sponsorService.searchByMinBudget(100000);
for (Sponsor s : grosSponsors) {
    System.out.println(s.getNom() + " - Budget: " + s.getBudget());
}
```

### Cas 2: Rechercher les contrats impayés
```
// Code Java
List<ContratSponsor> contratsPending = contratService.searchByStatutPaiement("PENDING");
for (ContratSponsor c : contratsPending) {
    System.out.println("Montant: " + c.getMontant() + " - Statut: " + c.getStatutPaiement());
}
```

### Cas 3: Trouver les contrats actifs de valeur élevée
```
// Code Java
List<ContratSponsor> contrats = contratService.searchByStatut("ACTIVE");
List<ContratSponsor> resultat = new ArrayList<>();
for (ContratSponsor c : contrats) {
    if (c.getMontant() >= 100000) {
        resultat.add(c);
    }
}
```

### Cas 4: Rechercher des sponsors par nom et budget
```
// Code Java
List<Sponsor> apple = sponsorService.searchByName("Apple");
List<Sponsor> appleGros = new ArrayList<>();
for (Sponsor s : apple) {
    if (s.getBudget() >= 100000) {
        appleGros.add(s);
    }
}
```

---

## 🎮 Utilisation dans le Menu CLI

### Recherche de Sponsors par Nom
```
Menu Sponsor Management
Options:
1. Add Sponsor
2. View All Sponsors
3. View Sponsor by ID
4. Search Sponsor (générique)
5. Search Sponsor by Name (nouveau!)
6. Search Sponsor by Budget (nouveau!)
7. Update Sponsor
8. Delete Sponsor
0. Back to Main Menu
```

### Recherche de Contrats par Montant
```
Menu Contract Management
Options:
1. Add Contract
2. View All Contracts
3. View Contract by ID
4. Search Contract (générique)
5. Search Contract by Amount (nouveau!)
6. Search Contract by Status (nouveau!)
7. Update Contract
8. Delete Contract
9. View Contracts by Sponsor ID
0. Back to Main Menu
```

---

## 📊 Combinaisons de Recherches

### Exemple 1: Sponsors avec "Apple" dans le nom ET budget >= 100 000
```java
List<Sponsor> apple = sponsorService.searchByName("Apple");
List<Sponsor> result = new ArrayList<>();
for (Sponsor s : apple) {
    if (s.getBudget() >= 100000) {
        result.add(s);
    }
}
```

### Exemple 2: Contrats actifs de montant entre 50 000 et 150 000
```java
List<ContratSponsor> contrats = contratService.searchByStatut("ACTIVE");
List<ContratSponsor> result = contratService.searchByMontantRange(50000, 150000);
// Les contrats dans result sont filtrés par montant
// À combiner avec le statut ACTIVE manuellement
```

### Exemple 3: Contrats du sponsor ID 1 avec montant >= 50 000
```java
List<ContratSponsor> contratsSponsor = contratService.searchByMontantRange(50000, Double.MAX_VALUE);
List<ContratSponsor> result = new ArrayList<>();
for (ContratSponsor c : contratsSponsor) {
    if (c.getSponsorId() == 1) {
        result.add(c);
    }
}
```

---

## 🔧 Architecture des Recherches

### Service Layer
```
SponsorService
├── search(String keyword)           - Générique (nom/email/tel/adresse)
├── searchByName(String nom)         - Par nom exact
├── searchByMinBudget(double)        - Budget >= X
├── searchByMaxBudget(double)        - Budget <= X
└── searchByBudgetRange(min, max)    - Budget entre X et Y

ContratSponsorService
├── search(String keyword)           - Générique (description/statut/paiement)
├── searchBySponsorId(int)          - Par sponsor
├── searchByMinMontant(double)       - Montant >= X
├── searchByMaxMontant(double)       - Montant <= X
├── searchByMontantRange(min, max)   - Montant entre X et Y
├── searchByStatut(String)           - Par statut
└── searchByStatutPaiement(String)   - Par statut paiement
```

### Méthodes Utilitaires
```
SponsorService
└── mapResultSetToSponsor(ResultSet)   - Conversion BD -> Objet

ContratSponsorService
└── mapResultSetToContrat(ResultSet)   - Conversion BD -> Objet
```

---

## 📈 Performance des Recherches

### Optimisations Appliquées
✅ **Prepared Statements** - Requêtes pré-compilées
✅ **Indexes SQL** - Colonnes indexées (nom, budget, montant, statut)
✅ **Tri automatique** - Résultats triés par montant/budget DESC
✅ **Fermeture ressources** - Try-with-resources

### Exemple de Requête SQL
```sql
-- Recherche par nom avec LIKE
SELECT * FROM sponsor WHERE nom LIKE '%Apple%'

-- Recherche par budget min
SELECT * FROM sponsor WHERE budget >= 100000 ORDER BY budget DESC

-- Recherche par plage
SELECT * FROM sponsor WHERE budget BETWEEN 50000 AND 150000 ORDER BY budget DESC

-- Recherche par statut exact
SELECT * FROM contrat_sponsor WHERE statut = 'ACTIVE'
```

---

## ⚠️ Points d'Attention

### À Faire
✅ Utiliser searchByName() pour des recherches spécifiques
✅ Utiliser searchByBudgetRange() pour les plages
✅ Combiner les résultats pour des filtres multiples
✅ Vérifier les résultats vides

### À Éviter
❌ Ne pas confondre search() (générique) et searchByName() (spécifique)
❌ Montant et Budget peuvent être confondus (utiliser le bon service)
❌ Oublier de boucler sur les résultats pour des filtres multiples

---

## 🧪 Exemples de Flux de Travail

### Flux 1: Créer un contrat et le rechercher
```
1. Menu Contrats → Ajouter
2. Remplir: montant = 100 000, statut = ACTIVE
3. Menu Contrats → Recherche par montant
4. Entrer: min = 50 000, max = 150 000
5. Résultat: Contrat trouvé avec montant = 100 000
```

### Flux 2: Trouver un sponsor par budget
```
1. Menu Sponsors → Ajouter
2. Remplir: nom = Nike, budget = 200 000
3. Menu Sponsors → Recherche par budget
4. Entrer: min = 100 000
5. Résultat: Nike trouvé avec budget = 200 000
```

### Flux 3: Recherche combinée
```
1. Menu Sponsors → Recherche par nom
2. Entrer: "Nike"
3. Résultat: Liste des Nike
4. Manuellement filtrer ceux avec budget >= 100 000
5. Résultat final: Nike avec grand budget
```

---

## 📚 Méthodes Disponibles (Résumé)

| Méthode | Paramètre | Retour | Utilité |
|---------|-----------|--------|---------|
| **searchByName()** | nom:String | List<Sponsor> | Sponsors contenant le nom |
| **searchByMinBudget()** | min:double | List<Sponsor> | Sponsors avec budget >= min |
| **searchByMaxBudget()** | max:double | List<Sponsor> | Sponsors avec budget <= max |
| **searchByBudgetRange()** | min, max:double | List<Sponsor> | Sponsors avec budget entre min et max |
| **searchByMinMontant()** | min:double | List<Contrat> | Contrats avec montant >= min |
| **searchByMaxMontant()** | max:double | List<Contrat> | Contrats avec montant <= max |
| **searchByMontantRange()** | min, max:double | List<Contrat> | Contrats avec montant entre min et max |
| **searchByStatut()** | statut:String | List<Contrat> | Contrats avec ce statut |
| **searchByStatutPaiement()** | statut:String | List<Contrat> | Contrats avec ce statut paiement |

---

## 🎓 Pour Apprendre Plus

1. Consulter **SponsorService.java** pour voir l'implémentation
2. Consulter **ContratSponsorService.java** pour voir l'implémentation
3. Vérifier les méthodes utilitaires `mapResultSet*`
4. Tester les méthodes dans le menu CLI

---

**Version:** 1.0.0  
**Status:** ✅ Complètement implémenté


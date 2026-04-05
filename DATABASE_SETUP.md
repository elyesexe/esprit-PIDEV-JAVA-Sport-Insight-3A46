# Scripts SQL pour le CRUD Sponsor et Contrat Sponsor

## Préparation de la Base de Données

### 1. Vérifier/Créer la Base de Données
```sql
-- Créer la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS sport_insight;
USE sport_insight;
```

### 2. Table SPONSOR

#### Option A : Table minimale requise
```sql
CREATE TABLE IF NOT EXISTS sponsor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(20),
    budget DECIMAL(10, 2) NOT NULL DEFAULT 0,
    logo_name VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    adresse VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Option B : Table avec contraintes complètes
```sql
CREATE TABLE IF NOT EXISTS sponsor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(20),
    budget DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (budget >= 0),
    logo_name VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    adresse VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_nom (nom),
    INDEX idx_email (email),
    INDEX idx_telephone (telephone)
);
```

### 3. Table CONTRAT_SPONSOR

#### Option A : Table minimale requise
```sql
CREATE TABLE IF NOT EXISTS contrat_sponsor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    montant DECIMAL(10, 2) NOT NULL DEFAULT 0,
    description TEXT,
    statut VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notified BOOLEAN DEFAULT FALSE,
    statut_paiement VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sponsor_id INT NOT NULL,
    equipe_id INT,
    FOREIGN KEY (sponsor_id) REFERENCES sponsor(id) ON DELETE CASCADE
);
```

#### Option B : Table avec contraintes complètes et vérification de dates
```sql
CREATE TABLE IF NOT EXISTS contrat_sponsor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    montant DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (montant >= 0),
    description TEXT,
    statut VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notified BOOLEAN DEFAULT FALSE,
    statut_paiement VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sponsor_id INT NOT NULL,
    equipe_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (sponsor_id) REFERENCES sponsor(id) ON DELETE CASCADE,
    CONSTRAINT check_dates CHECK (date_fin >= date_debut),
    INDEX idx_sponsor_id (sponsor_id),
    INDEX idx_statut (statut),
    INDEX idx_date_debut (date_debut),
    INDEX idx_date_fin (date_fin)
);
```

---

## Insertion de Données de Test

### Insérer des Sponsors
```sql
INSERT INTO sponsor (nom, email, telephone, budget, logo_name, adresse) VALUES
('Apple Inc', 'contact@apple.com', '+33612345678', 100000.00, 'apple_logo.png', 'Cupertino, California'),
('Microsoft Corporation', 'info@microsoft.com', '+33687654321', 150000.00, 'microsoft_logo.png', 'Redmond, Washington'),
('Google LLC', 'support@google.com', '+33645321098', 120000.00, 'google_logo.png', 'Mountain View, California'),
('Amazon.com Inc', 'business@amazon.com', '+33611223344', 180000.00, 'amazon_logo.png', 'Seattle, Washington'),
('Nike Inc', 'marketing@nike.com', '+33656789012', 200000.00, 'nike_logo.png', 'Beaverton, Oregon');
```

### Insérer des Contrats Sponsor
```sql
INSERT INTO contrat_sponsor (date_debut, date_fin, montant, description, statut, statut_paiement, sponsor_id, equipe_id) VALUES
('2024-01-01', '2024-12-31', 50000.00, 'Sponsorship for football team', 'ACTIVE', 'PAID', 1, 5),
('2024-03-15', '2025-03-14', 75000.00, 'Equipment sponsorship', 'PENDING', 'PENDING', 2, 5),
('2024-06-01', '2025-05-31', 60000.00, 'Digital marketing campaign', 'ACTIVE', 'PARTIAL', 3, 6),
('2024-02-01', '2024-11-30', 90000.00, 'Infrastructure sponsorship', 'ACTIVE', 'PAID', 4, 7),
('2024-04-01', '2026-03-31', 150000.00, 'Major sponsorship agreement', 'ACTIVE', 'PENDING', 5, 8);
```

---

## Requêtes de Maintenance

### Voir tous les Sponsors
```sql
SELECT * FROM sponsor ORDER BY nom;
```

### Voir tous les Contrats avec infos Sponsor
```sql
SELECT 
    cs.id,
    cs.date_debut,
    cs.date_fin,
    cs.montant,
    cs.description,
    cs.statut,
    cs.statut_paiement,
    s.nom as sponsor_nom,
    s.email as sponsor_email
FROM contrat_sponsor cs
LEFT JOIN sponsor s ON cs.sponsor_id = s.id
ORDER BY cs.date_debut DESC;
```

### Contrats actifs d'un sponsor
```sql
SELECT cs.* FROM contrat_sponsor cs
WHERE cs.sponsor_id = 1 AND cs.statut = 'ACTIVE';
```

### Contrats non payés
```sql
SELECT * FROM contrat_sponsor 
WHERE statut_paiement != 'PAID' 
ORDER BY date_debut;
```

### Contrats expirés
```sql
SELECT * FROM contrat_sponsor 
WHERE date_fin < CURDATE() 
ORDER BY date_fin DESC;
```

### Budgets totaux par statut
```sql
SELECT 
    statut,
    COUNT(*) as nombre_contrats,
    SUM(montant) as montant_total
FROM contrat_sponsor
GROUP BY statut;
```

---

## Opérations de Suppression (À utiliser avec prudence)

### Supprimer tous les contrats d'un sponsor
```sql
DELETE FROM contrat_sponsor WHERE sponsor_id = 1;
```

### Supprimer un sponsor (les contrats seront supprimés en cascade)
```sql
DELETE FROM sponsor WHERE id = 1;
```

### Supprimer tous les contrats expirés
```sql
DELETE FROM contrat_sponsor WHERE date_fin < CURDATE();
```

---

## Modifications/Mises à Jour

### Mettre à jour le budget d'un sponsor
```sql
UPDATE sponsor SET budget = 250000.00 WHERE id = 1;
```

### Marquer un contrat comme payé
```sql
UPDATE contrat_sponsor SET statut_paiement = 'PAID' WHERE id = 1;
```

### Terminer un contrat
```sql
UPDATE contrat_sponsor SET statut = 'COMPLETED' WHERE id = 1;
```

### Réinitialiser le statut de notification
```sql
UPDATE contrat_sponsor SET notified = FALSE;
```

---

## Vérification de l'Intégrité de la Base de Données

### Vérifier les contraintes de clés étrangères
```sql
SELECT * FROM contrat_sponsor 
WHERE sponsor_id NOT IN (SELECT id FROM sponsor);
```

### Trouver les sponsors sans contrats
```sql
SELECT s.* FROM sponsor s
LEFT JOIN contrat_sponsor cs ON s.id = cs.sponsor_id
WHERE cs.id IS NULL;
```

### Compter les sponsors et contrats
```sql
SELECT 
    (SELECT COUNT(*) FROM sponsor) as total_sponsors,
    (SELECT COUNT(*) FROM contrat_sponsor) as total_contrats;
```

---

## Index et Performance

### Vérifier les index existants
```sql
SHOW INDEX FROM sponsor;
SHOW INDEX FROM contrat_sponsor;
```

### Ajouter des index supplémentaires si manquants
```sql
ALTER TABLE sponsor ADD INDEX idx_nom (nom);
ALTER TABLE sponsor ADD INDEX idx_email (email);
ALTER TABLE contrat_sponsor ADD INDEX idx_sponsor_id (sponsor_id);
ALTER TABLE contrat_sponsor ADD INDEX idx_statut (statut);
```

---

## Export et Sauvegarde

### Sauvegarder la table sponsor
```bash
mysqldump -u utilisateur -p sport_insight sponsor > sponsor_backup.sql
```

### Sauvegarder toute la base de données
```bash
mysqldump -u utilisateur -p sport_insight > sport_insight_backup.sql
```

### Restaurer une sauvegarde
```bash
mysql -u utilisateur -p sport_insight < sport_insight_backup.sql
```

---

## Statuts Courants

### Statuts de Contrat
- `ACTIVE` : Contrat actif
- `PENDING` : En attente
- `COMPLETED` : Terminé
- `CANCELLED` : Annulé
- `PAUSED` : En pause

### Statuts de Paiement
- `PENDING` : En attente de paiement
- `PARTIAL` : Paiement partiel
- `PAID` : Entièrement payé
- `OVERDUE` : En retard

---

**Notes importantes :**
1. Assurez-vous que la base de données `sport_insert` existe avant d'exécuter les requêtes
2. Utilisez `CASCADE` pour les clés étrangères pour supprimer automatiquement les contrats orphelins
3. Les recherches utilisent des `INDEX` pour améliorer les performances
4. Toujours faire une sauvegarde avant d'effectuer des suppressions en masse


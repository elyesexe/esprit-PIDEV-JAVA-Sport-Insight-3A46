-- Création de la base de données Sport Insight
-- SQL Script pour initialiser la base de données

-- Créer la base de données
CREATE DATABASE IF NOT EXISTS sport_insight;
USE sport_insight;

-- Table User (utilisateurs/entraîneurs)
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    mot_de_passe VARCHAR(255),
    role VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table Annonce
CREATE TABLE IF NOT EXISTS annonce (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    poste_recherche VARCHAR(100),
    niveau_requis VARCHAR(100),
    date_publication DATE NOT NULL,
    statut VARCHAR(20) DEFAULT 'ACTIVE',
    entraineur_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (entraineur_id) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table Commentaire
CREATE TABLE IF NOT EXISTS commentaire (
    id INT PRIMARY KEY AUTO_INCREMENT,
    contenu TEXT NOT NULL,
    date_commentaire DATE NOT NULL,
    joueur_id INT,
    annonce_id INT NOT NULL,
    auteur_anonyme VARCHAR(100),
    nb_likes INT DEFAULT 0,
    moderation_status VARCHAR(20) DEFAULT 'PENDING',
    moderation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (joueur_id) REFERENCES user(id) ON DELETE SET NULL,
    FOREIGN KEY (annonce_id) REFERENCES annonce(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Créer les index pour les performances
CREATE INDEX idx_annonce_titre ON annonce(titre);
CREATE INDEX idx_annonce_date ON annonce(date_publication);
CREATE INDEX idx_annonce_poste ON annonce(poste_recherche);
CREATE INDEX idx_annonce_statut ON annonce(statut);
CREATE INDEX idx_annonce_entraineur ON annonce(entraineur_id);
CREATE INDEX idx_commentaire_annonce ON commentaire(annonce_id);
CREATE INDEX idx_commentaire_joueur ON commentaire(joueur_id);
CREATE INDEX idx_commentaire_date ON commentaire(date_commentaire);
CREATE INDEX idx_commentaire_statut ON commentaire(moderation_status);

-- Données de test: Créer un utilisateur test (entraîneur)
INSERT INTO user (nom, prenom, email, role) VALUES
('Dupont', 'Jean', 'jean.dupont@example.com', 'COACH'),
('Martin', 'Pierre', 'pierre.martin@example.com', 'PLAYER'),
('Bernard', 'Luc', 'luc.bernard@example.com', 'ADMIN');

-- Données de test: Créer des annonces
INSERT INTO annonce (titre, description, poste_recherche, niveau_requis, date_publication, statut, entraineur_id) VALUES
('Recherche Gardien Expérimenté', 'Nous cherchons un gardien avec 5+ ans d\'expérience pour rejoindre notre équipe', 'Gardien', 'Professionnel', '2026-04-11', 'ACTIVE', 1),
('Attaquant talentueux requis', 'Cherchons un attaquant dynamique et créatif pour la saison 2026-2027', 'Attaquant', 'Intermédiaire', '2026-04-10', 'ACTIVE', 1),
('Défenseur central recherché', 'Nous recrutons un défenseur central expérimenté et fiable', 'Défenseur', 'Professionnel', '2026-04-09', 'CLOSED', 1);

-- Données de test: Créer des commentaires
INSERT INTO commentaire (contenu, date_commentaire, joueur_id, annonce_id, auteur_anonyme, nb_likes, moderation_status) VALUES
('Excellent profil, je recommande!', '2026-04-11', 2, 1, 'Expert Scout', 12, 'APPROVED'),
('Intéressé par cette position', '2026-04-10', 3, 1, 'Candidat', 5, 'PENDING'),
('Belle opportunité', '2026-04-09', 2, 2, 'Fan', 8, 'APPROVED');

-- Afficher les données
SELECT '=== UTILISATEURS ===' as '';
SELECT * FROM user;

SELECT '=== ANNONCES ===' as '';
SELECT * FROM annonce;

SELECT '=== COMMENTAIRES ===' as '';
SELECT * FROM commentaire;

-- Statistiques
SELECT '=== STATISTIQUES ===' as '';
SELECT
    (SELECT COUNT(*) FROM annonce) as 'Total Annonces',
    (SELECT COUNT(*) FROM annonce WHERE statut = 'ACTIVE') as 'Annonces Actives',
    (SELECT COUNT(*) FROM commentaire) as 'Total Commentaires',
    (SELECT COUNT(*) FROM user) as 'Total Utilisateurs';


CREATE DATABASE IF NOT EXISTS `sport_insight`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `sport_insight`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `contrat_sponsor`;
DROP TABLE IF EXISTS `commentaire`;
DROP TABLE IF EXISTS `annonce`;
DROP TABLE IF EXISTS `participation`;
DROP TABLE IF EXISTS `evaluation`;
DROP TABLE IF EXISTS `entrainement`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `match_follow_target`;
DROP TABLE IF EXISTS `matchs`;
DROP TABLE IF EXISTS `joueur`;
DROP TABLE IF EXISTS `sponsor`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `equipe`;

CREATE TABLE `equipe` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nom` VARCHAR(255) NULL,
    `coach` VARCHAR(255) NULL,
    `adresse` VARCHAR(255) NULL,
    `telephone` VARCHAR(50) NULL,
    `email` VARCHAR(180) NULL,
    `image` VARCHAR(255) NULL,
    `external_api_id` BIGINT NULL,
    `external_source` VARCHAR(32) NULL,
    `competition_code` VARCHAR(16) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_equipe_external_sync` (`external_source`, `external_api_id`),
    KEY `idx_equipe_competition_code` (`competition_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(180) NOT NULL,
    `roles` LONGTEXT NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `nom` VARCHAR(255) NOT NULL,
    `prenom` VARCHAR(255) NOT NULL,
    `telephone` VARCHAR(50) NULL,
    `date_naissance` DATE NULL,
    `photo` VARCHAR(255) NULL,
    `statut` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    `date_inscription` DATETIME NOT NULL,
    `cv_name` VARCHAR(255) NULL,
    `updated_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_email` (`email`),
    KEY `idx_user_status` (`statut`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sponsor` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nom` VARCHAR(255) NOT NULL,
    `email` VARCHAR(180) NULL,
    `telephone` VARCHAR(50) NULL,
    `budget` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `logo_name` VARCHAR(255) NULL,
    `updated_at` DATETIME NULL,
    `adresse` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `joueur` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nom` VARCHAR(255) NOT NULL,
    `prenom` VARCHAR(255) NOT NULL,
    `date_naissance` DATE NULL,
    `numero` INT NOT NULL,
    `image` VARCHAR(255) NULL,
    `equipe_id` INT NULL,
    `external_api_id` BIGINT NULL,
    `external_source` VARCHAR(32) NULL,
    `position` VARCHAR(120) NULL,
    `nationalite` VARCHAR(120) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_joueur_equipe_id` (`equipe_id`),
    KEY `idx_joueur_external_sync` (`external_source`, `external_api_id`),
    CONSTRAINT `fk_joueur_equipe`
        FOREIGN KEY (`equipe_id`) REFERENCES `equipe` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `matchs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `id_match` VARCHAR(100) NOT NULL,
    `date_match` DATE NOT NULL,
    `heure_debut` TIME NOT NULL,
    `lieu` VARCHAR(255) NULL,
    `type` VARCHAR(100) NULL,
    `statut` VARCHAR(100) NULL,
    `lineup_domicile` LONGTEXT NULL,
    `lineup_exterieur` LONGTEXT NULL,
    `score_equipe_domicile` INT NULL,
    `score_equipe_exterieur` INT NULL,
    `equipe_domicile_id` INT NULL,
    `equipe_exterieur_id` INT NULL,
    `external_api_id` BIGINT NULL,
    `external_source` VARCHAR(32) NULL,
    `competition_code` VARCHAR(16) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_matchs_external_sync` (`external_source`, `external_api_id`),
    KEY `idx_matchs_competition_code` (`competition_code`),
    KEY `idx_matchs_equipe_domicile_id` (`equipe_domicile_id`),
    KEY `idx_matchs_equipe_exterieur_id` (`equipe_exterieur_id`),
    CONSTRAINT `fk_matchs_equipe_domicile`
        FOREIGN KEY (`equipe_domicile_id`) REFERENCES `equipe` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_matchs_equipe_exterieur`
        FOREIGN KEY (`equipe_exterieur_id`) REFERENCES `equipe` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `category` VARCHAR(120) NULL,
    `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `stock` INT NOT NULL DEFAULT 0,
    `size` VARCHAR(50) NULL,
    `brand` VARCHAR(120) NULL,
    `image` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `quantity` INT NOT NULL,
    `order_date` DATE NOT NULL,
    `status` VARCHAR(100) NULL,
    `payment_method` VARCHAR(100) NULL,
    `payment_status` VARCHAR(100) NULL,
    `size` VARCHAR(50) NULL,
    `contact_email` VARCHAR(180) NULL,
    `contact_phone` VARCHAR(50) NULL,
    `shipping_address` VARCHAR(255) NULL,
    `billing_address` VARCHAR(255) NULL,
    `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `product_id` INT NOT NULL,
    `entraineur_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order_product_id` (`product_id`),
    KEY `idx_order_entraineur_id` (`entraineur_id`),
    CONSTRAINT `fk_order_product`
        FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_order_entraineur`
        FOREIGN KEY (`entraineur_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `entrainement` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `date_entrainement` DATE NOT NULL,
    `heure_debut` TIME NOT NULL,
    `heure_fin` TIME NOT NULL,
    `type` VARCHAR(120) NOT NULL,
    `objectif` VARCHAR(255) NULL,
    `lieu` VARCHAR(255) NULL,
    `entraineur_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_entrainement_entraineur_id` (`entraineur_id`),
    CONSTRAINT `fk_entrainement_entraineur`
        FOREIGN KEY (`entraineur_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `evaluation` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `note_physique` DOUBLE NOT NULL,
    `note_technique` DOUBLE NOT NULL,
    `note_tactique` DOUBLE NOT NULL,
    `commentaire` TEXT NULL,
    `entrainement_id` INT NOT NULL,
    `joueur_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_evaluation_entrainement_id` (`entrainement_id`),
    KEY `idx_evaluation_joueur_id` (`joueur_id`),
    CONSTRAINT `fk_evaluation_entrainement`
        FOREIGN KEY (`entrainement_id`) REFERENCES `entrainement` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_evaluation_joueur`
        FOREIGN KEY (`joueur_id`) REFERENCES `joueur` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `participation` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `presence` VARCHAR(50) NOT NULL,
    `justification_absence` TEXT NULL,
    `entrainement_id` INT NOT NULL,
    `joueur_id` INT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_participation_entrainement_id` (`entrainement_id`),
    KEY `idx_participation_joueur_id` (`joueur_id`),
    CONSTRAINT `fk_participation_entrainement`
        FOREIGN KEY (`entrainement_id`) REFERENCES `entrainement` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_participation_joueur`
        FOREIGN KEY (`joueur_id`) REFERENCES `joueur` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `annonce` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `titre` VARCHAR(255) NOT NULL,
    `description` TEXT NULL,
    `poste_recherche` VARCHAR(100) NULL,
    `niveau_requis` VARCHAR(100) NULL,
    `date_publication` DATE NOT NULL,
    `statut` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `entraineur_id` INT NULL,
    `comments_enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `urgent` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_annonce_titre` (`titre`),
    KEY `idx_annonce_date_publication` (`date_publication`),
    KEY `idx_annonce_poste_recherche` (`poste_recherche`),
    KEY `idx_annonce_statut` (`statut`),
    KEY `idx_annonce_urgent` (`urgent`),
    KEY `idx_annonce_entraineur_id` (`entraineur_id`),
    CONSTRAINT `fk_annonce_entraineur`
        FOREIGN KEY (`entraineur_id`) REFERENCES `user` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `commentaire` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `contenu` TEXT NOT NULL,
    `date_commentaire` DATE NOT NULL,
    `joueur_id` INT NULL,
    `annonce_id` INT NULL,
    `auteur_anonyme` VARCHAR(100) NULL,
    `nb_likes` INT NOT NULL DEFAULT 0,
    `moderation_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    `moderation_reason` TEXT NULL,
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_commentaire_annonce_id` (`annonce_id`),
    KEY `idx_commentaire_joueur_id` (`joueur_id`),
    KEY `idx_commentaire_date_commentaire` (`date_commentaire`),
    KEY `idx_commentaire_moderation_status` (`moderation_status`),
    CONSTRAINT `fk_commentaire_annonce`
        FOREIGN KEY (`annonce_id`) REFERENCES `annonce` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `contrat_sponsor` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `date_debut` DATE NOT NULL,
    `date_fin` DATE NOT NULL,
    `montant` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `description` TEXT NULL,
    `statut` VARCHAR(100) NULL,
    `notified` BOOLEAN NOT NULL DEFAULT FALSE,
    `statut_paiement` VARCHAR(100) NULL,
    `sponsor_id` INT NOT NULL,
    `equipe_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_contrat_sponsor_sponsor_id` (`sponsor_id`),
    KEY `idx_contrat_sponsor_equipe_id` (`equipe_id`),
    CONSTRAINT `fk_contrat_sponsor_sponsor`
        FOREIGN KEY (`sponsor_id`) REFERENCES `sponsor` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_contrat_sponsor_equipe`
        FOREIGN KEY (`equipe_id`) REFERENCES `equipe` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `match_follow_target` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `target_type` VARCHAR(16) NOT NULL,
    `team_id` INT NULL,
    `match_id` INT NULL,
    `competition_code` VARCHAR(16) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_match_follow_target_user` (`user_id`),
    KEY `idx_match_follow_target_team` (`team_id`),
    KEY `idx_match_follow_target_match` (`match_id`),
    KEY `idx_match_follow_target_competition` (`competition_code`),
    UNIQUE KEY `uq_match_follow_target_team` (`user_id`, `target_type`, `team_id`),
    UNIQUE KEY `uq_match_follow_target_match` (`user_id`, `target_type`, `match_id`),
    UNIQUE KEY `uq_match_follow_target_competition` (`user_id`, `target_type`, `competition_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NULL,
    `message` TEXT NOT NULL,
    `type` VARCHAR(32) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `user_id` INT NOT NULL,
    `match_id` INT NULL,
    `dedupe_key` VARCHAR(255) NULL,
    `competition_code` VARCHAR(16) NULL,
    `home_team_name` VARCHAR(255) NULL,
    `away_team_name` VARCHAR(255) NULL,
    `home_team_logo` VARCHAR(255) NULL,
    `away_team_logo` VARCHAR(255) NULL,
    `actor_name` VARCHAR(255) NULL,
    `minute_label` VARCHAR(32) NULL,
    `accent_tone` VARCHAR(32) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_created` (`user_id`, `created_at`),
    KEY `idx_notification_match` (`match_id`),
    UNIQUE KEY `uq_notification_dedupe` (`user_id`, `dedupe_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

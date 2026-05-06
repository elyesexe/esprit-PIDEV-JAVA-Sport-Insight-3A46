CREATE DATABASE IF NOT EXISTS `sport_insight`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `sport_insight`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `contrat_sponsor`;
<<<<<<< HEAD
DROP TABLE IF EXISTS `comment_favorite`;
DROP TABLE IF EXISTS `comment_reaction`;
=======
DROP TABLE IF EXISTS `match_lineup`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `messenger_message`;
DROP TABLE IF EXISTS `entrainement_user`;
DROP TABLE IF EXISTS `doctrine_migration_version`;
DROP TABLE IF EXISTS `daily_nutrition_summary`;
DROP TABLE IF EXISTS `food_log`;
DROP TABLE IF EXISTS `ai_checklist_progress`;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
DROP TABLE IF EXISTS `commentaire`;
DROP TABLE IF EXISTS `annonce`;
DROP TABLE IF EXISTS `participation`;
DROP TABLE IF EXISTS `evaluation`;
DROP TABLE IF EXISTS `entrainement`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `product`;
<<<<<<< HEAD
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `match_follow_target`;
DROP TABLE IF EXISTS `matchs`;
DROP TABLE IF EXISTS `joueur`;
DROP TABLE IF EXISTS `sponsor`;
DROP TABLE IF EXISTS `face_profile`;
=======
DROP TABLE IF EXISTS `matchs`;
DROP TABLE IF EXISTS `joueur`;
DROP TABLE IF EXISTS `sponsor`;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
    `face_registered` TINYINT(1) NOT NULL DEFAULT 0,
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    PRIMARY KEY (`id`),
    KEY `idx_user_email` (`email`),
    KEY `idx_user_status` (`statut`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

<<<<<<< HEAD
CREATE TABLE `password_reset_token` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `token` VARCHAR(16) NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `used` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_password_reset_user` (`user_id`),
    KEY `idx_password_reset_token` (`token`),
    CONSTRAINT `fk_password_reset_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `face_profile` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `embedding_json` LONGTEXT NOT NULL,
    `model_name` VARCHAR(80) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_face_profile_user` (`user_id`),
    CONSTRAINT `fk_face_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
    `description` TEXT NULL,
    `tags` VARCHAR(255) NULL,
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `quantity` INT NOT NULL,
    `order_date` DATE NOT NULL,
<<<<<<< HEAD
    `client_name` VARCHAR(120) NULL,
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
    `entraineur_id` INT NULL,
=======
    `entraineur_id` INT NOT NULL,
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        FOREIGN KEY (`joueur_id`) REFERENCES `user` (`id`)
=======
        FOREIGN KEY (`joueur_id`) REFERENCES `joueur` (`id`)
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        FOREIGN KEY (`joueur_id`) REFERENCES `user` (`id`)
=======
        FOREIGN KEY (`joueur_id`) REFERENCES `joueur` (`id`)
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
    `cv_name` VARCHAR(255) NULL,
    `cv_title` VARCHAR(255) NULL,
    `nb_likes` INT NOT NULL DEFAULT 0,
    `nb_dislikes` INT NOT NULL DEFAULT 0,
    `moderation_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    `moderation_reason` TEXT NULL,
    `author_user_id` INT NULL,
    `author_role` VARCHAR(32) NULL,
=======
    `nb_likes` INT NOT NULL DEFAULT 0,
    `moderation_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    `moderation_reason` TEXT NULL,
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_commentaire_annonce_id` (`annonce_id`),
    KEY `idx_commentaire_joueur_id` (`joueur_id`),
<<<<<<< HEAD
    KEY `idx_commentaire_author_user_id` (`author_user_id`),
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    KEY `idx_commentaire_date_commentaire` (`date_commentaire`),
    KEY `idx_commentaire_moderation_status` (`moderation_status`),
    CONSTRAINT `fk_commentaire_annonce`
        FOREIGN KEY (`annonce_id`) REFERENCES `annonce` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

<<<<<<< HEAD
CREATE TABLE `comment_reaction` (
    `user_id` INT NOT NULL,
    `commentaire_id` INT NOT NULL,
    `reaction_type` VARCHAR(16) NOT NULL,
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `commentaire_id`),
    KEY `idx_comment_reaction_commentaire` (`commentaire_id`),
    KEY `idx_comment_reaction_type` (`reaction_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comment_favorite` (
    `user_id` INT NOT NULL,
    `commentaire_id` INT NOT NULL,
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `commentaire_id`),
    KEY `idx_comment_favorite_commentaire` (`commentaire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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

<<<<<<< HEAD
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
=======
CREATE TABLE `ai_checklist_progress` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `plan_type` VARCHAR(50) NOT NULL,
    `plan_category` VARCHAR(50) NOT NULL,
    `item_text` VARCHAR(500) NOT NULL,
    `is_completed` BOOLEAN NOT NULL DEFAULT FALSE,
    `completed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_checklist_user_id` (`user_id`),
    KEY `idx_checklist_plan_type` (`plan_type`),
    KEY `idx_checklist_plan_category` (`plan_category`),
    CONSTRAINT `fk_checklist_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `food_log` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `log_date` DATE NOT NULL,
    `meal_type` VARCHAR(50) NOT NULL,
    `food_description` TEXT NOT NULL,
    `calories` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `protein_g` DECIMAL(8,2) NULL,
    `carbs_g` DECIMAL(8,2) NULL,
    `fat_g` DECIMAL(8,2) NULL,
    `fiber_g` DECIMAL(8,2) NULL,
    `api_response` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_food_log_user_id` (`user_id`),
    KEY `idx_food_log_date` (`log_date`),
    KEY `idx_food_log_meal_type` (`meal_type`),
    CONSTRAINT `fk_food_log_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `daily_nutrition_summary` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `summary_date` DATE NOT NULL,
    `total_calories` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_protein_g` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_carbs_g` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_fat_g` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_fiber_g` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `target_calories` DECIMAL(8,2) NULL,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_user_date` (`user_id`, `summary_date`),
    KEY `idx_summary_user_id` (`user_id`),
    KEY `idx_summary_date` (`summary_date`),
    CONSTRAINT `fk_summary_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `doctrine_migration_version` (
    `version` VARCHAR(191) NOT NULL,
    `executed_at` DATETIME NULL,
    `execution_time` INT NULL,
    PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `entrainement_user` (
    `entrainement_id` INT NOT NULL,
    `user_id` INT NOT NULL,
    PRIMARY KEY (`entrainement_id`, `user_id`),
    KEY `idx_entrainement_user_entrainement_id` (`entrainement_id`),
    KEY `idx_entrainement_user_user_id` (`user_id`),
    CONSTRAINT `fk_entrainement_user_entrainement`
        FOREIGN KEY (`entrainement_id`) REFERENCES `entrainement` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_entrainement_user_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order_item` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `quantity` INT NOT NULL,
    `unit_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `product_id` INT NOT NULL,
    `order_ref_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order_item_product_id` (`product_id`),
    KEY `idx_order_item_order_ref_id` (`order_ref_id`),
    CONSTRAINT `fk_order_item_product`
        FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_order_item_order`
        FOREIGN KEY (`order_ref_id`) REFERENCES `order` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification` (
    `id` INT NOT NULL AUTO_INCREMENT,
<<<<<<< HEAD
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
    `actor_image` VARCHAR(255) NULL,
    `minute_label` VARCHAR(32) NULL,
    `accent_tone` VARCHAR(32) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_created` (`user_id`, `created_at`),
    KEY `idx_notification_match` (`match_id`),
    UNIQUE KEY `uq_notification_dedupe` (`user_id`, `dedupe_key`)
=======
    `message` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `user_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_id` (`user_id`),
    KEY `idx_notification_is_read` (`is_read`),
    CONSTRAINT `fk_notification_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `message` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `sender_id` INT NOT NULL,
    `receiver_id` INT NOT NULL,
    `content` TEXT NOT NULL,
    `sent_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (`id`),
    KEY `idx_message_sender_id` (`sender_id`),
    KEY `idx_message_receiver_id` (`receiver_id`),
    KEY `idx_message_is_read` (`is_read`),
    CONSTRAINT `fk_message_sender`
        FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_message_receiver`
        FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_message` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `auteur_id` INT NOT NULL,
    `destinataire_id` INT NOT NULL,
    `annonce_id` INT NULL,
    `message` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `notification_sent` BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (`id`),
    KEY `idx_chat_message_auteur_id` (`auteur_id`),
    KEY `idx_chat_message_destinataire_id` (`destinataire_id`),
    KEY `idx_chat_message_annonce_id` (`annonce_id`),
    CONSTRAINT `fk_chat_message_auteur`
        FOREIGN KEY (`auteur_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_chat_message_destinataire`
        FOREIGN KEY (`destinataire_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_chat_message_annonce`
        FOREIGN KEY (`annonce_id`) REFERENCES `annonce` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `messenger_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `body` LONGTEXT NOT NULL,
    `headers` LONGTEXT NULL,
    `queue_name` VARCHAR(255) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `available_at` DATETIME NULL,
    `delivered_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    KEY `idx_messenger_queue_name` (`queue_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `match_lineup` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `type` VARCHAR(50) NOT NULL,
    `buts` INT NULL,
    `cartons_jaunes` INT NULL,
    `cartons_rouges` INT NULL,
    `position_x` DOUBLE NULL,
    `position_y` DOUBLE NULL,
    `matchs_id` INT NOT NULL,
    `joueur_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_match_lineup_matchs_id` (`matchs_id`),
    KEY `idx_match_lineup_joueur_id` (`joueur_id`),
    CONSTRAINT `fk_match_lineup_matchs`
        FOREIGN KEY (`matchs_id`) REFERENCES `matchs` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_match_lineup_joueur`
        FOREIGN KEY (`joueur_id`) REFERENCES `joueur` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

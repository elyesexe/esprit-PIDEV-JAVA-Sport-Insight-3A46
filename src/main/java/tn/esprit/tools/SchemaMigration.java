package tn.esprit.tools;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaMigration {
    private SchemaMigration() {
    }

    public static void ensureFootballDataColumns(Connection connection) throws SQLException {
<<<<<<< HEAD
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            addColumnIfMissing(metaData, catalog, statement, "product", "description", "TEXT NULL");
            addColumnIfMissing(metaData, catalog, statement, "product", "tags", "VARCHAR(255) NULL");
=======
        ensureFullSchema(connection);
    }

    public static void ensureFullSchema(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            ensureBaseSchema(statement);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

            addColumnIfMissing(metaData, catalog, statement, "equipe", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, catalog, statement, "equipe", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, catalog, statement, "equipe", "competition_code", "VARCHAR(16) NULL");
<<<<<<< HEAD
            addColumnIfMissing(metaData, catalog, statement, "equipe", "api_football_id", "BIGINT NULL");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

            addColumnIfMissing(metaData, catalog, statement, "joueur", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, catalog, statement, "joueur", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, catalog, statement, "joueur", "position", "VARCHAR(120) NULL");
            addColumnIfMissing(metaData, catalog, statement, "joueur", "nationalite", "VARCHAR(120) NULL");

            addColumnIfMissing(metaData, catalog, statement, "matchs", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "competition_code", "VARCHAR(16) NULL");
<<<<<<< HEAD
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_id", "BIGINT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_stats_json", "LONGTEXT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_lineup_json", "LONGTEXT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_incidents_json", "LONGTEXT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_synced_at", "DATETIME NULL");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

            addIndexIfMissing(metaData, catalog, statement, "equipe", "idx_equipe_external_sync",
                    "CREATE INDEX idx_equipe_external_sync ON equipe (external_source, external_api_id)");
            addIndexIfMissing(metaData, catalog, statement, "equipe", "idx_equipe_competition_code",
                    "CREATE INDEX idx_equipe_competition_code ON equipe (competition_code)");
<<<<<<< HEAD
            addIndexIfMissing(metaData, catalog, statement, "equipe", "idx_equipe_api_football_id",
                    "CREATE INDEX idx_equipe_api_football_id ON equipe (api_football_id)");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            addIndexIfMissing(metaData, catalog, statement, "joueur", "idx_joueur_external_sync",
                    "CREATE INDEX idx_joueur_external_sync ON joueur (external_source, external_api_id)");
            addIndexIfMissing(metaData, catalog, statement, "matchs", "idx_matchs_external_sync",
                    "CREATE INDEX idx_matchs_external_sync ON matchs (external_source, external_api_id)");
            addIndexIfMissing(metaData, catalog, statement, "matchs", "idx_matchs_competition_code",
                    "CREATE INDEX idx_matchs_competition_code ON matchs (competition_code)");
<<<<<<< HEAD
            addIndexIfMissing(metaData, catalog, statement, "matchs", "idx_matchs_api_football_id",
                    "CREATE INDEX idx_matchs_api_football_id ON matchs (api_football_id)");
=======
            addIndexIfMissing(metaData, catalog, statement, "order", "idx_order_product_id",
                    "CREATE INDEX idx_order_product_id ON `order` (product_id)");
            addIndexIfMissing(metaData, catalog, statement, "order", "idx_order_entraineur_id",
                    "CREATE INDEX idx_order_entraineur_id ON `order` (entraineur_id)");
            addIndexIfMissing(metaData, catalog, statement, "entrainement", "idx_entrainement_entraineur_id",
                    "CREATE INDEX idx_entrainement_entraineur_id ON entrainement (entraineur_id)");
            addIndexIfMissing(metaData, catalog, statement, "evaluation", "idx_evaluation_entrainement_id",
                    "CREATE INDEX idx_evaluation_entrainement_id ON evaluation (entrainement_id)");
            addIndexIfMissing(metaData, catalog, statement, "evaluation", "idx_evaluation_joueur_id",
                    "CREATE INDEX idx_evaluation_joueur_id ON evaluation (joueur_id)");
            addIndexIfMissing(metaData, catalog, statement, "participation", "idx_participation_entrainement_id",
                    "CREATE INDEX idx_participation_entrainement_id ON participation (entrainement_id)");
            addIndexIfMissing(metaData, catalog, statement, "participation", "idx_participation_joueur_id",
                    "CREATE INDEX idx_participation_joueur_id ON participation (joueur_id)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_titre",
                    "CREATE INDEX idx_annonce_titre ON annonce (titre)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_date_publication",
                    "CREATE INDEX idx_annonce_date_publication ON annonce (date_publication)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_poste_recherche",
                    "CREATE INDEX idx_annonce_poste_recherche ON annonce (poste_recherche)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_statut",
                    "CREATE INDEX idx_annonce_statut ON annonce (statut)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_urgent",
                    "CREATE INDEX idx_annonce_urgent ON annonce (urgent)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_annonce_id",
                    "CREATE INDEX idx_commentaire_annonce_id ON commentaire (annonce_id)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_joueur_id",
                    "CREATE INDEX idx_commentaire_joueur_id ON commentaire (joueur_id)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_date_commentaire",
                    "CREATE INDEX idx_commentaire_date_commentaire ON commentaire (date_commentaire)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_moderation_status",
                    "CREATE INDEX idx_commentaire_moderation_status ON commentaire (moderation_status)");
            addIndexIfMissing(metaData, catalog, statement, "contrat_sponsor", "idx_contrat_sponsor_sponsor_id",
                    "CREATE INDEX idx_contrat_sponsor_sponsor_id ON contrat_sponsor (sponsor_id)");
            addIndexIfMissing(metaData, catalog, statement, "contrat_sponsor", "idx_contrat_sponsor_equipe_id",
                    "CREATE INDEX idx_contrat_sponsor_equipe_id ON contrat_sponsor (equipe_id)");
            addIndexIfMissing(metaData, catalog, statement, "ai_checklist_progress", "idx_checklist_user_id",
                    "CREATE INDEX idx_checklist_user_id ON ai_checklist_progress (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "ai_checklist_progress", "idx_checklist_plan_type",
                    "CREATE INDEX idx_checklist_plan_type ON ai_checklist_progress (plan_type)");
            addIndexIfMissing(metaData, catalog, statement, "ai_checklist_progress", "idx_checklist_plan_category",
                    "CREATE INDEX idx_checklist_plan_category ON ai_checklist_progress (plan_category)");
            addIndexIfMissing(metaData, catalog, statement, "food_log", "idx_food_log_user_id",
                    "CREATE INDEX idx_food_log_user_id ON food_log (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "food_log", "idx_food_log_date",
                    "CREATE INDEX idx_food_log_date ON food_log (log_date)");
            addIndexIfMissing(metaData, catalog, statement, "food_log", "idx_food_log_meal_type",
                    "CREATE INDEX idx_food_log_meal_type ON food_log (meal_type)");
            addIndexIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "idx_summary_user_id",
                    "CREATE INDEX idx_summary_user_id ON daily_nutrition_summary (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "idx_summary_date",
                    "CREATE INDEX idx_summary_date ON daily_nutrition_summary (summary_date)");

            addForeignKeyIfMissing(metaData, catalog, statement, "joueur", "fk_joueur_equipe",
                    "ALTER TABLE `joueur` ADD CONSTRAINT `fk_joueur_equipe` FOREIGN KEY (`equipe_id`) REFERENCES `equipe` (`id`) ON DELETE SET NULL ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "matchs", "fk_matchs_equipe_domicile",
                    "ALTER TABLE `matchs` ADD CONSTRAINT `fk_matchs_equipe_domicile` FOREIGN KEY (`equipe_domicile_id`) REFERENCES `equipe` (`id`) ON DELETE SET NULL ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "matchs", "fk_matchs_equipe_exterieur",
                    "ALTER TABLE `matchs` ADD CONSTRAINT `fk_matchs_equipe_exterieur` FOREIGN KEY (`equipe_exterieur_id`) REFERENCES `equipe` (`id`) ON DELETE SET NULL ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "order", "fk_order_product",
                    "ALTER TABLE `order` ADD CONSTRAINT `fk_order_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "order", "fk_order_entraineur",
                    "ALTER TABLE `order` ADD CONSTRAINT `fk_order_entraineur` FOREIGN KEY (`entraineur_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "entrainement", "fk_entrainement_entraineur",
                    "ALTER TABLE `entrainement` ADD CONSTRAINT `fk_entrainement_entraineur` FOREIGN KEY (`entraineur_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "evaluation", "fk_evaluation_entrainement",
                    "ALTER TABLE `evaluation` ADD CONSTRAINT `fk_evaluation_entrainement` FOREIGN KEY (`entrainement_id`) REFERENCES `entrainement` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "evaluation", "fk_evaluation_joueur",
                    "ALTER TABLE `evaluation` ADD CONSTRAINT `fk_evaluation_joueur` FOREIGN KEY (`joueur_id`) REFERENCES `joueur` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "participation", "fk_participation_entrainement",
                    "ALTER TABLE `participation` ADD CONSTRAINT `fk_participation_entrainement` FOREIGN KEY (`entrainement_id`) REFERENCES `entrainement` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "participation", "fk_participation_joueur",
                    "ALTER TABLE `participation` ADD CONSTRAINT `fk_participation_joueur` FOREIGN KEY (`joueur_id`) REFERENCES `joueur` (`id`) ON DELETE SET NULL ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "annonce", "fk_annonce_entraineur",
                    "ALTER TABLE `annonce` ADD CONSTRAINT `fk_annonce_entraineur` FOREIGN KEY (`entraineur_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "commentaire", "fk_commentaire_annonce",
                    "ALTER TABLE `commentaire` ADD CONSTRAINT `fk_commentaire_annonce` FOREIGN KEY (`annonce_id`) REFERENCES `annonce` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "contrat_sponsor", "fk_contrat_sponsor_sponsor",
                    "ALTER TABLE `contrat_sponsor` ADD CONSTRAINT `fk_contrat_sponsor_sponsor` FOREIGN KEY (`sponsor_id`) REFERENCES `sponsor` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "contrat_sponsor", "fk_contrat_sponsor_equipe",
                    "ALTER TABLE `contrat_sponsor` ADD CONSTRAINT `fk_contrat_sponsor_equipe` FOREIGN KEY (`equipe_id`) REFERENCES `equipe` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "ai_checklist_progress", "fk_checklist_user",
                    "ALTER TABLE `ai_checklist_progress` ADD CONSTRAINT `fk_checklist_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "food_log", "fk_food_log_user",
                    "ALTER TABLE `food_log` ADD CONSTRAINT `fk_food_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
            addForeignKeyIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "fk_summary_user",
                    "ALTER TABLE `daily_nutrition_summary` ADD CONSTRAINT `fk_summary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE");
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

            try {
                backfillEquipeCompetitionCodes(statement);
            } catch (SQLException e) {
                System.err.println("Team competition backfill skipped: " + e.getMessage());
            }
        }
<<<<<<< HEAD

        ensureAnnonceSchema(connection);
        ensureUserSchema(connection);
        ensureSponsorSchema(connection);
        ensureFaceIdSchema(connection);
        ensureOrderSchema(connection);
        ensureMatchLiveSchema(connection);
        ensureTrainingUserLinks(connection);
        ensureNutritionSchema(connection);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private static void backfillEquipeCompetitionCodes(Statement statement) throws SQLException {
        statement.executeUpdate("""
                UPDATE equipe e
                JOIN (
                    SELECT counts.team_id,
                           SUBSTRING_INDEX(
                               GROUP_CONCAT(
                                   counts.competition_code
                                   ORDER BY counts.match_count DESC, counts.priority ASC
                                   SEPARATOR ','
                               ),
                               ',',
                               1
                           ) AS dominant_competition_code
                    FROM (
                        SELECT appearances.team_id,
                               appearances.competition_code,
                               COUNT(*) AS match_count,
                               CASE appearances.competition_code
                                   WHEN 'PL' THEN 1
                                   WHEN 'PD' THEN 2
                                   WHEN 'BL1' THEN 3
                                   WHEN 'SA' THEN 4
                                   WHEN 'FL1' THEN 5
                                   ELSE 99
                               END AS priority
                        FROM (
                            SELECT equipe_domicile_id AS team_id, competition_code
                            FROM matchs
                            UNION ALL
                            SELECT equipe_exterieur_id AS team_id, competition_code
                            FROM matchs
                        ) appearances
                        WHERE appearances.team_id IS NOT NULL
                          AND appearances.competition_code IN ('PL', 'PD', 'BL1', 'SA', 'FL1')
                        GROUP BY appearances.team_id, appearances.competition_code
                    ) counts
                    GROUP BY counts.team_id
                ) dominant ON dominant.team_id = e.id
                SET e.competition_code = dominant.dominant_competition_code
                WHERE (e.competition_code IS NULL OR e.competition_code = '' OR e.competition_code = 'CL')
                  AND dominant.dominant_competition_code IS NOT NULL
                """);
    }

<<<<<<< HEAD
=======
    private static void ensureBaseSchema(Statement statement) throws SQLException {
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `equipe` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `user` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `sponsor` (
                    `id` INT NOT NULL AUTO_INCREMENT,
                    `nom` VARCHAR(255) NOT NULL,
                    `email` VARCHAR(180) NULL,
                    `telephone` VARCHAR(50) NULL,
                    `budget` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                    `logo_name` VARCHAR(255) NULL,
                    `updated_at` DATETIME NULL,
                    `adresse` VARCHAR(255) NULL,
                    PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `joueur` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `matchs` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `product` (
                    `id` INT NOT NULL AUTO_INCREMENT,
                    `name` VARCHAR(255) NOT NULL,
                    `category` VARCHAR(120) NULL,
                    `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                    `stock` INT NOT NULL DEFAULT 0,
                    `size` VARCHAR(50) NULL,
                    `brand` VARCHAR(120) NULL,
                    `image` VARCHAR(255) NULL,
                    PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `order` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `entrainement` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `evaluation` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `participation` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `annonce` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `commentaire` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `contrat_sponsor` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ai_checklist_progress` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `food_log` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `daily_nutrition_summary` (
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static void addColumnIfMissing(
            DatabaseMetaData metaData,
            String catalog,
            Statement statement,
            String tableName,
            String columnName,
            String definition
    ) throws SQLException {
        if (columnExists(metaData, catalog, tableName, columnName)) {
            return;
        }

        statement.executeUpdate("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition);
    }

<<<<<<< HEAD
=======
    private static void addForeignKeyIfMissing(
            DatabaseMetaData metaData,
            String catalog,
            Statement statement,
            String tableName,
            String foreignKeyName,
            String sql
    ) throws SQLException {
        if (foreignKeyExists(metaData, catalog, tableName, foreignKeyName)) {
            return;
        }
        statement.executeUpdate(sql);
    }

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static boolean columnExists(DatabaseMetaData metaData, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName, columnName)) {
            while (resultSet.next()) {
                String discovered = resultSet.getString("COLUMN_NAME");
                if (columnName.equalsIgnoreCase(discovered)) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            while (resultSet.next()) {
                String discovered = resultSet.getString("COLUMN_NAME");
                if (columnName.equalsIgnoreCase(discovered)) {
                    return true;
                }
            }
        }
        return false;
    }

<<<<<<< HEAD
=======
    private static boolean foreignKeyExists(
            DatabaseMetaData metaData,
            String catalog,
            String tableName,
            String foreignKeyName
    ) throws SQLException {
        try (ResultSet resultSet = metaData.getImportedKeys(catalog, null, tableName)) {
            while (resultSet.next()) {
                String discovered = resultSet.getString("FK_NAME");
                if (foreignKeyName.equalsIgnoreCase(discovered)) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metaData.getImportedKeys(null, null, tableName)) {
            while (resultSet.next()) {
                String discovered = resultSet.getString("FK_NAME");
                if (foreignKeyName.equalsIgnoreCase(discovered)) {
                    return true;
                }
            }
        }
        return false;
    }

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static void addIndexIfMissing(
            DatabaseMetaData metaData,
            String catalog,
            Statement statement,
            String tableName,
            String indexName,
            String sql
    ) throws SQLException {
        if (indexExists(metaData, catalog, tableName, indexName)) {
            return;
        }
        statement.executeUpdate(sql);
    }

    private static boolean indexExists(DatabaseMetaData metaData, String catalog, String tableName, String indexName) throws SQLException {
        try (ResultSet resultSet = metaData.getIndexInfo(catalog, null, tableName, false, false)) {
            while (resultSet.next()) {
                String discovered = resultSet.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(discovered)) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                String discovered = resultSet.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(discovered)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void ensureAnnonceSchema(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            if (!tableExists(metaData, catalog, "annonce")) {
                statement.executeUpdate("""
                        CREATE TABLE annonce (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            titre VARCHAR(255) NOT NULL,
                            description TEXT,
                            poste_recherche VARCHAR(100),
                            niveau_requis VARCHAR(100),
                            date_publication DATE NOT NULL,
                            statut VARCHAR(32) DEFAULT 'ACTIVE',
                            entraineur_id INT NULL,
                            comments_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                            urgent BOOLEAN NOT NULL DEFAULT FALSE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
            }

            addColumnIfMissing(metaData, catalog, statement, "annonce", "comments_enabled", "BOOLEAN NOT NULL DEFAULT TRUE");
            addColumnIfMissing(metaData, catalog, statement, "annonce", "urgent", "BOOLEAN NOT NULL DEFAULT FALSE");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_titre",
                    "CREATE INDEX idx_annonce_titre ON annonce (titre)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_date_publication",
                    "CREATE INDEX idx_annonce_date_publication ON annonce (date_publication)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_poste_recherche",
                    "CREATE INDEX idx_annonce_poste_recherche ON annonce (poste_recherche)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_statut",
                    "CREATE INDEX idx_annonce_statut ON annonce (statut)");
            addIndexIfMissing(metaData, catalog, statement, "annonce", "idx_annonce_urgent",
                    "CREATE INDEX idx_annonce_urgent ON annonce (urgent)");

            if (!tableExists(metaData, catalog, "commentaire")) {
                statement.executeUpdate("""
                        CREATE TABLE commentaire (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            contenu TEXT NOT NULL,
                            date_commentaire DATE NOT NULL,
                            joueur_id INT NULL,
                            annonce_id INT NULL,
                            auteur_anonyme VARCHAR(100),
<<<<<<< HEAD
                            cv_name VARCHAR(255) NULL,
                            cv_title VARCHAR(255) NULL,
                            nb_likes INT DEFAULT 0,
                            nb_dislikes INT DEFAULT 0,
                            moderation_status VARCHAR(32) DEFAULT 'PENDING',
                            moderation_reason TEXT,
                            author_user_id INT NULL,
                            author_role VARCHAR(32) NULL,
=======
                            nb_likes INT DEFAULT 0,
                            moderation_status VARCHAR(32) DEFAULT 'PENDING',
                            moderation_reason TEXT,
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
            }

<<<<<<< HEAD
            addColumnIfMissing(metaData, catalog, statement, "commentaire", "cv_name", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "commentaire", "cv_title", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "commentaire", "nb_dislikes", "INT NOT NULL DEFAULT 0");
            addColumnIfMissing(metaData, catalog, statement, "commentaire", "author_user_id", "INT NULL");
            addColumnIfMissing(metaData, catalog, statement, "commentaire", "author_role", "VARCHAR(32) NULL");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_annonce_id",
                    "CREATE INDEX idx_commentaire_annonce_id ON commentaire (annonce_id)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_joueur_id",
                    "CREATE INDEX idx_commentaire_joueur_id ON commentaire (joueur_id)");
<<<<<<< HEAD
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_author_user_id",
                    "CREATE INDEX idx_commentaire_author_user_id ON commentaire (author_user_id)");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_date_commentaire",
                    "CREATE INDEX idx_commentaire_date_commentaire ON commentaire (date_commentaire)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_moderation_status",
                    "CREATE INDEX idx_commentaire_moderation_status ON commentaire (moderation_status)");
<<<<<<< HEAD

            if (!tableExists(metaData, catalog, "comment_reaction")) {
                statement.executeUpdate("""
                        CREATE TABLE comment_reaction (
                            user_id INT NOT NULL,
                            commentaire_id INT NOT NULL,
                            reaction_type VARCHAR(16) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, commentaire_id)
                        )
                        """);
            }
            addIndexIfMissing(metaData, catalog, statement, "comment_reaction", "idx_comment_reaction_commentaire",
                    "CREATE INDEX idx_comment_reaction_commentaire ON comment_reaction (commentaire_id)");
            addIndexIfMissing(metaData, catalog, statement, "comment_reaction", "idx_comment_reaction_type",
                    "CREATE INDEX idx_comment_reaction_type ON comment_reaction (reaction_type)");

            if (!tableExists(metaData, catalog, "comment_favorite")) {
                statement.executeUpdate("""
                        CREATE TABLE comment_favorite (
                            user_id INT NOT NULL,
                            commentaire_id INT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, commentaire_id)
                        )
                        """);
            }
            addIndexIfMissing(metaData, catalog, statement, "comment_favorite", "idx_comment_favorite_commentaire",
                    "CREATE INDEX idx_comment_favorite_commentaire ON comment_favorite (commentaire_id)");

            try {
                statement.executeUpdate("""
                        UPDATE commentaire
                        SET author_user_id = joueur_id
                        WHERE author_user_id IS NULL AND joueur_id IS NOT NULL
                        """);
                statement.executeUpdate("""
                        UPDATE commentaire
                        SET author_role = 'ROLE_JOUEUR'
                        WHERE author_role IS NULL AND joueur_id IS NOT NULL
                        """);
            } catch (SQLException ignored) {
                // Best-effort backfill for legacy rows.
            }
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    private static void ensureUserSchema(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            if (!tableExists(metaData, catalog, "user")) {
                statement.executeUpdate("""
                        CREATE TABLE `user` (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            email VARCHAR(180) NOT NULL,
                            roles LONGTEXT NOT NULL,
                            password VARCHAR(255) NOT NULL,
                            nom VARCHAR(255) NOT NULL,
                            prenom VARCHAR(255) NOT NULL,
                            telephone VARCHAR(50) NULL,
                            date_naissance DATE NULL,
                            photo VARCHAR(255) NULL,
                            statut VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                            date_inscription DATETIME NOT NULL,
                            cv_name VARCHAR(255) NULL,
<<<<<<< HEAD
                            updated_at DATETIME NULL,
                            face_registered TINYINT(1) NOT NULL DEFAULT 0
=======
                            updated_at DATETIME NULL
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                        )
                        """);
            }

            addColumnIfMissing(metaData, catalog, statement, "user", "telephone", "VARCHAR(50) NULL");
            addColumnIfMissing(metaData, catalog, statement, "user", "date_naissance", "DATE NULL");
            addColumnIfMissing(metaData, catalog, statement, "user", "photo", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "user", "statut", "VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'");
            addColumnIfMissing(metaData, catalog, statement, "user", "date_inscription", "DATETIME NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "user", "cv_name", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "user", "updated_at", "DATETIME NULL");
<<<<<<< HEAD
            addColumnIfMissing(metaData, catalog, statement, "user", "face_registered", "TINYINT(1) NOT NULL DEFAULT 0");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

            addIndexIfMissing(metaData, catalog, statement, "user", "idx_user_email",
                    "CREATE INDEX idx_user_email ON `user` (email)");
            addIndexIfMissing(metaData, catalog, statement, "user", "idx_user_status",
                    "CREATE INDEX idx_user_status ON `user` (statut)");
<<<<<<< HEAD

            if (!tableExists(metaData, catalog, "password_reset_token")) {
                statement.executeUpdate("""
                        CREATE TABLE password_reset_token (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            token VARCHAR(16) NOT NULL,
                            expires_at DATETIME NOT NULL,
                            used TINYINT(1) NOT NULL DEFAULT 0,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_password_reset_user
                                FOREIGN KEY (user_id) REFERENCES `user` (id)
                                ON DELETE CASCADE ON UPDATE CASCADE
                        )
                        """);
            }

            addIndexIfMissing(metaData, catalog, statement, "password_reset_token", "idx_password_reset_user",
                    "CREATE INDEX idx_password_reset_user ON password_reset_token (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "password_reset_token", "idx_password_reset_token",
                    "CREATE INDEX idx_password_reset_token ON password_reset_token (token)");
        }
    }

    private static void ensureSponsorSchema(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            if (!tableExists(metaData, catalog, "sponsor")) {
                statement.executeUpdate("""
                        CREATE TABLE sponsor (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            nom VARCHAR(255) NOT NULL,
                            email VARCHAR(180) NULL,
                            telephone VARCHAR(50) NULL,
                            budget DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                            logo_name VARCHAR(255) NULL,
                            updated_at DATETIME NULL,
                            adresse VARCHAR(255) NULL
                        )
                        """);
            }

            addColumnIfMissing(metaData, catalog, statement, "sponsor", "email", "VARCHAR(180) NULL");
            addColumnIfMissing(metaData, catalog, statement, "sponsor", "telephone", "VARCHAR(50) NULL");
            addColumnIfMissing(metaData, catalog, statement, "sponsor", "budget", "DECIMAL(12,2) NOT NULL DEFAULT 0.00");
            addColumnIfMissing(metaData, catalog, statement, "sponsor", "logo_name", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "sponsor", "updated_at", "DATETIME NULL");
            addColumnIfMissing(metaData, catalog, statement, "sponsor", "adresse", "VARCHAR(255) NULL");

            if (tableExists(metaData, catalog, "equipe") && !tableExists(metaData, catalog, "contrat_sponsor")) {
                statement.executeUpdate("""
                        CREATE TABLE contrat_sponsor (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            date_debut DATE NOT NULL,
                            date_fin DATE NOT NULL,
                            montant DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                            description TEXT NULL,
                            statut VARCHAR(100) NULL,
                            notified BOOLEAN NOT NULL DEFAULT FALSE,
                            statut_paiement VARCHAR(100) NULL,
                            sponsor_id INT NOT NULL,
                            equipe_id INT NOT NULL,
                            CONSTRAINT fk_contrat_sponsor_sponsor
                                FOREIGN KEY (sponsor_id) REFERENCES sponsor (id)
                                ON DELETE CASCADE ON UPDATE CASCADE,
                            CONSTRAINT fk_contrat_sponsor_equipe
                                FOREIGN KEY (equipe_id) REFERENCES equipe (id)
                                ON DELETE CASCADE ON UPDATE CASCADE
                        )
                        """);
            }

            if (tableExists(metaData, catalog, "contrat_sponsor")) {
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "date_debut", "DATE NOT NULL");
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "date_fin", "DATE NOT NULL");
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "montant", "DECIMAL(12,2) NOT NULL DEFAULT 0.00");
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "description", "TEXT NULL");
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "statut", "VARCHAR(100) NULL");
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "notified", "BOOLEAN NOT NULL DEFAULT FALSE");
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "statut_paiement", "VARCHAR(100) NULL");
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "sponsor_id", "INT NOT NULL");
                addColumnIfMissing(metaData, catalog, statement, "contrat_sponsor", "equipe_id", "INT NOT NULL");
                addIndexIfMissing(metaData, catalog, statement, "contrat_sponsor", "idx_contrat_sponsor_sponsor_id",
                        "CREATE INDEX idx_contrat_sponsor_sponsor_id ON contrat_sponsor (sponsor_id)");
                addIndexIfMissing(metaData, catalog, statement, "contrat_sponsor", "idx_contrat_sponsor_equipe_id",
                        "CREATE INDEX idx_contrat_sponsor_equipe_id ON contrat_sponsor (equipe_id)");
            }
        }
    }

    private static void ensureFaceIdSchema(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            if (!tableExists(metaData, catalog, "face_profile")) {
                statement.executeUpdate("""
                        CREATE TABLE face_profile (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            embedding_json LONGTEXT NOT NULL,
                            model_name VARCHAR(80) NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            CONSTRAINT uk_face_profile_user UNIQUE (user_id),
                            CONSTRAINT fk_face_profile_user FOREIGN KEY (user_id) REFERENCES `user` (id)
                                ON DELETE CASCADE
                        )
                        """);
            }

            addColumnIfMissing(metaData, catalog, statement, "face_profile", "user_id", "INT NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "face_profile", "embedding_json", "LONGTEXT NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "face_profile", "model_name", "VARCHAR(80) NULL");
            addColumnIfMissing(metaData, catalog, statement, "face_profile", "created_at",
                    "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            addColumnIfMissing(metaData, catalog, statement, "face_profile", "updated_at",
                    "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            addIndexIfMissing(metaData, catalog, statement, "face_profile", "uk_face_profile_user",
                    "CREATE UNIQUE INDEX uk_face_profile_user ON face_profile (user_id)");
        }
    }

    private static void ensureOrderSchema(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            addColumnIfMissing(metaData, catalog, statement, "order", "client_name", "VARCHAR(120) NULL AFTER `order_date`");

            statement.executeUpdate("""
                    UPDATE `order` o
                    LEFT JOIN `user` u ON u.id = o.entraineur_id
                    SET o.client_name = COALESCE(
                        NULLIF(TRIM(o.client_name), ''),
                        NULLIF(TRIM(CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, ''))), ''),
                        NULLIF(TRIM(u.email), ''),
                        CASE
                            WHEN o.contact_email IS NOT NULL AND TRIM(o.contact_email) <> ''
                                THEN SUBSTRING_INDEX(TRIM(o.contact_email), '@', 1)
                            WHEN o.contact_phone IS NOT NULL AND TRIM(o.contact_phone) <> ''
                                THEN CONCAT('Client ', TRIM(o.contact_phone))
                            ELSE 'Client Sport Insight'
                        END
                    )
                    WHERE o.client_name IS NULL OR TRIM(o.client_name) = ''
                    """);

            try {
                statement.executeUpdate("ALTER TABLE `order` MODIFY COLUMN `entraineur_id` INT NULL");
            } catch (SQLException ignored) {
                // Ignore if the column is already nullable or the engine treats this as a no-op.
            }
        }
    }

    private static void ensureMatchLiveSchema(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            if (!tableExists(metaData, catalog, "match_follow_target")) {
                statement.executeUpdate("""
                        CREATE TABLE match_follow_target (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            target_type VARCHAR(16) NOT NULL,
                            team_id INT NULL,
                            match_id INT NULL,
                            competition_code VARCHAR(16) NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
            }

            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "user_id", "INT NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "target_type", "VARCHAR(16) NOT NULL DEFAULT 'TEAM'");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "team_id", "INT NULL");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "match_id", "INT NULL");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "competition_code", "VARCHAR(16) NULL");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "created_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "idx_match_follow_target_user",
                    "CREATE INDEX idx_match_follow_target_user ON match_follow_target (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "idx_match_follow_target_team",
                    "CREATE INDEX idx_match_follow_target_team ON match_follow_target (team_id)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "idx_match_follow_target_match",
                    "CREATE INDEX idx_match_follow_target_match ON match_follow_target (match_id)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "idx_match_follow_target_competition",
                    "CREATE INDEX idx_match_follow_target_competition ON match_follow_target (competition_code)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "uq_match_follow_target_team",
                    "CREATE UNIQUE INDEX uq_match_follow_target_team ON match_follow_target (user_id, target_type, team_id)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "uq_match_follow_target_match",
                    "CREATE UNIQUE INDEX uq_match_follow_target_match ON match_follow_target (user_id, target_type, match_id)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "uq_match_follow_target_competition",
                    "CREATE UNIQUE INDEX uq_match_follow_target_competition ON match_follow_target (user_id, target_type, competition_code)");

            if (!tableExists(metaData, catalog, "notification")) {
                statement.executeUpdate("""
                        CREATE TABLE notification (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            title VARCHAR(255) NULL,
                            message TEXT NOT NULL,
                            type VARCHAR(32) NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            is_read BOOLEAN NOT NULL DEFAULT FALSE,
                            user_id INT NOT NULL,
                            match_id INT NULL,
                            dedupe_key VARCHAR(255) NULL,
                            competition_code VARCHAR(16) NULL,
                            home_team_name VARCHAR(255) NULL,
                            away_team_name VARCHAR(255) NULL,
                            home_team_logo VARCHAR(255) NULL,
                            away_team_logo VARCHAR(255) NULL,
                            actor_name VARCHAR(255) NULL,
                            actor_image VARCHAR(255) NULL,
                            minute_label VARCHAR(32) NULL,
                            accent_tone VARCHAR(32) NULL
                        )
                        """);
            }

            addColumnIfMissing(metaData, catalog, statement, "notification", "title", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "type", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "match_id", "INT NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "dedupe_key", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "competition_code", "VARCHAR(16) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "home_team_name", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "away_team_name", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "home_team_logo", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "away_team_logo", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "actor_name", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "actor_image", "VARCHAR(255) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "minute_label", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, catalog, statement, "notification", "accent_tone", "VARCHAR(32) NULL");

            addIndexIfMissing(metaData, catalog, statement, "notification", "idx_notification_user_created",
                    "CREATE INDEX idx_notification_user_created ON notification (user_id, created_at)");
            addIndexIfMissing(metaData, catalog, statement, "notification", "idx_notification_match",
                    "CREATE INDEX idx_notification_match ON notification (match_id)");
            addIndexIfMissing(metaData, catalog, statement, "notification", "uq_notification_dedupe",
                    "CREATE UNIQUE INDEX uq_notification_dedupe ON notification (user_id, dedupe_key)");
        }
    }

    private static void ensureNutritionSchema(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            if (!tableExists(metaData, catalog, "ai_checklist_progress")) {
                statement.executeUpdate("""
                        CREATE TABLE ai_checklist_progress (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            plan_type VARCHAR(50) NOT NULL,
                            plan_category VARCHAR(50) NOT NULL,
                            item_text VARCHAR(500) NOT NULL,
                            is_completed BOOLEAN NOT NULL DEFAULT FALSE,
                            completed_at DATETIME NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_checklist_user
                                FOREIGN KEY (user_id) REFERENCES `user` (id)
                                ON DELETE CASCADE ON UPDATE CASCADE
                        )
                        """);
            }

            if (!tableExists(metaData, catalog, "food_log")) {
                statement.executeUpdate("""
                        CREATE TABLE food_log (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            log_date DATE NOT NULL,
                            meal_type VARCHAR(50) NOT NULL,
                            food_description TEXT NOT NULL,
                            calories DECIMAL(8,2) NOT NULL DEFAULT 0.00,
                            protein_g DECIMAL(8,2) NULL,
                            carbs_g DECIMAL(8,2) NULL,
                            fat_g DECIMAL(8,2) NULL,
                            fiber_g DECIMAL(8,2) NULL,
                            api_response TEXT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_food_log_user
                                FOREIGN KEY (user_id) REFERENCES `user` (id)
                                ON DELETE CASCADE ON UPDATE CASCADE
                        )
                        """);
            }

            if (!tableExists(metaData, catalog, "daily_nutrition_summary")) {
                statement.executeUpdate("""
                        CREATE TABLE daily_nutrition_summary (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            summary_date DATE NOT NULL,
                            total_calories DECIMAL(8,2) NOT NULL DEFAULT 0.00,
                            total_protein_g DECIMAL(8,2) NOT NULL DEFAULT 0.00,
                            total_carbs_g DECIMAL(8,2) NOT NULL DEFAULT 0.00,
                            total_fat_g DECIMAL(8,2) NOT NULL DEFAULT 0.00,
                            total_fiber_g DECIMAL(8,2) NOT NULL DEFAULT 0.00,
                            target_calories DECIMAL(8,2) NULL,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            UNIQUE KEY unique_user_date (user_id, summary_date),
                            CONSTRAINT fk_summary_user
                                FOREIGN KEY (user_id) REFERENCES `user` (id)
                                ON DELETE CASCADE ON UPDATE CASCADE
                        )
                        """);
            }

            addColumnIfMissing(metaData, catalog, statement, "ai_checklist_progress", "user_id", "INT NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "ai_checklist_progress", "plan_type", "VARCHAR(50) NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "ai_checklist_progress", "plan_category", "VARCHAR(50) NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "ai_checklist_progress", "item_text", "VARCHAR(500) NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "ai_checklist_progress", "is_completed", "BOOLEAN NOT NULL DEFAULT FALSE");
            addColumnIfMissing(metaData, catalog, statement, "ai_checklist_progress", "completed_at", "DATETIME NULL");
            addColumnIfMissing(metaData, catalog, statement, "ai_checklist_progress", "created_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

            addColumnIfMissing(metaData, catalog, statement, "food_log", "user_id", "INT NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "log_date", "DATE NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "meal_type", "VARCHAR(50) NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "food_description", "TEXT NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "calories", "DECIMAL(8,2) NOT NULL DEFAULT 0.00");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "protein_g", "DECIMAL(8,2) NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "carbs_g", "DECIMAL(8,2) NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "fat_g", "DECIMAL(8,2) NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "fiber_g", "DECIMAL(8,2) NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "api_response", "TEXT NULL");
            addColumnIfMissing(metaData, catalog, statement, "food_log", "created_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "user_id", "INT NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "summary_date", "DATE NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "total_calories", "DECIMAL(8,2) NOT NULL DEFAULT 0.00");
            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "total_protein_g", "DECIMAL(8,2) NOT NULL DEFAULT 0.00");
            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "total_carbs_g", "DECIMAL(8,2) NOT NULL DEFAULT 0.00");
            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "total_fat_g", "DECIMAL(8,2) NOT NULL DEFAULT 0.00");
            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "total_fiber_g", "DECIMAL(8,2) NOT NULL DEFAULT 0.00");
            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "target_calories", "DECIMAL(8,2) NULL");
            addColumnIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "updated_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

            addIndexIfMissing(metaData, catalog, statement, "ai_checklist_progress", "idx_checklist_user_id",
                    "CREATE INDEX idx_checklist_user_id ON ai_checklist_progress (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "ai_checklist_progress", "idx_checklist_plan_type",
                    "CREATE INDEX idx_checklist_plan_type ON ai_checklist_progress (plan_type)");
            addIndexIfMissing(metaData, catalog, statement, "ai_checklist_progress", "idx_checklist_plan_category",
                    "CREATE INDEX idx_checklist_plan_category ON ai_checklist_progress (plan_category)");
            addIndexIfMissing(metaData, catalog, statement, "food_log", "idx_food_log_user_id",
                    "CREATE INDEX idx_food_log_user_id ON food_log (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "food_log", "idx_food_log_date",
                    "CREATE INDEX idx_food_log_date ON food_log (log_date)");
            addIndexIfMissing(metaData, catalog, statement, "food_log", "idx_food_log_meal_type",
                    "CREATE INDEX idx_food_log_meal_type ON food_log (meal_type)");
            addIndexIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "idx_summary_user_id",
                    "CREATE INDEX idx_summary_user_id ON daily_nutrition_summary (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "daily_nutrition_summary", "idx_summary_date",
                    "CREATE INDEX idx_summary_date ON daily_nutrition_summary (summary_date)");
        }
    }

    private static void ensureTrainingUserLinks(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            addIndexIfMissing(metaData, catalog, statement, "evaluation", "idx_evaluation_joueur_id",
                    "CREATE INDEX idx_evaluation_joueur_id ON evaluation (joueur_id)");
            addIndexIfMissing(metaData, catalog, statement, "participation", "idx_participation_joueur_id",
                    "CREATE INDEX idx_participation_joueur_id ON participation (joueur_id)");
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    private static boolean tableExists(DatabaseMetaData metaData, String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = metaData.getTables(catalog, null, tableName, new String[] { "TABLE" })) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getTables(catalog, null, tableName.toUpperCase(), new String[] { "TABLE" })) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, new String[] { "TABLE" })) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getTables(null, null, tableName.toUpperCase(), new String[] { "TABLE" })) {
            return resultSet.next();
        }
    }

    private static String currentCatalog(Connection connection) throws SQLException {
        String catalog = connection.getCatalog();
        return catalog == null || catalog.isBlank() ? null : catalog;
    }
}

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
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = currentCatalog(connection);
        try (Statement statement = connection.createStatement()) {
            addColumnIfMissing(metaData, catalog, statement, "equipe", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, catalog, statement, "equipe", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, catalog, statement, "equipe", "competition_code", "VARCHAR(16) NULL");
            addColumnIfMissing(metaData, catalog, statement, "equipe", "api_football_id", "BIGINT NULL");

            addColumnIfMissing(metaData, catalog, statement, "joueur", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, catalog, statement, "joueur", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, catalog, statement, "joueur", "position", "VARCHAR(120) NULL");
            addColumnIfMissing(metaData, catalog, statement, "joueur", "nationalite", "VARCHAR(120) NULL");

            addColumnIfMissing(metaData, catalog, statement, "matchs", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "competition_code", "VARCHAR(16) NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_id", "BIGINT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_stats_json", "LONGTEXT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_lineup_json", "LONGTEXT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_incidents_json", "LONGTEXT NULL");
            addColumnIfMissing(metaData, catalog, statement, "matchs", "api_football_synced_at", "DATETIME NULL");

            addIndexIfMissing(metaData, catalog, statement, "equipe", "idx_equipe_external_sync",
                    "CREATE INDEX idx_equipe_external_sync ON equipe (external_source, external_api_id)");
            addIndexIfMissing(metaData, catalog, statement, "equipe", "idx_equipe_competition_code",
                    "CREATE INDEX idx_equipe_competition_code ON equipe (competition_code)");
            addIndexIfMissing(metaData, catalog, statement, "equipe", "idx_equipe_api_football_id",
                    "CREATE INDEX idx_equipe_api_football_id ON equipe (api_football_id)");
            addIndexIfMissing(metaData, catalog, statement, "joueur", "idx_joueur_external_sync",
                    "CREATE INDEX idx_joueur_external_sync ON joueur (external_source, external_api_id)");
            addIndexIfMissing(metaData, catalog, statement, "matchs", "idx_matchs_external_sync",
                    "CREATE INDEX idx_matchs_external_sync ON matchs (external_source, external_api_id)");
            addIndexIfMissing(metaData, catalog, statement, "matchs", "idx_matchs_competition_code",
                    "CREATE INDEX idx_matchs_competition_code ON matchs (competition_code)");
            addIndexIfMissing(metaData, catalog, statement, "matchs", "idx_matchs_api_football_id",
                    "CREATE INDEX idx_matchs_api_football_id ON matchs (api_football_id)");

            try {
                backfillEquipeCompetitionCodes(statement);
            } catch (SQLException e) {
                System.err.println("Team competition backfill skipped: " + e.getMessage());
            }
        }

        ensureAnnonceSchema(connection);
        ensureUserSchema(connection);
        ensureMatchLiveSchema(connection);
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
                            nb_likes INT DEFAULT 0,
                            moderation_status VARCHAR(32) DEFAULT 'PENDING',
                            moderation_reason TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
            }

            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_annonce_id",
                    "CREATE INDEX idx_commentaire_annonce_id ON commentaire (annonce_id)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_joueur_id",
                    "CREATE INDEX idx_commentaire_joueur_id ON commentaire (joueur_id)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_date_commentaire",
                    "CREATE INDEX idx_commentaire_date_commentaire ON commentaire (date_commentaire)");
            addIndexIfMissing(metaData, catalog, statement, "commentaire", "idx_commentaire_moderation_status",
                    "CREATE INDEX idx_commentaire_moderation_status ON commentaire (moderation_status)");
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
                            updated_at DATETIME NULL
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

            addIndexIfMissing(metaData, catalog, statement, "user", "idx_user_email",
                    "CREATE INDEX idx_user_email ON `user` (email)");
            addIndexIfMissing(metaData, catalog, statement, "user", "idx_user_status",
                    "CREATE INDEX idx_user_status ON `user` (statut)");
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
                            competition_code VARCHAR(16) NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
            }

            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "user_id", "INT NOT NULL");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "target_type", "VARCHAR(16) NOT NULL DEFAULT 'TEAM'");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "team_id", "INT NULL");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "competition_code", "VARCHAR(16) NULL");
            addColumnIfMissing(metaData, catalog, statement, "match_follow_target", "created_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "idx_match_follow_target_user",
                    "CREATE INDEX idx_match_follow_target_user ON match_follow_target (user_id)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "idx_match_follow_target_team",
                    "CREATE INDEX idx_match_follow_target_team ON match_follow_target (team_id)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "idx_match_follow_target_competition",
                    "CREATE INDEX idx_match_follow_target_competition ON match_follow_target (competition_code)");
            addIndexIfMissing(metaData, catalog, statement, "match_follow_target", "uq_match_follow_target_team",
                    "CREATE UNIQUE INDEX uq_match_follow_target_team ON match_follow_target (user_id, target_type, team_id)");
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

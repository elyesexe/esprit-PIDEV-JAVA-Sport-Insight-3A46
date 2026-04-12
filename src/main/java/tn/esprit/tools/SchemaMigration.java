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
        try (Statement statement = connection.createStatement()) {
            addColumnIfMissing(metaData, statement, "equipe", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, statement, "equipe", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, statement, "equipe", "competition_code", "VARCHAR(16) NULL");

            addColumnIfMissing(metaData, statement, "joueur", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, statement, "joueur", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, statement, "joueur", "position", "VARCHAR(120) NULL");
            addColumnIfMissing(metaData, statement, "joueur", "nationalite", "VARCHAR(120) NULL");

            addColumnIfMissing(metaData, statement, "matchs", "external_api_id", "BIGINT NULL");
            addColumnIfMissing(metaData, statement, "matchs", "external_source", "VARCHAR(32) NULL");
            addColumnIfMissing(metaData, statement, "matchs", "competition_code", "VARCHAR(16) NULL");

            addIndexIfMissing(metaData, statement, "equipe", "idx_equipe_external_sync",
                    "CREATE INDEX idx_equipe_external_sync ON equipe (external_source, external_api_id)");
            addIndexIfMissing(metaData, statement, "equipe", "idx_equipe_competition_code",
                    "CREATE INDEX idx_equipe_competition_code ON equipe (competition_code)");
            addIndexIfMissing(metaData, statement, "joueur", "idx_joueur_external_sync",
                    "CREATE INDEX idx_joueur_external_sync ON joueur (external_source, external_api_id)");
            addIndexIfMissing(metaData, statement, "matchs", "idx_matchs_external_sync",
                    "CREATE INDEX idx_matchs_external_sync ON matchs (external_source, external_api_id)");
            addIndexIfMissing(metaData, statement, "matchs", "idx_matchs_competition_code",
                    "CREATE INDEX idx_matchs_competition_code ON matchs (competition_code)");

            try {
                backfillEquipeCompetitionCodes(statement);
            } catch (SQLException e) {
                System.err.println("Team competition backfill skipped: " + e.getMessage());
            }
        }
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
            Statement statement,
            String tableName,
            String columnName,
            String definition
    ) throws SQLException {
        if (columnExists(metaData, tableName, columnName)) {
            return;
        }

        statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private static boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
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
            Statement statement,
            String tableName,
            String indexName,
            String sql
    ) throws SQLException {
        if (indexExists(metaData, tableName, indexName)) {
            return;
        }
        statement.executeUpdate(sql);
    }

    private static boolean indexExists(DatabaseMetaData metaData, String tableName, String indexName) throws SQLException {
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
}

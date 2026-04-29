package tn.esprit.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class DatabaseBootstrap {
    private static final String DATABASE_DIRECTORY = "database";
    private static final String SCHEMA_SCRIPT = "sport_insight_schema.sql";
    private static final String DATA_SCRIPT = "sport_insight_data.sql";
    private static final Pattern VERSIONED_COMMENT_PATTERN = Pattern.compile("/\\*!\\d+\\s*(.*?)\\*/", Pattern.DOTALL);
    private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile("/\\*(?!\\!).*?\\*/", Pattern.DOTALL);
    private static final Pattern DASH_COMMENT_PATTERN = Pattern.compile("(?m)^\\s*--(?:\\s.*)?$");
    private static final Pattern HASH_COMMENT_PATTERN = Pattern.compile("(?m)^\\s*#.*$");

    private DatabaseBootstrap() {
    }

    public static void initializeIfNeeded(Connection connection) throws SQLException {
        if (!isDatabaseEmpty(connection)) {
            return;
        }

        executeScriptIfPresent(connection, SCHEMA_SCRIPT);
        executeScriptIfPresent(connection, DATA_SCRIPT);
    }

    private static boolean isDatabaseEmpty(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet tables = metaData.getTables(catalog, null, "%", new String[] { "TABLE" })) {
            return !tables.next();
        }
    }

    private static void executeScriptIfPresent(Connection connection, String scriptName) throws SQLException {
        Path scriptPath = resolveScriptPath(scriptName);
        if (scriptPath == null) {
            return;
        }

        String sql;
        try {
            sql = Files.readString(scriptPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Unable to read SQL bootstrap script: " + scriptPath, e);
        }

        List<String> statements = splitStatements(sql);
        try (Statement statement = connection.createStatement()) {
            for (String sqlStatement : statements) {
                if (!sqlStatement.isBlank()) {
                    statement.execute(sqlStatement);
                }
            }
        }
    }

    static List<String> splitStatements(String script) {
        String normalized = normalizeScript(script);
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBackticks = false;

        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            char next = index + 1 < normalized.length() ? normalized.charAt(index + 1) : '\0';
            char previous = index > 0 ? normalized.charAt(index - 1) : '\0';

            if (!inDoubleQuote && !inBackticks && character == '\'') {
                if (inSingleQuote && next == '\'') {
                    current.append(character).append(next);
                    index++;
                    continue;
                }
                if (previous != '\\') {
                    inSingleQuote = !inSingleQuote;
                }
            } else if (!inSingleQuote && !inBackticks && character == '"') {
                if (inDoubleQuote && next == '"') {
                    current.append(character).append(next);
                    index++;
                    continue;
                }
                if (previous != '\\') {
                    inDoubleQuote = !inDoubleQuote;
                }
            } else if (!inSingleQuote && !inDoubleQuote && character == '`') {
                inBackticks = !inBackticks;
            }

            if (character == ';' && !inSingleQuote && !inDoubleQuote && !inBackticks) {
                String statement = current.toString().trim();
                if (!statement.isBlank()) {
                    statements.add(statement);
                }
                current.setLength(0);
                continue;
            }

            current.append(character);
        }

        String trailing = current.toString().trim();
        if (!trailing.isBlank()) {
            statements.add(trailing);
        }
        return statements;
    }

    private static String normalizeScript(String script) {
        String withoutBom = script.startsWith("\uFEFF") ? script.substring(1) : script;
        String expandedVersionComments = VERSIONED_COMMENT_PATTERN.matcher(withoutBom).replaceAll("$1");
        String withoutBlockComments = BLOCK_COMMENT_PATTERN.matcher(expandedVersionComments).replaceAll("");
        String withoutDashComments = DASH_COMMENT_PATTERN.matcher(withoutBlockComments).replaceAll("");
        return HASH_COMMENT_PATTERN.matcher(withoutDashComments).replaceAll("");
    }

    private static Path resolveScriptPath(String fileName) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(DATABASE_DIRECTORY).resolve(fileName);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }
}

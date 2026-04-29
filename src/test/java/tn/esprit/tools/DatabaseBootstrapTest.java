package tn.esprit.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseBootstrapTest {
    @Test
    void splitStatementsKeepsQuotedSemicolonsAndExpandsMysqlVersionComments() {
        String script = """
                -- regular comment
                CREATE TABLE demo (id INT);
                /*!40101 SET NAMES utf8mb4 */;
                INSERT INTO demo VALUES (1, 'semi;colon', 'It''s fine');
                # another comment
                INSERT INTO demo VALUES (2, "double;quote", "ok");
                """;

        List<String> statements = DatabaseBootstrap.splitStatements(script);

        assertEquals(List.of(
                "CREATE TABLE demo (id INT)",
                "SET NAMES utf8mb4",
                "INSERT INTO demo VALUES (1, 'semi;colon', 'It''s fine')",
                "INSERT INTO demo VALUES (2, \"double;quote\", \"ok\")"
        ), statements);
    }
}

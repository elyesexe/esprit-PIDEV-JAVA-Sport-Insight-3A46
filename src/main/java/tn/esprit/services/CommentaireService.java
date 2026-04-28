package tn.esprit.services;

import tn.esprit.entities.Commentaire;
import tn.esprit.security.UserRoles;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class CommentaireService implements IService<Commentaire> {
    private final Connection connection;
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d[\\d\\s\\-]{7,}\\d");
    private static final Pattern REPEATED_CHAR_PATTERN = Pattern.compile("(.)\\1{6,}");
    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final List<String> SPAM_KEYWORDS = List.of(
            "bit.ly", "t.me", "telegram", "whatsapp", "casino", "crypto",
            "invest", "argent facile", "promo", "gratuit", "click here", "dm me",
            "contact me", "signal", "forex", "nft"
    );

    public CommentaireService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public CommentaireService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void add(Commentaire commentaire) throws SQLException {
        validateReferences(commentaire);
        if (commentaire.getAnnonceId() != null && !areCommentsEnabled(commentaire.getAnnonceId())) {
            throw new SQLException("Comments are disabled for the selected announcement.");
        }

        applySpamModeration(commentaire);

        String query = """
                INSERT INTO commentaire (
                    contenu, date_commentaire, joueur_id, annonce_id,
                    auteur_anonyme, nb_likes, moderation_status, moderation_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, commentaire);
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Commentaire commentaire) throws SQLException {
        validateReferences(commentaire);

        String query = """
                UPDATE commentaire
                SET contenu = ?, date_commentaire = ?, joueur_id = ?, annonce_id = ?,
                    auteur_anonyme = ?, nb_likes = ?, moderation_status = ?, moderation_reason = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, commentaire);
            statement.setInt(9, commentaire.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM commentaire WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Commentaire> getAll() throws SQLException {
        return executeListQuery("SELECT * FROM commentaire ORDER BY date_commentaire DESC, id DESC");
    }

    @Override
    public Commentaire getById(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM commentaire WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        }
        return null;
    }

    public List<Commentaire> getCommentairesByAnnonce(int annonceId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = "SELECT * FROM commentaire WHERE annonce_id = ? ORDER BY date_commentaire DESC, id DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, annonceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    commentaires.add(mapRow(resultSet));
                }
            }
        }
        return commentaires;
    }

    public List<Commentaire> getCommentairesByJoueur(int joueurId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = "SELECT * FROM commentaire WHERE joueur_id = ? ORDER BY date_commentaire DESC, id DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, joueurId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    commentaires.add(mapRow(resultSet));
                }
            }
        }
        return commentaires;
    }

    @Override
    public List<Commentaire> search(String keyword) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = """
                SELECT * FROM commentaire
                WHERE LOWER(contenu) LIKE LOWER(?)
                   OR LOWER(auteur_anonyme) LIKE LOWER(?)
                   OR LOWER(moderation_status) LIKE LOWER(?)
                ORDER BY date_commentaire DESC, id DESC
                """;
        String pattern = "%" + keyword + "%";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    commentaires.add(mapRow(resultSet));
                }
            }
        }
        return commentaires;
    }

    public int countAll() throws SQLException {
        return executeCountQuery("SELECT COUNT(*) FROM commentaire");
    }

    public int countPendingModeration() throws SQLException {
        return executeCountQuery("SELECT COUNT(*) FROM commentaire WHERE moderation_status = 'PENDING'");
    }

    public int countCommentairesByAnnonce(int annonceId) throws SQLException {
        String query = "SELECT COUNT(*) FROM commentaire WHERE annonce_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, annonceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    public Map<Integer, Integer> countCommentairesGroupByAnnonce() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String query = "SELECT annonce_id, COUNT(*) AS total FROM commentaire GROUP BY annonce_id";
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                counts.put(resultSet.getInt("annonce_id"), resultSet.getInt("total"));
            }
        }
        return counts;
    }

    public void deleteByAnnonce(int annonceId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM commentaire WHERE annonce_id = ?")) {
            statement.setInt(1, annonceId);
            statement.executeUpdate();
        }
    }

    private void applySpamModeration(Commentaire commentaire) throws SQLException {
        SpamDetectionResult spamDetection = detectSpam(commentaire);
        if (spamDetection.spam()) {
            commentaire.setModerationStatus("REJECTED");
            commentaire.setModerationReason("Spam detecte: " + spamDetection.reason());
            if (spamDetection.shouldBlockUser() && commentaire.getJoueurId() != null) {
                blockUserForSpam(commentaire.getJoueurId());
            }
        } else if (commentaire.getModerationStatus() == null || commentaire.getModerationStatus().isBlank()) {
            commentaire.setModerationStatus("APPROVED");
        }
    }

    private record SpamDetectionResult(boolean spam, boolean shouldBlockUser, String reason) {
    }

    private SpamDetectionResult detectSpam(Commentaire commentaire) throws SQLException {
        if (commentaire == null) {
            return new SpamDetectionResult(false, false, null);
        }

        String content = commentaire.getContenu() == null ? null : commentaire.getContenu().trim();
        if (content == null || content.isBlank()) {
            return new SpamDetectionResult(false, false, null);
        }

        String lowercase = content.toLowerCase(Locale.ROOT);
        int score = 0;
        int hardSignals = 0;
        String reason = "contenu suspect detecte";

        int urlCount = countPatternMatches(URL_PATTERN, lowercase);
        if (REPEATED_CHAR_PATTERN.matcher(lowercase).find()) {
            score += 5;
            hardSignals++;
            reason = "caracteres repetes";
        }
        if (urlCount >= 2) {
            score += 5;
            hardSignals++;
            reason = "liens suspects";
        } else if (urlCount == 1) {
            score += 2;
        }

        long keywordHits = SPAM_KEYWORDS.stream().filter(lowercase::contains).count();
        if (keywordHits >= 3) {
            score += 6;
            hardSignals++;
            reason = "mots-cles spam";
        } else if (keywordHits >= 1) {
            score += (int) keywordHits + 1;
        }

        if (countPatternMatches(PHONE_PATTERN, content) >= 1) {
            score += 2;
        }

        int letterCount = countPatternMatches(LETTER_PATTERN, content);
        if (letterCount >= 12) {
            long uppercaseCount = content.chars().filter(Character::isUpperCase).count();
            if ((double) uppercaseCount / (double) letterCount > 0.85d) {
                score += 2;
                reason = "majuscules excessives";
            }
        }

        Integer joueurId = commentaire.getJoueurId();
        if (joueurId != null) {
            long sameContentToday = countSameContentToday(joueurId, lowercase);
            if (sameContentToday >= 2) {
                score += 4;
                reason = "message repetitif";
            } else if (sameContentToday == 1) {
                score += 2;
            }

            long todayCount = countMessagesToday(joueurId);
            if (todayCount >= 8) {
                score += 4;
                reason = "trop de messages aujourd'hui";
            } else if (todayCount >= 4) {
                score += 2;
            }
            if (todayCount >= 4 && urlCount >= 1) {
                score += 2;
                reason = "envoi massif avec lien";
            }
        }

        String[] tokens = lowercase.split("\\s+");
        long words = tokens.length;
        long uniqueWords = Arrays.stream(tokens)
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .count();
        if (words >= 6 && uniqueWords > 0) {
            double duplicateRatio = 1d - ((double) uniqueWords / (double) words);
            if (duplicateRatio >= 0.65d) {
                score += 2;
            }
        }

        boolean hasSpamIntent = urlCount > 0 || keywordHits > 0;
        boolean spam = hardSignals >= 1 || score >= 5 || (score >= 4 && hasSpamIntent);
        boolean shouldBlockUser = score >= 8 || hardSignals >= 2;
        return new SpamDetectionResult(spam, shouldBlockUser, reason);
    }

    private long countSameContentToday(Integer joueurId, String lowercaseContent) throws SQLException {
        String query = """
                SELECT COUNT(*) FROM commentaire
                WHERE joueur_id = ?
                  AND date_commentaire = ?
                  AND LOWER(contenu) = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, joueurId);
            statement.setDate(2, Date.valueOf(LocalDate.now()));
            statement.setString(3, lowercaseContent);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        return 0;
    }

    private long countMessagesToday(Integer joueurId) throws SQLException {
        String query = "SELECT COUNT(*) FROM commentaire WHERE joueur_id = ? AND date_commentaire = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, joueurId);
            statement.setDate(2, Date.valueOf(LocalDate.now()));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        return 0;
    }

    private int countPatternMatches(Pattern pattern, String value) {
        if (pattern == null || value == null || value.isBlank()) {
            return 0;
        }

        int count = 0;
        var matcher = pattern.matcher(value);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private void blockUserForSpam(Integer userId) throws SQLException {
        String query = "UPDATE `user` SET statut = 'BLOCKED', updated_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private void fillStatement(PreparedStatement statement, Commentaire commentaire) throws SQLException {
        statement.setString(1, commentaire.getContenu());
        statement.setDate(2, Date.valueOf(commentaire.getDateCommentaire()));
        if (commentaire.getJoueurId() != null) {
            statement.setInt(3, commentaire.getJoueurId());
        } else {
            statement.setNull(3, Types.INTEGER);
        }
        if (commentaire.getAnnonceId() != null) {
            statement.setInt(4, commentaire.getAnnonceId());
        } else {
            statement.setNull(4, Types.INTEGER);
        }
        statement.setString(5, commentaire.getAuteurAnonyme());
        statement.setInt(6, commentaire.getNbLikes());
        statement.setString(7, commentaire.getModerationStatus());
        statement.setString(8, commentaire.getModerationReason());
    }

    private void validateReferences(Commentaire commentaire) throws SQLException {
        if (commentaire.getAnnonceId() != null && !recordExists("annonce", commentaire.getAnnonceId())) {
            throw new SQLException("The selected announcement does not exist.");
        }

        if (commentaire.getJoueurId() != null) {
            boolean joueurExists = tableExists("joueur") && recordExists("joueur", commentaire.getJoueurId());
            String userRoles = tableExists("user") ? getUserRoles(commentaire.getJoueurId()) : null;
            boolean userExists = userRoles != null;
            if (!joueurExists && !userExists) {
                throw new SQLException("The selected player does not exist.");
            }
            if (userExists && !UserRoles.hasRole(userRoles, UserRoles.ROLE_JOUEUR)) {
                throw new SQLException("Only player accounts can add comments.");
            }
        }
    }

    private boolean areCommentsEnabled(int annonceId) {
        String query = "SELECT comments_enabled FROM annonce WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, annonceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    boolean enabled = resultSet.getBoolean("comments_enabled");
                    return resultSet.wasNull() || enabled;
                }
            }
        } catch (SQLException ignored) {
            return true;
        }
        return true;
    }

    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, null)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getTables(null, null, tableName.toUpperCase(), null)) {
            return resultSet.next();
        }
    }

    private boolean recordExists(String tableName, int id) throws SQLException {
        String query = "SELECT id FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String getUserRoles(int userId) throws SQLException {
        String query = "SELECT roles FROM user WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("roles");
                }
            }
        }
        return null;
    }

    private List<Commentaire> executeListQuery(String query) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                commentaires.add(mapRow(resultSet));
            }
        }
        return commentaires;
    }

    private int executeCountQuery(String query) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    private Commentaire mapRow(ResultSet resultSet) throws SQLException {
        Integer joueurId = resultSet.getInt("joueur_id");
        if (resultSet.wasNull()) {
            joueurId = null;
        }

        Integer annonceId = resultSet.getInt("annonce_id");
        if (resultSet.wasNull()) {
            annonceId = null;
        }

        Date commentDate = resultSet.getDate("date_commentaire");
        return new Commentaire(
                resultSet.getInt("id"),
                resultSet.getString("contenu"),
                commentDate != null ? commentDate.toLocalDate() : null,
                joueurId,
                annonceId,
                resultSet.getString("auteur_anonyme"),
                resultSet.getInt("nb_likes"),
                resultSet.getString("moderation_status"),
                resultSet.getString("moderation_reason")
        );
    }
}

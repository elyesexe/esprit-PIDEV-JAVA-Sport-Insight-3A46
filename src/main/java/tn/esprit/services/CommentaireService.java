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
<<<<<<< HEAD
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class CommentaireService implements IService<Commentaire> {
    public static final String REACTION_LIKE = "LIKE";
    public static final String REACTION_DISLIKE = "DISLIKE";

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d[\\d\\s\\-]{7,}\\d");
    private static final Pattern REPEATED_CHAR_PATTERN = Pattern.compile("(.)\\1{6,}");
    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final List<String> SPAM_KEYWORDS = List.of(
            "bit.ly", "t.me", "telegram", "whatsapp", "casino", "crypto",
            "invest", "argent facile", "promo", "gratuit", "click here", "dm me",
            "contact me", "signal", "forex", "nft"
    );

=======
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentaireService implements IService<Commentaire> {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private final Connection connection;

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

<<<<<<< HEAD
        applySpamModeration(commentaire);

        String query = """
                INSERT INTO commentaire (
                    contenu, date_commentaire, joueur_id, annonce_id,
                    auteur_anonyme, cv_name, cv_title, nb_likes, nb_dislikes, moderation_status, moderation_reason,
                    author_user_id, author_role
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
=======
        String query = """
                INSERT INTO commentaire (
                    contenu, date_commentaire, joueur_id, annonce_id,
                    auteur_anonyme, nb_likes, moderation_status, moderation_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
                    auteur_anonyme = ?, cv_name = ?, cv_title = ?, nb_likes = ?, nb_dislikes = ?, moderation_status = ?, moderation_reason = ?,
                    author_user_id = ?, author_role = ?
=======
                    auteur_anonyme = ?, nb_likes = ?, moderation_status = ?, moderation_reason = ?
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, commentaire);
<<<<<<< HEAD
            statement.setInt(14, commentaire.getId());
=======
            statement.setInt(9, commentaire.getId());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
<<<<<<< HEAD
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM comment_reaction WHERE commentaire_id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // Older databases may not have reaction tables yet.
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM comment_favorite WHERE commentaire_id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // Older databases may not have favorite tables yet.
        }
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
                   OR LOWER(COALESCE(cv_title, '')) LIKE LOWER(?)
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                ORDER BY date_commentaire DESC, id DESC
                """;
        String pattern = "%" + keyword + "%";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
<<<<<<< HEAD
            statement.setString(4, pattern);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        List<Integer> commentIds = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement("SELECT id FROM commentaire WHERE annonce_id = ?")) {
            select.setInt(1, annonceId);
            try (ResultSet resultSet = select.executeQuery()) {
                while (resultSet.next()) {
                    commentIds.add(resultSet.getInt("id"));
                }
            }
        }

        for (Integer commentId : commentIds) {
            delete(commentId);
        }
    }

    public Map<Integer, String> getReactionMapForUser(Integer userId) throws SQLException {
        if (userId == null) {
            return Map.of();
        }
        Map<Integer, String> reactions = new HashMap<>();
        String query = "SELECT commentaire_id, reaction_type FROM comment_reaction WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reactions.put(resultSet.getInt("commentaire_id"), resultSet.getString("reaction_type"));
                }
            }
        }
        return reactions;
    }

    public Set<Integer> getFavoriteCommentIdsByUser(Integer userId) throws SQLException {
        if (userId == null) {
            return Set.of();
        }
        Set<Integer> ids = new LinkedHashSet<>();
        String query = "SELECT commentaire_id FROM comment_favorite WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("commentaire_id"));
                }
            }
        }
        return ids;
    }

    public void reactToComment(Integer commentaireId, Integer userId, String reactionType) throws SQLException {
        if (commentaireId == null || userId == null) {
            throw new SQLException("Missing comment/user for reaction.");
        }
        String normalizedReaction = normalizeReaction(reactionType);
        if (normalizedReaction == null) {
            throw new SQLException("Unknown reaction type.");
        }

        String existingReaction = null;
        String selectQuery = "SELECT reaction_type FROM comment_reaction WHERE user_id = ? AND commentaire_id = ?";
        try (PreparedStatement select = connection.prepareStatement(selectQuery)) {
            select.setInt(1, userId);
            select.setInt(2, commentaireId);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    existingReaction = resultSet.getString("reaction_type");
                }
            }
        }

        if (normalizedReaction.equalsIgnoreCase(existingReaction)) {
            removeReaction(commentaireId, userId, normalizedReaction);
            return;
        }

        if (existingReaction != null) {
            decrementReactionCounter(commentaireId, existingReaction);
            updateReaction(commentaireId, userId, normalizedReaction);
        } else {
            insertReaction(commentaireId, userId, normalizedReaction);
        }
        incrementReactionCounter(commentaireId, normalizedReaction);
    }

    public boolean toggleFavorite(Integer commentaireId, Integer userId) throws SQLException {
        if (commentaireId == null || userId == null) {
            throw new SQLException("Missing comment/user for favorite.");
        }
        String existsQuery = "SELECT 1 FROM comment_favorite WHERE user_id = ? AND commentaire_id = ?";
        try (PreparedStatement exists = connection.prepareStatement(existsQuery)) {
            exists.setInt(1, userId);
            exists.setInt(2, commentaireId);
            try (ResultSet resultSet = exists.executeQuery()) {
                if (resultSet.next()) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM comment_favorite WHERE user_id = ? AND commentaire_id = ?")) {
                        delete.setInt(1, userId);
                        delete.setInt(2, commentaireId);
                        delete.executeUpdate();
                    }
                    return false;
                }
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO comment_favorite (user_id, commentaire_id) VALUES (?, ?)")) {
            insert.setInt(1, userId);
            insert.setInt(2, commentaireId);
            insert.executeUpdate();
        }
        return true;
    }

    private void applySpamModeration(Commentaire commentaire) throws SQLException {
        SpamDetectionResult spamDetection = detectSpam(commentaire);
        if (spamDetection.spam()) {
            boolean privateConversation = "PRIVATE".equalsIgnoreCase(
                    commentaire.getModerationStatus() == null ? null : commentaire.getModerationStatus().trim()
            );
            if (!privateConversation) {
                commentaire.setModerationStatus("REJECTED");
            }
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
=======
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM commentaire WHERE annonce_id = ?")) {
            statement.setInt(1, annonceId);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        statement.setString(6, commentaire.getCvName());
        statement.setString(7, commentaire.getCvTitle());
        statement.setInt(8, commentaire.getNbLikes());
        statement.setInt(9, commentaire.getNbDislikes());
        statement.setString(10, commentaire.getModerationStatus());
        statement.setString(11, commentaire.getModerationReason());
        if (commentaire.getAuthorUserId() != null) {
            statement.setInt(12, commentaire.getAuthorUserId());
        } else {
            statement.setNull(12, Types.INTEGER);
        }
        statement.setString(13, commentaire.getAuthorRole());
=======
        statement.setInt(6, commentaire.getNbLikes());
        statement.setString(7, commentaire.getModerationStatus());
        statement.setString(8, commentaire.getModerationReason());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private void validateReferences(Commentaire commentaire) throws SQLException {
        if (commentaire.getAnnonceId() != null && !recordExists("annonce", commentaire.getAnnonceId())) {
            throw new SQLException("The selected announcement does not exist.");
        }

<<<<<<< HEAD
        if (commentaire.getAuthorUserId() != null) {
            String userRoles = tableExists("user") ? getUserRoles(commentaire.getAuthorUserId()) : null;
            if (userRoles == null) {
                throw new SQLException("The selected message author does not exist.");
            }
            if (!UserRoles.hasRole(userRoles, UserRoles.ROLE_JOUEUR)
                    && !UserRoles.hasRole(userRoles, UserRoles.ROLE_USER)
                    && !UserRoles.hasRole(userRoles, UserRoles.ROLE_ENTRAINEUR)
                    && !UserRoles.hasRole(userRoles, UserRoles.ROLE_ADMIN)) {
                throw new SQLException("The selected message author cannot participate in announcement chat.");
            }
        }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        if (commentaire.getJoueurId() != null) {
            boolean joueurExists = tableExists("joueur") && recordExists("joueur", commentaire.getJoueurId());
            String userRoles = tableExists("user") ? getUserRoles(commentaire.getJoueurId()) : null;
            boolean userExists = userRoles != null;
            if (!joueurExists && !userExists) {
                throw new SQLException("The selected player does not exist.");
            }
<<<<<<< HEAD
            if (userExists
                    && !UserRoles.hasRole(userRoles, UserRoles.ROLE_JOUEUR)
                    && !UserRoles.hasRole(userRoles, UserRoles.ROLE_USER)) {
=======
            if (userExists && !UserRoles.hasRole(userRoles, UserRoles.ROLE_JOUEUR)) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        Integer joueurId = getNullableInt(resultSet, "joueur_id");
        Integer annonceId = getNullableInt(resultSet, "annonce_id");

        Date commentDate = resultSet.getDate("date_commentaire");
        Commentaire commentaire = new Commentaire(
=======
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
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                resultSet.getInt("id"),
                resultSet.getString("contenu"),
                commentDate != null ? commentDate.toLocalDate() : null,
                joueurId,
                annonceId,
                resultSet.getString("auteur_anonyme"),
<<<<<<< HEAD
                getNullableString(resultSet, "cv_name"),
                getNullableString(resultSet, "cv_title"),
                resultSet.getInt("nb_likes"),
                resultSet.getString("moderation_status"),
                resultSet.getString("moderation_reason"),
                getNullableInt(resultSet, "author_user_id"),
                getNullableString(resultSet, "author_role")
        );
        Integer dislikes = getNullableInt(resultSet, "nb_dislikes");
        commentaire.setNbDislikes(dislikes == null ? 0 : dislikes);
        return commentaire;
    }

    private void insertReaction(Integer commentaireId, Integer userId, String reactionType) throws SQLException {
        String query = "INSERT INTO comment_reaction (user_id, commentaire_id, reaction_type) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, commentaireId);
            statement.setString(3, reactionType);
            statement.executeUpdate();
        }
    }

    private void updateReaction(Integer commentaireId, Integer userId, String reactionType) throws SQLException {
        String query = "UPDATE comment_reaction SET reaction_type = ? WHERE user_id = ? AND commentaire_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, reactionType);
            statement.setInt(2, userId);
            statement.setInt(3, commentaireId);
            statement.executeUpdate();
        }
    }

    private void removeReaction(Integer commentaireId, Integer userId, String reactionType) throws SQLException {
        String query = "DELETE FROM comment_reaction WHERE user_id = ? AND commentaire_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, commentaireId);
            statement.executeUpdate();
        }
        decrementReactionCounter(commentaireId, reactionType);
    }

    private void incrementReactionCounter(Integer commentaireId, String reactionType) throws SQLException {
        String column = REACTION_DISLIKE.equalsIgnoreCase(reactionType) ? "nb_dislikes" : "nb_likes";
        String query = "UPDATE commentaire SET " + column + " = " + column + " + 1 WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, commentaireId);
            statement.executeUpdate();
        }
    }

    private void decrementReactionCounter(Integer commentaireId, String reactionType) throws SQLException {
        String column = REACTION_DISLIKE.equalsIgnoreCase(reactionType) ? "nb_dislikes" : "nb_likes";
        String query = "UPDATE commentaire SET " + column + " = CASE WHEN " + column + " > 0 THEN " + column + " - 1 ELSE 0 END WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, commentaireId);
            statement.executeUpdate();
        }
    }

    private String normalizeReaction(String reactionType) {
        if (reactionType == null) {
            return null;
        }
        String normalized = reactionType.trim().toUpperCase(Locale.ROOT);
        return REACTION_LIKE.equals(normalized) || REACTION_DISLIKE.equals(normalized) ? normalized : null;
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        try {
            int value = resultSet.getInt(columnName);
            return resultSet.wasNull() ? null : value;
        } catch (SQLException ignored) {
            return null;
        }
    }

    private String getNullableString(ResultSet resultSet, String columnName) throws SQLException {
        try {
            return resultSet.getString(columnName);
        } catch (SQLException ignored) {
            return null;
        }
=======
                resultSet.getInt("nb_likes"),
                resultSet.getString("moderation_status"),
                resultSet.getString("moderation_reason")
        );
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }
}

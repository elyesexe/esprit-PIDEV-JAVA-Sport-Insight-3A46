package tn.esprit.services;

import tn.esprit.entities.Commentaire;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    @Override
    public void add(Commentaire commentaire) throws SQLException {
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

        String query = "INSERT INTO commentaire (contenu, date_commentaire, joueur_id, annonce_id, auteur_anonyme, nb_likes, moderation_status, moderation_reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, commentaire.getContenu());
        statement.setDate(2, java.sql.Date.valueOf(commentaire.getDateCommentaire()));
        if (commentaire.getJoueurId() != null) {
            statement.setInt(3, commentaire.getJoueurId());
        } else {
            statement.setNull(3, java.sql.Types.INTEGER);
        }
        if (commentaire.getAnnonceId() != null) {
            statement.setInt(4, commentaire.getAnnonceId());
        } else {
            statement.setNull(4, java.sql.Types.INTEGER);
        }
        statement.setString(5, commentaire.getAuteurAnonyme());
        statement.setInt(6, commentaire.getNbLikes());
        statement.setString(7, commentaire.getModerationStatus());
        statement.setString(8, commentaire.getModerationReason());
        statement.executeUpdate();
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
        long uniqueWords = java.util.Arrays.stream(tokens)
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

        boolean spam = score >= 4 || hardSignals >= 1;
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
                return 0;
            }
        }
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
                return 0;
            }
        }
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
            statement.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Commentaire commentaire) throws SQLException {
        String query = "UPDATE commentaire SET contenu = ?, date_commentaire = ?, joueur_id = ?, annonce_id = ?, auteur_anonyme = ?, nb_likes = ?, moderation_status = ?, moderation_reason = ? WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, commentaire.getContenu());
        statement.setDate(2, java.sql.Date.valueOf(commentaire.getDateCommentaire()));
        if (commentaire.getJoueurId() != null) {
            statement.setInt(3, commentaire.getJoueurId());
        } else {
            statement.setNull(3, java.sql.Types.INTEGER);
        }
        if (commentaire.getAnnonceId() != null) {
            statement.setInt(4, commentaire.getAnnonceId());
        } else {
            statement.setNull(4, java.sql.Types.INTEGER);
        }
        statement.setString(5, commentaire.getAuteurAnonyme());
        statement.setInt(6, commentaire.getNbLikes());
        statement.setString(7, commentaire.getModerationStatus());
        statement.setString(8, commentaire.getModerationReason());
        statement.setInt(9, commentaire.getId());
        statement.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM commentaire WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        statement.executeUpdate();
    }

    @Override
    public List<Commentaire> getAll() throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = "SELECT * FROM commentaire";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            Commentaire commentaire = new Commentaire(
                    resultSet.getInt("id"),
                    resultSet.getString("contenu"),
                    resultSet.getDate("date_commentaire").toLocalDate(),
                    resultSet.getInt("joueur_id"),
                    resultSet.getInt("annonce_id"),
                    resultSet.getString("auteur_anonyme"),
                    resultSet.getInt("nb_likes"),
                    resultSet.getString("moderation_status"),
                    resultSet.getString("moderation_reason")
            );
            commentaires.add(commentaire);
        }
        return commentaires;
    }

    @Override
    public Commentaire getById(int id) throws SQLException {
        String query = "SELECT * FROM commentaire WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return new Commentaire(
                    resultSet.getInt("id"),
                    resultSet.getString("contenu"),
                    resultSet.getDate("date_commentaire").toLocalDate(),
                    resultSet.getInt("joueur_id"),
                    resultSet.getInt("annonce_id"),
                    resultSet.getString("auteur_anonyme"),
                    resultSet.getInt("nb_likes"),
                    resultSet.getString("moderation_status"),
                    resultSet.getString("moderation_reason")
            );
        }
        return null;
    }

    /**
     * Récupère tous les commentaires d'une annonce
     */
    public List<Commentaire> getCommentairesByAnnonce(int annonceId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = "SELECT * FROM commentaire WHERE annonce_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, annonceId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Commentaire commentaire = new Commentaire(
                    resultSet.getInt("id"),
                    resultSet.getString("contenu"),
                    resultSet.getDate("date_commentaire").toLocalDate(),
                    resultSet.getInt("joueur_id"),
                    resultSet.getInt("annonce_id"),
                    resultSet.getString("auteur_anonyme"),
                    resultSet.getInt("nb_likes"),
                    resultSet.getString("moderation_status"),
                    resultSet.getString("moderation_reason")
            );
            commentaires.add(commentaire);
        }
        return commentaires;
    }

    /**
     * Récupère les commentaires d'un joueur
     */
    public List<Commentaire> getCommentairesByJoueur(int joueurId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = "SELECT * FROM commentaire WHERE joueur_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, joueurId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Commentaire commentaire = new Commentaire(
                    resultSet.getInt("id"),
                    resultSet.getString("contenu"),
                    resultSet.getDate("date_commentaire").toLocalDate(),
                    resultSet.getInt("joueur_id"),
                    resultSet.getInt("annonce_id"),
                    resultSet.getString("auteur_anonyme"),
                    resultSet.getInt("nb_likes"),
                    resultSet.getString("moderation_status"),
                    resultSet.getString("moderation_reason")
            );
            commentaires.add(commentaire);
        }
        return commentaires;
    }
}


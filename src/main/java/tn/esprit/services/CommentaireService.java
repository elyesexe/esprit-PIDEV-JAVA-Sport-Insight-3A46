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
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CommentaireService implements IService<Commentaire> {
    public static final String REACTION_LIKE = "LIKE";
    public static final String REACTION_DISLIKE = "DISLIKE";
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

        String query = """
                INSERT INTO commentaire (
                    contenu, date_commentaire, joueur_id, annonce_id,
                    auteur_anonyme, cv_name, cv_title, nb_likes, nb_dislikes, moderation_status, moderation_reason,
                    author_user_id, author_role
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    auteur_anonyme = ?, cv_name = ?, cv_title = ?, nb_likes = ?, nb_dislikes = ?, moderation_status = ?, moderation_reason = ?,
                    author_user_id = ?, author_role = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, commentaire);
            statement.setInt(14, commentaire.getId());
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
                   OR LOWER(COALESCE(cv_title, '')) LIKE LOWER(?)
                ORDER BY date_commentaire DESC, id DESC
                """;
        String pattern = "%" + keyword + "%";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
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
    }

    private void validateReferences(Commentaire commentaire) throws SQLException {
        if (commentaire.getAnnonceId() != null && !recordExists("annonce", commentaire.getAnnonceId())) {
            throw new SQLException("The selected announcement does not exist.");
        }

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

        if (commentaire.getJoueurId() != null) {
            boolean joueurExists = tableExists("joueur") && recordExists("joueur", commentaire.getJoueurId());
            String userRoles = tableExists("user") ? getUserRoles(commentaire.getJoueurId()) : null;
            boolean userExists = userRoles != null;
            if (!joueurExists && !userExists) {
                throw new SQLException("The selected player does not exist.");
            }
            if (userExists
                    && !UserRoles.hasRole(userRoles, UserRoles.ROLE_JOUEUR)
                    && !UserRoles.hasRole(userRoles, UserRoles.ROLE_USER)) {
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
        Commentaire commentaire = new Commentaire(
                resultSet.getInt("id"),
                resultSet.getString("contenu"),
                commentDate != null ? commentDate.toLocalDate() : null,
                joueurId,
                annonceId,
                resultSet.getString("auteur_anonyme"),
                resultSet.getString("cv_name"),
                resultSet.getString("cv_title"),
                resultSet.getInt("nb_likes"),
                resultSet.getString("moderation_status"),
                resultSet.getString("moderation_reason"),
                getNullableInt(resultSet, "author_user_id"),
                resultSet.getString("author_role")
        );
        try {
            commentaire.setNbDislikes(resultSet.getInt("nb_dislikes"));
        } catch (SQLException ignored) {
            commentaire.setNbDislikes(0);
        }
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
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }
}

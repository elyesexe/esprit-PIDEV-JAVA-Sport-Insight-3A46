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
import java.util.List;
import java.util.Map;

public class CommentaireService implements IService<Commentaire> {
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

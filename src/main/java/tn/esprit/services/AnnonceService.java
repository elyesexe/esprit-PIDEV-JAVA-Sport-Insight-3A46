package tn.esprit.services;

import tn.esprit.entities.Annonce;
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
import java.util.List;

public class AnnonceService implements IService<Annonce> {
    private final Connection connection;

    public AnnonceService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public AnnonceService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void add(Annonce annonce) throws SQLException {
        validateCoachAuthor(annonce);
        String query = """
                INSERT INTO annonce (
                    titre, description, poste_recherche, niveau_requis,
                    date_publication, statut, entraineur_id, comments_enabled, urgent
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, annonce, false);
            statement.executeUpdate();
        } catch (SQLException ignored) {
            String fallback = """
                    INSERT INTO annonce (
                        titre, description, poste_recherche, niveau_requis,
                        date_publication, statut, entraineur_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(fallback)) {
                fillStatement(statement, annonce, true);
                statement.executeUpdate();
            }
        }
    }

    @Override
    public void update(Annonce annonce) throws SQLException {
        validateCoachAuthor(annonce);
        String query = """
                UPDATE annonce
                SET titre = ?, description = ?, poste_recherche = ?, niveau_requis = ?,
                    date_publication = ?, statut = ?, entraineur_id = ?, comments_enabled = ?, urgent = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, annonce, false);
            statement.setInt(10, annonce.getId());
            statement.executeUpdate();
        } catch (SQLException ignored) {
            String fallback = """
                    UPDATE annonce
                    SET titre = ?, description = ?, poste_recherche = ?, niveau_requis = ?,
                        date_publication = ?, statut = ?, entraineur_id = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(fallback)) {
                fillStatement(statement, annonce, true);
                statement.setInt(8, annonce.getId());
                statement.executeUpdate();
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (PreparedStatement deleteCommentaires = connection.prepareStatement("DELETE FROM commentaire WHERE annonce_id = ?")) {
            deleteCommentaires.setInt(1, id);
            deleteCommentaires.executeUpdate();
        } catch (SQLException ignored) {
            // Older local databases might not have the comments table yet.
        }

        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM annonce WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Annonce> getAll() throws SQLException {
        return executeListQuery("SELECT * FROM annonce ORDER BY urgent DESC, date_publication DESC, id DESC");
    }

    @Override
    public Annonce getById(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM annonce WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        }
        return null;
    }

    public List<Annonce> getAnnoncesByEntraineur(int entraineurId) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE entraineur_id = ? ORDER BY urgent DESC, date_publication DESC, id DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, entraineurId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    annonces.add(mapRow(resultSet));
                }
            }
        }
        return annonces;
    }

    public List<Annonce> getAnnoncesActives() throws SQLException {
        return executeListQuery("SELECT * FROM annonce WHERE statut = 'ACTIVE' ORDER BY urgent DESC, date_publication DESC, id DESC");
    }

    public List<Annonce> getAnnoncesByPoste(String poste) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE poste_recherche = ? ORDER BY urgent DESC, date_publication DESC, id DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, poste);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    annonces.add(mapRow(resultSet));
                }
            }
        }
        return annonces;
    }

    @Override
    public List<Annonce> search(String keyword) throws SQLException {
        return searchByTitre(keyword);
    }

    public List<Annonce> searchByTitre(String titre) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = """
                SELECT * FROM annonce
                WHERE LOWER(titre) LIKE LOWER(?)
                   OR LOWER(description) LIKE LOWER(?)
                   OR LOWER(poste_recherche) LIKE LOWER(?)
                ORDER BY urgent DESC, date_publication DESC, id DESC
                """;
        String pattern = "%" + titre + "%";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    annonces.add(mapRow(resultSet));
                }
            }
        }
        return annonces;
    }

    public List<Annonce> searchByDatePublication(java.time.LocalDate date) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE date_publication = ? ORDER BY urgent DESC, id DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDate(1, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    annonces.add(mapRow(resultSet));
                }
            }
        }
        return annonces;
    }

    public List<Annonce> searchByTitreAndDate(String titre, java.time.LocalDate date) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = """
                SELECT * FROM annonce
                WHERE LOWER(titre) LIKE LOWER(?)
                  AND date_publication = ?
                ORDER BY urgent DESC, id DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "%" + titre + "%");
            statement.setDate(2, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    annonces.add(mapRow(resultSet));
                }
            }
        }
        return annonces;
    }

    public int countAll() throws SQLException {
        return executeCountQuery("SELECT COUNT(*) FROM annonce");
    }

    public int countActive() throws SQLException {
        return executeCountQuery("SELECT COUNT(*) FROM annonce WHERE statut = 'ACTIVE'");
    }

    public int countUrgent() throws SQLException {
        return executeCountQuery("SELECT COUNT(*) FROM annonce WHERE urgent = TRUE");
    }

    private void validateCoachAuthor(Annonce annonce) throws SQLException {
        if (annonce == null || annonce.getEntraineurId() == null) {
            throw new SQLException("Only coach accounts can create or update announcements.");
        }
        if (!tableExists("user")) {
            return;
        }
        String role = getUserRoles(annonce.getEntraineurId());
        if (role == null) {
            throw new SQLException("The selected coach account does not exist.");
        }
        if (!UserRoles.hasRole(role, UserRoles.ROLE_ENTRAINEUR)) {
            throw new SQLException("Only coach accounts can create or update announcements.");
        }
    }

    private void fillStatement(PreparedStatement statement, Annonce annonce, boolean fallbackMode) throws SQLException {
        statement.setString(1, annonce.getTitre());
        statement.setString(2, annonce.getDescription());
        statement.setString(3, annonce.getPosteRecherche());
        statement.setString(4, annonce.getNiveauRequis());
        statement.setDate(5, Date.valueOf(annonce.getDatePublication()));
        statement.setString(6, annonce.getStatut());
        if (annonce.getEntraineurId() != null) {
            statement.setInt(7, annonce.getEntraineurId());
        } else {
            statement.setNull(7, Types.INTEGER);
        }

        if (!fallbackMode) {
            statement.setBoolean(8, annonce.getCommentsEnabled() == null || annonce.getCommentsEnabled());
            statement.setBoolean(9, annonce.getUrgent() != null && annonce.getUrgent());
        }
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

    private List<Annonce> executeListQuery(String query) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                annonces.add(mapRow(resultSet));
            }
        }
        return annonces;
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

    private Annonce mapRow(ResultSet resultSet) throws SQLException {
        Date publicationDate = resultSet.getDate("date_publication");

        Integer entraineurId = resultSet.getInt("entraineur_id");
        if (resultSet.wasNull()) {
            entraineurId = null;
        }

        Boolean commentsEnabled = null;
        try {
            commentsEnabled = resultSet.getBoolean("comments_enabled");
            if (resultSet.wasNull()) {
                commentsEnabled = null;
            }
        } catch (SQLException ignored) {
            commentsEnabled = null;
        }

        Boolean urgent = null;
        try {
            urgent = resultSet.getBoolean("urgent");
            if (resultSet.wasNull()) {
                urgent = null;
            }
        } catch (SQLException ignored) {
            urgent = null;
        }

        return new Annonce(
                resultSet.getInt("id"),
                resultSet.getString("titre"),
                resultSet.getString("description"),
                resultSet.getString("poste_recherche"),
                resultSet.getString("niveau_requis"),
                publicationDate != null ? publicationDate.toLocalDate() : null,
                resultSet.getString("statut"),
                entraineurId,
                commentsEnabled,
                urgent
        );
    }
}

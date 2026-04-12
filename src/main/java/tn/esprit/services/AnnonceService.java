package tn.esprit.services;

import tn.esprit.entities.Annonce;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnonceService implements IService<Annonce> {
    private final Connection connection;

    public AnnonceService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }

    // Added for testability: allow creating service with a provided Connection (e.g. H2 in-memory)
    public AnnonceService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Map a ResultSet row to an Annonce object, safely reading new optional columns.
     */
    private Annonce mapRowToAnnonce(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String titre = rs.getString("titre");
        String description = rs.getString("description");
        String poste = rs.getString("poste_recherche");
        String niveau = rs.getString("niveau_requis");
        java.time.LocalDate datePub = null;
        Date d = rs.getDate("date_publication");
        if (d != null) datePub = d.toLocalDate();
        String statut = rs.getString("statut");
        Integer entraineurId = null;
        try {
            int eid = rs.getInt("entraineur_id");
            if (!rs.wasNull()) entraineurId = eid;
        } catch (SQLException ex) {
            entraineurId = null;
        }

        // optional columns
        Boolean commentsEnabled = null;
        Boolean urgent = null;
        try {
            commentsEnabled = rs.getBoolean("comments_enabled");
            if (rs.wasNull()) commentsEnabled = null;
        } catch (SQLException ex) {
            commentsEnabled = null;
        }
        try {
            urgent = rs.getBoolean("urgent");
            if (rs.wasNull()) urgent = null;
        } catch (SQLException ex) {
            urgent = null;
        }

        return new Annonce(id, titre, description, poste, niveau, datePub, statut, entraineurId, commentsEnabled, urgent);
    }

    @Override
    public void add(Annonce annonce) throws SQLException {
        String query = "INSERT INTO annonce (titre, description, poste_recherche, niveau_requis, date_publication, statut, entraineur_id, comments_enabled, urgent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, annonce.getTitre());
            statement.setString(2, annonce.getDescription());
            statement.setString(3, annonce.getPosteRecherche());
            statement.setString(4, annonce.getNiveauRequis());
            statement.setDate(5, java.sql.Date.valueOf(annonce.getDatePublication()));
            statement.setString(6, annonce.getStatut());
            if (annonce.getEntraineurId() != null) {
                statement.setInt(7, annonce.getEntraineurId());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            // comments_enabled and urgent
            statement.setBoolean(8, annonce.getCommentsEnabled() != null ? annonce.getCommentsEnabled() : true);
            statement.setBoolean(9, annonce.getUrgent() != null ? annonce.getUrgent() : false);
            statement.executeUpdate();
        } catch (SQLException ex) {
            // Fallback for databases that don't have new columns yet: try insert without comments_enabled/urgent
            String fallback = "INSERT INTO annonce (titre, description, poste_recherche, niveau_requis, date_publication, statut, entraineur_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt2 = connection.prepareStatement(fallback);
            stmt2.setString(1, annonce.getTitre());
            stmt2.setString(2, annonce.getDescription());
            stmt2.setString(3, annonce.getPosteRecherche());
            stmt2.setString(4, annonce.getNiveauRequis());
            stmt2.setDate(5, java.sql.Date.valueOf(annonce.getDatePublication()));
            stmt2.setString(6, annonce.getStatut());
            if (annonce.getEntraineurId() != null) {
                stmt2.setInt(7, annonce.getEntraineurId());
            } else {
                stmt2.setNull(7, java.sql.Types.INTEGER);
            }
            stmt2.executeUpdate();
        }
    }

    @Override
    public void update(Annonce annonce) throws SQLException {
        String query = "UPDATE annonce SET titre = ?, description = ?, poste_recherche = ?, niveau_requis = ?, date_publication = ?, statut = ?, entraineur_id = ?, comments_enabled = ?, urgent = ? WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, annonce.getTitre());
            statement.setString(2, annonce.getDescription());
            statement.setString(3, annonce.getPosteRecherche());
            statement.setString(4, annonce.getNiveauRequis());
            statement.setDate(5, java.sql.Date.valueOf(annonce.getDatePublication()));
            statement.setString(6, annonce.getStatut());
            if (annonce.getEntraineurId() != null) {
                statement.setInt(7, annonce.getEntraineurId());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            statement.setBoolean(8, annonce.getCommentsEnabled() != null ? annonce.getCommentsEnabled() : true);
            statement.setBoolean(9, annonce.getUrgent() != null ? annonce.getUrgent() : false);
            statement.setInt(10, annonce.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            // Fallback: update without new columns
            String fallback = "UPDATE annonce SET titre = ?, description = ?, poste_recherche = ?, niveau_requis = ?, date_publication = ?, statut = ?, entraineur_id = ? WHERE id = ?";
            PreparedStatement stmt2 = connection.prepareStatement(fallback);
            stmt2.setString(1, annonce.getTitre());
            stmt2.setString(2, annonce.getDescription());
            stmt2.setString(3, annonce.getPosteRecherche());
            stmt2.setString(4, annonce.getNiveauRequis());
            stmt2.setDate(5, java.sql.Date.valueOf(annonce.getDatePublication()));
            stmt2.setString(6, annonce.getStatut());
            if (annonce.getEntraineurId() != null) {
                stmt2.setInt(7, annonce.getEntraineurId());
            } else {
                stmt2.setNull(7, java.sql.Types.INTEGER);
            }
            stmt2.setInt(8, annonce.getId());
            stmt2.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM annonce WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        statement.executeUpdate();
    }

    @Override
    public List<Annonce> getAll() throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            Annonce annonce = mapRowToAnnonce(resultSet);
            annonces.add(annonce);
        }
        return annonces;
    }

    @Override
    public Annonce getById(int id) throws SQLException {
        String query = "SELECT * FROM annonce WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return mapRowToAnnonce(resultSet);
        }
        return null;
    }

    /**
     * Récupère les annonces d'un entraîneur
     */
    public List<Annonce> getAnnoncesByEntraineur(int entraineurId) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE entraineur_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, entraineurId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Annonce annonce = mapRowToAnnonce(resultSet);
            annonces.add(annonce);
        }
        return annonces;
    }

    /**
     * Récupère les annonces actives
     */
    public List<Annonce> getAnnoncesActives() throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE statut = 'ACTIVE'";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            Annonce annonce = mapRowToAnnonce(resultSet);
            annonces.add(annonce);
        }
        return annonces;
    }

    /**
     * Récupère les annonces par poste de recherche
     */
    public List<Annonce> getAnnoncesByPoste(String poste) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE poste_recherche = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, poste);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Annonce annonce = mapRowToAnnonce(resultSet);
            annonces.add(annonce);
        }
        return annonces;
    }

    /**
     * Recherche les annonces par titre (recherche partielle avec LIKE)
     */
    public List<Annonce> searchByTitre(String titre) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE titre LIKE ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, "%" + titre + "%");
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Annonce annonce = mapRowToAnnonce(resultSet);
            annonces.add(annonce);
        }
        return annonces;
    }

    /**
     * Recherche les annonces par date de publication
     */
    public List<Annonce> searchByDatePublication(java.time.LocalDate date) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE date_publication = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setDate(1, java.sql.Date.valueOf(date));
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Annonce annonce = mapRowToAnnonce(resultSet);
            annonces.add(annonce);
        }
        return annonces;
    }

    /**
     * Recherche les annonces par titre ET date de publication
     */
    public List<Annonce> searchByTitreAndDate(String titre, java.time.LocalDate date) throws SQLException {
        List<Annonce> annonces = new ArrayList<>();
        String query = "SELECT * FROM annonce WHERE titre LIKE ? AND date_publication = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, "%" + titre + "%");
        statement.setDate(2, java.sql.Date.valueOf(date));
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Annonce annonce = mapRowToAnnonce(resultSet);
            annonces.add(annonce);
        }
        return annonces;
    }
}


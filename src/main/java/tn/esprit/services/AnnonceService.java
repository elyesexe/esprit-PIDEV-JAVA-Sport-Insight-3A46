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

    @Override
    public void add(Annonce annonce) throws SQLException {
        String query = "INSERT INTO annonce (titre, description, poste_recherche, niveau_requis, date_publication, statut, entraineur_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
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
        statement.executeUpdate();
    }

    @Override
    public void update(Annonce annonce) throws SQLException {
        String query = "UPDATE annonce SET titre = ?, description = ?, poste_recherche = ?, niveau_requis = ?, date_publication = ?, statut = ?, entraineur_id = ? WHERE id = ?";
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
        statement.setInt(8, annonce.getId());
        statement.executeUpdate();
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
            Annonce annonce = new Annonce(
                    resultSet.getInt("id"),
                    resultSet.getString("titre"),
                    resultSet.getString("description"),
                    resultSet.getString("poste_recherche"),
                    resultSet.getString("niveau_requis"),
                    resultSet.getDate("date_publication").toLocalDate(),
                    resultSet.getString("statut"),
                    resultSet.getInt("entraineur_id")
            );
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
            return new Annonce(
                    resultSet.getInt("id"),
                    resultSet.getString("titre"),
                    resultSet.getString("description"),
                    resultSet.getString("poste_recherche"),
                    resultSet.getString("niveau_requis"),
                    resultSet.getDate("date_publication").toLocalDate(),
                    resultSet.getString("statut"),
                    resultSet.getInt("entraineur_id")
            );
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
            Annonce annonce = new Annonce(
                    resultSet.getInt("id"),
                    resultSet.getString("titre"),
                    resultSet.getString("description"),
                    resultSet.getString("poste_recherche"),
                    resultSet.getString("niveau_requis"),
                    resultSet.getDate("date_publication").toLocalDate(),
                    resultSet.getString("statut"),
                    resultSet.getInt("entraineur_id")
            );
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
            Annonce annonce = new Annonce(
                    resultSet.getInt("id"),
                    resultSet.getString("titre"),
                    resultSet.getString("description"),
                    resultSet.getString("poste_recherche"),
                    resultSet.getString("niveau_requis"),
                    resultSet.getDate("date_publication").toLocalDate(),
                    resultSet.getString("statut"),
                    resultSet.getInt("entraineur_id")
            );
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
            Annonce annonce = new Annonce(
                    resultSet.getInt("id"),
                    resultSet.getString("titre"),
                    resultSet.getString("description"),
                    resultSet.getString("poste_recherche"),
                    resultSet.getString("niveau_requis"),
                    resultSet.getDate("date_publication").toLocalDate(),
                    resultSet.getString("statut"),
                    resultSet.getInt("entraineur_id")
            );
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
            Annonce annonce = new Annonce(
                    resultSet.getInt("id"),
                    resultSet.getString("titre"),
                    resultSet.getString("description"),
                    resultSet.getString("poste_recherche"),
                    resultSet.getString("niveau_requis"),
                    resultSet.getDate("date_publication").toLocalDate(),
                    resultSet.getString("statut"),
                    resultSet.getInt("entraineur_id")
            );
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
            Annonce annonce = new Annonce(
                    resultSet.getInt("id"),
                    resultSet.getString("titre"),
                    resultSet.getString("description"),
                    resultSet.getString("poste_recherche"),
                    resultSet.getString("niveau_requis"),
                    resultSet.getDate("date_publication").toLocalDate(),
                    resultSet.getString("statut"),
                    resultSet.getInt("entraineur_id")
            );
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
            Annonce annonce = new Annonce(
                    resultSet.getInt("id"),
                    resultSet.getString("titre"),
                    resultSet.getString("description"),
                    resultSet.getString("poste_recherche"),
                    resultSet.getString("niveau_requis"),
                    resultSet.getDate("date_publication").toLocalDate(),
                    resultSet.getString("statut"),
                    resultSet.getInt("entraineur_id")
            );
            annonces.add(annonce);
        }
        return annonces;
    }
}


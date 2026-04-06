package tn.esprit.services;

import tn.esprit.entities.Commentaire;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService implements IService<Commentaire> {
    private final Connection connection;

    public CommentaireService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Commentaire commentaire) throws SQLException {
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


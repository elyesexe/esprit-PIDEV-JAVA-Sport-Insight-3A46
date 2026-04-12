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

    // Added for testability: allow creating service with a provided Connection (e.g. H2 in-memory)
    public CommentaireService(Connection connection) {
        this.connection = connection;
    }

    private Commentaire mapRowToCommentaire(ResultSet resultSet) throws SQLException {
        Integer joueurId = resultSet.getInt("joueur_id");
        if (resultSet.wasNull()) {
            joueurId = null;
        }

        Integer annonceId = resultSet.getInt("annonce_id");
        if (resultSet.wasNull()) {
            annonceId = null;
        }

        return new Commentaire(
                resultSet.getInt("id"),
                resultSet.getString("contenu"),
                resultSet.getDate("date_commentaire").toLocalDate(),
                joueurId,
                annonceId,
                resultSet.getString("auteur_anonyme"),
                resultSet.getInt("nb_likes"),
                resultSet.getString("moderation_status"),
                resultSet.getString("moderation_reason")
        );
    }

    @Override
    public void add(Commentaire commentaire) throws SQLException {
        validateReferences(commentaire.getAnnonceId(), commentaire.getJoueurId());

        if (commentaire.getAnnonceId() != null) {
            try (PreparedStatement check = connection.prepareStatement("SELECT * FROM annonce WHERE id = ?")) {
                check.setInt(1, commentaire.getAnnonceId());
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && hasColumn(rs, "comments_enabled")) {
                        boolean enabled = rs.getBoolean("comments_enabled");
                        if (!rs.wasNull() && !enabled) {
                            throw new SQLException("Les commentaires sont fermes pour cette annonce.");
                        }
                    }
                }
            }
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

    private void validateReferences(Integer annonceId, Integer joueurId) throws SQLException {
        if (annonceId != null && !recordExists("annonce", annonceId)) {
            throw new SQLException("Annonce introuvable pour l'id " + annonceId + ".");
        }

        if (joueurId != null && tableExists("user") && !recordExists("user", joueurId)) {
            throw new SQLException("Joueur introuvable pour l'id " + joueurId + ".");
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), null)) {
            return tables.next();
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

    private boolean hasColumn(ResultSet resultSet, String columnName) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnName(i))) {
                return true;
            }
        }
        return false;
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
            Commentaire commentaire = mapRowToCommentaire(resultSet);
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
            return mapRowToCommentaire(resultSet);
        }
        return null;
    }

    public List<Commentaire> getCommentairesByAnnonce(int annonceId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = "SELECT * FROM commentaire WHERE annonce_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, annonceId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Commentaire commentaire = mapRowToCommentaire(resultSet);
            commentaires.add(commentaire);
        }
        return commentaires;
    }

    public List<Commentaire> getCommentairesByJoueur(int joueurId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = "SELECT * FROM commentaire WHERE joueur_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, joueurId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            Commentaire commentaire = mapRowToCommentaire(resultSet);
            commentaires.add(commentaire);
        }
        return commentaires;
    }

    public List<Commentaire> searchAdvanced(java.time.LocalDate datePublication, Integer joueurId) throws SQLException {
        List<Commentaire> commentaires = new ArrayList<>();
        StringBuilder query = new StringBuilder(
                "SELECT c.* FROM commentaire c JOIN annonce a ON c.annonce_id = a.id WHERE 1=1"
        );

        if (datePublication != null) {
            query.append(" AND a.date_publication = ?");
        }
        if (joueurId != null) {
            query.append(" AND c.joueur_id = ?");
        }
        query.append(" ORDER BY a.date_publication DESC, c.joueur_id ASC, c.id ASC");

        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            int parameterIndex = 1;
            if (datePublication != null) {
                statement.setDate(parameterIndex++, java.sql.Date.valueOf(datePublication));
            }
            if (joueurId != null) {
                statement.setInt(parameterIndex++, joueurId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    commentaires.add(mapRowToCommentaire(resultSet));
                }
            }
        }

        return commentaires;
    }

    public int countCommentairesByAnnonce(int annonceId) throws SQLException {
        String query = "SELECT COUNT(*) AS cnt FROM commentaire WHERE annonce_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, annonceId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        }
        return 0;
    }

    public java.util.Map<Integer, Integer> countCommentairesGroupByAnnonce() throws SQLException {
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        String query = "SELECT annonce_id, COUNT(*) AS cnt FROM commentaire GROUP BY annonce_id";
        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int annonceId = rs.getInt("annonce_id");
                int cnt = rs.getInt("cnt");
                map.put(annonceId, cnt);
            }
        }
        return map;
    }
}

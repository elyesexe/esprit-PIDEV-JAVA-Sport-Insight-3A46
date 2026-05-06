package tn.esprit.services;

import tn.esprit.entities.Joueur;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JoueurService implements IService<Joueur> {
    private final Connection connection;

    public JoueurService() throws SQLException {
        connection = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Joueur joueur) throws SQLException {
        String sql = "INSERT INTO joueur (nom, prenom, date_naissance, numero, image, equipe_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, joueur.getNom());
            statement.setString(2, joueur.getPrenom());
            statement.setDate(3, Date.valueOf(joueur.getDateNaissance()));
            statement.setInt(4, joueur.getNumero());
            statement.setString(5, joueur.getImage());
            if (joueur.getEquipeId() != null) {
                statement.setInt(6, joueur.getEquipeId());
            } else {
                statement.setNull(6, java.sql.Types.INTEGER);
            }
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Joueur joueur) throws SQLException {
        String sql = "UPDATE joueur SET nom = ?, prenom = ?, date_naissance = ?, numero = ?, image = ?, equipe_id = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, joueur.getNom());
            statement.setString(2, joueur.getPrenom());
            statement.setDate(3, Date.valueOf(joueur.getDateNaissance()));
            statement.setInt(4, joueur.getNumero());
            statement.setString(5, joueur.getImage());
            if (joueur.getEquipeId() != null) {
                statement.setInt(6, joueur.getEquipeId());
            } else {
                statement.setNull(6, java.sql.Types.INTEGER);
            }
            statement.setInt(7, joueur.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM joueur WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Joueur> getAll() throws SQLException {
        String sql = "SELECT id, nom, prenom, date_naissance, numero, image, equipe_id, external_api_id, external_source, position, nationalite FROM joueur";
        List<Joueur> joueurs = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                joueurs.add(mapRow(rs));
            }
        }

        return joueurs;
    }

    @Override
    public Joueur getById(int id) throws SQLException {
        String sql = "SELECT id, nom, prenom, date_naissance, numero, image, equipe_id, external_api_id, external_source, position, nationalite FROM joueur WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    public List<Joueur> getByEquipeId(int equipeId) throws SQLException {
        String sql = "SELECT id, nom, prenom, date_naissance, numero, image, equipe_id, external_api_id, external_source, position, nationalite FROM joueur WHERE equipe_id = ?";
        List<Joueur> joueurs = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipeId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    joueurs.add(mapRow(rs));
                }
            }
        }

        return joueurs;
    }

    public void updateImage(int joueurId, String imagePath) throws SQLException {
        String sql = "UPDATE joueur SET image = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, imagePath);
            statement.setInt(2, joueurId);
            statement.executeUpdate();
        }
    }

    private Joueur mapRow(ResultSet rs) throws SQLException {
        Date dateNaissance = rs.getDate("date_naissance");
        int equipeId = rs.getInt("equipe_id");
        Integer equipeValue = rs.wasNull() ? null : equipeId;
        Joueur joueur = new Joueur(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                dateNaissance != null ? dateNaissance.toLocalDate() : null,
                rs.getInt("numero"),
                rs.getString("image"),
                equipeValue
        );
        long externalApiId = rs.getLong("external_api_id");
        joueur.setExternalApiId(rs.wasNull() ? null : externalApiId);
        joueur.setExternalSource(rs.getString("external_source"));
        joueur.setPosition(rs.getString("position"));
        joueur.setNationalite(rs.getString("nationalite"));
        return joueur;
    }

    /**
     * Players currently assigned to a team ({@code equipe_id} is set).
     */
    public int countActivePlayers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM joueur WHERE equipe_id IS NOT NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /** Total rows in {@code joueur} (all registered players). */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM joueur";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}

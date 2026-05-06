package tn.esprit.services;

import tn.esprit.entities.Equipe;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EquipeService implements IService<Equipe> {
    private final Connection connection;

    public EquipeService() throws SQLException {
        connection = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Equipe equipe) throws SQLException {
        String sql = "INSERT INTO equipe (nom, coach, adresse, telephone, email, image, competition_code) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, equipe.getNom());
            setNullableString(statement, 2, equipe.getCoach());
            setNullableString(statement, 3, equipe.getAdresse());
            setNullableString(statement, 4, equipe.getTelephone());
            setNullableString(statement, 5, equipe.getEmail());
            setNullableString(statement, 6, equipe.getImage());
            setNullableString(statement, 7, equipe.getCompetitionCode());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Equipe equipe) throws SQLException {
        String sql = "UPDATE equipe SET nom = ?, coach = ?, adresse = ?, telephone = ?, email = ?, image = ?, competition_code = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, equipe.getNom());
            setNullableString(statement, 2, equipe.getCoach());
            setNullableString(statement, 3, equipe.getAdresse());
            setNullableString(statement, 4, equipe.getTelephone());
            setNullableString(statement, 5, equipe.getEmail());
            setNullableString(statement, 6, equipe.getImage());
            setNullableString(statement, 7, equipe.getCompetitionCode());
            statement.setInt(8, equipe.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM equipe WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Equipe> getAll() throws SQLException {
        String sql = "SELECT id, nom, coach, adresse, telephone, email, image, external_api_id, external_source, competition_code, api_football_id FROM equipe";
        List<Equipe> equipes = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                equipes.add(mapRow(rs));
            }
        }

        return equipes;
    }

    @Override
    public Equipe getById(int id) throws SQLException {
        String sql = "SELECT id, nom, coach, adresse, telephone, email, image, external_api_id, external_source, competition_code, api_football_id FROM equipe WHERE id = ?";

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

    private Equipe mapRow(ResultSet rs) throws SQLException {
        Equipe equipe = new Equipe(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("coach"),
                rs.getString("adresse"),
                rs.getString("telephone"),
                rs.getString("email"),
                rs.getString("image")
        );
        long externalApiId = rs.getLong("external_api_id");
        equipe.setExternalApiId(rs.wasNull() ? null : externalApiId);
        equipe.setExternalSource(rs.getString("external_source"));
        equipe.setCompetitionCode(rs.getString("competition_code"));
        long apiFootballId = rs.getLong("api_football_id");
        equipe.setApiFootballId(rs.wasNull() ? null : apiFootballId);
        return equipe;
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }

        statement.setString(index, value);
    }
}

package tn.esprit.services;

import tn.esprit.entities.Matchs;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MatchsService implements IService<Matchs> {
    private final Connection connection;

    public MatchsService() throws SQLException {
        connection = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Matchs matchs) throws SQLException {
        String sql = "INSERT INTO matchs (id_match, date_match, heure_debut, lieu, type, statut, lineup_domicile, lineup_exterieur, score_equipe_domicile, score_equipe_exterieur, equipe_domicile_id, equipe_exterieur_id, competition_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, matchs.getIdMatch());
            statement.setDate(2, Date.valueOf(matchs.getDateMatch()));
            statement.setTime(3, Time.valueOf(matchs.getHeureDebut()));
            statement.setString(4, matchs.getLieu());
            statement.setString(5, matchs.getType());
            statement.setString(6, matchs.getStatut());
            statement.setString(7, matchs.getLineupDomicile());
            statement.setString(8, matchs.getLineupExterieur());
            setNullableInt(statement, 9, matchs.getScoreEquipeDomicile());
            setNullableInt(statement, 10, matchs.getScoreEquipeExterieur());
            setNullableInt(statement, 11, matchs.getEquipeDomicileId());
            setNullableInt(statement, 12, matchs.getEquipeExterieurId());
            statement.setString(13, matchs.getCompetitionCode());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Matchs matchs) throws SQLException {
        String sql = "UPDATE matchs SET id_match = ?, date_match = ?, heure_debut = ?, lieu = ?, type = ?, statut = ?, lineup_domicile = ?, lineup_exterieur = ?, score_equipe_domicile = ?, score_equipe_exterieur = ?, equipe_domicile_id = ?, equipe_exterieur_id = ?, competition_code = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, matchs.getIdMatch());
            statement.setDate(2, Date.valueOf(matchs.getDateMatch()));
            statement.setTime(3, Time.valueOf(matchs.getHeureDebut()));
            statement.setString(4, matchs.getLieu());
            statement.setString(5, matchs.getType());
            statement.setString(6, matchs.getStatut());
            statement.setString(7, matchs.getLineupDomicile());
            statement.setString(8, matchs.getLineupExterieur());
            setNullableInt(statement, 9, matchs.getScoreEquipeDomicile());
            setNullableInt(statement, 10, matchs.getScoreEquipeExterieur());
            setNullableInt(statement, 11, matchs.getEquipeDomicileId());
            setNullableInt(statement, 12, matchs.getEquipeExterieurId());
            statement.setString(13, matchs.getCompetitionCode());
            statement.setInt(14, matchs.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM matchs WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Matchs> getAll() throws SQLException {
        String sql = "SELECT id, id_match, date_match, heure_debut, lieu, type, statut, lineup_domicile, lineup_exterieur, score_equipe_domicile, score_equipe_exterieur, equipe_domicile_id, equipe_exterieur_id, external_api_id, external_source, competition_code FROM matchs";
        List<Matchs> matchsList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                matchsList.add(mapRow(rs));
            }
        }

        return matchsList;
    }

    @Override
    public Matchs getById(int id) throws SQLException {
        String sql = "SELECT id, id_match, date_match, heure_debut, lieu, type, statut, lineup_domicile, lineup_exterieur, score_equipe_domicile, score_equipe_exterieur, equipe_domicile_id, equipe_exterieur_id, external_api_id, external_source, competition_code FROM matchs WHERE id = ?";

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

    private Matchs mapRow(ResultSet rs) throws SQLException {
        Date dateMatch = rs.getDate("date_match");
        Time heureDebut = rs.getTime("heure_debut");
        Integer scoreEquipeDomicile = getNullableInt(rs, "score_equipe_domicile");
        Integer scoreEquipeExterieur = getNullableInt(rs, "score_equipe_exterieur");
        Integer equipeDomicileId = getNullableInt(rs, "equipe_domicile_id");
        Integer equipeExterieurId = getNullableInt(rs, "equipe_exterieur_id");

        Matchs matchs = new Matchs(
                rs.getInt("id"),
                rs.getString("id_match"),
                dateMatch != null ? dateMatch.toLocalDate() : null,
                heureDebut != null ? heureDebut.toLocalTime() : null,
                rs.getString("lieu"),
                rs.getString("type"),
                rs.getString("statut"),
                rs.getString("lineup_domicile"),
                rs.getString("lineup_exterieur"),
                scoreEquipeDomicile,
                scoreEquipeExterieur,
                equipeDomicileId,
                equipeExterieurId
        );
        long externalApiId = rs.getLong("external_api_id");
        matchs.setExternalApiId(rs.wasNull() ? null : externalApiId);
        matchs.setExternalSource(rs.getString("external_source"));
        matchs.setCompetitionCode(rs.getString("competition_code"));
        return matchs;
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value != null) {
            statement.setInt(index, value);
        } else {
            statement.setNull(index, java.sql.Types.INTEGER);
        }
    }

    private Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Number of matches scheduled on the given calendar day.
     */
    public int countMatchesOnDate(LocalDate date) throws SQLException {
        Objects.requireNonNull(date, "date");
        String sql = "SELECT COUNT(*) FROM matchs WHERE date_match = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Number of matches with the given status (e.g. {@code Programme}, {@code Fini}).
     */
    public int countByStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM matchs WHERE statut = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, statut);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}

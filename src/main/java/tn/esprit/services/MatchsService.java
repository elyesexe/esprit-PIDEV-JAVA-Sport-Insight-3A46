package tn.esprit.services;

import tn.esprit.entities.Matchs;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MatchsService implements IService<Matchs> {
    private final Connection connection;
    private final MatchLiveCompanionAnalyzer liveCompanionAnalyzer;

    public MatchsService() throws SQLException {
        connection = MyConnection.getInstance().getConnection();
        liveCompanionAnalyzer = new MatchLiveCompanionAnalyzer();
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
        String sql = "SELECT id, id_match, date_match, heure_debut, lieu, type, statut, lineup_domicile, lineup_exterieur, score_equipe_domicile, score_equipe_exterieur, equipe_domicile_id, equipe_exterieur_id, external_api_id, external_source, competition_code, api_football_id, api_football_stats_json, api_football_lineup_json, api_football_incidents_json, api_football_synced_at, odds_snapshot_json, odds_source, odds_synced_at FROM matchs";
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
        String sql = "SELECT id, id_match, date_match, heure_debut, lieu, type, statut, lineup_domicile, lineup_exterieur, score_equipe_domicile, score_equipe_exterieur, equipe_domicile_id, equipe_exterieur_id, external_api_id, external_source, competition_code, api_football_id, api_football_stats_json, api_football_lineup_json, api_football_incidents_json, api_football_synced_at, odds_snapshot_json, odds_source, odds_synced_at FROM matchs WHERE id = ?";

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

    public MatchLiveCompanionResponse getLiveCompanion(int matchId) throws SQLException {
        Matchs match = getById(matchId);
        if (match == null) {
            throw new MatchNotFoundException(matchId);
        }
        return getLiveCompanion(match);
    }

    public MatchLiveCompanionResponse getLiveCompanion(Matchs match) {
        if (match == null || match.getId() == null) {
            throw new IllegalArgumentException("Match not found.");
        }
        return liveCompanionAnalyzer.analyze(match);
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
        long apiFootballId = rs.getLong("api_football_id");
        matchs.setApiFootballId(rs.wasNull() ? null : apiFootballId);
        matchs.setApiFootballStatsJson(rs.getString("api_football_stats_json"));
        matchs.setApiFootballLineupJson(rs.getString("api_football_lineup_json"));
        matchs.setApiFootballIncidentsJson(rs.getString("api_football_incidents_json"));
        Timestamp syncedAt = rs.getTimestamp("api_football_synced_at");
        matchs.setApiFootballSyncedAt(syncedAt == null ? null : syncedAt.toLocalDateTime());
        matchs.setOddsSnapshotJson(rs.getString("odds_snapshot_json"));
        matchs.setOddsSource(rs.getString("odds_source"));
        Timestamp oddsSyncedAt = rs.getTimestamp("odds_synced_at");
        matchs.setOddsSyncedAt(oddsSyncedAt == null ? null : oddsSyncedAt.toLocalDateTime());
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

    public List<Matchs> findNextMatchesForTeam(Integer equipeId, int limit) throws SQLException {
        if (equipeId == null || limit <= 0) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        return getAll().stream()
                .filter(match -> isTeamMatch(match, equipeId))
                .filter(match -> !isFinishedStatus(match.getStatut()))
                .filter(match -> {
                    LocalDateTime kickoff = matchDateTime(match);
                    return kickoff != null && !kickoff.isBefore(now);
                })
                .sorted(Comparator.comparing(this::matchDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    public List<Matchs> findLastResultsForTeam(Integer equipeId, int limit) throws SQLException {
        if (equipeId == null || limit <= 0) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        return getAll().stream()
                .filter(match -> isTeamMatch(match, equipeId))
                .filter(this::hasRecordedScore)
                .filter(match -> isFinishedStatus(match.getStatut()) || isPastMatch(match, now))
                .sorted(Comparator.comparing(this::matchDateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();
    }

    private boolean isTeamMatch(Matchs match, Integer equipeId) {
        return match != null
                && (Objects.equals(match.getEquipeDomicileId(), equipeId)
                || Objects.equals(match.getEquipeExterieurId(), equipeId));
    }

    private boolean hasRecordedScore(Matchs match) {
        return match != null
                && match.getScoreEquipeDomicile() != null
                && match.getScoreEquipeExterieur() != null;
    }

    private boolean isPastMatch(Matchs match, LocalDateTime now) {
        LocalDateTime kickoff = matchDateTime(match);
        return kickoff != null && kickoff.isBefore(now);
    }

    private LocalDateTime matchDateTime(Matchs match) {
        if (match == null || match.getDateMatch() == null) {
            return null;
        }
        LocalTime time = match.getHeureDebut() == null ? LocalTime.MIDNIGHT : match.getHeureDebut();
        return match.getDateMatch().atTime(time);
    }

    private boolean isFinishedStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("not finished") || normalized.contains("unfinished")) {
            return false;
        }
        return normalized.equals("finished")
                || normalized.equals("ft")
                || normalized.equals("fini")
                || normalized.contains("match finished")
                || normalized.contains("full time")
                || normalized.contains("fini")
                || normalized.contains("termine")
                || normalized.contains("termin")
                || normalized.contains("ended")
                || normalized.contains("complete");
    }
}

package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import tn.esprit.services.football.ApiFootballClient;
import tn.esprit.services.football.ApiFootballCompetitionMappings;
import tn.esprit.services.football.FootballDataApiClient;
import tn.esprit.services.football.FootballDataCompetitions;
import tn.esprit.services.wikidata.WikidataPlayerImageService;
import tn.esprit.tools.FootballDataConfig;
import tn.esprit.tools.JoueurAvatarGenerator;
import tn.esprit.tools.MyConnection;

import java.io.IOException;
import java.text.Normalizer;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class FootballDataSyncService {
    private final Connection connection;
    private final FootballDataApiClient apiClient;
    private final ApiFootballClient apiFootballClient;
    private final WikidataPlayerImageService wikidataImageService;

    public FootballDataSyncService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
        this.apiClient = new FootballDataApiClient();
        ApiFootballClient optionalApiFootballClient;
        try {
            optionalApiFootballClient = new ApiFootballClient();
        } catch (IllegalStateException e) {
            optionalApiFootballClient = null;
        }
        this.apiFootballClient = optionalApiFootballClient;
        this.wikidataImageService = new WikidataPlayerImageService();
    }

    /**
     * Clears the local players table before a fresh external sync.
     * We try TRUNCATE (fast), fallback to DELETE if the DB user lacks privileges.
     */
    public void clearJoueurTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
            } catch (SQLException ignored) {
                // Non-MySQL or permissions; continue.
            }

            // Best-effort cleanup of known dependent tables if present.
            for (String table : new String[] { "commentaire", "evaluation", "participation" }) {
                try {
                    statement.executeUpdate("DELETE FROM " + table);
                } catch (SQLException ignored) {
                    // Table missing or restricted; ignore.
                }
            }

            try {
                statement.executeUpdate("TRUNCATE TABLE joueur");
            } catch (SQLException truncateFailed) {
                statement.executeUpdate("DELETE FROM joueur");
            }

            try {
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }

    public FootballDataSyncSummary syncTeamsAndPlayers(List<String> competitionCodes, Consumer<String> progressReporter)
            throws SQLException, IOException, InterruptedException {
        int teamsUpserted = 0;
        int playersUpserted = 0;
        int playersSkipped = 0;

        for (int index = 0; index < competitionCodes.size(); index++) {
            String competitionCode = FootballDataCompetitions.normalizeCode(competitionCodes.get(index));
            progressReporter.accept("Import des clubs et effectifs " + FootballDataCompetitions.labelOf(competitionCode)
                    + " (" + (index + 1) + "/" + competitionCodes.size() + ")...");

            JsonNode payload = apiClient.fetchCompetitionTeams(competitionCode);
            JsonNode teamsNode = payload.path("teams");
            if (!teamsNode.isArray()) {
                continue;
            }
            ApiFootballCompetitionContext apiFootballContext = loadApiFootballCompetitionContext(competitionCode, progressReporter);

            for (JsonNode teamNode : teamsNode) {
                TeamUpsertResult teamResult = upsertTeam(teamNode, competitionCode);
                teamsUpserted += teamResult.createdOrUpdated ? 1 : 0;
                if (teamResult.localId == null) {
                    continue;
                }

                Long apiFootballTeamId = resolveApiFootballTeamId(apiFootballContext, text(teamNode, "name"));
                if (apiFootballTeamId != null) {
                    updateTeamApiFootballId(teamResult.localId, apiFootballTeamId);
                }
                Map<String, ApiFootballPlayerProfile> apiFootballPlayers =
                        loadApiFootballPlayers(apiFootballContext, apiFootballTeamId, progressReporter);

                JsonNode squadNode = teamNode.path("squad");
                if (!squadNode.isArray()) {
                    continue;
                }

                for (JsonNode playerNode : squadNode) {
                    PlayerUpsertOutcome outcome = upsertPlayer(playerNode, teamResult.localId, apiFootballPlayers);
                    if (outcome == PlayerUpsertOutcome.UPSERTED) {
                        playersUpserted++;
                    } else {
                        playersSkipped++;
                    }
                }
            }
        }

        return new FootballDataSyncSummary(competitionCodes.size(), teamsUpserted, playersUpserted, playersSkipped, 0);
    }

    /**
     * Enriches players (already in DB) with real photos from Wikidata (Commons) when available.
     * This only updates existing rows and will never insert new players (no duplicates).
     */
    public int enrichPlayerImagesFromWikidata(Consumer<String> progressReporter) {
        int updated = 0;
        List<PlayerIdentityRow> candidates;
        try {
            candidates = listPlayersMissingImages();
        } catch (SQLException e) {
            return 0;
        }

        for (int i = 0; i < candidates.size(); i++) {
            PlayerIdentityRow row = candidates.get(i);
            if (progressReporter != null) {
                progressReporter.accept("Enriching player photos (Wikidata) " + (i + 1) + "/" + candidates.size()
                        + " (updated: " + updated + ")...");
            }
            try {
                tn.esprit.entities.Joueur joueur = new tn.esprit.entities.Joueur();
                joueur.setId(row.id);
                joueur.setNom(row.nom);
                joueur.setPrenom(row.prenom);
                joueur.setDateNaissance(row.dateNaissance);

                String pathOrUrl = wikidataImageService.resolvePlayerImagePath(joueur);
                if (pathOrUrl == null || pathOrUrl.isBlank()) {
                    continue;
                }
                if (updateJoueurImage(row.id, pathOrUrl)) {
                    updated++;
                }
            } catch (Exception e) {
                // Keep enrichment best-effort; move on to next player, but surface the reason.
                if (progressReporter != null && updated == 0 && i < 3) {
                    progressReporter.accept("Wikidata enrichment warning: " + safeOneLine(e.getMessage()));
                }
                System.err.println("Wikidata enrichment failed for joueur #" + row.id + ": " + e.getMessage());
            }
        }

        if (progressReporter != null) {
            progressReporter.accept("Wikidata enrichment complete: " + updated + " photo(s) updated.");
        }
        return updated;
    }

    private static String safeOneLine(String value) {
        if (value == null) {
            return "Unknown error";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "...";
    }

    public FootballDataSyncSummary syncMatches(List<String> competitionCodes, Consumer<String> progressReporter)
            throws SQLException, IOException, InterruptedException {
        int matchesUpserted = 0;
        int teamsUpserted = 0;

        for (int index = 0; index < competitionCodes.size(); index++) {
            String competitionCode = FootballDataCompetitions.normalizeCode(competitionCodes.get(index));
            progressReporter.accept("Import du calendrier " + FootballDataCompetitions.labelOf(competitionCode)
                    + " (" + (index + 1) + "/" + competitionCodes.size() + ")...");

            JsonNode payload = apiClient.fetchCompetitionMatches(competitionCode);
            JsonNode matchesNode = payload.path("matches");
            if (!matchesNode.isArray()) {
                continue;
            }

            for (JsonNode matchNode : matchesNode) {
                TeamUpsertResult homeResult = upsertBasicTeam(matchNode.path("homeTeam"), competitionCode);
                TeamUpsertResult awayResult = upsertBasicTeam(matchNode.path("awayTeam"), competitionCode);
                teamsUpserted += homeResult.createdOrUpdated ? 1 : 0;
                teamsUpserted += awayResult.createdOrUpdated ? 1 : 0;

                if (homeResult.localId == null || awayResult.localId == null) {
                    continue;
                }

                if (upsertMatch(matchNode, competitionCode, homeResult.localId, awayResult.localId)) {
                    matchesUpserted++;
                }
            }
        }

        return new FootballDataSyncSummary(competitionCodes.size(), teamsUpserted, 0, 0, matchesUpserted);
    }

    private TeamUpsertResult upsertTeam(JsonNode teamNode, String competitionCode) throws SQLException {
        long externalId = teamNode.path("id").asLong(0);
        if (externalId <= 0) {
            return TeamUpsertResult.missing();
        }

        String name = text(teamNode, "name");
        String coachName = text(teamNode.path("coach"), "name");
        String address = firstNonBlank(text(teamNode, "address"), text(teamNode, "venue"));
        String crest = text(teamNode, "crest");
        String teamCompetitionCode = normalizeTeamCompetitionCode(competitionCode);

        TeamRow existing = findTeam(externalId, name);
        if (existing == null) {
            int localId = insertTeam(externalId, name, coachName, address, crest, null, null, teamCompetitionCode);
            return new TeamUpsertResult(localId, true);
        }

        updateTeam(
                existing.id,
                externalId,
                firstNonBlank(name, existing.nom),
                firstNonBlank(coachName, existing.coach),
                firstNonBlank(address, existing.adresse),
                firstNonBlank(crest, existing.image),
                existing.telephone,
                existing.email,
                mergeTeamCompetitionCode(existing.competitionCode, teamCompetitionCode)
        );
        return new TeamUpsertResult(existing.id, changed(existing, name, coachName, address, crest, teamCompetitionCode));
    }

    private TeamUpsertResult upsertBasicTeam(JsonNode teamNode, String competitionCode) throws SQLException {
        long externalId = teamNode.path("id").asLong(0);
        if (externalId <= 0) {
            return TeamUpsertResult.missing();
        }

        String name = text(teamNode, "name");
        String crest = text(teamNode, "crest");
        String teamCompetitionCode = normalizeTeamCompetitionCode(competitionCode);
        TeamRow existing = findTeam(externalId, name);
        if (existing == null) {
            int localId = insertTeam(externalId, name, null, null, crest, null, null, teamCompetitionCode);
            return new TeamUpsertResult(localId, true);
        }

        updateTeam(
                existing.id,
                externalId,
                firstNonBlank(name, existing.nom),
                existing.coach,
                existing.adresse,
                firstNonBlank(crest, existing.image),
                existing.telephone,
                existing.email,
                mergeTeamCompetitionCode(existing.competitionCode, teamCompetitionCode)
        );
        return new TeamUpsertResult(existing.id, changed(existing, name, null, null, crest, teamCompetitionCode));
    }

    private PlayerUpsertOutcome upsertPlayer(JsonNode playerNode, int equipeId, Map<String, ApiFootballPlayerProfile> apiFootballPlayers)
            throws SQLException {
        long externalId = playerNode.path("id").asLong(0);
        if (externalId <= 0) {
            return PlayerUpsertOutcome.SKIPPED;
        }

        NameParts nameParts = splitPlayerName(text(playerNode, "name"));
        if (nameParts.nom == null) {
            return PlayerUpsertOutcome.SKIPPED;
        }

        LocalDate dateNaissance = parseDate(text(playerNode, "dateOfBirth"));
        if (dateNaissance == null) {
            return PlayerUpsertOutcome.SKIPPED;
        }

        String position = normalizeNullable(text(playerNode, "position"));
        String nationalite = normalizeNullable(text(playerNode, "nationality"));
        PlayerRow existing = findPlayer(externalId, equipeId, nameParts);
        int numero = existing != null && existing.numero > 0 ? existing.numero : 0;
        ApiFootballPlayerProfile apiFootballProfile = resolveApiFootballPlayer(nameParts, apiFootballPlayers);
        String apiPhoto = firstNonBlank(
                text(playerNode, "photo"),
                firstNonBlank(text(playerNode, "image"), apiFootballProfile == null ? null : apiFootballProfile.photoUrl())
        );
        String image = choosePlayerImage(existing == null ? null : existing.image, apiPhoto, externalId, nameParts);

        if (existing == null) {
            insertPlayer(externalId, nameParts.nom, nameParts.prenom, dateNaissance, numero, image, equipeId, position, nationalite);
            return PlayerUpsertOutcome.UPSERTED;
        }

        updatePlayer(
                existing.id,
                externalId,
                nameParts.nom,
                nameParts.prenom,
                dateNaissance,
                numero,
                image,
                equipeId,
                firstNonBlank(position, existing.position),
                firstNonBlank(nationalite, existing.nationalite)
        );
        return PlayerUpsertOutcome.UPSERTED;
    }

    private ApiFootballCompetitionContext loadApiFootballCompetitionContext(String competitionCode, Consumer<String> progressReporter)
            throws InterruptedException {
        if (apiFootballClient == null || !ApiFootballCompetitionMappings.supportsCompetition(competitionCode)) {
            return null;
        }

        Integer leagueId = ApiFootballCompetitionMappings.leagueIdOf(competitionCode);
        if (leagueId == null) {
            return null;
        }

        int seasonYear = ApiFootballCompetitionMappings.resolveSeasonYear(competitionCode, LocalDate.now());
        try {
            JsonNode payload = apiFootballClient.fetchCompetitionTeams(leagueId, seasonYear);
            JsonNode responseNode = payload.path("response");
            Map<String, Long> teamsByName = new LinkedHashMap<>();
            if (responseNode.isArray()) {
                for (JsonNode teamEntry : responseNode) {
                    JsonNode teamNode = teamEntry.path("team");
                    long teamId = teamNode.path("id").asLong(0);
                    String teamName = normalizeNullable(teamNode.path("name").asText(null));
                    String normalizedTeamName = normalizeNameKey(teamName);
                    if (teamId > 0 && normalizedTeamName != null) {
                        teamsByName.put(normalizedTeamName, teamId);
                    }
                }
            }
            return new ApiFootballCompetitionContext(leagueId, seasonYear, teamsByName);
        } catch (IOException e) {
            if (progressReporter != null) {
                progressReporter.accept("Photos API-Football indisponibles: " + safeOneLine(e.getMessage()));
            }
            return null;
        }
    }

    private Long resolveApiFootballTeamId(ApiFootballCompetitionContext context, String teamName) {
        if (context == null || context.teamsByName().isEmpty()) {
            return null;
        }
        return resolveNamedId(context.teamsByName(), teamName);
    }

    private Map<String, ApiFootballPlayerProfile> loadApiFootballPlayers(
            ApiFootballCompetitionContext context,
            Long apiFootballTeamId,
            Consumer<String> progressReporter
    ) throws InterruptedException {
        if (context == null || apiFootballTeamId == null || apiFootballClient == null) {
            return Map.of();
        }

        Map<String, ApiFootballPlayerProfile> playersByName = new LinkedHashMap<>();
        int page = 1;
        while (true) {
            try {
                JsonNode payload = apiFootballClient.fetchTeamPlayers(
                        Math.toIntExact(apiFootballTeamId),
                        context.leagueId(),
                        context.seasonYear(),
                        page
                );
                JsonNode responseNode = payload.path("response");
                if (!responseNode.isArray() || responseNode.isEmpty()) {
                    break;
                }

                for (JsonNode entry : responseNode) {
                    JsonNode playerNode = entry.path("player");
                    long playerId = playerNode.path("id").asLong(0);
                    String playerName = normalizeNullable(playerNode.path("name").asText(null));
                    String photoUrl = normalizeNullable(playerNode.path("photo").asText(null));
                    String normalizedPlayerName = normalizeNameKey(playerName);
                    if (normalizedPlayerName != null && photoUrl != null) {
                        playersByName.put(normalizedPlayerName, new ApiFootballPlayerProfile(playerId <= 0 ? null : playerId, photoUrl));
                    }
                }

                JsonNode pagingNode = payload.path("paging");
                int totalPages = pagingNode.path("total").asInt(page);
                if (page >= totalPages || page >= 8) {
                    break;
                }
                page++;
            } catch (IOException | ArithmeticException e) {
                if (progressReporter != null) {
                    progressReporter.accept("Photos joueurs API-Football ignorees: " + safeOneLine(e.getMessage()));
                }
                break;
            }
        }
        return playersByName;
    }

    private ApiFootballPlayerProfile resolveApiFootballPlayer(NameParts nameParts, Map<String, ApiFootballPlayerProfile> playersByName) {
        if (playersByName == null || playersByName.isEmpty() || nameParts == null) {
            return null;
        }

        String fullName = ((nameParts.prenom == null ? "" : nameParts.prenom) + " " + (nameParts.nom == null ? "" : nameParts.nom)).trim();
        String normalizedName = normalizeNameKey(fullName);
        if (normalizedName == null) {
            return null;
        }

        ApiFootballPlayerProfile exact = playersByName.get(normalizedName);
        if (exact != null) {
            return exact;
        }

        double bestScore = 0.0;
        ApiFootballPlayerProfile bestMatch = null;
        for (Map.Entry<String, ApiFootballPlayerProfile> entry : playersByName.entrySet()) {
            double score = similarity(normalizedName, entry.getKey());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry.getValue();
            }
        }
        return bestScore >= 0.82 ? bestMatch : null;
    }

    private Long resolveNamedId(Map<String, Long> mappedIds, String localName) {
        String normalizedLocalName = normalizeNameKey(localName);
        if (normalizedLocalName == null || mappedIds == null || mappedIds.isEmpty()) {
            return null;
        }

        Long exact = mappedIds.get(normalizedLocalName);
        if (exact != null) {
            return exact;
        }

        double bestScore = 0.0;
        Long bestMatch = null;
        for (Map.Entry<String, Long> entry : mappedIds.entrySet()) {
            double score = similarity(normalizedLocalName, entry.getKey());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry.getValue();
            }
        }
        return bestScore >= 0.72 ? bestMatch : null;
    }

    private String normalizeNameKey(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private double similarity(String left, String right) {
        if (left == null || right == null) {
            return 0.0;
        }
        if (Objects.equals(left, right)) {
            return 1.0;
        }
        if (left.contains(right) || right.contains(left)) {
            return 0.86;
        }

        List<String> leftTokens = List.of(left.split("\\s+"));
        List<String> rightTokens = List.of(right.split("\\s+"));
        long overlap = leftTokens.stream()
                .filter(rightTokens::contains)
                .distinct()
                .count();
        long union = leftTokens.stream().distinct().count()
                + rightTokens.stream()
                .filter(token -> !leftTokens.contains(token))
                .distinct()
                .count();
        return union == 0 ? 0.0 : (double) overlap / union;
    }

    private String choosePlayerImage(String existingImage, String apiPhoto, long externalId, NameParts nameParts) {
        String normalizedExisting = normalizeNullable(existingImage);
        String normalizedApiPhoto = normalizeNullable(apiPhoto);
        if (normalizedApiPhoto != null && shouldReplacePlayerPhoto(normalizedExisting)) {
            return normalizedApiPhoto;
        }
        if (normalizedExisting != null) {
            return normalizedExisting;
        }
        return JoueurAvatarGenerator.ensureAvatarPath(externalId, nameParts.prenom, nameParts.nom);
    }

    private boolean shouldReplacePlayerPhoto(String image) {
        if (image == null || image.isBlank()) {
            return true;
        }
        String normalized = image.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("fd-player-")
                || normalized.contains("commons.wikimedia.org/wiki/special:filepath")
                || normalized.contains("wikidata")
                || normalized.contains("wikipedia.org");
    }

    private boolean upsertMatch(JsonNode matchNode, String competitionCode, int homeEquipeId, int awayEquipeId) throws SQLException {
        long externalId = matchNode.path("id").asLong(0);
        if (externalId <= 0) {
            return false;
        }

        LocalDateTime matchDateTime = parseMatchDateTime(text(matchNode, "utcDate"));
        LocalDate dateMatch = matchDateTime == null ? null : matchDateTime.toLocalDate();
        LocalTime heureDebut = matchDateTime == null ? null : matchDateTime.toLocalTime().withSecond(0).withNano(0);
        if (dateMatch == null || heureDebut == null) {
            return false;
        }

        JsonNode fullTimeNode = matchNode.path("score").path("fullTime");
        Integer scoreHome = integerValue(fullTimeNode, "home");
        Integer scoreAway = integerValue(fullTimeNode, "away");
        String statut = defaultIfBlank(mapMatchStatus(text(matchNode, "status")), "Programme");
        String type = defaultIfBlank(buildMatchType(matchNode, competitionCode), FootballDataCompetitions.labelOf(competitionCode));
        String reference = "FD-" + competitionCode + "-" + externalId;

        MatchRow existing = findMatch(externalId, dateMatch, heureDebut, homeEquipeId, awayEquipeId);
        String lieu = defaultIfBlank(existing == null ? null : existing.lieu, buildMatchLocation(matchNode));
        String lineupHome = defaultIfBlank(existing == null ? null : existing.lineupDomicile, "");
        String lineupAway = defaultIfBlank(existing == null ? null : existing.lineupExterieur, "");

        if (existing == null) {
            insertMatch(externalId, reference, dateMatch, heureDebut, lieu, type, statut, lineupHome, lineupAway,
                    scoreHome, scoreAway, homeEquipeId, awayEquipeId, competitionCode);
            return true;
        }

        updateMatch(existing.id, externalId, reference, dateMatch, heureDebut, lieu, type, statut, lineupHome, lineupAway,
                scoreHome, scoreAway, homeEquipeId, awayEquipeId, competitionCode);
        return true;
    }

    private TeamRow findTeam(long externalId, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, nom, coach, adresse, telephone, email, image, competition_code FROM equipe WHERE external_source = ? AND external_api_id = ? LIMIT 1")) {
            statement.setString(1, FootballDataConfig.SOURCE);
            statement.setLong(2, externalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTeamRow(resultSet);
                }
            }
        }

        if (name == null) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, nom, coach, adresse, telephone, email, image, competition_code FROM equipe WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?)) LIMIT 1")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTeamRow(resultSet);
                }
            }
        }

        return null;
    }

    private List<PlayerIdentityRow> listPlayersMissingImages() throws SQLException {
        // Also refresh placeholder avatars produced by earlier syncs.
        String sql = """
                SELECT id, nom, prenom, date_naissance
                FROM joueur
                WHERE date_naissance IS NOT NULL
                  AND (
                        image IS NULL
                        OR TRIM(image) = ''
                        OR image LIKE '%fd-player-%'
                  )
                ORDER BY id ASC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            java.util.ArrayList<PlayerIdentityRow> rows = new java.util.ArrayList<>();
            while (resultSet.next()) {
                Date dob = resultSet.getDate("date_naissance");
                rows.add(new PlayerIdentityRow(
                        resultSet.getInt("id"),
                        resultSet.getString("nom"),
                        resultSet.getString("prenom"),
                        dob == null ? null : dob.toLocalDate()
                ));
            }
            return rows;
        }
    }

    private boolean updateJoueurImage(int joueurId, String imageUrl) throws SQLException {
        String sql = "UPDATE joueur SET image = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, imageUrl);
            statement.setInt(2, joueurId);
            return statement.executeUpdate() > 0;
        }
    }

    private PlayerRow findPlayer(long externalId, int equipeId, NameParts nameParts) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, numero, image, position, nationalite FROM joueur WHERE external_source = ? AND external_api_id = ? LIMIT 1")) {
            statement.setString(1, FootballDataConfig.SOURCE);
            statement.setLong(2, externalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPlayerRow(resultSet);
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, numero, image, position, nationalite FROM joueur WHERE equipe_id = ? AND LOWER(TRIM(nom)) = LOWER(TRIM(?)) AND LOWER(TRIM(prenom)) = LOWER(TRIM(?)) LIMIT 1")) {
            statement.setInt(1, equipeId);
            statement.setString(2, nameParts.nom);
            statement.setString(3, nameParts.prenom == null ? "" : nameParts.prenom);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapPlayerRow(resultSet);
                }
            }
        }

        return null;
    }

    private MatchRow findMatch(long externalId, LocalDate dateMatch, LocalTime heureDebut, int homeEquipeId, int awayEquipeId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, lieu, lineup_domicile, lineup_exterieur FROM matchs WHERE external_source = ? AND external_api_id = ? LIMIT 1")) {
            statement.setString(1, FootballDataConfig.SOURCE);
            statement.setLong(2, externalId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapMatchRow(resultSet);
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, lieu, lineup_domicile, lineup_exterieur FROM matchs WHERE date_match = ? AND heure_debut = ? AND equipe_domicile_id = ? AND equipe_exterieur_id = ? LIMIT 1")) {
            statement.setDate(1, Date.valueOf(dateMatch));
            statement.setTime(2, Time.valueOf(heureDebut));
            statement.setInt(3, homeEquipeId);
            statement.setInt(4, awayEquipeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapMatchRow(resultSet);
                }
            }
        }

        return null;
    }

    private int insertTeam(long externalId, String nom, String coach, String adresse, String image, String telephone, String email, String competitionCode)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO equipe (nom, coach, adresse, telephone, email, image, external_api_id, external_source, competition_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, nom);
            statement.setString(2, coach);
            statement.setString(3, adresse);
            statement.setString(4, telephone);
            statement.setString(5, email);
            statement.setString(6, image);
            statement.setLong(7, externalId);
            statement.setString(8, FootballDataConfig.SOURCE);
            statement.setString(9, competitionCode);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Insertion equipe impossible pour " + nom);
    }

    private void updateTeam(
            int id,
            long externalId,
            String nom,
            String coach,
            String adresse,
            String image,
            String telephone,
            String email,
            String competitionCode
    )
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE equipe SET nom = ?, coach = ?, adresse = ?, telephone = ?, email = ?, image = ?, external_api_id = ?, external_source = ?, competition_code = ? WHERE id = ?")) {
            statement.setString(1, nom);
            statement.setString(2, coach);
            statement.setString(3, adresse);
            statement.setString(4, telephone);
            statement.setString(5, email);
            statement.setString(6, image);
            statement.setLong(7, externalId);
            statement.setString(8, FootballDataConfig.SOURCE);
            statement.setString(9, competitionCode);
            statement.setInt(10, id);
            statement.executeUpdate();
        }
    }

    private void updateTeamApiFootballId(Integer teamId, Long apiFootballId) throws SQLException {
        if (teamId == null || apiFootballId == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE equipe SET api_football_id = ? WHERE id = ? AND (api_football_id IS NULL OR api_football_id <> ?)")) {
            statement.setLong(1, apiFootballId);
            statement.setInt(2, teamId);
            statement.setLong(3, apiFootballId);
            statement.executeUpdate();
        }
    }

    private void insertPlayer(
            long externalId,
            String nom,
            String prenom,
            LocalDate dateNaissance,
            int numero,
            String image,
            int equipeId,
            String position,
            String nationalite
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO joueur (nom, prenom, date_naissance, numero, image, equipe_id, external_api_id, external_source, position, nationalite) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, nom);
            statement.setString(2, prenom);
            statement.setDate(3, Date.valueOf(dateNaissance));
            statement.setInt(4, numero);
            statement.setString(5, image);
            statement.setInt(6, equipeId);
            statement.setLong(7, externalId);
            statement.setString(8, FootballDataConfig.SOURCE);
            statement.setString(9, position);
            statement.setString(10, nationalite);
            statement.executeUpdate();
        }
    }

    private void updatePlayer(
            int id,
            long externalId,
            String nom,
            String prenom,
            LocalDate dateNaissance,
            int numero,
            String image,
            int equipeId,
            String position,
            String nationalite
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE joueur SET nom = ?, prenom = ?, date_naissance = ?, numero = ?, image = ?, equipe_id = ?, external_api_id = ?, external_source = ?, position = ?, nationalite = ? WHERE id = ?")) {
            statement.setString(1, nom);
            statement.setString(2, prenom);
            statement.setDate(3, Date.valueOf(dateNaissance));
            statement.setInt(4, numero);
            statement.setString(5, image);
            statement.setInt(6, equipeId);
            statement.setLong(7, externalId);
            statement.setString(8, FootballDataConfig.SOURCE);
            statement.setString(9, position);
            statement.setString(10, nationalite);
            statement.setInt(11, id);
            statement.executeUpdate();
        }
    }

    private void insertMatch(
            long externalId,
            String idMatch,
            LocalDate dateMatch,
            LocalTime heureDebut,
            String lieu,
            String type,
            String statut,
            String lineupDomicile,
            String lineupExterieur,
            Integer scoreHome,
            Integer scoreAway,
            int homeEquipeId,
            int awayEquipeId,
            String competitionCode
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO matchs (id_match, date_match, heure_debut, lieu, type, statut, lineup_domicile, lineup_exterieur, score_equipe_domicile, score_equipe_exterieur, equipe_domicile_id, equipe_exterieur_id, external_api_id, external_source, competition_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, idMatch);
            statement.setDate(2, Date.valueOf(dateMatch));
            statement.setTime(3, Time.valueOf(heureDebut));
            statement.setString(4, lieu);
            statement.setString(5, type);
            statement.setString(6, statut);
            statement.setString(7, lineupDomicile);
            statement.setString(8, lineupExterieur);
            setNullableInt(statement, 9, scoreHome);
            setNullableInt(statement, 10, scoreAway);
            statement.setInt(11, homeEquipeId);
            statement.setInt(12, awayEquipeId);
            statement.setLong(13, externalId);
            statement.setString(14, FootballDataConfig.SOURCE);
            statement.setString(15, competitionCode);
            statement.executeUpdate();
        }
    }

    private void updateMatch(
            int id,
            long externalId,
            String idMatch,
            LocalDate dateMatch,
            LocalTime heureDebut,
            String lieu,
            String type,
            String statut,
            String lineupDomicile,
            String lineupExterieur,
            Integer scoreHome,
            Integer scoreAway,
            int homeEquipeId,
            int awayEquipeId,
            String competitionCode
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE matchs SET id_match = ?, date_match = ?, heure_debut = ?, lieu = ?, type = ?, statut = ?, lineup_domicile = ?, lineup_exterieur = ?, score_equipe_domicile = ?, score_equipe_exterieur = ?, equipe_domicile_id = ?, equipe_exterieur_id = ?, external_api_id = ?, external_source = ?, competition_code = ? WHERE id = ?")) {
            statement.setString(1, idMatch);
            statement.setDate(2, Date.valueOf(dateMatch));
            statement.setTime(3, Time.valueOf(heureDebut));
            statement.setString(4, lieu);
            statement.setString(5, type);
            statement.setString(6, statut);
            statement.setString(7, lineupDomicile);
            statement.setString(8, lineupExterieur);
            setNullableInt(statement, 9, scoreHome);
            setNullableInt(statement, 10, scoreAway);
            statement.setInt(11, homeEquipeId);
            statement.setInt(12, awayEquipeId);
            statement.setLong(13, externalId);
            statement.setString(14, FootballDataConfig.SOURCE);
            statement.setString(15, competitionCode);
            statement.setInt(16, id);
            statement.executeUpdate();
        }
    }

    private TeamRow mapTeamRow(ResultSet resultSet) throws SQLException {
        return new TeamRow(
                resultSet.getInt("id"),
                resultSet.getString("nom"),
                resultSet.getString("coach"),
                resultSet.getString("adresse"),
                resultSet.getString("telephone"),
                resultSet.getString("email"),
                resultSet.getString("image"),
                resultSet.getString("competition_code")
        );
    }

    private PlayerRow mapPlayerRow(ResultSet resultSet) throws SQLException {
        return new PlayerRow(
                resultSet.getInt("id"),
                resultSet.getInt("numero"),
                resultSet.getString("image"),
                resultSet.getString("position"),
                resultSet.getString("nationalite")
        );
    }

    private MatchRow mapMatchRow(ResultSet resultSet) throws SQLException {
        return new MatchRow(
                resultSet.getInt("id"),
                resultSet.getString("lieu"),
                resultSet.getString("lineup_domicile"),
                resultSet.getString("lineup_exterieur")
        );
    }

    private boolean changed(TeamRow existing, String nom, String coach, String adresse, String image, String competitionCode) {
        return !Objects.equals(existing.nom, firstNonBlank(nom, existing.nom))
                || !Objects.equals(existing.coach, firstNonBlank(coach, existing.coach))
                || !Objects.equals(existing.adresse, firstNonBlank(adresse, existing.adresse))
                || !Objects.equals(existing.image, firstNonBlank(image, existing.image))
                || !Objects.equals(normalizeTeamCompetitionCode(existing.competitionCode), mergeTeamCompetitionCode(existing.competitionCode, competitionCode));
    }

    private String normalizeTeamCompetitionCode(String competitionCode) {
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        return FootballDataCompetitions.isTeamCompetition(normalizedCode) ? normalizedCode : null;
    }

    private String mergeTeamCompetitionCode(String existingCompetitionCode, String incomingCompetitionCode) {
        String normalizedIncoming = normalizeTeamCompetitionCode(incomingCompetitionCode);
        if (normalizedIncoming != null) {
            return normalizedIncoming;
        }
        return normalizeTeamCompetitionCode(existingCompetitionCode);
    }

    private String text(JsonNode node, String fieldName) {
        return normalizeNullable(node.path(fieldName).asText(null));
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseMatchDateTime(String utcDateValue) {
        if (utcDateValue == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(utcDateValue)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer integerValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isNumber() ? valueNode.asInt() : null;
    }

    private String mapMatchStatus(String apiStatus) {
        if (apiStatus == null) {
            return "Programme";
        }
        return switch (apiStatus.toUpperCase(Locale.ROOT)) {
            case "FINISHED" -> "Fini";
            case "IN_PLAY", "PAUSED" -> "En direct";
            case "POSTPONED", "SUSPENDED" -> "Reporte";
            case "CANCELLED" -> "Annule";
            case "TIMED", "SCHEDULED" -> "Programme";
            default -> "Programme";
        };
    }

    private String buildMatchType(JsonNode matchNode, String competitionCode) {
        String stage = normalizeNullable(text(matchNode, "stage"));
        String group = normalizeNullable(text(matchNode, "group"));
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);

        if (stage == null && group == null) {
            return competitionLabel;
        }
        if (group == null) {
            return competitionLabel + " | " + prettifyEnum(stage);
        }
        return competitionLabel + " | " + prettifyEnum(stage) + " | " + group;
    }

    private String buildMatchLocation(JsonNode matchNode) {
        String venue = normalizeNullable(text(matchNode, "venue"));
        if (venue != null) {
            return venue;
        }
        String area = normalizeNullable(text(matchNode.path("area"), "name"));
        if (area != null) {
            return area;
        }
        return "Lieu a confirmer";
    }

    private String prettifyEnum(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private NameParts splitPlayerName(String fullName) {
        String cleaned = normalizeNullable(fullName);
        if (cleaned == null) {
            return new NameParts(null, null);
        }

        String[] parts = cleaned.split("\\s+");
        if (parts.length == 1) {
            return new NameParts(parts[0], "");
        }

        String prenom = parts[0];
        String nom = cleaned.substring(prenom.length()).trim();
        return new NameParts(nom, prenom);
    }

    private enum PlayerUpsertOutcome {
        UPSERTED,
        SKIPPED
    }

    private record TeamUpsertResult(Integer localId, boolean createdOrUpdated) {
        private static TeamUpsertResult missing() {
            return new TeamUpsertResult(null, false);
        }
    }

    private record NameParts(String nom, String prenom) {
    }

    private record TeamRow(int id, String nom, String coach, String adresse, String telephone, String email, String image, String competitionCode) {
    }

    private record PlayerRow(int id, int numero, String image, String position, String nationalite) {
    }

    private record PlayerIdentityRow(int id, String nom, String prenom, LocalDate dateNaissance) {
    }

    private record MatchRow(int id, String lieu, String lineupDomicile, String lineupExterieur) {
    }

    private record ApiFootballCompetitionContext(int leagueId, int seasonYear, Map<String, Long> teamsByName) {
    }

    private record ApiFootballPlayerProfile(Long playerId, String photoUrl) {
    }
}

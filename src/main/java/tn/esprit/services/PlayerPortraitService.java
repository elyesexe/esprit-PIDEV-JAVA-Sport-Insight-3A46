package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.services.football.TheSportsDbClient;
import tn.esprit.services.wikidata.WikidataPlayerImageService;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PlayerPortraitService {
    private final TheSportsDbClient theSportsDbClient;

    public PlayerPortraitService() {
        this.theSportsDbClient = new TheSportsDbClient();
    }

    public boolean shouldRefreshPortrait(Joueur joueur) {
        if (joueur == null) {
            return false;
        }
        String image = joueur.getImage();
        if (image == null || image.isBlank()) {
            return true;
        }

        String normalized = image.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("fd-player-")
                || normalized.contains("commons.wikimedia.org/wiki/special:filepath")
                || normalized.contains("wikidata")
                || normalized.contains("wikipedia.org");
    }

    public String resolvePortrait(Joueur joueur, Equipe team) throws Exception {
        String sportsDbPortrait = resolveTheSportsDbPortrait(joueur, team);
        if (sportsDbPortrait != null && !sportsDbPortrait.isBlank()) {
            return sportsDbPortrait;
        }

        WikidataPlayerImageService wikidataPlayerImageService = new WikidataPlayerImageService();
        return wikidataPlayerImageService.resolvePlayerImagePath(joueur);
    }

    private String resolveTheSportsDbPortrait(Joueur joueur, Equipe team) throws Exception {
        String query = buildFullName(joueur);
        if (query == null || query.isBlank()) {
            return null;
        }

        JsonNode payload = theSportsDbClient.searchPlayers(query);
        JsonNode playersNode = payload.path("player");
        if (!playersNode.isArray() || playersNode.isEmpty()) {
            return null;
        }

        PlayerPhotoCandidate bestCandidate = null;
        for (JsonNode playerNode : playersNode) {
            String sport = playerNode.path("strSport").asText(null);
            if (sport != null && !sport.isBlank() && !"Soccer".equalsIgnoreCase(sport.trim())) {
                continue;
            }

            String photoUrl = firstNonBlankText(playerNode, "strThumb", "strCutout", "strRender", "strFanart1");
            if (photoUrl == null) {
                continue;
            }

            double score = scoreTheSportsDbPlayer(joueur, team, playerNode);
            if (bestCandidate == null || score > bestCandidate.score()) {
                bestCandidate = new PlayerPhotoCandidate(photoUrl, score);
            }
        }

        return bestCandidate == null || bestCandidate.score() < 0.72 ? null : bestCandidate.photoUrl();
    }

    private double scoreTheSportsDbPlayer(Joueur joueur, Equipe team, JsonNode playerNode) {
        double score = similarity(
                normalizeForMatch(buildFullName(joueur)),
                normalizeForMatch(playerNode.path("strPlayer").asText(null))
        );

        LocalDate dateBorn = parseLocalDate(playerNode.path("dateBorn").asText(null));
        if (joueur.getDateNaissance() != null && dateBorn != null) {
            score += joueur.getDateNaissance().equals(dateBorn) ? 0.35 : -0.2;
        }

        if (similarity(normalizeForMatch(joueur.getNationalite()),
                normalizeForMatch(playerNode.path("strNationality").asText(null))) >= 0.8) {
            score += 0.15;
        }

        String localTeamName = team == null ? null : team.getNom();
        if (similarity(normalizeForMatch(localTeamName), normalizeForMatch(playerNode.path("strTeam").asText(null))) >= 0.6) {
            score += 0.12;
        }

        return score;
    }

    private String buildFullName(Joueur joueur) {
        if (joueur == null) {
            return null;
        }
        String prenom = joueur.getPrenom() == null ? "" : joueur.getPrenom().trim();
        String nom = joueur.getNom() == null ? "" : joueur.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private String firstNonBlankText(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeForMatch(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.isBlank() ? null : normalized.replaceAll("\\s+", " ");
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
        long overlap = leftTokens.stream().filter(rightTokens::contains).count();
        long union = leftTokens.stream().distinct().count()
                + rightTokens.stream().filter(token -> !leftTokens.contains(token)).distinct().count();
        return union == 0 ? 0.0 : (double) overlap / union;
    }

    private record PlayerPhotoCandidate(String photoUrl, double score) {
    }
}

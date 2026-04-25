package tn.esprit.services.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import tn.esprit.entities.Joueur;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WikidataPlayerImageService {
    private static final String PROP_INSTANCE_OF = "P31";
    private static final String PROP_OCCUPATION = "P106";
    private static final String PROP_DATE_OF_BIRTH = "P569";
    private static final String PROP_IMAGE = "P18";

    // Q ids we use for quick checks
    private static final String Q_HUMAN = "Q5";
    private static final String Q_FOOTBALL_PLAYER = "Q937857";

    private final WikidataApiClient apiClient;
    private final WikidataSparqlClient sparqlClient;
    private final Map<String, Optional<String>> cache = new ConcurrentHashMap<>();

    public WikidataPlayerImageService() {
        this.apiClient = new WikidataApiClient();
        this.sparqlClient = new WikidataSparqlClient();
    }

    /**
     * Returns a local cached file path (preferred) or a Commons URL (fallback).
     */
    public String resolvePlayerImagePath(Joueur joueur) throws Exception {
        if (joueur == null) {
            return null;
        }

        String fullName = buildFullName(joueur);
        LocalDate dob = joueur.getDateNaissance();
        if (fullName == null || dob == null) {
            return null;
        }

        String cacheKey = (fullName.toLowerCase(Locale.ROOT) + "|" + dob);
        Optional<String> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached.orElse(null);
        }

        String resolved = null;
        for (String candidateName : buildCandidateNames(fullName)) {
            resolved = resolveViaSparqlAndCache(candidateName, dob);
            if (resolved != null) {
                break;
            }
        }
        if (resolved == null) {
            for (String candidateName : buildCandidateNames(fullName)) {
                String commonsUrl = resolveCommonsImageUrlUncached(candidateName, dob);
                if (commonsUrl != null) {
                    String local = downloadToCache(candidateName, dob, commonsUrl);
                    resolved = local != null ? local : commonsUrl;
                    break;
                }
            }
        }
        cache.put(cacheKey, Optional.ofNullable(resolved));
        return resolved;
    }

    private String resolveViaSparqlAndCache(String fullName, LocalDate dob) {
        try {
            String commonsUrl = resolveCommonsUrlViaSparqlEntitySearch(fullName, dob, true);
            if (commonsUrl == null) {
                commonsUrl = resolveCommonsUrlViaSparqlEntitySearch(fullName, dob, false);
            }
            if (commonsUrl == null) {
                return null;
            }
            String local = downloadToCache(fullName, dob, commonsUrl);
            return local != null ? local : commonsUrl;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveCommonsUrlViaSparqlEntitySearch(String fullName, LocalDate dob, boolean strictOccupation) throws Exception {
        // Use Wikidata EntitySearch (mwapi) to find likely items by name, then filter by DOB.
        String dobValue = dob.toString();
        String safeSearch = fullName.replace("\"", "\\\"");

        String occupationOptional = """
                  OPTIONAL { ?item wdt:P106/wdt:P279* wd:Q937857 . BIND(1 AS ?isFootballer) }
                """;
        String occupationRequired = """
                  ?item wdt:P106/wdt:P279* wd:Q937857 .
                  BIND(1 AS ?isFootballer)
                """;

        String occupationBlock = strictOccupation ? occupationRequired : occupationOptional;

        String sparql = """
                PREFIX wd: <http://www.wikidata.org/entity/>
                PREFIX wdt: <http://www.wikidata.org/prop/direct/>
                PREFIX wikibase: <http://wikiba.se/ontology#>
                PREFIX bd: <http://www.bigdata.com/rdf#>
                PREFIX mwapi: <https://www.mediawiki.org/ontology#API/>
                PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

                SELECT ?image ?isFootballer WHERE {
                  SERVICE wikibase:mwapi {
                    bd:serviceParam wikibase:endpoint "www.wikidata.org" ;
                                   wikibase:api "EntitySearch" ;
                                   mwapi:search "%s" ;
                                   mwapi:language "en" ;
                                   mwapi:limit 12 .
                    ?item wikibase:apiOutputItem mwapi:item .
                  }

                  ?item wdt:P31 wd:Q5 ;
                        wdt:P569 ?dob ;
                        wdt:P18 ?image .

                  BIND(xsd:date(?dob) AS ?dobDate)
                  FILTER(?dobDate = "%s"^^xsd:date)

                %s

                  BIND(COALESCE(?isFootballer, 0) AS ?isFootballer)
                }
                ORDER BY DESC(?isFootballer)
                LIMIT 1
                """.formatted(safeSearch, dobValue, occupationBlock);

        JsonNode payload = sparqlClient.query(sparql);
        JsonNode bindings = payload.path("results").path("bindings");
        if (!bindings.isArray() || bindings.isEmpty()) {
            return null;
        }
        String imageValue = bindings.get(0).path("image").path("value").asText(null);
        if (imageValue == null || imageValue.isBlank()) {
            return null;
        }
        // SPARQL may return either a filename literal or a direct URL.
        if (imageValue.startsWith("http://") || imageValue.startsWith("https://")) {
            return imageValue;
        }
        return toCommonsFilePathUrl(imageValue);
    }

    private static String extractFileNameFromUrl(String url) {
        if (url == null) {
            return null;
        }
        int slash = url.lastIndexOf('/');
        String file = slash >= 0 ? url.substring(slash + 1) : url;
        int query = file.indexOf('?');
        if (query >= 0) {
            file = file.substring(0, query);
        }
        return file.replace("_", " ");
    }

    private String downloadToCache(String fullName, LocalDate dob, String commonsFilePathUrl) {
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".sport-insight", "avatars", "wikidata");
            Files.createDirectories(dir);

            String safe = (fullName + "-" + dob).toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("(^-|-$)", "");
            if (safe.isBlank()) {
                safe = "player";
            }
            Path target = dir.resolve(safe + ".jpg");
            if (Files.exists(target)) {
                return target.toAbsolutePath().toString();
            }

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(20))
                    .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(commonsFilePathUrl))
                    .header("User-Agent", "SportInsight/1.0 (JavaFX; Wikidata enrichment)")
                    .timeout(java.time.Duration.ofSeconds(45))
                    .GET()
                    .build();
            java.net.http.HttpResponse<byte[]> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            byte[] body = response.body();
            if (body == null || body.length < 2048) {
                return null;
            }
            Files.write(target, body);
            return target.toAbsolutePath().toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveCommonsImageUrlUncached(String fullName, LocalDate dob) throws Exception {
        JsonNode searchPayload = apiClient.searchEntities(fullName, 8);
        JsonNode results = searchPayload.path("search");
        if (!results.isArray()) {
            return null;
        }

        for (JsonNode hit : results) {
            String id = hit.path("id").asText(null);
            if (id == null || id.isBlank()) {
                continue;
            }

            JsonNode entityPayload = apiClient.getEntity(id);
            JsonNode entity = entityPayload.path("entities").path(id);
            if (entity.isMissingNode()) {
                continue;
            }

            JsonNode claims = entity.path("claims");
            if (!isHuman(claims)) {
                continue;
            }

            LocalDate entityDob = parseWikidataDate(claims.path(PROP_DATE_OF_BIRTH));
            if (!Objects.equals(entityDob, dob)) {
                continue;
            }

            // Prefer football players, but do not hard-fail: many entities miss the occupation claim.
            boolean likelyFootballer = hasOccupation(claims, Q_FOOTBALL_PLAYER);

            String fileName = extractCommonsFileName(claims.path(PROP_IMAGE));
            if (fileName != null) {
                return toCommonsFilePathUrl(fileName);
            }

            if (likelyFootballer) {
                // no image claim; continue scanning
            }
        }

        return null;
    }

    private static boolean isHuman(JsonNode claims) {
        return hasClaimItem(claims.path(PROP_INSTANCE_OF), Q_HUMAN);
    }

    private static boolean hasOccupation(JsonNode claims, String qid) {
        return hasClaimItem(claims.path(PROP_OCCUPATION), qid);
    }

    private static boolean hasClaimItem(JsonNode claimArray, String qid) {
        if (!claimArray.isArray()) {
            return false;
        }
        for (JsonNode claim : claimArray) {
            String valueId = claim.path("mainsnak").path("datavalue").path("value").path("id").asText(null);
            if (qid.equals(valueId)) {
                return true;
            }
        }
        return false;
    }

    private static LocalDate parseWikidataDate(JsonNode claimArray) {
        if (!claimArray.isArray()) {
            return null;
        }
        for (JsonNode claim : claimArray) {
            String time = claim.path("mainsnak").path("datavalue").path("value").path("time").asText(null);
            if (time == null || time.length() < 11) {
                continue;
            }
            // format: +1987-06-24T00:00:00Z
            try {
                String datePart = time.startsWith("+") ? time.substring(1, 11) : time.substring(0, 10);
                return LocalDate.parse(datePart);
            } catch (Exception ignored) {
                // continue
            }
        }
        return null;
    }

    private static String extractCommonsFileName(JsonNode claimArray) {
        if (!claimArray.isArray()) {
            return null;
        }
        for (JsonNode claim : claimArray) {
            String fileName = claim.path("mainsnak").path("datavalue").path("value").asText(null);
            if (fileName != null && !fileName.isBlank()) {
                return fileName;
            }
        }
        return null;
    }

    private static String toCommonsFilePathUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        // Special:FilePath returns a direct image redirect; width helps keep UI fast.
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + encoded + "?width=512";
    }

    private static String buildFullName(Joueur joueur) {
        String prenom = clean(joueur.getPrenom());
        String nom = clean(joueur.getNom());
        String combined = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return combined.isBlank() ? null : combined;
    }

    private static Iterable<String> buildCandidateNames(String fullName) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String cleaned = clean(fullName);
        if (cleaned == null) {
            return candidates;
        }

        candidates.add(cleaned);

        String ascii = stripDiacritics(cleaned);
        if (!Objects.equals(ascii, cleaned)) {
            candidates.add(ascii);
        }

        String compact = cleaned.replaceAll("\\s+", " ").trim();
        if (!Objects.equals(compact, cleaned)) {
            candidates.add(compact);
        }

        String asciiCompact = stripDiacritics(compact);
        if (!Objects.equals(asciiCompact, compact)) {
            candidates.add(asciiCompact);
        }

        return candidates;
    }

    private static String stripDiacritics(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

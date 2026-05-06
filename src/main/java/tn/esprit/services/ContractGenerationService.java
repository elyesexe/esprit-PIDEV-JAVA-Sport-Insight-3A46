package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tn.esprit.tools.OpenAiConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class ContractGenerationService {
    public static final String SOURCE_OPENAI = "openai";
    public static final String SOURCE_LOCAL_TEMPLATE = "local-template";
    public static final String SOURCE_OPENAI_FALLBACK = "local-openai-error";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ContractGenerationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return OpenAiConfig.isConfigured();
    }

    public GeneratedContract generateContractDraft(ContractDraft draft) {
        GeneratedContract local = new GeneratedContract(buildLocalDraft(draft), SOURCE_LOCAL_TEMPLATE);
        if (!isConfigured()) {
            return local;
        }

        try {
            String responseText = sendPrompt(
                    """
                    Tu rediges des contrats de sponsoring sportif en francais.
                    Retourne uniquement un JSON avec les cles: title, body.
                    - title: une courte ligne de titre
                    - body: un texte de contrat clair, structure, pret a coller dans une description
                    Garde un ton professionnel, concret et presentable.
                    Limite la redaction a des sections courtes et directement exploitables dans un PDF professionnel.
                    """,
                    buildGenerationPrompt(draft)
            );
            String parsed = parseGeneratedDraft(responseText);
            if (parsed == null || parsed.isBlank()) {
                return new GeneratedContract(local.body(), SOURCE_OPENAI_FALLBACK);
            }
            return new GeneratedContract(parsed, SOURCE_OPENAI);
        } catch (Exception e) {
            return new GeneratedContract(local.body(), SOURCE_OPENAI_FALLBACK);
        }
    }

    private String buildGenerationPrompt(ContractDraft draft) {
        return """
                Sponsor: %s
                Equipe: %s
                Date debut: %s
                Date fin: %s
                Montant: %s
                Statut contrat: %s
                Statut paiement: %s

                Redige un projet de contrat de sponsoring sportif en francais avec:
                1. objet du partenariat
                2. engagements du sponsor
                3. engagements de l'equipe
                4. conditions financieres
                5. duree et renouvellement
                6. clause de resiliation
                7. clause de confidentialite
                8. validation finale
                """.formatted(
                safe(draft.sponsorName(), "Sponsor"),
                safe(draft.teamName(), "Equipe"),
                safe(draft.startDate() == null ? null : draft.startDate().toString(), "-"),
                safe(draft.endDate() == null ? null : draft.endDate().toString(), "-"),
                formatAmount(draft.amount()),
                safe(draft.contractStatus(), "ACTIVE"),
                safe(draft.paymentStatus(), "PENDING")
        );
    }

    private String buildLocalDraft(ContractDraft draft) {
        String sponsor = safe(draft.sponsorName(), "Sponsor");
        String team = safe(draft.teamName(), "Equipe");
        String start = formatDate(draft.startDate());
        String end = formatDate(draft.endDate());
        String amount = formatAmount(draft.amount());
        long durationDays = draft.startDate() != null && draft.endDate() != null
                ? Math.max(0, ChronoUnit.DAYS.between(draft.startDate(), draft.endDate()))
                : 0;

        return """
                CONTRAT DE SPONSORING SPORTIF

                Entre la societe %s, ci-apres denommee le Sponsor, et le club %s, ci-apres denommee l'Equipe, il est convenu ce qui suit.

                1. Objet
                Le present contrat a pour objet de definir un partenariat de sponsoring visant a renforcer la visibilite du Sponsor a travers les activites sportives, evenements et supports de communication de l'Equipe.

                2. Duree
                Le contrat prend effet a compter du %s et se termine le %s, pour une duree estimative de %d jours, sauf renouvellement ou resiliation anticipee selon les clauses prevues.

                3. Engagements du Sponsor
                Le Sponsor s'engage a verser un montant global de %s selon les modalites de paiement convenues, a fournir les elements visuels utiles a la communication et a respecter l'image et les valeurs du club.

                4. Engagements de l'Equipe
                L'Equipe s'engage a assurer la presence du Sponsor sur ses supports promotionnels appropries, a valoriser le partenariat lors des actions de communication et a maintenir un niveau de professionnalisme conforme aux attentes du partenariat.

                5. Conditions financieres
                Le montant contractuel est fixe a %s. Le suivi de paiement est actuellement renseigne comme %s et le statut administratif du contrat est %s.

                6. Resiliation
                En cas de manquement grave par l'une des parties, le contrat pourra etre resilie apres notification ecrite et expiration d'un delai raisonnable de regularisation.

                7. Confidentialite
                Les informations financieres, commerciales et strategiques echangees dans le cadre du partenariat demeurent confidentielles sauf obligation legale ou accord ecrit entre les parties.

                8. Validation
                Le present projet constitue une base de travail a valider, completer et signer par les representants habilites du Sponsor et de l'Equipe.
                """.formatted(
                sponsor,
                team,
                start,
                end,
                durationDays,
                amount,
                amount,
                safe(draft.paymentStatus(), "PENDING"),
                safe(draft.contractStatus(), "ACTIVE")
        );
    }

    private String parseGeneratedDraft(String rawJson) throws IOException {
        JsonNode root = objectMapper.readTree(cleanJson(rawJson));
        String title = trimToNull(root.path("title").asText(null));
        String body = trimToNull(root.path("body").asText(null));
        if (body == null) {
            return null;
        }
        return title == null ? body : title + "\n\n" + body;
    }

    private String sendPrompt(String instructions, String prompt) throws IOException, InterruptedException {
        String apiKey = OpenAiConfig.resolveApiKey();
        if (apiKey == null) {
            throw new IOException("OpenAI API key is missing.");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", OpenAiConfig.resolveModel());
        ArrayNode input = payload.putArray("input");

        ObjectNode developerMessage = input.addObject();
        developerMessage.put("role", "developer");
        developerMessage.put("content", instructions);

        ObjectNode userMessage = input.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        HttpRequest request = HttpRequest.newBuilder(URI.create(OpenAiConfig.BASE_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI request failed with status " + response.statusCode() + ".");
        }

        JsonNode root = objectMapper.readTree(response.body());
        String outputText = extractOutputText(root);
        if (outputText == null) {
            throw new IOException("OpenAI response did not contain text output.");
        }
        return outputText;
    }

    private String extractOutputText(JsonNode root) {
        String directText = trimToNull(root.path("output_text").asText(null));
        if (directText != null) {
            return directText;
        }

        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    String text = contentItem.path("text").asText("");
                    if (!text.isBlank()) {
                        if (builder.length() > 0) {
                            builder.append('\n');
                        }
                        builder.append(text);
                    }
                }
            }
        }
        return trimToNull(builder.toString());
    }

    private String cleanJson(String rawValue) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return "{}";
        }
        String cleaned = normalized;
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "");
            cleaned = cleaned.replaceFirst("```$", "");
        }
        String trimmed = cleaned.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            int objectStart = trimmed.indexOf('{');
            int objectEnd = trimmed.lastIndexOf('}');
            if (objectStart >= 0 && objectEnd > objectStart) {
                return trimmed.substring(objectStart, objectEnd + 1);
            }
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : value.toString();
    }

    private String formatAmount(Double amount) {
        if (amount == null) {
            return "-";
        }
        return String.format(Locale.ENGLISH, "%,.2f DT", amount);
    }

    public record ContractDraft(
            String sponsorName,
            String teamName,
            LocalDate startDate,
            LocalDate endDate,
            Double amount,
            String contractStatus,
            String paymentStatus
    ) {
    }

    public record GeneratedContract(String body, String source) {
    }
}

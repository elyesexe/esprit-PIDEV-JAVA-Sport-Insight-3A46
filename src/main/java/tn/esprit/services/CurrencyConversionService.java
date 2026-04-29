package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CurrencyConversionService {
    private static final List<String> ENDPOINT_TEMPLATES = List.of(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/%s.json",
            "https://latest.currency-api.pages.dev/v1/currencies/%s.json"
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, ExchangeRateSheet> sheetCache = new ConcurrentHashMap<>();

    public CurrencyConversionService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), new ObjectMapper());
    }

    CurrencyConversionService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public ConversionResult convert(BigDecimal amount, String baseCurrency, List<String> targetCurrencies) throws IOException, InterruptedException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IOException("Amount must be strictly positive.");
        }

        String normalizedBase = normalizeCurrency(baseCurrency);
        List<String> normalizedTargets = targetCurrencies == null
                ? List.of()
                : targetCurrencies.stream()
                .map(this::normalizeCurrency)
                .filter(Objects::nonNull)
                .filter(target -> !target.equals(normalizedBase))
                .distinct()
                .toList();
        if (normalizedBase == null || normalizedTargets.isEmpty()) {
            throw new IOException("At least one valid target currency is required.");
        }

        ExchangeRateSheet sheet = sheetCache.computeIfAbsent(normalizedBase, base -> {
            try {
                return fetchRateSheet(base);
            } catch (IOException | InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        });

        Map<String, BigDecimal> convertedAmounts = new LinkedHashMap<>();
        for (String target : normalizedTargets) {
            BigDecimal rate = sheet.rates().get(target);
            if (rate != null) {
                convertedAmounts.put(target, amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
            }
        }
        if (convertedAmounts.isEmpty()) {
            throw new IOException("No target currency rates were returned by the provider.");
        }

        return new ConversionResult(
                amount.setScale(2, RoundingMode.HALF_UP),
                normalizedBase,
                convertedAmounts,
                sheet.date(),
                sheet.provider()
        );
    }

    ExchangeRateSheet parseSheet(String baseCurrency, String body, String provider) throws IOException {
        String normalizedBase = normalizeCurrency(baseCurrency);
        if (normalizedBase == null) {
            throw new IOException("Base currency is invalid.");
        }

        JsonNode root = objectMapper.readTree(body);
        JsonNode ratesNode = root.path(normalizedBase.toLowerCase(Locale.ROOT));
        if (!ratesNode.isObject()) {
            throw new IOException("Currency payload is missing base rates for " + normalizedBase + ".");
        }

        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        ratesNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNumber()) {
                rates.put(entry.getKey().toUpperCase(Locale.ROOT), entry.getValue().decimalValue());
            }
        });
        if (rates.isEmpty()) {
            throw new IllegalStateException("Currency payload did not contain any rates.");
        }

        String date = trimToNull(root.path("date").asText(null));
        return new ExchangeRateSheet(
                normalizedBase,
                date == null ? "unknown" : date,
                rates,
                provider
        );
    }

    private ExchangeRateSheet fetchRateSheet(String baseCurrency) throws IOException, InterruptedException {
        IOException lastException = null;
        for (String template : ENDPOINT_TEMPLATES) {
            String url = template.formatted(baseCurrency.toLowerCase(Locale.ROOT));
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseSheet(baseCurrency, response.body(), url);
                }
                lastException = new IOException("Currency API returned status " + response.statusCode() + ".");
            } catch (IOException exception) {
                lastException = exception;
            }
        }
        throw lastException == null ? new IOException("Currency API is unavailable.") : lastException;
    }

    private String normalizeCurrency(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ConversionResult(
            BigDecimal originalAmount,
            String baseCurrency,
            Map<String, BigDecimal> convertedAmounts,
            String rateDate,
            String provider
    ) {
    }

    record ExchangeRateSheet(
            String baseCurrency,
            String date,
            Map<String, BigDecimal> rates,
            String provider
    ) {
    }
}

package tn.esprit.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyConversionServiceTest {

    @Test
    void parseSheetExtractsRatesForSupportedTargets() throws Exception {
        CurrencyConversionService service = new CurrencyConversionService(HttpClient.newHttpClient(), new ObjectMapper());
        String body = """
                {
                  "date": "2026-04-28",
                  "tnd": {
                    "usd": 0.34290373,
                    "eur": 0.29272334,
                    "gbp": 0.25338688
                  }
                }
                """;

        CurrencyConversionService.ExchangeRateSheet sheet = service.parseSheet("TND", body, "test-provider");

        assertEquals("TND", sheet.baseCurrency());
        assertEquals("2026-04-28", sheet.date());
        assertEquals(new BigDecimal("0.34290373"), sheet.rates().get("USD"));
        assertEquals(new BigDecimal("0.29272334"), sheet.rates().get("EUR"));
        assertEquals("test-provider", sheet.provider());
    }
}

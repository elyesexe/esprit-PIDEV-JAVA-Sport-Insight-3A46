package tn.esprit.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StripeCheckoutSessionStoreTest {

    @Test
    void saveGetAndClearPendingCheckoutByUser() {
        Integer userId = 42;
        StripeCheckoutSessionStore.clear(userId);

        StripeCheckoutSessionStore.PendingStripeCheckout pendingCheckout =
                new StripeCheckoutSessionStore.PendingStripeCheckout(
                        "cs_test_123",
                        "https://checkout.stripe.com/test",
                        "client@example.com",
                        "+21612345678",
                        "1 rue test",
                        "2 rue bill",
                        new BigDecimal("150.00"),
                        "USD",
                        new BigDecimal("49.90"),
                        "2026-04-29",
                        "1:2:75.00",
                        LocalDateTime.now()
                );

        StripeCheckoutSessionStore.save(userId, pendingCheckout);

        StripeCheckoutSessionStore.PendingStripeCheckout restored = StripeCheckoutSessionStore.get(userId);
        assertEquals("cs_test_123", restored.sessionId());
        assertEquals(new BigDecimal("49.90"), restored.chargeAmount());
        assertEquals("1:2:75.00", restored.cartSignature());

        StripeCheckoutSessionStore.clear(userId);
        assertNull(StripeCheckoutSessionStore.get(userId));
    }
}

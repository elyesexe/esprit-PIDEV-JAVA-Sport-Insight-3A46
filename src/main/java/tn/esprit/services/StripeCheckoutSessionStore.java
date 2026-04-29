package tn.esprit.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StripeCheckoutSessionStore {
    private static final Integer GUEST_KEY = 0;
    private static final Map<Integer, PendingStripeCheckout> PENDING_BY_USER = new ConcurrentHashMap<>();

    private StripeCheckoutSessionStore() {
    }

    public static PendingStripeCheckout get(Integer userId) {
        return PENDING_BY_USER.get(resolveUserKey(userId));
    }

    public static void save(Integer userId, PendingStripeCheckout checkout) {
        if (checkout == null) {
            clear(userId);
            return;
        }
        PENDING_BY_USER.put(resolveUserKey(userId), checkout);
    }

    public static void clear(Integer userId) {
        PENDING_BY_USER.remove(resolveUserKey(userId));
    }

    private static Integer resolveUserKey(Integer userId) {
        return userId == null ? GUEST_KEY : userId;
    }

    public record PendingStripeCheckout(
            String sessionId,
            String checkoutUrl,
            String email,
            String phone,
            String shippingAddress,
            String billingAddress,
            BigDecimal totalAmount,
            String chargeCurrency,
            BigDecimal chargeAmount,
            String rateDate,
            String cartSignature,
            LocalDateTime createdAt
    ) {
    }
}

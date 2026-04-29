package tn.esprit.services;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import tn.esprit.entities.Product;
import tn.esprit.tools.StripeConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class StripeCheckoutService {
    private static final String DEFAULT_SUCCESS_URL = "https://example.com/?payment=success&session_id={CHECKOUT_SESSION_ID}";
    private static final String DEFAULT_CANCEL_URL = "https://example.com/?payment=cancelled";

    public boolean isConfigured() {
        return StripeConfig.isConfigured();
    }

    public CheckoutLaunch createCheckoutSession(StripeCheckoutRequest request) throws StripeException {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe is not configured.");
        }
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Checkout cart is empty.");
        }

        StripeClient client = new StripeClient(StripeConfig.resolveSecretKey());
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(DEFAULT_SUCCESS_URL)
                .setCancelUrl(DEFAULT_CANCEL_URL)
                .setCustomerEmail(request.customerEmail());

        for (StripeCheckoutLine line : request.lines()) {
            builder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity((long) line.quantity())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(convertToCents(line.unitPrice()))
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(line.productName())
                                                            .setDescription(line.description())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        if (request.referenceId() != null && !request.referenceId().isBlank()) {
            builder.setClientReferenceId(request.referenceId());
        }

        Session session = client.checkout().sessions().create(builder.build());
        return new CheckoutLaunch(session.getId(), session.getUrl());
    }

    public CheckoutVerification verifyCheckoutSession(String sessionId) throws StripeException {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe is not configured.");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Stripe session id is missing.");
        }

        StripeClient client = new StripeClient(StripeConfig.resolveSecretKey());
        Session session = client.checkout().sessions().retrieve(sessionId);
        String paymentStatus = session.getPaymentStatus();
        String status = session.getStatus();

        CheckoutVerificationStatus verificationStatus;
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            verificationStatus = CheckoutVerificationStatus.PAID;
        } else if ("expired".equalsIgnoreCase(status)) {
            verificationStatus = CheckoutVerificationStatus.EXPIRED;
        } else if ("complete".equalsIgnoreCase(status) && !"paid".equalsIgnoreCase(paymentStatus)) {
            verificationStatus = CheckoutVerificationStatus.FAILED;
        } else {
            verificationStatus = CheckoutVerificationStatus.PENDING;
        }

        return new CheckoutVerification(
                verificationStatus,
                session.getId(),
                session.getUrl(),
                paymentStatus,
                status
        );
    }

    private long convertToCents(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        return safe.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    public record StripeCheckoutRequest(
            String customerEmail,
            String referenceId,
            List<StripeCheckoutLine> lines
    ) {
    }

    public record StripeCheckoutLine(
            String productName,
            String description,
            int quantity,
            BigDecimal unitPrice
    ) {
    }

    public record CheckoutLaunch(
            String sessionId,
            String checkoutUrl
    ) {
    }

    public record CheckoutVerification(
            CheckoutVerificationStatus status,
            String sessionId,
            String checkoutUrl,
            String paymentStatus,
            String sessionStatus
    ) {
    }

    public enum CheckoutVerificationStatus {
        PAID,
        PENDING,
        FAILED,
        EXPIRED
    }
}

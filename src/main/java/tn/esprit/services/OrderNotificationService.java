package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;
import tn.esprit.tools.ResendConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderNotificationService {
    private static final DateTimeFormatter FILE_SUFFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OrderNotificationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return ResendConfig.isConfigured();
    }

    public DeliveryResult sendOrderNotification(Order order, Product product, String clientLabel, boolean updateMode) throws IOException, InterruptedException {
        if (order == null) {
            throw new IOException("Order payload is missing.");
        }

        NotificationContent content = buildContent(order, product, clientLabel, updateMode);
        if (!isConfigured()) {
            return writePreview(order, content);
        }

        String recipient = trimToNull(order.getContactEmail());
        if (recipient == null) {
            throw new IOException("Order contact email is missing.");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("from", ResendConfig.resolveFromAddress());
        ArrayNode to = payload.putArray("to");
        to.add(recipient);
        payload.put("subject", content.subject());
        payload.put("html", content.html());
        payload.put("text", content.text());

        HttpRequest request = HttpRequest.newBuilder(URI.create(ResendConfig.BASE_URL))
                .header("Authorization", "Bearer " + ResendConfig.resolveApiKey())
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", "order-" + fallback(order.getId(), 0) + "-" + (updateMode ? "update" : "create"))
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            DeliveryResult fallback = writePreview(order, content);
            return new DeliveryResult(false, fallback.message(), fallback.previewPath(), null, response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String messageId = trimToNull(root.path("id").asText(null));
        return new DeliveryResult(true, "Email sent with Resend.", null, messageId, response.statusCode());
    }

    private NotificationContent buildContent(Order order, Product product, String clientLabel, boolean updateMode) {
        String productName = product == null ? "Produit" : fallback(product.getName(), "Produit");
        String client = fallback(trimToNull(clientLabel), "Client");
        String orderId = order.getId() == null ? "pending" : String.valueOf(order.getId());
        String total = order.getTotalAmount() == null ? "0.00 DT" : order.getTotalAmount().toPlainString() + " DT";
        String quantity = String.valueOf(fallback(order.getQuantity(), 0));
        String subject = updateMode
                ? "Sport Insight | Order #" + orderId + " updated"
                : "Sport Insight | Order #" + orderId + " confirmed";

        String text = """
                Order %s
                Product: %s
                Client: %s
                Quantity: %s
                Total: %s
                Status: %s
                Payment: %s
                Shipping: %s
                """.formatted(
                orderId,
                productName,
                client,
                quantity,
                total,
                fallback(order.getStatus(), "PENDING"),
                fallback(order.getPaymentStatus(), "UNPAID"),
                fallback(order.getShippingAddress(), "-")
        );

        String html = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #0f172a;">
                    <h2>Sport Insight order %s</h2>
                    <p>Your order has been %s.</p>
                    <ul>
                      <li><strong>Product:</strong> %s</li>
                      <li><strong>Client:</strong> %s</li>
                      <li><strong>Quantity:</strong> %s</li>
                      <li><strong>Total:</strong> %s</li>
                      <li><strong>Status:</strong> %s</li>
                      <li><strong>Payment:</strong> %s</li>
                    </ul>
                    <p><strong>Shipping address:</strong> %s</p>
                  </body>
                </html>
                """.formatted(
                orderId,
                updateMode ? "updated" : "confirmed",
                escapeHtml(productName),
                escapeHtml(client),
                escapeHtml(quantity),
                escapeHtml(total),
                escapeHtml(fallback(order.getStatus(), "PENDING")),
                escapeHtml(fallback(order.getPaymentStatus(), "UNPAID")),
                escapeHtml(fallback(order.getShippingAddress(), "-"))
        );

        return new NotificationContent(subject, text, html);
    }

    private DeliveryResult writePreview(Order order, NotificationContent content) throws IOException {
        Path targetDirectory = Path.of("generated", "order-notifications");
        Files.createDirectories(targetDirectory);

        String orderId = order.getId() == null ? "pending" : String.valueOf(order.getId());
        String fileName = "order-" + orderId + "-" + FILE_SUFFIX_FORMAT.format(LocalDateTime.now()) + ".html";
        Path target = targetDirectory.resolve(fileName);

        String preview = """
                <html>
                  <body>
                    <h3>%s</h3>
                    <pre>%s</pre>
                    <hr/>
                    %s
                  </body>
                </html>
                """.formatted(
                escapeHtml(content.subject()),
                escapeHtml(content.text()),
                content.html()
        );
        Files.writeString(target, preview, StandardCharsets.UTF_8);
        return new DeliveryResult(false, "Email API not configured. Preview saved locally.", target.toAbsolutePath(), null, null);
    }

    private String escapeHtml(String value) {
        String normalized = fallback(value, "");
        return normalized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int fallback(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    public record DeliveryResult(
            boolean delivered,
            String message,
            Path previewPath,
            String providerMessageId,
            Integer statusCode
    ) {
    }

    private record NotificationContent(
            String subject,
            String text,
            String html
    ) {
    }
}

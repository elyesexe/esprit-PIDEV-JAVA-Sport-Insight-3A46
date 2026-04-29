package tn.esprit.services;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderNotificationServiceTest {

    @Test
    void fallbackPreviewStillWorksWithoutEmailWhenApiIsNotConfigured() throws Exception {
        OrderNotificationService service = new OrderNotificationService();
        Order order = new Order();
        order.setId(77);
        order.setQuantity(2);
        order.setStatus("CONFIRMED");
        order.setPaymentStatus("PAID");
        order.setPaymentMethod("CARD");
        order.setShippingAddress("Tunis Centre, Bloc A");
        order.setBillingAddress("Tunis Centre, Bloc A");
        order.setContactPhone("+21655111222");
        order.setTotalAmount(new BigDecimal("180.00"));

        Product product = new Product(5, "Predator SG Boots", "Boots", new BigDecimal("90.00"), 10, "42", "Adidas", "boot.png", null, null);

        Method buildContent = OrderNotificationService.class.getDeclaredMethod(
                "buildContent",
                Order.class,
                Product.class,
                String.class,
                boolean.class
        );
        buildContent.setAccessible(true);
        Object content = buildContent.invoke(service, order, product, "Coach Fawzi", false);

        Method writePreview = OrderNotificationService.class.getDeclaredMethod("writePreview", Order.class, content.getClass());
        writePreview.setAccessible(true);
        OrderNotificationService.DeliveryResult result =
                (OrderNotificationService.DeliveryResult) writePreview.invoke(service, order, content);

        assertFalse(result.delivered());
        assertNotNull(result.previewPath());
        assertTrue(Files.exists(result.previewPath()));
        Files.deleteIfExists(result.previewPath());
    }
}

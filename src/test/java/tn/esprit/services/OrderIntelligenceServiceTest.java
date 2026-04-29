package tn.esprit.services;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderIntelligenceServiceTest {

    @Test
    void assessAnomalyFlagsHighRiskOrder() {
        OrderIntelligenceService service = new OrderIntelligenceService();
        Product product = new Product(5, "Mercurial Elite", "Boots", new BigDecimal("400.00"), 10, "42", "Nike", "boots.png", null, null);
        List<Order> history = List.of(
                buildOrder(1, product.getId(), 2, "client@example.com", "+21611111111", LocalDate.of(2026, 4, 10), "800.00"),
                buildOrder(2, product.getId(), 1, "client@example.com", "+21611111111", LocalDate.of(2026, 4, 12), "400.00"),
                buildOrder(3, product.getId(), 2, "client@example.com", "+21611111111", LocalDate.of(2026, 4, 14), "800.00")
        );
        Order candidate = buildOrder(9, product.getId(), 8, "client@example.com", "+21611111111", LocalDate.of(2026, 4, 15), "3200.00");

        OrderIntelligenceService.OrderAnomalyAssessment assessment = service.assessAnomaly(candidate, product, history);

        assertEquals("HIGH", assessment.level());
        assertTrue(assessment.score() >= 60);
        assertFalse(assessment.reasons().isEmpty());
    }

    @Test
    void recommendProductsReturnsCrossSellSuggestions() {
        OrderIntelligenceService service = new OrderIntelligenceService();
        Product base = new Product(1, "Match Boots", "Boots", new BigDecimal("280.00"), 4, "42", "Nike", "boots.png", null, null);
        Product accessory = new Product(2, "Grip Socks", "Accessories", new BigDecimal("35.00"), 20, "M", "Nike", "socks.png", null, null);
        Product sameCategory = new Product(3, "Predator Boots", "Boots", new BigDecimal("300.00"), 8, "42", "Adidas", "alt-boots.png", null, null);
        Product irrelevant = new Product(4, "Goalkeeper Gloves", "Gloves", new BigDecimal("110.00"), 9, "L", "Puma", "gloves.png", null, null);

        List<Order> orders = List.of(
                buildOrder(20, accessory.getId(), 5, "a@example.com", "+21620000000", LocalDate.of(2026, 4, 10), "175.00"),
                buildOrder(21, sameCategory.getId(), 3, "b@example.com", "+21630000000", LocalDate.of(2026, 4, 11), "900.00")
        );

        List<OrderIntelligenceService.ProductRecommendation> recommendations =
                service.recommendProducts(base, List.of(base, accessory, sameCategory, irrelevant), orders);

        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(item -> item.productId().equals(accessory.getId())));
    }

    private Order buildOrder(
            int id,
            Integer productId,
            int quantity,
            String email,
            String phone,
            LocalDate orderDate,
            String total
    ) {
        Order order = new Order();
        order.setId(id);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setContactEmail(email);
        order.setContactPhone(phone);
        order.setOrderDate(orderDate);
        order.setTotalAmount(new BigDecimal(total));
        return order;
    }
}

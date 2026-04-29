package tn.esprit.services;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductAnalyticsServiceTest {

    @Test
    void predictDemandFlagsRestockRiskForFastMovingProduct() {
        ProductAnalyticsService service = new ProductAnalyticsService();
        Product product = new Product(10, "Tempo Ball", "Balls", new BigDecimal("89.00"), 4, null, "Puma", "ball.png", null, null);
        List<Order> history = List.of(
                buildOrder(1, product.getId(), 3, LocalDate.of(2026, 4, 1), "270.00"),
                buildOrder(2, product.getId(), 2, LocalDate.of(2026, 4, 3), "180.00"),
                buildOrder(3, product.getId(), 4, LocalDate.of(2026, 4, 5), "360.00")
        );

        ProductAnalyticsService.ProductDemandSnapshot snapshot = service.predictDemand(product, history);

        assertEquals("HIGH", snapshot.riskLevel());
        assertTrue(snapshot.averageDailyDemand() > 1.0);
        assertTrue(snapshot.suggestedRestock() > 0);
        assertTrue(snapshot.summary().contains("risk HIGH"));
    }

    @Test
    void predictDemandWithoutHistoryUsesStockHeuristic() {
        ProductAnalyticsService service = new ProductAnalyticsService();
        Product product = new Product(11, "Low Stock Socks", "Accessories", new BigDecimal("18.00"), 4, "M", "Nike", "socks.png", null, null);

        ProductAnalyticsService.ProductDemandSnapshot snapshot = service.predictDemand(product, List.of());

        assertEquals("HIGH", snapshot.riskLevel());
        assertTrue(snapshot.summary().contains("stock actuel est critique"));
    }

    @Test
    void selectTrendingProductsReturnsHighestOrderedProducts() {
        ProductAnalyticsService service = new ProductAnalyticsService();
        Product first = new Product(10, "Predator Boots", "Boots", new BigDecimal("220.00"), 6, "42", "Adidas", "predator.png", null, null);
        Product second = new Product(11, "Mercurial Boots", "Boots", new BigDecimal("320.00"), 5, "42", "Nike", "mercurial.png", null, null);
        Product third = new Product(12, "Training Jersey", "Jerseys", new BigDecimal("90.00"), 14, "M", "Nike", "jersey.png", null, null);

        List<Order> history = List.of(
                buildOrder(1, first.getId(), 3, LocalDate.of(2026, 4, 1), "660.00"),
                buildOrder(2, second.getId(), 5, LocalDate.of(2026, 4, 3), "1600.00"),
                buildOrder(3, second.getId(), 4, LocalDate.of(2026, 4, 5), "1280.00"),
                buildOrder(4, third.getId(), 2, LocalDate.of(2026, 4, 6), "180.00")
        );

        ProductAnalyticsService.TrendingSelection selection =
                service.selectTrendingProducts(List.of(first, second, third), history, 2);

        assertEquals(List.of(11, 10), selection.products().stream().map(Product::getId).toList());
        assertTrue(selection.summary().contains("Mercurial Boots"));
    }

    private Order buildOrder(int id, Integer productId, int quantity, LocalDate orderDate, String total) {
        Order order = new Order();
        order.setId(id);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setOrderDate(orderDate);
        order.setTotalAmount(new BigDecimal(total));
        return order;
    }
}

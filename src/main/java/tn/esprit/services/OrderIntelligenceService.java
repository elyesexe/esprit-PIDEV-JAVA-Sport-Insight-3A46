package tn.esprit.services;

import tn.esprit.entities.Order;
import tn.esprit.entities.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderIntelligenceService {
    private static final Map<String, List<String>> COMPLEMENTARY_CATEGORIES = buildComplementaryCategories();

    public OrderAnomalyAssessment assessAnomaly(Order order, Product product, List<Order> history) {
        if (order == null) {
            return OrderAnomalyAssessment.empty();
        }

        List<String> reasons = new ArrayList<>();
        int score = 0;

        int quantity = order.getQuantity() == null ? 0 : order.getQuantity();
        if (quantity >= 8) {
            score += 35;
            reasons.add("large quantity");
        } else if (quantity >= 5) {
            score += 18;
            reasons.add("above normal quantity");
        }

        double averageQuantity = history == null ? 0.0 : history.stream()
                .filter(existing -> Objects.equals(order.getProductId(), existing.getProductId()))
                .filter(existing -> existing.getQuantity() != null)
                .mapToInt(Order::getQuantity)
                .average()
                .orElse(0.0);
        if (averageQuantity > 0.0 && quantity >= Math.ceil(averageQuantity * 2.0)) {
            score += 20;
            reasons.add("well above product average");
        }

        BigDecimal orderTotal = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal averageTotal = history == null || history.isEmpty()
                ? BigDecimal.ZERO
                : history.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1L, history.stream().filter(existing -> existing.getTotalAmount() != null).count())), 2, RoundingMode.HALF_UP);
        if (averageTotal.compareTo(BigDecimal.ZERO) > 0 && orderTotal.compareTo(averageTotal.multiply(BigDecimal.valueOf(2.5))) >= 0) {
            score += 20;
            reasons.add("high order amount");
        }

        if (history != null) {
            LocalDate orderDate = order.getOrderDate() == null ? LocalDate.now() : order.getOrderDate();
            long recentCustomerOrders = history.stream()
                    .filter(existing -> existing.getId() == null || !existing.getId().equals(order.getId()))
                    .filter(existing -> sameCustomer(order, existing))
                    .filter(existing -> existing.getOrderDate() != null && !existing.getOrderDate().isBefore(orderDate.minusDays(7)) && !existing.getOrderDate().isAfter(orderDate))
                    .count();
            if (recentCustomerOrders >= 3) {
                score += 25;
                reasons.add("many recent orders from same customer");
            } else if (recentCustomerOrders >= 1) {
                score += 10;
                reasons.add("repeat customer in a short window");
            }
        }

        if (product != null && product.getStock() > 0 && quantity >= Math.max(1, (int) Math.ceil(product.getStock() * 0.6))) {
            score += 15;
            reasons.add("consumes most of remaining stock");
        }

        String level = score >= 60 ? "HIGH" : score >= 30 ? "MEDIUM" : "LOW";
        String summary = reasons.isEmpty()
                ? "No anomaly detected."
                : "Risk " + level + " | " + String.join(", ", reasons);

        return new OrderAnomalyAssessment(score, level, reasons, summary);
    }

    public List<ProductRecommendation> recommendProducts(Product baseProduct, List<Product> products, List<Order> orders) {
        if (baseProduct == null || products == null || products.isEmpty()) {
            return List.of();
        }

        String category = normalize(baseProduct.getCategory());
        List<String> preferredCategories = COMPLEMENTARY_CATEGORIES.getOrDefault(category, List.of());
        Map<Integer, Long> popularity = buildPopularityMap(orders);

        return products.stream()
                .filter(candidate -> candidate.getId() != null && !candidate.getId().equals(baseProduct.getId()))
                .filter(candidate -> candidate.getStock() > 0)
                .filter(candidate -> shouldRecommend(baseProduct, candidate, preferredCategories))
                .sorted(Comparator
                        .comparing((Product candidate) -> preferredCategoryRank(candidate, preferredCategories))
                        .thenComparing((Product candidate) -> sameBrand(baseProduct, candidate) ? 0 : 1)
                        .thenComparing((Product candidate) -> popularity.getOrDefault(candidate.getId(), 0L), Comparator.reverseOrder())
                        .thenComparing(Product::getPrice, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(3)
                .map(candidate -> new ProductRecommendation(
                        candidate.getId(),
                        fallback(candidate.getName(), "Produit"),
                        buildRecommendationReason(baseProduct, candidate, preferredCategories, popularity.getOrDefault(candidate.getId(), 0L))
                ))
                .toList();
    }

    private boolean shouldRecommend(Product baseProduct, Product candidate, List<String> preferredCategories) {
        String candidateCategory = normalize(candidate.getCategory());
        if (sameBrand(baseProduct, candidate) && !candidateCategory.equals(normalize(baseProduct.getCategory()))) {
            return true;
        }
        if (preferredCategories.contains(candidateCategory)) {
            return true;
        }
        return normalize(baseProduct.getCategory()).equals(candidateCategory) && !sameBrand(baseProduct, candidate);
    }

    private int preferredCategoryRank(Product candidate, List<String> preferredCategories) {
        String category = normalize(candidate.getCategory());
        int index = preferredCategories.indexOf(category);
        return index < 0 ? preferredCategories.size() + 1 : index;
    }

    private Map<Integer, Long> buildPopularityMap(List<Order> orders) {
        if (orders == null) {
            return Map.of();
        }
        return orders.stream()
                .filter(order -> order.getProductId() != null && order.getQuantity() != null)
                .collect(Collectors.groupingBy(Order::getProductId, Collectors.summingLong(order -> order.getQuantity().longValue())));
    }

    private String buildRecommendationReason(Product baseProduct, Product candidate, List<String> preferredCategories, long popularity) {
        if (sameBrand(baseProduct, candidate) && !Objects.equals(normalize(baseProduct.getCategory()), normalize(candidate.getCategory()))) {
            return "same brand cross-sell";
        }
        if (preferredCategories.contains(normalize(candidate.getCategory()))) {
            return "complements " + fallback(baseProduct.getCategory(), "this product").toLowerCase(Locale.ROOT);
        }
        if (popularity > 0) {
            return "popular with recent orders";
        }
        return "related product";
    }

    private boolean sameCustomer(Order left, Order right) {
        return equalsIgnoreCase(left.getContactEmail(), right.getContactEmail())
                || equalsIgnoreCase(normalizePhone(left.getContactPhone()), normalizePhone(right.getContactPhone()));
    }

    private boolean sameBrand(Product left, Product right) {
        return equalsIgnoreCase(left == null ? null : left.getBrand(), right == null ? null : right.getBrand());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9+]", "");
        return digits.isBlank() ? null : digits;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static Map<String, List<String>> buildComplementaryCategories() {
        Map<String, List<String>> mapping = new LinkedHashMap<>();
        mapping.put("boots", List.of("accessories", "protection", "training wear"));
        mapping.put("jerseys", List.of("accessories", "training wear", "protection"));
        mapping.put("balls", List.of("training", "accessories", "boots"));
        mapping.put("gloves", List.of("protection", "accessories", "training"));
        mapping.put("protection", List.of("accessories", "training wear", "boots"));
        mapping.put("training", List.of("accessories", "balls", "training wear"));
        mapping.put("training wear", List.of("accessories", "boots", "jerseys"));
        mapping.put("accessories", List.of("boots", "jerseys", "training"));
        return mapping;
    }

    public record OrderAnomalyAssessment(
            int score,
            String level,
            List<String> reasons,
            String summary
    ) {
        public static OrderAnomalyAssessment empty() {
            return new OrderAnomalyAssessment(0, "LOW", List.of(), "No anomaly detected.");
        }
    }

    public record ProductRecommendation(
            Integer productId,
            String productName,
            String reason
    ) {
    }
}

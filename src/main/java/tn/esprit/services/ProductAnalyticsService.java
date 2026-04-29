package tn.esprit.services;

import tn.esprit.entities.Order;
import tn.esprit.entities.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ProductAnalyticsService {
    public ProductDemandSnapshot predictDemand(Product product, List<Order> orders) {
        if (product == null) {
            return ProductDemandSnapshot.empty();
        }

        List<Order> productOrders = orders == null
                ? List.of()
                : orders.stream()
                .filter(order -> Objects.equals(product.getId(), order.getProductId()))
                .filter(order -> order.getQuantity() != null && order.getQuantity() > 0)
                .toList();

        if (productOrders.isEmpty()) {
            String riskLevel = riskLevelWithoutHistory(product.getStock());
            return new ProductDemandSnapshot(
                    product.getId(),
                    fallback(product.getName(), "Produit"),
                    0.0,
                    null,
                    0,
                    riskLevel,
                    buildNoHistorySummary(product.getStock(), riskLevel)
            );
        }

        LocalDate earliest = productOrders.stream()
                .map(Order::getOrderDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate latest = productOrders.stream()
                .map(Order::getOrderDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        long observedDays = Math.max(7L, ChronoUnit.DAYS.between(earliest, latest) + 1L);
        int totalQuantity = productOrders.stream().mapToInt(order -> order.getQuantity() == null ? 0 : order.getQuantity()).sum();
        double dailyDemand = round((double) totalQuantity / observedDays);
        Double daysUntilStockout = dailyDemand <= 0.0 ? null : round(product.getStock() / dailyDemand);
        int suggestedRestock = dailyDemand <= 0.0
                ? 0
                : Math.max(0, (int) Math.ceil((dailyDemand * 30.0) - product.getStock()));

        String riskLevel = riskLevel(product.getStock(), daysUntilStockout);
        String summary = buildSummary(totalQuantity, observedDays, dailyDemand, daysUntilStockout, suggestedRestock, riskLevel);

        return new ProductDemandSnapshot(
                product.getId(),
                fallback(product.getName(), "Produit"),
                dailyDemand,
                daysUntilStockout,
                suggestedRestock,
                riskLevel,
                summary
        );
    }

    public DashboardDemandSummary summarize(List<Product> products, List<Order> orders) {
        List<ProductDemandSnapshot> predictions = products == null
                ? List.of()
                : products.stream()
                .map(product -> predictDemand(product, orders))
                .toList();

        long restockSoon = predictions.stream()
                .filter(snapshot -> "HIGH".equals(snapshot.riskLevel()) || "MEDIUM".equals(snapshot.riskLevel()))
                .count();

        ProductDemandSnapshot topRisk = predictions.stream()
                .sorted(Comparator
                        .comparing(ProductDemandSnapshot::riskWeight).reversed()
                        .thenComparing(snapshot -> snapshot.daysUntilStockout() == null ? Double.MAX_VALUE : snapshot.daysUntilStockout()))
                .findFirst()
                .orElse(ProductDemandSnapshot.empty());

        return new DashboardDemandSummary((int) restockSoon, topRisk);
    }

    public TrendingSelection selectTrendingProducts(List<Product> products, List<Order> orders, int limit) {
        List<Product> safeProducts = products == null ? List.of() : products;
        int resolvedLimit = limit <= 0 ? 6 : limit;
        if (safeProducts.isEmpty()) {
            return new TrendingSelection(List.of(), List.of(), resolvedLimit, "Aucun produit disponible pour les tendances.");
        }

        List<TrendingProductSnapshot> ranking = safeProducts.stream()
                .map(product -> buildTrendingSnapshot(product, orders))
                .filter(snapshot -> snapshot.totalOrderedQuantity() > 0)
                .sorted(Comparator
                        .comparingInt(TrendingProductSnapshot::totalOrderedQuantity).reversed()
                        .thenComparing(Comparator.comparingInt(TrendingProductSnapshot::orderCount).reversed())
                        .thenComparing(TrendingProductSnapshot::productName, String.CASE_INSENSITIVE_ORDER))
                .limit(resolvedLimit)
                .toList();

        if (ranking.isEmpty()) {
            return new TrendingSelection(List.of(), List.of(), resolvedLimit, "Aucune tendance disponible: aucune commande liee aux produits visibles.");
        }

        Map<Integer, Integer> rankByProductId = new HashMap<>();
        for (int index = 0; index < ranking.size(); index++) {
            rankByProductId.put(ranking.get(index).productId(), index);
        }

        List<Product> trendingProducts = safeProducts.stream()
                .filter(product -> product.getId() != null && rankByProductId.containsKey(product.getId()))
                .sorted(Comparator.comparingInt(product -> rankByProductId.get(product.getId())))
                .toList();

        TrendingProductSnapshot topProduct = ranking.get(0);
        String summary = "Top tendances: "
                + trendingProducts.size()
                + " produit(s) recommande(s) | #1 "
                + topProduct.productName()
                + " ("
                + topProduct.totalOrderedQuantity()
                + " unites / "
                + topProduct.orderCount()
                + " commandes)";

        return new TrendingSelection(trendingProducts, ranking, resolvedLimit, summary);
    }

    private String riskLevel(int stock, Double daysUntilStockout) {
        if (stock <= 0) {
            return "HIGH";
        }
        if (daysUntilStockout == null) {
            return stock <= 5 ? "MEDIUM" : "LOW";
        }
        if (daysUntilStockout <= 7.0) {
            return "HIGH";
        }
        if (daysUntilStockout <= 21.0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String riskLevelWithoutHistory(int stock) {
        if (stock <= 0) {
            return "HIGH";
        }
        if (stock <= 5) {
            return "HIGH";
        }
        if (stock <= 12) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildSummary(
            int totalQuantity,
            long observedDays,
            double dailyDemand,
            Double daysUntilStockout,
            int suggestedRestock,
            String riskLevel
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("Demand avg ").append(formatDecimal(dailyDemand)).append("/day over ").append(observedDays).append(" day(s)");
        builder.append(" | ").append(totalQuantity).append(" units ordered");
        if (daysUntilStockout != null) {
            builder.append(" | stockout in about ").append(formatDecimal(daysUntilStockout)).append(" day(s)");
        }
        if (suggestedRestock > 0) {
            builder.append(" | restock +").append(suggestedRestock);
        }
        builder.append(" | risk ").append(riskLevel);
        return builder.toString();
    }

    private String buildNoHistorySummary(int stock, String riskLevel) {
        if ("HIGH".equals(riskLevel)) {
            return "Aucune commande recente, mais le stock actuel est critique (" + stock + ").";
        }
        if ("MEDIUM".equals(riskLevel)) {
            return "Aucune commande recente, mais le stock actuel reste limite (" + stock + ").";
        }
        return "Aucune commande recente pour ce produit.";
    }

    private TrendingProductSnapshot buildTrendingSnapshot(Product product, List<Order> orders) {
        if (product == null || product.getId() == null) {
            return TrendingProductSnapshot.empty();
        }

        List<Order> productOrders = orders == null
                ? List.of()
                : orders.stream()
                .filter(order -> Objects.equals(product.getId(), order.getProductId()))
                .filter(order -> order.getQuantity() != null && order.getQuantity() > 0)
                .toList();

        int totalOrderedQuantity = productOrders.stream()
                .mapToInt(order -> order.getQuantity() == null ? 0 : order.getQuantity())
                .sum();

        return new TrendingProductSnapshot(
                product.getId(),
                fallback(product.getName(), "Produit"),
                totalOrderedQuantity,
                productOrders.size(),
                totalOrderedQuantity <= 0
                        ? "Aucune commande tendance."
                        : totalOrderedQuantity + " unites sur " + productOrders.size() + " commande(s)."
        );
    }

    private String formatDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public record ProductDemandSnapshot(
            Integer productId,
            String productName,
            double averageDailyDemand,
            Double daysUntilStockout,
            int suggestedRestock,
            String riskLevel,
            String summary
    ) {
        public static ProductDemandSnapshot empty() {
            return new ProductDemandSnapshot(null, "-", 0.0, null, 0, "LOW", "No forecast data.");
        }

        public int riskWeight() {
            return switch (riskLevel == null ? "LOW" : riskLevel.toUpperCase(Locale.ROOT)) {
                case "HIGH" -> 3;
                case "MEDIUM" -> 2;
                default -> 1;
            };
        }
    }

    public record TrendingProductSnapshot(
            Integer productId,
            String productName,
            int totalOrderedQuantity,
            int orderCount,
            String summary
    ) {
        public static TrendingProductSnapshot empty() {
            return new TrendingProductSnapshot(null, "-", 0, 0, "Aucune tendance.");
        }
    }

    public record DashboardDemandSummary(
            int restockSoonCount,
            ProductDemandSnapshot topRiskProduct
    ) {
    }

    public record TrendingSelection(
            List<Product> products,
            List<TrendingProductSnapshot> ranking,
            int limit,
            String summary
    ) {
    }
}

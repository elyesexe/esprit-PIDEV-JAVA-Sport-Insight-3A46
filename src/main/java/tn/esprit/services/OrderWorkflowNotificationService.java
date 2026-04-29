package tn.esprit.services;

import tn.esprit.entities.Notification;
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;
import tn.esprit.gui.NavbarNotificationCenter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class OrderWorkflowNotificationService {
    private final NotificationService notificationService;

    public OrderWorkflowNotificationService() throws SQLException {
        this.notificationService = new NotificationService();
    }

    public boolean notifyCartAdded(Integer userId, Product product, int itemCount, BigDecimal total) {
        return pushSingle(
                userId,
                "Produit ajoute au panier",
                fallback(product == null ? null : product.getName(), "Produit")
                        + " a ete ajoute. Total panier: " + formatPrice(total)
                        + ". Ouvrez Paiement pour valider l'achat.",
                Notification.TYPE_STORE_CART,
                fallback(product == null ? null : product.getName(), "Produit"),
                itemCount + " article(s)",
                "emerald"
        );
    }

    public boolean notifyPaymentSucceeded(Integer userId, int orderCount, BigDecimal total, Path invoicePath) {
        String suffix = invoicePath == null
                ? "Paiement confirme."
                : "Facture generee: " + invoicePath.getFileName() + ".";
        return pushSingle(
                userId,
                "Paiement valide",
                orderCount + " commande(s) enregistree(s) pour " + formatPrice(total) + ". " + suffix,
                Notification.TYPE_PAYMENT_SUCCESS,
                "Commande confirmee",
                formatPrice(total),
                "emerald"
        );
    }

    public boolean notifyPaymentFailed(Integer userId, BigDecimal total, String reason) {
        return pushSingle(
                userId,
                "Paiement impossible",
                fallback(reason, "Le paiement n'a pas pu etre valide.")
                        + (total == null ? "" : " Total concerne: " + formatPrice(total) + "."),
                Notification.TYPE_PAYMENT_FAILED,
                "Paiement a reprendre",
                total == null ? "Verification requise" : formatPrice(total),
                "rose"
        );
    }

    public boolean notifyOrderCreated(Collection<Integer> userIds, Order order, Product product) {
        return pushMany(
                userIds,
                "Commande creee",
                buildOrderMessage(order, product, "cree"),
                Notification.TYPE_ORDER_CREATED,
                resolveProductLabel(order, product),
                "Commande #" + fallback(order == null ? null : order.getId(), 0),
                "emerald"
        );
    }

    public boolean notifyOrderUpdated(Collection<Integer> userIds, Order order, Product product) {
        return pushMany(
                userIds,
                "Commande modifiee",
                buildOrderMessage(order, product, "mise a jour"),
                Notification.TYPE_ORDER_UPDATED,
                resolveProductLabel(order, product),
                "Commande #" + fallback(order == null ? null : order.getId(), 0),
                "amber"
        );
    }

    public boolean notifyOrderDeleted(Collection<Integer> userIds, Integer orderId, String productLabel) {
        return pushMany(
                userIds,
                "Commande supprimee",
                "La commande #" + fallback(orderId, 0) + " a ete supprimee.",
                Notification.TYPE_ORDER_DELETED,
                fallback(productLabel, "Commande"),
                "Commande #" + fallback(orderId, 0),
                "rose"
        );
    }

    public boolean notifyInvoiceExported(Collection<Integer> userIds, Integer orderId, Path target) {
        return pushMany(
                userIds,
                "Facture exportee",
                "La facture de la commande #" + fallback(orderId, 0) + " a ete exportee vers "
                        + fallback(target == null ? null : target.getFileName().toString(), "un fichier PDF") + ".",
                Notification.TYPE_INVOICE_READY,
                "Commande #" + fallback(orderId, 0),
                fallback(target == null ? null : target.getFileName().toString(), "PDF"),
                "emerald"
        );
    }

    public boolean notifyOrdersExported(Integer userId, int count, Path target) {
        return pushSingle(
                userId,
                "Export des commandes",
                count + " commande(s) exportee(s) dans "
                        + fallback(target == null ? null : target.getFileName().toString(), "le PDF") + ".",
                Notification.TYPE_ORDER_EXPORT,
                "Commandes exportees",
                count + " element(s)",
                "slate"
        );
    }

    public boolean notifyOrderEmailResult(Collection<Integer> userIds, Integer orderId, OrderNotificationService.DeliveryResult result) {
        if (result == null) {
            return false;
        }
        String message = result.delivered()
                ? "La notification email de la commande #" + fallback(orderId, 0) + " a ete envoyee."
                : result.previewPath() != null
                ? "Preview locale de la commande #" + fallback(orderId, 0) + " enregistree: " + result.previewPath().getFileName() + "."
                : fallback(result.message(), "Notification email non envoyee.");
        String accent = result.delivered() ? "emerald" : "amber";
        String meta = result.delivered()
                ? "Email envoye"
                : result.previewPath() != null
                ? result.previewPath().getFileName().toString()
                : "Verification requise";
        return pushMany(
                userIds,
                "Notification commande",
                message,
                Notification.TYPE_ORDER_EMAIL,
                "Commande #" + fallback(orderId, 0),
                meta,
                accent
        );
    }

    private String buildOrderMessage(Order order, Product product, String action) {
        String orderId = String.valueOf(fallback(order == null ? null : order.getId(), 0));
        String productLabel = resolveProductLabel(order, product);
        String paymentStatus = fallback(order == null ? null : order.getPaymentStatus(), "PENDING");
        String total = formatPrice(order == null ? null : order.getTotalAmount());
        return "La commande #" + orderId + " a ete " + action + " pour " + productLabel
                + ". Paiement: " + paymentStatus + ". Total: " + total + ".";
    }

    private String resolveProductLabel(Order order, Product product) {
        if (product != null && product.getName() != null && !product.getName().isBlank()) {
            return product.getName().trim();
        }
        Integer productId = order == null ? null : order.getProductId();
        return productId == null ? "Produit" : "Produit #" + productId;
    }

    private boolean pushMany(Collection<Integer> userIds, String title, String message, String type, String actorName, String minuteLabel, String accentTone) {
        boolean createdAny = false;
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        Set<Integer> recipients = new LinkedHashSet<>();
        for (Integer userId : userIds) {
            if (userId != null && userId > 0) {
                recipients.add(userId);
            }
        }
        for (Integer userId : recipients) {
            createdAny |= pushSingle(userId, title, message, type, actorName, minuteLabel, accentTone);
        }
        return createdAny;
    }

    private boolean pushSingle(Integer userId, String title, String message, String type, String actorName, String minuteLabel, String accentTone) {
        if (userId == null || userId <= 0) {
            return false;
        }
        try {
            Notification notification = new Notification(
                    title,
                    message,
                    type,
                    LocalDateTime.now(),
                    false,
                    userId,
                    null,
                    buildDedupeKey(type, userId),
                    resolveGroupLabel(type),
                    null,
                    null,
                    null,
                    null,
                    actorName,
                    minuteLabel,
                    accentTone
            );
            Notification created = notificationService.createIfAbsent(notification);
            if (created != null) {
                NavbarNotificationCenter.requestRefreshAll();
                return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    private String buildDedupeKey(String type, Integer userId) {
        return fallback(type, "workflow") + "-" + fallback(userId, 0) + "-" + System.currentTimeMillis();
    }

    private String resolveGroupLabel(String type) {
        if (type == null) {
            return "Workflow";
        }
        return switch (type) {
            case Notification.TYPE_STORE_CART,
                 Notification.TYPE_PAYMENT_SUCCESS,
                 Notification.TYPE_PAYMENT_FAILED -> "Paiement";
            case Notification.TYPE_INVOICE_READY -> "Facture";
            default -> "Commande";
        };
    }

    private String formatPrice(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return safe.toPlainString() + " DT";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int fallback(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}

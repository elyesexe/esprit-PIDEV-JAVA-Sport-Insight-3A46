package tn.esprit.services;

import tn.esprit.entities.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StoreCartSession {
    private static final Integer GUEST_KEY = 0;
    private static final Map<Integer, List<CartEntry>> CARTS_BY_USER = new ConcurrentHashMap<>();

    private StoreCartSession() {
    }

    public static List<CartEntry> loadCart(Integer userId) {
        List<CartEntry> entries = CARTS_BY_USER.get(resolveUserKey(userId));
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<CartEntry> copies = new ArrayList<>(entries.size());
        for (CartEntry entry : entries) {
            if (entry != null && entry.product() != null && entry.quantity() > 0) {
                copies.add(new CartEntry(copyProduct(entry.product()), entry.quantity()));
            }
        }
        return copies;
    }

    public static void saveCart(Integer userId, List<CartEntry> entries) {
        Integer key = resolveUserKey(userId);
        if (entries == null || entries.isEmpty()) {
            CARTS_BY_USER.remove(key);
            return;
        }

        List<CartEntry> sanitized = new ArrayList<>();
        for (CartEntry entry : entries) {
            if (entry == null || entry.product() == null || entry.quantity() <= 0) {
                continue;
            }
            sanitized.add(new CartEntry(copyProduct(entry.product()), entry.quantity()));
        }

        if (sanitized.isEmpty()) {
            CARTS_BY_USER.remove(key);
        } else {
            CARTS_BY_USER.put(key, sanitized);
        }
    }

    public static void clearCart(Integer userId) {
        CARTS_BY_USER.remove(resolveUserKey(userId));
    }

    private static Integer resolveUserKey(Integer userId) {
        return userId == null ? GUEST_KEY : userId;
    }

    private static Product copyProduct(Product product) {
        if (product == null) {
            return null;
        }
        return new Product(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getStock(),
                product.getSize(),
                product.getBrand(),
                product.getImage(),
                product.getDescription(),
                product.getTags()
        );
    }

    public record CartEntry(Product product, int quantity) {
    }
}

package tn.esprit.services;

import tn.esprit.entities.Order;
import tn.esprit.entities.Product;
import tn.esprit.tools.MyConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
<<<<<<< HEAD
import java.sql.Statement;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
<<<<<<< HEAD
import java.util.Map;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import java.util.Set;

public class OrderService implements IService<Order> {
    private static final List<String> ORDER_STATUSES = List.of(
<<<<<<< HEAD
            "pending",
            "confirmed",
            "shipped",
            "delivered",
            "rejected"
    );
    private static final List<String> PAYMENT_METHODS = List.of(
            "cod",
            "online"
    );
    private static final List<String> PAYMENT_STATUSES = List.of(
            "pending",
            "paid",
            "failed"
=======
            "PENDING",
            "CONFIRMED",
            "SHIPPED",
            "DELIVERED",
            "CANCELLED"
    );
    private static final List<String> PAYMENT_METHODS = List.of(
            "CARD",
            "CASH",
            "CASH_ON_DELIVERY",
            "BANK_TRANSFER"
    );
    private static final List<String> PAYMENT_STATUSES = List.of(
            "UNPAID",
            "PENDING",
            "PAID",
            "FAILED",
            "REFUNDED"
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    );
    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.copyOf(ORDER_STATUSES);
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.copyOf(PAYMENT_METHODS);
    private static final Set<String> ALLOWED_PAYMENT_STATUSES = Set.copyOf(PAYMENT_STATUSES);
<<<<<<< HEAD
    private static final Map<String, String> ORDER_STATUS_ALIASES = Map.of(
            "PENDING", "pending",
            "CONFIRMED", "confirmed",
            "SHIPPED", "shipped",
            "DELIVERED", "delivered",
            "CANCELLED", "rejected",
            "CANCELED", "rejected",
            "REJECTED", "rejected"
    );
    private static final Map<String, String> PAYMENT_METHOD_ALIASES = Map.of(
            "COD", "cod",
            "CASH", "cod",
            "CASH_ON_DELIVERY", "cod",
            "CARD", "online",
            "ONLINE", "online",
            "STRIPE", "online",
            "STRIPE_CHECKOUT", "online"
    );
    private static final Map<String, String> PAYMENT_STATUS_ALIASES = Map.of(
            "UNPAID", "pending",
            "PENDING", "pending",
            "PAID", "paid",
            "FAILED", "failed",
            "REFUNDED", "failed"
    );

    private static final int MIN_ADDRESS_LENGTH = 8;
    private static final int MAX_ADDRESS_LENGTH = 300;
    private static final int MAX_CLIENT_NAME_LENGTH = 120;
=======

    private static final int MIN_ADDRESS_LENGTH = 8;
    private static final int MAX_ADDRESS_LENGTH = 300;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_SIZE_LENGTH = 20;

    private final Connection connection;

    public OrderService() throws SQLException {
        this(MyConnection.getInstance().getConnection());
    }

    OrderService(Connection connection) {
        this.connection = connection;
    }

    public static List<String> allowedOrderStatuses() {
        return ORDER_STATUSES;
    }

    public static List<String> allowedPaymentMethods() {
        return PAYMENT_METHODS;
    }

    public static List<String> allowedPaymentStatuses() {
        return PAYMENT_STATUSES;
    }

    @Override
    public void add(Order order) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            Product product = getProductById(order == null ? null : order.getProductId(), true);
            Order normalized = normalizeForPersistence(order, product, false);
            ensureStockAvailable(product, normalized.getQuantity());

            insertOrder(normalized);
            updateProductStock(product.getId(), product.getStock() - normalized.getQuantity());

            connection.commit();
        } catch (RuntimeException | SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    @Override
    public void update(Order order) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            if (order == null || order.getId() == null || order.getId() <= 0) {
                throw new IllegalArgumentException("The order id is required for updates.");
            }

            Order existingOrder = getById(order.getId(), true);
            if (existingOrder == null) {
                throw new IllegalArgumentException("The order to update was not found.");
            }

            restoreStock(existingOrder);

            Product product = getProductById(order.getProductId(), true);
            Order normalized = normalizeForPersistence(order, product, true);
            ensureStockAvailable(product, normalized.getQuantity());

            updateOrderRow(normalized);
            updateProductStock(product.getId(), product.getStock() - normalized.getQuantity());

            connection.commit();
        } catch (RuntimeException | SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("The order id is invalid.");
        }

        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            Order existingOrder = getById(id, true);
            if (existingOrder == null) {
                throw new IllegalArgumentException("The order to delete was not found.");
            }

            restoreStock(existingOrder);

            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `order` WHERE id = ?")) {
                statement.setInt(1, id);
                statement.executeUpdate();
            }

            connection.commit();
        } catch (RuntimeException | SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    @Override
    public List<Order> getAll() throws SQLException {
        String sql = """
                SELECT id, quantity, order_date, status, payment_method, payment_status, size,
<<<<<<< HEAD
                       client_name, contact_email, contact_phone, shipping_address, billing_address,
=======
                       contact_email, contact_phone, shipping_address, billing_address,
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                       total_amount, product_id, entraineur_id
                FROM `order`
                ORDER BY order_date DESC, id DESC
                """;

        List<Order> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                orders.add(mapRow(resultSet));
            }
        }
        return orders;
    }

    @Override
    public Order getById(int id) throws SQLException {
        return getById(id, false);
    }

    @Override
    public List<Order> search(String keyword) throws SQLException {
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword == null) {
            return getAll();
        }

        String sql = """
                SELECT id, quantity, order_date, status, payment_method, payment_status, size,
<<<<<<< HEAD
                       client_name, contact_email, contact_phone, shipping_address, billing_address,
=======
                       contact_email, contact_phone, shipping_address, billing_address,
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                       total_amount, product_id, entraineur_id
                FROM `order`
                WHERE LOWER(COALESCE(status, '')) LIKE ?
                   OR LOWER(COALESCE(payment_method, '')) LIKE ?
                   OR LOWER(COALESCE(payment_status, '')) LIKE ?
<<<<<<< HEAD
                   OR LOWER(COALESCE(client_name, '')) LIKE ?
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                   OR LOWER(COALESCE(contact_email, '')) LIKE ?
                   OR LOWER(COALESCE(contact_phone, '')) LIKE ?
                   OR CAST(id AS CHAR) LIKE ?
                   OR CAST(COALESCE(product_id, 0) AS CHAR) LIKE ?
                ORDER BY order_date DESC, id DESC
                """;

        String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            statement.setString(5, pattern);
            statement.setString(6, pattern);
            statement.setString(7, pattern);
<<<<<<< HEAD
            statement.setString(8, pattern);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(mapRow(resultSet));
                }
            }
        }
        return orders;
    }

    static Order normalizeForPersistence(Order order, Product product, boolean updateMode) {
        if (order == null) {
            throw new IllegalArgumentException("The order is required.");
        }

        Integer id = order.getId();
        if (updateMode && (id == null || id <= 0)) {
            throw new IllegalArgumentException("The order id is required.");
        }

        if (product == null || product.getId() == null || product.getId() <= 0) {
            throw new IllegalArgumentException("The ordered product was not found.");
        }

        Integer quantity = order.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be strictly positive.");
        }

        LocalDate orderDate = order.getOrderDate() == null ? LocalDate.now() : order.getOrderDate();

<<<<<<< HEAD
        String paymentMethod = normalizeEnum(order.getPaymentMethod(), ALLOWED_PAYMENT_METHODS, PAYMENT_METHOD_ALIASES, true, "Payment method is required.");
        String paymentStatus = normalizeEnum(order.getPaymentStatus(), ALLOWED_PAYMENT_STATUSES, PAYMENT_STATUS_ALIASES, false, null);
        if (paymentStatus == null) {
            paymentStatus = "cod".equals(paymentMethod) ? "pending" : "paid";
        }

        String status = normalizeEnum(order.getStatus(), ALLOWED_ORDER_STATUSES, ORDER_STATUS_ALIASES, false, null);
        if (status == null) {
            status = "paid".equals(paymentStatus) ? "confirmed" : "pending";
=======
        String paymentMethod = normalizeEnum(order.getPaymentMethod(), ALLOWED_PAYMENT_METHODS, true, "Payment method is required.");
        String paymentStatus = normalizeEnum(order.getPaymentStatus(), ALLOWED_PAYMENT_STATUSES, false, null);
        if (paymentStatus == null) {
            paymentStatus = ("CASH".equals(paymentMethod) || "CASH_ON_DELIVERY".equals(paymentMethod)) ? "PENDING" : "PAID";
        }

        String status = normalizeEnum(order.getStatus(), ALLOWED_ORDER_STATUSES, false, null);
        if (status == null) {
            status = ("PAID".equals(paymentStatus) || "CONFIRMED".equals(order.getStatus())) ? "CONFIRMED" : "PENDING";
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }

        String size = normalizeOptionalText(order.getSize(), MAX_SIZE_LENGTH, "Size is too long.");
        if (size == null) {
            size = normalizeOptionalText(product.getSize(), MAX_SIZE_LENGTH, "Product size is invalid.");
        }

        String contactEmail = normalizeEmail(order.getContactEmail());
        String contactPhone = normalizePhone(order.getContactPhone());
        String shippingAddress = normalizeAddress(order.getShippingAddress(), "Shipping address is required.");
        String billingAddress = normalizeAddress(order.getBillingAddress(), "Billing address is required.");
<<<<<<< HEAD
        String clientName = normalizeClientName(order.getClientName(), contactEmail, contactPhone);
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

        BigDecimal totalAmount = normalizeTotalAmount(order.getTotalAmount(), product, quantity);
        Integer entraineurId = normalizeOptionalPositiveInteger(order.getEntraineurId(), "Coach id is invalid.");

        return new Order(
                id,
                quantity,
                orderDate,
<<<<<<< HEAD
                clientName,
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                status,
                paymentMethod,
                paymentStatus,
                size,
                contactEmail,
                contactPhone,
                shippingAddress,
                billingAddress,
                totalAmount,
                product.getId(),
                entraineurId
        );
    }

    private void insertOrder(Order order) throws SQLException {
        String sql = """
                INSERT INTO `order`
<<<<<<< HEAD
                    (quantity, order_date, client_name, status, payment_method, payment_status, size,
                     contact_email, contact_phone, shipping_address, billing_address,
                     total_amount, product_id, entraineur_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindOrder(statement, order, false);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    order.setId(generatedKeys.getInt(1));
                }
            }
=======
                    (quantity, order_date, status, payment_method, payment_status, size,
                     contact_email, contact_phone, shipping_address, billing_address,
                     total_amount, product_id, entraineur_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindOrder(statement, order, false);
            statement.executeUpdate();
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    private void updateOrderRow(Order order) throws SQLException {
        String sql = """
                UPDATE `order`
<<<<<<< HEAD
                SET quantity = ?, order_date = ?, client_name = ?, status = ?, payment_method = ?, payment_status = ?, size = ?,
=======
                SET quantity = ?, order_date = ?, status = ?, payment_method = ?, payment_status = ?, size = ?,
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                    contact_email = ?, contact_phone = ?, shipping_address = ?, billing_address = ?,
                    total_amount = ?, product_id = ?, entraineur_id = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindOrder(statement, order, true);
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new IllegalArgumentException("The order to update was not found.");
            }
        }
    }

    private void bindOrder(PreparedStatement statement, Order order, boolean includeId) throws SQLException {
        statement.setInt(1, order.getQuantity());
        statement.setDate(2, Date.valueOf(order.getOrderDate()));
<<<<<<< HEAD
        statement.setString(3, order.getClientName());
        statement.setString(4, order.getStatus());
        statement.setString(5, order.getPaymentMethod());
        statement.setString(6, order.getPaymentStatus());
        statement.setString(7, order.getSize());
        statement.setString(8, order.getContactEmail());
        statement.setString(9, order.getContactPhone());
        statement.setString(10, order.getShippingAddress());
        statement.setString(11, order.getBillingAddress());
        statement.setBigDecimal(12, order.getTotalAmount());
        statement.setInt(13, order.getProductId());
        setNullableInteger(statement, 14, order.getEntraineurId());
        if (includeId) {
            statement.setInt(15, order.getId());
=======
        statement.setString(3, order.getStatus());
        statement.setString(4, order.getPaymentMethod());
        statement.setString(5, order.getPaymentStatus());
        statement.setString(6, order.getSize());
        statement.setString(7, order.getContactEmail());
        statement.setString(8, order.getContactPhone());
        statement.setString(9, order.getShippingAddress());
        statement.setString(10, order.getBillingAddress());
        statement.setBigDecimal(11, order.getTotalAmount());
        statement.setInt(12, order.getProductId());
        setNullableInteger(statement, 13, order.getEntraineurId());
        if (includeId) {
            statement.setInt(14, order.getId());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        }
    }

    private Order getById(int id, boolean forUpdate) throws SQLException {
        if (id <= 0) {
            return null;
        }

        String sql = """
                SELECT id, quantity, order_date, status, payment_method, payment_status, size,
<<<<<<< HEAD
                       client_name, contact_email, contact_phone, shipping_address, billing_address,
=======
                       contact_email, contact_phone, shipping_address, billing_address,
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                       total_amount, product_id, entraineur_id
                FROM `order`
                WHERE id = ?
                """ + (forUpdate ? " FOR UPDATE" : "");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    private Product getProductById(Integer productId, boolean forUpdate) throws SQLException {
        if (productId == null || productId <= 0) {
            return null;
        }

        String sql = """
                SELECT id, name, category, price, stock, size, brand, image
                FROM product
                WHERE id = ?
                """ + (forUpdate ? " FOR UPDATE" : "");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new Product(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getString("category"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getInt("stock"),
                        resultSet.getString("size"),
                        resultSet.getString("brand"),
                        resultSet.getString("image")
                );
            }
        }
    }

    private void ensureStockAvailable(Product product, Integer quantity) {
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for \"" + product.getName() + "\". Available: " + product.getStock() + "."
            );
        }
    }

    private void updateProductStock(Integer productId, int newStock) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE product SET stock = ? WHERE id = ?")) {
            statement.setInt(1, newStock);
            statement.setInt(2, productId);
            statement.executeUpdate();
        }
    }

    private void restoreStock(Order order) throws SQLException {
        Product product = getProductById(order.getProductId(), true);
        if (product == null) {
            return;
        }
        updateProductStock(product.getId(), product.getStock() + order.getQuantity());
    }

    private Order mapRow(ResultSet resultSet) throws SQLException {
        Date orderDate = resultSet.getDate("order_date");
        Integer productId = getNullableInteger(resultSet, "product_id");
        Integer entraineurId = getNullableInteger(resultSet, "entraineur_id");

        return new Order(
                resultSet.getInt("id"),
                resultSet.getInt("quantity"),
                orderDate == null ? null : orderDate.toLocalDate(),
<<<<<<< HEAD
                resultSet.getString("client_name"),
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                resultSet.getString("status"),
                resultSet.getString("payment_method"),
                resultSet.getString("payment_status"),
                resultSet.getString("size"),
                resultSet.getString("contact_email"),
                resultSet.getString("contact_phone"),
                resultSet.getString("shipping_address"),
                resultSet.getString("billing_address"),
                resultSet.getBigDecimal("total_amount"),
                productId,
                entraineurId
        );
    }

    private static BigDecimal normalizeTotalAmount(BigDecimal totalAmount, Product product, int quantity) {
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return totalAmount.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal price = product.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The product price is invalid for this order.");
        }
        return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

<<<<<<< HEAD
    private static String normalizeEnum(
            String value,
            Set<String> allowedValues,
            Map<String, String> aliases,
            boolean required,
            String requiredMessage
    ) {
=======
    private static String normalizeEnum(String value, Set<String> allowedValues, boolean required, String requiredMessage) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        String normalized = trimToNull(value);
        if (normalized == null) {
            if (required) {
                throw new IllegalArgumentException(requiredMessage);
            }
            return null;
        }

<<<<<<< HEAD
        String aliasKey = normalized.toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        String aliased = aliases.get(aliasKey);
        if (aliased != null) {
            return aliased;
        }

        String canonical = normalized.toLowerCase(Locale.ROOT)
=======
        String canonical = normalized.toUpperCase(Locale.ROOT)
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                .replace(' ', '_')
                .replace('-', '_');
        if (!allowedValues.contains(canonical)) {
            throw new IllegalArgumentException("Invalid value: " + normalized + ".");
        }
        return canonical;
    }

    private static String normalizeEmail(String email) {
        String normalized = trimToNull(email);
        if (normalized == null) {
            throw new IllegalArgumentException("Contact email is required.");
        }
        if (!normalized.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Contact email is invalid.");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String phone) {
        String normalized = trimToNull(phone);
        if (normalized == null) {
            throw new IllegalArgumentException("Contact phone is required.");
        }
        String compact = normalized.replaceAll("[\\s().-]", "");
        if (!compact.matches("^\\+?[0-9]{8," + MAX_PHONE_LENGTH + "}$")) {
            throw new IllegalArgumentException("Contact phone is invalid.");
        }
        return normalized;
    }

<<<<<<< HEAD
    private static String normalizeClientName(String clientName, String contactEmail, String contactPhone) {
        String normalized = trimToNull(clientName);
        if (normalized != null) {
            if (normalized.length() > MAX_CLIENT_NAME_LENGTH) {
                throw new IllegalArgumentException("Client name is too long.");
            }
            return normalized;
        }
        if (contactEmail != null && contactEmail.contains("@")) {
            return contactEmail.substring(0, contactEmail.indexOf('@'));
        }
        if (contactPhone != null) {
            return "Client " + contactPhone;
        }
        return "Client Sport Insight";
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private static String normalizeAddress(String address, String requiredMessage) {
        String normalized = trimToNull(address);
        if (normalized == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (normalized.length() < MIN_ADDRESS_LENGTH || normalized.length() > MAX_ADDRESS_LENGTH) {
            throw new IllegalArgumentException(
                    "Addresses must contain between " + MIN_ADDRESS_LENGTH + " and " + MAX_ADDRESS_LENGTH + " characters."
            );
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value, int maxLength, String errorMessage) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private static Integer normalizeOptionalPositiveInteger(Integer value, String errorMessage) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private static Integer getNullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

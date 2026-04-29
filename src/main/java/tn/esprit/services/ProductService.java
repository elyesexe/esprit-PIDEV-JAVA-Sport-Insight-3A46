package tn.esprit.services;

import tn.esprit.entities.Product;
import tn.esprit.repositories.ProductRepository;
import tn.esprit.tools.MyConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductService implements IService<Product> {
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MIN_CATEGORY_LENGTH = 2;
    private static final int MAX_CATEGORY_LENGTH = 80;
    private static final int MIN_BRAND_LENGTH = 2;
    private static final int MAX_BRAND_LENGTH = 60;
    private static final int MAX_SIZE_LENGTH = 20;
    private static final int MAX_IMAGE_LENGTH = 255;
    private static final int MAX_STOCK = 100_000;
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999.99");

    private final Connection connection;

    public ProductService() throws SQLException {
        this(MyConnection.getInstance().getConnection());
    }

    ProductService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void add(Product product) throws SQLException {
        Product normalized = normalizeForPersistence(product, false);
        ensureNoDuplicateProduct(normalized, null);

        String sql = """
                INSERT INTO product (name, category, price, stock, size, brand, image)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProduct(statement, normalized, false);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    normalized.setId(generatedId);
                    product.setId(generatedId);
                }
            }
        }
    }

    @Override
    public void update(Product product) throws SQLException {
        Product normalized = normalizeForPersistence(product, true);
        ensureNoDuplicateProduct(normalized, normalized.getId());

        String sql = """
                UPDATE product
                SET name = ?, category = ?, price = ?, stock = ?, size = ?, brand = ?, image = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindProduct(statement, normalized, true);
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new IllegalArgumentException("The product to update was not found.");
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("The product id is invalid.");
        }

        if (hasRelatedOrders(id)) {
            throw new IllegalArgumentException("This product already has linked orders and cannot be deleted.");
        }

        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM product WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Product> getAll() throws SQLException {
        return getAllSorted("id", true);
    }

    @Override
    public Product getById(int id) throws SQLException {
        if (id <= 0) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, category, price, stock, size, brand, image FROM product WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    @Override
    public List<Product> search(String keyword) throws SQLException {
        return search(keyword, "name", true);
    }

    public List<Product> findProducts(
            String keyword,
            ProductRepository.ProductSortField sortField,
            ProductRepository.SortDirection sortDirection
    ) throws SQLException {
        String resolvedSortField = sortField == null ? "name" : sortField.name();
        boolean ascending = sortDirection == null || sortDirection == ProductRepository.SortDirection.ASC;
        return search(keyword, resolvedSortField, ascending);
    }

    public Map<String, String> validate(Product product) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (product == null) {
            errors.put("global", "The product is required.");
            return errors;
        }

        validateRequiredText(errors, "name", product.getName(), "Product name is required.", MIN_NAME_LENGTH, MAX_NAME_LENGTH,
                "Product name must contain between %d and %d characters.".formatted(MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        validateRequiredText(errors, "category", product.getCategory(), "Category is required.", MIN_CATEGORY_LENGTH, MAX_CATEGORY_LENGTH,
                "Category must contain between %d and %d characters.".formatted(MIN_CATEGORY_LENGTH, MAX_CATEGORY_LENGTH));
        validateRequiredText(errors, "brand", product.getBrand(), "Brand is required.", MIN_BRAND_LENGTH, MAX_BRAND_LENGTH,
                "Brand must contain between %d and %d characters.".formatted(MIN_BRAND_LENGTH, MAX_BRAND_LENGTH));

        if (product.getPrice() == null) {
            errors.put("price", "Price is required.");
        } else {
            BigDecimal normalized = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
                errors.put("price", "Price must be strictly positive.");
            } else if (normalized.compareTo(MAX_PRICE) > 0) {
                errors.put("price", "Price cannot exceed " + MAX_PRICE + ".");
            }
        }

        if (product.getStock() < 0 || product.getStock() > MAX_STOCK) {
            errors.put("stock", "Stock must stay between 0 and " + MAX_STOCK + ".");
        }

        validateOptionalText(errors, "size", product.getSize(), MAX_SIZE_LENGTH,
                "Size cannot exceed %d characters.".formatted(MAX_SIZE_LENGTH));
        validateOptionalText(errors, "image", product.getImage(), MAX_IMAGE_LENGTH,
                "Image path cannot exceed %d characters.".formatted(MAX_IMAGE_LENGTH));

        return errors;
    }

    public List<Product> search(String keyword, String sortField, boolean ascending) throws SQLException {
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword == null) {
            return getAllSorted(sortField, ascending);
        }

        String sql = """
                SELECT id, name, category, price, stock, size, brand, image
                FROM product
                WHERE LOWER(COALESCE(name, '')) LIKE ?
                   OR LOWER(COALESCE(category, '')) LIKE ?
                   OR LOWER(COALESCE(brand, '')) LIKE ?
                ORDER BY %s %s, id ASC
                """.formatted(resolveSortColumn(sortField), ascending ? "ASC" : "DESC");

        String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(mapRow(resultSet));
                }
            }
        }

        return products;
    }

    public List<Product> advancedSearch(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minStock,
            String size,
            boolean inStockOnly
    ) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, category, price, stock, size, brand, image
                FROM product
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();

        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            sql.append("""
                     AND (LOWER(COALESCE(name, '')) LIKE ?
                       OR LOWER(COALESCE(category, '')) LIKE ?
                       OR LOWER(COALESCE(brand, '')) LIKE ?)
                    """);
            String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        String normalizedCategory = trimToNull(category);
        if (normalizedCategory != null) {
            sql.append(" AND LOWER(COALESCE(category, '')) = ?");
            params.add(normalizedCategory.toLowerCase(Locale.ROOT));
        }

        String normalizedBrand = trimToNull(brand);
        if (normalizedBrand != null) {
            sql.append(" AND LOWER(COALESCE(brand, '')) = ?");
            params.add(normalizedBrand.toLowerCase(Locale.ROOT));
        }

        if (minPrice != null) {
            sql.append(" AND price >= ?");
            params.add(minPrice);
        }

        if (maxPrice != null) {
            sql.append(" AND price <= ?");
            params.add(maxPrice);
        }

        if (minStock != null) {
            sql.append(" AND stock >= ?");
            params.add(minStock);
        }

        String normalizedSize = trimToNull(size);
        if (normalizedSize != null) {
            sql.append(" AND LOWER(COALESCE(size, '')) = ?");
            params.add(normalizedSize.toLowerCase(Locale.ROOT));
        }

        if (inStockOnly) {
            sql.append(" AND stock > 0");
        }

        sql.append(" ORDER BY name ASC, id ASC");

        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(mapRow(resultSet));
                }
            }
        }
        return products;
    }

    public List<Product> getAllSorted(String sortField, boolean ascending) throws SQLException {
        String sql = """
                SELECT id, name, category, price, stock, size, brand, image
                FROM product
                ORDER BY %s %s, id ASC
                """.formatted(resolveSortColumn(sortField), ascending ? "ASC" : "DESC");

        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(mapRow(resultSet));
            }
        }
        return products;
    }

    static Product normalizeForPersistence(Product product, boolean updateMode) {
        if (product == null) {
            throw new IllegalArgumentException("The product is required.");
        }

        Integer id = product.getId();
        if (updateMode && (id == null || id <= 0)) {
            throw new IllegalArgumentException("The product id is required for updates.");
        }

        String name = normalizeRequiredText(product.getName(), "Product name is required.", MIN_NAME_LENGTH, MAX_NAME_LENGTH,
                "Product name must contain between %d and %d characters.".formatted(MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        String category = normalizeRequiredText(product.getCategory(), "Category is required.", MIN_CATEGORY_LENGTH, MAX_CATEGORY_LENGTH,
                "Category must contain between %d and %d characters.".formatted(MIN_CATEGORY_LENGTH, MAX_CATEGORY_LENGTH));
        String brand = normalizeRequiredText(product.getBrand(), "Brand is required.", MIN_BRAND_LENGTH, MAX_BRAND_LENGTH,
                "Brand must contain between %d and %d characters.".formatted(MIN_BRAND_LENGTH, MAX_BRAND_LENGTH));
        String size = normalizeOptionalText(product.getSize(), MAX_SIZE_LENGTH, "Size cannot exceed %d characters.".formatted(MAX_SIZE_LENGTH));
        String image = normalizeOptionalText(product.getImage(), MAX_IMAGE_LENGTH, "Image path cannot exceed %d characters.".formatted(MAX_IMAGE_LENGTH));

        BigDecimal price = normalizePrice(product.getPrice());
        int stock = product.getStock();
        if (stock < 0 || stock > MAX_STOCK) {
            throw new IllegalArgumentException("Stock must stay between 0 and " + MAX_STOCK + ".");
        }

        return new Product(id, name, category, price, stock, size, brand, image);
    }

    private void bindProduct(PreparedStatement statement, Product product, boolean includeId) throws SQLException {
        statement.setString(1, product.getName());
        statement.setString(2, product.getCategory());
        statement.setBigDecimal(3, product.getPrice());
        statement.setInt(4, product.getStock());
        statement.setString(5, product.getSize());
        statement.setString(6, product.getBrand());
        statement.setString(7, product.getImage());
        if (includeId) {
            statement.setInt(8, product.getId());
        }
    }

    private Product mapRow(ResultSet resultSet) throws SQLException {
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

    private boolean hasRelatedOrders(int productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM `order` WHERE product_id = ?")) {
            statement.setInt(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void ensureNoDuplicateProduct(Product product, Integer excludedId) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT id
                FROM product
                WHERE LOWER(TRIM(name)) = ?
                  AND LOWER(TRIM(category)) = ?
                  AND LOWER(TRIM(brand)) = ?
                  AND COALESCE(LOWER(TRIM(size)), '') = ?
                """);

        if (excludedId != null) {
            sql.append(" AND id <> ?");
        }

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, normalizeComparableText(product.getName()));
            statement.setString(2, normalizeComparableText(product.getCategory()));
            statement.setString(3, normalizeComparableText(product.getBrand()));
            statement.setString(4, normalizeComparableText(product.getSize()));
            if (excludedId != null) {
                statement.setInt(5, excludedId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException("A similar product already exists for the same category, brand, and size.");
                }
            }
        }
    }

    private String resolveSortColumn(String sortField) {
        String normalized = trimToNull(sortField);
        if (normalized == null) {
            return "id";
        }

        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "name", "nom" -> "name";
            case "category", "categorie" -> "category";
            case "price", "prix" -> "price";
            case "stock" -> "stock";
            case "brand", "marque" -> "brand";
            default -> "id";
        };
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price is required.");
        }
        BigDecimal normalized = price.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be strictly positive.");
        }
        if (normalized.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Price cannot exceed " + MAX_PRICE + ".");
        }
        return normalized;
    }

    private static String normalizeRequiredText(String value, String requiredMessage, int minLength, int maxLength, String lengthMessage) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value, int maxLength, String lengthMessage) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeComparableText(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT);
    }

    private static void validateRequiredText(
            Map<String, String> errors,
            String field,
            String value,
            String requiredMessage,
            int minLength,
            int maxLength,
            String lengthMessage
    ) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            errors.put(field, requiredMessage);
            return;
        }
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            errors.put(field, lengthMessage);
        }
    }

    private static void validateOptionalText(Map<String, String> errors, String field, String value, int maxLength, String lengthMessage) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > maxLength) {
            errors.put(field, lengthMessage);
        }
    }
}

package tn.esprit.services;

import tn.esprit.entities.Product;
import tn.esprit.tools.MyConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductService implements IService<Product> {
    private final Connection connection;

    public ProductService() throws SQLException {
        connection = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Product product) throws SQLException {
        String sql = "INSERT INTO product (name, category, price, stock, size, brand, image) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getName());
            statement.setString(2, product.getCategory());
            statement.setBigDecimal(3, product.getPrice());
            statement.setInt(4, product.getStock());
            statement.setString(5, product.getSize());
            statement.setString(6, product.getBrand());
            statement.setString(7, product.getImage());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Product product) throws SQLException {
        String sql = "UPDATE product SET name = ?, category = ?, price = ?, stock = ?, size = ?, brand = ?, image = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getName());
            statement.setString(2, product.getCategory());
            statement.setBigDecimal(3, product.getPrice());
            statement.setInt(4, product.getStock());
            statement.setString(5, product.getSize());
            statement.setString(6, product.getBrand());
            statement.setString(7, product.getImage());
            statement.setInt(8, product.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM product WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Product> getAll() throws SQLException {
        String sql = "SELECT id, name, category, price, stock, size, brand, image FROM product";
        List<Product> products = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        }

        return products;
    }

    @Override
    public Product getById(int id) throws SQLException {
        String sql = "SELECT id, name, category, price, stock, size, brand, image FROM product WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    public List<Product> advancedSearch(String keyword, String category, String brand,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        Integer minStock, String size,
                                        boolean inStockOnly) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, name, category, price, stock, size, brand, image FROM product WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(category) LIKE ? OR LOWER(brand) LIKE ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(category) = ?");
            params.add(category.toLowerCase());
        }

        if (brand != null && !brand.isBlank()) {
            sql.append(" AND LOWER(brand) = ?");
            params.add(brand.toLowerCase());
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

        if (size != null && !size.isBlank()) {
            sql.append(" AND LOWER(size) = ?");
            params.add(size.toLowerCase());
        }

        if (inStockOnly) {
            sql.append(" AND stock > 0");
        }

        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        }

        return products;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getBigDecimal("price"),
                rs.getInt("stock"),
                rs.getString("size"),
                rs.getString("brand"),
                rs.getString("image")
        );
    }
}

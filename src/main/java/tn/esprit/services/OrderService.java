package tn.esprit.services;

import tn.esprit.entities.Order;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderService implements IService<Order> {
    private final Connection connection;

    public OrderService() throws SQLException {
        connection = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Order order) throws SQLException {
        String sql = "INSERT INTO `order` (quantity, order_date, status, payment_method, payment_status, size, contact_email, contact_phone, shipping_address, billing_address, total_amount, product_id, entraineur_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, order);
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Order order) throws SQLException {
        String sql = "UPDATE `order` SET quantity = ?, order_date = ?, status = ?, payment_method = ?, payment_status = ?, size = ?, contact_email = ?, contact_phone = ?, shipping_address = ?, billing_address = ?, total_amount = ?, product_id = ?, entraineur_id = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, order);
            statement.setInt(14, order.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM `order` WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Order> getAll() throws SQLException {
        String sql = "SELECT id, quantity, order_date, status, payment_method, payment_status, size, contact_email, contact_phone, shipping_address, billing_address, total_amount, product_id, entraineur_id FROM `order`";
        List<Order> orders = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        }

        return orders;
    }

    @Override
    public Order getById(int id) throws SQLException {
        String sql = "SELECT id, quantity, order_date, status, payment_method, payment_status, size, contact_email, contact_phone, shipping_address, billing_address, total_amount, product_id, entraineur_id FROM `order` WHERE id = ?";

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

    private void fillStatement(PreparedStatement statement, Order order) throws SQLException {
        statement.setInt(1, order.getQuantity());
        statement.setDate(2, Date.valueOf(order.getOrderDate()));
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
        statement.setInt(13, order.getEntraineurId());
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Date orderDate = rs.getDate("order_date");

        return new Order(
                rs.getInt("id"),
                rs.getInt("quantity"),
                orderDate == null ? null : orderDate.toLocalDate(),
                rs.getString("status"),
                rs.getString("payment_method"),
                rs.getString("payment_status"),
                rs.getString("size"),
                rs.getString("contact_email"),
                rs.getString("contact_phone"),
                rs.getString("shipping_address"),
                rs.getString("billing_address"),
                rs.getBigDecimal("total_amount"),
                rs.getInt("product_id"),
                rs.getInt("entraineur_id")
        );
    }
}

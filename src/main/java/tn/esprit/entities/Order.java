package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Order {
    private Integer id;
    private Integer quantity;
    private LocalDate orderDate;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String size;
    private String contactEmail;
    private String contactPhone;
    private String shippingAddress;
    private String billingAddress;
    private BigDecimal totalAmount;
    private Integer productId;
    private Integer entraineurId;

    public Order() {
    }

    public Order(Integer quantity, LocalDate orderDate, String status, String paymentMethod, String paymentStatus, String size, String contactEmail, String contactPhone, String shippingAddress, String billingAddress, BigDecimal totalAmount, Integer productId, Integer entraineurId) {
        this.quantity = quantity;
        this.orderDate = orderDate;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.size = size;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.totalAmount = totalAmount;
        this.productId = productId;
        this.entraineurId = entraineurId;
    }
}

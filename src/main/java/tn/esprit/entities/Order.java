package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Order {
    private Integer id;
    private Integer quantity;
    private LocalDate orderDate;
<<<<<<< HEAD
    private String clientName;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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
<<<<<<< HEAD
        this(quantity, orderDate, null, status, paymentMethod, paymentStatus, size, contactEmail, contactPhone, shippingAddress, billingAddress, totalAmount, productId, entraineurId);
    }

    public Order(Integer quantity, LocalDate orderDate, String clientName, String status, String paymentMethod, String paymentStatus, String size, String contactEmail, String contactPhone, String shippingAddress, String billingAddress, BigDecimal totalAmount, Integer productId, Integer entraineurId) {
        this.quantity = quantity;
        this.orderDate = orderDate;
        this.clientName = clientName;
=======
        this.quantity = quantity;
        this.orderDate = orderDate;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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

    public Order(Integer id, Integer quantity, LocalDate orderDate, String status, String paymentMethod, String paymentStatus, String size, String contactEmail, String contactPhone, String shippingAddress, String billingAddress, BigDecimal totalAmount, Integer productId, Integer entraineurId) {
<<<<<<< HEAD
        this(id, quantity, orderDate, null, status, paymentMethod, paymentStatus, size, contactEmail, contactPhone, shippingAddress, billingAddress, totalAmount, productId, entraineurId);
    }

    public Order(Integer id, Integer quantity, LocalDate orderDate, String clientName, String status, String paymentMethod, String paymentStatus, String size, String contactEmail, String contactPhone, String shippingAddress, String billingAddress, BigDecimal totalAmount, Integer productId, Integer entraineurId) {
        this.id = id;
        this.quantity = quantity;
        this.orderDate = orderDate;
        this.clientName = clientName;
=======
        this.id = id;
        this.quantity = quantity;
        this.orderDate = orderDate;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

<<<<<<< HEAD
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getEntraineurId() {
        return entraineurId;
    }

    public void setEntraineurId(Integer entraineurId) {
        this.entraineurId = entraineurId;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", orderDate=" + orderDate +
<<<<<<< HEAD
                ", clientName='" + clientName + '\'' +
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                ", status='" + status + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", size='" + size + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", billingAddress='" + billingAddress + '\'' +
                ", totalAmount=" + totalAmount +
                ", productId=" + productId +
                ", entraineurId=" + entraineurId +
                '}';
    }
}

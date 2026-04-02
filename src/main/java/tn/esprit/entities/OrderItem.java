package tn.esprit.entities;

import java.math.BigDecimal;

public class OrderItem {
    private Integer id;
    private int quantity;
    private BigDecimal unitPrice;
    private Integer productId;
    private Integer orderRefId;

    public OrderItem() {
    }

    public OrderItem(int quantity, BigDecimal unitPrice, Integer productId, Integer orderRefId) {
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.productId = productId;
        this.orderRefId = orderRefId;
    }
}

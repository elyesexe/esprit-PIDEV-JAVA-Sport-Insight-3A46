package tn.esprit.entities;

import java.math.BigDecimal;

public class Product {
    private Integer id;
    private String name;
    private String category;
    private BigDecimal price;
    private int stock;
    private String size;
    private String brand;
    private String image;

    public Product() {
    }

    public Product(String name, String category, BigDecimal price, int stock, String size, String brand, String image) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.size = size;
        this.brand = brand;
        this.image = image;
    }
}

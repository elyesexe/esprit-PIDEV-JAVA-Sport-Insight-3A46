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
    private String description;
    private String tags;

    public Product() {
    }

    public Product(String name, String category, BigDecimal price, int stock, String size, String brand, String image) {
        this(name, category, price, stock, size, brand, image, null, null);
    }

    public Product(String name, String category, BigDecimal price, int stock, String size, String brand, String image, String description, String tags) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.size = size;
        this.brand = brand;
        this.image = image;
        this.description = description;
        this.tags = tags;
    }

    public Product(Integer id, String name, String category, BigDecimal price, int stock, String size, String brand, String image) {
        this(id, name, category, price, stock, size, brand, image, null, null);
    }

    public Product(Integer id, String name, String category, BigDecimal price, int stock, String size, String brand, String image, String description, String tags) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.size = size;
        this.brand = brand;
        this.image = image;
        this.description = description;
        this.tags = tags;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", size='" + size + '\'' +
                ", brand='" + brand + '\'' +
                ", image='" + image + '\'' +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                '}';
    }
}

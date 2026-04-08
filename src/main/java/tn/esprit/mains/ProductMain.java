package tn.esprit.mains;

import tn.esprit.entities.Order;
import tn.esprit.entities.Product;
import tn.esprit.services.OrderService;
import tn.esprit.services.ProductService;
import tn.esprit.tools.MyConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class ProductMain {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("Connected to database: " + connection.getConnection().getCatalog());

            ProductService productService = new ProductService();
            OrderService orderService = new OrderService();

            boolean running = true;
            while (running) {
                System.out.println("\n--- PRODUCT MODULE ---");
                System.out.println("1. Manage products");
                System.out.println("2. Manage orders");
                System.out.println("0. Exit");
                System.out.print("Choice: ");
                int choice = Integer.parseInt(SCANNER.nextLine());

                switch (choice) {
                    case 1 -> handleProduct(productService);
                    case 2 -> handleOrder(orderService);
                    case 0 -> {
                        System.out.println("Application closed.");
                        running = false;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleProduct(ProductService productService) throws Exception {
        System.out.println("\n--- PRODUCTS ---");
        System.out.println("1. Add product");
        System.out.println("2. Display all products");
        System.out.println("3. Update product");
        System.out.println("4. Delete product");
        System.out.println("5. Search product");
        System.out.println("6. Sort products");
        System.out.println("7. Advanced search");
        System.out.print("Choice: ");
        int choice = Integer.parseInt(SCANNER.nextLine());

        switch (choice) {
            case 1 -> {
                System.out.print("Name: ");
                String name = SCANNER.nextLine();
                System.out.print("Category: ");
                String category = SCANNER.nextLine();
                System.out.print("Price: ");
                BigDecimal price = new BigDecimal(SCANNER.nextLine());
                System.out.print("Stock: ");
                int stock = Integer.parseInt(SCANNER.nextLine());
                System.out.print("Size: ");
                String size = SCANNER.nextLine();
                System.out.print("Brand: ");
                String brand = SCANNER.nextLine();
                System.out.print("Image: ");
                String image = SCANNER.nextLine();

                productService.add(new Product(name, category, price, stock, size, brand, image));
                System.out.println("Product added successfully.");
            }
            case 2 -> productService.getAll().forEach(System.out::println);
            case 3 -> {
                System.out.print("Product id to update: ");
                int productId = Integer.parseInt(SCANNER.nextLine());
                Product product = productService.getById(productId);
                if (product == null) {
                    System.out.println("Product not found.");
                    return;
                }

                System.out.print("New name: ");
                product.setName(SCANNER.nextLine());
                System.out.print("New category: ");
                product.setCategory(SCANNER.nextLine());
                System.out.print("New price: ");
                product.setPrice(new BigDecimal(SCANNER.nextLine()));
                System.out.print("New stock: ");
                product.setStock(Integer.parseInt(SCANNER.nextLine()));
                System.out.print("New size: ");
                product.setSize(SCANNER.nextLine());
                System.out.print("New brand: ");
                product.setBrand(SCANNER.nextLine());
                System.out.print("New image: ");
                product.setImage(SCANNER.nextLine());
                productService.update(product);
                System.out.println("Product updated successfully.");
            }
            case 4 -> {
                System.out.print("Product id to delete: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                productService.delete(id);
                System.out.println("Product deleted successfully.");
            }
            case 5 -> {
                System.out.print("Keyword: ");
                String keyword = SCANNER.nextLine().trim().toLowerCase();
                productService.getAll().stream()
                        .filter(product -> containsIgnoreCase(product.getName(), keyword)
                                || containsIgnoreCase(product.getCategory(), keyword)
                                || containsIgnoreCase(product.getBrand(), keyword))
                        .forEach(System.out::println);
            }
            case 6 -> {
                List<Product> products = productService.getAll();
                System.out.println("Sort by: 1.Name  2.Category  3.Price");
                System.out.print("Choice: ");
                int sortChoice = Integer.parseInt(SCANNER.nextLine());
                switch (sortChoice) {
                    case 1 -> products.sort(Comparator.comparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    case 2 -> products.sort(Comparator.comparing(Product::getCategory, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    case 3 -> products.sort(Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo)));
                    default -> {
                        System.out.println("Invalid choice.");
                        return;
                    }
                }
                products.forEach(System.out::println);
            }
            case 7 -> {
                System.out.print("Keyword (Enter to skip): ");
                String keyword = emptyToNull(SCANNER.nextLine());
                System.out.print("Category (Enter to skip): ");
                String category = emptyToNull(SCANNER.nextLine());
                System.out.print("Brand (Enter to skip): ");
                String brand = emptyToNull(SCANNER.nextLine());
                System.out.print("Min price (Enter to skip): ");
                BigDecimal minPrice = parseBigDecimalOrNull(SCANNER.nextLine());
                System.out.print("Max price (Enter to skip): ");
                BigDecimal maxPrice = parseBigDecimalOrNull(SCANNER.nextLine());
                System.out.print("Min stock (Enter to skip): ");
                Integer minStock = parseIntegerOrNull(SCANNER.nextLine());
                System.out.print("Size (Enter to skip): ");
                String size = emptyToNull(SCANNER.nextLine());
                System.out.print("In-stock only? (y/n): ");
                boolean inStockOnly = SCANNER.nextLine().trim().equalsIgnoreCase("y");

                List<Product> products = productService.advancedSearch(
                        keyword, category, brand, minPrice, maxPrice, minStock, size, inStockOnly
                );

                if (products.isEmpty()) {
                    System.out.println("No products found.");
                } else {
                    products.forEach(System.out::println);
                }
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void handleOrder(OrderService orderService) throws Exception {
        System.out.println("\n--- ORDERS ---");
        System.out.println("1. Add order");
        System.out.println("2. Display all orders");
        System.out.println("3. Update order");
        System.out.println("4. Delete order");
        System.out.println("5. Search order");
        System.out.println("6. Sort orders");
        System.out.print("Choice: ");
        int choice = Integer.parseInt(SCANNER.nextLine());

        switch (choice) {
            case 1 -> {
                System.out.print("Quantity: ");
                int quantity = Integer.parseInt(SCANNER.nextLine());
                System.out.print("Order date (yyyy-mm-dd): ");
                LocalDate orderDate = LocalDate.parse(SCANNER.nextLine());
                System.out.print("Status: ");
                String status = SCANNER.nextLine();
                System.out.print("Payment method: ");
                String paymentMethod = SCANNER.nextLine();
                System.out.print("Payment status: ");
                String paymentStatus = SCANNER.nextLine();
                System.out.print("Size: ");
                String size = SCANNER.nextLine();
                System.out.print("Contact email: ");
                String contactEmail = SCANNER.nextLine();
                System.out.print("Contact phone: ");
                String contactPhone = SCANNER.nextLine();
                System.out.print("Shipping address: ");
                String shippingAddress = SCANNER.nextLine();
                System.out.print("Billing address: ");
                String billingAddress = SCANNER.nextLine();
                System.out.print("Total amount: ");
                BigDecimal totalAmount = new BigDecimal(SCANNER.nextLine());
                System.out.print("Product id: ");
                int productId = Integer.parseInt(SCANNER.nextLine());
                System.out.print("Entraineur id: ");
                int entraineurId = Integer.parseInt(SCANNER.nextLine());

                orderService.add(new Order(quantity, orderDate, status, paymentMethod, paymentStatus, size, contactEmail,
                        contactPhone, shippingAddress, billingAddress, totalAmount, productId, entraineurId));
                System.out.println("Order added successfully.");
            }
            case 2 -> orderService.getAll().forEach(System.out::println);
            case 3 -> {
                System.out.print("Order id to update: ");
                int orderId = Integer.parseInt(SCANNER.nextLine());
                Order order = orderService.getById(orderId);
                if (order == null) {
                    System.out.println("Order not found.");
                    return;
                }

                System.out.print("New quantity: ");
                order.setQuantity(Integer.parseInt(SCANNER.nextLine()));
                System.out.print("New order date (yyyy-mm-dd): ");
                order.setOrderDate(LocalDate.parse(SCANNER.nextLine()));
                System.out.print("New status: ");
                order.setStatus(SCANNER.nextLine());
                System.out.print("New payment method: ");
                order.setPaymentMethod(SCANNER.nextLine());
                System.out.print("New payment status: ");
                order.setPaymentStatus(SCANNER.nextLine());
                System.out.print("New size: ");
                order.setSize(SCANNER.nextLine());
                System.out.print("New contact email: ");
                order.setContactEmail(SCANNER.nextLine());
                System.out.print("New contact phone: ");
                order.setContactPhone(SCANNER.nextLine());
                System.out.print("New shipping address: ");
                order.setShippingAddress(SCANNER.nextLine());
                System.out.print("New billing address: ");
                order.setBillingAddress(SCANNER.nextLine());
                System.out.print("New total amount: ");
                order.setTotalAmount(new BigDecimal(SCANNER.nextLine()));
                System.out.print("New product id: ");
                order.setProductId(Integer.parseInt(SCANNER.nextLine()));
                System.out.print("New entraineur id: ");
                order.setEntraineurId(Integer.parseInt(SCANNER.nextLine()));
                orderService.update(order);
                System.out.println("Order updated successfully.");
            }
            case 4 -> {
                System.out.print("Order id to delete: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                orderService.delete(id);
                System.out.println("Order deleted successfully.");
            }
            case 5 -> {
                System.out.print("Keyword: ");
                String keyword = SCANNER.nextLine().trim().toLowerCase();
                orderService.getAll().stream()
                        .filter(order -> containsIgnoreCase(order.getStatus(), keyword)
                                || containsIgnoreCase(order.getPaymentMethod(), keyword)
                                || containsIgnoreCase(order.getPaymentStatus(), keyword)
                                || containsIgnoreCase(order.getContactEmail(), keyword))
                        .forEach(System.out::println);
            }
            case 6 -> {
                List<Order> orders = orderService.getAll();
                System.out.println("Sort by: 1.Order Date  2.Status  3.Total Amount");
                System.out.print("Choice: ");
                int sortChoice = Integer.parseInt(SCANNER.nextLine());
                switch (sortChoice) {
                    case 1 -> orders.sort(Comparator.comparing(Order::getOrderDate, Comparator.nullsLast(LocalDate::compareTo)));
                    case 2 -> orders.sort(Comparator.comparing(Order::getStatus, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    case 3 -> orders.sort(Comparator.comparing(Order::getTotalAmount, Comparator.nullsLast(BigDecimal::compareTo)));
                    default -> {
                        System.out.println("Invalid choice.");
                        return;
                    }
                }
                orders.forEach(System.out::println);
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private static String emptyToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal parseBigDecimalOrNull(String value) {
        String trimmed = emptyToNull(value);
        return trimmed == null ? null : new BigDecimal(trimmed);
    }

    private static Integer parseIntegerOrNull(String value) {
        String trimmed = emptyToNull(value);
        return trimmed == null ? null : Integer.parseInt(trimmed);
    }
}

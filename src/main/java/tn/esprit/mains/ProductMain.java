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

            while (true) {
                System.out.print("Choose a table to manipulate (product, order) or type exit: ");
                String tableChoice = SCANNER.nextLine().trim().toLowerCase();

                switch (tableChoice) {
                    case "product":
                        handleProduct(productService);
                        break;
                    case "order":
                        handleOrder(orderService);
                        break;
                    case "exit":
                        System.out.println("Application closed.");
                        return;
                    default:
                        System.out.println("Unknown table. Type product, order, or exit.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleProduct(ProductService productService) throws Exception {
        System.out.print("Do you want to add a new product? (yes/no): ");
        String addProductChoice = SCANNER.nextLine().trim();
        if (addProductChoice.equalsIgnoreCase("yes")) {
            System.out.println("Add a new product:");
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

            Product product = new Product(name, category, price, stock, size, brand, image);
            productService.add(product);
            System.out.println("Product added successfully.");
        }

        System.out.print("Do you want to display all products? (yes/no): ");
        String readAllProductsChoice = SCANNER.nextLine().trim();
        if (readAllProductsChoice.equalsIgnoreCase("yes")) {
            System.out.println("All products:");
            List<Product> products = productService.getAll();
            System.out.print("Do you want to sort products by name from A to Z? (yes/no): ");
            String sortByNameChoice = SCANNER.nextLine().trim();
            if (sortByNameChoice.equalsIgnoreCase("yes")) {
                products.sort(Comparator.comparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            }
            for (Product product : products) {
                System.out.println(product);
            }
        }

        System.out.print("Do you want to search products with advanced filters? (yes/no): ");
        String advancedSearchChoice = SCANNER.nextLine().trim();
        if (advancedSearchChoice.equalsIgnoreCase("yes")) {
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
                System.out.println("Advanced search results:");
                for (Product product : products) {
                    System.out.println(product);
                }
            }
        }

        System.out.print("Do you want to display one product by id? (yes/no): ");
        String readOneProductChoice = SCANNER.nextLine().trim();
        if (readOneProductChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the product to display: ");
            int productIdToRead = Integer.parseInt(SCANNER.nextLine());
            Product productToRead = productService.getById(productIdToRead);
            if (productToRead != null) {
                System.out.println("Product with id " + productIdToRead + ":");
                System.out.println(productToRead);
            } else {
                System.out.println("No product found with id " + productIdToRead);
            }
        }

        System.out.print("Do you want to update a product? (yes/no): ");
        String updateProductChoice = SCANNER.nextLine().trim();
        if (updateProductChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the product to update: ");
            int productId = Integer.parseInt(SCANNER.nextLine());

            Product productToUpdate = productService.getById(productId);
            if (productToUpdate != null) {
                System.out.print("New name: ");
                productToUpdate.setName(SCANNER.nextLine());
                System.out.print("New category: ");
                productToUpdate.setCategory(SCANNER.nextLine());
                System.out.print("New price: ");
                productToUpdate.setPrice(new BigDecimal(SCANNER.nextLine()));
                System.out.print("New stock: ");
                productToUpdate.setStock(Integer.parseInt(SCANNER.nextLine()));
                System.out.print("New size: ");
                productToUpdate.setSize(SCANNER.nextLine());
                System.out.print("New brand: ");
                productToUpdate.setBrand(SCANNER.nextLine());
                System.out.print("New image: ");
                productToUpdate.setImage(SCANNER.nextLine());

                productService.update(productToUpdate);
                System.out.println("Product updated successfully.");
                System.out.println("Updated product with id " + productId + ":");
                System.out.println(productService.getById(productId));
            } else {
                System.out.println("No product found with id " + productId);
            }
        }

        System.out.print("Do you want to delete a product? (yes/no): ");
        String deleteProductChoice = SCANNER.nextLine().trim();
        if (deleteProductChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the product to delete: ");
            int productIdToDelete = Integer.parseInt(SCANNER.nextLine());
            Product productToDelete = productService.getById(productIdToDelete);
            if (productToDelete != null) {
                productService.delete(productIdToDelete);
                System.out.println("Product deleted successfully.");
                System.out.println("Product with id " + productIdToDelete + ":");
                System.out.println(productService.getById(productIdToDelete));
            } else {
                System.out.println("No product found with id " + productIdToDelete);
            }
        }
    }

    private static void handleOrder(OrderService orderService) throws Exception {
        System.out.print("Do you want to add a new order? (yes/no): ");
        String addOrderChoice = SCANNER.nextLine().trim();
        if (addOrderChoice.equalsIgnoreCase("yes")) {
            System.out.println("Add a new order:");
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

            Order order = new Order(quantity, orderDate, status, paymentMethod, paymentStatus, size, contactEmail,
                    contactPhone, shippingAddress, billingAddress, totalAmount, productId, entraineurId);
            orderService.add(order);
            System.out.println("Order added successfully.");
        }

        System.out.print("Do you want to display all orders? (yes/no): ");
        String readAllOrdersChoice = SCANNER.nextLine().trim();
        if (readAllOrdersChoice.equalsIgnoreCase("yes")) {
            System.out.println("All orders:");
            List<Order> orders = orderService.getAll();
            for (Order order : orders) {
                System.out.println(order);
            }
        }

        System.out.print("Do you want to display one order by id? (yes/no): ");
        String readOneOrderChoice = SCANNER.nextLine().trim();
        if (readOneOrderChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the order to display: ");
            int orderIdToRead = Integer.parseInt(SCANNER.nextLine());
            Order orderToRead = orderService.getById(orderIdToRead);
            if (orderToRead != null) {
                System.out.println("Order with id " + orderIdToRead + ":");
                System.out.println(orderToRead);
            } else {
                System.out.println("No order found with id " + orderIdToRead);
            }
        }

        System.out.print("Do you want to update an order? (yes/no): ");
        String updateOrderChoice = SCANNER.nextLine().trim();
        if (updateOrderChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the order to update: ");
            int orderId = Integer.parseInt(SCANNER.nextLine());

            Order orderToUpdate = orderService.getById(orderId);
            if (orderToUpdate != null) {
                System.out.print("New quantity: ");
                orderToUpdate.setQuantity(Integer.parseInt(SCANNER.nextLine()));
                System.out.print("New order date (yyyy-mm-dd): ");
                orderToUpdate.setOrderDate(LocalDate.parse(SCANNER.nextLine()));
                System.out.print("New status: ");
                orderToUpdate.setStatus(SCANNER.nextLine());
                System.out.print("New payment method: ");
                orderToUpdate.setPaymentMethod(SCANNER.nextLine());
                System.out.print("New payment status: ");
                orderToUpdate.setPaymentStatus(SCANNER.nextLine());
                System.out.print("New size: ");
                orderToUpdate.setSize(SCANNER.nextLine());
                System.out.print("New contact email: ");
                orderToUpdate.setContactEmail(SCANNER.nextLine());
                System.out.print("New contact phone: ");
                orderToUpdate.setContactPhone(SCANNER.nextLine());
                System.out.print("New shipping address: ");
                orderToUpdate.setShippingAddress(SCANNER.nextLine());
                System.out.print("New billing address: ");
                orderToUpdate.setBillingAddress(SCANNER.nextLine());
                System.out.print("New total amount: ");
                orderToUpdate.setTotalAmount(new BigDecimal(SCANNER.nextLine()));
                System.out.print("New product id: ");
                orderToUpdate.setProductId(Integer.parseInt(SCANNER.nextLine()));
                System.out.print("New entraineur id: ");
                orderToUpdate.setEntraineurId(Integer.parseInt(SCANNER.nextLine()));

                orderService.update(orderToUpdate);
                System.out.println("Order updated successfully.");
                System.out.println("Updated order with id " + orderId + ":");
                System.out.println(orderService.getById(orderId));
            } else {
                System.out.println("No order found with id " + orderId);
            }
        }

        System.out.print("Do you want to delete an order? (yes/no): ");
        String deleteOrderChoice = SCANNER.nextLine().trim();
        if (deleteOrderChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the order to delete: ");
            int orderIdToDelete = Integer.parseInt(SCANNER.nextLine());
            Order orderToDelete = orderService.getById(orderIdToDelete);
            if (orderToDelete != null) {
                orderService.delete(orderIdToDelete);
                System.out.println("Order deleted successfully.");
                System.out.println("Order with id " + orderIdToDelete + ":");
                System.out.println(orderService.getById(orderIdToDelete));
            } else {
                System.out.println("No order found with id " + orderIdToDelete);
            }
        }
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

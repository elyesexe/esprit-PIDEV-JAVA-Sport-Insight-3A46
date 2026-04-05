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
    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("Connected to database: " + connection.getConnection().getCatalog());

            ProductService productService = new ProductService();
            OrderService orderService = new OrderService();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Choose a table to manipulate (product, order) or type exit: ");
                String tableChoice = scanner.nextLine().trim().toLowerCase();

                switch (tableChoice) {
                    case "product":
                        handleProduct(scanner, productService);
                        break;
                    case "order":
                        handleOrder(scanner, orderService);
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

    private static void handleProduct(Scanner scanner, ProductService productService) throws Exception {
        System.out.print("Do you want to add a new product? (yes/no): ");
        String addProductChoice = scanner.nextLine().trim();
        if (addProductChoice.equalsIgnoreCase("yes")) {
            System.out.println("Add a new product:");
            System.out.print("Name: ");
            String name = scanner.nextLine();
            System.out.print("Category: ");
            String category = scanner.nextLine();
            System.out.print("Price: ");
            BigDecimal price = new BigDecimal(scanner.nextLine());
            System.out.print("Stock: ");
            int stock = Integer.parseInt(scanner.nextLine());
            System.out.print("Size: ");
            String size = scanner.nextLine();
            System.out.print("Brand: ");
            String brand = scanner.nextLine();
            System.out.print("Image: ");
            String image = scanner.nextLine();

            Product product = new Product(name, category, price, stock, size, brand, image);
            productService.add(product);
            System.out.println("Product added successfully.");
        }

        System.out.print("Do you want to display all products? (yes/no): ");
        String readAllProductsChoice = scanner.nextLine().trim();
        if (readAllProductsChoice.equalsIgnoreCase("yes")) {
            System.out.println("All products:");
            List<Product> products = productService.getAll();
            System.out.print("Do you want to sort products by name from A to Z? (yes/no): ");
            String sortByNameChoice = scanner.nextLine().trim();
            if (sortByNameChoice.equalsIgnoreCase("yes")) {
                products.sort(Comparator.comparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            }
            for (Product product : products) {
                System.out.println(product);
            }
        }

        System.out.print("Do you want to display one product by id? (yes/no): ");
        String readOneProductChoice = scanner.nextLine().trim();
        if (readOneProductChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the product to display: ");
            int productIdToRead = Integer.parseInt(scanner.nextLine());
            Product productToRead = productService.getById(productIdToRead);
            if (productToRead != null) {
                System.out.println("Product with id " + productIdToRead + ":");
                System.out.println(productToRead);
            } else {
                System.out.println("No product found with id " + productIdToRead);
            }
        }

        System.out.print("Do you want to update a product? (yes/no): ");
        String updateProductChoice = scanner.nextLine().trim();
        if (updateProductChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the product to update: ");
            int productId = Integer.parseInt(scanner.nextLine());

            Product productToUpdate = productService.getById(productId);
            if (productToUpdate != null) {
                System.out.print("New name: ");
                productToUpdate.setName(scanner.nextLine());
                System.out.print("New category: ");
                productToUpdate.setCategory(scanner.nextLine());
                System.out.print("New price: ");
                productToUpdate.setPrice(new BigDecimal(scanner.nextLine()));
                System.out.print("New stock: ");
                productToUpdate.setStock(Integer.parseInt(scanner.nextLine()));
                System.out.print("New size: ");
                productToUpdate.setSize(scanner.nextLine());
                System.out.print("New brand: ");
                productToUpdate.setBrand(scanner.nextLine());
                System.out.print("New image: ");
                productToUpdate.setImage(scanner.nextLine());

                productService.update(productToUpdate);
                System.out.println("Product updated successfully.");
                System.out.println("Updated product with id " + productId + ":");
                System.out.println(productService.getById(productId));
            } else {
                System.out.println("No product found with id " + productId);
            }
        }

        System.out.print("Do you want to delete a product? (yes/no): ");
        String deleteProductChoice = scanner.nextLine().trim();
        if (deleteProductChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the product to delete: ");
            int productIdToDelete = Integer.parseInt(scanner.nextLine());
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

    private static void handleOrder(Scanner scanner, OrderService orderService) throws Exception {
        System.out.print("Do you want to add a new order? (yes/no): ");
        String addOrderChoice = scanner.nextLine().trim();
        if (addOrderChoice.equalsIgnoreCase("yes")) {
            System.out.println("Add a new order:");
            System.out.print("Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine());
            System.out.print("Order date (yyyy-mm-dd): ");
            LocalDate orderDate = LocalDate.parse(scanner.nextLine());
            System.out.print("Status: ");
            String status = scanner.nextLine();
            System.out.print("Payment method: ");
            String paymentMethod = scanner.nextLine();
            System.out.print("Payment status: ");
            String paymentStatus = scanner.nextLine();
            System.out.print("Size: ");
            String size = scanner.nextLine();
            System.out.print("Contact email: ");
            String contactEmail = scanner.nextLine();
            System.out.print("Contact phone: ");
            String contactPhone = scanner.nextLine();
            System.out.print("Shipping address: ");
            String shippingAddress = scanner.nextLine();
            System.out.print("Billing address: ");
            String billingAddress = scanner.nextLine();
            System.out.print("Total amount: ");
            BigDecimal totalAmount = new BigDecimal(scanner.nextLine());
            System.out.print("Product id: ");
            int productId = Integer.parseInt(scanner.nextLine());
            System.out.print("Entraineur id: ");
            int entraineurId = Integer.parseInt(scanner.nextLine());

            Order order = new Order(quantity, orderDate, status, paymentMethod, paymentStatus, size, contactEmail,
                    contactPhone, shippingAddress, billingAddress, totalAmount, productId, entraineurId);
            orderService.add(order);
            System.out.println("Order added successfully.");
        }

        System.out.print("Do you want to display all orders? (yes/no): ");
        String readAllOrdersChoice = scanner.nextLine().trim();
        if (readAllOrdersChoice.equalsIgnoreCase("yes")) {
            System.out.println("All orders:");
            List<Order> orders = orderService.getAll();
            for (Order order : orders) {
                System.out.println(order);
            }
        }

        System.out.print("Do you want to display one order by id? (yes/no): ");
        String readOneOrderChoice = scanner.nextLine().trim();
        if (readOneOrderChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the order to display: ");
            int orderIdToRead = Integer.parseInt(scanner.nextLine());
            Order orderToRead = orderService.getById(orderIdToRead);
            if (orderToRead != null) {
                System.out.println("Order with id " + orderIdToRead + ":");
                System.out.println(orderToRead);
            } else {
                System.out.println("No order found with id " + orderIdToRead);
            }
        }

        System.out.print("Do you want to update an order? (yes/no): ");
        String updateOrderChoice = scanner.nextLine().trim();
        if (updateOrderChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the order to update: ");
            int orderId = Integer.parseInt(scanner.nextLine());

            Order orderToUpdate = orderService.getById(orderId);
            if (orderToUpdate != null) {
                System.out.print("New quantity: ");
                orderToUpdate.setQuantity(Integer.parseInt(scanner.nextLine()));
                System.out.print("New order date (yyyy-mm-dd): ");
                orderToUpdate.setOrderDate(LocalDate.parse(scanner.nextLine()));
                System.out.print("New status: ");
                orderToUpdate.setStatus(scanner.nextLine());
                System.out.print("New payment method: ");
                orderToUpdate.setPaymentMethod(scanner.nextLine());
                System.out.print("New payment status: ");
                orderToUpdate.setPaymentStatus(scanner.nextLine());
                System.out.print("New size: ");
                orderToUpdate.setSize(scanner.nextLine());
                System.out.print("New contact email: ");
                orderToUpdate.setContactEmail(scanner.nextLine());
                System.out.print("New contact phone: ");
                orderToUpdate.setContactPhone(scanner.nextLine());
                System.out.print("New shipping address: ");
                orderToUpdate.setShippingAddress(scanner.nextLine());
                System.out.print("New billing address: ");
                orderToUpdate.setBillingAddress(scanner.nextLine());
                System.out.print("New total amount: ");
                orderToUpdate.setTotalAmount(new BigDecimal(scanner.nextLine()));
                System.out.print("New product id: ");
                orderToUpdate.setProductId(Integer.parseInt(scanner.nextLine()));
                System.out.print("New entraineur id: ");
                orderToUpdate.setEntraineurId(Integer.parseInt(scanner.nextLine()));

                orderService.update(orderToUpdate);
                System.out.println("Order updated successfully.");
                System.out.println("Updated order with id " + orderId + ":");
                System.out.println(orderService.getById(orderId));
            } else {
                System.out.println("No order found with id " + orderId);
            }
        }

        System.out.print("Do you want to delete an order? (yes/no): ");
        String deleteOrderChoice = scanner.nextLine().trim();
        if (deleteOrderChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the order to delete: ");
            int orderIdToDelete = Integer.parseInt(scanner.nextLine());
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
}

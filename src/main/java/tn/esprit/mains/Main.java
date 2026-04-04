package tn.esprit.mains;

import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.Order;
import tn.esprit.entities.Product;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.OrderService;
import tn.esprit.services.ProductService;
import tn.esprit.tools.MyConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("Connected to database: " + connection.getConnection().getCatalog());

            EquipeService equipeService = new EquipeService();
            JoueurService joueurService = new JoueurService();
            MatchsService matchsService = new MatchsService();
            ProductService productService = new ProductService();
            OrderService orderService = new OrderService();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Choose a table to manipulate (equipe, joueur, matchs, product, order) or type exit: ");
                String tableChoice = scanner.nextLine().trim().toLowerCase();

                switch (tableChoice) {
                    case "equipe":
                        handleEquipe(scanner, equipeService);
                        break;
                    case "joueur":
                        handleJoueur(scanner, joueurService);
                        break;
                    case "matchs":
                        handleMatchs(scanner, matchsService);
                        break;
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
                        System.out.println("Unknown table. Type equipe, joueur, matchs, product, order, or exit.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleEquipe(Scanner scanner, EquipeService equipeService) throws Exception {
        System.out.print("Do you want to add a new equipe? (yes/no): ");
        String addChoice = scanner.nextLine().trim();
        if (addChoice.equalsIgnoreCase("yes")) {
            System.out.println("Add a new equipe:");
            System.out.print("Nom: ");
            String nom = scanner.nextLine();
            System.out.print("Coach: ");
            String coach = scanner.nextLine();
            System.out.print("Adresse: ");
            String adresse = scanner.nextLine();
            System.out.print("Telephone: ");
            String telephone = scanner.nextLine();
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Image: ");
            String image = scanner.nextLine();

            Equipe nouvelleEquipe = new Equipe(nom, coach, adresse, telephone, email, image);
            equipeService.add(nouvelleEquipe);
            System.out.println("Equipe added successfully.");
        }

        System.out.print("Do you want to display all equipes? (yes/no): ");
        String readAllChoice = scanner.nextLine().trim();
        if (readAllChoice.equalsIgnoreCase("yes")) {
            System.out.println("All equipes:");
            List<Equipe> equipes = equipeService.getAll();
            System.out.print("Do you want to sort equipes by nom from A to Z? (yes/no): ");
            String sortByNomChoice = scanner.nextLine().trim();
            if (sortByNomChoice.equalsIgnoreCase("yes")) {
                equipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            }
            for (Equipe equipe : equipes) {
                System.out.println(equipe);
            }
        }

        System.out.print("Do you want to display one equipe by id? (yes/no): ");
        String readOneChoice = scanner.nextLine().trim();
        if (readOneChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the equipe to display: ");
            int equipeIdToRead = Integer.parseInt(scanner.nextLine());
            Equipe equipeToRead = equipeService.getById(equipeIdToRead);
            if (equipeToRead != null) {
                System.out.println("Equipe with id " + equipeIdToRead + ":");
                System.out.println(equipeToRead);
            } else {
                System.out.println("No equipe found with id " + equipeIdToRead);
            }
        }

        System.out.print("Do you want to update an equipe? (yes/no): ");
        String updateChoice = scanner.nextLine().trim();
        if (updateChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the equipe to update: ");
            int equipeId = Integer.parseInt(scanner.nextLine());

            Equipe equipeToUpdate = equipeService.getById(equipeId);
            if (equipeToUpdate != null) {
                System.out.print("New nom: ");
                equipeToUpdate.setNom(scanner.nextLine());
                System.out.print("New coach: ");
                equipeToUpdate.setCoach(scanner.nextLine());
                System.out.print("New adresse: ");
                equipeToUpdate.setAdresse(scanner.nextLine());
                System.out.print("New telephone: ");
                equipeToUpdate.setTelephone(scanner.nextLine());
                System.out.print("New email: ");
                equipeToUpdate.setEmail(scanner.nextLine());
                System.out.print("New image: ");
                equipeToUpdate.setImage(scanner.nextLine());

                equipeService.update(equipeToUpdate);
                System.out.println("Equipe updated successfully.");
                System.out.println("Updated equipe with id " + equipeId + ":");
                System.out.println(equipeService.getById(equipeId));
            } else {
                System.out.println("No equipe found with id " + equipeId);
            }
        }

        System.out.print("Do you want to delete an equipe? (yes/no): ");
        String deleteChoice = scanner.nextLine().trim();
        if (deleteChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the equipe to delete: ");
            int equipeIdToDelete = Integer.parseInt(scanner.nextLine());
            Equipe equipeToDelete = equipeService.getById(equipeIdToDelete);
            if (equipeToDelete != null) {
                equipeService.delete(equipeIdToDelete);
                System.out.println("Equipe deleted successfully.");
                System.out.println("Equipe with id " + equipeIdToDelete + ":");
                System.out.println(equipeService.getById(equipeIdToDelete));
            } else {
                System.out.println("No equipe found with id " + equipeIdToDelete);
            }
        }
    }

    private static void handleJoueur(Scanner scanner, JoueurService joueurService) throws Exception {
        System.out.print("Do you want to add a new joueur? (yes/no): ");
        String addJoueurChoice = scanner.nextLine().trim();
        if (addJoueurChoice.equalsIgnoreCase("yes")) {
            System.out.println("Add a new joueur:");
            System.out.print("Nom: ");
            String nom = scanner.nextLine();
            System.out.print("Prenom: ");
            String prenom = scanner.nextLine();
            System.out.print("Date naissance (yyyy-mm-dd): ");
            LocalDate dateNaissance = LocalDate.parse(scanner.nextLine());
            System.out.print("Numero: ");
            int numero = Integer.parseInt(scanner.nextLine());
            System.out.print("Image: ");
            String image = scanner.nextLine();
            System.out.print("Equipe id: ");
            String equipeIdInput = scanner.nextLine().trim();

            Joueur nouveauJoueur = new Joueur(
                    nom,
                    prenom,
                    dateNaissance,
                    numero,
                    image,
                    equipeIdInput.isEmpty() ? null : Integer.parseInt(equipeIdInput)
            );
            joueurService.add(nouveauJoueur);
            System.out.println("Joueur added successfully.");
        }

        System.out.print("Do you want to display all joueurs? (yes/no): ");
        String readAllJoueursChoice = scanner.nextLine().trim();
        if (readAllJoueursChoice.equalsIgnoreCase("yes")) {
            System.out.println("All joueurs:");
            List<Joueur> joueurs = joueurService.getAll();
            System.out.print("Do you want to sort joueurs by nom from A to Z? (yes/no): ");
            String sortByNomChoice = scanner.nextLine().trim();
            if (sortByNomChoice.equalsIgnoreCase("yes")) {
                joueurs.sort(Comparator.comparing(Joueur::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            }
            for (Joueur joueur : joueurs) {
                System.out.println(joueur);
            }
        }

        System.out.print("Do you want to display one joueur by id? (yes/no): ");
        String readOneJoueurChoice = scanner.nextLine().trim();
        if (readOneJoueurChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the joueur to display: ");
            int joueurIdToRead = Integer.parseInt(scanner.nextLine());
            Joueur joueurToRead = joueurService.getById(joueurIdToRead);
            if (joueurToRead != null) {
                System.out.println("Joueur with id " + joueurIdToRead + ":");
                System.out.println(joueurToRead);
            } else {
                System.out.println("No joueur found with id " + joueurIdToRead);
            }
        }

        System.out.print("Do you want to update a joueur? (yes/no): ");
        String updateJoueurChoice = scanner.nextLine().trim();
        if (updateJoueurChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the joueur to update: ");
            int joueurId = Integer.parseInt(scanner.nextLine());

            Joueur joueurToUpdate = joueurService.getById(joueurId);
            if (joueurToUpdate != null) {
                System.out.print("New nom: ");
                joueurToUpdate.setNom(scanner.nextLine());
                System.out.print("New prenom: ");
                joueurToUpdate.setPrenom(scanner.nextLine());
                System.out.print("New date naissance (yyyy-mm-dd): ");
                joueurToUpdate.setDateNaissance(LocalDate.parse(scanner.nextLine()));
                System.out.print("New numero: ");
                joueurToUpdate.setNumero(Integer.parseInt(scanner.nextLine()));
                System.out.print("New image: ");
                joueurToUpdate.setImage(scanner.nextLine());
                System.out.print("New equipe id: ");
                String newEquipeId = scanner.nextLine().trim();
                joueurToUpdate.setEquipeId(newEquipeId.isEmpty() ? null : Integer.parseInt(newEquipeId));

                joueurService.update(joueurToUpdate);
                System.out.println("Joueur updated successfully.");
                System.out.println("Updated joueur with id " + joueurId + ":");
                System.out.println(joueurService.getById(joueurId));
            } else {
                System.out.println("No joueur found with id " + joueurId);
            }
        }

        System.out.print("Do you want to delete a joueur? (yes/no): ");
        String deleteJoueurChoice = scanner.nextLine().trim();
        if (deleteJoueurChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the joueur to delete: ");
            int joueurIdToDelete = Integer.parseInt(scanner.nextLine());
            Joueur joueurToDelete = joueurService.getById(joueurIdToDelete);
            if (joueurToDelete != null) {
                joueurService.delete(joueurIdToDelete);
                System.out.println("Joueur deleted successfully.");
                System.out.println("Joueur with id " + joueurIdToDelete + ":");
                System.out.println(joueurService.getById(joueurIdToDelete));
            } else {
                System.out.println("No joueur found with id " + joueurIdToDelete);
            }
        }
    }

    private static void handleMatchs(Scanner scanner, MatchsService matchsService) throws Exception {
        System.out.print("Do you want to add a new match? (yes/no): ");
        String addMatchChoice = scanner.nextLine().trim();
        if (addMatchChoice.equalsIgnoreCase("yes")) {
            System.out.println("Add a new match:");
            System.out.print("Id match: ");
            String idMatch = scanner.nextLine();
            System.out.print("Date match (yyyy-mm-dd): ");
            LocalDate dateMatch = LocalDate.parse(scanner.nextLine());
            System.out.print("Heure debut (HH:mm:ss): ");
            LocalTime heureDebut = LocalTime.parse(scanner.nextLine());
            System.out.print("Lieu: ");
            String lieu = scanner.nextLine();
            System.out.print("Type: ");
            String type = scanner.nextLine();
            System.out.print("Statut: ");
            String statut = scanner.nextLine();
            System.out.print("Lineup domicile: ");
            String lineupDomicile = scanner.nextLine();
            System.out.print("Lineup exterieur: ");
            String lineupExterieur = scanner.nextLine();
            System.out.print("Score equipe domicile: ");
            String scoreDomicileInput = scanner.nextLine().trim();
            System.out.print("Score equipe exterieur: ");
            String scoreExterieurInput = scanner.nextLine().trim();
            System.out.print("Equipe domicile id: ");
            String equipeDomicileIdInput = scanner.nextLine().trim();
            System.out.print("Equipe exterieur id: ");
            String equipeExterieurIdInput = scanner.nextLine().trim();

            Matchs nouveauMatch = new Matchs(
                    idMatch,
                    dateMatch,
                    heureDebut,
                    lieu,
                    type,
                    statut,
                    lineupDomicile,
                    lineupExterieur,
                    scoreDomicileInput.isEmpty() ? null : Integer.parseInt(scoreDomicileInput),
                    scoreExterieurInput.isEmpty() ? null : Integer.parseInt(scoreExterieurInput),
                    equipeDomicileIdInput.isEmpty() ? null : Integer.parseInt(equipeDomicileIdInput),
                    equipeExterieurIdInput.isEmpty() ? null : Integer.parseInt(equipeExterieurIdInput)
            );
            matchsService.add(nouveauMatch);
            System.out.println("Match added successfully.");
        }

        System.out.print("Do you want to display all matchs? (yes/no): ");
        String readAllMatchsChoice = scanner.nextLine().trim();
        if (readAllMatchsChoice.equalsIgnoreCase("yes")) {
            System.out.println("All matchs:");
            List<Matchs> matchsList = matchsService.getAll();
            for (Matchs match : matchsList) {
                System.out.println(match);
            }
        }

        System.out.print("Do you want to display one match by id? (yes/no): ");
        String readOneMatchChoice = scanner.nextLine().trim();
        if (readOneMatchChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the match to display: ");
            int matchIdToRead = Integer.parseInt(scanner.nextLine());
            Matchs matchToRead = matchsService.getById(matchIdToRead);
            if (matchToRead != null) {
                System.out.println("Match with id " + matchIdToRead + ":");
                System.out.println(matchToRead);
            } else {
                System.out.println("No match found with id " + matchIdToRead);
            }
        }

        System.out.print("Do you want to update a match? (yes/no): ");
        String updateMatchChoice = scanner.nextLine().trim();
        if (updateMatchChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the match to update: ");
            int matchId = Integer.parseInt(scanner.nextLine());

            Matchs matchToUpdate = matchsService.getById(matchId);
            if (matchToUpdate != null) {
                System.out.print("New id match: ");
                matchToUpdate.setIdMatch(scanner.nextLine());
                System.out.print("New date match (yyyy-mm-dd): ");
                matchToUpdate.setDateMatch(LocalDate.parse(scanner.nextLine()));
                System.out.print("New heure debut (HH:mm:ss): ");
                matchToUpdate.setHeureDebut(LocalTime.parse(scanner.nextLine()));
                System.out.print("New lieu: ");
                matchToUpdate.setLieu(scanner.nextLine());
                System.out.print("New type: ");
                matchToUpdate.setType(scanner.nextLine());
                System.out.print("New statut: ");
                matchToUpdate.setStatut(scanner.nextLine());
                System.out.print("New lineup domicile: ");
                matchToUpdate.setLineupDomicile(scanner.nextLine());
                System.out.print("New lineup exterieur: ");
                matchToUpdate.setLineupExterieur(scanner.nextLine());
                System.out.print("New score equipe domicile: ");
                String newScoreDomicile = scanner.nextLine().trim();
                matchToUpdate.setScoreEquipeDomicile(newScoreDomicile.isEmpty() ? null : Integer.parseInt(newScoreDomicile));
                System.out.print("New score equipe exterieur: ");
                String newScoreExterieur = scanner.nextLine().trim();
                matchToUpdate.setScoreEquipeExterieur(newScoreExterieur.isEmpty() ? null : Integer.parseInt(newScoreExterieur));
                System.out.print("New equipe domicile id: ");
                String newEquipeDomicileId = scanner.nextLine().trim();
                matchToUpdate.setEquipeDomicileId(newEquipeDomicileId.isEmpty() ? null : Integer.parseInt(newEquipeDomicileId));
                System.out.print("New equipe exterieur id: ");
                String newEquipeExterieurId = scanner.nextLine().trim();
                matchToUpdate.setEquipeExterieurId(newEquipeExterieurId.isEmpty() ? null : Integer.parseInt(newEquipeExterieurId));

                matchsService.update(matchToUpdate);
                System.out.println("Match updated successfully.");
                System.out.println("Updated match with id " + matchId + ":");
                System.out.println(matchsService.getById(matchId));
            } else {
                System.out.println("No match found with id " + matchId);
            }
        }

        System.out.print("Do you want to delete a match? (yes/no): ");
        String deleteMatchChoice = scanner.nextLine().trim();
        if (deleteMatchChoice.equalsIgnoreCase("yes")) {
            System.out.print("Enter the id of the match to delete: ");
            int matchIdToDelete = Integer.parseInt(scanner.nextLine());
            Matchs matchToDelete = matchsService.getById(matchIdToDelete);
            if (matchToDelete != null) {
                matchsService.delete(matchIdToDelete);
                System.out.println("Match deleted successfully.");
                System.out.println("Match with id " + matchIdToDelete + ":");
                System.out.println(matchsService.getById(matchIdToDelete));
            } else {
                System.out.println("No match found with id " + matchIdToDelete);
            }
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

package tn.esprit.mains;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.MyConnection;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class UserMain {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("Connected to database: " + connection.getConnection().getCatalog());

            UserService userService = new UserService();
            boolean running = true;
            while (running) {
                System.out.println("\n--- USER MODULE ---");
                System.out.println("1. Add user");
                System.out.println("2. Display all users");
                System.out.println("3. View user by id");
                System.out.println("4. Search users");
                System.out.println("5. Update user");
                System.out.println("6. Delete user");
                System.out.println("0. Exit");
                System.out.print("Choice: ");

                int choice = parseInt(SCANNER.nextLine());
                switch (choice) {
                    case 1 -> addUser(userService);
                    case 2 -> displayAllUsers(userService);
                    case 3 -> displayUserById(userService);
                    case 4 -> searchUsers(userService);
                    case 5 -> updateUser(userService);
                    case 6 -> deleteUser(userService);
                    case 0 -> {
                        System.out.println("Application closed.");
                        running = false;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    private static void addUser(UserService userService) throws SQLException {
        User user = new User();
        fillRequiredFields(user);
        user.setDateInscription(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userService.add(user);
    }

    private static void displayAllUsers(UserService userService) throws SQLException {
        List<User> users = userService.getAll();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        users.forEach(System.out::println);
    }

    private static void displayUserById(UserService userService) throws SQLException {
        System.out.print("User id: ");
        User user = userService.getById(parseInt(SCANNER.nextLine()));
        System.out.println(user != null ? user : "User not found.");
    }

    private static void searchUsers(UserService userService) throws SQLException {
        System.out.print("Keyword: ");
        List<User> users = userService.search(SCANNER.nextLine());
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        users.forEach(System.out::println);
    }

    private static void updateUser(UserService userService) throws SQLException {
        System.out.print("User id to update: ");
        User user = userService.getById(parseInt(SCANNER.nextLine()));
        if (user == null) {
            System.out.println("User not found.");
            return;
        }

        System.out.print("New email (press Enter to skip): ");
        String email = SCANNER.nextLine();
        if (!email.isBlank()) {
            user.setEmail(email);
        }

        System.out.print("New roles (press Enter to skip): ");
        String roles = SCANNER.nextLine();
        if (!roles.isBlank()) {
            user.setRoles(roles);
        }

        System.out.print("New password (press Enter to skip): ");
        String password = SCANNER.nextLine();
        if (!password.isBlank()) {
            user.setPassword(password);
        }

        System.out.print("New last name (press Enter to skip): ");
        String nom = SCANNER.nextLine();
        if (!nom.isBlank()) {
            user.setNom(nom);
        }

        System.out.print("New first name (press Enter to skip): ");
        String prenom = SCANNER.nextLine();
        if (!prenom.isBlank()) {
            user.setPrenom(prenom);
        }

        System.out.print("New telephone (press Enter to skip): ");
        String telephone = SCANNER.nextLine();
        if (!telephone.isBlank()) {
            user.setTelephone(telephone);
        }

        System.out.print("New birth date yyyy-mm-dd (press Enter to skip): ");
        String dateNaissance = SCANNER.nextLine();
        if (!dateNaissance.isBlank()) {
            user.setDateNaissance(parseDate(dateNaissance));
        }

        System.out.print("New photo (press Enter to skip): ");
        String photo = SCANNER.nextLine();
        if (!photo.isBlank()) {
            user.setPhoto(photo);
        }

        System.out.print("New status (press Enter to skip): ");
        String statut = SCANNER.nextLine();
        if (!statut.isBlank()) {
            user.setStatut(statut);
        }

        System.out.print("New cv name (press Enter to skip): ");
        String cvName = SCANNER.nextLine();
        if (!cvName.isBlank()) {
            user.setCvName(cvName);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userService.update(user);
    }

    private static void deleteUser(UserService userService) throws SQLException {
        System.out.print("User id to delete: ");
        userService.delete(parseInt(SCANNER.nextLine()));
    }

    private static void fillRequiredFields(User user) {
        System.out.print("Email: ");
        user.setEmail(SCANNER.nextLine());

        System.out.print("Roles: ");
        user.setRoles(SCANNER.nextLine());

        System.out.print("Password: ");
        user.setPassword(SCANNER.nextLine());

        System.out.print("Last name: ");
        user.setNom(SCANNER.nextLine());

        System.out.print("First name: ");
        user.setPrenom(SCANNER.nextLine());

        System.out.print("Telephone: ");
        user.setTelephone(SCANNER.nextLine());

        System.out.print("Birth date yyyy-mm-dd: ");
        user.setDateNaissance(parseDate(SCANNER.nextLine()));

        System.out.print("Photo: ");
        user.setPhoto(SCANNER.nextLine());

        System.out.print("Status: ");
        user.setStatut(SCANNER.nextLine());

        System.out.print("CV name: ");
        String cvName = SCANNER.nextLine();
        user.setCvName(cvName.isBlank() ? null : cvName);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy-mm-dd.");
        }
    }
}

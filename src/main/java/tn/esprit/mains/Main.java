package tn.esprit.mains;

import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Sponsor;
import tn.esprit.services.ContratSponsorService;
import tn.esprit.services.SponsorService;
import tn.esprit.tools.MyConnection;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static SponsorService sponsorService;
    private static ContratSponsorService contratService;
    private static Scanner scanner;

    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("✓ Connected to database: " + connection.getConnection().getCatalog());
            
            sponsorService = new SponsorService();
            contratService = new ContratSponsorService();
            scanner = new Scanner(System.in);
            
            showMainMenu();
            
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    private static void showMainMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n============================================");
            System.out.println("      SPONSOR & CONTRACT MANAGEMENT");
            System.out.println("============================================");
            System.out.println("1. Sponsor Management");
            System.out.println("2. Contract Sponsor Management");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            
            int choice = getIntInput();
            switch (choice) {
                case 1:
                    sponsorMenu();
                    break;
                case 2:
                    contratMenu();
                    break;
                case 0:
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void sponsorMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- SPONSOR MANAGEMENT ---");
            System.out.println("1. Add Sponsor");
            System.out.println("2. View All Sponsors");
            System.out.println("3. View Sponsor by ID");
            System.out.println("4. Search Sponsor by Name");
            System.out.println("5. Search Sponsor by Budget");
            System.out.println("6. Update Sponsor");
            System.out.println("7. Delete Sponsor");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choose an option: ");
            
            int choice = getIntInput();
            try {
                switch (choice) {
                    case 1:
                        addSponsor();
                        break;
                    case 2:
                        viewAllSponsors();
                        break;
                    case 3:
                        viewSponsorById();
                        break;
                    case 4:
                        searchSponsor();
                        break;
                    case 5:
                        searchSponsorByBudget();
                        break;
                    case 6:
                        updateSponsor();
                        break;
                    case 7:
                        deleteSponsor();
                        break;
                    case 0:
                        inMenu = false;
                        break;
                    default:
                        System.out.println("Invalid option!");
                }
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void contratMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- CONTRACT SPONSOR MANAGEMENT ---");
            System.out.println("1. Add Contract");
            System.out.println("2. View All Contracts");
            System.out.println("3. View Contract by ID");
            System.out.println("4. Search Contract by Keyword");
            System.out.println("5. Search Contract by Amount");
            System.out.println("6. Search Contract by Status");
            System.out.println("7. Update Contract");
            System.out.println("8. Delete Contract");
            System.out.println("9. View Contracts by Sponsor ID");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choose an option: ");
            
            int choice = getIntInput();
            try {
                switch (choice) {
                    case 1:
                        addContract();
                        break;
                    case 2:
                        viewAllContracts();
                        break;
                    case 3:
                        viewContractById();
                        break;
                    case 4:
                        searchContract();
                        break;
                    case 5:
                        searchContractByAmount();
                        break;
                    case 6:
                        searchContractByStatus();
                        break;
                    case 7:
                        updateContract();
                        break;
                    case 8:
                        deleteContract();
                        break;
                    case 9:
                        viewContractBySponsorId();
                        break;
                    case 0:
                        inMenu = false;
                        break;
                    default:
                        System.out.println("Invalid option!");
                }
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // SPONSOR OPERATIONS
    private static void addSponsor() throws SQLException {
        System.out.println("\n--- ADD NEW SPONSOR ---");
        System.out.print("Name: ");
        String nom = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Telephone: ");
        String telephone = scanner.nextLine();
        System.out.print("Budget: ");
        double budget = getDoubleInput();
        System.out.print("Logo Name: ");
        String logoName = scanner.nextLine();
        System.out.print("Address: ");
        String adresse = scanner.nextLine();
        
        Sponsor sponsor = new Sponsor(nom, email, telephone, budget, logoName, LocalDateTime.now(), adresse);
        sponsorService.add(sponsor);
    }

    private static void viewAllSponsors() throws SQLException {
        System.out.println("\n--- ALL SPONSORS ---");
        List<Sponsor> sponsors = sponsorService.getAll();
        if (sponsors.isEmpty()) {
            System.out.println("No sponsors found!");
        } else {
            for (Sponsor s : sponsors) {
                System.out.println(s);
            }
        }
    }

    private static void viewSponsorById() throws SQLException {
        System.out.print("\nEnter Sponsor ID: ");
        int id = getIntInput();
        Sponsor sponsor = sponsorService.getById(id);
        if (sponsor != null) {
            System.out.println(sponsor);
        } else {
            System.out.println("Sponsor not found!");
        }
    }

    private static void searchSponsor() throws SQLException {
        System.out.println("\n--- SEARCH SPONSOR BY NAME ---");
        System.out.print("Enter sponsor name: ");
        String nom = scanner.nextLine();
        
        if (nom.isEmpty()) {
            System.out.println("Error: Please enter a sponsor name!");
            return;
        }
        
        List<Sponsor> results = sponsorService.searchByName(nom);
        System.out.println("Search completed. Found " + results.size() + " result(s).");
        
        if (results.isEmpty()) {
            System.out.println("No sponsors found with name: " + nom);
        } else {
            System.out.println("\n--- SEARCH RESULTS ---");
            for (Sponsor s : results) {
                System.out.println(s);
            }
        }
    }

    private static void searchSponsorByBudget() throws SQLException {
        System.out.println("\n--- SEARCH SPONSOR BY BUDGET ---");
        System.out.println("1. Budget >= Amount");
        System.out.println("2. Budget <= Amount");
        System.out.println("3. Budget between Min and Max");
        System.out.print("Choose search type: ");
        
        int choice = getIntInput();
        List<Sponsor> results = new ArrayList<>();
        
        try {
            switch (choice) {
                case 1:
                    System.out.print("Enter minimum budget: ");
                    double minBudget = getDoubleInput();
                    results = sponsorService.searchByMinBudget(minBudget);
                    break;
                case 2:
                    System.out.print("Enter maximum budget: ");
                    double maxBudget = getDoubleInput();
                    results = sponsorService.searchByMaxBudget(maxBudget);
                    break;
                case 3:
                    System.out.print("Enter minimum budget: ");
                    double min = getDoubleInput();
                    System.out.print("Enter maximum budget: ");
                    double max = getDoubleInput();
                    results = sponsorService.searchByBudgetRange(min, max);
                    break;
                default:
                    System.out.println("Invalid option!");
                    return;
            }
            
            if (results.isEmpty()) {
                System.out.println("No sponsors found!");
            } else {
                System.out.println("\n--- SEARCH RESULTS ---");
                for (Sponsor s : results) {
                    System.out.println(s);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void updateSponsor() throws SQLException {
        System.out.print("\nEnter Sponsor ID to update: ");
        int id = getIntInput();
        Sponsor sponsor = sponsorService.getById(id);
        if (sponsor == null) {
            System.out.println("Sponsor not found!");
            return;
        }
        
        System.out.println("Current data: " + sponsor);
        System.out.print("New Name (press Enter to skip): ");
        String nom = scanner.nextLine();
        if (!nom.isEmpty()) sponsor.setNom(nom);
        
        System.out.print("New Email (press Enter to skip): ");
        String email = scanner.nextLine();
        if (!email.isEmpty()) sponsor.setEmail(email);
        
        System.out.print("New Telephone (press Enter to skip): ");
        String telephone = scanner.nextLine();
        if (!telephone.isEmpty()) sponsor.setTelephone(telephone);
        
        System.out.print("New Budget (press Enter to skip): ");
        String budgetStr = scanner.nextLine();
        if (!budgetStr.isEmpty()) sponsor.setBudget(Double.parseDouble(budgetStr));
        
        System.out.print("New Address (press Enter to skip): ");
        String adresse = scanner.nextLine();
        if (!adresse.isEmpty()) sponsor.setAdresse(adresse);
        
        sponsorService.update(sponsor);
    }

    private static void deleteSponsor() throws SQLException {
        System.out.print("\nEnter Sponsor ID to delete: ");
        int id = getIntInput();
        sponsorService.delete(id);
    }

    // CONTRACT OPERATIONS
    private static void addContract() throws SQLException {
        System.out.println("\n--- ADD NEW CONTRACT ---");
        System.out.print("Start Date (YYYY-MM-DD): ");
        LocalDate dateDebut = LocalDate.parse(scanner.nextLine());
        System.out.print("End Date (YYYY-MM-DD): ");
        LocalDate dateFin = LocalDate.parse(scanner.nextLine());
        System.out.print("Amount: ");
        double montant = getDoubleInput();
        System.out.print("Description: ");
        String description = scanner.nextLine();
        System.out.print("Status: ");
        String statut = scanner.nextLine();
        
        // Display available sponsors
        System.out.println("\n--- AVAILABLE SPONSORS ---");
        List<Sponsor> sponsors = sponsorService.getAll();
        if (sponsors.isEmpty()) {
            System.out.println("No sponsors available!");
            return;
        }
        
        for (Sponsor s : sponsors) {
            System.out.println("ID: " + s.getId() + " | Name: " + s.getNom());
        }
        
        System.out.print("\nEnter Sponsor Name: ");
        String sponsorName = scanner.nextLine();
        List<Sponsor> foundSponsors = sponsorService.searchByName(sponsorName);
        if (foundSponsors.isEmpty()) {
            System.out.println("Sponsor not found!");
            return;
        }
        int sponsorId = foundSponsors.get(0).getId();
        
        System.out.print("Enter Team ID: ");
        int equipeId = getIntInput();
        
        ContratSponsor contrat = new ContratSponsor(dateDebut, dateFin, montant, description, statut, false, "PENDING", sponsorId, equipeId);
        contratService.add(contrat);
    }

    private static void viewAllContracts() throws SQLException {
        System.out.println("\n--- ALL CONTRACTS ---");
        List<ContratSponsor> contrats = contratService.getAll();
        if (contrats.isEmpty()) {
            System.out.println("No contracts found!");
        } else {
            for (ContratSponsor c : contrats) {
                System.out.println(c);
            }
        }
    }

    private static void viewContractById() throws SQLException {
        System.out.print("\nEnter Contract ID: ");
        int id = getIntInput();
        ContratSponsor contrat = contratService.getById(id);
        if (contrat != null) {
            System.out.println(contrat);
        } else {
            System.out.println("Contract not found!");
        }
    }

    private static void searchContract() throws SQLException {
        System.out.print("\nSearch keyword (description/status/payment): ");
        String keyword = scanner.nextLine();
        List<ContratSponsor> results = contratService.search(keyword);
        if (results.isEmpty()) {
            System.out.println("No contracts found!");
        } else {
            System.out.println("\n--- SEARCH RESULTS ---");
            for (ContratSponsor c : results) {
                System.out.println(c);
            }
        }
    }

    private static void searchContractByAmount() throws SQLException {
        System.out.println("\n--- SEARCH CONTRACT BY AMOUNT ---");
        System.out.println("1. Amount >= Value");
        System.out.println("2. Amount <= Value");
        System.out.println("3. Amount between Min and Max");
        System.out.print("Choose search type: ");
        
        int choice = getIntInput();
        List<ContratSponsor> results = new ArrayList<>();
        
        try {
            switch (choice) {
                case 1:
                    System.out.print("Enter minimum amount: ");
                    double minAmount = getDoubleInput();
                    results = contratService.searchByMinMontant(minAmount);
                    break;
                case 2:
                    System.out.print("Enter maximum amount: ");
                    double maxAmount = getDoubleInput();
                    results = contratService.searchByMaxMontant(maxAmount);
                    break;
                case 3:
                    System.out.print("Enter minimum amount: ");
                    double min = getDoubleInput();
                    System.out.print("Enter maximum amount: ");
                    double max = getDoubleInput();
                    results = contratService.searchByMontantRange(min, max);
                    break;
                default:
                    System.out.println("Invalid option!");
                    return;
            }
            
            if (results.isEmpty()) {
                System.out.println("No contracts found!");
            } else {
                System.out.println("\n--- SEARCH RESULTS ---");
                for (ContratSponsor c : results) {
                    System.out.println(c);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void searchContractByStatus() throws SQLException {
        System.out.println("\n--- SEARCH CONTRACT BY STATUS ---");
        System.out.println("1. Search by Contract Status");
        System.out.println("2. Search by Payment Status");
        System.out.print("Choose search type: ");
        
        int choice = getIntInput();
        List<ContratSponsor> results = new ArrayList<>();
        
        try {
            switch (choice) {
                case 1:
                    System.out.print("Enter contract status (ACTIVE, PENDING, COMPLETED, etc.): ");
                    String statut = scanner.nextLine();
                    results = contratService.searchByStatut(statut);
                    break;
                case 2:
                    System.out.print("Enter payment status (PENDING, PARTIAL, PAID, OVERDUE): ");
                    String statutPaiement = scanner.nextLine();
                    results = contratService.searchByStatutPaiement(statutPaiement);
                    break;
                default:
                    System.out.println("Invalid option!");
                    return;
            }
            
            if (results.isEmpty()) {
                System.out.println("No contracts found!");
            } else {
                System.out.println("\n--- SEARCH RESULTS ---");
                for (ContratSponsor c : results) {
                    System.out.println(c);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void updateContract() throws SQLException {
        System.out.print("\nEnter Contract ID to update: ");
        int id = getIntInput();
        ContratSponsor contrat = contratService.getById(id);
        if (contrat == null) {
            System.out.println("Contract not found!");
            return;
        }
        
        System.out.println("Current data: " + contrat);
        System.out.print("New Amount (press Enter to skip): ");
        String montantStr = scanner.nextLine();
        if (!montantStr.isEmpty()) contrat.setMontant(Double.parseDouble(montantStr));
        
        System.out.print("New Status (press Enter to skip): ");
        String statut = scanner.nextLine();
        if (!statut.isEmpty()) contrat.setStatut(statut);
        
        System.out.print("New Payment Status (press Enter to skip): ");
        String statutPaiement = scanner.nextLine();
        if (!statutPaiement.isEmpty()) contrat.setStatutPaiement(statutPaiement);
        
        System.out.print("Change Sponsor (enter sponsor name or press Enter to skip): ");
        String sponsorName = scanner.nextLine();
        if (!sponsorName.isEmpty()) {
            List<Sponsor> foundSponsors = sponsorService.searchByName(sponsorName);
            if (!foundSponsors.isEmpty()) {
                contrat.setSponsorId(foundSponsors.get(0).getId());
            } else {
                System.out.println("Sponsor not found, keeping current sponsor!");
            }
        }
        
        contratService.update(contrat);
    }

    private static void deleteContract() throws SQLException {
        System.out.print("\nEnter Contract ID to delete: ");
        int id = getIntInput();
        contratService.delete(id);
    }

    private static void viewContractBySponsorId() throws SQLException {
        System.out.println("\n--- VIEW CONTRACTS BY SPONSOR ---");
        
        // Display available sponsors
        System.out.println("\n--- AVAILABLE SPONSORS ---");
        List<Sponsor> sponsors = sponsorService.getAll();
        if (sponsors.isEmpty()) {
            System.out.println("No sponsors available!");
            return;
        }
        
        for (Sponsor s : sponsors) {
            System.out.println("ID: " + s.getId() + " | Name: " + s.getNom());
        }
        
        System.out.print("\nEnter Sponsor Name: ");
        String sponsorName = scanner.nextLine();
        List<Sponsor> foundSponsors = sponsorService.searchByName(sponsorName);
        if (foundSponsors.isEmpty()) {
            System.out.println("Sponsor not found!");
            return;
        }
        int sponsorId = foundSponsors.get(0).getId();
        
        List<ContratSponsor> contrats = contratService.searchBySponsorId(sponsorId);
        if (contrats.isEmpty()) {
            System.out.println("No contracts found for sponsor: " + sponsorName);
        } else {
            System.out.println("\n--- CONTRACTS FOR SPONSOR: " + sponsorName + " ---");
            for (ContratSponsor c : contrats) {
                System.out.println(c);
            }
        }
    }

    private static int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a number.");
            return -1;
        }
    }

    private static double getDoubleInput() {
        try {
            return Double.parseDouble(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a number.");
            return 0.0;
        }
    }
}

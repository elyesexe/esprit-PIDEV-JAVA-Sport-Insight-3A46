package tn.esprit.mains;

import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.entities.Matchs;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.MatchsService;
import tn.esprit.tools.MyConnection;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MatchMain {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("Connected to database: " + connection.getConnection().getCatalog());

            EquipeService equipeService = new EquipeService();
            JoueurService joueurService = new JoueurService();
            MatchsService matchsService = new MatchsService();

            boolean running = true;
            while (running) {
                printSection("MATCH MODULE");
                printMenuOption("1", "Manage equipes");
                printMenuOption("2", "Manage joueurs");
                printMenuOption("3", "Manage matchs");
                printMenuOption("4", "Statistics");
                printMenuOption("0", "Exit");
                System.out.print("Select an option: ");
                int choice = Integer.parseInt(SCANNER.nextLine());

                switch (choice) {
                    case 1 -> handleEquipe(equipeService);
                    case 2 -> handleJoueur(joueurService);
                    case 3 -> handleMatchs(matchsService);
                    case 4 -> handleStatistics(equipeService, joueurService, matchsService);
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

    private static void handleEquipe(EquipeService equipeService) throws Exception {
        printSection("EQUIPES");
        printMenuOption("1", "Add equipe");
        printMenuOption("2", "Display all equipes");
        printMenuOption("3", "Update equipe");
        printMenuOption("4", "Delete equipe");
        printMenuOption("5", "Search equipe");
        printMenuOption("6", "Sort equipes");
        System.out.print("Select an action: ");
        int choice = Integer.parseInt(SCANNER.nextLine());

        switch (choice) {
            case 1 -> {
                System.out.print("Nom: ");
                String nom = SCANNER.nextLine();
                System.out.print("Coach: ");
                String coach = SCANNER.nextLine();
                System.out.print("Adresse: ");
                String adresse = SCANNER.nextLine();
                System.out.print("Telephone: ");
                String telephone = SCANNER.nextLine();
                System.out.print("Email: ");
                String email = SCANNER.nextLine();
                System.out.print("Image: ");
                String image = SCANNER.nextLine();

                equipeService.add(new Equipe(nom, coach, adresse, telephone, email, image));
                System.out.println("Equipe added successfully.");
            }
            case 2 -> displayEquipes(equipeService.getAll());
            case 3 -> {
                System.out.print("Equipe id to update: ");
                int equipeId = Integer.parseInt(SCANNER.nextLine());
                Equipe equipe = equipeService.getById(equipeId);
                if (equipe == null) {
                    System.out.println("Equipe not found.");
                    return;
                }

                System.out.print("New nom: ");
                equipe.setNom(SCANNER.nextLine());
                System.out.print("New coach: ");
                equipe.setCoach(SCANNER.nextLine());
                System.out.print("New adresse: ");
                equipe.setAdresse(SCANNER.nextLine());
                System.out.print("New telephone: ");
                equipe.setTelephone(SCANNER.nextLine());
                System.out.print("New email: ");
                equipe.setEmail(SCANNER.nextLine());
                System.out.print("New image: ");
                equipe.setImage(SCANNER.nextLine());
                equipeService.update(equipe);
                System.out.println("Equipe updated successfully.");
            }
            case 4 -> {
                System.out.print("Equipe id to delete: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                equipeService.delete(id);
                System.out.println("Equipe deleted successfully.");
            }
            case 5 -> {
                System.out.print("Keyword: ");
                String keyword = SCANNER.nextLine().trim().toLowerCase();
                displayEquipes(equipeService.getAll().stream()
                        .filter(equipe -> containsIgnoreCase(equipe.getNom(), keyword)
                                || containsIgnoreCase(equipe.getCoach(), keyword)
                                || containsIgnoreCase(equipe.getAdresse(), keyword)
                                || containsIgnoreCase(equipe.getEmail(), keyword))
                        .toList());
            }
            case 6 -> {
                List<Equipe> equipes = equipeService.getAll();
                printSection("SORT EQUIPES");
                printMenuOption("1", "By nom");
                printMenuOption("2", "By coach");
                printMenuOption("3", "By adresse");
                System.out.print("Select a sort mode: ");
                int sortChoice = Integer.parseInt(SCANNER.nextLine());
                switch (sortChoice) {
                    case 1 -> equipes.sort(Comparator.comparing(Equipe::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    case 2 -> equipes.sort(Comparator.comparing(Equipe::getCoach, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    case 3 -> equipes.sort(Comparator.comparing(Equipe::getAdresse, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    default -> {
                        System.out.println("Invalid choice.");
                        return;
                    }
                }
                displayEquipes(equipes);
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void handleJoueur(JoueurService joueurService) throws Exception {
        printSection("JOUEURS");
        printMenuOption("1", "Add joueur");
        printMenuOption("2", "Display all joueurs");
        printMenuOption("3", "Update joueur");
        printMenuOption("4", "Delete joueur");
        printMenuOption("5", "Search joueur");
        printMenuOption("6", "Sort joueurs");
        System.out.print("Select an action: ");
        int choice = Integer.parseInt(SCANNER.nextLine());

        switch (choice) {
            case 1 -> {
                System.out.print("Nom: ");
                String nom = SCANNER.nextLine();
                System.out.print("Prenom: ");
                String prenom = SCANNER.nextLine();
                System.out.print("Date naissance (yyyy-mm-dd): ");
                LocalDate dateNaissance = LocalDate.parse(SCANNER.nextLine());
                System.out.print("Numero: ");
                int numero = Integer.parseInt(SCANNER.nextLine());
                System.out.print("Image: ");
                String image = SCANNER.nextLine();
                System.out.print("Equipe id: ");
                String equipeIdInput = SCANNER.nextLine().trim();

                joueurService.add(new Joueur(
                        nom,
                        prenom,
                        dateNaissance,
                        numero,
                        image,
                        equipeIdInput.isEmpty() ? null : Integer.parseInt(equipeIdInput)
                ));
                System.out.println("Joueur added successfully.");
            }
            case 2 -> displayJoueurs(joueurService.getAll());
            case 3 -> {
                System.out.print("Joueur id to update: ");
                int joueurId = Integer.parseInt(SCANNER.nextLine());
                Joueur joueur = joueurService.getById(joueurId);
                if (joueur == null) {
                    System.out.println("Joueur not found.");
                    return;
                }

                System.out.print("New nom: ");
                joueur.setNom(SCANNER.nextLine());
                System.out.print("New prenom: ");
                joueur.setPrenom(SCANNER.nextLine());
                System.out.print("New date naissance (yyyy-mm-dd): ");
                joueur.setDateNaissance(LocalDate.parse(SCANNER.nextLine()));
                System.out.print("New numero: ");
                joueur.setNumero(Integer.parseInt(SCANNER.nextLine()));
                System.out.print("New image: ");
                joueur.setImage(SCANNER.nextLine());
                System.out.print("New equipe id: ");
                String equipeIdInput = SCANNER.nextLine().trim();
                joueur.setEquipeId(equipeIdInput.isEmpty() ? null : Integer.parseInt(equipeIdInput));
                joueurService.update(joueur);
                System.out.println("Joueur updated successfully.");
            }
            case 4 -> {
                System.out.print("Joueur id to delete: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                joueurService.delete(id);
                System.out.println("Joueur deleted successfully.");
            }
            case 5 -> {
                System.out.print("Keyword: ");
                String keyword = SCANNER.nextLine().trim().toLowerCase();
                displayJoueurs(joueurService.getAll().stream()
                        .filter(joueur -> containsIgnoreCase(joueur.getNom(), keyword)
                                || containsIgnoreCase(joueur.getPrenom(), keyword)
                                || String.valueOf(joueur.getNumero()).contains(keyword))
                        .toList());
            }
            case 6 -> {
                List<Joueur> joueurs = joueurService.getAll();
                printSection("SORT JOUEURS");
                printMenuOption("1", "By nom");
                printMenuOption("2", "By prenom");
                printMenuOption("3", "By numero");
                System.out.print("Select a sort mode: ");
                int sortChoice = Integer.parseInt(SCANNER.nextLine());
                switch (sortChoice) {
                    case 1 -> joueurs.sort(Comparator.comparing(Joueur::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    case 2 -> joueurs.sort(Comparator.comparing(Joueur::getPrenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    case 3 -> joueurs.sort(Comparator.comparingInt(Joueur::getNumero));
                    default -> {
                        System.out.println("Invalid choice.");
                        return;
                    }
                }
                displayJoueurs(joueurs);
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void handleMatchs(MatchsService matchsService) throws Exception {
        printSection("MATCHS");
        printMenuOption("1", "Add match");
        printMenuOption("2", "Display all matchs");
        printMenuOption("3", "Update match");
        printMenuOption("4", "Delete match");
        printMenuOption("5", "Search match");
        printMenuOption("6", "Sort matchs");
        System.out.print("Select an action: ");
        int choice = Integer.parseInt(SCANNER.nextLine());

        switch (choice) {
            case 1 -> {
                System.out.print("Id match: ");
                String idMatch = SCANNER.nextLine();
                System.out.print("Date match (yyyy-mm-dd): ");
                LocalDate dateMatch = LocalDate.parse(SCANNER.nextLine());
                System.out.print("Heure debut (HH:mm:ss): ");
                LocalTime heureDebut = LocalTime.parse(SCANNER.nextLine());
                System.out.print("Lieu: ");
                String lieu = SCANNER.nextLine();
                System.out.print("Type: ");
                String type = SCANNER.nextLine();
                System.out.print("Statut: ");
                String statut = SCANNER.nextLine();
                System.out.print("Lineup domicile: ");
                String lineupDomicile = SCANNER.nextLine();
                System.out.print("Lineup exterieur: ");
                String lineupExterieur = SCANNER.nextLine();
                System.out.print("Score equipe domicile: ");
                String scoreDomicileInput = SCANNER.nextLine().trim();
                System.out.print("Score equipe exterieur: ");
                String scoreExterieurInput = SCANNER.nextLine().trim();
                System.out.print("Equipe domicile id: ");
                String equipeDomicileIdInput = SCANNER.nextLine().trim();
                System.out.print("Equipe exterieur id: ");
                String equipeExterieurIdInput = SCANNER.nextLine().trim();

                matchsService.add(new Matchs(
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
                ));
                System.out.println("Match added successfully.");
            }
            case 2 -> displayMatchs(matchsService.getAll());
            case 3 -> {
                System.out.print("Match id to update: ");
                int matchId = Integer.parseInt(SCANNER.nextLine());
                Matchs match = matchsService.getById(matchId);
                if (match == null) {
                    System.out.println("Match not found.");
                    return;
                }

                System.out.print("New id match: ");
                match.setIdMatch(SCANNER.nextLine());
                System.out.print("New date match (yyyy-mm-dd): ");
                match.setDateMatch(LocalDate.parse(SCANNER.nextLine()));
                System.out.print("New heure debut (HH:mm:ss): ");
                match.setHeureDebut(LocalTime.parse(SCANNER.nextLine()));
                System.out.print("New lieu: ");
                match.setLieu(SCANNER.nextLine());
                System.out.print("New type: ");
                match.setType(SCANNER.nextLine());
                System.out.print("New statut: ");
                match.setStatut(SCANNER.nextLine());
                System.out.print("New lineup domicile: ");
                match.setLineupDomicile(SCANNER.nextLine());
                System.out.print("New lineup exterieur: ");
                match.setLineupExterieur(SCANNER.nextLine());
                System.out.print("New score equipe domicile: ");
                String scoreDomicileInput = SCANNER.nextLine().trim();
                match.setScoreEquipeDomicile(scoreDomicileInput.isEmpty() ? null : Integer.parseInt(scoreDomicileInput));
                System.out.print("New score equipe exterieur: ");
                String scoreExterieurInput = SCANNER.nextLine().trim();
                match.setScoreEquipeExterieur(scoreExterieurInput.isEmpty() ? null : Integer.parseInt(scoreExterieurInput));
                System.out.print("New equipe domicile id: ");
                String equipeDomicileIdInput = SCANNER.nextLine().trim();
                match.setEquipeDomicileId(equipeDomicileIdInput.isEmpty() ? null : Integer.parseInt(equipeDomicileIdInput));
                System.out.print("New equipe exterieur id: ");
                String equipeExterieurIdInput = SCANNER.nextLine().trim();
                match.setEquipeExterieurId(equipeExterieurIdInput.isEmpty() ? null : Integer.parseInt(equipeExterieurIdInput));
                matchsService.update(match);
                System.out.println("Match updated successfully.");
            }
            case 4 -> {
                System.out.print("Match id to delete: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                matchsService.delete(id);
                System.out.println("Match deleted successfully.");
            }
            case 5 -> {
                System.out.print("Keyword: ");
                String keyword = SCANNER.nextLine().trim().toLowerCase();
                displayMatchs(matchsService.getAll().stream()
                        .filter(match -> containsIgnoreCase(match.getIdMatch(), keyword)
                                || containsIgnoreCase(match.getLieu(), keyword)
                                || containsIgnoreCase(match.getType(), keyword)
                                || containsIgnoreCase(match.getStatut(), keyword))
                        .toList());
            }
            case 6 -> {
                List<Matchs> matchs = matchsService.getAll();
                printSection("SORT MATCHS");
                printMenuOption("1", "By date");
                printMenuOption("2", "By location");
                printMenuOption("3", "By type");
                System.out.print("Select a sort mode: ");
                int sortChoice = Integer.parseInt(SCANNER.nextLine());
                switch (sortChoice) {
                    case 1 -> matchs.sort(Comparator.comparing(Matchs::getDateMatch, Comparator.nullsLast(LocalDate::compareTo)));
                    case 2 -> matchs.sort(Comparator.comparing(Matchs::getLieu, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    case 3 -> matchs.sort(Comparator.comparing(Matchs::getType, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                    default -> {
                        System.out.println("Invalid choice.");
                        return;
                    }
                }
                displayMatchs(matchs);
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void handleStatistics(EquipeService equipeService, JoueurService joueurService, MatchsService matchsService) throws Exception {
        printSection("MATCH STATISTICS");
        printMenuOption("1", "Equipe statistics");
        printMenuOption("2", "Joueur statistics");
        printMenuOption("3", "Match statistics");
        printMenuOption("4", "Global summary");
        System.out.print("Select an action: ");
        int choice = Integer.parseInt(SCANNER.nextLine());

        switch (choice) {
            case 1 -> showEquipeStatistics(equipeService, joueurService);
            case 2 -> showJoueurStatistics(joueurService);
            case 3 -> showMatchStatistics(matchsService);
            case 4 -> {
                showEquipeStatistics(equipeService, joueurService);
                showJoueurStatistics(joueurService);
                showMatchStatistics(matchsService);
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void showEquipeStatistics(EquipeService equipeService, JoueurService joueurService) throws Exception {
        List<Equipe> equipes = equipeService.getAll();
        List<Joueur> joueurs = joueurService.getAll();

        printSection("EQUIPE STATISTICS");
        System.out.println("Total equipes: " + equipes.size());

        long coachCount = equipes.stream()
                .map(Equipe::getCoach)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .count();
        System.out.println("Distinct coaches: " + coachCount);

        Map<Integer, Long> playersPerEquipe = joueurs.stream()
                .filter(joueur -> joueur.getEquipeId() != null)
                .collect(Collectors.groupingBy(Joueur::getEquipeId, Collectors.counting()));

        equipes.stream()
                .max(Comparator.comparingLong(equipe -> playersPerEquipe.getOrDefault(equipe.getId(), 0L)))
                .ifPresent(equipe -> System.out.println("Equipe with most players: " + equipe.getNom()
                        + " (" + playersPerEquipe.getOrDefault(equipe.getId(), 0L) + " joueurs)"));

        printTopGrouping("Equipes by address", equipes, Equipe::getAdresse);
    }

    private static void showJoueurStatistics(JoueurService joueurService) throws Exception {
        List<Joueur> joueurs = joueurService.getAll();

        printSection("JOUEUR STATISTICS");
        System.out.println("Total joueurs: " + joueurs.size());
        System.out.println("Assigned to an equipe: " + joueurs.stream().filter(joueur -> joueur.getEquipeId() != null).count());
        System.out.println("Without equipe: " + joueurs.stream().filter(joueur -> joueur.getEquipeId() == null).count());

        joueurs.stream()
                .min(Comparator.comparing(Joueur::getDateNaissance, Comparator.nullsLast(LocalDate::compareTo)))
                .ifPresent(joueur -> System.out.println("Oldest joueur: " + joueur.getNom() + " " + joueur.getPrenom()
                        + " (" + joueur.getDateNaissance() + ")"));

        joueurs.stream()
                .max(Comparator.comparing(Joueur::getDateNaissance, Comparator.nullsFirst(LocalDate::compareTo)))
                .ifPresent(joueur -> System.out.println("Youngest joueur: " + joueur.getNom() + " " + joueur.getPrenom()
                        + " (" + joueur.getDateNaissance() + ")"));

        double averageNumero = joueurs.stream()
                .mapToInt(Joueur::getNumero)
                .average()
                .orElse(0);
        System.out.printf("Average player number: %.2f%n", averageNumero);
    }

    private static void showMatchStatistics(MatchsService matchsService) throws Exception {
        List<Matchs> matchs = matchsService.getAll();

        printSection("MATCH STATISTICS");
        System.out.println("Total matchs: " + matchs.size());
        System.out.println("Played matchs: " + matchs.stream().filter(match -> hasScore(match.getScoreEquipeDomicile(), match.getScoreEquipeExterieur())).count());
        System.out.println("Pending matchs: " + matchs.stream().filter(match -> !hasScore(match.getScoreEquipeDomicile(), match.getScoreEquipeExterieur())).count());

        double averageGoals = matchs.stream()
                .filter(match -> hasScore(match.getScoreEquipeDomicile(), match.getScoreEquipeExterieur()))
                .mapToInt(match -> match.getScoreEquipeDomicile() + match.getScoreEquipeExterieur())
                .average()
                .orElse(0);
        System.out.printf("Average total goals per played match: %.2f%n", averageGoals);

        matchs.stream()
                .filter(match -> hasScore(match.getScoreEquipeDomicile(), match.getScoreEquipeExterieur()))
                .max(Comparator.comparingInt(match -> match.getScoreEquipeDomicile() + match.getScoreEquipeExterieur()))
                .ifPresent(match -> System.out.println("Highest scoring match: " + match.getIdMatch()
                        + " (" + match.getScoreEquipeDomicile() + "-" + match.getScoreEquipeExterieur() + ")"));

        printTopGrouping("Matches by status", matchs, Matchs::getStatut);
        printTopGrouping("Matches by type", matchs, Matchs::getType);
        printTopGrouping("Matches by location", matchs, Matchs::getLieu);
    }

    private static <T> void printTopGrouping(String label, List<T> items, Function<T, String> classifier) {
        Map<String, Long> grouped = items.stream()
                .map(classifier)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        if (grouped.isEmpty()) {
            System.out.println(label + ": no data");
            return;
        }

        System.out.println(label + ":");
        grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .limit(3)
                .forEach(entry -> System.out.println("- " + entry.getKey() + ": " + entry.getValue()));
    }

    private static boolean hasScore(Integer domicile, Integer exterieur) {
        return domicile != null && exterieur != null;
    }

    private static void displayEquipes(List<Equipe> equipes) {
        if (equipes.isEmpty()) {
            System.out.println("No equipes found.");
            return;
        }

        printSection("EQUIPE LIST");
        equipes.forEach(equipe -> {
            System.out.println("[" + equipe.getId() + "] " + safeText(equipe.getNom()));
            System.out.println("  Coach    : " + safeText(equipe.getCoach()));
            System.out.println("  Adresse  : " + safeText(equipe.getAdresse()));
            System.out.println("  Telephone: " + safeText(equipe.getTelephone()));
            System.out.println("  Email    : " + safeText(equipe.getEmail()));
            System.out.println("  Image    : " + safeText(equipe.getImage()));
            System.out.println();
        });
    }

    private static void displayJoueurs(List<Joueur> joueurs) {
        if (joueurs.isEmpty()) {
            System.out.println("No joueurs found.");
            return;
        }

        printSection("JOUEUR LIST");
        joueurs.forEach(joueur -> {
            System.out.println("[" + joueur.getId() + "] " + safeText(joueur.getNom()) + " " + safeText(joueur.getPrenom()));
            System.out.println("  Birth date: " + String.valueOf(joueur.getDateNaissance()));
            System.out.println("  Numero    : " + joueur.getNumero());
            System.out.println("  Equipe ID : " + safeText(joueur.getEquipeId()));
            System.out.println("  Image     : " + safeText(joueur.getImage()));
            System.out.println();
        });
    }

    private static void displayMatchs(List<Matchs> matchs) {
        if (matchs.isEmpty()) {
            System.out.println("No matchs found.");
            return;
        }

        printSection("MATCH LIST");
        matchs.forEach(match -> {
            String score = hasScore(match.getScoreEquipeDomicile(), match.getScoreEquipeExterieur())
                    ? match.getScoreEquipeDomicile() + " - " + match.getScoreEquipeExterieur()
                    : "Not played yet";

            System.out.println("[" + match.getId() + "] " + safeText(match.getIdMatch()));
            System.out.println("  Date/Time        : " + String.valueOf(match.getDateMatch()) + " " + String.valueOf(match.getHeureDebut()));
            System.out.println("  Location         : " + safeText(match.getLieu()));
            System.out.println("  Type             : " + safeText(match.getType()));
            System.out.println("  Status           : " + safeText(match.getStatut()));
            System.out.println("  Score            : " + score);
            System.out.println("  Home team ID     : " + safeText(match.getEquipeDomicileId()));
            System.out.println("  Away team ID     : " + safeText(match.getEquipeExterieurId()));
            System.out.println("  Home lineup      : " + safeText(match.getLineupDomicile()));
            System.out.println("  Away lineup      : " + safeText(match.getLineupExterieur()));
            System.out.println();
        });
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println("========================================");
        System.out.println(" " + title);
        System.out.println("========================================");
    }

    private static void printMenuOption(String key, String label) {
        System.out.println(key + ". " + label);
    }

    private static String safeText(Object value) {
        if (value == null) {
            return "-";
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    private static boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}

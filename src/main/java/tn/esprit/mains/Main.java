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
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Choose a table to manipulate (equipe, joueur, matchs) or type exit: ");
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
                    case "exit":
                        System.out.println("Application closed.");
                        return;
                    default:
                        System.out.println("Unknown table. Type equipe, joueur, matchs, or exit.");
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
}

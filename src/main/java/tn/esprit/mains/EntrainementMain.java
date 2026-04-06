package tn.esprit.mains;

import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Participation;
import tn.esprit.services.EntrainementService;
import tn.esprit.services.EvaluationService;
import tn.esprit.services.ParticipationService;
import tn.esprit.tools.MyConnection;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

public class EntrainementMain {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("Connected to database: " + connection.getConnection().getCatalog());

            EntrainementService entrainementService = new EntrainementService();
            EvaluationService evaluationService = new EvaluationService();
            ParticipationService participationService = new ParticipationService();

            boolean running = true;
            while (running) {
                System.out.println("\nSPORT INSIGHT MENU");
                System.out.println("1. Manage entrainements");
                System.out.println("2. Manage evaluations");
                System.out.println("3. Manage participations");
                System.out.println("0. Exit");
                System.out.print("Choice: ");
                int choix = Integer.parseInt(SCANNER.nextLine());

                switch (choix) {
                    case 1 -> menuEntrainement(entrainementService);
                    case 2 -> menuEvaluation(evaluationService);
                    case 3 -> menuParticipation(participationService);
                    case 0 -> {
                        System.out.println("Application closed.");
                        running = false;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void menuEntrainement(EntrainementService service) throws Exception {
        System.out.println("\n--- ENTRAINEMENTS ---");
        System.out.println("1. Add entrainement");
        System.out.println("2. Display all entrainements");
        System.out.println("3. Update entrainement");
        System.out.println("4. Delete entrainement");
        System.out.println("5. Search entrainement");
        System.out.println("6. Sort entrainements");
        System.out.print("Choice: ");
        int choix = Integer.parseInt(SCANNER.nextLine());

        switch (choix) {
            case 1 -> {
                System.out.print("Date (yyyy-mm-dd): ");
                LocalDate date = LocalDate.parse(SCANNER.nextLine());
                System.out.print("Start time (HH:mm): ");
                LocalTime heureDebut = LocalTime.parse(SCANNER.nextLine());
                System.out.print("End time (HH:mm): ");
                LocalTime heureFin = LocalTime.parse(SCANNER.nextLine());
                System.out.print("Type: ");
                String type = SCANNER.nextLine();
                System.out.print("Objectif: ");
                String objectif = SCANNER.nextLine();
                System.out.print("Lieu: ");
                String lieu = SCANNER.nextLine();
                System.out.print("Entraineur id: ");
                int entraineurId = Integer.parseInt(SCANNER.nextLine());

                Entrainement entrainement = new Entrainement(date, heureDebut, heureFin, type, objectif, lieu, entraineurId);
                service.add(entrainement);
            }
            case 2 -> service.getAll().forEach(System.out::println);
            case 3 -> {
                System.out.print("Entrainement id to update: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                Entrainement entrainement = service.getById(id);
                if (entrainement == null) {
                    System.out.println("Entrainement not found.");
                    return;
                }

                System.out.print("New type (" + entrainement.getType() + "): ");
                entrainement.setType(SCANNER.nextLine());
                System.out.print("New lieu (" + entrainement.getLieu() + "): ");
                entrainement.setLieu(SCANNER.nextLine());
                System.out.print("New objectif (" + entrainement.getObjectif() + "): ");
                entrainement.setObjectif(SCANNER.nextLine());
                service.update(entrainement);
            }
            case 4 -> {
                System.out.print("Entrainement id to delete: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                service.delete(id);
            }
            case 5 -> {
                System.out.print("Keyword: ");
                String keyword = SCANNER.nextLine();
                service.search(keyword).forEach(System.out::println);
            }
            case 6 -> {
                System.out.println("Sort by: 1.Date  2.Type  3.Lieu");
                System.out.print("Choice: ");
                int tri = Integer.parseInt(SCANNER.nextLine());
                switch (tri) {
                    case 1 -> service.sortByDate().forEach(System.out::println);
                    case 2 -> service.sortByType().forEach(System.out::println);
                    case 3 -> service.sortByLieu().forEach(System.out::println);
                    default -> System.out.println("Invalid choice.");
                }
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void menuEvaluation(EvaluationService service) throws Exception {
        System.out.println("\n--- EVALUATIONS ---");
        System.out.println("1. Add evaluation");
        System.out.println("2. Display all evaluations");
        System.out.println("3. Update evaluation");
        System.out.println("4. Delete evaluation");
        System.out.println("5. Search evaluation");
        System.out.println("6. Sort evaluations");
        System.out.print("Choice: ");
        int choix = Integer.parseInt(SCANNER.nextLine());

        switch (choix) {
            case 1 -> {
                System.out.print("Note physique (0-20): ");
                double notePhysique = Double.parseDouble(SCANNER.nextLine());
                System.out.print("Note technique (0-20): ");
                double noteTechnique = Double.parseDouble(SCANNER.nextLine());
                System.out.print("Note tactique (0-20): ");
                double noteTactique = Double.parseDouble(SCANNER.nextLine());
                System.out.print("Commentaire: ");
                String commentaire = SCANNER.nextLine();
                System.out.print("Entrainement id: ");
                int entrainementId = Integer.parseInt(SCANNER.nextLine());
                System.out.print("Joueur id: ");
                int joueurId = Integer.parseInt(SCANNER.nextLine());

                Evaluation evaluation = new Evaluation(notePhysique, noteTechnique, noteTactique, commentaire, entrainementId, joueurId);
                service.add(evaluation);
            }
            case 2 -> service.getAll().forEach(System.out::println);
            case 3 -> {
                System.out.print("Evaluation id to update: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                Evaluation evaluation = service.getById(id);
                if (evaluation == null) {
                    System.out.println("Evaluation not found.");
                    return;
                }

                System.out.print("New note physique (" + evaluation.getNotePhysique() + "): ");
                evaluation.setNotePhysique(Double.parseDouble(SCANNER.nextLine()));
                System.out.print("New note technique (" + evaluation.getNoteTechnique() + "): ");
                evaluation.setNoteTechnique(Double.parseDouble(SCANNER.nextLine()));
                System.out.print("New note tactique (" + evaluation.getNoteTactique() + "): ");
                evaluation.setNoteTactique(Double.parseDouble(SCANNER.nextLine()));
                System.out.print("New commentaire (" + evaluation.getCommentaire() + "): ");
                evaluation.setCommentaire(SCANNER.nextLine());
                service.update(evaluation);
            }
            case 4 -> {
                System.out.print("Evaluation id to delete: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                service.delete(id);
            }
            case 5 -> {
                System.out.print("Keyword: ");
                String keyword = SCANNER.nextLine();
                service.search(keyword).forEach(System.out::println);
            }
            case 6 -> {
                System.out.println("Sort by: 1.Moyenne  2.Note Physique  3.Note Technique  4.Note Tactique");
                System.out.print("Choice: ");
                int tri = Integer.parseInt(SCANNER.nextLine());
                switch (tri) {
                    case 1 -> service.sortByMoyenne().forEach(System.out::println);
                    case 2 -> service.sortByNotePhysique().forEach(System.out::println);
                    case 3 -> service.sortByNoteTechnique().forEach(System.out::println);
                    case 4 -> service.sortByNoteTactique().forEach(System.out::println);
                    default -> System.out.println("Invalid choice.");
                }
            }
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void menuParticipation(ParticipationService service) throws Exception {
        System.out.println("\n--- PARTICIPATIONS ---");
        System.out.println("1. Add participation");
        System.out.println("2. Display all participations");
        System.out.println("3. Update participation");
        System.out.println("4. Delete participation");
        System.out.println("5. Search participation");
        System.out.println("6. Sort participations");
        System.out.print("Choice: ");
        int choix = Integer.parseInt(SCANNER.nextLine());

        switch (choix) {
            case 1 -> {
                System.out.print("Presence (Present/Absent): ");
                String presence = SCANNER.nextLine();
                System.out.print("Justification absence (optional): ");
                String justification = SCANNER.nextLine();
                if (justification.isBlank()) {
                    justification = null;
                }
                System.out.print("Entrainement id: ");
                int entrainementId = Integer.parseInt(SCANNER.nextLine());
                System.out.print("Joueur id: ");
                int joueurId = Integer.parseInt(SCANNER.nextLine());

                Participation participation = new Participation(presence, justification, entrainementId, joueurId);
                service.add(participation);
            }
            case 2 -> service.getAll().forEach(System.out::println);
            case 3 -> {
                System.out.print("Participation id to update: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                Participation participation = service.getById(id);
                if (participation == null) {
                    System.out.println("Participation not found.");
                    return;
                }

                System.out.print("New presence (" + participation.getPresence() + "): ");
                participation.setPresence(SCANNER.nextLine());
                System.out.print("New justification (" + participation.getJustificationAbsence() + "): ");
                String justification = SCANNER.nextLine();
                participation.setJustificationAbsence(justification.isBlank() ? null : justification);
                service.update(participation);
            }
            case 4 -> {
                System.out.print("Participation id to delete: ");
                int id = Integer.parseInt(SCANNER.nextLine());
                service.delete(id);
            }
            case 5 -> {
                System.out.print("Keyword: ");
                String keyword = SCANNER.nextLine();
                service.search(keyword).forEach(System.out::println);
            }
            case 6 -> {
                System.out.println("Sort by: 1.Presence  2.Joueur  3.Entrainement");
                System.out.print("Choice: ");
                int tri = Integer.parseInt(SCANNER.nextLine());
                switch (tri) {
                    case 1 -> service.sortByPresence().forEach(System.out::println);
                    case 2 -> service.sortByJoueur().forEach(System.out::println);
                    case 3 -> service.sortByEntrainement().forEach(System.out::println);
                    default -> System.out.println("Invalid choice.");
                }
            }
            default -> System.out.println("Invalid choice.");
        }
    }
}

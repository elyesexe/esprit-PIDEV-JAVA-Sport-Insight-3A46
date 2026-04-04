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

public class Main {
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("✅ Connected to database: " + connection.getConnection().getCatalog());

            EntrainementService entrainementService = new EntrainementService();
            EvaluationService evaluationService = new EvaluationService();
            ParticipationService participationService = new ParticipationService();

            boolean running = true;
            while (running) {
                System.out.println("\n╔══════════════════════════════════╗");
                System.out.println("║        SPORT INSIGHT MENU        ║");
                System.out.println("╠══════════════════════════════════╣");
                System.out.println("║  1. Gérer les Entrainements      ║");
                System.out.println("║  2. Gérer les Evaluations        ║");
                System.out.println("║  3. Gérer les Participations     ║");
                System.out.println("║  0. Quitter                      ║");
                System.out.println("╚══════════════════════════════════╝");
                System.out.print("Votre choix: ");
                int choix = Integer.parseInt(scanner.nextLine());

                switch (choix) {
                    case 1 -> menuEntrainement(entrainementService);
                    case 2 -> menuEvaluation(evaluationService);
                    case 3 -> menuParticipation(participationService);
                    case 0 -> {
                        System.out.println("Au revoir !");
                        running = false;
                    }
                    default -> System.out.println("❌ Choix invalide !");
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MENU ENTRAINEMENT
    // ════════════════════════════════════════════════════════════════
    static void menuEntrainement(EntrainementService service) throws Exception {
        System.out.println("\n--- ENTRAINEMENTS ---");
        System.out.println("1. Ajouter un entrainement");
        System.out.println("2. Afficher tous les entrainements");
        System.out.println("3. Modifier un entrainement");
        System.out.println("4. Supprimer un entrainement");
        System.out.println("5. Rechercher un entrainement");
        System.out.println("6. Trier les entrainements");
        System.out.print("Votre choix: ");
        int choix = Integer.parseInt(scanner.nextLine());

        switch (choix) {
            case 1 -> {
                System.out.println("\n➕ AJOUTER UN ENTRAINEMENT");
                System.out.print("Date (YYYY-MM-DD): ");
                LocalDate date = LocalDate.parse(scanner.nextLine());
                System.out.print("Heure début (HH:MM): ");
                LocalTime heureDebut = LocalTime.parse(scanner.nextLine());
                System.out.print("Heure fin (HH:MM): ");
                LocalTime heureFin = LocalTime.parse(scanner.nextLine());
                System.out.print("Type (ex: Cardio, Technique, Tactique): ");
                String type = scanner.nextLine();
                System.out.print("Objectif: ");
                String objectif = scanner.nextLine();
                System.out.print("Lieu: ");
                String lieu = scanner.nextLine();
                System.out.print("ID Entraineur: ");
                int entraineurId = Integer.parseInt(scanner.nextLine());

                Entrainement e = new Entrainement(date, heureDebut, heureFin, type, objectif, lieu, entraineurId);
                service.add(e);
            }
            case 2 -> {
                System.out.println("\n📋 TOUS LES ENTRAINEMENTS");
                service.getAll().forEach(System.out::println);
            }
            case 3 -> {
                System.out.print("\n✏️ ID de l'entrainement à modifier: ");
                int id = Integer.parseInt(scanner.nextLine());
                Entrainement e = service.getById(id);
                if (e == null) { System.out.println("❌ Entrainement introuvable !"); break; }
                System.out.println("Entrainement actuel: " + e);
                System.out.print("Nouveau type (actuel: " + e.getType() + "): ");
                e.setType(scanner.nextLine());
                System.out.print("Nouveau lieu (actuel: " + e.getLieu() + "): ");
                e.setLieu(scanner.nextLine());
                System.out.print("Nouvel objectif (actuel: " + e.getObjectif() + "): ");
                e.setObjectif(scanner.nextLine());
                service.update(e);
            }
            case 4 -> {
                System.out.print("\n🗑️ ID de l'entrainement à supprimer: ");
                int id = Integer.parseInt(scanner.nextLine());
                service.delete(id);
            }
            case 5 -> {
                System.out.print("\n🔍 Mot clé à rechercher (type/lieu/objectif): ");
                String keyword = scanner.nextLine();
                service.search(keyword).forEach(System.out::println);
            }
            case 6 -> {
                System.out.println("Trier par: 1.Date  2.Type  3.Lieu");
                System.out.print("Choix: ");
                int tri = Integer.parseInt(scanner.nextLine());
                switch (tri) {
                    case 1 -> service.sortByDate().forEach(System.out::println);
                    case 2 -> service.sortByType().forEach(System.out::println);
                    case 3 -> service.sortByLieu().forEach(System.out::println);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MENU EVALUATION
    // ════════════════════════════════════════════════════════════════
    static void menuEvaluation(EvaluationService service) throws Exception {
        System.out.println("\n--- EVALUATIONS ---");
        System.out.println("1. Ajouter une évaluation");
        System.out.println("2. Afficher toutes les évaluations");
        System.out.println("3. Modifier une évaluation");
        System.out.println("4. Supprimer une évaluation");
        System.out.println("5. Rechercher une évaluation");
        System.out.println("6. Trier les évaluations");
        System.out.print("Votre choix: ");
        int choix = Integer.parseInt(scanner.nextLine());

        switch (choix) {
            case 1 -> {
                System.out.println("\n➕ AJOUTER UNE EVALUATION");
                System.out.print("Note physique (0-20): ");
                double notePhysique = Double.parseDouble(scanner.nextLine());
                System.out.print("Note technique (0-20): ");
                double noteTechnique = Double.parseDouble(scanner.nextLine());
                System.out.print("Note tactique (0-20): ");
                double noteTactique = Double.parseDouble(scanner.nextLine());
                System.out.print("Commentaire: ");
                String commentaire = scanner.nextLine();
                System.out.print("ID Entrainement: ");
                int entrainementId = Integer.parseInt(scanner.nextLine());
                System.out.print("ID Joueur: ");
                int joueurId = Integer.parseInt(scanner.nextLine());

                Evaluation ev = new Evaluation(notePhysique, noteTechnique, noteTactique, commentaire, entrainementId, joueurId);
                service.add(ev);
            }
            case 2 -> {
                System.out.println("\n📋 TOUTES LES EVALUATIONS");
                service.getAll().forEach(System.out::println);
            }
            case 3 -> {
                System.out.print("\n✏️ ID de l'évaluation à modifier: ");
                int id = Integer.parseInt(scanner.nextLine());
                Evaluation ev = service.getById(id);
                if (ev == null) { System.out.println("❌ Evaluation introuvable !"); break; }
                System.out.println("Evaluation actuelle: " + ev);
                System.out.print("Nouvelle note physique (actuelle: " + ev.getNotePhysique() + "): ");
                ev.setNotePhysique(Double.parseDouble(scanner.nextLine()));
                System.out.print("Nouvelle note technique (actuelle: " + ev.getNoteTechnique() + "): ");
                ev.setNoteTechnique(Double.parseDouble(scanner.nextLine()));
                System.out.print("Nouvelle note tactique (actuelle: " + ev.getNoteTactique() + "): ");
                ev.setNoteTactique(Double.parseDouble(scanner.nextLine()));
                System.out.print("Nouveau commentaire (actuel: " + ev.getCommentaire() + "): ");
                ev.setCommentaire(scanner.nextLine());
                service.update(ev);
            }
            case 4 -> {
                System.out.print("\n🗑️ ID de l'évaluation à supprimer: ");
                int id = Integer.parseInt(scanner.nextLine());
                service.delete(id);
            }
            case 5 -> {
                System.out.print("\n🔍 Mot clé à rechercher (commentaire): ");
                String keyword = scanner.nextLine();
                service.search(keyword).forEach(System.out::println);
            }
            case 6 -> {
                System.out.println("Trier par: 1.Moyenne  2.Note Physique  3.Note Technique  4.Note Tactique");
                System.out.print("Choix: ");
                int tri = Integer.parseInt(scanner.nextLine());
                switch (tri) {
                    case 1 -> service.sortByMoyenne().forEach(System.out::println);
                    case 2 -> service.sortByNotePhysique().forEach(System.out::println);
                    case 3 -> service.sortByNoteTechnique().forEach(System.out::println);
                    case 4 -> service.sortByNoteTactique().forEach(System.out::println);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MENU PARTICIPATION
    // ════════════════════════════════════════════════════════════════
    static void menuParticipation(ParticipationService service) throws Exception {
        System.out.println("\n--- PARTICIPATIONS ---");
        System.out.println("1. Ajouter une participation");
        System.out.println("2. Afficher toutes les participations");
        System.out.println("3. Modifier une participation");
        System.out.println("4. Supprimer une participation");
        System.out.println("5. Rechercher une participation");
        System.out.println("6. Trier les participations");
        System.out.print("Votre choix: ");
        int choix = Integer.parseInt(scanner.nextLine());

        switch (choix) {
            case 1 -> {
                System.out.println("\n➕ AJOUTER UNE PARTICIPATION");
                System.out.print("Présence (Présent/Absent): ");
                String presence = scanner.nextLine();
                System.out.print("Justification absence (appuyer Entrée si présent): ");
                String justification = scanner.nextLine();
                if (justification.isBlank()) justification = null;
                System.out.print("ID Entrainement: ");
                int entrainementId = Integer.parseInt(scanner.nextLine());
                System.out.print("ID Joueur: ");
                int joueurId = Integer.parseInt(scanner.nextLine());

                Participation p = new Participation(presence, justification, entrainementId, joueurId);
                service.add(p);
            }
            case 2 -> {
                System.out.println("\n📋 TOUTES LES PARTICIPATIONS");
                service.getAll().forEach(System.out::println);
            }
            case 3 -> {
                System.out.print("\n✏️ ID de la participation à modifier: ");
                int id = Integer.parseInt(scanner.nextLine());
                Participation p = service.getById(id);
                if (p == null) { System.out.println("❌ Participation introuvable !"); break; }
                System.out.println("Participation actuelle: " + p);
                System.out.print("Nouvelle présence (actuelle: " + p.getPresence() + "): ");
                p.setPresence(scanner.nextLine());
                System.out.print("Nouvelle justification (actuelle: " + p.getJustificationAbsence() + "): ");
                String j = scanner.nextLine();
                p.setJustificationAbsence(j.isBlank() ? null : j);
                service.update(p);
            }
            case 4 -> {
                System.out.print("\n🗑️ ID de la participation à supprimer: ");
                int id = Integer.parseInt(scanner.nextLine());
                service.delete(id);
            }
            case 5 -> {
                System.out.print("\n🔍 Mot clé à rechercher (présence/justification): ");
                String keyword = scanner.nextLine();
                service.search(keyword).forEach(System.out::println);
            }
            case 6 -> {
                System.out.println("Trier par: 1.Présence  2.Joueur  3.Entrainement");
                System.out.print("Choix: ");
                int tri = Integer.parseInt(scanner.nextLine());
                switch (tri) {
                    case 1 -> service.sortByPresence().forEach(System.out::println);
                    case 2 -> service.sortByJoueur().forEach(System.out::println);
                    case 3 -> service.sortByEntrainement().forEach(System.out::println);
                }
            }
        }
    }
}

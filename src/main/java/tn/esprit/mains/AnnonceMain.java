package tn.esprit.mains;

import tn.esprit.entities.Annonce;
import tn.esprit.entities.Commentaire;
import tn.esprit.services.AnnonceService;
import tn.esprit.services.CommentaireService;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class AnnonceMain {
    private static final Scanner scanner = new Scanner(System.in);
    private static AnnonceService annonceService;
    private static CommentaireService commentaireService;
    private static int entraineurId;

    public static void main(String[] args) {
        try {
            System.out.println("========== GESTION DES ANNONCES ET COMMENTAIRES ==========\n");

            // Initialiser les services
            annonceService = new AnnonceService();
            commentaireService = new CommentaireService();

            // Récupérer un utilisateur valide pour les tests
            entraineurId = getOrCreateUser();

            if (entraineurId == -1) {
                System.out.println("\n❌ IMPOSSIBLE DE CONTINUER - Aucun utilisateur trouvé.");
                System.out.println("Ajoutez au moins un utilisateur à la table 'user' et relancez le programme.\n");
                return;
            }

            System.out.println("✓ Utilisation de l'entraîneur ID: " + entraineurId + "\n");

            // Menu principal
            afficherMenu();

        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void afficherMenu() {
        while (true) {
            System.out.println("\n========== MENU PRINCIPAL ==========");
            System.out.println("1. 📝 Gérer les Annonces");
            System.out.println("2. 💬 Gérer les Commentaires");
            System.out.println("3. 🔍 Recherches");
            System.out.println("4. 📊 Statistiques");
            System.out.println("0. 🚪 Quitter");
            System.out.print("Votre choix: ");

            int choix = lireEntier();

            switch (choix) {
                case 1:
                    menuAnnonces();
                    break;
                case 2:
                    menuCommentaires();
                    break;
                case 3:
                    menuRecherches();
                    break;
                case 4:
                    afficherStatistiques();
                    break;
                case 0:
                    System.out.println("👋 Au revoir!");
                    return;
                default:
                    System.out.println("❌ Choix invalide. Veuillez réessayer.");
            }
        }
    }

    private static void menuAnnonces() {
        while (true) {
            System.out.println("\n========== GESTION DES ANNONCES ==========");
            System.out.println("1. ➕ Ajouter une annonce");
            System.out.println("2. 📋 Voir toutes les annonces");
            System.out.println("3. 🔍 Voir une annonce par ID");
            System.out.println("4. ✏️ Modifier une annonce");
            System.out.println("5. 🗑️ Supprimer une annonce");
            System.out.println("6. 👤 Voir les annonces d'un entraîneur");
            System.out.println("7. ✅ Voir les annonces actives");
            System.out.println("0. 🔙 Retour au menu principal");
            System.out.print("Votre choix: ");

            int choix = lireEntier();

            switch (choix) {
                case 1:
                    ajouterAnnonce();
                    break;
                case 2:
                    voirToutesAnnonces();
                    break;
                case 3:
                    voirAnnonceParId();
                    break;
                case 4:
                    modifierAnnonce();
                    break;
                case 5:
                    supprimerAnnonce();
                    break;
                case 6:
                    voirAnnoncesParEntraineur();
                    break;
                case 7:
                    voirAnnoncesActives();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("❌ Choix invalide.");
            }
        }
    }

    private static void menuCommentaires() {
        while (true) {
            System.out.println("\n========== GESTION DES COMMENTAIRES ==========");
            System.out.println("1. ➕ Ajouter un commentaire");
            System.out.println("2. 📋 Voir tous les commentaires");
            System.out.println("3. 🔍 Voir un commentaire par ID");
            System.out.println("4. ✏️ Modifier un commentaire");
            System.out.println("5. 🗑️ Supprimer un commentaire");
            System.out.println("6. 📢 Voir les commentaires d'une annonce");
            System.out.println("7. 👤 Voir les commentaires d'un joueur");
            System.out.println("0. 🔙 Retour au menu principal");
            System.out.print("Votre choix: ");

            int choix = lireEntier();

            switch (choix) {
                case 1:
                    ajouterCommentaire();
                    break;
                case 2:
                    voirTousCommentaires();
                    break;
                case 3:
                    voirCommentaireParId();
                    break;
                case 4:
                    modifierCommentaire();
                    break;
                case 5:
                    supprimerCommentaire();
                    break;
                case 6:
                    voirCommentairesParAnnonce();
                    break;
                case 7:
                    voirCommentairesParJoueur();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("❌ Choix invalide.");
            }
        }
    }

    private static void menuRecherches() {
        while (true) {
            System.out.println("\n========== RECHERCHES ==========");
            System.out.println("1. 🔍 Rechercher des annonces par titre");
            System.out.println("2. 📅 Rechercher des annonces par date");
            System.out.println("3. 🔍 Rechercher des annonces par titre ET date");
            System.out.println("4. 📝 Rechercher des annonces par poste");
            System.out.println("0. 🔙 Retour au menu principal");
            System.out.print("Votre choix: ");

            int choix = lireEntier();

            switch (choix) {
                case 1:
                    rechercherAnnoncesParTitre();
                    break;
                case 2:
                    rechercherAnnoncesParDate();
                    break;
                case 3:
                    rechercherAnnoncesParTitreEtDate();
                    break;
                case 4:
                    rechercherAnnoncesParPoste();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("❌ Choix invalide.");
            }
        }
    }

    // ============ MÉTHODES POUR LES ANNONCES ============

    private static void ajouterAnnonce() {
        System.out.println("\n--- AJOUT D'UNE ANNONCE ---");

        scanner.nextLine(); // Vider le buffer

        System.out.print("Titre: ");
        String titre = scanner.nextLine();

        System.out.print("Description: ");
        String description = scanner.nextLine();

        System.out.print("Poste recherché: ");
        String posteRecherche = scanner.nextLine();

        System.out.print("Niveau requis: ");
        String niveauRequis = scanner.nextLine();

        LocalDate datePublication = lireDate("Date de publication (YYYY-MM-DD): ");

        System.out.print("Statut (ACTIVE/CLOSED): ");
        String statut = scanner.nextLine().toUpperCase();
        if (!statut.equals("ACTIVE") && !statut.equals("CLOSED")) {
            statut = "ACTIVE"; // Valeur par défaut
        }

        try {
            Annonce annonce = new Annonce(titre, description, posteRecherche, niveauRequis, datePublication, statut, entraineurId);
            annonceService.add(annonce);
            System.out.println("✅ Annonce ajoutée avec succès!");
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    private static void voirToutesAnnonces() {
        try {
            List<Annonce> annonces = annonceService.getAll();
            System.out.println("\n--- TOUTES LES ANNONCES (" + annonces.size() + ") ---");
            if (annonces.isEmpty()) {
                System.out.println("Aucune annonce trouvée.");
            } else {
                for (Annonce a : annonces) {
                    System.out.println("ID: " + a.getId() + " | " + a.getTitre() + " | " + a.getPosteRecherche() + " | " + a.getDatePublication() + " | " + a.getStatut());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void voirAnnonceParId() {
        System.out.print("ID de l'annonce: ");
        int id = lireEntier();

        try {
            Annonce annonce = annonceService.getById(id);
            if (annonce != null) {
                System.out.println("\n--- DÉTAILS DE L'ANNONCE ---");
                System.out.println("ID: " + annonce.getId());
                System.out.println("Titre: " + annonce.getTitre());
                System.out.println("Description: " + annonce.getDescription());
                System.out.println("Poste: " + annonce.getPosteRecherche());
                System.out.println("Niveau: " + annonce.getNiveauRequis());
                System.out.println("Date: " + annonce.getDatePublication());
                System.out.println("Statut: " + annonce.getStatut());
                System.out.println("Entraîneur ID: " + annonce.getEntraineurId());
            } else {
                System.out.println("❌ Annonce non trouvée.");
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void modifierAnnonce() {
        System.out.print("ID de l'annonce à modifier: ");
        int id = lireEntier();

        try {
            Annonce annonce = annonceService.getById(id);
            if (annonce == null) {
                System.out.println("❌ Annonce non trouvée.");
                return;
            }

            scanner.nextLine(); // Vider le buffer

            System.out.println("Ancienne valeur: " + annonce.getTitre());
            System.out.print("Nouveau titre (ou Enter pour garder): ");
            String titre = scanner.nextLine();
            if (!titre.trim().isEmpty()) {
                annonce.setTitre(titre);
            }

            System.out.println("Ancienne valeur: " + annonce.getDescription());
            System.out.print("Nouvelle description (ou Enter pour garder): ");
            String description = scanner.nextLine();
            if (!description.trim().isEmpty()) {
                annonce.setDescription(description);
            }

            System.out.println("Ancienne valeur: " + annonce.getPosteRecherche());
            System.out.print("Nouveau poste (ou Enter pour garder): ");
            String poste = scanner.nextLine();
            if (!poste.trim().isEmpty()) {
                annonce.setPosteRecherche(poste);
            }

            System.out.println("Ancienne valeur: " + annonce.getNiveauRequis());
            System.out.print("Nouveau niveau (ou Enter pour garder): ");
            String niveau = scanner.nextLine();
            if (!niveau.trim().isEmpty()) {
                annonce.setNiveauRequis(niveau);
            }

            System.out.println("Ancienne valeur: " + annonce.getStatut());
            System.out.print("Nouveau statut (ACTIVE/CLOSED ou Enter pour garder): ");
            String statut = scanner.nextLine().toUpperCase();
            if (!statut.trim().isEmpty() && (statut.equals("ACTIVE") || statut.equals("CLOSED"))) {
                annonce.setStatut(statut);
            }

            annonceService.update(annonce);
            System.out.println("✅ Annonce modifiée avec succès!");

        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void supprimerAnnonce() {
        System.out.print("ID de l'annonce à supprimer: ");
        int id = lireEntier();

        System.out.print("Êtes-vous sûr de vouloir supprimer cette annonce? (oui/non): ");
        scanner.nextLine(); // Vider le buffer
        String confirmation = scanner.nextLine().toLowerCase();

        if (confirmation.equals("oui") || confirmation.equals("o") || confirmation.equals("yes") || confirmation.equals("y")) {
            try {
                annonceService.delete(id);
                System.out.println("✅ Annonce supprimée avec succès!");
            } catch (Exception e) {
                System.out.println("❌ Erreur: " + e.getMessage());
            }
        } else {
            System.out.println("Suppression annulée.");
        }
    }

    private static void voirAnnoncesParEntraineur() {
        System.out.print("ID de l'entraîneur: ");
        int idEntraineur = lireEntier();

        try {
            List<Annonce> annonces = annonceService.getAnnoncesByEntraineur(idEntraineur);
            System.out.println("\n--- ANNONCES DE L'ENTRAÎNEUR " + idEntraineur + " (" + annonces.size() + ") ---");
            if (annonces.isEmpty()) {
                System.out.println("Aucune annonce trouvée pour cet entraîneur.");
            } else {
                for (Annonce a : annonces) {
                    System.out.println("ID: " + a.getId() + " | " + a.getTitre() + " | " + a.getDatePublication());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void voirAnnoncesActives() {
        try {
            List<Annonce> annonces = annonceService.getAnnoncesActives();
            System.out.println("\n--- ANNONCES ACTIVES (" + annonces.size() + ") ---");
            if (annonces.isEmpty()) {
                System.out.println("Aucune annonce active.");
            } else {
                for (Annonce a : annonces) {
                    System.out.println("ID: " + a.getId() + " | " + a.getTitre() + " | " + a.getPosteRecherche());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    // ============ MÉTHODES POUR LES COMMENTAIRES ============

    private static void ajouterCommentaire() {
        System.out.println("\n--- AJOUT D'UN COMMENTAIRE ---");

        System.out.print("ID du joueur: ");
        int joueurId = lireEntier();

        System.out.print("ID de l'annonce: ");
        int annonceId = lireEntier();

        scanner.nextLine(); // Vider le buffer

        System.out.print("Contenu du commentaire: ");
        String contenu = scanner.nextLine();

        LocalDate dateCommentaire = lireDate("Date du commentaire (YYYY-MM-DD): ");

        System.out.print("Auteur anonyme: ");
        String auteurAnonyme = scanner.nextLine();

        System.out.print("Nombre de likes: ");
        int nbLikes = lireEntier();

        System.out.print("Statut de modération (APPROVED/PENDING/REJECTED): ");
        scanner.nextLine(); // Vider le buffer
        String moderationStatus = scanner.nextLine().toUpperCase();
        if (!moderationStatus.equals("APPROVED") && !moderationStatus.equals("PENDING") && !moderationStatus.equals("REJECTED")) {
            moderationStatus = "APPROVED"; // Valeur par défaut
        }

        System.out.print("Raison de modération (optionnel): ");
        String moderationReason = scanner.nextLine();
        if (moderationReason.trim().isEmpty()) {
            moderationReason = null;
        }

        try {
            Commentaire commentaire = new Commentaire(contenu, dateCommentaire, joueurId, annonceId, auteurAnonyme, nbLikes, moderationStatus, moderationReason);
            commentaireService.add(commentaire);
            System.out.println("✅ Commentaire ajouté avec succès!");
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    private static void voirTousCommentaires() {
        try {
            List<Commentaire> commentaires = commentaireService.getAll();
            System.out.println("\n--- TOUS LES COMMENTAIRES (" + commentaires.size() + ") ---");
            if (commentaires.isEmpty()) {
                System.out.println("Aucun commentaire trouvé.");
            } else {
                for (Commentaire c : commentaires) {
                    System.out.println("ID: " + c.getId() + " | [" + c.getAuteurAnonyme() + "] " + c.getContenu().substring(0, Math.min(50, c.getContenu().length())) + "...");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void voirCommentaireParId() {
        System.out.print("ID du commentaire: ");
        int id = lireEntier();

        try {
            Commentaire commentaire = commentaireService.getById(id);
            if (commentaire != null) {
                System.out.println("\n--- DÉTAILS DU COMMENTAIRE ---");
                System.out.println("ID: " + commentaire.getId());
                System.out.println("Contenu: " + commentaire.getContenu());
                System.out.println("Date: " + commentaire.getDateCommentaire());
                System.out.println("Auteur: " + commentaire.getAuteurAnonyme());
                System.out.println("Likes: " + commentaire.getNbLikes());
                System.out.println("Joueur ID: " + commentaire.getJoueurId());
                System.out.println("Annonce ID: " + commentaire.getAnnonceId());
                System.out.println("Statut modération: " + commentaire.getModerationStatus());
                System.out.println("Raison modération: " + commentaire.getModerationReason());
            } else {
                System.out.println("❌ Commentaire non trouvé.");
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void modifierCommentaire() {
        System.out.print("ID du commentaire à modifier: ");
        int id = lireEntier();

        try {
            Commentaire commentaire = commentaireService.getById(id);
            if (commentaire == null) {
                System.out.println("❌ Commentaire non trouvé.");
                return;
            }

            scanner.nextLine(); // Vider le buffer

            System.out.println("Ancien contenu: " + commentaire.getContenu());
            System.out.print("Nouveau contenu (ou Enter pour garder): ");
            String contenu = scanner.nextLine();
            if (!contenu.trim().isEmpty()) {
                commentaire.setContenu(contenu);
            }

            System.out.println("Ancien auteur: " + commentaire.getAuteurAnonyme());
            System.out.print("Nouvel auteur (ou Enter pour garder): ");
            String auteur = scanner.nextLine();
            if (!auteur.trim().isEmpty()) {
                commentaire.setAuteurAnonyme(auteur);
            }

            System.out.println("Anciens likes: " + commentaire.getNbLikes());
            System.out.print("Nouveaux likes: ");
            String likesStr = scanner.nextLine();
            if (!likesStr.trim().isEmpty()) {
                try {
                    int likes = Integer.parseInt(likesStr);
                    commentaire.setNbLikes(likes);
                } catch (NumberFormatException e) {
                    System.out.println("Nombre invalide, likes non modifiés.");
                }
            }

            System.out.println("Ancien statut: " + commentaire.getModerationStatus());
            System.out.print("Nouveau statut (APPROVED/PENDING/REJECTED ou Enter pour garder): ");
            String statut = scanner.nextLine().toUpperCase();
            if (!statut.trim().isEmpty() && (statut.equals("APPROVED") || statut.equals("PENDING") || statut.equals("REJECTED"))) {
                commentaire.setModerationStatus(statut);
            }

            System.out.println("Ancienne raison: " + commentaire.getModerationReason());
            System.out.print("Nouvelle raison (ou Enter pour garder): ");
            String raison = scanner.nextLine();
            if (!raison.trim().isEmpty()) {
                commentaire.setModerationReason(raison);
            }

            commentaireService.update(commentaire);
            System.out.println("✅ Commentaire modifié avec succès!");

        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void supprimerCommentaire() {
        System.out.print("ID du commentaire à supprimer: ");
        int id = lireEntier();

        System.out.print("Êtes-vous sûr de vouloir supprimer ce commentaire? (oui/non): ");
        scanner.nextLine(); // Vider le buffer
        String confirmation = scanner.nextLine().toLowerCase();

        if (confirmation.equals("oui") || confirmation.equals("o") || confirmation.equals("yes") || confirmation.equals("y")) {
            try {
                commentaireService.delete(id);
                System.out.println("✅ Commentaire supprimé avec succès!");
            } catch (Exception e) {
                System.out.println("❌ Erreur: " + e.getMessage());
            }
        } else {
            System.out.println("Suppression annulée.");
        }
    }

    private static void voirCommentairesParAnnonce() {
        System.out.print("ID de l'annonce: ");
        int idAnnonce = lireEntier();

        try {
            List<Commentaire> commentaires = commentaireService.getCommentairesByAnnonce(idAnnonce);
            System.out.println("\n--- COMMENTAIRES DE L'ANNONCE " + idAnnonce + " (" + commentaires.size() + ") ---");
            if (commentaires.isEmpty()) {
                System.out.println("Aucun commentaire trouvé pour cette annonce.");
            } else {
                for (Commentaire c : commentaires) {
                    System.out.println("[" + c.getAuteurAnonyme() + "] " + c.getContenu().substring(0, Math.min(50, c.getContenu().length())) + "... (Likes: " + c.getNbLikes() + ")");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void voirCommentairesParJoueur() {
        System.out.print("ID du joueur: ");
        int idJoueur = lireEntier();

        try {
            List<Commentaire> commentaires = commentaireService.getCommentairesByJoueur(idJoueur);
            System.out.println("\n--- COMMENTAIRES DU JOUEUR " + idJoueur + " (" + commentaires.size() + ") ---");
            if (commentaires.isEmpty()) {
                System.out.println("Aucun commentaire trouvé pour ce joueur.");
            } else {
                for (Commentaire c : commentaires) {
                    System.out.println("Annonce " + c.getAnnonceId() + ": " + c.getContenu().substring(0, Math.min(50, c.getContenu().length())) + "...");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    // ============ MÉTHODES DE RECHERCHE ============

    private static void rechercherAnnoncesParTitre() {
        scanner.nextLine(); // Vider le buffer
        System.out.print("Mot-clé dans le titre: ");
        String titre = scanner.nextLine();

        try {
            List<Annonce> annonces = annonceService.searchByTitre(titre);
            System.out.println("\n--- RÉSULTATS DE RECHERCHE PAR TITRE '" + titre + "' (" + annonces.size() + ") ---");
            if (annonces.isEmpty()) {
                System.out.println("Aucune annonce trouvée.");
            } else {
                for (Annonce a : annonces) {
                    System.out.println("ID: " + a.getId() + " | " + a.getTitre() + " | " + a.getDatePublication());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void rechercherAnnoncesParDate() {
        LocalDate date = lireDate("Date de publication (YYYY-MM-DD): ");

        try {
            List<Annonce> annonces = annonceService.searchByDatePublication(date);
            System.out.println("\n--- ANNONCES DU " + date + " (" + annonces.size() + ") ---");
            if (annonces.isEmpty()) {
                System.out.println("Aucune annonce trouvée pour cette date.");
            } else {
                for (Annonce a : annonces) {
                    System.out.println("ID: " + a.getId() + " | " + a.getTitre() + " | " + a.getPosteRecherche());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void rechercherAnnoncesParTitreEtDate() {
        scanner.nextLine(); // Vider le buffer
        System.out.print("Mot-clé dans le titre: ");
        String titre = scanner.nextLine();

        LocalDate date = lireDate("Date de publication (YYYY-MM-DD): ");

        try {
            List<Annonce> annonces = annonceService.searchByTitreAndDate(titre, date);
            System.out.println("\n--- RÉSULTATS DE RECHERCHE '" + titre + "' DU " + date + " (" + annonces.size() + ") ---");
            if (annonces.isEmpty()) {
                System.out.println("Aucune annonce trouvée.");
            } else {
                for (Annonce a : annonces) {
                    System.out.println("ID: " + a.getId() + " | " + a.getTitre() + " | " + a.getPosteRecherche());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void rechercherAnnoncesParPoste() {
        scanner.nextLine(); // Vider le buffer
        System.out.print("Poste recherché: ");
        String poste = scanner.nextLine();

        try {
            List<Annonce> annonces = annonceService.getAnnoncesByPoste(poste);
            System.out.println("\n--- ANNONCES POUR LE POSTE '" + poste + "' (" + annonces.size() + ") ---");
            if (annonces.isEmpty()) {
                System.out.println("Aucune annonce trouvée pour ce poste.");
            } else {
                for (Annonce a : annonces) {
                    System.out.println("ID: " + a.getId() + " | " + a.getTitre() + " | " + a.getNiveauRequis() + " | " + a.getDatePublication());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    private static void afficherStatistiques() {
        try {
            List<Annonce> toutesAnnonces = annonceService.getAll();
            List<Annonce> annoncesActives = annonceService.getAnnoncesActives();
            List<Commentaire> tousCommentaires = commentaireService.getAll();

            System.out.println("\n--- STATISTIQUES ---");
            System.out.println("📊 Nombre total d'annonces: " + toutesAnnonces.size());
            System.out.println("✅ Annonces actives: " + annoncesActives.size());
            System.out.println("❌ Annonces fermées: " + (toutesAnnonces.size() - annoncesActives.size()));
            System.out.println("💬 Nombre total de commentaires: " + tousCommentaires.size());

            if (!toutesAnnonces.isEmpty()) {
                System.out.println("📅 Dernière annonce: " + toutesAnnonces.get(toutesAnnonces.size() - 1).getDatePublication());
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    // ============ MÉTHODES UTILITAIRES ============

    private static int lireEntier() {
        while (true) {
            try {
                int valeur = scanner.nextInt();
                return valeur;
            } catch (Exception e) {
                System.out.print("❌ Veuillez entrer un nombre valide: ");
                scanner.nextLine(); // Vider le buffer
            }
        }
    }

    private static LocalDate lireDate(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            System.out.print(message);
            String dateStr = scanner.nextLine();
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("❌ Format de date invalide. Utilisez le format YYYY-MM-DD (ex: 2026-04-05)");
            }
        }
    }

    /**
     * Récupère un utilisateur existant ou en crée un pour les tests
     */
    private static int getOrCreateUser() {
        try {
            Connection connection = MyConnection.getInstance().getConnection();

            // Essayer de récupérer le premier utilisateur existant
            String selectQuery = "SELECT id FROM user LIMIT 1";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(selectQuery);

            if (resultSet.next()) {
                int userId = resultSet.getInt("id");
                System.out.println("✓ Utilisateur existant trouvé avec l'ID: " + userId);
                return userId;
            }

            System.out.println("⚠ Aucun utilisateur trouvé dans la base de données.");
            System.out.println("   Veuillez créer manuellement un utilisateur avec les champs requis:");
            System.out.println("   - nom, prenom, email, mot_de_passe, role");
            System.out.println("   Pour continuer, retour à null (les annonces n'auront pas d'entraîneur)");
            return -1;

        } catch (Exception e) {
            System.out.println("⚠ Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
            return -1;
        }
    }
}

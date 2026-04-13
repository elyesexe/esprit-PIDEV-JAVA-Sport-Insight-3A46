package tn.esprit.services;

import tn.esprit.entities.Annonce;
import tn.esprit.entities.Commentaire;
import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Joueur;
import tn.esprit.entities.Order;
import tn.esprit.entities.Participation;
import tn.esprit.entities.Product;
import tn.esprit.entities.Sponsor;
import tn.esprit.entities.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

abstract class AbstractServiceTestSupport {

    protected User createUser(UserService service, String prefix, String label, String role) throws SQLException {
        User user = new User();
        user.setEmail(emailFor(prefix, label));
        user.setRoles(role);
        user.setPassword("Password@" + label);
        user.setNom(prefix + label);
        user.setPrenom("Prenom_" + label);
        user.setTelephone(phoneFor(prefix + label, 10000000));
        user.setDateNaissance(LocalDate.of(1990, 1, 1).plusDays(Math.floorMod(label.hashCode(), 3650)));
        user.setPhoto("photo-" + slug(label) + ".png");
        user.setStatut("ACTIVE");
        user.setDateInscription(LocalDateTime.of(2026, 1, 1, 9, 0));
        user.setCvName("cv-" + slug(label) + ".pdf");
        user.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        service.add(user);
        return user.getId() != null ? user : findUserByEmail(service, user.getEmail());
    }

    protected Equipe createEquipe(EquipeService service, String prefix, String label, String competitionCode) throws SQLException {
        String nom = prefix + label;
        Equipe equipe = new Equipe(
                nom,
                "Coach_" + label,
                "Adresse_" + label,
                phoneFor(nom, 20000000),
                emailFor(prefix, label),
                "logo-" + slug(label) + ".png"
        );
        equipe.setCompetitionCode(competitionCode);
        service.add(equipe);
        return findEquipeByName(service, nom);
    }

    protected Joueur createJoueur(JoueurService service, String prefix, String label, Integer equipeId) throws SQLException {
        String nom = prefix + label;
        String prenom = "Prenom_" + label;
        Joueur joueur = new Joueur(
                nom,
                prenom,
                LocalDate.of(1998, 1, 1).plusDays(Math.floorMod(label.hashCode(), 4000)),
                1 + Math.floorMod(label.hashCode(), 98),
                "joueur-" + slug(label) + ".png",
                equipeId
        );
        service.add(joueur);
        return findJoueurByIdentity(service, nom, prenom);
    }

    protected Entrainement createEntrainement(EntrainementService service, String prefix, String label, Integer entraineurId) throws SQLException {
        Entrainement entrainement = new Entrainement(
                LocalDate.of(2026, 5, 1).plusDays(Math.floorMod(label.hashCode(), 20)),
                LocalTime.of(8 + Math.floorMod(label.hashCode(), 8), 0),
                LocalTime.of(10 + Math.floorMod(label.hashCode(), 8), 0),
                "Type_" + label,
                prefix + label,
                "Lieu_" + label,
                entraineurId
        );
        service.add(entrainement);
        return entrainement.getId() != null ? entrainement : findEntrainementByObjectif(service, prefix + label);
    }

    protected Product createProduct(ProductService service, String prefix, String label) throws SQLException {
        String name = prefix + label;
        Product product = new Product(
                name,
                "Categorie_" + label,
                new BigDecimal("100.00").add(BigDecimal.valueOf(Math.floorMod(label.hashCode(), 100))),
                5 + Math.floorMod(label.hashCode(), 20),
                "L",
                "Brand_" + label,
                "product-" + slug(label) + ".png"
        );
        service.add(product);
        return findProductByName(service, name);
    }

    protected Sponsor createSponsor(SponsorService service, String prefix, String label) throws SQLException {
        String nom = prefix + label;
        Sponsor sponsor = new Sponsor(
                nom,
                emailFor(prefix, label),
                phoneFor(nom, 30000000),
                1000.0 + Math.floorMod(label.hashCode(), 5000),
                "logo-" + slug(label) + ".png",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                "Adresse_" + label
        );
        service.add(sponsor);
        return findSponsorByNom(service, nom);
    }

    protected Annonce createAnnonce(AnnonceService service, String prefix, String label, Integer entraineurId) throws SQLException {
        String titre = prefix + label;
        Annonce annonce = new Annonce(
                titre,
                "Description_" + label,
                "Poste_" + label,
                "Niveau_" + label,
                LocalDate.of(2026, 6, 1).plusDays(Math.floorMod(label.hashCode(), 15)),
                "ACTIVE",
                entraineurId,
                true,
                false
        );
        service.add(annonce);
        return findAnnonceByTitre(service, titre);
    }

    protected Commentaire createCommentaire(
            CommentaireService service,
            String prefix,
            String label,
            Integer joueurId,
            Integer annonceId
    ) throws SQLException {
        String contenu = prefix + label;
        Commentaire commentaire = new Commentaire(
                contenu,
                LocalDate.of(2026, 6, 10).plusDays(Math.floorMod(label.hashCode(), 10)),
                joueurId,
                annonceId,
                "Auteur_" + label,
                Math.floorMod(label.hashCode(), 10),
                "PENDING",
                "Reason_" + label
        );
        service.add(commentaire);
        return findCommentaireByContenu(service, contenu);
    }

    protected ContratSponsor createContrat(
            ContratSponsorService service,
            String prefix,
            String label,
            Integer sponsorId,
            Integer equipeId
    ) throws SQLException {
        String description = prefix + label;
        ContratSponsor contrat = new ContratSponsor(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31),
                5000.0 + Math.floorMod(label.hashCode(), 5000),
                description,
                "ACTIVE",
                false,
                "PENDING",
                sponsorId,
                equipeId
        );
        service.add(contrat);
        return findContratByDescription(service, description);
    }

    protected Order createOrder(
            OrderService service,
            String prefix,
            String label,
            Integer productId,
            Integer entraineurId
    ) throws SQLException {
        Order order = new Order(
                1 + Math.floorMod(label.hashCode(), 5),
                LocalDate.of(2026, 8, 1).plusDays(Math.floorMod(label.hashCode(), 10)),
                "PENDING",
                "CARD",
                "UNPAID",
                "M",
                emailFor(prefix, label),
                phoneFor(prefix + label, 40000000),
                "Shipping_" + label,
                "Billing_" + label,
                new BigDecimal("250.00"),
                productId,
                entraineurId
        );
        service.add(order);
        return findOrderByContactEmail(service, order.getContactEmail());
    }

    protected Evaluation createEvaluation(
            EvaluationService service,
            String prefix,
            String label,
            Integer entrainementId,
            Integer joueurId
    ) throws SQLException {
        Evaluation evaluation = new Evaluation(
                10.0 + Math.floorMod(label.hashCode(), 10),
                11.0 + Math.floorMod(label.hashCode(), 9),
                12.0 + Math.floorMod(label.hashCode(), 8),
                prefix + label,
                entrainementId,
                joueurId
        );
        service.add(evaluation);
        return evaluation.getId() != null ? service.getById(evaluation.getId()) : findEvaluationByComment(service, prefix + label);
    }

    protected Participation createParticipation(
            ParticipationService service,
            String prefix,
            String label,
            Integer entrainementId,
            Integer joueurId
    ) throws SQLException {
        Participation participation = new Participation(
                "PRESENT",
                prefix + label,
                entrainementId,
                joueurId
        );
        service.add(participation);
        return participation.getId() != null ? service.getById(participation.getId()) : findParticipationByJustification(service, prefix + label);
    }

    protected User findUserByEmail(UserService service, String email) throws SQLException {
        return service.findByEmail(email);
    }

    protected Equipe findEquipeByName(EquipeService service, String nom) throws SQLException {
        return service.getAll().stream()
                .filter(equipe -> nom.equals(equipe.getNom()))
                .findFirst()
                .orElse(null);
    }

    protected Joueur findJoueurByIdentity(JoueurService service, String nom, String prenom) throws SQLException {
        return service.getAll().stream()
                .filter(joueur -> nom.equals(joueur.getNom()) && prenom.equals(joueur.getPrenom()))
                .findFirst()
                .orElse(null);
    }

    protected Entrainement findEntrainementByObjectif(EntrainementService service, String objectif) throws SQLException {
        return service.getAll().stream()
                .filter(entrainement -> objectif.equals(entrainement.getObjectif()))
                .findFirst()
                .orElse(null);
    }

    protected Product findProductByName(ProductService service, String name) throws SQLException {
        return service.getAll().stream()
                .filter(product -> name.equals(product.getName()))
                .findFirst()
                .orElse(null);
    }

    protected Sponsor findSponsorByNom(SponsorService service, String nom) throws SQLException {
        return service.getAll().stream()
                .filter(sponsor -> nom.equals(sponsor.getNom()))
                .findFirst()
                .orElse(null);
    }

    protected Annonce findAnnonceByTitre(AnnonceService service, String titre) throws SQLException {
        return service.getAll().stream()
                .filter(annonce -> titre.equals(annonce.getTitre()))
                .findFirst()
                .orElse(null);
    }

    protected Commentaire findCommentaireByContenu(CommentaireService service, String contenu) throws SQLException {
        return service.getAll().stream()
                .filter(commentaire -> contenu.equals(commentaire.getContenu()))
                .findFirst()
                .orElse(null);
    }

    protected ContratSponsor findContratByDescription(ContratSponsorService service, String description) throws SQLException {
        return service.getAll().stream()
                .filter(contrat -> description.equals(contrat.getDescription()))
                .findFirst()
                .orElse(null);
    }

    protected Order findOrderByContactEmail(OrderService service, String contactEmail) throws SQLException {
        return service.getAll().stream()
                .filter(order -> contactEmail.equals(order.getContactEmail()))
                .findFirst()
                .orElse(null);
    }

    protected Evaluation findEvaluationByComment(EvaluationService service, String commentaire) throws SQLException {
        return service.getAll().stream()
                .filter(evaluation -> commentaire.equals(evaluation.getCommentaire()))
                .findFirst()
                .orElse(null);
    }

    protected Participation findParticipationByJustification(ParticipationService service, String justification) throws SQLException {
        return service.getAll().stream()
                .filter(participation -> justification.equals(participation.getJustificationAbsence()))
                .findFirst()
                .orElse(null);
    }

    protected String emailFor(String prefix, String label) {
        return (prefix + label).toLowerCase(Locale.ROOT) + "@test.com";
    }

    protected String phoneFor(String seed, int base) {
        return String.valueOf(base + Math.floorMod(seed.hashCode(), 10000000));
    }

    protected String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }
}

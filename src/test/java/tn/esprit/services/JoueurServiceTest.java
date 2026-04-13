package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JoueurServiceTest {

    private static final String TEST_JOUEUR_PREFIX = "JUNIT_JOUEUR_" + System.currentTimeMillis() + "_";
    private static final String TEST_EQUIPE_PREFIX = TEST_JOUEUR_PREFIX + "TEAM_";

    private static JoueurService joueurService;
    private static EquipeService equipeService;

    @BeforeAll
    static void setup() throws SQLException {
        joueurService = new JoueurService();
        equipeService = new EquipeService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        joueurService = new JoueurService();
        equipeService = new EquipeService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Joueur joueur : joueurService.getAll()) {
            if (isTestJoueur(joueur)) {
                joueurService.delete(joueur.getId());
            }
        }

        for (Equipe equipe : equipeService.getAll()) {
            if (isTestEquipe(equipe)) {
                equipeService.delete(equipe.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterJoueur() throws SQLException {
        Equipe equipe = createTeam("AJOUT", "PL");
        String nom = testNom("AJOUT");
        String prenom = testPrenom("AJOUT");

        Joueur joueur = buildJoueur(
                nom,
                prenom,
                LocalDate.of(2001, 5, 12),
                9,
                "joueur-ajout.png",
                equipe.getId()
        );

        joueurService.add(joueur);

        List<Joueur> joueurs = joueurService.getAll();
        assertFalse(joueurs.isEmpty());

        Joueur joueurAjoute = findByIdentity(nom, prenom);
        assertNotNull(joueurAjoute);
        assertEquals(LocalDate.of(2001, 5, 12), joueurAjoute.getDateNaissance());
        assertEquals(9, joueurAjoute.getNumero());
        assertEquals("joueur-ajout.png", joueurAjoute.getImage());
        assertEquals(equipe.getId(), joueurAjoute.getEquipeId());
        assertTrue(joueurs.stream().anyMatch(item -> nom.equals(item.getNom()) && prenom.equals(item.getPrenom())));
    }

    @Test
    @Order(2)
    void testModifierJoueur() throws SQLException {
        Equipe equipeInitiale = createTeam("MODIFIER_INIT", "PL");
        String nomInitial = testNom("MODIFIER");
        String prenomInitial = testPrenom("MODIFIER");

        joueurService.add(buildJoueur(
                nomInitial,
                prenomInitial,
                LocalDate.of(1999, 2, 10),
                7,
                "joueur-initial.png",
                equipeInitiale.getId()
        ));

        Joueur joueur = findByIdentity(nomInitial, prenomInitial);
        assertNotNull(joueur);

        Equipe equipeModifiee = createTeam("MODIFIER_NEW", "PD");
        joueur.setNom(testNom("MODIFIE"));
        joueur.setPrenom(testPrenom("MODIFIE"));
        joueur.setDateNaissance(LocalDate.of(1998, 8, 18));
        joueur.setNumero(10);
        joueur.setImage("joueur-modifie.png");
        joueur.setEquipeId(equipeModifiee.getId());

        joueurService.update(joueur);

        Joueur joueurModifie = joueurService.getById(joueur.getId());
        assertNotNull(joueurModifie);
        assertEquals(testNom("MODIFIE"), joueurModifie.getNom());
        assertEquals(testPrenom("MODIFIE"), joueurModifie.getPrenom());
        assertEquals(LocalDate.of(1998, 8, 18), joueurModifie.getDateNaissance());
        assertEquals(10, joueurModifie.getNumero());
        assertEquals("joueur-modifie.png", joueurModifie.getImage());
        assertEquals(equipeModifiee.getId(), joueurModifie.getEquipeId());
    }

    @Test
    @Order(3)
    void testSupprimerJoueur() throws SQLException {
        Equipe equipe = createTeam("SUPPRIMER", "SA");
        String nom = testNom("SUPPRIMER");
        String prenom = testPrenom("SUPPRIMER");

        joueurService.add(buildJoueur(
                nom,
                prenom,
                LocalDate.of(2003, 11, 5),
                15,
                "joueur-suppression.png",
                equipe.getId()
        ));

        Joueur joueur = findByIdentity(nom, prenom);
        assertNotNull(joueur);

        joueurService.delete(joueur.getId());

        Joueur joueurSupprime = joueurService.getById(joueur.getId());
        boolean existeEncore = joueurService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), joueur.getId()));

        assertNull(joueurSupprime);
        assertFalse(existeEncore);
    }

    private Joueur buildJoueur(
            String nom,
            String prenom,
            LocalDate dateNaissance,
            int numero,
            String image,
            Integer equipeId
    ) {
        return new Joueur(nom, prenom, dateNaissance, numero, image, equipeId);
    }

    private Equipe createTeam(String label, String competitionCode) throws SQLException {
        String nomEquipe = testTeamName(label);
        equipeService.add(buildEquipe(nomEquipe, "Coach " + label, competitionCode));

        Equipe equipe = findEquipeByName(nomEquipe);
        assertNotNull(equipe);
        return equipe;
    }

    private Equipe buildEquipe(String nom, String coach, String competitionCode) {
        Equipe equipe = new Equipe(
                nom,
                coach,
                "Adresse " + extractTeamLabel(nom),
                extractTeamPhone(nom),
                extractTeamEmail(nom),
                "logo-" + extractTeamLabel(nom).toLowerCase() + ".png"
        );
        equipe.setCompetitionCode(competitionCode);
        return equipe;
    }

    private Joueur findByIdentity(String nom, String prenom) throws SQLException {
        return joueurService.getAll().stream()
                .filter(joueur -> nom.equals(joueur.getNom()) && prenom.equals(joueur.getPrenom()))
                .findFirst()
                .orElse(null);
    }

    private Equipe findEquipeByName(String nom) throws SQLException {
        return equipeService.getAll().stream()
                .filter(equipe -> nom.equals(equipe.getNom()))
                .findFirst()
                .orElse(null);
    }

    private boolean isTestJoueur(Joueur joueur) {
        return joueur.getNom() != null && joueur.getNom().startsWith(TEST_JOUEUR_PREFIX);
    }

    private boolean isTestEquipe(Equipe equipe) {
        return equipe.getNom() != null && equipe.getNom().startsWith(TEST_EQUIPE_PREFIX);
    }

    private String testNom(String label) {
        return TEST_JOUEUR_PREFIX + label;
    }

    private String testPrenom(String label) {
        return "PRENOM_" + label;
    }

    private String testTeamName(String label) {
        return TEST_EQUIPE_PREFIX + label;
    }

    private String extractTeamLabel(String nom) {
        return nom.substring(TEST_EQUIPE_PREFIX.length());
    }

    private String extractTeamPhone(String nom) {
        return String.valueOf(71000000 + Math.abs(extractTeamLabel(nom).hashCode() % 10000000));
    }

    private String extractTeamEmail(String nom) {
        return extractTeamLabel(nom).toLowerCase() + "@test.com";
    }
}

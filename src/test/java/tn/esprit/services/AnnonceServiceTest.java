package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnnonceServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_ANNONCE_" + System.currentTimeMillis() + "_";
    private static final String USER_PREFIX = TEST_PREFIX + "USER_";

    private static AnnonceService annonceService;
    private static UserService userService;

    @BeforeAll
    static void setup() throws SQLException {
        annonceService = new AnnonceService();
        userService = new UserService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        annonceService = new AnnonceService();
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Annonce annonce : annonceService.getAll()) {
            if (annonce.getTitre() != null && annonce.getTitre().startsWith(TEST_PREFIX)) {
                annonceService.delete(annonce.getId());
            }
        }

        for (User user : userService.getAll()) {
            if (user.getEmail() != null && user.getEmail().startsWith(USER_PREFIX.toLowerCase(Locale.ROOT))) {
                userService.delete(user.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterAnnonce() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "AJOUT", UserRoles.ROLE_ENTRAINEUR);
        String titre = TEST_PREFIX + "AJOUT";

        Annonce annonce = new Annonce(
                titre,
                "Description ajout",
                "Attaquant",
                "Intermediaire",
                LocalDate.of(2026, 6, 1),
                "ACTIVE",
                coach.getId(),
                true,
                true
        );

        annonceService.add(annonce);

        Annonce annonceAjoutee = findAnnonceByTitre(annonceService, titre);
        assertNotNull(annonceAjoutee);
        assertEquals("Description ajout", annonceAjoutee.getDescription());
        assertEquals("Attaquant", annonceAjoutee.getPosteRecherche());
        assertEquals("Intermediaire", annonceAjoutee.getNiveauRequis());
        assertEquals(LocalDate.of(2026, 6, 1), annonceAjoutee.getDatePublication());
        assertEquals("ACTIVE", annonceAjoutee.getStatut());
        assertEquals(coach.getId(), annonceAjoutee.getEntraineurId());
        assertTrue(Boolean.TRUE.equals(annonceAjoutee.getCommentsEnabled()));
        assertTrue(Boolean.TRUE.equals(annonceAjoutee.getUrgent()));
    }

    @Test
    @Order(2)
    void testModifierAnnonce() throws SQLException {
        User coachInitial = createUser(userService, USER_PREFIX, "MODIFIER_INIT", UserRoles.ROLE_ENTRAINEUR);
        User coachModifie = createUser(userService, USER_PREFIX, "MODIFIER_NEW", UserRoles.ROLE_ENTRAINEUR);
        Annonce annonce = createAnnonce(annonceService, TEST_PREFIX, "MODIFIER", coachInitial.getId());
        assertNotNull(annonce);

        annonce.setTitre(TEST_PREFIX + "MODIFIE");
        annonce.setDescription("Description modifiee");
        annonce.setPosteRecherche("Milieu");
        annonce.setNiveauRequis("Avance");
        annonce.setDatePublication(LocalDate.of(2026, 6, 2));
        annonce.setStatut("ARCHIVED");
        annonce.setEntraineurId(coachModifie.getId());
        annonce.setCommentsEnabled(false);
        annonce.setUrgent(true);

        annonceService.update(annonce);

        Annonce annonceModifiee = annonceService.getById(annonce.getId());
        assertNotNull(annonceModifiee);
        assertEquals(TEST_PREFIX + "MODIFIE", annonceModifiee.getTitre());
        assertEquals("Description modifiee", annonceModifiee.getDescription());
        assertEquals("Milieu", annonceModifiee.getPosteRecherche());
        assertEquals("Avance", annonceModifiee.getNiveauRequis());
        assertEquals(LocalDate.of(2026, 6, 2), annonceModifiee.getDatePublication());
        assertEquals("ARCHIVED", annonceModifiee.getStatut());
        assertEquals(coachModifie.getId(), annonceModifiee.getEntraineurId());
        assertFalse(Boolean.TRUE.equals(annonceModifiee.getCommentsEnabled()));
        assertTrue(Boolean.TRUE.equals(annonceModifiee.getUrgent()));
    }

    @Test
    @Order(3)
    void testSupprimerAnnonce() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "SUPPRIMER", UserRoles.ROLE_ENTRAINEUR);
        Annonce annonce = createAnnonce(annonceService, TEST_PREFIX, "SUPPRIMER", coach.getId());
        assertNotNull(annonce);

        annonceService.delete(annonce.getId());

        Annonce annonceSupprimee = annonceService.getById(annonce.getId());
        boolean existeEncore = annonceService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), annonce.getId()));

        assertNull(annonceSupprimee);
        assertFalse(existeEncore);
    }
}

package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EntrainementServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_ENTRAINEMENT_" + System.currentTimeMillis() + "_";
    private static final String USER_PREFIX = TEST_PREFIX + "USER_";

    private static EntrainementService entrainementService;
    private static UserService userService;

    @BeforeAll
    static void setup() throws SQLException {
        entrainementService = new EntrainementService();
        userService = new UserService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        entrainementService = new EntrainementService();
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Entrainement entrainement : entrainementService.getAll()) {
            if (entrainement.getObjectif() != null && entrainement.getObjectif().startsWith(TEST_PREFIX)) {
                entrainementService.delete(entrainement.getId());
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
    void testAjouterEntrainement() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "AJOUT", UserRoles.ROLE_ENTRAINEUR);

        Entrainement entrainement = new Entrainement(
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                "Technique",
                TEST_PREFIX + "AJOUT",
                "Terrain A",
                coach.getId()
        );

        entrainementService.add(entrainement);

        Entrainement entrainementAjoute = findEntrainementByObjectif(entrainementService, TEST_PREFIX + "AJOUT");
        assertNotNull(entrainementAjoute);
        assertEquals(LocalDate.of(2026, 5, 10), entrainementAjoute.getDateEntrainement());
        assertEquals(LocalTime.of(9, 0), entrainementAjoute.getHeureDebut());
        assertEquals(LocalTime.of(11, 0), entrainementAjoute.getHeureFin());
        assertEquals("Technique", entrainementAjoute.getType());
        assertEquals("Terrain A", entrainementAjoute.getLieu());
        assertEquals(coach.getId(), entrainementAjoute.getEntraineurId());
    }

    @Test
    @Order(2)
    void testModifierEntrainement() throws SQLException {
        User coachInitial = createUser(userService, USER_PREFIX, "MODIFIER_INIT", UserRoles.ROLE_ENTRAINEUR);
        User coachModifie = createUser(userService, USER_PREFIX, "MODIFIER_NEW", UserRoles.ROLE_ENTRAINEUR);
        Entrainement entrainement = createEntrainement(entrainementService, TEST_PREFIX, "MODIFIER", coachInitial.getId());
        assertNotNull(entrainement);

        entrainement.setDateEntrainement(LocalDate.of(2026, 5, 11));
        entrainement.setHeureDebut(LocalTime.of(14, 0));
        entrainement.setHeureFin(LocalTime.of(16, 0));
        entrainement.setType("Tactique");
        entrainement.setObjectif(TEST_PREFIX + "MODIFIE");
        entrainement.setLieu("Terrain B");
        entrainement.setEntraineurId(coachModifie.getId());

        entrainementService.update(entrainement);

        Entrainement entrainementModifie = entrainementService.getById(entrainement.getId());
        assertNotNull(entrainementModifie);
        assertEquals(LocalDate.of(2026, 5, 11), entrainementModifie.getDateEntrainement());
        assertEquals(LocalTime.of(14, 0), entrainementModifie.getHeureDebut());
        assertEquals(LocalTime.of(16, 0), entrainementModifie.getHeureFin());
        assertEquals("Tactique", entrainementModifie.getType());
        assertEquals(TEST_PREFIX + "MODIFIE", entrainementModifie.getObjectif());
        assertEquals("Terrain B", entrainementModifie.getLieu());
        assertEquals(coachModifie.getId(), entrainementModifie.getEntraineurId());
    }

    @Test
    @Order(3)
    void testSupprimerEntrainement() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "SUPPRIMER", UserRoles.ROLE_ENTRAINEUR);
        Entrainement entrainement = createEntrainement(entrainementService, TEST_PREFIX, "SUPPRIMER", coach.getId());
        assertNotNull(entrainement);

        entrainementService.delete(entrainement.getId());

        Entrainement entrainementSupprime = entrainementService.getById(entrainement.getId());
        boolean existeEncore = entrainementService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), entrainement.getId()));

        assertNull(entrainementSupprime);
        assertFalse(existeEncore);
    }
}

package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Participation;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParticipationServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_PARTICIPATION_" + System.currentTimeMillis() + "_";
    private static final String ENTRAINEMENT_PREFIX = TEST_PREFIX + "ENTRAINEMENT_";
    private static final String USER_PREFIX = TEST_PREFIX + "USER_";

    private static ParticipationService participationService;
    private static EntrainementService entrainementService;
    private static UserService userService;

    @BeforeAll
    static void setup() throws SQLException {
        participationService = new ParticipationService();
        entrainementService = new EntrainementService();
        userService = new UserService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        participationService = new ParticipationService();
        entrainementService = new EntrainementService();
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Participation participation : participationService.getAll()) {
            if (participation.getJustificationAbsence() != null && participation.getJustificationAbsence().startsWith(TEST_PREFIX)) {
                participationService.delete(participation.getId());
            }
        }

        for (Entrainement entrainement : entrainementService.getAll()) {
            if (entrainement.getObjectif() != null && entrainement.getObjectif().startsWith(ENTRAINEMENT_PREFIX)) {
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
    void testAjouterParticipation() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "AJOUT", UserRoles.ROLE_ENTRAINEUR);
        User player = createUser(userService, USER_PREFIX, "PLAYER_AJOUT", UserRoles.ROLE_JOUEUR);
        Entrainement entrainement = createEntrainement(entrainementService, ENTRAINEMENT_PREFIX, "AJOUT", coach.getId());

        Participation participation = new Participation(
                "PRESENT",
                TEST_PREFIX + "AJOUT",
                entrainement.getId(),
                player.getId()
        );

        participationService.add(participation);

        Participation participationAjoutee = findParticipationByJustification(participationService, TEST_PREFIX + "AJOUT");
        assertNotNull(participationAjoutee);
        assertEquals("PRESENT", participationAjoutee.getPresence());
        assertEquals(entrainement.getId(), participationAjoutee.getEntrainementId());
        assertEquals(player.getId(), participationAjoutee.getJoueurId());
    }

    @Test
    @Order(2)
    void testModifierParticipation() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "MODIFIER", UserRoles.ROLE_ENTRAINEUR);
        User playerInitial = createUser(userService, USER_PREFIX, "PLAYER_MODIFIER_INIT", UserRoles.ROLE_JOUEUR);
        User playerModifie = createUser(userService, USER_PREFIX, "PLAYER_MODIFIER_NEW", UserRoles.ROLE_JOUEUR);
        Entrainement entrainementInitial = createEntrainement(entrainementService, ENTRAINEMENT_PREFIX, "MODIFIER_INIT", coach.getId());
        Entrainement entrainementModifie = createEntrainement(entrainementService, ENTRAINEMENT_PREFIX, "MODIFIER_NEW", coach.getId());
        Participation participation = createParticipation(participationService, TEST_PREFIX, "MODIFIER", entrainementInitial.getId(), playerInitial.getId());
        assertNotNull(participation);

        participation.setPresence("ABSENT");
        participation.setJustificationAbsence(TEST_PREFIX + "MODIFIE");
        participation.setEntrainementId(entrainementModifie.getId());
        participation.setJoueurId(playerModifie.getId());

        participationService.update(participation);

        Participation participationModifiee = participationService.getById(participation.getId());
        assertNotNull(participationModifiee);
        assertEquals("ABSENT", participationModifiee.getPresence());
        assertEquals(TEST_PREFIX + "MODIFIE", participationModifiee.getJustificationAbsence());
        assertEquals(entrainementModifie.getId(), participationModifiee.getEntrainementId());
        assertEquals(playerModifie.getId(), participationModifiee.getJoueurId());
    }

    @Test
    @Order(3)
    void testSupprimerParticipation() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "SUPPRIMER", UserRoles.ROLE_ENTRAINEUR);
        User player = createUser(userService, USER_PREFIX, "PLAYER_SUPPRIMER", UserRoles.ROLE_JOUEUR);
        Entrainement entrainement = createEntrainement(entrainementService, ENTRAINEMENT_PREFIX, "SUPPRIMER", coach.getId());
        Participation participation = createParticipation(participationService, TEST_PREFIX, "SUPPRIMER", entrainement.getId(), player.getId());
        assertNotNull(participation);

        participationService.delete(participation.getId());

        Participation participationSupprimee = participationService.getById(participation.getId());
        boolean existeEncore = participationService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), participation.getId()));

        assertNull(participationSupprimee);
        assertFalse(existeEncore);
    }
}

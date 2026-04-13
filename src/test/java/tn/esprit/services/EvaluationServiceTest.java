package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Evaluation;
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
class EvaluationServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_EVALUATION_" + System.currentTimeMillis() + "_";
    private static final String ENTRAINEMENT_PREFIX = TEST_PREFIX + "ENTRAINEMENT_";
    private static final String USER_PREFIX = TEST_PREFIX + "USER_";

    private static EvaluationService evaluationService;
    private static EntrainementService entrainementService;
    private static UserService userService;

    @BeforeAll
    static void setup() throws SQLException {
        evaluationService = new EvaluationService();
        entrainementService = new EntrainementService();
        userService = new UserService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        evaluationService = new EvaluationService();
        entrainementService = new EntrainementService();
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Evaluation evaluation : evaluationService.getAll()) {
            if (evaluation.getCommentaire() != null && evaluation.getCommentaire().startsWith(TEST_PREFIX)) {
                evaluationService.delete(evaluation.getId());
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
    void testAjouterEvaluation() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "AJOUT", UserRoles.ROLE_ENTRAINEUR);
        User player = createUser(userService, USER_PREFIX, "PLAYER_AJOUT", UserRoles.ROLE_JOUEUR);
        Entrainement entrainement = createEntrainement(entrainementService, ENTRAINEMENT_PREFIX, "AJOUT", coach.getId());

        Evaluation evaluation = new Evaluation(
                12.5,
                13.5,
                14.5,
                TEST_PREFIX + "AJOUT",
                entrainement.getId(),
                player.getId()
        );

        evaluationService.add(evaluation);

        Evaluation evaluationAjoutee = evaluation.getId() != null
                ? evaluationService.getById(evaluation.getId())
                : findEvaluationByComment(evaluationService, TEST_PREFIX + "AJOUT");
        assertNotNull(evaluationAjoutee);
        assertEquals(12.5, evaluationAjoutee.getNotePhysique(), 0.001);
        assertEquals(13.5, evaluationAjoutee.getNoteTechnique(), 0.001);
        assertEquals(14.5, evaluationAjoutee.getNoteTactique(), 0.001);
        assertEquals(entrainement.getId(), evaluationAjoutee.getEntrainementId());
        assertEquals(player.getId(), evaluationAjoutee.getJoueurId());
    }

    @Test
    @Order(2)
    void testModifierEvaluation() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "MODIFIER", UserRoles.ROLE_ENTRAINEUR);
        User playerInitial = createUser(userService, USER_PREFIX, "PLAYER_MODIFIER_INIT", UserRoles.ROLE_JOUEUR);
        User playerModifie = createUser(userService, USER_PREFIX, "PLAYER_MODIFIER_NEW", UserRoles.ROLE_JOUEUR);
        Entrainement entrainementInitial = createEntrainement(entrainementService, ENTRAINEMENT_PREFIX, "MODIFIER_INIT", coach.getId());
        Entrainement entrainementModifie = createEntrainement(entrainementService, ENTRAINEMENT_PREFIX, "MODIFIER_NEW", coach.getId());
        Evaluation evaluation = createEvaluation(evaluationService, TEST_PREFIX, "MODIFIER", entrainementInitial.getId(), playerInitial.getId());
        assertNotNull(evaluation);

        evaluation.setNotePhysique(16.0);
        evaluation.setNoteTechnique(17.0);
        evaluation.setNoteTactique(18.0);
        evaluation.setCommentaire(TEST_PREFIX + "MODIFIE");
        evaluation.setEntrainementId(entrainementModifie.getId());
        evaluation.setJoueurId(playerModifie.getId());

        evaluationService.update(evaluation);

        Evaluation evaluationModifiee = evaluationService.getById(evaluation.getId());
        assertNotNull(evaluationModifiee);
        assertEquals(16.0, evaluationModifiee.getNotePhysique(), 0.001);
        assertEquals(17.0, evaluationModifiee.getNoteTechnique(), 0.001);
        assertEquals(18.0, evaluationModifiee.getNoteTactique(), 0.001);
        assertEquals(TEST_PREFIX + "MODIFIE", evaluationModifiee.getCommentaire());
        assertEquals(entrainementModifie.getId(), evaluationModifiee.getEntrainementId());
        assertEquals(playerModifie.getId(), evaluationModifiee.getJoueurId());
    }

    @Test
    @Order(3)
    void testSupprimerEvaluation() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "SUPPRIMER", UserRoles.ROLE_ENTRAINEUR);
        User player = createUser(userService, USER_PREFIX, "PLAYER_SUPPRIMER", UserRoles.ROLE_JOUEUR);
        Entrainement entrainement = createEntrainement(entrainementService, ENTRAINEMENT_PREFIX, "SUPPRIMER", coach.getId());
        Evaluation evaluation = createEvaluation(evaluationService, TEST_PREFIX, "SUPPRIMER", entrainement.getId(), player.getId());
        assertNotNull(evaluation);

        evaluationService.delete(evaluation.getId());

        Evaluation evaluationSupprimee = evaluationService.getById(evaluation.getId());
        boolean existeEncore = evaluationService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), evaluation.getId()));

        assertNull(evaluationSupprimee);
        assertFalse(existeEncore);
    }
}

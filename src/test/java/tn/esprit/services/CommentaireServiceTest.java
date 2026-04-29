package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Commentaire;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentaireServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_COMMENT_" + System.currentTimeMillis() + "_";
    private static final String ANNONCE_PREFIX = TEST_PREFIX + "ANNONCE_";
    private static final String USER_PREFIX = TEST_PREFIX + "USER_";

    private static CommentaireService commentaireService;
    private static AnnonceService annonceService;
    private static UserService userService;

    @BeforeAll
    static void setup() throws SQLException {
        commentaireService = new CommentaireService();
        annonceService = new AnnonceService();
        userService = new UserService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        commentaireService = new CommentaireService();
        annonceService = new AnnonceService();
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Commentaire commentaire : commentaireService.getAll()) {
            if (commentaire.getContenu() != null && commentaire.getContenu().startsWith(TEST_PREFIX)) {
                commentaireService.delete(commentaire.getId());
            }
        }

        for (Annonce annonce : annonceService.getAll()) {
            if (annonce.getTitre() != null && annonce.getTitre().startsWith(ANNONCE_PREFIX)) {
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
    void testAjouterCommentaire() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "AJOUT", UserRoles.ROLE_ENTRAINEUR);
        User player = createUser(userService, USER_PREFIX, "PLAYER_AJOUT", UserRoles.ROLE_JOUEUR);
        Annonce annonce = createAnnonce(annonceService, ANNONCE_PREFIX, "AJOUT", coach.getId());
        assertNotNull(annonce);

        Commentaire commentaire = new Commentaire(
                TEST_PREFIX + "AJOUT",
                LocalDate.of(2026, 6, 10),
                player.getId(),
                annonce.getId(),
                "Auteur Ajout",
                "uploads/comment-cvs/test-ajout.pdf",
                4,
                "PENDING",
                "Reason Ajout"
        );

        commentaireService.add(commentaire);

        Commentaire commentaireAjoute = findCommentaireByContenu(commentaireService, TEST_PREFIX + "AJOUT");
        assertNotNull(commentaireAjoute);
        assertEquals(LocalDate.of(2026, 6, 10), commentaireAjoute.getDateCommentaire());
        assertEquals(player.getId(), commentaireAjoute.getJoueurId());
        assertEquals(annonce.getId(), commentaireAjoute.getAnnonceId());
        assertEquals("Auteur Ajout", commentaireAjoute.getAuteurAnonyme());
        assertEquals("uploads/comment-cvs/test-ajout.pdf", commentaireAjoute.getCvName());
        assertEquals(4, commentaireAjoute.getNbLikes());
        assertEquals("PENDING", commentaireAjoute.getModerationStatus());
        assertEquals("Reason Ajout", commentaireAjoute.getModerationReason());
    }

    @Test
    @Order(2)
    void testModifierCommentaire() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "MODIFIER", UserRoles.ROLE_ENTRAINEUR);
        User player = createUser(userService, USER_PREFIX, "PLAYER_MODIFIER", UserRoles.ROLE_JOUEUR);
        Annonce annonce = createAnnonce(annonceService, ANNONCE_PREFIX, "MODIFIER", coach.getId());
        Commentaire commentaire = createCommentaire(commentaireService, TEST_PREFIX, "MODIFIER", player.getId(), annonce.getId());
        assertNotNull(commentaire);

        commentaire.setContenu(TEST_PREFIX + "MODIFIE");
        commentaire.setDateCommentaire(LocalDate.of(2026, 6, 11));
        commentaire.setAuteurAnonyme("Auteur Modifie");
        commentaire.setCvName("uploads/comment-cvs/test-modifie.pdf");
        commentaire.setNbLikes(7);
        commentaire.setModerationStatus("APPROVED");
        commentaire.setModerationReason("Reason Modifiee");

        commentaireService.update(commentaire);

        Commentaire commentaireModifie = commentaireService.getById(commentaire.getId());
        assertNotNull(commentaireModifie);
        assertEquals(TEST_PREFIX + "MODIFIE", commentaireModifie.getContenu());
        assertEquals(LocalDate.of(2026, 6, 11), commentaireModifie.getDateCommentaire());
        assertEquals("Auteur Modifie", commentaireModifie.getAuteurAnonyme());
        assertEquals("uploads/comment-cvs/test-modifie.pdf", commentaireModifie.getCvName());
        assertEquals(7, commentaireModifie.getNbLikes());
        assertEquals("APPROVED", commentaireModifie.getModerationStatus());
        assertEquals("Reason Modifiee", commentaireModifie.getModerationReason());
    }

    @Test
    @Order(3)
    void testSupprimerCommentaire() throws SQLException {
        User coach = createUser(userService, USER_PREFIX, "SUPPRIMER", UserRoles.ROLE_ENTRAINEUR);
        User player = createUser(userService, USER_PREFIX, "PLAYER_SUPPRIMER", UserRoles.ROLE_JOUEUR);
        Annonce annonce = createAnnonce(annonceService, ANNONCE_PREFIX, "SUPPRIMER", coach.getId());
        Commentaire commentaire = createCommentaire(commentaireService, TEST_PREFIX, "SUPPRIMER", player.getId(), annonce.getId());
        assertNotNull(commentaire);

        commentaireService.delete(commentaire.getId());

        Commentaire commentaireSupprime = commentaireService.getById(commentaire.getId());
        boolean existeEncore = commentaireService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), commentaire.getId()));

        assertNull(commentaireSupprime);
        assertFalse(existeEncore);
    }
}

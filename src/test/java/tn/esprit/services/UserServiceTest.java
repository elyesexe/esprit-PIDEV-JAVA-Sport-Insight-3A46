package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.User;
import tn.esprit.security.PasswordSupport;
import tn.esprit.security.UserRoles;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_USER_" + System.currentTimeMillis() + "_";

    private static UserService userService;

    @BeforeAll
    static void setup() {
        userService = new UserService();
    }

    @BeforeEach
    void initServices() {
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (User user : userService.getAll()) {
            if (user.getEmail() != null && user.getEmail().startsWith(TEST_PREFIX.toLowerCase(Locale.ROOT))) {
                userService.delete(user.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterUser() throws SQLException {
        String email = emailFor(TEST_PREFIX, "AJOUT");
        User user = new User(
                email,
                UserRoles.ROLE_ENTRAINEUR,
                "Secret123",
                TEST_PREFIX + "NomAjout",
                "PrenomAjout",
                phoneFor(TEST_PREFIX + "AJOUT", 11000000),
                LocalDate.of(1992, 3, 14),
                "user-ajout.png",
                "ACTIVE",
                LocalDateTime.of(2026, 1, 1, 8, 30),
                "cv-ajout.pdf",
                LocalDateTime.of(2026, 1, 1, 8, 30)
        );

        userService.add(user);

        User userAjoute = userService.findByEmail(email);
        assertNotNull(userAjoute);
        assertEquals(TEST_PREFIX + "NomAjout", userAjoute.getNom());
        assertEquals("PrenomAjout", userAjoute.getPrenom());
        assertEquals(phoneFor(TEST_PREFIX + "AJOUT", 11000000), userAjoute.getTelephone());
        assertEquals(LocalDate.of(1992, 3, 14), userAjoute.getDateNaissance());
        assertEquals("user-ajout.png", userAjoute.getPhoto());
        assertEquals("ACTIVE", userAjoute.getStatut());
        assertTrue(userAjoute.hasRole(UserRoles.ROLE_ENTRAINEUR));
        assertTrue(PasswordSupport.matches("Secret123", userAjoute.getPassword()));
    }

    @Test
    @Order(2)
    void testModifierUser() throws SQLException {
        User user = createUser(userService, TEST_PREFIX, "MODIFIER", UserRoles.ROLE_USER);
        assertNotNull(user);

        user.setEmail(emailFor(TEST_PREFIX, "MODIFIE"));
        user.setRoles(UserRoles.ROLE_ADMIN);
        user.setPassword("NewSecret123");
        user.setNom(TEST_PREFIX + "NomModifie");
        user.setPrenom("PrenomModifie");
        user.setTelephone(phoneFor(TEST_PREFIX + "MODIFIE", 12000000));
        user.setDateNaissance(LocalDate.of(1989, 7, 21));
        user.setPhoto("user-modifie.png");
        user.setStatut("ACTIVE");
        user.setCvName("cv-modifie.pdf");
        user.setUpdatedAt(LocalDateTime.of(2026, 2, 2, 9, 45));

        userService.update(user);

        User userModifie = userService.getById(user.getId());
        assertNotNull(userModifie);
        assertEquals(emailFor(TEST_PREFIX, "MODIFIE"), userModifie.getEmail());
        assertEquals(TEST_PREFIX + "NomModifie", userModifie.getNom());
        assertEquals("PrenomModifie", userModifie.getPrenom());
        assertEquals(phoneFor(TEST_PREFIX + "MODIFIE", 12000000), userModifie.getTelephone());
        assertEquals(LocalDate.of(1989, 7, 21), userModifie.getDateNaissance());
        assertEquals("user-modifie.png", userModifie.getPhoto());
        assertEquals("cv-modifie.pdf", userModifie.getCvName());
        assertTrue(userModifie.hasRole(UserRoles.ROLE_ADMIN));
        assertTrue(PasswordSupport.matches("NewSecret123", userModifie.getPassword()));
    }

    @Test
    @Order(3)
    void testSupprimerUser() throws SQLException {
        User user = createUser(userService, TEST_PREFIX, "SUPPRIMER", UserRoles.ROLE_USER);
        assertNotNull(user);

        userService.delete(user.getId());

        User userSupprime = userService.getById(user.getId());
        boolean existeEncore = userService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), user.getId()));

        assertNull(userSupprime);
        assertFalse(existeEncore);
    }
}

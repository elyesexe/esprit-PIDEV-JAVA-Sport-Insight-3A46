package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.User;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    private static UserService userService;
    private static int savedUserId;

    // ── user de test ──────────────────────────────────────────────────────
    private static User createTestUser() {
        return new User(
            "test.junit@sportinsight.tn",
            "[\"ROLE_USER\"]",
            "testpass123",
            "TestNom",
            "TestPrenom",
            "+216 99 000 000",
            LocalDate.of(1995, 5, 15),
            "default.png",
            "actif",
            LocalDateTime.now(),
            null,
            LocalDateTime.now()
        );
    }

    @BeforeAll
    static void setup() {
        userService = new UserService();
    }


    // 1. CREATE

    @Test
    @Order(1)
    @DisplayName("1 - Ajouter un utilisateur")
    void testAddUser() {
        User user = createTestUser();
        assertDoesNotThrow(() -> userService.addUser(user),
            "addUser ne doit pas lancer d'exception");
    }


    // 2. READ ALL

    @Test
    @Order(2)
    @DisplayName("2 - Recuperer tous les utilisateurs")
    void testGetAllUsers() throws SQLException {
        List<User> users = userService.getAllUsers();

        assertNotNull(users, "La liste ne doit pas etre null");
        assertFalse(users.isEmpty(), "La liste ne doit pas etre vide");

        // Recuperer l'id du user de test pour les tests suivants
        users.stream()
            .filter(u -> "test.junit@sportinsight.tn".equals(u.getEmail()))
            .findFirst()
            .ifPresent(u -> savedUserId = u.getId());

        assertTrue(savedUserId > 0, "L'id du user de test doit etre > 0");
        System.out.println("ID du user de test : " + savedUserId);
    }


    // 3. FIND BY ID

    @Test
    @Order(3)
    @DisplayName("3 - Trouver un utilisateur par ID")
    void testFindUserById() {
        User found = userService.findUserById(savedUserId);

        assertNotNull(found, "L'utilisateur doit exister en base");
        assertEquals(savedUserId, found.getId());
        assertEquals("test.junit@sportinsight.tn", found.getEmail());
        assertEquals("TestNom", found.getNom());
        assertEquals("TestPrenom", found.getPrenom());
        assertEquals("actif", found.getStatut());
    }

    @Test
    @Order(4)
    @DisplayName("4 - findUserById avec un ID inexistant retourne null")
    void testFindUserByIdNotFound() {
        User notFound = userService.findUserById(999999);
        assertNull(notFound, "Un ID inexistant doit retourner null");
    }


    // 4. UPDATE

    @Test
    @Order(5)
    @DisplayName("5 - Modifier un utilisateur")
    void testUpdateUser() {
        User user = userService.findUserById(savedUserId);
        assertNotNull(user);

        user.setTelephone("+216 11 111 111");
        user.setStatut("inactif");
        user.setNom("NomModifie");
        user.setUpdatedAt(LocalDateTime.now());

        assertDoesNotThrow(() -> userService.updateUser(user),
            "updateUser ne doit pas lancer d'exception");

        // Verification en base
        User updated = userService.findUserById(savedUserId);
        assertNotNull(updated);
        assertEquals("NomModifie",    updated.getNom());
        assertEquals("+216 11 111 111", updated.getTelephone());
        assertEquals("inactif",        updated.getStatut());
    }


    // 5. VALIDATION

    @Test
    @Order(6)
    @DisplayName("6 - Email ne peut pas etre vide")
    void testEmailNotEmpty() {
        User user = createTestUser();
        user.setEmail("");
        assertThrows(Exception.class, () -> userService.addUser(user),
            "Un email vide doit lever une exception");
    }

    @Test
    @Order(7)
    @DisplayName("7 - Email doit contenir @")
    void testEmailFormat() {
        String email = "test.junit@sportinsight.tn";
        assertTrue(email.contains("@"), "L'email doit contenir @");
        assertFalse(email.isEmpty(), "L'email ne doit pas etre vide");
    }

    @Test
    @Order(8)
    @DisplayName("8 - Mot de passe ne peut pas etre null")
    void testPasswordNotNull() {
        User user = userService.findUserById(savedUserId);
        assertNotNull(user);
        assertNotNull(user.getPassword(), "Le mot de passe ne doit pas etre null");
        assertFalse(user.getPassword().isEmpty(), "Le mot de passe ne doit pas etre vide");
    }

    @Test
    @Order(9)
    @DisplayName("9 - Statut doit etre actif ou inactif")
    void testStatutValide() {
        User user = userService.findUserById(savedUserId);
        assertNotNull(user);
        String statut = user.getStatut();
        assertTrue(
            "actif".equals(statut) || "inactif".equals(statut),
            "Le statut doit etre 'actif' ou 'inactif', recu : " + statut
        );
    }

    @Test
    @Order(10)
    @DisplayName("10 - La liste contient au moins un utilisateur")
    void testListNotEmpty() throws SQLException {
        List<User> users = userService.getAllUsers();
        assertNotNull(users);
        assertTrue(users.size() >= 1, "Il doit y avoir au moins 1 utilisateur");
    }


    // 6. DELETE

    @Test
    @Order(11)
    @DisplayName("11 - Supprimer l'utilisateur de test")
    void testDeleteUser() {
        assertDoesNotThrow(() -> userService.deleteUser(savedUserId),
            "deleteUser ne doit pas lancer d'exception");

        User deleted = userService.findUserById(savedUserId);
        assertNull(deleted, "L'utilisateur supprime ne doit plus exister en base");
    }

    @AfterAll
    static void teardown() {
        System.out.println("Tests termines.");
    }
}

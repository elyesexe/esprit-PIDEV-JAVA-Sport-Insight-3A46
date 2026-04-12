package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.entities.Commentaire;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommentaireServiceTest {
    private Connection connection;
    private CommentaireService commentaireService;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS commentaire");
            st.execute("DROP TABLE IF EXISTS annonce");
            st.execute("CREATE TABLE annonce (id INT AUTO_INCREMENT PRIMARY KEY, titre VARCHAR(255), description VARCHAR(1024), poste_recherche VARCHAR(255), niveau_requis VARCHAR(255), date_publication DATE, statut VARCHAR(50), entraineur_id INT, comments_enabled BOOLEAN, urgent BOOLEAN)");
            st.execute("CREATE TABLE commentaire (id INT AUTO_INCREMENT PRIMARY KEY, contenu VARCHAR(1000), date_commentaire DATE, joueur_id INT, annonce_id INT, auteur_anonyme VARCHAR(255), nb_likes INT, moderation_status VARCHAR(50), moderation_reason VARCHAR(255))");
        }
        commentaireService = new CommentaireService(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS commentaire");
            st.execute("DROP TABLE IF EXISTS annonce");
        }
        connection.close();
    }

    private int insertAnnonceAndGetId(boolean commentsEnabled) throws SQLException {
        return insertAnnonceWithDateAndComments(LocalDate.now(), commentsEnabled);
    }

    private int insertAnnonceWithDateAndComments(LocalDate publicationDate, boolean commentsEnabled) throws SQLException {
        String sql = "INSERT INTO annonce (titre, description, poste_recherche, niveau_requis, date_publication, statut, comments_enabled, urgent) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "T1");
            ps.setString(2, "D1");
            ps.setString(3, "Poste");
            ps.setString(4, "Niveau");
            ps.setDate(5, Date.valueOf(publicationDate));
            ps.setString(6, "ACTIVE");
            ps.setBoolean(7, commentsEnabled);
            ps.setBoolean(8, false);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Impossible d'inserer annonce de test");
    }

    @Test
    void testAddAndGetAllAndGetByIdPreserveNullValues() throws Exception {
        int annonceId = insertAnnonceAndGetId(true);
        Commentaire commentaire = new Commentaire("contenu1", LocalDate.now(), null, annonceId, "anon", 0, "OK", null);
        commentaireService.add(commentaire);

        List<Commentaire> all = commentaireService.getAll();
        assertEquals(1, all.size());

        Commentaire retrieved = all.get(0);
        assertEquals("contenu1", retrieved.getContenu());
        assertNull(retrieved.getJoueurId());
        assertEquals(Integer.valueOf(annonceId), retrieved.getAnnonceId());

        Commentaire byId = commentaireService.getById(retrieved.getId());
        assertNotNull(byId);
        assertEquals(retrieved.getContenu(), byId.getContenu());
        assertNull(byId.getJoueurId());
    }

    @Test
    void testUpdateAndDelete() throws Exception {
        int annonceId = insertAnnonceAndGetId(true);
        Commentaire commentaire = new Commentaire("contenu2", LocalDate.now(), 1, annonceId, "anon", 2, "OK", null);
        commentaireService.add(commentaire);

        List<Commentaire> all = commentaireService.getAll();
        assertEquals(1, all.size());
        Commentaire stored = all.get(0);

        stored.setContenu("modifie");
        stored.setJoueurId(null);
        commentaireService.update(stored);

        Commentaire updated = commentaireService.getById(stored.getId());
        assertEquals("modifie", updated.getContenu());
        assertNull(updated.getJoueurId());

        commentaireService.delete(stored.getId());
        assertNull(commentaireService.getById(stored.getId()));
    }

    @Test
    void testGetCommentairesByAnnonceAndCount() throws Exception {
        int a1 = insertAnnonceAndGetId(true);
        int a2 = insertAnnonceAndGetId(true);

        commentaireService.add(new Commentaire("c1", LocalDate.now(), null, a1, "a", 0, "OK", null));
        commentaireService.add(new Commentaire("c2", LocalDate.now(), null, a1, "b", 0, "OK", null));
        commentaireService.add(new Commentaire("c3", LocalDate.now(), 7, a2, "c", 0, "OK", null));

        List<Commentaire> byAnnonce = commentaireService.getCommentairesByAnnonce(a1);
        assertEquals(2, byAnnonce.size());

        int countA1 = commentaireService.countCommentairesByAnnonce(a1);
        assertEquals(2, countA1);

        Map<Integer, Integer> map = commentaireService.countCommentairesGroupByAnnonce();
        assertEquals(2, map.get(a1));
        assertEquals(1, map.get(a2));
    }

    @Test
    void testGetCommentairesByJoueur() throws Exception {
        int annonceId = insertAnnonceAndGetId(true);
        commentaireService.add(new Commentaire("c1", LocalDate.now(), 5, annonceId, "a", 0, "OK", null));
        commentaireService.add(new Commentaire("c2", LocalDate.now(), 5, annonceId, "b", 0, "OK", null));
        commentaireService.add(new Commentaire("c3", LocalDate.now(), 9, annonceId, "c", 0, "OK", null));

        List<Commentaire> byJoueur = commentaireService.getCommentairesByJoueur(5);
        assertEquals(2, byJoueur.size());
    }

    @Test
    void testSearchAdvancedByPublicationDateAndJoueur() throws Exception {
        int annonceDate1 = insertAnnonceWithDateAndComments(LocalDate.of(2026, 4, 12), true);
        int annonceDate2 = insertAnnonceWithDateAndComments(LocalDate.of(2026, 4, 10), true);

        commentaireService.add(new Commentaire("c1", LocalDate.now(), 5, annonceDate1, "a", 0, "OK", null));
        commentaireService.add(new Commentaire("c2", LocalDate.now(), 7, annonceDate1, "b", 0, "OK", null));
        commentaireService.add(new Commentaire("c3", LocalDate.now(), 5, annonceDate2, "c", 0, "OK", null));

        List<Commentaire> byDateAndJoueur = commentaireService.searchAdvanced(LocalDate.of(2026, 4, 12), 5);
        assertEquals(1, byDateAndJoueur.size());
        assertEquals("c1", byDateAndJoueur.get(0).getContenu());

        List<Commentaire> byDateOnly = commentaireService.searchAdvanced(LocalDate.of(2026, 4, 12), null);
        assertEquals(2, byDateOnly.size());

        List<Commentaire> byJoueurOnly = commentaireService.searchAdvanced(null, 5);
        assertEquals(2, byJoueurOnly.size());
    }

    @Test
    void testAddThrowsWhenCommentsAreClosed() throws Exception {
        int annonceId = insertAnnonceAndGetId(false);

        SQLException exception = assertThrows(
                SQLException.class,
                () -> commentaireService.add(new Commentaire("blocked", LocalDate.now(), null, annonceId, "anon", 0, "OK", null))
        );

        assertEquals("Les commentaires sont fermes pour cette annonce.", exception.getMessage());
        assertEquals(0, commentaireService.getAll().size());
    }

    @Test
    void testAddThrowsWhenAnnonceDoesNotExist() {
        SQLException exception = assertThrows(
                SQLException.class,
                () -> commentaireService.add(new Commentaire("blocked", LocalDate.now(), null, 555, "anon", 0, "OK", null))
        );

        assertEquals("Annonce introuvable pour l'id 555.", exception.getMessage());
    }
}

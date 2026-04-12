package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.entities.Annonce;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnonceServiceTest {
    private Connection connection;
    private AnnonceService annonceService;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS annonce");
            st.execute("CREATE TABLE annonce (id INT AUTO_INCREMENT PRIMARY KEY, titre VARCHAR(255), description VARCHAR(1024), poste_recherche VARCHAR(255), niveau_requis VARCHAR(255), date_publication DATE, statut VARCHAR(50), entraineur_id INT, comments_enabled BOOLEAN, urgent BOOLEAN)");
        }
        annonceService = new AnnonceService(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS annonce");
        }
        connection.close();
    }

    @Test
    void testAddGetAllGetByIdUpdateDelete() throws Exception {
        Annonce annonce = new Annonce("T1", "D1", "Poste", "Niv", LocalDate.now(), "ACTIVE", null, false, true);
        annonceService.add(annonce);

        List<Annonce> all = annonceService.getAll();
        assertEquals(1, all.size());
        Annonce stored = all.get(0);
        assertEquals("T1", stored.getTitre());
        assertFalse(stored.getCommentsEnabled());
        assertTrue(stored.getUrgent());

        stored.setTitre("T1-mod");
        stored.setCommentsEnabled(true);
        stored.setUrgent(false);
        annonceService.update(stored);

        Annonce fetched = annonceService.getById(stored.getId());
        assertNotNull(fetched);
        assertEquals("T1-mod", fetched.getTitre());
        assertTrue(fetched.getCommentsEnabled());
        assertFalse(fetched.getUrgent());

        annonceService.delete(stored.getId());
        assertNull(annonceService.getById(stored.getId()));
    }

    @Test
    void testSearchMethodsPreserveOptionalFields() throws Exception {
        LocalDate publicationDate = LocalDate.now();
        Annonce matching = new Annonce("FindMe", "D", "Gardien", "N", publicationDate, "ACTIVE", 10, false, true);
        Annonce other = new Annonce("Other", "D", "Attaquant", "N", publicationDate.minusDays(1), "INACTIVE", 20, true, false);
        annonceService.add(matching);
        annonceService.add(other);

        List<Annonce> actives = annonceService.getAnnoncesActives();
        assertEquals(1, actives.size());
        assertFalse(actives.get(0).getCommentsEnabled());
        assertTrue(actives.get(0).getUrgent());

        List<Annonce> byPoste = annonceService.getAnnoncesByPoste("Gardien");
        assertEquals(1, byPoste.size());
        assertFalse(byPoste.get(0).getCommentsEnabled());
        assertTrue(byPoste.get(0).getUrgent());

        List<Annonce> byTitre = annonceService.searchByTitre("Find");
        assertEquals(1, byTitre.size());
        assertFalse(byTitre.get(0).getCommentsEnabled());
        assertTrue(byTitre.get(0).getUrgent());

        List<Annonce> byDate = annonceService.searchByDatePublication(publicationDate);
        assertEquals(1, byDate.size());
        assertFalse(byDate.get(0).getCommentsEnabled());
        assertTrue(byDate.get(0).getUrgent());

        List<Annonce> byTitreAndDate = annonceService.searchByTitreAndDate("Find", publicationDate);
        assertEquals(1, byTitreAndDate.size());
        assertFalse(byTitreAndDate.get(0).getCommentsEnabled());
        assertTrue(byTitreAndDate.get(0).getUrgent());

        List<Annonce> byEntraineur = annonceService.getAnnoncesByEntraineur(10);
        assertEquals(1, byEntraineur.size());
        assertEquals(Integer.valueOf(10), byEntraineur.get(0).getEntraineurId());
    }
}

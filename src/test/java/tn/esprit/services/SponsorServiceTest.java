package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Sponsor;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SponsorServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_SPONSOR_" + System.currentTimeMillis() + "_";

    private static SponsorService sponsorService;

    @BeforeAll
    static void setup() {
        sponsorService = new SponsorService();
    }

    @BeforeEach
    void initServices() {
        sponsorService = new SponsorService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Sponsor sponsor : sponsorService.getAll()) {
            if (sponsor.getNom() != null && sponsor.getNom().startsWith(TEST_PREFIX)) {
                sponsorService.delete(sponsor.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterSponsor() throws SQLException {
        String nom = TEST_PREFIX + "AJOUT";
        Sponsor sponsor = new Sponsor(
                nom,
                emailFor(TEST_PREFIX, "AJOUT"),
                phoneFor(nom, 31000000),
                4500.0,
                "sponsor-ajout.png",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                "Adresse Ajout"
        );

        sponsorService.add(sponsor);

        Sponsor sponsorAjoute = findSponsorByNom(sponsorService, nom);
        assertNotNull(sponsorAjoute);
        assertEquals(emailFor(TEST_PREFIX, "AJOUT"), sponsorAjoute.getEmail());
        assertEquals(phoneFor(nom, 31000000), sponsorAjoute.getTelephone());
        assertEquals(4500.0, sponsorAjoute.getBudget(), 0.001);
        assertEquals("sponsor-ajout.png", sponsorAjoute.getLogoName());
        assertEquals("Adresse Ajout", sponsorAjoute.getAdresse());
    }

    @Test
    @Order(2)
    void testModifierSponsor() throws SQLException {
        Sponsor sponsor = createSponsor(sponsorService, TEST_PREFIX, "MODIFIER");
        assertNotNull(sponsor);

        sponsor.setNom(TEST_PREFIX + "MODIFIE");
        sponsor.setEmail(emailFor(TEST_PREFIX, "MODIFIE"));
        sponsor.setTelephone(phoneFor(TEST_PREFIX + "MODIFIE", 32000000));
        sponsor.setBudget(6200.0);
        sponsor.setLogoName("sponsor-modifie.png");
        sponsor.setAdresse("Adresse Modifiee");

        sponsorService.update(sponsor);

        Sponsor sponsorModifie = sponsorService.getById(sponsor.getId());
        assertNotNull(sponsorModifie);
        assertEquals(TEST_PREFIX + "MODIFIE", sponsorModifie.getNom());
        assertEquals(emailFor(TEST_PREFIX, "MODIFIE"), sponsorModifie.getEmail());
        assertEquals(phoneFor(TEST_PREFIX + "MODIFIE", 32000000), sponsorModifie.getTelephone());
        assertEquals(6200.0, sponsorModifie.getBudget(), 0.001);
        assertEquals("sponsor-modifie.png", sponsorModifie.getLogoName());
        assertEquals("Adresse Modifiee", sponsorModifie.getAdresse());
    }

    @Test
    @Order(3)
    void testSupprimerSponsor() throws SQLException {
        Sponsor sponsor = createSponsor(sponsorService, TEST_PREFIX, "SUPPRIMER");
        assertNotNull(sponsor);

        sponsorService.delete(sponsor.getId());

        Sponsor sponsorSupprime = sponsorService.getById(sponsor.getId());
        boolean existeEncore = sponsorService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), sponsor.getId()));

        assertNull(sponsorSupprime);
        assertFalse(existeEncore);
    }
}

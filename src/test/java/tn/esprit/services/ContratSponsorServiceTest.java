package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Sponsor;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContratSponsorServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_CONTRAT_" + System.currentTimeMillis() + "_";
    private static final String SPONSOR_PREFIX = TEST_PREFIX + "SPONSOR_";
    private static final String EQUIPE_PREFIX = TEST_PREFIX + "EQUIPE_";

    private static ContratSponsorService contratService;
    private static SponsorService sponsorService;
    private static EquipeService equipeService;

    @BeforeAll
    static void setup() throws SQLException {
        contratService = new ContratSponsorService();
        sponsorService = new SponsorService();
        equipeService = new EquipeService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        contratService = new ContratSponsorService();
        sponsorService = new SponsorService();
        equipeService = new EquipeService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (ContratSponsor contrat : contratService.getAll()) {
            if (contrat.getDescription() != null && contrat.getDescription().startsWith(TEST_PREFIX)) {
                contratService.delete(contrat.getId());
            }
        }

        for (Sponsor sponsor : sponsorService.getAll()) {
            if (sponsor.getNom() != null && sponsor.getNom().startsWith(SPONSOR_PREFIX)) {
                sponsorService.delete(sponsor.getId());
            }
        }

        for (Equipe equipe : equipeService.getAll()) {
            if (equipe.getNom() != null && equipe.getNom().startsWith(EQUIPE_PREFIX)) {
                equipeService.delete(equipe.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterContratSponsor() throws SQLException {
        Sponsor sponsor = createSponsor(sponsorService, SPONSOR_PREFIX, "AJOUT");
        Equipe equipe = createEquipe(equipeService, EQUIPE_PREFIX, "AJOUT", "PL");
        assertNotNull(sponsor);
        assertNotNull(equipe);

        ContratSponsor contrat = new ContratSponsor(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31),
                8000.0,
                TEST_PREFIX + "AJOUT",
                "ACTIVE",
                true,
                "PAID",
                sponsor.getId(),
                equipe.getId()
        );

        contratService.add(contrat);

        ContratSponsor contratAjoute = findContratByDescription(contratService, TEST_PREFIX + "AJOUT");
        assertNotNull(contratAjoute);
        assertEquals(LocalDate.of(2026, 7, 1), contratAjoute.getDateDebut());
        assertEquals(LocalDate.of(2026, 12, 31), contratAjoute.getDateFin());
        assertEquals(8000.0, contratAjoute.getMontant(), 0.001);
        assertEquals("ACTIVE", contratAjoute.getStatut());
        assertTrue(contratAjoute.isNotified());
        assertEquals("PAID", contratAjoute.getStatutPaiement());
        assertEquals(sponsor.getId(), contratAjoute.getSponsorId());
        assertEquals(equipe.getId(), contratAjoute.getEquipeId());
    }

    @Test
    @Order(2)
    void testModifierContratSponsor() throws SQLException {
        Sponsor sponsorInitial = createSponsor(sponsorService, SPONSOR_PREFIX, "MODIFIER_INIT");
        Sponsor sponsorModifie = createSponsor(sponsorService, SPONSOR_PREFIX, "MODIFIER_NEW");
        Equipe equipeInitiale = createEquipe(equipeService, EQUIPE_PREFIX, "MODIFIER_INIT", "PL");
        Equipe equipeModifiee = createEquipe(equipeService, EQUIPE_PREFIX, "MODIFIER_NEW", "BL1");
        ContratSponsor contrat = createContrat(contratService, TEST_PREFIX, "MODIFIER", sponsorInitial.getId(), equipeInitiale.getId());
        assertNotNull(contrat);

        contrat.setDateDebut(LocalDate.of(2026, 8, 1));
        contrat.setDateFin(LocalDate.of(2027, 1, 31));
        contrat.setMontant(9500.0);
        contrat.setDescription(TEST_PREFIX + "MODIFIE");
        contrat.setStatut("RENEWED");
        contrat.setNotified(true);
        contrat.setStatutPaiement("PARTIAL");
        contrat.setSponsorId(sponsorModifie.getId());
        contrat.setEquipeId(equipeModifiee.getId());

        contratService.update(contrat);

        ContratSponsor contratModifie = contratService.getById(contrat.getId());
        assertNotNull(contratModifie);
        assertEquals(LocalDate.of(2026, 8, 1), contratModifie.getDateDebut());
        assertEquals(LocalDate.of(2027, 1, 31), contratModifie.getDateFin());
        assertEquals(9500.0, contratModifie.getMontant(), 0.001);
        assertEquals(TEST_PREFIX + "MODIFIE", contratModifie.getDescription());
        assertEquals("RENEWED", contratModifie.getStatut());
        assertTrue(contratModifie.isNotified());
        assertEquals("PARTIAL", contratModifie.getStatutPaiement());
        assertEquals(sponsorModifie.getId(), contratModifie.getSponsorId());
        assertEquals(equipeModifiee.getId(), contratModifie.getEquipeId());
    }

    @Test
    @Order(3)
    void testSupprimerContratSponsor() throws SQLException {
        Sponsor sponsor = createSponsor(sponsorService, SPONSOR_PREFIX, "SUPPRIMER");
        Equipe equipe = createEquipe(equipeService, EQUIPE_PREFIX, "SUPPRIMER", "SA");
        ContratSponsor contrat = createContrat(contratService, TEST_PREFIX, "SUPPRIMER", sponsor.getId(), equipe.getId());
        assertNotNull(contrat);

        contratService.delete(contrat.getId());

        ContratSponsor contratSupprime = contratService.getById(contrat.getId());
        boolean existeEncore = contratService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), contrat.getId()));

        assertNull(contratSupprime);
        assertFalse(existeEncore);
    }
}

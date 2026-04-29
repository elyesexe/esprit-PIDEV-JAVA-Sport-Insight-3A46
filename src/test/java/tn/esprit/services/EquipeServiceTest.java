package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import tn.esprit.entities.Equipe;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EquipeServiceTest {

    private static final String TEST_PREFIX = "JUNIT_EQUIPE_" + System.currentTimeMillis() + "_";

    private static EquipeService service;

    @BeforeAll
    static void setup() throws SQLException {
        service = new EquipeService();
    }

    @BeforeEach
    void initService() throws SQLException {
        service = new EquipeService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Equipe equipe : service.getAll()) {
            if (isTestEquipe(equipe)) {
                service.delete(equipe.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterEquipe() throws SQLException {
        String nomEquipe = testName("AJOUT");
        Equipe equipe = buildEquipe(nomEquipe, "Coach Ajout", "PL");

        service.add(equipe);

        List<Equipe> equipes = service.getAll();
        assertFalse(equipes.isEmpty());

        Equipe equipeAjoutee = findByName(nomEquipe);
        assertNotNull(equipeAjoutee);
        assertEquals("Coach Ajout", equipeAjoutee.getCoach());
        assertEquals("Adresse Ajout", equipeAjoutee.getAdresse());
        assertEquals("11111111", equipeAjoutee.getTelephone());
        assertEquals("ajout@test.com", equipeAjoutee.getEmail());
        assertEquals("logo-ajout.png", equipeAjoutee.getImage());
        assertEquals("PL", equipeAjoutee.getCompetitionCode());
        assertTrue(equipes.stream().anyMatch(item -> nomEquipe.equals(item.getNom())));
    }

    @Test
    @Order(2)
    void testModifierEquipe() throws SQLException {
        String nomInitial = testName("MODIFIER");
        service.add(buildEquipe(nomInitial, "Coach Initial", "PL"));

        Equipe equipe = findByName(nomInitial);
        assertNotNull(equipe);

        equipe.setNom(testName("MODIFIE"));
        equipe.setCoach("Coach Modifie");
        equipe.setAdresse("Adresse Modifiee");
        equipe.setTelephone("22222222");
        equipe.setEmail("modifie@test.com");
        equipe.setImage("logo-modifie.png");
        equipe.setCompetitionCode("BL1");

        service.update(equipe);

        Equipe equipeModifiee = service.getById(equipe.getId());
        assertNotNull(equipeModifiee);
        assertEquals(equipe.getNom(), equipeModifiee.getNom());
        assertEquals("Coach Modifie", equipeModifiee.getCoach());
        assertEquals("Adresse Modifiee", equipeModifiee.getAdresse());
        assertEquals("22222222", equipeModifiee.getTelephone());
        assertEquals("modifie@test.com", equipeModifiee.getEmail());
        assertEquals("logo-modifie.png", equipeModifiee.getImage());
        assertEquals("BL1", equipeModifiee.getCompetitionCode());
    }

    @Test
    @Order(3)
    void testSupprimerEquipe() throws SQLException {
        String nomEquipe = testName("SUPPRIMER");
        service.add(buildEquipe(nomEquipe, "Coach Suppression", "SA"));

        Equipe equipe = findByName(nomEquipe);
        assertNotNull(equipe);

        service.delete(equipe.getId());

        Equipe equipeSupprimee = service.getById(equipe.getId());
        boolean existeEncore = service.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), equipe.getId()));

        assertNull(equipeSupprimee);
        assertFalse(existeEncore);
    }

    private Equipe buildEquipe(String nom, String coach, String competitionCode) {
        String label = displayLabel(nom);
        Equipe equipe = new Equipe(
                nom,
                coach,
                "Adresse " + label,
                extractPhone(nom),
                extractEmail(nom),
                "logo-" + extractLabel(nom).toLowerCase() + ".png"
        );
        equipe.setCompetitionCode(competitionCode);
        return equipe;
    }

    private Equipe findByName(String nom) throws SQLException {
        return service.getAll().stream()
                .filter(equipe -> nom.equals(equipe.getNom()))
                .findFirst()
                .orElse(null);
    }

    private boolean isTestEquipe(Equipe equipe) {
        return equipe.getNom() != null && equipe.getNom().startsWith(TEST_PREFIX);
    }

    private String testName(String label) {
        return TEST_PREFIX + label;
    }

    private String extractLabel(String nom) {
        return nom.substring(TEST_PREFIX.length());
    }

    private String extractPhone(String nom) {
        String label = extractLabel(nom);
        return switch (label) {
            case "AJOUT" -> "11111111";
            case "MODIFIER" -> "33333333";
            case "SUPPRIMER" -> "44444444";
            case "MODIFIE" -> "22222222";
            default -> "99999999";
        };
    }

    private String extractEmail(String nom) {
        String label = extractLabel(nom).toLowerCase();
        return label + "@test.com";
    }

    private String displayLabel(String nom) {
        String label = extractLabel(nom).toLowerCase();
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
}

package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MatchsServiceTest {

    private static final String TEST_MATCH_PREFIX = "JUNIT_MATCH_" + System.currentTimeMillis() + "_";
    private static final String TEST_EQUIPE_PREFIX = TEST_MATCH_PREFIX + "TEAM_";

    private static MatchsService matchsService;
    private static EquipeService equipeService;

    @BeforeAll
    static void setup() throws SQLException {
        matchsService = new MatchsService();
        equipeService = new EquipeService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        matchsService = new MatchsService();
        equipeService = new EquipeService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Matchs matchs : matchsService.getAll()) {
            if (isTestMatch(matchs)) {
                matchsService.delete(matchs.getId());
            }
        }

        for (Equipe equipe : equipeService.getAll()) {
            if (isTestEquipe(equipe)) {
                equipeService.delete(equipe.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterMatch() throws SQLException {
        TeamPair teams = createTeamPair("AJOUT", "PL");
        String idMatch = testIdMatch("AJOUT");

        Matchs matchs = buildMatch(
                idMatch,
                teams.home().getId(),
                teams.away().getId(),
                LocalDate.of(2026, 4, 20),
                LocalTime.of(18, 30),
                "Stade Ajout",
                "Championnat",
                "Programme",
                "4-3-3",
                "4-4-2",
                2,
                1,
                "PL"
        );

        matchsService.add(matchs);

        List<Matchs> matchsList = matchsService.getAll();
        assertFalse(matchsList.isEmpty());

        Matchs matchAjoute = findByIdMatch(idMatch);
        assertNotNull(matchAjoute);
        assertEquals(LocalDate.of(2026, 4, 20), matchAjoute.getDateMatch());
        assertEquals(LocalTime.of(18, 30), matchAjoute.getHeureDebut());
        assertEquals("Stade Ajout", matchAjoute.getLieu());
        assertEquals("Championnat", matchAjoute.getType());
        assertEquals("Programme", matchAjoute.getStatut());
        assertEquals("4-3-3", matchAjoute.getLineupDomicile());
        assertEquals("4-4-2", matchAjoute.getLineupExterieur());
        assertEquals(2, matchAjoute.getScoreEquipeDomicile());
        assertEquals(1, matchAjoute.getScoreEquipeExterieur());
        assertEquals(teams.home().getId(), matchAjoute.getEquipeDomicileId());
        assertEquals(teams.away().getId(), matchAjoute.getEquipeExterieurId());
        assertEquals("PL", matchAjoute.getCompetitionCode());
        assertTrue(matchsList.stream().anyMatch(item -> idMatch.equals(item.getIdMatch())));
    }

    @Test
    @Order(2)
    void testModifierMatch() throws SQLException {
        TeamPair initialTeams = createTeamPair("MODIFIER_INIT", "PL");
        String idMatchInitial = testIdMatch("MODIFIER");

        matchsService.add(buildMatch(
                idMatchInitial,
                initialTeams.home().getId(),
                initialTeams.away().getId(),
                LocalDate.of(2026, 4, 21),
                LocalTime.of(20, 0),
                "Stade Initial",
                "Coupe",
                "Programme",
                "3-5-2",
                "4-3-3",
                0,
                0,
                "PL"
        ));

        Matchs match = findByIdMatch(idMatchInitial);
        assertNotNull(match);

        TeamPair updatedTeams = createTeamPair("MODIFIER_NEW", "BL1");
        match.setIdMatch(testIdMatch("MODIFIE"));
        match.setDateMatch(LocalDate.of(2026, 4, 22));
        match.setHeureDebut(LocalTime.of(21, 15));
        match.setLieu("Stade Modifie");
        match.setType("Amical");
        match.setStatut("Fini");
        match.setLineupDomicile("4-2-3-1");
        match.setLineupExterieur("5-3-2");
        match.setScoreEquipeDomicile(3);
        match.setScoreEquipeExterieur(2);
        match.setEquipeDomicileId(updatedTeams.home().getId());
        match.setEquipeExterieurId(updatedTeams.away().getId());
        match.setCompetitionCode("BL1");

        matchsService.update(match);

        Matchs matchModifie = matchsService.getById(match.getId());
        assertNotNull(matchModifie);
        assertEquals(testIdMatch("MODIFIE"), matchModifie.getIdMatch());
        assertEquals(LocalDate.of(2026, 4, 22), matchModifie.getDateMatch());
        assertEquals(LocalTime.of(21, 15), matchModifie.getHeureDebut());
        assertEquals("Stade Modifie", matchModifie.getLieu());
        assertEquals("Amical", matchModifie.getType());
        assertEquals("Fini", matchModifie.getStatut());
        assertEquals("4-2-3-1", matchModifie.getLineupDomicile());
        assertEquals("5-3-2", matchModifie.getLineupExterieur());
        assertEquals(3, matchModifie.getScoreEquipeDomicile());
        assertEquals(2, matchModifie.getScoreEquipeExterieur());
        assertEquals(updatedTeams.home().getId(), matchModifie.getEquipeDomicileId());
        assertEquals(updatedTeams.away().getId(), matchModifie.getEquipeExterieurId());
        assertEquals("BL1", matchModifie.getCompetitionCode());
    }

    @Test
    @Order(3)
    void testSupprimerMatch() throws SQLException {
        TeamPair teams = createTeamPair("SUPPRIMER", "SA");
        String idMatch = testIdMatch("SUPPRIMER");

        matchsService.add(buildMatch(
                idMatch,
                teams.home().getId(),
                teams.away().getId(),
                LocalDate.of(2026, 4, 23),
                LocalTime.of(19, 45),
                "Stade Suppression",
                "Championnat",
                "Programme",
                "4-4-2",
                "4-3-3",
                1,
                0,
                "SA"
        ));

        Matchs match = findByIdMatch(idMatch);
        assertNotNull(match);

        matchsService.delete(match.getId());

        Matchs matchSupprime = matchsService.getById(match.getId());
        boolean existeEncore = matchsService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), match.getId()));

        assertNull(matchSupprime);
        assertFalse(existeEncore);
    }

    @Test
    @Order(4)
    void findsNextFixturesAndLastResultsForTeam() throws SQLException {
        TeamPair teams = createTeamPair("DETAIL_MATCHES", "PL");
        LocalDate today = LocalDate.now();

        matchsService.add(buildMatch(
                testIdMatch("NEXT_2"),
                teams.home().getId(),
                teams.away().getId(),
                today.plusDays(2),
                LocalTime.of(20, 0),
                "Future 2",
                "Championnat",
                "Programme",
                "",
                "",
                null,
                null,
                "PL"
        ));
        matchsService.add(buildMatch(
                testIdMatch("NEXT_1"),
                teams.away().getId(),
                teams.home().getId(),
                today.plusDays(1),
                LocalTime.of(18, 0),
                "Future 1",
                "Championnat",
                "Programme",
                "",
                "",
                null,
                null,
                "PL"
        ));
        matchsService.add(buildMatch(
                testIdMatch("WIN"),
                teams.home().getId(),
                teams.away().getId(),
                today.minusDays(1),
                LocalTime.of(20, 0),
                "Past Win",
                "Championnat",
                "Fini",
                "",
                "",
                2,
                0,
                "PL"
        ));
        matchsService.add(buildMatch(
                testIdMatch("DRAW"),
                teams.away().getId(),
                teams.home().getId(),
                today.minusDays(2),
                LocalTime.of(20, 0),
                "Past Draw",
                "Championnat",
                "Fini",
                "",
                "",
                1,
                1,
                "PL"
        ));

        List<Matchs> nextMatches = matchsService.findNextMatchesForTeam(teams.home().getId(), 5);
        List<Matchs> lastResults = matchsService.findLastResultsForTeam(teams.home().getId(), 5);

        assertTrue(nextMatches.size() >= 2);
        assertEquals(testIdMatch("NEXT_1"), nextMatches.get(0).getIdMatch());
        assertEquals(testIdMatch("NEXT_2"), nextMatches.get(1).getIdMatch());
        assertTrue(lastResults.size() >= 2);
        assertEquals(testIdMatch("WIN"), lastResults.get(0).getIdMatch());
        assertEquals(testIdMatch("DRAW"), lastResults.get(1).getIdMatch());
    }

    private Matchs buildMatch(
            String idMatch,
            Integer equipeDomicileId,
            Integer equipeExterieurId,
            LocalDate dateMatch,
            LocalTime heureDebut,
            String lieu,
            String type,
            String statut,
            String lineupDomicile,
            String lineupExterieur,
            Integer scoreEquipeDomicile,
            Integer scoreEquipeExterieur,
            String competitionCode
    ) {
        Matchs matchs = new Matchs(
                idMatch,
                dateMatch,
                heureDebut,
                lieu,
                type,
                statut,
                lineupDomicile,
                lineupExterieur,
                scoreEquipeDomicile,
                scoreEquipeExterieur,
                equipeDomicileId,
                equipeExterieurId
        );
        matchs.setCompetitionCode(competitionCode);
        return matchs;
    }

    private TeamPair createTeamPair(String label, String competitionCode) throws SQLException {
        String homeName = testTeamName(label + "_HOME");
        String awayName = testTeamName(label + "_AWAY");

        equipeService.add(buildEquipe(homeName, "Coach Home " + label, competitionCode));
        equipeService.add(buildEquipe(awayName, "Coach Away " + label, competitionCode));

        Equipe home = findEquipeByName(homeName);
        Equipe away = findEquipeByName(awayName);
        assertNotNull(home);
        assertNotNull(away);

        return new TeamPair(home, away);
    }

    private Equipe buildEquipe(String nom, String coach, String competitionCode) {
        Equipe equipe = new Equipe(
                nom,
                coach,
                "Adresse " + extractTeamLabel(nom),
                extractTeamPhone(nom),
                extractTeamEmail(nom),
                "logo-" + extractTeamLabel(nom).toLowerCase() + ".png"
        );
        equipe.setCompetitionCode(competitionCode);
        return equipe;
    }

    private Matchs findByIdMatch(String idMatch) throws SQLException {
        return matchsService.getAll().stream()
                .filter(matchs -> idMatch.equals(matchs.getIdMatch()))
                .findFirst()
                .orElse(null);
    }

    private Equipe findEquipeByName(String nom) throws SQLException {
        return equipeService.getAll().stream()
                .filter(equipe -> nom.equals(equipe.getNom()))
                .findFirst()
                .orElse(null);
    }

    private boolean isTestMatch(Matchs matchs) {
        return matchs.getIdMatch() != null && matchs.getIdMatch().startsWith(TEST_MATCH_PREFIX);
    }

    private boolean isTestEquipe(Equipe equipe) {
        return equipe.getNom() != null && equipe.getNom().startsWith(TEST_EQUIPE_PREFIX);
    }

    private String testIdMatch(String label) {
        return TEST_MATCH_PREFIX + label;
    }

    private String testTeamName(String label) {
        return TEST_EQUIPE_PREFIX + label;
    }

    private String extractTeamLabel(String nom) {
        return nom.substring(TEST_EQUIPE_PREFIX.length());
    }

    private String extractTeamPhone(String nom) {
        return String.valueOf(70000000 + Math.abs(extractTeamLabel(nom).hashCode() % 10000000));
    }

    private String extractTeamEmail(String nom) {
        return extractTeamLabel(nom).toLowerCase() + "@test.com";
    }

    private record TeamPair(Equipe home, Equipe away) {
    }
}

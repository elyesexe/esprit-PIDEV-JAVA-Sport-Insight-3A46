package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;
import tn.esprit.tools.MyConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchFollowTargetServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "FOLLOW_PREF_" + System.currentTimeMillis() + "_";

    private MatchFollowTargetService matchFollowTargetService;
    private UserService userService;
    private EquipeService equipeService;
    private MatchsService matchsService;

    @BeforeEach
    void setUp() throws SQLException {
        matchFollowTargetService = new MatchFollowTargetService();
        userService = new UserService();
        equipeService = new EquipeService();
        matchsService = new MatchsService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        User user = findUserByEmail(userService, emailFor(TEST_PREFIX, "USER"));
        if (user != null && user.getId() != null) {
            deleteRowsForUser(user.getId());
            userService.delete(user.getId());
        }

        for (Matchs match : matchsService.getAll()) {
            if (match.getIdMatch() != null && match.getIdMatch().startsWith(TEST_PREFIX)) {
                matchsService.delete(match.getId());
            }
        }

        for (Equipe equipe : equipeService.getAll()) {
            if (equipe.getNom() != null && equipe.getNom().startsWith(TEST_PREFIX)) {
                equipeService.delete(equipe.getId());
            }
        }
    }

    @Test
    void addListAndRemoveTeamAndCompetitionFavorites() throws SQLException {
        User user = createUser(userService, TEST_PREFIX, "USER", UserRoles.ROLE_USER);
        Equipe realMadrid = createEquipe(equipeService, TEST_PREFIX, "REAL_MADRID", "PD");
        Equipe barcelona = createEquipe(equipeService, TEST_PREFIX, "BARCELONA", "PD");
        Matchs derby = createMatch(realMadrid, barcelona);

        assertTrue(matchFollowTargetService.addTeamFavorite(user.getId(), realMadrid.getId()));
        assertFalse(matchFollowTargetService.addTeamFavorite(user.getId(), realMadrid.getId()));
        assertTrue(matchFollowTargetService.addTeamFavorite(user.getId(), barcelona.getId()));
        assertTrue(matchFollowTargetService.addCompetitionFavorite(user.getId(), "PD"));
        assertFalse(matchFollowTargetService.addCompetitionFavorite(user.getId(), "pd"));
        assertTrue(matchFollowTargetService.addMatchFavorite(user.getId(), derby.getId()));
        assertFalse(matchFollowTargetService.addMatchFavorite(user.getId(), derby.getId()));

        Set<Integer> teamIds = matchFollowTargetService.getFollowedTeamIds(user.getId());
        Set<Integer> matchIds = matchFollowTargetService.getFollowedMatchIds(user.getId());
        Set<String> competitionCodes = matchFollowTargetService.getFollowedCompetitionCodes(user.getId());

        assertEquals(Set.of(realMadrid.getId(), barcelona.getId()), teamIds);
        assertEquals(Set.of(derby.getId()), matchIds);
        assertEquals(Set.of("PD"), competitionCodes);
        assertTrue(matchFollowTargetService.isTeamFavorite(user.getId(), realMadrid.getId()));
        assertTrue(matchFollowTargetService.isMatchFavorite(user.getId(), derby.getId()));
        assertTrue(matchFollowTargetService.isCompetitionFavorite(user.getId(), "PD"));
        assertEquals(4, matchFollowTargetService.getFavoritesByUser(user.getId()).size());

        assertTrue(matchFollowTargetService.removeTeamFavorite(user.getId(), barcelona.getId()));
        assertTrue(matchFollowTargetService.removeMatchFavorite(user.getId(), derby.getId()));
        assertTrue(matchFollowTargetService.removeCompetitionFavorite(user.getId(), "PD"));
        assertFalse(matchFollowTargetService.isTeamFavorite(user.getId(), barcelona.getId()));
        assertFalse(matchFollowTargetService.isMatchFavorite(user.getId(), derby.getId()));
        assertFalse(matchFollowTargetService.isCompetitionFavorite(user.getId(), "PD"));
        assertEquals(List.of(realMadrid.getId()), List.copyOf(matchFollowTargetService.getFollowedTeamIds(user.getId())));
    }

    private Matchs createMatch(Equipe homeTeam, Equipe awayTeam) throws SQLException {
        Matchs match = new Matchs(
                TEST_PREFIX + "DERBY",
                LocalDate.now().plusDays(1),
                LocalTime.of(20, 0),
                "Test stadium",
                "League",
                "Programme",
                "",
                "",
                null,
                null,
                homeTeam.getId(),
                awayTeam.getId()
        );
        match.setCompetitionCode("PD");
        matchsService.add(match);
        return matchsService.getAll().stream()
                .filter(candidate -> TEST_PREFIX.concat("DERBY").equals(candidate.getIdMatch()))
                .findFirst()
                .orElseThrow();
    }

    private void deleteRowsForUser(Integer userId) throws SQLException {
        try (PreparedStatement statement = MyConnection.getInstance().getConnection().prepareStatement(
                "DELETE FROM match_follow_target WHERE user_id = ?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }
}

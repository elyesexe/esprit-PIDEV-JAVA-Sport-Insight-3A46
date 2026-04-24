package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;
import tn.esprit.tools.MyConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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

    @BeforeEach
    void setUp() throws SQLException {
        matchFollowTargetService = new MatchFollowTargetService();
        userService = new UserService();
        equipeService = new EquipeService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        User user = findUserByEmail(userService, emailFor(TEST_PREFIX, "USER"));
        if (user != null && user.getId() != null) {
            deleteRowsForUser(user.getId());
            userService.delete(user.getId());
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

        assertTrue(matchFollowTargetService.addTeamFavorite(user.getId(), realMadrid.getId()));
        assertFalse(matchFollowTargetService.addTeamFavorite(user.getId(), realMadrid.getId()));
        assertTrue(matchFollowTargetService.addTeamFavorite(user.getId(), barcelona.getId()));
        assertTrue(matchFollowTargetService.addCompetitionFavorite(user.getId(), "PD"));
        assertFalse(matchFollowTargetService.addCompetitionFavorite(user.getId(), "pd"));

        Set<Integer> teamIds = matchFollowTargetService.getFollowedTeamIds(user.getId());
        Set<String> competitionCodes = matchFollowTargetService.getFollowedCompetitionCodes(user.getId());

        assertEquals(Set.of(realMadrid.getId(), barcelona.getId()), teamIds);
        assertEquals(Set.of("PD"), competitionCodes);
        assertTrue(matchFollowTargetService.isTeamFavorite(user.getId(), realMadrid.getId()));
        assertTrue(matchFollowTargetService.isCompetitionFavorite(user.getId(), "PD"));
        assertEquals(3, matchFollowTargetService.getFavoritesByUser(user.getId()).size());

        assertTrue(matchFollowTargetService.removeTeamFavorite(user.getId(), barcelona.getId()));
        assertTrue(matchFollowTargetService.removeCompetitionFavorite(user.getId(), "PD"));
        assertFalse(matchFollowTargetService.isTeamFavorite(user.getId(), barcelona.getId()));
        assertFalse(matchFollowTargetService.isCompetitionFavorite(user.getId(), "PD"));
        assertEquals(List.of(realMadrid.getId()), List.copyOf(matchFollowTargetService.getFollowedTeamIds(user.getId())));
    }

    private void deleteRowsForUser(Integer userId) throws SQLException {
        try (PreparedStatement statement = MyConnection.getInstance().getConnection().prepareStatement(
                "DELETE FROM match_follow_target WHERE user_id = ?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }
}

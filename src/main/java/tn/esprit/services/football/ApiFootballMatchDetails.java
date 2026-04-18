package tn.esprit.services.football;

import java.util.List;

public record ApiFootballMatchDetails(
        Long fixtureId,
        String syncedAtIso,
        ApiFootballLineupSide homeLineup,
        ApiFootballLineupSide awayLineup,
        List<ApiFootballStatisticRow> statistics
) {
    public boolean hasLineups() {
        return (homeLineup != null && homeLineup.hasStartingPlayers())
                || (awayLineup != null && awayLineup.hasStartingPlayers());
    }

    public boolean hasStatistics() {
        return statistics != null && !statistics.isEmpty();
    }
}

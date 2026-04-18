package tn.esprit.services.football;

import java.util.List;

public record ApiFootballLineupSide(
        String teamName,
        String formation,
        String coachName,
        List<ApiFootballLineupPlayer> startingPlayers,
        List<ApiFootballLineupPlayer> substitutePlayers
) {
    public boolean hasStartingPlayers() {
        return startingPlayers != null && !startingPlayers.isEmpty();
    }

    public int startingPlayerCount() {
        return startingPlayers == null ? 0 : startingPlayers.size();
    }

    public boolean hasCompleteStartingEleven() {
        return startingPlayerCount() >= 11;
    }
}

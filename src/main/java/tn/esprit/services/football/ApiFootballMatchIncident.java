package tn.esprit.services.football;

public record ApiFootballMatchIncident(
        String incidentType,
        String incidentClass,
        String minuteLabel,
        Integer minute,
        Integer addedTime,
        boolean homeSide,
        String playerName,
        Long playerId,
        String assistPlayerName,
        Long assistPlayerId,
        String playerInName,
        Long playerInId,
        String playerOutName,
        Long playerOutId,
        String reason,
        Integer homeScore,
        Integer awayScore
) {
    public boolean isGoal() {
        return "goal".equalsIgnoreCase(incidentType);
    }

    public boolean isCard() {
        return "card".equalsIgnoreCase(incidentType);
    }

    public boolean isSubstitution() {
        return "substitution".equalsIgnoreCase(incidentType);
    }

    public boolean isYellowCard() {
        return isCard() && "yellow".equalsIgnoreCase(incidentClass);
    }

    public boolean isRedCard() {
        return isCard() && ("red".equalsIgnoreCase(incidentClass) || "yellowRed".equalsIgnoreCase(incidentClass));
    }
}

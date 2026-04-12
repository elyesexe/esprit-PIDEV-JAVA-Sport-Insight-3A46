package tn.esprit.services;

public record FootballDataSyncSummary(
        int competitionsProcessed,
        int teamsUpserted,
        int playersUpserted,
        int playersSkipped,
        int matchesUpserted
) {
    public String toHumanMessage(boolean includePlayers, boolean includeMatches) {
        if (includePlayers && includeMatches) {
            return competitionsProcessed + " competition(s) | "
                    + teamsUpserted + " equipe(s) | "
                    + playersUpserted + " joueur(s) | "
                    + matchesUpserted + " match(s)";
        }
        if (includePlayers) {
            String skippedSuffix = playersSkipped > 0 ? " | " + playersSkipped + " joueur(s) ignores" : "";
            return competitionsProcessed + " competition(s) | "
                    + teamsUpserted + " equipe(s) | "
                    + playersUpserted + " joueur(s)" + skippedSuffix;
        }
        return competitionsProcessed + " competition(s) | " + matchesUpserted + " match(s)";
    }
}

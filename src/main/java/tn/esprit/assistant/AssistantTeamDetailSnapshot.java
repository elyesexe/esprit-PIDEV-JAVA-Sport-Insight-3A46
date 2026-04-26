package tn.esprit.assistant;

import java.util.List;

public record AssistantTeamDetailSnapshot(
        String teamName,
        String subtitle,
        String coachLabel,
        String competitionLabel,
        String playerCountLabel,
        String addressLabel,
        String phoneLabel,
        String emailLabel,
        String sourceLabel,
        List<String> squadSample,
        List<String> topScorers,
        List<String> nextMatches,
        List<String> recentResults,
        String topScorerStatusLabel
) {
    public AssistantTeamDetailSnapshot {
        squadSample = copyOrEmpty(squadSample);
        topScorers = copyOrEmpty(topScorers);
        nextMatches = copyOrEmpty(nextMatches);
        recentResults = copyOrEmpty(recentResults);
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}

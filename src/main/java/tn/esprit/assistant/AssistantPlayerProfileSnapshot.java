package tn.esprit.assistant;

public record AssistantPlayerProfileSnapshot(
        String playerName,
        String subtitle,
        String clubName,
        String numberLabel,
        String birthDateLabel,
        String ageLabel,
        String positionLabel,
        String nationalityLabel,
        String sourceLabel,
        String statsStatusLabel,
        String appearancesLabel,
        String goalsLabel,
        String assistsLabel,
        String yellowCardsLabel,
        String redCardsLabel,
        String minutesLabel,
        String recentFormLabel
) {
}

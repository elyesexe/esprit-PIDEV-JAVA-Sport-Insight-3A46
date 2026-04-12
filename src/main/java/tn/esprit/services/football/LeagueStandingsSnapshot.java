package tn.esprit.services.football;

import java.util.List;

public record LeagueStandingsSnapshot(
        String competitionCode,
        String competitionLabel,
        String areaName,
        String stage,
        String type,
        Integer currentMatchday,
        String seasonStartDate,
        String seasonEndDate,
        List<LeagueStandingEntry> table
) {
    public int clubCount() {
        return table == null ? 0 : table.size();
    }

    public boolean hasFormData() {
        return table != null && table.stream().anyMatch(LeagueStandingEntry::hasForm);
    }
}

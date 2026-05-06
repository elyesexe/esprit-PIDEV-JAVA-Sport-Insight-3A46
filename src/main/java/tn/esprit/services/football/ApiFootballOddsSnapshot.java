package tn.esprit.services.football;

import java.util.List;

public record ApiFootballOddsSnapshot(
        String sourceLabel,
        String stateLabel,
        String statusLabel,
        String updatedAt,
        String message,
        boolean apiBacked,
        boolean locked,
        List<Market> markets,
        GestureInsight gestureInsight
) {
    public boolean hasMarkets() {
        return markets != null && !markets.isEmpty();
    }

    public record Market(
            String name,
            String description,
            List<BookmakerRow> rows
    ) {
    }

    public record BookmakerRow(
            String bookmaker,
            List<Selection> selections
    ) {
    }

    public record Selection(
            String label,
            String odd,
            String trend,
            boolean suspended,
            boolean main
    ) {
    }

    public record GestureInsight(
            String title,
            String body,
            String primaryAction,
            String secondaryAction,
            int confidence,
            String tone
    ) {
    }
}

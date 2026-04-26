package tn.esprit.services.football;

public record ApiFootballLineupPlayer(
        String playerName,
        String shirtNumber,
        String position,
        String grid,
        String photoUrl,
        Long playerId,
        Double rating
) {
    public boolean hasPhoto() {
        return photoUrl != null && !photoUrl.isBlank();
    }

    public boolean hasPosition() {
        return position != null && !position.isBlank();
    }

    public boolean hasGrid() {
        return grid != null && !grid.isBlank();
    }

    public boolean hasRating() {
        return rating != null;
    }
}

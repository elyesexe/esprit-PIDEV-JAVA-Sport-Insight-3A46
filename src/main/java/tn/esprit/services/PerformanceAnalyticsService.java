package tn.esprit.services;

import tn.esprit.entities.Evaluation;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerformanceAnalyticsService {
    private final Connection connection;

    public PerformanceAnalyticsService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Get all evaluations for a specific player ordered by date
     */
    public List<Evaluation> getPlayerEvaluationHistory(Integer playerId) throws SQLException {
        List<Evaluation> evaluations = new ArrayList<>();
        String query = """
            SELECT e.*, entr.date_entrainement 
            FROM evaluation e
            JOIN entrainement entr ON e.entrainement_id = entr.id
            WHERE e.joueur_id = ?
            ORDER BY entr.date_entrainement ASC
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Evaluation eval = new Evaluation();
                eval.setId(rs.getInt("id"));
                eval.setEntrainementId(rs.getInt("entrainement_id"));
                eval.setJoueurId(rs.getInt("joueur_id"));
                eval.setNotePhysique(rs.getDouble("note_physique"));
                eval.setNoteTechnique(rs.getDouble("note_technique"));
                eval.setNoteTactique(rs.getDouble("note_tactique"));
                eval.setCommentaire(rs.getString("commentaire"));
                evaluations.add(eval);
            }
        }

        return evaluations;
    }

    /**
     * Get evaluations within a date range
     */
    public List<Evaluation> getPlayerEvaluationsByDateRange(Integer playerId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Evaluation> evaluations = new ArrayList<>();
        String query = """
            SELECT e.*, entr.date_entrainement 
            FROM evaluation e
            JOIN entrainement entr ON e.entrainement_id = entr.id
            WHERE e.joueur_id = ? 
            AND entr.date_entrainement BETWEEN ? AND ?
            ORDER BY entr.date_entrainement ASC
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, playerId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Evaluation eval = new Evaluation();
                eval.setId(rs.getInt("id"));
                eval.setEntrainementId(rs.getInt("entrainement_id"));
                eval.setJoueurId(rs.getInt("joueur_id"));
                eval.setNotePhysique(rs.getDouble("note_physique"));
                eval.setNoteTechnique(rs.getDouble("note_technique"));
                eval.setNoteTactique(rs.getDouble("note_tactique"));
                eval.setCommentaire(rs.getString("commentaire"));
                evaluations.add(eval);
            }
        }

        return evaluations;
    }

    /**
     * Calculate performance statistics
     */
    public PerformanceStats calculateStats(List<Evaluation> evaluations) {
        if (evaluations.isEmpty()) {
            return new PerformanceStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        double sumPhys = 0, sumTech = 0, sumTact = 0;
        double minPhys = 20, minTech = 20, minTact = 20;
        double maxPhys = 0, maxTech = 0, maxTact = 0;

        for (Evaluation eval : evaluations) {
            sumPhys += eval.getNotePhysique();
            sumTech += eval.getNoteTechnique();
            sumTact += eval.getNoteTactique();

            minPhys = Math.min(minPhys, eval.getNotePhysique());
            minTech = Math.min(minTech, eval.getNoteTechnique());
            minTact = Math.min(minTact, eval.getNoteTactique());

            maxPhys = Math.max(maxPhys, eval.getNotePhysique());
            maxTech = Math.max(maxTech, eval.getNoteTechnique());
            maxTact = Math.max(maxTact, eval.getNoteTactique());
        }

        int count = evaluations.size();
        double avgPhys = sumPhys / count;
        double avgTech = sumTech / count;
        double avgTact = sumTact / count;
        double avgOverall = (avgPhys + avgTech + avgTact) / 3.0;

        return new PerformanceStats(
            avgPhys, avgTech, avgTact, avgOverall,
            minPhys, minTech, minTact,
            maxPhys, maxTech, maxTact
        );
    }

    /**
     * Calculate improvement percentage (comparing first vs last evaluation)
     */
    public ImprovementStats calculateImprovement(List<Evaluation> evaluations) {
        if (evaluations.size() < 2) {
            return new ImprovementStats(0, 0, 0, 0);
        }

        Evaluation first = evaluations.get(0);
        Evaluation last = evaluations.get(evaluations.size() - 1);

        double physImprovement = ((last.getNotePhysique() - first.getNotePhysique()) / first.getNotePhysique()) * 100;
        double techImprovement = ((last.getNoteTechnique() - first.getNoteTechnique()) / first.getNoteTechnique()) * 100;
        double tactImprovement = ((last.getNoteTactique() - first.getNoteTactique()) / first.getNoteTactique()) * 100;
        double overallImprovement = (physImprovement + techImprovement + tactImprovement) / 3.0;

        return new ImprovementStats(physImprovement, techImprovement, tactImprovement, overallImprovement);
    }

    /**
     * Get training attendance rate
     */
    public double getAttendanceRate(Integer playerId) throws SQLException {
        String query = """
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN presence = 'Present' THEN 1 ELSE 0 END) as present
            FROM participation
            WHERE joueur_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int total = rs.getInt("total");
                int present = rs.getInt("present");
                return total > 0 ? (present * 100.0 / total) : 0;
            }
        }

        return 0;
    }

    // Data classes for statistics
    public record PerformanceStats(
        double avgPhysique, double avgTechnique, double avgTactique, double avgOverall,
        double minPhysique, double minTechnique, double minTactique,
        double maxPhysique, double maxTechnique, double maxTactique
    ) {}

    public record ImprovementStats(
        double physiqueImprovement,
        double techniqueImprovement,
        double tactiqueImprovement,
        double overallImprovement
    ) {}
}

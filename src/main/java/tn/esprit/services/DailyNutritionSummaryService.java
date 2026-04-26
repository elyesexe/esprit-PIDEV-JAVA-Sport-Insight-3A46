package tn.esprit.services;

import tn.esprit.entities.DailyNutritionSummary;
import tn.esprit.entities.FoodLog;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyNutritionSummaryService {
    private final Connection connection;
    private final FoodLogService foodLogService;
    
    public DailyNutritionSummaryService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
        this.foodLogService = new FoodLogService();
    }
    
    /**
     * Get or create summary for a specific user and date
     */
    public DailyNutritionSummary getOrCreate(Integer userId, LocalDate date) throws SQLException {
        DailyNutritionSummary summary = getByUserAndDate(userId, date);
        if (summary == null) {
            summary = new DailyNutritionSummary(userId, date);
            add(summary);
        }
        return summary;
    }
    
    /**
     * Recalculate and update summary based on food logs
     */
    public void recalculateSummary(Integer userId, LocalDate date) throws SQLException {
        List<FoodLog> logs = foodLogService.getByUserAndDate(userId, date);
        
        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;
        double totalFiber = 0;
        
        for (FoodLog log : logs) {
            totalCalories += log.getCalories();
            totalProtein += log.getProteinG();
            totalCarbs += log.getCarbsG();
            totalFat += log.getFatG();
            totalFiber += log.getFiberG();
        }
        
        DailyNutritionSummary summary = getOrCreate(userId, date);
        summary.setTotalCalories(totalCalories);
        summary.setTotalProteinG(totalProtein);
        summary.setTotalCarbsG(totalCarbs);
        summary.setTotalFatG(totalFat);
        summary.setTotalFiberG(totalFiber);
        
        update(summary);
    }
    
    private void add(DailyNutritionSummary summary) throws SQLException {
        String query = "INSERT INTO daily_nutrition_summary (user_id, summary_date, total_calories, " +
                      "total_protein_g, total_carbs_g, total_fat_g, total_fiber_g, target_calories) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, summary.getUserId());
            stmt.setDate(2, Date.valueOf(summary.getSummaryDate()));
            stmt.setDouble(3, summary.getTotalCalories());
            stmt.setDouble(4, summary.getTotalProteinG());
            stmt.setDouble(5, summary.getTotalCarbsG());
            stmt.setDouble(6, summary.getTotalFatG());
            stmt.setDouble(7, summary.getTotalFiberG());
            if (summary.getTargetCalories() != null) {
                stmt.setDouble(8, summary.getTargetCalories());
            } else {
                stmt.setNull(8, Types.DOUBLE);
            }
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    summary.setId(rs.getInt(1));
                }
            }
        }
    }
    
    private void update(DailyNutritionSummary summary) throws SQLException {
        String query = "UPDATE daily_nutrition_summary SET total_calories = ?, total_protein_g = ?, " +
                      "total_carbs_g = ?, total_fat_g = ?, total_fiber_g = ?, target_calories = ? " +
                      "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setDouble(1, summary.getTotalCalories());
            stmt.setDouble(2, summary.getTotalProteinG());
            stmt.setDouble(3, summary.getTotalCarbsG());
            stmt.setDouble(4, summary.getTotalFatG());
            stmt.setDouble(5, summary.getTotalFiberG());
            if (summary.getTargetCalories() != null) {
                stmt.setDouble(6, summary.getTargetCalories());
            } else {
                stmt.setNull(6, Types.DOUBLE);
            }
            stmt.setInt(7, summary.getId());
            
            stmt.executeUpdate();
        }
    }
    
    public DailyNutritionSummary getByUserAndDate(Integer userId, LocalDate date) throws SQLException {
        String query = "SELECT * FROM daily_nutrition_summary WHERE user_id = ? AND summary_date = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        
        return null;
    }
    
    public List<DailyNutritionSummary> getByUserAndDateRange(Integer userId, LocalDate startDate, LocalDate endDate) throws SQLException {
        String query = "SELECT * FROM daily_nutrition_summary WHERE user_id = ? AND summary_date BETWEEN ? AND ? ORDER BY summary_date DESC";
        List<DailyNutritionSummary> summaries = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    summaries.add(mapResultSet(rs));
                }
            }
        }
        
        return summaries;
    }
    
    private DailyNutritionSummary mapResultSet(ResultSet rs) throws SQLException {
        DailyNutritionSummary summary = new DailyNutritionSummary();
        summary.setId(rs.getInt("id"));
        summary.setUserId(rs.getInt("user_id"));
        summary.setSummaryDate(rs.getDate("summary_date").toLocalDate());
        summary.setTotalCalories(rs.getDouble("total_calories"));
        summary.setTotalProteinG(rs.getDouble("total_protein_g"));
        summary.setTotalCarbsG(rs.getDouble("total_carbs_g"));
        summary.setTotalFatG(rs.getDouble("total_fat_g"));
        summary.setTotalFiberG(rs.getDouble("total_fiber_g"));
        
        double targetCalories = rs.getDouble("target_calories");
        if (!rs.wasNull()) {
            summary.setTargetCalories(targetCalories);
        }
        
        summary.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return summary;
    }
}

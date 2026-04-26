package tn.esprit.services;

import tn.esprit.entities.FoodLog;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FoodLogService {
    private final Connection connection;
    
    public FoodLogService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }
    
    public void add(FoodLog foodLog) throws SQLException {
        String query = "INSERT INTO food_log (user_id, log_date, meal_type, food_description, " +
                      "calories, protein_g, carbs_g, fat_g, fiber_g, api_response, created_at) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, foodLog.getUserId());
            stmt.setDate(2, Date.valueOf(foodLog.getLogDate()));
            stmt.setString(3, foodLog.getMealType());
            stmt.setString(4, foodLog.getFoodDescription());
            stmt.setDouble(5, foodLog.getCalories());
            stmt.setDouble(6, foodLog.getProteinG());
            stmt.setDouble(7, foodLog.getCarbsG());
            stmt.setDouble(8, foodLog.getFatG());
            stmt.setDouble(9, foodLog.getFiberG());
            stmt.setString(10, foodLog.getApiResponse());
            stmt.setTimestamp(11, Timestamp.valueOf(foodLog.getCreatedAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    foodLog.setId(rs.getInt(1));
                }
            }
        }
    }
    
    public List<FoodLog> getByUserAndDate(Integer userId, LocalDate date) throws SQLException {
        String query = "SELECT * FROM food_log WHERE user_id = ? AND log_date = ? ORDER BY created_at ASC";
        List<FoodLog> logs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(date));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSet(rs));
                }
            }
        }
        
        return logs;
    }
    
    public List<FoodLog> getByUserAndDateRange(Integer userId, LocalDate startDate, LocalDate endDate) throws SQLException {
        String query = "SELECT * FROM food_log WHERE user_id = ? AND log_date BETWEEN ? AND ? ORDER BY log_date DESC, created_at DESC";
        List<FoodLog> logs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSet(rs));
                }
            }
        }
        
        return logs;
    }
    
    public void delete(Integer id) throws SQLException {
        String query = "DELETE FROM food_log WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    private FoodLog mapResultSet(ResultSet rs) throws SQLException {
        FoodLog log = new FoodLog();
        log.setId(rs.getInt("id"));
        log.setUserId(rs.getInt("user_id"));
        log.setLogDate(rs.getDate("log_date").toLocalDate());
        log.setMealType(rs.getString("meal_type"));
        log.setFoodDescription(rs.getString("food_description"));
        log.setCalories(rs.getDouble("calories"));
        log.setProteinG(rs.getDouble("protein_g"));
        log.setCarbsG(rs.getDouble("carbs_g"));
        log.setFatG(rs.getDouble("fat_g"));
        log.setFiberG(rs.getDouble("fiber_g"));
        log.setApiResponse(rs.getString("api_response"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return log;
    }
}

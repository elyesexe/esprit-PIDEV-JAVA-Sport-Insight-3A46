package tn.esprit.services;

import tn.esprit.entities.AiChecklistProgress;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AiChecklistProgressService {
    private final Connection connection;
    
    public AiChecklistProgressService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }
    
    public void add(AiChecklistProgress progress) throws SQLException {
        String query = "INSERT INTO ai_checklist_progress (user_id, plan_type, plan_category, " +
                      "item_text, is_completed, completed_at, created_at) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, progress.getUserId());
            stmt.setString(2, progress.getPlanType());
            stmt.setString(3, progress.getPlanCategory());
            stmt.setString(4, progress.getItemText());
            stmt.setBoolean(5, progress.getIsCompleted());
            if (progress.getCompletedAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(progress.getCompletedAt()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            stmt.setTimestamp(7, Timestamp.valueOf(progress.getCreatedAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    progress.setId(rs.getInt(1));
                }
            }
        }
    }
    
    public void update(AiChecklistProgress progress) throws SQLException {
        String query = "UPDATE ai_checklist_progress SET is_completed = ?, completed_at = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setBoolean(1, progress.getIsCompleted());
            if (progress.getCompletedAt() != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(progress.getCompletedAt()));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }
            stmt.setInt(3, progress.getId());
            
            stmt.executeUpdate();
        }
    }
    
    public List<AiChecklistProgress> getByUserAndPlan(Integer userId, String planType, String planCategory) throws SQLException {
        String query = "SELECT * FROM ai_checklist_progress WHERE user_id = ? AND plan_type = ? AND plan_category = ? ORDER BY created_at ASC";
        List<AiChecklistProgress> progressList = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, planType);
            stmt.setString(3, planCategory);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    progressList.add(mapResultSet(rs));
                }
            }
        }
        
        return progressList;
    }
    
    public AiChecklistProgress findByUserAndItem(Integer userId, String planType, String planCategory, String itemText) throws SQLException {
        String query = "SELECT * FROM ai_checklist_progress WHERE user_id = ? AND plan_type = ? AND plan_category = ? AND item_text = ? LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, planType);
            stmt.setString(3, planCategory);
            stmt.setString(4, itemText);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        
        return null;
    }
    
    public void toggleCompletion(Integer userId, String planType, String planCategory, String itemText, boolean isCompleted) throws SQLException {
        AiChecklistProgress existing = findByUserAndItem(userId, planType, planCategory, itemText);
        
        if (existing == null) {
            // Create new
            existing = new AiChecklistProgress(userId, planType, planCategory, itemText);
            existing.setIsCompleted(isCompleted);
            add(existing);
        } else {
            // Update existing
            existing.setIsCompleted(isCompleted);
            update(existing);
        }
    }
    
    private AiChecklistProgress mapResultSet(ResultSet rs) throws SQLException {
        AiChecklistProgress progress = new AiChecklistProgress();
        progress.setId(rs.getInt("id"));
        progress.setUserId(rs.getInt("user_id"));
        progress.setPlanType(rs.getString("plan_type"));
        progress.setPlanCategory(rs.getString("plan_category"));
        progress.setItemText(rs.getString("item_text"));
        progress.setIsCompleted(rs.getBoolean("is_completed"));
        
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            progress.setCompletedAt(completedAt.toLocalDateTime());
        }
        
        progress.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return progress;
    }
}

package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Manages 6-digit OTP tokens for password reset.
 *
 * Token lifecycle:
 *   1. createToken(userId)  — generates token, saves to DB, returns the code
 *   2. validateToken(email, code) — checks the token is valid, not expired, not used
 *   3. markUsed(token)      — marks token as consumed so it cannot be reused
 *   4. purgeExpired()       — housekeeping (call on app start or scheduled)
 */
public class PasswordResetService {

    private static final int    TOKEN_EXPIRY_MINUTES = 15;
    private static final Random RNG                  = new Random();

    private final Connection connection;

    public PasswordResetService() throws SQLException {
        this.connection = MyConnection.getInstance().getConnection();
    }

    // ── Token creation ────────────────────────────────────────────────────────

    /**
     * Generate a new 6-digit OTP for the given user and persist it.
     * Any previous unused tokens for the same user are invalidated first.
     *
     * @param userId DB id of the user
     * @return the 6-digit OTP string (e.g. "048291")
     */
    public String createToken(int userId) throws SQLException {
        // 1. Invalidate old tokens for this user
        invalidatePrevious(userId);

        // 2. Generate the 6-digit string
        String otp = generateOtp();

        // 3. Use MySQL's DATE_ADD and NOW() to handle the expiry.
        // This ensures the 'expires_at' is always relative to the DB's own clock.
        String sql = "INSERT INTO password_reset_token (user_id, token, expires_at) " +
                "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE))";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, otp);
            // Note: We no longer need to pass the 'exp' timestamp from Java!
            ps.executeUpdate();
        }

        return otp;
    }
    // ── Token validation ──────────────────────────────────────────────────────

    /**
     * Check that a token is valid for the given email.
     *
     * @param email user's email address
     * @param otp   the code the user typed
     * @return the user_id if valid, or -1 if invalid / expired / already used
     */
    public int validateToken(String email, String otp) throws SQLException {
        String sql = """
        SELECT t.user_id
        FROM password_reset_token t
        JOIN `user` u ON u.id = t.user_id
        WHERE LOWER(u.email) = LOWER(?)
          AND t.token = ?
          AND t.used = 0
          AND t.expires_at > NOW()
        LIMIT 1
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, otp);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("user_id");
            }
        }
        return -1;
    }
    /**
     * Mark a token as used so it cannot be reused.
     * Call this immediately after a successful password update.
     */
    public void markUsed(String email, String otp) throws SQLException {
        String sql = """
            UPDATE password_reset_token t
            JOIN `user` u ON u.id = t.user_id
            SET t.used = 1
            WHERE LOWER(u.email) = LOWER(?) AND t.token = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, otp);
            ps.executeUpdate();
        }
    }

    // ── Housekeeping ──────────────────────────────────────────────────────────

    /** Delete tokens that have already expired — call on app startup. */
    public void purgeExpired() throws SQLException {
        String sql = "DELETE FROM password_reset_token WHERE expires_at < NOW() OR used = 1";
        try (Statement st = connection.createStatement()) {
            int deleted = st.executeUpdate(sql);
            if (deleted > 0) System.out.println("[PasswordReset] Purged " + deleted + " expired token(s).");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void invalidatePrevious(int userId) throws SQLException {
        String sql = "UPDATE password_reset_token SET used = 1 WHERE user_id = ? AND used = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private String generateOtp() {
        // Zero-padded 6-digit number: 000000 – 999999
        return String.format("%06d", RNG.nextInt(1_000_000));
    }
}

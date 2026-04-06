package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection cnx;

    {
        try {
            cnx = MyConnection.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // ── ADD ───────────────────────────────────────────────────────────────
    public void addUser(User u) throws SQLException {
        String sql = "INSERT INTO user (email, roles, password, nom, prenom, telephone, " +
                "date_naissance, photo, statut, date_inscription, cv_name, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1,  u.getEmail());
        ps.setString(2,  u.getRoles());
        ps.setString(3,  u.getPassword());
        ps.setString(4,  u.getNom());
        ps.setString(5,  u.getPrenom());
        ps.setString(6,  u.getTelephone());
        ps.setDate(7,    u.getDateNaissance()    != null ? Date.valueOf(u.getDateNaissance())           : null);
        ps.setString(8,  u.getPhoto());
        ps.setString(9,  u.getStatut());
        ps.setTimestamp(10, u.getDateInscription() != null ? Timestamp.valueOf(u.getDateInscription()) : null);
        ps.setString(11, u.getCvName());
        ps.setTimestamp(12, u.getUpdatedAt()       != null ? Timestamp.valueOf(u.getUpdatedAt())       : null);
        ps.executeUpdate();
        System.out.println("✅ User added: " + u.getEmail());
    }

    // ── GET ALL ───────────────────────────────────────────────────────────
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            users.add(mapRow(rs));
        }
        return users;
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────
    public User findUserById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    public void updateUser(User u) {
        String sql = "UPDATE user SET email=?, roles=?, password=?, nom=?, prenom=?, telephone=?, " +
                "date_naissance=?, photo=?, statut=?, date_inscription=?, cv_name=?, updated_at=? " +
                "WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1,  u.getEmail());
            ps.setString(2,  u.getRoles());
            ps.setString(3,  u.getPassword());
            ps.setString(4,  u.getNom());
            ps.setString(5,  u.getPrenom());
            ps.setString(6,  u.getTelephone());
            ps.setDate(7,    u.getDateNaissance()    != null ? Date.valueOf(u.getDateNaissance())           : null);
            ps.setString(8,  u.getPhoto());
            ps.setString(9,  u.getStatut());
            ps.setTimestamp(10, u.getDateInscription() != null ? Timestamp.valueOf(u.getDateInscription()) : null);
            ps.setString(11, u.getCvName());
            ps.setTimestamp(12, u.getUpdatedAt()       != null ? Timestamp.valueOf(u.getUpdatedAt())       : null);
            ps.setInt(13, u.getId());
            ps.executeUpdate();
            System.out.println("✅ User updated: id=" + u.getId());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    public void deleteUser(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ User deleted: id=" + id);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── HELPER ────────────────────────────────────────────────────────────
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setRoles(rs.getString("roles"));
        u.setPassword(rs.getString("password"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setTelephone(rs.getString("telephone"));
        Date d = rs.getDate("date_naissance");
        if (d != null) u.setDateNaissance(d.toLocalDate());
        u.setPhoto(rs.getString("photo"));
        u.setStatut(rs.getString("statut"));
        Timestamp ti = rs.getTimestamp("date_inscription");
        if (ti != null) u.setDateInscription(ti.toLocalDateTime());
        u.setCvName(rs.getString("cv_name"));
        Timestamp tu = rs.getTimestamp("updated_at");
        if (tu != null) u.setUpdatedAt(tu.toLocalDateTime());
        return u;
    }
}
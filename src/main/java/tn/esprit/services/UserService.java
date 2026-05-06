package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.security.PasswordSupport;
import tn.esprit.security.UserRoles;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {
    private final Connection connection;

    public UserService() {
        try {
            this.connection = MyConnection.getInstance().getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Error connecting to database", e);
        }
    }

    @Override
    public void add(User user) throws SQLException {
<<<<<<< HEAD
        // face_registered added as column 13
        String query = "INSERT INTO `user` (email, roles, password, nom, prenom, telephone, date_naissance, photo, statut, date_inscription, cv_name, updated_at, face_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
=======
        String query = "INSERT INTO `user` (email, roles, password, nom, prenom, telephone, date_naissance, photo, statut, date_inscription, cv_name, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(statement, user);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }
            System.out.println("User added successfully.");
        }
    }

    @Override
    public void update(User user) throws SQLException {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id is required for updates.");
        }
<<<<<<< HEAD
        // face_registered added as column 13, WHERE id = ? shifted to param 14
        String query = "UPDATE `user` SET email = ?, roles = ?, password = ?, nom = ?, prenom = ?, telephone = ?, date_naissance = ?, photo = ?, statut = ?, date_inscription = ?, cv_name = ?, updated_at = ?, face_registered = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, user);
            statement.setInt(14, user.getId());   // shifted from 13 → 14
=======
        String query = "UPDATE `user` SET email = ?, roles = ?, password = ?, nom = ?, prenom = ?, telephone = ?, date_naissance = ?, photo = ?, statut = ?, date_inscription = ?, cv_name = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, user);
            statement.setInt(13, user.getId());
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            statement.executeUpdate();
            System.out.println("User updated successfully.");
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM `user` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            System.out.println("User deleted successfully.");
        }
    }

    @Override
    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM `user`";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }
        }
        return users;
    }

    @Override
    public User getById(int id) throws SQLException {
        String query = "SELECT * FROM `user` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<User> search(String keyword) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM `user` WHERE LOWER(email) LIKE LOWER(?) OR LOWER(roles) LIKE LOWER(?) OR LOWER(nom) LIKE LOWER(?) OR LOWER(prenom) LIKE LOWER(?) OR LOWER(telephone) LIKE LOWER(?) OR LOWER(statut) LIKE LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            String pattern = "%" + keyword + "%";
            for (int i = 1; i <= 6; i++) {
                statement.setString(i, pattern);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapRow(resultSet));
                }
            }
        }
        return users;
    }

    public User findByEmail(String email) throws SQLException {
        String query = "SELECT * FROM `user` WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        }
        return null;
    }

    public User authenticate(String email, String plainPassword) throws SQLException {
        User user = findByEmail(email);
        if (user == null) {
            return null;
        }
        if (!user.isActiveAccount()) {
            return null;
        }
        return PasswordSupport.matches(plainPassword, user.getPassword()) ? user : null;
    }

    public boolean emailExists(String email, Integer excludedUserId) throws SQLException {
        String query = excludedUserId == null
                ? "SELECT 1 FROM `user` WHERE LOWER(email) = LOWER(?) LIMIT 1"
                : "SELECT 1 FROM `user` WHERE LOWER(email) = LOWER(?) AND id <> ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            if (excludedUserId != null) {
                statement.setInt(2, excludedUserId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

<<<<<<< HEAD
    // ── fillStatement — param 13 added for face_registered ───────────────────

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private void fillStatement(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getEmail());
        statement.setString(2, normalizeRolesForStorage(user));
        statement.setString(3, PasswordSupport.prepareForStorage(user.getPassword()));
        statement.setString(4, user.getNom());
        statement.setString(5, user.getPrenom());
        statement.setString(6, user.getTelephone());
        statement.setDate(7, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
        statement.setString(8, user.getPhoto());
        statement.setString(9, normalizeStatusForStorage(user.getStatut()));
        statement.setTimestamp(10, user.getDateInscription() != null ? Timestamp.valueOf(user.getDateInscription()) : Timestamp.valueOf(LocalDateTime.now()));
        statement.setString(11, user.getCvName());
        statement.setTimestamp(12, user.getUpdatedAt() != null ? Timestamp.valueOf(user.getUpdatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
<<<<<<< HEAD
        statement.setBoolean(13, user.isFaceRegistered());   // NEW — param 13
    }

    // ── mapRow — face_registered read from ResultSet ──────────────────────────

=======
    }

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    private User mapRow(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setEmail(resultSet.getString("email"));
        user.setRoles(resultSet.getString("roles"));
        user.setPassword(resultSet.getString("password"));
        user.setNom(resultSet.getString("nom"));
        user.setPrenom(resultSet.getString("prenom"));
        user.setTelephone(resultSet.getString("telephone"));

        Date dateNaissance = resultSet.getDate("date_naissance");
        if (dateNaissance != null) {
            user.setDateNaissance(dateNaissance.toLocalDate());
        }

        user.setPhoto(resultSet.getString("photo"));
        user.setStatut(resultSet.getString("statut"));

        Timestamp dateInscription = resultSet.getTimestamp("date_inscription");
        if (dateInscription != null) {
            user.setDateInscription(dateInscription.toLocalDateTime());
        }

        user.setCvName(resultSet.getString("cv_name"));

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

<<<<<<< HEAD
        user.setFaceRegistered(resultSet.getBoolean("face_registered"));   // NEW

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        return user;
    }

    private String normalizeRolesForStorage(User user) {
        if (user == null) {
            return UserRoles.toDatabaseValue(List.of(UserRoles.ROLE_USER));
        }
        return UserRoles.toDatabaseValue(user.getRoleList());
    }

    private String normalizeStatusForStorage(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        return status.trim();
    }
}

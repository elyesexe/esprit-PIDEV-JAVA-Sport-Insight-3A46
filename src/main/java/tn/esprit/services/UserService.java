package tn.esprit.services;

import tn.esprit.entities.User;
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
        String query = "INSERT INTO `user` (email, roles, password, nom, prenom, telephone, date_naissance, photo, statut, date_inscription, cv_name, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, user);
            statement.executeUpdate();
            System.out.println("User added successfully.");
        }
    }

    @Override
    public void update(User user) throws SQLException {
        String query = "UPDATE `user` SET email = ?, roles = ?, password = ?, nom = ?, prenom = ?, telephone = ?, date_naissance = ?, photo = ?, statut = ?, date_inscription = ?, cv_name = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            fillStatement(statement, user);
            statement.setInt(13, user.getId());
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

    private void fillStatement(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getEmail());
        statement.setString(2, user.getRoles());
        statement.setString(3, user.getPassword());
        statement.setString(4, user.getNom());
        statement.setString(5, user.getPrenom());
        statement.setString(6, user.getTelephone());
        statement.setDate(7, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
        statement.setString(8, user.getPhoto());
        statement.setString(9, user.getStatut());
        statement.setTimestamp(10, user.getDateInscription() != null ? Timestamp.valueOf(user.getDateInscription()) : Timestamp.valueOf(LocalDateTime.now()));
        statement.setString(11, user.getCvName());
        statement.setTimestamp(12, user.getUpdatedAt() != null ? Timestamp.valueOf(user.getUpdatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
    }

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

        return user;
    }
}

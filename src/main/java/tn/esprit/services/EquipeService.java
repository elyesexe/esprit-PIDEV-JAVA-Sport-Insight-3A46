package tn.esprit.services;

import tn.esprit.entities.Equipe;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipeService implements IService<Equipe> {
    private Connection connection;

    public EquipeService() {
        try {
            this.connection = MyConnection.getInstance().getConnection();
        } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e.getMessage());
        }
    }

    @Override
    public void add(Equipe equipe) throws SQLException {
        String query = "INSERT INTO equipe (nom, coach, adresse, telephone, email, image) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, equipe.getNom());
            statement.setString(2, equipe.getCoach());
            statement.setString(3, equipe.getAdresse());
            statement.setString(4, equipe.getTelephone());
            statement.setString(5, equipe.getEmail());
            statement.setString(6, equipe.getImage());
            statement.executeUpdate();
            System.out.println("Equipe added successfully!");
        }
    }

    @Override
    public void update(Equipe equipe) throws SQLException {
        String query = "UPDATE equipe SET nom = ?, coach = ?, adresse = ?, telephone = ?, email = ?, image = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, equipe.getNom());
            statement.setString(2, equipe.getCoach());
            statement.setString(3, equipe.getAdresse());
            statement.setString(4, equipe.getTelephone());
            statement.setString(5, equipe.getEmail());
            statement.setString(6, equipe.getImage());
            statement.setInt(7, equipe.getId());
            statement.executeUpdate();
            System.out.println("Equipe updated successfully!");
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM equipe WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            System.out.println("Equipe deleted successfully!");
        }
    }

    @Override
    public List<Equipe> getAll() throws SQLException {
        List<Equipe> equipes = new ArrayList<>();
        String query = "SELECT * FROM equipe";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                Equipe equipe = new Equipe();
                equipe.setId(resultSet.getInt("id"));
                equipe.setNom(resultSet.getString("nom"));
                equipe.setCoach(resultSet.getString("coach"));
                equipe.setAdresse(resultSet.getString("adresse"));
                equipe.setTelephone(resultSet.getString("telephone"));
                equipe.setEmail(resultSet.getString("email"));
                equipe.setImage(resultSet.getString("image"));
                equipes.add(equipe);
            }
        }
        return equipes;
    }

    @Override
    public Equipe getById(int id) throws SQLException {
        String query = "SELECT * FROM equipe WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Equipe equipe = new Equipe();
                    equipe.setId(resultSet.getInt("id"));
                    equipe.setNom(resultSet.getString("nom"));
                    equipe.setCoach(resultSet.getString("coach"));
                    equipe.setAdresse(resultSet.getString("adresse"));
                    equipe.setTelephone(resultSet.getString("telephone"));
                    equipe.setEmail(resultSet.getString("email"));
                    equipe.setImage(resultSet.getString("image"));
                    return equipe;
                }
            }
        }
        return null;
    }

    @Override
    public List<Equipe> search(String keyword) throws SQLException {
        List<Equipe> equipes = new ArrayList<>();
        String query = "SELECT * FROM equipe WHERE LOWER(nom) LIKE LOWER(?) OR LOWER(coach) LIKE LOWER(?) OR LOWER(adresse) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            statement.setString(3, searchPattern);
            statement.setString(4, searchPattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Equipe equipe = new Equipe();
                    equipe.setId(resultSet.getInt("id"));
                    equipe.setNom(resultSet.getString("nom"));
                    equipe.setCoach(resultSet.getString("coach"));
                    equipe.setAdresse(resultSet.getString("adresse"));
                    equipe.setTelephone(resultSet.getString("telephone"));
                    equipe.setEmail(resultSet.getString("email"));
                    equipe.setImage(resultSet.getString("image"));
                    equipes.add(equipe);
                }
            }
        }
        return equipes;
    }
}

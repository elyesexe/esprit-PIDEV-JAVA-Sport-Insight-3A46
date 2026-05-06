package tn.esprit.services;

import tn.esprit.entities.Sponsor;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SponsorService implements IService<Sponsor> {
    private final Connection connection;

    public SponsorService() {
        try {
            this.connection = MyConnection.getInstance().getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Error connecting to database", e);
        }
    }

    @Override
    public void add(Sponsor sponsor) throws SQLException {
        String query = "INSERT INTO sponsor (nom, email, telephone, budget, logo_name, updated_at, adresse) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, sponsor.getNom());
            statement.setString(2, sponsor.getEmail());
            statement.setString(3, sponsor.getTelephone());
            statement.setDouble(4, sponsor.getBudget());
            statement.setString(5, sponsor.getLogoName());
            statement.setObject(6, sponsor.getUpdatedAt());
            statement.setString(7, sponsor.getAdresse());
            statement.executeUpdate();
            System.out.println("Sponsor added successfully.");
        }
    }

    @Override
    public void update(Sponsor sponsor) throws SQLException {
        String query = "UPDATE sponsor SET nom = ?, email = ?, telephone = ?, budget = ?, logo_name = ?, updated_at = ?, adresse = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, sponsor.getNom());
            statement.setString(2, sponsor.getEmail());
            statement.setString(3, sponsor.getTelephone());
            statement.setDouble(4, sponsor.getBudget());
            statement.setString(5, sponsor.getLogoName());
            statement.setObject(6, LocalDateTime.now());
            statement.setString(7, sponsor.getAdresse());
            statement.setInt(8, sponsor.getId());
            statement.executeUpdate();
            System.out.println("Sponsor updated successfully.");
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM sponsor WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            System.out.println("Sponsor deleted successfully.");
        }
    }

    @Override
    public List<Sponsor> getAll() throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM sponsor";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                sponsors.add(mapResultSetToSponsor(resultSet));
            }
        }
        return sponsors;
    }

    public PaginationResult<Sponsor> getPage(int page, int pageSize) throws SQLException {
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, pageSize);
        long totalItems = countSponsors();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / safePageSize);

        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM sponsor ORDER BY budget DESC, nom ASC LIMIT ? OFFSET ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, safePageSize);
            statement.setInt(2, safePage * safePageSize);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sponsors.add(mapResultSetToSponsor(resultSet));
                }
            }
        }
        return new PaginationResult<>(sponsors, safePage, safePageSize, totalItems, totalPages);
    }

    public long countSponsors() throws SQLException {
        String query = "SELECT COUNT(*) FROM sponsor";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }

    @Override
    public Sponsor getById(int id) throws SQLException {
        String query = "SELECT * FROM sponsor WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToSponsor(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<Sponsor> search(String keyword) throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM sponsor WHERE LOWER(nom) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) OR LOWER(telephone) LIKE LOWER(?) OR LOWER(adresse) LIKE LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            statement.setString(3, searchPattern);
            statement.setString(4, searchPattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sponsors.add(mapResultSetToSponsor(resultSet));
                }
            }
        }
        return sponsors;
    }

    public List<Sponsor> searchByName(String nom) throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM sponsor WHERE LOWER(nom) LIKE LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "%" + nom + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sponsors.add(mapResultSetToSponsor(resultSet));
                }
            }
        }
        return sponsors;
    }

    public List<Sponsor> searchByMinBudget(double minBudget) throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM sponsor WHERE budget >= ? ORDER BY budget DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDouble(1, minBudget);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sponsors.add(mapResultSetToSponsor(resultSet));
                }
            }
        }
        return sponsors;
    }

    public List<Sponsor> searchByMaxBudget(double maxBudget) throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM sponsor WHERE budget <= ? ORDER BY budget DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDouble(1, maxBudget);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sponsors.add(mapResultSetToSponsor(resultSet));
                }
            }
        }
        return sponsors;
    }

    public List<Sponsor> searchByBudgetRange(double minBudget, double maxBudget) throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>();
        String query = "SELECT * FROM sponsor WHERE budget BETWEEN ? AND ? ORDER BY budget DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDouble(1, minBudget);
            statement.setDouble(2, maxBudget);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sponsors.add(mapResultSetToSponsor(resultSet));
                }
            }
        }
        return sponsors;
    }

    private Sponsor mapResultSetToSponsor(ResultSet resultSet) throws SQLException {
        Sponsor sponsor = new Sponsor();
        sponsor.setId(resultSet.getInt("id"));
        sponsor.setNom(resultSet.getString("nom"));
        sponsor.setEmail(resultSet.getString("email"));
        sponsor.setTelephone(resultSet.getString("telephone"));
        sponsor.setBudget(resultSet.getDouble("budget"));
        sponsor.setLogoName(resultSet.getString("logo_name"));
        sponsor.setUpdatedAt(resultSet.getObject("updated_at", LocalDateTime.class));
        sponsor.setAdresse(resultSet.getString("adresse"));
        return sponsor;
    }
}

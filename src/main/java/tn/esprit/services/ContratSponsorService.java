package tn.esprit.services;

import tn.esprit.entities.ContratSponsor;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContratSponsorService implements IService<ContratSponsor> {
    private Connection connection;

    public ContratSponsorService() {
        try {
            this.connection = MyConnection.getInstance().getConnection();
        } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e.getMessage());
        }
    }

    @Override
    public void add(ContratSponsor contrat) throws SQLException {
        String query = "INSERT INTO contrat_sponsor (date_debut, date_fin, montant, description, statut, notified, statut_paiement, sponsor_id, equipe_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setObject(1, contrat.getDateDebut());
            statement.setObject(2, contrat.getDateFin());
            statement.setDouble(3, contrat.getMontant());
            statement.setString(4, contrat.getDescription());
            statement.setString(5, contrat.getStatut());
            statement.setBoolean(6, contrat.isNotified());
            statement.setString(7, contrat.getStatutPaiement());
            statement.setInt(8, contrat.getSponsorId());
            statement.setInt(9, contrat.getEquipeId());
            statement.executeUpdate();
            System.out.println("Contrat Sponsor added successfully!");
        }
    }

    @Override
    public void update(ContratSponsor contrat) throws SQLException {
        String query = "UPDATE contrat_sponsor SET date_debut = ?, date_fin = ?, montant = ?, description = ?, statut = ?, notified = ?, statut_paiement = ?, sponsor_id = ?, equipe_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setObject(1, contrat.getDateDebut());
            statement.setObject(2, contrat.getDateFin());
            statement.setDouble(3, contrat.getMontant());
            statement.setString(4, contrat.getDescription());
            statement.setString(5, contrat.getStatut());
            statement.setBoolean(6, contrat.isNotified());
            statement.setString(7, contrat.getStatutPaiement());
            statement.setInt(8, contrat.getSponsorId());
            statement.setInt(9, contrat.getEquipeId());
            statement.setInt(10, contrat.getId());
            statement.executeUpdate();
            System.out.println("Contrat Sponsor updated successfully!");
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM contrat_sponsor WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            System.out.println("Contrat Sponsor deleted successfully!");
        }
    }

    @Override
    public List<ContratSponsor> getAll() throws SQLException {
        List<ContratSponsor> contrats = new ArrayList<>();
        String query = "SELECT * FROM contrat_sponsor";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                ContratSponsor contrat = new ContratSponsor();
                contrat.setId(resultSet.getInt("id"));
                contrat.setDateDebut(resultSet.getObject("date_debut", java.sql.Date.class).toLocalDate());
                contrat.setDateFin(resultSet.getObject("date_fin", java.sql.Date.class).toLocalDate());
                contrat.setMontant(resultSet.getDouble("montant"));
                contrat.setDescription(resultSet.getString("description"));
                contrat.setStatut(resultSet.getString("statut"));
                contrat.setNotified(resultSet.getBoolean("notified"));
                contrat.setStatutPaiement(resultSet.getString("statut_paiement"));
                contrat.setSponsorId(resultSet.getInt("sponsor_id"));
                contrat.setEquipeId(resultSet.getInt("equipe_id"));
                contrats.add(contrat);
            }
        }
        return contrats;
    }

    @Override
    public ContratSponsor getById(int id) throws SQLException {
        String query = "SELECT * FROM contrat_sponsor WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    ContratSponsor contrat = new ContratSponsor();
                    contrat.setId(resultSet.getInt("id"));
                    contrat.setDateDebut(resultSet.getObject("date_debut", java.sql.Date.class).toLocalDate());
                    contrat.setDateFin(resultSet.getObject("date_fin", java.sql.Date.class).toLocalDate());
                    contrat.setMontant(resultSet.getDouble("montant"));
                    contrat.setDescription(resultSet.getString("description"));
                    contrat.setStatut(resultSet.getString("statut"));
                    contrat.setNotified(resultSet.getBoolean("notified"));
                    contrat.setStatutPaiement(resultSet.getString("statut_paiement"));
                    contrat.setSponsorId(resultSet.getInt("sponsor_id"));
                    contrat.setEquipeId(resultSet.getInt("equipe_id"));
                    return contrat;
                }
            }
        }
        return null;
    }

    @Override
    public List<ContratSponsor> search(String keyword) throws SQLException {
        List<ContratSponsor> contrats = new ArrayList<>();
        String query = "SELECT * FROM contrat_sponsor WHERE LOWER(description) LIKE LOWER(?) OR LOWER(statut) LIKE LOWER(?) OR LOWER(statut_paiement) LIKE LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            statement.setString(3, searchPattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ContratSponsor contrat = new ContratSponsor();
                    contrat.setId(resultSet.getInt("id"));
                    contrat.setDateDebut(resultSet.getObject("date_debut", java.sql.Date.class).toLocalDate());
                    contrat.setDateFin(resultSet.getObject("date_fin", java.sql.Date.class).toLocalDate());
                    contrat.setMontant(resultSet.getDouble("montant"));
                    contrat.setDescription(resultSet.getString("description"));
                    contrat.setStatut(resultSet.getString("statut"));
                    contrat.setNotified(resultSet.getBoolean("notified"));
                    contrat.setStatutPaiement(resultSet.getString("statut_paiement"));
                    contrat.setSponsorId(resultSet.getInt("sponsor_id"));
                    contrat.setEquipeId(resultSet.getInt("equipe_id"));
                    contrats.add(contrat);
                }
            }
        }
        return contrats;
    }

    public List<ContratSponsor> searchBySponsorId(int sponsorId) throws SQLException {
        List<ContratSponsor> contrats = new ArrayList<>();
        String query = "SELECT * FROM contrat_sponsor WHERE sponsor_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, sponsorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ContratSponsor contrat = new ContratSponsor();
                    contrat.setId(resultSet.getInt("id"));
                    contrat.setDateDebut(resultSet.getObject("date_debut", java.sql.Date.class).toLocalDate());
                    contrat.setDateFin(resultSet.getObject("date_fin", java.sql.Date.class).toLocalDate());
                    contrat.setMontant(resultSet.getDouble("montant"));
                    contrat.setDescription(resultSet.getString("description"));
                    contrat.setStatut(resultSet.getString("statut"));
                    contrat.setNotified(resultSet.getBoolean("notified"));
                    contrat.setStatutPaiement(resultSet.getString("statut_paiement"));
                    contrat.setSponsorId(resultSet.getInt("sponsor_id"));
                    contrat.setEquipeId(resultSet.getInt("equipe_id"));
                    contrats.add(contrat);
                }
            }
        }
        return contrats;
    }

    // Recherche par montant minimum
    public List<ContratSponsor> searchByMinMontant(double minMontant) throws SQLException {
        List<ContratSponsor> contrats = new ArrayList<>();
        String query = "SELECT * FROM contrat_sponsor WHERE montant >= ? ORDER BY montant DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDouble(1, minMontant);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ContratSponsor contrat = mapResultSetToContrat(resultSet);
                    contrats.add(contrat);
                }
            }
        }
        return contrats;
    }

    // Recherche par montant maximum
    public List<ContratSponsor> searchByMaxMontant(double maxMontant) throws SQLException {
        List<ContratSponsor> contrats = new ArrayList<>();
        String query = "SELECT * FROM contrat_sponsor WHERE montant <= ? ORDER BY montant DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDouble(1, maxMontant);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ContratSponsor contrat = mapResultSetToContrat(resultSet);
                    contrats.add(contrat);
                }
            }
        }
        return contrats;
    }

    // Recherche par plage de montant
    public List<ContratSponsor> searchByMontantRange(double minMontant, double maxMontant) throws SQLException {
        List<ContratSponsor> contrats = new ArrayList<>();
        String query = "SELECT * FROM contrat_sponsor WHERE montant BETWEEN ? AND ? ORDER BY montant DESC";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDouble(1, minMontant);
            statement.setDouble(2, maxMontant);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ContratSponsor contrat = mapResultSetToContrat(resultSet);
                    contrats.add(contrat);
                }
            }
        }
        return contrats;
    }

    // Recherche par statut
    public List<ContratSponsor> searchByStatut(String statut) throws SQLException {
        List<ContratSponsor> contrats = new ArrayList<>();
        String query = "SELECT * FROM contrat_sponsor WHERE LOWER(statut) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, statut);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ContratSponsor contrat = mapResultSetToContrat(resultSet);
                    contrats.add(contrat);
                }
            }
        }
        return contrats;
    }

    // Recherche par statut de paiement
    public List<ContratSponsor> searchByStatutPaiement(String statutPaiement) throws SQLException {
        List<ContratSponsor> contrats = new ArrayList<>();
        String query = "SELECT * FROM contrat_sponsor WHERE LOWER(statut_paiement) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, statutPaiement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ContratSponsor contrat = mapResultSetToContrat(resultSet);
                    contrats.add(contrat);
                }
            }
        }
        return contrats;
    }

    // Méthode utilitaire pour mapper ResultSet vers ContratSponsor
    private ContratSponsor mapResultSetToContrat(ResultSet resultSet) throws SQLException {
        ContratSponsor contrat = new ContratSponsor();
        contrat.setId(resultSet.getInt("id"));
        contrat.setDateDebut(resultSet.getObject("date_debut", java.sql.Date.class).toLocalDate());
        contrat.setDateFin(resultSet.getObject("date_fin", java.sql.Date.class).toLocalDate());
        contrat.setMontant(resultSet.getDouble("montant"));
        contrat.setDescription(resultSet.getString("description"));
        contrat.setStatut(resultSet.getString("statut"));
        contrat.setNotified(resultSet.getBoolean("notified"));
        contrat.setStatutPaiement(resultSet.getString("statut_paiement"));
        contrat.setSponsorId(resultSet.getInt("sponsor_id"));
        contrat.setEquipeId(resultSet.getInt("equipe_id"));
        return contrat;
    }
}

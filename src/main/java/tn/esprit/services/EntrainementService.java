package tn.esprit.services;

import tn.esprit.entities.Entrainement;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EntrainementService implements IService<Entrainement> {

    private final Connection con;

    public EntrainementService() throws SQLException {
        con = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Entrainement e) throws SQLException {
        String sql = "INSERT INTO entrainement (date_entrainement, heure_debut, heure_fin, type, objectif, lieu, entraineur_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setDate(1, Date.valueOf(e.getDateEntrainement()));
        ps.setTime(2, Time.valueOf(e.getHeureDebut()));
        ps.setTime(3, Time.valueOf(e.getHeureFin()));
        ps.setString(4, e.getType());
        ps.setString(5, e.getObjectif());
        ps.setString(6, e.getLieu());
        ps.setInt(7, e.getEntraineurId());
        ps.executeUpdate();
        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            e.setId(generatedKeys.getInt(1));
        }
        System.out.println("Entrainement ajouté avec succès ! ID=" + e.getId());
    }

    @Override
    public void update(Entrainement e) throws SQLException {
        String sql = "UPDATE entrainement SET date_entrainement=?, heure_debut=?, heure_fin=?, " +
                "type=?, objectif=?, lieu=?, entraineur_id=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(e.getDateEntrainement()));
        ps.setTime(2, Time.valueOf(e.getHeureDebut()));
        ps.setTime(3, Time.valueOf(e.getHeureFin()));
        ps.setString(4, e.getType());
        ps.setString(5, e.getObjectif());
        ps.setString(6, e.getLieu());
        ps.setInt(7, e.getEntraineurId());
        ps.setInt(8, e.getId());
        ps.executeUpdate();
        System.out.println("Entrainement mis à jour avec succès !");
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM entrainement WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Entrainement supprimé avec succès !");
    }

    @Override
    public List<Entrainement> getAll() throws SQLException {
        List<Entrainement> list = new ArrayList<>();
        String sql = "SELECT * FROM entrainement";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public Entrainement getById(int id) throws SQLException {
        String sql = "SELECT * FROM entrainement WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    public List<Entrainement> search(String keyword) throws SQLException {
        List<Entrainement> list = new ArrayList<>();
        String sql = "SELECT * FROM entrainement WHERE type LIKE ? OR lieu LIKE ? OR objectif LIKE ?";
        PreparedStatement ps = con.prepareStatement(sql);
        String kw = "%" + keyword + "%";
        ps.setString(1, kw);
        ps.setString(2, kw);
        ps.setString(3, kw);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public List<Entrainement> sortByDate() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparing(Entrainement::getDateEntrainement))
                .collect(Collectors.toList());
    }

    public List<Entrainement> sortByType() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparing(Entrainement::getType))
                .collect(Collectors.toList());
    }

    public List<Entrainement> sortByLieu() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparing(Entrainement::getLieu))
                .collect(Collectors.toList());
    }

    private Entrainement mapRow(ResultSet rs) throws SQLException {
        Entrainement e = new Entrainement();
        e.setId(rs.getInt("id"));
        e.setDateEntrainement(rs.getDate("date_entrainement").toLocalDate());
        e.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        e.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        e.setType(rs.getString("type"));
        e.setObjectif(rs.getString("objectif"));
        e.setLieu(rs.getString("lieu"));
        e.setEntraineurId(rs.getInt("entraineur_id"));
        return e;
    }
}

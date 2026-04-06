package tn.esprit.services;

import tn.esprit.entities.Participation;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ParticipationService implements IService<Participation> {

    private final Connection con;

    public ParticipationService() throws SQLException {
        con = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Participation p) throws SQLException {
        String sql = "INSERT INTO participation (presence, justification_absence, entrainement_id, joueur_id) " +
                "VALUES (?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, p.getPresence());
        ps.setString(2, p.getJustificationAbsence());
        ps.setInt(3, p.getEntrainementId());
        if (p.getJoueurId() != null) {
            ps.setInt(4, p.getJoueurId());
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        ps.executeUpdate();
        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            p.setId(generatedKeys.getInt(1));
        }
        System.out.println("Participation ajoutée avec succès ! ID=" + p.getId());
    }

    @Override
    public void update(Participation p) throws SQLException {
        String sql = "UPDATE participation SET presence=?, justification_absence=?, " +
                "entrainement_id=?, joueur_id=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, p.getPresence());
        ps.setString(2, p.getJustificationAbsence());
        ps.setInt(3, p.getEntrainementId());
        if (p.getJoueurId() != null) {
            ps.setInt(4, p.getJoueurId());
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        ps.setInt(5, p.getId());
        ps.executeUpdate();
        System.out.println("Participation mise à jour avec succès !");
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM participation WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Participation supprimée avec succès !");
    }

    @Override
    public List<Participation> getAll() throws SQLException {
        List<Participation> list = new ArrayList<>();
        String sql = "SELECT * FROM participation";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public Participation getById(int id) throws SQLException {
        String sql = "SELECT * FROM participation WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    public List<Participation> getByEntrainement(int entrainementId) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String sql = "SELECT * FROM participation WHERE entrainement_id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, entrainementId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public List<Participation> search(String keyword) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String sql = "SELECT * FROM participation WHERE presence LIKE ? OR justification_absence LIKE ?";
        PreparedStatement ps = con.prepareStatement(sql);
        String kw = "%" + keyword + "%";
        ps.setString(1, kw);
        ps.setString(2, kw);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public List<Participation> sortByPresence() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparing(Participation::getPresence))
                .collect(Collectors.toList());
    }

    public List<Participation> sortByJoueur() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparing(p -> p.getJoueurId() != null ? p.getJoueurId() : 0))
                .collect(Collectors.toList());
    }

    public List<Participation> sortByEntrainement() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparing(Participation::getEntrainementId))
                .collect(Collectors.toList());
    }

    private Participation mapRow(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId(rs.getInt("id"));
        p.setPresence(rs.getString("presence"));
        p.setJustificationAbsence(rs.getString("justification_absence"));
        p.setEntrainementId(rs.getInt("entrainement_id"));
        int joueurId = rs.getInt("joueur_id");
        p.setJoueurId(rs.wasNull() ? null : joueurId);
        return p;
    }
}
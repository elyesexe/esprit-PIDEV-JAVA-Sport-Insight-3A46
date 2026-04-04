package tn.esprit.services;

import tn.esprit.entities.Evaluation;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EvaluationService implements IService<Evaluation> {

    private final Connection con;

    public EvaluationService() throws SQLException {
        con = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Evaluation e) throws SQLException {
        String sql = "INSERT INTO evaluation (note_physique, note_technique, note_tactique, commentaire, entrainement_id, joueur_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setDouble(1, e.getNotePhysique());
        ps.setDouble(2, e.getNoteTechnique());
        ps.setDouble(3, e.getNoteTactique());
        ps.setString(4, e.getCommentaire());
        ps.setInt(5, e.getEntrainementId());
        ps.setInt(6, e.getJoueurId());
        ps.executeUpdate();
        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            e.setId(generatedKeys.getInt(1));
        }
        System.out.println("Evaluation ajoutée avec succès ! ID=" + e.getId());
    }

    @Override
    public void update(Evaluation e) throws SQLException {
        String sql = "UPDATE evaluation SET note_physique=?, note_technique=?, note_tactique=?, " +
                "commentaire=?, entrainement_id=?, joueur_id=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setDouble(1, e.getNotePhysique());
        ps.setDouble(2, e.getNoteTechnique());
        ps.setDouble(3, e.getNoteTactique());
        ps.setString(4, e.getCommentaire());
        ps.setInt(5, e.getEntrainementId());
        ps.setInt(6, e.getJoueurId());
        ps.setInt(7, e.getId());
        ps.executeUpdate();
        System.out.println("Evaluation mise à jour avec succès !");
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM evaluation WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Evaluation supprimée avec succès !");
    }

    @Override
    public List<Evaluation> getAll() throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT * FROM evaluation";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public Evaluation getById(int id) throws SQLException {
        String sql = "SELECT * FROM evaluation WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    public List<Evaluation> getByEntrainement(int entrainementId) throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT * FROM evaluation WHERE entrainement_id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, entrainementId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public List<Evaluation> search(String keyword) throws SQLException {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT * FROM evaluation WHERE commentaire LIKE ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    public List<Evaluation> sortByNotePhysique() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparingDouble(Evaluation::getNotePhysique).reversed())
                .collect(Collectors.toList());
    }

    public List<Evaluation> sortByNoteTechnique() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparingDouble(Evaluation::getNoteTechnique).reversed())
                .collect(Collectors.toList());
    }

    public List<Evaluation> sortByNoteTactique() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparingDouble(Evaluation::getNoteTactique).reversed())
                .collect(Collectors.toList());
    }

    public List<Evaluation> sortByMoyenne() throws SQLException {
        return getAll().stream()
                .sorted(Comparator.comparingDouble(e ->
                        -((e.getNotePhysique() + e.getNoteTechnique() + e.getNoteTactique()) / 3.0)))
                .collect(Collectors.toList());
    }

    private Evaluation mapRow(ResultSet rs) throws SQLException {
        Evaluation e = new Evaluation();
        e.setId(rs.getInt("id"));
        e.setNotePhysique(rs.getDouble("note_physique"));
        e.setNoteTechnique(rs.getDouble("note_technique"));
        e.setNoteTactique(rs.getDouble("note_tactique"));
        e.setCommentaire(rs.getString("commentaire"));
        e.setEntrainementId(rs.getInt("entrainement_id"));
        e.setJoueurId(rs.getInt("joueur_id"));
        return e;
    }
}
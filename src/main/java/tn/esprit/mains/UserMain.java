package tn.esprit.mains;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class UserMain {
    public static void main(String[] args) {

        UserService us = new UserService();

        // ── CREATE ────────────────────────────────────────────────────────
        System.out.println("===== AJOUTER =====");
        User u1 = new User("ali.ben@mail.com", "[\"ROLE_USER\"]", "pass123",
                "Ben Salem", "Ali", "+216 55 111 222",
                LocalDate.of(1998, 3, 20), "photo1.png", "actif",
                LocalDateTime.now(), null, LocalDateTime.now());

        User u2 = new User("sana.triki@mail.com", "[\"ROLE_ADMIN\"]", "admin456",
                "Triki", "Sana", "+216 22 333 444",
                LocalDate.of(1990, 7, 10), "photo2.png", "actif",
                LocalDateTime.now(), "cv_sana.pdf", LocalDateTime.now());
        try {
            us.addUser(u1);
            us.addUser(u2);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // ── READ ALL ──────────────────────────────────────────────────────
        System.out.println("\n===== LISTE DES USERS =====");
        List<User> users = null;
        try {
            users = us.getAllUsers();
            users.forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // ── READ BY ID ────────────────────────────────────────────────────
        System.out.println("\n===== CHERCHER PAR ID =====");
        int targetId = (users != null && !users.isEmpty()) ? users.get(0).getId() : 1;
        User found = us.findUserById(targetId);
        System.out.println(found);

        // ── UPDATE ────────────────────────────────────────────────────────
        System.out.println("\n===== MODIFIER =====");
        if (found != null) {
            found.setTelephone("+216 99 000 000");
            found.setStatut("inactif");
            found.setUpdatedAt(LocalDateTime.now());
            us.updateUser(found);
        }

        // ── READ ALL après update ─────────────────────────────────────────
        System.out.println("\n===== LISTE APRES MODIFICATION =====");
        try {
            us.getAllUsers().forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // ── DELETE ────────────────────────────────────────────────────────
        System.out.println("\n===== SUPPRIMER =====");
        if (found != null) us.deleteUser(found.getId());

        // ── READ ALL après delete ─────────────────────────────────────────
        System.out.println("\n===== LISTE APRES SUPPRESSION =====");
        try {
            us.getAllUsers().forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
package tn.esprit.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPdfExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void exportCreatesReadablePdfWithUsers() throws Exception {
        UserPdfExportService service = new UserPdfExportService();
        Path output = tempDir.resolve("users-export.pdf");

        User activeUser = buildUser(7, "sarah@example.com", "Sarah", "Connor", UserRoles.ROLE_ADMIN, "ACTIVE");
        User blockedUser = buildUser(12, "leo@example.com", "Leo", "Messi", UserRoles.ROLE_USER, "BLOCKED");

        service.export(output, List.of(activeUser, blockedUser));

        assertTrue(Files.exists(output), "The PDF file should be created.");
        assertTrue(Files.size(output) > 0, "The PDF file should not be empty.");

        try (var document = Loader.loadPDF(output.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Users export"), "The PDF should contain its title.");
            assertTrue(text.contains("Sarah Connor"), "The PDF should list exported users.");
            assertTrue(text.contains("leo@example.com"), "The PDF should contain user emails.");
            assertTrue(text.contains("BLOCKED"), "The PDF should contain account statuses.");
        }
    }

    private User buildUser(int id, String email, String prenom, String nom, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPrenom(prenom);
        user.setNom(nom);
        user.setRoleList(List.of(role));
        user.setStatut(status);
        user.setTelephone("+21600000000");
        user.setDateNaissance(LocalDate.of(1999, 1, 2));
        user.setDateInscription(LocalDateTime.of(2026, 1, 10, 11, 30));
        user.setUpdatedAt(LocalDateTime.of(2026, 2, 5, 8, 15));
        user.setCvName("profile.pdf");
        return user;
    }
}

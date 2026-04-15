package tn.esprit.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Commentaire;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnoncePdfExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void exportCreatesReadablePdfWithAnnouncementsAndComments() throws Exception {
        AnnoncePdfExportService service = new AnnoncePdfExportService();
        Path output = tempDir.resolve("annonces-export.pdf");

        Annonce urgentAnnonce = new Annonce(
                10,
                "Urgent keeper needed",
                "Need an experienced goalkeeper immediately.",
                "Goalkeeper",
                "Senior",
                LocalDate.of(2026, 4, 15),
                "ACTIVE",
                12,
                true,
                true
        );
        Annonce standardAnnonce = new Annonce(
                11,
                "Regular defender trial",
                "Open training session next week.",
                "Defender",
                "Junior",
                LocalDate.of(2026, 4, 14),
                "ACTIVE",
                13,
                true,
                false
        );

        Commentaire comment = new Commentaire(
                20,
                "I am interested in this urgent post.",
                LocalDate.of(2026, 4, 15),
                7,
                10,
                "Player One",
                3,
                "APPROVED",
                "Verified by admin"
        );

        service.export(output, List.of(standardAnnonce, urgentAnnonce), List.of(comment));

        assertTrue(Files.exists(output), "The PDF file should be created.");
        assertTrue(Files.size(output) > 0, "The PDF file should not be empty.");

        try (PDDocument document = Loader.loadPDF(output.toFile())) {
            assertTrue(document.getNumberOfPages() >= 1, "The exported PDF should be readable.");
        }
    }
}

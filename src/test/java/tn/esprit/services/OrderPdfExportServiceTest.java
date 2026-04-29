package tn.esprit.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPdfExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void exportInvoiceCreatesReadablePdfWithQrPayload() throws Exception {
        OrderPdfExportService service = new OrderPdfExportService();
        Path output = tempDir.resolve("invoice-order-15.pdf");
        OrderPdfExportService.Invoice invoice = new OrderPdfExportService.Invoice(
                "Coach Ahmed",
                "client@example.com",
                "+21655000000",
                "CARD",
                "Tunis Centre",
                "Tunis Centre",
                LocalDate.of(2026, 4, 28),
                new BigDecimal("560.00"),
                List.of(new OrderPdfExportService.InvoiceLine("Mercurial Elite", "42", 2, new BigDecimal("280.00"), new BigDecimal("560.00")))
        );

        service.exportInvoice(output, invoice, "ORDER:15\nPRODUCT:Mercurial Elite\nTOTAL:560.00 DT");

        assertTrue(Files.exists(output), "The invoice PDF should be created.");
        assertTrue(Files.size(output) > 0, "The invoice PDF should not be empty.");

        try (var document = Loader.loadPDF(output.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Facture"), "The PDF should contain the invoice title.");
            assertTrue(text.contains("Mercurial Elite"), "The PDF should contain the product name.");
            assertTrue(text.contains("560.00 DT"), "The PDF should contain the total amount.");
        }
    }
}

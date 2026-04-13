package tn.esprit.utils;

import tn.esprit.entities.ContratSponsor;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.List;

public class PDFExporter {

    public static void exportContratsToPDF(List<ContratSponsor> contrats, String filePath) throws FileNotFoundException {
        try {
            // Create PDF writer and document
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Add title
            Paragraph title = new Paragraph("📄 RAPPORT DES CONTRATS SPONSORS")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            
            // Add export date
            Paragraph dateInfo = new Paragraph("Date d'export: " + LocalDate.now())
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(dateInfo);
            
            Paragraph emptyLine = new Paragraph("\n");
            document.add(emptyLine);

            // Create table with 7 columns
            Table table = new Table(7);
            table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
            
            // Add headers with formatting
            String[] headers = {"ID", "Date Début", "Date Fin", "Montant", "Description", "Statut", "Statut Paiement"};
            for (String header : headers) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(header).setBold())
                        .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(66, 139, 202));
                headerCell.setTextAlignment(TextAlignment.CENTER);
                table.addCell(headerCell);
            }

            // Add data rows
            for (ContratSponsor c : contrats) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(c.getId()))).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(c.getDateDebut().toString())));
                table.addCell(new Cell().add(new Paragraph(c.getDateFin().toString())));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f DT", c.getMontant()))).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(c.getDescription())));
                table.addCell(new Cell().add(new Paragraph(c.getStatut())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(c.getStatutPaiement())).setTextAlignment(TextAlignment.CENTER));
            }

            document.add(table);
            
            // Add footer
            Paragraph footer = new Paragraph("\n✓ Rapport généré automatiquement par Sport Insight")
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(footer);
            
            document.close();
        } catch (Exception e) {
            throw new FileNotFoundException("Erreur lors de l'export PDF: " + e.getMessage());
        }
    }
}




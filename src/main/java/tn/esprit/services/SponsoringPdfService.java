package tn.esprit.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Sponsor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SponsoringPdfService {
    private static final float PAGE_MARGIN = 52f;
    private static final float BODY_FONT_SIZE = 11.5f;
    private static final float TITLE_FONT_SIZE = 22f;
    private static final float SUBTITLE_FONT_SIZE = 13f;
    private static final float LINE_HEIGHT = 16f;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);

    private final PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDType1Font italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    public Path exportContractPdf(Path targetFile, ContratSponsor contrat, Sponsor sponsor, Equipe equipe) throws IOException {
        if (targetFile == null) {
            throw new IOException("Target PDF path is missing.");
        }
        Path parent = targetFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - PAGE_MARGIN;
                y = writeTitle(content, "Sponsor Contract", "Sport Insight sponsorship module", y);

                y = writeSectionHeading(content, "Contract summary", y);
                y = writeWrapped(content, "Sponsor: " + safeName(sponsor == null ? null : sponsor.getNom(), "Unknown sponsor"), y);
                y = writeWrapped(content, "Team: " + safeName(equipe == null ? null : equipe.getNom(), "Unknown team"), y);
                y = writeWrapped(content, "Amount: " + formatCurrency(contrat == null ? 0.0 : contrat.getMontant()), y);
                y = writeWrapped(content, "Start date: " + formatDate(contrat == null ? null : contrat.getDateDebut()), y);
                y = writeWrapped(content, "End date: " + formatDate(contrat == null ? null : contrat.getDateFin()), y);
                y = writeWrapped(content, "Status: " + safeName(contrat == null ? null : contrat.getStatut(), "ACTIVE"), y);
                y = writeWrapped(content, "Payment status: " + safeName(contrat == null ? null : contrat.getStatutPaiement(), "PENDING"), y);

                y -= 6f;
                y = writeSectionHeading(content, "Sponsor contact", y);
                y = writeWrapped(content, "Email: " + safeName(sponsor == null ? null : sponsor.getEmail(), "-"), y);
                y = writeWrapped(content, "Phone: " + safeName(sponsor == null ? null : sponsor.getTelephone(), "-"), y);
                y = writeWrapped(content, "Address: " + safeName(sponsor == null ? null : sponsor.getAdresse(), "-"), y);

                y -= 6f;
                y = writeSectionHeading(content, "Description", y);
                y = writeWrapped(content, safeName(contrat == null ? null : contrat.getDescription(), "No description provided."), y);

                writeFooter(content, page, "Generated " + DATE_TIME_FORMATTER.format(LocalDateTime.now()));
            }

            document.save(targetFile.toFile());
        }
        return targetFile;
    }

    public Path exportSummaryPdf(
            Path targetFile,
            SponsoringWorkspaceService.SponsoringSnapshot snapshot,
            SponsoringWorkspaceService workspaceService
    ) throws IOException {
        if (targetFile == null) {
            throw new IOException("Target PDF path is missing.");
        }
        Path parent = targetFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - PAGE_MARGIN;
                y = writeTitle(content, "Sponsoring Overview", "Sport Insight reporting export", y);

                SponsoringWorkspaceService.SponsoringStats stats = snapshot.stats();
                y = writeSectionHeading(content, "Key numbers", y);
                y = writeWrapped(content, "Sponsors: " + stats.totalSponsors(), y);
                y = writeWrapped(content, "Contracts: " + stats.totalContrats(), y);
                y = writeWrapped(content, "Active contracts: " + stats.activeContracts(), y);
                y = writeWrapped(content, "Expired contracts: " + stats.expiredContracts(), y);
                y = writeWrapped(content, "Total sponsor budget: " + formatCurrency(stats.totalBudget()), y);
                y = writeWrapped(content, "Total contract amount: " + formatCurrency(stats.totalContractAmount()), y);
                y = writeWrapped(content, "Average contract amount: " + formatCurrency(stats.averageContractAmount()), y);

                y -= 4f;
                y = writeSectionHeading(content, "Top sponsors", y);
                List<SponsoringWorkspaceService.SponsorBudgetPoint> topSponsors = stats.topSponsors();
                if (topSponsors.isEmpty()) {
                    y = writeWrapped(content, "No sponsor data available.", y);
                } else {
                    for (SponsoringWorkspaceService.SponsorBudgetPoint point : topSponsors) {
                        y = writeWrapped(content, point.label() + " - " + formatCurrency(point.value()), y);
                    }
                }

                y -= 4f;
                y = writeSectionHeading(content, "Recent contracts", y);
                List<ContratSponsor> latestContracts = snapshot.contrats().stream().limit(8).toList();
                if (latestContracts.isEmpty()) {
                    y = writeWrapped(content, "No contract data available.", y);
                } else {
                    for (ContratSponsor contrat : latestContracts) {
                        Sponsor sponsor = snapshot.sponsorOf(contrat);
                        Equipe equipe = snapshot.equipeOf(contrat);
                        String line = safeName(sponsor == null ? null : sponsor.getNom(), "Sponsor")
                                + " / "
                                + safeName(equipe == null ? null : equipe.getNom(), "Team")
                                + " / "
                                + formatCurrency(contrat.getMontant())
                                + " / "
                                + workspaceService.resolveContractStatus(contrat);
                        y = writeWrapped(content, line, y);
                    }
                }

                y -= 4f;
                y = writeSectionHeading(content, "Breakdown", y);
                y = writeKeyValueLines(content, "Contract status", stats.contractStatusBreakdown(), y);
                y = writeKeyValueLines(content, "Payment status", stats.paymentBreakdown(), y);

                writeFooter(content, page, "Generated " + DATE_TIME_FORMATTER.format(LocalDateTime.now()));
            }

            document.save(targetFile.toFile());
        }
        return targetFile;
    }

    private float writeTitle(PDPageContentStream content, String title, String subtitle, float startY) throws IOException {
        beginText(content, boldFont, TITLE_FONT_SIZE, PAGE_MARGIN, startY);
        content.showText(title);
        content.endText();

        float subtitleY = startY - 24f;
        beginText(content, italicFont, SUBTITLE_FONT_SIZE, PAGE_MARGIN, subtitleY);
        content.showText(subtitle);
        content.endText();
        return subtitleY - 24f;
    }

    private float writeSectionHeading(PDPageContentStream content, String heading, float startY) throws IOException {
        beginText(content, boldFont, 13f, PAGE_MARGIN, startY);
        content.showText(heading);
        content.endText();
        return startY - 18f;
    }

    private float writeWrapped(PDPageContentStream content, String text, float startY) throws IOException {
        float availableWidth = PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2);
        List<String> lines = wrapText(text, availableWidth, regularFont, BODY_FONT_SIZE);
        float currentY = startY;
        for (String line : lines) {
            beginText(content, regularFont, BODY_FONT_SIZE, PAGE_MARGIN, currentY);
            content.showText(line);
            content.endText();
            currentY -= LINE_HEIGHT;
        }
        return currentY;
    }

    private float writeKeyValueLines(
            PDPageContentStream content,
            String title,
            Map<String, Long> values,
            float startY
    ) throws IOException {
        float currentY = writeWrapped(content, title + ":", startY);
        if (values == null || values.isEmpty()) {
            return writeWrapped(content, "  - None", currentY);
        }
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            currentY = writeWrapped(content, "  - " + entry.getKey() + ": " + entry.getValue(), currentY);
        }
        return currentY;
    }

    private void writeFooter(PDPageContentStream content, PDPage page, String footer) throws IOException {
        beginText(content, italicFont, 9.5f, PAGE_MARGIN, PAGE_MARGIN - 12f);
        content.showText(footer);
        content.endText();

        beginText(
                content,
                italicFont,
                9.5f,
                page.getMediaBox().getWidth() - PAGE_MARGIN - 48f,
                PAGE_MARGIN - 12f
        );
        content.showText("Page 1");
        content.endText();
    }

    private void beginText(PDPageContentStream content, PDType1Font font, float fontSize, float x, float y) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
    }

    private List<String> wrapText(String text, float width, PDType1Font font, float fontSize) throws IOException {
        if (text == null || text.isBlank()) {
            return List.of("");
        }

        String[] words = text.replace("\r", "").split("\\s+");
        StringBuilder line = new StringBuilder();
        List<String> lines = new java.util.ArrayList<>();

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            float candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize;
            if (candidateWidth <= width || line.length() == 0) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.ENGLISH, "%,.2f DT", amount);
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "-" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String safeName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

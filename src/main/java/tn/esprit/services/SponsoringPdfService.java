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

import java.awt.Color;
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
    private static final float SECTION_GAP = 18f;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);
    private static final Color PRIMARY = new Color(21, 94, 117);
    private static final Color SECONDARY = new Color(15, 23, 42);
    private static final Color ACCENT = new Color(14, 165, 164);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color PANEL = new Color(245, 247, 250);
    private static final Color BORDER = new Color(203, 213, 225);

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
                drawHeaderBand(content, page);
                y = writeHeroTitle(content, page, "Professional Sponsorship Contract", "Sport Insight | Partnership Office", y - 6f);

                y = writeContractMetaPanel(content, page, contrat, sponsor, equipe, y - 8f);
                y = writeProfessionalSection(content, page, "Parties", List.of(
                        "Sponsor: " + safeName(sponsor == null ? null : sponsor.getNom(), "Unknown sponsor"),
                        "Team: " + safeName(equipe == null ? null : equipe.getNom(), "Unknown team")
                ), y);

                y = writeProfessionalSection(content, page, "Commercial Terms", List.of(
                        "Contract value: " + formatCurrency(contrat == null ? 0.0 : contrat.getMontant()),
                        "Contract status: " + safeName(contrat == null ? null : contrat.getStatut(), "ACTIVE"),
                        "Payment status: " + safeName(contrat == null ? null : contrat.getStatutPaiement(), "PENDING"),
                        "Effective period: " + formatDate(contrat == null ? null : contrat.getDateDebut()) + " to " + formatDate(contrat == null ? null : contrat.getDateFin())
                ), y);

                y = writeProfessionalSection(content, page, "Sponsor Contact", List.of(
                        "Email: " + safeName(sponsor == null ? null : sponsor.getEmail(), "-"),
                        "Phone: " + safeName(sponsor == null ? null : sponsor.getTelephone(), "-"),
                        "Address: " + safeName(sponsor == null ? null : sponsor.getAdresse(), "-")
                ), y);

                y = writeDescriptionSection(
                        content,
                        page,
                        "Contract Clauses",
                        safeName(contrat == null ? null : contrat.getDescription(), "No description provided."),
                        y
                );

                writeSignatureBlock(content, page);
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
        content.setNonStrokingColor(MUTED);
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
        content.setNonStrokingColor(Color.BLACK);
    }

    private void drawHeaderBand(PDPageContentStream content, PDPage page) throws IOException {
        content.setNonStrokingColor(PRIMARY);
        content.addRect(0, page.getMediaBox().getHeight() - 110f, page.getMediaBox().getWidth(), 110f);
        content.fill();
        content.setNonStrokingColor(Color.WHITE);
    }

    private float writeHeroTitle(PDPageContentStream content, PDPage page, String title, String subtitle, float startY) throws IOException {
        beginText(content, boldFont, TITLE_FONT_SIZE, PAGE_MARGIN, startY);
        content.showText(title);
        content.endText();

        beginText(content, italicFont, SUBTITLE_FONT_SIZE, PAGE_MARGIN, startY - 24f);
        content.showText(subtitle);
        content.endText();

        content.setNonStrokingColor(new Color(210, 250, 250));
        drawRoundedPanel(content, page.getMediaBox().getWidth() - PAGE_MARGIN - 122f, startY - 34f, 122f, 26f, ACCENT);
        content.setNonStrokingColor(Color.WHITE);
        beginText(content, boldFont, 10.5f, page.getMediaBox().getWidth() - PAGE_MARGIN - 106f, startY - 18f);
        content.showText("READY TO SIGN");
        content.endText();
        content.setNonStrokingColor(SECONDARY);
        return startY - 52f;
    }

    private float writeContractMetaPanel(PDPageContentStream content, PDPage page, ContratSponsor contrat, Sponsor sponsor, Equipe equipe, float startY) throws IOException {
        float panelHeight = 110f;
        float panelWidth = page.getMediaBox().getWidth() - (PAGE_MARGIN * 2);
        drawPanel(content, PAGE_MARGIN, startY - panelHeight + 12f, panelWidth, panelHeight);

        float textY = startY - 12f;
        textY = writeLabelValue(content, "Contract Reference", "#" + safeName(contrat == null || contrat.getId() == null ? null : String.valueOf(contrat.getId()), "Pending"), PAGE_MARGIN + 16f, textY);
        textY = writeLabelValue(content, "Sponsor", safeName(sponsor == null ? null : sponsor.getNom(), "Unknown sponsor"), PAGE_MARGIN + 16f, textY - 3f);
        textY = writeLabelValue(content, "Team", safeName(equipe == null ? null : equipe.getNom(), "Unknown team"), PAGE_MARGIN + 16f, textY - 3f);

        float rightX = PAGE_MARGIN + (panelWidth / 2f) + 8f;
        float rightY = startY - 12f;
        rightY = writeLabelValue(content, "Amount", formatCurrency(contrat == null ? 0.0 : contrat.getMontant()), rightX, rightY);
        rightY = writeLabelValue(content, "Start Date", formatDate(contrat == null ? null : contrat.getDateDebut()), rightX, rightY - 3f);
        writeLabelValue(content, "End Date", formatDate(contrat == null ? null : contrat.getDateFin()), rightX, rightY - 3f);
        return startY - panelHeight - 6f;
    }

    private float writeProfessionalSection(PDPageContentStream content, PDPage page, String title, List<String> lines, float startY) throws IOException {
        float currentY = startY - SECTION_GAP;
        currentY = writeSectionHeadingStyled(content, title, currentY);
        for (String line : lines) {
            currentY = writeWrapped(content, line, currentY);
        }
        return currentY;
    }

    private float writeDescriptionSection(PDPageContentStream content, PDPage page, String title, String description, float startY) throws IOException {
        float currentY = startY - SECTION_GAP;
        currentY = writeSectionHeadingStyled(content, title, currentY);
        float boxY = currentY - 8f;
        float boxHeight = 188f;
        drawPanel(content, PAGE_MARGIN, boxY - boxHeight + 10f, page.getMediaBox().getWidth() - (PAGE_MARGIN * 2), boxHeight);
        content.setNonStrokingColor(SECONDARY);
        return writeWrappedInsideBox(content, description, PAGE_MARGIN + 18f, boxY - 14f, page.getMediaBox().getWidth() - (PAGE_MARGIN * 2) - 36f);
    }

    private float writeWrappedInsideBox(PDPageContentStream content, String text, float x, float startY, float width) throws IOException {
        List<String> lines = wrapText(text, width, regularFont, BODY_FONT_SIZE);
        float currentY = startY;
        for (String line : lines) {
            beginText(content, regularFont, BODY_FONT_SIZE, x, currentY);
            content.showText(line);
            content.endText();
            currentY -= LINE_HEIGHT;
        }
        return currentY;
    }

    private void writeSignatureBlock(PDPageContentStream content, PDPage page) throws IOException {
        float blockY = 126f;
        float blockWidth = (page.getMediaBox().getWidth() - (PAGE_MARGIN * 2) - 18f) / 2f;
        drawPanel(content, PAGE_MARGIN, blockY, blockWidth, 64f);
        drawPanel(content, PAGE_MARGIN + blockWidth + 18f, blockY, blockWidth, 64f);

        content.setNonStrokingColor(MUTED);
        beginText(content, boldFont, 11f, PAGE_MARGIN + 16f, blockY + 44f);
        content.showText("Sponsor Signature");
        content.endText();
        beginText(content, boldFont, 11f, PAGE_MARGIN + blockWidth + 34f, blockY + 44f);
        content.showText("Team Representative");
        content.endText();
        content.setNonStrokingColor(SECONDARY);
    }

    private float writeSectionHeadingStyled(PDPageContentStream content, String heading, float startY) throws IOException {
        content.setNonStrokingColor(ACCENT);
        content.addRect(PAGE_MARGIN, startY - 9f, 4f, 18f);
        content.fill();
        content.setNonStrokingColor(SECONDARY);
        beginText(content, boldFont, 13.5f, PAGE_MARGIN + 12f, startY);
        content.showText(heading);
        content.endText();
        return startY - 18f;
    }

    private float writeLabelValue(PDPageContentStream content, String label, String value, float x, float y) throws IOException {
        content.setNonStrokingColor(MUTED);
        beginText(content, boldFont, 10f, x, y);
        content.showText(label.toUpperCase(Locale.ROOT));
        content.endText();
        content.setNonStrokingColor(SECONDARY);
        beginText(content, regularFont, 11.5f, x, y - 13f);
        content.showText(safeName(value, "-"));
        content.endText();
        return y - 28f;
    }

    private void drawPanel(PDPageContentStream content, float x, float y, float width, float height) throws IOException {
        content.setNonStrokingColor(PANEL);
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(BORDER);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void drawRoundedPanel(PDPageContentStream content, float x, float y, float width, float height, Color color) throws IOException {
        content.setNonStrokingColor(color);
        content.addRect(x, y, width, height);
        content.fill();
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

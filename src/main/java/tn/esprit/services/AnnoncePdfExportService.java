package tn.esprit.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Commentaire;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AnnoncePdfExportService {
    private static final PDType1Font TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font SECTION_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private static final float PAGE_MARGIN = 48f;
    private static final float PAGE_TOP = 792f - PAGE_MARGIN;
    private static final float PAGE_BOTTOM = PAGE_MARGIN;
    private static final float TEXT_WIDTH = 612f - (PAGE_MARGIN * 2f);
    private static final float LINE_GAP = 5f;

    public void export(Path targetPath, List<Annonce> annonces, List<Commentaire> commentaires) throws IOException {
        if (targetPath == null) {
            throw new IOException("No destination file was selected.");
        }

        Path parent = targetPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<Annonce> sortedAnnonces = new ArrayList<>(annonces);
        sortedAnnonces.sort(Comparator
                .comparing((Annonce annonce) -> !Boolean.TRUE.equals(annonce.getUrgent()))
                .thenComparing(Annonce::getDatePublication, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Annonce::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        Map<Integer, List<Commentaire>> commentsByAnnonce = commentaires.stream()
                .filter(commentaire -> commentaire.getAnnonceId() != null)
                .collect(Collectors.groupingBy(Commentaire::getAnnonceId));

        try (PDDocument document = new PDDocument()) {
            DocumentWriter writer = new DocumentWriter(document);

            writer.writeTitle("Announcements export");
            writer.writeBody("Generated on " + formatDate(LocalDate.now()) + " - " + sortedAnnonces.size() + " announcement(s).");
            writer.writeBlankLine(8f);

            for (Annonce annonce : sortedAnnonces) {
                List<Commentaire> annonceComments = new ArrayList<>(
                        commentsByAnnonce.getOrDefault(annonce.getId(), List.of())
                );
                annonceComments.sort(Comparator
                        .comparing(Commentaire::getDateCommentaire, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Commentaire::getId, Comparator.nullsLast(Comparator.reverseOrder())));

                writer.writeSectionTitle(buildAnnonceHeading(annonce));
                writer.writeBody("Title: " + fallback(annonce.getTitre(), "Untitled announcement"));
                writer.writeBody("Role: " + fallback(annonce.getPosteRecherche(), "Not specified"));
                writer.writeBody("Level: " + fallback(annonce.getNiveauRequis(), "Not specified"));
                writer.writeBody("Status: " + fallback(annonce.getStatut(), "Unknown"));
                writer.writeBody("Priority: " + (Boolean.TRUE.equals(annonce.getUrgent()) ? "Urgent" : "Standard"));
                writer.writeBody("Coach ID: " + (annonce.getEntraineurId() == null ? "-" : annonce.getEntraineurId()));
                writer.writeBody("Publication date: " + formatDate(annonce.getDatePublication()));
                writer.writeBody("Description:");
                writer.writeIndentedBody(fallback(annonce.getDescription(), "No description provided."));

                writer.writeBody("Comments (" + annonceComments.size() + "):");
                if (annonceComments.isEmpty()) {
                    writer.writeIndentedBody("No comments.");
                } else {
                    for (Commentaire commentaire : annonceComments) {
                        writer.writeComment(commentaire);
                    }
                }
                writer.writeDivider();
            }

            writer.close();
            document.save(targetPath.toFile());
        }
    }

    private String buildAnnonceHeading(Annonce annonce) {
        String id = annonce.getId() == null ? "-" : String.valueOf(annonce.getId());
        return "Announcement #" + id + (Boolean.TRUE.equals(annonce.getUrgent()) ? " [URGENT]" : "");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static final class DocumentWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream stream;
        private float currentY;

        private DocumentWriter(PDDocument document) throws IOException {
            this.document = document;
            startPage();
        }

        private void writeTitle(String text) throws IOException {
            writeParagraph(text, TITLE_FONT, 18f, 0f);
        }

        private void writeSectionTitle(String text) throws IOException {
            ensureSpace(24f);
            writeParagraph(text, SECTION_FONT, 13f, 0f);
        }

        private void writeBody(String text) throws IOException {
            writeParagraph(text, BODY_FONT, 10.5f, 0f);
        }

        private void writeIndentedBody(String text) throws IOException {
            writeParagraph(text, BODY_FONT, 10.5f, 14f);
        }

        private void writeComment(Commentaire commentaire) throws IOException {
            String author = fallback(commentaire.getAuteurAnonyme(), "Anonymous");
            String date = commentaire.getDateCommentaire() == null ? "-" : DATE_FORMATTER.format(commentaire.getDateCommentaire());
            String status = fallback(commentaire.getModerationStatus(), "Unknown");
            writeIndentedBody("- " + author + " | " + date + " | " + status + " | Likes: " + commentaire.getNbLikes());
            writeIndentedBody("  " + fallback(commentaire.getContenu(), "No comment content."));
            if (commentaire.getModerationReason() != null && !commentaire.getModerationReason().isBlank()) {
                writeIndentedBody("  Moderation reason: " + commentaire.getModerationReason().trim());
            }
        }

        private void writeDivider() throws IOException {
            ensureSpace(14f);
            stream.setStrokingColor(0.705f, 0.705f, 0.705f);
            stream.moveTo(PAGE_MARGIN, currentY);
            stream.lineTo(PAGE_MARGIN + TEXT_WIDTH, currentY);
            stream.stroke();
            currentY -= 14f;
        }

        private void writeBlankLine(float height) {
            currentY -= height;
        }

        private void writeParagraph(String text, PDType1Font font, float fontSize, float indent) throws IOException {
            List<String> lines = wrapText(fallback(text, ""), font, fontSize, TEXT_WIDTH - indent);
            float lineHeight = fontSize + LINE_GAP;
            ensureSpace(lineHeight * Math.max(lines.size(), 1));
            for (String line : lines) {
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(PAGE_MARGIN + indent, currentY);
                stream.showText(sanitize(line));
                stream.endText();
                currentY -= lineHeight;
            }
            currentY -= 2f;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (currentY - requiredHeight < PAGE_BOTTOM) {
                startPage();
            }
        }

        private void startPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            currentY = PAGE_TOP;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

        private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String normalized = sanitize(text).replace("\r", "");
            for (String paragraph : normalized.split("\n")) {
                String safeParagraph = paragraph.isBlank() ? " " : paragraph.trim();
                StringBuilder line = new StringBuilder();
                for (String word : safeParagraph.split("\\s+")) {
                    if (word.isBlank()) {
                        continue;
                    }
                    String candidate = line.isEmpty() ? word : line + " " + word;
                    float width = font.getStringWidth(candidate) / 1000f * fontSize;
                    if (width > maxWidth && !line.isEmpty()) {
                        lines.add(line.toString());
                        line = new StringBuilder(word);
                    } else {
                        line = new StringBuilder(candidate);
                    }
                }
                lines.add(line.isEmpty() ? " " : line.toString());
            }
            return lines;
        }

        private String sanitize(String value) {
            String safe = value == null ? "" : value;
            return safe
                    .replace('\u2022', '-')
                    .replace('\u2013', '-')
                    .replace('\u2014', '-')
                    .replace('\u2019', '\'')
                    .replace('\u201c', '"')
                    .replace('\u201d', '"');
        }

        private String fallback(String value, String fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            return value.trim();
        }
    }
}

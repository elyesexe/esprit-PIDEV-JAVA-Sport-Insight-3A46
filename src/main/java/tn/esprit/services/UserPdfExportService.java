package tn.esprit.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class UserPdfExportService {
    private static final PDType1Font TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font HEADER_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ENGLISH);
    private static final float PAGE_MARGIN = 42f;
    private static final float PAGE_TOP = PDRectangle.LETTER.getHeight() - PAGE_MARGIN;
    private static final float PAGE_BOTTOM = PAGE_MARGIN;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth() - (PAGE_MARGIN * 2f);
    private static final float LINE_GAP = 4f;

    public void export(Path targetPath, List<User> users) throws IOException {
        if (targetPath == null) {
            throw new IOException("No destination file was selected.");
        }

        Path parent = targetPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<User> sortedUsers = new ArrayList<>(users);
        sortedUsers.sort(Comparator
                .comparing(User::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        try (PDDocument document = new PDDocument()) {
            DocumentWriter writer = new DocumentWriter(document);
            long activeCount = sortedUsers.stream().filter(User::isActiveAccount).count();
            long blockedCount = sortedUsers.stream()
                    .filter(user -> "BLOCKED".equals(normalizeStatus(user.getStatut())))
                    .count();

            writer.writeTitle("Users export");
            writer.writeBody("Generated on " + DATE_FORMATTER.format(LocalDate.now()) + " - " + sortedUsers.size() + " user(s).");
            writer.writeBody("Active accounts: " + activeCount + " | Blocked accounts: " + blockedCount);
            writer.writeBlankLine(8f);

            for (User user : sortedUsers) {
                writer.writeSectionTitle("User #" + fallback(user.getId() == null ? null : String.valueOf(user.getId()), "-"));
                writer.writeBody("Name: " + fallback(user.getDisplayName(), "Sport Insight user"));
                writer.writeBody("Email: " + fallback(user.getEmail(), "-"));
                writer.writeBody("Role: " + UserRoles.displayName(user.getPrimaryRole()));
                writer.writeBody("Status: " + fallback(user.getStatut(), "Unknown"));
                writer.writeBody("Phone: " + fallback(user.getTelephone(), "-"));
                writer.writeBody("Birth date: " + formatDate(user.getDateNaissance()));
                writer.writeBody("Joined: " + formatDateTime(user.getDateInscription()));
                writer.writeBody("Updated: " + formatDateTime(user.getUpdatedAt()));
                writer.writeBody("CV: " + fallback(user.getCvName(), "-"));
                writer.writeDivider();
            }

            writer.close();
            document.save(targetPath.toFile());
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static final class DocumentWriter {
        private final PDDocument document;
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
            writeParagraph(text, HEADER_FONT, 12.5f, 0f);
        }

        private void writeBody(String text) throws IOException {
            writeParagraph(text, BODY_FONT, 10.5f, 0f);
        }

        private void writeDivider() throws IOException {
            ensureSpace(14f);
            stream.setStrokingColor(0.72f, 0.72f, 0.72f);
            stream.moveTo(PAGE_MARGIN, currentY);
            stream.lineTo(PAGE_MARGIN + PAGE_WIDTH, currentY);
            stream.stroke();
            currentY -= 14f;
        }

        private void writeBlankLine(float height) {
            currentY -= height;
        }

        private void writeParagraph(String text, PDType1Font font, float fontSize, float indent) throws IOException {
            List<String> lines = wrapText(text, font, fontSize, PAGE_WIDTH - indent);
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
            PDPage page = new PDPage(PDRectangle.LETTER);
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
            String normalized = sanitize(text == null ? "" : text).replace("\r", "");
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
            return value
                    .replace('\u2022', '-')
                    .replace('\u2013', '-')
                    .replace('\u2014', '-')
                    .replace('\u2019', '\'')
                    .replace('\u201c', '"')
                    .replace('\u201d', '"');
        }
    }
}

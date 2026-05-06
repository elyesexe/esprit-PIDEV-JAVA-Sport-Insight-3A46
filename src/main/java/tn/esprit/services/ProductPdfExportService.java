package tn.esprit.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import tn.esprit.entities.Product;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProductPdfExportService {
    private static final PDType1Font TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font HEADER_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float PAGE_MARGIN = 42f;
    private static final float PAGE_TOP = PDRectangle.LETTER.getHeight() - PAGE_MARGIN;
    private static final float PAGE_BOTTOM = PAGE_MARGIN;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth() - (PAGE_MARGIN * 2f);
    private static final float LINE_GAP = 4f;

    public void export(Path targetPath, List<Product> products) throws IOException {
        if (targetPath == null) {
            throw new IOException("No destination file was selected.");
        }

        Path parent = targetPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<Product> sortedProducts = new ArrayList<>(products);
        sortedProducts.sort(
                Comparator.comparing((Product product) -> fallback(product.getName(), "Product"), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Product::getId, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        long lowStock = sortedProducts.stream()
                .filter(product -> product.getStock() > 0 && product.getStock() <= 5)
                .count();
        long outOfStock = sortedProducts.stream()
                .filter(product -> product.getStock() <= 0)
                .count();

        try (PDDocument document = new PDDocument()) {
            DocumentWriter writer = new DocumentWriter(document);
            writer.writeTitle("Products export");
            writer.writeBody("Generated on " + LocalDate.now() + " - " + sortedProducts.size() + " product(s).");
            writer.writeBody("Low stock: " + lowStock + " | Out of stock: " + outOfStock);
            writer.writeBlankLine(8f);

            for (Product product : sortedProducts) {
                writer.writeSectionTitle("Product #" + fallback(product.getId() == null ? null : String.valueOf(product.getId()), "-"));
                writer.writeBody("Name: " + fallback(product.getName(), "-"));
                writer.writeBody("Category: " + fallback(product.getCategory(), "-"));
                writer.writeBody("Brand: " + fallback(product.getBrand(), "-"));
                writer.writeBody("Price: " + formatPrice(product));
                writer.writeBody("Stock: " + product.getStock());
                writer.writeBody("Size: " + fallback(product.getSize(), "-"));
                writer.writeBody("Image: " + fallback(product.getImage(), "-"));
<<<<<<< HEAD
                writer.writeBody("Tags: " + fallback(product.getTags(), "-"));
                writer.writeBody("Description: " + fallback(product.getDescription(), "-"));
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                writer.writeDivider();
            }

            writer.close();
            document.save(targetPath.toFile());
        }
    }

    private String formatPrice(Product product) {
        if (product == null || product.getPrice() == null) {
            return "0.00 DT";
        }
        return product.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString() + " DT";
    }

    private String fallback(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return sanitize(value.trim());
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

    private static final class DocumentWriter {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float currentY;

        private DocumentWriter(PDDocument document) throws IOException {
            this.document = document;
            startPage();
        }

        private void writeTitle(String text) throws IOException {
            writeParagraph(text, TITLE_FONT, 18f);
        }

        private void writeSectionTitle(String text) throws IOException {
            ensureSpace(24f);
            writeParagraph(text, HEADER_FONT, 12.5f);
        }

        private void writeBody(String text) throws IOException {
            writeParagraph(text, BODY_FONT, 10.5f);
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

        private void writeParagraph(String text, PDType1Font font, float fontSize) throws IOException {
            List<String> lines = wrapText(text, font, fontSize, PAGE_WIDTH);
            float lineHeight = fontSize + LINE_GAP;
            ensureSpace(lineHeight * Math.max(lines.size(), 1));
            for (String line : lines) {
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(PAGE_MARGIN, currentY);
                stream.showText(line);
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
            String normalized = text == null ? "" : text.replace("\r", "");
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
    }
}

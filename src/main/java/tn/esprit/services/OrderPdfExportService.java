package tn.esprit.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import tn.esprit.entities.Order;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class OrderPdfExportService {
    private static final PDType1Font TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font HEADER_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private static final float PAGE_MARGIN = 42f;
    private static final float PAGE_TOP = PDRectangle.LETTER.getHeight() - PAGE_MARGIN;
    private static final float PAGE_BOTTOM = PAGE_MARGIN;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth() - (PAGE_MARGIN * 2f);
    private static final float LINE_GAP = 4f;

    public void exportOrders(
            Path targetPath,
            List<Order> orders,
            Function<Integer, String> productNameResolver
    ) throws IOException {
        ensureTarget(targetPath);

        List<Order> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort(Comparator
                .comparing(Order::getOrderDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Order::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        try (PDDocument document = new PDDocument()) {
            DocumentWriter writer = new DocumentWriter(document);
            writer.writeTitle("Orders export");
            writer.writeBody("Generated on " + DATE_FORMATTER.format(LocalDate.now()) + " - " + sortedOrders.size() + " order(s).");
            writer.writeBody("Delivered: " + countByStatus(sortedOrders, "DELIVERED")
                    + " | Pending: " + countByStatus(sortedOrders, "PENDING")
                    + " | Cancelled: " + countByStatus(sortedOrders, "CANCELLED"));
            writer.writeBlankLine(8f);

            for (Order order : sortedOrders) {
                writer.writeSectionTitle("Order #" + fallback(order.getId() == null ? null : String.valueOf(order.getId()), "-"));
                writer.writeBody("Product: " + fallback(resolveName(productNameResolver, order.getProductId(), "Product #" + order.getProductId()), "-"));
                writer.writeBody("Client: " + fallback(order.getClientName(), fallback(order.getContactEmail(), "-")));
                writer.writeBody("Date: " + formatDate(order.getOrderDate()));
                writer.writeBody("Quantity: " + (order.getQuantity() == null ? "-" : order.getQuantity()));
                writer.writeBody("Total: " + formatPrice(order.getTotalAmount()));
                writer.writeBody("Status: " + fallback(order.getStatus(), "-"));
                writer.writeBody("Payment method: " + fallback(order.getPaymentMethod(), "-"));
                writer.writeBody("Payment status: " + fallback(order.getPaymentStatus(), "-"));
                writer.writeBody("Email: " + fallback(order.getContactEmail(), "-"));
                writer.writeBody("Phone: " + fallback(order.getContactPhone(), "-"));
                writer.writeBody("Shipping: " + fallback(order.getShippingAddress(), "-"));
                writer.writeBody("Billing: " + fallback(order.getBillingAddress(), "-"));
                writer.writeDivider();
            }

            writer.close();
            document.save(targetPath.toFile());
        }
    }

    public void exportInvoice(Path targetPath, Invoice invoice) throws IOException {
        exportInvoice(targetPath, invoice, null);
    }

    public void exportInvoice(Path targetPath, Invoice invoice, String qrPayload) throws IOException {
        ensureTarget(targetPath);
        if (invoice == null) {
            throw new IOException("Invoice data is missing.");
        }

        try (PDDocument document = new PDDocument()) {
            DocumentWriter writer = new DocumentWriter(document);
            writer.writeTitle("Facture");
            writer.writeBody("Date: " + formatDate(invoice.invoiceDate()));
            writer.writeBody("Client: " + fallback(invoice.customerName(), "Sport Insight client"));
            writer.writeBody("Email: " + fallback(invoice.contactEmail(), "-"));
            writer.writeBody("Telephone: " + fallback(invoice.contactPhone(), "-"));
            writer.writeBody("Paiement: " + fallback(invoice.paymentMethod(), "-"));
            writer.writeBody("Livraison: " + fallback(invoice.shippingAddress(), "-"));
            writer.writeBody("Facturation: " + fallback(invoice.billingAddress(), "-"));
            if (qrPayload != null && !qrPayload.isBlank()) {
                writer.drawQrCode(createQrCode(qrPayload), 168f);
            }
            writer.writeBlankLine(10f);

            List<InvoiceLine> lines = invoice.lines() == null ? List.of() : invoice.lines();
            for (InvoiceLine line : lines) {
                writer.writeSectionTitle(fallback(line.productName(), "Produit"));
                writer.writeBody("Quantite: " + line.quantity());
                writer.writeBody("Prix unitaire: " + formatPrice(line.unitPrice()));
                writer.writeBody("Sous-total: " + formatPrice(line.subtotal()));
                if (line.size() != null && !line.size().isBlank()) {
                    writer.writeBody("Taille: " + line.size());
                }
                writer.writeDivider();
            }

            writer.writeSectionTitle("Total");
            writer.writeBody("Montant total: " + formatPrice(invoice.totalAmount()));

            writer.close();
            document.save(targetPath.toFile());
        }
    }

    private long countByStatus(List<Order> orders, String status) {
        return orders.stream()
                .filter(order -> status.equalsIgnoreCase(fallback(order.getStatus(), "")))
                .count();
    }

    private void ensureTarget(Path targetPath) throws IOException {
        if (targetPath == null) {
            throw new IOException("No destination file was selected.");
        }
        Path parent = targetPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private String resolveName(Function<Integer, String> resolver, Integer id, String fallback) {
        if (resolver == null || id == null) {
            return fallback;
        }
        String resolved = resolver.apply(id);
        return resolved == null || resolved.isBlank() ? fallback : resolved;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : DATE_FORMATTER.format(value);
    }

    private String formatPrice(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return safe.toPlainString() + " DT";
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
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

    private BufferedImage createQrCode(String payload) throws IOException {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 512, 512, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new IOException("Unable to generate invoice QR code.", e);
        }
    }

    public record Invoice(
            String customerName,
            String contactEmail,
            String contactPhone,
            String paymentMethod,
            String shippingAddress,
            String billingAddress,
            LocalDate invoiceDate,
            BigDecimal totalAmount,
            List<InvoiceLine> lines
    ) {}

    public record InvoiceLine(
            String productName,
            String size,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}

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

        private void drawQrCode(BufferedImage image, float size) throws IOException {
            if (image == null) {
                return;
            }
            ensureSpace(size + 12f);
            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
            float x = PAGE_MARGIN + PAGE_WIDTH - size;
            float y = currentY - size;
            stream.drawImage(pdfImage, x, y, size, size);
            currentY = y - 12f;
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

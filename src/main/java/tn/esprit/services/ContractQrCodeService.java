package tn.esprit.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Sponsor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ContractQrCodeService {
    public BufferedImage generateQrImage(ContratSponsor contrat, Sponsor sponsor, Equipe equipe) throws IOException {
        if (contrat == null) {
            throw new IOException("No contract selected.");
        }
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    buildPayload(contrat, sponsor, equipe),
                    BarcodeFormat.QR_CODE,
                    220,
                    220
            );
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (Exception e) {
            throw new IOException("Could not generate the QR code image.", e);
        }
    }

    public void generateQrCode(Path target, ContratSponsor contrat, Sponsor sponsor, Equipe equipe) throws IOException {
        if (target == null) {
            throw new IOException("QR code target path is missing.");
        }
        if (contrat == null) {
            throw new IOException("No contract selected.");
        }

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            BufferedImage image = generateQrImage(contrat, sponsor, equipe);
            ImageIO.write(image, "PNG", target.toFile());
        } catch (Exception e) {
            throw new IOException("Could not generate the QR code.", e);
        }
    }

    public SponsorQrData decodeQrCode(Path source) throws IOException {
        if (source == null || !Files.exists(source)) {
            throw new IOException("QR code image not found.");
        }

        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            throw new IOException("Unsupported QR code image.");
        }

        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            return parsePayload(result.getText());
        } catch (NotFoundException e) {
            throw new IOException("No QR code was detected in the selected image.", e);
        }
    }

    private String buildPayload(ContratSponsor contrat, Sponsor sponsor, Equipe equipe) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("contractId", valueOf(contrat.getId()));
        values.put("sponsorId", sponsor == null ? null : valueOf(sponsor.getId()));
        values.put("sponsorName", sponsor == null ? null : sponsor.getNom());
        values.put("teamName", equipe == null ? null : equipe.getNom());
        values.put("status", contrat.getStatut());
        values.put("endDate", contrat.getDateFin() == null ? null : contrat.getDateFin().toString());

        StringBuilder payload = new StringBuilder("SPORT_INSIGHT_CONTRACT");
        values.forEach((key, value) -> payload
                .append('\n')
                .append(key)
                .append('=')
                .append(escape(value)));
        return payload.toString();
    }

    private SponsorQrData parsePayload(String payload) {
        if (payload == null || !payload.startsWith("SPORT_INSIGHT_CONTRACT")) {
            return new SponsorQrData(null, null, null, null, payload);
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (String line : payload.split("\\R")) {
            int separatorIndex = line.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = line.substring(0, separatorIndex).trim();
            String value = unescape(line.substring(separatorIndex + 1));
            values.put(key, value);
        }

        Integer contractId = parseInteger(values.get("contractId"));
        Integer sponsorId = parseInteger(values.get("sponsorId"));
        LocalDate endDate = parseDate(values.get("endDate"));

        return new SponsorQrData(
                contractId,
                sponsorId,
                values.get("sponsorName"),
                values.get("teamName"),
                endDate == null ? null : endDate.toString()
        );
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String valueOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String unescape(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }

    public record SponsorQrData(
            Integer contractId,
            Integer sponsorId,
            String sponsorName,
            String teamName,
            String endDate
    ) {
    }
}

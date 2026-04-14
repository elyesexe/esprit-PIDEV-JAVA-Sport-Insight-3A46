package tn.esprit.tools;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JoueurAvatarGenerator {
    private static final int AVATAR_SIZE = 256;

    private JoueurAvatarGenerator() {
    }

    public static String ensureAvatarPath(long externalId, String prenom, String nom) {
        if (externalId <= 0) {
            return null;
        }

        String initials = buildInitials(prenom, nom);
        String fileName = "fd-player-" + externalId + ".png";
        Path output = baseDirectory().resolve(fileName);

        try {
            if (Files.exists(output)) {
                return output.toAbsolutePath().toString();
            }
            Files.createDirectories(output.getParent());
            writeAvatarPng(output, initials, externalId);
            return output.toAbsolutePath().toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path baseDirectory() {
        return Path.of(System.getProperty("user.home"), ".sport-insight", "avatars", "joueurs");
    }

    private static String buildInitials(String prenom, String nom) {
        String p = clean(prenom);
        String n = clean(nom);
        StringBuilder sb = new StringBuilder();
        if (p != null && !p.isEmpty()) {
            sb.append(Character.toUpperCase(p.charAt(0)));
        }
        if (n != null && !n.isEmpty()) {
            sb.append(Character.toUpperCase(n.charAt(0)));
        }
        return sb.isEmpty() ? "J" : sb.toString();
    }

    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    private static void writeAvatarPng(Path output, String initials, long seed) throws IOException {
        BufferedImage image = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color[] palette = buildPalette(seed);
            Ellipse2D circle = new Ellipse2D.Double(0, 0, AVATAR_SIZE, AVATAR_SIZE);

            GradientPaint gradient = new GradientPaint(0, 0, palette[0], AVATAR_SIZE, AVATAR_SIZE, palette[1]);
            g.setPaint(gradient);
            g.fill(circle);

            g.setColor(new Color(255, 255, 255, 45));
            g.setStroke(new BasicStroke(6f));
            g.draw(new Ellipse2D.Double(6, 6, AVATAR_SIZE - 12, AVATAR_SIZE - 12));

            g.setColor(new Color(255, 255, 255, 230));
            Font font = new Font("Segoe UI", Font.BOLD, initials.length() == 1 ? 108 : 96);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(initials);
            int x = (AVATAR_SIZE - textWidth) / 2;
            int y = (AVATAR_SIZE - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(initials, x, y);
        } finally {
            g.dispose();
        }

        ImageIO.write(image, "png", output.toFile());
    }

    private static Color[] buildPalette(long seed) {
        // Deterministic HSB palette from external id.
        float hue = (float) ((seed % 360) / 360.0);
        Color a = Color.getHSBColor(hue, 0.55f, 0.55f);
        Color b = Color.getHSBColor((hue + 0.12f) % 1.0f, 0.55f, 0.75f);
        return new Color[] { a, b };
    }
}


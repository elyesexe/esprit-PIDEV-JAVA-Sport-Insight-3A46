package tn.esprit.services;

import tn.esprit.entities.Entrainement;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.User;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public class EvaluationNotificationService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DEFAULT_GMAIL_SENDER = "bennjimamariem99@gmail.com";
    private static final String LOCAL_PROPERTIES_FILE = "evaluation-mail.local.properties";
    private static final String USER_HOME_PROPERTIES_FILE = ".sport-insight/evaluation-mail.local.properties";
    private static final String CLASSPATH_PROPERTIES = "/evaluation-mail.properties";
    private static final String[] CHECKED_LOCATIONS = {
            "evaluation-mail.local.properties",
            "src/main/resources/evaluation-mail.local.properties",
            ".sport-insight/evaluation-mail.local.properties"
    };

    public DeliveryResult sendEvaluationNotification(User recipient, Entrainement training, Evaluation evaluation, boolean updated) {
        String recipientEmail = trimToNull(recipient == null ? null : recipient.getEmail());
        if (recipientEmail == null) {
            return new DeliveryResult(false, "Adresse e-mail du joueur introuvable.");
        }

        SmtpConfig config = SmtpConfig.load();
        if (!config.isConfigured()) {
            return new DeliveryResult(false,
                    "SMTP configuration missing. Create evaluation-mail.local.properties at project root, or ~/.sport-insight/evaluation-mail.local.properties, then fill in SMTP values. " +
                            config.describeConfigurationStatus());
        }

        String subject = encodeSubject((updated ? "Mise a jour" : "Nouvelle") + " evaluation | Sport Insight");
        String body = buildBody(recipient, training, evaluation, updated);

        try (SmtpClient client = new SmtpClient(config)) {
            client.sendMail(config.fromAddress(), recipientEmail, subject, body);
            return new DeliveryResult(true, "Email envoye a " + recipientEmail + ".");
        } catch (IOException e) {
            String message = e.getMessage() == null ? "Erreur SMTP inconnue." : e.getMessage();
            if (message.contains("535")) {
                message = "Authentification Gmail refusee (535). Verifie que le compte utilise un mot de passe d'application valide et que la verification en deux etapes est active.";
            }
            return new DeliveryResult(false, "Envoi email echoue: " + message);
        }
    }

    private String buildBody(User recipient, Entrainement training, Evaluation evaluation, boolean updated) {
        String recipientName = safeName(recipient == null ? null : recipient.getDisplayName(), "Joueur");
        String trainingLabel = buildTrainingLabel(training, evaluation);
        String dateLabel = training != null && training.getDateEntrainement() != null
                ? DATE_FORMAT.format(training.getDateEntrainement())
                : "-";
        String comment = trimToNull(evaluation == null ? null : evaluation.getCommentaire());
        String average = evaluation == null ? "-" : String.format(Locale.US, "%.2f / 20",
                (evaluation.getNotePhysique() + evaluation.getNoteTechnique() + evaluation.getNoteTactique()) / 3.0);

        StringBuilder body = new StringBuilder();
        body.append("Bonjour ").append(recipientName).append(",\n\n");
        body.append("Votre coach a ").append(updated ? "mis a jour" : "enregistre")
                .append(" votre evaluation sur Sport Insight.\n\n");
        body.append("Entrainement : ").append(trainingLabel).append('\n');
        body.append("Date : ").append(dateLabel).append('\n');
        body.append("Note physique : ").append(formatScore(evaluation == null ? null : evaluation.getNotePhysique())).append("/20\n");
        body.append("Note technique : ").append(formatScore(evaluation == null ? null : evaluation.getNoteTechnique())).append("/20\n");
        body.append("Note tactique : ").append(formatScore(evaluation == null ? null : evaluation.getNoteTactique())).append("/20\n");
        body.append("Moyenne : ").append(average).append("\n\n");
        body.append("Commentaire : ").append(comment == null ? "Aucun commentaire" : comment).append("\n\n");
        body.append("Sport Insight\n");
        body.append("Gardez votre progression a jour et continuez a suivre vos performances.");
        return body.toString();
    }

    private String buildTrainingLabel(Entrainement training, Evaluation evaluation) {
        if (training == null) {
            return evaluation == null || evaluation.getEntrainementId() == null
                    ? "-"
                    : "Entrainement #" + evaluation.getEntrainementId();
        }
        String type = trimToNull(training.getType());
        String lieu = trimToNull(training.getLieu());
        StringBuilder label = new StringBuilder();
        if (type != null) {
            label.append(type);
        }
        if (lieu != null) {
            if (label.length() > 0) {
                label.append(" - ");
            }
            label.append(lieu);
        }
        if (label.length() == 0 && training.getId() != null) {
            label.append("Entrainement #").append(training.getId());
        }
        return label.length() == 0 ? "-" : label.toString();
    }

    private static String formatScore(Double value) {
        return value == null ? "-" : String.format(Locale.US, "%.1f", value);
    }

    private static String encodeSubject(String subject) {
        byte[] encoded = Base64.getEncoder().encode(subject.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + new String(encoded, StandardCharsets.US_ASCII) + "?=";
    }

    private static String safeName(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public record DeliveryResult(boolean sent, String message) {
    }

    private static final class SmtpConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final String fromAddress;
        private final boolean useStartTls;
        private final boolean useSsl;

        private SmtpConfig(String host, int port, String username, String password, String fromAddress, boolean useStartTls, boolean useSsl) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.fromAddress = fromAddress;
            this.useStartTls = useStartTls;
            this.useSsl = useSsl;
        }

        static SmtpConfig load() {
            Properties props = new Properties();
            loadFromFile(props, Path.of(LOCAL_PROPERTIES_FILE));
            loadFromFile(props, Path.of(System.getProperty("user.home"), USER_HOME_PROPERTIES_FILE));
            loadFromFile(props, Path.of("src/main/resources/evaluation-mail.local.properties"));
            loadFromClasspath(props, CLASSPATH_PROPERTIES);

            String host = firstNonBlank(
                    System.getProperty("sport.insight.smtp.host"),
                    System.getenv("SPORT_INSIGHT_SMTP_HOST"),
                    props.getProperty("smtp.host"),
                    props.getProperty("mail.smtp.host"),
                    "smtp.gmail.com");
            int port = parseInt(firstNonBlank(
                    System.getProperty("sport.insight.smtp.port"),
                    System.getenv("SPORT_INSIGHT_SMTP_PORT"),
                    props.getProperty("smtp.port"),
                    props.getProperty("mail.smtp.port")), 587);
            String username = firstNonBlank(
                    System.getProperty("sport.insight.smtp.username"),
                    System.getenv("SPORT_INSIGHT_SMTP_USERNAME"),
                    props.getProperty("smtp.username"),
                    props.getProperty("mail.sender"),
                    props.getProperty("mail.smtp.username"),
                    DEFAULT_GMAIL_SENDER);
            String password = firstNonBlank(
                    System.getProperty("sport.insight.smtp.password"),
                    System.getenv("SPORT_INSIGHT_SMTP_PASSWORD"),
                    props.getProperty("smtp.password"),
                    props.getProperty("mail.password"),
                    props.getProperty("mail.smtp.password"));
            String fromAddress = firstNonBlank(
                    System.getProperty("sport.insight.smtp.from"),
                    System.getenv("SPORT_INSIGHT_SMTP_FROM"),
                    props.getProperty("smtp.from"),
                    props.getProperty("mail.sender"),
                    username,
                    DEFAULT_GMAIL_SENDER);
            boolean useStartTls = parseBoolean(firstNonBlank(
                    System.getProperty("sport.insight.smtp.tls"),
                    System.getenv("SPORT_INSIGHT_SMTP_TLS"),
                    props.getProperty("smtp.tls"),
                    props.getProperty("mail.smtp.starttls.enable")), true);
            boolean useSsl = parseBoolean(firstNonBlank(
                    System.getProperty("sport.insight.smtp.ssl"),
                    System.getenv("SPORT_INSIGHT_SMTP_SSL"),
                    props.getProperty("smtp.ssl")), port == 465);
            return new SmtpConfig(host, port, username, normalizePassword(password), fromAddress, useStartTls, useSsl);
        }

        boolean isConfigured() {
            return trimToNull(host) != null
                    && trimToNull(username) != null
                    && trimToNull(password) != null
                    && trimToNull(fromAddress) != null
                    && port > 0;
        }

        String describeConfigurationStatus() {
            return "SMTP expected in: " + String.join(", ", CHECKED_LOCATIONS);
        }

        String host() { return host; }
        int port() { return port; }
        String username() { return username; }
        String password() { return password; }
        String fromAddress() { return fromAddress; }
        boolean useStartTls() { return useStartTls; }
        boolean useSsl() { return useSsl; }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                String trimmed = trimToNull(value);
                if (trimmed != null) {
                    return trimmed;
                }
            }
            return null;
        }

        private static int parseInt(String value, int fallback) {
            try {
                return value == null ? fallback : Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private static boolean parseBoolean(String value, boolean fallback) {
            if (value == null) {
                return fallback;
            }
            return Boolean.parseBoolean(value.trim());
        }

        private static String normalizePassword(String value) {
            String trimmed = trimToNull(value);
            if (trimmed == null) {
                return null;
            }
            return trimmed.replaceAll("\\s+", "");
        }

        private static void loadFromFile(Properties props, Path path) {
            if (path == null || !Files.exists(path)) {
                return;
            }
            try (var input = Files.newInputStream(path)) {
                props.load(input);
            } catch (IOException ignored) {
                // Best effort only.
            }
        }

        private static void loadFromClasspath(Properties props, String resourcePath) {
            try (var input = EvaluationNotificationService.class.getResourceAsStream(resourcePath)) {
                if (input != null) {
                    props.load(input);
                }
            } catch (IOException ignored) {
                // Best effort only.
            }
        }
    }

    private static final class SmtpClient implements Closeable {
        private final SmtpConfig config;
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;

        private SmtpClient(SmtpConfig config) throws IOException {
            this.config = Objects.requireNonNull(config, "config");
            open();
        }

        private void open() throws IOException {
            if (config.useSsl()) {
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = factory.createSocket(config.host(), config.port());
                ((SSLSocket) socket).startHandshake();
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(config.host(), config.port()), 30000);
            }
            socket.setSoTimeout(30000);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            expect(220);
            sendEhlo();
            if (config.useStartTls() && !config.useSsl()) {
                sendLine("STARTTLS");
                expect(220);
                upgradeToTls();
                sendEhlo();
            }
            authenticateIfNeeded();
        }

        void sendMail(String from, String to, String subject, String body) throws IOException {
            sendLine("MAIL FROM:<" + from + ">");
            expect(250);
            sendLine("RCPT TO:<" + to + ">");
            expect(250, 251);
            sendLine("DATA");
            expect(354);
            sendLine("From: " + from);
            sendLine("To: " + to);
            sendLine("Subject: " + subject);
            sendLine("MIME-Version: 1.0");
            sendLine("Content-Type: text/plain; charset=UTF-8");
            sendLine("Content-Transfer-Encoding: 8bit");
            sendLine("");
            writeBody(body);
            sendLine(".");
            expect(250);
            sendLine("QUIT");
            expect(221);
        }

        private void sendEhlo() throws IOException {
            sendLine("EHLO localhost");
            expect(250);
        }

        private void authenticateIfNeeded() throws IOException {
            if (trimToNull(config.username()) == null || trimToNull(config.password()) == null) {
                return;
            }
            try {
                String authPayload = "\u0000" + config.username() + "\u0000" + config.password();
                sendLine("AUTH PLAIN " + Base64.getEncoder().encodeToString(authPayload.getBytes(StandardCharsets.UTF_8)));
                expect(235);
                return;
            } catch (IOException firstFailure) {
                String detail = firstFailure.getMessage() == null ? "" : firstFailure.getMessage();
                if (!(detail.contains("535") || detail.contains("534") || detail.contains("530") || detail.contains("504"))) {
                    throw firstFailure;
                }
            }

            sendLine("AUTH LOGIN");
            expect(334);
            sendLine(Base64.getEncoder().encodeToString(config.username().getBytes(StandardCharsets.UTF_8)));
            expect(334);
            sendLine(Base64.getEncoder().encodeToString(config.password().getBytes(StandardCharsets.UTF_8)));
            expect(235);
        }

        private void upgradeToTls() throws IOException {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = factory.createSocket(socket, config.host(), config.port(), true);
            ((SSLSocket) socket).startHandshake();
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private void writeBody(String body) throws IOException {
            String normalized = body == null ? "" : body.replace("\r\n", "\n").replace('\r', '\n');
            String[] lines = normalized.split("\n", -1);
            for (String line : lines) {
                if (line.startsWith(".")) {
                    sendLine("." + line);
                } else {
                    sendLine(line);
                }
            }
        }

        private void sendLine(String line) throws IOException {
            writer.write(line);
            writer.write("\r\n");
            writer.flush();
        }

        private void expect(int... allowedCodes) throws IOException {
            String response = readResponse();
            int code = parseCode(response);
            for (int allowed : allowedCodes) {
                if (code == allowed) {
                    return;
                }
            }
            throw new IOException("SMTP response " + response + " unexpected");
        }

        private String readResponse() throws IOException {
            String line;
            String lastLine = null;
            do {
                line = reader.readLine();
                if (line == null) {
                    throw new IOException("SMTP server disconnected");
                }
                lastLine = line;
            } while (line.length() >= 4 && line.charAt(3) == '-');
            if (lastLine == null || lastLine.length() < 3) {
                throw new IOException("Invalid SMTP response");
            }
            return lastLine;
        }

        private int parseCode(String response) throws IOException {
            if (response == null || response.length() < 3) {
                throw new IOException("Invalid SMTP response");
            }
            try {
                return Integer.parseInt(response.substring(0, 3));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid SMTP response: " + response, e);
            }
        }

        @Override
        public void close() throws IOException {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}

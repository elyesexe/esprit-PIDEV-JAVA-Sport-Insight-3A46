package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Sends emails via Gmail SMTP using an App Password.
 *
 * ── Setup (one-time, 2 minutes) ────────────────────────────────────────────
 * 1. Go to myaccount.google.com → Security → 2-Step Verification → turn ON
 * 2. Go to myaccount.google.com → Security → App Passwords
 * 3. Select app: "Mail", device: "Windows Computer" → Generate
 * 4. Copy the 16-character password (e.g. "abcd efgh ijkl mnop")
 * 5. Paste it (without spaces) into SENDER_APP_PASSWORD below
 * 6. Set SENDER_EMAIL to the Gmail address you used
 *
 * ── Security note ──────────────────────────────────────────────────────────
 * Never commit the App Password to version control.
 * Move it to a config file or environment variable before going to production:
 *
 *   String pass = System.getenv("GMAIL_APP_PASSWORD");
 * ──────────────────────────────────────────────────────────────────────────
 */
public class EmailService {

    // ── Configure these two values ────────────────────────────────────────────
    private static final String SENDER_EMAIL        = "syrinesaiidaoui@gmail.com";
    private static final String SENDER_APP_PASSWORD = "lhjlqkpardrgulut";   // no spaces
    // ─────────────────────────────────────────────────────────────────────────

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    private final Session session;

    public EmailService() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD);
            }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Send a password-reset OTP email.
     *
     * @param toEmail   recipient address
     * @param firstName recipient's first name (used in greeting)
     * @param otp       the 6-digit code
     * @throws MessagingException if SMTP fails
     */
    public void sendPasswordResetOtp(String toEmail, String firstName, String otp)
            throws MessagingException {

        String subject = "Sport Insight — your password reset code";
        String body    = buildOtpEmailBody(firstName, otp);
        send(toEmail, subject, body);
    }

    /**
     * Generic send — use for any future email needs.
     */
    public void send(String toEmail, String subject, String htmlBody)
            throws MessagingException {

        Message message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(SENDER_EMAIL, "Sport Insight"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(SENDER_EMAIL));
        }

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(message);
    }
    // ── Email body ────────────────────────────────────────────────────────────

    private String buildOtpEmailBody(String firstName, String otp) {
        return """
            <div style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:32px 24px;">
              <h2 style="color:#1a1a2e;margin-bottom:8px;">Password reset</h2>
              <p style="color:#555;">Hi %s,</p>
              <p style="color:#555;">
                Use the code below to reset your Sport Insight password.
                It expires in <strong>15 minutes</strong>.
              </p>
              <div style="background:#f4f4f8;border-radius:10px;padding:24px;
                          text-align:center;margin:24px 0;">
                <span style="font-size:36px;font-weight:bold;letter-spacing:10px;
                             color:#5865f2;">%s</span>
              </div>
              <p style="color:#555;font-size:13px;">
                If you didn't request this, you can safely ignore this email.
              </p>
              <hr style="border:none;border-top:1px solid #eee;margin:24px 0;"/>
              <p style="color:#aaa;font-size:12px;">Sport Insight platform</p>
            </div>
            """.formatted(firstName == null || firstName.isBlank() ? "there" : firstName, otp);
    }
}

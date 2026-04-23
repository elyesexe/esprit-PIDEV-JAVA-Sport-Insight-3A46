package tn.esprit.services;

import org.json.JSONObject;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Google OAuth 2.0 for a Desktop JavaFX app — no SDK, no web server.
 *
 * Flow:
 *   1. Build the Google authorization URL (with PKCE for security).
 *   2. Open the system browser so the user can sign in and grant consent.
 *   3. Google redirects to http://localhost:8888/callback?code=…
 *   4. A tiny ServerSocket listens on port 8888, captures the code,
 *      sends a "you can close this tab" HTML page, and closes the socket.
 *   5. Exchange the code for an access token via HTTPS POST.
 *   6. Call the Google userinfo endpoint to retrieve the user's email.
 *
 * ── Google Cloud Console setup (5 minutes, one-time) ─────────────────────
 *
 *   1. Go to https://console.cloud.google.com/
 *   2. Create a project (or select an existing one)
 *   3. Enable the "Google People API" (or "Google+ API")
 *   4. Go to APIs & Services → Credentials → Create Credentials
 *      → OAuth 2.0 Client ID
 *   5. Application type: Desktop app
 *   6. Name: Sport Insight (or anything)
 *   7. Click Create — download the JSON or copy the Client ID and Secret
 *   8. Go to OAuth consent screen → add your Gmail as a Test User
 *      (required while the app is in "Testing" mode)
 *   9. Paste the Client ID and Secret into the constants below.
 *
 * ── Security ─────────────────────────────────────────────────────────────
 * Move CLIENT_ID and CLIENT_SECRET to a config file or environment variable:
 *   String id  = System.getenv("GOOGLE_CLIENT_ID");
 *   String sec = System.getenv("GOOGLE_CLIENT_SECRET");
 * ──────────────────────────────────────────────────────────────────────────
 */
public class GoogleOAuthService {

    // ── Configure these two values from Google Cloud Console ─────────────────
    private static final String CLIENT_ID     = "464851299567-794fifqd0vuv697osj23m0ovrvj908he.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "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";
    // ─────────────────────────────────────────────────────────────────────────

    private static final String REDIRECT_URI  = "http://localhost:8888/callback";
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT= "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL  = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final int    CALLBACK_PORT = 8888;
    private static final int    TIMEOUT_SECS  = 120;

    private final HttpClient http = HttpClient.newHttpClient();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Run the full OAuth flow asynchronously.
     *
     * @param onSuccess called on the calling thread with the user's email
     * @param onError   called on the calling thread with an error message
     */
    public void startLoginFlow(Consumer<String> onSuccess, Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                String email = runFlow();
                onSuccess.accept(email);
            } catch (Exception e) {
                onError.accept(e.getMessage());
            }
        });
    }

    // ── Core flow ─────────────────────────────────────────────────────────────

    private String runFlow() throws Exception {
        // 1 — Generate PKCE verifier + challenge
        String codeVerifier  = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String state         = generateState();

        // 2 — Build authorization URL
        String authUrl = AUTH_ENDPOINT + "?"
            + "client_id="             + encode(CLIENT_ID)
            + "&redirect_uri="         + encode(REDIRECT_URI)
            + "&response_type=code"
            + "&scope="                + encode("openid email profile")
            + "&state="                + state
            + "&code_challenge="       + codeChallenge
            + "&code_challenge_method=S256"
            + "&access_type=offline"
            + "&prompt=select_account";

        // 3 — Open browser
        Desktop.getDesktop().browse(new URI(authUrl));

        // 4 — Listen for callback
        String code = waitForCode(state, TIMEOUT_SECS);
        if (code == null) throw new TimeoutException("Google login timed out after " + TIMEOUT_SECS + " seconds.");

        // 5 — Exchange code for tokens
        String accessToken = exchangeCodeForToken(code, codeVerifier);

        // 6 — Fetch email
        return fetchEmail(accessToken);
    }

    // ── Step 4: listen for the redirect ──────────────────────────────────────

    private String waitForCode(String expectedState, int timeoutSecs) throws IOException {
        try (ServerSocket server = new ServerSocket(CALLBACK_PORT)) {
            server.setSoTimeout(timeoutSecs * 1000);
            try (Socket socket = server.accept()) {
                BufferedReader  reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter     writer = new PrintWriter(socket.getOutputStream(), true);

                String requestLine = reader.readLine();   // "GET /callback?code=...&state=... HTTP/1.1"
                if (requestLine == null) return null;

                // Parse query string
                String query = requestLine.split(" ")[1];   // "/callback?code=...&state=..."
                Map<String, String> params = parseQuery(query.contains("?") ? query.split("\\?", 2)[1] : "");

                // Send a friendly response page
                String html = "<html><body style='font-family:sans-serif;text-align:center;padding:60px'>"
                            + "<h2>Authentication successful!</h2>"
                            + "<p>You can close this tab and return to Sport Insight.</p>"
                            + "</body></html>";
                writer.println("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html);

                String returnedState = params.get("state");
                if (!expectedState.equals(returnedState)) return null;   // CSRF check

                return params.get("code");
            }
        }
    }

    // ── Step 5: exchange code → access token ─────────────────────────────────

    private String exchangeCodeForToken(String code, String verifier) throws Exception {
        String body = "client_id="     + encode(CLIENT_ID)
                    + "&client_secret="+ encode(CLIENT_SECRET)
                    + "&code="         + encode(code)
                    + "&code_verifier="+ encode(verifier)
                    + "&grant_type=authorization_code"
                    + "&redirect_uri=" + encode(REDIRECT_URI);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_ENDPOINT))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new IOException("Token exchange failed HTTP " + resp.statusCode() + ": " + resp.body());

        JSONObject json = new JSONObject(resp.body());
        return json.getString("access_token");
    }

    // ── Step 6: fetch email ───────────────────────────────────────────────────

    private String fetchEmail(String accessToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(USERINFO_URL))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new IOException("Userinfo fetch failed HTTP " + resp.statusCode());

        JSONObject json = new JSONObject(resp.body());
        String email = json.optString("email");
        if (email.isBlank()) throw new IOException("Google did not return an email address.");
        return email;
    }

    // ── PKCE helpers ──────────────────────────────────────────────────────────

    private String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String verifier) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                                     .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private String generateState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new LinkedHashMap<>();
        if (query == null || query.isBlank()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try { map.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8)); }
                catch (Exception ignored) {}
            }
        }
        return map;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

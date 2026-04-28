package tn.esprit.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tn.esprit.services.MatchLiveCompanionResponse;
import tn.esprit.services.MatchNotFoundException;
import tn.esprit.services.MatchsService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchApiController implements HttpHandler {
    private static final Pattern LIVE_COMPANION_ROUTE = Pattern.compile("^/api/matchs/(\\d+)/live-companion/?$");

    private final MatchsService matchsService;
    private final ObjectMapper objectMapper;

    public MatchApiController() throws SQLException {
        this(new MatchsService(), new ObjectMapper());
    }

    MatchApiController(MatchsService matchsService, ObjectMapper objectMapper) {
        this.matchsService = matchsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Matcher matcher = LIVE_COMPANION_ROUTE.matcher(exchange.getRequestURI().getPath());
        if (!matcher.matches()) {
            sendJson(exchange, 404, errorBody(404, "Not found."));
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, errorBody(405, "Method not allowed."));
            return;
        }

        int matchId;
        try {
            matchId = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, errorBody(400, "Invalid match id."));
            return;
        }

        try {
            MatchLiveCompanionResponse response = matchsService.getLiveCompanion(matchId);
            sendJson(exchange, 200, objectMapper.writeValueAsString(response));
        } catch (MatchNotFoundException e) {
            sendJson(exchange, 404, errorBody(404, e.getMessage()));
        } catch (SQLException e) {
            sendJson(exchange, 500, errorBody(500, "Unable to load match live companion."));
        }
    }

    private String errorBody(int status, String message) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("status", status);
        payload.put("message", message);
        return objectMapper.writeValueAsString(payload);
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, max-age=0");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}

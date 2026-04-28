package tn.esprit.services;

import com.sun.net.httpserver.HttpServer;
import tn.esprit.Controller.MatchApiController;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;

public class MatchApiServer implements AutoCloseable {
    private final HttpServer server;
    private final String baseUrl;

    public MatchApiServer(int port) throws IOException, SQLException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/api/matchs", new MatchApiController());
        server.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "sport-insight-match-api");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public String baseUrl() {
        return baseUrl;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}

package tn.esprit.mains;

import tn.esprit.services.MatchApiServer;

public class MatchApiMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? parsePort(args[0]) : 8080;
        MatchApiServer server = new MatchApiServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "sport-insight-match-api-shutdown"));
        System.out.println("Match API server running on " + server.baseUrl());
        Thread.currentThread().join();
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 8080;
        }
    }
}

package tn.esprit.assistant;

import java.text.Normalizer;
import java.util.List;

public final class AssistantScreenCatalog {
    private static final ScreenMeta LOGIN = new ScreenMeta(
            "Sign in",
            "Use this screen to authenticate and unlock the full Sport Insight workspace.",
            List.of(
                    "Sign in with an existing account.",
                    "Open Sign up if the user needs a new account.",
                    "Ask the assistant for a module overview before logging in."
            ),
            List.of("Explain the login flow", "Open sign up", "What modules exist?")
    );
    private static final ScreenMeta SIGNUP = new ScreenMeta(
            "Sign up",
            "Create a new user account before entering the application.",
            List.of(
                    "Fill in the registration fields and submit the form.",
                    "Return to Sign in when the account already exists.",
                    "Ask the assistant which modules a regular user can access."
            ),
            List.of("Explain sign up", "Open sign in", "What can a user do?")
    );
    private static final ScreenMeta HOME = new ScreenMeta(
            "Home",
            "The home page is the fastest way to explore teams, players, matches, announcements, training, sponsors, and store tools.",
            List.of(
                    "Use the large tiles to jump into a module.",
                    "Search by words like team, player, match, sponsor, or store.",
                    "Open the assistant for guided navigation across the app."
            ),
            List.of("What can I do here?", "Open teams", "Open matches")
    );
    private static final ScreenMeta ADMIN = new ScreenMeta(
            "Admin shell",
            "The admin workspace centralizes dashboard access, moderation, and CRUD screens for the main modules.",
            List.of(
                    "Use the left sidebar to jump between admin modules.",
                    "Dashboard and user moderation are admin-only areas.",
                    "Return to the user interface from the admin sidebar when needed."
            ),
            List.of("Explain the admin dashboard", "Open users", "Open products")
    );
    private static final ScreenMeta TEAMS = new ScreenMeta(
            "Equipes",
            "The teams area manages clubs, coaches, contact details, images, and competition-linked information.",
            List.of(
                    "Browse competitions and team lists.",
                    "Open details or forms to update a club profile.",
                    "Use this module when you need squad or club data."
            ),
            List.of("How do I manage teams?", "Open players", "Open leagues")
    );
    private static final ScreenMeta PLAYERS = new ScreenMeta(
            "Joueurs",
            "The players area manages athlete profiles, team assignment, positions, nationality, and external football enrichments.",
            List.of(
                    "Create or edit player profiles.",
                    "Browse the list to inspect player details.",
                    "Use this screen when linking players to teams."
            ),
            List.of("How do I add a player?", "Open teams", "Open matches")
    );
    private static final ScreenMeta MATCHES = new ScreenMeta(
            "Matchs",
            "The matches module handles fixtures, schedules, scores, statuses, and competition-linked match details.",
            List.of(
                    "Browse competitions or match lists.",
                    "Inspect a fixture to view details and lineups.",
                    "Use this area for scheduling and score tracking."
            ),
            List.of("Explain the match workflow", "Open Champions League matches", "Open Bayern vs Real Madrid details")
    );
    private static final ScreenMeta LEAGUES = new ScreenMeta(
            "Leagues",
            "League and competition screens help users browse competitions, standings, and football context around the managed data.",
            List.of(
                    "Open standings and competition views.",
                    "Use this area when exploring external football context.",
                    "Jump from leagues into teams or matches."
            ),
            List.of("Explain standings", "Open matches", "Open teams")
    );
    private static final ScreenMeta ANNOUNCEMENTS = new ScreenMeta(
            "Annonces",
            "The announcements module publishes updates and user-facing communication.",
            List.of(
                    "Browse or publish announcements depending on the screen.",
                    "Manage comments and communication around posts.",
                    "Use this module for platform news and updates."
            ),
            List.of("How do announcements work?", "Open home", "Open training")
    );
    private static final ScreenMeta TRAINING = new ScreenMeta(
            "Entrainements",
            "Training screens manage sessions, participation, and evaluation flows.",
            List.of(
                    "Create or browse training sessions.",
                    "Track participation and evaluation-related actions.",
                    "Use this module when planning practice activity."
            ),
            List.of("Explain training management", "Open players", "Open home")
    );
    private static final ScreenMeta SPONSORS = new ScreenMeta(
            "Sponsors",
            "The sponsoring module manages sponsor records, presentation screens, and contract workflows.",
            List.of(
                    "Browse sponsors and partnership details.",
                    "Use sponsor screens for contract follow-up.",
                    "Open the admin side when you need CRUD operations."
            ),
            List.of("Explain sponsor contracts", "Open store", "Open admin")
    );
    private static final ScreenMeta STORE = new ScreenMeta(
            "Store",
            "The store area covers products, catalog browsing, and order-related workflows.",
            List.of(
                    "Browse products and shopping flows.",
                    "Use admin screens to manage product or order data.",
                    "Open sponsors or announcements for adjacent user-facing content."
            ),
            List.of("Explain the store", "Open products", "Open home")
    );
    private static final ScreenMeta PROFILE = new ScreenMeta(
            "Profile",
            "The profile area contains account information and session-aware user details.",
            List.of(
                    "Review account information and personal data.",
                    "Return home to reach the functional modules.",
                    "Use the assistant to explain role-specific access."
            ),
            List.of("Explain my role", "Open home", "What can I access?")
    );
    private static final ScreenMeta DEFAULT = new ScreenMeta(
            "Sport Insight",
            "This screen belongs to the Sport Insight football management workspace.",
            List.of(
                    "Ask what you can do here for screen-specific tips.",
                    "Ask the assistant to open a module like teams, players, or matches.",
                    "Use the mic button for free offline voice input after the voice model installs."
            ),
            List.of("What can I do here?", "Open home", "What modules exist?")
    );

    private AssistantScreenCatalog() {
    }

    public static ScreenMeta resolve(String fxmlPath) {
        String normalized = normalize(fxmlPath);
        if (normalized.contains("login")) {
            return LOGIN;
        }
        if (normalized.contains("signup")) {
            return SIGNUP;
        }
        if (normalized.contains("home")) {
            return HOME;
        }
        if (normalized.contains("admin")) {
            return ADMIN;
        }
        if (normalized.contains("equipe")) {
            return TEAMS;
        }
        if (normalized.contains("joueur")) {
            return PLAYERS;
        }
        if (normalized.contains("match")) {
            return MATCHES;
        }
        if (normalized.contains("league")) {
            return LEAGUES;
        }
        if (normalized.contains("annonce")) {
            return ANNOUNCEMENTS;
        }
        if (normalized.contains("entrainement")) {
            return TRAINING;
        }
        if (normalized.contains("sponsor")) {
            return SPONSORS;
        }
        if (normalized.contains("store") || normalized.contains("product") || normalized.contains("order")) {
            return STORE;
        }
        if (normalized.contains("profile")) {
            return PROFILE;
        }
        return DEFAULT;
    }

    public static ScreenMeta resolve(AssistantNavigationTarget target) {
        if (target == null) {
            return DEFAULT;
        }
        return switch (target) {
            case LOGIN -> LOGIN;
            case SIGNUP -> SIGNUP;
            case HOME -> HOME;
            case ADMIN -> ADMIN;
            case TEAMS -> TEAMS;
            case PLAYERS -> PLAYERS;
            case MATCHES -> MATCHES;
            case LEAGUES -> LEAGUES;
            case ANNOUNCEMENTS -> ANNOUNCEMENTS;
            case TRAINING -> TRAINING;
            case SPONSORS -> SPONSORS;
            case STORE -> STORE;
            case PROFILE -> PROFILE;
        };
    }

    private static String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        return Normalizer.normalize(rawText, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record ScreenMeta(
            String title,
            String description,
            List<String> tips,
            List<String> quickPrompts
    ) {
    }
}

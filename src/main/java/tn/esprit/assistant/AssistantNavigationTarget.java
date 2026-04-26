package tn.esprit.assistant;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum AssistantNavigationTarget {
    LOGIN(
            "Login",
            "/tn/esprit/views/login-view.fxml",
            "/tn/esprit/styles/auth-theme.css",
            "Sport Insight | Sign in",
            false,
            false,
            List.of("login", "log in", "sign in", "signin", "connexion")
    ),
    SIGNUP(
            "Sign up",
            "/tn/esprit/views/signup-view.fxml",
            "/tn/esprit/styles/auth-theme.css",
            "Sport Insight | Sign up",
            false,
            false,
            List.of("signup", "sign up", "register", "registration", "inscription")
    ),
    HOME(
            "Home",
            "/tn/esprit/views/home-view.fxml",
            "/tn/esprit/styles/home-theme.css",
            "Sport Insight | Accueil",
            true,
            false,
            List.of("home", "accueil", "dashboard home", "main page")
    ),
    ADMIN(
            "Admin",
            "/tn/esprit/views/admin-shell.fxml",
            "/tn/esprit/styles/admin-theme-fixed.css",
            "Sport Insight | Admin",
            true,
            true,
            List.of("admin", "dashboard", "moderation", "back office")
    ),
    TEAMS(
            "Equipes",
            "/tn/esprit/views/equipe-competitions-view.fxml",
            "/tn/esprit/styles/equipe-theme.css",
            "Equipes | Competitions",
            true,
            false,
            List.of("team", "teams", "club", "clubs", "equipe", "equipes")
    ),
    PLAYERS(
            "Joueurs",
            "/tn/esprit/views/joueur-crud-view.fxml",
            "/tn/esprit/styles/joueur-theme.css",
            "Joueurs | Sport Insight",
            true,
            false,
            List.of("player", "players", "joueur", "joueurs", "squad")
    ),
    MATCHES(
            "Matchs",
            "/tn/esprit/views/match-competitions-view.fxml",
            "/tn/esprit/styles/match-theme.css",
            "Matchs | Competitions",
            true,
            false,
            List.of("match", "matches", "matchs", "fixture", "fixtures", "game", "games")
    ),
    LEAGUES(
            "Leagues",
            "/tn/esprit/views/league-competitions-view.fxml",
            "/tn/esprit/styles/league-theme.css",
            "Leagues | Top 5",
            true,
            false,
            List.of("league", "leagues", "competition", "competitions", "standings", "table")
    ),
    ANNOUNCEMENTS(
            "Annonces",
            "/tn/esprit/views/annonce-user-view.fxml",
            "/tn/esprit/styles/annonce-theme.css",
            "Anonce | Sport Insight",
            true,
            false,
            List.of("announcement", "announcements", "annonce", "annonces", "news", "updates")
    ),
    TRAINING(
            "Entrainements",
            "/tn/esprit/views/entrainement-user-view.fxml",
            "/tn/esprit/styles/entrainement-theme.css",
            "Entrainements | Sport Insight",
            true,
            false,
            List.of("training", "trainings", "train", "session", "sessions", "entrainement", "entrainements")
    ),
    SPONSORS(
            "Sponsors",
            "/tn/esprit/views/sponsor-user-view.fxml",
            "/tn/esprit/styles/sponsor-theme.css",
            "Sponsors | Sport Insight",
            true,
            false,
            List.of("sponsor", "sponsors", "contract", "contracts", "partnership", "partnerships")
    ),
    STORE(
            "Store",
            "/tn/esprit/views/store-view.fxml",
            "/tn/esprit/styles/store-theme.css",
            "Store | Sport Insight",
            true,
            false,
            List.of("store", "shop", "product", "products", "order", "orders")
    ),
    PROFILE(
            "Profile",
            "/tn/esprit/views/profile-view.fxml",
            "/tn/esprit/styles/profile-theme.css",
            "Profile | Sport Insight",
            true,
            false,
            List.of("profile", "account", "user profile", "my profile")
    );

    private final String label;
    private final String fxmlPath;
    private final String cssPath;
    private final String title;
    private final boolean requiresAuthentication;
    private final boolean adminOnly;
    private final List<String> aliases;

    AssistantNavigationTarget(
            String label,
            String fxmlPath,
            String cssPath,
            String title,
            boolean requiresAuthentication,
            boolean adminOnly,
            List<String> aliases
    ) {
        this.label = label;
        this.fxmlPath = fxmlPath;
        this.cssPath = cssPath;
        this.title = title;
        this.requiresAuthentication = requiresAuthentication;
        this.adminOnly = adminOnly;
        this.aliases = aliases;
    }

    public String label() {
        return label;
    }

    public String fxmlPath() {
        return fxmlPath;
    }

    public String cssPath() {
        return cssPath;
    }

    public String title() {
        return title;
    }

    public boolean requiresAuthentication() {
        return requiresAuthentication;
    }

    public boolean adminOnly() {
        return adminOnly;
    }

    public List<String> aliases() {
        return aliases;
    }

    public static Optional<AssistantNavigationTarget> findMatch(String rawText) {
        String normalized = normalize(rawText);
        return Arrays.stream(values())
                .filter(target -> target.aliases.stream().map(AssistantNavigationTarget::normalize).anyMatch(normalized::contains))
                .findFirst();
    }

    private static String normalize(String rawText) {
        return AssistantService.normalize(rawText);
    }
}

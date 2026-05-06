package tn.esprit.Controller;

import javafx.fxml.FXML;
<<<<<<< HEAD
import javafx.geometry.Insets;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
<<<<<<< HEAD
import javafx.scene.layout.Region;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.AdminNavigation;
<<<<<<< HEAD
import tn.esprit.gui.LiveMatchNotificationRuntime;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.AuthSession;
import tn.esprit.services.MatchFollowTargetService;
import tn.esprit.services.football.FootballDataCompetitions;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

=======
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.football.FootballDataCompetitions;

>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
public class LeagueCompetitionController {
    private static final double COMPETITION_CARD_WIDTH = 220;
    private static final double COMPETITION_CARD_HEIGHT = 238;
    private static final double COMPETITION_LOGO_SIZE = 114;

    @FXML
    private HBox navbarRoot;
    @FXML
    private Button adminNavButton;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button annonceNavButton;
    @FXML
    private HBox sidebarModuleChildrenBox;
    @FXML
    private Button leaguesNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private FlowPane competitionCardsPane;

    private SidebarModuleGroup sidebarModuleGroup;
<<<<<<< HEAD
    private MatchFollowTargetService matchFollowTargetService;
    private Set<String> followedCompetitionCodes = Set.of();
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
<<<<<<< HEAD
        try {
            if (AuthSession.isAuthenticated()) {
                matchFollowTargetService = new MatchFollowTargetService();
                refreshFollowedCompetitions();
            }
        } catch (SQLException ignored) {
            followedCompetitionCodes = Set.of();
        }
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        populateCompetitionCards();
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(adminNavButton);
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(equipesNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
    }

    @FXML
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/match-competitions-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Competitions");
    }

    @FXML
    private void handleOpenLeagues() {
        // Already on the leagues selector page.
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }


    @FXML
    private void handleOpenAnnonces() {
        SceneNavigator.switchScene(annonceNavButton != null ? annonceNavButton : matchsNavButton, "/tn/esprit/views/annonce-user-view.fxml", "/tn/esprit/styles/annonce-theme.css", "Anonce | Sport Insight");
    }
    private void configureSidebar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.LEAGUES);
    }

    private void populateCompetitionCards() {
        competitionCardsPane.getChildren().clear();
        for (String competitionCode : FootballDataCompetitions.TEAM_CODES) {
            competitionCardsPane.getChildren().add(createCompetitionCard(competitionCode));
        }
    }

<<<<<<< HEAD
    private StackPane createCompetitionCard(String competitionCode) {
=======
    private Button createCompetitionCard(String competitionCode) {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(COMPETITION_LOGO_SIZE);
        imageView.setFitHeight(COMPETITION_LOGO_SIZE);
        imageView.setPreserveRatio(true);

        Image image = EquipeUiSupport.loadResourceImage(LeagueCompetitionController.class, FootballDataCompetitions.logoResourceOf(competitionCode));
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(competitionLabel, "L"));
        fallbackLabel.getStyleClass().add("competition-card-logo-fallback");
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);

        StackPane logoPane = new StackPane(imageView, fallbackLabel);
        logoPane.setMinSize(COMPETITION_LOGO_SIZE + 24, COMPETITION_LOGO_SIZE + 24);
        logoPane.setPrefSize(COMPETITION_LOGO_SIZE + 24, COMPETITION_LOGO_SIZE + 24);
        logoPane.setMaxSize(COMPETITION_LOGO_SIZE + 24, COMPETITION_LOGO_SIZE + 24);
        logoPane.getStyleClass().add("competition-card-logo-shell");

        Label titleLabel = new Label(competitionLabel);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(COMPETITION_CARD_WIDTH - 36);
        titleLabel.getStyleClass().add("competition-card-title");

        VBox content = new VBox(18, logoPane, titleLabel);
        content.setAlignment(Pos.CENTER);

        Button cardButton = new Button();
        cardButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        cardButton.setGraphic(content);
        cardButton.getStyleClass().add("competition-card-button");
        cardButton.setPrefSize(COMPETITION_CARD_WIDTH, COMPETITION_CARD_HEIGHT);
        cardButton.setMinSize(COMPETITION_CARD_WIDTH, COMPETITION_CARD_HEIGHT);
        cardButton.setMaxSize(COMPETITION_CARD_WIDTH, COMPETITION_CARD_HEIGHT);
        cardButton.setOnAction(event -> openCompetition(cardButton, competitionCode));
<<<<<<< HEAD

        Button favouriteButton = createFavouriteButton(competitionCode);

        StackPane shell = new StackPane(cardButton, favouriteButton);
        shell.getStyleClass().add("competition-card-shell");
        StackPane.setAlignment(favouriteButton, Pos.TOP_LEFT);
        StackPane.setMargin(favouriteButton, new Insets(14, 0, 0, 14));
        return shell;
    }

    private Button createFavouriteButton(String competitionCode) {
        Button favouriteButton = new Button();
        favouriteButton.getStyleClass().add("favorite-star-button");
        favouriteButton.setFocusTraversable(false);
        updateFavouriteButton(favouriteButton, followedCompetitionCodes.contains(FootballDataCompetitions.normalizeCode(competitionCode)));
        favouriteButton.setOnAction(event -> {
            event.consume();
            toggleFavouriteCompetition(competitionCode, favouriteButton);
        });
        return favouriteButton;
    }

    private void toggleFavouriteCompetition(String competitionCode, Button favouriteButton) {
        Integer userId = currentUserId();
        String normalizedCode = FootballDataCompetitions.normalizeCode(competitionCode);
        if (userId == null || normalizedCode == null || matchFollowTargetService == null) {
            return;
        }

        try {
            boolean followed = followedCompetitionCodes.contains(normalizedCode);
            Set<String> nextCodes = new LinkedHashSet<>(followedCompetitionCodes);
            if (followed) {
                if (matchFollowTargetService.removeCompetitionFavorite(userId, normalizedCode)) {
                    nextCodes.remove(normalizedCode);
                }
            } else if (matchFollowTargetService.addCompetitionFavorite(userId, normalizedCode)) {
                nextCodes.add(normalizedCode);
                LiveMatchNotificationRuntime.getInstance().requestImmediatePoll();
            }

            followedCompetitionCodes = Set.copyOf(nextCodes);
            updateFavouriteButton(favouriteButton, followedCompetitionCodes.contains(normalizedCode));
        } catch (SQLException ignored) {
            // Keep league navigation responsive even if favourites storage is unavailable.
        }
    }

    private void updateFavouriteButton(Button favouriteButton, boolean followed) {
        favouriteButton.setText(followed ? "★" : "☆");
        favouriteButton.getStyleClass().remove("favorite-star-button-active");
        if (followed) {
            favouriteButton.getStyleClass().add("favorite-star-button-active");
        }
    }

    private void refreshFollowedCompetitions() throws SQLException {
        Integer userId = currentUserId();
        if (matchFollowTargetService == null || userId == null) {
            followedCompetitionCodes = Set.of();
            return;
        }
        followedCompetitionCodes = Set.copyOf(matchFollowTargetService.getFollowedCompetitionCodes(userId));
    }

    private Integer currentUserId() {
        return AuthSession.getCurrentUser() == null ? null : AuthSession.getCurrentUser().getId();
=======
        return cardButton;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private void openCompetition(Button source, String competitionCode) {
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);
        SceneNavigator.switchScene(
                source,
                "/tn/esprit/views/league-table-view.fxml",
                "/tn/esprit/styles/league-theme.css",
                competitionLabel + " | League Table",
                controller -> {
                    if (controller instanceof LeagueTableController leagueTableController) {
                        leagueTableController.setCompetitionFilter(competitionCode);
                    }
                }
        );
    }

}


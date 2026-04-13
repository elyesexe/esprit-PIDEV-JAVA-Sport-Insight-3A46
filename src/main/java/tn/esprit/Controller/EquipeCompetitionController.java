package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.gui.EquipeUiSupport;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.football.FootballDataCompetitions;

public class EquipeCompetitionController {
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

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);
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
        // Already on the team competition selector.
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
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
    }

    @FXML
    private void handleAddEquipe() {
        SceneNavigator.switchScene(
                equipesNavButton,
                "/tn/esprit/views/equipe-form-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                "Ajouter une equipe",
                controller -> {
                    if (controller instanceof EquipeFormController equipeFormController) {
                        equipeFormController.configureForCreate(null);
                    }
                }
        );
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.EQUIPES);
    }

    private void populateCompetitionCards() {
        competitionCardsPane.getChildren().clear();
        for (String competitionCode : FootballDataCompetitions.TEAM_CODES) {
            competitionCardsPane.getChildren().add(createCompetitionCard(competitionCode));
        }
    }

    private Button createCompetitionCard(String competitionCode) {
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(COMPETITION_LOGO_SIZE);
        imageView.setFitHeight(COMPETITION_LOGO_SIZE);
        imageView.setPreserveRatio(true);
        Image image = EquipeUiSupport.loadResourceImage(EquipeCompetitionController.class, FootballDataCompetitions.logoResourceOf(competitionCode));
        boolean hasImage = image != null;
        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);

        Label fallbackLabel = new Label(EquipeUiSupport.buildInitials(competitionLabel, "E"));
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
        return cardButton;
    }

    private void openCompetition(Button source, String competitionCode) {
        String competitionLabel = FootballDataCompetitions.labelOf(competitionCode);
        SceneNavigator.switchScene(
                source,
                "/tn/esprit/views/equipe-list-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                competitionLabel + " | Equipes",
                controller -> {
                    if (controller instanceof EquipeListController equipeListController) {
                        equipeListController.setCompetitionFilter(competitionCode);
                    }
                }
        );
    }

}


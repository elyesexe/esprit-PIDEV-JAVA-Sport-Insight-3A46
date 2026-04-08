package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tn.esprit.gui.SceneNavigator;

import java.util.Objects;

public class HomeController {
    private static final double SIDEBAR_EXPANDED_WIDTH = 256;

    @FXML
    private VBox sidebarRoot;
    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private Label sidebarSectionLabel;
    @FXML
    private Button sidebarToggleButton;
    @FXML
    private Button sidebarOpenButton;
    @FXML
    private Button equipesNavButton;
    @FXML
    private Button matchsNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private Button equipesButton;
    @FXML
    private Button joueursButton;
    @FXML
    private Button matchsButton;
    @FXML
    private StackPane heroShell;
    @FXML
    private ImageView heroImageView;

    private boolean sidebarVisible;

    @FXML
    public void initialize() {
        configureHeroImage();
        sidebarVisible = false;
        applySidebarState();
    }

    @FXML
    private void handleOpenHome() {
        // Already on the homepage.
    }

    @FXML
    private void handleOpenSidebar() {
        sidebarVisible = true;
        applySidebarState();
    }

    @FXML
    private void handleToggleSidebar() {
        sidebarVisible = false;
        applySidebarState();
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(resolveNavigationSource(equipesButton, equipesNavButton), "/tn/esprit/views/equipe-crud-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Sport Insight");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(resolveNavigationSource(joueursButton, joueursNavButton), "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenMatchs() {
        SceneNavigator.switchScene(resolveNavigationSource(matchsButton, matchsNavButton), "/tn/esprit/views/match-crud-view.fxml", "/tn/esprit/styles/match-theme.css", "Matchs | Sport Insight");
    }

    private Button resolveNavigationSource(Button primary, Button fallback) {
        return primary != null ? primary : fallback;
    }

    private void configureHeroImage() {
        Image heroImage = new Image(Objects.requireNonNull(
                HomeController.class.getResource("/tn/esprit/images/bg11.jpg"),
                "Homepage hero image is missing"
        ).toExternalForm());

        heroImageView.setManaged(false);
        heroImageView.setImage(heroImage);
        heroImageView.fitWidthProperty().bind(heroShell.widthProperty());
        heroImageView.fitHeightProperty().bind(heroShell.heightProperty());

        Rectangle clip = new Rectangle();
        clip.setArcWidth(60);
        clip.setArcHeight(60);
        clip.widthProperty().bind(heroShell.widthProperty());
        clip.heightProperty().bind(heroShell.heightProperty());
        heroImageView.setClip(clip);
    }

    private void applySidebarState() {
        sidebarRoot.setManaged(sidebarVisible);
        sidebarRoot.setVisible(sidebarVisible);
        sidebarSectionLabel.setManaged(sidebarVisible);
        sidebarSectionLabel.setVisible(sidebarVisible);
        sidebarOpenButton.setManaged(!sidebarVisible);
        sidebarOpenButton.setVisible(!sidebarVisible);

        sidebarBrandBox.setVisible(sidebarVisible);
        sidebarBrandBox.setManaged(sidebarVisible);
        sidebarToggleButton.setText("<");

        sidebarRoot.setMinWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setPrefWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
        sidebarRoot.setMaxWidth(sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : 0);
    }
}

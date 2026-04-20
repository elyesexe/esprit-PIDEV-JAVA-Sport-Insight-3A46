package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import tn.esprit.assistant.AssistantContextProvider;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.JoueurUiSupport;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.JoueurService;
import tn.esprit.services.wikidata.WikidataPlayerImageService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class JoueurDetailController implements AssistantContextProvider {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ExecutorService IMAGE_IMPORT_EXECUTOR =
            Executors.newSingleThreadExecutor(daemonFactory("joueur-detail-image-import"));

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
    private ImageView detailImageView;
    @FXML
    private Label detailImageFallbackLabel;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailSubtitleLabel;
    @FXML
    private Label detailIdValueLabel;
    @FXML
    private Label detailEquipeValueLabel;
    @FXML
    private Label detailNumeroValueLabel;
    @FXML
    private Label detailDateNaissanceValueLabel;
    @FXML
    private Label detailAgeValueLabel;
    @FXML
    private Label detailPositionValueLabel;
    @FXML
    private Label detailNationaliteValueLabel;
    @FXML
    private Label detailSourceValueLabel;

    private JoueurService joueurService;
    private EquipeService equipeService;
    private Joueur joueur;
    private SidebarModuleGroup sidebarModuleGroup;
    private boolean imageImportInProgress;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);

        try {
            joueurService = new JoueurService();
            equipeService = new EquipeService();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de preparer la fiche joueur.\n" + e.getMessage());
        }

        if (joueur != null) {
            renderJoueur();
        }
    }

    public void setJoueurContext(Joueur joueur) {
        this.joueur = joueur;
        if (detailNameLabel != null) {
            renderJoueur();
        }
    }

    @Override
    public String assistantContextSummary() {
        return """
                Current player detail screen.
                Player: %s.
                Subtitle: %s.
                Team: %s. Number: %s.
                Birth date: %s. Age: %s.
                Position: %s. Nationality: %s.
                Source: %s.
                """.formatted(
                emptyToFallback(detailNameLabel == null ? null : detailNameLabel.getText(), "Player"),
                emptyToFallback(detailSubtitleLabel == null ? null : detailSubtitleLabel.getText(), "No subtitle"),
                emptyToFallback(detailEquipeValueLabel == null ? null : detailEquipeValueLabel.getText(), "Unknown"),
                emptyToFallback(detailNumeroValueLabel == null ? null : detailNumeroValueLabel.getText(), "Unknown"),
                emptyToFallback(detailDateNaissanceValueLabel == null ? null : detailDateNaissanceValueLabel.getText(), "Unknown"),
                emptyToFallback(detailAgeValueLabel == null ? null : detailAgeValueLabel.getText(), "Unknown"),
                emptyToFallback(detailPositionValueLabel == null ? null : detailPositionValueLabel.getText(), "Unknown"),
                emptyToFallback(detailNationaliteValueLabel == null ? null : detailNationaliteValueLabel.getText(), "Unknown"),
                emptyToFallback(detailSourceValueLabel == null ? null : detailSourceValueLabel.getText(), "Unknown")
        );
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
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(joueursNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleBack() {
        SceneNavigator.switchScene(detailNameLabel, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
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
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.JOUEURS);
    }

    private void renderJoueur() {
        if (joueur == null || joueurService == null || equipeService == null) {
            return;
        }

        try {
            if (joueur.getId() != null) {
                Joueur refreshed = joueurService.getById(joueur.getId());
                if (refreshed != null) {
                    joueur = refreshed;
                }
            }

            Equipe equipe = resolveEquipe(joueur.getEquipeId());
            String equipeName = equipe == null ? "Sans equipe" : emptyToFallback(equipe.getNom(), "Sans equipe");
            String position = emptyToFallback(joueur.getPosition(), "Non renseigne");
            String nationalite = emptyToFallback(joueur.getNationalite(), "Non renseignee");
            String source = emptyToFallback(joueur.getExternalSource(), "Manuel");

            detailNameLabel.setText(buildFullName(joueur));
            detailSubtitleLabel.setText(buildSubtitle(equipeName, joueur.getDateNaissance(), position, nationalite));
            detailIdValueLabel.setText(joueur.getId() == null ? "-" : "#" + joueur.getId());
            detailEquipeValueLabel.setText(equipeName);
            detailNumeroValueLabel.setText(joueur.getNumero() > 0 ? "#" + joueur.getNumero() : "Non defini");
            detailDateNaissanceValueLabel.setText(formatDate(joueur.getDateNaissance()));
            detailAgeValueLabel.setText(formatAge(joueur.getDateNaissance()));
            detailPositionValueLabel.setText(position);
            detailNationaliteValueLabel.setText(nationalite);
            detailSourceValueLabel.setText(source);

            JoueurUiSupport.clearImageCache();
            updatePhoto();
            triggerLazyImageImport();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Chargement", "Impossible de charger les informations du joueur.\n" + e.getMessage());
        }
    }

    private Equipe resolveEquipe(Integer equipeId) throws SQLException {
        return equipeId == null ? null : equipeService.getById(equipeId);
    }

    private void updatePhoto() {
        Image image = JoueurUiSupport.loadJoueurImage(joueur.getImage());
        boolean hasImage = image != null;
        detailImageView.setImage(image);
        detailImageView.setManaged(hasImage);
        detailImageView.setVisible(hasImage);
        detailImageFallbackLabel.setManaged(!hasImage);
        detailImageFallbackLabel.setVisible(!hasImage);
        detailImageFallbackLabel.setText(JoueurUiSupport.buildInitials(joueur.getPrenom(), joueur.getNom(), "J"));
    }

    private void triggerLazyImageImport() {
        if (imageImportInProgress || joueur == null || joueur.getId() == null || !needsLazyImageImport(joueur)) {
            return;
        }

        imageImportInProgress = true;
        detailSourceValueLabel.setText("Importing photo...");

        Integer joueurId = joueur.getId();

        Task<Joueur> imageImportTask = new Task<>() {
            @Override
            protected Joueur call() throws Exception {
                JoueurService backgroundService = new JoueurService();
                Joueur fresh = backgroundService.getById(joueurId);
                if (fresh == null || !needsLazyImageImport(fresh)) {
                    return null;
                }

                WikidataPlayerImageService imageService = new WikidataPlayerImageService();
                String imagePath = imageService.resolvePlayerImagePath(fresh);
                if (imagePath == null || imagePath.isBlank()) {
                    return null;
                }

                backgroundService.updateImage(joueurId, imagePath);
                fresh.setImage(imagePath);
                return fresh;
            }
        };

        imageImportTask.setOnSucceeded(event -> {
            imageImportInProgress = false;
            Joueur imported = imageImportTask.getValue();
            if (imported == null) {
                detailSourceValueLabel.setText(emptyToFallback(joueur.getExternalSource(), "Manuel"));
                return;
            }

            joueur = imported;
            JoueurUiSupport.clearImageCache();
            updatePhoto();
            detailSourceValueLabel.setText(emptyToFallback(joueur.getExternalSource(), "Manuel") + " | Photo imported");
        });

        imageImportTask.setOnFailed(event -> {
            imageImportInProgress = false;
            detailSourceValueLabel.setText(emptyToFallback(joueur.getExternalSource(), "Manuel"));
            Throwable throwable = imageImportTask.getException();
            if (throwable != null) {
                System.err.println("Detail lazy player image import failed: " + throwable.getMessage());
            }
        });

        IMAGE_IMPORT_EXECUTOR.execute(imageImportTask);
    }

    private boolean needsLazyImageImport(Joueur currentJoueur) {
        if (currentJoueur == null) {
            return false;
        }
        String image = currentJoueur.getImage();
        if (image == null || image.isBlank()) {
            return true;
        }
        String normalized = image.replace('\\', '/').toLowerCase();
        return normalized.contains("fd-player-");
    }

    private String buildSubtitle(String equipeName, LocalDate dateNaissance, String position, String nationalite) {
        List<String> parts = new ArrayList<>();
        parts.add(equipeName);
        if (dateNaissance != null) {
            parts.add("Ne le " + formatDate(dateNaissance));
        }
        parts.add(position);
        parts.add(nationalite);
        return String.join(" | ", parts);
    }

    private String buildFullName(Joueur joueur) {
        String prenom = joueur.getPrenom() == null ? "" : joueur.getPrenom().trim();
        String nom = joueur.getNom() == null ? "" : joueur.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        return fullName.isBlank() ? "Joueur" : fullName;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "Non renseignee" : DATE_FORMATTER.format(date);
    }

    private String formatAge(LocalDate date) {
        if (date == null) {
            return "Age indisponible";
        }
        return Period.between(date, LocalDate.now()).getYears() + " ans";
    }

    private String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}


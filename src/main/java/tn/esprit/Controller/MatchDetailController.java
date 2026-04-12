package tn.esprit.Controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Matchs;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.EquipeService;
import tn.esprit.services.MatchsService;
import tn.esprit.services.football.FootballDataCompetitions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MatchDetailController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Path SYMFONY_UPLOADS_DIRECTORY = Path.of("C:", "final", "sport_insight_final", "public", "uploads", "equipes");
    private static final Map<String, String> COMPETITION_LABELS = FootballDataCompetitions.labels();

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
    private HBox sidebarModuleChildrenBox;
    @FXML
    private Button leaguesNavButton;
    @FXML
    private Button joueursNavButton;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private Label competitionBadgeLabel;
    @FXML
    private Label detailStatusChipLabel;
    @FXML
    private Label detailTitleLabel;
    @FXML
    private Label detailSubtitleLabel;
    @FXML
    private ImageView detailHomeLogoView;
    @FXML
    private Label detailHomeLogoFallbackLabel;
    @FXML
    private Label detailHomeNameLabel;
    @FXML
    private ImageView detailAwayLogoView;
    @FXML
    private Label detailAwayLogoFallbackLabel;
    @FXML
    private Label detailAwayNameLabel;
    @FXML
    private Label detailScoreValueLabel;
    @FXML
    private Label detailDateValueLabel;
    @FXML
    private Label detailHeureValueLabel;
    @FXML
    private Label detailLieuValueLabel;
    @FXML
    private Label detailTypeValueLabel;
    @FXML
    private Label detailStatutValueLabel;
    @FXML
    private Label detailIdValueLabel;
    @FXML
    private Label detailCompetitionValueLabel;
    @FXML
    private TextArea lineupDomicileArea;
    @FXML
    private TextArea lineupExterieurArea;

    private MatchsService matchsService;
    private EquipeService equipeService;
    private Matchs match;
    private SidebarModuleGroup sidebarModuleGroup;

    @FXML
    public void initialize() {
        configureSidebar();
        ThemeManager.bindToggle(themeToggleButton);

        try {
            matchsService = new MatchsService();
            equipeService = new EquipeService();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Connexion", "Impossible de preparer la fiche match.\n" + e.getMessage());
        }

        if (match != null) {
            renderMatch();
        }
    }

    public void setMatchContext(Matchs match) {
        this.match = match;
        if (detailTitleLabel != null) {
            renderMatch();
        }
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
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/equipe-competitions-view.fxml", "/tn/esprit/styles/equipe-theme.css", "Equipes | Competitions");
    }

    @FXML
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        openMatchList();
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(leaguesNavButton, "/tn/esprit/views/league-competitions-view.fxml", "/tn/esprit/styles/league-theme.css", "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(matchsNavButton, "/tn/esprit/views/joueur-crud-view.fxml", "/tn/esprit/styles/joueur-theme.css", "Joueurs | Sport Insight");
    }

    @FXML
    private void handleBack() {
        openMatchList();
    }

    @FXML
    private void handleEdit() {
        if (match == null) {
            return;
        }

        SceneNavigator.switchScene(
                detailTitleLabel,
                "/tn/esprit/views/match-form-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Modifier un match",
                controller -> {
                    if (controller instanceof MatchFormController matchFormController) {
                        matchFormController.configureForUpdate(match);
                    }
                }
        );
    }

    @FXML
    private void handleDelete() {
        if (match == null || match.getId() == null || matchsService == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer le match \"" + buildMatchLabel(match) + "\" ?");
        alert.setContentText("Cette action est definitive.");

        Optional<ButtonType> confirmation = alert.showAndWait();
        if (confirmation.isEmpty() || confirmation.get() != ButtonType.OK) {
            return;
        }

        try {
            matchsService.delete(match.getId());
            openMatchList();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Suppression", "Impossible de supprimer le match.\n" + e.getMessage());
        }
    }

    private void renderMatch() {
        if (match == null || equipeService == null) {
            return;
        }

        try {
            Equipe homeTeam = resolveEquipe(match.getEquipeDomicileId());
            Equipe awayTeam = resolveEquipe(match.getEquipeExterieurId());
            String competitionLabel = resolveCompetitionLabel(match.getCompetitionCode());

            competitionBadgeLabel.setText(competitionLabel == null ? "Competition" : competitionLabel);
            detailStatusChipLabel.setText(resolveStatus(match));
            applyDetailStatusStyle(detailStatusChipLabel, match.getStatut());
            detailTitleLabel.setText(buildMatchLabel(match));
            detailSubtitleLabel.setText(buildSubtitle(competitionLabel, homeTeam, awayTeam));
            detailScoreValueLabel.setText(buildScore(match));
            detailDateValueLabel.setText(formatDate(match.getDateMatch()));
            detailHeureValueLabel.setText(formatTime(match.getHeureDebut()));
            detailLieuValueLabel.setText(emptyToFallback(match.getLieu(), "Non renseigne"));
            detailTypeValueLabel.setText(emptyToFallback(match.getType(), "Non renseigne"));
            detailStatutValueLabel.setText(resolveStatus(match));
            detailIdValueLabel.setText(match.getIdMatch() == null ? "#" + match.getId() : match.getIdMatch());
            detailCompetitionValueLabel.setText(competitionLabel == null ? "Non renseignee" : competitionLabel);
            detailHomeNameLabel.setText(homeTeam == null ? "Equipe domicile" : emptyToFallback(homeTeam.getNom(), "Equipe domicile"));
            detailAwayNameLabel.setText(awayTeam == null ? "Equipe exterieur" : emptyToFallback(awayTeam.getNom(), "Equipe exterieur"));
            lineupDomicileArea.setText(emptyToFallback(match.getLineupDomicile(), "Aucun onze de depart renseigne."));
            lineupExterieurArea.setText(emptyToFallback(match.getLineupExterieur(), "Aucun onze de depart renseigne."));

            updateLogo(detailHomeLogoView, detailHomeLogoFallbackLabel, homeTeam, "D");
            updateLogo(detailAwayLogoView, detailAwayLogoFallbackLabel, awayTeam, "E");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Chargement", "Impossible de charger les informations du match.\n" + e.getMessage());
        }
    }

    private Equipe resolveEquipe(Integer equipeId) throws SQLException {
        return equipeId == null || equipeService == null ? null : equipeService.getById(equipeId);
    }

    private void updateLogo(ImageView imageView, Label fallbackLabel, Equipe equipe, String defaultLetter) {
        Image image = equipe == null ? null : loadImage(equipe.getImage());
        boolean hasImage = image != null;

        imageView.setImage(image);
        imageView.setManaged(hasImage);
        imageView.setVisible(hasImage);
        fallbackLabel.setManaged(!hasImage);
        fallbackLabel.setVisible(!hasImage);
        fallbackLabel.setText(equipe == null ? defaultLetter : buildInitials(equipe.getNom(), defaultLetter));
    }

    private String buildSubtitle(String competitionLabel, Equipe homeTeam, Equipe awayTeam) {
        String home = homeTeam == null ? "Equipe domicile" : emptyToFallback(homeTeam.getNom(), "Equipe domicile");
        String away = awayTeam == null ? "Equipe exterieur" : emptyToFallback(awayTeam.getNom(), "Equipe exterieur");
        if (competitionLabel == null) {
            return home + " recoit " + away + ".";
        }
        return competitionLabel + " | " + home + " recoit " + away + ".";
    }

    private void openMatchList() {
        String competitionCode = match == null ? null : match.getCompetitionCode();
        SceneNavigator.switchScene(
                detailTitleLabel,
                "/tn/esprit/views/match-crud-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                (resolveCompetitionLabel(competitionCode) == null ? "Matchs" : resolveCompetitionLabel(competitionCode)) + " | Matchs",
                controller -> {
                    if (controller instanceof MatchListController matchListController) {
                        matchListController.setCompetitionFilter(competitionCode);
                    }
                }
        );
    }

    private void configureSidebar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.MATCHS);
    }

    private String buildMatchLabel(Matchs value) {
        return getEquipeName(value.getEquipeDomicileId()) + " vs " + getEquipeName(value.getEquipeExterieurId());
    }

    private String getEquipeName(Integer equipeId) {
        if (equipeService == null) {
            return "Equipe inconnue";
        }
        try {
            Equipe equipe = resolveEquipe(equipeId);
            return equipe == null ? "Equipe inconnue" : emptyToFallback(equipe.getNom(), "Equipe inconnue");
        } catch (SQLException e) {
            return "Equipe inconnue";
        }
    }

    private String buildScore(Matchs value) {
        return (value.getScoreEquipeDomicile() == null ? "-" : value.getScoreEquipeDomicile())
                + " : "
                + (value.getScoreEquipeExterieur() == null ? "-" : value.getScoreEquipeExterieur());
    }

    private String resolveStatus(Matchs value) {
        String status = value == null ? null : emptyToNull(value.getStatut());
        return status == null ? "Programme" : status;
    }

    private String resolveCompetitionLabel(String competitionCode) {
        if (competitionCode == null) {
            return null;
        }
        return COMPETITION_LABELS.getOrDefault(competitionCode, competitionCode);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : TIME_FORMATTER.format(time);
    }

    private void applyDetailStatusStyle(Label label, String status) {
        label.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        String styleClass = switch (resolveFixtureStatusClass(status)) {
            case "fixture-status-live" -> "status-success";
            case "fixture-status-finished" -> "status-muted";
            case "fixture-status-cancelled" -> "status-error";
            default -> "status-warning";
        };
        if (!label.getStyleClass().contains(styleClass)) {
            label.getStyleClass().add(styleClass);
        }
    }

    private String resolveFixtureStatusClass(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return "fixture-status-scheduled";
        }
        if (normalized.contains("cours") || normalized.contains("live")) {
            return "fixture-status-live";
        }
        if (normalized.contains("fini") || normalized.contains("term")) {
            return "fixture-status-finished";
        }
        if (normalized.contains("annul")) {
            return "fixture-status-cancelled";
        }
        return "fixture-status-scheduled";
    }

    private String emptyToFallback(String value, String fallback) {
        String cleaned = emptyToNull(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        String cleaned = emptyToNull(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }

    private String buildInitials(String teamName, String fallback) {
        String normalizedName = emptyToNull(teamName);
        if (normalizedName == null) {
            return fallback;
        }

        StringBuilder initials = new StringBuilder();
        for (String part : normalizedName.split("\\s+")) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.isEmpty() ? fallback : initials.toString();
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalizedPath = imagePath.trim();
        Image image = loadImageFromUri(normalizedPath);
        if (image != null) {
            return image;
        }

        Path directPath = toPathIfValid(normalizedPath);
        if (directPath != null && directPath.isAbsolute()) {
            image = loadImageFromFile(directPath);
            if (image != null) {
                return image;
            }
        }

        URL resource = resolveResource(normalizedPath);
        if (resource != null) {
            image = createImage(resource.toExternalForm());
            if (image != null) {
                return image;
            }
        }

        if (directPath != null && !directPath.isAbsolute()) {
            image = loadImageFromFile(directPath);
            if (image != null) {
                return image;
            }
        }

        for (Path candidate : buildRelativeCandidates(normalizedPath)) {
            image = loadImageFromFile(candidate);
            if (image != null) {
                return image;
            }
        }

        return null;
    }

    private URL resolveResource(String imagePath) {
        String[] resourceCandidates = {
                imagePath.startsWith("/") ? imagePath : "/" + imagePath,
                "/tn/esprit/" + imagePath,
                "/tn/esprit/images/" + imagePath,
                "/tn/esprit/uploads/equipes/" + imagePath,
                "/uploads/equipes/" + imagePath
        };

        for (String candidate : resourceCandidates) {
            URL resource = MatchDetailController.class.getResource(candidate);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    private List<Path> buildRelativeCandidates(String imagePath) {
        List<Path> candidates = new ArrayList<>();
        appendCandidate(candidates, Path.of("uploads", "equipes"), imagePath);
        appendCandidate(candidates, Path.of("public", "uploads", "equipes"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit", "images"), imagePath);
        appendCandidate(candidates, SYMFONY_UPLOADS_DIRECTORY, imagePath);
        return candidates;
    }

    private void appendCandidate(List<Path> candidates, Path base, String imagePath) {
        Path childPath = toPathIfValid(imagePath);
        if (childPath == null || childPath.isAbsolute()) {
            return;
        }
        candidates.add(base.resolve(childPath));
    }

    private Image loadImageFromUri(String imagePath) {
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("file:/")) {
            return createImage(imagePath);
        }
        return null;
    }

    private Path toPathIfValid(String pathValue) {
        try {
            return Path.of(pathValue);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private Image loadImageFromFile(Path path) {
        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        Image image = createImage(file.toURI().toString());
        if (image != null) {
            return image;
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            return bufferedImage == null ? null : SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
    }

    private Image createImage(String imageSource) {
        try {
            Image image = new Image(imageSource, false);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

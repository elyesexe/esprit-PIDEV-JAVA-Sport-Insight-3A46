package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Commentaire;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.AnnonceService;
import tn.esprit.services.CommentaireService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class AnnonceUserController {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final String SORT_RECENT = "Most recent";
    private static final String SORT_ALPHA = "A-Z";

    @FXML private BorderPane pageRoot;
    @FXML private Region heroPhotoRegion;
    @FXML private HBox navbarRoot;
    @FXML private Button adminNavButton;
    @FXML private HBox sidebarBrandBox;
    @FXML private Button matchsNavButton;
    @FXML private HBox sidebarModuleChildrenBox;
    @FXML private Button equipesNavButton;
    @FXML private Button leaguesNavButton;
    @FXML private Button joueursNavButton;
    @FXML private Button annonceNavButton;
    @FXML private ToggleButton themeToggleButton;
    @FXML private Label resultCountLabel;
    @FXML private Label selectionStateLabel;
    @FXML private Label totalAnnoncesMetricLabel;
    @FXML private Label activeAnnoncesMetricLabel;
    @FXML private Label urgentAnnoncesMetricLabel;
    @FXML private Label pendingCommentsMetricLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> niveauFilterComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Label resultsMetaLabel;
    @FXML private Label statusLabel;
    @FXML private VBox annonceCardsPane;

    private final List<Annonce> annonces = new ArrayList<>();
    private final List<Commentaire> commentaires = new ArrayList<>();
    private final List<Annonce> visibleAnnonces = new ArrayList<>();
    private final java.util.HashMap<Integer, Integer> commentCounts = new java.util.HashMap<>();

    private SidebarModuleGroup sidebarModuleGroup;
    private AnnonceService annonceService;
    private CommentaireService commentaireService;
    private boolean serviceReady;

    @FXML
    public void initialize() {
        configureSidebar();
        configureHeroImage();
        ThemeManager.bindToggle(themeToggleButton);
        applyThemeState(themeToggleButton != null && themeToggleButton.isSelected());
        if (themeToggleButton != null) {
            themeToggleButton.selectedProperty().addListener((obs, oldValue, selected) -> applyThemeState(selected));
        }
        configureFilters();

        try {
            annonceService = new AnnonceService();
            commentaireService = new CommentaireService();
            serviceReady = true;
            refreshData();
            showSuccessStatus("Announcement feed ready.");
        } catch (SQLException e) {
            serviceReady = false;
            showErrorStatus("Database connection unavailable.");
            showAlert(Alert.AlertType.ERROR, "Announcements",
                    "Could not load the user announcement feed.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        refreshData();
        showMutedStatus("Feed refreshed.");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        niveauFilterComboBox.getSelectionModel().clearSelection();
        sortComboBox.setValue(SORT_RECENT);
        applyFilters();
        showMutedStatus("Filters reset.");
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox,
                "/tn/esprit/views/home-view.fxml",
                "/tn/esprit/styles/home-theme.css",
                "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(adminNavButton);
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(equipesNavButton,
                "/tn/esprit/views/equipe-competitions-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                "Equipes | Competitions");
    }

    @FXML
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(matchsNavButton,
                "/tn/esprit/views/match-competitions-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Matchs | Competitions");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(leaguesNavButton,
                "/tn/esprit/views/league-competitions-view.fxml",
                "/tn/esprit/styles/league-theme.css",
                "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(joueursNavButton,
                "/tn/esprit/views/joueur-crud-view.fxml",
                "/tn/esprit/styles/joueur-theme.css",
                "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenAnnonces() {
        applyFilters();
    }

    private void configureSidebar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.NONE);
        if (annonceNavButton != null && !annonceNavButton.getStyleClass().contains("navbar-nav-button-active")) {
            annonceNavButton.getStyleClass().add("navbar-nav-button-active");
        }
    }

    private void configureHeroImage() {
        if (heroPhotoRegion == null) {
            return;
        }
        var imageUrl = getClass().getResource("/tn/esprit/images/annonce.jpg");
        if (imageUrl == null) {
            return;
        }
        BackgroundSize backgroundSize = new BackgroundSize(
                100, 100, true, true, true, true
        );
        BackgroundImage backgroundImage = new BackgroundImage(
                new javafx.scene.image.Image(imageUrl.toExternalForm()),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                backgroundSize
        );
        heroPhotoRegion.setBackground(new Background(backgroundImage));
    }

    private void applyThemeState(boolean darkMode) {
        if (pageRoot == null) {
            return;
        }
        pageRoot.getStyleClass().removeAll("annonce-user-dark", "annonce-user-light");
        pageRoot.getStyleClass().add(darkMode ? "annonce-user-dark" : "annonce-user-light");
    }

    private void configureFilters() {
        sortComboBox.getItems().setAll(SORT_RECENT, SORT_ALPHA);
        sortComboBox.setValue(SORT_RECENT);
        niveauFilterComboBox.setPromptText("All levels");

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        niveauFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void refreshData() {
        if (!serviceReady || annonceService == null || commentaireService == null) {
            return;
        }

        try {
            annonces.clear();
            annonces.addAll(annonceService.getAll());

            commentaires.clear();
            commentaires.addAll(commentaireService.getAll());

            rebuildCommentCounts();
            rebuildLevelFilterItems();
            updateMetrics();
            applyFilters();
        } catch (SQLException e) {
            showErrorStatus("Could not refresh announcements.");
            showAlert(Alert.AlertType.ERROR, "Announcements", "Refresh failed.\n" + e.getMessage());
        }
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        String selectedLevel = normalize(niveauFilterComboBox.getValue());
        String selectedSort = sortComboBox.getValue();

        Comparator<Annonce> comparator = SORT_ALPHA.equals(selectedSort)
                ? Comparator.comparing(annonce -> emptyIfNull(annonce.getTitre()).toLowerCase(Locale.ROOT))
                : Comparator.comparing(Annonce::getDatePublication, Comparator.nullsLast(Comparator.reverseOrder()));

        visibleAnnonces.clear();
        visibleAnnonces.addAll(annonces.stream()
                .filter(annonce -> matchesAnnonceFilters(annonce, query, selectedLevel))
                .sorted(comparator)
                .toList());

        renderFeed();

        resultsMetaLabel.setText(visibleAnnonces.size() + " announcement(s) visible");
        resultCountLabel.setText(annonces.size() + " total announcement(s)");
        selectionStateLabel.setText("Feed mode");
    }

    private void rebuildCommentCounts() {
        commentCounts.clear();
        for (Commentaire commentaire : commentaires) {
            if (commentaire.getAnnonceId() == null) {
                continue;
            }
            commentCounts.merge(commentaire.getAnnonceId(), 1, Integer::sum);
        }
    }

    private void rebuildLevelFilterItems() {
        String selectedLevel = niveauFilterComboBox.getValue();
        List<String> levels = annonces.stream()
                .map(Annonce::getNiveauRequis)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        niveauFilterComboBox.getItems().setAll(levels);
        if (selectedLevel != null && levels.contains(selectedLevel)) {
            niveauFilterComboBox.setValue(selectedLevel);
        } else if (selectedLevel != null) {
            niveauFilterComboBox.getSelectionModel().clearSelection();
        }
    }

    private boolean matchesAnnonceFilters(Annonce annonce, String query, String selectedLevel) {
        boolean matchesQuery = query == null
                || containsNormalized(annonce.getTitre(), query)
                || containsNormalized(annonce.getDescription(), query)
                || containsNormalized(annonce.getPosteRecherche(), query)
                || containsNormalized(annonce.getNiveauRequis(), query);

        boolean matchesLevel = selectedLevel == null
                || Objects.equals(normalize(annonce.getNiveauRequis()), selectedLevel);

        return matchesQuery && matchesLevel;
    }

    private void renderFeed() {
        annonceCardsPane.getChildren().clear();

        if (visibleAnnonces.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER_LEFT);
            emptyState.getStyleClass().addAll("panel-card");

            Label title = new Label("No posts found");
            title.getStyleClass().add("section-title");

            Label text = new Label("Try another search or change the level filter.");
            text.setWrapText(true);
            text.getStyleClass().add("section-subtitle");

            emptyState.getChildren().addAll(title, text);
            annonceCardsPane.getChildren().add(emptyState);
            return;
        }

        for (Annonce annonce : visibleAnnonces) {
            annonceCardsPane.getChildren().add(buildPostCard(annonce));
        }
    }

    private VBox buildPostCard(Annonce annonce) {
        VBox card = new VBox(14);
        card.getStyleClass().add("annonce-post-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("annonce-post-header");

        VBox headerText = new VBox(4);

        Label dateLabel = new Label(formatDate(annonce.getDatePublication()));
        dateLabel.getStyleClass().add("annonce-post-date");

        Label titleLabel = new Label(fallbackText(annonce.getTitre(), "Untitled announcement"));
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("annonce-post-title");

        Label subtitleLabel = new Label(buildSubtitle(annonce));
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("annonce-post-subtitle");

        headerText.getChildren().addAll(dateLabel, titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightMeta = new VBox(8);
        rightMeta.setAlignment(Pos.TOP_RIGHT);
        rightMeta.getChildren().add(createPill(humanizeStatus(annonce.getStatut()), resolveAnnonceStatusStyle(annonce.getStatut())));
        if (Boolean.TRUE.equals(annonce.getUrgent())) {
            rightMeta.getChildren().add(createMetaChip("Urgent"));
        }

        header.getChildren().addAll(headerText, spacer, rightMeta);

        FlowPane metaFlow = new FlowPane();
        metaFlow.setHgap(8);
        metaFlow.setVgap(8);
        metaFlow.getChildren().addAll(
                createMetaChip("Role: " + fallbackText(annonce.getPosteRecherche(), "Not specified")),
                createMetaChip("Level: " + fallbackText(annonce.getNiveauRequis(), "Not specified")),
                createMetaChip(commentCounts.getOrDefault(annonce.getId(), 0) + " comment(s)")
        );

        VBox descriptionBox = new VBox(6);
        descriptionBox.getStyleClass().add("annonce-user-description-box");
        Label descriptionLabel = new Label(fallbackText(annonce.getDescription(), "No description available."));
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("annonce-user-description-text");
        descriptionBox.getChildren().add(descriptionLabel);

        VBox commentsSection = new VBox(10);
        Label commentsTitle = new Label("Comments");
        commentsTitle.getStyleClass().add("annonce-comments-title");

        VBox commentsStack = new VBox(10);
        commentsStack.getStyleClass().add("annonce-comment-stack");

        List<Commentaire> postComments = commentaires.stream()
                .filter(commentaire -> Objects.equals(commentaire.getAnnonceId(), annonce.getId()))
                .sorted(Comparator.comparing(Commentaire::getDateCommentaire, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (postComments.isEmpty()) {
            Label emptyComments = new Label(
                    isCommentsEnabled(annonce)
                            ? "No comments yet. Be the first to comment."
                            : "Comments are disabled for this announcement."
            );
            emptyComments.getStyleClass().add("annonce-comment-empty");
            commentsStack.getChildren().add(emptyComments);
        } else {
            for (Commentaire commentaire : postComments) {
                commentsStack.getChildren().add(buildCommentCard(commentaire));
            }
        }

        VBox addCommentSection = buildInlineCommentForm(annonce);

        commentsSection.getChildren().addAll(commentsTitle, commentsStack, addCommentSection);
        card.getChildren().addAll(header, metaFlow, descriptionBox, commentsSection);
        return card;
    }

    private VBox buildInlineCommentForm(Annonce annonce) {
        VBox formBox = new VBox(10);
        formBox.getStyleClass().add("annonce-post-comment-form");

        Label hint = new Label(
                isCommentsEnabled(annonce)
                        ? "Add a comment to this post."
                        : "Comments are disabled for this post."
        );
        hint.getStyleClass().add("annonce-section-note");

        TextField auteurField = new TextField();
        auteurField.setPromptText("Your name");
        auteurField.getStyleClass().add("form-text-field");

        TextField joueurIdField = new TextField();
        joueurIdField.setPromptText("Player ID (optional)");
        joueurIdField.getStyleClass().add("form-text-field");
        configureNumericField(joueurIdField);

        HBox topRow = new HBox(10, auteurField, joueurIdField);
        topRow.getStyleClass().add("annonce-form-inline");
        HBox.setHgrow(auteurField, Priority.ALWAYS);
        HBox.setHgrow(joueurIdField, Priority.ALWAYS);

        TextArea commentaireArea = new TextArea();
        commentaireArea.setPromptText("Write a comment...");
        commentaireArea.setWrapText(true);
        commentaireArea.setPrefRowCount(3);
        commentaireArea.getStyleClass().add("annonce-text-area");

        Label validationLabel = new Label();
        validationLabel.getStyleClass().add("annonce-section-note");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);

        HBox actions = new HBox(10);
        Button postButton = new Button("Post comment");
        postButton.getStyleClass().add("primary-button");

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("ghost-button");

        actions.getChildren().addAll(postButton, clearButton);

        boolean commentsOpen = isCommentsEnabled(annonce) && serviceReady;
        auteurField.setDisable(!commentsOpen);
        joueurIdField.setDisable(!commentsOpen);
        commentaireArea.setDisable(!commentsOpen);
        postButton.setDisable(!commentsOpen);

        clearButton.setOnAction(event -> {
            auteurField.clear();
            joueurIdField.clear();
            commentaireArea.clear();
            validationLabel.setText("");
            validationLabel.setManaged(false);
            validationLabel.setVisible(false);
            clearFieldError(auteurField);
            clearFieldError(joueurIdField);
            clearFieldError(commentaireArea);
        });

        postButton.setOnAction(event -> {
            validationLabel.setText("");
            validationLabel.setManaged(false);
            validationLabel.setVisible(false);
            clearFieldError(auteurField);
            clearFieldError(joueurIdField);
            clearFieldError(commentaireArea);

            String contenu = emptyToNull(commentaireArea.getText());
            Integer joueurId = parseOptionalInteger(joueurIdField);
            String auteur = emptyToNull(auteurField.getText());

            if (contenu == null) {
                markFieldInvalid(commentaireArea);
                validationLabel.setText("Comment content is required.");
                validationLabel.setManaged(true);
                validationLabel.setVisible(true);
                return;
            }

            if (joueurId == Integer.MIN_VALUE) {
                validationLabel.setText("Player ID must be numeric.");
                validationLabel.setManaged(true);
                validationLabel.setVisible(true);
                return;
            }

            Commentaire commentaire = new Commentaire(
                    contenu,
                    LocalDate.now(),
                    joueurId,
                    annonce.getId(),
                    auteur == null ? "Anonymous" : auteur,
                    0,
                    "PENDING",
                    null
            );

            try {
                commentaireService.add(commentaire);
                refreshData();
                showSuccessStatus("Comment posted.");
            } catch (SQLException e) {
                showErrorStatus("Could not post the comment.");
                showAlert(Alert.AlertType.ERROR, "Comments", "Add failed.\n" + e.getMessage());
            }
        });

        formBox.getChildren().addAll(hint, topRow, commentaireArea, validationLabel, actions);
        return formBox;
    }

    private VBox buildCommentCard(Commentaire commentaire) {
        VBox card = new VBox(10);
        card.getStyleClass().add("annonce-comment-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox authorBox = new VBox(2);
        Label authorLabel = new Label(fallbackText(commentaire.getAuteurAnonyme(), "Anonymous"));
        authorLabel.getStyleClass().add("annonce-comment-author");

        Label dateLabel = new Label(formatDate(commentaire.getDateCommentaire()));
        dateLabel.getStyleClass().add("annonce-comment-date");
        authorBox.getChildren().addAll(authorLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusPill = createPill(
                humanizeStatus(commentaire.getModerationStatus()),
                resolveCommentStatusStyle(commentaire.getModerationStatus())
        );

        header.getChildren().addAll(authorBox, spacer, statusPill);

        Label bodyLabel = new Label(fallbackText(commentaire.getContenu(), ""));
        bodyLabel.setWrapText(true);
        bodyLabel.getStyleClass().add("annonce-comment-body");

        FlowPane footer = new FlowPane();
        footer.setHgap(8);
        footer.setVgap(8);
        footer.getChildren().add(createMetaChip(commentaire.getNbLikes() + " like(s)"));
        if (emptyToNull(commentaire.getModerationReason()) != null) {
            footer.getChildren().add(createMetaChip(commentaire.getModerationReason()));
        }

        card.getChildren().addAll(header, bodyLabel, footer);
        return card;
    }

    private Label createPill(String text, String statusStyle) {
        Label label = new Label(text);
        label.getStyleClass().addAll("annonce-user-meta-chip", statusStyle);
        return label;
    }

    private Label createMetaChip(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("annonce-user-meta-chip");
        return label;
    }

    private void updateMetrics() {
        int activeCount = 0;
        int urgentCount = 0;
        int pendingComments = 0;

        for (Annonce annonce : annonces) {
            if ("ACTIVE".equalsIgnoreCase(annonce.getStatut())) {
                activeCount++;
            }
            if (Boolean.TRUE.equals(annonce.getUrgent())) {
                urgentCount++;
            }
        }
        for (Commentaire commentaire : commentaires) {
            if ("PENDING".equalsIgnoreCase(commentaire.getModerationStatus())) {
                pendingComments++;
            }
        }

        totalAnnoncesMetricLabel.setText(String.valueOf(annonces.size()));
        activeAnnoncesMetricLabel.setText(String.valueOf(activeCount));
        urgentAnnoncesMetricLabel.setText(String.valueOf(urgentCount));
        pendingCommentsMetricLabel.setText(String.valueOf(pendingComments));
    }

    private String buildSubtitle(Annonce annonce) {
        List<String> parts = new ArrayList<>();
        if (emptyToNull(annonce.getPosteRecherche()) != null) {
            parts.add(annonce.getPosteRecherche());
        }
        if (emptyToNull(annonce.getNiveauRequis()) != null) {
            parts.add(annonce.getNiveauRequis());
        }
        if (annonce.getEntraineurId() != null) {
            parts.add("Coach #" + annonce.getEntraineurId());
        }
        return parts.isEmpty() ? "Club announcement" : String.join(" • ", parts);
    }

    private void configureNumericField(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    private Integer parseOptionalInteger(TextField field) {
        String value = emptyToNull(field.getText());
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            markFieldInvalid(field);
            return Integer.MIN_VALUE;
        }
    }

    private void markFieldInvalid(Control control) {
        if (!control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        control.getStyleClass().remove("invalid-field");
    }

    private void showMutedStatus(String message) {
        setStatus(statusLabel, message, "status-muted");
    }

    private void showSuccessStatus(String message) {
        setStatus(statusLabel, message, "status-success");
    }

    private void showErrorStatus(String message) {
        setStatus(statusLabel, message, "status-error");
    }

    private void setStatus(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!label.getStyleClass().contains(styleClass)) {
            label.getStyleClass().add(styleClass);
        }
    }

    private String resolveAnnonceStatusStyle(String statut) {
        if (statut == null) {
            return "status-muted";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "status-success";
            case "EN_ATTENTE" -> "status-warning";
            case "EXPIREE", "CLOSED", "ARCHIVED" -> "status-error";
            default -> "status-muted";
        };
    }

    private String resolveCommentStatusStyle(String statut) {
        if (statut == null) {
            return "status-muted";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case "APPROVED" -> "status-success";
            case "PENDING" -> "status-warning";
            case "REJECTED", "BLOCKED" -> "status-error";
            default -> "status-muted";
        };
    }

    private String humanizeStatus(String raw) {
        String safe = emptyToNull(raw);
        if (safe == null) {
            return "Unknown";
        }
        String[] words = safe.toLowerCase(Locale.ROOT).replace('_', ' ').split("\\s+");
        List<String> formatted = new ArrayList<>();
        for (String word : words) {
            if (!word.isBlank()) {
                formatted.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
            }
        }
        return formatted.isEmpty() ? "Unknown" : String.join(" ", formatted);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private boolean containsNormalized(String source, String query) {
        String normalizedSource = normalize(source);
        return query != null && normalizedSource != null && normalizedSource.contains(query);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String fallbackText(String value, String fallback) {
        String safe = emptyToNull(value);
        return safe == null ? fallback : safe;
    }

    private boolean isCommentsEnabled(Annonce annonce) {
        return annonce.getCommentsEnabled() == null || annonce.getCommentsEnabled();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Commentaire;
import tn.esprit.entities.User;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.security.AuthSession;
import tn.esprit.security.UserRoles;
import tn.esprit.services.AnnonceService;
import tn.esprit.services.CommentaireService;
import tn.esprit.services.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AnnonceUserController {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final String SORT_RECENT = "Most recent";
    private static final String SORT_ALPHA = "A-Z";
    private static final String EDIT_ICON_PATH = "/tn/esprit/icons/comment-edit-white.png";
    private static final String DELETE_ICON_PATH = "/tn/esprit/icons/comment-delete-white.png";

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
    @FXML private VBox composerCard;
    @FXML private StackPane composerAvatarShell;
    @FXML private ImageView composerAvatarImage;
    @FXML private Label composerAvatarInitialsLabel;
    @FXML private Label composerIdentityLabel;
    @FXML private Label composerHintLabel;
    @FXML private TextField postTitleField;
    @FXML private TextField postRoleField;
    @FXML private TextField postLevelField;
    @FXML private TextArea postDescriptionArea;
    @FXML private Label composerValidationLabel;
    @FXML private CheckBox urgentPostCheck;
    @FXML private Button publishPostButton;
    @FXML private Button clearPostButton;

    private final List<Annonce> annonces = new ArrayList<>();
    private final List<Commentaire> commentaires = new ArrayList<>();
    private final List<Annonce> visibleAnnonces = new ArrayList<>();
    private final HashMap<Integer, Integer> commentCounts = new HashMap<>();
    private final Map<Integer, User> userCache = new HashMap<>();

    private SidebarModuleGroup sidebarModuleGroup;
    private AnnonceService annonceService;
    private CommentaireService commentaireService;
    private UserService userService;
    private boolean serviceReady;

    @FXML
    public void initialize() {
        configureSidebar();
        configureHeroImage();
        applyCircularImageClip(composerAvatarImage);
        ThemeManager.bindToggle(themeToggleButton);
        applyThemeState(themeToggleButton != null && themeToggleButton.isSelected());
        if (themeToggleButton != null) {
            themeToggleButton.selectedProperty().addListener((obs, oldValue, selected) -> applyThemeState(selected));
        }
        configureFilters();

        try {
            annonceService = new AnnonceService();
            commentaireService = new CommentaireService();
            userService = new UserService();
            serviceReady = true;
            refreshData();
            updateComposerState();
            showSuccessStatus("Announcement feed ready.");
        } catch (Exception e) {
            serviceReady = false;
            updateComposerState();
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

    @FXML
    private void handlePublishPost() {
        clearComposerValidation();
        clearFieldError(postTitleField);
        clearFieldError(postRoleField);
        clearFieldError(postLevelField);
        clearFieldError(postDescriptionArea);

        User currentUser = getCurrentUser();
        if (!isCurrentUserCoach()) {
            setComposerValidation("Only coach accounts can publish announcements.");
            return;
        }
        if (!serviceReady || annonceService == null || currentUser == null || currentUser.getId() == null) {
            setComposerValidation("The post composer is not ready yet.");
            return;
        }

        String title = emptyToNull(postTitleField.getText());
        String role = emptyToNull(postRoleField.getText());
        String level = emptyToNull(postLevelField.getText());
        String description = emptyToNull(postDescriptionArea.getText());

        boolean valid = true;
        if (title == null) {
            markFieldInvalid(postTitleField);
            valid = false;
        }
        if (description == null) {
            markFieldInvalid(postDescriptionArea);
            valid = false;
        }
        if (!valid) {
            setComposerValidation("Title and post content are required.");
            return;
        }

        Annonce annonce = new Annonce(
                title,
                description,
                role == null ? "Player" : role,
                level == null ? "Community" : level,
                LocalDate.now(),
                "ACTIVE",
                currentUser.getId(),
                true,
                urgentPostCheck != null && urgentPostCheck.isSelected()
        );

        try {
            annonceService.add(annonce);
            handleClearPostComposer();
            refreshData();
            showSuccessStatus("Announcement published.");
        } catch (SQLException e) {
            showErrorStatus("Could not publish the announcement.");
            showAlert(Alert.AlertType.ERROR, "Announcements", "Publish failed.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClearPostComposer() {
        if (postTitleField != null) {
            postTitleField.clear();
        }
        if (postRoleField != null) {
            postRoleField.clear();
        }
        if (postLevelField != null) {
            postLevelField.clear();
        }
        if (postDescriptionArea != null) {
            postDescriptionArea.clear();
        }
        if (urgentPostCheck != null) {
            urgentPostCheck.setSelected(false);
        }
        clearComposerValidation();
        clearFieldError(postTitleField);
        clearFieldError(postRoleField);
        clearFieldError(postLevelField);
        clearFieldError(postDescriptionArea);
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
        BackgroundSize backgroundSize = new BackgroundSize(100, 100, true, true, true, true);
        BackgroundImage backgroundImage = new BackgroundImage(
                new Image(imageUrl.toExternalForm()),
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

            userCache.clear();
            User currentUser = getCurrentUser();
            if (currentUser != null && currentUser.getId() != null) {
                userCache.put(currentUser.getId(), currentUser);
            }

            rebuildCommentCounts();
            rebuildLevelFilterItems();
            updateMetrics();
            updateComposerState();
            applyFilters();
        } catch (SQLException e) {
            showErrorStatus("Could not refresh announcements.");
            showAlert(Alert.AlertType.ERROR, "Announcements", "Refresh failed.\n" + e.getMessage());
        }
    }

    private void updateComposerState() {
        User currentUser = getCurrentUser();
        boolean isCoach = isCurrentUserCoach();
        boolean enabled = serviceReady && isCoach && currentUser != null;

        if (composerCard != null) {
            composerCard.setManaged(enabled);
            composerCard.setVisible(enabled);
        }

        if (composerIdentityLabel != null) {
            composerIdentityLabel.setText("Publish as " + buildDisplayName(currentUser));
        }
        if (composerHintLabel != null) {
            composerHintLabel.setText(enabled
                    ? "Your coach profile is used automatically for announcements."
                    : "Only coach accounts can publish announcements. All roles can read comments; only player accounts can post and manage their own comments.");
        }

        updateAvatarGraphic(
                composerAvatarShell,
                composerAvatarImage,
                composerAvatarInitialsLabel,
                currentUser,
                buildDisplayName(currentUser)
        );

        if (postTitleField != null) {
            postTitleField.setDisable(!enabled);
        }
        if (postRoleField != null) {
            postRoleField.setDisable(!enabled);
        }
        if (postLevelField != null) {
            postLevelField.setDisable(!enabled);
        }
        if (postDescriptionArea != null) {
            postDescriptionArea.setDisable(!enabled);
        }
        if (publishPostButton != null) {
            publishPostButton.setDisable(!enabled);
        }
        if (clearPostButton != null) {
            clearPostButton.setDisable(!enabled);
        }
        if (urgentPostCheck != null) {
            urgentPostCheck.setDisable(!enabled);
        }
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        String selectedLevel = normalize(niveauFilterComboBox.getValue());
        String selectedSort = sortComboBox.getValue();

        Comparator<Annonce> comparator = SORT_ALPHA.equals(selectedSort)
                ? Comparator.comparing((Annonce annonce) -> !Boolean.TRUE.equals(annonce.getUrgent()))
                .thenComparing(annonce -> emptyIfNull(annonce.getTitre()).toLowerCase(Locale.ROOT))
                : Comparator.comparing((Annonce annonce) -> !Boolean.TRUE.equals(annonce.getUrgent()))
                .thenComparing(Annonce::getDatePublication, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Annonce::getId, Comparator.nullsLast(Comparator.reverseOrder()));

        visibleAnnonces.clear();
        visibleAnnonces.addAll(annonces.stream()
                .filter(annonce -> matchesAnnonceFilters(annonce, query, selectedLevel))
                .sorted(comparator)
                .toList());

        renderFeed();

        resultsMetaLabel.setText(visibleAnnonces.size() + " announcement(s) visible");
        resultCountLabel.setText(annonces.size() + " total announcement(s)");
        selectionStateLabel.setText(isCurrentUserCoach() ? "Coach publisher view" : "Community feed");
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
                || containsNormalized(annonce.getNiveauRequis(), query)
                || containsNormalized(resolveAnnonceAuthorName(annonce), query);

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
        if (Boolean.TRUE.equals(annonce.getUrgent())) {
            card.getStyleClass().add("annonce-post-card-urgent");
        }

        User author = resolveAnnonceAuthor(annonce);
        String authorName = resolveAnnonceAuthorName(annonce);

        HBox identityRow = new HBox(12);
        identityRow.setAlignment(Pos.CENTER_LEFT);
        identityRow.getStyleClass().add("annonce-post-author-row");

        StackPane avatar = createAvatarNode(author, authorName, false);

        VBox identityText = new VBox(4);
        Label authorLabel = new Label(authorName);
        authorLabel.getStyleClass().add("annonce-post-author");

        Label dateLabel = new Label(formatDate(annonce.getDatePublication()));
        dateLabel.getStyleClass().add("annonce-post-date");

        Label subtitleLabel = new Label(buildSubtitle(annonce));
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("annonce-post-subtitle");
        identityText.getChildren().addAll(authorLabel, dateLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightMeta = new VBox(8);
        rightMeta.setAlignment(Pos.TOP_RIGHT);
        rightMeta.getChildren().add(createPill(humanizeStatus(annonce.getStatut()), resolveAnnonceStatusStyle(annonce.getStatut())));
        if (Boolean.TRUE.equals(annonce.getUrgent())) {
            rightMeta.getChildren().add(createMetaChip("Urgent"));
        }

        identityRow.getChildren().addAll(avatar, identityText, spacer, rightMeta);

        Label titleLabel = new Label(fallbackText(annonce.getTitre(), "Untitled announcement"));
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("annonce-post-title");

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
                            ? "No comments yet. Players can start the discussion."
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
        commentsSection.getChildren().addAll(commentsTitle, commentsStack);
        if (addCommentSection != null) {
            commentsSection.getChildren().add(addCommentSection);
        }
        card.getChildren().addAll(identityRow, titleLabel, metaFlow, descriptionBox, commentsSection);
        return card;
    }

    private VBox buildInlineCommentForm(Annonce annonce) {
        VBox formBox = new VBox(10);
        formBox.getStyleClass().add("annonce-post-comment-form");

        User currentUser = getCurrentUser();
        boolean canComment = serviceReady && isCurrentUserJoueur() && isCommentsEnabled(annonce)
                && currentUser != null && currentUser.getId() != null;
        if (!canComment) {
            return null;
        }

        HBox authorRow = new HBox(10);
        authorRow.setAlignment(Pos.CENTER_LEFT);
        authorRow.getStyleClass().add("annonce-inline-author-row");
        authorRow.getChildren().addAll(
                createAvatarNode(currentUser, buildDisplayName(currentUser), true),
                buildInlineAuthorInfo(currentUser, canComment, annonce)
        );

        TextArea commentaireArea = new TextArea();
        commentaireArea.setPromptText("Write a comment as " + buildDisplayName(currentUser) + "...");
        commentaireArea.setWrapText(true);
        commentaireArea.setPrefRowCount(3);
        commentaireArea.getStyleClass().add("annonce-text-area");
        if (Boolean.TRUE.equals(annonce.getUrgent())) {
            formBox.getStyleClass().add("annonce-post-comment-form-urgent");
            commentaireArea.getStyleClass().add("annonce-urgent-comment-field");
        }

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

        clearButton.setOnAction(event -> {
            commentaireArea.clear();
            validationLabel.setText("");
            validationLabel.setManaged(false);
            validationLabel.setVisible(false);
            clearFieldError(commentaireArea);
        });

        postButton.setOnAction(event -> {
            validationLabel.setText("");
            validationLabel.setManaged(false);
            validationLabel.setVisible(false);
            clearFieldError(commentaireArea);

            String contenu = emptyToNull(commentaireArea.getText());
            if (contenu == null) {
                markFieldInvalid(commentaireArea);
                validationLabel.setText("Comment content is required.");
                validationLabel.setManaged(true);
                validationLabel.setVisible(true);
                return;
            }

            Commentaire commentaire = new Commentaire(
                    contenu,
                    LocalDate.now(),
                    currentUser.getId(),
                    annonce.getId(),
                    buildDisplayName(currentUser),
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

        formBox.getChildren().addAll(authorRow, commentaireArea, validationLabel, actions);
        return formBox;
    }

    private VBox buildInlineAuthorInfo(User currentUser, boolean canComment, Annonce annonce) {
        VBox infoBox = new VBox(3);

        Label authorLabel = new Label(buildDisplayName(currentUser));
        authorLabel.getStyleClass().add("annonce-comment-author");

        Label hint = new Label(canComment
                ? "Commenting on " + fallbackText(annonce.getTitre(), "this post")
                : isCommentsEnabled(annonce)
                ? "Only player accounts can comment on announcements."
                : "Comments are disabled for this post.");
        hint.setWrapText(true);
        hint.getStyleClass().add("annonce-section-note");

        infoBox.getChildren().addAll(authorLabel, hint);
        return infoBox;
    }

    private VBox buildCommentCard(Commentaire commentaire) {
        VBox card = new VBox(10);
        card.getStyleClass().add("annonce-comment-card");

        User author = resolveCommentAuthor(commentaire);
        String authorName = resolveCommentAuthorName(commentaire);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = createAvatarNode(author, authorName, true);

        VBox authorBox = new VBox(2);
        Label authorLabel = new Label(authorName);
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

        header.getChildren().addAll(avatar, authorBox, spacer, statusPill);

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

        VBox editSection = buildInlineCommentEditor(commentaire, bodyLabel, footer);
        card.getChildren().addAll(header, bodyLabel, footer);
        if (editSection != null) {
            card.getChildren().add(editSection);
        }
        return card;
    }

    private VBox buildInlineCommentEditor(Commentaire commentaire, Label bodyLabel, FlowPane footer) {
        if (!canCurrentUserManageComment(commentaire)) {
            return null;
        }

        VBox editorBox = new VBox(8);
        editorBox.setManaged(false);
        editorBox.setVisible(false);

        TextArea editorArea = new TextArea(emptyIfNull(commentaire.getContenu()));
        editorArea.setWrapText(true);
        editorArea.setPrefRowCount(3);
        editorArea.getStyleClass().add("annonce-text-area");

        Label validationLabel = new Label();
        validationLabel.getStyleClass().add("annonce-section-note");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);

        HBox actions = new HBox(10);
        Button saveButton = new Button("Save");
        saveButton.getStyleClass().add("primary-button");
        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("ghost-button");
        Button editButton = createIconButton("Edit comment", EDIT_ICON_PATH, "annonce-icon-button-edit");
        Button deleteButton = createIconButton("Delete comment", DELETE_ICON_PATH, "annonce-icon-button-delete");
        actions.getStyleClass().add("annonce-comment-icon-actions");
        actions.getChildren().addAll(editButton, deleteButton);
        footer.getChildren().add(actions);

        HBox editorActions = new HBox(10, saveButton, cancelButton);

        editButton.setOnAction(event -> {
            editorArea.setText(emptyIfNull(commentaire.getContenu()));
            validationLabel.setText("");
            validationLabel.setManaged(false);
            validationLabel.setVisible(false);
            bodyLabel.setManaged(false);
            bodyLabel.setVisible(false);
            editorBox.setManaged(true);
            editorBox.setVisible(true);
        });

        cancelButton.setOnAction(event -> {
            editorBox.setManaged(false);
            editorBox.setVisible(false);
            bodyLabel.setManaged(true);
            bodyLabel.setVisible(true);
        });

        saveButton.setOnAction(event -> {
            String contenu = emptyToNull(editorArea.getText());
            if (contenu == null) {
                validationLabel.setText("Comment content is required.");
                validationLabel.setManaged(true);
                validationLabel.setVisible(true);
                markFieldInvalid(editorArea);
                return;
            }

            clearFieldError(editorArea);
            Commentaire updated = new Commentaire(
                    commentaire.getId(),
                    contenu,
                    commentaire.getDateCommentaire(),
                    commentaire.getJoueurId(),
                    commentaire.getAnnonceId(),
                    commentaire.getAuteurAnonyme(),
                    commentaire.getNbLikes(),
                    commentaire.getModerationStatus(),
                    commentaire.getModerationReason()
            );

            try {
                commentaireService.update(updated);
                refreshData();
                showSuccessStatus("Comment updated.");
            } catch (SQLException e) {
                showErrorStatus("Could not update the comment.");
                showAlert(Alert.AlertType.ERROR, "Comments", "Update failed.\n" + e.getMessage());
            }
        });

        deleteButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete comment");
            alert.setHeaderText("Delete your comment?");
            alert.setContentText(fallbackText(commentaire.getContenu(), "This comment"));

            if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) != javafx.scene.control.ButtonType.OK) {
                return;
            }

            try {
                commentaireService.delete(commentaire.getId());
                refreshData();
                showSuccessStatus("Comment deleted.");
            } catch (SQLException e) {
                showErrorStatus("Could not delete the comment.");
                showAlert(Alert.AlertType.ERROR, "Comments", "Delete failed.\n" + e.getMessage());
            }
        });

        editorBox.getChildren().addAll(editorArea, validationLabel, editorActions);
        return editorBox;
    }

    private Button createIconButton(String accessibleText, String iconPath, String styleClass) {
        Button button = new Button();
        button.getStyleClass().addAll("annonce-icon-button", styleClass);
        button.setAccessibleText(accessibleText);
        button.setTooltip(new Tooltip(accessibleText));

        var iconUrl = getClass().getResource(iconPath);
        if (iconUrl != null) {
            ImageView iconView = new ImageView(new Image(iconUrl.toExternalForm()));
            iconView.setFitWidth(18);
            iconView.setFitHeight(18);
            iconView.setPreserveRatio(true);
            iconView.setSmooth(true);
            button.setGraphic(iconView);
        }
        return button;
    }

    private StackPane createAvatarNode(User user, String displayName, boolean compact) {
        StackPane shell = new StackPane();
        shell.getStyleClass().add("annonce-avatar-shell");
        shell.getStyleClass().add(compact ? "annonce-avatar-shell-sm" : "annonce-avatar-shell-md");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(compact ? 42 : 54);
        imageView.setFitHeight(compact ? 42 : 54);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("annonce-avatar-image");
        applyCircularImageClip(imageView);

        Label initialsLabel = new Label();
        initialsLabel.getStyleClass().add("annonce-avatar-fallback");
        initialsLabel.getStyleClass().add(compact ? "annonce-avatar-fallback-sm" : "annonce-avatar-fallback-md");

        shell.getChildren().addAll(imageView, initialsLabel);
        updateAvatarGraphic(shell, imageView, initialsLabel, user, displayName);
        return shell;
    }

    private void applyCircularImageClip(ImageView imageView) {
        if (imageView == null) {
            return;
        }
        Circle clip = new Circle();
        clip.centerXProperty().bind(imageView.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(imageView.fitHeightProperty().divide(2));
        clip.radiusProperty().bind(imageView.fitWidthProperty().divide(2));
        imageView.setClip(clip);
    }

    private void updateAvatarGraphic(StackPane shell, ImageView imageView, Label fallbackLabel, User user, String displayName) {
        Image image = loadProfileImage(user == null ? null : user.getPhoto());
        boolean showImage = image != null;

        if (shell != null) {
            shell.getStyleClass().remove("annonce-avatar-shell-image");
            if (showImage) {
                shell.getStyleClass().add("annonce-avatar-shell-image");
            }
        }
        if (imageView != null) {
            imageView.setImage(image);
            imageView.setVisible(showImage);
            imageView.setManaged(showImage);
        }
        if (fallbackLabel != null) {
            fallbackLabel.setText(buildInitials(displayName));
            fallbackLabel.setVisible(!showImage);
            fallbackLabel.setManaged(!showImage);
        }
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
        parts.add(Boolean.TRUE.equals(annonce.getUrgent()) ? "Urgent update" : "Club feed");
        return String.join(" • ", parts);
    }

    private void markFieldInvalid(Control control) {
        if (control != null && !control.getStyleClass().contains("invalid-field")) {
            control.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Control control) {
        if (control != null) {
            control.getStyleClass().remove("invalid-field");
        }
    }

    private void clearComposerValidation() {
        if (composerValidationLabel != null) {
            composerValidationLabel.setText("");
            composerValidationLabel.setManaged(false);
            composerValidationLabel.setVisible(false);
        }
    }

    private void setComposerValidation(String message) {
        if (composerValidationLabel != null) {
            composerValidationLabel.setText(message);
            composerValidationLabel.setManaged(true);
            composerValidationLabel.setVisible(true);
        }
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

    private User getCurrentUser() {
        return AuthSession.getCurrentUser();
    }

    private boolean isCurrentUserJoueur() {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.hasRole(UserRoles.ROLE_JOUEUR);
    }

    private boolean canCurrentUserManageComment(Commentaire commentaire) {
        User currentUser = getCurrentUser();
        return commentaire != null
                && currentUser != null
                && currentUser.hasRole(UserRoles.ROLE_JOUEUR)
                && currentUser.getId() != null
                && Objects.equals(currentUser.getId(), commentaire.getJoueurId());
    }

    private boolean isCurrentUserCoach() {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.hasRole(UserRoles.ROLE_ENTRAINEUR);
    }

    private User resolveAnnonceAuthor(Annonce annonce) {
        if (annonce == null || annonce.getEntraineurId() == null) {
            return null;
        }
        return resolveUserById(annonce.getEntraineurId());
    }

    private String resolveAnnonceAuthorName(Annonce annonce) {
        User author = resolveAnnonceAuthor(annonce);
        if (author != null) {
            return buildDisplayName(author);
        }
        if (annonce != null && annonce.getEntraineurId() != null) {
            return "User #" + annonce.getEntraineurId();
        }
        return "Sport Insight user";
    }

    private User resolveCommentAuthor(Commentaire commentaire) {
        if (commentaire == null || commentaire.getJoueurId() == null) {
            return null;
        }
        return resolveUserById(commentaire.getJoueurId());
    }

    private String resolveCommentAuthorName(Commentaire commentaire) {
        User author = resolveCommentAuthor(commentaire);
        if (author != null) {
            return buildDisplayName(author);
        }
        return fallbackText(commentaire == null ? null : commentaire.getAuteurAnonyme(), "Anonymous");
    }

    private User resolveUserById(Integer userId) {
        if (userId == null) {
            return null;
        }
        if (userCache.containsKey(userId)) {
            return userCache.get(userId);
        }
        if (userService == null) {
            userCache.put(userId, null);
            return null;
        }
        try {
            User user = userService.getById(userId);
            userCache.put(userId, user);
            return user;
        } catch (SQLException ignored) {
            userCache.put(userId, null);
            return null;
        }
    }

    private Image loadProfileImage(String rawPath) {
        String candidate = emptyToNull(rawPath);
        if (candidate == null) {
            return null;
        }

        try {
            if (candidate.startsWith("http://") || candidate.startsWith("https://") || candidate.startsWith("file:")) {
                Image image = new Image(candidate, false);
                return image.isError() ? null : image;
            }

            Path path = Path.of(candidate);
            if (Files.exists(path)) {
                Image image = new Image(path.toUri().toString(), false);
                return image.isError() ? null : image;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String buildDisplayName(User user) {
        if (user == null) {
            return "Sport Insight user";
        }
        String displayName = emptyToNull(user.getDisplayName());
        if (displayName != null) {
            return displayName;
        }

        String fullName = ((emptyIfNull(user.getPrenom()) + " " + emptyIfNull(user.getNom())).trim());
        if (!fullName.isBlank()) {
            return fullName;
        }
        return fallbackText(user.getEmail(), "Sport Insight user");
    }

    private String buildInitials(String displayName) {
        String safeName = fallbackText(displayName, "Sport Insight");
        String[] parts = safeName.trim().split("\\s+");
        if (parts.length == 1) {
            return safeName.substring(0, Math.min(2, safeName.length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
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

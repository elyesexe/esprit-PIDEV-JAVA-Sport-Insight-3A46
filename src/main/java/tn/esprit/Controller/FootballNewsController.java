package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tn.esprit.assistant.AssistantContextProvider;
import tn.esprit.gui.AdminNavigation;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.SidebarModuleGroup;
import tn.esprit.gui.ThemeManager;
import tn.esprit.services.FootballNewsArticle;
import tn.esprit.services.FootballNewsService;

import java.awt.Desktop;
import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

public class FootballNewsController implements AssistantContextProvider {
    private static final ExecutorService NEWS_EXECUTOR = Executors.newSingleThreadExecutor(daemonFactory());
    private static final String DARK_BACKGROUND_STYLE = "-fx-background-color:"
            + " radial-gradient(center 50% 30%, radius 120%, #0b2943 0%, #020617 100%);";
    private static final String LIGHT_BACKGROUND_STYLE = "-fx-background-color:"
            + " radial-gradient(center 12% 12%, radius 34%, rgba(16, 185, 129, 0.12) 0%, rgba(16, 185, 129, 0) 100%),"
            + " radial-gradient(center 86% 14%, radius 30%, rgba(59, 130, 246, 0.10) 0%, rgba(59, 130, 246, 0) 100%),"
            + " linear-gradient(from 0% 0% to 100% 100%, #f8fffb 0%, #f0fdf4 38%, #f8fafc 100%);";
    private static final DateTimeFormatter DETAIL_DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());
    private static final int INITIAL_CARD_LIMIT = 18;
    private static final int CARD_LIMIT_STEP = 12;
    private static final double LEAD_IMAGE_WIDTH = 430;
    private static final double LEAD_IMAGE_HEIGHT = 242;
    private static final double CARD_IMAGE_WIDTH = 270;
    private static final double CARD_IMAGE_HEIGHT = 152;

    @FXML private BorderPane pageRoot;
    @FXML private ScrollPane pageScroll;
    @FXML private StackPane pageHeroShell;
    @FXML private ImageView pageHeroImageView;
    @FXML private HBox navbarRoot;
    @FXML private Button adminNavButton;
    @FXML private HBox sidebarBrandBox;
    @FXML private Button matchsNavButton;
    @FXML private HBox sidebarModuleChildrenBox;
    @FXML private Button equipesNavButton;
    @FXML private Button leaguesNavButton;
    @FXML private Button joueursNavButton;
    @FXML private Button annonceNavButton;
    @FXML private Button footballNewsNavButton;
    @FXML private ToggleButton themeToggleButton;

    @FXML private TextField searchField;
    @FXML private ToggleButton allTopicButton;
    @FXML private ToggleButton premierLeagueButton;
    @FXML private ToggleButton championsLeagueButton;
    @FXML private ToggleButton transfersButton;
    @FXML private ToggleButton womenButton;
    @FXML private ToggleButton europeButton;
    @FXML private ToggleButton savedOnlyButton;
    @FXML private Button refreshButton;
    @FXML private Button resetButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private Label totalCountLabel;
    @FXML private Label savedCountLabel;
    @FXML private Label latestTimeLabel;

    @FXML private ImageView heroImageView;
    @FXML private Label heroMetaLabel;
    @FXML private Label heroTitleLabel;
    @FXML private Label heroSummaryLabel;
    @FXML private Button heroOpenButton;
    @FXML private Button heroBookmarkButton;
    @FXML private VBox rightRailBox;
    @FXML private FlowPane cardsPane;

    private final FootballNewsService newsService = new FootballNewsService();
    private final ObservableList<FootballNewsArticle> allArticles = FXCollections.observableArrayList();
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Set<String> savedUrls = new HashSet<>();

    private SidebarModuleGroup sidebarModuleGroup;
    private TopicFilter activeTopic = TopicFilter.ALL;
    private FootballNewsArticle heroArticle;
    private Instant lastLoadedAt;
    private String lastLoadStatus = "Sport Insight news ready.";
    private int visibleCardLimit = INITIAL_CARD_LIMIT;

    @FXML
    public void initialize() {
        configureNavbar();
        ThemeManager.bindToggle(themeToggleButton);
        configureHeroImage();
        configureThemeBackground();
        configureScrollPane();
        configureFilters();
        refreshNews();
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(resolveNavigationSource(sidebarBrandBox, navbarRoot),
                "/tn/esprit/views/home-view.fxml",
                "/tn/esprit/styles/home-theme.css",
                "Sport Insight | Accueil");
    }

    @FXML
    private void handleOpenAdmin() {
        AdminNavigation.openAdmin(resolveNavigationSource(adminNavButton, navbarRoot));
    }

    @FXML
    private void handleOpenMatchs() {
        if (sidebarModuleGroup != null && sidebarModuleGroup.handleMatchsClick()) {
            return;
        }
        SceneNavigator.switchScene(resolveNavigationSource(matchsNavButton, navbarRoot),
                "/tn/esprit/views/match-competitions-view.fxml",
                "/tn/esprit/styles/match-theme.css",
                "Matchs | Competitions");
    }

    @FXML
    private void handleOpenEquipes() {
        SceneNavigator.switchScene(resolveNavigationSource(equipesNavButton, navbarRoot),
                "/tn/esprit/views/equipe-competitions-view.fxml",
                "/tn/esprit/styles/equipe-theme.css",
                "Equipes | Competitions");
    }

    @FXML
    private void handleOpenLeagues() {
        SceneNavigator.switchScene(resolveNavigationSource(leaguesNavButton, navbarRoot),
                "/tn/esprit/views/league-competitions-view.fxml",
                "/tn/esprit/styles/league-theme.css",
                "Leagues | Top 5");
    }

    @FXML
    private void handleOpenJoueurs() {
        SceneNavigator.switchScene(resolveNavigationSource(joueursNavButton, navbarRoot),
                "/tn/esprit/views/joueur-crud-view.fxml",
                "/tn/esprit/styles/joueur-theme.css",
                "Joueurs | Sport Insight");
    }

    @FXML
    private void handleOpenAnnonces() {
        SceneNavigator.switchScene(resolveNavigationSource(annonceNavButton, navbarRoot),
                "/tn/esprit/views/annonce-user-view.fxml",
                "/tn/esprit/styles/annonce-theme.css",
                "Annonce | Sport Insight");
    }

    @FXML
    private void handleOpenFootballNews() {
        applyFilters();
    }

    @FXML
    private void handleRefresh() {
        refreshNews();
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        setActiveTopic(TopicFilter.ALL);
        if (savedOnlyButton != null) {
            savedOnlyButton.setSelected(false);
        }
        resetVisibleCards();
        applyFilters();
    }

    @FXML
    private void handleTopicFilter(ActionEvent event) {
        Object source = event == null ? null : event.getSource();
        if (source == premierLeagueButton) {
            setActiveTopic(TopicFilter.PREMIER_LEAGUE);
        } else if (source == championsLeagueButton) {
            setActiveTopic(TopicFilter.CHAMPIONS_LEAGUE);
        } else if (source == transfersButton) {
            setActiveTopic(TopicFilter.TRANSFERS);
        } else if (source == womenButton) {
            setActiveTopic(TopicFilter.WOMEN);
        } else if (source == europeButton) {
            setActiveTopic(TopicFilter.EUROPE);
        } else {
            setActiveTopic(TopicFilter.ALL);
        }
        resetVisibleCards();
        applyFilters();
    }

    @FXML
    private void handleSavedOnly() {
        resetVisibleCards();
        applyFilters();
    }

    @FXML
    private void handleOpenHero() {
        openArticle(heroArticle);
    }

    @FXML
    private void handleBookmarkHero() {
        toggleSaved(heroArticle);
    }

    @Override
    public String assistantContextSummary() {
        String top = heroArticle == null ? "No top story selected." : "Top football story: " + heroArticle.title();
        return "Sport Insight News page. "
                + allArticles.size()
                + " article(s) loaded, "
                + savedUrls.size()
                + " saved locally. "
                + top;
    }

    private void configureNavbar() {
        sidebarModuleGroup = new SidebarModuleGroup(
                matchsNavButton,
                sidebarModuleChildrenBox,
                equipesNavButton,
                leaguesNavButton,
                joueursNavButton
        );
        sidebarModuleGroup.initialize(SidebarModuleGroup.ActiveModule.NONE);
        if (footballNewsNavButton != null
                && !footballNewsNavButton.getStyleClass().contains("navbar-nav-button-active")) {
            footballNewsNavButton.getStyleClass().add("navbar-nav-button-active");
        }
    }

    private void configureScrollPane() {
        if (pageScroll == null) {
            return;
        }
        pageScroll.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(this::forceScrollPaneViewportTransparent));
        pageScroll.sceneProperty().addListener((obs, oldScene, newScene) -> Platform.runLater(this::forceScrollPaneViewportTransparent));
        Platform.runLater(this::forceScrollPaneViewportTransparent);
    }

    private void configureHeroImage() {
        if (pageHeroImageView == null) {
            return;
        }
        var imageUrl = getClass().getResource("/tn/esprit/images/News hero.png");
        if (imageUrl == null) {
            return;
        }
        Image heroImage = new Image(imageUrl.toExternalForm(), false);
        pageHeroImageView.setImage(heroImage);
        pageHeroImageView.setManaged(false);
        pageHeroImageView.setMouseTransparent(true);
        pageHeroImageView.setPreserveRatio(false);
        if (pageHeroShell != null) {
            pageHeroShell.setMinHeight(320);
            pageHeroShell.setPrefHeight(320);
            pageHeroShell.setMaxHeight(320);
            pageHeroImageView.fitWidthProperty().bind(pageHeroShell.widthProperty());
            pageHeroImageView.fitHeightProperty().bind(pageHeroShell.heightProperty());
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(pageHeroShell.widthProperty());
            clip.heightProperty().bind(pageHeroShell.heightProperty());
            clip.setArcWidth(60);
            clip.setArcHeight(60);
            pageHeroImageView.setClip(clip);
            pageHeroShell.widthProperty().addListener((obs, oldValue, newValue) -> updateHeroImageViewport());
            pageHeroShell.heightProperty().addListener((obs, oldValue, newValue) -> updateHeroImageViewport());
        }
        heroImage.widthProperty().addListener((obs, oldValue, newValue) -> updateHeroImageViewport());
        heroImage.heightProperty().addListener((obs, oldValue, newValue) -> updateHeroImageViewport());
        Platform.runLater(this::updateHeroImageViewport);
    }

    private void updateHeroImageViewport() {
        if (pageHeroImageView == null || pageHeroShell == null || pageHeroImageView.getImage() == null) {
            return;
        }
        Image image = pageHeroImageView.getImage();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double targetWidth = pageHeroShell.getWidth();
        double targetHeight = pageHeroShell.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return;
        }

        double targetRatio = targetWidth / targetHeight;
        double imageRatio = imageWidth / imageHeight;
        double viewportWidth = imageWidth;
        double viewportHeight = imageHeight;
        double viewportX = 0;
        double viewportY = 0;

        if (imageRatio > targetRatio) {
            viewportWidth = imageHeight * targetRatio;
            viewportX = (imageWidth - viewportWidth) / 2.0;
        } else if (imageRatio < targetRatio) {
            viewportHeight = imageWidth / targetRatio;
            viewportY = (imageHeight - viewportHeight) / 2.0;
        }

        pageHeroImageView.setViewport(new Rectangle2D(viewportX, viewportY, viewportWidth, viewportHeight));
    }

    private void configureThemeBackground() {
        applyThemeBackground(ThemeManager.isDarkMode());
        if (themeToggleButton != null) {
            themeToggleButton.selectedProperty().addListener((obs, oldValue, darkMode) -> {
                applyThemeBackground(darkMode);
                Platform.runLater(this::forceScrollPaneViewportTransparent);
            });
        }
        if (pageRoot != null) {
            pageRoot.sceneProperty().addListener((obs, oldScene, newScene) ->
                    Platform.runLater(() -> applyThemeBackground(ThemeManager.isDarkMode())));
        }
    }

    private void configureFilters() {
        setActiveTopic(TopicFilter.ALL);
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> {
                resetVisibleCards();
                applyFilters();
            });
        }
        if (statusLabel != null) {
            statusLabel.setText(lastLoadStatus);
        }
    }

    private void refreshNews() {
        resetVisibleCards();
        setLoading(true);
        setStatus("Loading live Sport Insight news...");

        Task<List<FootballNewsArticle>> task = new Task<>() {
            @Override
            protected List<FootballNewsArticle> call() throws Exception {
                return newsService.fetchLatest();
            }
        };

        task.setOnSucceeded(event -> {
            List<FootballNewsArticle> articles = new ArrayList<>(task.getValue());
            articles.sort(Comparator.comparing(FootballNewsArticle::publishedAt).reversed());
            allArticles.setAll(articles);
            lastLoadedAt = Instant.now();
            lastLoadStatus = "Live Sport Insight news loaded.";
            setLoading(false);
            applyFilters();
        });
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            allArticles.setAll(newsService.fallbackArticles());
            lastLoadedAt = Instant.now();
            lastLoadStatus = ex == null
                    ? "Live feed unavailable. Showing offline fallback."
                    : "Live feed unavailable. Showing offline fallback: " + shortMessage(ex.getMessage());
            setLoading(false);
            applyFilters();
        });

        NEWS_EXECUTOR.execute(task);
    }

    private void applyFilters() {
        String query = normalize(searchField == null ? "" : searchField.getText());
        boolean savedOnly = savedOnlyButton != null && savedOnlyButton.isSelected();

        List<FootballNewsArticle> filtered = allArticles.stream()
                .filter(article -> !savedOnly || isSaved(article))
                .filter(article -> activeTopic.matches(article))
                .filter(article -> matchesQuery(article, query))
                .sorted(Comparator.comparing(FootballNewsArticle::publishedAt).reversed())
                .collect(Collectors.toList());

        updateStats(filtered);
        renderArticles(filtered);
    }

    private boolean matchesQuery(FootballNewsArticle article, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String haystack = normalize(article.title() + " " + article.summary());
        return haystack.contains(query);
    }

    private void renderArticles(List<FootballNewsArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            heroArticle = null;
            renderEmptyState();
            return;
        }

        heroArticle = articles.get(0);
        renderHero(heroArticle);
        renderRightRail(articles.subList(1, Math.min(4, articles.size())));
        renderCards(articles.size() > 1 ? articles.subList(1, articles.size()) : List.of());
    }

    private void renderHero(FootballNewsArticle article) {
        if (heroMetaLabel != null) {
            heroMetaLabel.setText(formatMeta(article));
        }
        if (heroTitleLabel != null) {
            heroTitleLabel.setText(limitText(emptyToFallback(article.title(), "Sport Insight news"), 112));
        }
        if (heroSummaryLabel != null) {
            heroSummaryLabel.setText(limitText(emptyToFallback(article.summary(), "Open the story for more detail."), 160));
        }
        if (heroOpenButton != null) {
            heroOpenButton.setDisable(article.url() == null || article.url().isBlank());
        }
        refreshBookmarkButton(heroBookmarkButton, article);

        if (heroImageView != null) {
            Image image = loadImage(article.imageUrl(), LEAD_IMAGE_WIDTH * 2, LEAD_IMAGE_HEIGHT * 2);
            if (image == null) {
                image = loadLocalHeroImage(LEAD_IMAGE_WIDTH * 2, LEAD_IMAGE_HEIGHT * 2);
            }
            installImageFallback(heroImageView, image, LEAD_IMAGE_WIDTH * 2, LEAD_IMAGE_HEIGHT * 2);
            heroImageView.setImage(image);
            heroImageView.setVisible(image != null);
            heroImageView.setManaged(image != null);
        }
    }

    private void renderRightRail(List<FootballNewsArticle> railArticles) {
        if (rightRailBox == null) {
            return;
        }
        rightRailBox.getChildren().clear();
        if (railArticles == null || railArticles.isEmpty()) {
            rightRailBox.getChildren().add(createMutedLabel("No more stories match these filters."));
            return;
        }
        for (FootballNewsArticle article : railArticles) {
            rightRailBox.getChildren().add(createRailItem(article));
        }
    }

    private void renderCards(List<FootballNewsArticle> articles) {
        if (cardsPane == null) {
            return;
        }
        cardsPane.getChildren().clear();
        if (articles == null || articles.isEmpty()) {
            cardsPane.getChildren().add(createMutedLabel("The top story is the only result for this filter."));
            return;
        }
        List<FootballNewsArticle> stableArticles = List.copyOf(articles);
        int renderedCount = Math.min(stableArticles.size(), Math.max(INITIAL_CARD_LIMIT, visibleCardLimit));
        for (FootballNewsArticle article : stableArticles.subList(0, renderedCount)) {
            cardsPane.getChildren().add(createArticleCard(article));
        }
        if (stableArticles.size() > renderedCount) {
            cardsPane.getChildren().add(createShowMoreCard(stableArticles, stableArticles.size() - renderedCount));
        }
    }

    private void renderEmptyState() {
        if (heroMetaLabel != null) {
            heroMetaLabel.setText("Try a different filter");
        }
        if (heroTitleLabel != null) {
            heroTitleLabel.setText("No football stories found");
        }
        if (heroSummaryLabel != null) {
            heroSummaryLabel.setText("Reset the filters or refresh the live feed.");
        }
        if (heroImageView != null) {
            heroImageView.setImage(null);
            heroImageView.setVisible(false);
            heroImageView.setManaged(false);
        }
        if (heroOpenButton != null) {
            heroOpenButton.setDisable(true);
        }
        refreshBookmarkButton(heroBookmarkButton, null);
        if (rightRailBox != null) {
            rightRailBox.getChildren().setAll(createMutedLabel("No stories in the latest column."));
        }
        if (cardsPane != null) {
            cardsPane.getChildren().setAll(createMutedLabel("No cards to show."));
        }
    }

    private Node createRailItem(FootballNewsArticle article) {
        Button button = new Button();
        button.setMnemonicParsing(false);
        button.setFocusTraversable(false);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("news-rail-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> renderHero(article));

        Label title = new Label(limitText(article.title(), 74));
        title.setWrapText(true);
        title.getStyleClass().add("news-rail-title");

        Label meta = new Label(formatMeta(article));
        meta.getStyleClass().add("news-rail-meta");

        VBox content = new VBox(6, title, meta);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("news-rail-content");
        button.setGraphic(content);
        return button;
    }

    private Node createArticleCard(FootballNewsArticle article) {
        VBox card = new VBox(10);
        card.getStyleClass().add("news-card");
        card.setPrefWidth(270);
        card.setMaxWidth(270);

        StackPane media = new StackPane();
        media.getStyleClass().add("news-card-media");
        media.setMinSize(270, 152);
        media.setPrefSize(270, 152);
        media.setMaxSize(270, 152);

        Image image = loadImage(article.imageUrl(), CARD_IMAGE_WIDTH * 2, CARD_IMAGE_HEIGHT * 2);
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(CARD_IMAGE_WIDTH);
            imageView.setFitHeight(CARD_IMAGE_HEIGHT);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageView.getStyleClass().add("news-card-image");
            installImageFallback(imageView, image, CARD_IMAGE_WIDTH * 2, CARD_IMAGE_HEIGHT * 2);
            media.getChildren().add(imageView);
        } else {
            ImageView fallbackImageView = createLocalHeroFallback(CARD_IMAGE_WIDTH, CARD_IMAGE_HEIGHT);
            if (fallbackImageView != null) {
                media.getChildren().add(fallbackImageView);
            } else {
                Label placeholder = new Label("Sport Insight");
                placeholder.getStyleClass().add("news-image-placeholder");
                media.getChildren().add(placeholder);
            }
        }

        Label title = new Label(limitText(article.title(), 86));
        title.setWrapText(true);
        title.getStyleClass().add("news-card-title");

        Label summary = new Label(limitText(article.summary(), 118));
        summary.setWrapText(true);
        summary.getStyleClass().add("news-card-summary");

        Label meta = new Label(formatMeta(article));
        meta.getStyleClass().add("news-card-meta");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button saveButton = new Button();
        saveButton.setMnemonicParsing(false);
        saveButton.getStyleClass().add("news-secondary-action");
        refreshBookmarkButton(saveButton, article);
        saveButton.setOnAction(event -> toggleSaved(article));

        Button openButton = new Button("Open");
        openButton.setMnemonicParsing(false);
        openButton.getStyleClass().add("news-primary-action-small");
        openButton.setOnAction(event -> openArticle(article));

        HBox actions = new HBox(8, saveButton, openButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(media, title, summary, meta, spacer, actions);
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openArticle(article);
            }
        });
        return card;
    }

    private Node createShowMoreCard(List<FootballNewsArticle> articles, int remainingCount) {
        Button button = new Button("Show more\n" + remainingCount + " more stories");
        button.setMnemonicParsing(false);
        button.setFocusTraversable(false);
        button.getStyleClass().add("news-more-card");
        button.setOnAction(event -> {
            visibleCardLimit += CARD_LIMIT_STEP;
            renderCards(articles);
        });
        return button;
    }

    private Label createMutedLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("news-muted-message");
        return label;
    }

    private void toggleSaved(FootballNewsArticle article) {
        if (article == null || article.url() == null || article.url().isBlank()) {
            return;
        }
        if (!savedUrls.add(article.url())) {
            savedUrls.remove(article.url());
        }
        applyFilters();
    }

    private boolean isSaved(FootballNewsArticle article) {
        return article != null && article.url() != null && savedUrls.contains(article.url());
    }

    private void refreshBookmarkButton(Button button, FootballNewsArticle article) {
        if (button == null) {
            return;
        }
        button.setDisable(article == null);
        boolean saved = isSaved(article);
        button.setText(saved ? "Saved" : "Save");
        button.getStyleClass().removeAll("news-saved-action");
        if (saved) {
            button.getStyleClass().add("news-saved-action");
        }
    }

    private void updateStats(List<FootballNewsArticle> filtered) {
        if (totalCountLabel != null) {
            totalCountLabel.setText(String.valueOf(filtered == null ? 0 : filtered.size()));
        }
        if (savedCountLabel != null) {
            savedCountLabel.setText(String.valueOf(savedUrls.size()));
        }
        if (latestTimeLabel != null) {
            latestTimeLabel.setText(lastLoadedAt == null ? "--" : timeAgo(lastLoadedAt));
        }
        setStatus(lastLoadStatus);
    }

    private void openArticle(FootballNewsArticle article) {
        if (article == null || article.url() == null || article.url().isBlank()) {
            return;
        }
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                showInfo("Open article", article.url());
                return;
            }
            Desktop.getDesktop().browse(URI.create(article.url()));
        } catch (Exception e) {
            showInfo("Open article", article.url());
        }
    }

    private Image loadImage(String rawUrl, double requestedWidth, double requestedHeight) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String url = rawUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return null;
        }
        String cacheKey = url + "|" + Math.round(requestedWidth) + "x" + Math.round(requestedHeight);
        return imageCache.computeIfAbsent(cacheKey,
                key -> new Image(url, requestedWidth, requestedHeight, true, true, true));
    }

    private ImageView createLocalHeroFallback(double width, double height) {
        Image image = loadLocalHeroImage(width, height);
        if (image == null) {
            return null;
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("news-card-image");
        return imageView;
    }

    private Image loadLocalHeroImage(double width, double height) {
        var imageUrl = getClass().getResource("/tn/esprit/images/News hero.png");
        if (imageUrl == null) {
            return null;
        }
        return new Image(imageUrl.toExternalForm(), width, height, false, true, true);
    }

    private void installImageFallback(ImageView imageView, Image image, double width, double height) {
        if (imageView == null || image == null) {
            return;
        }
        Runnable applyFallback = () -> {
            Image fallback = loadLocalHeroImage(width, height);
            if (fallback != null) {
                imageView.setImage(fallback);
            }
        };
        if (image.isError()) {
            applyFallback.run();
            return;
        }
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (Boolean.TRUE.equals(isError)) {
                applyFallback.run();
            }
        });
    }

    private String formatMeta(FootballNewsArticle article) {
        if (article == null) {
            return "";
        }
        return timeAgo(article.publishedAt())
                + " | "
                + readingTime(article)
                + " min read | "
                + DETAIL_DATE_FORMATTER.format(article.publishedAt());
    }

    private int readingTime(FootballNewsArticle article) {
        String text = emptyToFallback(article.title(), "") + " " + emptyToFallback(article.summary(), "");
        int words = text.isBlank() ? 120 : text.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 220.0));
    }

    private String timeAgo(Instant instant) {
        if (instant == null) {
            return "just now";
        }
        Duration duration = Duration.between(instant, Instant.now());
        if (duration.isNegative() || duration.toMinutes() < 1) {
            return "just now";
        }
        if (duration.toMinutes() < 60) {
            return duration.toMinutes() + " min ago";
        }
        if (duration.toHours() < 24) {
            return duration.toHours() + " hr ago";
        }
        return duration.toDays() + " day" + (duration.toDays() == 1 ? "" : "s") + " ago";
    }

    private void setActiveTopic(TopicFilter topic) {
        activeTopic = topic == null ? TopicFilter.ALL : topic;
        setTopicSelected(allTopicButton, activeTopic == TopicFilter.ALL);
        setTopicSelected(premierLeagueButton, activeTopic == TopicFilter.PREMIER_LEAGUE);
        setTopicSelected(championsLeagueButton, activeTopic == TopicFilter.CHAMPIONS_LEAGUE);
        setTopicSelected(transfersButton, activeTopic == TopicFilter.TRANSFERS);
        setTopicSelected(womenButton, activeTopic == TopicFilter.WOMEN);
        setTopicSelected(europeButton, activeTopic == TopicFilter.EUROPE);
    }

    private void setTopicSelected(ToggleButton button, boolean selected) {
        if (button != null) {
            button.setSelected(selected);
        }
    }

    private void setLoading(boolean loading) {
        if (loadingIndicator != null) {
            loadingIndicator.setManaged(loading);
            loadingIndicator.setVisible(loading);
        }
        if (refreshButton != null) {
            refreshButton.setDisable(loading);
        }
        if (resetButton != null) {
            resetButton.setDisable(loading);
        }
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(emptyToFallback(text, "Ready."));
        }
    }

    private void forceScrollPaneViewportTransparent() {
        if (pageScroll == null) {
            return;
        }
        boolean darkMode = ThemeManager.isDarkMode();
        String baseStyle = "-fx-background-color: transparent; -fx-background: transparent;";
        pageScroll.setStyle(scrollPaneStyle(darkMode));
        Node viewport = pageScroll.lookup(".viewport");
        if (viewport != null) {
            viewport.setStyle("-fx-background-color: transparent;");
        }
        if (pageScroll.getContent() != null) {
            pageScroll.getContent().setStyle(baseStyle);
        }
        Node content = pageScroll.lookup(".viewport > .content");
        if (content != null) {
            content.setStyle(baseStyle);
        }
        Node corner = pageScroll.lookup(".corner");
        if (corner != null) {
            corner.setStyle(scrollPaneStyle(darkMode));
        }
        String trackStyle = darkMode
                ? "-fx-background-color: rgba(51, 65, 85, 0.8); -fx-background-radius: 999;"
                : "-fx-background-color: rgba(226, 232, 240, 0.72); -fx-background-radius: 999;";
        String thumbStyle = darkMode
                ? "-fx-background-color: rgba(45, 212, 191, 0.42); -fx-background-radius: 999;"
                : "-fx-background-color: rgba(15, 118, 110, 0.52); -fx-background-radius: 999;";
        pageScroll.lookupAll(".scroll-bar").forEach(node -> node.setStyle(scrollPaneStyle(darkMode)));
        pageScroll.lookupAll(".track").forEach(node -> node.setStyle(trackStyle));
        pageScroll.lookupAll(".track-background").forEach(node -> node.setStyle(trackStyle));
        pageScroll.lookupAll(".thumb").forEach(node -> node.setStyle(thumbStyle));
        pageScroll.lookupAll(".increment-button").forEach(node -> node.setStyle("-fx-background-color: transparent; -fx-padding: 0;"));
        pageScroll.lookupAll(".decrement-button").forEach(node -> node.setStyle("-fx-background-color: transparent; -fx-padding: 0;"));
        applyThemeBackground(darkMode);
    }

    private Node resolveNavigationSource(Node primary, Node fallback) {
        return primary != null ? primary : fallback;
    }

    private void resetVisibleCards() {
        visibleCardLimit = INITIAL_CARD_LIMIT;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (pageRoot != null && pageRoot.getScene() != null) {
            alert.initOwner(pageRoot.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String emptyToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void applyThemeBackground(boolean darkMode) {
        String style = darkMode ? DARK_BACKGROUND_STYLE : LIGHT_BACKGROUND_STYLE;
        if (pageRoot != null) {
            pageRoot.setStyle(style);
            if (pageRoot.getScene() != null && pageRoot.getScene().getRoot() != null) {
                pageRoot.getScene().getRoot().setStyle(style);
            }
        }
        if (pageScroll != null) {
            pageScroll.setStyle(scrollPaneStyle(darkMode));
        }
    }

    private static String scrollPaneStyle(boolean darkMode) {
        return "-fx-background-color: transparent; -fx-background: transparent;";
    }

    private static String limitText(String value, int maxLength) {
        String safeValue = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        int end = Math.max(0, safeValue.lastIndexOf(' ', maxLength - 1));
        if (end < maxLength / 2) {
            end = maxLength - 1;
        }
        return safeValue.substring(0, end).trim() + "...";
    }

    private static String shortMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        String cleaned = message.replace('\r', ' ').replace('\n', ' ').trim();
        return cleaned.length() <= 120 ? cleaned : cleaned.substring(0, 120) + "...";
    }

    private static ThreadFactory daemonFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "football-news-feed");
            thread.setDaemon(true);
            return thread;
        };
    }

    private enum TopicFilter {
        ALL(),
        PREMIER_LEAGUE("premier league", "epl", "arsenal", "chelsea", "liverpool", "manchester", "tottenham"),
        CHAMPIONS_LEAGUE("champions league", "uefa", "real madrid", "barcelona", "bayern", "psg", "inter"),
        TRANSFERS("transfer", "signing", "deal", "contract", "bid", "loan", "window"),
        WOMEN("women", "wsl", "lionesses", "women s", "female"),
        EUROPE("la liga", "serie a", "bundesliga", "ligue 1", "europa", "conference league");

        private final List<String> tokens;

        TopicFilter(String... tokens) {
            this.tokens = List.of(tokens);
        }

        private boolean matches(FootballNewsArticle article) {
            if (this == ALL || article == null) {
                return true;
            }
            String text = normalize(article.title() + " " + article.summary());
            return tokens.stream().anyMatch(text::contains);
        }
    }
}

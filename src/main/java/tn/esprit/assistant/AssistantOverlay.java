package tn.esprit.assistant;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.util.Duration;

public final class AssistantOverlay extends StackPane {
    private static final String JARVIS_ICON_PATH = "/tn/esprit/images/Jarvis.png";
    private static final String CHATBOT_AVATAR_PATH = "/tn/esprit/images/chatbot/logo-chatbot.png";
    private static final String SEND_ICON_PATH = "/tn/esprit/images/chatbot/msg.png";
    private static final AtomicReference<Image> JARVIS_IMAGE = new AtomicReference<>();
    private static final AtomicReference<Image> CHATBOT_AVATAR_IMAGE = new AtomicReference<>();
    private static final AtomicReference<Image> SEND_ICON_IMAGE = new AtomicReference<>();

    private final AssistantService service = AssistantService.getInstance();
    private final Stage stage;
    private final String fxmlPath;
    private final String title;
    private final AssistantService.Context context;
    private final AssistantScreenCatalog.ScreenMeta screenMeta;

    private final VBox panel = new VBox(12);
    private final Button launcherButton = new Button();
    private final Label screenLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label voiceChip = new Label();
    private final Label wakeChip = new Label();
    private final VBox messagesBox = new VBox(10);
    private final ScrollPane messagesScroll = new ScrollPane(messagesBox);
    private final FlowPane quickActionsBox = new FlowPane();
    private final TextArea composer = new TextArea();
    private final Button sendButton = new Button("Send");
    private final Button micButton = new Button();
    private final ToggleButton speakToggle = new ToggleButton("Voice");
    private final StackPane launcherOrb = new StackPane();

    private boolean panelVisible;
    private boolean panelStateInitialized;
    private ParallelTransition panelTransition;
    private ScaleTransition listeningPulse;

    private AssistantOverlay(Stage stage, String fxmlPath, String title, Object controller) {
        this.stage = stage;
        this.fxmlPath = fxmlPath;
        this.title = title;
        this.context = AssistantService.contextFor(fxmlPath, title, controller);
        this.screenMeta = AssistantScreenCatalog.resolve(fxmlPath);
        this.panelVisible = service.isPanelOpen();

        setPickOnBounds(false);
        setPadding(new Insets(20));
        getStyleClass().add("assistant-overlay-root");
        service.setWakeWordListener(signal -> Platform.runLater(() -> handleWakeSignal(signal)));

        VBox dock = new VBox(12);
        dock.setAlignment(Pos.BOTTOM_RIGHT);
        dock.setPickOnBounds(false);
        dock.getStyleClass().add("assistant-dock");

        configurePanel();
        configureLauncher();

        dock.getChildren().addAll(panel, launcherButton);
        getChildren().add(dock);
        StackPane.setAlignment(dock, Pos.BOTTOM_RIGHT);

        updatePanelVisibility();
        refreshMessages();
        refreshStatus(null);
    }

    public static Parent wrap(Parent content, Stage stage, String fxmlPath, String title, Object controller) {
        StackPane host = new StackPane();
        host.getStyleClass().add("assistant-scene-host");
        mirrorThemeClasses(host, content);
        host.getChildren().add(content);

        AssistantOverlay overlay = new AssistantOverlay(stage, fxmlPath, title, controller);
        host.getChildren().add(overlay);
        StackPane.setAlignment(overlay, Pos.BOTTOM_RIGHT);
        return host;
    }

    private static void mirrorThemeClasses(Parent host, Parent content) {
        Runnable sync = () -> {
            boolean dark = host.getStyleClass().contains("theme-dark");
            boolean light = host.getStyleClass().contains("theme-light");

            content.getStyleClass().removeAll("theme-dark", "theme-light");
            if (dark) {
                content.getStyleClass().add("theme-dark");
            } else if (light) {
                content.getStyleClass().add("theme-light");
            }
        };

        host.getStyleClass().addListener((ListChangeListener<String>) change -> sync.run());
        sync.run();
    }

    private void configurePanel() {
        panel.getStyleClass().add("assistant-panel");
        panel.setSpacing(0);
        panel.setPrefWidth(420);
        panel.setMaxWidth(420);
        panel.setPrefHeight(560);
        panel.setMaxHeight(560);

        Label titleLabel = new Label("Sport Insight AI");
        titleLabel.getStyleClass().add("assistant-title");
        VBox titleBox = new VBox(titleLabel);
        titleBox.getStyleClass().add("assistant-title-box");

        screenLabel.setText(screenMeta.title());
        screenLabel.getStyleClass().add("assistant-screen-label");

        Label roleChip = new Label(context.admin() ? "Admin access" : (context.authenticated() ? "User access" : "Guest"));
        roleChip.getStyleClass().add("assistant-chip");

        Label modelChip = new Label("Local AI: " + service.modelRoutingLabel());
        modelChip.getStyleClass().add("assistant-chip");

        voiceChip.setText("Voice: " + service.voiceLabel());
        voiceChip.getStyleClass().add("assistant-chip");
        wakeChip.getStyleClass().add("assistant-chip");
        refreshWakeChip();

        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("assistant-close-button");
        closeButton.setMinSize(34, 34);
        closeButton.setPrefSize(34, 34);
        closeButton.setMaxSize(34, 34);
        closeButton.setOnAction(event -> togglePanel(false));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerTop = new HBox(12, titleBox, spacer, closeButton);
        headerTop.setAlignment(Pos.CENTER_LEFT);
        headerTop.getStyleClass().add("assistant-header-top");

        FlowPane chipRow = new FlowPane(8, 8, screenLabel, roleChip, modelChip, voiceChip, wakeChip);
        chipRow.setAlignment(Pos.CENTER_LEFT);
        chipRow.setPrefWrapLength(340);
        chipRow.getStyleClass().add("assistant-chip-row");

        statusLabel.getStyleClass().add("assistant-status");
        statusLabel.setWrapText(true);

        VBox headerBox = new VBox(headerTop);
        headerBox.getStyleClass().add("assistant-header");

        quickActionsBox.setHgap(8);
        quickActionsBox.setVgap(8);
        quickActionsBox.getStyleClass().add("assistant-quick-actions");
        rebuildQuickActions();

        messagesBox.getStyleClass().add("assistant-messages");
        messagesBox.setFillWidth(true);
        messagesScroll.setFitToWidth(true);
        messagesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messagesScroll.getStyleClass().add("assistant-scroll");
        messagesScroll.setPrefViewportHeight(364);

        composer.getStyleClass().add("assistant-composer");
        composer.setPromptText("Posez une question sur ce match");
        composer.setPrefRowCount(1);
        composer.setMinHeight(58);
        composer.setPrefHeight(58);
        composer.setWrapText(true);
        composer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendComposerText();
            }
        });

        sendButton.getStyleClass().add("assistant-send-button");
        sendButton.setText("");
        sendButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        sendButton.setMinSize(64, 58);
        sendButton.setPrefSize(64, 58);
        ImageView sendIcon = createSendIconView();
        if (sendIcon != null) {
            sendButton.setGraphic(sendIcon);
        } else {
            sendButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
            sendButton.setText(">");
        }
        sendButton.setOnAction(event -> sendComposerText());

        micButton.getStyleClass().add("assistant-mic-button");
        micButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        micButton.setMinSize(46, 42);
        micButton.setPrefSize(46, 42);
        micButton.setMaxSize(46, 42);
        micButton.setOnAction(event -> handleMicButton());

        speakToggle.getStyleClass().add("assistant-toggle-button");
        speakToggle.setSelected(service.isSpeakRepliesEnabled());
        refreshVoiceControls();
        speakToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            service.setSpeakRepliesEnabled(selected);
            refreshVoiceControls();
            refreshStatus(selected
                    ? "Voice replies are on. I'll answer back out loud."
                    : "Voice replies are muted. Turn Voice back on for spoken answers.");
        });

        refreshMicButtonState();

        HBox formRow = new HBox(10, composer, sendButton);
        formRow.setAlignment(Pos.CENTER_LEFT);
        formRow.getStyleClass().add("assistant-form-row");
        HBox.setHgrow(composer, Priority.ALWAYS);

        HBox actions = new HBox(8, speakToggle, micButton, new Region());
        HBox.setHgrow(actions.getChildren().get(2), Priority.ALWAYS);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("assistant-actions-row");

        VBox composerBox = new VBox(8, formRow, actions);
        composerBox.getStyleClass().add("assistant-composer-box");

        panel.getChildren().addAll(headerBox, messagesScroll, composerBox);
    }

    private void configureLauncher() {
        launcherButton.getStyleClass().add("assistant-launcher");
        launcherButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        launcherButton.setMinSize(62, 62);
        launcherButton.setPrefSize(62, 62);
        launcherButton.setMaxSize(62, 62);
        launcherButton.setGraphic(createLauncherGraphic());
        launcherButton.setOnAction(event -> togglePanel(!panelVisible));
        updateLauncherLabel();
    }

    private void rebuildQuickActions() {
        quickActionsBox.getChildren().clear();
        List<String> prompts = screenMeta.quickPrompts();
        for (String prompt : prompts) {
            Button chip = new Button(prompt);
            chip.getStyleClass().add("assistant-quick-action-button");
            chip.setOnAction(event -> sendPrompt(prompt));
            quickActionsBox.getChildren().add(chip);
        }
    }

    private void refreshMessages() {
        messagesBox.getChildren().clear();
        for (AssistantMessage message : service.historySnapshot()) {
            boolean userMessage = message.role() == AssistantMessage.Role.USER;
            Label bubble = new Label(message.content());
            bubble.setWrapText(true);
            bubble.setMaxWidth(userMessage ? 310 : 318);
            bubble.getStyleClass().addAll(
                    "assistant-message-bubble",
                    userMessage ? "assistant-message-user" : "assistant-message-assistant"
            );

            HBox row = userMessage ? new HBox(bubble) : new HBox(8, createBotAvatar(), bubble);
            row.setAlignment(userMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.getStyleClass().addAll("assistant-message-row", userMessage ? "assistant-message-row-user" : "assistant-message-row-assistant");
            messagesBox.getChildren().add(row);
        }
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private void sendComposerText() {
        sendPrompt(composer.getText());
    }

    private void sendPrompt(String rawPrompt) {
        String prompt = rawPrompt == null ? "" : rawPrompt.trim();
        if (prompt.isBlank()) {
            return;
        }

        composer.clear();
        setBusy(true, "Thinking...");
        refreshMessages();

        service.submit(prompt, context).whenComplete((reply, throwable) -> Platform.runLater(() -> {
            setBusy(false, null);
            if (throwable != null) {
                refreshMessages();
                refreshStatus("The assistant could not answer right now: " + throwable.getMessage());
                return;
            }

            refreshMessages();
            refreshStatus(null);
            refreshVoiceControls();
            if (reply != null && reply.command() != null) {
                reply.command().execute(stage);
            }
        }));
    }

    private void handleMicButton() {
        if (!service.isVoiceRecording()) {
            beginVoiceRecording("Microphone on. Speak naturally, then click the mic icon to stop and send.");
            return;
        }

        micButton.setDisable(true);
        refreshStatus("Stopping the microphone and preparing transcription...");
        service.stopVoiceRecording(this::refreshStatus).whenComplete((result, throwable) -> Platform.runLater(() -> {
            micButton.setDisable(false);
            refreshMicButtonState();
            refreshWakeChip();
            updateListeningState();
            if (throwable != null) {
                refreshStatus("Voice transcription failed: " + throwable.getMessage());
                return;
            }
            String normalizedTranscript = result == null ? "" : result.transcript().trim();
            if (normalizedTranscript.isBlank()) {
                refreshStatus("I did not catch any clear speech. Try again and speak a little closer to the microphone.");
                return;
            }
            composer.setText(normalizedTranscript);
            if (result != null && result.clarificationNeeded()) {
                refreshStatus(result.clarificationPrompt());
                return;
            }
            refreshStatus("Voice captured: " + normalizedTranscript);
            sendVoicePrompt(normalizedTranscript);
        }));
    }

    private void beginVoiceRecording(String statusText) {
        if (!service.isSpeakRepliesEnabled()) {
            speakToggle.setSelected(true);
        }

        try {
            service.startVoiceRecording(update -> Platform.runLater(() -> handleVoiceUpdate(update)));
            refreshMicButtonState();
            refreshWakeChip();
            updateListeningState();
            refreshStatus(statusText);
        } catch (Exception ex) {
            refreshMicButtonState();
            refreshWakeChip();
            updateListeningState();
            refreshStatus("Microphone setup failed: " + ex.getMessage());
        }
    }

    private void handleWakeSignal(AssistantWakeSignal signal) {
        refreshWakeChip();
        if (signal == null) {
            return;
        }

        togglePanel(true);
        if (service.isVoiceRecording()) {
            refreshStatus("Hello sir. I'm already listening.");
            return;
        }
        if (composer.isDisabled()) {
            refreshStatus("Hello sir. I'm finishing the current reply first.");
            return;
        }
        service.announceWakeGreeting("Hello sir.");
        refreshMessages();
        beginVoiceRecording("Hello sir. Listening now.");
    }

    private void handleVoiceUpdate(VoiceCaptureUpdate update) {
        if (update == null) {
            return;
        }
        if (update.type() == VoiceCaptureUpdate.Type.LISTENING) {
            refreshStatus("Listening... Speak naturally.");
            return;
        }

        String text = update.text() == null ? "" : update.text().trim();
        if (text.isBlank()) {
            return;
        }
        composer.setText(text);
        if (update.type() == VoiceCaptureUpdate.Type.PARTIAL) {
            refreshStatus("Hearing: " + text);
            return;
        }
        refreshStatus("Heard: " + text);
    }

    private void sendVoicePrompt(String rawPrompt) {
        String prompt = rawPrompt == null ? "" : rawPrompt.trim();
        if (prompt.isBlank()) {
            return;
        }

        composer.clear();
        setBusy(true, "Thinking...");
        refreshMessages();

        service.submitVoice(prompt, context).whenComplete((reply, throwable) -> Platform.runLater(() -> {
            setBusy(false, null);
            if (throwable != null) {
                refreshMessages();
                refreshStatus("The assistant could not answer right now: " + throwable.getMessage());
                return;
            }

            refreshMessages();
            refreshStatus(null);
            refreshVoiceControls();
            if (reply != null && reply.command() != null) {
                reply.command().execute(stage);
            }
        }));
    }

    private void togglePanel(boolean visible) {
        panelVisible = visible;
        service.setPanelOpen(visible);
        updatePanelVisibility();
    }

    private void updatePanelVisibility() {
        if (!panelStateInitialized) {
            applyPanelState(panelVisible);
            panelStateInitialized = true;
            updateLauncherLabel();
            return;
        }
        animatePanel(panelVisible);
        updateLauncherLabel();
    }

    private void updateLauncherLabel() {
        launcherButton.setText("");
        launcherButton.setAccessibleText(panelVisible ? "Hide Sport Insight AI" : "Open Sport Insight AI");
        if (panelVisible) {
            launcherButton.getStyleClass().add("assistant-launcher-open");
        } else {
            launcherButton.getStyleClass().remove("assistant-launcher-open");
        }
    }

    private void setBusy(boolean busy, String statusText) {
        composer.setDisable(busy);
        sendButton.setDisable(busy);
        refreshMicButtonState();
        updateListeningState();
        if (!busy) {
            refreshStatus(statusText);
            return;
        }
        refreshStatus(statusText);
    }

    private void refreshStatus(String override) {
        statusLabel.setText(override == null || override.isBlank() ? service.runtimeStatus(context) : override);
    }

    private void refreshVoiceControls() {
        boolean voiceEnabled = service.isSpeakRepliesEnabled();
        voiceChip.setText("Voice: " + service.voiceLabel() + (voiceEnabled ? " on" : " muted"));
        speakToggle.setText(voiceEnabled ? "Voice on" : "Voice off");
        refreshWakeChip();
        refreshMicButtonState();
        updateListeningState();
    }

    private void refreshMicButtonState() {
        micButton.getStyleClass().removeAll("assistant-mic-on", "assistant-mic-off");
        if (service.isVoiceRecording()) {
            micButton.setText("");
            micButton.setAccessibleText("Microphone on");
            micButton.setGraphic(createMicrophoneIcon(true));
            micButton.getStyleClass().add("assistant-mic-on");
            return;
        }
        micButton.setText("");
        micButton.setAccessibleText("Microphone off");
        micButton.setGraphic(createMicrophoneIcon(false));
        micButton.getStyleClass().add("assistant-mic-off");
    }

    private void refreshWakeChip() {
        wakeChip.setText("Wake: " + service.wakeWordLabel());
    }

    private void applyPanelState(boolean visible) {
        panel.setManaged(visible);
        panel.setVisible(visible);
        panel.setOpacity(visible ? 1.0 : 0.0);
        panel.setScaleX(1.0);
        panel.setScaleY(1.0);
        panel.setTranslateY(0.0);
    }

    private void animatePanel(boolean visible) {
        if (panelTransition != null) {
            panelTransition.stop();
        }

        if (visible) {
            panel.setManaged(true);
            panel.setVisible(true);
            panel.setOpacity(0.0);
            panel.setScaleX(0.88);
            panel.setScaleY(0.88);
            panel.setTranslateY(22);

            FadeTransition fade = new FadeTransition(Duration.millis(220), panel);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_BOTH);

            ScaleTransition scale = new ScaleTransition(Duration.millis(240), panel);
            scale.setFromX(0.88);
            scale.setFromY(0.88);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(240), panel);
            slide.setFromY(22);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            panelTransition = new ParallelTransition(fade, scale, slide);
            panelTransition.play();
            return;
        }

        if (!panel.isVisible()) {
            panel.setManaged(false);
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(170), panel);
        fade.setFromValue(Math.max(0.0, panel.getOpacity()));
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(Duration.millis(180), panel);
        scale.setFromX(panel.getScaleX() == 0.0 ? 1.0 : panel.getScaleX());
        scale.setFromY(panel.getScaleY() == 0.0 ? 1.0 : panel.getScaleY());
        scale.setToX(0.94);
        scale.setToY(0.94);
        scale.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), panel);
        slide.setFromY(panel.getTranslateY());
        slide.setToY(18);
        slide.setInterpolator(Interpolator.EASE_IN);

        panelTransition = new ParallelTransition(fade, scale, slide);
        panelTransition.setOnFinished(event -> {
            panel.setVisible(false);
            panel.setManaged(false);
            panel.setTranslateY(0);
            panel.setScaleX(1.0);
            panel.setScaleY(1.0);
        });
        panelTransition.play();
    }

    private StackPane createLauncherGraphic() {
        launcherOrb.getChildren().clear();
        launcherOrb.getStyleClass().setAll("assistant-launcher-orb");
        launcherOrb.setMinSize(56, 56);
        launcherOrb.setPrefSize(56, 56);
        launcherOrb.setMaxSize(56, 56);
        ImageView logo = createLogoView(44);
        if (logo != null) {
            logo.getStyleClass().add("assistant-launcher-image");
            launcherOrb.getChildren().add(logo);
        } else {
            Label fallback = new Label("J");
            fallback.getStyleClass().add("assistant-launcher-fallback");
            launcherOrb.getChildren().add(fallback);
        }
        return launcherOrb;
    }

    private ImageView createLogoView(double size) {
        return createImageView(loadJarvisImage(), size);
    }

    private StackPane createBotAvatar() {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("assistant-bot-avatar");
        avatar.setMinSize(30, 30);
        avatar.setPrefSize(30, 30);
        avatar.setMaxSize(30, 30);

        ImageView logo = createImageView(loadChatbotAvatarImage(), 22);
        if (logo != null) {
            logo.getStyleClass().add("assistant-bot-avatar-image");
            avatar.getChildren().add(logo);
        } else {
            Label fallback = new Label("AI");
            fallback.getStyleClass().add("assistant-bot-avatar-fallback");
            avatar.getChildren().add(fallback);
        }
        return avatar;
    }

    private ImageView createSendIconView() {
        ImageView icon = createImageView(loadSendIconImage(), 22);
        if (icon != null) {
            icon.getStyleClass().add("assistant-send-icon");
        }
        return icon;
    }

    private StackPane createMicrophoneIcon(boolean active) {
        StackPane icon = new StackPane();
        icon.getStyleClass().addAll("assistant-mic-icon", active ? "assistant-mic-icon-on" : "assistant-mic-icon-off");
        icon.setMinSize(30, 30);
        icon.setPrefSize(30, 30);
        icon.setMaxSize(30, 30);

        if (active) {
            icon.getChildren().addAll(
                    createMicStroke("M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"),
                    createMicStroke("M19 10v2a7 7 0 0 1-14 0v-2"),
                    createMicStroke("M12 19v3"),
                    createMicStroke("M8 22h8")
            );
        } else {
            icon.getChildren().addAll(
                    createMicStroke("M2 2l20 20"),
                    createMicStroke("M18.89 13.23A7.12 7.12 0 0 0 19 12v-2"),
                    createMicStroke("M5 10v2a7 7 0 0 0 12 5"),
                    createMicStroke("M15 9.34V5a3 3 0 0 0-5.68-1.33"),
                    createMicStroke("M9 9v3a3 3 0 0 0 5.12 2.12"),
                    createMicStroke("M12 19v3"),
                    createMicStroke("M8 22h8")
            );
        }
        return icon;
    }

    private SVGPath createMicStroke(String content) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#64748b"));
        path.setStrokeWidth(2.25);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        path.getStyleClass().add("assistant-mic-stroke");
        return path;
    }

    private ImageView createImageView(Image image, double size) {
        if (image == null) {
            return null;
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setCacheHint(CacheHint.QUALITY);
        return imageView;
    }

    private Image loadJarvisImage() {
        return loadImage(JARVIS_ICON_PATH, JARVIS_IMAGE);
    }

    private Image loadChatbotAvatarImage() {
        return loadImage(CHATBOT_AVATAR_PATH, CHATBOT_AVATAR_IMAGE);
    }

    private Image loadSendIconImage() {
        return loadImage(SEND_ICON_PATH, SEND_ICON_IMAGE);
    }

    private Image loadImage(String path, AtomicReference<Image> cache) {
        Image cached = cache.get();
        if (cached != null) {
            return cached;
        }

        return Optional.ofNullable(AssistantOverlay.class.getResourceAsStream(path))
                .map(stream -> {
                    try (var input = stream) {
                        Image image = new Image(input);
                        cache.compareAndSet(null, image);
                        return image;
                    } catch (Exception ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .orElse(null);
    }

    private void updateListeningState() {
        boolean listening = service.isVoiceRecording();
        if (listening) {
            if (!launcherButton.getStyleClass().contains("assistant-launcher-listening")) {
                launcherButton.getStyleClass().add("assistant-launcher-listening");
            }
            startListeningPulse();
            return;
        }

        launcherButton.getStyleClass().remove("assistant-launcher-listening");
        stopListeningPulse();
    }

    private void startListeningPulse() {
        if (listeningPulse == null) {
            listeningPulse = new ScaleTransition(Duration.millis(900), launcherOrb);
            listeningPulse.setFromX(1.0);
            listeningPulse.setFromY(1.0);
            listeningPulse.setToX(1.08);
            listeningPulse.setToY(1.08);
            listeningPulse.setInterpolator(Interpolator.EASE_BOTH);
            listeningPulse.setCycleCount(Animation.INDEFINITE);
            listeningPulse.setAutoReverse(true);
        }
        if (listeningPulse.getStatus() != Animation.Status.RUNNING) {
            listeningPulse.playFromStart();
        }
    }

    private void stopListeningPulse() {
        if (listeningPulse != null) {
            listeningPulse.stop();
        }
        launcherOrb.setScaleX(1.0);
        launcherOrb.setScaleY(1.0);
    }
}

package tn.esprit.assistant;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.gui.SceneNavigator;

import java.util.List;

public final class AssistantOverlay extends StackPane {
    private final AssistantService service = AssistantService.getInstance();
    private final Stage stage;
    private final String fxmlPath;
    private final String title;
    private final AssistantService.Context context;
    private final AssistantScreenCatalog.ScreenMeta screenMeta;

    private final VBox panel = new VBox(14);
    private final Button launcherButton = new Button();
    private final Label screenLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label voiceChip = new Label();
    private final VBox messagesBox = new VBox(10);
    private final ScrollPane messagesScroll = new ScrollPane(messagesBox);
    private final FlowPane quickActionsBox = new FlowPane();
    private final TextArea composer = new TextArea();
    private final Button sendButton = new Button("Send");
    private final Button micButton = new Button("Mic");
    private final ToggleButton speakToggle = new ToggleButton("Voice");

    private boolean panelVisible;

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
        panel.setPrefWidth(390);
        panel.setMaxWidth(390);

        Label titleLabel = new Label("Jarvis");
        titleLabel.getStyleClass().add("assistant-title");

        screenLabel.setText(screenMeta.title());
        screenLabel.getStyleClass().add("assistant-screen-label");

        Label roleChip = new Label(context.admin() ? "Admin access" : (context.authenticated() ? "User access" : "Guest"));
        roleChip.getStyleClass().add("assistant-chip");

        Label modelChip = new Label("Local AI: " + service.modelRoutingLabel());
        modelChip.getStyleClass().add("assistant-chip");

        voiceChip.setText("Voice: " + service.voiceLabel());
        voiceChip.getStyleClass().add("assistant-chip");

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("assistant-close-button");
        closeButton.setOnAction(event -> togglePanel(false));

        HBox headerTop = new HBox(10, titleLabel, new Region(), closeButton);
        HBox.setHgrow(headerTop.getChildren().get(1), Priority.ALWAYS);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        HBox chipRow = new HBox(8, screenLabel, roleChip, modelChip, voiceChip);
        chipRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel.getStyleClass().add("assistant-status");
        statusLabel.setWrapText(true);

        VBox headerBox = new VBox(8, headerTop, chipRow, statusLabel);
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
        messagesScroll.setPrefViewportHeight(290);

        composer.getStyleClass().add("assistant-composer");
        composer.setPromptText("Ask about this screen, or say open teams / open Champions League / open Bayern vs Real Madrid details...");
        composer.setPrefRowCount(2);
        composer.setWrapText(true);
        composer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendComposerText();
            }
        });

        sendButton.getStyleClass().add("assistant-send-button");
        sendButton.setOnAction(event -> sendComposerText());

        micButton.getStyleClass().add("assistant-mic-button");
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

        HBox actions = new HBox(8, speakToggle, micButton, new Region(), sendButton);
        HBox.setHgrow(actions.getChildren().get(2), Priority.ALWAYS);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox composerBox = new VBox(8, composer, actions);
        composerBox.getStyleClass().add("assistant-composer-box");

        panel.getChildren().addAll(headerBox, quickActionsBox, messagesScroll, composerBox);
    }

    private void configureLauncher() {
        launcherButton.getStyleClass().add("assistant-launcher");
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
            bubble.setMaxWidth(285);
            bubble.getStyleClass().addAll(
                    "assistant-message-bubble",
                    userMessage ? "assistant-message-user" : "assistant-message-assistant"
            );

            HBox row = new HBox(bubble);
            row.setAlignment(userMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.getStyleClass().add("assistant-message-row");
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
        if (!service.isSpeakRepliesEnabled()) {
            speakToggle.setSelected(true);
        }

        if (!service.isVoiceRecording()) {
            try {
                service.startVoiceRecording();
                micButton.setText("Stop");
                refreshStatus("Listening... Speak naturally, then click Stop and I'll answer back with voice.");
            } catch (Exception ex) {
                refreshStatus("Microphone setup failed: " + ex.getMessage());
            }
            return;
        }

        micButton.setDisable(true);
        refreshStatus("Stopping the microphone and preparing transcription...");
        service.stopVoiceRecording(this::refreshStatus).whenComplete((transcript, throwable) -> Platform.runLater(() -> {
            micButton.setDisable(false);
            micButton.setText("Mic");
            if (throwable != null) {
                refreshStatus("Voice transcription failed: " + throwable.getMessage());
                return;
            }
            String normalizedTranscript = transcript == null ? "" : transcript.trim();
            if (normalizedTranscript.isBlank()) {
                refreshStatus("I did not catch any clear speech. Try again and speak a little closer to the microphone.");
                return;
            }
            composer.setText(normalizedTranscript);
            refreshStatus("Voice captured: " + normalizedTranscript);
            sendVoicePrompt(normalizedTranscript);
        }));
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
        panel.setVisible(panelVisible);
        panel.setManaged(panelVisible);
        updateLauncherLabel();
    }

    private void updateLauncherLabel() {
        launcherButton.setText(panelVisible ? "Hide Jarvis" : "Jarvis");
    }

    private void setBusy(boolean busy, String statusText) {
        composer.setDisable(busy);
        sendButton.setDisable(busy);
        if (!service.isVoiceRecording()) {
            micButton.setText("Mic");
        }
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
    }
}

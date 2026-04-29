package tn.esprit.gui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Notification;
import tn.esprit.services.NotificationService;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MatchAlertPopupManager {
    private static final MatchAlertPopupManager INSTANCE = new MatchAlertPopupManager();

    private final List<Popup> activePopups = new ArrayList<>();

    private MatchAlertPopupManager() {
    }

    public static MatchAlertPopupManager getInstance() {
        return INSTANCE;
    }

    public void show(Stage owner, Notification notification) {
        if (owner == null || notification == null || !owner.isShowing()) {
            return;
        }

        boolean dark = ThemeManager.isDarkMode();
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(false);
        popup.setHideOnEscape(true);

        VBox root = buildPopupContent(notification, dark);
        root.setOpacity(0.0);
        root.setOnMouseClicked(event -> hidePopup(owner, popup));
        popup.getContent().add(root);
        popup.show(owner);

        synchronized (activePopups) {
            activePopups.add(popup);
        }
        relayout(owner);
        playSound();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(Duration.seconds(6.5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(240), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition transition = new SequentialTransition(fadeIn, hold, fadeOut);
        transition.setOnFinished(event -> hidePopup(owner, popup));
        transition.play();
    }

    public void dismissAll() {
        List<Popup> toHide;
        synchronized (activePopups) {
            toHide = new ArrayList<>(activePopups);
            activePopups.clear();
        }
        for (Popup popup : toHide) {
            if (popup != null) {
                popup.hide();
            }
        }
    }

    private VBox buildPopupContent(Notification notification, boolean dark) {
        if (isUrgentAnnonce(notification)) {
            return buildUrgentAnnoncePopupContent(notification, dark);
        }

        String cardBackground = dark
                ? "linear-gradient(from 0% 0% to 100% 100%, rgba(15, 23, 42, 0.98) 0%, rgba(30, 41, 59, 0.98) 100%)"
                : "linear-gradient(from 0% 0% to 100% 100%, rgba(255, 255, 255, 0.98) 0%, rgba(240, 253, 250, 0.98) 100%)";
        String borderColor = dark ? "rgba(96, 165, 250, 0.34)" : "rgba(15, 118, 110, 0.24)";
        String primaryText = dark ? "#f8fafc" : "#0f172a";
        String secondaryText = dark ? "#bfdbfe" : "#475569";
        String accent = resolveAccentColor(notification.getAccentTone(), dark);

        Label badgeLabel = new Label(resolveBadge(notification));
        badgeLabel.setStyle(
                "-fx-background-color: " + accent + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-padding: 6 10 6 10;"
        );

        Label minuteLabel = new Label(resolveMinuteLabel(notification));
        minuteLabel.setStyle(
                "-fx-background-color: " + (dark ? "rgba(59, 130, 246, 0.16)" : "rgba(15, 118, 110, 0.10)") + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-text-fill: " + primaryText + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-padding: 6 10 6 10;"
        );
        minuteLabel.setManaged(!minuteLabel.getText().isBlank());
        minuteLabel.setVisible(!minuteLabel.getText().isBlank());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, badgeLabel, minuteLabel, topSpacer);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane homeLogo = buildLogo(notification.getHomeTeamLogo(), notification.getHomeTeamName(), dark);
        StackPane awayLogo = buildLogo(notification.getAwayTeamLogo(), notification.getAwayTeamName(), dark);

        Label homeName = new Label(empty(notification.getHomeTeamName(), "Home"));
        homeName.setStyle("-fx-text-fill: " + primaryText + "; -fx-font-size: 12px; -fx-font-weight: 800;");

        Label awayName = new Label(empty(notification.getAwayTeamName(), "Away"));
        awayName.setStyle("-fx-text-fill: " + primaryText + "; -fx-font-size: 12px; -fx-font-weight: 800;");

        Label vsLabel = new Label(buildScoreLabel(notification));
        vsLabel.setStyle("-fx-text-fill: " + primaryText + "; -fx-font-size: 16px; -fx-font-weight: 900;");

        Label competitionLabel = new Label(notification.getCompetitionCode() == null ? "" : notification.getCompetitionCode());
        competitionLabel.setStyle("-fx-text-fill: " + secondaryText + "; -fx-font-size: 11px; -fx-font-weight: 700;");
        competitionLabel.setManaged(!competitionLabel.getText().isBlank());
        competitionLabel.setVisible(!competitionLabel.getText().isBlank());

        VBox centerBox = new VBox(2, vsLabel, competitionLabel);
        centerBox.setAlignment(Pos.CENTER);

        VBox teamsBlock = new VBox(8);
        teamsBlock.getChildren().addAll(
                new HBox(12, homeLogo, createTeamTextColumn(homeName, true), centerBox, createTeamTextColumn(awayName, false), awayLogo)
        );
        ((HBox) teamsBlock.getChildren().get(0)).setAlignment(Pos.CENTER);

        Label titleLabel = new Label(empty(notification.getTitle(), "Match alert"));
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: " + primaryText + "; -fx-font-size: 15px; -fx-font-weight: 900;");

        Label messageLabel = new Label(empty(notification.getMessage(), ""));
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: " + secondaryText + "; -fx-font-size: 12px; -fx-font-weight: 600;");

        VBox textBlock = new VBox(4, titleLabel, messageLabel);

        VBox root = new VBox(12, topRow, teamsBlock, textBlock);
        root.setPrefWidth(360);
        root.setMaxWidth(360);
        root.setPadding(new Insets(16, 16, 16, 16));
        root.setStyle(
                "-fx-background-color: " + cardBackground + ";" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 22;" +
                        "-fx-border-width: 1.1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.34), 26, 0.22, 0, 10);"
        );
        return root;
    }

    private VBox buildUrgentAnnoncePopupContent(Notification notification, boolean dark) {
        String cardBackground = dark
                ? "linear-gradient(from 0% 0% to 100% 100%, rgba(69, 10, 10, 0.98) 0%, rgba(127, 29, 29, 0.96) 100%)"
                : "linear-gradient(from 0% 0% to 100% 100%, rgba(255, 245, 245, 0.99) 0%, rgba(255, 255, 255, 0.98) 100%)";
        String borderColor = dark ? "rgba(248, 113, 113, 0.46)" : "rgba(220, 38, 38, 0.28)";
        String primaryText = dark ? "#fff7ed" : "#111827";
        String secondaryText = dark ? "#fecaca" : "#7f1d1d";
        String accent = resolveAccentColor(notification.getAccentTone(), dark);

        Label badgeLabel = new Label("URGENT POST");
        badgeLabel.setStyle(
                "-fx-background-color: " + accent + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-padding: 6 10 6 10;"
        );

        Label timeLabel = new Label(resolveMinuteLabel(notification));
        timeLabel.setStyle(
                "-fx-background-color: " + (dark ? "rgba(248, 113, 113, 0.18)" : "rgba(220, 38, 38, 0.10)") + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-text-fill: " + primaryText + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-padding: 6 10 6 10;"
        );
        timeLabel.setManaged(!timeLabel.getText().isBlank());
        timeLabel.setVisible(!timeLabel.getText().isBlank());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(8, badgeLabel, timeLabel, topSpacer);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String coachName = empty(notification.getActorName(), "Coach");
        StackPane coachAvatar = buildActorAvatar(notification.getActorImage(), coachName, dark, 58.0, 42.0);

        Label coachLabel = new Label(coachName);
        coachLabel.setStyle("-fx-text-fill: " + primaryText + "; -fx-font-size: 14px; -fx-font-weight: 900;");

        Label actorRoleLabel = new Label("Coach");
        actorRoleLabel.setStyle("-fx-text-fill: " + secondaryText + "; -fx-font-size: 11px; -fx-font-weight: 800;");

        VBox coachText = new VBox(2, coachLabel, actorRoleLabel);
        coachText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(coachText, Priority.ALWAYS);

        HBox coachRow = new HBox(12, coachAvatar, coachText);
        coachRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(empty(notification.getTitle(), "Urgent post"));
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: " + primaryText + "; -fx-font-size: 17px; -fx-font-weight: 900;");

        Label messageLabel = new Label(empty(notification.getMessage(), "A coach published an urgent announcement."));
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: " + secondaryText + "; -fx-font-size: 12px; -fx-font-weight: 700;");

        VBox root = new VBox(12, topRow, coachRow, titleLabel, messageLabel);
        root.setPrefWidth(360);
        root.setMaxWidth(360);
        root.setPadding(new Insets(16, 16, 16, 16));
        root.setStyle(
                "-fx-background-color: " + cardBackground + ";" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 22;" +
                        "-fx-border-width: 1.1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.34), 26, 0.22, 0, 10);"
        );
        return root;
    }

    private VBox createTeamTextColumn(Label teamName, boolean homeSide) {
        VBox box = new VBox(teamName);
        box.setAlignment(homeSide ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private StackPane buildActorAvatar(String imagePath, String actorName, boolean dark, double shellSize, double imageSize) {
        StackPane shell = new StackPane();
        shell.setMinSize(shellSize, shellSize);
        shell.setPrefSize(shellSize, shellSize);
        shell.setMaxSize(shellSize, shellSize);
        shell.setStyle(
                "-fx-background-color: " + (dark ? "rgba(30, 41, 59, 0.96)" : "rgba(255, 255, 255, 0.98)") + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: " + (dark ? "rgba(248, 113, 113, 0.36)" : "rgba(220, 38, 38, 0.22)") + ";" +
                        "-fx-border-radius: 999;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.16), 14, 0.12, 0, 5);"
        );

        Image image = EquipeUiSupport.loadEquipeImage(imagePath);
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(imageSize);
            imageView.setFitHeight(imageSize);
            imageView.setPreserveRatio(false);
            shell.getChildren().add(imageView);
            return shell;
        }

        Label fallback = new Label(EquipeUiSupport.buildInitials(actorName, "C"));
        fallback.setStyle(
                "-fx-text-fill: " + (dark ? "#fef2f2" : "#7f1d1d") + ";" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: 900;"
        );
        shell.getChildren().add(fallback);
        return shell;
    }

    private StackPane buildLogo(String logoPath, String teamName, boolean dark) {
        double size = 48.0;
        StackPane shell = new StackPane();
        shell.setMinSize(size, size);
        shell.setPrefSize(size, size);
        shell.setMaxSize(size, size);
        shell.setStyle(
                "-fx-background-color: " + (dark ? "rgba(30, 41, 59, 0.96)" : "rgba(241, 245, 249, 0.96)") + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: " + (dark ? "rgba(148, 163, 184, 0.28)" : "rgba(148, 163, 184, 0.22)") + ";" +
                        "-fx-border-radius: 999;"
        );

        Image image = EquipeUiSupport.loadEquipeImage(logoPath);
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(34);
            imageView.setFitHeight(34);
            imageView.setPreserveRatio(true);
            shell.getChildren().add(imageView);
            return shell;
        }

        Label fallback = new Label(EquipeUiSupport.buildInitials(teamName, "SI"));
        fallback.setStyle(
                "-fx-text-fill: " + (dark ? "#f8fafc" : "#0f172a") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 900;"
        );
        shell.getChildren().add(fallback);
        return shell;
    }

    private void hidePopup(Stage owner, Popup popup) {
        if (popup == null) {
            return;
        }
        popup.hide();
        synchronized (activePopups) {
            activePopups.remove(popup);
        }
        if (owner != null && owner.isShowing()) {
            Platform.runLater(() -> relayout(owner));
        }
    }

    private void relayout(Stage owner) {
        if (owner == null || !owner.isShowing()) {
            return;
        }

        synchronized (activePopups) {
            double bottomMargin = 26.0;
            double rightMargin = 20.0;
            double stackedOffset = 14.0;
            double nextBottom = owner.getY() + owner.getHeight() - bottomMargin;

            for (int index = activePopups.size() - 1; index >= 0; index--) {
                Popup popup = activePopups.get(index);
                if (popup == null || popup.getContent().isEmpty()) {
                    continue;
                }

                Region content = (Region) popup.getContent().get(0);
                content.applyCss();
                content.autosize();

                double width = Math.max(content.prefWidth(-1), content.getWidth());
                double height = Math.max(content.prefHeight(-1), content.getHeight());
                double x = owner.getX() + owner.getWidth() - width - rightMargin;
                double y = nextBottom - height;
                popup.setX(x);
                popup.setY(y);
                nextBottom = y - stackedOffset;
            }
        }
    }

    private void playSound() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {
            // Best-effort audible hint.
        }
    }

    private String resolveAccentColor(String accentTone, boolean dark) {
        String normalized = accentTone == null ? "" : accentTone.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "danger", "red" -> dark ? "#ef4444" : "#dc2626";
            case "warning", "yellow" -> dark ? "#f59e0b" : "#d97706";
            case "success", "goal", "green" -> dark ? "#22c55e" : "#16a34a";
            case "info", "blue" -> dark ? "#3b82f6" : "#0f766e";
            default -> dark ? "#64748b" : "#475569";
        };
    }

    private String resolveBadge(Notification notification) {
        if (notification == null || notification.getType() == null || notification.getType().isBlank()) {
            return "ALERT";
        }
        return notification.getType().trim().replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private String resolveMinuteLabel(Notification notification) {
        if (notification == null || notification.getMinuteLabel() == null) {
            return "";
        }
        return notification.getMinuteLabel().trim();
    }

    private String buildScoreLabel(Notification notification) {
        if (notification == null) {
            return "VS";
        }

        String title = notification.getTitle() == null ? "" : notification.getTitle();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+\\s*[-:]\\s*\\d+)").matcher(title);
        if (matcher.find()) {
            return matcher.group(1).replace(':', '-');
        }
        return "VS";
    }

    private boolean isUrgentAnnonce(Notification notification) {
        return notification != null
                && NotificationService.TYPE_URGENT_ANNONCE.equalsIgnoreCase(empty(notification.getType(), ""));
    }

    private String empty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

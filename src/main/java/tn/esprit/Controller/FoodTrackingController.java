package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.DailyNutritionSummary;
import tn.esprit.entities.FoodLog;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.security.AuthSession;
import tn.esprit.services.DailyNutritionSummaryService;
import tn.esprit.services.FoodLogService;
import tn.esprit.services.NutritionApiService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FoodTrackingController {

    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> mealTypeComboBox;
    @FXML
    private TextArea foodDescriptionArea;
    @FXML
    private Button analyzeButton;
    @FXML
    private Label caloriesLabel;
    @FXML
    private Label proteinLabel;
    @FXML
    private Label carbsLabel;
    @FXML
    private Label fatLabel;
    @FXML
    private Label fiberLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button backButton;
    @FXML
    private VBox foodLogContainer;
    @FXML
    private Label totalCaloriesLabel;
    @FXML
    private Label targetCaloriesLabel;
    @FXML
    private ProgressBar calorieProgressBar;

    private FoodLogService foodLogService;
    private DailyNutritionSummaryService summaryService;
    private NutritionApiService nutritionApiService;
    private Integer currentUserId;
    private NutritionApiService.NutritionInfo currentNutritionInfo;

    @FXML
    public void initialize() {
        try {
            foodLogService = new FoodLogService();
            summaryService = new DailyNutritionSummaryService();
            nutritionApiService = new NutritionApiService();

            currentUserId = AuthSession.getCurrentUser() != null ? AuthSession.getCurrentUser().getId() : null;
            if (currentUserId == null) {
                showError("Erreur", "Vous devez être connecté pour utiliser cette fonctionnalité.");
                return;
            }

            mealTypeComboBox.getItems().addAll("breakfast", "lunch", "dinner", "snack");
            mealTypeComboBox.setValue("breakfast");

            datePicker.setValue(LocalDate.now());
            loadFoodLog(LocalDate.now());

            datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadFoodLog(newVal);
                }
            });
        } catch (SQLException e) {
            showError("Erreur", "Impossible d'initialiser le suivi nutritionnel: " + e.getMessage());
        }
    }

    @FXML
    private void handleAnalyzeFood() {
        String foodDescription = foodDescriptionArea.getText();
        if (foodDescription == null || foodDescription.trim().isEmpty()) {
            showError("Erreur", "Veuillez décrire ce que vous avez mangé.");
            return;
        }

        analyzeButton.setDisable(true);
        analyzeButton.setText("Analyse en cours...");

        new Thread(() -> {
            try {
                currentNutritionInfo = nutritionApiService.analyzeFood(foodDescription.trim());
                javafx.application.Platform.runLater(() -> {
                    displayNutritionInfo(currentNutritionInfo);
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("🔍 Analyser");
                    saveButton.setDisable(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Erreur API", "Impossible d'analyser l'aliment: " + e.getMessage());
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("🔍 Analyser");
                });
            }
        }).start();
    }

    @FXML
    private void handleSaveFood() {
        if (currentNutritionInfo == null) {
            showError("Erreur", "Veuillez d'abord analyser l'aliment.");
            return;
        }

        try {
            FoodLog log = new FoodLog();
            log.setUserId(currentUserId);
            log.setLogDate(datePicker.getValue());
            log.setMealType(mealTypeComboBox.getValue());
            log.setFoodDescription(currentNutritionInfo.getFoodDescription());
            log.setCalories(currentNutritionInfo.getCalories());
            log.setProteinG(currentNutritionInfo.getProteinG());
            log.setCarbsG(currentNutritionInfo.getCarbsG());
            log.setFatG(currentNutritionInfo.getFatG());
            log.setFiberG(currentNutritionInfo.getFiberG());
            log.setApiResponse(currentNutritionInfo.getApiResponse());

            foodLogService.add(log);
            summaryService.recalculateSummary(currentUserId, datePicker.getValue());

            foodDescriptionArea.clear();
            clearNutritionInfo();
            currentNutritionInfo = null;
            saveButton.setDisable(true);

            loadFoodLog(datePicker.getValue());
            showInfo("Succès", "Repas enregistré avec succès!");
        } catch (SQLException e) {
            showError("Erreur", "Impossible d'enregistrer le repas: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToEntrainement() {
        SceneNavigator.switchScene(
                backButton,
                "/tn/esprit/views/entrainement-user-view.fxml",
                "/tn/esprit/styles/entrainement-theme.css",
                "Entrainements | Sport Insight"
        );
    }

    private void loadFoodLog(LocalDate date) {
        try {
            List<FoodLog> logs = foodLogService.getByUserAndDate(currentUserId, date);
            DailyNutritionSummary summary = summaryService.getByUserAndDate(currentUserId, date);

            displayFoodLogs(logs);

            if (summary != null) {
                displayDailySummary(summary);
            } else {
                totalCaloriesLabel.setText("0 kcal");
                targetCaloriesLabel.setText("Objectif: 2500 kcal");
                calorieProgressBar.setProgress(0);
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les données: " + e.getMessage());
        }
    }

    private void displayFoodLogs(List<FoodLog> logs) {
        foodLogContainer.getChildren().clear();

        if (logs.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.getStyleClass().add("empty-log-state");

            Label emptyEmoji = new Label("🥗");
            emptyEmoji.getStyleClass().add("empty-log-emoji");

            Label emptyLabel = new Label("Aucun repas enregistré pour cette journée");
            emptyLabel.getStyleClass().add("empty-log-title");

            Label emptyHint = new Label("Analysez un repas, enregistrez-le, puis il apparaîtra ici avec ses calories et macros.");
            emptyHint.setWrapText(true);
            emptyHint.getStyleClass().add("empty-log-text");

            emptyState.getChildren().addAll(emptyEmoji, emptyLabel, emptyHint);
            foodLogContainer.getChildren().add(emptyState);
            return;
        }

        for (FoodLog log : logs) {
            foodLogContainer.getChildren().add(createFoodLogCard(log));
        }
    }

    private VBox createFoodLogCard(FoodLog log) {
        String mealType = safeMealType(log.getMealType());

        VBox card = new VBox(12);
        card.getStyleClass().add("food-log-card");

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("food-log-header");

        HBox mealBadge = new HBox(10);
        mealBadge.setAlignment(Pos.CENTER_LEFT);
        mealBadge.setStyle(
                "-fx-background-color: " + getMealTypeColor(mealType) + ";" +
                "-fx-padding: 8 16;" +
                "-fx-background-radius: 12;"
        );
        mealBadge.getStyleClass().add("food-log-badge");

        Label emojiLabel = new Label(getMealTypeEmoji(mealType));
        emojiLabel.setStyle("-fx-font-size: 20px;");

        Label mealTypeLabel = new Label(getMealTypeName(mealType));
        mealTypeLabel.getStyleClass().add("food-log-meal-label");

        mealBadge.getChildren().addAll(emojiLabel, mealTypeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteButton = new Button("🗑️");
        deleteButton.getStyleClass().add("food-log-delete-button");
        deleteButton.setOnAction(e -> handleDeleteFoodLog(log));

        header.getChildren().addAll(mealBadge, spacer, deleteButton);

        HBox metaRow = new HBox();
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getStyleClass().add("food-log-meta");

        Label dateLabel = new Label("📅 " + (log.getLogDate() != null
                ? log.getLogDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "Date inconnue"));
        dateLabel.getStyleClass().add("food-log-date-pill");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        metaRow.getChildren().addAll(dateLabel, metaSpacer);

        HBox descBox = new HBox(10);
        descBox.setAlignment(Pos.CENTER_LEFT);
        descBox.getStyleClass().add("food-log-description-box");

        Label descIcon = new Label("📝");
        descIcon.setStyle("-fx-font-size: 18px;");

        Label foodLabel = new Label(log.getFoodDescription() == null ? "Description indisponible" : log.getFoodDescription());
        foodLabel.setWrapText(true);
        foodLabel.getStyleClass().add("food-log-description");
        HBox.setHgrow(foodLabel, Priority.ALWAYS);

        descBox.getChildren().addAll(descIcon, foodLabel);

        VBox caloriesBanner = new VBox(4);
        caloriesBanner.setAlignment(Pos.CENTER);
        caloriesBanner.getStyleClass().add("food-log-calories-banner");
        caloriesBanner.setMaxWidth(Double.MAX_VALUE);

        Label caloriesCaption = new Label("Calories du repas");
        caloriesCaption.getStyleClass().add("food-log-calories-caption");

        Label caloriesValue = new Label(String.format("%.0f kcal", log.getCalories()));
        caloriesValue.getStyleClass().add("food-log-calories-value");

        caloriesBanner.getChildren().addAll(caloriesCaption, caloriesValue);

        HBox metricsRow = new HBox(10);
        metricsRow.setAlignment(Pos.CENTER_LEFT);
        metricsRow.getStyleClass().add("food-log-metrics-row");
        metricsRow.setFillHeight(true);
        metricsRow.setMaxWidth(Double.MAX_VALUE);

        VBox proteinBox = createNutrientBox("💪", "Protéines",
                String.format("%.1fg", log.getProteinG()),
                "rgba(34,211,238,0.18)", "#06b6d4");
        VBox carbsBox = createNutrientBox("🍞", "Glucides",
                String.format("%.1fg", log.getCarbsG()),
                "rgba(236,72,153,0.16)", "#ec4899");
        VBox fatBox = createNutrientBox("🥑", "Lipides",
                String.format("%.1fg", log.getFatG()),
                "rgba(245,158,11,0.16)", "#f59e0b");
        VBox fiberBox = createNutrientBox("🌾", "Fibres",
                String.format("%.1fg", log.getFiberG()),
                "rgba(59,130,246,0.16)", "#3b82f6");

        HBox.setHgrow(caloriesBanner, Priority.ALWAYS);
        HBox.setHgrow(proteinBox, Priority.ALWAYS);
        HBox.setHgrow(carbsBox, Priority.ALWAYS);
        HBox.setHgrow(fatBox, Priority.ALWAYS);
        HBox.setHgrow(fiberBox, Priority.ALWAYS);

        metricsRow.getChildren().addAll(caloriesBanner, proteinBox, carbsBox, fatBox, fiberBox);

        card.getChildren().addAll(header, metaRow, descBox, metricsRow);
        return card;
    }

    private VBox createNutrientBox(String emoji, String label, String value, String bgColor, String textColor) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-border-color: " + textColor + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 10;"
        );
        box.getStyleClass().add("food-log-nutrient-box");

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 18px;");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("food-log-nutrient-value");
        valueLabel.setStyle("-fx-text-fill: white;");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("food-log-nutrient-label");
        nameLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.88);");

        box.getChildren().addAll(emojiLabel, valueLabel, nameLabel);
        return box;
    }

    private String getMealTypeColor(String mealType) {
        return switch (safeMealType(mealType).toLowerCase()) {
            case "breakfast" -> "linear-gradient(135deg, #FFA07A 0%, #FF7F50 100%)";
            case "lunch" -> "linear-gradient(135deg, #BB8FCE 0%, #9B59B6 100%)";
            case "dinner" -> "linear-gradient(135deg, #F8B88B 0%, #F39C12 100%)";
            case "snack" -> "linear-gradient(135deg, #85C1E2 0%, #3498DB 100%)";
            default -> "linear-gradient(135deg, #95A5A6 0%, #7F8C8D 100%)";
        };
    }

    private void handleDeleteFoodLog(FoodLog log) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce repas ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    foodLogService.delete(log.getId());
                    summaryService.recalculateSummary(currentUserId, log.getLogDate());
                    loadFoodLog(datePicker.getValue());
                } catch (SQLException e) {
                    showError("Erreur", "Impossible de supprimer le repas: " + e.getMessage());
                }
            }
        });
    }

    private void displayDailySummary(DailyNutritionSummary summary) {
        totalCaloriesLabel.setText(String.format("%.0f kcal", summary.getTotalCalories()));

        double target = summary.getTargetCalories() != null ? summary.getTargetCalories() : 2500.0;
        targetCaloriesLabel.setText(String.format("Objectif: %.0f kcal", target));

        double progress = summary.getTotalCalories() / target;
        calorieProgressBar.setProgress(Math.min(progress, 1.0));

        if (progress < 0.8) {
            calorieProgressBar.setStyle("-fx-accent: #3498db;");
        } else if (progress <= 1.1) {
            calorieProgressBar.setStyle("-fx-accent: #2ecc71;");
        } else {
            calorieProgressBar.setStyle("-fx-accent: #e74c3c;");
        }
    }

    private void displayNutritionInfo(NutritionApiService.NutritionInfo info) {
        caloriesLabel.setText(String.format("%.0f kcal", info.getCalories()));
        proteinLabel.setText(String.format("%.1f g", info.getProteinG()));
        carbsLabel.setText(String.format("%.1f g", info.getCarbsG()));
        fatLabel.setText(String.format("%.1f g", info.getFatG()));
        fiberLabel.setText(String.format("%.1f g", info.getFiberG()));
    }

    private void clearNutritionInfo() {
        caloriesLabel.setText("-");
        proteinLabel.setText("-");
        carbsLabel.setText("-");
        fatLabel.setText("-");
        fiberLabel.setText("-");
    }

    private String getMealTypeEmoji(String mealType) {
        return switch (safeMealType(mealType).toLowerCase()) {
            case "breakfast" -> "🌅";
            case "lunch" -> "🍽️";
            case "dinner" -> "🌙";
            case "snack" -> "🍎";
            default -> "🍴";
        };
    }

    private String getMealTypeName(String mealType) {
        return switch (safeMealType(mealType).toLowerCase()) {
            case "breakfast" -> "Petit-déjeuner";
            case "lunch" -> "Déjeuner";
            case "dinner" -> "Dîner";
            case "snack" -> "Collation";
            default -> safeMealType(mealType);
        };
    }

    private String safeMealType(String mealType) {
        return (mealType == null || mealType.trim().isEmpty()) ? "unknown" : mealType.trim();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

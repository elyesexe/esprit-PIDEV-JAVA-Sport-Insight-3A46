package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.User;
import tn.esprit.gui.SceneNavigator;
import tn.esprit.gui.ThemeManager;
import tn.esprit.gui.UserNavbarMenu;
import tn.esprit.security.AuthSession;
import tn.esprit.services.AIRecommendationService;
import tn.esprit.services.EntrainementService;
import tn.esprit.services.PerformanceAnalyticsService;
import tn.esprit.services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PlayerPerformanceController {

    @FXML
    private HBox sidebarBrandBox;
    @FXML
    private ToggleButton themeToggleButton;
    @FXML
    private Button userMenuButton;

    @FXML
    private Label playerNameLabel;
    @FXML
    private Label totalEvaluationsLabel;
    @FXML
    private Label attendanceRateLabel;

    @FXML
    private Label avgPhysiqueLabel;
    @FXML
    private Label avgTechniqueLabel;
    @FXML
    private Label avgTactiqueLabel;
    @FXML
    private Label avgOverallLabel;

    @FXML
    private Label improvementPhysiqueLabel;
    @FXML
    private Label improvementTechniqueLabel;
    @FXML
    private Label improvementTactiqueLabel;
    @FXML
    private Label improvementOverallLabel;

    @FXML
    private LineChart<Number, Number> performanceChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Button filterButton;
    @FXML
    private Button resetButton;

    @FXML
    private VBox weakAreasBox;
    @FXML
    private Button getRecommendationsButton;

    private PerformanceAnalyticsService analyticsService;
    private EntrainementService entrainementService;
    private UserService userService;
    private AIRecommendationService aiService;
    private User currentUser;
    private List<Evaluation> allEvaluations;

    @FXML
    public void initialize() {
        // Theme setup
        ThemeManager.bindToggle(themeToggleButton);

        // User menu
        currentUser = AuthSession.getCurrentUser();
        if (currentUser != null) {
            UserNavbarMenu.configureLoadedController(this);
            playerNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        }

        // Initialize services
        try {
            analyticsService = new PerformanceAnalyticsService();
            entrainementService = new EntrainementService();
            userService = new UserService();
            aiService = new AIRecommendationService();
            loadPerformanceData();
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les données: " + e.getMessage());
        }

        // Setup date pickers
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusMonths(3));

        // Setup chart
        yAxis.setLabel("Score");
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(20);
        yAxis.setTickUnit(2);
        xAxis.setLabel("Évaluation #");

        // Event handlers
        filterButton.setOnAction(e -> applyDateFilter());
        resetButton.setOnAction(e -> resetFilter());
        getRecommendationsButton.setOnAction(e -> openAIRecommendations());
    }

    private void loadPerformanceData() throws SQLException {
        if (currentUser == null) return;

        // Load all evaluations
        allEvaluations = analyticsService.getPlayerEvaluationHistory(currentUser.getId());
        
        if (allEvaluations.isEmpty()) {
            showInfo("Aucune donnée", "Vous n'avez pas encore d'évaluations.");
            return;
        }

        // Update statistics
        updateStatistics(allEvaluations);
        
        // Update chart
        updateChart(allEvaluations);
        
        // Analyze weak areas
        analyzeWeakAreas(allEvaluations);
    }

    private void updateStatistics(List<Evaluation> evaluations) throws SQLException {
        // Basic stats
        totalEvaluationsLabel.setText(String.valueOf(evaluations.size()));
        
        double attendanceRate = analyticsService.getAttendanceRate(currentUser.getId());
        attendanceRateLabel.setText(String.format("%.1f%%", attendanceRate));

        // Performance averages
        PerformanceAnalyticsService.PerformanceStats stats = analyticsService.calculateStats(evaluations);
        avgPhysiqueLabel.setText(String.format("%.2f / 20", stats.avgPhysique()));
        avgTechniqueLabel.setText(String.format("%.2f / 20", stats.avgTechnique()));
        avgTactiqueLabel.setText(String.format("%.2f / 20", stats.avgTactique()));
        avgOverallLabel.setText(String.format("%.2f / 20", stats.avgOverall()));

        // Improvement percentages
        if (evaluations.size() >= 2) {
            PerformanceAnalyticsService.ImprovementStats improvement = analyticsService.calculateImprovement(evaluations);
            improvementPhysiqueLabel.setText(formatImprovement(improvement.physiqueImprovement()));
            improvementTechniqueLabel.setText(formatImprovement(improvement.techniqueImprovement()));
            improvementTactiqueLabel.setText(formatImprovement(improvement.tactiqueImprovement()));
            improvementOverallLabel.setText(formatImprovement(improvement.overallImprovement()));
        } else {
            improvementPhysiqueLabel.setText("N/A");
            improvementTechniqueLabel.setText("N/A");
            improvementTactiqueLabel.setText("N/A");
            improvementOverallLabel.setText("N/A");
        }
    }

    private void updateChart(List<Evaluation> evaluations) {
        performanceChart.getData().clear();

        XYChart.Series<Number, Number> physiqueSeries = new XYChart.Series<>();
        physiqueSeries.setName("Physique");

        XYChart.Series<Number, Number> techniqueSeries = new XYChart.Series<>();
        techniqueSeries.setName("Technique");

        XYChart.Series<Number, Number> tactiqueSeries = new XYChart.Series<>();
        tactiqueSeries.setName("Tactique");

        for (int i = 0; i < evaluations.size(); i++) {
            Evaluation eval = evaluations.get(i);
            physiqueSeries.getData().add(new XYChart.Data<>(i + 1, eval.getNotePhysique()));
            techniqueSeries.getData().add(new XYChart.Data<>(i + 1, eval.getNoteTechnique()));
            tactiqueSeries.getData().add(new XYChart.Data<>(i + 1, eval.getNoteTactique()));
        }

        performanceChart.getData().addAll(physiqueSeries, techniqueSeries, tactiqueSeries);
    }

    private void analyzeWeakAreas(List<Evaluation> evaluations) {
        weakAreasBox.getChildren().clear();

        PerformanceAnalyticsService.PerformanceStats stats = analyticsService.calculateStats(evaluations);
        
        // Find the weakest area
        double minScore = Math.min(stats.avgPhysique(), Math.min(stats.avgTechnique(), stats.avgTactique()));
        
        if (stats.avgPhysique() == minScore && stats.avgPhysique() < 15) {
            addWeakAreaLabel("💪 Physique: Besoin d'amélioration", stats.avgPhysique());
        }
        if (stats.avgTechnique() == minScore && stats.avgTechnique() < 15) {
            addWeakAreaLabel("⚽ Technique: Besoin d'amélioration", stats.avgTechnique());
        }
        if (stats.avgTactique() == minScore && stats.avgTactique() < 15) {
            addWeakAreaLabel("🎯 Tactique: Besoin d'amélioration", stats.avgTactique());
        }

        if (weakAreasBox.getChildren().isEmpty()) {
            Label excellentLabel = new Label("✅ Excellentes performances dans tous les domaines!");
            excellentLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 14px; -fx-padding: 10px;");
            weakAreasBox.getChildren().add(excellentLabel);
        }
    }

    private void addWeakAreaLabel(String text, double score) {
        Label label = new Label(text + String.format(" (%.2f/20)", score));
        label.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 14px; -fx-padding: 5px;");
        weakAreasBox.getChildren().add(label);
    }

    private void applyDateFilter() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            showError("Erreur", "Veuillez sélectionner les dates de début et de fin");
            return;
        }

        if (start.isAfter(end)) {
            showError("Erreur", "La date de début doit être avant la date de fin");
            return;
        }

        try {
            List<Evaluation> filtered = analyticsService.getPlayerEvaluationsByDateRange(
                currentUser.getId(), start, end
            );
            
            if (filtered.isEmpty()) {
                showInfo("Aucune donnée", "Aucune évaluation trouvée pour cette période");
                return;
            }

            updateStatistics(filtered);
            updateChart(filtered);
            analyzeWeakAreas(filtered);
        } catch (SQLException e) {
            showError("Erreur", "Erreur lors du filtrage: " + e.getMessage());
        }
    }

    private void resetFilter() {
        try {
            loadPerformanceData();
            startDatePicker.setValue(LocalDate.now().minusMonths(3));
            endDatePicker.setValue(LocalDate.now());
        } catch (SQLException e) {
            showError("Erreur", "Erreur lors de la réinitialisation: " + e.getMessage());
        }
    }

    private void openAIRecommendations() {
        if (allEvaluations == null || allEvaluations.isEmpty()) {
            showInfo("Pas de données", "Vous devez avoir au moins une évaluation pour obtenir des recommandations.");
            return;
        }

        // Show loading dialog
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Génération en cours...");
        loadingAlert.setHeaderText("🤖 L'IA analyse vos performances");
        loadingAlert.setContentText("Veuillez patienter...");
        loadingAlert.show();

        // Generate recommendations in background thread
        new Thread(() -> {
            try {
                // Calculate stats
                PerformanceAnalyticsService.PerformanceStats stats = analyticsService.calculateStats(allEvaluations);
                double attendanceRate = analyticsService.getAttendanceRate(currentUser.getId());
                
                String playerName = currentUser.getPrenom() + " " + currentUser.getNom();
                
                // Call AI service
                AIRecommendationService.AIAdvice advice = aiService.generateComprehensiveAdvice(
                    playerName,
                    stats.avgPhysique(),
                    stats.avgTechnique(),
                    stats.avgTactique(),
                    attendanceRate,
                    allEvaluations.size()
                );

                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showAIRecommendationsDialog(advice);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showError("Erreur IA", "Erreur lors de la génération des recommandations:\n" + e.getMessage());
                });
            }
        }).start();
    }

    private void showAIRecommendationsDialog(AIRecommendationService.AIAdvice advice) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("🤖 Recommandations IA Personnalisées");
        dialog.setResizable(true);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.setStyle("-fx-background-color: white;");
        root.setMaxWidth(800);
        root.setMaxHeight(700);

        // Header
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(10);
        header.setStyle("-fx-background: linear-gradient(to right, #667eea 0%, #764ba2 100%); -fx-padding: 25px;");
        
        Label titleLabel = new Label("🤖 Recommandations IA Personnalisées");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label subtitleLabel = new Label("Basé sur l'analyse de vos " + allEvaluations.size() + " évaluations");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.9);");
        
        Label weakAreaLabel = new Label("Point faible identifié: " + advice.weakArea());
        weakAreaLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffd700; -fx-font-weight: bold;");
        
        header.getChildren().addAll(titleLabel, subtitleLabel, weakAreaLabel);

        // Content with tabs
        javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
        tabPane.setStyle("-fx-padding: 20px;");

        // Training tab
        javafx.scene.control.Tab trainingTab = new javafx.scene.control.Tab("🏃 Entraînement");
        trainingTab.setClosable(false);
        javafx.scene.control.TextArea trainingArea = new javafx.scene.control.TextArea(advice.trainingRecommendations());
        trainingArea.setWrapText(true);
        trainingArea.setEditable(false);
        trainingArea.setStyle("-fx-font-size: 14px; -fx-font-family: 'Segoe UI'; -fx-control-inner-background: #f9f9f9;");
        trainingArea.setPrefHeight(400);
        trainingTab.setContent(new javafx.scene.control.ScrollPane(trainingArea));

        // Nutrition tab
        javafx.scene.control.Tab nutritionTab = new javafx.scene.control.Tab("🍎 Nutrition");
        nutritionTab.setClosable(false);
        javafx.scene.control.TextArea nutritionArea = new javafx.scene.control.TextArea(advice.nutritionAdvice());
        nutritionArea.setWrapText(true);
        nutritionArea.setEditable(false);
        nutritionArea.setStyle("-fx-font-size: 14px; -fx-font-family: 'Segoe UI'; -fx-control-inner-background: #f9f9f9;");
        nutritionArea.setPrefHeight(400);
        nutritionTab.setContent(new javafx.scene.control.ScrollPane(nutritionArea));

        tabPane.getTabs().addAll(trainingTab, nutritionTab);

        // Footer
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(15);
        footer.setStyle("-fx-padding: 20px; -fx-alignment: center-right; -fx-background-color: #f5f5f5;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 30px; -fx-background-radius: 8px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("💾 Sauvegarder");
        saveBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 30px; -fx-background-radius: 8px; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            // TODO: Save recommendations to database
            showInfo("Sauvegarde", "Fonctionnalité de sauvegarde à venir!");
        });

        footer.getChildren().addAll(saveBtn, closeBtn);

        root.getChildren().addAll(header, tabPane, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML
    private void handleOpenHome() {
        SceneNavigator.switchScene(sidebarBrandBox, "/tn/esprit/views/home-view.fxml", 
            "/tn/esprit/styles/home-theme.css", "Sport Insight | Accueil");
    }

    private String formatImprovement(double improvement) {
        String sign = improvement >= 0 ? "+" : "";
        String color = improvement >= 0 ? "#4caf50" : "#f44336";
        return String.format("%s%.1f%%", sign, improvement);
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

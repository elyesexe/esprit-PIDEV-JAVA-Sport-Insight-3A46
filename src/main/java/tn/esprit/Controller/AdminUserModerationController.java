package tn.esprit.Controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import tn.esprit.entities.User;
import tn.esprit.security.AuthSession;
import tn.esprit.security.UserRoles;
import tn.esprit.services.UserService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

public class AdminUserModerationController {
    private static final String DARK_TABLE_CLASS = "admin-dashboard-force-dark";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label statusLabel;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label adminUsersLabel;
    @FXML
    private Label inactiveUsersLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Label resultsMetaLabel;
    @FXML
    private TableView<User> userTableView;
    @FXML
    private TableColumn<User, Integer> idColumn;
    @FXML
    private TableColumn<User, String> nameColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, String> statusColumn;
    @FXML
    private TableColumn<User, String> createdColumn;
    @FXML
    private Label selectionStateLabel;
    @FXML
    private Label validationLabel;
    @FXML
    private TextField emailField;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField nomField;
    @FXML
    private TextField telephoneField;
    @FXML
    private DatePicker dateNaissancePicker;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private ComboBox<String> statusComboBox;
    @FXML
    private TextField photoField;
    @FXML
    private TextField cvField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button saveButton;
    @FXML
    private Button deleteButton;

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final FilteredList<User> filteredUsers = new FilteredList<>(users, user -> true);

    private UserService userService;
    private User selectedUser;

    @FXML
    public void initialize() {
        configureInputs();
        configureTable();
        bindUi();

        try {
            userService = new UserService();
            refreshUsers(null);
        } catch (IllegalStateException ex) {
            saveButton.setDisable(true);
            deleteButton.setDisable(true);
            showStatus("User moderation is unavailable because the database connection failed.", "status-error");
        }
    }

    public void setDarkMode(boolean darkMode) {
        if (userTableView == null) {
            return;
        }
        if (darkMode) {
            if (!userTableView.getStyleClass().contains(DARK_TABLE_CLASS)) {
                userTableView.getStyleClass().add(DARK_TABLE_CLASS);
            }
            return;
        }
        userTableView.getStyleClass().remove(DARK_TABLE_CLASS);
    }

    @FXML
    private void handleRefresh() {
        refreshUsers(selectedUser == null ? null : selectedUser.getId());
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        applyFilter();
        showStatus("Filters reset.", "status-muted");
    }

    @FXML
    private void handleSave() {
        hideValidation();

        if (selectedUser == null) {
            showValidation("Select a user before saving changes.");
            return;
        }
        if (userService == null) {
            showValidation("The user moderation service is not available.");
            return;
        }

        String email = clean(emailField.getText());
        String prenom = clean(prenomField.getText());
        String nom = clean(nomField.getText());
        String telephone = clean(telephoneField.getText());
        String role = UserRoles.coerceSingleRole(roleComboBox.getValue());
        String status = clean(statusComboBox.getEditor().getText());
        String photo = clean(photoField.getText());
        String cvName = clean(cvField.getText());
        String newPassword = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (email == null || prenom == null || nom == null) {
            showValidation("Email, first name, and last name are required.");
            return;
        }
        if (status == null) {
            showValidation("Status is required.");
            return;
        }
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 8) {
                showValidation("New passwords must contain at least 8 characters.");
                return;
            }
            if (!Objects.equals(newPassword, confirmPassword)) {
                showValidation("Password confirmation does not match.");
                return;
            }
        }

        User currentUser = AuthSession.getCurrentUser();
        if (currentUser != null
                && currentUser.getId() != null
                && currentUser.getId().equals(selectedUser.getId())
                && !UserRoles.ROLE_ADMIN.equals(role)) {
            showValidation("You cannot remove your own admin access.");
            return;
        }

        try {
            if (userService.emailExists(email, selectedUser.getId())) {
                showValidation("Another user already uses this email address.");
                return;
            }

            selectedUser.setEmail(email);
            selectedUser.setPrenom(prenom);
            selectedUser.setNom(nom);
            selectedUser.setTelephone(telephone);
            selectedUser.setDateNaissance(dateNaissancePicker.getValue());
            selectedUser.setRoleList(java.util.List.of(role));
            selectedUser.setStatut(status);
            selectedUser.setPhoto(photo);
            selectedUser.setCvName(cvName);
            selectedUser.setUpdatedAt(LocalDateTime.now());
            if (selectedUser.getDateInscription() == null) {
                selectedUser.setDateInscription(LocalDateTime.now());
            }
            if (newPassword != null && !newPassword.isBlank()) {
                selectedUser.setPassword(newPassword);
            }

            userService.update(selectedUser);
            refreshUsers(selectedUser.getId());
            showStatus("User profile updated successfully.", "status-success");
        } catch (SQLException | IllegalArgumentException ex) {
            showValidation("The user could not be updated. " + ex.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        hideValidation();

        if (selectedUser == null) {
            showValidation("Select a user before deleting.");
            return;
        }
        if (userService == null) {
            showValidation("The user moderation service is not available.");
            return;
        }

        User currentUser = AuthSession.getCurrentUser();
        if (currentUser != null
                && currentUser.getId() != null
                && currentUser.getId().equals(selectedUser.getId())) {
            showValidation("You cannot delete the account you are currently using.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete user");
        alert.setHeaderText("Delete " + selectedUser.getDisplayName() + "?");
        alert.setContentText("This removes the user account from the local Sport Insight database.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            userService.delete(selectedUser.getId());
            clearForm();
            refreshUsers(null);
            showStatus("User deleted successfully.", "status-success");
        } catch (SQLException ex) {
            showValidation("The user could not be deleted. " + ex.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
        showStatus("Selection cleared.", "status-muted");
    }

    private void configureInputs() {
        roleComboBox.setItems(FXCollections.observableArrayList(UserRoles.allowedRoles()));
        statusComboBox.setItems(FXCollections.observableArrayList(
                "ACTIVE",
                "INACTIVE",
                "BLOCKED",
                "PENDING",
                "ACTIF",
                "INACTIF"
        ));
        statusComboBox.setEditable(true);
        if (dateNaissancePicker != null) {
            dateNaissancePicker.setEditable(false);
        }
        clearForm();
        hideValidation();
    }

    private void configureTable() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDisplayName()));
        emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getEmail())));
        roleColumn.setCellValueFactory(cell -> new SimpleStringProperty(UserRoles.displayName(cell.getValue().getPrimaryRole())));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(emptyIfNull(cell.getValue().getStatut())));
        createdColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDateTime(cell.getValue().getDateInscription())));

        userTableView.setItems(filteredUsers);
        userTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userTableView.setPlaceholder(new Label("No users found."));
    }

    private void bindUi() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        userTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> populateForm(newValue));
    }

    private void refreshUsers(Integer preferredUserId) {
        if (userService == null) {
            return;
        }

        try {
            users.setAll(userService.getAll().stream()
                    .sorted(Comparator
                            .comparing(User::getDateInscription, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(User::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList());
            applyFilter();
            updateMetrics();
            if (preferredUserId != null) {
                selectUserById(preferredUserId);
            }
            showStatus("User moderation data loaded.", "status-muted");
        } catch (SQLException ex) {
            showStatus("User moderation data could not be loaded.", "status-error");
        }
    }

    private void applyFilter() {
        String query = normalize(searchField.getText());
        filteredUsers.setPredicate(user -> {
            if (query == null || query.isBlank()) {
                return true;
            }
            return normalize(user.getDisplayName()).contains(query)
                    || normalize(user.getEmail()).contains(query)
                    || normalize(user.getPrimaryRole()).contains(query)
                    || normalize(user.getStatut()).contains(query);
        });
        resultsMetaLabel.setText(filteredUsers.size() + " user(s)");
    }

    private void updateMetrics() {
        long adminCount = users.stream().filter(User::isAdmin).count();
        long inactiveCount = users.stream().filter(user -> !user.isActiveAccount()).count();

        totalUsersLabel.setText(String.valueOf(users.size()));
        adminUsersLabel.setText(String.valueOf(adminCount));
        inactiveUsersLabel.setText(String.valueOf(inactiveCount));
    }

    private void populateForm(User user) {
        selectedUser = user;
        hideValidation();

        if (user == null) {
            selectionStateLabel.setText("Select a user");
            emailField.clear();
            prenomField.clear();
            nomField.clear();
            telephoneField.clear();
            dateNaissancePicker.setValue(null);
            roleComboBox.setValue(UserRoles.ROLE_USER);
            statusComboBox.setValue("ACTIVE");
            statusComboBox.getEditor().setText("ACTIVE");
            photoField.clear();
            cvField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
            deleteButton.setDisable(true);
            return;
        }

        selectionStateLabel.setText("Editing #" + user.getId());
        emailField.setText(emptyIfNull(user.getEmail()));
        prenomField.setText(emptyIfNull(user.getPrenom()));
        nomField.setText(emptyIfNull(user.getNom()));
        telephoneField.setText(emptyIfNull(user.getTelephone()));
        dateNaissancePicker.setValue(user.getDateNaissance());
        roleComboBox.setValue(UserRoles.coerceSingleRole(user.getPrimaryRole()));
        statusComboBox.setValue(emptyIfNull(user.getStatut(), "ACTIVE"));
        statusComboBox.getEditor().setText(emptyIfNull(user.getStatut(), "ACTIVE"));
        photoField.setText(emptyIfNull(user.getPhoto()));
        cvField.setText(emptyIfNull(user.getCvName()));
        passwordField.clear();
        confirmPasswordField.clear();
        deleteButton.setDisable(false);
    }

    private void clearForm() {
        selectedUser = null;
        if (userTableView != null) {
            userTableView.getSelectionModel().clearSelection();
        }
        populateForm(null);
    }

    private void selectUserById(Integer userId) {
        if (userId == null) {
            return;
        }
        for (User user : filteredUsers) {
            if (Objects.equals(userId, user.getId())) {
                userTableView.getSelectionModel().select(user);
                userTableView.scrollTo(user);
                return;
            }
        }
    }

    private void showStatus(String message, String styleClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-muted", "status-success", "status-warning", "status-error");
        if (!statusLabel.getStyleClass().contains("status-pill")) {
            statusLabel.getStyleClass().add("status-pill");
        }
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    private void showValidation(String message) {
        validationLabel.setText(message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }

    private void hideValidation() {
        validationLabel.setText("");
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String emptyIfNull(String value) {
        return emptyIfNull(value, "");
    }

    private String emptyIfNull(String value, String fallback) {
        return value == null ? fallback : value;
    }
}

package tn.esprit.gui;

import javafx.scene.Node;

/**
 * Opens the admin shell from any user (app) screen.
 */
public final class AdminNavigation {
    private AdminNavigation() {
    }

    public static void openAdmin(Node source) {
        if (source == null) {
            return;
        }
        SceneNavigator.switchScene(
                source,
                "/tn/esprit/views/admin-shell.fxml",
                "/tn/esprit/styles/admin-theme-fixed.css",
                "Sport Insight | Admin"
        );
    }
}

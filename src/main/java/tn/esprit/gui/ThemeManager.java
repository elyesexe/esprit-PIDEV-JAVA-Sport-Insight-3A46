package tn.esprit.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

public final class ThemeManager {
    private static final String PREF_THEME_MODE = "theme_mode";
    private static final String THEME_CLASS_LIGHT = "theme-light";
    private static final String THEME_CLASS_DARK = "theme-dark";
    private static final String POPUP_DARK_STYLESHEET = resolveStylesheet("/tn/esprit/styles/popup-dark.css");
    private static final String POPUP_LIGHT_STYLESHEET = resolveStylesheet("/tn/esprit/styles/popup-light.css");

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ThemeManager.class);
    private static final ObjectProperty<ThemeMode> CURRENT_MODE = new SimpleObjectProperty<>(loadPersistedMode());
    private static final Map<Scene, Boolean> REGISTERED_SCENES = new WeakHashMap<>();

    static {
        CURRENT_MODE.addListener((observable, oldMode, newMode) -> {
            persistMode(newMode);
            applyModeToRegisteredScenes();
        });
    }

    private ThemeManager() {
    }

    public static void registerScene(Scene scene) {
        if (scene == null) {
            return;
        }

        REGISTERED_SCENES.put(scene, Boolean.TRUE);
        applyMode(scene, CURRENT_MODE.get());
    }

    public static void bindToggle(ToggleButton toggleButton) {
        if (toggleButton == null) {
            return;
        }

        if (!toggleButton.getStyleClass().contains("theme-toggle")) {
            toggleButton.getStyleClass().add("theme-toggle");
        }

        boolean dark = isDarkMode();
        toggleButton.setSelected(dark);
        updateToggleAccessibility(toggleButton, dark);

        toggleButton.selectedProperty().addListener((observable, oldValue, selected) -> {
            ThemeMode requestedMode = selected ? ThemeMode.DARK : ThemeMode.LIGHT;
            if (CURRENT_MODE.get() != requestedMode) {
                setMode(requestedMode);
            } else {
                updateToggleAccessibility(toggleButton, selected);
            }
        });

        CURRENT_MODE.addListener((observable, oldMode, newMode) -> {
            boolean shouldSelect = newMode == ThemeMode.DARK;
            if (toggleButton.isSelected() != shouldSelect) {
                toggleButton.setSelected(shouldSelect);
            }
            updateToggleAccessibility(toggleButton, shouldSelect);
        });

        if (toggleButton.getScene() != null) {
            registerScene(toggleButton.getScene());
        }

        toggleButton.sceneProperty().addListener((observable, oldScene, newScene) -> registerScene(newScene));
    }

    public static void toggleMode() {
        setMode(isDarkMode() ? ThemeMode.LIGHT : ThemeMode.DARK);
    }

    public static void setMode(ThemeMode mode) {
        ThemeMode safeMode = mode == null ? ThemeMode.LIGHT : mode;
        CURRENT_MODE.set(safeMode);
    }

    public static ThemeMode getMode() {
        return CURRENT_MODE.get();
    }

    public static boolean isDarkMode() {
        return CURRENT_MODE.get() == ThemeMode.DARK;
    }

    private static void applyModeToRegisteredScenes() {
        ThemeMode currentMode = CURRENT_MODE.get();
        for (Scene scene : REGISTERED_SCENES.keySet()) {
            applyMode(scene, currentMode);
        }
    }

    private static void applyMode(Scene scene, ThemeMode mode) {
        Parent root = scene.getRoot();
        if (root == null) {
            return;
        }

        applyPopupStylesheet(scene, mode);
        root.getStyleClass().removeAll(THEME_CLASS_LIGHT, THEME_CLASS_DARK);
        root.getStyleClass().add(mode == ThemeMode.DARK ? THEME_CLASS_DARK : THEME_CLASS_LIGHT);
    }

    private static void applyPopupStylesheet(Scene scene, ThemeMode mode) {
        scene.getStylesheets().removeIf(stylesheet ->
                Objects.equals(stylesheet, POPUP_DARK_STYLESHEET) || Objects.equals(stylesheet, POPUP_LIGHT_STYLESHEET)
        );

        String targetStylesheet = mode == ThemeMode.DARK ? POPUP_DARK_STYLESHEET : POPUP_LIGHT_STYLESHEET;
        if (targetStylesheet != null && !scene.getStylesheets().contains(targetStylesheet)) {
            scene.getStylesheets().add(targetStylesheet);
        }
    }

    private static String resolveStylesheet(String path) {
        var url = ThemeManager.class.getResource(path);
        return url == null ? null : url.toExternalForm();
    }

    private static ThemeMode loadPersistedMode() {
        return ThemeMode.fromValue(PREFERENCES.get(PREF_THEME_MODE, ThemeMode.LIGHT.value));
    }

    private static void persistMode(ThemeMode mode) {
        PREFERENCES.put(PREF_THEME_MODE, mode.value);
    }

    private static void updateToggleAccessibility(ToggleButton toggleButton, boolean dark) {
        toggleButton.setAccessibleText(dark ? "Switch to light mode" : "Switch to dark mode");
    }

    public enum ThemeMode {
        LIGHT("light"),
        DARK("dark");

        private final String value;

        ThemeMode(String value) {
            this.value = value;
        }

        public static ThemeMode fromValue(String value) {
            if ("dark".equalsIgnoreCase(value)) {
                return DARK;
            }
            return LIGHT;
        }
    }
}

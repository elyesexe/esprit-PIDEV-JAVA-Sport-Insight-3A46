package tn.esprit.security;

import javafx.scene.control.Button;
import tn.esprit.entities.User;
import tn.esprit.gui.UserNavbarMenu;

import java.lang.reflect.Field;

public final class AuthSession {
    private static volatile User currentUser;

    private AuthSession() {
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static void logoutAndForget() {
        RememberMeService.forget();
        logout();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static void configureLoadedController(Object controller) {
        applyButtonVisibility(controller, "adminNavButton", isAdmin());
        UserNavbarMenu.configureLoadedController(controller);
    }

    private static void applyButtonVisibility(Object controller, String fieldName, boolean visible) {
        if (controller == null) {
            return;
        }

        Field field = findField(controller.getClass(), fieldName);
        if (field == null || !Button.class.isAssignableFrom(field.getType())) {
            return;
        }

        try {
            field.setAccessible(true);
            Object candidate = field.get(controller);
            if (candidate instanceof Button button) {
                button.setManaged(visible);
                button.setVisible(visible);
            }
        } catch (IllegalAccessException ignored) {
            // Best-effort session UI configuration.
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
}

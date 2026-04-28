package tn.esprit.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public final class I18n {
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static final String PREF_LANGUAGE = "language";
    private static final Locale DEFAULT_LOCALE = Locale.FRENCH;
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(I18n.class);

    private static volatile Locale currentLocale = loadSavedLocale();

    static {
        Locale.setDefault(currentLocale);
    }

    private I18n() {
    }

    public static Locale getLocale() {
        return currentLocale;
    }

    public static Locale getDefaultLocale() {
        return DEFAULT_LOCALE;
    }

    public static void setLocale(Locale locale) {
        Locale resolvedLocale = normalize(locale);
        currentLocale = resolvedLocale;
        Locale.setDefault(resolvedLocale);
        PREFERENCES.put(PREF_LANGUAGE, resolvedLocale.getLanguage());
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
    }

    public static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, normalize(locale));
    }

    public static boolean hasKey(String key) {
        try {
            return getBundle().containsKey(key);
        } catch (MissingResourceException e) {
            return false;
        }
    }

    public static String get(String key) {
        return getOrDefault(key, "!" + key + "!");
    }

    public static String getOrDefault(String key, String fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        try {
            ResourceBundle bundle = getBundle();
            return bundle.containsKey(key) ? bundle.getString(key) : fallback;
        } catch (MissingResourceException e) {
            return fallback;
        }
    }

    public static String format(String key, Object... args) {
        String pattern = get(key);
        MessageFormat formatter = new MessageFormat(pattern, currentLocale);
        return formatter.format(args == null ? new Object[0] : args);
    }

    public static String formatOrDefault(String key, String fallback, Object... args) {
        String pattern = getOrDefault(key, fallback);
        MessageFormat formatter = new MessageFormat(pattern, currentLocale);
        return formatter.format(args == null ? new Object[0] : args);
    }

    public static Locale normalize(Locale locale) {
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        return Locale.ENGLISH.getLanguage().equalsIgnoreCase(locale.getLanguage())
                ? Locale.ENGLISH
                : Locale.FRENCH;
    }

    private static Locale loadSavedLocale() {
        String savedLanguage = PREFERENCES.get(PREF_LANGUAGE, DEFAULT_LOCALE.getLanguage());
        return normalize(Locale.forLanguageTag(savedLanguage));
    }
}

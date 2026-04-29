package tn.esprit.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserRoles {
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_JOUEUR = "ROLE_JOUEUR";
    public static final String ROLE_ENTRAINEUR = "ROLE_ENTRAINEUR";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static final List<String> ALLOWED_ROLES = List.of(
            ROLE_USER,
            ROLE_JOUEUR,
            ROLE_ENTRAINEUR,
            ROLE_ADMIN
    );
    private static final List<String> PRIORITY = List.of(
            ROLE_ADMIN,
            ROLE_ENTRAINEUR,
            ROLE_JOUEUR,
            ROLE_USER
    );

    private UserRoles() {
    }

    public static List<String> allowedRoles() {
        return ALLOWED_ROLES;
    }

    public static List<String> parseRoles(String rawRoles) {
        if (rawRoles == null || rawRoles.isBlank()) {
            return List.of(ROLE_USER);
        }

        Set<String> roles = new LinkedHashSet<>();
        String sanitized = rawRoles
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('"', ' ')
                .replace('\'', ' ');

        for (String token : sanitized.split("[,;\\s]+")) {
            String normalized = token == null ? "" : token.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isBlank() && normalized.startsWith("ROLE_")) {
                roles.add(normalized);
            }
        }

        if (roles.isEmpty()) {
            roles.add(ROLE_USER);
        }
        return List.copyOf(roles);
    }

    public static String toDatabaseValue(Collection<String> roles) {
        List<String> normalizedRoles = new ArrayList<>();
        if (roles != null) {
            normalizedRoles.addAll(
                    roles.stream()
                            .filter(role -> role != null && !role.isBlank())
                            .map(role -> role.trim().toUpperCase(Locale.ROOT))
                            .filter(role -> role.startsWith("ROLE_"))
                            .distinct()
                            .toList()
            );
        }

        if (normalizedRoles.isEmpty()) {
            normalizedRoles = List.of(ROLE_USER);
        }

        return normalizedRoles.stream()
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static boolean hasRole(String rawRoles, String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return !normalizedRole.isBlank() && parseRoles(rawRoles).contains(normalizedRole);
    }

    public static String resolvePrimaryRole(String rawRoles) {
        List<String> parsedRoles = parseRoles(rawRoles);
        for (String candidate : PRIORITY) {
            if (parsedRoles.contains(candidate)) {
                return candidate;
            }
        }
        return parsedRoles.get(0);
    }

    public static String coerceSingleRole(String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ROLES.contains(normalizedRole) ? normalizedRole : ROLE_USER;
    }

    public static String displayName(String role) {
        String normalizedRole = coerceSingleRole(role);
        return switch (normalizedRole) {
            case ROLE_ADMIN -> "Admin";
            case ROLE_ENTRAINEUR -> "Coach";
            case ROLE_JOUEUR -> "Player";
            default -> "User";
        };
    }
}

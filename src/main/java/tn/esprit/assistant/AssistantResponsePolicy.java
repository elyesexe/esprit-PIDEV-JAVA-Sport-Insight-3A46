package tn.esprit.assistant;

public record AssistantResponsePolicy(
        boolean directAnswerFirst,
        boolean includeSecondaryDetail,
        boolean requiresConfirmation
) {
    public static AssistantResponsePolicy directFact() {
        return new AssistantResponsePolicy(true, false, false);
    }

    public static AssistantResponsePolicy directWithDetail() {
        return new AssistantResponsePolicy(true, true, false);
    }

    public static AssistantResponsePolicy riskyAction() {
        return new AssistantResponsePolicy(true, false, true);
    }

    public String compose(String directAnswer, String secondaryDetail) {
        String direct = clean(directAnswer);
        String detail = clean(secondaryDetail);
        if (direct.isBlank()) {
            return detail;
        }
        if (!includeSecondaryDetail || detail.isBlank()) {
            return direct;
        }
        return direct + " " + detail;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

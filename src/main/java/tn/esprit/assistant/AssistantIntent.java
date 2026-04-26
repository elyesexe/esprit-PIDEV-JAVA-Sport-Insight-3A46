package tn.esprit.assistant;

public record AssistantIntent(
        AssistantIntentType type,
        AssistantIntentTarget target,
        AssistantIntentScope scope,
        String subject,
        boolean followUp,
        AssistantResponsePolicy policy
) {
    public static AssistantIntent unknown() {
        return new AssistantIntent(
                AssistantIntentType.UNKNOWN,
                AssistantIntentTarget.NONE,
                AssistantIntentScope.DEFAULT,
                "",
                false,
                AssistantResponsePolicy.directFact()
        );
    }

    public boolean isUnknown() {
        return type == AssistantIntentType.UNKNOWN;
    }
}

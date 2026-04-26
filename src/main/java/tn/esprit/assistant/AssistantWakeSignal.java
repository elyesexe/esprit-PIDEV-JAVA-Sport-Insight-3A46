package tn.esprit.assistant;

public record AssistantWakeSignal(
        String phrase,
        double confidence
) {
    public static AssistantWakeSignal doubleClap(double confidence) {
        return new AssistantWakeSignal("Double clap", confidence);
    }
}

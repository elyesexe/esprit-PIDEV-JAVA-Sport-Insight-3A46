package tn.esprit.assistant;

public record VoiceCaptureUpdate(
        Type type,
        String text,
        double confidence
) {
    public enum Type {
        LISTENING,
        PARTIAL,
        FINAL
    }

    public static VoiceCaptureUpdate listening() {
        return new VoiceCaptureUpdate(Type.LISTENING, "", 0.0);
    }

    public static VoiceCaptureUpdate partial(String text, double confidence) {
        return new VoiceCaptureUpdate(Type.PARTIAL, text == null ? "" : text.trim(), confidence);
    }

    public static VoiceCaptureUpdate finalText(String text, double confidence) {
        return new VoiceCaptureUpdate(Type.FINAL, text == null ? "" : text.trim(), confidence);
    }
}

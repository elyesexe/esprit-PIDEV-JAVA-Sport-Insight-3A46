package tn.esprit.assistant;

public record VoiceCaptureResult(
        String transcript,
        double confidence,
        boolean clarificationNeeded,
        String clarificationPrompt,
        boolean refinedTranscript
) {
    public static VoiceCaptureResult empty() {
        return new VoiceCaptureResult("", 0.0, false, "", false);
    }
}

package tn.esprit.assistant;

final class AssistantClapPatternDetector {
    private static final long CLAP_COOLDOWN_MILLIS = 80;
    private static final long DOUBLE_CLAP_MIN_GAP_MILLIS = 500;
    private static final long DOUBLE_CLAP_MAX_GAP_MILLIS = 1_000;

    private double noiseFloorRms = 0.012;
    private double noiseFloorPeak = 0.035;
    private long lastClapAt = Long.MIN_VALUE;
    private int clapCount;

    boolean accept(double rmsLevel, double peakLevel, long timestampMillis) {
        noiseFloorRms = (noiseFloorRms * 0.985) + (Math.min(rmsLevel, 0.06) * 0.015);
        noiseFloorPeak = (noiseFloorPeak * 0.985) + (Math.min(peakLevel, 0.14) * 0.015);
        double rmsThreshold = Math.max(0.018, noiseFloorRms * 1.9);
        double peakThreshold = Math.max(0.12, noiseFloorPeak * 2.8);
        double transientRise = peakLevel - rmsLevel;
        double riseThreshold = Math.max(0.045, noiseFloorPeak * 1.15);
        double crestFactor = peakLevel / Math.max(rmsLevel, 0.008);
        boolean clapLike = peakLevel >= peakThreshold
                && transientRise >= riseThreshold
                && (rmsLevel >= rmsThreshold || crestFactor >= 2.3);

        if (!clapLike) {
            if (clapCount > 0 && lastClapAt != Long.MIN_VALUE && timestampMillis - lastClapAt > DOUBLE_CLAP_MAX_GAP_MILLIS) {
                clapCount = 0;
            }
            return false;
        }

        if (lastClapAt != Long.MIN_VALUE && timestampMillis - lastClapAt < CLAP_COOLDOWN_MILLIS) {
            return false;
        }

        if (clapCount == 0 || lastClapAt == Long.MIN_VALUE || timestampMillis - lastClapAt > DOUBLE_CLAP_MAX_GAP_MILLIS) {
            clapCount = 1;
            lastClapAt = timestampMillis;
            return false;
        }

        long gap = timestampMillis - lastClapAt;
        lastClapAt = timestampMillis;
        if (gap >= DOUBLE_CLAP_MIN_GAP_MILLIS && gap <= DOUBLE_CLAP_MAX_GAP_MILLIS) {
            clapCount = 0;
            return true;
        }

        clapCount = 1;
        return false;
    }
}

package dev.slopium.client;

public final class SlopiumStats {
    private SlopiumStats() {}
    public static long lightQueries, lightCacheHits, hiddenTicksSkipped, breakingOverlaysSkipped;
    public static long breakingOverlaysChecked;
    public static final FrameTimeStats FRAME_TIMES = new FrameTimeStats(16_384);
    public static int indexedSources;
    public static long indexBuildNanos;
    public static long eligibleTicksChecked, tickNotReady, tickVisibleOrExcluded, tickNear, tickSableProtected, hiddenTicksChecked;

    public static void reset() {
        lightQueries = lightCacheHits = hiddenTicksSkipped = breakingOverlaysSkipped = 0;
        breakingOverlaysChecked = 0;
        FRAME_TIMES.reset();
        indexedSources = 0;
        indexBuildNanos = 0;
        eligibleTicksChecked = tickNotReady = tickVisibleOrExcluded = tickNear = tickSableProtected = hiddenTicksChecked = 0;
    }
}


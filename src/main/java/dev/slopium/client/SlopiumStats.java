package dev.slopium.client;

public final class SlopiumStats {
    private SlopiumStats() {}
    public static long lightQueries, lightCacheHits, hiddenTicksSkipped, breakingOverlaysSkipped;
    public static int indexedSources;
    public static long indexBuildNanos;

    public static void reset() {
        lightQueries = lightCacheHits = hiddenTicksSkipped = breakingOverlaysSkipped = 0;
        indexedSources = 0;
        indexBuildNanos = 0;
    }
}


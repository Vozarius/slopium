package dev.slopium.client;

public final class SlopiumStats {
    private SlopiumStats() {}
    public static long lightQueries, lightCacheHits, hiddenTicksSkipped, breakingOverlaysSkipped;
    public static long breakingOverlaysChecked;
    public static final FrameTimeStats FRAME_TIMES = new FrameTimeStats(16_384);
    public static long blockEntityDrawsSkipped, flywheelDrawsSkipped, particleCallsSkipped, ambientPassesSkipped, frozenCreateTicks;
    public static long itemDrawsSkipped, itemCopiesSkipped, shadowsSkipped, sodiumPassesSkipped, xaeroMeasurementGuardsSkipped;
    public static long itemImpostorsRendered, particleBudgetSkipped, textureInterpolationsSkipped;
    public static final java.util.concurrent.atomic.LongAdder CREATE_VISUALS_SKIPPED = new java.util.concurrent.atomic.LongAdder();
    public static long particleLightHits, particleDrawsSkipped, factoryPathsSkipped, factoryBulbsSkipped, distanceCreateTicksSkipped;
    public static long wallParticlesSkipped, wallCreateDrawsSkipped, wallCreateTicksSkipped, wallFlywheelInstancesHidden, occlusionRays, occlusionCacheHits, occlusionBudgetMisses;
    public static long factoryItemPassesSkipped;
    public static long particleOcclusionChecks, particleOcclusionDeferred, particleOcclusionProtected;
    public static int indexedSources;
    public static long indexBuildNanos;
    public static long eligibleTicksChecked, tickNotReady, tickVisibleOrExcluded, tickNear, tickSableProtected, hiddenTicksChecked;

    public static void reset() {
        lightQueries = lightCacheHits = hiddenTicksSkipped = breakingOverlaysSkipped = 0;
        breakingOverlaysChecked = 0;
        blockEntityDrawsSkipped = flywheelDrawsSkipped = particleCallsSkipped = ambientPassesSkipped = frozenCreateTicks = 0;
        itemDrawsSkipped = itemCopiesSkipped = shadowsSkipped = sodiumPassesSkipped = xaeroMeasurementGuardsSkipped = 0;
        itemImpostorsRendered = particleBudgetSkipped = textureInterpolationsSkipped = 0;
        CREATE_VISUALS_SKIPPED.reset();
        particleLightHits = particleDrawsSkipped = factoryPathsSkipped = factoryBulbsSkipped = distanceCreateTicksSkipped = 0;
        wallParticlesSkipped = wallCreateDrawsSkipped = wallCreateTicksSkipped = wallFlywheelInstancesHidden = occlusionRays = occlusionCacheHits = occlusionBudgetMisses = 0;
        factoryItemPassesSkipped = 0;
        particleOcclusionChecks = particleOcclusionDeferred = particleOcclusionProtected = 0;
        FRAME_TIMES.reset();
        indexedSources = 0;
        indexBuildNanos = 0;
        eligibleTicksChecked = tickNotReady = tickVisibleOrExcluded = tickNear = tickSableProtected = hiddenTicksChecked = 0;
    }
}


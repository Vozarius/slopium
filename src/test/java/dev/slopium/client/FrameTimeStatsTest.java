package dev.slopium.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameTimeStatsTest {
    @Test void measuresIntervalsInsteadOfAveragingInstantaneousFps() {
        var stats = new FrameTimeStats(8);
        stats.frameStart(1_000_000, true);
        stats.frameStart(11_000_000, true);
        stats.frameStart(41_000_000, true);
        var result = stats.snapshot();
        assertEquals(2, result.samples());
        assertEquals(0.04, result.seconds(), 1e-9);
        assertEquals(50, result.averageFps(), 1e-9);
        assertEquals(10, result.medianMs());
        assertEquals(30, result.p95Ms());
        assertEquals(30, result.p99Ms());
        assertEquals(30, result.maxMs());
        assertEquals(50, result.overBudgetPercent());
    }

    @Test void pauseAndResetDoNotTurnTimeInMenusIntoAStall() {
        var stats = new FrameTimeStats(8);
        stats.frameStart(1_000_000, true);
        stats.frameStart(11_000_000, true);
        stats.frameStart(0, false);
        stats.frameStart(1_000_000_000, true);
        assertEquals(1, stats.snapshot().samples());
        stats.frameStart(1_020_000_000, true);
        assertEquals(20, stats.snapshot().maxMs());
        stats.reset();
        assertEquals(0, stats.snapshot().samples());
        stats.frameStart(2_000_000_000, true);
        assertEquals(0, stats.snapshot().samples());
    }

    @Test void retainsLatestIntervalsAndUsesNearestRankPercentiles() {
        var stats = new FrameTimeStats(100);
        long now = 1;
        stats.frameStart(now, true);
        stats.frameStart(now += 1_000_000_000, true);
        for (int ms = 1; ms <= 100; ms++) stats.frameStart(now += ms * 1_000_000L, true);
        var result = stats.snapshot();
        assertEquals(100, result.samples());
        assertEquals(50, result.medianMs());
        assertEquals(95, result.p95Ms());
        assertEquals(99, result.p99Ms());
        assertEquals(100, result.maxMs());
        assertEquals(84, result.overBudgetPercent());
        assertEquals(100 / 5.05, result.averageFps(), 1e-9);
    }
}

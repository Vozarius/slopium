package dev.slopium.client;

import java.util.Arrays;

/** Render-thread-only intervals between frame starts; bounded, allocation-free recording. */
public final class FrameTimeStats {
    private final long[] intervals;
    private int size, next;
    private long lastStart;
    private boolean hasStart;

    public FrameTimeStats(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        intervals = new long[capacity];
    }

    public void frameStart(long now, boolean recording) {
        if (!recording) {
            hasStart = false;
            return;
        }
        if (hasStart) {
            long elapsed = now - lastStart;
            if (elapsed > 0) {
                intervals[next] = elapsed;
                next = (next + 1) % intervals.length;
                if (size < intervals.length) size++;
            }
        }
        lastStart = now;
        hasStart = true;
    }

    public void reset() {
        size = next = 0;
        hasStart = false;
    }

    public Snapshot snapshot() {
        if (size == 0) return new Snapshot(0, 0, 0, 0, 0, 0, 0, 0);
        long[] sorted = Arrays.copyOf(intervals, size);
        Arrays.sort(sorted);
        double total = 0;
        int overBudget = 0;
        for (long interval : sorted) {
            total += interval;
            if (interval > 16_670_000L) overBudget++;
        }
        return new Snapshot(size, total / 1e9, size * 1e9 / total,
            percentile(sorted, 0.5), percentile(sorted, 0.95), percentile(sorted, 0.99),
            sorted[size - 1] / 1e6, overBudget * 100.0 / size);
    }

    private static double percentile(long[] sorted, double rank) {
        return sorted[(int) Math.ceil(sorted.length * rank) - 1] / 1e6;
    }

    public record Snapshot(int samples, double seconds, double averageFps, double medianMs,
                           double p95Ms, double p99Ms, double maxMs, double overBudgetPercent) {}
}

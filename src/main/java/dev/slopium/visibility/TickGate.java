package dev.slopium.visibility;

/** Per-object bounded state. Scheduling spreads wakeups over different ticks. */
public final class TickGate {
    private long hiddenSince = Long.MIN_VALUE;
    private long wakeUntil = Long.MIN_VALUE;

    public void wake(long tick, int grace) {
        hiddenSince = Long.MIN_VALUE;
        wakeUntil = tick + grace;
    }

    public boolean skip(long tick, boolean eligibleAndHidden, int grace, int interval, int phase) {
        if (!eligibleAndHidden || tick < wakeUntil) {
            hiddenSince = Long.MIN_VALUE;
            return false;
        }
        if (hiddenSince == Long.MIN_VALUE || tick < hiddenSince) hiddenSince = tick;
        if (tick - hiddenSince < grace || interval <= 1) return false;
        return Math.floorMod(tick + phase, interval) != 0;
    }
}


package dev.slopium.visibility;

/** Per visual, bounded interval. Always refresh on first use, reentry and time reset. */
public final class VisualFrameGate {
    private long last = Long.MIN_VALUE;
    public boolean skip(long frame, boolean distant, int interval) {
        if (!distant || last == Long.MIN_VALUE || frame < last || frame - last >= Math.max(1, interval)) {
            last = frame;
            return false;
        }
        return true;
    }
}

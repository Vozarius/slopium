package dev.slopium.visibility;

/** Counts accepted requests, not live particles. Reset once per ParticleEngine tick. */
public final class EmissionBudget {
    private int used;
    public void reset() { used = 0; }
    public boolean allow(int limit) {
        if (used >= Math.max(0, limit)) return false;
        used++;
        return true;
    }
}

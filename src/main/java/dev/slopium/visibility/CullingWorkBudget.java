package dev.slopium.visibility;

import java.util.function.LongSupplier;

/** Cooperative total-work budget. A single operation may overrun; no busy waiting. */
public final class CullingWorkBudget {
    private final LongSupplier clock;
    private final long allowance;
    private final int limit;
    private long frame = Long.MIN_VALUE, spent, started, deadline;
    private int used;
    public CullingWorkBudget(long allowance, int limit, LongSupplier clock) {
        this.allowance=allowance; this.limit=limit; this.clock=clock;
    }
    public void begin(long now) {
        if (frame!=now) { frame=now; spent=0; used=0; }
        started=clock.getAsLong(); deadline=started+Math.max(0,allowance-spent);
    }
    public boolean take() {
        if (used>=limit || clock.getAsLong()>=deadline) return false;
        used++; return true;
    }
    public long deadline() { return deadline; }
    public void end() { spent+=Math.max(0,clock.getAsLong()-started); }
}

package dev.slopium.visibility;

public interface TrackedBlockEntity {
    TickGate slopium$tickGate();
    boolean slopium$allowsProcessing();
    VisibilityPool.Handle slopium$visibility();
    void slopium$resetHiddenState();
}


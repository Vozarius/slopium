package dev.slopium.visibility;
public interface VisualOcclusionState {
    void slopium$setOccluded(boolean hidden, long frame);
    void slopium$setOccludedUntil(boolean hidden, long frame, long until);
    boolean slopium$isOccluded(long frame);
}

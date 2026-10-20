package dev.slopium.visibility;
public interface VisualOcclusionState {
    void slopium$setOccluded(boolean hidden, long frame);
    boolean slopium$isOccluded(long frame);
}

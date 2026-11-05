package dev.slopium.visibility;

/** Metadata lives on the handle, so transfer checks need no global instance map. */
public interface InstanceOwnership {
    OwnedInstances slopium$owner();
    void slopium$owner(OwnedInstances owner);
    boolean slopium$hidden();
    void slopium$hidden(boolean hidden);
}

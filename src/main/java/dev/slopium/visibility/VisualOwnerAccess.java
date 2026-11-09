package dev.slopium.visibility;
/** Lifecycle-bound metadata; avoids a shared map lookup for ordinary Flywheel visuals. */
public interface VisualOwnerAccess {
    OwnedInstances slopium$owner();
    void slopium$owner(OwnedInstances owner);
}

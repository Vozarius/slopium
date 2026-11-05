package dev.slopium.mixin;

import dev.slopium.visibility.InstanceOwnership;
import dev.slopium.visibility.OwnedInstances;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets="dev.engine_room.flywheel.backend.engine.InstanceHandleImpl", remap=false)
public abstract class InstanceOwnershipMixin implements InstanceOwnership {
    @Unique private volatile OwnedInstances slopium$owner;
    @Unique private volatile boolean slopium$hidden;
    public OwnedInstances slopium$owner() { return slopium$owner; }
    public void slopium$owner(OwnedInstances owner) { slopium$owner=owner; }
    public boolean slopium$hidden() { return slopium$hidden; }
    public void slopium$hidden(boolean hidden) { slopium$hidden=hidden; }
}

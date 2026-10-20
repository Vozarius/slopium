package dev.slopium.mixin;

import dev.engine_room.flywheel.api.visual.Visual;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import dev.slopium.client.*;
import dev.slopium.visibility.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(targets = "dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl", remap = false)
public abstract class FlywheelWallMixin {
    @Shadow @Final private VisualManagerImpl<BlockEntity, BlockEntityStorage> blockEntities;
    @Unique private final Map<Visual, InstanceVisibility> slopium$hidden = new IdentityHashMap<>();
    @Unique private static final Set<String> slopium$audited = Set.of(
        "com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual",
        "com.simibubi.create.content.kinetics.base.ShaftVisual",
        "com.simibubi.create.content.kinetics.press.PressVisual",
        "com.simibubi.create.content.kinetics.mixer.MixerVisual",
        "com.simibubi.create.content.logistics.funnel.FunnelVisual");

    // Tick workers have finished; frame workers and instance compaction have not started yet.
    @Inject(method = "beginFrame", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/impl/task/Flag;lower()V"))
    private void slopium$beforeFrame(CallbackInfo ci) {
        var c = SlopiumConfig.get();
        boolean active = c.enabled && (c.occludeFlywheel || c.occludeCreateAnimations);
        if (!active && slopium$hidden.isEmpty()) return;
        var visuals = blockEntities.getStorage().getAllVisuals();
        if (!slopium$hidden.isEmpty()) {
            Set<Visual> live = Collections.newSetFromMap(new IdentityHashMap<>());
            live.addAll(visuals);
            slopium$hidden.keySet().removeIf(v -> !live.contains(v)); // Removed visuals already deleted their instances.
        }
        for (Visual visual : visuals) {
            if (!(visual instanceof VisualBlockEntityAccess access) || !(visual instanceof VisualOcclusionState state)
                    || !slopium$audited.contains(visual.getClass().getName())) continue;
            var be = access.slopium$getBlockEntity();
            boolean hidden = c.enabled && c.occludeFlywheel && CreateWallControl.hidden(be, c.flywheelAnyBlock);
            boolean animationHidden = c.enabled && c.occludeCreateAnimations &&
                (c.occludeFlywheel && c.animationAnyBlock == c.flywheelAnyBlock ? hidden : CreateWallControl.hidden(be, c.animationAnyBlock));
            state.slopium$setOccluded(animationHidden, Slopium.frame());
            var previous = slopium$hidden.get(visual);
            if (hidden && c.occludeFlywheel) {
                if (previous == null) {
                    var visibility = new InstanceVisibility();
                    ((BlockEntityVisual<?>)visual).collectCrumblingInstances(visibility);
                    slopium$hidden.put(visual, visibility);
                    if (c.diagnostics) SlopiumStats.wallFlywheelInstancesHidden += visibility.size();
                }
            } else if (previous != null) { previous.restore(); slopium$hidden.remove(visual); }
        }
    }
}

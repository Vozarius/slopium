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
    @Unique private boolean slopium$hadPaused;
    @Unique private static final Set<String> slopium$audited = Set.of(
        "com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual",
        "com.simibubi.create.content.kinetics.base.ShaftVisual",
        "com.simibubi.create.content.kinetics.base.OrientedRotatingVisual",
        "com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityVisual$LargeCogVisual",
        "com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual",
        "com.simibubi.create.content.kinetics.gearbox.GearboxVisual",
        "com.simibubi.create.content.kinetics.fan.FanVisual",
        "com.simibubi.create.content.kinetics.transmission.SplitShaftVisual",
        "com.simibubi.create.content.kinetics.press.PressVisual",
        "com.simibubi.create.content.kinetics.mixer.MixerVisual",
        "com.simibubi.create.content.logistics.funnel.FunnelVisual");

    // Tick workers have finished; frame workers and instance compaction have not started yet.
    @Inject(method = "beginFrame", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/impl/task/Flag;lower()V"))
    private void slopium$beforeFrame(CallbackInfo ci) {
        var c = SlopiumConfig.get();
        boolean active = c.enabled && (c.occludeFlywheel || c.occludeCreateAnimations || c.freezeCreateAnimations);
        if (!active && slopium$hidden.isEmpty() && !slopium$hadPaused) return;
        long costStart = c.diagnostics ? System.nanoTime() : 0;
        boolean updateAnimations = slopium$hadPaused || (c.enabled && (c.freezeCreateAnimations || c.occludeCreateAnimations));
        var mc = net.minecraft.client.Minecraft.getInstance();
        float seconds = (((LevelRendererAccessor)mc.levelRenderer).slopium$ticks()
            + mc.getTimer().getGameTimeDeltaPartialTick(true)) / 20f;
        slopium$hadPaused = c.enabled && (c.freezeCreateAnimations || c.occludeCreateAnimations);
        var visuals = blockEntities.getStorage().getAllVisuals();
        if (!slopium$hidden.isEmpty()) {
            Set<Visual> live = Collections.newSetFromMap(new IdentityHashMap<>());
            live.addAll(visuals);
            slopium$hidden.keySet().removeIf(v -> !live.contains(v)); // Removed visuals already deleted their instances.
        }
        for (Visual stored : visuals) {
            Visual visual = slopium$unwrap(stored);
            if (c.diagnostics) {
                SlopiumStats.flywheelVisualsChecked++;
                if (stored instanceof WrappedVisualAccess) SlopiumStats.flywheelVisualsWrapped++;
            }
            if (!(visual instanceof VisualBlockEntityAccess access) || !(visual instanceof VisualOcclusionState state)
                    || !slopium$audited.contains(visual.getClass().getName())) {
                if (c.diagnostics) SlopiumStats.flywheelVisualsUnsupported++;
                continue;
            }
            var be = access.slopium$getBlockEntity();
            boolean hidden = c.enabled && c.occludeFlywheel && CreateWallControl.hidden(be, c.flywheelAnyBlock);
            boolean animationHidden = c.enabled && ((c.freezeCreateAnimations && be.getLevel() == mc.level && be.getLevel() != null
                && (!(be instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smart) || !smart.isVirtual())
                && !SableGuard.protects(be.getLevel(), be.getBlockPos())) || (c.occludeCreateAnimations &&
                (c.occludeFlywheel && c.animationAnyBlock == c.flywheelAnyBlock ? hidden : CreateWallControl.hidden(be, c.animationAnyBlock))));
            state.slopium$setOccluded(animationHidden, Slopium.frame());
            if (updateAnimations) ((BlockEntityVisual<?>)visual).collectCrumblingInstances(instance -> {
                if (instance instanceof PausedRotation rotation) rotation.slopium$pauseRotation(animationHidden, seconds);
            });
            var previous = slopium$hidden.get(stored);
            if (hidden) {
                if (previous == null) {
                    previous = new InstanceVisibility();
                    slopium$hidden.put(stored, previous);
                }
                // BnB can replace the delegate's rotating model while it is hidden.
                int newlyHidden = previous.hide((BlockEntityVisual<?>)visual);
                if (c.diagnostics) SlopiumStats.wallFlywheelInstancesHidden += newlyHidden;
            } else if (previous != null) { previous.restore(); slopium$hidden.remove(stored); }
        }
        if (c.diagnostics) {
            long elapsed = System.nanoTime() - costStart;
            SlopiumStats.flywheelCullPasses++;
            SlopiumStats.flywheelCullNanos += elapsed;
            SlopiumStats.flywheelCullMaxNanos = Math.max(SlopiumStats.flywheelCullMaxNanos, elapsed);
        }
    }

    @Unique private static Visual slopium$unwrap(Visual visual) {
        for (int depth = 0; depth < 8; depth++) {
            if (!(visual instanceof WrappedVisualAccess wrapper)) return visual;
            Visual delegate = wrapper.slopium$delegateVisual();
            if (delegate == visual || delegate == null) return null;
            visual = delegate;
        }
        return null; // Unknown/cyclic wrapper chains fail open.
    }
}

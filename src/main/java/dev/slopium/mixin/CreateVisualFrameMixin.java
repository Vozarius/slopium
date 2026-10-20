package dev.slopium.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.visibility.SableGuard;
import dev.slopium.visibility.VisualFrameGate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Audited beginFrame bodies only set visual transforms from absolute animation state.
// Do not extend this list to recycler/contraption visuals without an additional audit.
@Mixin(targets = {
    "com.simibubi.create.content.kinetics.press.PressVisual",
    "com.simibubi.create.content.kinetics.mixer.MixerVisual",
    "com.simibubi.create.content.logistics.funnel.FunnelVisual"
})
public abstract class CreateVisualFrameMixin {
    @Unique private static final java.util.Set<String> slopium$audited = java.util.Set.of(
        "com.simibubi.create.content.kinetics.press.PressVisual",
        "com.simibubi.create.content.kinetics.mixer.MixerVisual",
        "com.simibubi.create.content.logistics.funnel.FunnelVisual");
    @Unique private final VisualFrameGate slopium$visualGate = new VisualFrameGate();
    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true)
    private void slopium$visualFrame(DynamicVisual.Context context, CallbackInfo ci) {
        var c = SlopiumConfig.get();
        if (!c.enabled) return;
        if (c.occludeCreateAnimations && ((dev.slopium.visibility.VisualOcclusionState)this).slopium$isOccluded(Slopium.frame())) {
            if (c.diagnostics) SlopiumStats.CREATE_VISUALS_SKIPPED.increment();
            ci.cancel(); return;
        }
        if (!c.throttleCreateVisuals) return;
        BlockEntity blockEntity = ((VisualBlockEntityAccess) this).slopium$getBlockEntity();
        long frame = Slopium.frame();
        boolean distant = false;
        if (c.enabled && c.throttleCreateVisuals && blockEntity != null && slopium$audited.contains(getClass().getName())) {
            var level = blockEntity.getLevel();
            if (level != null && level.isClientSide && level == Minecraft.getInstance().level
                    && blockEntity instanceof SmartBlockEntity smart && !smart.isVirtual()) {
                var pos = blockEntity.getBlockPos();
                // Add four blocks to protect nearby extended models; this is distance LOD, not occlusion.
                double radius = c.createVisualDistance + 4.0;
                distant = pos.distToCenterSqr(context.camera().getPosition()) > radius * radius
                    && !SableGuard.protects(level, pos);
            }
        }
        if (slopium$visualGate.skip(frame, distant, c.createVisualInterval)) {
            if (c.diagnostics) SlopiumStats.CREATE_VISUALS_SKIPPED.increment();
            ci.cancel();
        }
    }
}

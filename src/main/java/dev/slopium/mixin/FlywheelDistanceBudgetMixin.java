package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses Flywheel's opt-in visual update limiter; never skips queue/resource processing. */
@Mixin(targets = "dev.engine_room.flywheel.impl.visual.NonLimiter")
public abstract class FlywheelDistanceBudgetMixin {
    @Unique private volatile long slopium$frame;
    @Inject(method = "tick", at = @At("HEAD"))
    private void slopium$advance(CallbackInfo ci) { slopium$frame++; }
    @Inject(method = "shouldUpdate", at = @At("HEAD"), cancellable = true)
    private void slopium$distance(double distanceSquared, CallbackInfoReturnable<Boolean> cir) {
        var c = SlopiumConfig.get();
        if (c.enabled && c.throttleFlywheelDistance
                && distanceSquared > (double) c.flywheelFullRateDistance * c.flywheelFullRateDistance) {
            // This target has no native throttling; its original method always returns true.
            cir.setReturnValue(slopium$frame % Math.clamp(c.flywheelFrameInterval, 2, 8) == 0);
        }
    }
}

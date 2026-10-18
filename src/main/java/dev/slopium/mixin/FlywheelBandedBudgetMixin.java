package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.slopium.client.SlopiumConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "dev.engine_room.flywheel.impl.visual.BandedPrimeLimiter")
public abstract class FlywheelBandedBudgetMixin {
    @ModifyReturnValue(method = "getUpdateDivisor", at = @At("RETURN"))
    private int slopium$divisor(int original, double distanceSquared) {
        var c = SlopiumConfig.get();
        if (c.enabled && c.throttleFlywheelDistance
                && distanceSquared > (double) c.flywheelFullRateDistance * c.flywheelFullRateDistance) {
            // A single schedule avoids multiplying periods and never increases native update frequency.
            return Math.max(original, Math.clamp(c.flywheelFrameInterval, 2, 8));
        }
        return original;
    }
}

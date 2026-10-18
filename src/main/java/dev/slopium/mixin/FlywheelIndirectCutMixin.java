package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.engine_room.flywheel.backend.engine.indirect.IndirectCullingGroup")
public abstract class FlywheelIndirectCutMixin {
    @Inject(method = {"submitSolid", "submitTransparent"}, at = @At("HEAD"), cancellable = true)
    private void slopium$cut(CallbackInfo ci) {
        var config = SlopiumConfig.get();
        if (config.enabled && config.disableFlywheelRendering) {
            if (config.diagnostics) SlopiumStats.flywheelDrawsSkipped++;
            ci.cancel();
        }
    }
}

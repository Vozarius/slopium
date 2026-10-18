package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel")
public abstract class AmbientCutMixin {
    @Inject(method = {"animateTick"}, at = @At("HEAD"), cancellable = true)
    private void slopium$cut(CallbackInfo ci) {
        var config = SlopiumConfig.get();
        if (config.enabled && config.disableAmbientEffects) {
            if (config.diagnostics) SlopiumStats.ambientPassesSkipped++;
            ci.cancel();
        }
    }
}

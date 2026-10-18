package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$InterpolationData")
public abstract class TextureInterpolationMixin {
    @Inject(method = "uploadInterpolatedFrame", at = @At("HEAD"), cancellable = true)
    private void slopium$discreteFrames(CallbackInfo ci) {
        var c = SlopiumConfig.get();
        if (c.enabled && c.disableTextureInterpolation) {
            if (c.diagnostics) SlopiumStats.textureInterpolationsSkipped++;
            ci.cancel();
        }
    }
}

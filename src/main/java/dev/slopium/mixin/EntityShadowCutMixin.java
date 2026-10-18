package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityShadowCutMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void slopium$shadow(CallbackInfo ci) {
        var c = SlopiumConfig.get();
        if (c.enabled && c.disableEntityShadows) {
            if (c.diagnostics) SlopiumStats.shadowsSkipped++;
            ci.cancel();
        }
    }
}

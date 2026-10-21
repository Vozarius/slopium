package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher")
public abstract class BlockEntityRenderCutMixin {
    @Inject(method = {"render"}, at = @At("HEAD"), cancellable = true)
    private void slopium$cut(CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.level.block.entity.BlockEntity be) {
        var config = SlopiumConfig.get();
        if (config.enabled && (config.disableBlockEntityRendering || (config.hideAllCreateModels
                && net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getNamespace().equals("create")))) {
            if (config.diagnostics) SlopiumStats.blockEntityDrawsSkipped++;
            ci.cancel();
        }
    }
}

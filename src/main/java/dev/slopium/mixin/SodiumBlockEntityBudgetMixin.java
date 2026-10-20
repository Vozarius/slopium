package dev.slopium.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.visibility.RenderBudget;
import dev.slopium.visibility.SableGuard;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer")
public abstract class SodiumBlockEntityBudgetMixin {
    @Inject(method = "renderBlockEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderBuffers;Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;Lnet/minecraft/client/Camera;FLcom/llamalad7/mixinextras/sugar/ref/LocalBooleanRef;)V",
        at = @At("HEAD"), cancellable = true)
    private void slopium$wholePass(CallbackInfo ci) {
        var c = SlopiumConfig.get();
        if (c.enabled && c.disableBlockEntityRendering) {
            if (c.diagnostics) SlopiumStats.sodiumPassesSkipped++;
            ci.cancel();
        }
    }

    @Inject(method = "renderBlockEntity", at = @At("HEAD"), cancellable = true)
    private static void slopium$distance(CallbackInfo ci, @Local(argsOnly = true) BlockEntity be,
            @Local(argsOnly = true, ordinal = 0) double x,
            @Local(argsOnly = true, ordinal = 1) double y,
            @Local(argsOnly = true, ordinal = 2) double z) {
        var c = SlopiumConfig.get();
        if (c.enabled && c.occludeCreateRendering && dev.slopium.client.Slopium.createAvailable()
                && dev.slopium.visibility.CreateWallControl.hidden(be, c.createRenderAnyBlock)) {
            if (c.diagnostics) SlopiumStats.wallCreateDrawsSkipped++;
            ci.cancel(); return;
        }
        if (!c.enabled || !c.limitBlockEntityRendering || be.getLevel() == null) return;
        if (SableGuard.protects(be.getLevel(), be.getBlockPos())) return;
        var renderer = net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        if (renderer == null) return;
        if (RenderBudget.outside(renderer.getRenderBoundingBox(be), x, y, z, RenderBudget.radius(c.blockEntityRenderDistance))) {
            if (c.diagnostics) SlopiumStats.blockEntityDrawsSkipped++;
            ci.cancel();
        }
    }
}

package dev.slopium.mixin;

import com.simibubi.create.content.logistics.factoryBoard.*;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.visibility.RenderBudget;
import dev.slopium.visibility.SableGuard;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FactoryPanelRenderer.class, remap = false)
public abstract class FactoryPanelBudgetMixin {
    @Unique private static boolean slopium$far(FactoryPanelBehaviour panel, int distance) {
        var client = Minecraft.getInstance();
        var level = panel.getWorld();
        if (level == null || !level.isClientSide || level != client.level || client.screen != null
                || panel.blockEntity.isVirtual() || SableGuard.protects(level, panel.blockEntity.getBlockPos())) return false;
        // Always retain nearby controls and the board currently targeted for interaction.
        var pos = panel.blockEntity.getBlockPos();
        if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getBlockPos().equals(pos)) return false;
        double radius = RenderBudget.radius(distance) + 2.0;
        return pos.distToCenterSqr(client.gameRenderer.getMainCamera().getPosition()) > radius * radius;
    }
    @Inject(method = "renderPath", at = @At("HEAD"), cancellable = true)
    private static void slopium$connection(FactoryPanelBehaviour panel, FactoryPanelConnection connection,
            float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        var c = SlopiumConfig.get();
        if (!c.enabled || !c.factoryConnectionRange || !slopium$far(panel, c.factoryConnectionDistance)) return;
        // Retain a connection if either endpoint is nearby. This is an explicit detail range, not occlusion.
        double radius = RenderBudget.radius(c.factoryConnectionDistance) + 2.0;
        if (connection.from.pos().distToCenterSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) <= radius * radius) return;
        if (c.diagnostics) SlopiumStats.factoryPathsSkipped++;
        ci.cancel();
    }
    @Inject(method = "renderBulb", at = @At("HEAD"), cancellable = true)
    private static void slopium$bulb(FactoryPanelBehaviour panel, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
            int light, int overlay, CallbackInfo ci) {
        var c = SlopiumConfig.get();
        if (c.enabled && c.factoryBulbRange && slopium$far(panel, c.factoryBulbDistance)) {
            if (c.diagnostics) SlopiumStats.factoryBulbsSkipped++;
            ci.cancel();
        }
    }
}

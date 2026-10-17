package dev.slopium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.visibility.SableGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public abstract class BreakingOverlayMixin {
    // The five-argument vanilla overload delegates here. LevelRenderer calls this
    // NeoForge overload directly, so injecting only the vanilla method misses it.
    @Inject(method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/neoforged/neoforge/client/model/data/ModelData;)V",
        at = @At("HEAD"), cancellable = true)
    private void slopium$cullCracks(BlockState state, BlockPos pos, BlockAndTintGetter world,
                                    PoseStack pose, VertexConsumer consumer, ModelData modelData, CallbackInfo ci) {
        var config = SlopiumConfig.get();
        if (!config.enabled || !config.cullBreakingOverlays) return;
        if (config.diagnostics) SlopiumStats.breakingOverlaysChecked++;
        var client = Minecraft.getInstance();
        if (client.level == null || world != client.level) return;
        var frustum = ((LevelRendererAccessor) client.levelRenderer).slopium$frustum();
        // Extra margin protects models extending outside their owning block.
        if (frustum != null && !frustum.isVisible(new AABB(
                pos.getX() - 2.0, pos.getY() - 2.0, pos.getZ() - 2.0,
                pos.getX() + 3.0, pos.getY() + 3.0, pos.getZ() + 3.0))
                && !SableGuard.protects(client.level, pos)) {
            if (config.diagnostics) SlopiumStats.breakingOverlaysSkipped++;
            ci.cancel();
        }
    }
}

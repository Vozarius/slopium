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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public abstract class BreakingOverlayMixin {
    @Inject(method = "renderBreakingTexture", at = @At("HEAD"), cancellable = true)
    private void slopium$cullCracks(BlockState state, BlockPos pos, BlockAndTintGetter world,
                                    PoseStack pose, VertexConsumer consumer, CallbackInfo ci) {
        var config = SlopiumConfig.get();
        if (!config.enabled || !config.cullBreakingOverlays) return;
        var client = Minecraft.getInstance();
        if (client.level == null || world != client.level || SableGuard.protects(client.level, pos)) return;
        var frustum = ((LevelRendererAccessor) client.levelRenderer).slopium$frustum();
        // Extra margin protects models extending outside their owning block.
        if (frustum != null && !frustum.isVisible(new AABB(pos).inflate(2))) {
            if (config.diagnostics) SlopiumStats.breakingOverlaysSkipped++;
            ci.cancel();
        }
    }
}

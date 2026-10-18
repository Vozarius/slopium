package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.visibility.RenderBudget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemRenderBudgetMixin {
    // Scope strictly to world ItemEntity rendering, including recursive/custom renderers.
    @Unique private static final ThreadLocal<Boolean> slopium$single = ThreadLocal.withInitial(() -> false);
    @WrapMethod(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void slopium$worldItem(ItemEntity entity, float yaw, float partial, PoseStack pose,
                                  MultiBufferSource buffers, int light, Operation<Void> original) {
        var c = SlopiumConfig.get();
        if (!c.enabled || (!c.itemStackLod && !c.limitItemRendering)) {
            original.call(entity, yaw, partial, pose, buffers, light);
            return;
        }
        boolean single = false;
        if (c.enabled && (c.itemStackLod || c.limitItemRendering)) {
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            var bounds = entity.getBoundingBox();
            if (c.limitItemRendering && RenderBudget.outside(bounds, camera.x, camera.y, camera.z, RenderBudget.radius(c.itemRenderDistance))) {
                if (c.diagnostics) SlopiumStats.itemDrawsSkipped++;
                return;
            }
            single = c.itemStackLod && RenderBudget.outside(bounds, camera.x, camera.y, camera.z, RenderBudget.radius(c.itemLodDistance));
        }
        boolean previous = slopium$single.get();
        slopium$single.set(single);
        try { original.call(entity, yaw, partial, pose, buffers, light); }
        finally { slopium$single.set(previous); }
    }

    @ModifyExpressionValue(method = "renderMultipleFromCount(Lnet/minecraft/client/renderer/entity/ItemRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/resources/model/BakedModel;ZLnet/minecraft/util/RandomSource;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemEntityRenderer;getRenderedAmount(I)I"))
    private static int slopium$copies(int original) {
        if (!slopium$single.get() || original <= 1) return original;
        if (SlopiumConfig.get().diagnostics) SlopiumStats.itemCopiesSkipped += original - 1;
        return 1;
    }
}

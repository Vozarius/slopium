package dev.slopium.mixin;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import dev.slopium.client.*;
import dev.slopium.visibility.FactoryDetailControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FilteringRenderer.class, remap = false)
public abstract class FactoryItemBudgetMixin {
    @Inject(method = "renderOnBlockEntity", at = @At("HEAD"), cancellable = true)
    private static void slopium$factoryItems(SmartBlockEntity be, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
            int light, int overlay, CallbackInfo ci) {
        var c = SlopiumConfig.get();
        if (c.enabled && c.factoryItemRange && be instanceof FactoryPanelBlockEntity panel
                && FactoryDetailControl.far(panel,c.factoryItemDistance)) {
            if (c.diagnostics) SlopiumStats.factoryItemPassesSkipped++;
            ci.cancel();
        }
    }
}

package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="dev.engine_room.flywheel.backend.engine.indirect.DepthPyramid",remap=false)
public abstract class FlywheelDepthSourceMixin {
    @WrapOperation(method="generate",at=@At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget slopium$glassDepth(Minecraft minecraft,Operation<RenderTarget> original) {
        return dev.slopium.visibility.GpuGlassDepth.depthTarget(original.call(minecraft));
    }
}

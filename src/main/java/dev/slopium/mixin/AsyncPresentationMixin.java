package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.slopium.framegen.AsyncPresenter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class AsyncPresentationMixin {
    @Inject(method="runTick",at=@At("HEAD"))
    private void slopium$parallelBegin(boolean render,CallbackInfo ci){AsyncPresenter.beginFrame();}
    @WrapOperation(method="runTick",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V"))
    private void slopium$publish(RenderTarget target,int w,int h,Operation<Void> original){
        if(!AsyncPresenter.publish(target))original.call(target,w,h);
    }
}

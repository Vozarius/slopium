package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.slopium.framegen.FastFramePath;
import net.minecraft.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class FramePresentationMixin {
    @Unique private boolean slopium$measureFrame;
    @Unique private boolean slopium$skipEvents;
    @Inject(method="runTick",at=@At("HEAD"))
    private void slopium$beginLoop(boolean render,CallbackInfo ci){
        var mc=(Minecraft)(Object)this;
        slopium$measureFrame=mc.isGameLoadFinished()&&mc.level!=null;
        slopium$skipEvents=false;
        if(slopium$measureFrame)FastFramePath.loopStart();
    }
    @WrapOperation(method="runTick",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/client/ClientHooks;fireRenderFramePre(Lnet/minecraft/client/DeltaTracker;)V"))
    private void slopium$framePre(DeltaTracker delta,Operation<Void> original){
        slopium$skipEvents=slopium$measureFrame&&FastFramePath.begin(delta);
        if(!slopium$skipEvents)original.call(delta);
    }
    @WrapOperation(method="runTick",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/client/ClientHooks;fireRenderFramePost(Lnet/minecraft/client/DeltaTracker;)V"))
    private void slopium$framePost(DeltaTracker delta,Operation<Void> original){if(!slopium$skipEvents)original.call(delta);if(slopium$measureFrame)FastFramePath.end();}
    @WrapOperation(method="runTick",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/Window;updateDisplay()V"))
    private void slopium$swap(com.mojang.blaze3d.platform.Window window,Operation<Void> original){
        if(!slopium$measureFrame){original.call(window);return;}
        FastFramePath.swapStart();try{original.call(window);}finally{FastFramePath.swapEnd();}
    }
}

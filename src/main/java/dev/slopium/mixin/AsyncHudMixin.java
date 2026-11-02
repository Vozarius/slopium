package dev.slopium.mixin;
import dev.slopium.framegen.AsyncPresenter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class AsyncHudMixin {
    @Inject(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V"))
    private void slopium$beforeHand(DeltaTracker delta,CallbackInfo ci){AsyncPresenter.beginOverlay();}
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void slopium$withoutHand(DeltaTracker delta,CallbackInfo ci){AsyncPresenter.beginOverlay();}
}

package dev.slopium.mixin;
import dev.slopium.visibility.VisibilityPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class VisibilityFrameMixin {
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void slopium$visibilityFrame(net.minecraft.client.DeltaTracker delta,boolean outline,net.minecraft.client.Camera camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,net.minecraft.client.renderer.LightTexture light,
            org.joml.Matrix4f view,org.joml.Matrix4f projection,CallbackInfo ci) {
        VisibilityPool.captureCamera(view,projection,camera.getPosition());
        VisibilityPool.process(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }
}

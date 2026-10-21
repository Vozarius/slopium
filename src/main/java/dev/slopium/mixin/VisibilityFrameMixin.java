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
    private void slopium$visibilityFrame(CallbackInfo ci) {
        VisibilityPool.process(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }
}

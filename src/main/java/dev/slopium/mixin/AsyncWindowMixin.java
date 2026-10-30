package dev.slopium.mixin;
import com.mojang.blaze3d.platform.Window;
import dev.slopium.framegen.AsyncPresenter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Window.class)
public abstract class AsyncWindowMixin {
    @Inject(method="close",at=@At("HEAD"))
    private void slopium$stopPresenter(CallbackInfo ci){AsyncPresenter.shutdown();}
}

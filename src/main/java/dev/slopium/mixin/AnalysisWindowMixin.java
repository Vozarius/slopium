package dev.slopium.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(com.mojang.blaze3d.platform.Window.class)
public abstract class AnalysisWindowMixin {
    @Inject(method="close",at=@At("HEAD"))
    private void slopium$restore(CallbackInfo ci){dev.slopium.benchmark.PerformanceAnalysis.cancel("logout");}
}

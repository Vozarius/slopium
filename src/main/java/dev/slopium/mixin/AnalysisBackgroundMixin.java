package dev.slopium.mixin;

import dev.slopium.benchmark.PerformanceAnalysis;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Focus is virtual only during the benchmark; actual OS focus and input callbacks are preserved. */
@Mixin(Minecraft.class)
public abstract class AnalysisBackgroundMixin {
    @Shadow public boolean noRender;
    @Inject(method="isWindowActive",at=@At("HEAD"),cancellable=true)
    private void slopium$foreground(CallbackInfoReturnable<Boolean> ci){if(PerformanceAnalysis.running())ci.setReturnValue(true);}
    @Inject(method="runTick",at=@At("HEAD"))
    private void slopium$renderMinimized(boolean render,CallbackInfo ci){if(PerformanceAnalysis.running())noRender=false;}
}

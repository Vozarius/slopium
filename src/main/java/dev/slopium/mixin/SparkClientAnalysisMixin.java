package dev.slopium.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="me.lucko.spark.neoforge.plugin.NeoForgeClientSparkPlugin",remap=false)
public abstract class SparkClientAnalysisMixin {
    @Inject(method="enable",at=@At("RETURN"))
    private void slopium$attach(CallbackInfo ci){dev.slopium.benchmark.SparkProfiler.attach(this);}
}

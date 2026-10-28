package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
@Mixin(Minecraft.class)
public abstract class FrameGenerationLimitMixin {
    @ModifyReturnValue(method="getFramerateLimit",at=@At("RETURN"))
    private int slopium$outputLimit(int limit){return dev.slopium.framegen.FrameGeneration.limit(limit);}
    // Vanilla otherwise skips its pacer for every rate >= 260, including e.g. 120 input x4.
    @ModifyConstant(method="runTick",constant=@Constant(intValue=260))
    private int slopium$finiteHighLimit(int threshold){return dev.slopium.framegen.FrameGeneration.limiterThreshold();}
}

package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(Minecraft.class)
public abstract class FrameGenerationLimitMixin {
    @ModifyReturnValue(method="getFramerateLimit",at=@At("RETURN"))
    private int slopium$outputLimit(int limit){return dev.slopium.framegen.FrameGeneration.limit(limit);}
}

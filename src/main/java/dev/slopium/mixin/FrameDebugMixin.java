package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.slopium.framegen.FrameMetrics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(DebugScreenOverlay.class)
public abstract class FrameDebugMixin {
    @ModifyExpressionValue(method="getGameInformation",at=@At(value="FIELD",target="Lnet/minecraft/client/Minecraft;fpsString:Ljava/lang/String;"))
    private String slopium$frameRates(String original){return FrameMetrics.debugLine(original);}
    // Sodium Extra may cache the text after getGameInformation. Refresh only the FPS row
    // at the final consumer, leaving its expensive debug-data cache intact.
    @ModifyVariable(method="renderLines",at=@At("HEAD"),argsOnly=true)
    private java.util.List<String> slopium$currentCounters(java.util.List<String> lines){return FrameMetrics.refreshDebugLines(lines);}
}

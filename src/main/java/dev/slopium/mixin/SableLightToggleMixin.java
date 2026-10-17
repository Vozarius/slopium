package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import net.createmod.catnip.config.ConfigBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "top.leonx.dynlight.sodium.sable.SableSubLevelEventHandler", remap = false)
public abstract class SableLightToggleMixin {
    @Redirect(method = "onTick", at = @At(value = "INVOKE",
        target = "Lnet/createmod/catnip/config/ConfigBase$ConfigBool;get()Ljava/lang/Object;"))
    private static Object slopium$enabled(ConfigBase.ConfigBool original) {
        SlopiumConfig config = SlopiumConfig.get();
        // Both original flags are still respected. The handler clears sources and
        // stops scanning while disabled; on re-enable it performs a fresh scan.
        return original.get() && (!config.enabled || (config.createLight && config.sableLight));
    }
}

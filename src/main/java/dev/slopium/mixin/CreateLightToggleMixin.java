package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import net.createmod.catnip.config.ConfigBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Use each addon's existing enable/disable transition, including source cleanup. */
@Pseudo
@Mixin(targets = {
    "top.leonx.dynlight.sodium.ContraptionEntityEventHandler",
    "top.leonx.dynlight.lamb.ContraptionEntityEventHandler"
}, remap = false)
public abstract class CreateLightToggleMixin {
    @Redirect(method = {"onContraptionEntityJoin", "onTick", "onDynamicLightEnabledChanged"},
        at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/config/ConfigBase$ConfigBool;get()Ljava/lang/Object;"))
    private static Object slopium$enabled(ConfigBase.ConfigBool original) {
        SlopiumConfig config = SlopiumConfig.get();
        return original.get() && (!config.enabled || config.createLight);
    }
}

package dev.slopium.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import dev.slopium.client.SlopiumConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shader rotation needs no upload when the stress tint is already white. */
@Mixin(value=KineticBlockEntityVisual.class,remap=false)
public abstract class KineticCleanColorMixin {
    @Inject(method="applyOverstressEffect",at=@At("HEAD"),cancellable=true)
    private static void slopium$unchanged(KineticBlockEntity be,RotatingInstance[] instances,CallbackInfo ci){
        if(!SlopiumConfig.get().enabled||((KineticStressAccess)((KineticEffectsAccess)be).slopium$effects()).slopium$overStressedEffect()!=0)return;
        for(var instance:instances)if(instance.red!=(byte)255||instance.green!=(byte)255||instance.blue!=(byte)255)return;
        ci.cancel();
    }
}

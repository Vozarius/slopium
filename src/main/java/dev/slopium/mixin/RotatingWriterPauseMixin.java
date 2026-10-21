package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import dev.slopium.visibility.PausedRotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.simibubi.create.foundation.render.AllInstanceTypes", remap = false)
public abstract class RotatingWriterPauseMixin {
    @ModifyExpressionValue(method = "lambda$static$0", at = @At(value = "FIELD",
        target = "Lcom/simibubi/create/content/kinetics/base/RotatingInstance;rotationalSpeed:F"))
    private static float slopium$speed(float value, @Local(argsOnly = true) RotatingInstance instance) {
        return ((PausedRotation)instance).slopium$renderSpeed(value);
    }
    @ModifyExpressionValue(method = "lambda$static$0", at = @At(value = "FIELD",
        target = "Lcom/simibubi/create/content/kinetics/base/RotatingInstance;rotationOffset:F"))
    private static float slopium$offset(float value, @Local(argsOnly = true) RotatingInstance instance) {
        return ((PausedRotation)instance).slopium$renderOffset(value);
    }
}

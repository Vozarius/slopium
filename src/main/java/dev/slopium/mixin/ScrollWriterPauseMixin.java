package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.slopium.visibility.PausedScroll;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="com.simibubi.create.foundation.render.AllInstanceTypes",remap=false)
public abstract class ScrollWriterPauseMixin {
    @ModifyExpressionValue(method="lambda$static$1",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/ScrollInstance;speedU:F"))
    private static float slopium$speedU1(float value,@Local(argsOnly=true) com.simibubi.create.content.processing.burner.ScrollInstance instance){return ((PausedScroll)instance).slopium$renderSpeed(value);}
    @ModifyExpressionValue(method="lambda$static$1",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/ScrollInstance;speedV:F"))
    private static float slopium$speedV1(float value,@Local(argsOnly=true) com.simibubi.create.content.processing.burner.ScrollInstance instance){return ((PausedScroll)instance).slopium$renderSpeed(value);}
    @ModifyExpressionValue(method="lambda$static$1",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/ScrollInstance;offsetU:F"))
    private static float slopium$offsetU1(float value,@Local(argsOnly=true) com.simibubi.create.content.processing.burner.ScrollInstance instance){return ((PausedScroll)instance).slopium$scrollOffset(value,false);}
    @ModifyExpressionValue(method="lambda$static$1",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/ScrollInstance;offsetV:F"))
    private static float slopium$offsetV1(float value,@Local(argsOnly=true) com.simibubi.create.content.processing.burner.ScrollInstance instance){return ((PausedScroll)instance).slopium$scrollOffset(value,true);}
    @ModifyExpressionValue(method="lambda$static$2",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/ScrollTransformedInstance;speedU:F"))
    private static float slopium$speedU2(float value,@Local(argsOnly=true) com.simibubi.create.content.processing.burner.ScrollTransformedInstance instance){return ((PausedScroll)instance).slopium$renderSpeed(value);}
    @ModifyExpressionValue(method="lambda$static$2",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/ScrollTransformedInstance;speedV:F"))
    private static float slopium$speedV2(float value,@Local(argsOnly=true) com.simibubi.create.content.processing.burner.ScrollTransformedInstance instance){return ((PausedScroll)instance).slopium$renderSpeed(value);}
    @ModifyExpressionValue(method="lambda$static$2",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/ScrollTransformedInstance;offsetU:F"))
    private static float slopium$offsetU2(float value,@Local(argsOnly=true) com.simibubi.create.content.processing.burner.ScrollTransformedInstance instance){return ((PausedScroll)instance).slopium$scrollOffset(value,false);}
    @ModifyExpressionValue(method="lambda$static$2",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/ScrollTransformedInstance;offsetV:F"))
    private static float slopium$offsetV2(float value,@Local(argsOnly=true) com.simibubi.create.content.processing.burner.ScrollTransformedInstance instance){return ((PausedScroll)instance).slopium$scrollOffset(value,true);}
}

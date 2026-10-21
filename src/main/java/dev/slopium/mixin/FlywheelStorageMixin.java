package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.engine_room.flywheel.api.visual.Visual;
import dev.slopium.visibility.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="dev.engine_room.flywheel.impl.visualization.storage.Storage", remap=false)
public abstract class FlywheelStorageMixin implements CullingStorage {
    @Unique private final FlywheelCullingIndex slopium$index=new FlywheelCullingIndex();
    public FlywheelCullingIndex slopium$cullingIndex() { return slopium$index; }
    @Inject(method="setup", at=@At("TAIL"))
    private void slopium$add(Visual visual, float partialTick, CallbackInfo ci) { slopium$index.add(visual); }
    @WrapOperation(method={"remove", "lambda$recreateAll$4"}, at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/api/visual/Visual;delete()V"))
    private void slopium$remove(Visual visual, Operation<Void> original) {
        original.call(visual); slopium$index.remove(visual);
    }
    @Inject(method="invalidate", at=@At("TAIL"))
    private void slopium$clear(CallbackInfo ci) { slopium$index.clear(); }
}

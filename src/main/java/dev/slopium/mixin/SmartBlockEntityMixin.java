package dev.slopium.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.visibility.CreateState;
import dev.slopium.visibility.TrackedBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SmartBlockEntity.class, remap = false)
public abstract class SmartBlockEntityMixin implements CreateState {
    @Shadow private boolean initialized;
    @Override public boolean slopium$initialized() { return initialized; }

    @Inject(method = "read", at = @At("RETURN"))
    private void slopium$wakeOnUpdate(CallbackInfo ci) {
        var be = (SmartBlockEntity) (Object) this;
        if (be.getLevel() != null && be.getLevel().isClientSide) {
            ((TrackedBlockEntity) be).slopium$tickGate().wake(be.getLevel().getGameTime(), SlopiumConfig.get().hiddenGraceTicks);
        }
    }
}


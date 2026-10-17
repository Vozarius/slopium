package dev.slopium.mixin;

import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.visibility.CreateTickControl;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityMixin {
    @Shadow @Final private BlockEntity blockEntity;
    @Unique private boolean slopium$eligible;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void slopium$classifyOnce(CallbackInfo ci) {
        slopium$eligible = Slopium.createTickIntegrationAvailable() && CreateTickControl.isCandidate(blockEntity);
    }

    // Reuse the existing guard: no cancellable CallbackInfo on every BE tick.
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/entity/BlockEntity;hasLevel()Z"))
    private boolean slopium$allowClientTick(boolean hasLevel) {
        if (!hasLevel || !slopium$eligible) return hasLevel;
        SlopiumConfig config = SlopiumConfig.get();
        return !config.enabled || !config.throttleHiddenCreate || !CreateTickControl.skipEligible(blockEntity, config);
    }
}


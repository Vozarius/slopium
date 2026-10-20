package dev.slopium.mixin;

import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.visibility.CreateTickControl;
import dev.slopium.visibility.CreateFreezeControl;
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
    @Unique private boolean slopium$create;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void slopium$classifyOnce(CallbackInfo ci) {
        slopium$create = Slopium.createAvailable() && CreateFreezeControl.isCandidate(blockEntity);
        slopium$eligible = Slopium.createTickIntegrationAvailable() && CreateTickControl.isCandidate(blockEntity);
    }

    // Reuse the existing guard: no cancellable CallbackInfo on every BE tick.
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/entity/BlockEntity;hasLevel()Z"))
    private boolean slopium$allowClientTick(boolean hasLevel) {
        if (!hasLevel || (!slopium$eligible && !slopium$create)) return hasLevel;
        SlopiumConfig config = SlopiumConfig.get();
        if (!config.enabled) return true;
        if (slopium$create && config.freezeCreateClientTicks && CreateFreezeControl.skip(blockEntity, config.diagnostics)) return false;
        if (slopium$create && config.occludeCreateTicks) return !dev.slopium.visibility.CreateWallControl.skipTick(blockEntity, config);
        if (slopium$create && config.distanceCreateTicks) return !dev.slopium.visibility.CreateDistanceControl.skip(blockEntity, config);
        return !slopium$eligible || !config.throttleHiddenCreate || !CreateTickControl.skipEligible(blockEntity, config);
    }
}


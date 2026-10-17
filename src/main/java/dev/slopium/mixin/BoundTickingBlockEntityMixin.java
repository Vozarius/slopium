package dev.slopium.mixin;

import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.visibility.CreateTickControl;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityMixin {
    @Shadow @Final private BlockEntity blockEntity;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void slopium$skipHiddenClientTick(CallbackInfo ci) {
        SlopiumConfig config = SlopiumConfig.get();
        if (!config.enabled || !config.throttleHiddenCreate || blockEntity.getLevel() == null
                || !blockEntity.getLevel().isClientSide) return;
        if (Slopium.loaded("create") && Slopium.loaded("entityculling") && CreateTickControl.skip(blockEntity)) ci.cancel();
    }
}


package dev.slopium.mixin;

import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import dev.slopium.visibility.CullingStorage;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl",remap=false)
public abstract class FlywheelWallMixin {
    @Shadow @Final private VisualManagerImpl<BlockEntity,BlockEntityStorage> blockEntities;
    @Inject(method="beginFrame",at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/impl/task/Flag;lower()V"))
    private void slopium$beforeFrame(CallbackInfo ci) {
        ((CullingStorage)blockEntities.getStorage()).slopium$cullingIndex().frame();
    }
}

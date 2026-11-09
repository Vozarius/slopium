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
    @Unique private int slopium$tracking=dev.slopium.visibility.OwnedInstances.mode();
    @Shadow @Final private VisualManagerImpl<BlockEntity,BlockEntityStorage> blockEntities;
    @Inject(method="beginFrame",at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/impl/task/Flag;lower()V"))
    private void slopium$beforeFrame(dev.engine_room.flywheel.api.backend.RenderContext context,CallbackInfo ci) {
        var camera=context.camera().getPosition();
        dev.slopium.visibility.VisibilityPool.captureCamera(context.modelView(),context.projection(),camera);
        dev.slopium.visibility.VisibilityPool.process(camera);
        var engine=((dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl)(Object)this).getEngineImpl();
        int tracking=dev.slopium.visibility.OwnedInstances.mode();
        if(slopium$tracking!=tracking){
            // Tick workers have joined at this hook. Recreate only existing visuals, not world blocks.
            // This both restores hidden instances and supplies ownership when enabled in a live world.
            if(engine!=null)blockEntities.getStorage().recreateAll(engine.createVisualizationContext(),context.partialTick());
            slopium$tracking=tracking;
        }
        boolean nativeDepth=engine!=null && engine.drawManager() instanceof dev.engine_room.flywheel.backend.engine.indirect.IndirectDrawManager;
        ((CullingStorage)blockEntities.getStorage()).slopium$cullingIndex().frame(nativeDepth);
    }
}

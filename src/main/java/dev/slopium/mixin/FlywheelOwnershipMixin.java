package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.engine_room.flywheel.api.visual.*;
import dev.engine_room.flywheel.api.visualization.*;
import dev.slopium.visibility.OwnedInstances;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage",remap=false)
public abstract class FlywheelOwnershipMixin {
    @WrapOperation(method="createRaw(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Ldev/engine_room/flywheel/api/visual/BlockEntityVisual;",
        at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/api/visualization/BlockEntityVisualizer;createVisual(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Ldev/engine_room/flywheel/api/visual/BlockEntityVisual;"))
    private BlockEntityVisual<?> slopium$owner(BlockEntityVisualizer visualizer,VisualizationContext ctx,BlockEntity block,float partial,Operation<BlockEntityVisual<?>> original){
        int mode=OwnedInstances.mode();
        if(mode==0)return original.call(visualizer,ctx,block,partial);
        var owner=new OwnedInstances(block);var visual=original.call(visualizer,mode==2?owner.wrap(ctx):ctx,block,partial);OwnedInstances.register(visual,owner);return visual;
    }
}

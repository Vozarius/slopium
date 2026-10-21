package dev.slopium.visibility;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import net.minecraft.world.level.block.entity.BlockEntity;
final class FlywheelCullingProbe {
    static String describe(BlockEntity block) {
        var manager=VisualizationManagerImpl.get(block.getLevel());
        if(manager==null) return "Flywheel backend is not active";
        var storage=((VisualManagerImpl<?,?>)manager.blockEntities()).getStorage();
        return ((CullingStorage)storage).slopium$cullingIndex().describe(block);
    }
}

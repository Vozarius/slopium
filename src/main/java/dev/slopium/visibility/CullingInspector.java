package dev.slopium.visibility;

import dev.slopium.client.Slopium;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/** Explicit one-shot probe: no per-frame collection and no mutation of the culling cache. */
public final class CullingInspector {
    private CullingInspector() {}
    public static String describe(BlockPos pos) {
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level==null||!level.hasChunkAt(pos)) return "Target world/chunk is not loaded";
        var state=level.getBlockState(pos);var be=level.getBlockEntity(pos);
        String result="Target "+pos.toShortString()+" | "+BuiltInRegistries.BLOCK.getKey(state.getBlock())
            +" | BE="+(be==null?"none":be.getClass().getName());
        if(be!=null && Slopium.loaded("flywheel")) result+="\n"+FlywheelCullingProbe.describe(be);
        result+="\n"+VisibilityPool.describe(pos);
        Vec3 from=mc.gameRenderer.getMainCamera().getPosition(),to=Vec3.atCenterOf(pos);
        if(from.distanceToSqr(to)>128*128) return result+"\nOutside 128-block occlusion range";
        if(SableGuard.protects(level,pos)||SableGuard.protects(level,BlockPos.containing(from))) return result+"\nProtected Sable position";
        for(boolean any:new boolean[]{false,true}) {
            int[] steps={0};
            String obstacle=BlockGetter.traverseBlocks(from,to,any,(mode,cell)->{
                if(++steps[0]>512||!level.hasChunkAt(cell)) return "unknown/unloaded";
                if(cell.equals(pos)) return null;
                var block=level.getBlockState(cell);
                return OccluderShapes.blocks(block,level,cell,from,to,mode)?BuiltInRegistries.BLOCK.getKey(block.getBlock())+" @ "+cell.toShortString():null;
            },mode->"none");
            result+="\n"+(any?"Any blocks":"Opaque blocks")+": "+obstacle;
        }
        return result+"\nChat does not disable culling. Probe ignores aim protection; actual state is shown above. Static chunk geometry is not controlled by Flywheel hiding.";
    }
}

package dev.slopium.visibility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Use outline geometry, not a full collision cube requirement. Never fill holes in a shape. */
public final class OccluderShapes {
    private OccluderShapes() {}
    public static boolean blocks(BlockState state, BlockGetter world, BlockPos pos, Vec3 from, Vec3 to, boolean anyBlock) {
        if (state.isAir()) return false;
        if (state.isSolidRender(world,pos)) return true;
        if (!anyBlock && !state.canOcclude()) return false;
        var shape = state.getShape(world,pos,CollisionContext.empty());
        if (!shape.isEmpty() && shape.clip(from,to,pos) != null) return true;
        if (anyBlock) {
            var fluid = state.getFluidState();
            if (!fluid.isEmpty() && fluid.getShape(world,pos).clip(from,to,pos) != null) return true;
        }
        return false;
    }
}

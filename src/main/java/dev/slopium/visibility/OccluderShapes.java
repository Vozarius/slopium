package dev.slopium.visibility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Use outline geometry, not a full collision cube requirement. Never fill holes in a shape. */
public final class OccluderShapes {
    private OccluderShapes() {}
    public static net.minecraft.world.phys.shapes.VoxelShape volume(BlockState state,BlockGetter world,BlockPos pos,boolean any){
        if(state.isAir())return net.minecraft.world.phys.shapes.Shapes.empty();
        if(state.isSolidRender(world,pos))return net.minecraft.world.phys.shapes.Shapes.block();
        var shape=any||state.canOcclude()?state.getShape(world,pos,CollisionContext.empty()):net.minecraft.world.phys.shapes.Shapes.empty();
        if(any&&!state.getFluidState().isEmpty())shape=net.minecraft.world.phys.shapes.Shapes.or(shape,state.getFluidState().getShape(world,pos));
        return shape;
    }
    public static boolean blocks(BlockState state, BlockGetter world, BlockPos pos, Vec3 from, Vec3 to, boolean anyBlock) {
        return (mask(state,world,pos,from,to)&(anyBlock?2:1))!=0;
    }
    /** Bit 1: opaque obstruction; bit 2: any obstruction. Geometry is evaluated once. */
    public static int mask(BlockState state, BlockGetter world, BlockPos pos, Vec3 from, Vec3 to) {
        if(state.isAir()) return 0;
        if(state.isSolidRender(world,pos)) return 3;
        var shape=state.getShape(world,pos,CollisionContext.empty());
        if(!shape.isEmpty()&&shape.clip(from,to,pos)!=null) return state.canOcclude()?3:2;
        var fluid=state.getFluidState();
        return !fluid.isEmpty()&&fluid.getShape(world,pos).clip(from,to,pos)!=null?2:0;
    }
}

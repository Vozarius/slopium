package dev.slopium.smoke;
import dev.slopium.client.*;
import dev.slopium.visibility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
final class SmokeVisibilityReuseChecks {
    private static void step(Field frame,Vec3 eye) throws ReflectiveOperationException {
        frame.setLong(null,frame.getLong(null)+1);VisibilityPool.process(eye);
    }
    static void run(SmokeWallChecks.TestLevel level,Field frame) throws ReflectiveOperationException {
        var target=new BlockPos(5,0,0);var eye=new Vec3(-60,.5,.5);
        VisibilityPool.clear();level.wall=Blocks.STONE.defaultBlockState();WallOcclusion.invalidate();
        CreateOcclusion.hidden(target,eye,true);SmokeVisibilityPoolChecks.pump(frame,eye);
        if(!CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Initial long ray failed");
        long rays=SlopiumStats.visibilityRays,cells=SlopiumStats.visibilityCells,proofs=SlopiumStats.visibilityWitnessReuse;
        int[] referenceCells={0};
        for(int i=0;i<32;i++) {
            Vec3 moved=new Vec3(-60,.5+i*.0001,.5);
            net.minecraft.world.level.BlockGetter.traverseBlocks(moved,Vec3.atCenterOf(target),level,(w,pos)->{
                if(pos.equals(target))return null;
                referenceCells[0]++;
                return (OccluderShapes.mask(w.getBlockState(pos),w,pos,moved,Vec3.atCenterOf(target))&1)!=0?Boolean.TRUE:null;
            },w->Boolean.FALSE);
            CreateOcclusion.hidden(target,moved,true);step(frame,moved);
            if(!CreateOcclusion.hidden(target,moved,true))throw new AssertionError("Blocker proof lost hidden object");
        }
        long cellDelta=SlopiumStats.visibilityCells-cells;
        if(SlopiumStats.visibilityRays!=rays||cellDelta>32||SlopiumStats.visibilityWitnessReuse<=proofs)
            throw new AssertionError("Camera motion re-traversed the unchanged corridor");
        var crossed=new Vec3(12,.5,.5);CreateOcclusion.hidden(target,crossed,true);SmokeVisibilityPoolChecks.pump(frame,crossed);
        if(CreateOcclusion.hidden(target,crossed,true))throw new AssertionError("Witness outside the ray hid target");
        // Requesting only 'any' stops at glass; requesting opaque later must complete that answer.
        VisibilityPool.clear();level.wall=Blocks.GLASS.defaultBlockState();WallOcclusion.invalidate();
        eye=new Vec3(.5,.5,.5);
        CreateOcclusion.hidden(target,eye,true);SmokeVisibilityPoolChecks.pump(frame,eye);
        if(CreateOcclusion.decision(target,eye,false,true)!=null)throw new AssertionError("Opaque answer invented from glass-only proof");
        SmokeVisibilityPoolChecks.pump(frame,eye);
        if(CreateOcclusion.hidden(target,eye,false)||!CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Late second mode failed");
        rays=SlopiumStats.visibilityRays;long reuse=SlopiumStats.visibilityPathReuse;
        for(int i=0;i<8;i++) {
            VisibilityGeometry.invalidate(new BlockPos(1000,0,1000));
            CreateOcclusion.hidden(target,eye,true);step(frame,eye);
        }
        if(SlopiumStats.visibilityRays!=rays||SlopiumStats.visibilityPathReuse<=reuse)throw new AssertionError("Unrelated updates invalidated this path");
        level.wall=Blocks.AIR.defaultBlockState();VisibilityGeometry.invalidate(new BlockPos(4,0,0));
        CreateOcclusion.hidden(target,eye,true);SmokeVisibilityPoolChecks.pump(frame,eye);
        if(CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Local edit reused a stale path");
        level.wall=Blocks.STONE.defaultBlockState();VisibilityGeometry.chunk(0,0);
        CreateOcclusion.hidden(target,eye,true);SmokeVisibilityPoolChecks.pump(frame,eye);
        if(!CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Chunk change reused a stale path");
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: reuse: 32 camera samples used {} cell reads versus {} reference reads and zero new full rays; unrelated updates, late opaque mode, crossing/removing wall and chunk refresh passed",cellDelta,referenceCells[0]);
        VisibilityPool.clear();
    }
}

package dev.slopium.smoke;
import dev.slopium.client.*;
import dev.slopium.visibility.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;

final class SmokeVisibilityPoolChecks {
    static void pump(Field frame,Vec3 eye) throws ReflectiveOperationException {
        for(int i=0;i<8;i++){frame.setLong(null,frame.getLong(null)+1);VisibilityPool.process(eye);}
    }
    static void run(SmokeWallChecks.TestLevel level,Field frame) throws ReflectiveOperationException {
        VisibilityPool.clear();level.loaded=true;
        var eye=new Vec3(3.75,.5,.5);var target=new BlockPos(5,0,0);
        level.wall=Blocks.STONE.defaultBlockState();WallOcclusion.invalidate();
        long rays=SlopiumStats.visibilityRays;
        if(CreateOcclusion.decision(target,eye,false,true)!=null||CreateOcclusion.decision(target,eye,true,true)!=null)
            throw new AssertionError("Unsampled object has a result");
        if(rays!=SlopiumStats.visibilityRays)throw new AssertionError("A reader performed geometry tracing");
        pump(frame,eye);
        if(!CreateOcclusion.hidden(target,eye,false)||!CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Opaque wall did not classify both modes");
        if(SlopiumStats.visibilityRays!=rays+1)throw new AssertionError("Two modes performed separate traversals");
        level.wall=Blocks.GLASS.defaultBlockState();WallOcclusion.invalidate();
        CreateOcclusion.hidden(target,eye,true);pump(frame,eye);
        if(CreateOcclusion.hidden(target,eye,false)||!CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Glass did not separate modes");
        // No processor / exhausted frame budget: retain last answer, never oscillate to visible.
        rays=SlopiumStats.visibilityRays;
        for(int i=0;i<40;i++) {
            frame.setLong(null,frame.getLong(null)+1);
            if(!CreateOcclusion.hidden(target,new Vec3(3.75+i*.0001,.5,.5),true))throw new AssertionError("Deferred update revealed hidden target");
        }
        if(rays!=SlopiumStats.visibilityRays)throw new AssertionError("Deferred reader traced geometry");
        var crossed=new Vec3(6,.5,.5);CreateOcclusion.hidden(target,crossed,true);pump(frame,crossed);
        if(CreateOcclusion.hidden(target,crossed,true))throw new AssertionError("Fresh camera result failed to restore target");
        CreateOcclusion.hidden(target,eye,true);ParticleOcclusion.hidden(new Vec3(5.1,.5,.5),eye,true);pump(frame,eye);
        if(!CreateOcclusion.hidden(target,eye,true)||!ParticleOcclusion.hidden(new Vec3(5.1,.5,.5),eye,true))throw new AssertionError("Shared processor lost a consumer group");
        level.wall=Blocks.AIR.defaultBlockState();WallOcclusion.invalidate();CreateOcclusion.hidden(target,eye,true);pump(frame,eye);
        if(CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Geometry change never restored target");
        level.loaded=false;WallOcclusion.invalidate();CreateOcclusion.hidden(target,eye,true);pump(frame,eye);
        if(CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Unloaded geometry hidden");
        level.loaded=true;
        SmokeVisibilityReuseChecks.run(level,frame);
        // A full particle quota must not consume mechanism capacity or starve its queue.
        VisibilityPool.clear();level.wall=Blocks.GLASS.defaultBlockState();WallOcclusion.invalidate();
        for(int i=0;i<16384;i++) ParticleOcclusion.hidden(new Vec3(5+(i%64)/8.0,(i/64%64)/8.0,(i/4096)/8.0),eye,true);
        if(CreateOcclusion.decision(target,eye,true,true)!=null)throw new AssertionError("Particle quota consumed mechanism slot");
        pump(frame,eye);
        if(!CreateOcclusion.hidden(target,eye,true))throw new AssertionError("Particle flood starved mechanism");
        VisibilityPool.clear();
        if(java.util.Arrays.stream(net.minecraft.client.renderer.LevelRenderer.class.getDeclaredMethods()).noneMatch(m->m.getName().endsWith("slopium$visibilityFrame")))
            throw new AssertionError("Frame processor hook missing");
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: shared pool: zero reader rays, both modes in one traversal, stable deferred results, camera/geometry refresh, particles and frame hook passed");
    }
}

package dev.slopium.smoke;

import dev.slopium.client.*;
import dev.slopium.visibility.WallOcclusion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

final class SmokeWallChecks {
    static final class TestLevel extends ClientLevel {
        BlockState wall;
        boolean loaded;
        TestLevel() { super(null,null,null,null,0,0,null,null,false,0); }
        @Override public boolean hasChunkAt(BlockPos pos) { return loaded; }
        @Override public BlockState getBlockState(BlockPos pos) { return pos.getX()==4 ? wall : Blocks.AIR.defaultBlockState(); }
    }
    static void run() throws ReflectiveOperationException {
        var mc=Minecraft.getInstance(); var old=mc.level;
        var c=SlopiumConfig.get(); boolean diagnostics=c.diagnostics;
        var frame=Slopium.class.getDeclaredField("frame");frame.setAccessible(true);long oldFrame=frame.getLong(null);
        try {
            c.diagnostics=true;
            if (Slopium.loaded("flywheel")) SmokeFlywheelWallChecks.run();
            var level=(TestLevel)SmokeRadicalChecks.allocate(TestLevel.class);level.loaded=true;
            attachSable(level);
            mc.level=level;
            AABB box=new AABB(10.25,.25,.25,10.75,.75,.75);
            Vec3 camera=new Vec3(.5,.5,.5);
            level.wall=Blocks.STONE.defaultBlockState();
            next(frame);
            if (!WallOcclusion.hidden(box,camera,false)) throw new AssertionError("Opaque wall did not hide target");
            if (Slopium.loaded("create") && Slopium.loaded("flywheel")) {
                next(frame);SmokeFlywheelWallChecks.world();next(frame);
                if (!WallOcclusion.hidden(box,camera,false)) throw new AssertionError("Wall retest failed");
            }
            long rays=SlopiumStats.occlusionRays;
            if (!WallOcclusion.hidden(box,camera,false) || rays!=SlopiumStats.occlusionRays) throw new AssertionError("Shared wall cache failed");
            level.wall=Blocks.AIR.defaultBlockState();next(frame);
            if (WallOcclusion.hidden(box,camera,false)) throw new AssertionError("Open space hidden");
            level.wall=Blocks.GLASS.defaultBlockState();next(frame);
            if (WallOcclusion.hidden(box,camera,false)) throw new AssertionError("Default glass blocked");
            next(frame);
            if (!WallOcclusion.hidden(box,camera,true)) throw new AssertionError("Glass toggle failed");
            level.wall=Blocks.GLASS_PANE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH,true)
                .setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH,true);
            next(frame);
            if (WallOcclusion.hidden(box,camera,false)) throw new AssertionError("Pane blocked opaque mode");
            next(frame);
            if (!WallOcclusion.hidden(box,camera,true)) throw new AssertionError("Thin glass pane not occluding");
            level.wall=Blocks.IRON_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.EAST);
            next(frame);
            if (!WallOcclusion.hidden(box,camera,true)) throw new AssertionError("Closed door not occluding");
            level.wall=level.wall.setValue(net.minecraft.world.level.block.DoorBlock.OPEN,true);
            next(frame);
            if (WallOcclusion.hidden(box,camera,true)) throw new AssertionError("Open doorway was filled by cube test");
            level.wall=Blocks.OAK_SLAB.defaultBlockState();next(frame);
            Vec3 aboveFrom=new Vec3(.5,.8,.5), aboveTo=new Vec3(10.5,.8,.5);
            if (dev.slopium.visibility.OccluderShapes.blocks(level.wall,level,new BlockPos(4,0,0),aboveFrom,aboveTo,true)) throw new AssertionError("Empty half of slab occluded");
            Vec3 belowFrom=new Vec3(.5,.2,.5), belowTo=new Vec3(10.5,.2,.5);
            if (!dev.slopium.visibility.OccluderShapes.blocks(level.wall,level,new BlockPos(4,0,0),belowFrom,belowTo,true)) throw new AssertionError("Slab shape ignored");
            level.wall=Blocks.STONE.defaultBlockState();next(frame);
            if (WallOcclusion.hidden(box,new Vec3(10.5,.5,.5),false)) throw new AssertionError("Camera inside bounds hidden");
            next(frame);level.loaded=false;
            if (WallOcclusion.hidden(box,camera,false)) throw new AssertionError("Unknown chunk hidden");
            next(frame);level.loaded=true;
            var steps=WallOcclusion.class.getDeclaredField("steps");steps.setAccessible(true);
            WallOcclusion.hidden(new AABB(200,0,0,201,1,1),camera,false); // Start this frame's budget, distant bound fails open.
            steps.setInt(null,0);
            if (WallOcclusion.hidden(box,camera,false)) throw new AssertionError("Exhausted budget hid target");
            createQueue(level,frame);
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: wall/open/glass/pane/door/slab/camera/cache/unloaded/budget checks passed");
        } finally { mc.level=old; c.diagnostics=diagnostics; frame.setLong(null,oldFrame);WallOcclusion.invalidate();SlopiumStats.reset(); }
    }
    private static void createQueue(TestLevel level, java.lang.reflect.Field frame) throws ReflectiveOperationException {
        var eye = new Vec3(.5,.5,.5);
        var target = new BlockPos(5,0,0);
        var outside = new BlockPos(512,0,0);
        next(frame);
        dev.slopium.visibility.CreateOcclusion.hidden(outside,eye,true); // Initialize this frame/world only.
        var rays = dev.slopium.visibility.CreateOcclusion.class.getDeclaredField("rays"); rays.setAccessible(true); rays.setInt(null,0);
        if (dev.slopium.visibility.CreateOcclusion.hidden(target,eye,true)) throw new AssertionError("Unverified Create target hidden");
        level.wall=Blocks.AIR.defaultBlockState();next(frame); // Factory block update while the request is queued.
        dev.slopium.visibility.CreateOcclusion.hidden(outside,eye,true); // Must service the old request before any new one.
        long hits=SlopiumStats.createOcclusionCacheHits;
        if (dev.slopium.visibility.CreateOcclusion.hidden(target,eye,true) || SlopiumStats.createOcclusionCacheHits!=hits+1)
            throw new AssertionError("Geometry update lost the deferred Create request or reused old geometry");
        level.wall=Blocks.GLASS.defaultBlockState();next(frame);
        if (!dev.slopium.visibility.CreateOcclusion.hidden(target,eye,true)) throw new AssertionError("New window did not invalidate Create cache");
        if (dev.slopium.visibility.CreateOcclusion.hidden(target,new Vec3(6,.5,.5),true)) throw new AssertionError("Camera crossed glass but hidden result remained");
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: Create deferred queue survives geometry updates, uses current geometry, camera invalidation passed");
    }
    static void attachSable(ClientLevel level) throws ReflectiveOperationException {
        if (!Slopium.sableAvailable()) return;
        var type = Class.forName("dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer");
        Object container = type.getConstructor(net.minecraft.world.level.Level.class, int.class, int.class, int.class, int.class)
            .newInstance(level, 2, 7, 4096, 4096);
        var field = java.util.Arrays.stream(ClientLevel.class.getDeclaredFields())
            .filter(f -> f.getType().getName().equals("dev.ryanhcode.sable.api.sublevel.SubLevelContainer")).findFirst().orElseThrow();
        field.setAccessible(true); field.set(level, container);
        if (dev.slopium.visibility.SableGuard.protects(level, BlockPos.ZERO)) throw new AssertionError("Ready Sable container protected ordinary terrain");
        if (!dev.slopium.visibility.SableGuard.protects(level, new BlockPos(4096 << 11, 0, 4096 << 11))) throw new AssertionError("Sable plot protection lost");
    }
    private static void next(java.lang.reflect.Field frame) throws IllegalAccessException {
        frame.setLong(null,frame.getLong(null)+1);WallOcclusion.invalidate();
    }
}

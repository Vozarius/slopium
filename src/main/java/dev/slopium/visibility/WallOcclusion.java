package dev.slopium.visibility;

import dev.slopium.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;

/** Main-thread only. Shared cell cache, hard ray/voxel/time budgets; unknown means visible. */
public final class WallOcclusion {
    private record Key(int x0,int y0,int z0,int x1,int y1,int z1,boolean anyBlock) {
        AABB box() { return new AABB(x0/4.,y0/4.,z0/4.,x1/4.,y1/4.,z1/4.); }
    }
    private record Entry(boolean hidden, long frame) {}
    private static final HashMap<Key,Entry> CACHE = new HashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong EPOCH = new java.util.concurrent.atomic.AtomicLong();
    private static long seenEpoch;
    private static ClientLevel world;
    private static Vec3 origin = Vec3.ZERO;
    private static long frame = Long.MIN_VALUE;
    private static int rays, steps;
    private static long spent;
    private WallOcclusion() {}
    public static void invalidate() { EPOCH.incrementAndGet(); }
    public static long invalidateAndGet() { return EPOCH.incrementAndGet(); }
    public static long epoch() { return EPOCH.get(); }
    public static boolean hidden(AABB bounds, Vec3 camera, boolean anyBlock) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.level == null || bounds == null) return false;
        long epoch = EPOCH.get();
        if (seenEpoch != epoch) { CACHE.clear(); seenEpoch = epoch; }
        long now = Slopium.frame();
        if (frame != now) { frame = now; rays = 256; steps = 8192; spent = 0; }
        if (world != mc.level || origin.distanceToSqr(camera) > .0625) {
            CACHE.clear(); world = mc.level; origin = camera;
        }
        if (!Double.isFinite(bounds.getSize()) || bounds.getSize() > 32 || bounds.contains(camera)
                || bounds.getCenter().distanceToSqr(camera) > 128*128) return false;
        Key key = new Key((int)Math.floor(bounds.minX*4), (int)Math.floor(bounds.minY*4), (int)Math.floor(bounds.minZ*4),
            (int)Math.ceil(bounds.maxX*4), (int)Math.ceil(bounds.maxY*4), (int)Math.ceil(bounds.maxZ*4), anyBlock);
        if (camera.x >= key.x0/4. && camera.x < key.x1/4.
                && camera.y >= key.y0/4. && camera.y < key.y1/4.
                && camera.z >= key.z0/4. && camera.z < key.z1/4.) return false;
        Entry cached = CACHE.get(key);
        if (cached != null && now - cached.frame <= 4 && now >= cached.frame) {
            if (SlopiumConfig.get().diagnostics) SlopiumStats.occlusionCacheHits++;
            return cached.hidden;
        }
        if (rays < 9 || steps < 256 || spent >= 1_000_000) {
            if (SlopiumConfig.get().diagnostics) SlopiumStats.occlusionBudgetMisses++;
            return false;
        }
        // Plot coordinates and non-ready Sable worlds must not use static-world occlusion.
        if (SableGuard.protects(world, BlockPos.containing(bounds.getCenter()))
                || SableGuard.protects(world, BlockPos.containing(camera))) return false;
        AABB box = key.box();
        long start = System.nanoTime();
        boolean hidden = OcclusionSamples.hidden(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ,
            (x,y,z) -> blocked(box, camera, new Vec3(x,y,z), anyBlock));
        spent += System.nanoTime()-start;
        if (CACHE.size() >= 8192) CACHE.clear();
        CACHE.put(key, new Entry(hidden, now));
        return hidden;
    }
    private static boolean blocked(AABB target, Vec3 from, Vec3 to, boolean anyBlock) {
        if (--rays < 0) return false;
        if (SlopiumConfig.get().diagnostics) SlopiumStats.occlusionRays++;
        return BlockGetter.traverseBlocks(from, to, target, (box,pos) -> {
            if (--steps < 0 || !world.hasChunkAt(pos)) return Boolean.FALSE;
            // Never let the object itself, or a block overlapping its expanded bounds, hide it.
            if (box.intersects(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1.,pos.getY()+1.,pos.getZ()+1.)) return null;
            var state = world.getBlockState(pos);
            if (OccluderShapes.blocks(state,world,pos,from,to,anyBlock)) return Boolean.TRUE;
            return null;
        }, box -> Boolean.FALSE);
    }
}

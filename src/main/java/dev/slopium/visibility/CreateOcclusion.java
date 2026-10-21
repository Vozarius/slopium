package dev.slopium.visibility;

import dev.slopium.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * Mechanism centre visibility, independent of inflated renderer bounds.
 * A separate budget keeps these checks independent from particles. Deferred cells
 * are serviced FIFO before new requests on the next frame. No synchronous GPU reads.
 */
public final class CreateOcclusion {
    private record Key(int x, int y, int z, boolean any) {}
    private record Entry(boolean hidden, long frame) {}
    private static final Map<Key, Entry> CACHE = new HashMap<>();
    private static final LinkedHashMap<Key, Vec3> PENDING = new LinkedHashMap<>();
    private static ClientLevel world;
    private static Vec3 camera = Vec3.ZERO;
    private static long epoch = Long.MIN_VALUE, frame = Long.MIN_VALUE, spent;
    private static int rays, steps;
    private static long deadline=Long.MAX_VALUE, queueFrame=Long.MIN_VALUE;
    public static void limitUntil(long value) { deadline=value; }
    private CreateOcclusion() {}

    public static boolean hidden(BlockPos target, Vec3 eye, boolean any) {
        return Boolean.TRUE.equals(decision(target,eye,any,true));
    }
    public static Boolean decision(BlockPos target, Vec3 eye, boolean any, boolean queued) {
        Vec3 point = Vec3.atCenterOf(target);
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.level == null || !finite(point) || !finite(eye)) return false;
        long now = Slopium.frame(), currentEpoch = WallOcclusion.epoch();
        // Never reuse a hidden result after the camera moves (including small movements past a pane).
        if (world != mc.level || !camera.equals(eye)) {
            CACHE.clear(); PENDING.clear(); world = mc.level; camera = eye;
        }
        if (epoch != currentEpoch) {
            // Geometry changes invalidate answers, not requests: trace queued cells against
            // the new geometry. Repeated factory updates must not starve the queue.
            CACHE.clear(); epoch = currentEpoch;
        }
        if (now != frame) {
            frame = now; rays = 1024; steps = 32768; spent = 0;
        }
        if(queued && queueFrame!=now) {
            queueFrame=now;
            var iterator = PENDING.entrySet().iterator();
            while (iterator.hasNext() && budget()) {
                var request = iterator.next();
                Boolean result = trace(request.getValue(), request.getKey().any);
                if (result != null) remember(request.getKey(), result, now);
                iterator.remove();
            }
        }
        if (SlopiumConfig.get().diagnostics) SlopiumStats.createOcclusionChecks++;
        if (point.distanceToSqr(eye) < .0001 || point.distanceToSqr(eye) > 128*128) return false;
        if (SableGuard.protects(world, BlockPos.containing(point)) || SableGuard.protects(world, BlockPos.containing(eye))) {
            if (SlopiumConfig.get().diagnostics) SlopiumStats.createOcclusionProtected++;
            return false;
        }
        // One cache entry per block and obstacle mode.
        Key key = new Key(target.getX(), target.getY(), target.getZ(), any);
        Entry previous = CACHE.get(key);
        if (previous != null && now >= previous.frame && now - previous.frame <= 20) {
            if (SlopiumConfig.get().diagnostics) SlopiumStats.createOcclusionCacheHits++;
            return previous.hidden;
        }
        if (budget()) {
            Boolean result = trace(point, any);
            if (result != null) {
                remember(key, result, now);
                PENDING.remove(key);
                return result;
            }
        }
        if (SlopiumConfig.get().diagnostics) SlopiumStats.createOcclusionDeferred++;
        if (queued && PENDING.size() < 4096) PENDING.putIfAbsent(key, point);
        return null;
    }

    private static boolean finite(Vec3 p) { return Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z); }
    private static void remember(Key key, boolean hidden, long now) {
        if (CACHE.size() >= 16384) CACHE.clear();
        CACHE.put(key, new Entry(hidden, now));
    }
    private static boolean budget() { return rays > 0 && steps >= 256 && spent < 1_000_000 && System.nanoTime()<deadline; }
    private static Boolean trace(Vec3 point, boolean any) {
        --rays;
        if (SlopiumConfig.get().diagnostics) SlopiumStats.createOcclusionRays++;
        long start = System.nanoTime();
        // Exclude only the mechanism itself, never adjacent window cells.
        BlockPos target = BlockPos.containing(point);
        int hit = BlockGetter.traverseBlocks(camera, point, any, (mode, pos) -> {
            if (--steps < 0 || !world.hasChunkAt(pos)) return 0;
            if (pos.equals(target)) return null;
            return OccluderShapes.blocks(world.getBlockState(pos), world, pos, camera, point, mode) ? 2 : null;
        }, mode -> 1);
        // Exhaustion is not evidence of visibility and must not be cached.
        long elapsed = System.nanoTime() - start;
        spent += elapsed;
        if (SlopiumConfig.get().diagnostics) SlopiumStats.createTraceNanos += elapsed;
        return hit == 0 ? null : hit == 2;
    }
}

package dev.slopium.visibility;

import dev.slopium.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * Particle centre visibility, independent of the inflated NeoForge frustum bounds.
 * A separate budget prevents Create checks from starving particles. Deferred cells
 * are serviced FIFO before new requests on the next frame. No synchronous GPU reads.
 */
public final class ParticleOcclusion {
    private record Key(int x, int y, int z, boolean any) {}
    private record Entry(boolean hidden, long frame) {}
    private static final Map<Key, Entry> CACHE = new HashMap<>();
    private static final LinkedHashMap<Key, Vec3> PENDING = new LinkedHashMap<>();
    private static ClientLevel world;
    private static Vec3 camera = Vec3.ZERO;
    private static long epoch = Long.MIN_VALUE, frame = Long.MIN_VALUE, spent;
    private static int rays, steps;
    private ParticleOcclusion() {}

    public static boolean hidden(Vec3 point, Vec3 eye, boolean any) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.level == null || !finite(point) || !finite(eye)) return false;
        long now = Slopium.frame(), currentEpoch = WallOcclusion.epoch();
        // Never reuse a hidden result after the camera moves (including small movements past a pane).
        if (world != mc.level || epoch != currentEpoch || !camera.equals(eye)) {
            CACHE.clear(); PENDING.clear(); world = mc.level; camera = eye; epoch = currentEpoch;
        }
        if (now != frame) {
            frame = now; rays = 1024; steps = 32768; spent = 0;
            var iterator = PENDING.entrySet().iterator();
            while (iterator.hasNext() && budget()) {
                var request = iterator.next();
                Boolean result = trace(request.getValue(), request.getKey().any);
                if (result != null) remember(request.getKey(), result, now);
                iterator.remove();
            }
        }
        if (SlopiumConfig.get().diagnostics) SlopiumStats.particleOcclusionChecks++;
        if (point.distanceToSqr(eye) < .0001 || point.distanceToSqr(eye) > 128*128) return false;
        if (SableGuard.protects(world, BlockPos.containing(point)) || SableGuard.protects(world, BlockPos.containing(eye))) {
            if (SlopiumConfig.get().diagnostics) SlopiumStats.particleOcclusionProtected++;
            return false;
        }
        // Fine cells share results for dense clouds; the actual particle centre is traced.
        Key key = new Key((int)Math.floor(point.x*8), (int)Math.floor(point.y*8), (int)Math.floor(point.z*8), any);
        Entry previous = CACHE.get(key);
        if (previous != null && now >= previous.frame && now - previous.frame <= 20) return previous.hidden;
        if (budget()) {
            Boolean result = trace(point, any);
            if (result != null) {
                remember(key, result, now);
                PENDING.remove(key);
                return result;
            }
        }
        if (SlopiumConfig.get().diagnostics) SlopiumStats.particleOcclusionDeferred++;
        if (PENDING.size() < 4096) PENDING.putIfAbsent(key, point);
        return false;
    }

    private static boolean finite(Vec3 p) { return Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z); }
    private static void remember(Key key, boolean hidden, long now) {
        if (CACHE.size() >= 16384) CACHE.clear();
        CACHE.put(key, new Entry(hidden, now));
    }
    private static boolean budget() { return rays > 0 && steps >= 256 && spent < 1_000_000; }
    private static Boolean trace(Vec3 point, boolean any) {
        --rays;
        long start = System.nanoTime();
        // No "target bounds" exclusion: a nearby window is an obstacle even when
        // it overlaps the particle's enlarged frustum box.
        int hit = BlockGetter.traverseBlocks(camera, point, any, (mode, pos) -> {
            if (--steps < 0 || !world.hasChunkAt(pos)) return 0;
            return OccluderShapes.blocks(world.getBlockState(pos), world, pos, camera, point, mode) ? 2 : null;
        }, mode -> 1);
        // Exhaustion is not evidence of visibility and must not be cached.
        spent += System.nanoTime() - start;
        return hit == 0 ? null : hit == 2;
    }
}

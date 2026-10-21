package dev.slopium.visibility;

import dev.slopium.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Render-thread snapshot. Readers enqueue work; only the frame processor traces geometry. */
public final class VisibilityPool {
    private record Key(int x, int y, int z, boolean mechanism) {}
    private static final class Entry {
        final Key key;
        Vec3 point;
        Vec3 eye;
        long sampled=-1, epoch=-1, requested;
        int result;
        boolean known, queued;
        Entry(Key key, Vec3 point) { this.key=key; this.point=point; }
    }
    private static final Map<Key,Entry> TABLE=new HashMap<>();
    private static final ArrayDeque<Entry> CREATE=new ArrayDeque<>(), PARTICLES=new ArrayDeque<>(), SWEEP=new ArrayDeque<>();
    private static ClientLevel world;
    private static long processed=Long.MIN_VALUE;
    private static boolean particleTurn;
    private static int steps, createEntries, particleEntries;
    private static long deadline;
    private VisibilityPool() {}

    private static void world(ClientLevel level) {
        if(world==level) return;
        TABLE.clear(); CREATE.clear(); PARTICLES.clear(); SWEEP.clear();
        world=level; processed=Long.MIN_VALUE; createEntries=particleEntries=0;
    }
    public static void clear() { world(null); }
    private static void discard(Entry e) {
        if(TABLE.remove(e.key,e)) { if(e.key.mechanism) createEntries--; else particleEntries--; }
    }
    private static boolean finite(Vec3 p) { return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z); }
    private static void enqueue(Entry e) {
        if(e.queued) return;
        e.queued=true; (e.key.mechanism?CREATE:PARTICLES).addLast(e);
    }
    /** null means not sampled yet. Budget pressure never replaces a known answer with unknown. */
    public static Boolean read(Vec3 point,Vec3 eye,boolean mechanism,boolean any) {
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()||mc.level==null||!finite(point)||!finite(eye)) return false;
        world(mc.level);
        if(point.distanceToSqr(eye)<.0001||point.distanceToSqr(eye)>128*128) return false;
        long now=Slopium.frame();
        Key key=new Key((int)Math.floor(point.x*8),(int)Math.floor(point.y*8),(int)Math.floor(point.z*8),mechanism);
        Entry e=TABLE.get(key);
        if(e==null) {
            if((mechanism?createEntries:particleEntries)>=16384) return false;
            e=new Entry(key,point); TABLE.put(key,e); SWEEP.addLast(e);
            if(mechanism) createEntries++; else particleEntries++;
        }
        e.point=point; e.requested=now;
        boolean stale=!e.known||e.epoch!=WallOcclusion.epoch()||!eye.equals(e.eye)||now-e.sampled>=8;
        if(stale) enqueue(e);
        if(SlopiumConfig.get().diagnostics) {
            SlopiumStats.visibilityReads++;
            if(e.known) SlopiumStats.visibilityHits++;
            else SlopiumStats.visibilityUnknown++;
        }
        // Retain the previous answer while a fresh check is queued. No camera/TTL flicker.
        return e.known ? (e.result & (any?2:1))!=0 : null;
    }
    /** Called once at LevelRenderer entry, after the camera has been positioned. */
    public static void process(Vec3 eye) {
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()) return;
        world(mc.level);
        if(world==null||!finite(eye)||processed==Slopium.frame()) return;
        processed=Slopium.frame();
        long start=System.nanoTime(); deadline=start+750_000; steps=32768;
        // Snapshot queue lengths: requests are never processed twice in the same frame.
        int count=Math.min(512,CREATE.size()+PARTICLES.size());
        try {
            for(int i=0;i<count && steps>=256 && System.nanoTime()<deadline;i++) {
                particleTurn=!particleTurn;
                var queue=particleTurn?PARTICLES:CREATE;
                if(queue.isEmpty()) queue=particleTurn?CREATE:PARTICLES;
                Entry e=queue.pollFirst();
                if(e==null) break;
                e.queued=false;
                if(processed-e.requested>120) { discard(e); continue; }
                Integer answer=trace(e,eye);
                if(answer==null) { enqueue(e); continue; }
                e.result=answer; e.known=true; e.sampled=processed;
                e.eye=eye; e.epoch=WallOcclusion.epoch();
            }
            // Incremental reclamation: no periodic full scan or wholesale cache eviction.
            int sweepCount=Math.min(64,SWEEP.size());
            for(int i=0;i<sweepCount && System.nanoTime()<deadline;i++) {
                Entry e=SWEEP.removeFirst();
                if(!e.queued && processed-e.requested>120) discard(e);
                else if(TABLE.get(e.key)==e) SWEEP.addLast(e);
            }
        } finally {
            if(SlopiumConfig.get().diagnostics) {
                long cost=System.nanoTime()-start;
                SlopiumStats.visibilityPasses++; SlopiumStats.visibilityNanos+=cost;
                SlopiumStats.visibilityMaxNanos=Math.max(SlopiumStats.visibilityMaxNanos,cost);
                SlopiumStats.visibilityEntries=TABLE.size();
                SlopiumStats.visibilityPending=CREATE.size()+PARTICLES.size();
            }
        }
    }
    private static Integer trace(Entry e,Vec3 eye) {
        Vec3 point=e.point;
        BlockPos target=BlockPos.containing(point);
        if(point.distanceToSqr(eye)>128*128||SableGuard.protects(world,target)
                ||SableGuard.protects(world,BlockPos.containing(eye))) return 0;
        if(SlopiumConfig.get().diagnostics) {
            SlopiumStats.visibilityRays++;
            if(e.key.mechanism) SlopiumStats.createOcclusionRays++;
        }
        int[] found={0};
        Integer result=BlockGetter.traverseBlocks(eye,point,e,(entry,pos)->{
            if(--steps<0) return -1; // Incomplete traversal must not overwrite a known answer.
            if(!world.hasChunkAt(pos)) return 0;
            if(entry.key.mechanism && pos.equals(target)) return null;
            found[0]|=OccluderShapes.mask(world.getBlockState(pos),world,pos,eye,point);
            return (found[0]&1)!=0 ? 3 : null;
        },entry->found[0]);
        return result<0?null:result;
    }
    public static String describe(BlockPos pos) {
        Vec3 p=Vec3.atCenterOf(pos);
        Entry e=TABLE.get(new Key((int)Math.floor(p.x*8),(int)Math.floor(p.y*8),(int)Math.floor(p.z*8),true));
        return e==null?"Pool: not requested": "Pool: opaque="+(e.known?(e.result&1)!=0:"unknown")
            +" | any="+(e.known?(e.result&2)!=0:"unknown")+" | queued="+e.queued
            +" | sample age="+(e.known?Slopium.frame()-e.sampled:"never")+" frames";
    }
}

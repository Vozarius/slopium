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
        Vec3 eye, samplePoint;
        BlockPos witness;
        BlockPos shapeWitness;
        long shapeGeneration=Long.MIN_VALUE,shapeVersion;
        java.util.List<net.minecraft.world.phys.AABB> opaqueBoxes=List.of(),anyBoxes=List.of();
        Vec3 proofEye;
        net.minecraft.world.phys.AABB proofBounds;
        int proofKnown,proofResult;
        long geometryGeneration, traced;
        long[] columns, versions;
        int pathCount;
        boolean pathValid;
        long sampled=-1, epoch=-1, requested;
        int result, knownMask, wanted;
        int distanceLimit=128;
        boolean known, queued, live=true;
        long cameraFrame=Long.MIN_VALUE;
        net.minecraft.world.phys.AABB bounds;
        boolean outside;
        Entry(Key key, Vec3 point) { this.key=key; this.point=point; }
    }
    /** BE-owned direct reference into the same pool used by particles and other readers. */
    public static final class Handle {
        final Vec3 point;
        private Entry entry;
        private long generation=-1;
        public long boundsFrame=Long.MIN_VALUE;
        public net.minecraft.world.phys.AABB bounds, rawBounds;
        public Handle(Vec3 point){this.point=point;}
    }
    private static long generation;
    private static Entry entry(Handle handle){
        if(handle.generation!=generation || handle.entry==null || !handle.entry.live){
            handle.entry=entry(handle.point,true);handle.generation=generation;
        }
        if(handle.entry!=null)handle.entry.requested=Slopium.frame();
        return handle.entry;
    }
    private static final Map<Key,Entry> TABLE=new HashMap<>();
    private static final ArrayDeque<Entry> CREATE=new ArrayDeque<>(), PARTICLES=new ArrayDeque<>(), SWEEP=new ArrayDeque<>();
    private static ClientLevel world;
    private static net.minecraft.client.renderer.culling.Frustum cameraFrustum;
    private static final org.joml.Matrix4f viewMatrix=new org.joml.Matrix4f(),projectionMatrix=new org.joml.Matrix4f();
    private static Vec3 viewEye;
    private static long cameraFrame=Long.MIN_VALUE, cameraRevision;
    private static long processed=Long.MIN_VALUE;
    private static boolean particleTurn;
    private static int steps, createEntries, particleEntries;
    private static long deadline;
    private VisibilityPool() {}

    private static void world(ClientLevel level) {
        if(world==level) return;
        generation++; TABLE.clear(); CREATE.clear(); PARTICLES.clear(); SWEEP.clear();
        VisibilityGeometry.reset();
        SectionVisibility.clear();
        cameraFrustum=null;cameraFrame=Long.MIN_VALUE;
        viewEye=null;
        world=level; processed=Long.MIN_VALUE; createEntries=particleEntries=0;
    }
    public static void clear() { world(null); }
    private static void discard(Entry e) {
        if(TABLE.remove(e.key,e)) { e.live=false; if(e.key.mechanism) createEntries--; else particleEntries--; }
    }
    private static boolean finite(Vec3 p) { return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z); }
    private static void enqueue(Entry e) {
        if(e.queued) return;
        e.queued=true; (e.key.mechanism?CREATE:PARTICLES).addLast(e);
    }
    public static void captureCamera(org.joml.Matrix4fc view,org.joml.Matrix4fc projection,Vec3 eye) {
        world(Minecraft.getInstance().level);
        cameraFrame=Slopium.frame();
        var c=SlopiumConfig.get();
        if(!c.enabled||!c.cameraCulling){cameraFrustum=null;return;}
        if(cameraFrustum!=null&&view.equals(viewMatrix)&&projection.equals(projectionMatrix)&&eye.equals(viewEye))return;
        cameraRevision++;
        viewMatrix.set(view);projectionMatrix.set(projection);viewEye=eye;
        var frustum=new net.minecraft.client.renderer.culling.Frustum(viewMatrix,projectionMatrix);
        frustum.prepare(eye.x,eye.y,eye.z);
        if(c.cameraCulling)cameraFrustum=frustum;
    }
    public static boolean cameraReady(){return cameraFrustum!=null&&cameraFrame==Slopium.frame()&&world==Minecraft.getInstance().level;}
    /** Camera and wall results occupy the same entry, but have independent lifetimes. */
    public static boolean outsideCamera(Vec3 point,boolean mechanism,net.minecraft.world.phys.AABB bounds) {
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        if(!c.enabled||!c.cameraCulling||!mc.isSameThread()||mc.level==null||bounds==null||!finite(point)
            ||cameraFrustum==null||cameraFrame!=Slopium.frame()||world!=mc.level
            ||!Double.isFinite(bounds.minX+bounds.minY+bounds.minZ+bounds.maxX+bounds.maxY+bounds.maxZ)) return false;
        if(SableGuard.protects(mc.level,BlockPos.containing(point))) return false;
        return outsideCamera(entry(point,mechanism),bounds,c);
    }
    public static boolean outsideCamera(Handle handle,net.minecraft.world.phys.AABB bounds){
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        if(!c.enabled||!c.cameraCulling||!mc.isSameThread()||mc.level==null||world!=mc.level
            ||bounds==null||cameraFrustum==null||cameraFrame!=Slopium.frame())return false;
        if(!Double.isFinite(bounds.minX+bounds.minY+bounds.minZ+bounds.maxX+bounds.maxY+bounds.maxZ))return false;
        return outsideCamera(entry(handle),bounds,c);
    }
    private static boolean outsideCamera(Entry e,net.minecraft.world.phys.AABB bounds,SlopiumConfig c){
        if(e==null)return false;
        if(e.cameraFrame!=cameraRevision||!bounds.equals(e.bounds)) {
            e.outside=!cameraFrustum.isVisible(bounds);e.bounds=bounds;e.cameraFrame=cameraRevision;
            if(c.diagnostics) SlopiumStats.cameraChecks++;
        }
        if(e.outside&&c.diagnostics) SlopiumStats.cameraRejects++;
        return e.outside;
    }
    private static Entry entry(Vec3 point,boolean mechanism) {
        Key key=new Key((int)Math.floor(point.x*8),(int)Math.floor(point.y*8),(int)Math.floor(point.z*8),mechanism);
        Entry e=TABLE.get(key);
        if(e==null) {
            if((mechanism?createEntries:particleEntries)>=16384) return null;
            e=new Entry(key,point);TABLE.put(key,e);SWEEP.addLast(e);
            if(mechanism) createEntries++;else particleEntries++;
        }
        e.point=point;e.requested=Slopium.frame();return e;
    }
    /** null means not sampled yet. Budget pressure never replaces a known answer with unknown. */
    public static Boolean read(Vec3 point,Vec3 eye,boolean mechanism,boolean any) {
        return request(point,eye,mechanism,any,128);
    }
    private static Boolean request(Vec3 point,Vec3 eye,boolean mechanism,boolean any,int distanceLimit) {
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()||mc.level==null||!finite(point)||!finite(eye)) return false;
        world(mc.level);
        if(point.distanceToSqr(eye)<.0001||point.distanceToSqr(eye)>(double)distanceLimit*distanceLimit) return false;
        long now=Slopium.frame();
        Entry e=entry(point,mechanism);
        if(e==null) return false;
        return request(e,point,eye,mechanism,any,distanceLimit,now);
    }
    public static Boolean read(Handle handle,Vec3 eye,boolean any){
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()||mc.level==null||!finite(eye))return false;
        world(mc.level);
        double distance=handle.point.distanceToSqr(eye);
        if(distance<.0001||distance>128.0*128)return false;
        var e=entry(handle);if(e==null)return false;
        return request(e,handle.point,eye,true,any,128,Slopium.frame());
    }
    private static Boolean request(Entry e,Vec3 point,Vec3 eye,boolean mechanism,boolean any,int distanceLimit,long now){
        e.distanceLimit=Math.max(e.distanceLimit,distanceLimit);
        int mode=any?2:1; e.wanted|=mode;
        boolean stale=(e.knownMask&mode)==0||e.epoch!=WallOcclusion.epoch()||!eye.equals(e.eye)
            ||(!mechanism&&!point.equals(e.samplePoint))||now-e.sampled>=20;
        if(stale) enqueue(e);
        if(SlopiumConfig.get().diagnostics) {
            SlopiumStats.visibilityReads++;
            if((e.knownMask&mode)!=0) SlopiumStats.visibilityHits++;
            else SlopiumStats.visibilityUnknown++;
        }
        // Retain the previous answer while a fresh check is queued. No camera/TTL flicker.
        return (e.knownMask&mode)!=0 ? (e.result & mode)!=0 : null;
    }
    public static long cameraRevision(){return cameraRevision;}
    /** Re-prove a hidden MODEL against its remembered blocker, never against an old centre-ray result.
     * Unknown/partial bounds stay visible. The two policies share the same pool and witness geometry. */
    public static boolean provesHidden(Handle handle,Vec3 eye,boolean any,net.minecraft.world.phys.AABB bounds){
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()||mc.level==null||world!=mc.level||bounds==null||!finite(eye))return false;
        Entry e=entry(handle);
        int mode=any?2:1;
        if(e==null||(e.knownMask&mode)==0||(e.result&mode)==0||e.witness==null)return false;
        BlockPos blocker=e.witness;
        if(!world.hasChunkAt(blocker))return false;
        long gen=VisibilityGeometry.generation();
        long version=VisibilityGeometry.version(net.minecraft.world.level.ChunkPos.asLong(blocker.getX()>>4,blocker.getZ()>>4));
        if(!blocker.equals(e.shapeWitness)||gen!=e.shapeGeneration||version!=e.shapeVersion){
            var state=world.getBlockState(blocker);
            e.opaqueBoxes=OccluderShapes.volume(state,world,blocker,false).toAabbs();
            e.anyBoxes=OccluderShapes.volume(state,world,blocker,true).toAabbs();
            e.shapeWitness=blocker;e.shapeGeneration=gen;e.shapeVersion=version;e.proofKnown=0;
        }
        if(!eye.equals(e.proofEye)||!bounds.equals(e.proofBounds)){
            e.proofEye=eye;e.proofBounds=bounds;e.proofKnown=0;
        }
        if((e.proofKnown&mode)!=0)return (e.proofResult&mode)!=0;
        e.proofKnown|=mode;e.proofResult&=~mode;
        var boxes=any?e.anyBoxes:e.opaqueBoxes;
        if(boxes.size()>16)return false;
        double x=blocker.getX(),y=blocker.getY(),z=blocker.getZ();
        for(var box:boxes)if(BlockShadow.covers(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ,
            bounds.minX-x,bounds.minY-y,bounds.minZ-z,bounds.maxX-x,bounds.maxY-y,bounds.maxZ-z,eye.x-x,eye.y-y,eye.z-z)){e.proofResult|=mode;return true;}
        return false;
    }
    /** Called once at LevelRenderer entry, after the camera has been positioned. */
    public static void process(Vec3 eye) {
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()) return;
        world(mc.level);
        if(world==null||!finite(eye)||processed==Slopium.frame()) return;
        processed=Slopium.frame();
        if(TABLE.isEmpty()) return;
        long start=System.nanoTime();long totalDeadline=start+750_000;deadline=totalDeadline;steps=32768;
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
                e.result=answer&3; e.knownMask=answer>>2; e.known=true; e.sampled=processed;
                e.eye=eye; e.samplePoint=e.point; e.epoch=WallOcclusion.epoch();
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
    private static boolean unchanged(Entry e,Vec3 eye,long generation) {
        if(!e.pathValid||generation!=e.geometryGeneration||(e.knownMask&e.wanted)!=e.wanted
            ||processed-e.traced>=20||!eye.equals(e.eye)||!e.point.equals(e.samplePoint)) return false;
        for(int i=0;i<e.pathCount;i++) if(VisibilityGeometry.version(e.columns[i])!=e.versions[i]) return false;
        return true;
    }
    private static Integer trace(Entry e,Vec3 eye) {
        Vec3 point=e.point;
        BlockPos target=BlockPos.containing(point);
        if(point.distanceToSqr(eye)>(double)e.distanceLimit*e.distanceLimit||SableGuard.protects(world,target)
                ||SableGuard.protects(world,BlockPos.containing(eye))) return 12;
        long generation=VisibilityGeometry.generation();
        if(unchanged(e,eye,generation)) {
            if(SlopiumConfig.get().diagnostics) SlopiumStats.visibilityPathReuse++;
            return e.result|(e.knownMask<<2);
        }
        // A fresh intersection with one remembered blocker proves occlusion even after camera movement.
        // Full-cube broad phase is necessary: isSolidRender alone assumes the ray visits this cell.
        if(e.witness!=null && e.witness.distToCenterSqr(eye)>=16 && (!e.key.mechanism||!e.witness.equals(target)) && world.hasChunkAt(e.witness)
            && net.minecraft.world.phys.shapes.Shapes.block().clip(eye,point,e.witness)!=null) {
            if(SlopiumConfig.get().diagnostics) SlopiumStats.visibilityCells++;
            int mask=OccluderShapes.mask(world.getBlockState(e.witness),world,e.witness,eye,point);
            if((mask&1)!=0 || (e.wanted==2 && (mask&2)!=0)) {
                e.pathValid=false; // This proves hiding, not visibility of the rest of the path.
                if(SlopiumConfig.get().diagnostics) SlopiumStats.visibilityWitnessReuse++;
                return (mask&1)!=0?15:10;
            }
        }
        e.pathValid=false; e.pathCount=0; e.witness=null;
        if(e.columns==null) { e.columns=new long[16]; e.versions=new long[16]; }
        if(SlopiumConfig.get().diagnostics) {
            SlopiumStats.visibilityRays++;
            if(e.key.mechanism) SlopiumStats.createOcclusionRays++;
        }
        int[] found={0};
        Integer result=BlockGetter.traverseBlocks(eye,point,e,(entry,pos)->{
            if(--steps<0) return -1;
            long column=net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4);
            if(entry.pathCount==0 || entry.columns[entry.pathCount-1]!=column) {
                if(entry.pathCount>=128) return -1;
                if(entry.pathCount==entry.columns.length){entry.columns=Arrays.copyOf(entry.columns,entry.columns.length*2);entry.versions=Arrays.copyOf(entry.versions,entry.versions.length*2);}
                entry.columns[entry.pathCount]=column;
                entry.versions[entry.pathCount++]=VisibilityGeometry.version(column);
            }
            if(!world.hasChunkAt(pos)) return 12;
            if(entry.key.mechanism && pos.equals(target)) return null;
            if(SlopiumConfig.get().diagnostics) SlopiumStats.visibilityCells++;
            int mask=OccluderShapes.mask(world.getBlockState(pos),world,pos,eye,point);
            found[0]|=mask;
            if((mask&1)!=0) { entry.witness=pos.immutable();return 15; }
            if((mask&2)!=0) {
                entry.witness=pos.immutable();
                if(entry.wanted==2) return 10; // Opaque was not requested; do not traverse the factory behind the glass.
            }
            return null;
        },entry->12|found[0]);
        if(result<0) return null;
        e.pathValid=true; e.geometryGeneration=generation; e.traced=processed;
        return result;
    }
    public static String describe(BlockPos pos) {
        Vec3 p=Vec3.atCenterOf(pos);
        Entry e=TABLE.get(new Key((int)Math.floor(p.x*8),(int)Math.floor(p.y*8),(int)Math.floor(p.z*8),true));
        return e==null?"Pool: not requested": "Pool: opaque="+((e.knownMask&1)!=0?(e.result&1)!=0:"unknown")
            +" | any="+((e.knownMask&2)!=0?(e.result&2)!=0:"unknown")+" | queued="+e.queued
            +" | sample age="+(e.known?Slopium.frame()-e.sampled:"never")+" frames | blocker="+e.witness+" | witness shapes="+e.anyBoxes.size();
    }
}

package dev.slopium.visibility;

import dev.slopium.client.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import java.util.ArrayDeque;

/** Volume entries in the shared visibility scheduler. Terrain and models only read results. */
public final class SectionVisibility {
    private static final class Entry {
        final long key;final AABB bounds;final Vec3 center;
        long requested,checked=Long.MIN_VALUE,epoch=-1;Vec3 eye;boolean hidden,queued;
        long generation;
        final it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap walls=new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();
        Entry(int x,int y,int z){key=SectionPos.asLong(x,y,z);bounds=new AABB(x*16.,y*16.,z*16.,x*16.+16,y*16.+16,z*16.+16).inflate(.001);center=bounds.getCenter();}
    }
    private static final Long2ObjectOpenHashMap<Entry> entries=new Long2ObjectOpenHashMap<>();
    private static final ArrayDeque<Entry> queue=new ArrayDeque<>(),sweep=new ArrayDeque<>();
    private static Object world;private static Vec3 eye=Vec3.ZERO;private static long frame=Long.MIN_VALUE;
    private static int radius,minimum,policy=-1;
    public static long proofs,cells,nanos,reads,hits,hiddenDraws,frames,regionNanos,regions;
    public static void resetCounters(){proofs=cells=nanos=reads=hits=hiddenDraws=frames=regionNanos=regions=0;}
    private SectionVisibility(){}
    public static boolean active(){var c=SlopiumConfig.get();return c.enabled&&c.visibleSections;}
    public static int minimum(){return SectionRange.minimum(SlopiumConfig.get().minimumSections,Minecraft.getInstance().options.renderDistance().get());}
    public static void clear(){entries.clear();queue.clear();sweep.clear();world=null;frame=Long.MIN_VALUE;policy=-1;}
    public static void begin(Vec3 camera){
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        int next=(active()?1:0)|(c.sectionsAnyBlock?2:0);
        int r=mc.options.renderDistance().get(),m=SectionRange.minimum(c.minimumSections,r);
        c.minimumSections=m;
        if(world!=mc.level||policy!=next||radius!=r||minimum!=m){clear();world=mc.level;policy=next;radius=r;minimum=m;}
        if(frame!=Slopium.frame()&&active())frames++;
        eye=camera;frame=Slopium.frame();
    }
    private static boolean eligible(int x,int y,int z){return active()&&world!=null&&SectionRange.eligible(x,y,z,SectionPos.blockToSectionCoord(eye.x),SectionPos.blockToSectionCoord(eye.y),SectionPos.blockToSectionCoord(eye.z),minimum,radius);}
    private static boolean valid(Entry e){
        if(e==null||!eye.equals(e.eye))return false;
        long epoch=WallOcclusion.epoch();if(e.epoch==epoch)return true;
        if(!e.hidden||e.generation!=VisibilityGeometry.generation())return false;
        for(var wall:e.walls.long2LongEntrySet())if(VisibilityGeometry.version(wall.getLongKey())!=wall.getLongValue())return false;
        e.epoch=epoch;return true;
    }
    public static boolean request(int x,int y,int z){
        if(!eligible(x,y,z))return false;
        long key=SectionPos.asLong(x,y,z);Entry e=entries.get(key);
        if(e==null){if(entries.size()>=8192)return false;e=new Entry(x,y,z);entries.put(key,e);sweep.add(e);}
        if(e.requested!=frame||e.checked==Long.MIN_VALUE){
            e.requested=frame;
            if(!valid(e)||!e.hidden&&frame-e.checked>=20){
                VisibilityPool.requestSection(e.center,eye,SlopiumConfig.get().sectionsAnyBlock,radius*28+32);
                if(!e.queued){e.queued=true;queue.add(e);}
            }
        }
        reads++;if(valid(e)){hits++;return e.hidden;}return false;
    }
    /** Runs after shared point traces, within their original 0.75 ms total budget. */
    public static void process(long deadline){
        if(!active()||world==null)return;
        long started=System.nanoTime();int count=Math.min(128,queue.size());
        for(int i=0;i<count&&System.nanoTime()<deadline;i++){
            Entry e=queue.removeFirst();e.queued=false;
            if(frame-e.requested>120)continue;
            BlockPos witness=VisibilityPool.sectionWitness(e.center,SlopiumConfig.get().sectionsAnyBlock);
            boolean hidden=false;
            e.walls.clear();e.generation=VisibilityGeometry.generation();
            if(witness!=null){proofs++;hidden=SectionOcclusion.hidden(Minecraft.getInstance().level,e.bounds,eye,witness,SlopiumConfig.get().sectionsAnyBlock,()->{cells++;return System.nanoTime()<deadline;},pos->{
                long column=net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4);
                e.walls.put(column,VisibilityGeometry.version(column));return SectionMeshes.ready(pos);
            });}
            // An unfinished proof never hides a section. Geometry/camera changes fail open immediately.
            e.hidden=hidden;e.eye=eye;e.epoch=WallOcclusion.epoch();e.checked=witness==null?frame-19:frame;
        }
        for(int i=0,n=Math.min(64,sweep.size());i<n;i++){Entry e=sweep.removeFirst();if(!e.queued&&frame-e.requested>120)entries.remove(e.key);else sweep.addLast(e);}
        nanos+=System.nanoTime()-started;
    }
    public static boolean hidden(AABB bounds){
        if(!active()||world!=Minecraft.getInstance().level||frame!=Slopium.frame()||bounds==null||!Double.isFinite(bounds.getSize()))return false;
        int x0=SectionPos.blockToSectionCoord(bounds.minX),y0=SectionPos.blockToSectionCoord(bounds.minY),z0=SectionPos.blockToSectionCoord(bounds.minZ);
        int x1=SectionPos.blockToSectionCoord(bounds.maxX),y1=SectionPos.blockToSectionCoord(bounds.maxY),z1=SectionPos.blockToSectionCoord(bounds.maxZ);
        if((long)x1-x0>3||(long)y1-y0>3||(long)z1-z0>3||x1<x0||y1<y0||z1<z0)return false;
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++){
            if(!eligible(x,y,z))return false;Entry e=entries.get(SectionPos.asLong(x,y,z));if(!valid(e)||!e.hidden)return false;
        }
        return true;
    }
    public static boolean hidden(BlockEntity be){
        if(!active()||be==null||be.getLevel()==null||be.getLevel()!=Minecraft.getInstance().level
            ||Slopium.loaded("create")&&be instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smart&&smart.isVirtual()
            ||SableGuard.protects(be.getLevel(),be.getBlockPos()))return false;
        var renderer=Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        return renderer!=null&&hidden(renderer.getRenderBoundingBox(be));
    }
    public static String status(){return "Sections | reads="+reads+" | hits="+hits+" | proofs="+proofs+" | cells="+cells+" | skipped commands="+hiddenDraws+" | proof ms="+String.format(java.util.Locale.ROOT,"%.3f",nanos/1e6)+" | lists ms/frame="+String.format(java.util.Locale.ROOT,"%.3f",regionNanos/1e6/Math.max(1,frames))+" | entries="+entries.size();}
}

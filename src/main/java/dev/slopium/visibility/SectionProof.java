package dev.slopium.visibility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.ArrayDeque;
import java.util.function.*;

/** Resumable coverage proof of the faces of a box facing the camera.
 * Different tiles may be covered by different walls. A point ray never proves coverage. */
public final class SectionProof {
    public enum Result { PENDING, HIDDEN, VISIBLE }
    private record Tile(AABB box,int depth) {}
    private final BlockGetter world;
    private final Vec3 eye;
    private final boolean any;
    private final Predicate<AABB> outside;
    private final Consumer<BlockPos> observe;
    private final ArrayDeque<Tile> pending=new ArrayDeque<>();
    private int visited;
    private boolean viewDependent;
    private String reason="pending";
    public SectionProof(BlockGetter world,AABB bounds,Vec3 eye,boolean any,Predicate<AABB> outside,Consumer<BlockPos> observe){
        this.world=world;this.eye=eye;this.any=any;this.outside=outside;this.observe=observe;
        if(!Double.isFinite(bounds.getSize())||bounds.contains(eye)){reason="camera inside section";return;}
        if(eye.x<bounds.minX)add(new AABB(bounds.minX,bounds.minY,bounds.minZ,bounds.minX,bounds.maxY,bounds.maxZ));
        else if(eye.x>bounds.maxX)add(new AABB(bounds.maxX,bounds.minY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.maxZ));
        if(eye.y<bounds.minY)add(new AABB(bounds.minX,bounds.minY,bounds.minZ,bounds.maxX,bounds.minY,bounds.maxZ));
        else if(eye.y>bounds.maxY)add(new AABB(bounds.minX,bounds.maxY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.maxZ));
        if(eye.z<bounds.minZ)add(new AABB(bounds.minX,bounds.minY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.minZ));
        else if(eye.z>bounds.maxZ)add(new AABB(bounds.minX,bounds.minY,bounds.maxZ,bounds.maxX,bounds.maxY,bounds.maxZ));
        if(pending.isEmpty())reason="camera intersects section";
    }
    private void add(AABB box){pending.add(new Tile(box,0));}
    public boolean viewDependent(){return viewDependent;}
    public String reason(){return reason;}
    public Result step(BooleanSupplier budget,Predicate<BlockPos> ready){
        if(!reason.equals("pending"))return reason.equals("covered")?Result.HIDDEN:Result.VISIBLE;
        while(!pending.isEmpty()){
            if(!budget.getAsBoolean())return Result.PENDING;
            Tile tile=pending.peek();AABB box=tile.box;
            if(outside.test(box.inflate(.00001))){viewDependent=true;pending.pop();continue;}
            if(visited>=512){reason="coverage limit";return Result.VISIBLE;}
            Vec3 center=box.getCenter();boolean[] exhausted={false},unknown={false};
            BooleanSupplier allowance=()->{boolean ok=budget.getAsBoolean();if(!ok)exhausted[0]=true;return ok;};
            BlockPos witness=BlockGetter.traverseBlocks(eye,center,0,(ignored,pos)->{
                if(!allowance.getAsBoolean()){unknown[0]=true;return pos.immutable();}
                observe.accept(pos);
                if(world instanceof Level level&&(!level.hasChunkAt(pos)||SableGuard.protects(level,pos))){unknown[0]=true;return pos.immutable();}
                if(pos.equals(BlockPos.containing(center)))return null;
                return OccluderShapes.blocks(world.getBlockState(pos),world,pos,eye,center,any)?pos.immutable():null;
            },ignored->null);
            if(exhausted[0])return Result.PENDING;
            if(unknown[0]){reason="world not ready";return Result.VISIBLE;}
            if(witness!=null&&SectionOcclusion.hidden(world,box,eye,witness,any,allowance,ready)){
                pending.pop();visited++;continue;
            }
            if(exhausted[0])return Result.PENDING;
            if(witness==null&&!outside.test(new AABB(center,center).inflate(.00001))){reason="visible ray";return Result.VISIBLE;}
            if(tile.depth>=10){reason="uncovered tile";return Result.VISIBLE;}
            pending.pop();visited++;split(box,tile.depth+1);
        }
        reason="covered";return Result.HIDDEN;
    }
    private void split(AABB b,int depth){
        double x=b.getXsize(),y=b.getYsize(),z=b.getZsize();
        if(x>=y&&x>=z){double m=(b.minX+b.maxX)*.5;pending.push(new Tile(new AABB(m,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ),depth));pending.push(new Tile(new AABB(b.minX,b.minY,b.minZ,m,b.maxY,b.maxZ),depth));}
        else if(y>=z){double m=(b.minY+b.maxY)*.5;pending.push(new Tile(new AABB(b.minX,m,b.minZ,b.maxX,b.maxY,b.maxZ),depth));pending.push(new Tile(new AABB(b.minX,b.minY,b.minZ,b.maxX,m,b.maxZ),depth));}
        else{double m=(b.minZ+b.maxZ)*.5;pending.push(new Tile(new AABB(b.minX,b.minY,m,b.maxX,b.maxY,b.maxZ),depth));pending.push(new Tile(new AABB(b.minX,b.minY,b.minZ,b.maxX,b.maxY,m),depth));}
    }
}

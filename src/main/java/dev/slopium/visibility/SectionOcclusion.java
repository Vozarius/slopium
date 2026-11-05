package dev.slopium.visibility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import java.util.function.BooleanSupplier;

/** Conservative volume proof: the entire perspective footprint must fit in a solid wall slice.
 * A centre/corner ray is only a candidate locator, never sufficient evidence to hide terrain. */
public final class SectionOcclusion {
    private SectionOcclusion() {}
    public static boolean hidden(BlockGetter world,AABB target,Vec3 eye,BlockPos witness,boolean any,BooleanSupplier budget){
        return hidden(world,target,eye,witness,any,budget,pos->true);
    }
    public static boolean hidden(BlockGetter world,AABB target,Vec3 eye,BlockPos witness,boolean any,BooleanSupplier budget,java.util.function.Predicate<BlockPos> ready){
        if(witness==null||target.contains(eye)||!Double.isFinite(target.getSize()))return false;
        var shape=OccluderShapes.volume(world.getBlockState(witness),world,witness,any);
        if(shape.isEmpty())return false;
        int candidates=0;
        for(var part:shape.toAabbs())for(int axis=0;axis<3;axis++){
            if(++candidates>24||!budget.getAsBoolean())return false;
            double plane=coord(witness.getX(),witness.getY(),witness.getZ(),axis)+(min(part,axis)+max(part,axis))*.5;
            double origin=coord(eye.x,eye.y,eye.z,axis);
            if(!(origin<plane&&plane<min(target,axis)-.001||origin>plane&&plane>max(target,axis)+.001))continue;
            int u=(axis+1)%3,v=(axis+2)%3;
            double eu=coord(eye.x,eye.y,eye.z,u),ev=coord(eye.x,eye.y,eye.z,v);
            double u0=Double.POSITIVE_INFINITY,v0=u0,u1=Double.NEGATIVE_INFINITY,v1=u1;
            for(int i=0;i<8;i++){
                double x=(i&1)==0?target.minX:target.maxX,y=(i&2)==0?target.minY:target.maxY,z=(i&4)==0?target.minZ:target.maxZ;
                double t=(plane-origin)/(coord(x,y,z,axis)-origin);
                double a=eu+t*(coord(x,y,z,u)-eu),b=ev+t*(coord(x,y,z,v)-ev);
                u0=Math.min(u0,a);u1=Math.max(u1,a);v0=Math.min(v0,b);v1=Math.max(v1,b);
            }
            // Expand, never shrink, the footprint; exact cell boundaries must not leave cracks.
            u0-=.00001;v0-=.00001;u1+=.00001;v1+=.00001;
            int loU=(int)Math.floor(u0),hiU=(int)Math.floor(u1),loV=(int)Math.floor(v0),hiV=(int)Math.floor(v1);
            if((long)(hiU-loU+1)*(hiV-loV+1)>256)continue;
            boolean covered=true;int fixed=(int)Math.floor(plane);
            for(int a=loU;a<=hiU&&covered;a++)for(int b=loV;b<=hiV;b++){
                if(!budget.getAsBoolean())return false;
                var pos=position(axis,fixed,a,b);
                if(!ready.test(pos)){covered=false;break;}
                if(world instanceof Level level&&(!level.hasChunkAt(pos)||SableGuard.protects(level,pos))){covered=false;break;}
                var cell=OccluderShapes.volume(world.getBlockState(pos),world,pos,any);
                if(cell.isEmpty()){covered=false;break;}
                double p=plane-fixed;
                AABB slab=box(axis,p-.000001,p+.000001,Math.max(u0,a)-a,Math.min(u1,a+1)-a,Math.max(v0,b)-b,Math.min(v1,b+1)-b);
                if(Shapes.joinIsNotEmpty(Shapes.create(slab),cell,BooleanOp.ONLY_FIRST)){covered=false;break;}
            }
            if(covered)return true;
        }
        return false;
    }
    private static double coord(double x,double y,double z,int axis){return axis==0?x:axis==1?y:z;}
    private static double min(AABB b,int axis){return coord(b.minX,b.minY,b.minZ,axis);}
    private static double max(AABB b,int axis){return coord(b.maxX,b.maxY,b.maxZ,axis);}
    private static BlockPos position(int axis,int p,int u,int v){return axis==0?new BlockPos(p,u,v):axis==1?new BlockPos(v,p,u):new BlockPos(u,v,p);}
    private static AABB box(int axis,double p0,double p1,double u0,double u1,double v0,double v1){
        return axis==0?new AABB(p0,u0,v0,p1,u1,v1):axis==1?new AABB(v0,p0,u0,v1,p1,u1):new AABB(u0,v0,p0,u1,v1,p1);
    }
}

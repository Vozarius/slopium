package dev.slopium.visibility;

/** Conservative shadow of one actual shape cuboid; no allocations or voxel traversal. */
public final class BlockShadow {
    private static final double EPS=1e-6;
    private BlockShadow(){}
    public static boolean covers(double x0,double y0,double z0,double x1,double y1,double z1,
            double bx0,double by0,double bz0,double bx1,double by1,double bz1,double ex,double ey,double ez){
        if(!Double.isFinite(x0+y0+z0+x1+y1+z1+bx0+by0+bz0+bx1+by1+bz1+ex+ey+ez))return false;
        if(x0>=x1||y0>=y1||z0>=z1||bx0>bx1||by0>by1||bz0>bz1)return false;
        if(ex>=x0-EPS&&ex<=x1+EPS&&ey>=y0-EPS&&ey<=y1+EPS&&ez>=z0-EPS&&ez<=z1+EPS)return false;
        // Shared/touching geometry and a camera inside the model are deliberately visible.
        if(bx1>=x0-EPS&&bx0<=x1+EPS&&by1>=y0-EPS&&by0<=y1+EPS&&bz1>=z0-EPS&&bz0<=z1+EPS)return false;
        if(ex>=bx0&&ex<=bx1&&ey>=by0&&ey<=by1&&ez>=bz0&&ez<=bz1)return false;
        for(int corner=0;corner<8;corner++){
            double dx=((corner&1)==0?bx0:bx1)-ex,dy=((corner&2)==0?by0:by1)-ey,dz=((corner&4)==0?bz0:bz1)-ez;
            double enter=0,exit=1;
            if(Math.abs(dx)<1e-12){if(ex<=x0+EPS||ex>=x1-EPS)return false;}
            else{double a=(x0-ex)/dx,b=(x1-ex)/dx;enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));}
            if(Math.abs(dy)<1e-12){if(ey<=y0+EPS||ey>=y1-EPS)return false;}
            else{double a=(y0-ey)/dy,b=(y1-ey)/dy;enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));}
            if(Math.abs(dz)<1e-12){if(ez<=z0+EPS||ez>=z1-EPS)return false;}
            else{double a=(z0-ez)/dz,b=(z1-ez)/dz;enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));}
            if(enter>=exit-EPS||enter>=1-EPS||exit<=EPS)return false;
        }
        return true;
    }
}

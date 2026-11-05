package dev.slopium.visibility;

/** Vanilla-style cube radius, extended to Y; the camera's own section is always protected. */
public final class SectionRange {
    private SectionRange() {}
    public static int minimum(int requested,int renderDistance){return Math.clamp(requested,0,Math.max(0,renderDistance-1));}
    public static boolean eligible(int x,int y,int z,int cx,int cy,int cz,int minimum,int renderDistance){
        long distance=Math.max(Math.max(Math.abs((long)x-cx),Math.abs((long)y-cy)),Math.abs((long)z-cz));
        return distance>minimum(minimum,renderDistance)&&distance<=renderDistance;
    }
}

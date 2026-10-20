package dev.slopium.visibility;

/** Bounded sampling heuristic, not an exact pixel visibility proof. */
public final class OcclusionSamples {
    private OcclusionSamples() {}
    @FunctionalInterface public interface Ray { boolean blocked(double x, double y, double z); }
    public static boolean hidden(double x0, double y0, double z0, double x1, double y1, double z1, Ray ray) {
        if (!ray.blocked((x0+x1)*.5, (y0+y1)*.5, (z0+z1)*.5)) return false;
        for (int i = 0; i < 8; i++)
            if (!ray.blocked((i&1)==0 ? x0 : x1, (i&2)==0 ? y0 : y1, (i&4)==0 ? z0 : z1)) return false;
        return true;
    }
}

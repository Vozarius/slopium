package dev.slopium.visibility;
import dev.slopium.client.*;
import net.minecraft.world.phys.Vec3;
/** Particle cells share the processor and budget with Create, but retain distinct target semantics. */
public final class ParticleOcclusion {
    private ParticleOcclusion() {}
    public static boolean hidden(Vec3 point,Vec3 eye,boolean any) {
        Boolean result=VisibilityPool.read(point,eye,false,any);
        if(SlopiumConfig.get().diagnostics) {
            SlopiumStats.particleOcclusionChecks++;
            if(result==null) SlopiumStats.particleOcclusionDeferred++;
        }
        return Boolean.TRUE.equals(result);
    }
}

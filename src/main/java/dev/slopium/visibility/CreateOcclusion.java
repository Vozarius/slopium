package dev.slopium.visibility;
import dev.slopium.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
/** All Create consumers read the same two-mode snapshot; no geometry work in render/tick calls. */
public final class CreateOcclusion {
    private CreateOcclusion() {}
    public static boolean hidden(BlockPos target,Vec3 eye,boolean any) {
        return Boolean.TRUE.equals(decision(target,eye,any,true));
    }
    public static Boolean decision(BlockPos target,Vec3 eye,boolean any,boolean queued) {
        Boolean result=VisibilityPool.read(Vec3.atCenterOf(target),eye,true,any);
        if(SlopiumConfig.get().diagnostics) {
            SlopiumStats.createOcclusionChecks++;
            if(result==null) SlopiumStats.createOcclusionDeferred++;
            else SlopiumStats.createOcclusionCacheHits++;
        }
        return result;
    }
}

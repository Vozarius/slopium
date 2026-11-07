package dev.slopium.visibility;
import dev.engine_room.flywheel.api.visual.*;
import dev.slopium.client.*;
import dev.slopium.mixin.VisualBlockEntityAccess;
/** Worker reads: one selection lookup, published visibility flag and camera distance. No traces. */
public final class VisualUpdatePolicy {
    public static boolean allow(Visual visual,DynamicVisual.Context context){
        var c=SlopiumConfig.get();if(!c.enabled)return true;
        if(!c.visibleSections&&!c.freezeCreateAnimations&&!c.occludeCreateAnimations
                &&(context==null||!c.throttleCreateVisuals&&!c.throttleFlywheelDistance))return true;
        var owner=OwnedInstances.get(visual);
        for(int n=0;n<8&&visual instanceof WrappedVisualAccess wrapper;n++){var next=wrapper.slopium$delegateVisual();if(next==null||next==visual)return true;visual=next;}
        var be=owner!=null?owner.block:visual instanceof VisualBlockEntityAccess access?access.slopium$getBlockEntity():null;
        if(!ProcessedBlocks.allows(be))return true;
        if(be.getLevel()==null||!be.getLevel().isClientSide||(be instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smart&&smart.isVirtual()))return true;
        var state=owner!=null?owner:visual instanceof VisualOcclusionState s?s:null;
        if((c.freezeCreateAnimations||c.occludeCreateAnimations)&&state!=null&&state.slopium$isOccluded(Slopium.frame()))return skipped(c);
        if(context==null)return true;
        if(!c.throttleCreateVisuals&&!c.throttleFlywheelDistance)return true;
        if(be.getLevel()==null||!be.getLevel().isClientSide||SableGuard.protects(be.getLevel(),be.getBlockPos()))return true;
        double distance=be.getBlockPos().distToCenterSqr(context.camera().getPosition());int interval=1;
        if(c.throttleCreateVisuals&&distance>Math.pow(c.createVisualDistance+4.0,2))interval=c.createVisualInterval;
        if(c.throttleFlywheelDistance&&distance>Math.pow(c.flywheelFullRateDistance+4.0,2))interval=Math.max(interval,c.flywheelFrameInterval);
        return interval<=1||Math.floorMod(Slopium.frame()+be.getBlockPos().asLong(),interval)==0||skipped(c);
    }
    private static boolean skipped(SlopiumConfig c){if(c.diagnostics)SlopiumStats.CREATE_VISUALS_SKIPPED.increment();return false;}
}

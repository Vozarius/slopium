package dev.slopium.visibility;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.slopium.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CreateWallControl {
    private CreateWallControl() {}
    public static boolean hidden(BlockEntity be) { return hidden(be, false); }
    public static boolean hidden(BlockEntity be, boolean anyBlock) {
        return Boolean.TRUE.equals(decision(be,anyBlock,true));
    }
    public static Boolean decision(BlockEntity be, boolean anyBlock) { return decision(be,anyBlock,false); }
    private static Boolean decision(BlockEntity be, boolean anyBlock, boolean queued) {
        if(!ProcessedBlocks.allows(be))return false;
        return decisionSelected(be,anyBlock);
    }
    private static Boolean decisionSelected(BlockEntity be,boolean anyBlock) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || (be instanceof SmartBlockEntity smart && smart.isVirtual())
                || be.getLevel() == null || !be.getLevel().isClientSide || be.getLevel() != mc.level
                || mc.player == null) return false;
        var pos = be.getBlockPos();
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) return false;
        if(outsideCameraSelected(be)) return true;
        return CreateOcclusion.decision(((TrackedBlockEntity)be).slopium$visibility(), mc.gameRenderer.getMainCamera().getPosition(), anyBlock);
    }
    public static boolean outsideCamera(BlockEntity be) {
        return SlopiumConfig.get().cameraCulling && ProcessedBlocks.allows(be) && outsideCameraSelected(be);
    }
    private static boolean outsideCameraSelected(BlockEntity be) {
        var mc=Minecraft.getInstance();
        if(!SlopiumConfig.get().cameraCulling||!VisibilityPool.cameraReady()||!mc.isSameThread()||be.getLevel()!=mc.level||mc.level==null
            ||SableGuard.protects(mc.level,be.getBlockPos())) return false;
        var handle=((TrackedBlockEntity)be).slopium$visibility();
        return VisibilityPool.outsideCamera(handle,bounds(be));
    }
    private static net.minecraft.world.phys.AABB bounds(BlockEntity be){
        var handle=((TrackedBlockEntity)be).slopium$visibility();
        if(handle.boundsFrame!=Slopium.frame()){
            var renderer=Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
            var bounds=renderer==null?null:renderer.getRenderBoundingBox(be);
            if(!java.util.Objects.equals(bounds,handle.rawBounds)){
                handle.rawBounds=bounds;handle.bounds=bounds==null?null:bounds.inflate(.25);
            }
            handle.boundsFrame=Slopium.frame();
        }
        return handle.bounds;
    }
    /** Model hiding requires current bounds proof; CPU animation/tick cadence remains separate. */
    public static Boolean modelDecision(BlockEntity be,boolean anyBlock,boolean camera){
        if(!ProcessedBlocks.allows(be))return false;
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()||be.getLevel()==null||be.getLevel()!=mc.level||!be.getLevel().isClientSide||mc.player==null
            ||(be instanceof SmartBlockEntity smart&&smart.isVirtual())||SableGuard.protects(mc.level,be.getBlockPos()))return false;
        if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&hit.getBlockPos().equals(be.getBlockPos()))return false;
        if(camera&&outsideCameraSelected(be))return true;
        var handle=((TrackedBlockEntity)be).slopium$visibility();
        var eye=mc.gameRenderer.getMainCamera().getPosition();
        if(SableGuard.protects(mc.level,net.minecraft.core.BlockPos.containing(eye)))return false;
        Boolean answer=CreateOcclusion.decision(handle,eye,anyBlock);
        if(!Boolean.TRUE.equals(answer))return answer;
        return VisibilityPool.provesHidden(handle,eye,anyBlock,bounds(be));
    }
    public static boolean skipTick(BlockEntity be, SlopiumConfig c) {
        return ProcessedBlocks.allows(be) && skipTickSelected(be,c);
    }
    public static boolean skipTickSelected(BlockEntity be,SlopiumConfig c) {
        var level = be.getLevel();
        if (level == null || !level.isClientSide) return false;
        var mc = Minecraft.getInstance();
        var tracked = (TrackedBlockEntity)be;
        if (mc.player == null || mc.isPaused() || !((CreateState)be).slopium$initialized()
                || be.getBlockPos().distToCenterSqr(mc.player.position()) <= 16 || !Boolean.TRUE.equals(decisionSelected(be, c.tickAnyBlock))) {
            tracked.slopium$resetHiddenState(); return false;
        }
        boolean skip = tracked.slopium$tickGate().skip(level.getGameTime(),true,c.hiddenGraceTicks,c.hiddenTickInterval,be.getBlockPos().hashCode());
        if (skip && c.diagnostics) SlopiumStats.wallCreateTicksSkipped++;
        return skip;
    }
}

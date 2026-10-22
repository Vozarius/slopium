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
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || !(be instanceof SmartBlockEntity smart) || smart.isVirtual()
                || be.getLevel() == null || !be.getLevel().isClientSide || be.getLevel() != mc.level
                || mc.player == null) return false;
        var pos = be.getBlockPos();
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) return false;
        if(outsideCamera(be)) return true;
        return CreateOcclusion.decision(pos, mc.gameRenderer.getMainCamera().getPosition(), anyBlock, queued);
    }
    public static boolean outsideCamera(BlockEntity be) {
        var mc=Minecraft.getInstance();
        if(!SlopiumConfig.get().cameraCulling||!mc.isSameThread()||be.getLevel()!=mc.level||mc.level==null
            ||SableGuard.protects(mc.level,be.getBlockPos())) return false;
        var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(be);
        return renderer!=null && VisibilityPool.outsideCamera(net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()),true,renderer.getRenderBoundingBox(be).inflate(.25));
    }
    public static boolean skipTick(BlockEntity be, SlopiumConfig c) {
        var level = be.getLevel();
        if (level == null || !level.isClientSide) return false;
        var mc = Minecraft.getInstance();
        var tracked = (TrackedBlockEntity)be;
        if (mc.player == null || mc.isPaused() || !((CreateState)be).slopium$initialized()
                || be.getBlockPos().distToCenterSqr(mc.player.position()) <= 16 || !hidden(be, c.tickAnyBlock)) {
            tracked.slopium$resetHiddenState(); return false;
        }
        boolean skip = tracked.slopium$tickGate().skip(level.getGameTime(),true,c.hiddenGraceTicks,c.hiddenTickInterval,be.getBlockPos().hashCode());
        if (skip && c.diagnostics) SlopiumStats.wallCreateTicksSkipped++;
        return skip;
    }
}

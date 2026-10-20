package dev.slopium.visibility;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.slopium.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public final class CreateWallControl {
    private CreateWallControl() {}
    public static boolean hidden(BlockEntity be) { return hidden(be, false); }
    public static boolean hidden(BlockEntity be, boolean anyBlock) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread() || !(be instanceof SmartBlockEntity smart) || smart.isVirtual()
                || be.getLevel() == null || !be.getLevel().isClientSide || be.getLevel() != mc.level
                || mc.screen != null || mc.player == null) return false;
        var pos = be.getBlockPos();
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getBlockPos().equals(pos)) return false;
        var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
        if (renderer == null) return false;
        // Renderer bounds matter: pipes, belts and panel connections can extend beyond a block.
        AABB bounds = renderer.getRenderBoundingBox(be).inflate(.25);
        return WallOcclusion.hidden(bounds, mc.gameRenderer.getMainCamera().getPosition(), anyBlock);
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

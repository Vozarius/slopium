package dev.slopium.visibility;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Broad experimental client cadence, not a claim that arbitrary behaviours are cosmetic. */
public final class CreateDistanceControl {
    private CreateDistanceControl() {}
    public static boolean skip(BlockEntity be, SlopiumConfig c) {
        var level = be.getLevel();
        if (level == null || !level.isClientSide) return false;
        var client = Minecraft.getInstance();
        var tracked = (TrackedBlockEntity) be;
        var gate = tracked.slopium$tickGate();
        var pos = be.getBlockPos();
        double radius = RenderBudget.radius(c.createTickDistance) + 4.0;
        if (level != client.level || client.player == null || client.isPaused() || client.screen != null
                || ((SmartBlockEntity) be).isVirtual() || !((CreateState) be).slopium$initialized()
                || pos.distToCenterSqr(client.gameRenderer.getMainCamera().getPosition()) <= radius * radius
                || pos.distToCenterSqr(client.player.position()) <= 64.0
                || (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getBlockPos().equals(pos))
                || SableGuard.protects(level, pos)) {
            gate.resetHidden();
            return false;
        }
        boolean skip = gate.skip(level.getGameTime(), true, c.hiddenGraceTicks, c.createTickInterval, pos.hashCode());
        if (skip && c.diagnostics) SlopiumStats.distanceCreateTicksSkipped++;
        return skip;
    }
}

package dev.slopium.visibility;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.slopium.client.SlopiumStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Diagnostic only. Never used for server worlds or virtual render worlds. */
public final class CreateFreezeControl {
    private CreateFreezeControl() {}
    public static boolean isCandidate(BlockEntity be) { return be instanceof SmartBlockEntity; }
    public static boolean skip(BlockEntity be, boolean diagnostics) {
        var level = be.getLevel();
        if (level == null || !level.isClientSide) return false;
        if (level != Minecraft.getInstance().level || ((SmartBlockEntity) be).isVirtual()
                || !((CreateState) be).slopium$initialized() || SableGuard.protects(level, be.getBlockPos())) return false;
        if (diagnostics) SlopiumStats.frozenCreateTicks++;
        return true;
    }
}

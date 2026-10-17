package dev.slopium.visibility;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.tr7zw.entityculling.versionless.access.Cullable;
import dev.tr7zw.entityculling.EntityCullingModBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

public final class CreateTickControl {
    // Exact classes: a new subclass/addon must be audited before inheriting this policy.
    // Belts, trains, bearings, ropes and contraption controllers are intentionally absent.
    private static final Set<String> CLASSES = Set.of(
        "com.simibubi.create.content.kinetics.base.KineticBlockEntity",
        "com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity",
        "com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity",
        "com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity",
        "com.simibubi.create.content.fluids.tank.FluidTankBlockEntity",
        "com.simibubi.create.content.logistics.funnel.FunnelBlockEntity",
        "com.simibubi.create.content.logistics.chute.ChuteBlockEntity"
    );
    private static final ClassValue<Boolean> ELIGIBLE = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) { return CLASSES.contains(type.getName()); }
    };
    private CreateTickControl() {}

    public static boolean skip(BlockEntity be) {
        if (!ELIGIBLE.get(be.getClass()) || !(be instanceof SmartBlockEntity smart)
                || !(be instanceof CreateState state) || !(be instanceof TrackedBlockEntity tracked)
                || !(be instanceof Cullable cullable)) return false;
        var level = be.getLevel();
        var client = Minecraft.getInstance();
        if (level == null || !level.isClientSide || level != client.level || client.player == null
                || client.isPaused() || smart.isVirtual() || !state.slopium$initialized()) return false;
        SlopiumConfig config = SlopiumConfig.get();
        var pos = be.getBlockPos();
        var camera = client.gameRenderer.getMainCamera();
        double distance = camera.getPosition().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        var culling = EntityCullingModBase.instance;
        boolean hidden = EntityCullingModBase.enabled && culling != null
            && !culling.config.skipBlockEntityCulling
            && !cullable.isForcedVisible() && cullable.isCulled()
            && distance > (double) config.protectedDistance * config.protectedDistance;
        if (hidden && (culling.blockEntityWhitelist.contains(be.getType()) || culling.isDynamicWhitelisted(be))) hidden = false;
        if (hidden && SableGuard.protects(level, pos)) hidden = false;
        boolean skip = tracked.slopium$tickGate().skip(level.getGameTime(), hidden,
            config.hiddenGraceTicks, config.hiddenTickInterval, pos.hashCode());
        if (skip && config.diagnostics) SlopiumStats.hiddenTicksSkipped++;
        return skip;
    }
}


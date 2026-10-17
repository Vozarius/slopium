package dev.slopium.visibility;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
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

    /** Evaluated once per ticker binding, never for each tick. */
    public static boolean isCandidate(BlockEntity be) {
        return ELIGIBLE.get(be.getClass()) && be instanceof SmartBlockEntity
            && be instanceof CreateState && be instanceof TrackedBlockEntity && be instanceof Cullable;
    }

    /** Caller has already cached isCandidate for this final block-entity reference. */
    public static boolean skipEligible(BlockEntity be, SlopiumConfig config) {
        var level = be.getLevel();
        if (level == null || !level.isClientSide) return false;
        var client = Minecraft.getInstance();
        var smart = (SmartBlockEntity) be;
        var tracked = (TrackedBlockEntity) be;
        var cullable = (Cullable) be;
        if (config.diagnostics) SlopiumStats.eligibleTicksChecked++;
        if (level != client.level || client.player == null || client.isPaused()
                || smart.isVirtual() || !((CreateState) be).slopium$initialized()) {
            tracked.slopium$resetHiddenState();
            if (config.diagnostics) SlopiumStats.tickNotReady++;
            return false;
        }
        var culling = EntityCullingModBase.instance;
        if (!EntityCullingModBase.enabled || culling == null || culling.config.skipBlockEntityCulling
                || cullable.isForcedVisible() || !cullable.isCulled()
                || culling.blockEntityWhitelist.contains(be.getType()) || culling.isDynamicWhitelisted(be)) {
            tracked.slopium$resetHiddenState();
            if (config.diagnostics) SlopiumStats.tickVisibleOrExcluded++;
            return false;
        }
        var pos = be.getBlockPos();
        var camera = client.gameRenderer.getMainCamera();
        double distance = camera.getPosition().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance <= (double) config.protectedDistance * config.protectedDistance) {
            tracked.slopium$resetHiddenState();
            if (config.diagnostics) SlopiumStats.tickNear++;
            return false;
        }
        if (SableGuard.protects(level, pos)) {
            tracked.slopium$resetHiddenState();
            if (config.diagnostics) SlopiumStats.tickSableProtected++;
            return false;
        }
        if (config.diagnostics) SlopiumStats.hiddenTicksChecked++;
        boolean skip = tracked.slopium$tickGate().skip(level.getGameTime(), true,
            config.hiddenGraceTicks, config.hiddenTickInterval, pos.hashCode());
        if (skip && config.diagnostics) SlopiumStats.hiddenTicksSkipped++;
        return skip;
    }
}


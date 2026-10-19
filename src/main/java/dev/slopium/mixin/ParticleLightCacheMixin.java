package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;

/** Only the base light lookup: subclass brightness/age formulas remain outside the cache. */
@Mixin(Particle.class)
public abstract class ParticleLightCacheMixin {
    @Shadow protected ClientLevel level;
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Unique private long slopium$lightTick = Long.MIN_VALUE;
    @Unique private long slopium$lightPosition;
    @Unique private int slopium$light;
    @WrapMethod(method = "getLightColor")
    private int slopium$cachedLight(float partialTick, Operation<Integer> original) {
        var c = SlopiumConfig.get();
        if (!c.enabled || !c.cacheParticleLight || level == null) {
            slopium$lightTick = Long.MIN_VALUE;
            return original.call(partialTick);
        }
        long tick = level.getGameTime();
        long position = BlockPos.asLong(net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(y), net.minecraft.util.Mth.floor(z));
        if (tick == slopium$lightTick && position == slopium$lightPosition) {
            if (c.diagnostics) SlopiumStats.particleLightHits++;
            return slopium$light;
        }
        int result = original.call(partialTick);
        slopium$lightTick = tick;
        slopium$lightPosition = position;
        return slopium$light = result;
    }
}

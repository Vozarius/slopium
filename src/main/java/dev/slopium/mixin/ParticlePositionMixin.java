package dev.slopium.mixin;

import dev.slopium.visibility.ParticlePosition;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public abstract class ParticlePositionMixin implements ParticlePosition {
    @Shadow protected double xo, yo, zo, x, y, z;

    @Override public Vec3 slopium$renderPosition(float partialTick) {
        return new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z));
    }
}

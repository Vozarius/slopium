package dev.slopium.mixin;

import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.visibility.EmissionBudget;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleBudgetMixin {
    @Unique private final EmissionBudget slopium$emissions = new EmissionBudget();
    @Inject(method = {"tick", "clearParticles"}, at = @At("HEAD"))
    private void slopium$resetBudget(CallbackInfo ci) { slopium$emissions.reset(); }
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void slopium$limitEmission(CallbackInfo ci) {
        var c = SlopiumConfig.get();
        if (!c.enabled || !c.particleBudget || c.disableParticles) { slopium$emissions.reset(); return; }
        if (!slopium$emissions.allow(c.particlesPerTick)) {
            if (c.diagnostics) SlopiumStats.particleBudgetSkipped++;
            ci.cancel();
        }
    }
}

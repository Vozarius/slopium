package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.visibility.RenderBudget;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public abstract class ParticleRangeMixin {
    @WrapWithCondition(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
    private boolean slopium$particleRange(Particle particle, VertexConsumer buffer, Camera camera, float partialTick, @com.llamalad7.mixinextras.sugar.Local(argsOnly=true) net.minecraft.client.renderer.culling.Frustum nativeFrustum) {
        var c = SlopiumConfig.get();
        if (!c.enabled || (!c.particleRange && !c.occludeParticles && !c.cameraCulling)) return true;
        var p = camera.getPosition();
        if(nativeFrustum==null && dev.slopium.visibility.VisibilityPool.outsideCamera(
            ((dev.slopium.visibility.ParticlePosition)particle).slopium$renderPosition(partialTick),false,particle.getRenderBoundingBox(partialTick).inflate(.1))) return false;
        boolean skip = c.particleRange && RenderBudget.outside(particle.getRenderBoundingBox(partialTick), p.x, p.y, p.z,
            RenderBudget.radius(c.particleRenderDistance));
        if (skip && c.diagnostics) SlopiumStats.particleDrawsSkipped++;
        if (skip) return false;
        if (c.occludeParticles && dev.slopium.visibility.ParticleOcclusion.hidden(
                ((dev.slopium.visibility.ParticlePosition)particle).slopium$renderPosition(partialTick), p, c.opaqueGlassParticles)) {
            if (c.diagnostics) SlopiumStats.wallParticlesSkipped++;
            return false;
        }
        return true;
    }
}

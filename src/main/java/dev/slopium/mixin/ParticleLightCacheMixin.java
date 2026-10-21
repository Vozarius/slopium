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
    @Unique private static final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap slopium$sharedLight=new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    @Unique private static java.lang.ref.WeakReference<Object> slopium$sharedWorld=new java.lang.ref.WeakReference<>(null);
    @Unique private static long slopium$sharedFrame=Long.MIN_VALUE, slopium$sharedEpoch, slopium$sharedConfig;
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="getLightColor",at=@org.spongepowered.asm.mixin.injection.At(
        value="INVOKE",target="Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int slopium$sharedBlockLight(net.minecraft.world.level.BlockAndTintGetter world,BlockPos pos,Operation<Integer> original) {
        if(!net.minecraft.client.Minecraft.getInstance().isSameThread()) return original.call(world,pos);
        var c=SlopiumConfig.get();
        if(!c.enabled||!c.cacheParticleLight) {
            slopium$sharedFrame=Long.MIN_VALUE;return original.call(world,pos);
        }
        long frame=dev.slopium.client.Slopium.frame(), epoch=dev.slopium.visibility.WallOcclusion.epoch(), config=dev.slopium.client.Slopium.configRevision();
        if(world!=slopium$sharedWorld.get()||frame!=slopium$sharedFrame||epoch!=slopium$sharedEpoch||config!=slopium$sharedConfig) {
            slopium$sharedLight.clear();slopium$sharedLight.defaultReturnValue(Integer.MIN_VALUE);
            if(slopium$sharedWorld.get()!=world) slopium$sharedWorld=new java.lang.ref.WeakReference<>(world);
            slopium$sharedFrame=frame;slopium$sharedEpoch=epoch;slopium$sharedConfig=config;
        }
        long key=pos.asLong();int cached=slopium$sharedLight.get(key);
        if(cached!=Integer.MIN_VALUE) {
            if(c.diagnostics) SlopiumStats.particleSharedLightHits++;
            return cached;
        }
        int result=original.call(world,pos);
        if(slopium$sharedLight.size()<4096) slopium$sharedLight.put(key,result);
        return result;
    }
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

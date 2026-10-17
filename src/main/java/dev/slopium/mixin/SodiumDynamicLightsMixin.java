package dev.slopium.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.light.LightGrid;
import dev.slopium.light.LightSnapshot;
import dev.slopium.light.LightIndexControl;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.DynamicLightSource;
import toni.sodiumdynamiclights.SodiumDynamicLights;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(value = SodiumDynamicLights.class, remap = false)
public abstract class SodiumDynamicLightsMixin implements LightIndexControl {
    @Shadow @Final private Set<DynamicLightSource> dynamicLightSources;
    @Shadow @Final private ReentrantReadWriteLock lightSourcesLock;
    @Unique private volatile LightSnapshot slopium$snapshot;
    @Unique private final Long2DoubleOpenHashMap slopium$queryCache = new Long2DoubleOpenHashMap();
    @Unique private LightSnapshot slopium$cacheSnapshot;
    @Unique private final AtomicLong slopium$sourcesRevision = new AtomicLong();

    @Override public void slopium$clearCaches() {
        slopium$sourcesRevision.incrementAndGet();
        slopium$snapshot = null;
        slopium$cacheSnapshot = null;
        slopium$queryCache.clear();
    }

    @Inject(method = "updateAll", at = @At("RETURN"))
    private void slopium$indexLights(LevelRenderer renderer, CallbackInfo ci) {
        SlopiumConfig config = SlopiumConfig.get();
        Level level = Minecraft.getInstance().level;
        if (!config.enabled || !config.indexedLighting || level == null || !RenderSystem.isOnRenderThread()) {
            slopium$snapshot = null;
            return;
        }
        LightSnapshot previous = slopium$snapshot;
        long frame = Slopium.frame(), revision = Slopium.configRevision();
        if (previous != null && previous.frame() == frame && previous.revision() == revision && previous.level() == level
            && previous.sourcesRevision() == slopium$sourcesRevision.get()) return;
        long start = config.diagnostics ? System.nanoTime() : 0;
        var lights = new ArrayList<LightGrid.Light>();
        long sourcesRevision = slopium$sourcesRevision.get();
        lightSourcesLock.readLock().lock();
        try {
            for (DynamicLightSource source : dynamicLightSources) {
                if (!slopium$allowed(source, config)) continue;
                int luminance = source.sdl$getLuminance();
                if (luminance <= 0) continue;
                double x = source.sdl$getDynamicLightX(), y = source.sdl$getDynamicLightY(), z = source.sdl$getDynamicLightZ();
                if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
                    lights.add(new LightGrid.Light(x, y, z, luminance));
                }
            }
        } finally {
            lightSourcesLock.readLock().unlock();
        }
        // Publication is atomic; chunk workers only read primitive immutable snapshots.
        slopium$snapshot = new LightSnapshot(new LightGrid(lights), level, frame, revision, sourcesRevision);
        SlopiumStats.indexedSources = lights.size();
        if (config.diagnostics) SlopiumStats.indexBuildNanos = System.nanoTime() - start;
    }

    @Inject(method = {"addLightSource", "removeLightSource", "clearLightSources", "removeLightSources"}, at = @At("RETURN"))
    private void slopium$invalidateSources(CallbackInfo ci) {
        slopium$sourcesRevision.incrementAndGet();
        slopium$snapshot = null;
    }

    @Inject(method = "getDynamicLightLevel", at = @At("HEAD"), cancellable = true)
    private void slopium$sample(BlockPos pos, CallbackInfoReturnable<Double> cir) {
        SlopiumConfig config = SlopiumConfig.get();
        if (!config.enabled) return;
        LightSnapshot snapshot = slopium$snapshot;
        boolean renderThread = RenderSystem.isOnRenderThread();
        if (config.indexedLighting && snapshot != null && snapshot.level() == Minecraft.getInstance().level
                && snapshot.frame() == Slopium.frame() && snapshot.revision() == Slopium.configRevision()
                && snapshot.sourcesRevision() == slopium$sourcesRevision.get()) {
            if (renderThread && config.diagnostics) SlopiumStats.lightQueries++;
            // With Create/Sable light disabled, many frames have no sources.
            // Every position is then zero; a per-position hash cache only adds work.
            if (snapshot.grid().size() == 0) {
                cir.setReturnValue(0.0);
                return;
            }
            boolean packable = pos.getX() >= -33554432 && pos.getX() < 33554432
                && pos.getZ() >= -33554432 && pos.getZ() < 33554432 && pos.getY() >= -2048 && pos.getY() < 2048;
            if (renderThread && config.lightQueryCache && packable) {
                if (slopium$cacheSnapshot != snapshot) {
                    slopium$queryCache.clear();
                    slopium$queryCache.defaultReturnValue(Double.NaN);
                    slopium$cacheSnapshot = snapshot;
                }
                long key = pos.asLong();
                double value = slopium$queryCache.get(key);
                if (!Double.isNaN(value)) {
                    if (config.diagnostics) SlopiumStats.lightCacheHits++;
                    cir.setReturnValue(value);
                    return;
                }
                value = snapshot.grid().sample(pos.getX(), pos.getY(), pos.getZ());
                if (slopium$queryCache.size() < 16384) slopium$queryCache.put(key, value);
                cir.setReturnValue(value);
            } else {
                cir.setReturnValue(snapshot.grid().sample(pos.getX(), pos.getY(), pos.getZ()));
            }
        } else if (!config.createLight || !config.sableLight) {
            // Before a fresh snapshot exists, preserve toggles using the original calculation.
            double result = 0;
            lightSourcesLock.readLock().lock();
            try {
                for (DynamicLightSource source : dynamicLightSources) {
                    if (slopium$allowed(source, config)) result = SodiumDynamicLights.maxDynamicLightLevel(pos, source, result);
                }
            } finally {
                lightSourcesLock.readLock().unlock();
            }
            cir.setReturnValue(Math.clamp(result, 0, 15));
        }
    }

    @Unique private static boolean slopium$allowed(DynamicLightSource source, SlopiumConfig config) {
        String name = source.getClass().getName();
        if (name.startsWith("top.leonx.dynlight.sodium.sable.")) return config.createLight && config.sableLight;
        return !name.startsWith("top.leonx.dynlight.") || config.createLight;
    }
}


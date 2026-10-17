package dev.slopium.smoke;

import com.mojang.logging.LogUtils;
import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import dev.slopium.light.LightGrid;
import dev.slopium.light.LightIndexControl;
import dev.slopium.light.LightSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import toni.sodiumdynamiclights.DynamicLightSource;
import toni.sodiumdynamiclights.SodiumDynamicLights;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/** Test-only assertions against the actual transformed target and original SDL formula. */
final class SmokeLightChecks {
    static void run(Object instance) throws ReflectiveOperationException {
        var sodium = (SodiumDynamicLights) instance;
        var lights = new ArrayList<LightGrid.Light>();
        var sources = new ArrayList<DynamicLightSource>();
        var random = new Random(100);
        for (int i = 0; i < 64; i++) {
            var light = new LightGrid.Light(random.nextDouble(-20, 20), random.nextDouble(-20, 20),
                random.nextDouble(-20, 20), random.nextInt(1, 20));
            lights.add(light);
            sources.add((DynamicLightSource) Proxy.newProxyInstance(DynamicLightSource.class.getClassLoader(),
                new Class<?>[]{DynamicLightSource.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "sdl$getDynamicLightX" -> light.x();
                    case "sdl$getDynamicLightY" -> light.y();
                    case "sdl$getDynamicLightZ" -> light.z();
                    case "sdl$getLuminance" -> light.luminance();
                    default -> throw new AssertionError("Unexpected source method: " + method.getName());
                }));
        }
        var grid = new LightGrid(lights);
        for (int i = 0; i < 256; i++) {
            var pos = new BlockPos(random.nextInt(-25, 26), random.nextInt(-25, 26), random.nextInt(-25, 26));
            double expected = 0;
            for (var source : sources) expected = SodiumDynamicLights.maxDynamicLightLevel(pos, source, expected);
            assertClose(Math.clamp(expected, 0, 15), grid.sample(pos.getX(), pos.getY(), pos.getZ()));
        }

        var snapshotField = SodiumDynamicLights.class.getDeclaredField("slopium$snapshot");
        var generationField = SodiumDynamicLights.class.getDeclaredField("slopium$sourcesRevision");
        snapshotField.setAccessible(true); generationField.setAccessible(true);
        long generation = ((AtomicLong) generationField.get(sodium)).get();
        var config = SlopiumConfig.get();
        boolean enabled = config.enabled, index = config.indexedLighting, cache = config.lightQueryCache, diagnostics = config.diagnostics;
        try {
            config.enabled = true; config.indexedLighting = true; config.lightQueryCache = true; config.diagnostics = true;
            SlopiumStats.reset();
            // No world is opened by this test. Inject a known snapshot solely to
            // exercise the transformed query method, cache and invalidation.
            var point = new LightGrid(java.util.List.of(new LightGrid.Light(0.5, 0.5, 0.5, 15)));
            snapshotField.set(sodium, new LightSnapshot(point, Minecraft.getInstance().level,
                Slopium.frame(), Slopium.configRevision(), generation));
            assertClose(15, sodium.getDynamicLightLevel(BlockPos.ZERO));
            assertClose(15, sodium.getDynamicLightLevel(BlockPos.ZERO));
            if (SlopiumStats.lightQueries != 2 || SlopiumStats.lightCacheHits != 1) throw new AssertionError("Frame cache did not run");
            snapshotField.set(sodium, new LightSnapshot(new LightGrid(java.util.List.of()), Minecraft.getInstance().level,
                Slopium.frame(), Slopium.configRevision(), generation));
            assertClose(0, sodium.getDynamicLightLevel(BlockPos.ZERO));
            sodium.clearLightSources();
            if (snapshotField.get(sodium) != null) throw new AssertionError("Source mutation did not invalidate snapshot");
            assertClose(0, sodium.getDynamicLightLevel(BlockPos.ZERO));
        } finally {
            ((LightIndexControl) sodium).slopium$clearCaches();
            config.enabled = enabled; config.indexedLighting = index; config.lightQueryCache = cache; config.diagnostics = diagnostics;
            SlopiumStats.reset();
        }
        LogUtils.getLogger().info("SLOPIUM_SMOKE: original SDL formula, injected queries, frame cache and source invalidation passed");
    }

    private static void assertClose(double expected, double actual) {
        if (Math.abs(expected - actual) > 1e-12) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}

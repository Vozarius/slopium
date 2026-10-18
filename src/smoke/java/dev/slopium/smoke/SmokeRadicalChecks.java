package dev.slopium.smoke;

import dev.slopium.client.*;
import dev.slopium.visibility.EmissionBudget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

final class SmokeRadicalChecks {
    static void run() throws ReflectiveOperationException {
        var c = SlopiumConfig.get();
        var saved = new java.util.HashMap<java.lang.reflect.Field, Object>();
        for (var f : SlopiumConfig.class.getFields()) saved.put(f, f.get(c));
        try {
            c.enabled = c.diagnostics = true; c.disableParticles = false;
            c.particleBudget = true; c.particlesPerTick = 0;
            var engine = Minecraft.getInstance().particleEngine;
            long before = SlopiumStats.particleBudgetSkipped;
            engine.add(null); // Must return before the original queue can touch this sentinel.
            if (SlopiumStats.particleBudgetSkipped != before + 1) throw new AssertionError("Particle admission hook missing");
            var hook = Arrays.stream(engine.getClass().getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$limitEmission")).findFirst().orElseThrow();
            hook.setAccessible(true);
            var reset = Arrays.stream(engine.getClass().getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$resetBudget")).findFirst().orElseThrow();
            reset.setAccessible(true); reset.invoke(engine, new CallbackInfo("reset", false));
            c.particlesPerTick = 2;
            for (int i = 0; i < 3; i++) {
                var ci = new CallbackInfo("add", true); hook.invoke(engine, ci);
                if (ci.isCancelled() != (i == 2)) throw new AssertionError("Particle quota failed");
            }
            c.enabled = false;
            var disabled = new CallbackInfo("add", true); hook.invoke(engine, disabled);
            if (disabled.isCancelled()) throw new AssertionError("Particle global off failed");
            c.enabled = true;
            var after = new CallbackInfo("add", true); hook.invoke(engine, after);
            if (after.isCancelled()) throw new AssertionError("Particle reenable retained stale budget");
            var type = Class.forName("net.minecraft.client.renderer.texture.SpriteContents$InterpolationData");
            Object interpolation = allocate(type);
            var blend = Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().equals("uploadInterpolatedFrame")).findFirst().orElseThrow();
            blend.setAccessible(true);
            c.disableTextureInterpolation = true;
            before = SlopiumStats.textureInterpolationsSkipped;
            blend.invoke(interpolation, 0, 0, null);
            if (SlopiumStats.textureInterpolationsSkipped != before + 1) throw new AssertionError("Interpolation entry not cut");
            var cut = Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$discreteFrames")).findFirst().orElseThrow();
            cut.setAccessible(true); c.enabled = false;
            disabled = new CallbackInfo("blend", true); cut.invoke(interpolation, disabled);
            if (disabled.isCancelled()) throw new AssertionError("Interpolation global off failed");
            c.enabled = true;
            if (Slopium.loaded("create")) SmokeCreateVisualChecks.run();
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: radical particle admission/reset, interpolation on/off and optional Create visual hooks passed");
        } finally {
            for (var e : saved.entrySet()) e.getKey().set(c, e.getValue());
            SlopiumStats.reset();
        }
    }
    static Object allocate(Class<?> type) throws ReflectiveOperationException {
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return ((sun.misc.Unsafe) f.get(null)).allocateInstance(type);
    }
}

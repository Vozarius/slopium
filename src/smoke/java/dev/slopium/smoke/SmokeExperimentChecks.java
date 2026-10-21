package dev.slopium.smoke;

import com.mojang.logging.LogUtils;
import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exercises transformed entry points and verifies opt-in/global-off cancellation. */
final class SmokeExperimentChecks {
    static void run() throws ReflectiveOperationException {
        var c = SlopiumConfig.get();
        boolean enabled = c.enabled, diagnostics = c.diagnostics;
        String[] flags = {"disableBlockEntityRendering", "disableParticles", "disableAmbientEffects", "disableFlywheelRendering"};
        boolean[] saved = new boolean[flags.length];
        for (int i = 0; i < flags.length; i++) saved[i] = SlopiumConfig.class.getField(flags[i]).getBoolean(c);
        try {
            c.enabled = c.diagnostics = true;
            for (String flag : flags) SlopiumConfig.class.getField(flag).setBoolean(c, true);
            var mc = Minecraft.getInstance();
            long before = SlopiumStats.blockEntityDrawsSkipped;
            mc.getBlockEntityRenderDispatcher().render(null, 0, null, null);
            if (SlopiumStats.blockEntityDrawsSkipped != before + 1) throw new AssertionError("BE draw cut not reached");
            before = SlopiumStats.particleCallsSkipped;
            mc.particleEngine.add(null);
            mc.particleEngine.render(null, null, 0);
            if (SlopiumStats.particleCallsSkipped != before + 2) throw new AssertionError("Particle entry cuts not reached");
            checkHandlers(mc.getBlockEntityRenderDispatcher(), "disableBlockEntityRendering");
            checkHandlers(mc.particleEngine, "disableParticles");
            // Uninitialized fixtures are used only for injected HEAD guards. No world or GL body may run.
            checkHandlers(allocate("net.minecraft.client.multiplayer.ClientLevel"), "disableAmbientEffects");
            if (Slopium.loaded("flywheel")) {
                for (String name : new String[]{
                    "dev.engine_room.flywheel.backend.engine.instancing.InstancedDraw",
                    "dev.engine_room.flywheel.backend.engine.indirect.IndirectCullingGroup",
                    "dev.engine_room.flywheel.backend.engine.EngineImpl"}) {
                    Object target = allocate(name);
                    checkHandlers(target, "disableFlywheelRendering");
                    c.enabled = c.disableFlywheelRendering = true;
                    for (var method : target.getClass().getDeclaredMethods()) {
                        if (!java.util.Set.of("render", "renderOne", "submitSolid", "submitTransparent", "renderCrumbling").contains(method.getName())) continue;
                        // EngineImpl.render must run normally; only its crumbling entry is cut.
                        if (name.endsWith("EngineImpl") && method.getName().equals("render")) continue;
                        before = SlopiumStats.flywheelDrawsSkipped;
                        method.invoke(target, new Object[method.getParameterCount()]);
                        if (SlopiumStats.flywheelDrawsSkipped != before + 1) throw new AssertionError("Flywheel draw cut not reached: " + method);
                    }
                }
            }
            LogUtils.getLogger().info("SLOPIUM_SMOKE: experimental draw/particle entry points and all opt-in/global-off guards passed");
        } finally {
            c.enabled = enabled; c.diagnostics = diagnostics;
            for (int i = 0; i < flags.length; i++) SlopiumConfig.class.getField(flags[i]).setBoolean(c, saved[i]);
            SlopiumStats.reset();
        }
    }

    private static Object allocate(String name) throws ReflectiveOperationException {
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return ((sun.misc.Unsafe) field.get(null)).allocateInstance(Class.forName(name));
    }

    private static void checkHandlers(Object target, String flag) throws ReflectiveOperationException {
        var c = SlopiumConfig.get();
        var field = SlopiumConfig.class.getField(flag);
        boolean found = false;
        for (var method : target.getClass().getDeclaredMethods()) {
            if (!method.getName().endsWith("slopium$cut")) continue;
            found = true; method.setAccessible(true);
            for (int mode = 0; mode < 3; mode++) {
                c.enabled = mode != 2; field.setBoolean(c, mode != 1);
                var ci = new CallbackInfo("smoke", true);
                if (method.getParameterCount() == 1) method.invoke(target, ci);
                else method.invoke(target, ci, null);
                if (ci.isCancelled() != (mode == 0)) throw new AssertionError("Cut guard failed: " + flag + "/" + mode);
            }
        }
        if (!found) throw new AssertionError("Missing transformed cut: " + target.getClass());
        c.enabled = true; field.setBoolean(c, true);
    }
}

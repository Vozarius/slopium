package dev.slopium.smoke;

import dev.slopium.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.Arrays;

final class SmokeTargetedChecks {
    static final class TestParticle extends Particle {
        private TestParticle() { super(null, 0, 0, 0); }
        public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partial) {}
        public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }
        public AABB getRenderBoundingBox(float partial) { return new AABB(1000, 0, 0, 1001, 1, 1); }
    }
    static void run() throws ReflectiveOperationException {
        var c = SlopiumConfig.get();
        var saved = new java.util.HashMap<java.lang.reflect.Field, Object>();
        for (var f : SlopiumConfig.class.getFields()) saved.put(f, f.get(c));
        try {
            c.enabled = c.diagnostics = true;
            var particle = (TestParticle) SmokeRadicalChecks.allocate(TestParticle.class);
            var level = (ClientLevel) SmokeRadicalChecks.allocate(ClientLevel.class);
            var data = (ClientLevel.ClientLevelData) SmokeRadicalChecks.allocate(ClientLevel.ClientLevelData.class);
            var ld = Level.class.getDeclaredField("levelData"); ld.setAccessible(true); ld.set(level, data);
            var pl = Particle.class.getDeclaredField("level"); pl.setAccessible(true); pl.set(particle, level);
            var hook = Arrays.stream(Particle.class.getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$cachedLight")).findFirst().orElseThrow();
            hook.setAccessible(true);
            var x = Particle.class.getDeclaredField("x"); x.setAccessible(true);
            int[] calls = {0};
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Integer> original = args -> ++calls[0];
            c.cacheParticleLight = true; data.setGameTime(20); x.setDouble(particle, 10);
            int a = (int) hook.invoke(particle, 0f, original);
            int b = (int) hook.invoke(particle, .5f, original);
            if (a != b || calls[0] != 1) throw new AssertionError("Particle light not cached");
            var entry = Particle.class.getDeclaredMethod("getLightColor", float.class); entry.setAccessible(true);
            if ((int) entry.invoke(particle, .75f) != a) throw new AssertionError("Actual particle light entry bypassed cache");
            data.setGameTime(21); hook.invoke(particle, 0f, original);
            x.setDouble(particle, 11); hook.invoke(particle, 0f, original);
            c.enabled = false; hook.invoke(particle, 0f, original);
            c.enabled = true; hook.invoke(particle, 0f, original);
            if (calls[0] != 5) throw new AssertionError("Particle cache invalidation failed");
            var engine = Minecraft.getInstance().particleEngine;
            var range = Arrays.stream(engine.getClass().getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$particleRange")).findFirst().orElseThrow(); range.setAccessible(true);
            c.particleRange = true; c.particleRenderDistance = 16;
            if ((boolean) range.invoke(engine, particle, null, Minecraft.getInstance().gameRenderer.getMainCamera(), 0f)) throw new AssertionError("Distant particle drawn");
            c.enabled = false;
            if (!(boolean) range.invoke(engine, particle, null, Minecraft.getInstance().gameRenderer.getMainCamera(), 0f)) throw new AssertionError("Particle global off failed");
            c.enabled = true;
            if (Slopium.loaded("create")) {
                var type = Class.forName("com.simibubi.create.content.logistics.factoryBoard.FactoryPanelRenderer");
                for (String name : new String[]{"slopium$connection", "slopium$bulb"})
                    if (Arrays.stream(type.getDeclaredMethods()).noneMatch(m -> m.getName().endsWith(name))) throw new AssertionError("Missing factory hook " + name);
                SmokeFactoryChecks.run();
            }
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: targeted particle cache invalidation/range and factory hooks passed");
        } finally {
            for (var e : saved.entrySet()) e.getKey().set(c, e.getValue());
            SlopiumStats.reset();
        }
    }
}

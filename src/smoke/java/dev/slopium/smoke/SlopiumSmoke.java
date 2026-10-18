package dev.slopium.smoke;

import com.mojang.logging.LogUtils;
import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.light.LightIndexControl;
import dev.slopium.visibility.CreateState;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Not included in the release JAR. Opens the actual Sodium page and exits. */
@Mod(value = "slopium_smoke", dist = Dist.CLIENT)
public final class SlopiumSmoke {
    private int frames;
    private boolean opened, captured;

    public SlopiumSmoke() {
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::frame);
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (opened || !(client.screen instanceof TitleScreen)) return;
        var options = ConfigManager.CONFIG.getModOptions().stream()
            .filter(mod -> mod.configId().equals("slopium")).findFirst().orElseThrow();
        if (options.pages().size() != 5) throw new IllegalStateException("Missing Slopium pages");
        validateOptionalMixins();
        client.setScreen(VideoSettingsScreen.createScreen(client.screen, (OptionPage) options.pages().get(4)));
        // Reese replaces createScreen and ignores Sodium's initial-page argument.
        if (Slopium.loaded("reeses_sodium_options")) {
            try {
                Object frame = client.screen.getClass().getMethod("rso$getTabFrame").invoke(client.screen);
                var tabs = (java.util.List<?>) frame.getClass().getMethod("getTabs").invoke(frame);
                Object selected = tabs.stream().filter(tab -> {
                    try { return tab.getClass().getMethod("getPage").invoke(tab) == options.pages().get(4); }
                    catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                }).findFirst().orElseThrow();
                frame.getClass().getMethod("setTab", java.util.Optional.class).invoke(frame, java.util.Optional.of(selected));
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
        opened = true;
        LogUtils.getLogger().info("SLOPIUM_SMOKE: options registered; actual Sodium page opened");
    }

    private static void validateOptionalMixins() {
        try {
            SmokeTickChecks.run(Class.forName("net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity"));
            SmokeBreakingChecks.run();
            SmokeExperimentChecks.run();
            SmokeBudgetChecks.run();
            if (Slopium.loaded("sodiumdynamiclights")) {
                Object lights = Class.forName("toni.sodiumdynamiclights.SodiumDynamicLights").getMethod("get").invoke(null);
                if (!(lights instanceof LightIndexControl)) throw new AssertionError("Light mixin missing");
                ((LightIndexControl) lights).slopium$clearCaches();
                SmokeLightChecks.run(lights);
            }
            if (Slopium.loaded("create") && !CreateState.class.isAssignableFrom(
                Class.forName("com.simibubi.create.foundation.blockEntity.SmartBlockEntity"))) {
                throw new AssertionError("Create state mixin missing");
            }
            if (Slopium.loaded("createdynlight")) {
                String base = "top.leonx.dynlight.";
                Class<?> handler;
                try { handler = Class.forName(base + "sodium.ContraptionEntityEventHandler"); }
                catch (ClassNotFoundException absent) { handler = Class.forName(base + "lamb.ContraptionEntityEventHandler"); }
                var tick = handler.getMethod("onTick", net.minecraft.world.level.Level.class);
                var state = handler.getDeclaredField("lastDynamicLightEnabled");
                state.setAccessible(true);
                var config = SlopiumConfig.get();
                boolean enabled = config.enabled, createLight = config.createLight;
                try {
                    config.enabled = true;
                    config.createLight = false;
                    tick.invoke(null, new Object[]{null});
                    if (state.getBoolean(null)) throw new AssertionError("Light toggle failed to disable source handler");
                    config.createLight = true;
                    tick.invoke(null, new Object[]{null});
                    if (!state.getBoolean(null)) throw new AssertionError("Light toggle failed to restore source handler");
                } finally { config.enabled = enabled; config.createLight = createLight; }
                if (Slopium.loaded("sable")) Class.forName(base + "sodium.sable.SableSubLevelEventHandler");
            }
            LogUtils.getLogger().info("SLOPIUM_SMOKE: mixin targets loaded; available light adapters verified");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private void frame(RenderFrameEvent.Post event) {
        if (!opened || captured || Minecraft.getInstance().getOverlay() != null || ++frames < 60) return;
        captured = true;
        Minecraft client = Minecraft.getInstance();
        Screenshot.grab(client.gameDirectory, "slopium-smoke.png", client.getMainRenderTarget(), message -> {
            LogUtils.getLogger().info("SLOPIUM_SMOKE: screenshot saved; success");
            client.execute(client::stop);
        });
    }
}

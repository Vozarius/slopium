package dev.slopium.client;

import com.mojang.brigadier.Command;
import dev.slopium.light.SodiumLightBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Slopium.MOD_ID, dist = Dist.CLIENT)
public final class Slopium {
    public static final String MOD_ID = "slopium";
    private static volatile long configRevision;
    private static volatile long frame;
    private static boolean hadLevel;

    public Slopium() {
        SlopiumConfig.get();
        NeoForge.EVENT_BUS.addListener(Slopium::registerCommands);
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre event) -> {
            frame++;
            Minecraft client = Minecraft.getInstance();
            boolean hasLevel = client.level != null;
            boolean recordFrames = SlopiumConfig.get().diagnostics && hasLevel
                && client.screen == null && !client.isPaused();
            SlopiumStats.FRAME_TIMES.frameStart(recordFrames ? System.nanoTime() : 0, recordFrames);
            if (hadLevel && !hasLevel && loaded("sodiumdynamiclights")) SodiumLightBridge.clear();
            hadLevel = hasLevel;
        });
    }

    public static long configRevision() { return configRevision; }
    public static long frame() { return frame; }

    public static void applyOptions() {
        SlopiumConfig.get().save();
        configRevision++;
        SlopiumStats.FRAME_TIMES.reset();
        if (loaded("sodiumdynamiclights")) SodiumLightBridge.clear();
        // Cached chunk light must be rebuilt when a source group is disabled/re-enabled.
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) client.levelRenderer.allChanged();
    }

    public static boolean loaded(String id) { return ModList.get().isLoaded(id); }

    // Loaded lazily after mod discovery, then reused in hot paths.
    private static final class Integrations {
        static final boolean CREATE = loaded("create");
        static final boolean CREATE_TICKS = loaded("create") && loaded("entityculling");
        static final boolean SABLE = loaded("sable");
    }
    public static boolean createTickIntegrationAvailable() { return Integrations.CREATE_TICKS; }
    public static boolean createAvailable() { return Integrations.CREATE; }
    public static boolean sableAvailable() { return Integrations.SABLE; }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("slopium")
            .then(Commands.literal("status").executes(context -> {
                context.getSource().sendSuccess(() -> Component.literal(
                    "Slopium | sources=" + SlopiumStats.indexedSources
                    + " | queries=" + SlopiumStats.lightQueries + " | cache hits=" + SlopiumStats.lightCacheHits
                    + " | skipped ticks=" + SlopiumStats.hiddenTicksSkipped
                    + " | culled cracks=" + SlopiumStats.breakingOverlaysSkipped
                    + " | checked cracks=" + SlopiumStats.breakingOverlaysChecked
                    + " | last index=" + String.format(java.util.Locale.ROOT, "%.3f ms", SlopiumStats.indexBuildNanos / 1e6)
                    + " | counters=" + SlopiumConfig.get().diagnostics), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    "Create ticks | checked eligible=" + SlopiumStats.eligibleTicksChecked
                    + " | not ready=" + SlopiumStats.tickNotReady
                    + " | visible/excluded=" + SlopiumStats.tickVisibleOrExcluded
                    + " | near=" + SlopiumStats.tickNear
                    + " | Sable protected=" + SlopiumStats.tickSableProtected
                    + " | hidden=" + SlopiumStats.hiddenTicksChecked
                    + " | skipped=" + SlopiumStats.hiddenTicksSkipped), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    "Tests | skipped BE draws=" + SlopiumStats.blockEntityDrawsSkipped
                    + " | Flywheel draws=" + SlopiumStats.flywheelDrawsSkipped
                    + " | particle calls=" + SlopiumStats.particleCallsSkipped
                    + " | ambient passes=" + SlopiumStats.ambientPassesSkipped
                    + " | frozen Create ticks=" + SlopiumStats.frozenCreateTicks), false);
                context.getSource().sendSuccess(() -> Component.literal("Render budget | item draws=" + SlopiumStats.itemDrawsSkipped
                    + " | copies=" + SlopiumStats.itemCopiesSkipped + " | shadows=" + SlopiumStats.shadowsSkipped
                    + " | Sodium passes=" + SlopiumStats.sodiumPassesSkipped + " | Xaero guards=" + SlopiumStats.xaeroMeasurementGuardsSkipped), false);
                var timing = SlopiumStats.FRAME_TIMES.snapshot();
                context.getSource().sendSuccess(() -> Component.literal(timing.samples() == 0
                    ? "Frames | no samples; enable diagnostics and close menus in the world"
                    : String.format(java.util.Locale.ROOT,
                        "Frames | n=%d | %.1fs | avg=%.1f FPS | p50=%.2f ms | p95=%.2f ms | p99=%.2f ms | max=%.2f ms | >16.67ms=%.1f%%",
                        timing.samples(), timing.seconds(), timing.averageFps(), timing.medianMs(),
                        timing.p95Ms(), timing.p99Ms(), timing.maxMs(), timing.overBudgetPercent())), false);
                return Command.SINGLE_SUCCESS;
            }))
            .then(Commands.literal("reset").executes(context -> {
                SlopiumStats.reset();
                context.getSource().sendSuccess(() -> Component.literal("Slopium: counters reset"), false);
                return Command.SINGLE_SUCCESS;
            })));
    }
}

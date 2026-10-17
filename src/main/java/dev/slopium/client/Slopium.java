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
            boolean hasLevel = Minecraft.getInstance().level != null;
            if (hadLevel && !hasLevel && loaded("sodiumdynamiclights")) SodiumLightBridge.clear();
            hadLevel = hasLevel;
        });
    }

    public static long configRevision() { return configRevision; }
    public static long frame() { return frame; }

    public static void applyOptions() {
        SlopiumConfig.get().save();
        configRevision++;
        if (loaded("sodiumdynamiclights")) SodiumLightBridge.clear();
        // Cached chunk light must be rebuilt when a source group is disabled/re-enabled.
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) client.levelRenderer.allChanged();
    }

    public static boolean loaded(String id) { return ModList.get().isLoaded(id); }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("slopium")
            .then(Commands.literal("status").executes(context -> {
                context.getSource().sendSuccess(() -> Component.literal(
                    "Slopium | sources=" + SlopiumStats.indexedSources
                    + " | queries=" + SlopiumStats.lightQueries + " | cache hits=" + SlopiumStats.lightCacheHits
                    + " | skipped ticks=" + SlopiumStats.hiddenTicksSkipped
                    + " | culled cracks=" + SlopiumStats.breakingOverlaysSkipped
                    + " | last index=" + String.format(java.util.Locale.ROOT, "%.3f ms", SlopiumStats.indexBuildNanos / 1e6)
                    + " | counters=" + SlopiumConfig.get().diagnostics), false);
                return Command.SINGLE_SUCCESS;
            }))
            .then(Commands.literal("reset").executes(context -> {
                SlopiumStats.reset();
                context.getSource().sendSuccess(() -> Component.literal("Slopium: counters reset"), false);
                return Command.SINGLE_SUCCESS;
            })));
    }
}

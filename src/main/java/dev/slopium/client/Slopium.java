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
        NeoForge.EVENT_BUS.addListener(dev.slopium.framegen.FrameGeneration::overlay);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> {
            dev.slopium.framegen.FrameGeneration.release();
            if(loaded("flywheel")) dev.slopium.visibility.GpuGlassDepth.release();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load event) -> {
            if(event.getLevel().isClientSide()) dev.slopium.visibility.VisibilityGeometry.chunk(event.getChunk().getPos().x,event.getChunk().getPos().z);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Unload event) -> {
            if(event.getLevel().isClientSide()) dev.slopium.visibility.VisibilityGeometry.chunk(event.getChunk().getPos().x,event.getChunk().getPos().z);
        });
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre event) -> {
            frame++;
            Minecraft client = Minecraft.getInstance();
            boolean hasLevel = client.level != null;
            boolean recordFrames = SlopiumConfig.get().diagnostics && hasLevel
                && client.screen == null && !client.isPaused();
            SlopiumStats.FRAME_TIMES.frameStart(recordFrames ? System.nanoTime() : 0, recordFrames);
            if (hadLevel && !hasLevel && loaded("sodiumdynamiclights")) SodiumLightBridge.clear();
            if(hadLevel && !hasLevel) dev.slopium.visibility.VisibilityPool.clear();
            hadLevel = hasLevel;
        });
    }

    public static long configRevision() { return configRevision; }
    public static long frame() { return frame; }
    public static void generatedFrameStart() {
        frame++;
        boolean record=SlopiumConfig.get().diagnostics;
        SlopiumStats.FRAME_TIMES.frameStart(record?System.nanoTime():0,record);
    }

    public static void applyOptions() {
        SlopiumConfig.get().save();
        configRevision++;
        dev.slopium.framegen.FrameGeneration.release();
        dev.slopium.framegen.FrameGeneration.resetCounters();
        if(loaded("flywheel")&&(!SlopiumConfig.get().enabled||!SlopiumConfig.get().gpuGlassCulling))
            dev.slopium.visibility.GpuGlassDepth.release();
        dev.slopium.visibility.WallOcclusion.invalidate();
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
                context.getSource().sendSuccess(() -> Component.literal("Radical | item sprites=" + SlopiumStats.itemImpostorsRendered
                    + " | Create visual frames=" + SlopiumStats.CREATE_VISUALS_SKIPPED.sum()
                    + " | particles rejected=" + SlopiumStats.particleBudgetSkipped
                    + " | texture blends=" + SlopiumStats.textureInterpolationsSkipped), false);
                context.getSource().sendSuccess(() -> Component.literal("Targeted | particle light hits=" + SlopiumStats.particleLightHits
                    + " | particle draws=" + SlopiumStats.particleDrawsSkipped + " | factory paths=" + SlopiumStats.factoryPathsSkipped
                    + " | factory items=" + SlopiumStats.factoryItemPassesSkipped + " | factory bulbs=" + SlopiumStats.factoryBulbsSkipped + " | distant Create ticks=" + SlopiumStats.distanceCreateTicksSkipped), false);
                context.getSource().sendSuccess(() -> Component.literal("Walls | particles=" + SlopiumStats.wallParticlesSkipped
                    + " | Create draws=" + SlopiumStats.wallCreateDrawsSkipped + " | Create ticks=" + SlopiumStats.wallCreateTicksSkipped
                    + " | Flywheel instances=" + SlopiumStats.wallFlywheelInstancesHidden + " | shared rays=" + SlopiumStats.occlusionRays
                    + " | cache=" + SlopiumStats.occlusionCacheHits + " | budget misses=" + SlopiumStats.occlusionBudgetMisses), false);
                context.getSource().sendSuccess(() -> Component.literal("Particle visibility | checked=" + SlopiumStats.particleOcclusionChecks
                    + " | hidden=" + SlopiumStats.wallParticlesSkipped + " | deferred=" + SlopiumStats.particleOcclusionDeferred
                    + " | Sable protected=" + SlopiumStats.particleOcclusionProtected), false);
                context.getSource().sendSuccess(() -> Component.literal("Create visibility | checked=" + SlopiumStats.createOcclusionChecks
                    + " | rays=" + SlopiumStats.createOcclusionRays + " | cache=" + SlopiumStats.createOcclusionCacheHits
                    + " | deferred=" + SlopiumStats.createOcclusionDeferred + " | Sable protected=" + SlopiumStats.createOcclusionProtected), false);
                context.getSource().sendSuccess(() -> Component.literal("Flywheel visibility | checked=" + SlopiumStats.flywheelVisualsChecked
                    + " | registered wrapped=" + SlopiumStats.flywheelVisualsWrapped + " | registered unsupported=" + SlopiumStats.flywheelVisualsUnsupported), false);
                context.getSource().sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                    "Culling cost | Flywheel passes=%d | avg=%.3f ms | max=%.3f ms | Create trace total=%.1f ms",
                    SlopiumStats.flywheelCullPasses,
                    SlopiumStats.flywheelCullPasses == 0 ? 0.0 : SlopiumStats.flywheelCullNanos / (double)SlopiumStats.flywheelCullPasses / 1_000_000.0,
                    SlopiumStats.flywheelCullMaxNanos / 1_000_000.0, SlopiumStats.createTraceNanos / 1_000_000.0)), false);
                context.getSource().sendSuccess(() -> Component.literal("Culling queue | tracked="+SlopiumStats.flywheelTracked
                    +" | supported="+SlopiumStats.flywheelSupported+" | altered="+SlopiumStats.flywheelAltered
                    +" | registered="+SlopiumStats.flywheelRegistered+" | budget yields="+SlopiumStats.flywheelBudgetYields),false);
                context.getSource().sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                    "Visibility pool | entries=%d | pending=%d | reads=%d | hits=%d | unknown=%d | rays=%d | avg=%.3f ms | max=%.3f ms",
                    SlopiumStats.visibilityEntries,SlopiumStats.visibilityPending,SlopiumStats.visibilityReads,
                    SlopiumStats.visibilityHits,SlopiumStats.visibilityUnknown,SlopiumStats.visibilityRays,
                    SlopiumStats.visibilityPasses==0?0:SlopiumStats.visibilityNanos/(double)SlopiumStats.visibilityPasses/1e6,
                    SlopiumStats.visibilityMaxNanos/1e6)),false);
                context.getSource().sendSuccess(() -> Component.literal("Visibility reuse | unchanged paths="+SlopiumStats.visibilityPathReuse
                    +" | blocker proofs="+SlopiumStats.visibilityWitnessReuse+" | cells="+SlopiumStats.visibilityCells+" | shared particle light="+SlopiumStats.particleSharedLightHits),false);
                context.getSource().sendSuccess(() -> Component.literal("Flywheel path | native GPU depth="+SlopiumStats.flywheelNativeDepth
                    +" | models delegated="+SlopiumStats.flywheelGpuModels+" | animations/ticks=CPU"),false);
                context.getSource().sendSuccess(() -> Component.literal("Camera culling | checks="+SlopiumStats.cameraChecks+" | rejected="+SlopiumStats.cameraRejects),false);
                if(loaded("flywheel")) context.getSource().sendSuccess(() -> Component.literal("GPU glass | "+dev.slopium.visibility.GpuGlassDepth.state
                    +" | passes="+dev.slopium.visibility.GpuGlassDepth.passes+" | CPU submit total ms="+dev.slopium.visibility.GpuGlassDepth.submitNanos/1_000_000.0),false);
                var timing = SlopiumStats.FRAME_TIMES.snapshot();
                context.getSource().sendSuccess(()->Component.literal(dev.slopium.framegen.FrameGeneration.status()),false);
                context.getSource().sendSuccess(()->Component.literal(dev.slopium.framegen.AsyncPresenter.status()),false);
                context.getSource().sendSuccess(()->Component.literal(dev.slopium.framegen.FastFramePath.status()),false);
                context.getSource().sendSuccess(() -> Component.literal(timing.samples() == 0
                    ? "Frames | no samples; enable diagnostics and close menus in the world"
                    : String.format(java.util.Locale.ROOT,
                        "Frames | n=%d | %.1fs | avg=%.1f FPS | p50=%.2f ms | p95=%.2f ms | p99=%.2f ms | max=%.2f ms | >16.67ms=%.1f%%",
                        timing.samples(), timing.seconds(), timing.averageFps(), timing.medianMs(),
                        timing.p95Ms(), timing.p99Ms(), timing.maxMs(), timing.overBudgetPercent())), false);
                return Command.SINGLE_SUCCESS;
            }))
            .then(Commands.literal("inspect").executes(context -> {
                var mc=Minecraft.getInstance();
                if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
                    context.getSource().sendSuccess(() -> Component.literal(dev.slopium.visibility.CullingInspector.describe(hit.getBlockPos())),false);
                } else context.getSource().sendSuccess(() -> Component.literal("Aim at a block, or use /slopium inspect x y z"),false);
                return Command.SINGLE_SUCCESS;
            }).then(Commands.argument("x",com.mojang.brigadier.arguments.IntegerArgumentType.integer(-30000000,30000000))
                .then(Commands.argument("y",com.mojang.brigadier.arguments.IntegerArgumentType.integer(-2048,2048))
                .then(Commands.argument("z",com.mojang.brigadier.arguments.IntegerArgumentType.integer(-30000000,30000000)).executes(context -> {
                    var pos=new net.minecraft.core.BlockPos(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"x"),
                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"y"),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"z"));
                    context.getSource().sendSuccess(() -> Component.literal(dev.slopium.visibility.CullingInspector.describe(pos)),false);
                    return Command.SINGLE_SUCCESS;
                })))))
            .then(Commands.literal("reset").executes(context -> {
                SlopiumStats.reset();
                if(loaded("flywheel")) {dev.slopium.visibility.GpuGlassDepth.passes=0;dev.slopium.visibility.GpuGlassDepth.submitNanos=0;}
                context.getSource().sendSuccess(() -> Component.literal("Slopium: counters reset"), false);
                return Command.SINGLE_SUCCESS;
            })));
    }
}

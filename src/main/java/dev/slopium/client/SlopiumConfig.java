package dev.slopium.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SlopiumConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile SlopiumConfig instance;
    public volatile boolean enabled = true;
    public volatile boolean indexedLighting = true;
    public volatile boolean lightQueryCache = true;
    public volatile boolean createLight = true;
    public volatile boolean sableLight = true;
    public volatile boolean cullBreakingOverlays = true;
    // Experimental: changing tick cadence can affect third-party client behaviours.
    public boolean throttleHiddenCreate = false;
    public int hiddenTickInterval = 4;
    public int protectedDistance = 24;
    public int hiddenGraceTicks = 20;
    public boolean diagnostics = false;
    // Diagnostic visual cuts, deliberately opt-in.
    public volatile boolean disableBlockEntityRendering = false;
    public volatile boolean disableFlywheelRendering = false;
    public volatile boolean disableParticles = false;
    public volatile boolean disableAmbientEffects = false;
    public volatile boolean freezeCreateClientTicks = false;
    public volatile boolean itemStackLod = false;
    public int itemLodDistance = 16;
    public volatile boolean limitItemRendering = false;
    public int itemRenderDistance = 64;
    public volatile boolean limitBlockEntityRendering = false;
    public int blockEntityRenderDistance = 64;
    public volatile boolean pauseXaeroBenchmark = false;
    public volatile boolean disableEntityShadows = false;
    public volatile boolean throttleFlywheelDistance = false;
    public volatile int flywheelFullRateDistance = 32;
    public volatile int flywheelFrameInterval = 3;

    public static SlopiumConfig get() {
        if (instance == null) {
            synchronized (SlopiumConfig.class) {
                if (instance == null) instance = load();
            }
        }
        return instance;
    }

    private static Path path() { return FMLPaths.CONFIGDIR.get().resolve("slopium.json"); }

    private static SlopiumConfig load() {
        Path file = path();
        if (Files.isRegularFile(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                SlopiumConfig value = GSON.fromJson(reader, SlopiumConfig.class);
                if (value != null) { value.validate(); return value; }
            } catch (Exception e) {
                LogUtils.getLogger().error("Slopium cannot read {}; using defaults", file, e);
            }
        }
        return new SlopiumConfig();
    }

    public void save() {
        validate();
        Path target = path(), temporary = target.resolveSibling("slopium.json.tmp");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(temporary, GSON.toJson(this));
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LogUtils.getLogger().error("Slopium cannot save configuration", e);
        }
    }

    private void validate() {
        flywheelFullRateDistance = Math.clamp(flywheelFullRateDistance, 0, 512);
        flywheelFrameInterval = Math.clamp(flywheelFrameInterval, 2, 8);
        itemLodDistance = Math.clamp(itemLodDistance, 0, 512);
        itemRenderDistance = Math.clamp(itemRenderDistance, 0, 512);
        blockEntityRenderDistance = Math.clamp(blockEntityRenderDistance, 0, 512);
        hiddenTickInterval = Math.clamp(hiddenTickInterval, 2, 10);
        protectedDistance = Math.clamp(protectedDistance, 16, 64);
        hiddenGraceTicks = Math.clamp(hiddenGraceTicks, 10, 100);
    }
}


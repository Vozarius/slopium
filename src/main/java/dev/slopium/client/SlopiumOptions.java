package dev.slopium.client;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SlopiumOptions implements ConfigEntryPoint {
    private final StorageEventHandler storage = Slopium::applyOptions;

    @Override public void registerConfigLate(ConfigBuilder builder) {
        SlopiumConfig c = SlopiumConfig.get();
        boolean lights = Slopium.loaded("sodiumdynamiclights");
        boolean createLight = lights && Slopium.loaded("createdynlight");
        boolean sableLight = createLight && Slopium.loaded("sable") && classPresent("top.leonx.dynlight.sodium.sable.SableDynLightSource");
        String activeVersion = ModList.get().getModContainerById("createdynlight")
            .map(m -> m.getModInfo().getVersion().toString()).orElse("—");

        builder.registerOwnModOptions()
            .setColorTheme(builder.createColorTheme().setBaseThemeRGB(0x72c7a6))
            .addPage(builder.createOptionPage().setName(text("page.general"))
                .addOptionGroup(builder.createOptionGroup()
                    .addOption(bool(builder, "enabled", v -> c.enabled = v, () -> c.enabled, true, true))
                    .addOption(bool(builder, "diagnostics", v -> c.diagnostics = v, () -> c.diagnostics, false, true))))
            .addPage(builder.createOptionPage().setName(text("page.lights"))
                .addOptionGroup(builder.createOptionGroup()
                    .addOption(bool(builder, "indexed_lighting", v -> c.indexedLighting = v, () -> c.indexedLighting, true, lights))
                    .addOption(bool(builder, "light_cache", v -> c.lightQueryCache = v, () -> c.lightQueryCache, true, lights)))
                .addOptionGroup(builder.createOptionGroup().setName(Component.translatable("slopium.group.create_light", activeVersion))
                    .addOption(bool(builder, "create_light", v -> c.createLight = v, () -> c.createLight, true, createLight))
                    .addOption(bool(builder, "sable_light", v -> c.sableLight = v, () -> c.sableLight, true, sableLight))))
            .addPage(builder.createOptionPage().setName(text("page.visibility"))
                .addOptionGroup(builder.createOptionGroup()
                    .addOption(bool(builder, "cull_breaking", v -> c.cullBreakingOverlays = v, () -> c.cullBreakingOverlays, true, true)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.experimental"))
                    .addOption(bool(builder, "hidden_ticks", v -> c.throttleHiddenCreate = v, () -> c.throttleHiddenCreate, false,
                        Slopium.loaded("create") && Slopium.loaded("entityculling")))
                    .addOption(integer(builder, "tick_interval", v -> c.hiddenTickInterval = v, () -> c.hiddenTickInterval, 4, 2, 10, 1))
                    .addOption(integer(builder, "protected_distance", v -> c.protectedDistance = v, () -> c.protectedDistance, 24, 16, 64, 4))
                    .addOption(integer(builder, "hidden_grace", v -> c.hiddenGraceTicks = v, () -> c.hiddenGraceTicks, 20, 10, 100, 10))))
            .addPage(builder.createOptionPage().setName(text("page.tests"))
                .addOptionGroup(builder.createOptionGroup()
                    .addOption(bool(builder, "disable_be_render", v -> c.disableBlockEntityRendering = v, () -> c.disableBlockEntityRendering, false, true))
                    .addOption(bool(builder, "disable_flywheel_render", v -> c.disableFlywheelRendering = v, () -> c.disableFlywheelRendering, false, Slopium.loaded("flywheel")))
                    .addOption(bool(builder, "disable_particles", v -> c.disableParticles = v, () -> c.disableParticles, false, true))
                    .addOption(bool(builder, "disable_ambient", v -> c.disableAmbientEffects = v, () -> c.disableAmbientEffects, false, true))
                    .addOption(bool(builder, "freeze_create_ticks", v -> c.freezeCreateClientTicks = v, () -> c.freezeCreateClientTicks, false, Slopium.loaded("create")))))
            .addPage(builder.createOptionPage().setName(text("page.render_budget"))
                .addOptionGroup(builder.createOptionGroup()
                    .addOption(bool(builder, "item_lod", v -> c.itemStackLod = v, () -> c.itemStackLod, false, true))
                    .addOption(integer(builder, "item_lod_distance", v -> c.itemLodDistance = v, () -> c.itemLodDistance, 16, 0, 512, 1))
                    .addOption(bool(builder, "item_range", v -> c.limitItemRendering = v, () -> c.limitItemRendering, false, true))
                    .addOption(integer(builder, "item_render_distance", v -> c.itemRenderDistance = v, () -> c.itemRenderDistance, 64, 0, 512, 1)))
                .addOptionGroup(builder.createOptionGroup()
                    .addOption(bool(builder, "be_range", v -> c.limitBlockEntityRendering = v, () -> c.limitBlockEntityRendering, false, true))
                    .addOption(integer(builder, "be_render_distance", v -> c.blockEntityRenderDistance = v, () -> c.blockEntityRenderDistance, 64, 0, 512, 1))
                    .addOption(bool(builder, "pause_xaero_benchmark", v -> c.pauseXaeroBenchmark = v, () -> c.pauseXaeroBenchmark, false, Slopium.loaded("xaeroworldmap")))
                    .addOption(bool(builder, "flywheel_distance", v -> c.throttleFlywheelDistance = v, () -> c.throttleFlywheelDistance, false, Slopium.loaded("flywheel")))
                    .addOption(integer(builder, "flywheel_distance_radius", v -> c.flywheelFullRateDistance = v, () -> c.flywheelFullRateDistance, 32, 0, 512, 1))
                    .addOption(integer(builder, "flywheel_interval", v -> c.flywheelFrameInterval = v, () -> c.flywheelFrameInterval, 3, 2, 8, 1))
                    .addOption(bool(builder, "disable_shadows", v -> c.disableEntityShadows = v, () -> c.disableEntityShadows, false, true))))
            .addPage(builder.createOptionPage().setName(text("page.radical"))
                .addOptionGroup(builder.createOptionGroup()
                    .addOption(bool(builder, "item_impostors", v -> c.itemImpostors = v, () -> c.itemImpostors, false, true))
                    .addOption(integer(builder, "item_impostor_distance", v -> c.itemImpostorDistance = v, () -> c.itemImpostorDistance, 24, 0, 512, 1))
                    .addOption(bool(builder, "create_visuals", v -> c.throttleCreateVisuals = v, () -> c.throttleCreateVisuals, false, Slopium.loaded("create") && Slopium.loaded("flywheel")))
                    .addOption(integer(builder, "create_visual_distance", v -> c.createVisualDistance = v, () -> c.createVisualDistance, 24, 0, 512, 1))
                    .addOption(integer(builder, "create_visual_interval", v -> c.createVisualInterval = v, () -> c.createVisualInterval, 3, 2, 10, 1)))
                .addOptionGroup(builder.createOptionGroup()
                    .addOption(bool(builder, "particle_budget", v -> c.particleBudget = v, () -> c.particleBudget, false, true))
                    .addOption(integer(builder, "particles_per_tick", v -> c.particlesPerTick = v, () -> c.particlesPerTick, 128, 0, 1024, 16))
                    .addOption(bool(builder, "texture_interpolation", v -> c.disableTextureInterpolation = v, () -> c.disableTextureInterpolation, false, true))));
    }

    private BooleanOptionBuilder bool(ConfigBuilder b, String key, Consumer<Boolean> set, Supplier<Boolean> get, boolean defaults, boolean enabled) {
        return b.createBooleanOption(id(key)).setName(text(key)).setTooltip(text(key + ".tooltip"))
            .setStorageHandler(storage).setBinding(set, get).setDefaultValue(defaults).setEnabled(enabled).setImpact(OptionImpact.VARIES);
    }

    private IntegerOptionBuilder integer(ConfigBuilder b, String key, Consumer<Integer> set, Supplier<Integer> get, int defaults, int min, int max, int step) {
        return b.createIntegerOption(id(key)).setName(text(key)).setTooltip(text(key + ".tooltip"))
            .setStorageHandler(storage).setBinding(set, get).setDefaultValue(defaults).setRange(min, max, step)
            .setValueFormatter(value -> Component.literal(Integer.toString(value)));
    }

    private static boolean classPresent(String name) {
        return SlopiumOptions.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null;
    }
    private static ResourceLocation id(String key) { return ResourceLocation.fromNamespaceAndPath("slopium", key); }
    private static Component text(String key) { return Component.translatable("slopium." + key); }
}


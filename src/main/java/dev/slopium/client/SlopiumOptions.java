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
                    .addOption(integer(builder, "hidden_grace", v -> c.hiddenGraceTicks = v, () -> c.hiddenGraceTicks, 20, 10, 100, 10))));
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


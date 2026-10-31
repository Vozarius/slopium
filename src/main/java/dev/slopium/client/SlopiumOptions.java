package dev.slopium.client;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import dev.slopium.framegen.FrameGenerationMode;

public final class SlopiumOptions implements ConfigEntryPoint {
    private final StorageEventHandler storage = Slopium::applyOptions;

    @Override public void registerConfigLate(ConfigBuilder builder) {
        SlopiumConfig c = SlopiumConfig.get();
        boolean lights = Slopium.loaded("sodiumdynamiclights");
        boolean createLight = lights && Slopium.loaded("createdynlight");
        boolean sableLight = createLight && Slopium.loaded("sable") && classPresent("top.leonx.dynlight.sodium.sable.SableDynLightSource");

        builder.registerOwnModOptions()
            .setVersion(text("version").getString())
            .setNonTintedIcon(id("textures/gui/logo.png"))
            .setColorTheme(builder.createColorTheme().setBaseThemeRGB(0x72c7a6))
            .addPage(builder.createOptionPage().setName(text("page.general"))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.general"))
                    .addOption(bool(builder, "enabled", v -> c.enabled = v, () -> c.enabled, true, true))
                    .addOption(bool(builder, "camera_culling", v -> c.cameraCulling = v, () -> c.cameraCulling, false, true))
                    .addOption(bool(builder, "gpu_glass_culling", v -> c.gpuGlassCulling = v, () -> c.gpuGlassCulling, false, Slopium.loaded("flywheel")))
                    .addOption(bool(builder, "diagnostics", v -> c.diagnostics = v, () -> c.diagnostics, false, true))
                    .addOption(bool(builder, "cull_breaking", v -> c.cullBreakingOverlays = v, () -> c.cullBreakingOverlays, true, true))
                    .addOption(bool(builder, "pause_xaero_benchmark", v -> c.pauseXaeroBenchmark = v, () -> c.pauseXaeroBenchmark, false, Slopium.loaded("xaeroworldmap"))))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.lights"))
                    .addOption(bool(builder, "indexed_lighting", v -> c.indexedLighting = v, () -> c.indexedLighting, true, lights))
                    .addOption(bool(builder, "light_cache", v -> c.lightQueryCache = v, () -> c.lightQueryCache, true, lights))
                    .addOption(bool(builder, "create_light", v -> c.createLight = v, () -> c.createLight, true, createLight))
                    .addOption(bool(builder, "sable_light", v -> c.sableLight = v, () -> c.sableLight, true, sableLight))))
            .addPage(builder.createOptionPage().setName(text("page.frame_generation"))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.frame_generation"))
                    .addOption(mode(builder,"frame_generation_mode",FrameGenerationMode.class,v->c.frameGenerationMode=v,()->c.frameGenerationMode,FrameGenerationMode.OFF,true))
                    .addOption(bool(builder,"frame_generation_amortize",v->c.frameGenerationAmortize=v,()->c.frameGenerationAmortize,false,true))
                    .addOption(integer(builder,"frame_generation_multiplier",v->c.frameGenerationMultiplier=v,()->c.frameGenerationMultiplier,2,2,32,1))
                    .addOption(integer(builder,"frame_generation_input",v->c.frameGenerationInputFps=v,()->c.frameGenerationInputFps,0,0,240,1))
                    .addOption(bool(builder,"frame_generation_fast",v->c.frameGenerationFastPath=v,()->c.frameGenerationFastPath,false,true))
                    .addOption(bool(builder,"frame_generation_parallel",v->c.frameGenerationParallel=v,()->c.frameGenerationParallel,false,dev.slopium.framegen.AsyncPresenter.supported()))
                    .addOption(bool(builder,"frame_generation_mark",v->c.frameGenerationMarkFrames=v,()->c.frameGenerationMarkFrames,false,true))
                    .addOption(bool(builder,"frame_generation_counter",v->c.frameGenerationCounter=v,()->c.frameGenerationCounter,true,true))
                    .addOption(integer(builder,"frame_generation_target",v->c.frameGenerationTargetFps=v,()->c.frameGenerationTargetFps,0,0,240,10)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.frame_simple"))
                    .addOption(integer(builder,"frame_generation_grid",v->c.frameGenerationGridStep=v,()->c.frameGenerationGridStep,4,1,16,1)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.frame_hybrid"))
                    .addOption(integer(builder,"frame_generation_cell",v->c.frameGenerationFlowCell=v,()->c.frameGenerationFlowCell,8,4,16,4))
                    .addOption(integer(builder,"frame_generation_search",v->c.frameGenerationSearchPixels=v,()->c.frameGenerationSearchPixels,16,4,32,4))
                    .addOption(integer(builder,"frame_generation_rejection",v->c.frameGenerationRejectionPixels=v,()->c.frameGenerationRejectionPixels,4,1,16,1)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.frame_fsr"))
                    .addOption(bool(builder,"frame_generation_fsr_debug",v->c.frameGenerationFsrDebug=v,()->c.frameGenerationFsrDebug,false,true))))
            .addPage(builder.createOptionPage().setName(text("page.particles"))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.particle_visibility"))
                    .addOption(wall(builder, "particle_visibility", v -> c.occludeParticles = v, () -> c.occludeParticles, v -> c.opaqueGlassParticles = v, () -> c.opaqueGlassParticles, v -> c.disableParticles = v, () -> c.disableParticles, true))
                    .addOption(bool(builder, "particle_range", v -> c.particleRange = v, () -> c.particleRange, false, true))
                    .addOption(integer(builder, "particle_render_distance", v -> c.particleRenderDistance = v, () -> c.particleRenderDistance, 64, 0, 512, 1)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.particle_cost"))
                    .addOption(bool(builder, "particle_light_cache", v -> c.cacheParticleLight = v, () -> c.cacheParticleLight, false, true))
                    .addOption(bool(builder, "particle_budget", v -> c.particleBudget = v, () -> c.particleBudget, false, true))
                    .addOption(integer(builder, "particles_per_tick", v -> c.particlesPerTick = v, () -> c.particlesPerTick, 128, 0, 1024, 16))))
            .addPage(builder.createOptionPage().setName(text("page.create_render"))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.create_visibility"))
                    .addOption(wall(builder, "create_visibility", v -> c.occludeCreateRendering = v, () -> c.occludeCreateRendering, v -> c.createRenderAnyBlock = v, () -> c.createRenderAnyBlock, v -> c.hideAllCreateModels = v, () -> c.hideAllCreateModels, Slopium.loaded("create")))
                    .addOption(wall(builder, "flywheel_visibility", v -> c.occludeFlywheel = v, () -> c.occludeFlywheel, v -> c.flywheelAnyBlock = v, () -> c.flywheelAnyBlock, v -> c.disableFlywheelRendering = v, () -> c.disableFlywheelRendering, Slopium.loaded("flywheel"))))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.create_animation"))
                    .addOption(wall(builder, "animation_visibility", v -> c.occludeCreateAnimations = v, () -> c.occludeCreateAnimations, v -> c.animationAnyBlock = v, () -> c.animationAnyBlock, v -> c.freezeCreateAnimations = v, () -> c.freezeCreateAnimations, Slopium.loaded("create") && Slopium.loaded("flywheel")))
                    .addOption(bool(builder, "create_visuals", v -> c.throttleCreateVisuals = v, () -> c.throttleCreateVisuals, false, Slopium.loaded("create") && Slopium.loaded("flywheel")))
                    .addOption(integer(builder, "create_visual_distance", v -> c.createVisualDistance = v, () -> c.createVisualDistance, 24, 0, 512, 1))
                    .addOption(integer(builder, "create_visual_interval", v -> c.createVisualInterval = v, () -> c.createVisualInterval, 3, 2, 10, 1)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.flywheel_distance"))
                    .addOption(bool(builder, "flywheel_distance", v -> c.throttleFlywheelDistance = v, () -> c.throttleFlywheelDistance, false, Slopium.loaded("flywheel")))
                    .addOption(integer(builder, "flywheel_distance_radius", v -> c.flywheelFullRateDistance = v, () -> c.flywheelFullRateDistance, 32, 0, 512, 1))
                    .addOption(integer(builder, "flywheel_interval", v -> c.flywheelFrameInterval = v, () -> c.flywheelFrameInterval, 3, 2, 8, 1))))
            .addPage(builder.createOptionPage().setName(text("page.factory"))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.factory_items"))
                    .addOption(bool(builder, "factory_items", v -> c.factoryItemRange = v, () -> c.factoryItemRange, false, Slopium.loaded("create")))
                    .addOption(integer(builder, "factory_item_distance", v -> c.factoryItemDistance = v, () -> c.factoryItemDistance, 24, 0, 512, 1)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.factory_bulbs"))
                    .addOption(bool(builder, "factory_bulbs", v -> c.factoryBulbRange = v, () -> c.factoryBulbRange, false, Slopium.loaded("create")))
                    .addOption(integer(builder, "factory_bulb_distance", v -> c.factoryBulbDistance = v, () -> c.factoryBulbDistance, 32, 0, 512, 1)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.factory_connections"))
                    .addOption(mode(builder, "factory_connection_mode", Connections.class, v -> { c.hideFactoryConnections = v == Connections.HIDE; c.factoryConnectionRange = v == Connections.NEAR; }, () -> c.hideFactoryConnections ? Connections.HIDE : c.factoryConnectionRange ? Connections.NEAR : Connections.SHOW, Connections.SHOW, Slopium.loaded("create")))
                    .addOption(integer(builder, "factory_connection_distance", v -> c.factoryConnectionDistance = v, () -> c.factoryConnectionDistance, 24, 0, 512, 1))))
            .addPage(builder.createOptionPage().setName(text("page.create_ticks"))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.tick_mode"))
                    .addOption(mode(builder, "create_tick_mode", TickMode.class, v -> setTickMode(c, v), () -> tickMode(c), TickMode.OFF, Slopium.loaded("create"))))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.tick_parameters"))
                    .addOption(integer(builder, "tick_interval", v -> c.hiddenTickInterval = v, () -> c.hiddenTickInterval, 4, 2, 10, 1))
                    .addOption(integer(builder, "hidden_grace", v -> c.hiddenGraceTicks = v, () -> c.hiddenGraceTicks, 20, 10, 100, 10))
                    .addOption(integer(builder, "protected_distance", v -> c.protectedDistance = v, () -> c.protectedDistance, 24, 16, 64, 4))
                    .addOption(integer(builder, "create_tick_distance", v -> c.createTickDistance = v, () -> c.createTickDistance, 32, 0, 512, 1))
                    .addOption(integer(builder, "create_tick_interval", v -> c.createTickInterval = v, () -> c.createTickInterval, 4, 2, 10, 1))))
            .addPage(builder.createOptionPage().setName(text("page.world"))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.dropped_items"))
                    .addOption(bool(builder, "item_range", v -> c.limitItemRendering = v, () -> c.limitItemRendering, false, true))
                    .addOption(integer(builder, "item_render_distance", v -> c.itemRenderDistance = v, () -> c.itemRenderDistance, 64, 0, 512, 1))
                    .addOption(bool(builder, "item_lod", v -> c.itemStackLod = v, () -> c.itemStackLod, false, true))
                    .addOption(integer(builder, "item_lod_distance", v -> c.itemLodDistance = v, () -> c.itemLodDistance, 16, 0, 512, 1))
                    .addOption(bool(builder, "item_impostors", v -> c.itemImpostors = v, () -> c.itemImpostors, false, true))
                    .addOption(integer(builder, "item_impostor_distance", v -> c.itemImpostorDistance = v, () -> c.itemImpostorDistance, 24, 0, 512, 1)))
                .addOptionGroup(builder.createOptionGroup().setName(text("group.world_detail"))
                    .addOption(mode(builder, "block_models", BlockModels.class, v -> { c.disableBlockEntityRendering = v == BlockModels.HIDE; c.limitBlockEntityRendering = v == BlockModels.NEAR; }, () -> c.disableBlockEntityRendering ? BlockModels.HIDE : c.limitBlockEntityRendering ? BlockModels.NEAR : BlockModels.SHOW, BlockModels.SHOW, true))
                    .addOption(integer(builder, "be_render_distance", v -> c.blockEntityRenderDistance = v, () -> c.blockEntityRenderDistance, 64, 0, 512, 1))
                    .addOption(bool(builder, "disable_shadows", v -> c.disableEntityShadows = v, () -> c.disableEntityShadows, false, true))
                    .addOption(bool(builder, "texture_interpolation", v -> c.disableTextureInterpolation = v, () -> c.disableTextureInterpolation, false, true))
                    .addOption(bool(builder, "disable_ambient", v -> c.disableAmbientEffects = v, () -> c.disableAmbientEffects, false, true))));

    }

    // One control owns each pair/set of legacy flags; saved alpha configurations remain readable.
    private enum Visibility { OFF, OPAQUE, ANY, ALL }
    private enum BlockModels { SHOW, NEAR, HIDE }
    private enum TickMode { OFF, OPAQUE, ANY, DISTANCE, ENTITY_CULLING, ALL }
    private enum Connections { SHOW, NEAR, HIDE }

    private static TickMode tickMode(SlopiumConfig c) {
        if (c.freezeCreateClientTicks) return TickMode.ALL;
        if (c.occludeCreateTicks) return c.tickAnyBlock ? TickMode.ANY : TickMode.OPAQUE;
        if (c.distanceCreateTicks) return TickMode.DISTANCE;
        return c.throttleHiddenCreate && Slopium.loaded("entityculling") ? TickMode.ENTITY_CULLING : TickMode.OFF;
    }
    private static void setTickMode(SlopiumConfig c, TickMode mode) {
        c.freezeCreateClientTicks = mode == TickMode.ALL;
        c.occludeCreateTicks = mode == TickMode.OPAQUE || mode == TickMode.ANY;
        c.tickAnyBlock = mode == TickMode.ANY;
        c.distanceCreateTicks = mode == TickMode.DISTANCE;
        c.throttleHiddenCreate = mode == TickMode.ENTITY_CULLING;
    }
    private EnumOptionBuilder<Visibility> wall(ConfigBuilder b, String key, Consumer<Boolean> activeSet, Supplier<Boolean> active,
            Consumer<Boolean> anySet, Supplier<Boolean> any, Consumer<Boolean> allSet, Supplier<Boolean> all, boolean available) {
        return mode(b, key, Visibility.class, v -> { allSet.accept(v == Visibility.ALL); activeSet.accept(v == Visibility.OPAQUE || v == Visibility.ANY); anySet.accept(v == Visibility.ANY); },
            () -> all.get() ? Visibility.ALL : !active.get() ? Visibility.OFF : any.get() ? Visibility.ANY : Visibility.OPAQUE, Visibility.OFF, available);
    }
    private <E extends Enum<E>> EnumOptionBuilder<E> mode(ConfigBuilder b, String key, Class<E> type, Consumer<E> set, Supplier<E> get, E defaults, boolean available) {
        var values = java.util.EnumSet.allOf(type);
        if (!Slopium.loaded("entityculling")) values.removeIf(v -> v.name().equals("ENTITY_CULLING"));
        return b.createEnumOption(id(key), type).setName(text(key)).setTooltip(tooltip(key))
            .setStorageHandler(storage).setBinding(set, get).setDefaultValue(defaults)
            .setAllowedValues(values)
            .setEnabledProvider(state -> available && allowed(key, state), dependencies(key))
            .setElementNameProvider(value -> text(value.name().equals("ALL") ? key + ".all" : "value." + type.getSimpleName() + "." + value.name()));
    }
    private BooleanOptionBuilder bool(ConfigBuilder b, String key, Consumer<Boolean> set, Supplier<Boolean> get, boolean defaults, boolean available) {
        return b.createBooleanOption(id(key)).setName(text(key)).setTooltip(tooltip(key))
            .setStorageHandler(storage).setBinding(set, get).setDefaultValue(defaults)
            .setEnabledProvider(state -> available && allowed(key, state), dependencies(key)).setImpact(OptionImpact.VARIES);
    }
    private IntegerOptionBuilder integer(ConfigBuilder b, String key, Consumer<Integer> set, Supplier<Integer> get, int defaults, int min, int max, int step) {
        return b.createIntegerOption(id(key)).setName(text(key)).setTooltip(tooltip(key))
            .setStorageHandler(storage).setBinding(set, get).setDefaultValue(defaults).setRange(min, max, step)
            .setEnabledProvider(state -> allowed(key, state), dependencies(key))
            .setValueFormatter(value -> key.equals("frame_generation_multiplier")?Component.translatable("slopium.frame_generation_multiplier.value",value,value-1):key.equals("frame_generation_target")&&value==0?text("frame_generation_target.unlimited"):key.equals("frame_generation_input")&&value==0?text("frame_generation_input.unlimited"):Component.literal(Integer.toString(value)));
    }
    private static final java.util.Map<String,String> PARENTS = java.util.Map.ofEntries(
        java.util.Map.entry("particle_render_distance", "particle_range"), java.util.Map.entry("particles_per_tick", "particle_budget"),
        java.util.Map.entry("factory_item_distance", "factory_items"), java.util.Map.entry("factory_bulb_distance", "factory_bulbs"),
        java.util.Map.entry("item_render_distance", "item_range"), java.util.Map.entry("item_lod_distance", "item_lod"),
        java.util.Map.entry("item_impostor_distance", "item_impostors"),
        java.util.Map.entry("create_visual_distance", "create_visuals"), java.util.Map.entry("create_visual_interval", "create_visuals"),
        java.util.Map.entry("flywheel_distance_radius", "flywheel_distance"), java.util.Map.entry("flywheel_interval", "flywheel_distance"),
        java.util.Map.entry("light_cache", "indexed_lighting"), java.util.Map.entry("sable_light", "create_light"));
    private static String override(String key) {
        if (key.startsWith("particle") && !key.equals("particle_visibility")) return "particle_visibility";
        if (key.startsWith("create_visual")) return "animation_visibility";
        return null;
    }
    private static ResourceLocation[] dependencies(String key) {
        var result = new java.util.ArrayList<ResourceLocation>();
        if (!key.equals("enabled") && !key.equals("diagnostics")) result.add(id("enabled"));
        if(key.startsWith("frame_generation_")&&!key.equals("frame_generation_mode"))result.add(id("frame_generation_mode"));
        if(key.startsWith("frame_generation_")&&!key.equals("frame_generation_mode")&&!key.equals("frame_generation_parallel"))result.add(id("frame_generation_parallel"));
        if(key.equals("frame_generation_cell")||key.equals("frame_generation_search")||key.equals("frame_generation_rejection"))result.add(id("frame_generation_multiplier"));
        if(key.startsWith("frame_generation_")&&!key.equals("frame_generation_mode")&&!key.equals("frame_generation_amortize")&&!key.equals("frame_generation_parallel"))result.add(id("frame_generation_amortize"));
        String parent = PARENTS.get(key), override = override(key);
        if (parent != null) result.add(id(parent));
        if (override != null) result.add(id(override));
        if (key.equals("create_visibility") || key.startsWith("factory_") || key.equals("be_render_distance")) result.add(id("block_models"));
        if (key.startsWith("factory_")) result.add(id("create_visibility"));
        if (key.equals("factory_connection_distance")) result.add(id("factory_connection_mode"));
        if (key.equals("tick_interval") || key.equals("hidden_grace") || key.equals("protected_distance") || key.equals("create_tick_distance") || key.equals("create_tick_interval")) result.add(id("create_tick_mode"));
        return result.toArray(ResourceLocation[]::new);
    }
    private static boolean allowed(String key, net.caffeinemc.mods.sodium.api.config.ConfigState state) {
        if (key.equals("enabled") || key.equals("diagnostics")) return true;
        if (!state.readBooleanOption(id("enabled"))) return false;
        if(key.startsWith("frame_generation_")&&!key.equals("frame_generation_mode")) {
            var fg=state.readEnumOption(id("frame_generation_mode"),FrameGenerationMode.class);
            if(key.equals("frame_generation_parallel"))return fg==FrameGenerationMode.SIMPLE;
            if(key.equals("frame_generation_amortize"))return fg!=FrameGenerationMode.OFF;
            boolean amortize=state.readBooleanOption(id("frame_generation_amortize"));
            if(key.equals("frame_generation_multiplier"))return fg!=FrameGenerationMode.OFF&&!amortize;
            boolean parallel=fg==FrameGenerationMode.SIMPLE&&state.readBooleanOption(id("frame_generation_parallel"));
            if(key.equals("frame_generation_fast")||key.equals("frame_generation_mark"))return fg!=FrameGenerationMode.OFF&&!parallel;
            if(key.equals("frame_generation_grid"))return fg==FrameGenerationMode.SIMPLE&&!parallel;
            if(parallel&&(key.equals("frame_generation_cell")||key.equals("frame_generation_search")||key.equals("frame_generation_rejection")))return true;
            if(key.equals("frame_generation_target")||key.equals("frame_generation_input")||key.equals("frame_generation_multiplier")||key.equals("frame_generation_mark")||key.equals("frame_generation_counter")||key.equals("frame_generation_fast"))return fg!=FrameGenerationMode.OFF;
            if(key.equals("frame_generation_grid"))return fg==FrameGenerationMode.SIMPLE;
            if(key.equals("frame_generation_fsr_debug"))return fg==FrameGenerationMode.FSR;
            return fg==FrameGenerationMode.FSR&&(amortize||state.readIntOption(id("frame_generation_multiplier"))>2);
        }
        String parent = PARENTS.get(key), override = override(key);
        if (parent != null && !state.readBooleanOption(id(parent))) return false;
        if (override != null && state.readEnumOption(id(override), Visibility.class) == Visibility.ALL) return false;
        if ((key.equals("create_visibility") || key.startsWith("factory_")) && state.readEnumOption(id("block_models"), BlockModels.class) == BlockModels.HIDE) return false;
        if (key.startsWith("factory_") && state.readEnumOption(id("create_visibility"), Visibility.class) == Visibility.ALL) return false;
        if (key.equals("be_render_distance")) return state.readEnumOption(id("block_models"), BlockModels.class) == BlockModels.NEAR;
        if (key.startsWith("factory_") && !Slopium.loaded("create")) return false;
        if (key.startsWith("create_visual_") && (!Slopium.loaded("create") || !Slopium.loaded("flywheel"))) return false;
        if ((key.equals("flywheel_distance_radius") || key.equals("flywheel_interval")) && !Slopium.loaded("flywheel")) return false;
        if (key.equals("factory_connection_distance")) return state.readEnumOption(id("factory_connection_mode"), Connections.class) == Connections.NEAR;
        if (key.equals("tick_interval") || key.equals("hidden_grace") || key.equals("protected_distance") || key.equals("create_tick_distance") || key.equals("create_tick_interval")) {
            if (!Slopium.loaded("create")) return false;
            TickMode mode = state.readEnumOption(id("create_tick_mode"), TickMode.class);
            if (key.equals("protected_distance")) return mode == TickMode.ENTITY_CULLING && Slopium.loaded("entityculling");
            if (key.equals("create_tick_distance") || key.equals("create_tick_interval")) return mode == TickMode.DISTANCE;
            return mode == TickMode.OPAQUE || mode == TickMode.ANY || mode == TickMode.ENTITY_CULLING && Slopium.loaded("entityculling");
        }
        return true;
    }
    private static Component tooltip(String key) {
        var result = text(key + ".tooltip").copy();
        if(key.equals("frame_generation_mode")&&dev.slopium.framegen.FrameGeneration.conflictingGenerator())
            result=text("frame_generation.conflict").copy().withStyle(net.minecraft.ChatFormatting.RED).append("\n\n").append(result);
        String parent = PARENTS.get(key), fullCut = override(key);
        if (parent != null) result.append(Component.translatable("slopium.hint.parent", text(parent)));
        if (fullCut != null) result.append(Component.translatable("slopium.hint.override", text(fullCut)));
        if (key.equals("create_visibility") || key.startsWith("factory_"))
            result.append(Component.translatable("slopium.hint.override", text("block_models")));
        if (key.startsWith("factory_")) result.append(Component.translatable("slopium.hint.override", text("create_visibility")));
        if (key.equals("be_render_distance")) result.append(text("hint.block_radius"));
        return result;
    }
    private static boolean classPresent(String name) {
        return SlopiumOptions.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null;
    }
    private static ResourceLocation id(String key) { return ResourceLocation.fromNamespaceAndPath("slopium", key); }
    private static Component text(String key) { return Component.translatable("slopium." + key); }
}

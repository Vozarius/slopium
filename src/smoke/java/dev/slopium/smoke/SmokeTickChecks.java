package dev.slopium.smoke;

import com.mojang.logging.LogUtils;
import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;

/** Checks the actual transformed ticker's cached classification and boolean guard. */
final class SmokeTickChecks {
    static void run(Class<?> type) throws ReflectiveOperationException {
        check(type, "minecraft", "chest", false);
        if (Slopium.loaded("create")) check(type, "create", "shaft", Slopium.createTickIntegrationAvailable());
        LogUtils.getLogger().info("SLOPIUM_SMOKE: ticker binding classification and boolean guard passed");
    }

    private static void check(Class<?> type, String namespace, String name, boolean expected) throws ReflectiveOperationException {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, name));
        var be = ((EntityBlock) block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        var constructor = java.util.Arrays.stream(type.getDeclaredConstructors())
            .filter(c -> c.getParameterCount() == 3).findFirst().orElseThrow();
        constructor.setAccessible(true);
        // No world or ticker operation is needed: only the patched guard is invoked.
        Object bound = constructor.newInstance(null, be, null);
        var eligible = type.getDeclaredField("slopium$eligible");
        eligible.setAccessible(true);
        if (eligible.getBoolean(bound) != expected) throw new AssertionError("Classification failed for " + namespace + ':' + name);
        var guard = java.util.Arrays.stream(type.getDeclaredMethods())
            .filter(m -> m.getName().endsWith("slopium$allowClientTick")).findFirst().orElseThrow();
        guard.setAccessible(true);
        var config = SlopiumConfig.get();
        boolean enabled = config.enabled, throttle = config.throttleHiddenCreate, freeze = config.freezeCreateClientTicks;
        try {
            config.enabled = true;
            config.throttleHiddenCreate = true;
            config.freezeCreateClientTicks = true;
            var create = type.getDeclaredField("slopium$create");
            create.setAccessible(true);
            if (create.getBoolean(bound) != namespace.equals("create")) throw new AssertionError("Create freeze classification failed");
            if ((boolean) guard.invoke(bound, false)) throw new AssertionError("Missing world must retain original false guard");
            if (!(boolean) guard.invoke(bound, true)) throw new AssertionError("Unattached object must not be skipped");
            if (namespace.equals("create")) {
                var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
                var server = (net.minecraft.world.level.Level) unsafe.allocateInstance(net.minecraft.server.level.ServerLevel.class);
                be.setLevel(server);
                if (!(boolean) guard.invoke(bound, true)) throw new AssertionError("Diagnostic freeze must never skip server ticks");
                be.setLevel(null);
                if (!Slopium.sableAvailable()) {
                    var client = net.minecraft.client.Minecraft.getInstance();
                    var previous = client.level;
                    var level = (net.minecraft.client.multiplayer.ClientLevel) unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientLevel.class);
                    var side = net.minecraft.world.level.Level.class.getDeclaredField("isClientSide");
                    side.setAccessible(true); side.setBoolean(level, true);
                    var init = Class.forName("com.simibubi.create.foundation.blockEntity.SmartBlockEntity").getDeclaredField("initialized");
                    init.setAccessible(true);
                    try {
                        client.level = level; be.setLevel(level);
                        init.setBoolean(be, false);
                        if (!(boolean) guard.invoke(bound, true)) throw new AssertionError("Freeze must allow initialization");
                        init.setBoolean(be, true);
                        if ((boolean) guard.invoke(bound, true)) throw new AssertionError("Initialized client Create tick was not frozen");
                        config.enabled = false;
                        if (!(boolean) guard.invoke(bound, true)) throw new AssertionError("Global off must restore Create tick");
                        config.enabled = true; config.freezeCreateClientTicks = false; config.throttleHiddenCreate = false;
                        if (!(boolean) guard.invoke(bound, true)) throw new AssertionError("Freeze off must restore Create tick");
                    } finally { client.level = previous; be.setLevel(null); }
                }
            }
            config.throttleHiddenCreate = false;
            if (!(boolean) guard.invoke(bound, true)) throw new AssertionError("Disabled policy must allow ticks");
        } finally {
            config.enabled = enabled;
            config.throttleHiddenCreate = throttle;
            config.freezeCreateClientTicks = freeze;
        }
    }
}

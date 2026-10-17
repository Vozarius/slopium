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
        boolean enabled = config.enabled, throttle = config.throttleHiddenCreate;
        try {
            config.enabled = true;
            config.throttleHiddenCreate = true;
            if ((boolean) guard.invoke(bound, false)) throw new AssertionError("Missing world must retain original false guard");
            if (!(boolean) guard.invoke(bound, true)) throw new AssertionError("Unattached object must not be skipped");
            config.throttleHiddenCreate = false;
            if (!(boolean) guard.invoke(bound, true)) throw new AssertionError("Disabled policy must allow ticks");
        } finally {
            config.enabled = enabled;
            config.throttleHiddenCreate = throttle;
        }
    }
}

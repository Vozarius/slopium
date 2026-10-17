package dev.slopium.visibility;

import com.mojang.logging.LogUtils;
import dev.slopium.client.Slopium;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Optional integration. Unrecognized Sable versions fail open: do not cull. */
public final class SableGuard {
    private static boolean initialized, failed;
    private static MethodHandle container, inBounds;
    private SableGuard() {}

    public static boolean protects(Level level, BlockPos pos) {
        if (!Slopium.sableAvailable()) return false;
        if (!initialized) {
            initialized = true;
            try {
                Class<?> type = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
                container = MethodHandles.publicLookup().findStatic(type, "getContainer", MethodType.methodType(type, Level.class));
                inBounds = MethodHandles.publicLookup().findVirtual(type, "inBounds", MethodType.methodType(boolean.class, BlockPos.class));
            } catch (ReflectiveOperationException | LinkageError e) {
                failed = true;
                LogUtils.getLogger().warn("Slopium: unknown Sable API; visibility shortcuts disabled", e);
            }
        }
        if (failed) return true;
        try {
            Object value = container.invoke(level);
            // No container during loading: avoid making visibility decisions from incomplete data.
            return value == null || (boolean) inBounds.invoke(value, pos);
        } catch (Throwable e) {
            failed = true;
            LogUtils.getLogger().warn("Slopium: Sable lookup failed; visibility shortcuts disabled", e);
            return true;
        }
    }
}


package dev.slopium.smoke;

import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.slopium.client.*;
import dev.slopium.visibility.VisualFrameGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

final class SmokeCreateVisualChecks {
    static void run() throws ReflectiveOperationException {
        var c = SlopiumConfig.get(); var mc = Minecraft.getInstance(); var previous = mc.level;
        var level = (ClientLevel) SmokeRadicalChecks.allocate(ClientLevel.class);
        var side = Level.class.getDeclaredField("isClientSide"); side.setAccessible(true); side.setBoolean(level, true);
        DynamicVisual.Context context = new DynamicVisual.Context() {
            public net.minecraft.client.Camera camera() { return mc.gameRenderer.getMainCamera(); }
            public org.joml.FrustumIntersection frustum() { return null; }
            public float partialTick() { return 0; }
            public dev.engine_room.flywheel.api.visual.DistanceUpdateLimiter limiter() { return distance -> true; }
        };
        String[][] cases = {
            {"kinetics.press.PressVisual", "mechanical_press"},
            {"kinetics.mixer.MixerVisual", "mechanical_mixer"},
            {"logistics.funnel.FunnelVisual", "andesite_funnel"}
        };
        try {
            mc.level = level;
            for (String[] test : cases) {
                var type = Class.forName("com.simibubi.create.content." + test[0]);
                Object visual = SmokeRadicalChecks.allocate(type);
                var gate = Arrays.stream(type.getDeclaredFields()).filter(f -> f.getType() == VisualFrameGate.class).findFirst().orElseThrow(); gate.setAccessible(true); gate.set(visual, new VisualFrameGate());
                var hook = Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$visualFrame")).findFirst().orElseThrow();
                hook.setAccessible(true);
                c.throttleCreateVisuals = true; c.createVisualDistance = 16; c.createVisualInterval = 3;
                // Null BE must fail open (also avoids touching an uninitialized Sable test world).
                var ci = new CallbackInfo("frame", true); hook.invoke(visual, context, ci);
                if (ci.isCancelled()) throw new AssertionError("Unbound visual skipped");
                if (Slopium.sableAvailable()) continue;
                gate.set(visual, new VisualFrameGate());
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", test[1]));
                var be = ((EntityBlock) block).newBlockEntity(new BlockPos(200, 0, 0), block.defaultBlockState());
                be.setLevel(level);
                var beField = Class.forName("dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual").getDeclaredField("blockEntity");
                beField.setAccessible(true); beField.set(visual, be);
                ci = new CallbackInfo("first", true); hook.invoke(visual, context, ci);
                if (ci.isCancelled()) throw new AssertionError("First visual update skipped");
                long before = SlopiumStats.CREATE_VISUALS_SKIPPED.sum();
                type.getMethod("beginFrame", DynamicVisual.Context.class).invoke(visual, context);
                if (SlopiumStats.CREATE_VISUALS_SKIPPED.sum() != before + 1) throw new AssertionError("Actual visual entry not skipped");
                c.createVisualDistance = 512;
                ci = new CallbackInfo("near", true); hook.invoke(visual, context, ci);
                if (ci.isCancelled()) throw new AssertionError("Nearby visual skipped");
                c.createVisualDistance = 16; c.enabled = false;
                ci = new CallbackInfo("off", true); hook.invoke(visual, context, ci);
                if (ci.isCancelled()) throw new AssertionError("Global off visual skipped");
                c.enabled = true;
                be.setLevel((Level) SmokeRadicalChecks.allocate(net.minecraft.server.level.ServerLevel.class));
                ci = new CallbackInfo("server", true); hook.invoke(visual, context, ci);
                if (ci.isCancelled()) throw new AssertionError("Server level passed visual policy");
            }
        } finally { mc.level = previous; }
    }
}

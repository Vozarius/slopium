package dev.slopium.smoke;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import dev.slopium.client.*;
import dev.slopium.visibility.RenderBudget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import java.util.Arrays;

final class SmokeBudgetChecks {
    static void run() throws ReflectiveOperationException {
        var c = SlopiumConfig.get();
        var saved = new java.util.HashMap<java.lang.reflect.Field, Object>();
        for (var f : SlopiumConfig.class.getFields()) saved.put(f, f.get(c));
        try {
            c.enabled = c.diagnostics = true;
            if (RenderBudget.outside(new AABB(-100, -1, -1, 100, 1, 1), 0, 0, 0, 0)) throw new AssertionError("Large bounds cut");
            if (!RenderBudget.outside(new AABB(30, 0, 0, 31, 1, 1), 0, 0, 0, 16)) throw new AssertionError("Far bounds retained");
            var mc = Minecraft.getInstance();
            var stack = new ItemStack(Items.DIAMOND, 64);
            var item = new ItemEntity(null, 100, 0, 0, stack, 0, 0, 0) {
                @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return null; }
            };
            var renderer = mc.getEntityRenderDispatcher().getRenderer(item);
            c.itemImpostors = true; c.itemImpostorDistance = 0;
            long sprites = SlopiumStats.itemImpostorsRendered;
            renderer.render(item, 0, 0, new PoseStack(), mc.renderBuffers().bufferSource(), 15728880);
            if (SlopiumStats.itemImpostorsRendered != sprites + 1 || stack.getCount() != 64) throw new AssertionError("Impostor/count failed");
            item.setCustomName(net.minecraft.network.chat.Component.literal("protected"));
            renderer.render(item, 0, 0, new PoseStack(), mc.renderBuffers().bufferSource(), 15728880);
            if (SlopiumStats.itemImpostorsRendered != sprites + 1) throw new AssertionError("Named item replaced");
            item.setCustomName(null);
            c.enabled = false;
            renderer.render(item, 0, 0, new PoseStack(), mc.renderBuffers().bufferSource(), 15728880);
            if (SlopiumStats.itemImpostorsRendered != sprites + 1) throw new AssertionError("Impostor ignores global off");
            c.enabled = true; c.itemImpostors = false;
            c.itemStackLod = true; c.itemLodDistance = 0; c.limitItemRendering = false;
            long before = SlopiumStats.itemCopiesSkipped;
            renderer.render(item, 0, 0, new PoseStack(), mc.renderBuffers().bufferSource(), 15728880);
            if (SlopiumStats.itemCopiesSkipped != before + 4 || stack.getCount() != 64) throw new AssertionError("World stack LOD/count failed");
            c.enabled = false;
            renderer.render(item, 0, 0, new PoseStack(), mc.renderBuffers().bufferSource(), 15728880);
            if (SlopiumStats.itemCopiesSkipped != before + 4) throw new AssertionError("Disabled LOD leaked");
            c.enabled = true; c.limitItemRendering = true; c.itemRenderDistance = 0;
            before = SlopiumStats.itemDrawsSkipped;
            renderer.render(item, 0, 0, null, null, 0);
            if (SlopiumStats.itemDrawsSkipped != before + 1) throw new AssertionError("Item range did not cut before render");
            // Context must also be restored when a custom rendering path throws.
            c.limitItemRendering = false;
            try { renderer.render(item, 0, 0, null, null, 0); throw new AssertionError("Expected null-pose failure"); }
            catch (NullPointerException expected) { }
            var itemClass = Class.forName("net.minecraft.client.renderer.entity.ItemEntityRenderer");
            var copies = Arrays.stream(itemClass.getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$copies")).findFirst().orElseThrow();
            copies.setAccessible(true);
            if ((int) copies.invoke(null, 5) != 5) throw new AssertionError("World LOD leaked into GUI/static renderer");
            var sodiumClass = Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
            var pass = Arrays.stream(sodiumClass.getDeclaredMethods()).filter(m -> m.getName().equals("renderBlockEntities") && m.getParameterCount() == 6).findFirst().orElseThrow();
            c.disableBlockEntityRendering = true;
            before = SlopiumStats.sodiumPassesSkipped;
            pass.invoke(allocate(sodiumClass), null, null, null, null, 0f, null);
            if (SlopiumStats.sodiumPassesSkipped != before + 1) throw new AssertionError("Sodium early pass not cut");
            if (Slopium.createAvailable()) {
                var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "shaft"));
                var mechanism = ((net.minecraft.world.level.block.EntityBlock)block).newBlockEntity(net.minecraft.core.BlockPos.ZERO, block.defaultBlockState());
                c.disableBlockEntityRendering = false; c.hideAllCreateModels = true;
                before = SlopiumStats.blockEntityDrawsSkipped;
                mc.getBlockEntityRenderDispatcher().render(mechanism, 0, null, null);
                if (SlopiumStats.blockEntityDrawsSkipped != before + 1) throw new AssertionError("Create-only dispatcher cut not reached");
                var renderCreate = Arrays.stream(sodiumClass.getDeclaredMethods()).filter(m -> m.getName().equals("renderBlockEntity")).findFirst().orElseThrow();
                renderCreate.setAccessible(true);
                before = SlopiumStats.wallCreateDrawsSkipped;
                renderCreate.invoke(null, null, null, null, 0f, null, 0d, 0d, 0d, mc.getBlockEntityRenderDispatcher(), mechanism, null, null);
                if (SlopiumStats.wallCreateDrawsSkipped != before + 1) throw new AssertionError("Create-only Sodium cut not reached");
                var dispatcher = mc.getBlockEntityRenderDispatcher();
                var guard = Arrays.stream(dispatcher.getClass().getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$cut")).findFirst().orElseThrow();guard.setAccessible(true);
                var chest = new net.minecraft.world.level.block.entity.ChestBlockEntity(net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
                var ci = new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("chest", true);
                guard.invoke(dispatcher, ci, chest);
                if (ci.isCancelled()) throw new AssertionError("Create-only mode also hid a vanilla chest");
                c.enabled = false; ci = new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("off", true);
                guard.invoke(dispatcher, ci, mechanism);
                if (ci.isCancelled()) throw new AssertionError("Create-only cut ignored global off");
                c.enabled = true; c.hideAllCreateModels = false;
                LogUtils.getLogger().info("SLOPIUM_SMOKE: Create-only cuts reached actual vanilla and Sodium entries; vanilla chest and global off preserved");
            }
            if (!Slopium.sableAvailable()) {
                var be = new net.minecraft.world.level.block.entity.ChestBlockEntity(net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
                be.setLevel((net.minecraft.client.multiplayer.ClientLevel) allocate(net.minecraft.client.multiplayer.ClientLevel.class));
                c.disableBlockEntityRendering = false; c.limitBlockEntityRendering = true; c.blockEntityRenderDistance = 16;
                var renderBe = Arrays.stream(sodiumClass.getDeclaredMethods()).filter(m -> m.getName().equals("renderBlockEntity")).findFirst().orElseThrow();
                renderBe.setAccessible(true);
                before = SlopiumStats.blockEntityDrawsSkipped;
                renderBe.invoke(null, null, null, null, 0f, null, 1000d, 1000d, 1000d, mc.getBlockEntityRenderDispatcher(), be, null, null);
                if (SlopiumStats.blockEntityDrawsSkipped != before + 1) throw new AssertionError("Sodium radius entry not cut");
            }
            var shadow = Arrays.stream(net.minecraft.client.renderer.entity.EntityRenderDispatcher.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("renderShadow")).findFirst().orElseThrow();
            shadow.setAccessible(true);
            Object[] shadowArgs = new Object[shadow.getParameterCount()];
            for (int i = 0; i < shadowArgs.length; i++) {
                var type = shadow.getParameterTypes()[i];
                if (type == float.class) shadowArgs[i] = 0f;
                if (type == double.class) shadowArgs[i] = 0d;
                if (type == int.class) shadowArgs[i] = 0;
            }
            c.disableEntityShadows = true;
            before = SlopiumStats.shadowsSkipped;
            shadow.invoke(null, shadowArgs);
            if (SlopiumStats.shadowsSkipped != before + 1) throw new AssertionError("Shadow entry not cut");
            if (Slopium.loaded("xaeroworldmap")) {
                var type = Class.forName("xaero.map.graphics.TextureUploader");
                var guard = Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().endsWith("slopium$measurementFinished")).findFirst().orElseThrow();
                guard.setAccessible(true); Object uploader = allocate(type);
                c.pauseXaeroBenchmark = true;
                if (!(boolean) guard.invoke(uploader, false)) throw new AssertionError("Xaero measurement not paused");
                c.enabled = false;
                if ((boolean) guard.invoke(uploader, false)) throw new AssertionError("Xaero global off failed");
                c.enabled = true; c.pauseXaeroBenchmark = false;
                if ((boolean) guard.invoke(uploader, false)) throw new AssertionError("Xaero resume failed");
            }
            if (Slopium.loaded("flywheel")) {
                for (String name : new String[]{"NonLimiter", "BandedPrimeLimiter"}) {
                    var type = Class.forName("dev.engine_room.flywheel.impl.visual." + name);
                    Object limiter = type.getConstructor().newInstance();
                    var tick = type.getMethod("tick"); var update = type.getMethod("shouldUpdate", double.class);
                    c.throttleFlywheelDistance = true; c.flywheelFullRateDistance = 16; c.flywheelFrameInterval = 3;
                    int allowed = 0;
                    for (int i = 0; i < 9; i++) {
                        tick.invoke(limiter);
                        if ((boolean) update.invoke(limiter, 500d)) allowed++;
                        if (!(boolean) update.invoke(limiter, 0d)) throw new AssertionError("Near visual throttled");
                    }
                    if (allowed != 3) throw new AssertionError("Flywheel interval/starvation: " + allowed);
                    c.throttleFlywheelDistance = false;
                    boolean normal = (boolean) update.invoke(limiter, 500d);
                    c.throttleFlywheelDistance = true; c.enabled = false;
                    if ((boolean) update.invoke(limiter, 500d) != normal) throw new AssertionError("Flywheel global off");
                    c.enabled = true;
                }
            }
            LogUtils.getLogger().info("SLOPIUM_SMOKE: render budget: actual item LOD/range, stack preservation, exception cleanup, early Sodium pass, optional Xaero and Flywheel policies passed");
        } finally {
            for (var e : saved.entrySet()) e.getKey().set(c, e.getValue());
            SlopiumStats.reset();
        }
    }
    private static Object allocate(Class<?> type) throws ReflectiveOperationException {
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return ((sun.misc.Unsafe) f.get(null)).allocateInstance(type);
    }
}

package dev.slopium.smoke;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Regression: the world's six-argument NeoForge overload must reach our hook. */
final class SmokeBreakingChecks {
    @SuppressWarnings("deprecation")
    static void run() {
        var config = SlopiumConfig.get();
        boolean enabled = config.enabled, cull = config.cullBreakingOverlays, diagnostics = config.diagnostics;
        long checks = SlopiumStats.breakingOverlaysChecked;
        try {
            config.enabled = config.cullBreakingOverlays = config.diagnostics = true;
            var renderer = Minecraft.getInstance().getBlockRenderer();
            var air = Blocks.AIR.defaultBlockState();
            // AIR has no geometry; no world or vertex consumer is accessed.
            renderer.renderBreakingTexture(air, BlockPos.ZERO, null, new PoseStack(), null, ModelData.EMPTY);
            if (SlopiumStats.breakingOverlaysChecked != checks + 1)
                throw new AssertionError("NeoForge breaking overload bypassed Slopium");
            renderer.renderBreakingTexture(air, BlockPos.ZERO, null, new PoseStack(), null);
            if (SlopiumStats.breakingOverlaysChecked != checks + 2)
                throw new AssertionError("Vanilla breaking overload did not delegate exactly once");
            config.enabled = false;
            renderer.renderBreakingTexture(air, BlockPos.ZERO, null, new PoseStack(), null, ModelData.EMPTY);
            if (SlopiumStats.breakingOverlaysChecked != checks + 2)
                throw new AssertionError("Disabled Slopium should bypass the culling check");
            LogUtils.getLogger().info("SLOPIUM_SMOKE: NeoForge and vanilla breaking overloads reach one hook; disabled bypass passed");
        } finally {
            config.enabled = enabled;
            config.cullBreakingOverlays = cull;
            config.diagnostics = diagnostics;
            SlopiumStats.breakingOverlaysChecked = checks;
        }
    }
}

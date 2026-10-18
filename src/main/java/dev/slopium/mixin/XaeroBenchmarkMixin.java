package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.client.SlopiumStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "xaero.map.graphics.TextureUploader")
public abstract class XaeroBenchmarkMixin {
    // Only the two measurement guards in uploadTextures, NOT requestUpload's scheduling estimates.
    // Existing benchmark state is untouched; toggling off resumes accurate GPU measurements.
    @ModifyExpressionValue(method = "uploadTextures", at = @At(value = "INVOKE",
        target = "Lxaero/map/graphics/TextureUploadBenchmark;isFinished(I)Z"), require = 2)
    private boolean slopium$measurementFinished(boolean original) {
        var c = SlopiumConfig.get();
        if (!original && c.enabled && c.pauseXaeroBenchmark) {
            if (c.diagnostics) SlopiumStats.xaeroMeasurementGuardsSkipped++;
            return true;
        }
        return original;
    }
}

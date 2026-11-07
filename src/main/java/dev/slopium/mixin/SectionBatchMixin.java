package dev.slopium.mixin;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=MultiDrawBatch.class,remap=false)
public abstract class SectionBatchMixin {
    @Inject(method={"clear","delete"},at=@At("HEAD"))
    private void slopium$invalidateCommands(CallbackInfo ci){dev.slopium.visibility.GpuSections.geometryChanged();dev.slopium.visibility.GpuSections.dropBatch((MultiDrawBatch)(Object)this);}
}

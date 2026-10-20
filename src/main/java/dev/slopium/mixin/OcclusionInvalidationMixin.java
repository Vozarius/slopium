package dev.slopium.mixin;
import dev.slopium.visibility.WallOcclusion;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public abstract class OcclusionInvalidationMixin {
    @Inject(method = {"sendBlockUpdated", "setBlocksDirty"}, at = @At("HEAD"))
    private void slopium$invalidateWalls(CallbackInfo ci) { WallOcclusion.invalidate(); }
}

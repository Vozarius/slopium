package dev.slopium.mixin;
import dev.slopium.visibility.VisibilityGeometry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public abstract class OcclusionInvalidationMixin {
    @Inject(method="sendBlockUpdated",at=@At("HEAD"))
    private void slopium$blockUpdated(BlockPos pos,BlockState oldState,BlockState newState,int flags,CallbackInfo ci) {
        VisibilityGeometry.invalidate(pos);
    }
    @Inject(method="setBlocksDirty",at=@At("HEAD"))
    private void slopium$blockDirty(BlockPos pos,BlockState oldState,BlockState newState,CallbackInfo ci) {
        VisibilityGeometry.invalidate(pos);
    }
}

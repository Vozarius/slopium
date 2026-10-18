package dev.slopium.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual", remap = false)
public interface VisualBlockEntityAccess {
    @Accessor("blockEntity") BlockEntity slopium$getBlockEntity();
}

package dev.slopium.mixin;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.slopium.visibility.WrappedVisualAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.cake.azimuth.behaviour.render.WrappedVisual", remap = false)
public interface AzimuthVisualAccessor extends WrappedVisualAccess {
    @Override @Accessor("delegateVisual") BlockEntityVisual<?> slopium$delegateVisual();
}

package dev.slopium.visibility;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;

/** Only known wrappers opt in. Behaviour extensions are not owned by the delegate. */
public interface WrappedVisualAccess {
    BlockEntityVisual<?> slopium$delegateVisual();
}

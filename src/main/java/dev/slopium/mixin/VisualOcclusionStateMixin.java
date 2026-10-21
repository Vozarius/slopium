package dev.slopium.mixin;
import dev.slopium.visibility.VisualOcclusionState;
import org.spongepowered.asm.mixin.*;
@Mixin(targets = "dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual", remap = false)
public abstract class VisualOcclusionStateMixin implements VisualOcclusionState {
    @Unique private volatile boolean slopium$hidden;
    @Unique private volatile long slopium$hiddenFrame = Long.MIN_VALUE;
    @Unique private volatile long slopium$until = Long.MIN_VALUE;
    public void slopium$setOccluded(boolean hidden, long frame) { slopium$hidden = hidden; slopium$hiddenFrame = frame; slopium$until = frame; }
    public void slopium$setOccludedUntil(boolean hidden, long frame, long until) { slopium$hidden=hidden; slopium$hiddenFrame=frame; slopium$until=until; }
    public boolean slopium$isOccluded(long frame) { return frame >= slopium$hiddenFrame && frame <= slopium$until && slopium$hidden; }
}

package dev.slopium.mixin;

import dev.slopium.visibility.PausedRotation;
import dev.engine_room.flywheel.api.instance.Instance;
import org.spongepowered.asm.mixin.*;

@Mixin(targets = "com.simibubi.create.content.kinetics.base.RotatingInstance", remap = false)
public abstract class RotatingInstancePauseMixin implements PausedRotation {
    @Shadow public float rotationalSpeed;
    @Shadow public float rotationOffset;
    @Unique private volatile boolean slopium$paused;
    @Unique private volatile float slopium$angle;
    public void slopium$pauseRotation(boolean paused, float seconds) {
        if (slopium$paused == paused) return;
        if (paused) slopium$angle = (rotationOffset + seconds * rotationalSpeed) % 360f;
        slopium$paused = paused;
        ((Instance)(Object)this).setChanged();
    }
    public float slopium$renderSpeed(float speed) { return slopium$paused ? 0 : speed; }
    public float slopium$renderOffset(float offset) { return slopium$paused ? slopium$angle : offset; }
}

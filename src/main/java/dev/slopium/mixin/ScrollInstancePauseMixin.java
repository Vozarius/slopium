package dev.slopium.mixin;
import dev.slopium.visibility.PausedScroll;
import dev.engine_room.flywheel.api.instance.Instance;
import org.spongepowered.asm.mixin.*;
@Mixin(targets={"com.simibubi.create.content.processing.burner.ScrollInstance","com.simibubi.create.content.processing.burner.ScrollTransformedInstance"},remap=false)
public abstract class ScrollInstancePauseMixin implements PausedScroll {
    @Shadow(remap=false) public float speedU,speedV,offsetU,offsetV;
    @Unique private boolean slopium$paused;
    @Unique private float slopium$u,slopium$v;
    public void slopium$pauseRotation(boolean pause,float seconds){
        if(slopium$paused==pause)return;
        if(pause){slopium$u=(offsetU+seconds*20*speedU)%1f;slopium$v=(offsetV+seconds*20*speedV)%1f;}
        slopium$paused=pause;((Instance)(Object)this).setChanged();
    }
    public float slopium$renderSpeed(float value){return slopium$paused?0:value;}
    public float slopium$renderOffset(float value){return value;}
    public float slopium$scrollOffset(float value,boolean v){return slopium$paused?v?slopium$v:slopium$u:value;}
}

package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.slopium.visibility.GpuSections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="dev.engine_room.flywheel.backend.engine.indirect.IndirectCullingGroup",remap=false)
public abstract class FlywheelSectionDispatchMixin {
    @WrapOperation(method="dispatchCull",at=@At(value="INVOKE",target="Lorg/lwjgl/opengl/GL43;glDispatchCompute(III)V"))
    private void slopium$mask(int x,int y,int z,Operation<Void> original){
        long[] saved=GpuSections.bindFlywheel();try{original.call(x,y,z);}finally{GpuSections.restoreFlywheel(saved);}
    }
}

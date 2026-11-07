package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.slopium.visibility.GpuGlassDepth;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="dev.engine_room.flywheel.backend.engine.EngineImpl",remap=false)
public abstract class FlywheelGpuContextMixin {
    @WrapMethod(method="render")
    private void slopium$depthContext(RenderContext context,Operation<Void> original) {
        var previous=GpuGlassDepth.context(context);
        boolean old=dev.slopium.visibility.GpuSections.flywheelWorld(context.level()==net.minecraft.client.Minecraft.getInstance().level&&dev.slopium.visibility.GpuSections.matches(context.modelView(),context.projection(),context.camera().getPosition()));
        try {original.call(context);} finally {GpuGlassDepth.context(previous);dev.slopium.visibility.GpuSections.flywheelWorld(old);}
    }
}

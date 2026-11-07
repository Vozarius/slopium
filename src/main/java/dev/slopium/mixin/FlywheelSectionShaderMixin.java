package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.engine_room.flywheel.backend.glsl.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.Function;
@Mixin(targets="dev.engine_room.flywheel.backend.glsl.ShaderSources$SourceFinder",remap=false)
public abstract class FlywheelSectionShaderMixin {
    @WrapOperation(method="readResource",at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/backend/glsl/SourceFile;parse(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/String;)Ldev/engine_room/flywheel/backend/glsl/LoadResult;"))
    private LoadResult slopium$mask(Function<ResourceLocation,LoadResult> resolver,ResourceLocation name,String source,Operation<LoadResult> original){
        if(name.toString().equals("flywheel:internal/indirect/cull.glsl")){
            try(var in=dev.slopium.visibility.GpuSections.class.getResourceAsStream("/assets/slopium/shaders/sections/flywheel.glsl")){
                String helper=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                source=source.replace("bool _flw_isVisible(",helper+"\nbool _flw_isVisible(").replace("    bool isVisible = _flw_testSphere(center, radius);","    if(matrixIndex==0u){int sectionResult=slopiumSectionVisibility(center+vec3(flw_renderOrigin),radius);if(sectionResult==0)return false;}\n    bool isVisible = _flw_testSphere(center, radius);");
            }catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        return original.call(resolver,name,source);
    }
}

package dev.slopium.mixin;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ShaderLoader.class,remap=false)
public abstract class SectionShaderMixin {
    @Inject(method="getShaderSource",at=@At("RETURN"),cancellable=true)
    private static void slopium$id(ResourceLocation name,CallbackInfoReturnable<String> ci){
        if(!name.getNamespace().equals("sodium")||!name.getPath().startsWith("blocks/block_layer_opaque."))return;
        dev.slopium.visibility.GpuSections.shadersReloaded();
        String s=ci.getReturnValue();
        if(name.getPath().endsWith("vsh"))s=s.replace("void main() {","flat out uint slopiumSection;\nuniform uint slopiumBase;\nvoid main() {").replace("_vert_init();","_vert_init(); slopiumSection=slopiumBase+_draw_id+1u;");
        else s=s.replace("void main() {","flat in uint slopiumSection;\nlayout(location=1) out uint slopiumId;\nuniform int slopiumDepth;\nvoid main() {").replace("    fragColor =", "    slopiumId=slopiumSection;\n    if(slopiumDepth!=0){fragColor=vec4(0);return;}\n    fragColor =");
        s=s.replace("if (color.a < _material_alpha_cutoff(v_Material))", "if (color.a < _material_alpha_cutoff(v_Material) && !(slopiumDepth==2 && (v_Material & 128u)!=0u))");
        ci.setReturnValue(s);
    }
}

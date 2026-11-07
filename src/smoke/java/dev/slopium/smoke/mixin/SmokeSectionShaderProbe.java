package dev.slopium.smoke.mixin;
import net.caffeinemc.mods.sodium.client.gl.shader.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GlShader.class,remap=false)
public abstract class SmokeSectionShaderProbe {
    @Inject(method="<init>",at=@At("RETURN"))
    private void source(ShaderType type,ResourceLocation name,ShaderParser.ParsedShader parsed,CallbackInfo ci){
        if(!Boolean.getBoolean("slopium.sectionSmoke")||!name.getPath().contains("block_layer_opaque"))return;
        try{var folder=java.nio.file.Path.of("section-shaders");java.nio.file.Files.createDirectories(folder);java.nio.file.Files.writeString(folder.resolve(type+"-"+Integer.toUnsignedString(parsed.src().hashCode())+".glsl"),parsed.src());}catch(java.io.IOException e){throw new AssertionError(e);}
    }
}

package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.slopium.visibility.GpuSections;
import net.caffeinemc.mods.sodium.client.render.chunk.*;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.gl.device.*;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlTessellation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=DefaultChunkRenderer.class,remap=false)
public abstract class SectionDrawMixin {
    @WrapMethod(method="render")
    private void slopium$frame(ChunkRenderMatrices matrices,CommandList commands,ChunkRenderListIterable lists,TerrainRenderPass pass,CameraTransform camera,boolean sorted,Operation<Void> original){
        GpuSections.before(matrices,lists,pass,camera,p->original.call(matrices,commands,lists,p,camera,sorted));
        original.call(matrices,commands,lists,pass,camera,sorted);
    }
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;executeDrawBatch(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/gl/tessellation/GlTessellation;Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;)V"))
    private void slopium$draw(CommandList commands,GlTessellation tess,MultiDrawBatch batch,Operation<Void> original,@Local RenderRegion region,@Local(argsOnly=true) TerrainRenderPass pass){
        if(!GpuSections.draw(commands,tess,batch,region,pass))original.call(commands,tess,batch);
    }
}

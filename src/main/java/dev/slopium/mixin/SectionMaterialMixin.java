package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.slopium.visibility.SectionMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=BlockRenderer.class,remap=false)
public abstract class SectionMaterialMixin {
    @WrapMethod(method="renderModel")
    private void slopium$material(BakedModel model,BlockState state,BlockPos pos,BlockPos origin,Operation<Void> original){
        boolean old=SectionMaterials.GLASS.get();SectionMaterials.GLASS.set(SectionMaterials.glass(state.getBlock()));
        try{original.call(model,state,pos,origin);}finally{SectionMaterials.GLASS.set(old);}
    }
}

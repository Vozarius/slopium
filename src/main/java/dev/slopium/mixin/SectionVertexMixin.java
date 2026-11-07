package dev.slopium.mixin;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Tag material before either Sodium's or Veil's encoder. Do not assume a vertex stride. */
@Mixin(value=ChunkMeshBufferBuilder.class,remap=false)
public abstract class SectionVertexMixin {
    @ModifyArg(method={"push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V","writeExternal"},at=@At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder;write(JI[Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)J"),index=1)
    private int slopium$material(int material){
        return dev.slopium.visibility.SectionMaterials.GLASS.get()?material|128:material;
    }
}

package dev.slopium.smoke.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=ChunkMeshBufferBuilder.class,remap=false,priority=500)
public abstract class SmokeSectionMaterialProbe {
    private static final java.util.Set<String> reported=java.util.concurrent.ConcurrentHashMap.newKeySet();
    @WrapOperation(method={"push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V","writeExternal"},at=@At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder;write(JI[Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)J"))
    private long probe(ChunkVertexEncoder encoder,long ptr,int material,ChunkVertexEncoder.Vertex[] vertices,int section,Operation<Long> original){
        if(reported.add("any:"+encoder.getClass().getName()))com.mojang.logging.LogUtils.getLogger().info("SECTION_ENCODER {} glassContext={}",encoder.getClass(),dev.slopium.visibility.SectionMaterials.GLASS.get());
        long end=original.call(encoder,ptr,material,vertices,section);
        if(dev.slopium.visibility.SectionMaterials.GLASS.get()){
            int encoded=(org.lwjgl.system.MemoryUtil.memGetInt(ptr+16)>>>16)&255;
            if((encoded&128)==0)throw new AssertionError("Glass tag lost: "+encoder.getClass()+" material="+material+" encoded="+encoded);
            if(reported.add(encoder.getClass().getName()))com.mojang.logging.LogUtils.getLogger().info("SECTION_MATERIAL tag verified: {} material={} encoded={}",encoder.getClass(),material,encoded);
        }
        return end;
    }
}

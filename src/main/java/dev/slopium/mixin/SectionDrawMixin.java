package dev.slopium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.slopium.visibility.*;
import net.caffeinemc.mods.sodium.client.render.chunk.*;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=DefaultChunkRenderer.class,remap=false)
public abstract class SectionDrawMixin {
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;getCachedBatch(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;"))
    private MultiDrawBatch slopium$prepare(RenderRegion region,TerrainRenderPass pass,Operation<MultiDrawBatch> original){
        ((SectionRegionState)region).slopium$prepareSections();return original.call(region,pass);
    }
    @WrapOperation(method="fillCommandBuffer",at=@At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;sectionsWithGeometryIterator(Z)Lnet/caffeinemc/mods/sodium/client/util/iterator/ByteIterator;"))
    private static ByteIterator slopium$filter(ChunkRenderList list,boolean reverse,Operation<ByteIterator> original,@Local(argsOnly=true) RenderRegion region){
        ByteIterator input=original.call(list,reverse);if(input==null||!SectionVisibility.active())return input;
        var state=(SectionRegionState)region;
        return new ByteIterator(){int next=-1;
            public boolean hasNext(){while(next<0&&input.hasNext()){int i=input.nextByteAsInt();if(!state.slopium$hiddenSection(i))next=i;else SectionVisibility.hiddenDraws++;}return next>=0;}
            public int nextByteAsInt(){if(!hasNext())throw new java.util.NoSuchElementException();int value=next;next=-1;return value;}
        };
    }
}

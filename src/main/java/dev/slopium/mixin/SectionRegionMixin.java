package dev.slopium.mixin;

import dev.slopium.client.Slopium;
import dev.slopium.visibility.*;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import org.spongepowered.asm.mixin.*;

@Mixin(value=RenderRegion.class,remap=false)
public abstract class SectionRegionMixin implements SectionRegionState {
    @Unique private long slopium$frame=Long.MIN_VALUE;
    @Unique private boolean slopium$hadHidden;
    @Unique private final long[] slopium$mask=new long[(RenderRegion.REGION_SIZE+63)/64];
    @Unique private final long[] slopium$next=new long[(RenderRegion.REGION_SIZE+63)/64];
    public void slopium$prepareSections(){
        if(!SectionVisibility.active()&&!slopium$hadHidden)return;
        if(slopium$frame==Slopium.frame())return;slopium$frame=Slopium.frame();
        long started=dev.slopium.client.SlopiumConfig.get().diagnostics?System.nanoTime():0;
        var region=(RenderRegion)(Object)this;
        java.util.Arrays.fill(slopium$next,0);
        slopium$hadHidden=false;
        if(SectionVisibility.active()){
            var iterator=region.getRenderList().sectionsWithGeometryIterator(false);
            if(iterator!=null)while(iterator.hasNext()){
                int i=iterator.nextByteAsInt();
                if(SectionVisibility.request(region.getChunkX()+LocalSectionIndex.unpackX(i),region.getChunkY()+LocalSectionIndex.unpackY(i),region.getChunkZ()+LocalSectionIndex.unpackZ(i))){slopium$next[i>>>6]|=1L<<(i&63);slopium$hadHidden=true;}
            }
        }
        if(!java.util.Arrays.equals(slopium$mask,slopium$next)){
            System.arraycopy(slopium$next,0,slopium$mask,0,slopium$mask.length);
            region.clearAllCachedBatches();
        }
        if(started!=0){SectionVisibility.regions++;SectionVisibility.regionNanos+=System.nanoTime()-started;}
    }
    public boolean slopium$hiddenSection(int i){return (slopium$mask[i>>>6]&(1L<<(i&63)))!=0;}
}

package dev.slopium.visibility;
import dev.slopium.mixin.*;
import net.minecraft.core.BlockPos;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
/** Do not let a newly placed wall hide terrain before its mesh has been uploaded. */
public final class SectionMeshes {
    private SectionMeshes(){}
    public static boolean ready(BlockPos pos){
        var renderer=SodiumWorldRenderer.instanceNullable();if(renderer==null)return false;
        var manager=((SodiumSectionAccess)renderer).slopium$sections();if(manager==null)return false;
        var section=((SectionManagerAccess)manager).slopium$section(pos.getX()>>4,pos.getY()>>4,pos.getZ()>>4);
        return section!=null&&section.isBuilt()&&!section.isDisposed()&&section.getPendingUpdate()==0&&section.getRunningJob()==null;
    }
}

package dev.slopium.visibility;
import dev.slopium.client.SlopiumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
/** Section policy only. Visibility lives on the GPU; there is no CPU shadow mask. */
public final class SectionVisibility {
    private SectionVisibility() {}
    public static boolean active(){var c=SlopiumConfig.get();return c.enabled&&c.visibleSections&&c.gpuGlassCulling;}
    public static int minimum(){return SectionRange.minimum(SlopiumConfig.get().minimumSections,Minecraft.getInstance().options.renderDistance().get());}
    public static void resetCounters(){GpuSections.resetCounters();}
    public static void clear(){GpuSections.invalidate();}
    public static String describe(BlockPos p){return "Section "+(p.getX()>>4)+", "+(p.getY()>>4)+", "+(p.getZ()>>4)+" | minimum="+minimum()+" | "+GpuSections.describe(p);}
    public static String status(){return GpuSections.status();}
}

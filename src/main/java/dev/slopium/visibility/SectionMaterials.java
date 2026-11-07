package dev.slopium.visibility;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.ConcurrentHashMap;
/** Annotate already-emitted glass meshes. No world scan or per-frame block queries. */
public final class SectionMaterials {
    public static final ThreadLocal<Boolean> GLASS=ThreadLocal.withInitial(()->false);
    private static final ConcurrentHashMap<Block,Boolean> TYPES=new ConcurrentHashMap<>();
    public static boolean glass(Block block){return TYPES.computeIfAbsent(block,b->{
        if(b==Blocks.GLASS||b==Blocks.GLASS_PANE)return true;
        for(Class<?> t=b.getClass();t!=null;t=t.getSuperclass()){
            String name=t.getSimpleName();if(name.equals("StainedGlassBlock")||name.equals("StainedGlassPaneBlock")||name.equals("TintedGlassBlock")||name.equals("ConnectedGlassBlock")||name.equals("ConnectedGlassPaneBlock")||name.equals("WindowBlock"))return true;
        }
        return false;
    });}
}

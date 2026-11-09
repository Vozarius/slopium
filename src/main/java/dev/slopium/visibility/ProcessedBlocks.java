package dev.slopium.visibility;
import dev.slopium.client.*;
import dev.slopium.mixin.BlockEntityTypeBlocksAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;
/** Published once at client setup/settings changes. All consumers read the same dense ID table. */
public final class ProcessedBlocks {
    public record Entry(Block block,String id,boolean flywheel){}
    private static volatile boolean[] allowed=new boolean[0];
    private static List<Entry> catalog=List.of();
    private static volatile long revision;
    private ProcessedBlocks(){}
    public static void discover(){
        Map<Block,Boolean> blocks=new IdentityHashMap<>();
        for(var type:BuiltInRegistries.BLOCK_ENTITY_TYPE){
            boolean flywheel=Slopium.loaded("flywheel")&&dev.engine_room.flywheel.api.visualization.VisualizerRegistry.getVisualizer(type)!=null;
            boolean create=BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).getNamespace().equals("create");
            if(flywheel||create)for(Block block:((BlockEntityTypeBlocksAccess)type).slopium$blocks())blocks.merge(block,flywheel,(a,b)->a||b);
        }
        catalog=blocks.entrySet().stream().map(e->new Entry(e.getKey(),BuiltInRegistries.BLOCK.getKey(e.getKey()).toString(),e.getValue()))
            .sorted(Comparator.comparing(Entry::id)).toList();
        rebuild();
        com.mojang.logging.LogUtils.getLogger().info("Slopium block catalog: {} blocks, {} with Flywheel visualizers",catalog.size(),catalog.stream().filter(Entry::flywheel).count());
    }
    public static List<Entry> catalog(){return catalog;}
    public static long revision(){return revision;}
    public static boolean all(){return SlopiumConfig.get().processedBlockMode==BlockSelectionMode.ALL;}
    public static void rebuild(){
        var c=SlopiumConfig.get();var next=new boolean[BuiltInRegistries.BLOCK.size()];
        var selected=new HashSet<>(c.processedBlockIds);
        for(var e:catalog)next[BuiltInRegistries.BLOCK.getId(e.block)]=accept(c.processedBlockMode,selected.contains(e.id));
        allowed=next;revision++;
    }
    public static boolean accept(BlockSelectionMode mode,boolean listed){return mode.allows(listed);}
    public static boolean allows(BlockEntity be){return be instanceof TrackedBlockEntity tracked?tracked.slopium$allowsProcessing():be!=null&&allows(be.getBlockState().getBlock());}
    public static boolean allows(Block block){int id=BuiltInRegistries.BLOCK.getId(block);var table=allowed;return id>=0&&id<table.length&&table[id];}
}

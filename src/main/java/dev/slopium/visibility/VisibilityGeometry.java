package dev.slopium.visibility;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
/** Bounded render-thread revision index. Off-thread updates fall back to global invalidation. */
public final class VisibilityGeometry {
    private static final Long2LongOpenHashMap COLUMNS=new Long2LongOpenHashMap();
    private static long accounted=Long.MIN_VALUE, generation, revision;
    private VisibilityGeometry() {}
    public static void reset() { COLUMNS.clear(); generation++; accounted=WallOcclusion.epoch(); }
    public static long generation() {
        if(accounted!=WallOcclusion.epoch()) reset();
        return generation;
    }
    private static void publish() {
        long next=WallOcclusion.invalidateAndGet();
        if(next!=accounted+1) { COLUMNS.clear(); generation++; }
        accounted=next;
    }
    public static long version(long column) { return COLUMNS.get(column); }
    public static void invalidate(BlockPos pos) {
        if(!Minecraft.getInstance().isSameThread()) { WallOcclusion.invalidate(); return; }
        generation();
        if(COLUMNS.size()>65536) reset();
        long value=++revision;
        // Neighbour-dependent shapes can cross a chunk boundary.
        for(int x=(pos.getX()-1)>>4;x<=((pos.getX()+1)>>4);x++)
            for(int z=(pos.getZ()-1)>>4;z<=((pos.getZ()+1)>>4);z++) COLUMNS.put(ChunkPos.asLong(x,z),value);
        publish();
    }
    public static void chunk(int x,int z) {
        if(!Minecraft.getInstance().isSameThread()) { WallOcclusion.invalidate(); return; }
        generation();
        if(COLUMNS.size()>65536) reset();
        long value=++revision;
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) COLUMNS.put(ChunkPos.asLong(x+dx,z+dz),value);
        publish();
    }
}

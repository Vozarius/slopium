package dev.slopium.mixin;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Set;
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeBlocksAccess {
    @Accessor("validBlocks") Set<Block> slopium$blocks();
}

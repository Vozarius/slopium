package dev.slopium.mixin;

import dev.slopium.visibility.TickGate;
import dev.slopium.visibility.TrackedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityStateMixin implements TrackedBlockEntity {
    @Unique private TickGate slopium$gate;
    @Unique private volatile net.minecraft.world.level.block.Block slopium$block;
    @Unique private volatile long slopium$selection = Long.MIN_VALUE;
    @Unique private volatile boolean slopium$allowed;
    @Unique private dev.slopium.visibility.VisibilityPool.Handle slopium$visibility;
    @Override public boolean slopium$allowsProcessing() {
        var block=((BlockEntity)(Object)this).getBlockState().getBlock();
        long revision=dev.slopium.visibility.ProcessedBlocks.revision();
        if(slopium$block!=block || slopium$selection!=revision){
            slopium$allowed=dev.slopium.visibility.ProcessedBlocks.allows(block);
            slopium$block=block;slopium$selection=revision;
        }
        return slopium$allowed;
    }
    @Override public dev.slopium.visibility.VisibilityPool.Handle slopium$visibility() {
        if(slopium$visibility==null)slopium$visibility=new dev.slopium.visibility.VisibilityPool.Handle(
            net.minecraft.world.phys.Vec3.atCenterOf(((BlockEntity)(Object)this).getBlockPos()));
        return slopium$visibility;
    }
    @Override public TickGate slopium$tickGate() {
        if (slopium$gate == null) slopium$gate = new TickGate();
        return slopium$gate;
    }
    @Override public void slopium$resetHiddenState() {
        if (slopium$gate != null) slopium$gate.resetHidden();
    }
}

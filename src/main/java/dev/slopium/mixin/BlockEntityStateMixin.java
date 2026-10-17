package dev.slopium.mixin;

import dev.slopium.visibility.TickGate;
import dev.slopium.visibility.TrackedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityStateMixin implements TrackedBlockEntity {
    @Unique private TickGate slopium$gate;
    @Override public TickGate slopium$tickGate() {
        if (slopium$gate == null) slopium$gate = new TickGate();
        return slopium$gate;
    }
}

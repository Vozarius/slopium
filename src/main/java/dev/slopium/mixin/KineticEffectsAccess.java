package dev.slopium.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="com.simibubi.create.content.kinetics.base.KineticBlockEntity",remap=false)
public interface KineticEffectsAccess {
    @Accessor("effects") com.simibubi.create.content.kinetics.base.KineticEffectHandler slopium$effects();
}

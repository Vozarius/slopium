package dev.slopium.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="com.simibubi.create.content.kinetics.base.KineticEffectHandler",remap=false)
public interface KineticStressAccess {
    @Accessor("overStressedEffect") float slopium$overStressedEffect();
}

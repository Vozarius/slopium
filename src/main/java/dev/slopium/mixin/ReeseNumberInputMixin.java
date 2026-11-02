package dev.slopium.mixin;
import dev.slopium.client.ExactNumberInput;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Reese owns its slider rows; Sodium's SliderControl is never called for them. */
@Mixin(targets="me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.AbstractOptionRow",remap=false)
public abstract class ReeseNumberInputMixin {
    @Shadow public abstract Option getOption();
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void slopium$number(double x,double y,int button,CallbackInfoReturnable<Boolean> ci){
        if(((GuiEventListener)(Object)this).isMouseOver(x,y)&&ExactNumberInput.open(getOption(),button))ci.setReturnValue(true);
    }
}

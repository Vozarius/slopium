package dev.slopium.mixin;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl$SliderControlElement",remap=false)
public abstract class SlopiumSliderMixin {
    @Shadow @Final private IntegerOption option;
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void slopium$number(double x,double y,int button,CallbackInfoReturnable<Boolean> ci){
        if(((ControlElement)(Object)this).isMouseOver(x,y)&&dev.slopium.client.ExactNumberInput.open(option,button))ci.setReturnValue(true);
    }
}

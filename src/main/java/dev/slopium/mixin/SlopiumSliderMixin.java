package dev.slopium.mixin;
import dev.slopium.client.NumberEntryScreen;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl$SliderControlElement",remap=false)
public abstract class SlopiumSliderMixin {
    @Shadow @Final private IntegerOption option;
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void slopium$number(double x,double y,int button,CallbackInfoReturnable<Boolean> ci){
        if(button!=1||!option.isEnabled()||!((ControlElement)(Object)this).isMouseOver(x,y))return;
        if(!(option.getName().getContents() instanceof TranslatableContents text)||!text.getKey().startsWith("slopium."))return;
        var mc=Minecraft.getInstance();mc.setScreen(new NumberEntryScreen(mc.screen,option));ci.setReturnValue(true);
    }
}

package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.slopium.framegen.AsyncPresenter;
import net.minecraft.client.gui.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(Gui.class)
public abstract class AsyncVignetteMixin {
    @WrapOperation(method="renderCameraOverlays",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Gui;renderVignette(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;)V"))
    private void slopium$worldVignette(Gui gui,GuiGraphics graphics,Entity entity,Operation<Void> original){
        if(!AsyncPresenter.overlayActive()){original.call(gui,graphics,entity);return;}
        graphics.flush();int previous=AsyncPresenter.beginWorldEffect();
        try{original.call(gui,graphics,entity);graphics.flush();}finally{AsyncPresenter.endWorldEffect(previous);}
    }
}

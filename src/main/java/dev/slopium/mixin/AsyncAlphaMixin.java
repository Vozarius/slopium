package dev.slopium.mixin;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.slopium.framegen.AsyncPresenter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static org.lwjgl.opengl.GL33C.*;
@Mixin(GlStateManager.class)
public abstract class AsyncAlphaMixin {
    @Unique private static boolean slopium$adjusting;
    @Inject(method="_blendFuncSeparate",at=@At("HEAD"),cancellable=true)
    private static void slopium$alpha(int src,int dst,int srcAlpha,int dstAlpha,CallbackInfo ci){
        if(!AsyncPresenter.overlayActive()||slopium$adjusting)return;
        boolean crosshair=src==GL_ONE_MINUS_DST_COLOR&&dst==GL_ONE_MINUS_SRC_COLOR;
        if(!crosshair&&srcAlpha==GL_ONE&&dstAlpha==GL_ONE_MINUS_SRC_ALPHA)return;
        slopium$adjusting=true;
        try{GlStateManager._blendFuncSeparate(crosshair?GL_SRC_ALPHA:src,crosshair?GL_ONE_MINUS_SRC_ALPHA:dst,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);}
        finally{slopium$adjusting=false;}
        ci.cancel();
    }
    @Inject(method="_blendFunc",at=@At("HEAD"),cancellable=true)
    private static void slopium$alphaSimple(int src,int dst,CallbackInfo ci){
        if(!AsyncPresenter.overlayActive())return;
        GlStateManager._blendFuncSeparate(src,dst,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);ci.cancel();
    }
}

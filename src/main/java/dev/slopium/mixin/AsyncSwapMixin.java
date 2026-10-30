package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.slopium.framegen.AsyncPresenter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(RenderSystem.class)
public abstract class AsyncSwapMixin {
    // Retain pollEvents, replayQueue and Tesselator cleanup on Minecraft's own thread.
    @WrapOperation(method="flipFrame",at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"))
    private static void slopium$workerOwnsSwap(long window,Operation<Void> original){if(!AsyncPresenter.active())original.call(window);}
}

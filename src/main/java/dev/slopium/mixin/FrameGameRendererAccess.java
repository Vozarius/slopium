package dev.slopium.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface FrameGameRendererAccess {
    @Accessor("renderHand") boolean slopium$renderHand();
    @Accessor("panoramicMode") boolean slopium$panoramic();
    @Invoker("renderItemInHand") void slopium$hand(Camera camera,float partial,Matrix4f view);
    @Invoker("getFov") double slopium$fov(Camera camera,float partial,boolean world);
    @Invoker("bobView") void slopium$bob(com.mojang.blaze3d.vertex.PoseStack poses,float partial);
}

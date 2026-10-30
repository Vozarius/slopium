package dev.slopium.framegen;

import dev.slopium.client.*;
import dev.slopium.mixin.FrameGameRendererAccess;
import net.minecraft.client.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Short presentation path on the existing render thread. Never reads world state on a worker. */
public final class FastFramePath {
    private static boolean fast;
    private static long loopStart,renderStart,swapStart;
    private static long prepareNanos,renderNanos,swapNanos,frames;
    public static long fastFrames,normalFrames;
    private static final Matrix4f view=new Matrix4f();
    private static final Matrix4f projection=new Matrix4f();
    private static final com.mojang.blaze3d.vertex.PoseStack bob=new com.mojang.blaze3d.vertex.PoseStack();
    private static final Quaternionf rotation=new Quaternionf();
    private FastFramePath() {}
    public static void loopStart(){fast=false;loopStart=System.nanoTime();}
    public static boolean begin(DeltaTracker delta) {
        renderStart=System.nanoTime();prepareNanos+=renderStart-loopStart;
        try {return prepare(delta);}catch(RuntimeException|LinkageError e){FrameGeneration.disableFast(e);fast=false;return false;}
    }
    private static boolean prepare(DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        if(!FrameGeneration.fastEligible(renderStart)||mc.gameRenderer.currentEffect()!=null
            ||((FrameGameRendererAccess)mc.gameRenderer).slopium$panoramic())return false;
        var camera=mc.gameRenderer.getMainCamera();var entity=mc.getCameraEntity();
        if(entity==null)return false;
        float partial=delta.getGameTimeDeltaPartialTick(true);
        mc.gameRenderer.pick(partial); // Keep crosshair targeting current, just like the ordinary frame.
        camera.setup(mc.level,entity,!mc.options.getCameraType().isFirstPerson(),mc.options.getCameraType().isMirrored(),
            mc.level.tickRateManager().isEntityFrozen(entity)?1:partial);
        view.rotation(camera.rotation().conjugate(rotation));
        var access=(FrameGameRendererAccess)mc.gameRenderer;
        projection.set(mc.gameRenderer.getProjectionMatrix(access.slopium$fov(camera,partial,true)));
        if(mc.options.bobView().get()) {
            bob.pushPose();try{access.slopium$bob(bob,camera.getPartialTickTime());projection.mul(bob.last().pose());}finally{bob.popPose();}
        }
        fast=FrameGeneration.tryFast(camera,view,projection);
        if(fast)Slopium.generatedFrameStart();
        return fast;
    }
    public static boolean active(){return fast;}
    public static void hand(DeltaTracker delta) {
        var mc=Minecraft.getInstance();var game=mc.gameRenderer;
        FrameGeneration.finishFastWorld(view,projection);
        if(((FrameGameRendererAccess)game).slopium$renderHand()) {
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL33C.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            ((FrameGameRendererAccess)game).slopium$hand(game.getMainCamera(),delta.getGameTimeDeltaPartialTick(true),view);
        }
    }
    public static void end(){renderNanos+=System.nanoTime()-renderStart;frames++;if(fast)fastFrames++;else normalFrames++;}
    public static void swapStart(){swapStart=System.nanoTime();}
    public static void swapEnd(){swapNanos+=System.nanoTime()-swapStart;}
    public static void reset(){fast=false;prepareNanos=renderNanos=swapNanos=frames=fastFrames=normalFrames=0;}
    public static String status(){return String.format(java.util.Locale.ROOT,
        "Frame path | fast=%d | regular=%d | CPU prepare=%.3f ms | CPU render=%.3f ms | swap=%.3f ms",
        fastFrames,normalFrames,frames==0?0:prepareNanos/1e6/frames,frames==0?0:renderNanos/1e6/frames,frames==0?0:swapNanos/1e6/frames)+FrameGeneration.fastFailure();}
}

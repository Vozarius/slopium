package dev.slopium.smoke;
import dev.slopium.client.*;
import dev.slopium.framegen.*;
import net.minecraft.client.*;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL33C.*;

/** Real GL renderer lifecycle and wall-clock priority regression (no fixed output-count expectations). */
final class SmokeFrameLifecycleChecks {
    static void run() throws ReflectiveOperationException {
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        var oldWorld=mc.level;var oldScreen=mc.screen;var overlay=mc.getOverlay();boolean focus=mc.isWindowActive(),enabled=c.enabled,diagnostics=c.diagnostics;
        var oldMode=c.frameGenerationMode;int oldMultiplier=c.frameGenerationMultiplier;boolean oldMark=c.frameGenerationMarkFrames;var graphics=mc.options.graphicsMode().get();
        int oldInput=c.frameGenerationInputFps,oldTarget=c.frameGenerationTargetFps;
        try(var state=new GlState()) {
            float[] clear=new float[4];glGetFloatv(GL_COLOR_CLEAR_VALUE,clear);double clearDepth=glGetDouble(GL_DEPTH_CLEAR_VALUE);boolean mask=glGetBoolean(GL_DEPTH_WRITEMASK);
            try {
                mc.level=(SmokeWallChecks.TestLevel)SmokeRadicalChecks.allocate(SmokeWallChecks.TestLevel.class);
                mc.screen=null;mc.setOverlay(null);mc.setWindowActive(true);mc.options.graphicsMode().set(GraphicsStatus.FANCY);c.enabled=true;c.diagnostics=true;
                var camera=new Camera();var view=new Matrix4f();var projection=new Matrix4f().perspective((float)Math.toRadians(70),1280f/720,.05f,1000);
                for(var mode:new FrameGenerationMode[]{FrameGenerationMode.SIMPLE,FrameGenerationMode.FSR}) for(int factor:new int[]{2,3,4,8,32}) {
                    FrameGeneration.release();FrameGeneration.resetCounters();c.frameGenerationMode=mode;c.frameGenerationMultiplier=factor;c.frameGenerationMarkFrames=true;c.frameGenerationInputFps=30;
                    int[] draws={0};Runnable original=()->{
                        ++draws[0];GlState.fullscreen();glDepthMask(true);glClearDepth(.375);glClearColor(.25f,.5f,.75f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
                    };
                    mc.getMainRenderTarget().bindWrite(true);FrameGeneration.render(camera,view,projection,original);
                    long start=System.nanoTime(),beforeReal=FrameGeneration.realFrames;int outputs=0;
                    do {
                        long priorGenerated=FrameGeneration.generatedFrames;
                        mc.getMainRenderTarget().bindWrite(true);FrameGeneration.render(camera,view,projection,original);outputs++;
                        if(GL_NO_ERROR!=glGetError())throw new AssertionError("Production FG GL error: "+mode);
                        glBindFramebuffer(GL_READ_FRAMEBUFFER,mc.getMainRenderTarget().frameBufferId);
                        float[] marker=new float[4];glReadPixels(8,mc.getMainRenderTarget().height-2,1,1,GL_RGBA,GL_FLOAT,marker);
                        if((marker[0]>.9f)!=(FrameGeneration.generatedFrames>priorGenerated))throw new AssertionError("Wrong generated-frame marker: "+mode+" x"+factor);
                        // Slow the output consumer: x32 must not require 32 calls before the next real frame.
                        java.util.concurrent.locks.LockSupport.parkNanos(factor==2?16_666_667:4_166_667);
                    }while(System.nanoTime()-start<1_000_000_000L);
                    double seconds=(System.nanoTime()-start)/1e9;long real=FrameGeneration.realFrames-beforeReal;
                    if(real<seconds*20||real>Math.ceil(seconds*30)+1)throw new AssertionError("Real-frame priority failed: "+mode+" x"+factor+" "+real+" in "+seconds+" outputs="+outputs);
                    if(FrameGeneration.generatedFrames==0)throw new AssertionError("No generated images: "+mode+" x"+factor);
                    glBindFramebuffer(GL_READ_FRAMEBUFFER,mc.getMainRenderTarget().frameBufferId);float[] depth=new float[1];glReadPixels(64,64,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,depth);
                    if(Math.abs(depth[0]-.375f)>.001f)throw new AssertionError("World depth lost");
                    c.frameGenerationTargetFps=120;
                    if(FrameGeneration.limit(260)>120||FrameGeneration.limit(30)!=30)throw new AssertionError("Output limiter ignored Minecraft cap");
                    c.frameGenerationTargetFps=0;c.frameGenerationInputFps=120;
                    if(FrameGeneration.limit(260)!=120*factor||FrameGeneration.limiterThreshold()!=Integer.MAX_VALUE)throw new AssertionError("High-rate limit failed");
                    c.frameGenerationInputFps=30;
                    mc.screen=oldScreen;FrameGeneration.render(camera,view,projection,original);mc.screen=null;
                    int before=draws[0];
                    try {FrameGeneration.render(camera,view,projection,()->{draws[0]++;throw new IllegalArgumentException("world-render-test");});throw new AssertionError("World error swallowed");}
                    catch(IllegalArgumentException expected){if(!expected.getMessage().equals("world-render-test"))throw expected;}
                    if(draws[0]!=before+1)throw new AssertionError("World error duplicated render");
                    var field=FrameGeneration.class.getDeclaredField("current");field.setAccessible(true);Object retained=field.get(null);
                    mc.screen=oldScreen;FrameGeneration.render(camera,view,projection,original);
                    if(field.get(null)!=retained||FrameGeneration.limit(260)!=260)throw new AssertionError("Pause lost resources or kept limiter");
                    mc.screen=null;before=draws[0];long generated=FrameGeneration.generatedFrames;
                    FrameGeneration.render(camera,view,projection,original);
                    if(draws[0]!=before+1||FrameGeneration.generatedFrames!=generated)throw new AssertionError("Resume reused temporal history");
                    c.frameGenerationMode=FrameGenerationMode.OFF;FrameGeneration.render(camera,view,projection,original);
                    if(FrameGeneration.limit(260)!=260)throw new AssertionError("Off retained limiter");
                    com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: priority {} x{}: {} real FPS, {} outputs, {} generated; marker, depth, pause, off and limits passed",mode,factor,real/seconds,outputs,generated);
                }
                for(var mode:new FrameGenerationMode[]{FrameGenerationMode.SIMPLE,FrameGenerationMode.FSR}) {
                    FrameGeneration.release();FrameGeneration.resetCounters();c.frameGenerationMode=mode;c.frameGenerationMultiplier=32;c.frameGenerationInputFps=0;c.diagnostics=false;
                    Runnable expensiveWorld=()->{GlState.fullscreen();glClearColor(.25f,.5f,.75f,1);glClear(GL_COLOR_BUFFER_BIT);java.util.concurrent.locks.LockSupport.parkNanos(33_333_333);};
                    mc.getMainRenderTarget().bindWrite(true);FrameGeneration.render(camera,view,projection,expensiveWorld);
                    long start=System.nanoTime(),before=FrameGeneration.realFrames;
                    do {mc.getMainRenderTarget().bindWrite(true);FrameGeneration.render(camera,view,projection,expensiveWorld);}while(System.nanoTime()-start<1_200_000_000L);
                    double rate=(FrameGeneration.realFrames-before)/((System.nanoTime()-start)/1e9);
                    if(rate<24)throw new AssertionError("Automatic x32 starved 33 ms real frames: "+mode+" "+rate);
                    com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: automatic {} x32 with simulated 33 ms world work: {} real FPS; {}",mode,rate,FrameGeneration.status());
                }
                if(java.util.Arrays.stream(Minecraft.class.getDeclaredMethods()).noneMatch(m->m.getName().contains("slopium$finiteHighLimit")))throw new AssertionError("High-rate hook absent");
                if(java.util.Arrays.stream(net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethods()).noneMatch(m->m.getName().contains("slopium$worldFrame")))throw new AssertionError("Frame hook absent");
            } finally {
                FrameGeneration.release();FrameGeneration.resetCounters();c.frameGenerationMode=oldMode;c.enabled=enabled;c.diagnostics=diagnostics;c.frameGenerationMultiplier=oldMultiplier;c.frameGenerationMarkFrames=oldMark;
                c.frameGenerationInputFps=oldInput;c.frameGenerationTargetFps=oldTarget;
                mc.level=oldWorld;mc.screen=oldScreen;mc.setOverlay(overlay);mc.setWindowActive(focus);mc.options.graphicsMode().set(graphics);
                glClearColor(clear[0],clear[1],clear[2],clear[3]);glClearDepth(clearDepth);glDepthMask(mask);
            }
        }
    }
}

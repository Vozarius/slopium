package dev.slopium.smoke;
import dev.slopium.client.*;
import dev.slopium.framegen.*;
import net.minecraft.client.*;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL33C.*;

/** Exercise the production renderer gate with a test world identity and real main framebuffer. */
final class SmokeFrameLifecycleChecks {
    static void run() throws ReflectiveOperationException {
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        var oldWorld=mc.level;var oldScreen=mc.screen;var overlay=mc.getOverlay();boolean focus=mc.isWindowActive(),enabled=c.enabled,diagnostics=c.diagnostics;
        var oldMode=c.frameGenerationMode;int oldMultiplier=c.frameGenerationMultiplier;boolean oldMark=c.frameGenerationMarkFrames;var graphics=mc.options.graphicsMode().get();
        int oldInput=c.frameGenerationInputFps;
        try(var state=new GlState()) {
            float[] clear=new float[4];glGetFloatv(GL_COLOR_CLEAR_VALUE,clear);double clearDepth=glGetDouble(GL_DEPTH_CLEAR_VALUE);boolean mask=glGetBoolean(GL_DEPTH_WRITEMASK);
            try {
                mc.level=(SmokeWallChecks.TestLevel)SmokeRadicalChecks.allocate(SmokeWallChecks.TestLevel.class);
                mc.screen=null;mc.setOverlay(null);mc.setWindowActive(true);mc.options.graphicsMode().set(GraphicsStatus.FANCY);c.enabled=true;c.diagnostics=true;
                c.frameGenerationInputFps=0;
                var camera=new Camera();var view=new Matrix4f();var projection=new Matrix4f().perspective((float)Math.toRadians(70),1280f/720,.05f,1000);
                for(var mode:new FrameGenerationMode[]{FrameGenerationMode.SIMPLE,FrameGenerationMode.FSR}) for(int factor:new int[]{2,3,4,8,32}) {
                    FrameGeneration.release();FrameGeneration.resetCounters();c.frameGenerationMode=mode;c.frameGenerationMultiplier=factor;c.frameGenerationMarkFrames=true;
                    int[] draws={0};Runnable original=()->{
                        ++draws[0];GlState.fullscreen();glDepthMask(true);glClearDepth(.375);glClearColor(.25f,.5f,.75f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
                    };
                    for(int i=0;i<2*factor+1;i++) {
                        mc.getMainRenderTarget().bindWrite(true);FrameGeneration.render(camera,view,projection,original);
                        if(GL_NO_ERROR!=glGetError())throw new AssertionError("Production FG gate leaked GL error: "+mode);
                        glBindFramebuffer(GL_READ_FRAMEBUFFER,mc.getMainRenderTarget().frameBufferId);
                        float[] marker=new float[4];glReadPixels(8,mc.getMainRenderTarget().height-2,1,1,GL_RGBA,GL_FLOAT,marker);
                        boolean synthetic=mode==FrameGenerationMode.SIMPLE?i%factor!=0:i>0&&(i-1)%factor+1<factor;
                        if((marker[0]>.9f)!=synthetic)throw new AssertionError("Wrong generated-frame marker: "+mode+" x"+factor+" output "+i);
                    }
                    if(draws[0]!=3||FrameGeneration.generatedFrames!=2*(factor-1)||FrameGeneration.endpointFrames!=(mode==FrameGenerationMode.SIMPLE?0:2)||FrameGeneration.outputFrames!=2*factor+1)
                        throw new AssertionError("World work not reduced: "+mode+", draws="+draws[0]+", "+FrameGeneration.status());
                    glBindFramebuffer(GL_READ_FRAMEBUFFER,mc.getMainRenderTarget().frameBufferId);float[] depth=new float[1];glReadPixels(64,64,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,depth);
                    if(Math.abs(depth[0]-.375f)>.001f)throw new AssertionError("Endpoint lost world depth");
                    int oldTarget=c.frameGenerationTargetFps;
                    c.frameGenerationTargetFps=120;
                    if(FrameGeneration.limit(260)!=120||FrameGeneration.limit(30)!=30)throw new AssertionError("Output limiter ignored Minecraft cap");
                    c.frameGenerationTargetFps=0;
                    if(FrameGeneration.limit(260)!=260||FrameGeneration.limit(60)!=60)throw new AssertionError("No-extra-cap setting changed Minecraft cap");
                    c.frameGenerationTargetFps=oldTarget;
                    int before=draws[0];
                    // Force a fresh capture independently of the selected generation schedule.
                    mc.screen=oldScreen;FrameGeneration.render(camera,view,projection,original);mc.screen=null;before=draws[0];
                    try {FrameGeneration.render(camera,view,projection,()->{draws[0]++;throw new IllegalArgumentException("world-render-test");});throw new AssertionError("World error was swallowed");}
                    catch(IllegalArgumentException expected){if(!expected.getMessage().equals("world-render-test"))throw expected;}
                    if(draws[0]!=before+1)throw new AssertionError("World error triggered duplicate render");
                    var field=FrameGeneration.class.getDeclaredField("current");field.setAccessible(true);Object retained=field.get(null);
                    mc.screen=oldScreen;FrameGeneration.render(camera,view,projection,original);
                    if(field.get(null)!=retained||FrameGeneration.limit(260)!=260)throw new AssertionError("Menu pause recreated resources or kept the limiter");
                    mc.screen=null;before=draws[0];long generated=FrameGeneration.generatedFrames;
                    FrameGeneration.render(camera,view,projection,original);
                    if(draws[0]!=before+1||FrameGeneration.generatedFrames!=generated)throw new AssertionError("Resume reused pre-menu temporal history");
                    c.frameGenerationMode=FrameGenerationMode.OFF;FrameGeneration.render(camera,view,projection,original);
                    if(FrameGeneration.limit(260)!=260)throw new AssertionError("Frame limiter retained while off");
                    com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: production {} x{}: {} outputs / 3 world renders, {} intermediate frames, marker, depth, pause, off and cap passed",mode,factor,2*factor+1,2*(factor-1));
                }
                for(var mode:new FrameGenerationMode[]{FrameGenerationMode.SIMPLE,FrameGenerationMode.FSR}) {
                    FrameGeneration.release();FrameGeneration.resetCounters();c.frameGenerationMode=mode;c.frameGenerationMultiplier=4;c.frameGenerationInputFps=30;
                    Runnable draw=()->{GlState.fullscreen();glDepthMask(true);glClearDepth(.375);glClearColor(.25f,.5f,.75f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);};
                    // Initialize outside the timed interval, then stress the gate faster than its cap.
                    mc.getMainRenderTarget().bindWrite(true);FrameGeneration.render(camera,view,projection,draw);
                    long start=System.nanoTime(),before=FrameGeneration.realFrames;
                    do {mc.getMainRenderTarget().bindWrite(true);FrameGeneration.render(camera,view,projection,draw);}while(System.nanoTime()-start<600_000_000L);
                    double seconds=(System.nanoTime()-start)/1e9;long rendered=FrameGeneration.realFrames-before;
                    if(rendered<3||rendered>Math.ceil(seconds*30)+1)throw new AssertionError("Input cap failed: "+mode+" "+rendered+" in "+seconds);
                    if(mode==FrameGenerationMode.FSR&&FrameGeneration.heldFrames==0)throw new AssertionError("FSR did not hold exhausted pair");
                    long cutStart=System.nanoTime(),cutBefore=FrameGeneration.realFrames;int turn=0;
                    do {
                        camera.rotation().rotationY((++turn%2)*.7f);
                        mc.getMainRenderTarget().bindWrite(true);FrameGeneration.render(camera,view,projection,draw);
                    }while(System.nanoTime()-cutStart<400_000_000L);
                    double cutSeconds=(System.nanoTime()-cutStart)/1e9;
                    if(FrameGeneration.realFrames-cutBefore>Math.ceil(cutSeconds*30)+1)throw new AssertionError("Camera cuts bypassed input cap: "+mode);
                    camera.rotation().identity();
                    if(GL_NO_ERROR!=glGetError())throw new AssertionError("Input cap leaked GL state");
                    int oldTarget=c.frameGenerationTargetFps;c.frameGenerationTargetFps=0;c.frameGenerationInputFps=120;
                    if(FrameGeneration.limit(260)!=480||FrameGeneration.limiterThreshold()!=Integer.MAX_VALUE)throw new AssertionError("High output limit lost to vanilla unlimited sentinel");
                    c.frameGenerationTargetFps=oldTarget;
                    com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: {} input cap 30: {} real captures in {} s; {} held frames",mode,rendered,seconds,FrameGeneration.heldFrames);
                }
                if(java.util.Arrays.stream(Minecraft.class.getDeclaredMethods()).noneMatch(m->m.getName().contains("slopium$finiteHighLimit")))throw new AssertionError("High-rate limiter hook absent");
                if(java.util.Arrays.stream(net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethods()).noneMatch(m->m.getName().contains("slopium$worldFrame")))throw new AssertionError("Frame generation mixin absent");
            } finally {
                FrameGeneration.release();FrameGeneration.resetCounters();c.frameGenerationMode=oldMode;c.enabled=enabled;c.diagnostics=diagnostics;c.frameGenerationMultiplier=oldMultiplier;c.frameGenerationMarkFrames=oldMark;
                c.frameGenerationInputFps=oldInput;
                mc.level=oldWorld;mc.screen=oldScreen;mc.setOverlay(overlay);mc.setWindowActive(focus);mc.options.graphicsMode().set(graphics);
                glClearColor(clear[0],clear[1],clear[2],clear[3]);glClearDepth(clearDepth);glDepthMask(mask);
            }
        }
    }
}

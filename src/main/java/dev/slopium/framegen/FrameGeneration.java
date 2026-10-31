package dev.slopium.framegen;

import dev.slopium.client.*;
import net.minecraft.client.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import static org.lwjgl.opengl.GL33C.*;

/** Render-thread-only world reprojection / interpolation. Simulation, picking, hand and HUD remain live. */
public final class FrameGeneration {
    private static final FrameCadence CADENCE=new FrameCadence();
    private static final FrameSchedule SCHEDULE=new FrameSchedule();
    private static GlImage previous,current,generated,fsrMiddle;
    private static final float[] FRAME_MARKER={1,.1f,.7f,1};
    private static SimpleInterpolator simple;
    private static dev.slopium.framegen.reference.Reprojector reprojection;
    private static int heldSource;
    private static String lastNotice="";
    private static FsrInterpolator fsr;
    private static GpuTimer timer;
    private static double gpuMilliseconds;
    private static Matrix4f previousVp;
    private static boolean lastWorldReal;
    private static long fastFailedRevision=-1;
    private static String fastProblem="";
    private static Vec3 eye;
    private static Quaternionf rotation;
    private static Object world;
    private static long revision=-1,failedRevision=-1,lastCapture,lastOutput,elapsedNanos,outputStarts,realStarts;
    private static FrameGenerationMode mode=FrameGenerationMode.OFF;
    private static boolean active;
    public static String state="off";
    private static String backend="not started",failureState="";
    public static long realFrames,generatedFrames,endpointFrames,outputFrames,heldFrames;
    private FrameGeneration() {}
    public static boolean conflictingGenerator(){return Slopium.loaded("framegen")||Slopium.loaded("pfg");}
    public static void overlay(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        if(!c.enabled||c.frameGenerationMode==FrameGenerationMode.OFF||mc.level==null||mc.screen!=null)return;
        if(AsyncPresenter.active()) {
            if(c.frameGenerationCounter)event.getGuiGraphics().drawString(mc.font,AsyncPresenter.overlay(),8,event.getGuiGraphics().guiHeight()-64,0xFFFFFFFF);
            return;
        }
        if(c.frameGenerationParallel&&AsyncPresenter.failureNotice()!=null)
            event.getGuiGraphics().drawString(mc.font,AsyncPresenter.failureNotice(),8,event.getGuiGraphics().guiHeight()-80,0xFFFFAA55);
        // Errors must remain visible even with diagnostics and the optional counter off.
        if(!c.frameGenerationCounter&&active)return;
        var line=net.minecraft.network.chat.Component.literal("Slopium FG: "+state);
        var gui=event.getGuiGraphics();int y=gui.guiHeight()-64;
        gui.drawString(mc.font,line,8,y,active?0xFFFFFFFF:0xFFFFAA55);
        if(active) {
            double seconds=elapsedNanos/1e9;
            gui.drawString(mc.font,net.minecraft.network.chat.Component.translatable("slopium.frame_generation.counter",
                seconds<=0?0:Math.round((realFrames-realStarts)/seconds),seconds<=0?0:Math.round((outputFrames-outputStarts)/seconds),generatedFrames),8,y+11,0xFFFFFFFF);
        }
    }
    public static int limit(int original) {
        if(AsyncPresenter.active())return AsyncPresenter.inputLimit(original);
        if(!active)return original;
        var c=SlopiumConfig.get();
        int input=SCHEDULE.probing()?0:(c.frameGenerationInputFps>0?c.frameGenerationInputFps:SCHEDULE.targetFps());
        int limit=InputFrameLimiter.outputLimit(original,c.frameGenerationTargetFps,input,c.frameGenerationMultiplier);
        return limit==0?original:limit;
    }
    public static int limiterThreshold(){var c=SlopiumConfig.get();return active&&(c.frameGenerationTargetFps>0||c.frameGenerationInputFps>0||!SCHEDULE.probing())?Integer.MAX_VALUE:260;}
    public static String status() {
        double seconds=elapsedNanos/1e9;
        return String.format(java.util.Locale.ROOT,"Frame generation | %s | real=%d | generated=%d | endpoints=%d | held=%d | input target=%d | scheduled=%d FPS | base=%.1f FPS | output=%.1f FPS | GPU generation=%.3f ms",
            state,realFrames,generatedFrames,endpointFrames,heldFrames,SlopiumConfig.get().frameGenerationInputFps,SCHEDULE.targetFps(),seconds<=0?0:(realFrames-realStarts)/seconds,seconds<=0?0:(outputFrames-outputStarts)/seconds,gpuMilliseconds);
    }
    public static void resetCounters(){AsyncPresenter.resetCounters();FastFramePath.reset();realFrames=generatedFrames=endpointFrames=outputFrames=heldFrames=0;lastOutput=elapsedNanos=outputStarts=realStarts=0;gpuMilliseconds=0;if(timer!=null){timer.nanos=timer.samples=0;}}
    private static void recordOutput(long now,boolean real) {
        if(lastOutput!=0)elapsedNanos+=now-lastOutput;
        else {outputStarts++;if(real)realStarts++;}
        lastOutput=now;
        outputFrames++;
    }
    public static void release() {
        try(var ignored=new GlState()) {
            if(simple!=null){simple.close();simple=null;}if(fsr!=null){fsr.close();fsr=null;}
            if(reprojection!=null){reprojection.close();reprojection=null;}
            if(timer!=null){gpuMilliseconds=timer.milliseconds();timer.close();timer=null;}
            if(previous!=null){previous.close();previous=null;}if(current!=null){current.close();current=null;}if(generated!=null){generated.close();generated=null;}
            if(fsrMiddle!=null){fsrMiddle.close();fsrMiddle=null;}
        }
        CADENCE.reset();SCHEDULE.reset();heldSource=0;previousVp=null;eye=null;rotation=null;world=null;active=false;lastCapture=lastOutput=0;
    }
    public static void render(Camera camera,Matrix4f view,Matrix4f projection,Runnable draw) {
        lastWorldReal=false;
        if(AsyncPresenter.active()){long stamp=System.nanoTime();draw.run();AsyncPresenter.captureWorld(camera,view,projection,stamp);return;}
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();var main=mc.getMainRenderTarget();
        String blocked=!c.enabled||c.frameGenerationMode==FrameGenerationMode.OFF?"off":
            mc.screen!=null?"paused (menu)":mc.getOverlay()!=null?"paused (loading overlay)":
            mc.isPaused()?"paused (game)":mc.level==null?"paused (no world)":!mc.isWindowActive()?"paused (window focus)":
            Slopium.loaded("iris")||Slopium.loaded("oculus")?"shader integration unsupported":
            conflictingGenerator()?net.minecraft.network.chat.Component.translatable("slopium.frame_generation.conflict").getString():
            mc.options.graphicsMode().get()==GraphicsStatus.FABULOUS?"Fabulous unsupported":
            glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)!=main.frameBufferId?"custom framebuffer unsupported":null;
        if(blocked!=null){
            if(blocked.startsWith("paused")&&mc.level!=null&&current!=null) {
                // Keep expensive native contexts over chat/menu pauses; never reuse temporal history.
                CADENCE.reset();SCHEDULE.reset();previousVp=null;eye=null;rotation=null;lastCapture=lastOutput=0;active=false;
            } else if(active||current!=null)release();
            state=blocked.equals("off")?blocked:
            failedRevision==Slopium.configRevision()?failureState:blocked;
            if(!blocked.equals("off")&&!blocked.startsWith("paused")&&!blocked.equals(lastNotice)&&mc.player!=null) {
                lastNotice=blocked;mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Slopium FG: "+blocked),false);
                com.mojang.logging.LogUtils.getLogger().warn("Slopium FG inactive: {}",blocked);
            }
            draw.run();return;}
        if(failedRevision==Slopium.configRevision()){state=failureState;draw.run();return;}
        boolean drew=false,drawing=false;
        try {
            int multiplier=Math.clamp(c.frameGenerationMultiplier,2,32);
            if(current==null||current.width!=main.width||current.height!=main.height||world!=mc.level||revision!=Slopium.configRevision()||mode!=c.frameGenerationMode||CADENCE.multiplier()!=multiplier) {
                release();mode=c.frameGenerationMode;revision=Slopium.configRevision();world=mc.level;
                CADENCE.configure(multiplier);
                try(var ignored=new GlState()) {
                    glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,main.getColorTextureId());
                    int colorFormat=glGetTexLevelParameteri(GL_TEXTURE_2D,0,GL_TEXTURE_INTERNAL_FORMAT);
                    if(colorFormat!=GL_RGBA8&&colorFormat!=GL_RGBA)throw new IllegalStateException("Only LDR RGBA8 framebuffers are supported");
                    glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,main.getDepthTextureId());
                    int depthFormat=glGetTexLevelParameteri(GL_TEXTURE_2D,0,GL_TEXTURE_INTERNAL_FORMAT);
                    current=new GlImage(main.width,main.height,GL_RGBA8,depthFormat);
                    if(mode==FrameGenerationMode.SIMPLE){reprojection=new dev.slopium.framegen.reference.Reprojector();generated=new GlImage(main.width,main.height,GL_RGBA8,depthFormat);}
                    if(mode==FrameGenerationMode.FSR&&multiplier>2)simple=new SimpleInterpolator(main.width,main.height,c.frameGenerationFlowCell);
                    if(mode==FrameGenerationMode.FSR) {
                        previous=new GlImage(main.width,main.height,GL_RGBA8,depthFormat);
                        generated=new GlImage(main.width,main.height,GL_RGBA8,0);
                        fsr=new FsrInterpolator(main.width,main.height);
                        if(multiplier>2)fsrMiddle=new GlImage(main.width,main.height,GL_RGBA8,0);
                    }
                }
            }
            long now=System.nanoTime();
            SCHEDULE.configure(c.frameGenerationInputFps);SCHEDULE.begin(now);
            if(SCHEDULE.probing()) {
                drawing=true;draw.run();drawing=false;drew=true;
                CADENCE.reset();previousVp=null;eye=null;rotation=null;lastCapture=0;
                realFrames++;recordOutput(now,true);active=true;state="measuring real frame rate";return;
            }
            // Low input targets can retain a frame longer than 250 ms: detect output stalls instead.
            if(eye!=null&&(eye.distanceToSqr(camera.getPosition())>64||Math.abs(rotation.dot(camera.rotation()))<.995f||lastOutput!=0&&now-lastOutput>250_000_000L)) {
                // Rapid camera turns do not force real frames ahead of their scheduled time.
                if(previousVp!=null&&!SCHEDULE.due(now)){hold(main,now);return;}
                CADENCE.reset();previousVp=null;
            }
            if(mode==FrameGenerationMode.SIMPLE) {
                Matrix4f vp=new Matrix4f(projection).mul(view);
                if(previousVp!=null&&!SCHEDULE.due(now)) {
                    intermediate(camera,view,projection,now,multiplier);
                } else {
                    drawing=true;draw.run();drawing=false;drew=true;
                    try(var ignored=new GlState()){present(main.frameBufferId,current.fbo,main.width,main.height,true);}
                    previousVp=vp;eye=camera.getPosition();rotation=new Quaternionf(camera.rotation());lastCapture=now;
                    heldSource=current.fbo;SCHEDULE.captured(now);lastWorldReal=true;realFrames++;recordOutput(now,true);
                }
                active=true;state=backend="FrameGen reprojection up to x"+multiplier;return;
            }
            if(previousVp!=null&&!SCHEDULE.due(now)) {
                intermediate(camera,view,projection,now,multiplier);return;
            }
            drawing=true;draw.run();drawing=false;drew=true;
            Matrix4f vp=new Matrix4f(projection).mul(view),toPrevious=new Matrix4f();
            if(previousVp!=null&&eye!=null){var delta=camera.getPosition().subtract(eye);toPrevious.set(previousVp).translate((float)delta.x,(float)delta.y,(float)delta.z).mul(new Matrix4f(vp).invert());}
            boolean interpolate=CADENCE.captured();
            try(var ignored=new GlState()) {
                // The adaptive FSR budget needs delayed GPU costs even when the debug overlay is off.
                if(timer==null)timer=new GpuTimer();
                if(timer!=null)timer.start();
                try {
                var swap=previous;previous=current;current=swap;
                GlState.fullscreen();glBindFramebuffer(GL_READ_FRAMEBUFFER,main.frameBufferId);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,current.fbo);
                glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT,GL_NEAREST);
                if(System.nanoTime()-now+Math.max(1_000_000,(long)(gpuMilliseconds*1e6))<SCHEDULE.period()) {
                    fsr.generate(current,fsrMiddle==null?generated:fsrMiddle,toPrevious,projection,!interpolate,lastCapture==0?16.667f:(now-lastCapture)/1e6f,c.frameGenerationFsrDebug);
                    if(interpolate&&simple!=null)simple.prepare(previous,current,toPrevious,new Matrix4f(toPrevious).invert(),c.frameGenerationSearchPixels);
                } else {CADENCE.reset();interpolate=false;}
                // Start the delayed pair at its older endpoint. Intermediate fractions follow time.
                heldSource=interpolate?previous.fbo:current.fbo;
                present(heldSource,main.frameBufferId,main.width,main.height,false);
                } finally {if(timer!=null){timer.stop();gpuMilliseconds=timer.milliseconds();}}
            }
            previousVp=vp;eye=camera.getPosition();rotation=new Quaternionf(camera.rotation());lastCapture=now;
            SCHEDULE.captured(now);lastWorldReal=true;
            recordOutput(now,true);realFrames++;active=true;
            state=backend="FSR "+fsr.provider+(multiplier>2?" + Slopium":"")+" up to x"+multiplier;
        } catch(RuntimeException|LinkageError failure) {
            if(drawing)throw failure;
            failedRevision=Slopium.configRevision();state=failureState="disabled: "+failure.getMessage();active=false;
            com.mojang.logging.LogUtils.getLogger().error("Slopium frame generation disabled until settings change",failure);
            if(mc.player!=null)mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("slopium.frame_generation.unavailable",failure.getMessage()),false);
            if(!drew)draw.run();
            // A valid current frame is already in the main target on dispatch failure.
            release();
        }
    }
    private static void intermediate(Camera camera,Matrix4f view,Matrix4f projection,long now,int multiplier) {
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();var main=mc.getMainRenderTarget();
        if(mode==FrameGenerationMode.SIMPLE) {
            Matrix4f vp=new Matrix4f(projection).mul(view);
            if(!SCHEDULE.canGenerate(now,multiplier)){hold(main,now);return;}
            try(var ignored=new GlState()) {
                if(c.diagnostics&&timer==null)timer=new GpuTimer();
                if(timer!=null)timer.start();
                try {
                    reprojection.draw(current,generated.fbo,previousVp,vp,eye.subtract(camera.getPosition()),projection,c.frameGenerationGridStep);
                    heldSource=generated.fbo;present(generated.fbo,main.frameBufferId,main.width,main.height,true);
                    mark(main,c);
                } finally {if(timer!=null){timer.stop();gpuMilliseconds=timer.milliseconds();}}
            }
            restoreLighting(mc);SCHEDULE.generated(Math.max(System.nanoTime()-now,(long)(gpuMilliseconds*1e6)));generatedFrames++;recordOutput(now,false);
            return;
        }
        // Skip obsolete fractions rather than delaying the next real frame to drain a pair.
        if(!CADENCE.sample(now-lastCapture,SCHEDULE.period())||!SCHEDULE.canGenerate(now,multiplier)) {hold(main,now);return;}
        try(var ignored=new GlState()) {
            if(CADENCE.endpoint()) {heldSource=current.fbo;present(current.fbo,main.frameBufferId,main.width,main.height,true);endpointFrames++;}
            else {
                if(timer!=null)timer.start();
                try {
                    GlState.fullscreen();glBindFramebuffer(GL_READ_FRAMEBUFFER,current.fbo);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,main.frameBufferId);
                    glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL_DEPTH_BUFFER_BIT,GL_NEAREST);
                    presentGenerated(main,c);
                }finally{if(timer!=null){timer.stop();gpuMilliseconds=timer.milliseconds();}}
            }
        }
        restoreLighting(mc);SCHEDULE.generated(Math.max(System.nanoTime()-now,(long)(gpuMilliseconds*1e6)));recordOutput(now,false);active=true;return;
    }
    public static boolean fastEligible(long now) {
        var c=SlopiumConfig.get();var mc=Minecraft.getInstance();var main=mc.getMainRenderTarget();
        return active&&c.enabled&&c.frameGenerationFastPath&&c.frameGenerationMode==mode&&mode!=FrameGenerationMode.OFF
            &&revision==Slopium.configRevision()&&fastFailedRevision!=revision&&current!=null&&previousVp!=null
            &&mc.level==world&&mc.player!=null&&mc.screen==null&&mc.getOverlay()==null&&!mc.isPaused()&&mc.isWindowActive()
            &&mc.options.graphicsMode().get()!=GraphicsStatus.FABULOUS&&!Slopium.loaded("iris")&&!Slopium.loaded("oculus")
            &&current.width==main.width&&current.height==main.height&&!SCHEDULE.probeDue(now)&&!SCHEDULE.due(now)
            &&CADENCE.multiplier()==c.frameGenerationMultiplier;
    }
    public static boolean tryFast(Camera camera,Matrix4f view,Matrix4f projection) {
        long now=System.nanoTime();if(!fastEligible(now))return false;
        var mc=Minecraft.getInstance();if(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)!=mc.getMainRenderTarget().frameBufferId)return false;
        // Keep normal camera-cut handling and any changed camera effects on the complete path.
        if(eye==null||eye.distanceToSqr(camera.getPosition())>64||Math.abs(rotation.dot(camera.rotation()))<.995f
            ||mc.player.spinningEffectIntensity>0||mc.player.hurtTime>0)return false;
        try {
            SCHEDULE.begin(now);lastWorldReal=false;
            intermediate(camera,view,projection,now,CADENCE.multiplier());return true;
        } catch(RuntimeException|LinkageError e) {
            disableFast(e);return false;
        }
    }
    public static void disableFast(Throwable e) {
        fastFailedRevision=Slopium.configRevision();fastProblem=e.getClass().getSimpleName()+": "+e.getMessage();
        com.mojang.logging.LogUtils.getLogger().warn("Slopium short frame path failed; using ordinary rendering until settings change",e);
        release();
    }
    public static String fastFailure(){return fastFailedRevision==Slopium.configRevision()?" | fallback="+fastProblem:"";}
    public static void captureLateWorld() {
        if(!lastWorldReal||mode!=FrameGenerationMode.SIMPLE||current==null||!SlopiumConfig.get().frameGenerationFastPath)return;
        var main=Minecraft.getInstance().getMainRenderTarget();
        // Include AFTER_LEVEL geometry in simple reprojection, before hand/HUD are drawn.
        try(var ignored=new GlState()){present(main.frameBufferId,current.fbo,main.width,main.height,true);}
    }
    public static void finishFastWorld(Matrix4f view,Matrix4f projection) {
        var mc=Minecraft.getInstance();mc.gameRenderer.resetProjectionMatrix(projection);
        if(mode==FrameGenerationMode.FSR) {
            // FSR history is captured earlier: keep late mod geometry live instead of dropping it.
            net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(
                net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL,mc.levelRenderer,null,
                view,projection,mc.levelRenderer.getTicks(),mc.gameRenderer.getMainCamera(),mc.levelRenderer.getFrustum());
        }
    }
    private static void hold(com.mojang.blaze3d.pipeline.RenderTarget main,long now) {
        try(var ignored=new GlState()){
            present(heldSource==0?current.fbo:heldSource,main.frameBufferId,main.width,main.height,false);
            glBindFramebuffer(GL_READ_FRAMEBUFFER,mode==FrameGenerationMode.SIMPLE&&heldSource==generated.fbo?generated.fbo:current.fbo);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,main.frameBufferId);
            glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL_DEPTH_BUFFER_BIT,GL_NEAREST);
        }
        restoreLighting(Minecraft.getInstance());heldFrames++;recordOutput(now,false);active=true;
    }
    private static void presentGenerated(com.mojang.blaze3d.pipeline.RenderTarget main,SlopiumConfig c) {
        int source;
        if(fsr!=null&&CADENCE.midpoint())source=(fsrMiddle==null?generated:fsrMiddle).fbo;
        else {
            simple.synthesize(previous,current,generated,CADENCE.fraction(),c.frameGenerationRejectionPixels,fsrMiddle);
            source=generated.fbo;
        }
        heldSource=source;present(source,main.frameBufferId,main.width,main.height,false);
        mark(main,c);
        generatedFrames++;
    }
    private static void restoreLighting(Minecraft mc) {
        if(mc.level.effects().constantAmbientLight())com.mojang.blaze3d.platform.Lighting.setupNetherLevel();
        else com.mojang.blaze3d.platform.Lighting.setupLevel();
    }
    private static void mark(com.mojang.blaze3d.pipeline.RenderTarget main,SlopiumConfig c) {
        if(c.frameGenerationMarkFrames) {
            glEnable(GL_SCISSOR_TEST);glScissor(0,main.height-4,main.width,4);
            glClearBufferfv(GL_COLOR,0,FRAME_MARKER);
        }
    }
    private static void present(int source,int destination,int w,int h,boolean depth) {
        GlState.fullscreen();glBindFramebuffer(GL_READ_FRAMEBUFFER,source);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,destination);
        glBlitFramebuffer(0,0,w,h,0,0,w,h,GL_COLOR_BUFFER_BIT|(depth?GL_DEPTH_BUFFER_BIT:0),GL_NEAREST);
    }
}

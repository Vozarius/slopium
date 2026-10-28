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
    private static GlImage previous,current,generated,fsrMiddle;
    private static final float[] FRAME_MARKER={1,.1f,.7f,1};
    private static SimpleInterpolator simple;
    private static dev.slopium.framegen.reference.Reprojector reprojection;
    private static int reprojected;
    private static String lastNotice="";
    private static FsrInterpolator fsr;
    private static GpuTimer timer;
    private static double gpuMilliseconds;
    private static Matrix4f previousVp;
    private static Vec3 eye;
    private static Quaternionf rotation;
    private static Object world;
    private static long revision=-1,failedRevision=-1,lastCapture,lastOutput,elapsedNanos,outputStarts,realStarts;
    private static FrameGenerationMode mode=FrameGenerationMode.OFF;
    private static boolean active;
    public static String state="off";
    private static String backend="not started",failureState="";
    public static long realFrames,generatedFrames,endpointFrames,outputFrames;
    private FrameGeneration() {}
    public static boolean conflictingGenerator(){return Slopium.loaded("framegen")||Slopium.loaded("pfg");}
    public static void overlay(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        if(!c.enabled||c.frameGenerationMode==FrameGenerationMode.OFF||mc.level==null||mc.screen!=null)return;
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
        int target=SlopiumConfig.get().frameGenerationTargetFps;
        return active&&target>0?Math.min(original,target):original;
    }
    public static String status() {
        double seconds=elapsedNanos/1e9;
        return String.format(java.util.Locale.ROOT,"Frame generation | %s | real=%d | generated=%d | endpoints=%d | base=%.1f FPS | output=%.1f FPS | GPU generation=%.3f ms",
            state,realFrames,generatedFrames,endpointFrames,seconds<=0?0:(realFrames-realStarts)/seconds,seconds<=0?0:(outputFrames-outputStarts)/seconds,gpuMilliseconds);
    }
    public static void resetCounters(){realFrames=generatedFrames=endpointFrames=outputFrames=0;lastOutput=elapsedNanos=outputStarts=realStarts=0;gpuMilliseconds=0;if(timer!=null){timer.nanos=timer.samples=0;}}
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
        CADENCE.reset();reprojected=0;previousVp=null;eye=null;rotation=null;world=null;active=false;lastCapture=lastOutput=0;
    }
    public static void render(Camera camera,Matrix4f view,Matrix4f projection,Runnable draw) {
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
                CADENCE.reset();previousVp=null;eye=null;rotation=null;lastCapture=lastOutput=0;active=false;
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
                    if(mode==FrameGenerationMode.SIMPLE)reprojection=new dev.slopium.framegen.reference.Reprojector();
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
            // A high multiplier intentionally keeps one pair longer than 250 ms. Detect a stalled
            // output stream, not the age of the real frame, or x32 would reset halfway through.
            if(eye!=null&&(eye.distanceToSqr(camera.getPosition())>64||Math.abs(rotation.dot(camera.rotation()))<.995f||lastOutput!=0&&now-lastOutput>250_000_000L)) {CADENCE.reset();previousVp=null;}
            if(mode==FrameGenerationMode.SIMPLE) {
                Matrix4f vp=new Matrix4f(projection).mul(view);
                if(previousVp!=null&&reprojected<multiplier-1) {
                    try(var ignored=new GlState()) {
                        if(c.diagnostics&&timer==null)timer=new GpuTimer();
                        if(timer!=null)timer.start();
                        try {
                            reprojection.draw(current,main.frameBufferId,previousVp,vp,eye.subtract(camera.getPosition()),projection,c.frameGenerationGridStep);
                            mark(main,c);
                        } finally {if(timer!=null){timer.stop();gpuMilliseconds=timer.milliseconds();}}
                    }
                    restoreLighting(mc);reprojected++;generatedFrames++;recordOutput(now,false);
                } else {
                    drawing=true;draw.run();drawing=false;drew=true;
                    try(var ignored=new GlState()){present(main.frameBufferId,current.fbo,main.width,main.height,true);}
                    previousVp=vp;eye=camera.getPosition();rotation=new Quaternionf(camera.rotation());lastCapture=now;
                    reprojected=0;realFrames++;recordOutput(now,true);
                }
                active=true;state=backend="FrameGen reprojection x"+multiplier;return;
            }
            if(!CADENCE.needsWorld()) {
                try(var ignored=new GlState()) {
                    if(CADENCE.endpoint()) {present(current.fbo,main.frameBufferId,main.width,main.height,true);endpointFrames++;}
                    else {
                        if(timer!=null)timer.start();
                        try {
                            GlState.fullscreen();glBindFramebuffer(GL_READ_FRAMEBUFFER,current.fbo);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,main.frameBufferId);
                            glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL_DEPTH_BUFFER_BIT,GL_NEAREST);
                            presentGenerated(main,c);
                        }finally{if(timer!=null){timer.stop();gpuMilliseconds=timer.milliseconds();}}
                    }
                }
                restoreLighting(mc);CADENCE.presented();recordOutput(now,false);active=true;return;
            }
            drawing=true;draw.run();drawing=false;drew=true;
            Matrix4f vp=new Matrix4f(projection).mul(view),toPrevious=new Matrix4f();
            if(previousVp!=null&&eye!=null){var delta=camera.getPosition().subtract(eye);toPrevious.set(previousVp).translate((float)delta.x,(float)delta.y,(float)delta.z).mul(new Matrix4f(vp).invert());}
            boolean interpolate=CADENCE.captured();
            try(var ignored=new GlState()) {
                if(c.diagnostics&&timer==null)timer=new GpuTimer();
                if(timer!=null)timer.start();
                try {
                var swap=previous;previous=current;current=swap;
                GlState.fullscreen();glBindFramebuffer(GL_READ_FRAMEBUFFER,main.frameBufferId);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,current.fbo);
                glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT,GL_NEAREST);
                if(fsr!=null) fsr.generate(current,fsrMiddle==null?generated:fsrMiddle,toPrevious,projection,!interpolate,lastCapture==0?16.667f:(now-lastCapture)/1e6f,c.frameGenerationFsrDebug);
                if(interpolate&&simple!=null)simple.prepare(previous,current,toPrevious,new Matrix4f(toPrevious).invert(),c.frameGenerationSearchPixels);
                if(interpolate){presentGenerated(main,c);CADENCE.presented();}
                } finally {if(timer!=null){timer.stop();gpuMilliseconds=timer.milliseconds();}}
            }
            previousVp=vp;eye=camera.getPosition();rotation=new Quaternionf(camera.rotation());lastCapture=now;
            recordOutput(now,true);realFrames++;active=true;
            state=backend=(mode==FrameGenerationMode.SIMPLE?"Simple":"FSR "+fsr.provider+(multiplier>2?" + Slopium":""))+" x"+multiplier;
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
    private static void presentGenerated(com.mojang.blaze3d.pipeline.RenderTarget main,SlopiumConfig c) {
        int source;
        if(fsr!=null&&CADENCE.midpoint())source=(fsrMiddle==null?generated:fsrMiddle).fbo;
        else {
            simple.synthesize(previous,current,generated,CADENCE.fraction(),c.frameGenerationRejectionPixels,fsrMiddle);
            source=generated.fbo;
        }
        present(source,main.frameBufferId,main.width,main.height,false);
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

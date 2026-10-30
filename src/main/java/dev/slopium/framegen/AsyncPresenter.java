package dev.slopium.framegen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.logging.LogUtils;
import dev.slopium.client.Slopium;
import dev.slopium.client.SlopiumConfig;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.Platform;
import org.lwjgl.system.windows.GDI32;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import static org.lwjgl.opengl.GL33C.*;

/** Windows-only experimental presenter. Minecraft keeps its original GL context and all of its
 * objects, but its drawable becomes a hidden window. The worker exclusively owns the visible
 * drawable and a separate shared context. No Minecraft/world APIs are called on the worker.
 * Only fenced immutable colour snapshots cross the thread boundary; FBOs/VAOs are context-local. */
public final class AsyncPresenter {
    private static Session session;
    private static long failedRevision=-1;
    private static String problem="";
    public static String state="off";
    private AsyncPresenter() {}
    public static boolean supported(){return Platform.get()==Platform.WINDOWS;}
    public static boolean active(){return session!=null;}
    public static long outputs(){return session==null?0:session.outputs;}
    public static long generated(){return session==null?0:session.generated;}
    public static long published(){return session==null?0:session.published;}
    public static void resetCounters(){
        Session s=session;if(s==null)return;
        s.basePublished=s.published;s.baseDropped=s.dropped;s.baseOutputs=s.outputs;
        s.baseGenerated=s.generated;s.baseReceived=s.received;s.baseSwap=s.swapNanos;s.statsBegan=System.nanoTime();
    }
    public static String failureNotice(){return failedRevision==Slopium.configRevision()?"Parallel FG disabled: "+problem:null;}
    public static String status(){
        Session s=session;
        if(s==null)return "Parallel FG | "+state;
        double seconds=Math.max(.001,(System.nanoTime()-s.statsBegan)/1e9);
        long outputs=s.outputs-s.baseOutputs;
        return String.format(java.util.Locale.ROOT,"Parallel FG | %s | real=%.1f FPS | output=%.1f FPS | generated=%d | published=%d | dropped=%d | copied=%d | GPU=%.3f ms | swap=%.3f ms | %dx%d",
            state,(s.published-s.basePublished+s.dropped-s.baseDropped)/seconds,outputs/seconds,s.generated-s.baseGenerated,
            s.published-s.basePublished,s.dropped-s.baseDropped,s.received-s.baseReceived,s.gpuMs,
            outputs==0?0:(s.swapNanos-s.baseSwap)/1e6/outputs,s.width,s.height);
    }
    public static String overlay(){
        Session s=session;if(s==null)return state;
        double seconds=Math.max(.001,(System.nanoTime()-s.statsBegan)/1e9);
        return String.format(java.util.Locale.ROOT,"Parallel FG | real %.0f FPS | output %.0f FPS | generated %d",
            (s.published-s.basePublished+s.dropped-s.baseDropped)/seconds,(s.outputs-s.baseOutputs)/seconds,s.generated-s.baseGenerated);
    }
    // Render thread, before any per-frame work. Switching drawables preserves Minecraft's GL cache.
    public static void beginFrame(){
        var mc=Minecraft.getInstance();
        if(!mc.isGameLoadFinished())return;
        var c=SlopiumConfig.get();
        boolean wanted=c.enabled&&c.frameGenerationParallel&&c.frameGenerationMode==FrameGenerationMode.SIMPLE
            &&supported()&&mc.level!=null&&mc.player!=null&&mc.screen==null&&mc.getOverlay()==null
            &&!mc.isPaused()&&mc.isWindowActive()&&!FrameGeneration.conflictingGenerator()
            &&!Slopium.loaded("iris")&&!Slopium.loaded("oculus")
            &&mc.options.graphicsMode().get()!=net.minecraft.client.GraphicsStatus.FABULOUS;
        if(session!=null) {
            if(session.failure!=null){problem=session.failure.toString();failedRevision=Slopium.configRevision();wanted=false;}
            if(!wanted||session.world!=mc.level||session.width!=mc.getWindow().getWidth()||session.height!=mc.getWindow().getHeight()
                ||session.revision!=Slopium.configRevision()||!session.running) {
                stop();return;
            }
            session.settings=settings(mc,c);return;
        }
        if(!wanted){state=c.frameGenerationParallel?"paused / requires Simple, Windows, focused world":"off";return;}
        if(failedRevision==Slopium.configRevision()){state="disabled: "+problem;return;}
        FrameGeneration.release();
        Session next=new Session(mc.getWindow().getWindow(),mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.level,settings(mc,c));
        try {next.start();session=next;state="active (screen-space interpolation)";}
        catch(Exception|LinkageError e){
            next.running=false;
            // start() transfers ownership only after the worker is ready, and restores on failure.
            problem=e.toString();failedRevision=Slopium.configRevision();state="disabled: "+problem;
            LogUtils.getLogger().error("Slopium parallel presenter could not start; ordinary rendering retained",e);
        }
    }
    private static Settings settings(Minecraft mc,SlopiumConfig c){
        int vanilla=mc.options.framerateLimit().get();
        int cap=c.frameGenerationTargetFps;
        if(vanilla<260)cap=cap>0?Math.min(cap,vanilla):vanilla;
        return new Settings(Math.clamp(c.frameGenerationMultiplier,2,32),Math.max(0,cap),
            Math.clamp(c.frameGenerationFlowCell,4,16),Math.clamp(c.frameGenerationSearchPixels,4,32),
            Math.clamp(c.frameGenerationRejectionPixels,1,16),mc.options.enableVsync().get());
    }
    public static int inputLimit(int original){
        if(!active())return original;
        int target=SlopiumConfig.get().frameGenerationInputFps;
        return target<=0?original:original>=260?target:Math.min(original,target);
    }
    /** Called instead of the final blit. Never waits for a busy slot or the presenter's GPU work. */
    public static boolean publish(RenderTarget target){
        Session s=session;if(s==null)return false;
        var mc=Minecraft.getInstance();
        if(mc.level!=s.world||mc.screen!=null||mc.getOverlay()!=null||!mc.isWindowActive()){stop();return active();}
        if(!s.running)return true;
        try(var ignored=new GlState()) {
            for(Slot slot:s.slots) {
                if(slot.state.get()==4&&ready(slot.fence)){glDeleteSync(slot.fence);slot.fence=0;slot.state.set(0);}
                if(!slot.state.compareAndSet(0,1))continue;
                GlState.fullscreen();
                glBindFramebuffer(GL_READ_FRAMEBUFFER,target.frameBufferId);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER,slot.image.fbo);
                glBlitFramebuffer(0,0,target.width,target.height,0,0,s.width,s.height,GL_COLOR_BUFFER_BIT,GL_NEAREST);
                var camera=mc.gameRenderer.getMainCamera();
                slot.cut=s.lastEye==null||s.lastEye.distanceToSqr(camera.getPosition())>64
                    ||Math.abs(s.lastRotation.dot(camera.rotation()))<.995f;
                s.lastEye=camera.getPosition();s.lastRotation.set(camera.rotation());
                slot.timestamp=System.nanoTime();slot.sequence=++s.published;
                slot.fence=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);glFlush();slot.state.set(2);
                s.wake();return true;
            }
            s.dropped++;return true;
        } catch(RuntimeException e){s.failure=e;s.running=false;return true;}
    }
    private static boolean ready(long fence){
        int result=glClientWaitSync(fence,0,0);
        if(result==GL_WAIT_FAILED)throw new IllegalStateException("Shared GPU fence failed");
        return result==GL_ALREADY_SIGNALED||result==GL_CONDITION_SATISFIED;
    }
    public static void stop(){
        Session s=session;if(s==null)return;
        s.running=false;s.wake();
        try{s.worker.join(20);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        // A driver may still be in SwapBuffers. Do not destroy its live context or race its drawable.
        if(s.worker.isAlive()){state="stopping (waiting for GPU)";return;}
        if(!WGL.wglMakeCurrent(s.visibleDc,s.renderRc))throw new IllegalStateException("Cannot restore Minecraft drawable");
        try(var ignored=new GlState()){for(Slot slot:s.slots){if(slot.fence!=0)glDeleteSync(slot.fence);slot.image.close();}}
        GLFW.glfwDestroyWindow(s.hidden);session=null;
        if(s.pacer!=null)s.pacer.close();
        state=failedRevision==Slopium.configRevision()?"disabled: "+problem:"off";
    }
    /** Window destruction must not race a live context. Only shutdown may wait for completion. */
    public static void shutdown(){
        Session s=session;if(s==null)return;
        s.running=false;s.wake();
        try{s.worker.join(5000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        if(s.worker.isAlive())throw new IllegalStateException("Parallel presenter did not stop before window destruction");
        stop();
    }
    private record Settings(int multiplier,int cap,int cell,int search,int rejection,boolean vsync){}
    private static final class Slot {
        // FREE -> WRITING -> READY -> READING -> GPU_RELEASE_PENDING -> FREE.
        final AtomicInteger state=new AtomicInteger();final GlImage image;
        long timestamp,sequence,fence;boolean cut;
        Slot(int w,int h){image=new GlImage(w,h,GL_RGBA8,0);}
    }
    private static final class Session {
        final long window,revision=Slopium.configRevision();final int width,height;
        final Object world;
        net.minecraft.world.phys.Vec3 lastEye;
        final org.joml.Quaternionf lastRotation=new org.joml.Quaternionf();
        final Slot[] slots=new Slot[3];
        long hidden,visibleDc,renderRc,workerRc,began;
        long statsBegan,basePublished,baseDropped,baseOutputs,baseGenerated,baseReceived,baseSwap;
        Thread worker;volatile boolean running;volatile Throwable failure;
        PresentationTimer pacer;
        volatile Settings settings;
        volatile long published,outputs,generated,received,dropped,swapNanos;
        volatile double gpuMs;
        Session(long window,int w,int h,Object world,Settings settings){this.window=window;width=w;height=h;this.world=world;this.settings=settings;}
        void wake(){if(pacer!=null)pacer.wake();LockSupport.unpark(worker);}
        void await(long nanos){if(pacer!=null)pacer.await(nanos);else LockSupport.parkNanos(nanos);}
        void start() throws Exception {
            visibleDc=WGL.wglGetCurrentDC();renderRc=WGL.wglGetCurrentContext();
            if(visibleDc==0||renderRc==0)throw new IllegalStateException("Native Windows WGL context required");
            var capabilities=GL.getCapabilities();
            long hiddenDc;
            try {
                pacer=PresentationTimer.open();
                LogUtils.getLogger().info("Slopium presenter timer: {}",pacer==null?"JVM fallback":"Windows high resolution");
                // Match the original drawable's exact pixel format before exchanging drawables.
                try(var stack=org.lwjgl.system.MemoryStack.stackPush()) {
                    var pf=org.lwjgl.system.windows.PIXELFORMATDESCRIPTOR.calloc(stack);
                    int format=GDI32.GetPixelFormat(visibleDc);
                    GDI32.DescribePixelFormat(visibleDc,format,pf);
                    GLFW.glfwDefaultWindowHints();GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
                    GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED,GLFW.GLFW_FALSE);
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,glGetInteger(GL_MAJOR_VERSION));
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,glGetInteger(GL_MINOR_VERSION));
                    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
                    GLFW.glfwWindowHint(GLFW.GLFW_RED_BITS,Byte.toUnsignedInt(pf.cRedBits()));
                    GLFW.glfwWindowHint(GLFW.GLFW_GREEN_BITS,Byte.toUnsignedInt(pf.cGreenBits()));
                    GLFW.glfwWindowHint(GLFW.GLFW_BLUE_BITS,Byte.toUnsignedInt(pf.cBlueBits()));
                    GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS,Byte.toUnsignedInt(pf.cAlphaBits()));
                    GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS,Byte.toUnsignedInt(pf.cDepthBits()));
                    GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS,Byte.toUnsignedInt(pf.cStencilBits()));
                    GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES,glGetInteger(GL_SAMPLES));
                    hidden=GLFW.glfwCreateWindow(16,16,"Slopium offscreen",0,window);
                    if(hidden==0)throw new IllegalStateException("Shared context creation failed");
                    GLFW.glfwMakeContextCurrent(hidden);hiddenDc=WGL.wglGetCurrentDC();workerRc=WGL.wglGetCurrentContext();
                    GLFW.glfwMakeContextCurrent(window);GL.setCapabilities(capabilities);
                    if(GDI32.GetPixelFormat(hiddenDc)!=format)throw new IllegalStateException("Shared drawable pixel format mismatch");
                } finally {GLFW.glfwDefaultWindowHints();}
                try(var ignored=new GlState()){for(int i=0;i<slots.length;i++)slots[i]=new Slot(width,height);}
                // One-time ownership handoff. There are no glFinish calls in steady-state exchange.
                glFinish();
                if(!WGL.wglMakeCurrent(hiddenDc,renderRc))throw new IllegalStateException("Cannot move Minecraft to offscreen drawable");
                running=true;began=statsBegan=System.nanoTime();CountDownLatch ready=new CountDownLatch(1);
                worker=new Thread(()->run(ready),"Slopium presenter");worker.setDaemon(true);worker.start();
                if(!ready.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Presenter initialization timed out");
                if(failure!=null)throw new IllegalStateException("Presenter initialization failed",failure);
            } catch(Exception|LinkageError e) {
                running=false;
                if(worker!=null){wake();worker.join(5000);if(worker.isAlive()){session=this;throw new IllegalStateException("Presenter still owns the window after startup failure",e);}}
                WGL.wglMakeCurrent(visibleDc,renderRc);GL.setCapabilities(capabilities);
                try(var ignored=new GlState()){for(Slot slot:slots)if(slot!=null)slot.image.close();}
                if(hidden!=0)GLFW.glfwDestroyWindow(hidden);
                if(pacer!=null)pacer.close();
                throw e;
            }
        }
        void run(CountDownLatch readyLatch) {
            GlImage previous=null,current=null,output=null;SimpleInterpolator flow=null;GpuTimer timer=null;
            int sourceFbo=0;long gpuFence=0;
            try {
                if(!WGL.wglMakeCurrent(visibleDc,workerRc))throw new IllegalStateException("Cannot attach presenter to visible drawable");
                GL.createCapabilities();
                sourceFbo=glGenFramebuffers();previous=new GlImage(width,height,GL_RGBA8,0);
                current=new GlImage(width,height,GL_RGBA8,0);output=new GlImage(width,height,GL_RGBA8,0);
                flow=new SimpleInterpolator(width,height,settings.cell,true);timer=new GpuTimer();
                Matrix4f identity=new Matrix4f();
                boolean haveCurrent=false,pair=false,endpointShown=false;int phase=-1;
                long lastStamp=0,pairStart=0,period=50_000_000,nextPresent=0;
                boolean vsync=!settings.vsync;
                readyLatch.countDown();
                while(running) {
                    Settings cfg=settings;
                    if(vsync!=cfg.vsync){
                        vsync=cfg.vsync;
                        if(GL.getCapabilitiesWGL().WGL_EXT_swap_control){
                            if(!org.lwjgl.opengl.WGLEXTSwapControl.wglSwapIntervalEXT(vsync?1:0))throw new IllegalStateException("Cannot set presenter swap interval");
                            LogUtils.getLogger().info("Slopium presenter swap interval: {}",org.lwjgl.opengl.WGLEXTSwapControl.wglGetSwapIntervalEXT());
                        }
                    }
                    if(gpuFence!=0) {
                        if(!ready(gpuFence)){await(250_000);continue;}
                        glDeleteSync(gpuFence);gpuFence=0;
                    }
                    Slot newest=null;
                    for(Slot slot:slots)if(slot.state.get()==2&&ready(slot.fence)&&(newest==null||slot.sequence>newest.sequence))newest=slot;
                    if(newest!=null&&newest.state.compareAndSet(2,3)) {
                        // Drop older completed snapshots; never accumulate latency behind a queue.
                        for(Slot slot:slots)if(slot!=newest&&slot.state.get()==2&&slot.sequence<newest.sequence&&ready(slot.fence)&&slot.state.compareAndSet(2,3)) {
                            glDeleteSync(slot.fence);slot.fence=0;slot.state.set(0);
                        }
                        glDeleteSync(newest.fence);
                        GlImage swap=previous;previous=current;current=swap;
                        GlState.fullscreen();glBindFramebuffer(GL_READ_FRAMEBUFFER,sourceFbo);
                        glFramebufferTexture2D(GL_READ_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,newest.image.texture,0);
                        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,current.fbo);
                        glBlitFramebuffer(0,0,width,height,0,0,width,height,GL_COLOR_BUFFER_BIT,GL_NEAREST);
                        long stamp=newest.timestamp;boolean cut=newest.cut;
                        newest.fence=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);glFlush();newest.state.set(4);
                        received++;
                        pair=haveCurrent&&!cut&&stamp>lastStamp&&stamp-lastStamp<250_000_000L;
                        if(pair)period=Math.clamp(stamp-lastStamp,4_000_000L,250_000_000L);
                        lastStamp=stamp;haveCurrent=true;
                        // If the previous pair already reached its endpoint, the next pair starts
                        // with that same image. Do not count or swap the duplicate endpoint twice.
                        phase=pair&&endpointShown?0:-1;endpointShown=false;
                        if(pair){timer.start();flow.prepare(previous,current,identity,identity,cfg.search);timer.stop();gpuMs=timer.milliseconds();}
                        int error=glGetError();if(error!=GL_NO_ERROR)throw new IllegalStateException("Parallel GPU error "+error);
                        pairStart=System.nanoTime();
                    }
                    long now=System.nanoTime();
                    if(haveCurrent&&now>=nextPresent) {
                        int nextPhase=pair?(int)Math.min(cfg.multiplier,(now-pairStart)*cfg.multiplier/period):cfg.multiplier;
                        if(nextPhase>phase&&!endpointShown) {
                            int image;
                            if(pair&&nextPhase>0&&nextPhase<cfg.multiplier) {
                                timer.start();flow.synthesize(previous,current,output,nextPhase/(float)cfg.multiplier,cfg.rejection,null);timer.stop();
                                image=output.fbo;generated++;
                            } else image=pair&&nextPhase==0?previous.fbo:current.fbo;
                            GlState.fullscreen();glBindFramebuffer(GL_READ_FRAMEBUFFER,image);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,0);
                            glDrawBuffer(GL_BACK);glBlitFramebuffer(0,0,width,height,0,0,width,height,GL_COLOR_BUFFER_BIT,GL_NEAREST);
                            gpuFence=glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE,0);glFlush();
                            long swapStart=System.nanoTime();
                            if(!GDI32.SwapBuffers(visibleDc))throw new IllegalStateException("Parallel SwapBuffers failed");
                            swapNanos+=System.nanoTime()-swapStart;
                            outputs++;phase=nextPhase;endpointShown=nextPhase==cfg.multiplier;
                            nextPresent=System.nanoTime()+(cfg.cap==0?0:1_000_000_000L/cfg.cap);
                        }
                    }
                    // No repeated identical swaps after a pair ends, no busy spinning during a stall.
                    long nextPhaseAt=pairStart+(phase+1L)*period/cfg.multiplier;
                    long wait=!haveCurrent||endpointShown?5_000_000:Math.max(50_000,Math.min(5_000_000,Math.max(nextPresent,nextPhaseAt)-System.nanoTime()));
                    await(wait);
                }
            } catch(Throwable e) {failure=e;running=false;LogUtils.getLogger().error("Slopium parallel presenter failed",e);}
            finally {
                readyLatch.countDown();
                if(WGL.wglGetCurrentContext()==workerRc) {
                    // Only at teardown: producer resources cannot be reclaimed while GPU reads them.
                    glFinish();if(gpuFence!=0)glDeleteSync(gpuFence);
                    if(timer!=null)timer.close();if(flow!=null)flow.close();
                    if(previous!=null)previous.close();if(current!=null)current.close();if(output!=null)output.close();
                    if(sourceFbo!=0)glDeleteFramebuffers(sourceFbo);
                    WGL.wglMakeCurrent(0,0);GL.setCapabilities(null);
                }
            }
        }
    }
}

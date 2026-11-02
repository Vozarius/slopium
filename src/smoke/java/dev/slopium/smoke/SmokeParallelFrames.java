package dev.slopium.smoke;
import com.mojang.logging.LogUtils;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.framegen.*;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.concurrent.locks.LockSupport;

/** Proves actual swaps and synthesis continue while the Minecraft thread cannot execute code. */
final class SmokeParallelFrames {
    private final boolean amortize=Boolean.getBoolean("slopium.amortizeSmoke");
    private final FrameGenerationMode mode=Boolean.getBoolean("slopium.parallelFsrSmoke")?FrameGenerationMode.FSR:FrameGenerationMode.SIMPLE;
    private long recoveryGenerated;
    private boolean settled;
    private int phase=-1,frames,progressDuringStalls;
    private long began,startOutput,startGenerated;
    private int lifecycle;
    private long lifecycleAt;
    private String screenshot;
    SmokeParallelFrames(){NeoForge.EVENT_BUS.addListener(this::load);NeoForge.EVENT_BUS.addListener(this::lifecycle);}
    private void load(RenderFrameEvent.Pre event){
        // Previous main frame has been restored after composition by this point.
        if(screenshot!=null){var mc=Minecraft.getInstance();net.minecraft.client.Screenshot.grab(mc.gameDirectory,screenshot,mc.getMainRenderTarget(),m->{});screenshot=null;}
        if(phase<0||phase>2)return;
        long before=AsyncPresenter.generated();
        long beforeOutput=AsyncPresenter.outputs();
        long until=System.nanoTime()+(amortize&&phase==2?1_000_000_000L:55_000_000L);
        while(System.nanoTime()<until)LockSupport.parkNanos(until-System.nanoTime());
        if(AsyncPresenter.generated()>before&&AsyncPresenter.outputs()>beforeOutput)progressDuringStalls++;
    }
    void frame(){
        var mc=Minecraft.getInstance();
        if(phase>=4)return;
        if(phase<0){next();return;}
        if(amortize&&(phase==1||phase==2)&&!settled){
            if(System.nanoTime()-began<3_000_000_000L)return;
            settled=true;began=System.nanoTime();frames=progressDuringStalls=0;
            startOutput=AsyncPresenter.outputs();startGenerated=AsyncPresenter.generated();return;
        }
        frames++;
        if(amortize&&phase==3&&System.nanoTime()-began<3_000_000_000L)recoveryGenerated=AsyncPresenter.generated();
        if(System.nanoTime()-began<(phase==1?20_000_000_000L:8_000_000_000L))return;
        double seconds=(System.nanoTime()-began)/1e9;
        LogUtils.getLogger().info("SLOPIUM_PARALLEL: phase {} actual real {} FPS, phase output {} FPS; {}; progressing stalls={}",phase,frames/seconds,AsyncPresenter.active()?(AsyncPresenter.outputs()-startOutput)/seconds:0,AsyncPresenter.status(),progressDuringStalls);
        if(phase==1||phase==2){
            if(!AsyncPresenter.active())throw new AssertionError("Parallel presenter inactive: "+AsyncPresenter.status());
            if(AsyncPresenter.generated()-startGenerated<30||progressDuringStalls<(amortize&&phase==2?3:20))throw new AssertionError("No synthesis while Minecraft blocked");
            if((AsyncPresenter.outputs()-startOutput)/seconds<frames/seconds*2)throw new AssertionError("Parallel output did not exceed input x2");
            if(amortize&&Math.abs((AsyncPresenter.outputs()-startOutput)/seconds-60)>(phase==2?12:8))throw new AssertionError("Adaptive output did not approach 60 FPS");
            if(phase==2&&(AsyncPresenter.outputs()-startOutput)/seconds>(amortize?62:245))throw new AssertionError("Parallel output cap ignored");
            checkSeparatedWorld();
            screenshot="beta9-parallel-"+mode+"-"+phase+".png";
        }
        if(amortize&&phase==3){
            requireActive();
            if(frames/seconds<65||AsyncPresenter.generated()-recoveryGenerated>2)throw new AssertionError("Synthesis did not stop after source recovery: "+AsyncPresenter.status());
        }
        if(!amortize&&phase==3&&AsyncPresenter.active())throw new AssertionError("Presenter did not return window ownership");
        next();
    }
    private static void checkSeparatedWorld(){
        try(var state=new GlState()){
            var sf=AsyncPresenter.class.getDeclaredField("session");sf.setAccessible(true);Object s=sf.get(null);
            var pf=s.getClass().getDeclaredField("pending");pf.setAccessible(true);Object pending=pf.get(s);
            if(pending==null||!AsyncPresenter.overlayActive())throw new AssertionError("No separated source snapshot");
            var image=pending.getClass().getDeclaredField("image");image.setAccessible(true);var world=(GlImage)image.get(pending);
            float[] p=new float[4];
            org.lwjgl.opengl.GL33C.glBindFramebuffer(org.lwjgl.opengl.GL33C.GL_READ_FRAMEBUFFER,world.fbo);
            org.lwjgl.opengl.GL33C.glReadPixels(world.width/3,world.height/2,1,1,org.lwjgl.opengl.GL33C.GL_RGBA,org.lwjgl.opengl.GL33C.GL_FLOAT,p);
            if(p[0]+p[1]+p[2]<.05f)throw new AssertionError("World source empty");
            org.lwjgl.opengl.GL33C.glBindFramebuffer(org.lwjgl.opengl.GL33C.GL_READ_FRAMEBUFFER,Minecraft.getInstance().getMainRenderTarget().frameBufferId);
            org.lwjgl.opengl.GL33C.glReadPixels(world.width/3,world.height/2,1,1,org.lwjgl.opengl.GL33C.GL_RGBA,org.lwjgl.opengl.GL33C.GL_FLOAT,p);
            if(p[3]>.01f)throw new AssertionError("HUD empty space is not transparent: "+p[3]);
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_PARALLEL: real world snapshot populated; HUD background transparent before composition");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private void next(){
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        if(++phase==4){
            LogUtils.getLogger().info("SLOPIUM_PARALLEL: all blocked-render-thread phases passed; starting lifecycle checks");
            c.frameGenerationMode=mode;c.frameGenerationParallel=true;
            lifecycleAt=System.nanoTime()+1_000_000_000L;return;
        }
        if(phase==0)SmokeFrameGenerationChecks.screenSpace();
        c.frameGenerationMode=phase==0||phase==3&&!amortize?FrameGenerationMode.OFF:mode;
        c.frameGenerationAmortize=amortize;
        c.frameGenerationParallel=phase==1||phase==2||amortize&&phase==3;c.frameGenerationFastPath=false;
        c.frameGenerationInputFps=amortize?60:0;c.frameGenerationMultiplier=amortize?2:phase==2?32:6;c.frameGenerationTargetFps=amortize?0:phase==2?240:0;
        c.frameGenerationCounter=true;
        settled=false;began=System.nanoTime();frames=progressDuringStalls=0;startOutput=AsyncPresenter.outputs();startGenerated=AsyncPresenter.generated();
        LogUtils.getLogger().info("SLOPIUM_PARALLEL: starting phase {}",phase);
    }
    private void lifecycle(RenderFrameEvent.Post event){
        if(phase!=4||System.nanoTime()<lifecycleAt)return;
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        switch(lifecycle++) {
            case 0 -> {requireActive();mc.setScreen(new net.minecraft.client.gui.screens.PauseScreen(false));}
            case 1 -> {requireStopped();mc.setScreen(null);}
            case 2 -> {requireActive();org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),960,540);}
            case 3 -> {
                requireActive();if(mc.getWindow().getWidth()!=960||mc.getWindow().getHeight()!=540)throw new AssertionError("Resize not applied");
                mc.options.enableVsync().set(true);
            }
            case 4 -> {requireActive();mc.options.enableVsync().set(false);c.enabled=false;}
            case 5 -> {requireStopped();c.enabled=true;}
            case 6 -> {requireActive();c.frameGenerationMode=mode==FrameGenerationMode.FSR?FrameGenerationMode.SIMPLE:FrameGenerationMode.FSR;}
            case 7 -> {requireActive();c.frameGenerationParallel=false;}
            case 8 -> {
                requireStopped();if(org.lwjgl.opengl.GL33C.glGetError()!=0)throw new AssertionError("GL error after drawable restoration");
                LogUtils.getLogger().info("SLOPIUM_PARALLEL: menu, restart, resize, VSync, Simple/FSR mode switch and global disable passed; no surviving presenter thread");
                mc.stop();phase=5;
            }
        }
        lifecycleAt=System.nanoTime()+1_000_000_000L;
    }
    private static void requireActive(){if(!AsyncPresenter.active()||AsyncPresenter.outputs()<2)throw new AssertionError("Presenter not running: "+AsyncPresenter.status());}
    private static void requireStopped(){
        if(AsyncPresenter.active()||Thread.getAllStackTraces().keySet().stream().anyMatch(t->t.isAlive()&&t.getName().equals("Slopium presenter")))
            throw new AssertionError("Presenter did not release window/thread: "+AsyncPresenter.status());
    }
}

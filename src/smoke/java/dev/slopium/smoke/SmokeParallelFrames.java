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
    private long recoveryGenerated;
    private boolean settled;
    private int phase=-1,frames,progressDuringStalls;
    private long began,startOutput,startGenerated;
    private int lifecycle;
    private long lifecycleAt;
    SmokeParallelFrames(){NeoForge.EVENT_BUS.addListener(this::load);NeoForge.EVENT_BUS.addListener(this::lifecycle);}
    private void load(RenderFrameEvent.Pre event){
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
            net.minecraft.client.Screenshot.grab(mc.gameDirectory,"beta8-parallel-"+phase+".png",mc.getMainRenderTarget(),m->{});
        }
        if(amortize&&phase==3){
            requireActive();
            if(frames/seconds<65||AsyncPresenter.generated()-recoveryGenerated>2)throw new AssertionError("Synthesis did not stop after source recovery: "+AsyncPresenter.status());
        }
        if(!amortize&&phase==3&&AsyncPresenter.active())throw new AssertionError("Presenter did not return window ownership");
        next();
    }
    private void next(){
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        if(++phase==4){
            LogUtils.getLogger().info("SLOPIUM_PARALLEL: all blocked-render-thread phases passed; starting lifecycle checks");
            c.frameGenerationMode=FrameGenerationMode.SIMPLE;c.frameGenerationParallel=true;
            lifecycleAt=System.nanoTime()+1_000_000_000L;return;
        }
        if(phase==0)SmokeFrameGenerationChecks.screenSpace();
        c.frameGenerationMode=phase==0||phase==3&&!amortize?FrameGenerationMode.OFF:FrameGenerationMode.SIMPLE;
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
            case 6 -> {requireActive();c.frameGenerationParallel=false;}
            case 7 -> {
                requireStopped();if(org.lwjgl.opengl.GL33C.glGetError()!=0)throw new AssertionError("GL error after drawable restoration");
                LogUtils.getLogger().info("SLOPIUM_PARALLEL: menu, restart, resize, VSync and global disable passed; no surviving presenter thread");
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

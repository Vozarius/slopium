package dev.slopium.smoke;

import com.mojang.logging.LogUtils;
import dev.slopium.client.SlopiumConfig;
import dev.slopium.framegen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;

/** Actual integrated-server world + normal GameRenderer/swap loop; no simulated world draw. */
final class SmokeWorldFrames {
    private boolean started;
    private int warmup,phase=-1,frames,phaseTicks;
    private long began;
    private volatile boolean sceneReady;
    private final FrameGenerationMode[] modes={FrameGenerationMode.OFF,FrameGenerationMode.SIMPLE,FrameGenerationMode.SIMPLE,FrameGenerationMode.FSR,FrameGenerationMode.FSR};
    private final int[] factors={2,2,4,2,4};
    private final int[] inputs={0,30,120,30,120};
    SmokeWorldFrames(){NeoForge.EVENT_BUS.addListener(this::tick);NeoForge.EVENT_BUS.addListener(this::frame);}
    private void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null) {
            started=true;mc.options.pauseOnLostFocus=false;mc.options.enableVsync().set(false);mc.options.framerateLimit().set(260);
            mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.bobView().set(false);
            mc.options.graphicsMode().set(net.minecraft.client.GraphicsStatus.FANCY);
            var c=SlopiumConfig.get();c.frameGenerationMode=FrameGenerationMode.OFF;
            mc.createWorldOpenFlows().createFreshLevel("framegen-beta2-"+System.currentTimeMillis(),
                new LevelSettings("Slopium frame generation test",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                new WorldOptions(42,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(),mc.screen);
        }
        if(mc.level!=null&&mc.player!=null) {
            mc.setWindowActive(true);
            // Continuous slow rotation exercises changing camera matrices on generated frames.
            mc.player.setYRot((float)(6*Math.sin(++phaseTicks*Math.PI/80)));mc.player.setXRot(0);
            if(warmup++==100) {
                var server=mc.getSingleplayerServer();
                server.execute(()->{
                    var level=server.overworld();
                    var player=server.getPlayerList().getPlayers().getFirst();
                    // Worldgen mods may replace FLAT. Build a known scene above terrain rather
                    // than assuming vanilla's -60 surface (which can be lava in a modded world).
                    for(int x=-14;x<=14;x++)for(int z=-4;z<=16;z++) {
                        level.setBlock(new net.minecraft.core.BlockPos(x,300,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
                        for(int y=301;y<=313;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
                    }
                    for(int x=-12;x<=12;x++)for(int y=301;y<313;y++)
                        level.setBlock(new net.minecraft.core.BlockPos(x,y,14),(x%2==0?net.minecraft.world.level.block.Blocks.BRICKS:net.minecraft.world.level.block.Blocks.GOLD_BLOCK).defaultBlockState(),3);
                    player.clearFire();player.teleportTo(0,301,0);sceneReady=true;
                });
            }
        }
    }
    private void next() {
        var mc=Minecraft.getInstance();
        if(++phase==modes.length){LogUtils.getLogger().info("SLOPIUM_WORLD: all actual-world generation phases passed");mc.stop();return;}
        FrameGeneration.release();FrameGeneration.resetCounters();frames=phaseTicks=0;began=System.nanoTime();
        var c=SlopiumConfig.get();c.enabled=true;c.diagnostics=true;c.frameGenerationMode=modes[phase];c.frameGenerationMultiplier=factors[phase];
        c.frameGenerationTargetFps=0;c.frameGenerationCounter=true;c.frameGenerationMarkFrames=true;
        c.frameGenerationInputFps=Boolean.getBoolean("slopium.inputSmoke")?inputs[phase]:0;
        LogUtils.getLogger().info("SLOPIUM_WORLD: starting {} x{}",modes[phase],factors[phase]);
    }
    private void frame(RenderFrameEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(!started||mc.level==null||mc.player==null||mc.screen!=null||warmup<200||!sceneReady||mc.player.getY()<290||
            !mc.level.getBlockState(new net.minecraft.core.BlockPos(0,301,14)).is(net.minecraft.world.level.block.Blocks.BRICKS))return;
        if(phase<0){next();return;}
        frames++;
        if(System.nanoTime()-began<8_000_000_000L)return;
        if(modes[phase]!=FrameGenerationMode.OFF&&(FrameGeneration.generatedFrames<30||FrameGeneration.realFrames<5))throw new AssertionError("No actual-world generation: "+FrameGeneration.status());
        if(Boolean.getBoolean("slopium.inputSmoke")&&inputs[phase]>0) {
            double seconds=(System.nanoTime()-began)/1e9;
            if(FrameGeneration.realFrames>Math.ceil(seconds*inputs[phase])+2)throw new AssertionError("World input cap exceeded: "+FrameGeneration.status());
            if(frames>Math.ceil(seconds*inputs[phase]*factors[phase])+8)throw new AssertionError("Output pacing missing above vanilla sentinel: "+frames+" in "+seconds);
        }
        LogUtils.getLogger().info("SLOPIUM_WORLD: {} x{} actual loop {} FPS; {}",modes[phase],factors[phase],frames/((System.nanoTime()-began)/1e9),FrameGeneration.status());
        Screenshot.grab(mc.gameDirectory,(Boolean.getBoolean("slopium.inputSmoke")?"beta3-input-":"beta2-world-")+phase+".png",mc.getMainRenderTarget(),m->{});
        next();
    }
}

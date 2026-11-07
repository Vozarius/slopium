package dev.slopium.smoke;

import dev.slopium.client.*;
import dev.slopium.visibility.*;
import dev.slopium.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

/** Actual Sodium draw-list and Flywheel integration, not just a ray/proof fixture. */
final class SmokeSectionWorld {
    private dev.slopium.framegen.GpuTimer frameTimer;
    private final java.util.ArrayList<Double> frameMs=new java.util.ArrayList<>();
    private long lastFrame,measureStart;private int benchPhase=-1;private volatile boolean benchReady;
    SmokeSectionWorld(){net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event)->{if(benchPhase>=0&&measureStart>0){if(frameTimer==null)frameTimer=new dev.slopium.framegen.GpuTimer();frameTimer.start();}});}
    private long fence;private int snapshot, snapshotId, samples,snapshotBytes;private boolean snapshotExpected;
    private int phase=-1;private long started;private volatile boolean ready;
    Float fixedYaw(){
        if(phase==18||phase==19)return -45f;
        if(phase>=20)return 0f;
        if(phase==14)return 180f;
        if(phase==16){var mc=Minecraft.getInstance();return (float)Math.toDegrees(Math.atan(Math.tan(Math.toRadians(mc.options.fov().get())*.5)*mc.getWindow().getWidth()/mc.getWindow().getHeight()))-3f;}
        return phase>=15?0f:null;
    }
    static void scene(net.minecraft.server.level.ServerLevel level){
        // Worldgen mods can put terrain at Y=300. Leave an actual corridor and an empty
        // target section so native Sodium occlusion does not prevent its first mesh build.
        for(int x=0;x<16;x++)for(int y=288;y<320;y++)for(int z=16;z<48;z++)
            level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
        for(int x=-12;x<=12;x++)for(int y=301;y<313;y++)level.setBlock(new BlockPos(x,y,14),Blocks.AIR.defaultBlockState(),3);
        for(int x=-8;x<=8;x++)for(int y=286;y<=314;y++)level.setBlock(new BlockPos(x,y,4),Blocks.GLASS.defaultBlockState(),3);
        level.setBlock(new BlockPos(4,301,40),Blocks.GOLD_BLOCK.defaultBlockState(),3);
        level.setBlock(new BlockPos(6,301,40),com.simibubi.create.AllBlocks.SHAFT.getDefaultState(),3);
    }
    void frame(){
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        if(phase<0){next();return;}
        if(phase>=22){benchmark();return;}
        if(!ready||System.nanoTime()-started<3_000_000_000L)return;
        if(org.lwjgl.opengl.GL11C.glGetError()!=0)throw new AssertionError("GPU GL error: "+SectionVisibility.status());
        var manager=((SodiumSectionAccess)SodiumWorldRenderer.instance()).slopium$sections();
        var target=((SectionManagerAccess)manager).slopium$section(phase==18||phase==19?2:0,18,2);
        if(target==null||!target.isBuilt()){if(System.nanoTime()-started<60_000_000_000L)return;throw new AssertionError("Target section not built: section="+target+", client block="+mc.level.getBlockState(new BlockPos(4,301,40))+", camera="+mc.gameRenderer.getMainCamera().getPosition());}
        boolean expected=phase==0||phase==2||phase==5||phase==7||phase==9||phase==11||phase==14||phase==18||phase==21;
        if(phase!=4){
            if(!GpuSections.current())throw new AssertionError("GPU pass unavailable: "+SectionVisibility.status());
            int id=GpuSections.candidateId(phase==18||phase==19?2:0,18,2);
            if(id<0){if(phase!=14)throw new AssertionError("Target missing from candidate list, phase="+phase);}
            else if(!checkGpu(id,expected))return;
        }
        com.mojang.logging.LogUtils.getLogger().info("GPU sections phase {} expected hidden={} samples={} {}",phase,expected,samples,SectionVisibility.status());
        if(phase==7||phase==8||phase>=18)net.minecraft.client.Screenshot.grab(mc.gameDirectory,"beta14-section-"+phase+".png",mc.getMainRenderTarget(),message->{});
        samples=0;
        if(++phase==22){com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SECTIONS_WORLD: 22 phases passed, including Create windows, opening, outside frustum, conservative partial visibility and camera restoration");startBenchmark();return;}
        configure(c);started=System.nanoTime();
    }
    private void startBenchmark(){
        var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{
            var world=mc.getSingleplayerServer().overworld();
            world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,mc.getSingleplayerServer());
            world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false,mc.getSingleplayerServer());
            world.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING).set(0,mc.getSingleplayerServer());
            world.setDayTime(6000);world.setWeatherParameters(1000000,0,false,false);
            for(var item:world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-64,280,-64,96,320,96)))item.discard();
            for(int x=-8;x<=8;x++)for(int y=286;y<=314;y++)world.setBlock(new BlockPos(x,y,4),Blocks.GLASS.defaultBlockState(),2);
            int count=0;
            for(int x=-24;x<24;x+=2)for(int y=288;y<320;y+=2)for(int z=32;z<80;z+=2){
                world.setBlock(new BlockPos(x,y,z),com.simibubi.create.AllBlocks.SHAFT.getDefaultState(),2);count++;
            }
            // Shaft-only sections have no terrain mesh. Include ordinary machine housings,
            // as in a factory, so each benchmark section is a real Sodium candidate.
            for(int sx=-2;sx<=1;sx++)for(int sy=18;sy<=19;sy++)for(int sz=2;sz<=4;sz++)
                world.setBlock(new BlockPos(sx*16+1,sy*16+1,sz*16+1),Blocks.ANDESITE.defaultBlockState(),3);
            com.mojang.logging.LogUtils.getLogger().info("SECTION_BENCH placed {} Create shafts + 24 terrain anchors",count);benchReady=true;
        });started=System.nanoTime();
    }
    private void benchmark(){
        if(!benchReady)return;long now=System.nanoTime();
        if(benchPhase<0){if(now-started<40_000_000_000L)return;benchNext();return;}
        if(now-started<4_000_000_000L)return;
        if(measureStart==0){measureStart=lastFrame=now;GpuSections.resetCounters();return;}
        if(frameTimer!=null)frameTimer.stop();frameMs.add((now-lastFrame)/1e6);lastFrame=now;
        if(now-measureStart<15_000_000_000L)return;
        var values=frameMs.stream().mapToDouble(Double::doubleValue).sorted().toArray();double average=java.util.Arrays.stream(values).average().orElse(0);
        com.mojang.logging.LogUtils.getLogger().info("SECTION_AB phase={} enabled={} n={} mean={} p50={} p95={} fps={} GPU-frame={} {}",benchPhase,SlopiumConfig.get().visibleSections,values.length,average,values[values.length/2],values[(int)(values.length*.95)],1000/average,frameTimer==null?0:frameTimer.milliseconds(),GpuSections.status());
        net.minecraft.client.Screenshot.grab(Minecraft.getInstance().gameDirectory,"beta14-ab-"+benchPhase+".png",Minecraft.getInstance().getMainRenderTarget(),message->{});
        if(benchPhase==3){Minecraft.getInstance().stop();return;}benchNext();
    }
    private void benchNext(){
        benchPhase++;SlopiumConfig.get().visibleSections=benchPhase%2==1;SlopiumConfig.get().sectionsAnyBlock=true;
        SlopiumConfig.get().cameraCulling=true;GpuSections.invalidate();measureStart=0;frameMs.clear();if(frameTimer!=null){frameTimer.nanos=frameTimer.samples=0;}
        started=System.nanoTime();
    }
    private boolean checkGpu(int id,boolean hidden){
        // Test-only asynchronous snapshot. Never participates in production draw decisions.
        if(fence!=0){int status=org.lwjgl.opengl.GL32C.glClientWaitSync(fence,0,0);if(status==org.lwjgl.opengl.GL32C.GL_TIMEOUT_EXPIRED)return false;
            org.lwjgl.opengl.GL32C.glDeleteSync(fence);fence=0;
            int old=org.lwjgl.opengl.GL15C.glGetInteger(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER);
            org.lwjgl.opengl.GL15C.glBindBuffer(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER,snapshot);int[] result=new int[snapshotBytes/4];org.lwjgl.opengl.GL15C.glGetBufferSubData(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER,0,result);org.lwjgl.opengl.GL15C.glBindBuffer(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER,old);
            if((result[0]==0)!=snapshotExpected){dumpDepth();throw new AssertionError("GPU section phase="+phase+" expected hidden="+snapshotExpected+" bit="+result[0]+" "+SectionVisibility.status());}
            int matching=0;
            for(int i=1;i+5<result.length;i+=6)if(result[i+5]==snapshotId){matching++;if(result[i+1]!=(snapshotExpected?0:1))throw new AssertionError("Indirect command did not consume mask, phase="+phase);}
            if(matching==0)throw new AssertionError("No indirect commands mapped to target section, phase="+phase);
            samples++;if(samples>=60)return true;
        }
        if(snapshot==0)snapshot=org.lwjgl.opengl.GL15C.glGenBuffers();
        int read=org.lwjgl.opengl.GL11C.glGetInteger(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER),write=org.lwjgl.opengl.GL11C.glGetInteger(org.lwjgl.opengl.GL31C.GL_COPY_WRITE_BUFFER);
        org.lwjgl.opengl.GL15C.glBindBuffer(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER,GpuSections.maskBuffer());org.lwjgl.opengl.GL15C.glBindBuffer(org.lwjgl.opengl.GL31C.GL_COPY_WRITE_BUFFER,snapshot);java.util.ArrayList<int[]> sources=new java.util.ArrayList<>();snapshotBytes=4;
        try{
            var field=GpuSections.class.getDeclaredField("batches");field.setAccessible(true);var batches=(java.util.Map<?,?>)field.get(null);
            var manager=((SodiumSectionAccess)SodiumWorldRenderer.instance()).slopium$sections();var section=((SectionManagerAccess)manager).slopium$section(phase==18||phase==19?2:0,18,2);
            for(var pass:net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses.ALL){var cached=batches.get(section.getRegion().getCachedBatch(pass));if(cached==null)continue;var buffer=cached.getClass().getDeclaredMethod("buffer");var size=cached.getClass().getDeclaredMethod("size");buffer.setAccessible(true);size.setAccessible(true);int n=(int)size.invoke(cached)*24;sources.add(new int[]{(int)buffer.invoke(cached),n});snapshotBytes+=n;}
        }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
        org.lwjgl.opengl.GL15C.glBufferData(org.lwjgl.opengl.GL31C.GL_COPY_WRITE_BUFFER,snapshotBytes,org.lwjgl.opengl.GL15C.GL_STREAM_READ);
        org.lwjgl.opengl.GL31C.glCopyBufferSubData(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER,org.lwjgl.opengl.GL31C.GL_COPY_WRITE_BUFFER,(long)id*4,0,4);
        int offset=4;for(var source:sources){org.lwjgl.opengl.GL15C.glBindBuffer(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER,source[0]);org.lwjgl.opengl.GL31C.glCopyBufferSubData(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER,org.lwjgl.opengl.GL31C.GL_COPY_WRITE_BUFFER,0,offset,source[1]);offset+=source[1];}
        fence=org.lwjgl.opengl.GL32C.glFenceSync(org.lwjgl.opengl.GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE,0);snapshotExpected=hidden;snapshotId=id;
        org.lwjgl.opengl.GL15C.glBindBuffer(org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER,read);org.lwjgl.opengl.GL15C.glBindBuffer(org.lwjgl.opengl.GL31C.GL_COPY_WRITE_BUFFER,write);return false;
    }
    private void dumpDepth(){try{
        var f=GpuSections.class.getDeclaredField("depth");f.setAccessible(true);int depth=f.getInt(null);
        int tex=org.lwjgl.opengl.GL11C.glGetInteger(org.lwjgl.opengl.GL11C.GL_TEXTURE_BINDING_2D);org.lwjgl.opengl.GL11C.glBindTexture(org.lwjgl.opengl.GL11C.GL_TEXTURE_2D,depth);
        int w=org.lwjgl.opengl.GL11C.glGetTexLevelParameteri(org.lwjgl.opengl.GL11C.GL_TEXTURE_2D,0,org.lwjgl.opengl.GL11C.GL_TEXTURE_WIDTH),h=org.lwjgl.opengl.GL11C.glGetTexLevelParameteri(org.lwjgl.opengl.GL11C.GL_TEXTURE_2D,0,org.lwjgl.opengl.GL11C.GL_TEXTURE_HEIGHT);
        float[] data=new float[w*h];org.lwjgl.opengl.GL11C.glGetTexImage(org.lwjgl.opengl.GL11C.GL_TEXTURE_2D,0,org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT,org.lwjgl.opengl.GL11C.GL_FLOAT,data);org.lwjgl.opengl.GL11C.glBindTexture(org.lwjgl.opengl.GL11C.GL_TEXTURE_2D,tex);
        double min=1,max=0;int empty=0;for(float v:data){min=Math.min(min,v);max=Math.max(max,v);if(v==1)empty++;}
        com.mojang.logging.LogUtils.getLogger().error("DEPTH {}x{} min={} max={} center={} empty={}",w,h,min,max,data[h/2*w+w/2],empty);
        var img=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int y=0;y<h;y++)for(int x=0;x<w;x++){int v=(int)((1-data[y*w+x])*10000)%256;img.setRGB(x,h-y-1,(v<<16)|(v<<8)|v);}javax.imageio.ImageIO.write(img,"png",new java.io.File("section-depth.png"));
    }catch(Exception ex){throw new AssertionError(ex);}}
    private void next(){phase=0;configure(SlopiumConfig.get());started=System.nanoTime();}
    private void configure(SlopiumConfig c){
        SlopiumStats.reset();c.diagnostics=true;
        c.cameraCulling=true;c.gpuGlassCulling=true;c.enabled=true;c.visibleSections=phase!=4;c.minimumSections=phase==3?2:0;c.sectionsAnyBlock=phase==0||phase>=7&&phase!=8&&phase!=12;
        c.occludeFlywheel=c.freezeCreateAnimations=c.occludeCreateAnimations=c.disableFlywheelRendering=c.hideAllCreateModels=false;
        ready=phase!=2&&phase!=6&&phase!=7&&phase!=10&&phase!=11&&phase!=13&&phase<18;
        if(!ready){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().overworld();
            if(phase==18){
                for(int x=-16;x<=16;x++)for(int y=280;y<=319;y++)for(int z=3;z<=5;z++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
                for(int a=-32;a<=4;a++)for(int y=280;y<=319;y++){
                    level.setBlock(new BlockPos(4,y,a),Blocks.GLASS.defaultBlockState(),2);
                    level.setBlock(new BlockPos(a,y,4),Blocks.GLASS.defaultBlockState(),2);
                }
                level.setBlock(new BlockPos(40,301,40),Blocks.GOLD_BLOCK.defaultBlockState(),3);
                level.setBlock(new BlockPos(42,301,40),com.simibubi.create.AllBlocks.SHAFT.getDefaultState(),3);
            }
            else if(phase==19){for(int x=3;x<=5;x++)for(int z=3;z<=5;z++)for(int y=301;y<=304;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);}
            else if(phase==20||phase==21){
                for(int a=-32;a<=4;a++)for(int y=280;y<=319;y++)level.setBlock(new BlockPos(4,y,a),Blocks.AIR.defaultBlockState(),2);
                for(int x=-16;x<=16;x++)for(int y=280;y<=319;y++)level.setBlock(new BlockPos(x,y,4),(phase==20?Blocks.OAK_LEAVES:Blocks.STONE).defaultBlockState(),3);
            }
            else if(phase==10){level.setBlock(new BlockPos(24,300,0),Blocks.STONE.defaultBlockState(),3);var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();player.teleportTo(24.5,301,.5);}
            else if(phase==11){
                var pane=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:weathered_iron_window_pane")).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.IronBarsBlock.WEST,true).setValue(net.minecraft.world.level.block.IronBarsBlock.EAST,true);
                for(int x=-8;x<=8;x++)for(int y=286;y<=314;y++){
                    level.setBlock(new BlockPos(x,y,4),x<=0?pane:Blocks.AIR.defaultBlockState(),2);
                    level.setBlock(new BlockPos(x,y,5),x>=0?pane:Blocks.AIR.defaultBlockState(),2);
                }
                mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().teleportTo(0,301,0);
            }
            else if(phase==13)level.setBlock(new BlockPos(1,302,5),Blocks.AIR.defaultBlockState(),3);
            else for(int x=-8;x<=8;x++)for(int y=286;y<=314;y++)level.setBlock(new BlockPos(x,y,4),(phase==6?Blocks.AIR:phase==7?Blocks.GLASS:Blocks.STONE).defaultBlockState(),3);
            ready=true;
        });}
    }
}

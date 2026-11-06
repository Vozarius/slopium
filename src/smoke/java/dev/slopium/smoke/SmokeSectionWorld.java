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
    private int phase=-1;private long started;private volatile boolean ready;
    Float fixedYaw(){
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
        if(!ready||System.nanoTime()-started<3_000_000_000L)return;
        var manager=((SodiumSectionAccess)SodiumWorldRenderer.instance()).slopium$sections();
        var target=((SectionManagerAccess)manager).slopium$section(0,18,2);
        if(target==null||!target.isBuilt()){if(System.nanoTime()-started<60_000_000_000L)return;throw new AssertionError("Target section not built: section="+target+", client block="+mc.level.getBlockState(new BlockPos(4,301,40))+", camera="+mc.gameRenderer.getMainCamera().getPosition());}
        boolean expected=phase==0||phase==2||phase==5||phase==7||phase==9||phase==11||phase==14||phase==16;
        boolean hidden=SectionVisibility.request(0,18,2);
        var region=(SectionRegionState)target.getRegion();
        boolean masked=region.slopium$hiddenSection(target.getSectionIndex());
        if(hidden!=expected||phase!=14&&masked!=expected){if(System.nanoTime()-started<15_000_000_000L)return;diagnose();throw new AssertionError("Section mode "+phase+" expected="+expected+" pool="+hidden+" mask="+masked+" "+SectionVisibility.status());}
        if(phase<10&&SectionVisibility.request(0,18,0))throw new AssertionError("Camera section culled");
        var vm=dev.engine_room.flywheel.api.visualization.VisualizationManager.get(mc.level);
        var storage=(dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage)((dev.engine_room.flywheel.impl.visualization.VisualManagerImpl<?,?>)vm.blockEntities()).getStorage();
        var visual=storage.visualAtPos(new BlockPos(6,301,40).asLong());var owner=visual==null?null:OwnedInstances.get(visual);
        if(owner==null||owner.sectionHidden!=expected){if(System.nanoTime()-started<15_000_000_000L)return;throw new AssertionError("Flywheel did not consume section result, phase="+phase+" visual="+visual);}
        if(phase==0&&SectionVisibility.hiddenDraws==0)throw new AssertionError("Terrain draw commands not filtered");
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SECTIONS_WORLD: phase {} hidden={} terrain mask={} Flywheel={} {}",phase,hidden,masked,owner.sectionHidden,SectionVisibility.status());
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SECTIONS_COST: shared average={} ms, passes={}, section frames={}",SlopiumStats.visibilityNanos/1e6/Math.max(1,SlopiumStats.visibilityPasses),SlopiumStats.visibilityPasses,SectionVisibility.frames);
        if(++phase==18){com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SECTIONS_WORLD: 18 phases passed, including Create windows, opening, outside frustum, partially visible section and camera restoration");mc.stop();return;}
        configure(c);started=System.nanoTime();
    }
    private void next(){phase=0;configure(SlopiumConfig.get());started=System.nanoTime();}
    private void diagnose(){
        try{
            var mc=Minecraft.getInstance();var eye=mc.gameRenderer.getMainCamera().getPosition();var center=new net.minecraft.world.phys.Vec3(8,296,40);
            var method=VisibilityPool.class.getDeclaredMethod("sectionWitness",net.minecraft.world.phys.Vec3.class,boolean.class);method.setAccessible(true);
            var witness=(BlockPos)method.invoke(null,center,SlopiumConfig.get().sectionsAnyBlock);
            var bounds=new net.minecraft.world.phys.AABB(0,288,32,16,304,48).inflate(.001);
            var log=com.mojang.logging.LogUtils.getLogger();
            log.error("SLOPIUM_SECTION_DIAG eye={} witness={} state={} geometric={} ready proof={}",eye,witness,witness==null?null:mc.level.getBlockState(witness),SectionOcclusion.hidden(mc.level,bounds,eye,witness,SlopiumConfig.get().sectionsAnyBlock,()->true),SectionOcclusion.hidden(mc.level,bounds,eye,witness,SlopiumConfig.get().sectionsAnyBlock,()->true,SectionMeshes::ready));
            for(int x=-1;x<=1;x++)for(int y=300;y<=303;y++)log.error("SLOPIUM_SECTION_DIAG wall {} {} state={} ready={}",x,y,mc.level.getBlockState(new BlockPos(x,y,4)),SectionMeshes.ready(new BlockPos(x,y,4)));
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private void configure(SlopiumConfig c){
        SlopiumStats.reset();c.diagnostics=true;
        c.enabled=true;c.visibleSections=phase!=4;c.minimumSections=phase==3?2:0;c.sectionsAnyBlock=phase==0||phase>=7&&phase!=8&&phase!=12;
        c.occludeFlywheel=c.freezeCreateAnimations=c.occludeCreateAnimations=c.disableFlywheelRendering=c.hideAllCreateModels=false;
        ready=phase!=2&&phase!=6&&phase!=7&&phase!=10&&phase!=11&&phase!=13;
        if(!ready){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().overworld();
            if(phase==10){level.setBlock(new BlockPos(24,300,0),Blocks.STONE.defaultBlockState(),3);var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();player.teleportTo(24.5,301,.5);}
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

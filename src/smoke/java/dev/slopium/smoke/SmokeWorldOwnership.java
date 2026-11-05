package dev.slopium.smoke;

import dev.slopium.client.SlopiumConfig;
import dev.slopium.visibility.*;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.visualization.VisualManagerImpl;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Real registered Create visualizers, including Azimuth wrappers in the full profile. */
final class SmokeWorldOwnership {
    private static final String[] IDS={"create:shaft","create:mechanical_press","create:chain_conveyor"};
    private static BlockPos pos(int i){return new BlockPos(-4+i*4,302,7);}
    private int phase=-1,frames;
    private long waitingSince;
    private boolean enabled,hidden,freeze;
    private BlockSelectionMode mode;
    private java.util.List<String> ids;
    static void place(net.minecraft.server.level.ServerLevel level){
        for(int i=0;i<IDS.length;i++)level.setBlock(pos(i),net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(IDS[i])).defaultBlockState(),3);
        var end=pos(2).offset(0,0,4);
        level.setBlock(end,level.getBlockState(pos(2)),3);
        var first=(com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity)level.getBlockEntity(pos(2));
        var second=(com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity)level.getBlockEntity(end);
        first.prepareStats();second.prepareStats();
        first.addConnectionTo(end);second.addConnectionTo(pos(2));
    }
    boolean frame(){
        if(phase>=4)return true;
        var mc=Minecraft.getInstance();var c=SlopiumConfig.get();
        var manager=VisualizationManager.get(mc.level);
        if(manager==null)throw new AssertionError("Actual world Flywheel backend unavailable");
        var storage=(BlockEntityStorage)((VisualManagerImpl<?,?>)manager.blockEntities()).getStorage();
        if(phase<0){
            if(waitingSince==0)waitingSince=System.nanoTime();
            for(int i=0;i<IDS.length;i++)if(storage.visualAtPos(pos(i).asLong())==null){
                var be=mc.level.getBlockEntity(pos(i));
                if(++frames==60)com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_OWNERS: waiting for {} state={} BE={} storage={} accepted={}",IDS[i],mc.level.getBlockState(pos(i)),be,storage.getAllVisuals().size(),be!=null&&storage.willAccept(be));
                if(System.nanoTime()-waitingSince>30_000_000_000L)throw new AssertionError("Missing actual visual "+IDS[i]+" state="+mc.level.getBlockState(pos(i))+" BE="+be+" visuals="+storage.getAllVisuals().size()+" accepted="+(be!=null&&storage.willAccept(be)));
                return false;
            }
            enabled=c.enabled;hidden=c.disableFlywheelRendering;freeze=c.freezeCreateAnimations;mode=c.processedBlockMode;ids=c.processedBlockIds;
            c.enabled=true;c.disableFlywheelRendering=true;c.freezeCreateAnimations=true;
            next(c);return false;
        }
        if(++frames<35)return false;
        for(int i=0;i<IDS.length;i++){
            var visual=storage.visualAtPos(pos(i).asLong());var owner=OwnedInstances.get(visual);
            if(owner==null)throw new AssertionError("Real visual was not created through owned context: "+IDS[i]);
            boolean expected=phase==0||phase==1&&i==0||phase==2&&i!=0;
            int[] count={0,0};owner.forEach(instance->{count[0]++;if(instance.handle() instanceof InstanceOwnership tag&&tag.slopium$hidden())count[1]++;});
            if(count[0]==0||expected!=(count[1]>0)||expected!=owner.slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("Real selection/animation mismatch phase="+phase+" "+IDS[i]+" instances="+java.util.Arrays.toString(count));
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_OWNERS: phase {} {} instances={} hidden={} visual={}",phase,IDS[i],count[0],count[1],visual.getClass().getName());
        }
        next(c);return phase>=4;
    }
    private void next(SlopiumConfig c){
        frames=0;phase++;
        c.processedBlockIds=java.util.List.of(IDS[0]);
        c.processedBlockMode=phase==1?BlockSelectionMode.WHITELIST:phase==2?BlockSelectionMode.BLACKLIST:BlockSelectionMode.ALL;
        if(phase==3)c.enabled=false;
        if(phase==4){c.enabled=enabled;c.disableFlywheelRendering=hidden;c.freezeCreateAnimations=freeze;c.processedBlockMode=mode;c.processedBlockIds=ids;
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_OWNERS: actual registered shaft/press/conveyor creation, all/whitelist/blacklist and global restore passed");}
        ProcessedBlocks.rebuild();
    }
}

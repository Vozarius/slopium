package dev.slopium.smoke;

import dev.engine_room.flywheel.api.visual.*;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.storage.Storage;
import dev.slopium.client.*;
import dev.slopium.visibility.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.lang.reflect.Field;

final class SmokeCullingQueueChecks {
    static final class Plain implements BlockEntityVisual<BlockEntity> {
        boolean deleted;
        public void update(float partial) {}
        public void delete() { deleted=true; }
        public void collectCrumblingInstances(java.util.function.Consumer<dev.engine_room.flywheel.api.instance.Instance> consumer) {}
    }
    static final class TestStorage extends Storage<Object> {
        Plain latest;
        public boolean willAccept(Object object) { return true; }
        protected Visual createRaw(VisualizationContext context,Object object,float partial) { return latest=new Plain(); }
    }
    static void lifecycle() throws ReflectiveOperationException {
        var c=SlopiumConfig.get();boolean enabled=c.enabled,occlude=c.occludeFlywheel;
        c.enabled=c.occludeFlywheel=true;
        try {
        var storage=new TestStorage();var index=((CullingStorage)(Object)storage).slopium$cullingIndex();
        var object=new Object();
        storage.add(null,object,0);pump(index,8);
        if(index.tracked()!=1||index.supported()!=0)throw new AssertionError("Storage add hook missed unsupported visual");
        storage.add(null,object,0);pump(index,3);
        if(index.tracked()!=1)throw new AssertionError("Duplicate add queued twice");
        Plain old=storage.latest;
        storage.recreateAll(null,0);pump(index,8);
        if(!old.deleted||storage.latest==old||index.tracked()!=1)throw new AssertionError("Recreate lifecycle lost");
        storage.remove(object);pump(index,8);
        if(index.tracked()!=0||!storage.latest.deleted)throw new AssertionError("Remove lifecycle lost");
        storage.add(null,object,0);pump(index,8);storage.invalidate();pump(index,8);
        if(index.tracked()!=0)throw new AssertionError("Storage invalidate retained entries");
        c.enabled=false;
        storage.add(null,new Object(),0);pump(index,8);
        if(index.tracked()!=0)throw new AssertionError("Disabled master still indexes visuals");
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: real Storage add/duplicate/recreate/remove/invalidate lifecycle and disabled bypass passed");
        } finally {c.enabled=enabled;c.occludeFlywheel=occlude;}
    }
    static void pump(FlywheelCullingIndex index,int count) throws ReflectiveOperationException {
        var frame=Slopium.class.getDeclaredField("frame");frame.setAccessible(true);
        for(int i=0;i<count;i++){frame.setLong(null,frame.getLong(null)+1);VisibilityPool.process(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());index.frame();}
    }
    static void stress(BlockEntity be,Class<?> base,Class<?> type,Field beField,Field modelField,Field shaftField) throws ReflectiveOperationException {
        var c=SlopiumConfig.get();boolean enabled=c.enabled,fly=c.occludeFlywheel,freeze=c.freezeCreateAnimations,anim=c.occludeCreateAnimations;
        var index=new FlywheelCullingIndex();
        try {
            c.enabled=c.occludeFlywheel=true;c.freezeCreateAnimations=c.occludeCreateAnimations=false;
            for(int i=0;i<6000;i++) {
                Object visual=SmokeRadicalChecks.allocate(type);beField.set(visual,be);
                modelField.set(visual,new com.simibubi.create.content.kinetics.base.RotatingInstance(null,new SmokeFlywheelWallChecks.Handle(true)));
                shaftField.set(visual,new com.simibubi.create.content.kinetics.base.RotatingInstance(null,new SmokeFlywheelWallChecks.Handle(true)));
                index.add((Visual)visual);index.add(new Plain());
            }
            int maxChecks=0;
            for(int i=0;i<240;i++) {
                long before=SlopiumStats.flywheelVisualsChecked;pump(index,1);
                int checked=(int)(SlopiumStats.flywheelVisualsChecked-before);maxChecks=Math.max(maxChecks,checked);
                if(checked>512)throw new AssertionError("Full-scene scan escaped operation budget");
            }
            if(index.tracked()!=12000 || index.supported()!=6000)throw new AssertionError("Registration queue starved: "+index.tracked());
            long unsupported=SlopiumStats.flywheelVisualsUnsupported, count=SlopiumStats.flywheelVisualsChecked;
            long started=System.nanoTime();pump(index,120);long elapsed=System.nanoTime()-started;
            if(unsupported!=SlopiumStats.flywheelVisualsUnsupported)throw new AssertionError("Unsupported classes reclassified every frame");
            if(SlopiumStats.flywheelVisualsChecked<=count)throw new AssertionError("Scheduler stopped processing");
            // Force at least one real hide, then globally disable: pending restorations must drain.
            var level=(SmokeWallChecks.TestLevel)be.getLevel();var oldWall=level.wall;
            level.wall=net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();WallOcclusion.invalidate();
            pump(index,120);
            if(index.altered()==0)throw new AssertionError("Budgeted queue never hid a mechanism");
            int hiddenBefore=index.altered();
            if(hiddenBefore<=512)throw new AssertionError("Reveal stress fixture did not exceed the slow queue capacity");
            level.wall=net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();WallOcclusion.invalidate();
            var processed=VisibilityPool.class.getDeclaredField("processed");processed.setAccessible(true);
            processed.setLong(null,Slopium.frame()+1);
            long revealStarted=System.nanoTime(),rays=SlopiumStats.visibilityRays;
            pump(index,1);long revealNs=System.nanoTime()-revealStarted;
            if(index.altered()!=0||SlopiumStats.visibilityRays!=rays)throw new AssertionError("Bulk reveal waited for traces or the bounded hide queue");
            var result=java.util.Map.of("previouslyHidden",hiddenBefore,"revealedFrames",1,"revealCpuMs",revealNs/1e6,"newWorldRays",SlopiumStats.visibilityRays-rays,"steadyHideQueueCpuMs",elapsed/120.0/1e6);
            try{java.nio.file.Files.writeString(java.nio.file.Path.of("slopium-reveal-benchmark.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));}
            catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: BULK ONE-FRAME reveal: {} hidden visuals, {} ms, zero new world rays",hiddenBefore,revealNs/1e6);
            c.enabled=false;pump(index,120);
            if(index.altered()!=0||index.restoring())throw new AssertionError("Global off did not drain restoration queue");
            level.wall=oldWall;WallOcclusion.invalidate();
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: 12000 visuals, 6000 supported, max checks/frame={}, steady fixture avg={} ms, no repeated unsupported scan; bounded restore passed",maxChecks,elapsed/120.0/1_000_000);
        } finally {c.enabled=enabled;c.occludeFlywheel=fly;c.freezeCreateAnimations=freeze;c.occludeCreateAnimations=anim;}
    }
}

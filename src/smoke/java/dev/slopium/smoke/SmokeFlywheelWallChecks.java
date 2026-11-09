package dev.slopium.smoke;
import dev.engine_room.flywheel.api.instance.*;
import dev.slopium.visibility.InstanceVisibility;
import dev.slopium.visibility.VisualOcclusionState;

final class SmokeFlywheelWallChecks {
    private static org.joml.Matrix4f testView,testProjection;
    private static void deferProcessor()throws ReflectiveOperationException{
        var processed=dev.slopium.visibility.VisibilityPool.class.getDeclaredField("processed");processed.setAccessible(true);
        processed.setLong(null,dev.slopium.client.Slopium.frame()+1);
    }
    static final class Handle implements InstanceHandle {
        boolean visible; int changes; Handle(boolean value){visible=value;}
        public void setChanged(){changes++;} public void setDeleted(){} public void setVisible(boolean value){visible=value;} public boolean isVisible(){return visible;}
    }
    record Dummy(Handle handle) implements Instance { public InstanceType<?> type(){return null;} }
    static void run() throws ReflectiveOperationException {
        var shown=new Dummy(new Handle(true));var hidden=new Dummy(new Handle(false));
        var owner=new InstanceVisibility();owner.accept(shown);owner.accept(hidden);
        if(shown.handle.visible || hidden.handle.visible || owner.size()!=1)throw new AssertionError("Instance ownership failed");
        owner.restore();
        if(!shown.handle.visible || hidden.handle.visible || owner.size()!=0)throw new AssertionError("Instance restore failed");
        var manager=Class.forName("dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl");
        if(java.util.Arrays.stream(manager.getDeclaredMethods()).noneMatch(m->m.getName().endsWith("slopium$beforeFrame")))throw new AssertionError("Flywheel hook missing");
        if(dev.slopium.client.Slopium.loaded("create")) {
            SmokeGpuDepthChecks.run();
            rotationWriter();
            SmokeInstanceBackendChecks.run();
            SmokeCullingQueueChecks.lifecycle();
            var type=Class.forName("com.simibubi.create.content.kinetics.press.PressVisual");
            var state=(VisualOcclusionState)SmokeRadicalChecks.allocate(type);
            state.slopium$setOccluded(true,10);
            if(!state.slopium$isOccluded(10)||state.slopium$isOccluded(11))throw new AssertionError("Worker snapshot expiry failed");
            state.slopium$setOccluded(false,11);
            if(state.slopium$isOccluded(11))throw new AssertionError("Worker visible restore failed");
        }
    }
    static void world() throws ReflectiveOperationException {
        var mc=net.minecraft.client.Minecraft.getInstance(); var c=dev.slopium.client.SlopiumConfig.get();
        var oldPlayer=mc.player;var oldScreen=mc.screen;boolean old=c.occludeFlywheel;boolean enabled=c.enabled;
        boolean oldFreeze=c.freezeCreateAnimations;boolean oldGpu=c.gpuGlassCulling;
        boolean oldAnimation=c.occludeCreateAnimations, oldFlyMode=c.flywheelAnyBlock, oldAnimMode=c.animationAnyBlock;
        var testLevel=(SmokeWallChecks.TestLevel)mc.level;var oldWall=testLevel.wall;
        var camera=mc.gameRenderer.getMainCamera();var oldEye=camera.getPosition();
        var cameraPosition=net.minecraft.client.Camera.class.getDeclaredMethod("setPosition",double.class,double.class,double.class);cameraPosition.setAccessible(true);
        try {
            cameraPosition.invoke(camera,3.75,.5,.5);
            c.enabled=true;c.occludeFlywheel=true;c.gpuGlassCulling=false;mc.screen=null;
            mc.player=(net.minecraft.client.player.LocalPlayer)SmokeRadicalChecks.allocate(net.minecraft.client.player.LocalPlayer.class);
            var position=net.minecraft.world.entity.Entity.class.getDeclaredField("position");position.setAccessible(true);position.set(mc.player,net.minecraft.world.phys.Vec3.ZERO);
            var side=net.minecraft.world.level.Level.class.getDeclaredField("isClientSide");side.setAccessible(true);side.setBoolean(mc.level,true);
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","large_cogwheel"));
            var be=((net.minecraft.world.level.block.EntityBlock)block).newBlockEntity(new net.minecraft.core.BlockPos(9,0,0),block.defaultBlockState());be.setLevel(mc.level);
            var base=Class.forName("com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual");
            var type=Class.forName("com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityVisual$LargeCogVisual");
            var visual=SmokeRadicalChecks.allocate(type);
            var beField=Class.forName("dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual").getDeclaredField("blockEntity");beField.setAccessible(true);beField.set(visual,be);
            var handle=new Handle(true);
            var model=new com.simibubi.create.content.kinetics.base.RotatingInstance(null,handle);
            var modelField=base.getDeclaredField("rotatingModel");modelField.setAccessible(true);modelField.set(visual,model);
            var shaftHandle=new Handle(true);
            var shaft=new com.simibubi.create.content.kinetics.base.RotatingInstance(null,shaftHandle);
            var shaftField=type.getDeclaredField("additionalShaft");shaftField.setAccessible(true);shaftField.set(visual,shaft);
            var storage=new dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage();
            var mapField=Class.forName("dev.engine_room.flywheel.impl.visualization.storage.Storage").getDeclaredField("visuals");mapField.setAccessible(true);
            Object stored = visual;
            var extensionHandle = new Handle(true);
            if (dev.slopium.client.Slopium.loaded("azimuth")) {
                var wrapper = Class.forName("com.cake.azimuth.behaviour.render.WrappedVisual");
                var constructor = wrapper.getDeclaredConstructor(dev.engine_room.flywheel.api.visual.BlockEntityVisual.class, java.util.List.class);
                constructor.setAccessible(true);
                var extensionType = Class.forName("com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviourVisual");
                var extension = SmokeRadicalChecks.allocate(extensionType);
                var chainField = extensionType.getDeclaredField("chainInstance"); chainField.setAccessible(true);
                chainField.set(extension, new com.simibubi.create.content.processing.burner.ScrollTransformedInstance(null, extensionHandle));
                stored = constructor.newInstance(visual, java.util.List.of(extension));
                com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: manager receives real Azimuth wrapper {} around {}", stored.getClass().getName(), visual.getClass().getName());
            }
            ((java.util.Map)mapField.get(storage)).put(be,stored);
            var managerType=Class.forName("dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl");
            Object manager=SmokeRadicalChecks.allocate(managerType);
            var field=managerType.getDeclaredField("blockEntities");field.setAccessible(true);field.set(manager,new dev.engine_room.flywheel.impl.visualization.VisualManagerImpl<>(storage));
            var index=((dev.slopium.visibility.CullingStorage)storage).slopium$cullingIndex();
            index.add((dev.engine_room.flywheel.api.visual.Visual)stored);
            var hook=java.util.Arrays.stream(managerType.getDeclaredMethods()).filter(m->m.getName().endsWith("slopium$beforeFrame")).findFirst().orElseThrow();hook.setAccessible(true);
            settle(hook,manager);
            if(handle.visible || shaftHandle.visible)throw new AssertionError("Actual Flywheel manager policy did not hide instance: "+mc.getBlockEntityRenderDispatcher().getRenderer(be).getRenderBoundingBox(be)+" eye="+camera.getPosition()+" "+dev.slopium.visibility.VisibilityPool.describe(be.getBlockPos()));
            if(!extensionHandle.visible)throw new AssertionError("Unbounded BnB behaviour geometry was hidden with the delegate");
            // Opening chat and tiny movements must not reveal hidden shafts between scheduled checks.
            var eye=camera.getPosition();
            mc.screen=new net.minecraft.client.gui.screens.ChatScreen("");
            for(int i=0;i<32;i++) {
                cameraPosition.invoke(camera,eye.x+(i%2)*.001,eye.y,eye.z);
                step(hook,manager);
                if(handle.visible||shaftHandle.visible)throw new AssertionError("Chat or camera jitter revealed hidden shaft");
            }
            cameraPosition.invoke(camera,eye.x,eye.y,eye.z);mc.screen=null;

            // Walking around a corner must restore on the first new camera frame, not after the 8-frame queue.
            long revealRays=dev.slopium.client.SlopiumStats.visibilityRays;
            cameraPosition.invoke(camera,6,.5,.5);deferProcessor();
            step(hook,manager);
            if(dev.slopium.client.SlopiumStats.visibilityRays!=revealRays)throw new AssertionError("Reveal traversed world geometry");
            if(!handle.visible||!shaftHandle.visible)throw new AssertionError("Camera-side crossing did not reveal in the first frame");
            cameraPosition.invoke(camera,eye.x,eye.y,eye.z);settle(hook,manager);
            if(handle.visible||shaftHandle.visible)throw new AssertionError("Return behind wall did not re-hide");

            // BnB material changes replace this field. The new instance must not leak through a wall.
            var retiredHandle = handle;
            handle = new Handle(true);
            model = new com.simibubi.create.content.kinetics.base.RotatingInstance(null, handle);
            modelField.set(visual, model);
            settle(hook,manager);
            if(handle.visible || shaftHandle.visible || index.altered()!=1)
                throw new AssertionError("Replacement leaked or wrapper ownership was pruned");
            c.occludeFlywheel=false;
            settle(hook,manager);
            if(!handle.visible || !shaftHandle.visible)throw new AssertionError("Actual Flywheel manager policy failed to restore instance");
            if(retiredHandle.visible)throw new AssertionError("Retired model was resurrected on restore");
            if(handle.changes!=0 || shaftHandle.changes!=0)throw new AssertionError("Visibility-only mode dirtied rotation uploads");
            testLevel.wall=net.minecraft.world.level.block.Blocks.GLASS_PANE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.IronBarsBlock.NORTH,true)
                .setValue(net.minecraft.world.level.block.IronBarsBlock.SOUTH,true);
            c.occludeFlywheel=true;c.occludeCreateAnimations=true;
            c.flywheelAnyBlock=false;c.animationAnyBlock=true;
            nextFrame();
            settle(hook,manager);
            if(!handle.visible || !((VisualOcclusionState)visual).slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("Opaque Flywheel / all-block animation policies were coupled: "+mc.getBlockEntityRenderDispatcher().getRenderer(be).getRenderBoundingBox(be)+" eye="+camera.getPosition()+" "+dev.slopium.visibility.VisibilityPool.describe(be.getBlockPos()));
            c.flywheelAnyBlock=true;c.animationAnyBlock=false;
            nextFrame();
            settle(hook,manager);
            if(handle.visible || shaftHandle.visible || ((VisualOcclusionState)visual).slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("All-block Flywheel / opaque animation policies were coupled");
            nextFrame();
            if(dev.slopium.visibility.CreateWallControl.hidden(be,false) || !dev.slopium.visibility.CreateWallControl.hidden(be,true))
                throw new AssertionError("Create geometry policy ignored mode");
            testLevel.wall=net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();nextFrame();deferProcessor();
            step(hook,manager);
            if(!handle.visible||!shaftHandle.visible)throw new AssertionError("Removed obstacle did not reveal in the first frame");
            settle(hook,manager);
            if(!handle.visible || !shaftHandle.visible)throw new AssertionError("Removed window did not restore large cog and shaft");
            boolean previousCamera=c.cameraCulling,previousFly=c.occludeFlywheel,previousAnimation=c.occludeCreateAnimations;
            try {
                c.cameraCulling=true;c.occludeFlywheel=c.occludeCreateAnimations=false;
                testProjection=new org.joml.Matrix4f().perspective((float)Math.toRadians(110),1,.1f,100);
                testView=new org.joml.Matrix4f().rotateY((float)-Math.PI/2);
                settle(hook,manager);
                if(handle.visible||shaftHandle.visible)throw new AssertionError("Frustum reveal test did not first hide rear models");
                testView=new org.joml.Matrix4f().rotateY((float)Math.PI/2);deferProcessor();
                step(hook,manager);
                if(!handle.visible||!shaftHandle.visible)throw new AssertionError("Turn into view waited for the slow hide queue");
            } finally {c.cameraCulling=previousCamera;c.occludeFlywheel=previousFly;c.occludeCreateAnimations=previousAnimation;testView=testProjection=null;}
            c.occludeCreateAnimations=false;c.freezeCreateAnimations=true;model.rotationalSpeed=120;shaft.rotationalSpeed=60;
            int dirtyBefore=handle.changes+shaftHandle.changes;
            settle(hook,manager);
            if(!((VisualOcclusionState)visual).slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("CPU updates did not pause");
            if(model.rotationalSpeed!=120||shaft.rotationalSpeed!=60||handle.changes+shaftHandle.changes!=dirtyBefore)
                throw new AssertionError("CPU pause changed shader motion or dirtied GPU uploads");
            c.enabled=false;settle(hook,manager);
            if(!handle.visible||!shaftHandle.visible||((VisualOcclusionState)visual).slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("Global off did not restore visibility / CPU updates");
            // Exercise the real manager hook with an actual indirect manager type, without submitting GL draws.
            c.enabled=c.occludeFlywheel=true;c.freezeCreateAnimations=c.occludeCreateAnimations=false;c.flywheelAnyBlock=true;
            testLevel.wall=net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();nextFrame();settle(hook,manager);
            if(handle.visible || shaftHandle.visible)throw new AssertionError("GPU handoff precondition not hidden");
            var engineType=dev.engine_room.flywheel.backend.engine.EngineImpl.class;
            var engine=SmokeRadicalChecks.allocate(engineType);
            var drawField=engineType.getDeclaredField("drawManager");drawField.setAccessible(true);
            drawField.set(engine,SmokeRadicalChecks.allocate(dev.engine_room.flywheel.backend.engine.indirect.IndirectDrawManager.class));
            var lateType=Class.forName("dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl$LateInit");
            var late=SmokeRadicalChecks.allocate(lateType);
            var engineField=lateType.getDeclaredField("engine");engineField.setAccessible(true);engineField.set(late,engine);
            var lateField=managerType.getDeclaredField("lateInit");lateField.setAccessible(true);lateField.set(manager,late);
            c.flywheelAnyBlock=false;
            settle(hook,manager);
            if(!handle.visible||!shaftHandle.visible||index.altered()!=0)
                throw new AssertionError("CPU-hidden handles were not handed back to GPU culling");
            boolean diagnostics=c.diagnostics;c.diagnostics=true;
            try {
                long reads=dev.slopium.client.SlopiumStats.visibilityReads, checks=dev.slopium.client.SlopiumStats.flywheelVisualsChecked;
                settle(hook,manager);
                if(dev.slopium.client.SlopiumStats.visibilityReads!=reads||dev.slopium.client.SlopiumStats.flywheelVisualsChecked!=checks)
                    throw new AssertionError("Native GPU model-only policy still walks CPU visual queue");
            } finally {c.diagnostics=diagnostics;}
            if(!dev.slopium.client.SlopiumStats.flywheelGpuModels||!index.describe(be).contains("native GPU"))
                throw new AssertionError("Actual backend routing not reported");
            // A requested section pass cannot suppress the selective CPU fallback when its GPU shader cannot honor the filter.
            var savedMode=dev.slopium.client.SlopiumConfig.get().processedBlockMode;
            var savedIds=c.processedBlockIds;boolean savedSections=c.visibleSections,savedGpu=c.gpuGlassCulling;
            try {
                c.processedBlockMode=dev.slopium.visibility.BlockSelectionMode.BLACKLIST;
                c.processedBlockIds=java.util.List.of("create:chain_conveyor");
                dev.slopium.visibility.ProcessedBlocks.rebuild();c.visibleSections=c.gpuGlassCulling=true;
                settle(hook,manager);
                if(handle.visible||shaftHandle.visible||dev.slopium.client.SlopiumStats.flywheelGpuModels)
                    throw new AssertionError("Requested GPU mask swallowed filtered CPU fallback");
            } finally {c.processedBlockMode=savedMode;c.processedBlockIds=savedIds;c.visibleSections=savedSections;c.gpuGlassCulling=savedGpu;dev.slopium.visibility.ProcessedBlocks.rebuild();}
            c.occludeCreateAnimations=true;c.animationAnyBlock=false;settle(hook,manager);
            if(!handle.visible||!((VisualOcclusionState)visual).slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("GPU model delegation disabled CPU animation occlusion");
            c.occludeCreateAnimations=false;c.flywheelAnyBlock=true;settle(hook,manager);
            if(handle.visible||shaftHandle.visible)throw new AssertionError("Any-block mode lost CPU hiding on indirect");
            c.gpuGlassCulling=true;
            if(dev.slopium.visibility.GpuGlassDepth.available()) {
                settle(hook,manager);
                if(!handle.visible||!shaftHandle.visible||!dev.slopium.client.SlopiumStats.flywheelGpuModels)
                    throw new AssertionError("Any-block GPU toggle did not delegate model visibility");
                c.gpuGlassCulling=false;settle(hook,manager);
                if(handle.visible||shaftHandle.visible)throw new AssertionError("GPU glass off did not restore CPU mode");
            }
            c.gpuGlassCulling=false;
            c.flywheelAnyBlock=false;settle(hook,manager);
            lateField.set(manager,null);settle(hook,manager);
            if(handle.visible||shaftHandle.visible||dev.slopium.client.SlopiumStats.flywheelGpuModels)
                throw new AssertionError("Backend fallback did not restore CPU opaque policy");
            c.enabled=false;settle(hook,manager);
            if(!handle.visible||!shaftHandle.visible)throw new AssertionError("Routing test left hidden instances");
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: ONE-FRAME reveal on camera crossing, obstacle removal and frustum re-entry, with trace processor deliberately deferred, passed");
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: actual indirect hook handoff: zero CPU visibility reads/evaluations in 24 steady frames; animations, any-block mode and backend fallback passed");
            SmokeCullingQueueChecks.stress(be,base,type,beField,modelField,shaftField);
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: large cog + shaft behind adjacent pane, Azimuth delegate ownership, replacement, independent modes, removal, full pause and global restore passed");
        } finally {cameraPosition.invoke(camera,oldEye.x,oldEye.y,oldEye.z);mc.player=oldPlayer;mc.screen=oldScreen;c.occludeFlywheel=old;c.enabled=enabled;c.freezeCreateAnimations=oldFreeze;c.gpuGlassCulling=oldGpu;c.occludeCreateAnimations=oldAnimation;c.flywheelAnyBlock=oldFlyMode;c.animationAnyBlock=oldAnimMode;testLevel.wall=oldWall;dev.slopium.visibility.WallOcclusion.invalidate();}
    }

    private static void settle(java.lang.reflect.Method hook,Object manager) throws ReflectiveOperationException {
        for(int i=0;i<24;i++) step(hook,manager);
    }
    private static void step(java.lang.reflect.Method hook,Object manager) throws ReflectiveOperationException {
        var frame=dev.slopium.client.Slopium.class.getDeclaredField("frame");frame.setAccessible(true);
        frame.setLong(null,frame.getLong(null)+1);
        dev.slopium.visibility.VisibilityPool.process(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        var mc=net.minecraft.client.Minecraft.getInstance();
        var context=new dev.engine_room.flywheel.api.backend.RenderContext(){
            public net.minecraft.client.renderer.LevelRenderer renderer(){return mc.levelRenderer;}
            public net.minecraft.client.multiplayer.ClientLevel level(){return mc.level;}
            public net.minecraft.client.renderer.RenderBuffers buffers(){return mc.renderBuffers();}
            public org.joml.Matrix4fc modelView(){return testView==null?new org.joml.Matrix4f():testView;}
            public org.joml.Matrix4fc projection(){return testProjection==null?new org.joml.Matrix4f():testProjection;}
            public org.joml.Matrix4fc viewProjection(){return new org.joml.Matrix4f();}
            public net.minecraft.client.Camera camera(){return mc.gameRenderer.getMainCamera();}
            public float partialTick(){return 0;}
        };
        hook.invoke(manager,context,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("frame",false));
    }

    private static void rotationWriter() {
        var type=com.simibubi.create.foundation.render.AllInstanceTypes.ROTATING;
        var handle=new Handle(true);
        var instance=type.create(handle);
        long pointer=org.lwjgl.system.MemoryUtil.nmemAlloc(128);
        try {
            instance.rotationalSpeed=120;instance.rotationOffset=10;
            type.writer().write(pointer,instance);
            if(org.lwjgl.system.MemoryUtil.memGetFloat(pointer+40)!=120 || org.lwjgl.system.MemoryUtil.memGetFloat(pointer+44)!=10||handle.changes!=0)
                throw new AssertionError("Slopium changed native rotation payload");
        } finally {org.lwjgl.system.MemoryUtil.nmemFree(pointer);}
    }

    private static void nextFrame() throws ReflectiveOperationException {
        var frame=dev.slopium.client.Slopium.class.getDeclaredField("frame");frame.setAccessible(true);frame.setLong(null,frame.getLong(null)+1);
        dev.slopium.visibility.WallOcclusion.invalidate();
    }

}

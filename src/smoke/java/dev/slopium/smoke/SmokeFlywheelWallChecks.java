package dev.slopium.smoke;
import dev.engine_room.flywheel.api.instance.*;
import dev.slopium.visibility.InstanceVisibility;
import dev.slopium.visibility.VisualOcclusionState;

final class SmokeFlywheelWallChecks {
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
            rotationWriter();
            SmokeInstanceBackendChecks.run();
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
        boolean oldFreeze=c.freezeCreateAnimations;
        boolean oldAnimation=c.occludeCreateAnimations, oldFlyMode=c.flywheelAnyBlock, oldAnimMode=c.animationAnyBlock;
        var testLevel=(SmokeWallChecks.TestLevel)mc.level;var oldWall=testLevel.wall;
        try {
            c.enabled=true;c.occludeFlywheel=true;mc.screen=null;
            mc.player=(net.minecraft.client.player.LocalPlayer)SmokeRadicalChecks.allocate(net.minecraft.client.player.LocalPlayer.class);
            var position=net.minecraft.world.entity.Entity.class.getDeclaredField("position");position.setAccessible(true);position.set(mc.player,net.minecraft.world.phys.Vec3.ZERO);
            var side=net.minecraft.world.level.Level.class.getDeclaredField("isClientSide");side.setAccessible(true);side.setBoolean(mc.level,true);
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","large_cogwheel"));
            var be=((net.minecraft.world.level.block.EntityBlock)block).newBlockEntity(new net.minecraft.core.BlockPos(5,0,0),block.defaultBlockState());be.setLevel(mc.level);
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
            var owned=java.util.Arrays.stream(managerType.getDeclaredFields()).filter(f->f.getName().endsWith("slopium$hidden")).findFirst().orElseThrow();owned.setAccessible(true);owned.set(manager,new java.util.IdentityHashMap<>());
            var hook=java.util.Arrays.stream(managerType.getDeclaredMethods()).filter(m->m.getName().endsWith("slopium$beforeFrame")).findFirst().orElseThrow();hook.setAccessible(true);
            settle(hook,manager);
            if(handle.visible || shaftHandle.visible)throw new AssertionError("Actual Flywheel manager policy did not hide instance");
            if(!extensionHandle.visible)throw new AssertionError("Unbounded BnB behaviour geometry was hidden with the delegate");
            // BnB material changes replace this field. The new instance must not leak through a wall.
            var retiredHandle = handle;
            handle = new Handle(true);
            model = new com.simibubi.create.content.kinetics.base.RotatingInstance(null, handle);
            modelField.set(visual, model);
            settle(hook,manager);
            if(handle.visible || shaftHandle.visible || ((java.util.Map<?,?>)owned.get(manager)).size()!=1)
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
                throw new AssertionError("Opaque Flywheel / all-block animation policies were coupled");
            c.flywheelAnyBlock=true;c.animationAnyBlock=false;
            nextFrame();
            settle(hook,manager);
            if(handle.visible || shaftHandle.visible || ((VisualOcclusionState)visual).slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("All-block Flywheel / opaque animation policies were coupled");
            nextFrame();
            if(dev.slopium.visibility.CreateWallControl.hidden(be,false) || !dev.slopium.visibility.CreateWallControl.hidden(be,true))
                throw new AssertionError("Create geometry policy ignored mode");
            testLevel.wall=net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();nextFrame();
            settle(hook,manager);
            if(!handle.visible || !shaftHandle.visible)throw new AssertionError("Removed window did not restore large cog and shaft");
            c.occludeCreateAnimations=false;c.freezeCreateAnimations=true;model.rotationalSpeed=120;shaft.rotationalSpeed=60;
            settle(hook,manager);
            if(((dev.slopium.visibility.PausedRotation)model).slopium$renderSpeed(120)!=0 || ((dev.slopium.visibility.PausedRotation)shaft).slopium$renderSpeed(60)!=0)
                throw new AssertionError("Full pause did not reach both large cog instances");
            c.enabled=false;
            settle(hook,manager);
            if(((dev.slopium.visibility.PausedRotation)model).slopium$renderSpeed(120)!=120 || !handle.visible || !shaftHandle.visible)
                throw new AssertionError("Global off did not restore GPU rotation and visibility");
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: large cog + shaft behind adjacent pane, Azimuth delegate ownership, replacement, independent modes, removal, full pause and global restore passed");
        } finally {mc.player=oldPlayer;mc.screen=oldScreen;c.occludeFlywheel=old;c.enabled=enabled;c.freezeCreateAnimations=oldFreeze;c.occludeCreateAnimations=oldAnimation;c.flywheelAnyBlock=oldFlyMode;c.animationAnyBlock=oldAnimMode;testLevel.wall=oldWall;dev.slopium.visibility.WallOcclusion.invalidate();}
    }

    private static void settle(java.lang.reflect.Method hook, Object manager) throws ReflectiveOperationException {
        // A cold shape lookup can consume the one-ms budget. Wait for deferred work,
        // just as real frames do; do not raise the production budget in tests.
        var frame=dev.slopium.client.Slopium.class.getDeclaredField("frame");frame.setAccessible(true);
        for (int i=0;i<10;i++) {
            long deferred=dev.slopium.client.SlopiumStats.createOcclusionDeferred;
            hook.invoke(manager,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("frame",false));
            if (dev.slopium.client.SlopiumStats.createOcclusionDeferred==deferred) return;
            frame.setLong(null,frame.getLong(null)+1);
        }
        throw new AssertionError("Flywheel requests did not settle within ten frames");
    }

    private static void rotationWriter() {
        var type=com.simibubi.create.foundation.render.AllInstanceTypes.ROTATING;
        var handle=new Handle(true);
        var instance=type.create(handle);
        var pause=(dev.slopium.visibility.PausedRotation)instance;
        long pointer=org.lwjgl.system.MemoryUtil.nmemAlloc(128);
        try {
            instance.rotationalSpeed=120;instance.rotationOffset=10;
            pause.slopium$pauseRotation(true,2);
            type.writer().write(pointer,instance);
            if(org.lwjgl.system.MemoryUtil.memGetFloat(pointer+40)!=0 || org.lwjgl.system.MemoryUtil.memGetFloat(pointer+44)!=250)
                throw new AssertionError("Actual GPU writer did not freeze angle");
            if(instance.rotationalSpeed!=120 || instance.rotationOffset!=10 || handle.changes!=1)
                throw new AssertionError("Pause mutated mechanism state or failed to mark upload dirty");
            instance.rotationalSpeed=30;instance.rotationOffset=15;
            pause.slopium$pauseRotation(true,3);
            type.writer().write(pointer,instance);
            if(org.lwjgl.system.MemoryUtil.memGetFloat(pointer+40)!=0 || org.lwjgl.system.MemoryUtil.memGetFloat(pointer+44)!=250 || handle.changes!=1)
                throw new AssertionError("State update changed paused GPU angle");
            pause.slopium$pauseRotation(false,4);
            type.writer().write(pointer,instance);
            if(org.lwjgl.system.MemoryUtil.memGetFloat(pointer+40)!=30 || org.lwjgl.system.MemoryUtil.memGetFloat(pointer+44)!=15 || handle.changes!=2)
                throw new AssertionError("Resume did not upload current mechanism state");
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: actual rotating GPU writer pause, update while paused and resume passed");
        } finally {org.lwjgl.system.MemoryUtil.nmemFree(pointer);}
    }

    private static void nextFrame() throws ReflectiveOperationException {
        var frame=dev.slopium.client.Slopium.class.getDeclaredField("frame");frame.setAccessible(true);frame.setLong(null,frame.getLong(null)+1);
        dev.slopium.visibility.WallOcclusion.invalidate();
    }

}

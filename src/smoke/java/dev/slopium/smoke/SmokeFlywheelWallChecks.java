package dev.slopium.smoke;
import dev.engine_room.flywheel.api.instance.*;
import dev.slopium.visibility.InstanceVisibility;
import dev.slopium.visibility.VisualOcclusionState;

final class SmokeFlywheelWallChecks {
    static final class Handle implements InstanceHandle {
        boolean visible; Handle(boolean value){visible=value;}
        public void setChanged(){} public void setDeleted(){} public void setVisible(boolean value){visible=value;} public boolean isVisible(){return visible;}
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
        boolean oldAnimation=c.occludeCreateAnimations, oldFlyMode=c.flywheelAnyBlock, oldAnimMode=c.animationAnyBlock;
        var testLevel=(SmokeWallChecks.TestLevel)mc.level;var oldWall=testLevel.wall;
        try {
            c.enabled=true;c.occludeFlywheel=true;mc.screen=null;
            mc.player=(net.minecraft.client.player.LocalPlayer)SmokeRadicalChecks.allocate(net.minecraft.client.player.LocalPlayer.class);
            var position=net.minecraft.world.entity.Entity.class.getDeclaredField("position");position.setAccessible(true);position.set(mc.player,net.minecraft.world.phys.Vec3.ZERO);
            var side=net.minecraft.world.level.Level.class.getDeclaredField("isClientSide");side.setAccessible(true);side.setBoolean(mc.level,true);
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","shaft"));
            var be=((net.minecraft.world.level.block.EntityBlock)block).newBlockEntity(new net.minecraft.core.BlockPos(10,0,0),block.defaultBlockState());be.setLevel(mc.level);
            var type=Class.forName("com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual");
            var visual=SmokeRadicalChecks.allocate(type);
            var beField=Class.forName("dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual").getDeclaredField("blockEntity");beField.setAccessible(true);beField.set(visual,be);
            var handle=new Handle(true);
            var model=new com.simibubi.create.content.kinetics.base.RotatingInstance(null,handle);
            var modelField=type.getDeclaredField("rotatingModel");modelField.setAccessible(true);modelField.set(visual,model);
            var storage=new dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage();
            var mapField=Class.forName("dev.engine_room.flywheel.impl.visualization.storage.Storage").getDeclaredField("visuals");mapField.setAccessible(true);
            ((java.util.Map)mapField.get(storage)).put(be,visual);
            var managerType=Class.forName("dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl");
            Object manager=SmokeRadicalChecks.allocate(managerType);
            var field=managerType.getDeclaredField("blockEntities");field.setAccessible(true);field.set(manager,new dev.engine_room.flywheel.impl.visualization.VisualManagerImpl<>(storage));
            var owned=java.util.Arrays.stream(managerType.getDeclaredFields()).filter(f->f.getName().endsWith("slopium$hidden")).findFirst().orElseThrow();owned.setAccessible(true);owned.set(manager,new java.util.IdentityHashMap<>());
            var hook=java.util.Arrays.stream(managerType.getDeclaredMethods()).filter(m->m.getName().endsWith("slopium$beforeFrame")).findFirst().orElseThrow();hook.setAccessible(true);
            hook.invoke(manager,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("frame",false));
            if(handle.visible)throw new AssertionError("Actual Flywheel manager policy did not hide instance");
            c.occludeFlywheel=false;
            hook.invoke(manager,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("off",false));
            if(!handle.visible)throw new AssertionError("Actual Flywheel manager policy failed to restore instance");
            testLevel.wall=net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState();
            c.occludeFlywheel=true;c.occludeCreateAnimations=true;
            c.flywheelAnyBlock=false;c.animationAnyBlock=true;
            nextFrame();
            hook.invoke(manager,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("separate",false));
            if(!handle.visible || !((VisualOcclusionState)visual).slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("Opaque Flywheel / all-block animation policies were coupled");
            c.flywheelAnyBlock=true;c.animationAnyBlock=false;
            nextFrame();
            hook.invoke(manager,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("reverse",false));
            if(handle.visible || ((VisualOcclusionState)visual).slopium$isOccluded(dev.slopium.client.Slopium.frame()))
                throw new AssertionError("All-block Flywheel / opaque animation policies were coupled");
            nextFrame();
            if(dev.slopium.visibility.CreateWallControl.hidden(be,false) || !dev.slopium.visibility.CreateWallControl.hidden(be,true))
                throw new AssertionError("Create geometry policy ignored mode");
        } finally {mc.player=oldPlayer;mc.screen=oldScreen;c.occludeFlywheel=old;c.enabled=enabled;c.occludeCreateAnimations=oldAnimation;c.flywheelAnyBlock=oldFlyMode;c.animationAnyBlock=oldAnimMode;testLevel.wall=oldWall;dev.slopium.visibility.WallOcclusion.invalidate();}
    }

    private static void nextFrame() throws ReflectiveOperationException {
        var frame=dev.slopium.client.Slopium.class.getDeclaredField("frame");frame.setAccessible(true);frame.setLong(null,frame.getLong(null)+1);
        dev.slopium.visibility.WallOcclusion.invalidate();
    }

}

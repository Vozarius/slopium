package dev.slopium.smoke;

import dev.slopium.client.*;
import dev.slopium.visibility.*;
import net.minecraft.client.*;
import net.minecraft.client.particle.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Exercises the actual ParticleEngine render call with the default inflated particle bounds. */
final class SmokeParticleOcclusionChecks {
    static final class Dot extends Particle {
        int draws;
        Dot() { super(null,0,0,0); }
        void at(double px, double py, double pz) {
            xo=x=px; yo=y=py; zo=z=pz;
            setBoundingBox(new AABB(px-.1,py-.1,pz-.1,px+.1,py+.1,pz+.1));
        }
        public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, Camera camera, float partial) { draws++; }
        public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }
    }
    @SuppressWarnings("unchecked")
    static void run() throws ReflectiveOperationException {
        var mc=Minecraft.getInstance(); var oldLevel=mc.level;
        var c=SlopiumConfig.get(); var saved=new HashMap<java.lang.reflect.Field,Object>();
        for(var f:SlopiumConfig.class.getFields())saved.put(f,f.get(c));
        var frame=Slopium.class.getDeclaredField("frame");frame.setAccessible(true);long oldFrame=frame.getLong(null);
        var particleMap=ParticleEngine.class.getDeclaredField("particles");particleMap.setAccessible(true);
        var map=(Map<ParticleRenderType,Queue<Particle>>)particleMap.get(mc.particleEngine);
        var oldParticles=new HashMap<>(map);
        try {
            var level=(SmokeWallChecks.TestLevel)SmokeRadicalChecks.allocate(SmokeWallChecks.TestLevel.class);
            level.loaded=true;SmokeWallChecks.attachSable(level);mc.level=level;
            var particle=(Dot)SmokeRadicalChecks.allocate(Dot.class);particle.at(5.1,.5,.5);
            var camera=new Camera();
            var position=Camera.class.getDeclaredMethod("setPosition",double.class,double.class,double.class);position.setAccessible(true);
            position.invoke(camera,3.75,.5,.5);
            map.clear();map.put(ParticleRenderType.PARTICLE_SHEET_OPAQUE,new ArrayDeque<>(List.of(particle)));
            c.enabled=c.diagnostics=c.occludeParticles=true;c.disableParticles=c.particleRange=false;
            level.wall=Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH,true).setValue(IronBarsBlock.SOUTH,true);
            c.opaqueGlassParticles=false;next(frame);
            render(mc,camera);if(particle.draws!=1)throw new AssertionError("Opaque mode hid particle through glass");
            c.opaqueGlassParticles=true;next(frame);
            // Old code ignores the window because it intersects inflated particle bounds.
            if(WallOcclusion.hidden(particle.getRenderBoundingBox(0),camera.getPosition(),true))throw new AssertionError("Fixture must reproduce old nearby-pane miss");
            render(mc,camera);if(particle.draws!=1)throw new AssertionError("Actual ParticleEngine rendered particle behind nearby pane");
            ParticleOcclusion.hidden(particle.getPos(),new Vec3(5.2,.5,.5),true);
            frame.setLong(null,frame.getLong(null)+1);VisibilityPool.process(new Vec3(5.2,.5,.5));
            if(ParticleOcclusion.hidden(particle.getPos(),new Vec3(5.2,.5,.5),true))throw new AssertionError("Moving through window reused hidden result");
            level.wall=Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.EAST);next(frame);
            render(mc,camera);if(particle.draws!=1)throw new AssertionError("Closed door leaked particle");
            level.wall=level.wall.setValue(DoorBlock.OPEN,true);next(frame);
            render(mc,camera);if(particle.draws!=2)throw new AssertionError("Open doorway hid particle");
            level.wall=Blocks.GLASS.defaultBlockState();next(frame);
            // Exhausting Create's entire budget must not prevent particle checks.
            WallOcclusion.hidden(new AABB(200,0,0,201,1,1),camera.getPosition(),true);
            var steps=WallOcclusion.class.getDeclaredField("steps");steps.setAccessible(true);steps.setInt(null,0);
            render(mc,camera);if(particle.draws!=2)throw new AssertionError("Create budget starved particles");
            c.occludeParticles=false;render(mc,camera);if(particle.draws!=3)throw new AssertionError("Particle mode off did not restore");
            c.occludeParticles=true;c.enabled=false;render(mc,camera);if(particle.draws!=4)throw new AssertionError("Global off did not restore");
            c.enabled=true;level.loaded=false;next(frame);render(mc,camera);
            if(particle.draws!=5)throw new AssertionError("Unknown chunks hidden");
            level.loaded=true;next(frame);
            VisibilityPool.clear();
            if(ParticleOcclusion.hidden(particle.getPos(),camera.getPosition(),true))throw new AssertionError("Unverified request hidden");
            for(int i=0;i<8;i++){frame.setLong(null,frame.getLong(null)+1);VisibilityPool.process(camera.getPosition());}
            if(!ParticleOcclusion.hidden(particle.getPos(),camera.getPosition(),true))throw new AssertionError("Deferred particle check never serviced");
            // Interpolation follows the rendered centre, not just current-tick coordinates.
            var px=Particle.class.getDeclaredField("x");px.setAccessible(true);px.setDouble(particle,7.1);
            if(Math.abs(((ParticlePosition)(Object)particle).slopium$renderPosition(.5f).x-6.1)>1e-6)throw new AssertionError("Particle interpolation incorrect");
            level.wall=Blocks.AIR.defaultBlockState();next(frame);render(mc,camera);
            if(particle.draws!=6)throw new AssertionError("Removing window did not restore particle");
            c.cameraCulling=true;c.occludeParticles=false;particle.at(0,0,-5);position.invoke(camera,0.,0.,0.);
            var projection=new org.joml.Matrix4f().perspective((float)Math.toRadians(90),1,.1f,100);
            var nativeFrustum=new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f(),projection);nativeFrustum.prepare(0,0,0);
            VisibilityPool.captureCamera(new org.joml.Matrix4f().rotateY((float)Math.PI),projection,Vec3.ZERO);
            long cameraChecks=SlopiumStats.cameraChecks;int draws=particle.draws;
            mc.particleEngine.render(mc.gameRenderer.lightTexture(),camera,0f,nativeFrustum,type->true);
            if(particle.draws!=draws+1||SlopiumStats.cameraChecks!=cameraChecks)throw new AssertionError("Native particle frustum was redundantly tested");
            mc.particleEngine.render(mc.gameRenderer.lightTexture(),camera,0f,null,type->true);
            if(particle.draws!=draws+1||SlopiumStats.cameraChecks<=cameraChecks)throw new AssertionError("Missing particle frustum did not use shared camera fallback");
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: native particle frustum reused without duplicate tests; missing-frustum fallback culled correctly");
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: actual particle render near pane, closed/open door, shared pool, deferred queue, interpolation, invalidation and off switches passed");
        } finally {
            mc.level=oldLevel;map.clear();map.putAll(oldParticles);
            for(var e:saved.entrySet())e.getKey().set(c,e.getValue());
            frame.setLong(null,oldFrame);WallOcclusion.invalidate();SlopiumStats.reset();
        }
    }
    private static void render(Minecraft mc,Camera camera) throws ReflectiveOperationException {
        // Model the normal frame boundary before entering the actual ParticleEngine renderer.
        var field=ParticleEngine.class.getDeclaredField("particles");field.setAccessible(true);
        var all=(Map<ParticleRenderType,Queue<Particle>>)field.get(mc.particleEngine);
        for(var group:all.values()) for(var p:group) ParticleOcclusion.hidden(((ParticlePosition)p).slopium$renderPosition(0),camera.getPosition(),true);
        var frame=Slopium.class.getDeclaredField("frame");frame.setAccessible(true);
        for(int i=0;i<8;i++){frame.setLong(null,frame.getLong(null)+1);VisibilityPool.process(camera.getPosition());}

        mc.particleEngine.render(mc.gameRenderer.lightTexture(),camera,0f,null,type->true);
    }
    private static void next(java.lang.reflect.Field frame)throws IllegalAccessException {
        frame.setLong(null,frame.getLong(null)+1);WallOcclusion.invalidate();
    }
}

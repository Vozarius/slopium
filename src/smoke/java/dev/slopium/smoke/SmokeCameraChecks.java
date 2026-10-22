package dev.slopium.smoke;
import dev.slopium.client.*;
import dev.slopium.visibility.VisibilityPool;
import net.minecraft.world.phys.*;
final class SmokeCameraChecks {
    static void run() throws ReflectiveOperationException {
        var c=SlopiumConfig.get();boolean enabled=c.enabled,camera=c.cameraCulling,diag=c.diagnostics;
        try {
            c.enabled=c.cameraCulling=c.diagnostics=true;VisibilityPool.clear();
            var projection=new org.joml.Matrix4f().perspective((float)Math.toRadians(90),1,.1f,100);
            VisibilityPool.captureCamera(new org.joml.Matrix4f(),projection,Vec3.ZERO);
            Vec3 front=new Vec3(0,0,-5),back=new Vec3(0,0,5);
            AABB box=new AABB(-.5,-.5,-5.5,.5,.5,-4.5),rear=new AABB(-.5,-.5,4.5,.5,.5,5.5);
            if(VisibilityPool.outsideCamera(front,true,box)||!VisibilityPool.outsideCamera(back,true,rear))throw new AssertionError("Frustum front/back failed");
            long checks=SlopiumStats.cameraChecks,rays=SlopiumStats.visibilityRays;
            for(int i=0;i<100;i++)if(!VisibilityPool.outsideCamera(back,true,rear))throw new AssertionError("Cached frustum result changed");
            if(checks!=SlopiumStats.cameraChecks||rays!=SlopiumStats.visibilityRays)throw new AssertionError("Camera reads repeated geometry work");
            var table=VisibilityPool.class.getDeclaredField("TABLE");table.setAccessible(true);
            int before=((java.util.Map<?,?>)table.get(null)).size();VisibilityPool.read(front,Vec3.ZERO,true,true);
            if(((java.util.Map<?,?>)table.get(null)).size()!=before)throw new AssertionError("Camera and walls use duplicate entries");
            // Bounds crossing the screen edge must not be culled just because the centre is off-screen.
            if(VisibilityPool.outsideCamera(new Vec3(6,0,-5),true,new AABB(4,-1,-6,8,1,-4)))throw new AssertionError("Partial model incorrectly culled");
            VisibilityPool.captureCamera(new org.joml.Matrix4f().rotateY((float)Math.PI),projection,Vec3.ZERO);
            if(VisibilityPool.outsideCamera(back,true,rear)||!VisibilityPool.outsideCamera(front,true,box))throw new AssertionError("Camera rotation retained stale results");
            c.cameraCulling=false;if(VisibilityPool.outsideCamera(front,true,box))throw new AssertionError("Camera off ignored");
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: frustum front/back, partial bounds, rotation/off, shared wall entry and 100 cached reads passed");
        } finally {c.enabled=enabled;c.cameraCulling=camera;c.diagnostics=diag;VisibilityPool.clear();}
    }
}

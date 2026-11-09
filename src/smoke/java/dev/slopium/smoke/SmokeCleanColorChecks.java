package dev.slopium.smoke;

import com.simibubi.create.content.kinetics.base.*;
import dev.slopium.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import java.util.*;

/** Calls Create's actual transformed method; no fake copy of the stress calculation. */
final class SmokeCleanColorChecks {
    static void run() throws ReflectiveOperationException {
        var c=SlopiumConfig.get();boolean enabled=c.enabled;
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:shaft"));
        var be=(KineticBlockEntity)((EntityBlock)block).newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
        var effects=((dev.slopium.mixin.KineticEffectsAccess)be).slopium$effects();
        var stress=effects.getClass().getDeclaredField("overStressedEffect");stress.setAccessible(true);
        var handle=new SmokeFlywheelWallChecks.Handle(true);
        var instance=new RotatingInstance(null,handle);instance.rotationalSpeed=120;
        try {
            c.enabled=true;stress.setFloat(effects,0);instance.setColor(net.createmod.catnip.theme.Color.WHITE);
            KineticBlockEntityVisual.applyOverstressEffect(be,instance);
            if(handle.changes!=0)throw new AssertionError("Unchanged tint still dirty");
            stress.setFloat(effects,.5f);KineticBlockEntityVisual.applyOverstressEffect(be,instance);
            if(handle.changes!=1||instance.green==(byte)255)throw new AssertionError("Stress tint lost");
            stress.setFloat(effects,0);KineticBlockEntityVisual.applyOverstressEffect(be,instance);
            if(handle.changes!=2||instance.red!=(byte)255||instance.green!=(byte)255||instance.blue!=(byte)255)throw new AssertionError("Tint reset lost");
            KineticBlockEntityVisual.applyOverstressEffect(be,instance);
            if(handle.changes!=2||instance.rotationalSpeed!=120)throw new AssertionError("Clean tint changed rotation");
            c.enabled=false;KineticBlockEntityVisual.applyOverstressEffect(be,instance);
            if(handle.changes!=3)throw new AssertionError("Master off did not restore Create path");
            // Unchanged-data workload: ABBA, same arrays and instances, no raster rendering.
            int count=8192,ticks=100;var instances=new RotatingInstance[count];
            for(int i=0;i<count;i++){instances[i]=new RotatingInstance(null,handle);instances[i].setColor(net.createmod.catnip.theme.Color.WHITE);}
            var rows=new ArrayList<Map<String,Object>>();
            for(int repeat=-1;repeat<4;repeat++)for(boolean active:new boolean[]{false,true,true,false}){
                c.enabled=active;handle.changes=0;long start=System.nanoTime();
                for(int tick=0;tick<ticks;tick++)KineticBlockEntityVisual.applyOverstressEffect(be,instances);
                long ns=System.nanoTime()-start;
                if(handle.changes!=(active?0:count*ticks))throw new AssertionError("Dirty suppression count mismatch");
                if(repeat>=0)rows.add(Map.of("enabled",active,"milliseconds",ns/1e6,"dirtyWrites",handle.changes,"instances",count,"ticks",ticks));
            }
            var out=java.nio.file.Path.of("slopium-clean-color-benchmark.json");
            try{java.nio.file.Files.writeString(out,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));}
            catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: Create clean tint, stress onset/reset, RPM and master-off passed; isolated CPU benchmark {}",out.toAbsolutePath());
        } finally {c.enabled=enabled;}
    }
}

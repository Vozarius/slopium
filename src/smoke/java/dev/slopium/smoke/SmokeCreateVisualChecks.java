package dev.slopium.smoke;
import dev.slopium.client.*;
import dev.slopium.visibility.*;
import dev.engine_room.flywheel.api.visual.*;
import dev.engine_room.flywheel.api.task.*;
import dev.engine_room.flywheel.api.visualization.*;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;
final class SmokeCreateVisualChecks {
    static class Fixture implements BlockEntityVisual<BlockEntity>,DynamicVisual {
        int frames;
        public void update(float dt){} public void delete(){}
        public void collectCrumblingInstances(java.util.function.Consumer<dev.engine_room.flywheel.api.instance.Instance> c){}
        public Plan<DynamicVisual.Context> planFrame(){return SimplePlan.of((dev.engine_room.flywheel.lib.task.functional.RunnableWithContext<DynamicVisual.Context>)ctx->frames++);}
    }
    static final class SimpleFixture extends Fixture implements SimpleDynamicVisual {
        public void beginFrame(DynamicVisual.Context ctx){frames++;}
    }
    static final class Storage extends dev.engine_room.flywheel.impl.visualization.storage.Storage<Object>{
        Fixture next;
        public boolean willAccept(Object o){return true;}
        protected Visual createRaw(VisualizationContext ctx,Object o,float dt){return next;}
    }
    static void run() throws ReflectiveOperationException {
        var c=SlopiumConfig.get();var saved=new HashMap<java.lang.reflect.Field,Object>();for(var f:SlopiumConfig.class.getFields())saved.put(f,f.get(c));
        try{
            if(ProcessedBlocks.catalog().isEmpty())throw new AssertionError("Empty registry catalog");
            for(String id:List.of("create:shaft","create:mechanical_press","create:mechanical_mixer","create:andesite_funnel","create:belt","create:chain_conveyor"))
                if(ProcessedBlocks.catalog().stream().noneMatch(e->e.id().equals(id)))throw new AssertionError("Missing catalog entry "+id);
            for(var type:BuiltInRegistries.BLOCK_ENTITY_TYPE)if(VisualizerRegistry.getVisualizer(type)!=null)
                for(var block:((dev.slopium.mixin.BlockEntityTypeBlocksAccess)type).slopium$blocks())
                    if(ProcessedBlocks.catalog().stream().noneMatch(e->e.block()==block&&e.flywheel()))throw new AssertionError("Unlisted registered visualizer");
            var level=(net.minecraft.client.multiplayer.ClientLevel)SmokeRadicalChecks.allocate(net.minecraft.client.multiplayer.ClientLevel.class);
            var side=net.minecraft.world.level.Level.class.getDeclaredField("isClientSide");side.setAccessible(true);side.setBoolean(level,true);
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:chain_conveyor"));
            var be=((EntityBlock)block).newBlockEntity(new net.minecraft.core.BlockPos(200,0,0),block.defaultBlockState());be.setLevel(level);
            c.enabled=true;c.freezeCreateAnimations=true;c.throttleCreateVisuals=c.throttleFlywheelDistance=false;
            TaskExecutor executor=new TaskExecutor(){public int threadCount(){return 1;}public void execute(Runnable r){r.run();}};
            DynamicVisual.Context ctx=new DynamicVisual.Context(){
                public net.minecraft.client.Camera camera(){return net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();}
                public org.joml.FrustumIntersection frustum(){return null;}public float partialTick(){return 0;}
                public DistanceUpdateLimiter limiter(){return distance->true;}
            };
            for(Fixture visual:List.of(new Fixture(),new SimpleFixture())){
                var owner=new OwnedInstances(be);owner.slopium$setOccludedUntil(true,0,Long.MAX_VALUE);OwnedInstances.register(visual,owner);
                var storage=new Storage();storage.next=visual;storage.add(null,visual,0);
                var plan=storage.framePlan();
                for(var mode:BlockSelectionMode.values())for(boolean listed:new boolean[]{false,true}){
                    c.processedBlockMode=mode;c.processedBlockIds=listed?new ArrayList<>(List.of("create:chain_conveyor")):new ArrayList<>();ProcessedBlocks.rebuild();
                    int before=visual.frames;boolean[] finished={false};plan.execute(executor,ctx,()->finished[0]=true);
                    if(!finished[0]||visual.frames-before!=(mode.allows(listed)?0:1))throw new AssertionError("Storage did not honor shared filter: "+mode+"/"+listed+"/"+visual.getClass());
                }
                c.enabled=false;int before=visual.frames;plan.execute(executor,ctx);if(visual.frames!=before+1)throw new AssertionError("Disabled optimization still pauses visual");c.enabled=true;
                storage.invalidate();if(OwnedInstances.get(visual)!=null)throw new AssertionError("Owner leaked on invalidate");
            }
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: full registered block catalog ({}), whitelist/blacklist/all, actual simple and planned Storage dispatch, cancellation completion and owner cleanup passed",ProcessedBlocks.catalog().size());
        }finally{for(var e:saved.entrySet())e.getKey().set(c,e.getValue());ProcessedBlocks.rebuild();}
    }
}

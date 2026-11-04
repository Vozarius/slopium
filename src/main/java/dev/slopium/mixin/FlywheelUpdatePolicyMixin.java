package dev.slopium.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.engine_room.flywheel.api.visual.*;
import dev.engine_room.flywheel.api.task.*;
import dev.engine_room.flywheel.lib.task.*;
import dev.engine_room.flywheel.lib.task.functional.*;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.slopium.visibility.VisualUpdatePolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="dev.engine_room.flywheel.impl.visualization.storage.Storage",remap=false)
public abstract class FlywheelUpdatePolicyMixin {
    @WrapOperation(method="tickPlan",at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/lib/task/ForEachPlan;of(Ldev/engine_room/flywheel/lib/task/functional/SupplierWithContext$Ignored;Ldev/engine_room/flywheel/lib/task/functional/ConsumerWithContext;)Ldev/engine_room/flywheel/lib/task/ForEachPlan;"))
    private ForEachPlan<?,?> slopium$simpleTicks(SupplierWithContext.Ignored supplier,ConsumerWithContext consumer,Operation<ForEachPlan<?,?>> original){
        ConsumerWithContext<dev.engine_room.flywheel.lib.visual.SimpleTickableVisual,TickableVisual.Context> filtered=(visual,ctx)->{if(VisualUpdatePolicy.allow(visual,null))consumer.accept(visual,ctx);};
        return original.call(supplier,filtered);
    }
    @WrapOperation(method="setup",at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/api/visual/TickableVisual;planTick()Ldev/engine_room/flywheel/api/task/Plan;"))
    private Plan<TickableVisual.Context> slopium$tickPlan(TickableVisual visual,Operation<Plan<TickableVisual.Context>> original){
        var plan=original.call(visual);
        return new SimplyComposedPlan<>(){public void execute(TaskExecutor executor,TickableVisual.Context ctx,Runnable complete){if(VisualUpdatePolicy.allow(visual,null))plan.execute(executor,ctx,complete);else complete.run();}};
    }
    @WrapOperation(method="framePlan",at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/lib/task/ForEachPlan;of(Ldev/engine_room/flywheel/lib/task/functional/SupplierWithContext$Ignored;Ldev/engine_room/flywheel/lib/task/functional/ConsumerWithContext;)Ldev/engine_room/flywheel/lib/task/ForEachPlan;"))
    private ForEachPlan<?,?> slopium$simple(SupplierWithContext.Ignored supplier,ConsumerWithContext consumer,Operation<ForEachPlan<?,?>> original){
        ConsumerWithContext<SimpleDynamicVisual,DynamicVisual.Context> filtered=(visual,ctx)->{if(VisualUpdatePolicy.allow(visual,ctx))consumer.accept(visual,ctx);};
        return original.call(supplier,filtered);
    }
    @WrapOperation(method="setup",at=@At(value="INVOKE",target="Ldev/engine_room/flywheel/api/visual/DynamicVisual;planFrame()Ldev/engine_room/flywheel/api/task/Plan;"))
    private Plan<DynamicVisual.Context> slopium$plan(DynamicVisual visual,Operation<Plan<DynamicVisual.Context>> original){
        var plan=original.call(visual);
        return new SimplyComposedPlan<>(){public void execute(TaskExecutor executor,DynamicVisual.Context ctx,Runnable complete){if(VisualUpdatePolicy.allow(visual,ctx))plan.execute(executor,ctx,complete);else complete.run();}};
    }
}

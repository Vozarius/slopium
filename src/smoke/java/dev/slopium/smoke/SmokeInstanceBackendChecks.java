package dev.slopium.smoke;

import dev.engine_room.flywheel.api.instance.*;
import dev.engine_room.flywheel.backend.engine.*;
import dev.engine_room.flywheel.backend.engine.instancing.InstancedInstancer;
import dev.slopium.visibility.InstanceVisibility;

/** Real Flywheel handles and instance-list compaction; no scene or GL draw setup required. */
final class SmokeInstanceBackendChecks {
    private static final class MemoryManager extends DrawManager<AbstractInstancer<?>> {
        private final boolean indirect;
        MemoryManager(boolean indirect) { this.indirect=indirect; }
        @Override @SuppressWarnings("unchecked")
        public <I extends Instance> AbstractInstancer<I> getInstancer(InstancerKey<I> key) {
            return (AbstractInstancer<I>)instancers.computeIfAbsent(key, this::create);
        }
        protected <I extends Instance> AbstractInstancer<?> create(InstancerKey<I> key) {
            var recreate = new AbstractInstancer.Recreate<>(key, this);
            return indirect ? new dev.engine_room.flywheel.backend.engine.indirect.IndirectInstancer<>(key,recreate)
                : new InstancedInstancer<>(key,recreate);
        }
        protected <I extends Instance> void initialize(InstancerKey<I> key, AbstractInstancer<?> instancer) {}
        public void renderCrumbling(java.util.List<dev.engine_room.flywheel.api.backend.Engine.CrumblingBlock> blocks) {}
        public void triggerFallback() { throw new AssertionError("Unexpected backend fallback"); }
        public MeshPool meshPool() { return null; }
    }
    static void run() {
        run(false); run(true);
    }
    private static void run(boolean indirect) {
        var manager = new MemoryManager(indirect);
        var model = new dev.engine_room.flywheel.api.model.Model() {
            public java.util.List<ConfiguredMesh> meshes() { return java.util.List.of(); }
            public org.joml.Vector4fc boundingSphere() { return new org.joml.Vector4f(0,0,0,1); }
        };
        var key = new InstancerKey<>(null, com.simibubi.create.foundation.render.AllInstanceTypes.ROTATING, model, 0);
        var instancer = manager.getInstancer(key);
        var visible = instancer.createInstance();
        if (indirect) com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: indirect live instance count={}, public isVisible={}, state={}",
            instancer.instanceCount(), visible.handle().isVisible(), ((InstanceHandleImpl<?>)visible.handle()).state.getClass().getName());
        var preHidden = instancer.createInstance(); preHidden.setVisible(false);
        var owner = new InstanceVisibility(); owner.accept(visible); owner.accept(preHidden);
        instancer.parallelUpdate();
        if (instancer.instanceCount()!=0 || owner.size()!=1) throw new AssertionError("Real Flywheel draw list retained hidden instances; indirect="+indirect);
        owner.restore(); instancer.parallelUpdate();
        if (instancer.instanceCount()!=1 || owner.size()!=0 || preHidden.handle().isVisible())
            throw new AssertionError("Real Flywheel restore lost ownership");
        visible.delete(); instancer.parallelUpdate();
        if (instancer.instanceCount()!=0) throw new AssertionError("Deleted instance retained");
        preHidden.setVisible(true);
        if(instancer.instanceCount()!=1) throw new AssertionError("Foreign hidden instance was lost");
        preHidden.delete(); instancer.parallelUpdate();
        var retired=instancer.createInstance();owner.accept(retired);retired.delete();owner.restore();instancer.parallelUpdate();
        if(instancer.instanceCount()!=0)throw new AssertionError("Deleted hidden instance was resurrected");
        ownership(manager,model,indirect);
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: real Flywheel handles/list compaction 2 -> 0 -> 1 -> 0, pre-hidden instance preserved; indirect={}",indirect);
    }
    private static void ownership(MemoryManager manager,dev.engine_room.flywheel.api.model.Model model,boolean indirect) {
        var context=new dev.engine_room.flywheel.api.visualization.VisualEmbedding(){
            public InstancerProvider instancerProvider(){return new InstancerProvider(){
                public <I extends Instance> Instancer<I> instancer(InstanceType<I> type,dev.engine_room.flywheel.api.model.Model m,int bias){return manager.getInstancer(new InstancerKey<>(null,type,m,bias));}
            };}
            public net.minecraft.core.Vec3i renderOrigin(){return net.minecraft.core.Vec3i.ZERO;}
            public dev.engine_room.flywheel.api.visualization.VisualEmbedding createEmbedding(net.minecraft.core.Vec3i origin){return this;}
            public void transforms(org.joml.Matrix4fc pose,org.joml.Matrix3fc normal){}
            public void delete(){}
        };
        var a=new dev.slopium.visibility.OwnedInstances(null);
        var b=new dev.slopium.visibility.OwnedInstances(null);
        var type=com.simibubi.create.foundation.render.AllInstanceTypes.ROTATING;
        var first=a.wrap(context).instancerProvider().instancer(type,model,1);
        var second=b.wrap(context).createEmbedding(net.minecraft.core.Vec3i.ZERO).instancerProvider().instancer(type,model,2);
        var moved=first.createInstance();var authoredHidden=first.createInstance();authoredHidden.setVisible(false);
        var av=new InstanceVisibility();var bv=new InstanceVisibility();av.hide(a);bv.hide(b);
        var late=first.createInstance();
        if(late.handle().isVisible())throw new AssertionError("New instance flashed while owner hidden");
        second.stealInstance(moved);
        av.restore();
        if(moved.handle().isVisible()||authoredHidden.handle().isVisible())throw new AssertionError("Old owner restored transferred/authored-hidden instance");
        // Public isVisible is broken for the indirect backend; inspect the real list instead.
        var firstList=manager.getInstancer(new InstancerKey<>(null,type,model,1));
        var secondList=manager.getInstancer(new InstancerKey<>(null,type,model,2));
        firstList.parallelUpdate();secondList.parallelUpdate();
        if(secondList.instanceCount()!=0)throw new AssertionError("Old owner resurrected transferred instance in native backend");
        bv.restore();secondList.parallelUpdate();
        if(firstList.instanceCount()!=1||secondList.instanceCount()!=1)throw new AssertionError("Owned/embedded instances lost on transfer or restore");
        av.hide(a);late.delete();av.restore();firstList.parallelUpdate();
        if(firstList.instanceCount()!=0)throw new AssertionError("Deleted late instance resurrected");
        moved.delete();authoredHidden.delete();secondList.parallelUpdate();
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: context/embedding ownership, late creation, cross-owner transfer, authored invisibility and deletion passed; indirect={}",indirect);
    }
}

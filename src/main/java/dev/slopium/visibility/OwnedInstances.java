package dev.slopium.visibility;
import dev.engine_room.flywheel.api.instance.*;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.Visual;
import dev.engine_room.flywheel.api.visualization.*;
import net.minecraft.core.Vec3i;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
/** Track the context used by a whole block visual, including recyclers and nested embeddings.
 * Crumbling lists are incomplete (e.g. conveyor boxes/guards); never use them as ownership lists. */
public final class OwnedInstances implements VisualOcclusionState {
    public final net.minecraft.world.level.block.entity.BlockEntity block;
    private volatile boolean frozen;
    private volatile long frozenUntil;
    public OwnedInstances(net.minecraft.world.level.block.entity.BlockEntity block){this.block=block;}
    public void slopium$setOccluded(boolean value,long frame){slopium$setOccludedUntil(value,frame,frame);}
    public void slopium$setOccludedUntil(boolean value,long frame,long until){frozenUntil=until;frozen=value;}
    public boolean slopium$isOccluded(long frame){return frozen&&frame<=frozenUntil;}
    private static final Map<Visual,OwnedInstances> OWNERS=new ConcurrentHashMap<>();
    private final Queue<Instance> added=new ConcurrentLinkedQueue<>();
    private final Queue<Instance> hiddenAtCreation=new ConcurrentLinkedQueue<>();
    private volatile boolean hiding;
    private final Set<Instance> live=Collections.newSetFromMap(new IdentityHashMap<>());
    public static void register(Visual visual,OwnedInstances owner){if(visual!=null)OWNERS.put(visual,owner);}
    public static OwnedInstances get(Visual visual){return OWNERS.get(visual);}
    public static void remove(Visual visual){OWNERS.remove(visual);}
    public void hiding(boolean value){hiding=value;}
    public boolean owns(Instance instance) {
        return !(instance.handle() instanceof InstanceOwnership tag) || tag.slopium$owner()==this;
    }
    public void drainHidden(Consumer<Instance> action){Instance next;while((next=hiddenAtCreation.poll())!=null)if(owns(next))action.accept(next);}
    private <I extends Instance> I track(I instance){
        if(instance.handle() instanceof InstanceOwnership tag) {
            var old=tag.slopium$owner();
            if(old!=this) {
                if(tag.slopium$hidden()) { instance.setVisible(true);tag.slopium$hidden(false); }
                if(old!=null && instance instanceof PausedRotation rotation)rotation.slopium$pauseRotation(false,0);
                tag.slopium$owner(this);
            }
        }
        added.add(instance);
        if(hiding&&InstanceVisibility.isVisible(instance)){
            instance.setVisible(false);
            if(instance.handle() instanceof InstanceOwnership tag)tag.slopium$hidden(true);
            hiddenAtCreation.add(instance);
        }
        return instance;
    }
    public void forEach(Consumer<Instance> action){
        Instance next;while((next=added.poll())!=null)live.add(next);
        var it=live.iterator();while(it.hasNext()){
            Instance instance=it.next();
            if(!owns(instance)){it.remove();continue;}
            if(instance.handle() instanceof dev.engine_room.flywheel.backend.engine.InstanceHandleImpl<?> h&&h.index<0){it.remove();continue;}
            action.accept(instance);
        }
    }
    public VisualizationContext wrap(VisualizationContext delegate){return new Context(delegate);}
    private class Context implements VisualizationContext {
        final VisualizationContext delegate;
        private final InstancerProvider provider=new InstancerProvider(){
            public <I extends Instance> Instancer<I> instancer(InstanceType<I> type,Model model,int bias){
                Instancer<I> original=delegate.instancerProvider().instancer(type,model,bias);
                return new Instancer<>(){
                    public I createInstance(){return track(original.createInstance());}
                    public void stealInstance(I instance){original.stealInstance(instance);track(instance);}
                };
            }
        };
        Context(VisualizationContext delegate){this.delegate=delegate;}
        public InstancerProvider instancerProvider(){return provider;}
        public Vec3i renderOrigin(){return delegate.renderOrigin();}
        public VisualEmbedding createEmbedding(Vec3i origin){return new Embedding(delegate.createEmbedding(origin));}
    }
    private final class Embedding extends Context implements VisualEmbedding {
        private final VisualEmbedding embedding;
        Embedding(VisualEmbedding embedding){super(embedding);this.embedding=embedding;}
        public void transforms(org.joml.Matrix4fc pose,org.joml.Matrix3fc normal){embedding.transforms(pose,normal);}
        public void delete(){embedding.delete();}
    }
}

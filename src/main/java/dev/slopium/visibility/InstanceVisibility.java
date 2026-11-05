package dev.slopium.visibility;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

/** Restore only currently owned instances that Slopium hid; preserve existing invisibility. */
public final class InstanceVisibility implements Consumer<Instance> {
    private final Set<Instance> owned = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Instance> present = Collections.newSetFromMap(new IdentityHashMap<>());
    private int newlyHidden;
    private OwnedInstances owner;

    public void accept(Instance instance) {
        if (instance == null) return;
        present.add(instance);
        if (isVisible(instance)) {
            instance.setVisible(false);
            if(owner!=null && instance.handle() instanceof InstanceOwnership tag)tag.slopium$hidden(true);
            owned.add(instance);
            newlyHidden++;
        }
    }

    static boolean isVisible(Instance instance) {
        var handle = instance.handle();
        // Flywheel 1.0.6 isVisible() checks only AbstractInstancer. Indirect uses
        // InstancePage as its live handle state, so the public method reports false.
        // Recognize only this audited live state, never arbitrary non-deleted states.
        if (handle instanceof dev.engine_room.flywheel.backend.engine.InstanceHandleImpl<?> flywheel
                && flywheel.state != null && flywheel.state.getClass().getName().equals(
                    "dev.engine_room.flywheel.backend.engine.indirect.IndirectInstancer$InstancePage")) return true;
        return handle.isVisible();
    }

    /** Refresh the audited delegate, never a wrapper's additional behaviour geometry. */
    public int hide(BlockEntityVisual<?> visual) {
        present.clear(); newlyHidden = 0;
        visual.collectCrumblingInstances(this);
        owned.retainAll(present); // Replaced/deleted instances must not be resurrected on restore.
        present.clear();
        return newlyHidden;
    }

    public int size() { return owned.size(); }
    public int hide(OwnedInstances instances){
        owner=instances;instances.hiding(true);instances.drainHidden(owned::add);
        present.clear();newlyHidden=0;instances.forEach(this);owned.retainAll(present);present.clear();return newlyHidden;
    }
    public void restore() {
        if(owner!=null){owner.hiding(false);owner.drainHidden(owned::add);}
        for (var instance : owned) {
            if(owner!=null&&!owner.owns(instance))continue;
            // In Flywheel 1.0.6 deleting a Hidden handle leaves its state as Hidden,
            // but clears its index. Do not recreate that retired instance.
            if (instance.handle() instanceof dev.engine_room.flywheel.backend.engine.InstanceHandleImpl<?> flywheel
                    && flywheel.index < 0) continue;
            instance.setVisible(true);
            if(instance.handle() instanceof InstanceOwnership tag)tag.slopium$hidden(false);
        }
        owner=null;
        owned.clear(); present.clear();
    }
}

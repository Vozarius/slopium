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

    public void accept(Instance instance) {
        if (instance == null) return;
        present.add(instance);
        if (isVisible(instance)) {
            instance.setVisible(false);
            owned.add(instance);
            newlyHidden++;
        }
    }

    private static boolean isVisible(Instance instance) {
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
    public void restore() {
        for (var instance : owned) {
            // In Flywheel 1.0.6 deleting a Hidden handle leaves its state as Hidden,
            // but clears its index. Do not recreate that retired instance.
            if (instance.handle() instanceof dev.engine_room.flywheel.backend.engine.InstanceHandleImpl<?> flywheel
                    && flywheel.index < 0) continue;
            instance.setVisible(true);
        }
        owned.clear(); present.clear();
    }
}

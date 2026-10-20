package dev.slopium.visibility;
import dev.engine_room.flywheel.api.instance.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Restore only instances that Slopium hid; preserve pre-existing invisibility. */
public final class InstanceVisibility implements Consumer<Instance> {
    private final List<Instance> owned = new ArrayList<>();
    public void accept(Instance instance) {
        if (instance != null && instance.handle().isVisible()) { instance.setVisible(false); owned.add(instance); }
    }
    public int size() { return owned.size(); }
    public void restore() { for (var instance : owned) instance.setVisible(true); owned.clear(); }
}

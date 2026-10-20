package dev.slopium.visibility;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import net.minecraft.client.Minecraft;

/** Visual detail cutoff only. World data, panel behaviour ticks and item stacks are untouched. */
public final class FactoryDetailControl {
    private FactoryDetailControl() {}
    public static boolean far(FactoryPanelBlockEntity panel, int distance) {
        var mc = Minecraft.getInstance(); var level = panel.getLevel(); var pos = panel.getBlockPos();
        if (level == null || !level.isClientSide || level != mc.level || mc.screen != null || panel.isVirtual()
                || SableGuard.protects(level,pos)) return false;
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) return false;
        double radius = RenderBudget.radius(distance);
        return pos.distToCenterSqr(mc.gameRenderer.getMainCamera().getPosition()) > radius*radius;
    }
}

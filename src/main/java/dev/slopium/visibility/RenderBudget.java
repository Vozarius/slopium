package dev.slopium.visibility;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

public final class RenderBudget {
    private RenderBudget() {}
    public static int radius(int configured) {
        return Math.min(Math.max(0, configured), Minecraft.getInstance().options.getEffectiveRenderDistance() * 16);
    }
    /** Test closest point, preserving large models crossing the protected sphere. */
    public static boolean outside(AABB box, double x, double y, double z, double radius) {
        double dx = Math.max(Math.max(box.minX - x, 0), x - box.maxX);
        double dy = Math.max(Math.max(box.minY - y, 0), y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - z, 0), z - box.maxZ);
        return dx * dx + dy * dy + dz * dz > radius * radius;
    }
}

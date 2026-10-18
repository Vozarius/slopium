package dev.slopium.visibility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.slopium.client.SlopiumStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemImpostor {
    private ItemImpostor() {}
    public static boolean render(ItemEntity entity, PoseStack pose, MultiBufferSource buffers, int light, boolean diagnostics) {
        var stack = entity.getItem();
        if (entity.hasCustomName() || stack.isEmpty() || stack.hasFoil()) return false;
        var client = Minecraft.getInstance();
        var model = client.getItemRenderer().getModel(stack, entity.level(), null, entity.getId());
        if (model.isCustomRenderer()) return false;
        var sprite = model.getParticleIcon();
        // A shared atlas/render type lets the existing buffer source batch these quads.
        // No retained sprite cache: resource reloads naturally pick up new models and UVs.
        pose.pushPose();
        try {
            pose.translate(0, 0.25, 0);
            pose.mulPose(client.gameRenderer.getMainCamera().rotation());
            var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
            quad(consumer, pose.last(), sprite, light);
        } finally { pose.popPose(); }
        if (diagnostics) SlopiumStats.itemImpostorsRendered++;
        return true;
    }
    private static void quad(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite, int light) {
        vertex(out, pose, -0.2f, -0.2f, sprite.getU0(), sprite.getV1(), light);
        vertex(out, pose,  0.2f, -0.2f, sprite.getU1(), sprite.getV1(), light);
        vertex(out, pose,  0.2f,  0.2f, sprite.getU1(), sprite.getV0(), light);
        vertex(out, pose, -0.2f,  0.2f, sprite.getU0(), sprite.getV0(), light);
    }
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
        out.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
    }
}

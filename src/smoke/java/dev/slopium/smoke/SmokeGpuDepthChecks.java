package dev.slopium.smoke;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.slopium.visibility.GpuGlassDepth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL33C;
final class SmokeGpuDepthChecks {
    static void run() {
        var mc=Minecraft.getInstance();var projection=new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting=RenderSystem.getVertexSorting();
        var source=new TextureTarget(32,32,true,Minecraft.ON_OSX);
        var destination=new TextureTarget(32,32,true,Minecraft.ON_OSX);
        RenderSystem.getModelViewStack().pushMatrix();
        try {
            source.bindWrite(true);RenderSystem.depthMask(true);RenderSystem.clearDepth(.75);RenderSystem.clear(GL33C.GL_DEPTH_BUFFER_BIT,Minecraft.ON_OSX);
            RenderSystem.getModelViewStack().identity();RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new org.joml.Matrix4f(),VertexSorting.ORTHOGRAPHIC_Z);
            GpuGlassDepth.renderDepth(source,destination,()-> {
                RenderSystem.disableCull();RenderSystem.disableBlend();RenderSystem.setShader(GameRenderer::getPositionShader);
                var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
                b.addVertex(-1,-1,-.5f);b.addVertex(1,-1,-.5f);b.addVertex(1,1,-.5f);b.addVertex(-1,1,-.5f);
                BufferUploader.drawWithShader(b.buildOrThrow());
            });
            if(GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING)!=source.frameBufferId)throw new AssertionError("GPU pass leaked framebuffer");
            float[] pixel=new float[1];
            GL33C.glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER,destination.frameBufferId);GL33C.glReadPixels(16,16,1,1,GL33C.GL_DEPTH_COMPONENT,GL33C.GL_FLOAT,pixel);
            if(Math.abs(pixel[0]-.25f)>.01f)throw new AssertionError("GPU obstacle depth not written: "+pixel[0]);
            GL33C.glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER,source.frameBufferId);GL33C.glReadPixels(16,16,1,1,GL33C.GL_DEPTH_COMPONENT,GL33C.GL_FLOAT,pixel);
            if(Math.abs(pixel[0]-.75f)>.01f)throw new AssertionError("Main depth modified: "+pixel[0]);
            try {GpuGlassDepth.renderDepth(source,destination,()-> {throw new IllegalStateException("probe");});throw new AssertionError("Exception swallowed");}
            catch(IllegalStateException expected) {if(!expected.getMessage().equals("probe"))throw expected;}
            if(GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING)!=source.frameBufferId)throw new AssertionError("Exception path leaked framebuffer");
            int error=GL33C.glGetError();if(error!=GL33C.GL_NO_ERROR)throw new AssertionError("GPU depth GL error "+error);
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: GPU raster depth 0.25, original depth 0.75 preserved, framebuffer and exception restoration passed");
        } finally {
            source.destroyBuffers();destination.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.getModelViewStack().popMatrix();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.clearDepth(1);
        }
    }
}

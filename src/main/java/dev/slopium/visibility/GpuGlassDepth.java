package dev.slopium.visibility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.slopium.client.*;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL33C;

/** Optional extra terrain-depth pass. Never changes the main target or reads pixels on the CPU. */
public final class GpuGlassDepth {
    private static RenderContext context;
    private static TextureTarget target;
    private static Object world;
    private static long failedRevision=Long.MIN_VALUE;
    public static long passes, submitNanos;
    public static String state="not requested";
    private GpuGlassDepth() {}
    public static RenderContext context(RenderContext next) { var old=context;context=next;return old; }
    public static boolean available() {
        var c=SlopiumConfig.get();var mc=Minecraft.getInstance();
        if(!c.enabled||!c.gpuGlassCulling||!c.occludeFlywheel||!c.flywheelAnyBlock||c.disableFlywheelRendering) return false;
        if(Slopium.loaded("iris")||Slopium.loaded("oculus")) { state="shader integration: CPU fallback";return false; }
        if(mc.options.graphicsMode().get()==GraphicsStatus.FABULOUS) { state="fabulous: CPU fallback";return false; }
        return failedRevision!=Slopium.configRevision();
    }
    public static void release() {
        if(target!=null) { target.destroyBuffers();target=null; }
        world=null;state="not requested";
    }
    public static RenderTarget depthTarget(RenderTarget main) {
        if(!available()||context==null||context.level()!=Minecraft.getInstance().level) {
            if(!SlopiumConfig.get().gpuGlassCulling||Minecraft.getInstance().level==null) release();
            return main;
        }
        if(GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING)!=main.frameBufferId) {
            state="custom framebuffer: CPU fallback";failedRevision=Slopium.configRevision();return main;
        }
        long start=System.nanoTime();
        try {
            if(target==null||target.width!=main.width||target.height!=main.height||world!=context.level()) {
                release();target=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);world=context.level();
                main.bindWrite(false);
                int bound=GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D);
                try {
                    com.mojang.blaze3d.platform.GlStateManager._bindTexture(main.getDepthTextureId());
                    int mainFormat=GL33C.glGetTexLevelParameteri(GL33C.GL_TEXTURE_2D,0,GL33C.GL_TEXTURE_INTERNAL_FORMAT);
                    com.mojang.blaze3d.platform.GlStateManager._bindTexture(target.getDepthTextureId());
                    int targetFormat=GL33C.glGetTexLevelParameteri(GL33C.GL_TEXTURE_2D,0,GL33C.GL_TEXTURE_INTERNAL_FORMAT);
                    if(mainFormat!=targetFormat) throw new IllegalStateException("Incompatible depth formats; retaining CPU occlusion");
                } finally {com.mojang.blaze3d.platform.GlStateManager._bindTexture(bound);}
            }
            var ctx=context;
            renderDepth(main,target,()-> {
                RenderDevice.enterManagedCode();
                try {
                    var eye=ctx.camera().getPosition();
                    SodiumWorldRenderer.instance().drawChunkLayer(RenderType.translucent(),new ChunkRenderMatrices(ctx.projection(),ctx.modelView()),eye.x,eye.y,eye.z);
                } finally {RenderDevice.exitManagedCode();}
            });
            state="GPU terrain depth";
            if(SlopiumConfig.get().diagnostics) {passes++;submitNanos+=System.nanoTime()-start;}
            return target;
        } catch(RuntimeException error) {
            failedRevision=Slopium.configRevision();state="failed: CPU fallback";
            com.mojang.logging.LogUtils.getLogger().warn("Slopium GPU glass depth disabled until settings change",error);
            main.bindWrite(false);return main;
        }
    }
    /** Shared by production and the GPU framebuffer isolation smoke check. */
    public static void renderDepth(RenderTarget main,RenderTarget destination,Runnable drawTerrain) {
        int read=GL33C.glGetInteger(GL33C.GL_READ_FRAMEBUFFER_BINDING), draw=GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING);
        int program=GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM), active=GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
        int[] viewport=new int[4];GL33C.glGetIntegerv(GL33C.GL_VIEWPORT,viewport);
        boolean depth=GL33C.glIsEnabled(GL33C.GL_DEPTH_TEST),blend=GL33C.glIsEnabled(GL33C.GL_BLEND),cull=GL33C.glIsEnabled(GL33C.GL_CULL_FACE);
        boolean scissor=GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST);
        boolean depthMask=GL33C.glGetBoolean(GL33C.GL_DEPTH_WRITEMASK);
        int depthFunc=GL33C.glGetInteger(GL33C.GL_DEPTH_FUNC);
        int srcRgb=GL33C.glGetInteger(GL33C.GL_BLEND_SRC_RGB),dstRgb=GL33C.glGetInteger(GL33C.GL_BLEND_DST_RGB);
        int srcAlpha=GL33C.glGetInteger(GL33C.GL_BLEND_SRC_ALPHA),dstAlpha=GL33C.glGetInteger(GL33C.GL_BLEND_DST_ALPHA);
        var shader=RenderSystem.getShader();
        try {
            RenderSystem.disableScissor();
            destination.copyDepthFrom(main);destination.bindWrite(true);
            GL33C.glDrawBuffer(GL33C.GL_NONE);
            RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.depthFunc(GL33C.GL_LEQUAL);
            drawTerrain.run();
        } finally {
            GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER,destination.frameBufferId);GL33C.glDrawBuffer(GL33C.GL_COLOR_ATTACHMENT0);
            GL33C.glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER,read);GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER,draw);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            if(scissor) com.mojang.blaze3d.platform.GlStateManager._enableScissorTest();
            RenderSystem.depthMask(depthMask);RenderSystem.depthFunc(depthFunc);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
            RenderSystem.setShader(()->shader);GL33C.glUseProgram(program);RenderSystem.activeTexture(active);
        }
    }
}

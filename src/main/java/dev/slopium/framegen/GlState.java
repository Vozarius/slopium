package dev.slopium.framegen;

import static org.lwjgl.opengl.GL33C.*;

/** All GL calls inside this scope bypass Minecraft's state cache and restore its original state. */
public final class GlState implements AutoCloseable {
    private final int read=glGetInteger(GL_READ_FRAMEBUFFER_BINDING),draw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
    private final int program=glGetInteger(GL_CURRENT_PROGRAM),vao=glGetInteger(GL_VERTEX_ARRAY_BINDING);
    private final int active=glGetInteger(GL_ACTIVE_TEXTURE);
    private final int depthFunction=glGetInteger(GL_DEPTH_FUNC);
    private final boolean depthMask=glGetBoolean(GL_DEPTH_WRITEMASK);
    private final double clearDepth=glGetDouble(GL_DEPTH_CLEAR_VALUE);
    private final int[] viewport=new int[4],textures=new int[4],samplers=new int[4],scissorBox=new int[4];
    private final boolean depth=glIsEnabled(GL_DEPTH_TEST),blend=glIsEnabled(GL_BLEND),cull=glIsEnabled(GL_CULL_FACE);
    private final boolean scissor=glIsEnabled(GL_SCISSOR_TEST),srgb=glIsEnabled(GL_FRAMEBUFFER_SRGB);
    private final boolean[] mask=new boolean[4];
    public GlState() {
        glGetIntegerv(GL_VIEWPORT,viewport);
        glGetIntegerv(GL_SCISSOR_BOX,scissorBox);
        try(var stack=org.lwjgl.system.MemoryStack.stackPush()) {
            var bytes=stack.malloc(4);glGetBooleanv(GL_COLOR_WRITEMASK,bytes);
            for(int i=0;i<4;i++)mask[i]=bytes.get(i)!=0;
        }
        for(int i=0;i<4;i++){glActiveTexture(GL_TEXTURE0+i);textures[i]=glGetInteger(GL_TEXTURE_BINDING_2D);samplers[i]=glGetInteger(GL_SAMPLER_BINDING);glBindSampler(i,0);}
        glActiveTexture(GL_TEXTURE0);
    }
    public static void fullscreen() {
        glDisable(GL_DEPTH_TEST);glDisable(GL_BLEND);glDisable(GL_CULL_FACE);glDisable(GL_SCISSOR_TEST);glDisable(GL_FRAMEBUFFER_SRGB);
        glColorMask(true,true,true,true);
    }
    @Override public void close() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER,read);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw);
        glUseProgram(program);glBindVertexArray(vao);glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
        glScissor(scissorBox[0],scissorBox[1],scissorBox[2],scissorBox[3]);
        set(GL_DEPTH_TEST,depth);set(GL_BLEND,blend);set(GL_CULL_FACE,cull);set(GL_SCISSOR_TEST,scissor);set(GL_FRAMEBUFFER_SRGB,srgb);
        glColorMask(mask[0],mask[1],mask[2],mask[3]);
        glDepthFunc(depthFunction);glDepthMask(depthMask);glClearDepth(clearDepth);
        for(int i=0;i<4;i++){glActiveTexture(GL_TEXTURE0+i);glBindTexture(GL_TEXTURE_2D,textures[i]);glBindSampler(i,samplers[i]);}
        glActiveTexture(active);
    }
    private static void set(int cap,boolean value){if(value)glEnable(cap);else glDisable(cap);}
}

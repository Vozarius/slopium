package dev.slopium.framegen;
import static org.lwjgl.opengl.GL33C.*;

public final class GlImage implements AutoCloseable {
    public final int width,height,texture,fbo;
    public int depth;
    public GlImage(int w,int h,int format,int depthFormat) {
        width=w;height=h;texture=glGenTextures();fbo=glGenFramebuffers();
        try {
            glBindTexture(GL_TEXTURE_2D,texture);
            glTexImage2D(GL_TEXTURE_2D,0,format,w,h,0,GL_RGBA,GL_FLOAT,(java.nio.ByteBuffer)null);
            parameters();glBindFramebuffer(GL_FRAMEBUFFER,fbo);
            glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,texture,0);
            if(depthFormat!=0) {
                depth=glGenTextures();glBindTexture(GL_TEXTURE_2D,depth);
                boolean stencil=depthFormat==GL_DEPTH24_STENCIL8||depthFormat==GL_DEPTH32F_STENCIL8;
                glTexImage2D(GL_TEXTURE_2D,0,depthFormat,w,h,0,stencil?GL_DEPTH_STENCIL:GL_DEPTH_COMPONENT,
                    depthFormat==GL_DEPTH24_STENCIL8?GL_UNSIGNED_INT_24_8:stencil?GL_FLOAT_32_UNSIGNED_INT_24_8_REV:GL_FLOAT,(java.nio.ByteBuffer)null);
                parameters();glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
                glFramebufferTexture2D(GL_FRAMEBUFFER,stencil?GL_DEPTH_STENCIL_ATTACHMENT:GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,depth,0);
            }
            if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Incomplete frame-generation framebuffer");
        } catch(RuntimeException e){close();throw e;}
    }
    public static void parameters() {
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    }
    public void bind(){glBindFramebuffer(GL_DRAW_FRAMEBUFFER,fbo);glViewport(0,0,width,height);}
    @Override public void close(){glDeleteFramebuffers(fbo);glDeleteTextures(texture);if(depth!=0)glDeleteTextures(depth);}
}

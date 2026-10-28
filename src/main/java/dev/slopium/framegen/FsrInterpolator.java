package dev.slopium.framegen;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.*;
import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.EXTSemaphoreWin32.*;

/** AMD FSR 3 runtime, with explicit shared-resource ownership transfers. Windows x64 only. */
public final class FsrInterpolator implements AutoCloseable {
    private long session,frame;
    private int semaphore;
    private final int[] textures=new int[4],memory=new int[4],fbos=new int[4];
    private static final int[] LAYOUTS={GL_LAYOUT_GENERAL_EXT,GL_LAYOUT_GENERAL_EXT,GL_LAYOUT_GENERAL_EXT,GL_LAYOUT_GENERAL_EXT};
    private static final int[] NO_BUFFERS={};
    private GlProgram copy,motion;
    private final int width,height;
    public final String provider;
    public FsrInterpolator(int w,int h) {
        width=w;height=h;
        try {
            var caps=GL.getCapabilities();
            if(!caps.GL_EXT_memory_object_win32||!caps.GL_EXT_semaphore_win32)throw new IllegalStateException("Driver has no OpenGL/D3D12 texture sharing");
            var path=FsrNative.load();long luid;
            try(var stack=MemoryStack.stackPush()) {
                var bytes=stack.malloc(8);org.lwjgl.opengl.EXTMemoryObject.glGetUnsignedBytevEXT(org.lwjgl.opengl.EXTMemoryObjectWin32.GL_DEVICE_LUID_EXT,bytes);luid=bytes.getLong(0);
            }
            session=FsrNative.create(w,h,luid,path.toString());provider=FsrNative.provider(session);
            long[] handles=FsrNative.resources(session);
            int[] formats={GL_RGBA8,GL_R32F,GL_RG16F,GL_RGBA8};
            for(int i=0;i<4;i++) {
                memory[i]=glCreateMemoryObjectsEXT();glMemoryObjectParameteriEXT(memory[i],GL_DEDICATED_MEMORY_OBJECT_EXT,GL_TRUE);
                glImportMemoryWin32HandleEXT(memory[i],handles[4+i],GL_HANDLE_TYPE_D3D12_RESOURCE_EXT,handles[i]);
                textures[i]=glGenTextures();glBindTexture(GL_TEXTURE_2D,textures[i]);
                glTexStorageMem2DEXT(GL_TEXTURE_2D,1,formats[i],w,h,memory[i],0);GlImage.parameters();
                fbos[i]=glGenFramebuffers();glBindFramebuffer(GL_FRAMEBUFFER,fbos[i]);
                glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,textures[i],0);
                if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("FSR shared framebuffer incomplete");
            }
            semaphore=glGenSemaphoresEXT();glImportSemaphoreWin32HandleEXT(semaphore,GL_HANDLE_TYPE_D3D12_FENCE_EXT,handles[8]);
            copy=new GlProgram("copy");motion=new GlProgram("motion");
            int error=glGetError();if(error!=GL_NO_ERROR)throw new IllegalStateException("FSR resource sharing GL error "+error);
        }catch(Exception|LinkageError e){close();throw new IllegalStateException("FSR unavailable: "+e.getMessage(),e);}
    }
    public void generate(GlImage current,GlImage output,Matrix4f currentToPrevious,Matrix4f projection,boolean reset,float delta,boolean debug) {
        GlState.fullscreen();glViewport(0,0,width,height);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,fbos[0]);copy.use();copy.texture("source",0,current.texture);copy.draw();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,fbos[1]);copy.texture("source",0,current.depth);copy.draw();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,fbos[2]);motion.use();motion.texture("source",0,current.depth);motion.matrix("currentToPrevious",currentToPrevious);motion.draw();
        ++frame;
        glSemaphoreParameterui64EXT(semaphore,GL_D3D12_FENCE_VALUE_EXT,frame*2-1);
        glSignalSemaphoreEXT(semaphore,NO_BUFFERS,textures,LAYOUTS);glFlush();
        float near=Math.abs(projection.m32()/(projection.m22()-1)),far=Math.abs(projection.m32()/(projection.m22()+1));
        FsrNative.generate(session,frame,reset,delta,near,Float.isFinite(far)?far:10000,(float)(2*Math.atan(1/projection.m11())),debug);
        glSemaphoreParameterui64EXT(semaphore,GL_D3D12_FENCE_VALUE_EXT,frame*2);
        glWaitSemaphoreEXT(semaphore,NO_BUFFERS,textures,LAYOUTS);
        output.bind();copy.use();copy.texture("source",0,textures[3]);copy.draw();
    }
    @Override public void close() {
        // This wait happens only at resize/disable/shutdown, never in the frame loop.
        if(session!=0)glFinish();
        if(copy!=null)copy.close();if(motion!=null)motion.close();
        for(int f:fbos)if(f!=0)glDeleteFramebuffers(f);
        for(int t:textures)if(t!=0)glDeleteTextures(t);
        for(int m:memory)if(m!=0)glDeleteMemoryObjectsEXT(m);
        if(semaphore!=0)glDeleteSemaphoresEXT(semaphore);
        if(session!=0){FsrNative.destroy(session);session=0;}
    }
}

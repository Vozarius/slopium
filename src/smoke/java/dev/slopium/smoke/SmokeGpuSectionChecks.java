package dev.slopium.smoke;
import dev.slopium.visibility.GpuSections;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL43C.*;
/** Isolated shader oracle; synchronous reads exist ONLY in this non-shipped test source set. */
final class SmokeGpuSectionChecks {
    static void material(){
        var format=net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats.COMPACT;
        var builder=new net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder(format,4);
        var vertices=net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder.Vertex.uninitializedQuad();
        boolean old=dev.slopium.visibility.SectionMaterials.GLASS.get();
        try{
            for(boolean glass:new boolean[]{false,true}){
                builder.start(7);dev.slopium.visibility.SectionMaterials.GLASS.set(glass);builder.push(vertices,3);
                var encoded=builder.slice();int stride=format.getVertexFormat().getStride();
                for(int i=0;i<4;i++){
                    int bits=org.lwjgl.system.MemoryUtil.memGetInt(org.lwjgl.system.MemoryUtil.memAddress(encoded)+i*stride+16);
                    if(((bits>>>16)&255)!=(glass?131:3)||((bits>>>24)&255)!=7)throw new AssertionError("Material or section ID corrupted in active encoder");
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("SECTION_MATERIAL_ORACLE passed: active format {}, stride={}",format.getClass(),format.getVertexFormat().getStride());
        }finally{builder.destroy();dev.slopium.visibility.SectionMaterials.GLASS.set(old);}
    }
    static void run(){
        material();
        com.mojang.logging.LogUtils.getLogger().info("SECTION_GLASS classification={} methods={}",dev.slopium.visibility.SectionMaterials.glass(net.minecraft.world.level.block.Blocks.GLASS),java.util.Arrays.stream(net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName).filter(n->n.contains("slopium")).toList());
        if(!GpuSections.supported())throw new AssertionError("GPU section test needs compute support");
        int oldProgram=glGetInteger(GL_CURRENT_PROGRAM),oldBuffer=glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING),active=glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);int oldSampler=glGetIntegeri(GL_SAMPLER_BINDING,0);glBindSampler(0,0);int oldTex=glGetInteger(GL_TEXTURE_BINDING_2D),old0=glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING,0),old1=glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING,1);
        int[] unpackKeys={GL_UNPACK_ALIGNMENT,GL_UNPACK_ROW_LENGTH,GL_UNPACK_SKIP_ROWS,GL_UNPACK_SKIP_PIXELS};int[] unpack=new int[4];for(int i=0;i<4;i++){unpack[i]=glGetInteger(unpackKeys[i]);glPixelStorei(unpackKeys[i],i==0?4:0);}int unpackBuffer=glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);glBindBuffer(GL_PIXEL_UNPACK_BUFFER,0);
        int program=0,mask=glGenBuffers(),bounds=glGenBuffers(),tex=glGenTextures();
        try{
            var method=GpuSections.class.getDeclaredMethod("program",String.class);method.setAccessible(true);program=(int)method.invoke(null,"cull");
            float[] depth=new float[64*64];java.util.Arrays.fill(depth,.4f);depth[8*64+8]=1;
            glBindTexture(GL_TEXTURE_2D,tex);glTexImage2D(GL_TEXTURE_2D,0,GL_R32F,64,64,0,GL_RED,GL_FLOAT,depth);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAX_LEVEL,0);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
            float[] boxes={
                -.1f,-.1f,.5f,0, .1f,.1f,.6f,1,
                -.1f,-.1f,-.2f,0, .1f,.1f,.6f,1,
                -.1f,-.1f,-.5f,0, .1f,.1f,.6f,1,
                -.1f,-.1f,-2,0, .1f,.1f,.6f,1,
                -.1f,-.1f,.5f,1, .1f,.1f,.6f,1,
                2,2,.5f,0, 3,3,.6f,1,
                -.1f,-.1f,.5f,0, .1f,.1f,.6f,1,
                -.8f,-.8f,.5f,0, .8f,.8f,.6f,1};
            glBindBuffer(GL_SHADER_STORAGE_BUFFER,bounds);glBufferData(GL_SHADER_STORAGE_BUFFER,boxes,GL_STATIC_DRAW);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,1,bounds);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER,mask);glBufferData(GL_SHADER_STORAGE_BUFFER,new int[]{0,0,0,0,0,0,1,0},GL_DYNAMIC_DRAW);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,mask);
            glUseProgram(program);glUniformMatrix4fv(glGetUniformLocation(program,"matrix"),false,new Matrix4f().get(new float[16]));glUniform2i(glGetUniformLocation(program,"viewport"),64,64);glUniform1i(glGetUniformLocation(program,"levels"),0);glUniform1i(glGetUniformLocation(program,"cameraCull"),1);
            glDispatchCompute(1,1,1);glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT|GL_SHADER_STORAGE_BARRIER_BIT);int[] result=new int[8];glGetBufferSubData(GL_SHADER_STORAGE_BUFFER,0,result);
            if(!java.util.Arrays.equals(result,new int[]{0,1,1,1,1,0,1,1}))throw new AssertionError("GPU oracle: "+java.util.Arrays.toString(result));
            glUniform1i(glGetUniformLocation(program,"cameraCull"),0);glDispatchCompute(1,1,1);glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT|GL_SHADER_STORAGE_BARRIER_BIT);glGetBufferSubData(GL_SHADER_STORAGE_BUFFER,0,result);if(result[5]!=1)throw new AssertionError("Frustum toggle ignored: "+java.util.Arrays.toString(result));
            if(glGetError()!=GL_NO_ERROR)throw new AssertionError("GPU oracle GL state error");
            com.mojang.logging.LogUtils.getLogger().info("SECTION_GPU_ORACLE passed: hidden, equal-depth, nearer, near-plane, protected radius, frustum on/off, raster ID preservation, opening");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{
            for(int i=0;i<4;i++)glPixelStorei(unpackKeys[i],unpack[i]);glBindBuffer(GL_PIXEL_UNPACK_BUFFER,unpackBuffer);
            glUseProgram(oldProgram);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,old0);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,1,old1);glBindBuffer(GL_SHADER_STORAGE_BUFFER,oldBuffer);glBindTexture(GL_TEXTURE_2D,oldTex);glBindSampler(0,oldSampler);glActiveTexture(active);
            if(program!=0)glDeleteProgram(program);glDeleteBuffers(mask);glDeleteBuffers(bounds);glDeleteTextures(tex);
        }
    }
}

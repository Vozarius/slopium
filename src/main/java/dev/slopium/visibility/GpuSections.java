package dev.slopium.visibility;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.slopium.client.*;
import dev.slopium.framegen.GpuTimer;
import net.caffeinemc.mods.sodium.client.gl.device.*;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlTessellation;
import net.caffeinemc.mods.sodium.client.render.chunk.*;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.*;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import java.nio.*;
import java.util.*;
import java.util.function.Consumer;
import static org.lwjgl.opengl.GL43C.*;

/** Current-frame candidate-only GPU visibility. No CPU visibility readback, fences or old-frame reuse. */
public final class GpuSections {
    private static final IdentityHashMap<RenderRegion,Integer> bases=new IdentityHashMap<>();
    private static final HashMap<Long,Integer> ids=new HashMap<>();
    private static final HashMap<Integer,int[]> uniforms=new HashMap<>();
    private record CachedCommands(int buffer,int base,int size) {}
    private static final IdentityHashMap<MultiDrawBatch,CachedCommands> batches=new IdentityHashMap<>();
    private static int fbo,depth,idTexture,pyramid,width,height,pw,ph,levels,mask,bounds;
    private static int keys,hashMask;
    private static boolean flywheelWorld;
    private static int baseProgram,reduceProgram,cullProgram,commandProgram,slots;
    private static ByteBuffer staging;
    private static boolean prepass,ready;
    private static long frame=Long.MIN_VALUE,failed=-1;
    private static Object world;
    private static final Matrix4f capturedView=new Matrix4f(),capturedProjection=new Matrix4f();
    private static double eyeX,eyeY,eyeZ;
    private static int capturedPolicy;
    private static int policy(){var c=SlopiumConfig.get();return (c.sectionsAnyBlock?1:0)|(c.cameraCulling?2:0)|(SectionVisibility.minimum()<<2)|(Minecraft.getInstance().options.renderDistance().get()<<8);}
    public static void geometryChanged(){ready=false;}

    private static GpuTimer timer;
    private static String state="off";
    public static long passes,submitNanos,commands,candidates,flywheelDispatches;
    private GpuSections(){}
    public static boolean supported(){var c=GL.getCapabilities();return c.OpenGL43||(c.GL_ARB_compute_shader&&c.GL_ARB_shader_storage_buffer_object&&c.GL_ARB_multi_draw_indirect&&c.GL_ARB_shader_image_load_store&&c.GL_ARB_texture_storage&&c.GL_ARB_clear_buffer_object);}
    public static boolean requested(){return SectionVisibility.active();}
    public static boolean current(){return ready&&requested()&&frame==Slopium.frame()&&world==Minecraft.getInstance().level;}
    public static void dropBatch(MultiDrawBatch batch){var cached=batches.remove(batch);if(cached!=null)glDeleteBuffers(cached.buffer);}
    public static void shadersReloaded(){uniforms.clear();}
    public static void release(){
        for(var b:batches.values())glDeleteBuffers(b.buffer);batches.clear();
        invalidate();bases.clear();ids.clear();uniforms.clear();world=null;
        if(fbo!=0){glDeleteFramebuffers(fbo);glDeleteTextures(depth);glDeleteTextures(idTexture);glDeleteTextures(pyramid);fbo=0;}
        if(baseProgram!=0){glDeleteProgram(baseProgram);glDeleteProgram(reduceProgram);glDeleteProgram(cullProgram);glDeleteProgram(commandProgram);baseProgram=0;glDeleteBuffers(mask);glDeleteBuffers(bounds);glDeleteBuffers(keys);if(timer!=null)timer.close();timer=null;}
        if(staging!=null){MemoryUtil.memFree(staging);staging=null;}
    }
    public static void invalidate(){ready=false;frame=Long.MIN_VALUE;}
    public static void resetCounters(){passes=submitNanos=commands=candidates=flywheelDispatches=0;if(timer!=null){timer.nanos=timer.samples=0;}}
    public static String status(){return String.format(Locale.ROOT,"GPU sections | %s | candidates=%d | passes=%d | CPU=%.3f ms | GPU=%.3f ms | indirect=%d | Flywheel=%d",state,candidates,passes,submitNanos/1e6/Math.max(1,passes),timer==null?0:timer.milliseconds(),commands,flywheelDispatches);}
    public static String describe(BlockPos p){Integer id=ids.get(SectionPos.asLong(p.getX()>>4,p.getY()>>4,p.getZ()>>4));return !current()?state:id==null?"not a Sodium terrain candidate":"GPU candidate "+id+" (mask stays on GPU)";}
    public static int candidateId(int x,int y,int z){return ids.getOrDefault(SectionPos.asLong(x,y,z),-1);}
    // Test instrumentation may copy/read this buffer after a fence; production never reads it.
    public static int maskBuffer(){return current()?mask:0;}
    public static int slotCount(){return slots;}
    public static void before(ChunkRenderMatrices matrices,ChunkRenderListIterable lists,TerrainRenderPass pass,CameraTransform camera,Consumer<TerrainRenderPass> render){
        if(prepass)return;
        if(!requested()){ready=false;state="off";return;}
        var target=Minecraft.getInstance().getMainRenderTarget();
        boolean same=world==Minecraft.getInstance().level&&capturedView.equals(matrices.modelView())&&capturedProjection.equals(matrices.projection())&&eyeX==camera.x&&eyeY==camera.y&&eyeZ==camera.z&&capturedPolicy==policy()&&width==target.width&&height==target.height;
        if(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)!=target.frameBufferId){ready=false;state="custom framebuffer: visible";return;}
        if(frame==Slopium.frame()&&ready&&same)return;
        frame=Slopium.frame();ready=false;
        if(pass!=DefaultTerrainRenderPasses.SOLID){state="missing solid entry: visible";return;}
        if(!supported()||Slopium.loaded("iris")||Slopium.loaded("oculus")){state="unsupported renderer: visible";return;}
        if(failed==Slopium.configRevision()){state="GPU error: visible";return;}
        var mc=Minecraft.getInstance();var main=mc.getMainRenderTarget();
        if(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)!=main.frameBufferId||glGetInteger(GL_SAMPLES)>0){state="custom framebuffer / MSAA: visible";return;}
        long start=System.nanoTime();
        try(State saved=new State()){
            if(saved.viewport[0]!=0||saved.viewport[1]!=0||saved.viewport[2]!=main.width||saved.viewport[3]!=main.height){state="custom viewport: visible";return;}
            ensure(main.width,main.height);world=mc.level;
            capturedView.set(matrices.modelView());capturedProjection.set(matrices.projection());eyeX=camera.x;eyeY=camera.y;eyeZ=camera.z;capturedPolicy=policy();
            collect(lists,camera);
            if(slots==0){state="no candidates";return;}
            timer.start();
            glBindFramebuffer(GL_FRAMEBUFFER,fbo);glViewport(0,0,width,height);
            glDrawBuffers(new int[]{GL_NONE,GL_COLOR_ATTACHMENT1});
            RenderSystem.disableScissor();RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL_LEQUAL);
            glClearBufferuiv(GL_COLOR,1,new int[]{0,0,0,0});glClearBufferfv(GL_DEPTH,0,new float[]{1});
            prepass=true;
            try{render.accept(DefaultTerrainRenderPasses.SOLID);render.accept(DefaultTerrainRenderPasses.CUTOUT);
                if(SlopiumConfig.get().sectionsAnyBlock)render.accept(DefaultTerrainRenderPasses.TRANSLUCENT);
            }finally{prepass=false;}
            glBindFramebuffer(GL_FRAMEBUFFER,saved.draw);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,mask);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,1,bounds);
            glBindSampler(0,0);glBindSampler(1,0);
            glUseProgram(baseProgram);texture(0,depth);texture(1,idTexture);
            glUniform2i(glGetUniformLocation(baseProgram,"viewport"),width,height);
            glBindImageTexture(0,pyramid,0,false,0,GL_WRITE_ONLY,GL_R32F);
            glDispatchCompute((pw+15)/16,(ph+15)/16,1);glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT|GL_SHADER_STORAGE_BARRIER_BIT);
            glUseProgram(reduceProgram);
            for(int level=1;level<=levels;level++){
                glBindImageTexture(0,pyramid,level-1,false,0,GL_READ_ONLY,GL_R32F);glBindImageTexture(1,pyramid,level,false,0,GL_WRITE_ONLY,GL_R32F);
                glDispatchCompute((Math.max(1,pw>>level)+15)/16,(Math.max(1,ph>>level)+15)/16,1);glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            }
            glMemoryBarrier(GL_TEXTURE_FETCH_BARRIER_BIT);
            glUseProgram(cullProgram);texture(0,pyramid);
            glUniformMatrix4fv(glGetUniformLocation(cullProgram,"matrix"),false,new Matrix4f(matrices.projection()).mul(matrices.modelView()).get(new float[16]));
            glUniform2i(glGetUniformLocation(cullProgram,"viewport"),width,height);
            glUniform1i(glGetUniformLocation(cullProgram,"cameraCull"),SlopiumConfig.get().cameraCulling?1:0);
            glUniform1i(glGetUniformLocation(cullProgram,"levels"),levels);
            glUniform1i(glGetUniformLocation(cullProgram,"baseShift"),2);
            glDispatchCompute((slots+63)/64,1,1);glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
            timer.stop();ready=true;passes++;state="current-frame GPU mask";
        }catch(RuntimeException e){prepass=false;release();failed=Slopium.configRevision();state="failed: visible";com.mojang.logging.LogUtils.getLogger().error("Slopium section GPU pass disabled until settings change",e);}
        finally{submitNanos+=System.nanoTime()-start;}
    }
    private static void collect(ChunkRenderListIterable lists,CameraTransform camera){
        bases.clear();ids.clear();slots=0;
        var it=lists.iterator(false);while(it.hasNext()){var region=it.next().getRegion();if(!bases.containsKey(region)){bases.put(region,slots);slots+=RenderRegion.REGION_SIZE;}}
        if(slots==0)return;
        ByteBuffer data=staging(slots*32);for(int i=0;i<slots*8;i++)data.putFloat(0);data.flip();
        int cx=SectionPos.blockToSectionCoord(camera.x),cy=SectionPos.blockToSectionCoord(camera.y),cz=SectionPos.blockToSectionCoord(camera.z);
        int radius=Minecraft.getInstance().options.renderDistance().get(),minimum=SectionVisibility.minimum();
        long count=0;
        for(var pair:bases.entrySet()){
            var r=pair.getKey();
            // Both streams are existing Sodium candidates. Flywheel-only sections often
            // have block entities but no terrain triangles; do not lose those candidates.
            var streams=new net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator[]{r.getRenderList().sectionsWithGeometryIterator(false),r.getRenderList().sectionsWithEntitiesIterator()};
            for(var sections:streams){if(sections==null)continue;
            while(sections.hasNext()){
                int i=sections.nextByteAsInt(),id=pair.getValue()+i;
                int x=r.getChunkX()+LocalSectionIndex.unpackX(i),y=r.getChunkY()+LocalSectionIndex.unpackY(i),z=r.getChunkZ()+LocalSectionIndex.unpackZ(i);
                if(ids.putIfAbsent(SectionPos.asLong(x,y,z),id)!=null)continue;count++;
                // Sodium's compressed vertex format permits local coordinates [-8,24].
                // Use that envelope, not an unsafe assumption that all mesh vertices fit [0,16].
                data.putFloat(id*32,(float)(x*16.-8.01-camera.x));data.putFloat(id*32+4,(float)(y*16.-8.01-camera.y));data.putFloat(id*32+8,(float)(z*16.-8.01-camera.z));
                data.putFloat(id*32+12,SectionRange.eligible(x,y,z,cx,cy,cz,minimum,radius)?0:1);
                data.putFloat(id*32+16,(float)(x*16.+24.01-camera.x));data.putFloat(id*32+20,(float)(y*16.+24.01-camera.y));data.putFloat(id*32+24,(float)(z*16.+24.01-camera.z));data.putFloat(id*32+28,1);
            }
        }
        }
        // Open-addressed candidate lookup for Flywheel. Empty slots never represent hidden terrain.
        int size=1;while(size<ids.size()*4+1)size<<=1;hashMask=size-1;
        int[] table=new int[size*4];
        for(var entry:ids.entrySet()){
            long key=entry.getKey();int x=SectionPos.x(key),y=SectionPos.y(key),z=SectionPos.z(key),h=(x*73856093^y*19349663^z*83492791)&hashMask;
            while(table[h*4+3]!=0)h=(h+1)&hashMask;
            table[h*4]=x;table[h*4+1]=y;table[h*4+2]=z;table[h*4+3]=SectionRange.eligible(x,y,z,cx,cy,cz,minimum,radius)?entry.getValue()+1:-1;
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER,keys);glBufferData(GL_SHADER_STORAGE_BUFFER,table,GL_STREAM_DRAW);
        candidates=count;glBindBuffer(GL_SHADER_STORAGE_BUFFER,bounds);glBufferData(GL_SHADER_STORAGE_BUFFER,data,GL_STREAM_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER,mask);glBufferData(GL_SHADER_STORAGE_BUFFER,(long)slots*4,GL_STREAM_DRAW);glClearBufferData(GL_SHADER_STORAGE_BUFFER,GL_R32UI,GL_RED_INTEGER,GL_UNSIGNED_INT,new int[]{0});
    }
    public static boolean draw(CommandList commandsList,GlTessellation tess,MultiDrawBatch batch,RenderRegion region,TerrainRenderPass pass){
        int program=glGetInteger(GL_CURRENT_PROGRAM);
        int[] u=uniforms.computeIfAbsent(program,p->new int[]{glGetUniformLocation(p,"slopiumBase"),glGetUniformLocation(p,"slopiumDepth")});
        glUniform1ui(u[0],bases.getOrDefault(region,0));glUniform1i(u[1],prepass?(SlopiumConfig.get().sectionsAnyBlock?2:1):0);
        if(prepass){RenderSystem.disableBlend();RenderSystem.depthMask(true);RenderSystem.depthFunc(GL_LEQUAL);return false;}
        if(!current()||!bases.containsKey(region)||batch.size==0)return false;
        int savedIndirect=glGetInteger(GL_DRAW_INDIRECT_BUFFER_BINDING),savedSsbo=glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING);
        int s0=glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING,0),s2=glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING,2);
        long o0=glGetInteger64i(GL_SHADER_STORAGE_BUFFER_START,0),n0=glGetInteger64i(GL_SHADER_STORAGE_BUFFER_SIZE,0),o2=glGetInteger64i(GL_SHADER_STORAGE_BUFFER_START,2),n2=glGetInteger64i(GL_SHADER_STORAGE_BUFFER_SIZE,2);
        try{
            var cached=batches.get(batch);
            if(cached==null||cached.base!=bases.get(region)||cached.size!=batch.size){
            dropBatch(batch);
            ByteBuffer data=staging(batch.size*24);
            // Resolve command ownership only from the candidate storage. No world/block traversal.
            var storage=region.getStorage(pass);var iter=region.getRenderList().sectionsWithGeometryIterator(false);
            ArrayList<long[]> ranges=new ArrayList<>();
            while(iter!=null&&iter.hasNext()){int i=iter.nextByteAsInt();long ptr=storage.getDataPointer(i),base=SectionRenderDataUnsafe.getBaseVertex(ptr),length=0;for(int face=0;face<7;face++)length+=SectionRenderDataUnsafe.getVertexCount(ptr,face);if(length>0)ranges.add(new long[]{base,base+length,bases.get(region)+i});}
            ranges.sort(Comparator.comparingLong(a->a[0]));
            for(int i=0;i<batch.size;i++){
                int base=MemoryUtil.memGetInt(batch.pBaseVertex+i*4L),id=-1;
                int lo=0,hi=ranges.size()-1;while(lo<=hi){int m=(lo+hi)>>>1;var r=ranges.get(m);if(base<r[0])hi=m-1;else if(base>=r[1])lo=m+1;else{id=(int)r[2];break;}}
                data.putInt(MemoryUtil.memGetInt(batch.pElementCount+i*4L)).putInt(1).putInt((int)(MemoryUtil.memGetLong(batch.pElementPointer+i*8L)/4)).putInt(base).putInt(0).putInt(id);
            }
            data.flip();int buffer=glGenBuffers();glBindBuffer(GL_SHADER_STORAGE_BUFFER,buffer);glBufferData(GL_SHADER_STORAGE_BUFFER,data,GL_DYNAMIC_DRAW);
            cached=new CachedCommands(buffer,bases.get(region),batch.size);batches.put(batch,cached);
            }
            int indirect=cached.buffer;
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,mask);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,2,indirect);
            glUseProgram(commandProgram);glDispatchCompute((batch.size+63)/64,1,1);glMemoryBarrier(GL_COMMAND_BARRIER_BIT);
            glUseProgram(program);glBindBuffer(GL_DRAW_INDIRECT_BUFFER,indirect);
            try(var draw=commandsList.beginTessellating(tess)){glMultiDrawElementsIndirect(GL_TRIANGLES,GL_UNSIGNED_INT,0L,batch.size,24);}
            commands+=batch.size;return true;
        }finally{glUseProgram(program);glBindBuffer(GL_DRAW_INDIRECT_BUFFER,savedIndirect);restoreBuffer(0,s0,o0,n0);restoreBuffer(2,s2,o2,n2);glBindBuffer(GL_SHADER_STORAGE_BUFFER,savedSsbo);}
    }
    public static boolean matches(org.joml.Matrix4fc view,org.joml.Matrix4fc projection,net.minecraft.world.phys.Vec3 eye){return current()&&capturedView.equals(view)&&capturedProjection.equals(projection)&&eyeX==eye.x&&eyeY==eye.y&&eyeZ==eye.z;}
    public static boolean flywheelWorld(boolean next){boolean old=flywheelWorld;flywheelWorld=next;return old;}
    public static long[] bindFlywheel(){
        int program=glGetInteger(GL_CURRENT_PROGRAM);boolean active=current()&&flywheelWorld&&ProcessedBlocks.all();
        glUniform1i(glGetUniformLocation(program,"slopiumMaskEnabled"),active?1:0);
        if(!active)return null;
        long[] saved={glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING,4),glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING,5),glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING),glGetInteger64i(GL_SHADER_STORAGE_BUFFER_START,4),glGetInteger64i(GL_SHADER_STORAGE_BUFFER_SIZE,4),glGetInteger64i(GL_SHADER_STORAGE_BUFFER_START,5),glGetInteger64i(GL_SHADER_STORAGE_BUFFER_SIZE,5)};
        flywheelDispatches++;
        glUniform1ui(glGetUniformLocation(program,"slopiumHashMask"),hashMask);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER,4,mask);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,5,keys);return saved;
    }
    private static void restoreBuffer(int binding,int name,long start,long size){if(name!=0&&size>0)glBindBufferRange(GL_SHADER_STORAGE_BUFFER,binding,name,start,size);else glBindBufferBase(GL_SHADER_STORAGE_BUFFER,binding,name);}
    public static void restoreFlywheel(long[] saved){if(saved==null)return;restoreBuffer(4,(int)saved[0],saved[3],saved[4]);restoreBuffer(5,(int)saved[1],saved[5],saved[6]);glBindBuffer(GL_SHADER_STORAGE_BUFFER,(int)saved[2]);}
    private static ByteBuffer staging(int bytes){if(staging==null||staging.capacity()<bytes){if(staging!=null)MemoryUtil.memFree(staging);staging=MemoryUtil.memAlloc(Math.max(bytes,65536));}staging.clear();staging.limit(bytes);return staging;}
    private static void texture(int unit,int tex){RenderSystem.activeTexture(GL_TEXTURE0+unit);com.mojang.blaze3d.platform.GlStateManager._bindTexture(tex);}
    private static int program(String name){
        try(var in=GpuSections.class.getResourceAsStream("/assets/slopium/shaders/sections/"+name+".comp")){
            if(in==null)throw new IllegalStateException(name);int s=glCreateShader(GL_COMPUTE_SHADER);glShaderSource(s,new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));glCompileShader(s);
            if(glGetShaderi(s,GL_COMPILE_STATUS)==0)throw new IllegalStateException(name+": "+glGetShaderInfoLog(s));int p=glCreateProgram();glAttachShader(p,s);glLinkProgram(p);glDeleteShader(s);if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(p));return p;
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    private static int tex(int format,int w,int h,int levels){int t=glGenTextures();texture(0,t);glTexStorage2D(GL_TEXTURE_2D,levels,format,w,h);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST_MIPMAP_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);return t;}
    private static void ensure(int w,int h){
        if(baseProgram==0){baseProgram=program("base");reduceProgram=program("reduce");cullProgram=program("cull");commandProgram=program("commands");mask=glGenBuffers();bounds=glGenBuffers();keys=glGenBuffers();timer=new GpuTimer();}
        if(fbo!=0&&width==w&&height==h)return;
        if(fbo!=0){glDeleteFramebuffers(fbo);glDeleteTextures(depth);glDeleteTextures(idTexture);glDeleteTextures(pyramid);}
        width=w;height=h;pw=Math.max(1,(Integer.highestOneBit(w-1)<<1)/4);ph=Math.max(1,(Integer.highestOneBit(h-1)<<1)/4);levels=Math.max(0,29-Integer.numberOfLeadingZeros(Math.max(pw,ph)));
        depth=tex(GL_DEPTH_COMPONENT32F,w,h,1);idTexture=tex(GL_R32UI,w,h,1);pyramid=tex(GL_R32F,pw,ph,levels+1);
        fbo=glGenFramebuffers();glBindFramebuffer(GL_FRAMEBUFFER,fbo);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,depth,0);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT1,GL_TEXTURE_2D,idTexture,0);
        glDrawBuffers(new int[]{GL_NONE,GL_COLOR_ATTACHMENT1});glReadBuffer(GL_COLOR_ATTACHMENT1);
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Section framebuffer incomplete");
    }
    private static final class State implements AutoCloseable {
        final int draw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),read=glGetInteger(GL_READ_FRAMEBUFFER_BINDING),program=glGetInteger(GL_CURRENT_PROGRAM),active=glGetInteger(GL_ACTIVE_TEXTURE),ssbo=glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING);
        final int[] viewport=new int[4],textures=new int[2],samplers=new int[2],buffers=new int[3];
        final boolean depthTest=glIsEnabled(GL_DEPTH_TEST),blend=glIsEnabled(GL_BLEND),cull=glIsEnabled(GL_CULL_FACE),scissor=glIsEnabled(GL_SCISSOR_TEST),depthWrite=glGetBoolean(GL_DEPTH_WRITEMASK);
        final int depthFunc=glGetInteger(GL_DEPTH_FUNC),sr=glGetInteger(GL_BLEND_SRC_RGB),dr=glGetInteger(GL_BLEND_DST_RGB),sa=glGetInteger(GL_BLEND_SRC_ALPHA),da=glGetInteger(GL_BLEND_DST_ALPHA);
        final int[][] images=new int[2][6];
        final long[][] ranges=new long[3][2];
        State(){glGetIntegerv(GL_VIEWPORT,viewport);for(int i=0;i<2;i++){RenderSystem.activeTexture(GL_TEXTURE0+i);textures[i]=glGetInteger(GL_TEXTURE_BINDING_2D);samplers[i]=glGetIntegeri(GL_SAMPLER_BINDING,i);int[] keys={GL_IMAGE_BINDING_NAME,GL_IMAGE_BINDING_LEVEL,GL_IMAGE_BINDING_LAYERED,GL_IMAGE_BINDING_LAYER,GL_IMAGE_BINDING_ACCESS,GL_IMAGE_BINDING_FORMAT};for(int j=0;j<6;j++)images[i][j]=glGetIntegeri(keys[j],i);}for(int i=0;i<3;i++){buffers[i]=glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING,i);ranges[i][0]=glGetInteger64i(GL_SHADER_STORAGE_BUFFER_START,i);ranges[i][1]=glGetInteger64i(GL_SHADER_STORAGE_BUFFER_SIZE,i);}}
        public void close(){glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw);glBindFramebuffer(GL_READ_FRAMEBUFFER,read);RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            for(int i=0;i<2;i++){texture(i,textures[i]);glBindSampler(i,samplers[i]);int[] a=images[i];glBindImageTexture(i,a[0],a[1],a[2]!=0,a[3],a[4],a[5]);}RenderSystem.activeTexture(active);
            for(int i=0;i<3;i++)restoreBuffer(i,buffers[i],ranges[i][0],ranges[i][1]);glBindBuffer(GL_SHADER_STORAGE_BUFFER,ssbo);
            if(depthTest)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            if(scissor)com.mojang.blaze3d.platform.GlStateManager._enableScissorTest();else RenderSystem.disableScissor();RenderSystem.depthMask(depthWrite);RenderSystem.depthFunc(depthFunc);RenderSystem.blendFuncSeparate(sr,dr,sa,da);glUseProgram(program);
        }
    }
}

package dev.slopium.smoke;
import dev.slopium.framegen.*;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL33C.*;

/** Real GPU checks, intentionally outside the distributable JAR. */
final class SmokeFrameGenerationChecks {
    static void screenSpace() {
        try(var state=new GlState();var a=new GlImage(128,128,GL_RGBA8,0);var b=new GlImage(128,128,GL_RGBA8,0);
            var out=new GlImage(128,128,GL_RGBA8,0);var flow=new SimpleInterpolator(128,128,4,true)) {
            float[] clear=new float[4];glGetFloatv(GL_COLOR_CLEAR_VALUE,clear);
            try {
                patch(a,40);patch(b,48);flow.prepare(a,b,new Matrix4f(),new Matrix4f(),16);
                flow.synthesize(a,b,out,.25f,6,null);float quarter=red(out,44);
                flow.synthesize(a,b,out,.75f,6,null);float threeQuarter=red(out,44);
                if(quarter<.8f||threeQuarter>.25f)throw new AssertionError("Screen-space worker shader repeats endpoints: "+quarter+", "+threeQuarter);
                // Movement far beyond the residual search radius: the camera prior must carry it.
                // Sample only the area covered by both camera views; disoccluded borders use fallback.
                patch(a,32);patch(b,80);
                flow.prepare(a,b,new Matrix4f().translation(-96f/128,0,0),new Matrix4f().translation(96f/128,0,0),8);
                for(int phase=1;phase<4;phase++) {
                    flow.synthesize(a,b,out,phase*.25f,6,null);
                    int center=40+12*phase;
                    if(red(out,center)<.8f||red(out,38)>.25f||red(out,90)>.25f)
                        throw new AssertionError("Large camera motion is not a moving intermediate image at phase "+phase+": "+red(out,center)+", "+red(out,38)+", "+red(out,90));
                }
                // Exercise depth-dependent perspective motion, not just a uniform 2D offset.
                try(var da=new GlImage(128,128,GL_RGBA8,GL_DEPTH_COMPONENT32F);
                    var db=new GlImage(128,128,GL_RGBA8,GL_DEPTH_COMPONENT32F)) {
                    patch(da,32);patch(db,80); // depth .5 => clip z=0
                    var backward=new Matrix4f().translation(-96f/128,0,0).m20(.5f);
                    flow.prepare(da,db,backward,new Matrix4f(backward).invert(),8);
                    flow.synthesize(da,db,out,.5f,6,null);
                    if(red(out,64)<.8f||red(out,38)>.25f||red(out,90)>.25f)
                        throw new AssertionError("World depth was ignored in camera motion");
                }
                // Invalid flow must transition over time instead of displaying the new pixel at t=0.
                try(var vectors=new GlImage(128,128,GL_RGBA16F,0);var shader=new GlProgram("async_interpolate")) {
                    vectors.bind();glClearColor(2,2,2,2);glClear(GL_COLOR_BUFFER_BIT);
                    out.bind();shader.use();shader.texture("previousColor",0,a.texture);shader.texture("currentColor",1,b.texture);
                    shader.texture("flow",2,vectors.texture);shader.texture("fsrMiddle",3,b.texture);
                    shader.integer("useFsrMiddle",0);shader.vector("size",128,128);shader.scalar("consistencyPixels",6);
                    for(float fraction:new float[]{0,.25f,.75f,1}) {
                        shader.scalar("fraction",fraction);shader.draw();
                        if(Math.abs(red(out,84)-fraction)>.02f||Math.abs(red(out,36)-(1-fraction))>.02f)
                            throw new AssertionError("Uncertain motion jumps to the endpoint at "+fraction);
                    }
                }
                com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_PARALLEL: large camera motion, world-depth prior and continuous invalid-flow fallback passed");
                if(glGetError()!=GL_NO_ERROR)throw new AssertionError("Screen-space shader GL error");
                com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_PARALLEL: distinct GPU motion phases verified without depth textures");
            }finally{glClearColor(clear[0],clear[1],clear[2],clear[3]);}
        }
    }
    static void run() {
        try(var state=new GlState();var a=new GlImage(128,128,GL_RGBA8,GL_DEPTH_COMPONENT32F);
            var b=new GlImage(128,128,GL_RGBA8,GL_DEPTH_COMPONENT32F);var out=new GlImage(128,128,GL_RGBA8,0);
            var simple=new SimpleInterpolator(128,128,4)) {
            float[] clear=new float[4];glGetFloatv(GL_COLOR_CLEAR_VALUE,clear);
            boolean depthMask=glGetBoolean(GL_DEPTH_WRITEMASK);double depthClear=glGetDouble(GL_DEPTH_CLEAR_VALUE);
            try {
                patch(a,40);patch(b,48);
                simple.generate(a,b,out,new Matrix4f(),new Matrix4f(),16,6);
                float center=red(out,46),old=red(out,41),future=red(out,61);
                if(center<.8f||old>.25f||future>.25f)throw new AssertionError("Simple interpolation is not a translated midpoint: "+center+", "+old+", "+future);
                try(var phaseState=new GlState();var middle=new GlImage(128,128,GL_RGBA8,0)) {
                    simple.synthesize(a,b,middle,.5f,6,null);
                    for(boolean hybrid:new boolean[]{false,true}) {
                        simple.synthesize(a,b,out,.25f,6,hybrid?middle:null);float quarter=red(out,44);
                        simple.synthesize(a,b,out,.75f,6,hybrid?middle:null);float threeQuarter=red(out,44);
                        if(quarter<.8f||threeQuarter>.25f)throw new AssertionError("Temporal phases repeated instead of moving: hybrid="+hybrid+" values="+quarter+","+threeQuarter);
                    }
                    com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: distinct quarter/three-quarter motion positions verified for simple and hybrid synthesis");
                }
                int error=glGetError();if(error!=GL_NO_ERROR)throw new AssertionError("Simple FG GL error "+error);
                com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: simple FG moving patch midpoint passed: center={}, old={}, future={}",center,old,future);
                try(var fsrState=new GlState();var fsr=new FsrInterpolator(128,128)) {
                    var projection=new Matrix4f().perspective((float)Math.toRadians(70),1,.05f,1000);
                    fsr.generate(a,out,new Matrix4f(),projection,true,16.667f,false);
                    fsr.generate(b,out,new Matrix4f(),projection,false,16.667f,false);
                    glFinish();float value=red(out,52);
                    error=glGetError();if(error!=GL_NO_ERROR)throw new AssertionError("FSR FG GL error "+error);
                    if(value<.5f)throw new AssertionError("FSR output is empty: "+value);
                    com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: native FSR shared textures and two real dispatches passed; provider={}, output={}",fsr.provider,value);
                    var motion=new Matrix4f().translation(-16f/128,0,0);
                    for(int i=0;i<5;i++) {patch(b,48+i*4);fsr.generate(b,out,new Matrix4f().translation(-8f/128,0,0),projection,false,16.667f,false);}
                    patch(b,72);fsr.generate(b,out,motion,projection,false,16.667f,false);
                    float midpoint=red(out,70),trailing=red(out,65),leading=red(out,86);
                    com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: FSR motion output midpoint={}, trailing={}, leading={}",midpoint,trailing,leading);
                    if(midpoint<.7f||trailing>.35f||leading>.35f)throw new AssertionError("FSR repeated an endpoint instead of producing a midpoint");
                }
                try(var reprojState=new GlState();var reprojection=new dev.slopium.framegen.reference.Reprojector();var reprojOut=new GlImage(128,128,GL_RGBA8,GL_DEPTH_COMPONENT32F)) {
                    patch(a,40);var projection=new Matrix4f().perspective((float)Math.toRadians(70),1,.05f,1000);
                    reprojection.draw(a,reprojOut.fbo,new Matrix4f(),new Matrix4f().translation(.125f,0,0),net.minecraft.world.phys.Vec3.ZERO,projection,4);
                    if(red(reprojOut,50)<.8f||red(reprojOut,42)>.25f)throw new AssertionError("FrameGen camera reprojection did not move the image");
                    com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: FrameGen camera translation moves pixels; GPU reprojection passed");
                }
                benchmark();
            } finally {glClearColor(clear[0],clear[1],clear[2],clear[3]);glClearDepth(depthClear);glDepthMask(depthMask);}
        }
        int error=glGetError();if(error!=GL_NO_ERROR)throw new AssertionError("FG resource cleanup GL error "+error);
    }
    private static void benchmark() {
        try(var state=new GlState();var a=new GlImage(1280,720,GL_RGBA8,GL_DEPTH_COMPONENT32F);
            var b=new GlImage(1280,720,GL_RGBA8,GL_DEPTH_COMPONENT32F);var out=new GlImage(1280,720,GL_RGBA8,0);
            var simple=new SimpleInterpolator(1280,720,8);var fsr=new FsrInterpolator(1280,720)) {
            patch(a,40);patch(b,48);var identity=new Matrix4f();var projection=new Matrix4f().perspective((float)Math.toRadians(70),1280f/720,.05f,1000);
            int start=glGenQueries(),end=glGenQueries();
            try {
                for(int i=0;i<5;i++)simple.generate(a,b,out,identity,identity,16,4);
                glQueryCounter(start,GL_TIMESTAMP);
                for(int i=0;i<16;i++)simple.generate(a,b,out,identity,identity,16,4);
                glQueryCounter(end,GL_TIMESTAMP);
                double simpleMs=(glGetQueryObjecti64(end,GL_QUERY_RESULT)-glGetQueryObjecti64(start,GL_QUERY_RESULT))/16e6;
                for(int i=0;i<5;i++)fsr.generate((i&1)==0?a:b,out,identity,projection,i==0,16.667f,false);
                glQueryCounter(start,GL_TIMESTAMP);
                for(int i=0;i<16;i++)fsr.generate((i&1)==0?a:b,out,identity,projection,false,16.667f,false);
                glQueryCounter(end,GL_TIMESTAMP);
                double fsrMs=(glGetQueryObjecti64(end,GL_QUERY_RESULT)-glGetQueryObjecti64(start,GL_QUERY_RESULT))/16e6;
                int error=glGetError();if(error!=GL_NO_ERROR)throw new AssertionError("FG benchmark GL error "+error);
                com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: 1280x720 synthetic GPU timing, simple={} ms, FSR={} ms per generated frame (not a world FPS benchmark)",simpleMs,fsrMs);
            }finally{glDeleteQueries(start);glDeleteQueries(end);}
        }
    }
    private static void patch(GlImage image,int x) {
        image.bind();glDisable(GL_SCISSOR_TEST);glColorMask(true,true,true,true);glDepthMask(true);glClearDepth(.5);
        glClearColor(0,0,0,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        glEnable(GL_SCISSOR_TEST);glScissor(x,32,16,64);glClearColor(1,.2f,.1f,1);glClear(GL_COLOR_BUFFER_BIT);glDisable(GL_SCISSOR_TEST);
    }
    private static float red(GlImage image,int x) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER,image.fbo);float[] pixel=new float[4];glReadPixels(x,64,1,1,GL_RGBA,GL_FLOAT,pixel);return pixel[0];
    }
}

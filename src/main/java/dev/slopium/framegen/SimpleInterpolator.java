package dev.slopium.framegen;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL33C.*;

/** Motion stays in GPU textures. No readback, particle bookkeeping or per-object Java traversal. */
public final class SimpleInterpolator implements AutoCloseable {
    private GlProgram flowProgram,interpolation;
    private GlImage flow;
    public SimpleInterpolator(int w,int h,int cell) {
        try {
            flowProgram=new GlProgram("flow");interpolation=new GlProgram("interpolate");
            flow=new GlImage(Math.max(1,(w+cell-1)/cell),Math.max(1,(h+cell-1)/cell),GL_RGBA16F,0);
        }catch(RuntimeException e){close();throw e;}
    }
    public void generate(GlImage previous,GlImage current,GlImage output,Matrix4f currentToPrevious,Matrix4f previousToCurrent,int search,int rejection) {
        prepare(previous,current,currentToPrevious,previousToCurrent,search);
        synthesize(previous,current,output,.5f,rejection,null);
    }
    public void prepare(GlImage previous,GlImage current,Matrix4f currentToPrevious,Matrix4f previousToCurrent,int search) {
        GlState.fullscreen();flow.bind();flowProgram.use();
        flowProgram.texture("previousColor",0,previous.texture);flowProgram.texture("currentColor",1,current.texture);
        flowProgram.texture("previousDepth",2,previous.depth);flowProgram.texture("currentDepth",3,current.depth);
        flowProgram.matrix("currentToPrevious",currentToPrevious);flowProgram.matrix("previousToCurrent",previousToCurrent);
        flowProgram.vector("size",current.width,current.height);flowProgram.scalar("searchPixels",search);flowProgram.draw();
    }
    public void synthesize(GlImage previous,GlImage current,GlImage output,float fraction,int rejection,GlImage fsrMiddle) {
        GlState.fullscreen();
        output.bind();interpolation.use();interpolation.texture("previousColor",0,previous.texture);
        interpolation.texture("currentColor",1,current.texture);interpolation.texture("flow",2,flow.texture);
        interpolation.scalar("fraction",fraction);interpolation.integer("useFsrMiddle",fsrMiddle==null?0:1);
        interpolation.texture("fsrMiddle",3,fsrMiddle==null?current.texture:fsrMiddle.texture);
        interpolation.vector("size",current.width,current.height);interpolation.scalar("consistencyPixels",rejection);interpolation.draw();
    }
    @Override public void close(){if(flow!=null)flow.close();if(flowProgram!=null)flowProgram.close();if(interpolation!=null)interpolation.close();}
}

package dev.slopium.framegen;
import static org.lwjgl.opengl.GL33C.*;
/** Delayed timestamp queries; never wait for a GPU result in the rendering path. */
public final class GpuTimer implements AutoCloseable {
    private final int[] begin=new int[4],end=new int[4];
    private final boolean[] pending=new boolean[4];
    private int slot,recording=-1;
    public long nanos,samples;
    public GpuTimer(){for(int i=0;i<4;i++){begin[i]=glGenQueries();end[i]=glGenQueries();}}
    public void start(){
        recording=-1;
        if(pending[slot]) {
            if(glGetQueryObjecti(end[slot],GL_QUERY_RESULT_AVAILABLE)==0)return;
            nanos+=glGetQueryObjecti64(end[slot],GL_QUERY_RESULT)-glGetQueryObjecti64(begin[slot],GL_QUERY_RESULT);samples++;
            pending[slot]=false;
        }
        recording=slot;glQueryCounter(begin[slot],GL_TIMESTAMP);
    }
    public void stop(){if(recording>=0){glQueryCounter(end[recording],GL_TIMESTAMP);pending[recording]=true;slot=(recording+1)%4;recording=-1;}}
    public double milliseconds(){return samples==0?0:nanos/(double)samples/1e6;}
    @Override public void close(){for(int i=0;i<4;i++){glDeleteQueries(begin[i]);glDeleteQueries(end[i]);}}
}

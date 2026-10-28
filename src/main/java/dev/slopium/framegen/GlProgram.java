package dev.slopium.framegen;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL33C.*;

public final class GlProgram implements AutoCloseable {
    private final int program,vao;
    private final HashMap<String,Integer> uniforms=new HashMap<>();
    private static final String VERTEX="""
        #version 150
        out vec2 uv;
        void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);uv=p;gl_Position=vec4(p*2.0-1.0,0,1);}
        """;
    public GlProgram(String resource) {
        int v=0,f=0,p=0;
        try(var stream=GlProgram.class.getResourceAsStream("/assets/slopium/shaders/framegen/"+resource+".frag")) {
            if(stream==null)throw new IllegalStateException("Missing shader "+resource);
            v=compile(GL_VERTEX_SHADER,VERTEX);f=compile(GL_FRAGMENT_SHADER,new String(stream.readAllBytes(),StandardCharsets.UTF_8));
            p=glCreateProgram();glAttachShader(p,v);glAttachShader(p,f);glBindFragDataLocation(p,0,"color");glLinkProgram(p);
            if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(p));
            program=p;vao=glGenVertexArrays();
        } catch(Exception e){if(p!=0)glDeleteProgram(p);throw new IllegalStateException("Frame-generation shader "+resource,e);}
        finally{if(v!=0)glDeleteShader(v);if(f!=0)glDeleteShader(f);}
    }
    private static int compile(int type,String code) {
        int shader=glCreateShader(type);glShaderSource(shader,code);glCompileShader(shader);
        if(glGetShaderi(shader,GL_COMPILE_STATUS)==0){String log=glGetShaderInfoLog(shader);glDeleteShader(shader);throw new IllegalStateException(log);}return shader;
    }
    private int location(String name){return uniforms.computeIfAbsent(name,n->glGetUniformLocation(program,n));}
    public void use(){glUseProgram(program);glBindVertexArray(vao);}
    public void texture(String name,int unit,int texture){glActiveTexture(GL_TEXTURE0+unit);glBindTexture(GL_TEXTURE_2D,texture);glUniform1i(location(name),unit);}
    public void scalar(String name,float v){glUniform1f(location(name),v);}
    public void integer(String name,int v){glUniform1i(location(name),v);}
    public void vector(String name,float x,float y){glUniform2f(location(name),x,y);}
    public void matrix(String name,Matrix4f v){try(var stack=org.lwjgl.system.MemoryStack.stackPush()){glUniformMatrix4fv(location(name),false,v.get(stack.mallocFloat(16)));}}
    public void draw(){glDrawArrays(GL_TRIANGLES,0,3);}
    @Override public void close(){glDeleteProgram(program);glDeleteVertexArrays(vao);}
}

package dev.slopium.framegen;
import static org.lwjgl.opengl.GL33C.*;
/** Premultiplied UI is never sampled by motion estimation or temporal synthesis. */
public final class HudCompositor implements AutoCloseable {
    private final GlProgram program=new GlProgram("hud_composite");
    public void draw(int world,int hud,int destination,int width,int height){
        GlState.fullscreen();glBindFramebuffer(GL_DRAW_FRAMEBUFFER,destination);glViewport(0,0,width,height);
        program.use();program.texture("world",0,world);program.texture("hud",1,hud);program.draw();
    }
    public void close(){program.close();}
}

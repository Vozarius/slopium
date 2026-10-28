/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Adapted from FrameGen's FrameGenRenderer, Copyright (C) 2026 Screamix.
 * Slopium adaptation (2026): caller-owned history, exact GL-state restoration,
 * no duplicate captures, no GL-state-cache mutation, retained reprojected depth.
 * See licenses/FrameGen-COPYING and FrameGen-COPYING.LESSER.
 */
package dev.slopium.framegen.reference;

import dev.slopium.framegen.GlImage;
import dev.slopium.framegen.GlState;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;
import static org.lwjgl.opengl.GL33C.*;

/** FrameGen's two-pass depth reprojection, with Slopium resource ownership. */
public final class Reprojector implements AutoCloseable {
    private GlProgram background,grid;
    private int vao;
    public Reprojector() {
        try {
            background=GlProgram.compile("FrameGen/background",Shaders.FULLSCREEN_VS,Shaders.BACKGROUND_FS,"outColor");
            grid=GlProgram.compile("FrameGen/grid",Shaders.WARP_VS,Shaders.WARP_FS,"outColor");
            vao=glGenVertexArrays();
        } catch(RuntimeException e){close();throw e;}
    }
    public void draw(GlImage history,int destination,Matrix4f oldVp,Matrix4f newVp,Vec3 delta,Matrix4f projection,int step) {
        GlState.fullscreen();glBindFramebuffer(GL_DRAW_FRAMEBUFFER,destination);glViewport(0,0,history.width,history.height);
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,history.texture);
        glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,history.depth);glBindVertexArray(vao);
        Matrix4f oldInv=new Matrix4f(oldVp).invert();
        boolean unchanged=delta.lengthSqr()<1e-12&&oldVp.equals(newVp,1e-6f);
        float near=Math.abs(projection.m32()/(projection.m22()-1)),far=Math.abs(projection.m32()/(projection.m22()+1));
        if(!Float.isFinite(near)||near<=0)near=.05f;
        if(!Float.isFinite(far)||far<=near)far=10000;
        glDepthMask(false);background.use();
        common(background,oldInv,newVp,delta,near,far);
        background.setMatrix("uPrevVP",oldVp);background.setMatrix("uNewInvVP",new Matrix4f(newVp).invert());
        background.setFloat("uPassthrough",unchanged?1:0);glDrawArrays(GL_TRIANGLES,0,3);
        glDepthMask(true);
        if(unchanged) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER,history.fbo);
            glBlitFramebuffer(0,0,history.width,history.height,0,0,history.width,history.height,GL_DEPTH_BUFFER_BIT,GL_NEAREST);
            return;
        }
        glClearDepth(1);glClear(GL_DEPTH_BUFFER_BIT);glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LESS);
        grid.use();common(grid,oldInv,newVp,delta,near,far);
        int x=(history.width+step-1)/step,y=(history.height+step-1)/step;
        grid.setIVec2("uGrid",x,y);grid.setFloat("uEdgeThreshold",.15f);grid.setFloat("uTint",0);
        grid.setFloat("uSharpen",.15f);grid.setVec2("uTexel",1f/history.width,1f/history.height);
        glDrawArraysInstanced(GL_TRIANGLES,0,x*6,y);
    }
    private static void common(GlProgram program,Matrix4f oldInv,Matrix4f vp,Vec3 delta,float near,float far) {
        program.setInt("uHistColor",0);program.setInt("uHistDepth",1);
        program.setMatrix("uPrevInvVP",oldInv);program.setMatrix("uNewVP",vp);
        program.setVec3("uDeltaPos",(float)delta.x,(float)delta.y,(float)delta.z);
        program.setVec2("uNearFar",near,far);program.setFloat("uNearLock",0);program.setFloat("uDebugDepth",0);
    }
    public void close(){if(background!=null)background.close();if(grid!=null)grid.close();if(vao!=0)glDeleteVertexArrays(vao);}
}

package dev.slopium.smoke;

import dev.slopium.visibility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

final class SmokeSectionChecks {
    private static class Wall implements BlockGetter {
        BlockState state=Blocks.STONE.defaultBlockState();boolean hole;int axis,plane=4;
        public BlockState getBlockState(BlockPos p){return (axis==0?p.getX():axis==1?p.getY():p.getZ())==plane&&!(hole&&p.getY()==0&&p.getZ()==1)?state:Blocks.AIR.defaultBlockState();}
        public net.minecraft.world.level.material.FluidState getFluidState(BlockPos p){return getBlockState(p).getFluidState();}
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos p){return null;}
        public int getHeight(){return 384;}public int getMinBuildHeight(){return -64;}
    }
    static void run(){
        var wall=new Wall();var target=new AABB(32,-8,-8,48,8,8);var eye=new Vec3(.5,.5,.5);var witness=new BlockPos(4,0,0);
        java.util.function.BooleanSupplier yes=()->true;
        if(!SectionOcclusion.hidden(wall,target,eye,witness,false,yes))throw new AssertionError("Solid wall failed volume proof");
        wall.hole=true;if(SectionOcclusion.hidden(wall,target,eye,witness,false,yes))throw new AssertionError("Wall hole hidden by point/corner approximation");wall.hole=false;
        wall.state=Blocks.GLASS.defaultBlockState();
        if(SectionOcclusion.hidden(wall,target,eye,witness,false,yes)||!SectionOcclusion.hidden(wall,target,eye,witness,true,yes))throw new AssertionError("Glass mode mismatch");
        wall.state=Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH,true).setValue(IronBarsBlock.SOUTH,true);
        if(!SectionOcclusion.hidden(wall,target,eye,witness,true,yes))throw new AssertionError("Thin connected pane failed");
        wall.state=Blocks.GLASS_PANE.defaultBlockState();
        if(SectionOcclusion.hidden(wall,target,eye,witness,true,yes))throw new AssertionError("Pane gaps filled");
        wall.state=Blocks.OAK_SLAB.defaultBlockState();if(SectionOcclusion.hidden(wall,target,eye,witness,true,yes))throw new AssertionError("Slab gaps filled");
        wall.state=Blocks.STONE.defaultBlockState();
        if(SectionOcclusion.hidden(wall,target,eye,witness,false,()->false))throw new AssertionError("Exhausted budget hid section");
        if(SectionOcclusion.hidden(wall,target,eye,witness,false,yes,p->false))throw new AssertionError("Unuploaded wall hid section");
        if(SectionOcclusion.hidden(wall,new AABB(0,0,0,16,16,16),eye,witness,false,yes))throw new AssertionError("Camera section hidden");
        if(!SectionOcclusion.hidden(wall,new AABB(496,-8,-8,512,8,8),eye,witness,false,yes))throw new AssertionError("32-chunk distance lost");
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}){
            wall.axis=axis;wall.plane=4*sign;double lo=sign>0?32:-48,hi=sign>0?48:-32;
            var box=axis==0?new AABB(lo,-8,-8,hi,8,8):axis==1?new AABB(-8,lo,-8,8,hi,8):new AABB(-8,-8,lo,8,8,hi);
            var hit=axis==0?new BlockPos(wall.plane,0,0):axis==1?new BlockPos(0,wall.plane,0):new BlockPos(0,0,wall.plane);
            if(!SectionOcclusion.hidden(wall,box,eye,hit,false,yes))throw new AssertionError("Wall proof axis/sign "+axis+"/"+sign);
        }
        wall.axis=0;wall.plane=4;
        long start=System.nanoTime();int[] cells={0};
        for(int i=0;i<1000;i++)SectionOcclusion.hidden(wall,target,eye,witness,false,()->{cells[0]++;return true;});
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SECTIONS: full-volume solid/glass/pane, holes, partial shapes, camera, upload/budget fail-open and 512-block range passed; 1000 uncached proofs {} ms, {} cell/candidate visits",(System.nanoTime()-start)/1e6,cells[0]);
    }
}

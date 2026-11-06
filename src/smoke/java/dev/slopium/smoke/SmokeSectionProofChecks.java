package dev.slopium.smoke;

import dev.slopium.visibility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Regression: a window assembled from planes at different depths, not a flat infinite wall. */
final class SmokeSectionProofChecks {
    private static final class Window implements BlockGetter {
        final BlockState pane;
        boolean hole,corner;
        Window(){
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:weathered_iron_window_pane"));
            if(block==Blocks.AIR)throw new AssertionError("Real Create window missing");
            pane=block.defaultBlockState().setValue(IronBarsBlock.NORTH,true).setValue(IronBarsBlock.SOUTH,true);
        }
        public BlockState getBlockState(BlockPos p){
            if(corner){
                if(p.getX()==4&&p.getZ()==4)return pane.setValue(IronBarsBlock.WEST,true).setValue(IronBarsBlock.EAST,true);
                if(p.getX()==4&&p.getZ()<=4)return pane;
                if(p.getZ()==4&&p.getX()<=4)return pane.setValue(IronBarsBlock.NORTH,false).setValue(IronBarsBlock.SOUTH,false).setValue(IronBarsBlock.WEST,true).setValue(IronBarsBlock.EAST,true);
                return Blocks.AIR.defaultBlockState();
            }
            boolean front=p.getX()==4&&p.getZ()<=0,back=p.getX()==5&&p.getZ()>=0;
            if(hole&&p.getZ()==1&&p.getY()==0)back=false;
            return front||back?pane:Blocks.AIR.defaultBlockState();
        }
        public net.minecraft.world.level.material.FluidState getFluidState(BlockPos p){return getBlockState(p).getFluidState();}
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos p){return null;}
        public int getHeight(){return 384;}public int getMinBuildHeight(){return -64;}
    }
    static void run(){
        var world=new Window();var eye=new Vec3(.5,.5,.5);var box=new AABB(32,-8,-8,48,8,8);
        if(SectionOcclusion.hidden(world,box,eye,new BlockPos(4,0,0),true,()->true))throw new AssertionError("Fixture must reproduce the old single-plane miss");
        var proof=new SectionProof(world,box,eye,true,b->false,p->{});
        int steps=0;SectionProof.Result result;
        do{int[] budget={90};result=proof.step(()->--budget[0]>0,p->true);if(++steps>300)throw new AssertionError("Proof did not resume: "+proof.reason());}while(result==SectionProof.Result.PENDING);
        if(result!=SectionProof.Result.HIDDEN)throw new AssertionError("Staggered Create windows remain visible: "+proof.reason());
        if(steps<2)throw new AssertionError("Budget resume was not exercised");
        proof=new SectionProof(world,box,eye,false,b->false,p->{});
        if(proof.step(()->true,p->true)!=SectionProof.Result.VISIBLE)throw new AssertionError("Opaque mode hid through glass");
        world.hole=true;
        proof=new SectionProof(world,box,eye,true,b->false,p->{});
        if(proof.step(()->true,p->true)!=SectionProof.Result.VISIBLE)throw new AssertionError("Real opening was filled");
        proof=new SectionProof(world,box,eye,true,b->b.minZ>=2,p->{});
        if(proof.step(()->true,p->true)!=SectionProof.Result.HIDDEN||!proof.viewDependent())throw new AssertionError("Opening outside the view prevented culling");
        proof=new SectionProof(world,new AABB(4,-8,-8,20,8,8),eye,true,b->false,p->{});
        if(proof.step(()->true,p->true)!=SectionProof.Result.VISIBLE)throw new AssertionError("Section containing the window was hidden");
        proof=new SectionProof(world,box,eye,true,b->true,p->{});
        if(proof.step(()->true,p->true)!=SectionProof.Result.HIDDEN||!proof.viewDependent())throw new AssertionError("Frustum result not marked view-dependent");
        world.hole=false;
        proof=new SectionProof(world,box,eye,true,b->false,p->{});
        if(proof.step(()->true,p->false)==SectionProof.Result.HIDDEN)throw new AssertionError("Unbuilt window used for culling");
        world.corner=true;
        proof=new SectionProof(world,new AABB(16,-8,16,32,8,32),eye,true,b->false,p->{});
        if(proof.step(()->true,p->true)!=SectionProof.Result.HIDDEN)throw new AssertionError("Two perpendicular Create window walls failed: "+proof.reason());
        com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SECTION_PROOF: actual Create pane, staggered walls, opaque/any, opening, frustum, mesh readiness and resumable budget passed ({} slices)",steps);
    }
}

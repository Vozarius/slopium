package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdaptiveFrameTargetTest {
    @Test void targetSixtySupportsThirtyAndOneWithoutManualMultiplierCeiling(){
        assertEquals(2,AdaptiveFrameTarget.framesPerSource(33_333_333,60));
        assertEquals(60,AdaptiveFrameTarget.framesPerSource(1_000_000_000,60));
        assertTrue(AdaptiveFrameTarget.needsGeneration(1_000_000_000,60));
        assertFalse(AdaptiveFrameTarget.needsGeneration(16_666_667,60));
        assertFalse(AdaptiveFrameTarget.needsGeneration(8_333_333,60));
    }
    @Test void sourceRecoveryDisablesGenerationWithoutCountingSyntheticFrames(){
        var rate=new AdaptiveFrameTarget();rate.source(1);rate.source(1_000_000_001);
        assertTrue(rate.needed(60));long now=1_000_000_001;
        for(int i=0;i<40;i++)rate.source(now+=8_333_333);
        assertFalse(rate.needed(60));
        for(int i=0;i<4;i++)rate.source(now+=33_333_333);
        assertTrue(rate.needed(60));
    }
    @Test void oneFpsPairContainsFiftyNineIntermediateFramesOnSixtyHzGrid(){
        var t=new InterpolationTimeline();t.begin(0,1_000_000_000,true);
        int generated=0,outputs=0;
        for(long now=0;now<1_000_000_000;now+=16_666_667){
            int phase=t.phase(now,AdaptiveFrameTarget.PHASES);
            if(t.needsPresent(phase)){outputs++;if(phase>0)generated++;t.presented(phase,AdaptiveFrameTarget.PHASES);}
        }
        assertEquals(60,outputs);assertEquals(59,generated);
    }
    @Test void nonIntegerRatioDoesNotRoundOutputUpToNinety(){
        assertEquals(2,AdaptiveFrameTarget.framesPerSource(22_222_222,60));
        long deadline=0;int outputs=0;
        // 45 real FPS needs 15 synthetic per second, not a fixed x2 output of 90.
        for(long now=0;now<1_000_000_000;now+=100_000)if(now>=deadline){
            outputs++;deadline=AdaptiveFrameTarget.nextDeadline(deadline,now,60);
        }
        assertEquals(60,outputs);assertEquals(60,AdaptiveFrameTarget.cap(240,60));
        assertEquals(30,AdaptiveFrameTarget.cap(30,60));
    }
    @Test void missedDeadlinesDoNotCreateCatchUpBurst(){
        long next=AdaptiveFrameTarget.nextDeadline(16_666_667,1_000_000_000,60);
        assertTrue(next>1_000_000_000);assertEquals(60,AdaptiveFrameTarget.target(0));
    }
}

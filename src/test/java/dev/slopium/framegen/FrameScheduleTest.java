package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FrameScheduleTest {
    @Test void x32At240OutputDoesNotDivideThirtyRealFpsByMultiplier() {
        var s=new FrameSchedule();s.configure(30);int real=0,synthetic=0;
        for(long now=1;now<1_000_000_001L;now+=4_166_667L) {
            s.begin(now);
            if(s.due(now)){real++;s.captured(now);}
            else if(s.canGenerate(now,32)){s.generated(1_000_000);synthetic++;}
        }
        assertEquals(30,real);assertTrue(synthetic>100);
    }
    @Test void idealHeadroomCanSupplyThirtyTimesThirtyTwo() {
        var s=new FrameSchedule();s.configure(30);int real=0,synthetic=0;
        for(long now=1;now<1_000_000_001L;now+=1_041_667L) {
            s.begin(now);
            if(s.due(now)){real++;s.captured(now);}
            else if(s.canGenerate(now,32)){s.generated(1_000_000);synthetic++;}
        }
        assertEquals(30,real);assertTrue(synthetic>=900);
    }
    @Test void overloadRendersWorldInsteadOfDrainingSyntheticQueue() {
        var s=new FrameSchedule();s.configure(30);
        for(long now=1;now<1_000_000_001L;now+=40_000_000) {
            s.begin(now);assertTrue(s.due(now));s.captured(now);
            assertFalse(s.canGenerate(now+39_000_000,32));
        }
    }
    @Test void automaticBaselineMeasuresOrdinaryFrameIntervalsAndReprobes() {
        var s=new FrameSchedule();s.configure(0);
        for(int i=0;i<8;i++){s.begin(1+i*33_333_333L);assertTrue(s.probing());}
        s.begin(1+8*33_333_333L);assertFalse(s.probing());assertEquals(30,s.targetFps());
        s.captured(1+8*33_333_333L);
        s.begin(1+9*33_333_333L);assertTrue(s.due(1+9*33_333_333L));
        s.begin(6_000_000_000L);assertTrue(s.probing());
        for(int i=1;i<=8;i++)s.begin(6_000_000_000L+i*50_000_000L);
        assertFalse(s.probing());assertEquals(20,s.targetFps());
    }
    @Test void costlyGenerationIsSkippedAndStallsDoNotAccumulateWork() {
        var s=new FrameSchedule();s.configure(30);s.begin(1);s.captured(1);s.generated(20_000_000);
        s.begin(20_000_001);assertFalse(s.canGenerate(20_000_001,32));
        assertTrue(s.due(10_000_000_000L));s.captured(10_000_000_000L);
        assertFalse(s.due(10_000_000_001L));s.configure(60);assertTrue(s.due(10_000_000_001L));
    }
    @Test void automaticModeDoesNotIntroduceAThousandFpsSourceCeiling() {
        var s=new FrameSchedule();s.configure(0);
        for(int i=0;i<=8;i++)s.begin(1+i*500_000L);
        assertEquals(2000,s.targetFps());
        assertEquals(64000,InputFrameLimiter.outputLimit(260,0,s.targetFps(),32));
    }
    @Test void x2OutputPacingIsNotMistakenForExpensiveGeneration() {
        var s=new FrameSchedule();s.configure(30);int real=0,synthetic=0;
        for(long now=1;now<1_000_000_001L;now+=16_666_667) {
            s.begin(now);
            if(s.due(now)){s.captured(now);real++;}
            else if(s.canGenerate(now,2)){s.generated(300_000);synthetic++;}
        }
        assertEquals(30,real);assertEquals(30,synthetic);
    }
    @Test void coldShaderCostDoesNotPermanentlyDisableGeneration() {
        var s=new FrameSchedule();s.configure(30);s.begin(1);s.captured(1);s.generated(100_000_000);
        assertFalse(s.canGenerate(2,32));
        for(int i=1;i<=32;i++)s.captured(1+i*33_333_334L);
        assertTrue(s.canGenerate(1+32*33_333_334L+1_000_000,32));
    }
}

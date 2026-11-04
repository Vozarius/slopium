package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ImmediateTimelineTest {
    @Test void newSourceInterruptsPredictionWithoutFinishingOldPair(){
        var t=new ImmediateTimeline();t.source(0,1_000_000_000);assertEquals(0,t.phase(0,60));
        t.presented(20);t.source(340_000_000,340_000_000);
        assertEquals(0,t.phase(340_000_000,60));assertTrue(t.needs(t.phase(350_000_000,60),60));
    }
    @Test void boundedPredictionDoesNotInventUnlimitedMotionDuringStall(){
        var t=new ImmediateTimeline();t.source(0,1_000_000_000);
        assertTrue(t.needs(59,60));t.presented(59);assertFalse(t.needs(59,60));
        assertFalse(t.needs(t.phase(20_000_000_000L,60),60));
    }
}

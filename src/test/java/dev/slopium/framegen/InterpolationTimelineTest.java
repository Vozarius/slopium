package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InterpolationTimelineTest {
    @Test void incomingFramesCannotTruncateVisibleMotion() {
        var t=new InterpolationTimeline();t.begin(0,40_000_000,true);
        for(int phase=0;phase<=8;phase++) {
            assertEquals(phase,t.phase(phase*5_000_000L,8));
            assertTrue(t.needsPresent(phase));t.presented(phase,8);
            assertFalse(t.needsPresent(phase));
            if(phase<8)assertThrows(IllegalStateException.class,()->t.begin(20_000_000,40_000_000,true));
        }
        assertTrue(t.readyForNext());
        t.begin(41_000_000,40_000_000,true);
        assertFalse(t.needsPresent(0)); // previous endpoint is this pair's first image
        assertEquals(1,t.phase(45_000_000,8)); // no accumulated one-ms scheduling drift
    }
    @Test void lateArrivalStartsAtOldImageInsteadOfCatchingUpInOneJump() {
        var t=new InterpolationTimeline();t.begin(0,40_000_000,true);t.presented(8,8);
        t.begin(200_000_000,40_000_000,true);
        assertEquals(0,t.phase(200_000_000,8));assertEquals(4,t.phase(220_000_000,8));
    }
    @Test void onlyExplicitCutDiscardsUnfinishedPair() {
        var t=new InterpolationTimeline();t.begin(0,40_000_000,true);t.presented(2,8);
        t.cut();t.begin(10_000_000,40_000_000,false);
        assertEquals(8,t.phase(10_000_000,8));t.presented(8,8);assertTrue(t.readyForNext());
        t.begin(20_000_000,40_000_000,true);assertEquals(0,t.phase(20_000_000,8));
    }
}

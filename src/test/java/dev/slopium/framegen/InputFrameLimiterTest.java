package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputFrameLimiterTest {
    @Test void capsActualCapturesAtAllSupportedRates() {
        for(int fps:new int[]{1,30,60,75,120,240}) {
            var limiter=new InputFrameLimiter();limiter.configure(fps);int count=0;
            for(long now=0;now<1_000_000_000L;now+=100_000L)if(limiter.ready(now)){limiter.captured(now);count++;}
            assertEquals(fps,count,"rate "+fps);
        }
    }
    @Test void missedPeriodsDoNotProduceCatchUpBurst() {
        var l=new InputFrameLimiter();l.configure(60);l.captured(0);
        assertTrue(l.ready(10_000_000_000L));l.captured(10_000_000_000L);
        assertFalse(l.ready(10_000_000_001L));
    }
    @Test void jitterKeepsLongTermRateAndChangesResetSchedule() {
        var l=new InputFrameLimiter();l.configure(60);l.captured(0);
        assertFalse(l.ready(16_000_000));assertTrue(l.ready(17_000_000));l.captured(17_000_000);
        assertTrue(l.ready(33_500_000));l.configure(30);assertTrue(l.ready(33_500_000));l.captured(33_500_000);
        assertFalse(l.ready(40_000_000));l.reset();assertTrue(l.ready(40_000_000));
        l.configure(0);l.captured(40_000_000);assertTrue(l.ready(40_000_001));
    }
    @Test void outputCombinesInputMultiplierAndBothCapsIncludingAboveVanillaSentinel() {
        assertEquals(240,InputFrameLimiter.outputLimit(260,0,60,4));
        assertEquals(480,InputFrameLimiter.outputLimit(260,0,120,4));
        assertEquals(7680,InputFrameLimiter.outputLimit(260,0,240,32));
        assertEquals(180,InputFrameLimiter.outputLimit(260,180,120,4));
        assertEquals(60,InputFrameLimiter.outputLimit(60,180,120,4));
        assertEquals(0,InputFrameLimiter.outputLimit(260,0,0,4));
        assertEquals(120,InputFrameLimiter.outputLimit(260,120,0,4));
    }
}

package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FrameCadenceTest {
    @Test void timeSelectsDistinctOrderedFractionsForAllMultipliers() {
        for(int n=2;n<=32;n++) {
            var c=new FrameCadence();c.configure(n);
            assertFalse(c.captured());assertFalse(c.sample(50,100));assertTrue(c.captured());
            for(int k=1;k<=n;k++) {
                assertTrue(c.sample(k*100,n*100));assertEquals(k/(float)n,c.fraction());
                assertFalse(c.sample(k*100,n*100));assertEquals(k==n,c.endpoint());assertEquals(k*2==n,c.midpoint());
            }
        }
    }
    @Test void skippedFractionsNeverBlockANewRealFrame() {
        var c=new FrameCadence();c.configure(32);c.captured();c.captured();
        assertTrue(c.sample(16,32));assertEquals(.5f,c.fraction());
        assertTrue(c.captured());assertEquals(0,c.fraction());
        assertTrue(c.sample(24,32));assertEquals(.75f,c.fraction());
        assertFalse(c.sample(8,32));
    }
    @Test void cameraCutAndMultiplierChangeInvalidateHistory() {
        var c=new FrameCadence();c.captured();c.captured();c.reset();assertFalse(c.captured());
        c.configure(99);assertEquals(32,c.multiplier());assertFalse(c.captured());
        c.configure(-1);assertEquals(2,c.multiplier());assertFalse(c.captured());
    }
}

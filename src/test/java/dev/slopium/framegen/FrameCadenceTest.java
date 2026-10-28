package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FrameCadenceTest {
    @Test void everyMultiplierPresentsOrderedDistinctPhases() {
        for(int n=2;n<=32;n++) {
            var c=new FrameCadence();c.configure(n);
            assertFalse(c.captured());assertTrue(c.needsWorld());
            for(int pair=0;pair<3;pair++) {
                assertTrue(c.captured());
                for(int k=1;k<=n;k++) {
                    assertFalse(c.needsWorld());assertEquals(k/(float)n,c.fraction());
                    assertEquals(k==n,c.endpoint());assertEquals(2*k==n,c.midpoint());c.presented();
                }
                assertTrue(c.needsWorld());
            }
        }
    }
    @Test void cameraCutDiscardsPendingSyntheticPair() {
        var cadence=new FrameCadence();cadence.captured();cadence.captured();assertFalse(cadence.needsWorld());
        cadence.reset();assertTrue(cadence.needsWorld());assertFalse(cadence.captured());
    }
    @Test void multiplierChangeInvalidatesHistoryAndClampsInvalidValues() {
        var c=new FrameCadence();c.captured();c.captured();c.configure(4);
        assertTrue(c.needsWorld());assertFalse(c.captured());
        c.configure(99);assertEquals(32,c.multiplier());c.configure(-1);assertEquals(2,c.multiplier());
    }
}

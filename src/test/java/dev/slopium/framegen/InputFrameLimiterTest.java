package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputFrameLimiterTest {
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

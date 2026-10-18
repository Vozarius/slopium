package dev.slopium;
import dev.slopium.visibility.VisualFrameGate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VisualFrameGateTest {
    @Test void boundedAndReentry() {
        var gate = new VisualFrameGate();
        assertFalse(gate.skip(100, true, 3));
        assertTrue(gate.skip(101, true, 3));
        assertTrue(gate.skip(102, true, 3));
        assertFalse(gate.skip(103, true, 3));
        assertFalse(gate.skip(104, false, 3));
        assertTrue(gate.skip(105, true, 3));
        assertFalse(gate.skip(106, false, 3));
    }
    @Test void resetAndNoStarvation() {
        var gate = new VisualFrameGate();
        assertFalse(gate.skip(99, true, 10));
        assertFalse(gate.skip(1, true, 10));
        int updates = 0;
        for (int i = 2; i <= 101; i++) if (!gate.skip(i, true, 10)) updates++;
        assertEquals(10, updates);
        assertFalse(gate.skip(102, true, 0));
    }
}

package dev.slopium.visibility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TickGateTest {
    @Test void resetHiddenRestartsGraceAndPreservesPacketWake() {
        var gate = new TickGate();
        gate.skip(0, true, 20, 4, 0);
        assertTrue(gate.skip(21, true, 20, 4, 0));
        gate.resetHidden();
        assertFalse(gate.skip(22, true, 20, 4, 0));
        assertFalse(gate.skip(40, true, 20, 4, 0));
        gate.wake(50, 100);
        gate.resetHidden();
        assertFalse(gate.skip(99, true, 20, 4, 0));
        assertFalse(gate.skip(149, true, 20, 4, 0));
    }
    @Test void grantsInitialGraceAndStaggersHiddenTicks() {
        var gate = new TickGate();
        for (int t = 0; t < 20; t++) assertFalse(gate.skip(t, true, 20, 4, 0));
        int skipped = 0;
        for (int t = 20; t < 40; t++) if (gate.skip(t, true, 20, 4, 0)) skipped++;
        assertEquals(15, skipped);
    }

    @Test void becomingVisibleImmediatelyRestoresFullRateAndResetsGrace() {
        var gate = new TickGate();
        gate.skip(0, true, 20, 4, 0);
        assertTrue(gate.skip(21, true, 20, 4, 0));
        assertFalse(gate.skip(22, false, 20, 4, 0));
        assertFalse(gate.skip(23, true, 20, 4, 0));
        assertFalse(gate.skip(40, true, 20, 4, 0));
    }

    @Test void packetsForceWakeAndServerTimeResetDoesNotFreezeObject() {
        var gate = new TickGate();
        gate.skip(0, true, 20, 4, 0);
        gate.wake(25, 20);
        for (int t = 25; t < 45; t++) assertFalse(gate.skip(t, true, 20, 4, 0));
        assertFalse(gate.skip(0, true, 20, 4, 0));
    }
}

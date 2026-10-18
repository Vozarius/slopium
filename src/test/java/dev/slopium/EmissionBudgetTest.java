package dev.slopium;
import dev.slopium.visibility.EmissionBudget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EmissionBudgetTest {
    @Test void boundedAndReset() {
        var budget = new EmissionBudget();
        assertTrue(budget.allow(2)); assertTrue(budget.allow(2));
        assertFalse(budget.allow(2)); assertFalse(budget.allow(2));
        budget.reset(); assertTrue(budget.allow(2));
    }
    @Test void zeroAndChangedLimit() {
        var budget = new EmissionBudget();
        assertFalse(budget.allow(0)); assertFalse(budget.allow(-1));
        assertTrue(budget.allow(1)); assertFalse(budget.allow(1));
        assertTrue(budget.allow(2)); assertFalse(budget.allow(2));
    }
}

package dev.slopium;
import dev.slopium.visibility.CullingWorkBudget;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
class CullingWorkBudgetTest {
    @Test void sharedFrameAndTimeAreBounded() {
        var time=new AtomicLong();var b=new CullingWorkBudget(100,10,time::get);
        b.begin(1);assertTrue(b.take());time.set(70);b.end();
        time.set(1000);b.begin(1);assertTrue(b.take());time.set(1030);assertFalse(b.take());b.end();
        b.begin(2);assertTrue(b.take());b.end();
    }
    @Test void operationLimitAndOverrunDoNotGrantExtraWork() {
        var time=new AtomicLong();var b=new CullingWorkBudget(100,2,time::get);
        b.begin(1);assertTrue(b.take());assertTrue(b.take());assertFalse(b.take());b.end();
        b.begin(1);assertFalse(b.take());b.end();
        b.begin(2);assertTrue(b.take());time.set(250);b.end();
        b.begin(2);assertFalse(b.take());b.end();
        b.begin(3);assertTrue(b.take());
    }
}

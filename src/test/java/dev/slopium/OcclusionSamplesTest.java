package dev.slopium;
import dev.slopium.visibility.OcclusionSamples;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OcclusionSamplesTest {
    @Test void openCenterStopsImmediately() {
        int[] calls={0};
        assertFalse(OcclusionSamples.hidden(0,0,0,1,1,1,(x,y,z)->{calls[0]++;return false;}));
        assertEquals(1,calls[0]);
    }
    @Test void requiresCenterAndEveryCorner() {
        int[] calls={0};
        assertTrue(OcclusionSamples.hidden(0,0,0,1,1,1,(x,y,z)->{calls[0]++;return true;}));
        assertEquals(9,calls[0]);
        for(int visible=0;visible<9;visible++) {
            int index=visible;calls[0]=0;
            assertFalse(OcclusionSamples.hidden(0,0,0,1,1,1,(x,y,z)->calls[0]++!=index));
        }
    }
    @Test void samplesExpandedExtents() {
        var points=new java.util.HashSet<String>();
        OcclusionSamples.hidden(-2,-4,-6,2,4,6,(x,y,z)->{points.add(x+","+y+","+z);return true;});
        assertEquals(9,points.size());assertTrue(points.contains("0.0,0.0,0.0"));
        assertTrue(points.contains("-2.0,-4.0,-6.0"));assertTrue(points.contains("2.0,4.0,6.0"));
    }
}

package dev.slopium.framegen;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RecentFrameRateTest {
    @Test void thirtySourceAndSixtySwapsAreDifferentAndNotLifetimeAverages(){
        var source=new RecentFrameRate();var output=new RecentFrameRate();
        for(int i=0;i<=600;i++){long now=i*16_666_667L;output.event(now);if(i%2==0)source.event(now);}
        assertEquals(30,source.fps(10_000_000_200L),.01);assertEquals(60,output.fps(10_000_000_200L),.01);
        source.event(10_500_000_200L);source.event(11_000_000_200L);
        assertEquals(2,source.fps(11_000_000_200L),.01);
    }
    @Test void noEventsEventuallyMeansZeroAndFirstEventDoesNotInventFps(){
        var r=new RecentFrameRate();r.event(1);assertEquals(0,r.fps(1));
        r.event(1_000_000_001L);assertEquals(1,r.fps(1_000_000_001L));
        assertEquals(0,r.fps(3_000_000_002L));assertEquals(2,r.total);
    }
}

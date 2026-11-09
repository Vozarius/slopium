package dev.slopium.benchmark;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class GcAccountingTest {
    private AnalysisRuntime.GcCounter gc(String name,long count,long ms){return new AnalysisRuntime.GcCounter(count,ms,AnalysisRuntime.gcKind(name));}
    @Test void concurrentZgcCycleIsNotAPause(){
        var a=AnalysisRuntime.counters(0,0,Map.of("ZGC Cycles",gc("ZGC Cycles",1,1500),"ZGC Pauses",gc("ZGC Pauses",3,1)));
        var b=AnalysisRuntime.counters(0,0,Map.of("ZGC Cycles",gc("ZGC Cycles",2,3000),"ZGC Pauses",gc("ZGC Pauses",6,2)));
        var q=AnalysisRuntime.difference(a,b,100,0,0,0,true);
        assertEquals(1,q.gcMillis());assertEquals(3,q.gcCount());
        assertEquals(1500,q.collectors().get("ZGC Cycles").millis());
        assertTrue(BenchmarkReport.compatible(q,new AnalysisRuntime.Quality(100,0,0,0,true,0,0,0,0)));
    }
    @Test void g1AndUnknownAreKeptSeparate(){
        var c=AnalysisRuntime.counters(0,0,Map.of("G1 Young Generation",gc("G1 Young Generation",2,31),"New GC",gc("New GC",7,2000)));
        assertEquals(31,c.gcMillis());assertEquals(2,c.gcCount());
        assertEquals(AnalysisRuntime.GcKind.UNKNOWN,c.collectors().get("New GC").kind());
        assertEquals(AnalysisRuntime.GcKind.CONCURRENT,AnalysisRuntime.gcKind("Shenandoah Cycles"));
        assertEquals(AnalysisRuntime.GcKind.PAUSE,AnalysisRuntime.gcKind("ZGC Minor Pauses"));
    }
}

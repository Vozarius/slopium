package dev.slopium.benchmark;

import com.google.gson.*;
import dev.slopium.client.SlopiumConfig;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkTest {
    private JsonObject config(){return new Gson().toJsonTree(new SlopiumConfig()).getAsJsonObject();}
    @Test void planIsDeterministicBoundedAndDoesNotMutateSavedSettings(){
        var saved=config();saved.addProperty("enabled",false);saved.addProperty("frameGenerationMode","SIMPLE");var copy=saved.deepCopy();
        var plan=BenchmarkPlan.build(saved,true);
        assertEquals(copy,saved);assertEquals(plan,BenchmarkPlan.build(saved,true));assertTrue(plan.size()<300);assertTrue(plan.size()>100);
        var counts=new HashMap<String,Integer>();int gap=0;
        for(var t:plan){if(t.control()){gap=0;assertEquals("OFF",t.settings().get("frameGenerationMode").getAsString());assertTrue(t.settings().get("enabled").getAsBoolean());}else{counts.merge(t.id(),1,Integer::sum);assertTrue(++gap<=4);}}
        counts.values().forEach(n->assertEquals(2,n));assertEquals(2,counts.get("original"));assertEquals(2,counts.get("disabled"));
        assertTrue(plan.stream().filter(t->t.id().equals("visibleSections=true")).allMatch(t->t.settings().get("gpuGlassCulling").getAsBoolean()));
        assertTrue(plan.size()>BenchmarkPlan.build(saved,false).size());
    }
    @Test void recordsRealIntervalsSeparatelyFromParallelPresentations() throws Exception {
        var s=new BenchmarkSamples();s.source(1);s.source(10_000_001);s.source(40_000_001);
        var pool=Executors.newFixedThreadPool(2);
        try{var jobs=new ArrayList<Future<?>>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->{for(int j=0;j<100;j++)s.output(true);}));for(var j:jobs)j.get();}finally{pool.shutdown();}
        var r=s.result(50_000_000);assertEquals(3,r.sourceFrames());assertEquals(200,r.outputFrames());assertEquals(200,r.generatedFrames());
        assertEquals(60,r.sourceFps());assertEquals(4000,r.outputFps());assertEquals(20,r.meanMs());assertEquals(10,r.p50Ms());assertEquals(30,r.p95Ms());
    }
    @Test void driftAndMissingControlsCannotBecomePerformanceClaims(){
        assertFalse(BenchmarkReport.compare(100,140,150).reliable());assertFalse(BenchmarkReport.compare(0,100,120).reliable());
        var c=BenchmarkReport.compare(100,102,120);assertTrue(c.reliable());assertEquals(18.81188,c.percent(),.0001);assertEquals(5,c.noise());
    }
    @Test void partialReportCannotRecommendAnUnrepeatedChange(){
        var cfg=config();var m=new BenchmarkSamples();for(int i=1;i<=100;i++)m.source(i*10_000_000L);
        var result=m.result(1_000_000_000);
        var rows=List.of(new PerformanceAnalysis.Row(0,"control","",true,cfg,new JsonObject(),result,0,true,List.of(),"",""),
            new PerformanceAnalysis.Row(1,"test","",false,cfg,new JsonObject(),result,0,true,List.of(),"",""));
        assertTrue(BenchmarkReport.markdown("stopped",rows).contains("inconclusive / incomplete"));
    }
}

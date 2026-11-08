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
    @Test void lightweightSettingsDoNotRequestTerrainOrGeneratorRebuilds(){
        var a=config();var b=a.deepCopy();b.addProperty("cameraCulling",true);b.addProperty("occludeCreateTicks",true);
        assertEquals(new AnalysisChanges(false,false,false),AnalysisChanges.between(a,b));
        b.addProperty("createLight",false);assertTrue(AnalysisChanges.between(a,b).terrain());
        b=a.deepCopy();b.addProperty("frameGenerationMode","SIMPLE");assertEquals(new AnalysisChanges(false,true,false),AnalysisChanges.between(a,b));
        b=a.deepCopy();b.addProperty("processedBlockMode","BLACKLIST");assertEquals(new AnalysisChanges(false,false,true),AnalysisChanges.between(a,b));
    }
    @Test void invalidImmediateControlIsNeverReplacedByADistantGoodOne(){
        var cfg=config();var sample=new BenchmarkSamples();for(int i=1;i<=100;i++)sample.source(i*10_000_000L);var m=sample.result(1_000_000_000);
        var rows=new ArrayList<PerformanceAnalysis.Row>();
        rows.add(new PerformanceAnalysis.Row(0,"control","",true,cfg,new JsonObject(),m,0,true,List.of(),"",""));
        rows.add(new PerformanceAnalysis.Row(1,"control","",true,cfg,new JsonObject(),m,0,false,List.of(),"",""));
        for(int i=2;i<=3;i++)rows.add(new PerformanceAnalysis.Row(i,"test","",false,cfg,new JsonObject(),m,0,true,List.of(),"",""));
        rows.add(new PerformanceAnalysis.Row(4,"control","",true,cfg,new JsonObject(),m,0,true,List.of(),"",""));
        assertTrue(BenchmarkReport.markdown("complete",rows).contains("inconclusive / incomplete"));
    }
    @Test void qualityRejectsDifferentTerrainFocusOrGcLoads(){
        var q=new AnalysisRuntime.Quality(100,0,0,0,true,1,1,0,0);
        assertTrue(BenchmarkReport.compatible(q,new AnalysisRuntime.Quality(100,8,0,0,true,1,1,0,0)));
        assertFalse(BenchmarkReport.compatible(q,new AnalysisRuntime.Quality(100,30,0,0,true,1,1,0,0)));
        assertFalse(BenchmarkReport.compatible(q,new AnalysisRuntime.Quality(100,0,100,100,true,1,1,0,0)));
        assertFalse(BenchmarkReport.compatible(q,new AnalysisRuntime.Quality(100,0,0,0,true,1,1,1,200)));
    }
    @Test void withinMeasurementDriftDoesNotDisappearInAnAverage(){
        var s=new BenchmarkSamples(1);
        for(int sec=0;sec<8;sec++)for(int f=0;f<(sec<4?30:60);f++)s.source(1+sec*1_000_000_000L+f*1_000_000L);
        var result=s.result(8_000_000_000L);
        assertEquals(List.of(30L,30L,30L,30L,60L,60L,60L,60L),result.sourceCountsPerSecond());
        assertEquals(66.6666667,result.withinRunDriftPercent(),.0001);
    }
}

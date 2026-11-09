package dev.slopium.benchmark;

import java.util.*;

/** Relative effects are local to the saved configuration and this scene. */
public final class BenchmarkReport {
    private BenchmarkReport(){}
    public record Comparison(boolean reliable,double percent,double noise,String reason){}
    public static Comparison compare(double before,double after,double candidate){
        if(before<=0||after<=0||candidate<=0)return new Comparison(false,0,0,"missing control");
        double midpoint=(before+after)/2,drift=Math.abs(before-after)/midpoint*100;
        return new Comparison(drift<=10,(candidate/midpoint-1)*100,Math.max(5,drift),drift>10?"control drift >10%":"");
    }
    private static double fraction(int n,int total){return total==0?0:100.0*n/total;}
    public static boolean compatible(AnalysisRuntime.Quality a,AnalysisRuntime.Quality b){
        if(a==null||b==null)return a==b;
        return Math.abs(a.terrainPercent()-b.terrainPercent())<=10
            &&Math.abs(fraction(a.backgroundChecks(),a.checks())-fraction(b.backgroundChecks(),b.checks()))<=10
            &&Math.abs(fraction(a.minimizedChecks(),a.checks())-fraction(b.minimizedChecks(),b.checks()))<=10
            &&Math.abs(a.gcMillis()-b.gcMillis())<=100;
    }
    public static String markdown(String state,List<PerformanceAnalysis.Row> rows){
        var out=new StringBuilder("# Slopium performance analysis\n\nStatus: "+state+"\n\n");
        out.append("Real FPS counts completed world renders. Output FPS counts window presentations (including held images); it is not a count of unique new images. Frame times are intervals between completed real frames, not isolated GPU time.\n\n");
        out.append("Reference: your saved settings with Slopium enabled and frame generation OFF. Changes below are relative to that reference. Caps and VSync are unchanged; capped results cannot establish available performance headroom. GPU section timings are cumulative diagnostics, not isolated per-trial timings.\n\n");
        out.append("Each variant uses A-B-B-A: two repeats between immediate controls. Invalid controls are never skipped. Terrain-busy, background/minimized and GC pause proportions must match the controls; FPS is sampled before the analyzer starts Spark. GC ms includes known pause beans only; concurrent cycle time and unclassified collectors remain separately recorded in JSON. Per-second source counts and within-run drift are in JSON; drift over 15% invalidates a measurement. 'Faster/slower in this run' requires both repeats to agree beyond max(5%, control drift), at most 10% control drift, comparable background work, and no chunk streaming. These are screening results, not proof for other scenes. Visual removals and client tick changes have tradeoffs. Read the full JSON settings and warnings before keeping a change.\n\n");
        var groups=new LinkedHashMap<String,List<PerformanceAnalysis.Row>>();
        for(var row:rows)if(!row.control())groups.computeIfAbsent(row.id(),k->new ArrayList<>()).add(row);
        out.append("| Variant | Real FPS effect | Assessment |\n|---|---:|---|\n");
        for(var entry:groups.entrySet()){
            var effects=new ArrayList<Comparison>();
            for(var row:entry.getValue()) {
                PerformanceAnalysis.Row before=null,after=null;
                for(var c:rows)if(c.control()){if(c.index()<row.index())before=c;else if(c.index()>row.index()){after=c;break;}}
                if(row.valid()&&before!=null&&after!=null&&before.valid()&&after.valid()){
                    var comparison=compare(before.metrics().sourceFps(),after.metrics().sourceFps(),row.metrics().sourceFps());
                    if(!compatible(row.quality(),before.quality())||!compatible(row.quality(),after.quality()))comparison=new Comparison(false,comparison.percent(),comparison.noise(),"workload/focus/GC differs from adjacent controls");
                    effects.add(comparison);
                }
            }
            boolean reliable=effects.size()>=2&&effects.stream().allMatch(Comparison::reliable);
            String verdict=!reliable?"inconclusive / incomplete":effects.stream().allMatch(e->e.percent()>e.noise())?"faster in this run":effects.stream().allMatch(e->e.percent() < -e.noise())?"slower in this run":"no consistent benefit above noise";
            double effect=effects.stream().mapToDouble(Comparison::percent).average().orElse(0);
            out.append("| ").append(entry.getKey()).append(" | ").append(effects.isEmpty()?"n/a":String.format(Locale.ROOT,"%+.1f%%",effect)).append(" | ").append(verdict).append(" |\n");
        }
        out.append("\n## Measurements\n\n| # | Variant | Real FPS | Output FPS | Generated | p50 ms | p95 ms | p99 ms | Valid |\n|---:|---|---:|---:|---:|---:|---:|---:|---|\n");
        for(var r:rows){var m=r.metrics();out.append(String.format(Locale.ROOT,"| %d | %s | %.2f | %.2f | %d | %.2f | %.2f | %.2f | %s |\n",r.index()+1,r.id(),m.sourceFps(),m.outputFps(),m.generatedFrames(),m.p50Ms(),m.p95Ms(),m.p99Ms(),r.valid()));}
        out.append("\n## Measurement quality\n\n| # | Terrain busy % | Background % | Minimized % | Render CPU ms | Process CPU ms | GC pause ms | Quiet warmup |\n|---:|---:|---:|---:|---:|---:|---:|---|\n");
        for(var r:rows)if(r.quality()!=null){var q=r.quality();out.append(String.format(Locale.ROOT,"| %d | %.1f | %.1f | %.1f | %.1f | %.1f | %d | %s |\n",r.index()+1,q.terrainPercent(),fraction(q.backgroundChecks(),q.checks()),fraction(q.minimizedChecks(),q.checks()),q.renderCpuMillis(),q.processCpuMillis(),q.gcMillis(),q.warmedQuiet()));}
        out.append("\n## Changes and warnings\n\n");
        for(var e:groups.entrySet()){var r=e.getValue().getFirst();out.append("- **").append(e.getKey()).append("** (").append(r.category()).append("): `").append(r.changes()).append("`\n");for(var repeat:e.getValue())if(!repeat.warnings().isEmpty())out.append("  - Trial ").append(repeat.index()+1).append(": ").append(String.join("; ",repeat.warnings())).append('\n');}
        return out.toString();
    }
}

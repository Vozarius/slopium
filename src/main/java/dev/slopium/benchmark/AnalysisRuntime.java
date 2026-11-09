package dev.slopium.benchmark;

import java.lang.management.*;
import java.util.*;

/** Counters around a measurement, without a stack sampler on the timed path. */
public final class AnalysisRuntime {
    public enum GcKind { PAUSE, CONCURRENT, UNKNOWN }
    public record GcCounter(long count,long millis,GcKind kind){}
    public record Counters(long renderCpu,long processCpu,long gcCount,long gcMillis,Map<String,GcCounter> collectors){}
    public record Quality(int checks,int terrainBusyChecks,int backgroundChecks,int minimizedChecks,
        boolean warmedQuiet,double renderCpuMillis,double processCpuMillis,long gcCount,long gcMillis,
        Map<String,GcCounter> collectors) {
        public Quality(int checks,int busy,int background,int minimized,boolean quiet,double render,double process,long count,long millis){
            this(checks,busy,background,minimized,quiet,render,process,count,millis,Map.of());
        }
        public double terrainPercent(){return checks==0?0:100.0*terrainBusyChecks/checks;}
    }
    /** Collection time from ZGC/Shenandoah cycle beans includes concurrent work, not STW. */
    public static GcKind gcKind(String name){
        if(name.equals("ZGC Cycles")||name.equals("ZGC Minor Cycles")||name.equals("ZGC Major Cycles")||name.equals("Shenandoah Cycles"))return GcKind.CONCURRENT;
        if(name.equals("ZGC Pauses")||name.equals("ZGC Minor Pauses")||name.equals("ZGC Major Pauses")||name.equals("Shenandoah Pauses")
            ||name.equals("G1 Young Generation")||name.equals("G1 Old Generation")||name.equals("PS Scavenge")||name.equals("PS MarkSweep")
            ||name.equals("Copy")||name.equals("MarkSweepCompact")||name.equals("ParNew"))return GcKind.PAUSE;
        return GcKind.UNKNOWN;
    }
    public static Counters counters(){
        var threads=ManagementFactory.getThreadMXBean();long cpu=threads.isCurrentThreadCpuTimeSupported()?threads.getCurrentThreadCpuTime():-1;
        var os=ManagementFactory.getOperatingSystemMXBean();long process=os instanceof com.sun.management.OperatingSystemMXBean bean?bean.getProcessCpuTime():-1;
        var collectors=new TreeMap<String,GcCounter>();
        for(var gc:ManagementFactory.getGarbageCollectorMXBeans())collectors.put(gc.getName(),new GcCounter(Math.max(0,gc.getCollectionCount()),Math.max(0,gc.getCollectionTime()),gcKind(gc.getName())));
        return counters(cpu,process,collectors);
    }
    public static Counters counters(long cpu,long process,Map<String,GcCounter> collectors){
        long count=0,time=0;
        for(var gc:collectors.values())if(gc.kind()==GcKind.PAUSE){count+=gc.count();time+=gc.millis();}
        return new Counters(cpu,process,count,time,Map.copyOf(collectors));
    }
    public static Quality difference(Counters a,Counters b,int checks,int busy,int background,int minimized,boolean quiet){
        var collectors=new TreeMap<String,GcCounter>();
        b.collectors().forEach((name,value)->{
            var before=a.collectors().get(name);
            if(before!=null)collectors.put(name,new GcCounter(Math.max(0,value.count()-before.count()),Math.max(0,value.millis()-before.millis()),value.kind()));
        });
        return new Quality(checks,busy,background,minimized,quiet,a.renderCpu()<0||b.renderCpu()<0?-1:(b.renderCpu()-a.renderCpu())/1e6,
            a.processCpu()<0||b.processCpu()<0?-1:(b.processCpu()-a.processCpu())/1e6,Math.max(0,b.gcCount()-a.gcCount()),Math.max(0,b.gcMillis()-a.gcMillis()),Map.copyOf(collectors));
    }
}

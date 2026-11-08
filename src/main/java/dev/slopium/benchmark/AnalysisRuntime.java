package dev.slopium.benchmark;

import java.lang.management.*;

/** Counters around a measurement, without a stack sampler on the timed path. */
public final class AnalysisRuntime {
    public record Counters(long renderCpu,long processCpu,long gcCount,long gcMillis){}
    public record Quality(int checks,int terrainBusyChecks,int backgroundChecks,int minimizedChecks,
        boolean warmedQuiet,double renderCpuMillis,double processCpuMillis,long gcCount,long gcMillis) {
        public double terrainPercent(){return checks==0?0:100.0*terrainBusyChecks/checks;}
    }
    public static Counters counters(){
        var threads=ManagementFactory.getThreadMXBean();long cpu=threads.isCurrentThreadCpuTimeSupported()?threads.getCurrentThreadCpuTime():-1;
        var os=ManagementFactory.getOperatingSystemMXBean();long process=os instanceof com.sun.management.OperatingSystemMXBean bean?bean.getProcessCpuTime():-1;
        long count=0,time=0;for(var gc:ManagementFactory.getGarbageCollectorMXBeans()){count+=Math.max(0,gc.getCollectionCount());time+=Math.max(0,gc.getCollectionTime());}
        return new Counters(cpu,process,count,time);
    }
    public static Quality difference(Counters a,Counters b,int checks,int busy,int background,int minimized,boolean quiet){
        return new Quality(checks,busy,background,minimized,quiet,a.renderCpu()<0||b.renderCpu()<0?-1:(b.renderCpu()-a.renderCpu())/1e6,
            a.processCpu()<0||b.processCpu()<0?-1:(b.processCpu()-a.processCpu())/1e6,b.gcCount()-a.gcCount(),b.gcMillis()-a.gcMillis());
    }
}

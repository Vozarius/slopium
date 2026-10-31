package dev.slopium.framegen;

/** Source-only measurements: synthetic frames must never enter the source-rate estimate. */
public final class AdaptiveFrameTarget {
    public static final int PHASES=1_000_000;
    private long lastSource;
    private volatile long sourcePeriod;
    public static int target(int configured){return configured<=0?60:Math.clamp(configured,1,240);}
    public void reset(){lastSource=sourcePeriod=0;}
    public void source(long now){if(lastSource!=0)cost(now-lastSource);lastSource=now;}
    public void cost(long nanos){
        nanos=Math.clamp(nanos,1_000L,10_000_000_000L);
        sourcePeriod=sourcePeriod==0?nanos:(sourcePeriod*3+nanos)/4;
    }
    public long period(){return sourcePeriod;}
    public boolean needed(int target){return needsGeneration(sourcePeriod,target);}
    public static boolean needsGeneration(long period,int target){return period>1_000_000_000L/target+1;}
    public static int framesPerSource(long period,int target){
        return (int)Math.clamp(Math.ceil(period*(double)target/1e9-1e-7),1,2400);
    }
    public static int cap(int external,int target){return external>0?Math.min(external,target):target;}
    /** Preserve cadence, but never burst through missed deadlines after a stall. */
    public static long nextDeadline(long previous,long now,int fps){
        long step=(1_000_000_000L+fps-1)/fps;
        return previous==0||now-previous>=step?now+step:previous+step;
    }
}

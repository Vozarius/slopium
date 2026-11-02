package dev.slopium.framegen;
/** One writer per counter. Readers only see a published immutable sample; no render-thread locks. */
public final class RecentFrameRate {
    private long start,count;
    private boolean started;
    private volatile Sample sample=new Sample(0,0);
    private volatile long last;
    public volatile long total;
    private record Sample(long at,double fps){}
    public void event(long now){
        total++;last=now;
        if(!started){start=now;started=true;return;}
        count++;
        long elapsed=now-start;
        if(elapsed>=1_000_000_000L){sample=new Sample(now,count*1e9/elapsed);start=now;count=0;}
    }
    public double fps(long now){return now-last>2_000_000_000L?0:sample.fps;}
}

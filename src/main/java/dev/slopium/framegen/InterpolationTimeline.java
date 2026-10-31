package dev.slopium.framegen;

/** Presenter-owned playback clock. Incoming source frames may not truncate a displayed pair. */
public final class InterpolationTimeline {
    private long start, duration;
    private int displayed=-1;
    private boolean complete=true;
    public boolean readyForNext(){return complete;}
    public void begin(long now,long nanos,boolean interpolate) {
        long previousEnd=start+duration;
        boolean continuous=complete&&duration>0&&now>=previousEnd&&now-previousEnd<Math.min(5_000_000L,nanos/4);
        if(!complete)throw new IllegalStateException("Cannot replace an unfinished interpolation pair");
        start=continuous?previousEnd:now;
        duration=interpolate?Math.max(1,nanos):0;
        displayed=interpolate&&continuous?0:-1;
        complete=false;
    }
    /** Camera cuts / excessive backlog explicitly discard old history. */
    public void cut(){complete=true;duration=0;displayed=-1;}
    public int phase(long now,int multiplier){return duration==0?multiplier:(int)Math.clamp((now-start)*multiplier/duration,0,multiplier);}
    public boolean needsPresent(int phase){return !complete&&phase>displayed;}
    public void presented(int phase,int multiplier){displayed=phase;complete=phase==multiplier;}
    public long nextPhaseAt(int multiplier){return start+(displayed+1L)*duration/multiplier;}
}

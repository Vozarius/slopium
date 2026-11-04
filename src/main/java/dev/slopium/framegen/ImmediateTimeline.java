package dev.slopium.framegen;
/** A newly completed real frame preempts prediction. Predictions never replay an older endpoint. */
public final class ImmediateTimeline {
    private long start,period=1;
    private int displayed;
    public void source(long now,long period){start=now;this.period=Math.max(1,period);displayed=0;}
    public int phase(long now,int phases){return (int)Math.clamp((double)Math.max(0,now-start)*phases/period,0,phases);}
    public boolean needs(int phase,int phases){return phase>displayed&&phase<phases;}
    public void presented(int phase){displayed=phase;}
    public long nextAt(int phases){return start+(displayed+1L)*period/phases;}
}

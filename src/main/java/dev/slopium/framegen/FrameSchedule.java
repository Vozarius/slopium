package dev.slopium.framegen;

/** Wall-clock deadlines, independent of the number of synthetic frames produced. Render thread only. */
public final class FrameSchedule {
    private int target=-1, samples, generated;
    private long period, next, lastStart, probeSum, probeAt, generatedCost=1_000_000;
    private boolean probing, captured;
    public void configure(int fps) {
        fps=Math.clamp(fps,0,240);
        if(target!=fps){target=fps;reset();}
    }
    public void reset() {
        period=target>0?(1_000_000_000L+target-1)/target:0;
        probing=target<=0;samples=generated=0;next=lastStart=probeSum=probeAt=0;
        captured=false;generatedCost=1_000_000;
    }
    public void begin(long now) {
        if(!probing&&target==0&&now>=probeAt) {
            probing=true;samples=0;probeSum=0;lastStart=0;captured=false;
        }
        if(probing&&lastStart!=0) {
            probeSum+=Math.max(1,now-lastStart);
            if(++samples>=8) {
                // Ordinary complete frame intervals include HUD, swap/pacing and GPU backpressure.
                period=Math.max(1_000,probeSum/samples);
                probing=false;probeAt=now+5_000_000_000L;captured=false;
            }
        }
        lastStart=now;
    }
    public boolean probing(){return probing;}
    public long period(){return period;}
    public int targetFps(){return period==0?0:(int)Math.clamp(Math.round(1e9/period),1,1_000_000);}
    public boolean due(long now){return !captured||now-next>=0;}
    public void captured(long now) {
        // Preserve cadence under small jitter, but never build a backlog after a long stall.
        next=!captured||now-next>=period?now+period:next+period;
        captured=true;generated=0;
        // A one-off shader compilation or GPU stall must not disable synthesis forever.
        generatedCost=Math.max(100_000,generatedCost*7/8);
    }
    public boolean canGenerate(long now,int multiplier) {
        return captured&&!probing&&generated<multiplier-1&&next-now>generatedCost;
    }
    public void generated(long workNanos){
        generated++;
        // Measure actual synthesis work, not the following FPS-pacer sleep. Counting that sleep
        // as work would permanently reject the midpoint at x2 / 30 real / 60 output FPS.
        long cost=Math.max(1,workNanos);
        generatedCost=cost>generatedCost?cost:(generatedCost*7+cost)/8;
    }
}

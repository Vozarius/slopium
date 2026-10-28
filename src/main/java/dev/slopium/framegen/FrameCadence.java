package dev.slopium.framegen;

/** A, A/B at 1/N ... (N-1)/N, B. Only one pending phase, no queue of N full-size images. */
public final class FrameCadence {
    private boolean history;
    private int multiplier=2,phase;
    public int multiplier(){return multiplier;}
    public void configure(int value){value=Math.clamp(value,2,32);if(value!=multiplier){multiplier=value;reset();}}
    public boolean needsWorld() { return phase==0; }
    public boolean endpoint(){return phase==multiplier;}
    public boolean midpoint(){return phase*2==multiplier;}
    public float fraction(){return phase/(float)multiplier;}
    public boolean captured() {
        boolean interpolate = history;
        history = true;
        phase = interpolate?1:0;
        return interpolate;
    }
    public void presented() { if(phase>0)phase=phase==multiplier?0:phase+1; }
    public void reset() { history=false;phase=0; }
}

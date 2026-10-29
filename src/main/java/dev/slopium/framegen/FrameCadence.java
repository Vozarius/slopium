package dev.slopium.framegen;

/** Time-selected FSR pair. Missing phases are discarded when the next real frame is due. */
public final class FrameCadence {
    private boolean history,pair;
    private int multiplier=2,phase;
    public int multiplier(){return multiplier;}
    public void configure(int value){value=Math.clamp(value,2,32);if(value!=multiplier){multiplier=value;reset();}}
    public boolean endpoint(){return phase==multiplier;}
    public boolean midpoint(){return phase*2==multiplier;}
    public float fraction(){return phase/(float)multiplier;}
    public boolean captured() {
        boolean interpolate = history;
        history = true;
        pair=interpolate;phase=0;
        return interpolate;
    }
    public boolean sample(long elapsed,long period) {
        if(!pair||period<=0)return false;
        int selected=(int)Math.clamp((double)Math.max(0,elapsed)*multiplier/period,0,multiplier);
        if(selected<=phase)return false;
        phase=selected;return true;
    }
    public void reset() { history=pair=false;phase=0; }
}

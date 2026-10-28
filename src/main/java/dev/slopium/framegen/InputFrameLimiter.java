package dev.slopium.framegen;

/** Render-thread schedule. No sleeping, accumulated catch-up work or image allocations. */
public final class InputFrameLimiter {
    private int fps;
    private long interval,next;
    private boolean scheduled;
    public void configure(int value) {
        value=Math.clamp(value,0,240);
        if(value!=fps){fps=value;interval=fps==0?0:(1_000_000_000L+fps-1)/fps;reset();}
    }
    public boolean ready(long now){return fps==0||!scheduled||now-next>=0;}
    public void captured(long now) {
        if(fps==0)return;
        // Preserve cadence through small scheduling jitter; discard missed periods after stalls.
        next=!scheduled||now-next>=interval?now+interval:next+interval;
        scheduled=true;
    }
    public void reset(){scheduled=false;}

    /** Zero means uncapped. Minecraft's 260 (and above) is its unlimited sentinel. */
    public static int outputLimit(int minecraft,int output,int input,int multiplier) {
        int result=minecraft>=260?Integer.MAX_VALUE:minecraft;
        if(output>0)result=Math.min(result,output);
        if(input>0)result=Math.min(result,Math.clamp(input,1,240)*Math.clamp(multiplier,2,32));
        return result==Integer.MAX_VALUE?0:result;
    }
}

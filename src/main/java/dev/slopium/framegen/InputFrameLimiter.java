package dev.slopium.framegen;

/** Output ceiling shared by the native Minecraft pacer and frame generation. */
public final class InputFrameLimiter {
    private InputFrameLimiter() {}
    /** Zero means uncapped. Minecraft's 260 (and above) is its unlimited sentinel. */
    public static int outputLimit(int minecraft,int output,int input,int multiplier) {
        int result=minecraft>=260?Integer.MAX_VALUE:minecraft;
        if(output>0)result=Math.min(result,output);
        if(input>0)result=Math.min(result,Math.clamp(input,1,1_000_000)*Math.clamp(multiplier,2,32));
        return result==Integer.MAX_VALUE?0:result;
    }
}

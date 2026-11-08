package dev.slopium.benchmark;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Only the render thread writes source timings; the presenter may count output concurrently. */
public final class BenchmarkSamples {
    private final long[] intervals=new long[131072];
    private int size;private long last,sourceCount,totalIntervals,began;
    private final long[] seconds=new long[120];
    public BenchmarkSamples(){}
    public BenchmarkSamples(long began){this.began=began;}
    private final AtomicLong output=new AtomicLong(),generated=new AtomicLong();
    public void source(long now){if(began==0)began=now;int bucket=(int)((now-began)/1_000_000_000L);if(bucket>=0&&bucket<seconds.length)seconds[bucket]++;if(last!=0&&now>last){long d=now-last;totalIntervals+=d;if(size<intervals.length)intervals[size++]=d;}last=now;sourceCount++;}
    public void output(boolean synthetic){output.incrementAndGet();if(synthetic)generated.incrementAndGet();}
    public record Result(long sourceFrames,long outputFrames,long generatedFrames,double seconds,double sourceFps,double outputFps,double meanMs,double p50Ms,double p95Ms,double p99Ms,int intervalSamples,boolean truncated,List<Long> sourceCountsPerSecond,double withinRunDriftPercent){}
    public Result result(long elapsed){
        double seconds=Math.max(1,elapsed)/1e9;
        var buckets=new ArrayList<Long>();for(int i=0;i<Math.min(this.seconds.length,(int)Math.ceil(seconds));i++)buckets.add(this.seconds[i]);
        int full=Math.min(this.seconds.length,(int)seconds),half=full/2;double drift=0;
        if(full>=4){long first=0,last=0;for(int i=0;i<half;i++){first+=this.seconds[i];last+=this.seconds[full-1-i];}if(first+last>0)drift=200.0*Math.abs(first-last)/(first+last);}
        var sorted=Arrays.copyOf(intervals,size);Arrays.sort(sorted);
        return new Result(sourceCount,output.get(),generated.get(),seconds,sourceCount/seconds,output.get()/seconds,
            sourceCount<2?0:totalIntervals/1e6/(sourceCount-1),percentile(sorted,.5),percentile(sorted,.95),percentile(sorted,.99),size,sourceCount-1>size,List.copyOf(buckets),drift);
    }
    private static double percentile(long[] sorted,double p){return sorted.length==0?0:sorted[(int)Math.ceil(sorted.length*p)-1]/1e6;}
}

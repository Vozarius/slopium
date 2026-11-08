package dev.slopium.framegen;
import dev.slopium.client.SlopiumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
/** Source = completed world draw. Output = successful window swap, including held images.
 * F3 and HUD use one frozen sample per Minecraft frame, with a recent one-second measurement. */
public final class FrameMetrics {
    private static final class Bank {
        final RecentFrameRate source=new RecentFrameRate(),output=new RecentFrameRate();
        volatile long generated;
    }
    private static volatile Bank bank=new Bank();
    public record Snapshot(double source,double output,long generated){}
    private static Snapshot display=new Snapshot(0,0,0);
    private static boolean tracking,synthesized,sourceCounted;
    private static Object world;
    private FrameMetrics(){}
    public static void beginFrame(boolean render){
        var mc=Minecraft.getInstance();
        if(world!=mc.level){world=mc.level;reset();}
        boolean next=render&&mc.level!=null&&mc.screen==null&&mc.getOverlay()==null&&!mc.isPaused();
        if(next&&!tracking){var fresh=new Bank();fresh.generated=bank.generated;bank=fresh;}
        tracking=next;
        synthesized=false;sourceCounted=false;
        long now=System.nanoTime();var b=bank;
        display=new Snapshot(b.source.fps(now),b.output.fps(now),b.generated);
    }
    public static void reset(){bank=new Bank();display=new Snapshot(0,0,0);}
    public static Snapshot display(){return display;}
    public static void sourceCompleted(){if(tracking&&!sourceCounted){long now=System.nanoTime();bank.source.event(now);dev.slopium.benchmark.PerformanceAnalysis.source(now);sourceCounted=true;}}
    public static void synthesized(){synthesized=true;}
    public static void mainPresented(){if(tracking)presented(System.nanoTime(),synthesized);}
    public static void presented(long now,boolean generated){dev.slopium.benchmark.PerformanceAnalysis.presented(generated);var b=bank;b.output.event(now);if(generated)b.generated++;}
    public static Component counter(){var s=display;return Component.translatable("slopium.frame_generation.counter",Math.round(s.source),Math.round(s.output),s.generated);}
    public static String debugLine(String vanilla){
        if(SlopiumConfig.get().frameGenerationMode==FrameGenerationMode.OFF)return vanilla;
        int end=vanilla.indexOf(" fps");
        String suffix=end<0?"":vanilla.substring(end+4);
        var s=display;
        return Component.translatable("slopium.frame_generation.f3",Math.round(s.source),Math.round(s.output)).getString()+suffix;
    }
    public static java.util.List<String> refreshDebugLines(java.util.List<String> lines){
        if(SlopiumConfig.get().frameGenerationMode==FrameGenerationMode.OFF||lines.size()<2||!lines.getFirst().startsWith("Minecraft "))return lines;
        String current=debugLine(Minecraft.getInstance().fpsString);
        if(lines.get(1).equals(current))return lines;
        var copy=new java.util.ArrayList<>(lines);copy.set(1,current);return copy;
    }
}

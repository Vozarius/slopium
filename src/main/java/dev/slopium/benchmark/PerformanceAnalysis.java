package dev.slopium.benchmark;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import dev.slopium.client.*;
import dev.slopium.framegen.FrameGeneration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Render-thread state machine. Never saves trial settings or changes vanilla options. */
public final class PerformanceAnalysis {
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static PerformanceAnalysis active;
    private static volatile BenchmarkSamples sampling;
    private static final ExecutorService WRITER=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Slopium report writer");t.setDaemon(true);return t;});
    private static volatile String lastReport="";
    private final JsonObject original,environment;
    private final List<BenchmarkPlan.Trial> trials;
    private final List<Row> rows=new ArrayList<>();
    private final Object world;
    private final double x,y,z;private final float yaw,pitch;
    private final int width,height;
    private final long warmup,measurement;
    private final Path report;
    private final String startOptions;
    private int index=-1;
    private long stageStart,measureStart,chunkChanges,lastOptionCheck;
    private String phase="warmup";
    private boolean terrainBusy;
    public record Row(int index,String id,String category,boolean control,JsonObject settings,JsonObject changes,
        BenchmarkSamples.Result metrics,long chunkEvents,boolean valid,List<String> warnings,String gpuSections,String frameGeneration){}

    private PerformanceAnalysis(boolean extended,List<BenchmarkPlan.Trial> custom,long warmup,long measurement) {
        var mc=Minecraft.getInstance();original=snapshot();trials=custom==null?BenchmarkPlan.build(original,extended):List.copyOf(custom);
        this.warmup=warmup;this.measurement=measurement;world=mc.level;
        var camera=mc.gameRenderer.getMainCamera();var p=camera.getPosition();x=p.x;y=p.y;z=p.z;yaw=camera.getYRot();pitch=camera.getXRot();
        width=mc.getWindow().getWidth();height=mc.getWindow().getHeight();
        environment=environment();startOptions=options().toString();
        report=mc.gameDirectory.toPath().resolve("slopium-reports").resolve("performance-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+"-"+UUID.randomUUID().toString().substring(0,8));
    }
    public static JsonObject snapshot(){return JSON.toJsonTree(SlopiumConfig.get()).getAsJsonObject();}
    public static void restore(JsonObject snapshot){
        var copy=JSON.fromJson(snapshot,SlopiumConfig.class);var target=SlopiumConfig.get();
        try{for(var f:SlopiumConfig.class.getFields())if(!Modifier.isStatic(f.getModifiers()))f.set(target,f.get(copy));}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot restore Slopium settings",e);}
    }
    public static boolean running(){return active!=null;}
    public static String lastReport(){return lastReport;}
    public static int trialCount(boolean extended){return BenchmarkPlan.build(snapshot(),extended).size();}
    public static int secondsPerTrial(boolean extended){return extended?16:8;}
    public static void start(boolean extended){startCustom(null,extended,extended?6_000_000_000L:3_000_000_000L,extended?10_000_000_000L:5_000_000_000L);}
    /** Shortened durations are only used by the isolated integration harness. */
    public static void startCustom(List<BenchmarkPlan.Trial> trials,boolean extended,long warmup,long measurement){
        var mc=Minecraft.getInstance();if(active!=null||mc.level==null||mc.player==null)return;
        active=new PerformanceAnalysis(extended,trials,warmup,measurement);
        mc.setScreen(null);
        try{active.next();}catch(Exception e){fail(e);}
    }
    public static void source(long now){var s=sampling;if(s!=null)s.source(now);}
    public static void presented(boolean generated){var s=sampling;if(s!=null)s.output(generated);}
    public static void chunkChanged(){if(active!=null)active.chunkChanges++;}
    public static void tick(){
        var a=active;if(a==null)return;
        try{a.step();}catch(Exception e){fail(e);}
    }
    private static void fail(Exception e){LogUtils.getLogger().error("Slopium performance analysis failed",e);cancel("error: "+e.getClass().getSimpleName());}
    public static void cancel(String reason){var a=active;if(a!=null)a.finish(reason);}
    private void next(){
        sampling=null;
        if(++index>=trials.size()){finish("complete");return;}
        restore(trials.get(index).settings());Slopium.refreshOptions();
        stageStart=System.nanoTime();measureStart=0;chunkChanges=0;phase="warmup";terrainBusy=false;
    }
    private void step(){
        var mc=Minecraft.getInstance();
        if(mc.level!=world||mc.player==null){finish("world changed");return;}
        if(mc.screen!=null||mc.getOverlay()!=null||mc.isPaused()){finish("menu opened / paused");return;}
        if(!mc.isWindowActive()){finish("window lost focus");return;}
        if(mc.getWindow().getWidth()!=width||mc.getWindow().getHeight()!=height){finish("window resized");return;}
        long checked=System.nanoTime();
        if(checked-lastOptionCheck>500_000_000L){lastOptionCheck=checked;if(!options().toString().equals(startOptions)){finish("video settings changed");return;}}
        var camera=mc.gameRenderer.getMainCamera();var p=camera.getPosition();
        if(p.distanceToSqr(x,y,z)>.0025||Math.abs(net.minecraft.util.Mth.wrapDegrees(camera.getYRot()-yaw))>.15||Math.abs(camera.getXRot()-pitch)>.15){finish("camera moved");return;}
        long now=System.nanoTime();
        if(measureStart==0){
            if(now-stageStart<warmup)return;
            if(!terrainReady()&&now-stageStart<warmup+30_000_000_000L)return;
            measureStart=now;chunkChanges=0;sampling=new BenchmarkSamples();phase="measure";return;
        }
        if(!terrainReady())terrainBusy=true;
        if(now-measureStart<measurement)return;
        var s=sampling;sampling=null;var metrics=s.result(now-measureStart);var t=trials.get(index);
        var warnings=new ArrayList<String>();
        if(terrainBusy)warnings.add("Terrain rebuilds still pending during measurement");
        if(chunkChanges>0)warnings.add("Chunks loaded/unloaded during measurement: "+chunkChanges);
        if(metrics.sourceFrames()<30)warnings.add("Fewer than 30 real frames");
        if(metrics.truncated())warnings.add("Percentiles limited to the first 131072 intervals");
        if(metrics.p95Ms()>metrics.p50Ms()*2)warnings.add("Large frame-time spread");
        if(!t.settings().get("frameGenerationMode").getAsString().equals("OFF")&&metrics.generatedFrames()==0)warnings.add("Frame generation produced no synthetic output; inspect backend status");
        rows.add(new Row(index,t.id(),t.category(),t.control(),snapshot(),changes(BenchmarkPlan.reference(original),snapshot()),metrics,chunkChanges,
            !terrainBusy&&chunkChanges==0&&metrics.sourceFrames()>=30,warnings,dev.slopium.visibility.GpuSections.status(),FrameGeneration.status()+" | "+dev.slopium.framegen.AsyncPresenter.status()));
        write("in progress");next();
    }
    private void finish(String reason){
        sampling=null;active=null;
        try{restore(original);Slopium.refreshOptions();net.caffeinemc.mods.sodium.client.config.ConfigManager.CONFIG.resetAllOptionsFromBindings();}
        finally{write(reason);}
    }
    public static Component progress(){
        var a=active;if(a==null)return Component.empty();
        long remaining=(a.trials.size()-a.index)*(a.warmup+a.measurement)/1_000_000_000L;
        return Component.translatable("slopium.analysis.progress",a.index+1,a.trials.size(),Component.translatable("slopium.analysis."+a.phase),remaining/60);
    }
    public static void overlay(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event){
        if(active!=null)event.getGuiGraphics().drawString(Minecraft.getInstance().font,progress(),8,8,0xffffffff);
    }
    private void write(String state){
        // Freeze on the render thread; serialize and perform filesystem I/O away from timed rendering.
        var copy=List.copyOf(rows);var root=new JsonObject();root.addProperty("schema",1);root.addProperty("state",state);
        root.addProperty("plannedTrials",trials.size());root.addProperty("warmupSeconds",warmup/1e9);root.addProperty("measurementSeconds",measurement/1e9);
        root.add("originalSettings",original);root.add("referenceSettings",BenchmarkPlan.reference(original));root.add("environment",environment);
        root.addProperty("method","Two deterministic shuffled repeats; reference every four variants; current scene only. FG off in reference; all other saved settings retained; master enabled. No exhaustive Cartesian search.");
        WRITER.execute(()->{
            try{
                Files.createDirectories(report);root.add("trials",JSON.toJsonTree(copy));
                atomic(report.resolve("report.json"),JSON.toJson(root));atomic(report.resolve("report.md"),BenchmarkReport.markdown(state,copy));
                if(!state.equals("in progress")){
                    String completedPath=report.toAbsolutePath().normalize().toString();lastReport=completedPath;
                    Minecraft.getInstance().execute(()->{var mc=Minecraft.getInstance();if(mc.player!=null)mc.player.displayClientMessage(Component.translatable("slopium.analysis.finished",Component.translatable("slopium.analysis.reason."+reasonKey(state)),completedPath),false);});
                }
            }catch(Exception e){LogUtils.getLogger().error("Cannot write Slopium analysis report",e);Minecraft.getInstance().execute(()->{var mc=Minecraft.getInstance();if(mc.player!=null)mc.player.displayClientMessage(Component.translatable("slopium.analysis.write_failed"),false);});}
        });
    }
    private static String reasonKey(String state){return switch(state){
        case "complete" -> "complete";
        case "menu opened / paused" -> "menu";
        case "world changed", "logout" -> "world";
        case "window lost focus" -> "focus";
        case "video settings changed", "window resized" -> "settings";
        case "camera moved" -> "camera";
        case "stopped by command" -> "stop";
        default -> "error";
    };}
    private static void atomic(Path path,String value)throws java.io.IOException{var tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,value);try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}}
    public static JsonObject changes(JsonObject base,JsonObject settings){var result=new JsonObject();settings.entrySet().forEach(e->{if(!e.getValue().equals(base.get(e.getKey())))result.add(e.getKey(),e.getValue().deepCopy());});return result;}
    private static boolean terrainReady(){
        var renderer=net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer.instanceNullable();
        return renderer==null||renderer.isTerrainRenderComplete();
    }
    private static JsonObject options(){
        var mc=Minecraft.getInstance();var o=mc.options;var r=new JsonObject();
        r.addProperty("renderDistance",o.renderDistance().get());r.addProperty("effectiveRenderDistance",o.getEffectiveRenderDistance());
        r.addProperty("simulationDistance",o.simulationDistance().get());r.addProperty("maxFps",o.framerateLimit().get());r.addProperty("vsync",o.enableVsync().get());
        r.addProperty("graphics",o.graphicsMode().get().name());r.addProperty("particles",o.particles().get().name());r.addProperty("entityDistanceScaling",o.entityDistanceScaling().get());
        r.addProperty("entityShadows",o.entityShadows().get());r.addProperty("biomeBlendRadius",o.biomeBlendRadius().get());r.addProperty("mipmapLevels",o.mipmapLevels().get());
        r.addProperty("fov",o.fov().get());r.addProperty("guiScale",o.guiScale().get());r.addProperty("fullscreen",o.fullscreen().get());r.addProperty("hideGui",o.hideGui);
        return r;
    }
    private JsonObject environment(){
        var mc=Minecraft.getInstance();var r=new JsonObject();r.add("vanilla",options());
        r.addProperty("resolution",width+"x"+height);r.addProperty("gpu",GL11.glGetString(GL11.GL_RENDERER));r.addProperty("gl",GL11.glGetString(GL11.GL_VERSION));
        r.addProperty("java",System.getProperty("java.version"));r.addProperty("os",System.getProperty("os.name"));r.addProperty("processors",Runtime.getRuntime().availableProcessors());r.addProperty("maxHeapBytes",Runtime.getRuntime().maxMemory());
        r.addProperty("dimension",mc.level.dimension().location().toString());r.addProperty("position",x+", "+y+", "+z);r.addProperty("yaw",yaw);r.addProperty("pitch",pitch);
        r.addProperty("gameTime",mc.level.getGameTime());r.addProperty("raining",mc.level.isRaining());r.addProperty("integratedServer",mc.hasSingleplayerServer());
        try {
            var sodium=new JsonObject();
            for(var mod:net.caffeinemc.mods.sodium.client.config.ConfigManager.CONFIG.getModOptions()) {
                if(mod.configId().equals("slopium"))continue;
                for(var page:mod.pages())if(page instanceof net.caffeinemc.mods.sodium.client.config.structure.OptionPage p)
                    for(var group:p.groups())for(var option:group.options())if(option instanceof net.caffeinemc.mods.sodium.client.config.structure.StatefulOption state)
                        sodium.addProperty(mod.configId()+" / "+option.getName().getString(),String.valueOf(state.getBinding().load()));
            }
            r.add("otherVideoOptions",sodium);
        } catch(Exception failure){r.addProperty("otherVideoOptionsUnavailable",failure.getClass().getSimpleName());}
        var mods=new JsonObject();ModList.get().getMods().forEach(m->mods.addProperty(m.getModId(),m.getVersion().toString()));r.add("mods",mods);
        r.addProperty("limits","FPS/VSync caps remain unchanged. Dynamic entities, machines, server traffic and thermal changes can affect results. No server address, chat, token or player name recorded.");
        return r;
    }
}

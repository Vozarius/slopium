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
    private static volatile PerformanceAnalysis active;
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
    private boolean sparkRequested,profileMode,warmedQuiet;
    private int checks,busyChecks,backgroundChecks,minimizedChecks,terrainReloads,profileIndex=-1;
    private long quietSince;
    private AnalysisRuntime.Counters runtimeStart;
    private final List<Profile> profiles=new ArrayList<>();
    private List<BenchmarkPlan.Trial> profileTrials=List.of();
    private SparkProfiler.Handle profileHandle;
    private record Profile(String trial,JsonObject settings,String file,CompletableFuture<String> result){}
    public record ProfileExport(String trial,JsonObject settings,String file,String result){}
    private List<ProfileExport> profileExports(){return profiles.stream().map(p->new ProfileExport(p.trial(),p.settings(),p.file(),p.result().getNow("exporting"))).toList();}
    public record Row(int index,String id,String category,boolean control,JsonObject settings,JsonObject changes,
        BenchmarkSamples.Result metrics,long chunkEvents,boolean valid,List<String> warnings,String gpuSections,String frameGeneration,AnalysisRuntime.Quality quality){
        public Row(int index,String id,String category,boolean control,JsonObject settings,JsonObject changes,BenchmarkSamples.Result metrics,long chunkEvents,boolean valid,List<String> warnings,String gpuSections,String frameGeneration){this(index,id,category,control,settings,changes,metrics,chunkEvents,valid,warnings,gpuSections,frameGeneration,null);}
    }

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
    public static int secondsPerTrial(boolean extended){return extended?21:14;}
    public static void start(boolean extended){start(extended,SparkProfiler.available());}
    public static void start(boolean extended,boolean spark){startCustom(null,extended,extended?6_000_000_000L:4_000_000_000L,extended?15_000_000_000L:10_000_000_000L,spark);}
    /** Shortened durations are only used by the isolated integration harness. */
    public static void startCustom(List<BenchmarkPlan.Trial> trials,boolean extended,long warmup,long measurement){
        startCustom(trials,extended,warmup,measurement,SparkProfiler.available());
    }
    public static void startCustom(List<BenchmarkPlan.Trial> trials,boolean extended,long warmup,long measurement,boolean spark){
        var mc=Minecraft.getInstance();if(active!=null||mc.level==null||mc.player==null)return;
        if(SparkProfiler.busy()){mc.player.displayClientMessage(Component.translatable("slopium.analysis.spark_busy"),false);return;}
        active=new PerformanceAnalysis(extended,trials,warmup,measurement);active.sparkRequested=spark&&SparkProfiler.available();
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
    private void apply(JsonObject settings){
        var before=snapshot();if(before.equals(settings))return;
        var changes=AnalysisChanges.between(before,settings);restore(settings);
        if(changes.terrain()&&Slopium.loaded("sodiumdynamiclights"))terrainReloads++;
        Slopium.refreshOptions(changes.terrain(),changes.frameGeneration(),changes.blockFilter());
    }
    private void resetStage(){
        stageStart=System.nanoTime();measureStart=0;chunkChanges=0;quietSince=0;phase="warmup";
        terrainBusy=false;checks=busyChecks=backgroundChecks=minimizedChecks=0;warmedQuiet=false;
    }
    private void next(){
        sampling=null;
        if(++index>=trials.size()){
            if(sparkRequested){profileMode=true;selectProfiles();nextProfile();}else finish("complete");return;
        }
        apply(trials.get(index).settings());resetStage();
    }
    private void selectProfiles(){
        var selected=new LinkedHashMap<String,BenchmarkPlan.Trial>();
        var ref=BenchmarkPlan.reference(original);selected.put(ref.toString(),new BenchmarkPlan.Trial("control","profile",ref,true));
        if(!original.equals(ref))selected.put(original.toString(),new BenchmarkPlan.Trial("original","profile",original.deepCopy(),false));
        for(String key:List.of("cameraCulling=","occludeCreateTicks=","hideAllCreateModels=","freezeCreateClientTicks="))
            trials.stream().filter(t->t.id().startsWith(key)).findFirst().ifPresent(t->selected.putIfAbsent(t.settings().toString(),t));
        rows.stream().filter(r->!r.control()&&r.valid()).sorted(Comparator.comparingDouble((Row r)->r.metrics().sourceFps()).reversed()).limit(2)
            .forEach(r->selected.putIfAbsent(r.settings().toString(),new BenchmarkPlan.Trial(r.id(),"profile candidate, not proven winner",r.settings(),false)));
        profileTrials=selected.values().stream().limit(8).toList();
    }
    private void nextProfile(){
        if(++profileIndex>=profileTrials.size()){phase="export";measureStart=0;return;}
        apply(profileTrials.get(profileIndex).settings());resetStage();
    }
    private void step(){
        var mc=Minecraft.getInstance();
        if(mc.level!=world||mc.player==null){finish("world changed");return;}
        if(mc.screen!=null||mc.getOverlay()!=null||mc.isPaused()){finish("menu opened / paused");return;}
                if(mc.getWindow().getWidth()!=width||mc.getWindow().getHeight()!=height){finish("window resized");return;}
        long checked=System.nanoTime();
        if(checked-lastOptionCheck>500_000_000L){lastOptionCheck=checked;if(!options().toString().equals(startOptions)){finish("video settings changed");return;}}
        var camera=mc.gameRenderer.getMainCamera();var p=camera.getPosition();
        if(p.distanceToSqr(x,y,z)>.0025||Math.abs(net.minecraft.util.Mth.wrapDegrees(camera.getYRot()-yaw))>.15||Math.abs(camera.getXRot()-pitch)>.15){finish("camera moved");return;}
        long now=System.nanoTime();
        if(profileMode&&profileIndex>=profileTrials.size()){
            if(profiles.stream().allMatch(profile->profile.result().isDone()))finish("complete");return;
        }
        if(measureStart==0){
            if(terrainReady()){if(quietSince==0)quietSince=now;}else quietSince=0;
            if(now-stageStart<warmup)return;
            warmedQuiet=quietSince!=0&&now-quietSince>=500_000_000L;
            if(!warmedQuiet&&now-stageStart<warmup+10_000_000_000L)return;
            measureStart=now;chunkChanges=0;
            if(profileMode){
                profileHandle=SparkProfiler.start();phase="profile";
                if(profileHandle==null){var t=profileTrials.get(profileIndex);profiles.add(new Profile(t.id(),t.settings(),"",CompletableFuture.completedFuture(SparkProfiler.status())));nextProfile();}return;
            }
            runtimeStart=AnalysisRuntime.counters();sampling=new BenchmarkSamples(now);phase="measure";return;
        }
        if(profileMode){
            if(now-measureStart<Math.max(measurement,5_000_000_000L))return;
            var t=profileTrials.get(profileIndex);String file=String.format(Locale.ROOT,"spark/%02d.sparkprofile",profileIndex+1);
            profiles.add(new Profile(t.id(),t.settings(),file,profileHandle.finish(report.resolve(file),"Slopium diagnostic only, excluded from FPS comparisons: "+t.id())));profileHandle=null;
            write("profiling");nextProfile();return;
        }
        checks++;if(!terrainReady()){terrainBusy=true;busyChecks++;}
        long window=mc.getWindow().getWindow();
        if(org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(window,org.lwjgl.glfw.GLFW.GLFW_FOCUSED)==0)backgroundChecks++;
        if(org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(window,org.lwjgl.glfw.GLFW.GLFW_ICONIFIED)!=0)minimizedChecks++;
        if(now-measureStart<measurement)return;
        var s=sampling;sampling=null;var metrics=s.result(now-measureStart);var t=trials.get(index);
        var quality=AnalysisRuntime.difference(runtimeStart,AnalysisRuntime.counters(),checks,busyChecks,backgroundChecks,minimizedChecks,warmedQuiet);
        var warnings=new ArrayList<String>();
        if(terrainBusy)warnings.add(String.format(Locale.ROOT,"Terrain busy in %.1f%% of checks; compare with adjacent controls",quality.terrainPercent()));
        if(!warmedQuiet)warnings.add("No quiet terrain interval before measurement; ongoing workload");
        if(quality.collectors().values().stream().anyMatch(gc->gc.kind()==AnalysisRuntime.GcKind.UNKNOWN))warnings.add("Unclassified GC counters are reported separately; pause time may be incomplete");
        if(quality.gcMillis()>metrics.seconds()*100)warnings.add("GC pauses occupied over 10% of measurement");
        if(chunkChanges>0)warnings.add("Chunks loaded/unloaded during measurement: "+chunkChanges);
        if(metrics.sourceFrames()<30)warnings.add("Fewer than 30 real frames");
        if(metrics.withinRunDriftPercent()>15)warnings.add("Real FPS drifted over 15% within this measurement");
        if(metrics.truncated())warnings.add("Percentiles limited to the first 131072 intervals");
        if(metrics.p95Ms()>metrics.p50Ms()*2)warnings.add("Large frame-time spread");
        if(!t.settings().get("frameGenerationMode").getAsString().equals("OFF")&&metrics.generatedFrames()==0)warnings.add("Frame generation produced no synthetic output; inspect backend status");
        rows.add(new Row(index,t.id(),t.category(),t.control(),snapshot(),changes(BenchmarkPlan.reference(original),snapshot()),metrics,chunkChanges,
            chunkChanges==0&&metrics.sourceFrames()>=30&&metrics.withinRunDriftPercent()<=15&&quality.gcMillis()<=metrics.seconds()*100,warnings,dev.slopium.visibility.GpuSections.status(),FrameGeneration.status()+" | "+dev.slopium.framegen.AsyncPresenter.status(),quality));
        write("in progress");next();
    }
    private void finish(String reason){
        sampling=null;
        if(profileHandle!=null){profileHandle.cancel();profileHandle=null;}
        try{apply(original);active=null;
            var mc=Minecraft.getInstance();mc.noRender=org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),org.lwjgl.glfw.GLFW.GLFW_ICONIFIED)!=0;net.caffeinemc.mods.sodium.client.config.ConfigManager.CONFIG.resetAllOptionsFromBindings();}
        finally{active=null;write(reason);}
    }
    public static Component progress(){
        var a=active;if(a==null)return Component.empty();
        long profileDuration=a.warmup+Math.max(a.measurement,5_000_000_000L);
        long remaining=(a.profileMode?Math.max(0,a.profileTrials.size()-a.profileIndex)*profileDuration:
            (a.trials.size()-a.index)*(a.warmup+a.measurement)+(a.sparkRequested?8*profileDuration:0))/1_000_000_000L;
        return Component.translatable("slopium.analysis.progress",a.profileMode?Math.min(a.profileIndex+1,a.profileTrials.size()):a.index+1,a.profileMode?a.profileTrials.size():a.trials.size(),Component.translatable("slopium.analysis."+a.phase),remaining/60);
    }
    public static void overlay(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event){
        if(active!=null)event.getGuiGraphics().drawString(Minecraft.getInstance().font,progress(),8,8,0xffffffff);
    }
    private void write(String state){
        // Freeze on the render thread; serialize and perform filesystem I/O away from timed rendering.
        var copy=List.copyOf(rows);var root=new JsonObject();root.addProperty("schema",3);root.addProperty("state",state);
        root.addProperty("plannedTrials",trials.size());root.addProperty("warmupSeconds",warmup/1e9);root.addProperty("measurementSeconds",measurement/1e9);
        root.add("originalSettings",original);root.add("referenceSettings",BenchmarkPlan.reference(original));root.add("environment",environment);
        root.addProperty("method","A-B-B-A: two adjacent variant measurements bracketed by immediate controls; no skipping invalid controls. Frame times measured without an analyzer-started Spark recording; existing server/background samplers are not controlled. Optional separate 10 ms Java Spark diagnostics, all threads, no uploads. Foreground behavior forced during entire run. Only light-source changes rebuild terrain.");
        root.addProperty("terrainReloads",terrainReloads);root.addProperty("sparkRequested",sparkRequested);root.addProperty("sparkAdapter",SparkProfiler.status());
        root.add("sparkProfiles",JSON.toJsonTree(profileExports()));
        WRITER.execute(()->{
            try{
                Files.createDirectories(report);root.add("trials",JSON.toJsonTree(copy));
                atomic(report.resolve("report.json"),JSON.toJson(root));atomic(report.resolve("report.md"),BenchmarkReport.markdown(state,copy)+"\n## Spark diagnostics (excluded from FPS comparisons)\n\n"+JSON.toJson(root.get("sparkProfiles")));
                if(state.equals("complete"))archive();
                if(!state.equals("in progress")&&!state.equals("profiling")){
                    String completedPath=report.toAbsolutePath().normalize().toString();lastReport=completedPath;
                    Minecraft.getInstance().execute(()->{var mc=Minecraft.getInstance();if(mc.player!=null)mc.player.displayClientMessage(Component.translatable("slopium.analysis.finished",Component.translatable("slopium.analysis.reason."+reasonKey(state)),completedPath),false);});
                }
            }catch(Exception e){LogUtils.getLogger().error("Cannot write Slopium analysis report",e);Minecraft.getInstance().execute(()->{var mc=Minecraft.getInstance();if(mc.player!=null)mc.player.displayClientMessage(Component.translatable("slopium.analysis.write_failed"),false);});}
        });
    }
    private void archive()throws java.io.IOException{
        var zip=report.resolveSibling(report.getFileName()+".zip");var temp=zip.resolveSibling(zip.getFileName()+".tmp");
        try(var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(temp));var files=Files.walk(report)){
            for(var file:files.filter(Files::isRegularFile).filter(p->!p.getFileName().toString().endsWith(".tmp")).toList()){
                out.putNextEntry(new java.util.zip.ZipEntry(report.relativize(file).toString().replace('\\','/')));Files.copy(file,out);out.closeEntry();
            }
        }
        Files.move(temp,zip,StandardCopyOption.REPLACE_EXISTING);
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
    private static void atomic(Path path,String value)throws java.io.IOException{
        var tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,value);
        for(int attempt=0;;attempt++)try{
            try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}return;
        }catch(java.nio.file.AccessDeniedException busy){if(attempt>=4)throw busy;java.util.concurrent.locks.LockSupport.parkNanos(20_000_000L);}
    }
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
        r.addProperty("pauseOnLostFocus",o.pauseOnLostFocus);r.addProperty("fov",o.fov().get());r.addProperty("guiScale",o.guiScale().get());r.addProperty("fullscreen",o.fullscreen().get());r.addProperty("hideGui",o.hideGui);
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

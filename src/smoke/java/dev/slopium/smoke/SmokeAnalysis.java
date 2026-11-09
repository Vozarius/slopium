package dev.slopium.smoke;

import dev.slopium.benchmark.*;
import dev.slopium.client.SlopiumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import java.nio.file.*;
import java.util.*;

/** Real source/swap callbacks, full restore, actual on-disk JSON and menu cancellation. */
final class SmokeAnalysis {
    private int stage;
    private int minimizedTicks;
    private boolean restoredWhileMinimized;
    SmokeAnalysis(){net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->{
        if(stage==0||stage>=3)return;var mc=Minecraft.getInstance();
        if(dev.slopium.benchmark.PerformanceAnalysis.running()){
            if(!mc.isWindowActive())throw new AssertionError("Analysis focus override not active");
            if(org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),org.lwjgl.glfw.GLFW.GLFW_ICONIFIED)!=0)minimizedTicks++;
        }else{if(org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),org.lwjgl.glfw.GLFW.GLFW_ICONIFIED)!=0)restoredWhileMinimized=mc.noRender;org.lwjgl.glfw.GLFW.glfwRestoreWindow(mc.getWindow().getWindow());mc.noRender=false;mc.setWindowActive(true);}
    });}
    private com.google.gson.JsonObject original;
    private byte[] disk;
    private Path config;
    private String firstReport;
    void frame(){
        try{step();}catch(Exception e){throw new AssertionError("Analyzer integration failed",e);}
    }
    private void step()throws Exception{
        var mc=Minecraft.getInstance();
        if(stage==0){
            if(!SparkProfiler.available())throw new AssertionError("Spark client not attached: "+SparkProfiler.status());
            mc.options.pauseOnLostFocus=true;
            original=PerformanceAnalysis.snapshot();config=mc.gameDirectory.toPath().resolve("config/slopium.json");disk=Files.exists(config)?Files.readAllBytes(config):null;
            var base=BenchmarkPlan.reference(original);base.addProperty("frameGenerationInputFps",30);base.addProperty("frameGenerationTargetFps",120);
            var off=base.deepCopy();off.addProperty("enabled",false);
            var fg=base.deepCopy();fg.addProperty("frameGenerationMode","SIMPLE");fg.addProperty("frameGenerationParallel",true);fg.addProperty("frameGenerationMultiplier",4);fg.addProperty("frameGenerationAmortize",false);
            var trials=List.of(new BenchmarkPlan.Trial("control","",base,true),new BenchmarkPlan.Trial("disabled","",off,false),
                new BenchmarkPlan.Trial("parallel FG","FG",fg,false),new BenchmarkPlan.Trial("control","",base,true));
            PerformanceAnalysis.startCustom(trials,false,3_000_000_000L,3_000_000_000L);stage=1;mc.setWindowActive(false);org.lwjgl.glfw.GLFW.glfwIconifyWindow(mc.getWindow().getWindow());return;
        }
        if(stage==1){
            if(PerformanceAnalysis.running()||PerformanceAnalysis.lastReport().isEmpty())return;
            mc.setWindowActive(false);if(mc.isWindowActive())throw new AssertionError("Focus override survived completion");mc.setWindowActive(true);
            if(!restoredWhileMinimized)throw new AssertionError("Minimized render state not restored");
            if(!original.equals(PerformanceAnalysis.snapshot()))throw new AssertionError("Settings not restored after completion");
            checkDisk();firstReport=PerformanceAnalysis.lastReport();
            var root=com.google.gson.JsonParser.parseString(Files.readString(Path.of(firstReport,"report.json"))).getAsJsonObject();
            if(!root.get("state").getAsString().equals("complete")||root.getAsJsonArray("trials").size()!=4)throw new AssertionError("Incomplete report: "+root.get("state"));
            if(minimizedTicks<20)throw new AssertionError("Native window was not minimized long enough");
            if(!Files.isRegularFile(Path.of(firstReport+".zip")))throw new AssertionError("Missing combined report archive");
            var exports=root.getAsJsonArray("sparkProfiles");if(exports.isEmpty())throw new AssertionError("No Spark profiles");
            for(var export:exports){var record=export.getAsJsonObject();
                if(!record.get("result").getAsString().equals("saved"))throw new AssertionError("Spark export: "+record);
                try(var gzip=Files.newInputStream(Path.of(firstReport,record.get("file").getAsString()))){
                    var type=Class.forName("me.lucko.spark.proto.SparkSamplerProtos$SamplerData");var proto=type.getMethod("parseFrom",java.io.InputStream.class).invoke(null,gzip);
                    var metadata=type.getMethod("getMetadata").invoke(proto);var platform=metadata.getClass().getMethod("getPlatformMetadata").invoke(metadata);
                    if(!platform.getClass().getMethod("getType").invoke(platform).toString().equals("CLIENT"))throw new AssertionError("Profile captured server platform");
                    if((int)type.getMethod("getThreadsCount").invoke(proto)==0)throw new AssertionError("Empty Spark call tree");
                }
            }
            for(var r:root.getAsJsonArray("trials")){
                var m=r.getAsJsonObject().getAsJsonObject("metrics");var q=r.getAsJsonObject().getAsJsonObject("quality");if(q.get("minimizedChecks").getAsInt()<1)throw new AssertionError("Measurement not minimized");
                if(root.get("schema").getAsInt()!=3||q.getAsJsonObject("collectors").size()==0)throw new AssertionError("Missing per-collector GC accounting");
                long pauses=0;for(var gc:q.getAsJsonObject("collectors").entrySet())if(gc.getValue().getAsJsonObject().get("kind").getAsString().equals("PAUSE"))pauses+=gc.getValue().getAsJsonObject().get("millis").getAsLong();
                if(q.get("gcMillis").getAsLong()!=pauses)throw new AssertionError("Concurrent GC leaked into pause accounting");
                if(m.get("sourceFrames").getAsLong()<10||m.get("outputFrames").getAsLong()<10)throw new AssertionError("Missing real/swap samples");
                if(r.getAsJsonObject().get("id").getAsString().equals("parallel FG")&&m.get("generatedFrames").getAsLong()<10)throw new AssertionError("Parallel output not measured");
            }
            var owner=SparkProfiler.start();if(owner==null)throw new AssertionError("Cannot test sampler ownership");
            try{PerformanceAnalysis.startCustom(List.of(new BenchmarkPlan.Trial("busy","",original,false)),false,1,1,true);
                if(PerformanceAnalysis.running()||!SparkProfiler.busy())throw new AssertionError("Existing Spark sampler was replaced");
            }finally{owner.cancel();}
            var base=BenchmarkPlan.reference(original);base.addProperty("disableParticles",!SlopiumConfig.get().disableParticles);
            PerformanceAnalysis.startCustom(List.of(new BenchmarkPlan.Trial("cancel","",base,false)),false,3_000_000_000L,3_000_000_000L);
            mc.setScreen(new AnalysisScreen(null));
            PerformanceAnalysis.tick();
            if(PerformanceAnalysis.running()||!original.equals(PerformanceAnalysis.snapshot()))throw new AssertionError("Menu did not cancel/restore");
            checkDisk();
            mc.setScreen(null);stage=2;return;
        }
        if(stage==2&&!PerformanceAnalysis.lastReport().equals(firstReport)){
            checkDisk();com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_ANALYSIS: real world + parallel output + reports + completion/cancel restore + unchanged config + native minimized rendering + decoded Spark call trees PASSED; {}",firstReport);
            mc.stop();stage=3;
        }
    }
    private void checkDisk()throws Exception{if(disk==null?Files.exists(config):!Arrays.equals(disk,Files.readAllBytes(config)))throw new AssertionError("Analyzer changed saved config");}
}

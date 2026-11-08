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
            original=PerformanceAnalysis.snapshot();config=mc.gameDirectory.toPath().resolve("config/slopium.json");disk=Files.exists(config)?Files.readAllBytes(config):null;
            var base=BenchmarkPlan.reference(original);base.addProperty("frameGenerationInputFps",30);base.addProperty("frameGenerationTargetFps",120);
            var off=base.deepCopy();off.addProperty("enabled",false);
            var fg=base.deepCopy();fg.addProperty("frameGenerationMode","SIMPLE");fg.addProperty("frameGenerationParallel",true);fg.addProperty("frameGenerationMultiplier",4);fg.addProperty("frameGenerationAmortize",false);
            var trials=List.of(new BenchmarkPlan.Trial("control","",base,true),new BenchmarkPlan.Trial("disabled","",off,false),
                new BenchmarkPlan.Trial("parallel FG","FG",fg,false),new BenchmarkPlan.Trial("control","",base,true));
            PerformanceAnalysis.startCustom(trials,false,3_000_000_000L,3_000_000_000L);stage=1;return;
        }
        if(stage==1){
            if(PerformanceAnalysis.running()||PerformanceAnalysis.lastReport().isEmpty())return;
            if(!original.equals(PerformanceAnalysis.snapshot()))throw new AssertionError("Settings not restored after completion");
            checkDisk();firstReport=PerformanceAnalysis.lastReport();
            var root=com.google.gson.JsonParser.parseString(Files.readString(Path.of(firstReport,"report.json"))).getAsJsonObject();
            if(!root.get("state").getAsString().equals("complete")||root.getAsJsonArray("trials").size()!=4)throw new AssertionError("Incomplete report: "+root.get("state"));
            for(var r:root.getAsJsonArray("trials")){
                var m=r.getAsJsonObject().getAsJsonObject("metrics");if(m.get("sourceFrames").getAsLong()<10||m.get("outputFrames").getAsLong()<10)throw new AssertionError("Missing real/swap samples");
                if(r.getAsJsonObject().get("id").getAsString().equals("parallel FG")&&m.get("generatedFrames").getAsLong()<10)throw new AssertionError("Parallel output not measured");
            }
            var base=BenchmarkPlan.reference(original);base.addProperty("disableParticles",!SlopiumConfig.get().disableParticles);
            PerformanceAnalysis.startCustom(List.of(new BenchmarkPlan.Trial("cancel","",base,false)),false,3_000_000_000L,3_000_000_000L);
            mc.setScreen(new AnalysisScreen(null));
            PerformanceAnalysis.tick();
            if(PerformanceAnalysis.running()||!original.equals(PerformanceAnalysis.snapshot()))throw new AssertionError("Menu did not cancel/restore");
            checkDisk();
            mc.setScreen(null);stage=2;return;
        }
        if(stage==2&&!PerformanceAnalysis.lastReport().equals(firstReport)){
            checkDisk();com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_ANALYSIS: real world + parallel output + reports + completion/cancel restore + unchanged config PASSED; {}",firstReport);
            mc.stop();stage=3;
        }
    }
    private void checkDisk()throws Exception{if(disk==null?Files.exists(config):!Arrays.equals(disk,Files.readAllBytes(config)))throw new AssertionError("Analyzer changed saved config");}
}

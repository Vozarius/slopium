package dev.slopium.benchmark;

import com.google.gson.JsonObject;

/** Only source-group changes alter cached chunk lighting. */
public record AnalysisChanges(boolean terrain,boolean frameGeneration,boolean blockFilter) {
    public static AnalysisChanges between(JsonObject a,JsonObject b){
        boolean terrain=false,fg=false,filter=false;
        for(var e:b.entrySet())if(!e.getValue().equals(a.get(e.getKey()))){
            var k=e.getKey();
            terrain|=k.equals("enabled")||k.equals("createLight")||k.equals("sableLight");
            fg|=k.startsWith("frameGeneration");
            filter|=k.startsWith("processedBlock");
        }
        return new AnalysisChanges(terrain,fg,filter);
    }
}

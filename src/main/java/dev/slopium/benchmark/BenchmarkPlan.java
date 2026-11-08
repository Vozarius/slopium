package dev.slopium.benchmark;

import com.google.gson.JsonObject;
import java.util.*;

/** Deterministic, bounded screening, not an exhaustive Cartesian product. */
public final class BenchmarkPlan {
    public record Trial(String id, String category, JsonObject settings, boolean control) {}
    private static final Set<String> EXCLUDED = Set.of("enabled", "diagnostics", "throttleFlywheelDistance",
        "opaqueGlassParticles", "createRenderAnyBlock", "flywheelAnyBlock", "animationAnyBlock", "tickAnyBlock", "sectionsAnyBlock");
    private static final Map<String,String> PARENTS = Map.of(
        "visibleSections", "gpuGlassCulling", "lightQueryCache", "indexedLighting");
    private BenchmarkPlan() {}

    public static JsonObject reference(JsonObject saved) {
        var base=saved.deepCopy();
        base.addProperty("enabled",true);
        base.addProperty("frameGenerationMode","OFF");
        return base;
    }
    public static List<Trial> build(JsonObject saved, boolean extended) {
        var base=reference(saved);
        var variants=new ArrayList<Trial>();
        variants.add(new Trial("original", "original settings (including FG)", saved.deepCopy(),false));
        var off=base.deepCopy();off.addProperty("enabled",false);
        variants.add(new Trial("disabled", "Slopium disabled; FG off",off,false));
        for(var entry:base.entrySet()) {
            String key=entry.getKey();
            if(!entry.getValue().isJsonPrimitive()||!entry.getValue().getAsJsonPrimitive().isBoolean()
                ||key.startsWith("frameGeneration")||EXCLUDED.contains(key))continue;
            var c=base.deepCopy();boolean next=!entry.getValue().getAsBoolean();c.addProperty(key,next);
            if(next&&PARENTS.containsKey(key))c.addProperty(PARENTS.get(key),true);
            variants.add(new Trial(key+"="+next, category(key), c,false));
        }
        for(boolean any:new boolean[]{false,true}) {
            var c=base.deepCopy();
            for(String key:List.of("occludeParticles","occludeCreateRendering","occludeFlywheel","occludeCreateAnimations"))c.addProperty(key,true);
            for(String key:List.of("opaqueGlassParticles","createRenderAnyBlock","flywheelAnyBlock","animationAnyBlock"))c.addProperty(key,any);
            variants.add(new Trial("walls="+(any?"any":"opaque"),"combined visibility; visual tradeoff",c,false));
            c=base.deepCopy();c.addProperty("visibleSections",true);c.addProperty("gpuGlassCulling",true);c.addProperty("sectionsAnyBlock",any);
            variants.add(new Trial("sections="+(any?"any":"opaque"),"GPU section visibility; visual tradeoff",c,false));
        }
        addGroup(variants,base,"particles",List.of("cacheParticleLight","particleRange","occludeParticles","particleBudget"));
        addGroup(variants,base,"create",List.of("occludeCreateRendering","occludeFlywheel","occludeCreateAnimations","throttleCreateVisuals","factoryItemRange","factoryBulbRange","factoryConnectionRange"));
        addGroup(variants,base,"items",List.of("itemStackLod","itemImpostors","limitItemRendering","disableEntityShadows"));
        addGroup(variants,base,"combined",List.of("cacheParticleLight","particleRange","occludeParticles","particleBudget","occludeCreateRendering","occludeFlywheel","occludeCreateAnimations","throttleCreateVisuals","factoryItemRange","factoryBulbRange","factoryConnectionRange","itemStackLod","itemImpostors","limitItemRendering"));
        for(String mode:List.of("SIMPLE","FSR"))for(boolean parallel:new boolean[]{false,true}) {
            var c=base.deepCopy();c.addProperty("frameGenerationMode",mode);c.addProperty("frameGenerationParallel",parallel);
            variants.add(new Trial("FG="+mode+"/parallel="+parallel,"frame generation; compare source AND output",c,false));
        }
        if(extended) {
            String[][] numeric={{"particleRenderDistance","particleRange"},{"createVisualDistance","throttleCreateVisuals"},
                {"factoryItemDistance","factoryItemRange"},{"factoryBulbDistance","factoryBulbRange"},
                {"factoryConnectionDistance","factoryConnectionRange"},{"itemLodDistance","itemStackLod"},
                {"itemRenderDistance","limitItemRendering"},{"blockEntityRenderDistance","limitBlockEntityRendering"}};
            for(var pair:numeric)for(int distance:new int[]{16,64}) {
                var c=base.deepCopy();c.addProperty(pair[1],true);c.addProperty(pair[0],distance);
                variants.add(new Trial(pair[0]+"="+distance,"distance / visual tradeoff",c,false));
            }
            for(int budget:new int[]{32,256}){var c=base.deepCopy();c.addProperty("particleBudget",true);c.addProperty("particlesPerTick",budget);variants.add(new Trial("particlesPerTick="+budget,"particle density tradeoff",c,false));}
            for(String[] pair:new String[][]{{"createVisualInterval","throttleCreateVisuals"},{"createTickInterval","distanceCreateTicks"},{"hiddenTickInterval","throttleHiddenCreate"}})
                for(int interval:new int[]{2,8}){var c=base.deepCopy();c.addProperty(pair[1],true);c.addProperty(pair[0],interval);variants.add(new Trial(pair[0]+"="+interval,"update cadence tradeoff",c,false));}
            for(int radius:new int[]{0,2}){var c=base.deepCopy();c.addProperty("visibleSections",true);c.addProperty("gpuGlassCulling",true);c.addProperty("minimumSections",radius);variants.add(new Trial("minimumSections="+radius,"GPU section visibility; protected radius",c,false));}
            for(String mode:List.of("SIMPLE","FSR"))for(int multiplier:new int[]{2,4,8}) {
                var c=base.deepCopy();c.addProperty("frameGenerationMode",mode);c.addProperty("frameGenerationParallel",true);c.addProperty("frameGenerationAmortize",false);c.addProperty("frameGenerationMultiplier",multiplier);
                variants.add(new Trial("FG="+mode+"/parallel/x"+multiplier,"frame generation; compare source AND output",c,false));
            }
            // All six pairs of the four independent groups, as well as the all-groups trial above.
            var groups=variants.stream().filter(t->Set.of("particles","create","items","walls=opaque").contains(t.id())).toList();
            for(int i=0;i<groups.size();i++)for(int j=i+1;j<groups.size();j++) {
                var c=base.deepCopy();mergeChanges(c,base,groups.get(i).settings());mergeChanges(c,base,groups.get(j).settings());
                variants.add(new Trial(groups.get(i).id()+" + "+groups.get(j).id(),"combined; visual tradeoff",c,false));
            }
        }
        // Remove ineffective duplicates; retain original as a distinct measurement.
        var unique=new LinkedHashMap<String,Trial>();
        for(var t:variants)if(!t.settings().equals(base)||t.id().equals("original"))unique.putIfAbsent(t.settings().toString(),t);
        var result=new ArrayList<Trial>();
        var shuffled=new ArrayList<>(unique.values());Collections.shuffle(shuffled,new Random(15371L));
        result.add(new Trial("control", "reference: current settings, enabled, FG off",base.deepCopy(),true));
        // Finish both repeats of a small batch before moving on: a stopped run can still be useful.
        for(int offset=0;offset<shuffled.size();offset+=4) {
            var batch=new ArrayList<>(shuffled.subList(offset,Math.min(offset+4,shuffled.size())));
            for(int repeat=0;repeat<2;repeat++) {
                if(repeat>0)Collections.shuffle(batch,new Random(15372L+offset));
                result.addAll(batch);
                result.add(new Trial("control","reference",base.deepCopy(),true));
            }
        }
        return List.copyOf(result);
    }
    private static void addGroup(List<Trial> list,JsonObject base,String id,List<String> fields){
        var c=base.deepCopy();for(var f:fields)c.addProperty(f,true);list.add(new Trial(id,"combined; visual tradeoff",c,false));
    }
    private static void mergeChanges(JsonObject target,JsonObject base,JsonObject source){source.entrySet().forEach(e->{if(!e.getValue().equals(base.get(e.getKey())))target.add(e.getKey(),e.getValue().deepCopy());});}
    private static String category(String key){
        if(key.equals("createLight")||key.equals("sableLight"))return "lighting sources / visual tradeoff";
        if(key.contains("Ticks")||key.equals("throttleHiddenCreate"))return "client update cadence; possible visual/compatibility effects";
        if(key.startsWith("disable")||key.startsWith("hide")||key.startsWith("freeze")||key.equals("pauseXaeroBenchmark"))return "diagnostic removal; functionality/visual tradeoff";
        return "single feature (prerequisites included); inspect settings diff";
    }
}

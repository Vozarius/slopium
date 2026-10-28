package dev.slopium.smoke;

import dev.slopium.client.*;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Tests real registered Sodium controls and unsaved dependency updates. Does not write config files. */
@SuppressWarnings({"rawtypes","unchecked"})
final class SmokeOptionsChecks {
    static StatefulOption option(String key) {
        return (StatefulOption)ConfigManager.CONFIG.getOption(ResourceLocation.fromNamespaceAndPath("slopium",key));
    }
    static void value(String key,Object v){option(key).modifyValue(v);}
    static Object enumValue(String key,String name){
        return Enum.valueOf(((Enum)option(key).getBinding().load()).getDeclaringClass(),name);
    }
    static void run()throws ReflectiveOperationException {
        var c=SlopiumConfig.get();var saved=new HashMap<java.lang.reflect.Field,Object>();
        for(var f:SlopiumConfig.class.getFields())saved.put(f,f.get(c));
        try {
            var mod=ConfigManager.CONFIG.getModOptions().stream().filter(m->m.configId().equals("slopium")).findFirst().orElseThrow();
            var ids=new HashSet<Object>();var id=Option.class.getDeclaredField("id");id.setAccessible(true);
            for(var page:mod.pages())for(var group:((OptionPage)page).groups())for(var o:group.options()){
                if(!ids.add(id.get(o)))throw new AssertionError("Duplicate UI option "+id.get(o));
                if(o.getName().getString().startsWith("slopium.") || o.getTooltip().getString().startsWith("slopium."))throw new AssertionError("Missing translation "+id.get(o));
            }
            value("enabled",true);value("particle_visibility",enumValue("particle_visibility","OFF"));value("particle_range",false);
            value("frame_generation_mode",enumValue("frame_generation_mode","OFF"));
            if(option("frame_generation_target").isEnabled())throw new AssertionError("Inactive FG target enabled");
            value("frame_generation_mode",enumValue("frame_generation_mode","SIMPLE"));
            if(!option("frame_generation_grid").isEnabled()||option("frame_generation_cell").isEnabled()||option("frame_generation_fsr_debug").isEnabled())throw new AssertionError("Simple FG controls conflict");
            value("frame_generation_mode",enumValue("frame_generation_mode","FSR"));
            value("frame_generation_multiplier",2);
            if(option("frame_generation_cell").isEnabled()||!option("frame_generation_fsr_debug").isEnabled())throw new AssertionError("FSR controls conflict");
            value("frame_generation_multiplier",32);
            if(!option("frame_generation_cell").isEnabled()||!option("frame_generation_multiplier").isEnabled())throw new AssertionError("FSR hybrid controls disabled");
            value("frame_generation_multiplier",2);
            value("frame_generation_mode",enumValue("frame_generation_mode","OFF"));
            if(option("frame_generation_multiplier").isEnabled()||option("frame_generation_mark").isEnabled())throw new AssertionError("Inactive FG multiplier/marker enabled");
            if(option("particle_render_distance").isEnabled())throw new AssertionError("Inactive radius enabled");
            value("particle_range",true);
            if(!option("particle_render_distance").isEnabled())throw new AssertionError("Radius did not react to unsaved toggle");
            value("particle_visibility",enumValue("particle_visibility","ALL"));
            if(option("particle_render_distance").isEnabled() || !option("particle_visibility").isEnabled())throw new AssertionError("Full cut did not override particle controls");
            value("particle_visibility",enumValue("particle_visibility","OFF"));
            if(!option("particle_visibility").isEnabled())throw new AssertionError("Particle controls did not restore");
            for(String wall:List.of("particle_visibility","create_visibility","flywheel_visibility","animation_visibility")){
                option(wall).getBinding().save(enumValue(wall,"ALL"));
                if(!option(wall).getBinding().load().toString().equals("ALL"))throw new AssertionError("Full mode migration failed");
                option(wall).getBinding().save(enumValue(wall,"ANY"));
                if(!option(wall).getBinding().load().toString().equals("ANY"))throw new AssertionError("Combined any-block setting failed");
                option(wall).getBinding().save(enumValue(wall,"OFF"));
                if(!option(wall).getBinding().load().toString().equals("OFF"))throw new AssertionError("Combined off setting failed");
            }
            if(Slopium.loaded("create")){
                value("block_models",enumValue("block_models","SHOW"));
                value("create_tick_mode",enumValue("create_tick_mode","DISTANCE"));
                if(!option("create_tick_distance").isEnabled() || option("hidden_grace").isEnabled())throw new AssertionError("Distance controls conflict");
                c.throttleHiddenCreate=true;c.occludeCreateTicks=true;c.distanceCreateTicks=true;
                option("create_tick_mode").getBinding().save(enumValue("create_tick_mode","DISTANCE"));
                if(c.occludeCreateTicks || c.throttleHiddenCreate || !c.distanceCreateTicks)throw new AssertionError("Competing tick flags not cleared");
                value("create_tick_mode",enumValue("create_tick_mode","ANY"));
                if(option("create_tick_distance").isEnabled() || !option("hidden_grace").isEnabled())throw new AssertionError("Wall controls conflict");
                value("create_tick_mode",enumValue("create_tick_mode","ALL"));
                if(!option("create_tick_mode").isEnabled() || option("tick_interval").isEnabled())throw new AssertionError("Freeze override failed");
                value("block_models",enumValue("block_models","SHOW"));value("factory_connection_mode",enumValue("factory_connection_mode","NEAR"));
                if(!option("factory_connection_distance").isEnabled())throw new AssertionError("Connection radius disabled");
                value("factory_connection_mode",enumValue("factory_connection_mode","HIDE"));
                if(option("factory_connection_distance").isEnabled())throw new AssertionError("Hidden connections still have active radius");
            }
            value("block_models",enumValue("block_models","HIDE"));
            if(option("create_visibility").isEnabled() || option("factory_items").isEnabled() || option("be_render_distance").isEnabled())throw new AssertionError("Global model cut did not override controls");
            value("enabled",false);
            if(option("particle_visibility").isEnabled() || !option("enabled").isEnabled() || !option("diagnostics").isEnabled())throw new AssertionError("Global enable dependency failed");
            com.mojang.logging.LogUtils.getLogger().info("SLOPIUM_SMOKE: unique translated controls, unsaved dependencies, combined modes and diagnostic overrides passed ({} controls)",ids.size());
        } finally {
            for(var e:saved.entrySet())e.getKey().set(c,e.getValue());
            ConfigManager.CONFIG.resetAllOptionsFromBindings();
        }
    }
}

package dev.slopium.benchmark;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Optional adapter: the Spark-linked class is loaded only when Spark's client platform is attached. */
public final class SparkProfiler {
    private static Object platform;
    private static String status="Spark client unavailable";
    public interface Handle {CompletableFuture<String> finish(Path file,String comment);void cancel();}
    private SparkProfiler(){}
    public static void attach(Object plugin){
        try{var field=plugin.getClass().getSuperclass().getDeclaredField("platform");field.setAccessible(true);platform=field.get(plugin);status="Spark client attached";}
        catch(ReflectiveOperationException e){status="Spark adapter unavailable: "+e.getClass().getSimpleName();}
    }
    public static boolean available(){return platform!=null;}
    public static boolean busy(){
        if(platform==null)return false;
        try{var container=platform.getClass().getMethod("getSamplerContainer").invoke(platform);return container.getClass().getMethod("getActiveSampler").invoke(container)!=null;}
        catch(ReflectiveOperationException e){return false;}
    }
    public static String status(){return status;}
    public static Handle start(){
        if(platform==null)return null;
        try{return (Handle)Class.forName("dev.slopium.benchmark.SparkSupport").getMethod("start",Object.class).invoke(null,platform);}
        catch(ReflectiveOperationException|LinkageError e){var cause=e.getCause()==null?e:e.getCause();status="Spark profiling unavailable: "+cause.getClass().getSimpleName()+": "+cause.getMessage();return null;}
    }
}

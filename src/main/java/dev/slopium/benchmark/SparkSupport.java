package dev.slopium.benchmark;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.*;
import me.lucko.spark.common.sampler.java.MergeStrategy;
import me.lucko.spark.common.command.sender.CommandSender;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;

/** Spark 1.10 client internals are isolated here; no commands, uploads, or manual samplers are stopped. */
public final class SparkSupport {
    public static SparkProfiler.Handle start(Object value){
        var platform=(SparkPlatform)value;var container=platform.getSamplerContainer();
        if(container.getActiveSampler()!=null)throw new IllegalStateException("Spark already recording; user's sampler left untouched");
        var sampler=new SamplerBuilder().samplingInterval(10).forceJavaSampler(true).threadDumper(ThreadDumper.ALL)
            .threadGrouper(ThreadGrouper.BY_NAME).ignoreSleeping(true).start(platform);
        container.setActiveSampler(sampler);
        return new SparkProfiler.Handle(){
            private boolean stopped;
            private synchronized void stop(){if(!stopped){stopped=true;sampler.stop(false);container.unsetActiveSampler(sampler);}}
            public void cancel(){stop();}
            public CompletableFuture<String> finish(Path file,String comment){
                stop();
                return CompletableFuture.supplyAsync(()->{
                    try{
                        var props=new Sampler.ExportProps().comment(comment).mergeStrategy(MergeStrategy.SEPARATE_PARENT_CALLS)
                            .creator(new CommandSender.Data("Slopium analysis",new UUID(0,0))).classSourceLookup(platform::createClassSourceLookup);
                        var proto=sampler.toProto(platform,props);Files.createDirectories(file.getParent());
                        Files.write(file,proto.toByteArray());
                        return "saved";
                    }catch(Exception e){return "failed: "+e.getClass().getSimpleName()+": "+e.getMessage();}
                });
            }
        };
    }
}

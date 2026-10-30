package dev.slopium.framegen;

/** Per-presenter high-resolution waitable timer. No process/global timer-resolution changes.
 * The render thread owns lifetime/wakeup; close only after the worker has joined. */
public final class PresentationTimer implements AutoCloseable {
    private long handle;
    private PresentationTimer(long handle){this.handle=handle;}
    public static PresentationTimer open() {
        try {FsrNative.load();long handle=create();return handle==0?null:new PresentationTimer(handle);}
        catch(Exception|LinkageError e){com.mojang.logging.LogUtils.getLogger().warn("Slopium precise presenter timer unavailable; using JVM waits",e);return null;}
    }
    public void await(long nanos){if(!awaitNative(handle,Math.clamp(nanos,50_000L,5_000_000L)))throw new IllegalStateException("Presenter timer wait failed");}
    public void wake(){wakeNative(handle);}
    public void close(){if(handle!=0){destroy(handle);handle=0;}}
    private static native long create();
    private static native boolean awaitNative(long handle,long nanos);
    private static native void wakeNative(long handle);
    private static native void destroy(long handle);
}

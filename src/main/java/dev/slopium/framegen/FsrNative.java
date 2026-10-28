package dev.slopium.framegen;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Loads only the packaged, versioned runtime; never searches PATH for native libraries. */
final class FsrNative {
    private static Path runtime;
    static synchronized Path load() throws Exception {
        if(runtime!=null)return runtime;
        if(!System.getProperty("os.name").startsWith("Windows")||!System.getProperty("os.arch").equals("amd64"))
            throw new IllegalStateException("FSR bridge requires Windows x64");
        Path folder=net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("slopium/native/alpha19");
        Files.createDirectories(folder);
        for(String file:new String[]{"slopium_fsr.dll","amd_fidelityfx_loader_dx12.dll","amd_fidelityfx_framegeneration_dx12.dll"}) {
            try(var input=FsrNative.class.getResourceAsStream("/native/windows-x64/"+file)) {
                if(input==null)throw new IllegalStateException("Missing packaged runtime: "+file);
                byte[] bytes=input.readAllBytes();
                String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                // Content-address the bridge so an updated development binary is never silently reused.
                if(file.equals("slopium_fsr.dll")) {
                    Path dll=folder.resolve("slopium_fsr-"+hash+".dll");
                    if(!Files.exists(dll))Files.write(dll,bytes,StandardOpenOption.CREATE_NEW);
                    System.load(dll.toAbsolutePath().toString());
                } else {
                    Path dll=folder.resolve(file);
                    if(!Files.exists(dll))Files.write(dll,bytes,StandardOpenOption.CREATE_NEW);
                    else if(!MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(dll)),MessageDigest.getInstance("SHA-256").digest(bytes)))
                        throw new IllegalStateException("FSR runtime differs from packaged version: "+file);
                }
            }
        }
        return runtime=folder.resolve("amd_fidelityfx_framegeneration_dx12.dll").toAbsolutePath();
    }
    static native long create(int w,int h,long luid,String path);
    static native long[] resources(long ptr);
    static native String provider(long ptr);
    static native void generate(long ptr,long frame,boolean reset,float delta,float near,float far,float fov,boolean debug);
    static native void destroy(long ptr);
}

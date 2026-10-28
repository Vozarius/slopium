# Slopium FSR bridge

Original JNI bridge, Windows x64. Uses AMD FidelityFX **frame generation**, not upscaling. The current pinned SDK is v2.3.0; the runtime provider tested on NVIDIA is **3.1.6**.

## Build

Requirements: Java 21 (`JAVA_HOME`), PowerShell 7 (`pwsh`), CMake in PATH, Visual Studio 2019/2022 C++ tools and a Windows SDK.

From the repository root, prepare the pinned dependencies once:

```powershell
pwsh -NoProfile -File native/prepare-fsr.ps1
```

Then the regular `gradlew.bat build --offline` compiles the bridge, verifies SHA-256 hashes, and packages the native DLL and AMD runtime. No download is performed by the normal Gradle build. Native artifacts are under `build/native/Release`; SDK downloads are under `build/fsr-sdk`. To compile the native bridge separately:

```powershell
pwsh -NoProfile -File native/build.ps1 -Offline
```

`fsr-dependencies.json` pins every downloaded file. For a new SDK, review its API and license, update the pins deliberately, and repeat the GPU smoke tests. Do not bypass a hash mismatch.

## Runtime

The packaged libraries are extracted under the game directory's `slopium/native/alpha19`. The JNI bridge is content-addressed; AMD files must match the bundled bytes. Only absolute paths are loaded. No system DLL installation or driver changes are performed.

The official AMD DLL depends on the Microsoft Visual C++ x64 runtime (`MSVCP140`, `VCRUNTIME140`, `VCRUNTIME140_1`). On machines missing it, FSR reports a loader failure and ordinary rendering continues; the simple generator does not use these native dependencies.

Shared D3D12 textures are imported with `GL_EXT_memory_object_win32`; GPU fence ownership transfers use `GL_EXT_semaphore_win32`. Resources stay on the adapter owning Minecraft's OpenGL context. Three command allocators are recycled after GPU completion. Shutdown and resize wait for outstanding work. Frame image readback is used only by smoke tests.

## Licenses

Slopium's bridge and shaders are original code under the repository license. No PFG or Screamix1/FrameGen code is bundled. AMD SDK headers retain their notices; the **AMD binary runtime has its own redistribution license**, reproduced in the release JAR as `LICENSE-AMD-FidelityFX.md`. It is not covered by Slopium's license. The complete SDK license is fetched and hash-verified along with the binaries; signed AMD binaries are redistributed unchanged.

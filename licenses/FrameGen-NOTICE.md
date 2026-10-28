# FrameGen integration

Source: https://github.com/Screamix1/FrameGen (user-provided checkout, 2026-10-28).
Copyright (C) 2026 Screamix. LGPL-3.0-or-later.

`dev/slopium/framegen/reference/Shaders.java` and `GlProgram.java` were copied
from FrameGen with their package changed. Copyright and license headers remain.
`Reprojector.java` adapts FrameGenRenderer's background and grid passes. It uses
Slopium-owned history textures, restores the caller's exact OpenGL state, keeps
reprojected depth, and omits duplicate resource management, UI and HUD caching.
These three files, including the modifications, are LGPL-3.0-or-later.

The library sources are embedded under `sources/` in the binary JAR. The companion
sources JAR contains the application sources, resources and build scripts so the
library can be modified and the combined application rebuilt. Replacing these
classes in the JAR is also permitted; Slopium does not verify their signatures.
Reverse engineering for debugging modifications of this library is permitted.
The complete LGPL and incorporated GPL texts accompany both distributions.

The remaining original Slopium code retains its MIT license. AMD's separately
licensed runtime retains LICENSE-AMD-FidelityFX.md; it is not part of FrameGen.

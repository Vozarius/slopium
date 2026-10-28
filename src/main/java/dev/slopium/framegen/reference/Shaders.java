/*
 * FrameGen -- frame generation for Minecraft via depth reprojection.
 * Copyright (C) 2026 Screamix
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.slopium.framegen.reference;

/**
 * GLSL sources, embedded so nothing depends on the resource pipeline.
 *
 * Everything targets {@code #version 150 core}, which is what Minecraft's
 * OpenGL 3.2 core context guarantees.
 */
public final class Shaders {

    private Shaders() {}

    /** Fullscreen triangle, no vertex buffer, driven purely by gl_VertexID. */
    public static final String FULLSCREEN_VS = """
            #version 150 core

            out vec2 vUv;

            void main() {
                vec2 p = vec2(gl_VertexID == 1 ? 2.0 : 0.0,
                              gl_VertexID == 2 ? 2.0 : 0.0);
                vUv = p;
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    /**
     * Capture pass: copies Minecraft's main colour buffer and its depth buffer
     * into our own history textures (RGBA8 + R32F) in a single MRT draw.
     * Reading depth through a shader instead of glBlitFramebuffer keeps us
     * independent of whatever internal depth format the driver picked.
     */
    public static final String CAPTURE_FS = """
            #version 150 core

            uniform sampler2D uSrcColor;
            uniform sampler2D uSrcDepth;

            in vec2 vUv;

            out vec4 outColor;
            out vec4 outDepth;

            void main() {
                outColor = vec4(texture(uSrcColor, vUv).rgb, 1.0);
                outDepth = vec4(texture(uSrcDepth, vUv).r, 0.0, 0.0, 1.0);
            }
            """;

    /**
     * Background layer. Reprojects the history image assuming the camera only
     * rotated. That mapping is depth independent, so it covers every pixel with
     * no holes -- exact while standing still, a good guess otherwise. Drawn
     * first, with depth writes off, so the parallax-correct grid pass can
     * overwrite it and any discarded (disoccluded) region still shows something
     * plausible instead of black.
     */
    public static final String BACKGROUND_FS = """
            #version 150 core

            uniform sampler2D uHistColor;
            uniform sampler2D uHistDepth;
            uniform mat4 uNewInvVP;
            uniform mat4 uPrevVP;
            uniform mat4 uPrevInvVP;
            uniform mat4 uNewVP;
            uniform vec3 uDeltaPos;
            uniform float uDebugDepth;
            uniform float uNearLock;
            uniform vec2 uNearFar;
            uniform float uPassthrough;

            in vec2 vUv;

            out vec4 outColor;

            // Turning the head exposes a strip of screen the captured frame never
            // covered. Clamping there stretches a single row of pixels across it,
            // which reads as long streaks from the border. Mirroring instead folds
            // the adjacent strip back, which is still wrong but looks like content.
            vec2 edgeSample(vec2 uv) {
                // Mirror exactly once, then clamp. A plain mod() is periodic: a
                // pixel whose reprojection lands far outside the frame wraps around
                // and pulls in unrelated content, which shows up as repeating bands
                // marching across the screen as the camera turns.
                vec2 m = abs(uv);
                m = mix(m, 2.0 - m, step(1.0, m));
                return clamp(m, 0.0, 1.0);
            }

            float linearDepth(float depth) {
                float n = uNearFar.x;
                float f = uNearFar.y;
                return (2.0 * n * f) / (f + n - (depth * 2.0 - 1.0) * (f - n));
            }

            void main() {
                if (uDebugDepth > 0.5) {
                    // Diagnostic view of the captured depth buffer.
                    // Red = nothing was ever written there (raw depth still at the
                    // far plane). A fully red screen means depth capture is dead,
                    // which is the one thing that would break the parallax pass.
                    float d = textureLod(uHistDepth, vUv, 0.0).r;
                    outColor = d >= 0.999999
                            ? vec4(1.0, 0.0, 0.0, 1.0)
                            : vec4(vec3(pow(1.0 - d, 0.15)), 1.0);
                    return;
                }

                if (uPassthrough > 0.5) {
                    // The camera did not move at all, so the previous frame is
                    // already the correct answer. Reprojecting it would resample
                    // between texels and come back very slightly softer than a real
                    // frame -- invisible at 400 fps, but with vsync the two frame
                    // types alternate slowly enough for that difference to read as
                    // a flicker. Hand the pixels back untouched instead.
                    outColor = vec4(texture(uHistColor, vUv).rgb, 1.0);
                    return;
                }

                vec2 ndc = vUv * 2.0 - 1.0;

                vec4 nearP = uNewInvVP * vec4(ndc, -1.0, 1.0);
                vec4 farP  = uNewInvVP * vec4(ndc,  1.0, 1.0);
                vec3 dir = normalize(farP.xyz / farP.w - nearP.xyz / nearP.w);

                vec4 clip = uPrevVP * vec4(dir * 1024.0, 1.0);
                vec2 uv = clip.w > 0.0001 ? (clip.xy / clip.w) * 0.5 + 0.5 : vUv;
                uv = edgeSample(uv);

                // The rotation-only guess above treats the whole scene as infinitely
                // far away. That is exact while the camera only turns, but anything
                // close -- the player model in third person, four blocks out -- has
                // enormous parallax, and the guess lands a whole body-width off. The
                // grid pass then draws the correct copy while this layer shows the
                // displaced one through the discarded silhouette, which reads as the
                // character being duplicated.
                //
                // So refine the guess: un-project where we landed, shift it by the
                // camera translation, and see where that point actually belongs on
                // screen. The difference is the correction. Two steps converge on
                // any continuous surface.
                for (int i = 0; i < 2; i++) {
                    float d = min(textureLod(uHistDepth, uv, 0.0).r, 0.999999);
                    if (d >= 0.999) break;   // sky has no parallax to correct for

                    vec4 p = uPrevInvVP * vec4(uv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
                    vec4 landedClip = uNewVP * vec4(p.xyz / p.w + uDeltaPos, 1.0);
                    if (landedClip.w <= 0.0001) break;

                    vec2 landed = (landedClip.xy / landedClip.w) * 0.5 + 0.5;
                    uv = edgeSample(uv + (vUv - landed));
                }

                // The held item is camera-locked, so rotating it with the world
                // drags a smeared copy of it across the screen -- a ghost hand
                // trailing the real one. Wherever this layer would pull in hand
                // pixels, take them from the same place on screen instead, which is
                // exactly where the warp pass pins them.
                if (uNearLock > 0.0 && linearDepth(textureLod(uHistDepth, uv, 0.0).r) < uNearLock) {
                    uv = vUv;
                }

                outColor = vec4(texture(uHistColor, uv).rgb, 1.0);
            }
            """;

    /** Blits the cached HUD layer back over the frame. */
    public static final String HUD_COMPOSITE_FS = """
            #version 150 core

            uniform sampler2D uHud;

            in vec2 vUv;

            out vec4 outColor;

            void main() {
                outColor = texture(uHud, vUv);
            }
            """;

    /**
     * Depth-aware forward warp.
     *
     * A grid of vertices covers the screen; every vertex reads the history
     * depth, un-projects to a camera-relative world position, shifts it by the
     * camera translation since the captured frame and re-projects through the
     * current view-projection. Depth testing between the resulting triangles
     * resolves occlusion for free.
     *
     * Vertices sitting on a depth discontinuity are flagged, and the fragment
     * stage drops those triangles rather than letting them smear across the
     * silhouette; the background layer shows through instead.
     *
     * No vertex buffer is bound -- the grid is reconstructed from gl_VertexID.
     */
    public static final String WARP_VS = """
            #version 150 core

            uniform sampler2D uHistDepth;
            uniform mat4 uPrevInvVP;
            uniform mat4 uNewVP;
            uniform vec3 uDeltaPos;
            uniform ivec2 uGrid;
            uniform float uEdgeThreshold;
            uniform float uNearLock;
            uniform vec2 uNearFar;

            out vec2 vUv;
            out float vEdge;

            const float SKY_DEPTH = 0.999;
            const float DEPTH_CLAMP = 0.999999;

            vec3 unproject(vec2 uv, float depth) {
                vec4 p = uPrevInvVP * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
                return p.xyz / p.w;
            }

            // Silhouette detection only needs relative distances, and a scalar
            // linearisation is a fraction of the cost of un-projecting every
            // neighbour through a full matrix.
            float linearDepth(float depth) {
                float n = uNearFar.x;
                float f = uNearFar.y;
                return (2.0 * n * f) / (f + n - (depth * 2.0 - 1.0) * (f - n));
            }

            void main() {
                // One grid row per instance. Deriving the whole grid from a single
                // gl_VertexID meant counts in the millions, and drivers are free to
                // split a draw that large into internal batches -- with the vertex
                // counter restarting in each one, which repeats rows of the grid as
                // horizontal bands marching down the screen. Per-row instancing keeps
                // both counters small enough that this cannot happen.
                int qx = gl_VertexID / 6;
                int corner = gl_VertexID - qx * 6;
                int qy = gl_InstanceID;

                vec2 c;
                if (corner == 0)      c = vec2(0.0, 0.0);
                else if (corner == 1) c = vec2(1.0, 0.0);
                else if (corner == 2) c = vec2(0.0, 1.0);
                else if (corner == 3) c = vec2(1.0, 0.0);
                else if (corner == 4) c = vec2(1.0, 1.0);
                else                  c = vec2(0.0, 1.0);

                vec2 uv = (vec2(float(qx), float(qy)) + c) / vec2(uGrid);
                vUv = uv;

                vec2 texStep = 1.0 / vec2(uGrid);
                vec2 uvR = min(uv + vec2(texStep.x, 0.0), vec2(1.0));
                vec2 uvU = min(uv + vec2(0.0, texStep.y), vec2(1.0));

                float d = min(textureLod(uHistDepth, uv, 0.0).r, DEPTH_CLAMP);
                float z = linearDepth(d);

                // Camera-locked geometry stays put. Under Iris the held item is
                // rendered inside renderLevel and is therefore baked into the
                // captured image; warping it as if it were world geometry makes it
                // slide across the screen. Anything this close to the eye is the
                // hand, not the world.
                if (uNearLock > 0.0 && z < uNearLock) {
                    vEdge = 0.0;
                    gl_Position = vec4(uv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
                    return;
                }

                float dR = min(textureLod(uHistDepth, uvR, 0.0).r, DEPTH_CLAMP);
                float dU = min(textureLod(uHistDepth, uvU, 0.0).r, DEPTH_CLAMP);

                float rel = max(abs(z - linearDepth(dR)), abs(z - linearDepth(dU)))
                          / max(z, 0.001);
                vEdge = rel > uEdgeThreshold ? 1.0 : 0.0;

                vec3 pos = unproject(uv, d);

                // Sky pixels carry no meaningful parallax; translating them
                // would only amplify float error at the far plane.
                vec3 world = d < SKY_DEPTH ? pos + uDeltaPos : pos;

                gl_Position = uNewVP * vec4(world, 1.0);
            }
            """;

    public static final String WARP_FS = """
            #version 150 core

            uniform sampler2D uHistColor;
            uniform float uTint;
            uniform float uDebugDepth;
            uniform float uSharpen;
            uniform vec2 uTexel;

            in vec2 vUv;
            in float vEdge;

            out vec4 outColor;

            void main() {
                if (uDebugDepth > 0.5) discard;
                if (vEdge > 0.02) discard;

                vec3 c = texture(uHistColor, vUv).rgb;

                // Reprojection lands on fractional texels, so bilinear filtering
                // softens every synthesised frame. An unsharp mask puts the edges
                // back, which matters a lot on Minecraft's hard-edged textures.
                if (uSharpen > 0.0) {
                    vec3 blur = texture(uHistColor, vUv + vec2(uTexel.x, 0.0)).rgb
                              + texture(uHistColor, vUv - vec2(uTexel.x, 0.0)).rgb
                              + texture(uHistColor, vUv + vec2(0.0, uTexel.y)).rgb
                              + texture(uHistColor, vUv - vec2(0.0, uTexel.y)).rgb;
                    c = clamp(c + (c - blur * 0.25) * uSharpen, 0.0, 1.0);
                }

                outColor = vec4(mix(c, vec3(1.0, 0.35, 0.35), uTint), 1.0);
            }
            """;
}

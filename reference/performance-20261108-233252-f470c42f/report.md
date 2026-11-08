# Slopium performance analysis

Status: complete

Real FPS counts completed world renders. Output FPS counts window presentations (including held images); it is not a count of unique new images. Frame times are intervals between completed real frames, not isolated GPU time.

Reference: your saved settings with Slopium enabled and frame generation OFF. Changes below are relative to that reference. Caps and VSync are unchanged; capped results cannot establish available performance headroom. GPU section timings are cumulative diagnostics, not isolated per-trial timings.

Each variant uses A-B-B-A: two repeats between immediate controls. Invalid controls are never skipped. Terrain-busy, background/minimized and GC proportions must match the controls; FPS is sampled before the analyzer starts Spark. Per-second source counts and within-run drift are in JSON; drift over 15% invalidates a measurement. 'Faster/slower in this run' requires both repeats to agree beyond max(5%, control drift), at most 10% control drift, comparable background work, and no chunk streaming. These are screening results, not proof for other scenes. Visual removals and client tick changes have tradeoffs. Read the full JSON settings and warnings before keeping a change.

| Variant | Real FPS effect | Assessment |
|---|---:|---|
| FG=FSR/parallel=true | n/a | inconclusive / incomplete |
| limitBlockEntityRendering=false | n/a | inconclusive / incomplete |
| hideAllCreateModels=true | n/a | inconclusive / incomplete |
| cameraCulling=false | n/a | inconclusive / incomplete |
| factoryConnectionDistance=16 | n/a | inconclusive / incomplete |
| createVisualDistance=16 | n/a | inconclusive / incomplete |
| particlesPerTick=256 | n/a | inconclusive / incomplete |
| sections=opaque | n/a | inconclusive / incomplete |
| factoryConnectionRange=false | n/a | inconclusive / incomplete |
| disableEntityShadows=true | n/a | inconclusive / incomplete |
| sableLight=false | n/a | inconclusive / incomplete |
| FG=FSR/parallel=false | n/a | inconclusive / incomplete |
| createVisualInterval=8 | n/a | inconclusive / incomplete |
| occludeFlywheel=false | n/a | inconclusive / incomplete |
| FG=SIMPLE/parallel/x2 | n/a | inconclusive / incomplete |
| walls=opaque + particles | n/a | inconclusive / incomplete |
| freezeCreateAnimations=true | n/a | inconclusive / incomplete |
| blockEntityRenderDistance=64 | n/a | inconclusive / incomplete |
| factoryConnectionDistance=64 | n/a | inconclusive / incomplete |
| itemRenderDistance=16 | n/a | inconclusive / incomplete |
| particleRenderDistance=64 | n/a | inconclusive / incomplete |
| indexedLighting=false | n/a | inconclusive / incomplete |
| FG=FSR/parallel/x2 | n/a | inconclusive / incomplete |
| FG=SIMPLE/parallel/x4 | +1.6% | inconclusive / incomplete |
| hiddenTickInterval=8 | -3.3% | inconclusive / incomplete |
| disableFlywheelRendering=true | n/a | inconclusive / incomplete |
| cullBreakingOverlays=false | n/a | inconclusive / incomplete |
| occludeParticles=false | n/a | inconclusive / incomplete |
| hideFactoryConnections=true | n/a | inconclusive / incomplete |
| factoryBulbRange=false | n/a | inconclusive / incomplete |
| createLight=false | n/a | inconclusive / incomplete |
| factoryItemDistance=64 | n/a | inconclusive / incomplete |
| cacheParticleLight=false | n/a | inconclusive / incomplete |
| disabled | n/a | inconclusive / incomplete |
| createVisualDistance=64 | n/a | inconclusive / incomplete |
| itemImpostors=false | n/a | inconclusive / incomplete |
| occludeCreateTicks=false | n/a | inconclusive / incomplete |
| particleRenderDistance=16 | n/a | inconclusive / incomplete |
| disableAmbientEffects=false | n/a | inconclusive / incomplete |
| factoryItemDistance=16 | n/a | inconclusive / incomplete |
| walls=opaque + items | n/a | inconclusive / incomplete |
| FG=SIMPLE/parallel/x8 | n/a | inconclusive / incomplete |
| disableBlockEntityRendering=true | n/a | inconclusive / incomplete |
| disableParticles=true | n/a | inconclusive / incomplete |
| blockEntityRenderDistance=16 | n/a | inconclusive / incomplete |
| FG=FSR/parallel/x4 | n/a | inconclusive / incomplete |
| walls=opaque | n/a | inconclusive / incomplete |
| throttleCreateVisuals=false | n/a | inconclusive / incomplete |
| throttleHiddenCreate=true | n/a | inconclusive / incomplete |
| pauseXaeroBenchmark=false | n/a | inconclusive / incomplete |
| factoryBulbDistance=64 | n/a | inconclusive / incomplete |
| factoryItemRange=false | n/a | inconclusive / incomplete |
| particleBudget=false | n/a | inconclusive / incomplete |
| lightQueryCache=false | n/a | inconclusive / incomplete |
| FG=SIMPLE/parallel=false | n/a | inconclusive / incomplete |
| gpuGlassCulling=false | n/a | inconclusive / incomplete |
| particlesPerTick=32 | n/a | inconclusive / incomplete |
| createTickInterval=2 | n/a | inconclusive / incomplete |
| hiddenTickInterval=2 | n/a | inconclusive / incomplete |
| factoryBulbDistance=16 | n/a | inconclusive / incomplete |
| visibleSections=false | n/a | inconclusive / incomplete |
| FG=SIMPLE/parallel=true | n/a | inconclusive / incomplete |
| createTickInterval=8 | n/a | inconclusive / incomplete |
| particleRange=true | n/a | inconclusive / incomplete |
| disableTextureInterpolation=false | n/a | inconclusive / incomplete |
| particles + items | n/a | inconclusive / incomplete |
| distanceCreateTicks=true | n/a | inconclusive / incomplete |
| original | n/a | inconclusive / incomplete |
| freezeCreateClientTicks=true | n/a | inconclusive / incomplete |
| itemLodDistance=64 | n/a | inconclusive / incomplete |
| occludeCreateRendering=false | n/a | inconclusive / incomplete |
| occludeCreateAnimations=false | n/a | inconclusive / incomplete |
| minimumSections=2 | n/a | inconclusive / incomplete |
| FG=FSR/parallel/x8 | n/a | inconclusive / incomplete |
| itemStackLod=false | n/a | inconclusive / incomplete |
| limitItemRendering=false | n/a | inconclusive / incomplete |
| createVisualInterval=2 | n/a | inconclusive / incomplete |

## Measurements

| # | Variant | Real FPS | Output FPS | Generated | p50 ms | p95 ms | p99 ms | Valid |
|---:|---|---:|---:|---:|---:|---:|---:|---|
| 1 | control | 17.29 | 17.29 | 0 | 52.31 | 88.74 | 118.97 | false |
| 2 | FG=FSR/parallel=true | 18.85 | 58.35 | 593 | 50.53 | 82.61 | 97.10 | true |
| 3 | FG=FSR/parallel=true | 18.41 | 58.76 | 607 | 50.16 | 94.87 | 111.72 | false |
| 4 | control | 21.39 | 21.39 | 0 | 46.68 | 69.09 | 87.53 | true |
| 5 | limitBlockEntityRendering=false | 20.48 | 20.48 | 0 | 48.45 | 59.35 | 79.39 | false |
| 6 | limitBlockEntityRendering=false | 23.19 | 23.19 | 0 | 45.50 | 55.85 | 64.50 | false |
| 7 | control | 24.05 | 24.05 | 0 | 44.59 | 55.88 | 65.38 | false |
| 8 | hideAllCreateModels=true | 27.87 | 27.87 | 0 | 40.98 | 51.82 | 57.22 | false |
| 9 | hideAllCreateModels=true | 28.14 | 28.14 | 0 | 40.46 | 52.01 | 61.43 | false |
| 10 | control | 17.15 | 17.15 | 0 | 53.02 | 91.42 | 114.64 | false |
| 11 | cameraCulling=false | 26.92 | 26.92 | 0 | 40.66 | 52.69 | 59.05 | false |
| 12 | cameraCulling=false | 29.11 | 29.11 | 0 | 38.59 | 48.63 | 57.40 | true |
| 13 | control | 24.13 | 24.13 | 0 | 44.83 | 53.89 | 59.66 | true |
| 14 | factoryConnectionDistance=16 | 23.26 | 23.26 | 0 | 45.28 | 55.29 | 64.04 | false |
| 15 | factoryConnectionDistance=16 | 19.98 | 19.98 | 0 | 48.35 | 73.21 | 95.48 | false |
| 16 | control | 21.09 | 21.09 | 0 | 47.50 | 62.27 | 94.24 | false |
| 17 | createVisualDistance=16 | 20.75 | 20.75 | 0 | 46.91 | 62.27 | 100.84 | false |
| 18 | createVisualDistance=16 | 24.00 | 24.00 | 0 | 44.50 | 56.67 | 70.56 | false |
| 19 | control | 18.35 | 18.35 | 0 | 50.27 | 81.56 | 86.72 | false |
| 20 | particlesPerTick=256 | 20.39 | 20.39 | 0 | 48.58 | 67.99 | 75.95 | false |
| 21 | particlesPerTick=256 | 20.20 | 20.20 | 0 | 49.06 | 60.61 | 81.40 | false |
| 22 | control | 23.00 | 23.00 | 0 | 45.37 | 55.44 | 61.64 | false |
| 23 | sections=opaque | 23.57 | 23.57 | 0 | 45.04 | 55.38 | 65.83 | true |
| 24 | sections=opaque | 22.80 | 22.80 | 0 | 45.61 | 56.76 | 83.46 | false |
| 25 | control | 21.64 | 21.64 | 0 | 47.07 | 58.68 | 78.75 | false |
| 26 | factoryConnectionRange=false | 19.86 | 19.86 | 0 | 48.88 | 63.96 | 88.71 | true |
| 27 | factoryConnectionRange=false | 21.99 | 21.99 | 0 | 46.84 | 56.14 | 62.01 | false |
| 28 | control | 24.91 | 24.91 | 0 | 43.43 | 53.09 | 70.82 | true |
| 29 | disableEntityShadows=true | 25.37 | 25.37 | 0 | 43.46 | 52.42 | 63.80 | false |
| 30 | disableEntityShadows=true | 21.59 | 21.59 | 0 | 46.78 | 59.64 | 83.88 | false |
| 31 | control | 19.52 | 19.52 | 0 | 47.00 | 76.19 | 162.25 | false |
| 32 | sableLight=false | 26.94 | 26.94 | 0 | 41.66 | 50.52 | 58.82 | true |
| 33 | sableLight=false | 26.53 | 26.53 | 0 | 40.22 | 51.24 | 124.02 | false |
| 34 | control | 28.53 | 28.53 | 0 | 40.08 | 50.72 | 58.86 | false |
| 35 | FG=FSR/parallel=false | 14.22 | 33.16 | 274 | 64.22 | 98.28 | 112.02 | false |
| 36 | FG=FSR/parallel=false | 13.53 | 32.93 | 278 | 69.17 | 97.79 | 101.44 | true |
| 37 | control | 26.07 | 26.07 | 0 | 42.33 | 53.04 | 61.64 | true |
| 38 | createVisualInterval=8 | 25.41 | 25.41 | 0 | 43.29 | 55.22 | 61.91 | true |
| 39 | createVisualInterval=8 | 19.09 | 19.09 | 0 | 49.58 | 84.81 | 96.20 | false |
| 40 | control | 21.46 | 21.46 | 0 | 47.00 | 60.54 | 76.88 | false |
| 41 | occludeFlywheel=false | 20.57 | 20.57 | 0 | 48.01 | 65.91 | 85.80 | true |
| 42 | occludeFlywheel=false | 22.38 | 22.38 | 0 | 46.59 | 57.33 | 72.52 | true |
| 43 | control | 25.25 | 25.25 | 0 | 43.22 | 55.22 | 71.51 | false |
| 44 | FG=SIMPLE/parallel/x2 | 23.28 | 42.97 | 296 | 44.76 | 56.84 | 75.80 | false |
| 45 | FG=SIMPLE/parallel/x2 | 25.66 | 45.72 | 302 | 42.83 | 53.21 | 59.45 | true |
| 46 | control | 24.53 | 24.53 | 0 | 43.87 | 55.16 | 65.73 | false |
| 47 | walls=opaque + particles | 26.94 | 26.94 | 0 | 41.19 | 53.16 | 69.93 | false |
| 48 | walls=opaque + particles | 24.46 | 24.46 | 0 | 43.85 | 55.47 | 65.41 | false |
| 49 | control | 25.67 | 25.67 | 0 | 43.29 | 53.47 | 63.78 | false |
| 50 | freezeCreateAnimations=true | 29.78 | 29.78 | 0 | 38.73 | 49.56 | 58.98 | true |
| 51 | freezeCreateAnimations=true | 28.60 | 28.60 | 0 | 40.17 | 49.86 | 52.53 | true |
| 52 | control | 24.61 | 24.61 | 0 | 43.19 | 58.28 | 82.44 | false |
| 53 | blockEntityRenderDistance=64 | 24.15 | 24.15 | 0 | 43.97 | 56.06 | 65.68 | false |
| 54 | blockEntityRenderDistance=64 | 25.47 | 25.47 | 0 | 43.30 | 54.16 | 61.18 | false |
| 55 | control | 22.71 | 22.71 | 0 | 45.50 | 58.22 | 70.97 | false |
| 56 | factoryConnectionDistance=64 | 18.88 | 18.88 | 0 | 50.45 | 76.27 | 159.16 | false |
| 57 | factoryConnectionDistance=64 | 19.43 | 19.43 | 0 | 48.44 | 85.75 | 144.25 | false |
| 58 | control | 26.03 | 26.03 | 0 | 42.73 | 53.75 | 58.38 | false |
| 59 | itemRenderDistance=16 | 27.01 | 27.01 | 0 | 41.43 | 52.38 | 61.56 | true |
| 60 | itemRenderDistance=16 | 24.94 | 24.94 | 0 | 42.96 | 61.20 | 81.55 | false |
| 61 | control | 19.95 | 19.95 | 0 | 47.61 | 74.65 | 108.06 | false |
| 62 | particleRenderDistance=64 | 25.80 | 25.80 | 0 | 42.16 | 55.05 | 65.38 | false |
| 63 | particleRenderDistance=64 | 27.51 | 27.51 | 0 | 40.54 | 51.57 | 56.72 | false |
| 64 | control | 25.46 | 25.46 | 0 | 42.94 | 53.28 | 65.58 | false |
| 65 | indexedLighting=false | 24.20 | 24.20 | 0 | 43.07 | 53.70 | 63.90 | true |
| 66 | indexedLighting=false | 24.92 | 24.92 | 0 | 43.21 | 53.17 | 61.94 | false |
| 67 | control | 29.52 | 29.52 | 0 | 39.06 | 49.16 | 56.84 | false |
| 68 | FG=FSR/parallel/x2 | 27.30 | 47.21 | 299 | 41.23 | 51.66 | 64.58 | false |
| 69 | FG=FSR/parallel/x2 | 28.59 | 48.74 | 303 | 39.68 | 50.35 | 56.81 | false |
| 70 | control | 28.44 | 28.44 | 0 | 40.11 | 50.43 | 55.84 | true |
| 71 | FG=SIMPLE/parallel/x4 | 29.39 | 86.32 | 855 | 39.15 | 48.68 | 53.97 | true |
| 72 | FG=SIMPLE/parallel/x4 | 23.62 | 81.91 | 876 | 43.87 | 59.49 | 68.98 | false |
| 73 | control | 29.44 | 29.44 | 0 | 38.95 | 51.86 | 61.15 | true |
| 74 | hiddenTickInterval=8 | 26.06 | 26.06 | 0 | 42.15 | 52.08 | 61.31 | true |
| 75 | hiddenTickInterval=8 | 27.51 | 27.51 | 0 | 41.05 | 53.03 | 62.75 | false |
| 76 | control | 24.45 | 24.45 | 0 | 43.78 | 54.93 | 72.60 | true |
| 77 | disableFlywheelRendering=true | 26.18 | 26.18 | 0 | 42.25 | 52.22 | 59.60 | true |
| 78 | disableFlywheelRendering=true | 23.94 | 23.94 | 0 | 44.28 | 54.83 | 67.12 | false |
| 79 | control | 22.86 | 22.86 | 0 | 44.02 | 64.33 | 125.08 | false |
| 80 | cullBreakingOverlays=false | 24.21 | 24.21 | 0 | 43.89 | 55.41 | 75.14 | false |
| 81 | cullBreakingOverlays=false | 22.34 | 22.34 | 0 | 46.43 | 58.67 | 65.31 | false |
| 82 | control | 21.79 | 21.79 | 0 | 46.37 | 60.06 | 74.08 | false |
| 83 | occludeParticles=false | 24.04 | 24.04 | 0 | 44.47 | 56.20 | 63.06 | false |
| 84 | occludeParticles=false | 24.32 | 24.32 | 0 | 44.16 | 54.78 | 67.33 | false |
| 85 | control | 24.93 | 24.93 | 0 | 43.49 | 55.97 | 60.76 | false |
| 86 | hideFactoryConnections=true | 22.72 | 22.72 | 0 | 46.09 | 57.65 | 69.43 | false |
| 87 | hideFactoryConnections=true | 26.99 | 26.99 | 0 | 41.58 | 52.22 | 56.41 | false |
| 88 | control | 26.06 | 26.06 | 0 | 42.44 | 53.23 | 56.93 | false |
| 89 | factoryBulbRange=false | 26.71 | 26.71 | 0 | 41.65 | 52.99 | 59.13 | false |
| 90 | factoryBulbRange=false | 20.93 | 20.93 | 0 | 48.34 | 60.81 | 84.59 | false |
| 91 | control | 22.31 | 22.31 | 0 | 46.05 | 61.81 | 80.24 | false |
| 92 | createLight=false | 25.56 | 25.56 | 0 | 42.25 | 55.31 | 80.59 | false |
| 93 | createLight=false | 29.33 | 29.33 | 0 | 39.08 | 52.18 | 60.55 | false |
| 94 | control | 28.90 | 28.90 | 0 | 39.64 | 50.04 | 54.37 | true |
| 95 | factoryItemDistance=64 | 23.55 | 23.55 | 0 | 45.33 | 55.25 | 59.89 | true |
| 96 | factoryItemDistance=64 | 21.63 | 21.63 | 0 | 46.61 | 62.97 | 86.46 | true |
| 97 | control | 24.30 | 24.30 | 0 | 43.65 | 56.57 | 76.47 | false |
| 98 | cacheParticleLight=false | 25.11 | 25.11 | 0 | 43.17 | 53.91 | 66.84 | true |
| 99 | cacheParticleLight=false | 24.93 | 24.93 | 0 | 43.20 | 55.95 | 65.56 | false |
| 100 | control | 21.33 | 21.33 | 0 | 44.91 | 79.24 | 101.48 | false |
| 101 | disabled | 17.60 | 17.60 | 0 | 54.64 | 81.53 | 93.45 | false |
| 102 | disabled | 17.66 | 17.66 | 0 | 55.01 | 80.50 | 93.46 | false |
| 103 | control | 22.12 | 22.12 | 0 | 46.63 | 58.87 | 84.02 | false |
| 104 | createVisualDistance=64 | 25.58 | 25.58 | 0 | 42.68 | 55.12 | 64.58 | false |
| 105 | createVisualDistance=64 | 25.20 | 25.20 | 0 | 43.19 | 54.19 | 63.37 | false |
| 106 | control | 28.60 | 28.60 | 0 | 39.90 | 50.37 | 55.92 | false |
| 107 | itemImpostors=false | 31.20 | 31.20 | 0 | 38.06 | 46.70 | 51.92 | true |
| 108 | itemImpostors=false | 28.99 | 28.99 | 0 | 40.15 | 48.48 | 54.64 | false |
| 109 | control | 29.80 | 29.80 | 0 | 38.56 | 49.15 | 59.04 | false |
| 110 | occludeCreateTicks=false | 39.38 | 39.38 | 0 | 28.75 | 40.05 | 47.85 | true |
| 111 | occludeCreateTicks=false | 36.35 | 36.35 | 0 | 31.05 | 41.59 | 55.27 | false |
| 112 | control | 27.33 | 27.33 | 0 | 40.59 | 51.74 | 64.98 | false |
| 113 | particleRenderDistance=16 | 29.32 | 29.32 | 0 | 39.01 | 50.81 | 57.75 | false |
| 114 | particleRenderDistance=16 | 29.24 | 29.24 | 0 | 40.02 | 49.38 | 55.34 | true |
| 115 | control | 30.83 | 30.83 | 0 | 37.72 | 48.75 | 56.26 | true |
| 116 | disableAmbientEffects=false | 23.13 | 23.13 | 0 | 45.15 | 57.45 | 79.41 | false |
| 117 | disableAmbientEffects=false | 24.46 | 24.46 | 0 | 43.69 | 59.21 | 78.13 | false |
| 118 | control | 26.81 | 26.81 | 0 | 40.81 | 55.88 | 70.86 | false |
| 119 | factoryItemDistance=16 | 22.40 | 22.40 | 0 | 44.36 | 68.65 | 84.78 | false |
| 120 | factoryItemDistance=16 | 28.20 | 28.20 | 0 | 39.95 | 49.75 | 55.84 | false |
| 121 | control | 29.60 | 29.60 | 0 | 39.62 | 49.11 | 54.47 | false |
| 122 | walls=opaque + items | 28.80 | 28.80 | 0 | 40.14 | 49.62 | 55.14 | true |
| 123 | walls=opaque + items | 23.59 | 23.59 | 0 | 44.95 | 57.67 | 70.01 | false |
| 124 | control | 23.06 | 23.06 | 0 | 45.76 | 55.78 | 85.46 | false |
| 125 | FG=SIMPLE/parallel/x8 | 19.56 | 137.11 | 1767 | 49.41 | 82.94 | 95.87 | false |
| 126 | FG=SIMPLE/parallel/x8 | 25.03 | 143.12 | 1774 | 42.51 | 63.49 | 88.60 | false |
| 127 | control | 23.43 | 23.43 | 0 | 45.15 | 57.37 | 84.14 | false |
| 128 | disableBlockEntityRendering=true | 31.00 | 31.00 | 0 | 36.93 | 62.12 | 96.18 | false |
| 129 | disableBlockEntityRendering=true | 38.44 | 38.44 | 0 | 33.45 | 44.27 | 48.45 | true |
| 130 | control | 25.13 | 25.13 | 0 | 43.43 | 53.84 | 60.71 | false |
| 131 | disableParticles=true | 27.54 | 27.54 | 0 | 40.90 | 51.42 | 58.82 | true |
| 132 | disableParticles=true | 22.72 | 22.72 | 0 | 44.20 | 73.09 | 97.37 | false |
| 133 | control | 21.67 | 21.67 | 0 | 47.16 | 60.38 | 89.97 | true |
| 134 | blockEntityRenderDistance=16 | 24.92 | 24.92 | 0 | 43.05 | 56.64 | 78.70 | false |
| 135 | blockEntityRenderDistance=16 | 28.85 | 28.85 | 0 | 40.27 | 49.61 | 55.73 | true |
| 136 | control | 22.60 | 22.60 | 0 | 45.95 | 56.64 | 72.20 | false |
| 137 | FG=FSR/parallel/x4 | 25.55 | 83.97 | 877 | 42.99 | 51.53 | 57.89 | false |
| 138 | FG=FSR/parallel/x4 | 25.58 | 83.34 | 867 | 43.00 | 52.89 | 60.58 | false |
| 139 | control | 25.59 | 25.59 | 0 | 43.22 | 53.68 | 57.79 | true |
| 140 | walls=opaque | 22.68 | 22.68 | 0 | 45.66 | 56.98 | 72.25 | false |
| 141 | walls=opaque | 18.41 | 18.41 | 0 | 50.33 | 87.49 | 147.63 | false |
| 142 | control | 22.94 | 22.94 | 0 | 44.92 | 69.61 | 83.52 | false |
| 143 | throttleCreateVisuals=false | 24.77 | 24.77 | 0 | 43.09 | 55.52 | 74.20 | false |
| 144 | throttleCreateVisuals=false | 27.19 | 27.19 | 0 | 41.01 | 50.80 | 59.75 | false |
| 145 | control | 29.06 | 29.06 | 0 | 39.99 | 49.44 | 53.35 | false |
| 146 | throttleHiddenCreate=true | 30.19 | 30.19 | 0 | 39.37 | 46.67 | 50.72 | true |
| 147 | throttleHiddenCreate=true | 25.46 | 25.46 | 0 | 43.16 | 53.16 | 59.89 | false |
| 148 | control | 25.78 | 25.78 | 0 | 42.59 | 54.09 | 65.16 | false |
| 149 | pauseXaeroBenchmark=false | 26.13 | 26.13 | 0 | 42.10 | 53.19 | 63.80 | false |
| 150 | pauseXaeroBenchmark=false | 26.16 | 26.16 | 0 | 42.59 | 52.92 | 57.37 | false |
| 151 | control | 27.00 | 27.00 | 0 | 41.60 | 51.84 | 57.89 | true |
| 152 | factoryBulbDistance=64 | 24.02 | 24.02 | 0 | 42.87 | 70.41 | 90.95 | false |
| 153 | factoryBulbDistance=64 | 27.25 | 27.25 | 0 | 41.18 | 50.66 | 58.68 | false |
| 154 | control | 28.52 | 28.52 | 0 | 40.39 | 49.14 | 56.97 | true |
| 155 | factoryItemRange=false | 25.69 | 25.69 | 0 | 42.65 | 52.38 | 58.31 | false |
| 156 | factoryItemRange=false | 25.04 | 25.04 | 0 | 43.48 | 53.12 | 65.00 | false |
| 157 | control | 26.79 | 26.79 | 0 | 42.24 | 51.85 | 55.80 | false |
| 158 | particleBudget=false | 23.22 | 23.22 | 0 | 45.05 | 57.94 | 81.48 | false |
| 159 | particleBudget=false | 18.16 | 18.16 | 0 | 49.39 | 94.12 | 117.50 | false |
| 160 | control | 19.55 | 19.55 | 0 | 48.34 | 76.54 | 93.35 | false |
| 161 | lightQueryCache=false | 25.36 | 25.36 | 0 | 42.90 | 55.81 | 71.73 | false |
| 162 | lightQueryCache=false | 27.57 | 27.57 | 0 | 40.98 | 50.83 | 60.71 | false |
| 163 | control | 28.81 | 28.81 | 0 | 39.93 | 50.36 | 56.19 | false |
| 164 | FG=SIMPLE/parallel=false | 18.56 | 33.74 | 220 | 53.44 | 82.68 | 87.55 | false |
| 165 | FG=SIMPLE/parallel=false | 18.72 | 35.11 | 236 | 53.75 | 82.56 | 86.44 | true |
| 166 | control | 28.72 | 28.72 | 0 | 40.03 | 49.04 | 59.01 | false |
| 167 | gpuGlassCulling=false | 27.71 | 27.71 | 0 | 40.59 | 51.19 | 63.10 | false |
| 168 | gpuGlassCulling=false | 26.75 | 26.75 | 0 | 41.88 | 52.58 | 58.41 | false |
| 169 | control | 25.93 | 25.93 | 0 | 42.86 | 51.45 | 56.95 | true |
| 170 | particlesPerTick=32 | 20.82 | 20.82 | 0 | 45.87 | 80.66 | 113.92 | false |
| 171 | particlesPerTick=32 | 24.86 | 24.86 | 0 | 43.18 | 54.16 | 78.63 | true |
| 172 | control | 26.36 | 26.36 | 0 | 42.59 | 52.27 | 60.47 | false |
| 173 | createTickInterval=2 | 22.49 | 22.49 | 0 | 46.24 | 56.65 | 65.75 | false |
| 174 | createTickInterval=2 | 27.86 | 27.86 | 0 | 41.08 | 51.17 | 58.27 | false |
| 175 | control | 26.64 | 26.64 | 0 | 42.01 | 55.78 | 74.89 | false |
| 176 | hiddenTickInterval=2 | 25.38 | 25.38 | 0 | 43.49 | 52.22 | 60.28 | true |
| 177 | hiddenTickInterval=2 | 23.86 | 23.86 | 0 | 44.71 | 55.37 | 63.81 | true |
| 178 | control | 20.38 | 20.38 | 0 | 47.29 | 88.13 | 112.13 | false |
| 179 | factoryBulbDistance=16 | 28.44 | 28.44 | 0 | 39.93 | 51.30 | 59.95 | false |
| 180 | factoryBulbDistance=16 | 25.06 | 25.06 | 0 | 42.48 | 55.94 | 80.40 | false |
| 181 | control | 27.33 | 27.33 | 0 | 40.52 | 55.76 | 69.96 | false |
| 182 | visibleSections=false | 27.13 | 27.13 | 0 | 41.84 | 50.16 | 59.07 | false |
| 183 | visibleSections=false | 26.06 | 26.06 | 0 | 42.54 | 51.92 | 59.75 | false |
| 184 | control | 28.99 | 28.99 | 0 | 39.73 | 50.01 | 54.28 | false |
| 185 | FG=SIMPLE/parallel=true | 28.13 | 56.73 | 430 | 40.66 | 50.92 | 57.65 | true |
| 186 | FG=SIMPLE/parallel=true | 28.17 | 56.35 | 423 | 40.32 | 49.15 | 55.09 | true |
| 187 | control | 25.98 | 25.98 | 0 | 42.11 | 54.13 | 67.02 | false |
| 188 | createTickInterval=8 | 27.18 | 27.18 | 0 | 41.16 | 51.26 | 58.62 | false |
| 189 | createTickInterval=8 | 24.02 | 24.02 | 0 | 43.68 | 59.48 | 79.47 | false |
| 190 | control | 28.13 | 28.13 | 0 | 40.29 | 50.21 | 53.71 | true |
| 191 | particleRange=true | 29.60 | 29.60 | 0 | 39.37 | 48.88 | 52.54 | true |
| 192 | particleRange=true | 30.19 | 30.19 | 0 | 38.13 | 49.83 | 55.88 | false |
| 193 | control | 26.26 | 26.26 | 0 | 41.84 | 53.57 | 61.69 | false |
| 194 | disableTextureInterpolation=false | 26.69 | 26.69 | 0 | 41.22 | 52.65 | 57.89 | false |
| 195 | disableTextureInterpolation=false | 24.24 | 24.24 | 0 | 42.58 | 68.19 | 89.06 | false |
| 196 | control | 23.61 | 23.61 | 0 | 43.31 | 60.26 | 77.07 | false |
| 197 | particles + items | 28.40 | 28.40 | 0 | 39.87 | 49.10 | 62.14 | false |
| 198 | particles + items | 30.24 | 30.24 | 0 | 38.71 | 47.93 | 51.64 | false |
| 199 | control | 27.91 | 27.91 | 0 | 40.37 | 52.36 | 57.35 | false |
| 200 | distanceCreateTicks=true | 28.79 | 28.79 | 0 | 39.57 | 50.72 | 56.54 | false |
| 201 | distanceCreateTicks=true | 29.26 | 29.26 | 0 | 39.11 | 50.28 | 56.52 | false |
| 202 | control | 29.53 | 29.53 | 0 | 39.41 | 49.53 | 57.93 | true |
| 203 | original | 31.12 | 31.12 | 0 | 37.67 | 47.63 | 52.62 | true |
| 204 | original | 29.91 | 29.91 | 0 | 38.75 | 48.90 | 55.62 | true |
| 205 | control | 27.73 | 27.73 | 0 | 40.62 | 51.14 | 59.38 | false |
| 206 | freezeCreateClientTicks=true | 38.31 | 38.31 | 0 | 26.55 | 42.20 | 74.40 | false |
| 207 | freezeCreateClientTicks=true | 44.97 | 44.97 | 0 | 18.46 | 33.83 | 40.54 | false |
| 208 | control | 27.99 | 27.99 | 0 | 40.84 | 50.14 | 55.18 | false |
| 209 | itemLodDistance=64 | 27.59 | 27.59 | 0 | 41.16 | 51.22 | 53.47 | true |
| 210 | itemLodDistance=64 | 24.67 | 24.67 | 0 | 43.89 | 54.49 | 77.70 | true |
| 211 | control | 25.26 | 25.26 | 0 | 43.32 | 52.61 | 61.02 | true |
| 212 | occludeCreateRendering=false | 20.06 | 20.06 | 0 | 48.41 | 66.57 | 88.00 | true |
| 213 | occludeCreateRendering=false | 23.06 | 23.06 | 0 | 45.33 | 55.95 | 76.73 | true |
| 214 | control | 18.79 | 18.79 | 0 | 49.06 | 83.26 | 104.71 | false |
| 215 | occludeCreateAnimations=false | 25.27 | 25.27 | 0 | 43.08 | 56.29 | 68.54 | false |
| 216 | occludeCreateAnimations=false | 27.19 | 27.19 | 0 | 41.60 | 50.31 | 55.33 | false |
| 217 | control | 26.28 | 26.28 | 0 | 42.43 | 51.00 | 59.99 | false |
| 218 | minimumSections=2 | 26.40 | 26.40 | 0 | 42.25 | 51.33 | 56.98 | true |
| 219 | minimumSections=2 | 23.68 | 23.68 | 0 | 44.75 | 56.28 | 67.25 | false |
| 220 | control | 23.80 | 23.80 | 0 | 44.90 | 54.38 | 59.55 | true |
| 221 | FG=FSR/parallel/x8 | 25.00 | 154.47 | 1947 | 43.33 | 53.69 | 64.14 | false |
| 222 | FG=FSR/parallel/x8 | 22.46 | 149.20 | 1903 | 45.87 | 62.19 | 78.89 | false |
| 223 | control | 26.54 | 26.54 | 0 | 41.66 | 51.47 | 62.78 | false |
| 224 | itemStackLod=false | 27.77 | 27.77 | 0 | 40.77 | 50.01 | 57.82 | true |
| 225 | itemStackLod=false | 27.26 | 27.26 | 0 | 41.60 | 50.80 | 57.73 | false |
| 226 | control | 22.33 | 22.33 | 0 | 43.07 | 74.87 | 124.80 | false |
| 227 | limitItemRendering=false | 28.10 | 28.10 | 0 | 40.30 | 50.76 | 55.38 | false |
| 228 | limitItemRendering=false | 29.06 | 29.06 | 0 | 40.15 | 49.53 | 59.72 | false |
| 229 | control | 29.20 | 29.20 | 0 | 39.28 | 48.28 | 56.07 | false |
| 230 | createVisualInterval=2 | 29.21 | 29.21 | 0 | 39.44 | 48.84 | 55.72 | false |
| 231 | createVisualInterval=2 | 29.19 | 29.19 | 0 | 39.66 | 48.30 | 52.48 | true |
| 232 | control | 28.33 | 28.33 | 0 | 40.46 | 49.27 | 52.05 | true |

## Measurement quality

| # | Terrain busy % | Background % | Minimized % | Render CPU ms | Process CPU ms | GC ms | Quiet warmup |
|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 16.9 | 91.9 | 71.5 | 14421.9 | 47484.4 | 3238 | true |
| 2 | 10.2 | 100.0 | 100.0 | 14765.6 | 46437.5 | 1072 | true |
| 3 | 17.0 | 100.0 | 100.0 | 14765.6 | 47890.6 | 1964 | true |
| 4 | 9.0 | 100.0 | 100.0 | 14734.4 | 41203.1 | 757 | true |
| 5 | 0.0 | 100.0 | 100.0 | 14750.0 | 54875.0 | 2137 | true |
| 6 | 0.0 | 100.0 | 100.0 | 14671.9 | 32843.8 | 2173 | true |
| 7 | 4.2 | 100.0 | 100.0 | 14765.6 | 37937.5 | 3083 | true |
| 8 | 12.9 | 100.0 | 100.0 | 14703.1 | 44468.8 | 2094 | true |
| 9 | 15.3 | 100.0 | 100.0 | 14781.3 | 46031.3 | 3131 | true |
| 10 | 24.8 | 100.0 | 100.0 | 14781.3 | 54312.5 | 6504 | true |
| 11 | 18.8 | 100.0 | 100.0 | 14796.9 | 48890.6 | 1603 | true |
| 12 | 0.0 | 100.0 | 100.0 | 14812.5 | 38421.9 | 0 | true |
| 13 | 0.0 | 100.0 | 100.0 | 14734.4 | 34093.8 | 0 | true |
| 14 | 0.0 | 100.0 | 100.0 | 14718.8 | 33843.8 | 2132 | true |
| 15 | 14.7 | 100.0 | 100.0 | 14734.4 | 44578.1 | 3871 | true |
| 16 | 24.6 | 100.0 | 100.0 | 14796.9 | 50953.1 | 4413 | true |
| 17 | 12.8 | 100.0 | 100.0 | 14609.4 | 46390.6 | 2900 | true |
| 18 | 9.7 | 100.0 | 100.0 | 14734.4 | 42593.8 | 1811 | true |
| 19 | 14.9 | 100.0 | 100.0 | 14875.0 | 49796.9 | 4196 | true |
| 20 | 0.0 | 100.0 | 100.0 | 14718.8 | 34859.4 | 2095 | true |
| 21 | 0.0 | 100.0 | 100.0 | 14734.4 | 34531.3 | 2106 | true |
| 22 | 0.0 | 100.0 | 100.0 | 14765.6 | 39515.6 | 2118 | true |
| 23 | 9.3 | 100.0 | 100.0 | 14750.0 | 43812.5 | 1124 | true |
| 24 | 13.2 | 100.0 | 100.0 | 14734.4 | 52843.8 | 2357 | true |
| 25 | 7.1 | 100.0 | 100.0 | 14796.9 | 44125.0 | 2291 | true |
| 26 | 23.1 | 100.0 | 100.0 | 14796.9 | 49593.8 | 1105 | true |
| 27 | 0.0 | 100.0 | 100.0 | 14765.6 | 33765.6 | 2167 | true |
| 28 | 0.0 | 100.0 | 100.0 | 14781.3 | 32703.1 | 1112 | true |
| 29 | 0.0 | 100.0 | 100.0 | 14734.4 | 32296.9 | 2124 | true |
| 30 | 15.4 | 100.0 | 100.0 | 14781.3 | 53171.9 | 4454 | true |
| 31 | 18.1 | 100.0 | 100.0 | 14734.4 | 49890.6 | 3985 | true |
| 32 | 15.8 | 100.0 | 100.0 | 14671.9 | 40265.6 | 737 | true |
| 33 | 14.3 | 100.0 | 100.0 | 14343.8 | 38296.9 | 2677 | true |
| 34 | 0.0 | 100.0 | 100.0 | 14781.3 | 31484.4 | 2033 | true |
| 35 | 0.0 | 100.0 | 100.0 | 12656.3 | 34375.0 | 2005 | true |
| 36 | 0.0 | 100.0 | 100.0 | 12671.9 | 35046.9 | 1036 | true |
| 37 | 19.4 | 100.0 | 100.0 | 14671.9 | 36281.3 | 706 | true |
| 38 | 13.6 | 100.0 | 100.0 | 14875.0 | 37953.1 | 720 | true |
| 39 | 7.3 | 100.0 | 100.0 | 14671.9 | 52671.9 | 6085 | true |
| 40 | 15.5 | 100.0 | 100.0 | 14625.0 | 48046.9 | 5078 | true |
| 41 | 17.5 | 100.0 | 100.0 | 14593.8 | 46953.1 | 1131 | true |
| 42 | 0.0 | 100.0 | 100.0 | 14609.4 | 36359.4 | 1034 | true |
| 43 | 0.0 | 100.0 | 100.0 | 14718.8 | 43015.6 | 2138 | true |
| 44 | 0.3 | 100.0 | 100.0 | 14687.5 | 37921.9 | 2079 | true |
| 45 | 15.6 | 100.0 | 100.0 | 14921.9 | 43234.4 | 1012 | true |
| 46 | 17.9 | 100.0 | 100.0 | 14796.9 | 42937.5 | 1093 | true |
| 47 | 13.1 | 100.0 | 100.0 | 14718.8 | 44515.6 | 4091 | true |
| 48 | 16.6 | 100.0 | 100.0 | 14625.0 | 42546.9 | 4154 | true |
| 49 | 15.0 | 100.0 | 100.0 | 14859.4 | 43031.3 | 3077 | true |
| 50 | 4.0 | 100.0 | 100.0 | 14609.4 | 30984.4 | 0 | true |
| 51 | 0.0 | 100.0 | 100.0 | 14828.1 | 27265.6 | 0 | true |
| 52 | 1.4 | 100.0 | 100.0 | 14859.4 | 39343.8 | 1001 | true |
| 53 | 22.3 | 100.0 | 100.0 | 14796.9 | 47531.3 | 4925 | true |
| 54 | 15.4 | 100.0 | 100.0 | 14796.9 | 43828.1 | 1786 | true |
| 55 | 11.4 | 100.0 | 100.0 | 14781.3 | 48343.8 | 4081 | true |
| 56 | 14.4 | 100.0 | 100.0 | 14625.0 | 49218.8 | 3183 | true |
| 57 | 14.0 | 100.0 | 100.0 | 14796.9 | 41250.0 | 6003 | true |
| 58 | 0.0 | 100.0 | 100.0 | 14671.9 | 33000.0 | 1952 | true |
| 59 | 0.0 | 100.0 | 100.0 | 14750.0 | 31875.0 | 1015 | true |
| 60 | 16.3 | 100.0 | 100.0 | 14875.0 | 43906.3 | 2998 | true |
| 61 | 26.0 | 100.0 | 100.0 | 14812.5 | 55859.4 | 2839 | true |
| 62 | 24.7 | 100.0 | 100.0 | 14921.9 | 48750.0 | 3980 | true |
| 63 | 10.7 | 100.0 | 100.0 | 14734.4 | 45468.8 | 4958 | true |
| 64 | 9.7 | 100.0 | 100.0 | 14781.3 | 43109.4 | 5073 | true |
| 65 | 0.0 | 100.0 | 100.0 | 14453.1 | 30953.1 | 702 | true |
| 66 | 0.0 | 100.0 | 100.0 | 14796.9 | 32828.1 | 1976 | true |
| 67 | 11.3 | 100.0 | 100.0 | 14843.8 | 41437.5 | 2962 | true |
| 68 | 18.0 | 100.0 | 100.0 | 14796.9 | 45296.9 | 4198 | true |
| 69 | 8.1 | 100.0 | 100.0 | 14843.8 | 41031.3 | 2035 | true |
| 70 | 10.5 | 100.0 | 100.0 | 14796.9 | 41125.0 | 1030 | true |
| 71 | 0.0 | 100.0 | 100.0 | 14703.1 | 31000.0 | 998 | true |
| 72 | 2.3 | 88.7 | 89.0 | 14843.8 | 52968.8 | 5861 | true |
| 73 | 0.0 | 92.5 | 0.0 | 14750.0 | 29015.6 | 0 | true |
| 74 | 9.2 | 100.0 | 100.0 | 14703.1 | 37234.4 | 969 | true |
| 75 | 10.2 | 100.0 | 100.0 | 14890.6 | 41078.1 | 3922 | true |
| 76 | 10.1 | 100.0 | 100.0 | 14750.0 | 38187.5 | 783 | true |
| 77 | 10.7 | 100.0 | 100.0 | 14796.9 | 37703.1 | 717 | true |
| 78 | 0.3 | 100.0 | 100.0 | 14734.4 | 49125.0 | 2117 | true |
| 79 | 0.0 | 100.0 | 100.0 | 14578.1 | 40218.8 | 2861 | true |
| 80 | 0.0 | 100.0 | 100.0 | 14718.8 | 35062.5 | 2041 | true |
| 81 | 16.7 | 100.0 | 100.0 | 14796.9 | 42281.3 | 1826 | true |
| 82 | 25.0 | 100.0 | 100.0 | 14703.1 | 52406.3 | 2165 | true |
| 83 | 18.0 | 100.0 | 100.0 | 14796.9 | 46718.8 | 2838 | true |
| 84 | 20.0 | 100.0 | 100.0 | 14656.3 | 52875.0 | 2137 | true |
| 85 | 8.8 | 100.0 | 100.0 | 14750.0 | 45390.6 | 3198 | true |
| 86 | 0.0 | 100.0 | 100.0 | 14609.4 | 34343.8 | 2097 | true |
| 87 | 0.0 | 100.0 | 100.0 | 14718.8 | 32328.1 | 2021 | true |
| 88 | 0.0 | 100.0 | 100.0 | 14687.5 | 34812.5 | 2065 | true |
| 89 | 19.5 | 100.0 | 100.0 | 14796.9 | 44906.3 | 3993 | true |
| 90 | 19.1 | 100.0 | 100.0 | 14546.9 | 43640.6 | 2250 | true |
| 91 | 11.3 | 100.0 | 100.0 | 14640.6 | 44031.3 | 4077 | true |
| 92 | 11.5 | 100.0 | 100.0 | 14781.3 | 38453.1 | 3114 | true |
| 93 | 3.0 | 100.0 | 100.0 | 14828.1 | 36937.5 | 747 | true |
| 94 | 0.0 | 100.0 | 100.0 | 14812.5 | 29734.4 | 0 | true |
| 95 | 0.0 | 100.0 | 100.0 | 14687.5 | 32703.1 | 0 | true |
| 96 | 16.9 | 100.0 | 100.0 | 14734.4 | 45906.3 | 1051 | true |
| 97 | 19.7 | 100.0 | 100.0 | 14796.9 | 47000.0 | 2113 | true |
| 98 | 16.4 | 100.0 | 100.0 | 14781.3 | 40031.3 | 804 | true |
| 99 | 12.6 | 100.0 | 100.0 | 14796.9 | 40421.9 | 3109 | true |
| 100 | 0.3 | 100.0 | 100.0 | 14781.3 | 39343.8 | 3009 | true |
| 101 | 4.5 | 100.0 | 100.0 | 14625.0 | 40125.0 | 2016 | true |
| 102 | 0.0 | 100.0 | 100.0 | 14703.1 | 33546.9 | 2030 | true |
| 103 | 27.7 | 100.0 | 100.0 | 14656.3 | 64062.5 | 2399 | true |
| 104 | 15.6 | 100.0 | 100.0 | 14609.4 | 38640.6 | 4178 | true |
| 105 | 14.6 | 100.0 | 100.0 | 14703.1 | 43140.6 | 5162 | true |
| 106 | 10.7 | 100.0 | 100.0 | 14796.9 | 40531.3 | 4075 | true |
| 107 | 0.0 | 100.0 | 100.0 | 14734.4 | 31421.9 | 1023 | true |
| 108 | 0.0 | 100.0 | 100.0 | 14750.0 | 30656.3 | 1968 | true |
| 109 | 0.0 | 100.0 | 100.0 | 14531.3 | 30781.3 | 1939 | true |
| 110 | 16.2 | 100.0 | 100.0 | 14656.3 | 36125.0 | 1068 | true |
| 111 | 15.7 | 100.0 | 100.0 | 14750.0 | 44421.9 | 4021 | true |
| 112 | 12.7 | 100.0 | 100.0 | 14843.8 | 37484.4 | 2105 | true |
| 113 | 12.7 | 100.0 | 100.0 | 14843.8 | 37843.8 | 3006 | true |
| 114 | 0.0 | 100.0 | 100.0 | 14734.4 | 29484.4 | 696 | true |
| 115 | 0.0 | 100.0 | 100.0 | 14890.6 | 30343.8 | 721 | true |
| 116 | 19.6 | 100.0 | 100.0 | 14671.9 | 44843.8 | 3836 | true |
| 117 | 16.9 | 100.0 | 100.0 | 14531.3 | 40515.6 | 4284 | true |
| 118 | 9.4 | 100.0 | 100.0 | 14750.0 | 42203.1 | 1769 | true |
| 119 | 12.2 | 100.0 | 100.0 | 14671.9 | 46828.1 | 1753 | true |
| 120 | 0.0 | 100.0 | 100.0 | 14546.9 | 32281.3 | 1988 | true |
| 121 | 0.2 | 100.0 | 100.0 | 14703.1 | 31625.0 | 2009 | true |
| 122 | 0.0 | 100.0 | 100.0 | 14812.5 | 31375.0 | 0 | true |
| 123 | 28.8 | 100.0 | 100.0 | 14765.6 | 47921.9 | 4216 | true |
| 124 | 11.8 | 100.0 | 100.0 | 14562.5 | 44000.0 | 2188 | true |
| 125 | 20.7 | 100.0 | 100.0 | 14468.8 | 41578.1 | 4491 | true |
| 126 | 6.6 | 100.0 | 100.0 | 14515.6 | 45593.8 | 4074 | true |
| 127 | 9.9 | 100.0 | 100.0 | 14468.8 | 49390.6 | 1932 | true |
| 128 | 0.2 | 100.0 | 100.0 | 13531.3 | 29718.8 | 1058 | true |
| 129 | 0.0 | 100.0 | 100.0 | 14609.4 | 34875.0 | 761 | true |
| 130 | 0.0 | 100.0 | 100.0 | 14656.3 | 33484.4 | 2055 | true |
| 131 | 5.1 | 100.0 | 100.0 | 14671.9 | 34875.0 | 1095 | true |
| 132 | 23.2 | 100.0 | 100.0 | 14828.1 | 49640.6 | 1984 | true |
| 133 | 17.2 | 100.0 | 100.0 | 14562.5 | 40640.6 | 790 | true |
| 134 | 14.2 | 100.0 | 100.0 | 14500.0 | 38281.3 | 2240 | true |
| 135 | 24.2 | 100.0 | 100.0 | 14734.4 | 44171.9 | 1468 | true |
| 136 | 12.4 | 100.0 | 100.0 | 14671.9 | 41421.9 | 4287 | true |
| 137 | 0.0 | 100.0 | 100.0 | 14609.4 | 30515.6 | 2078 | true |
| 138 | 0.0 | 100.0 | 100.0 | 14781.3 | 33671.9 | 1964 | true |
| 139 | 0.3 | 100.0 | 100.0 | 14718.8 | 33406.3 | 1054 | true |
| 140 | 17.0 | 100.0 | 100.0 | 14640.6 | 45078.1 | 1154 | true |
| 141 | 18.4 | 100.0 | 100.0 | 14703.1 | 47625.0 | 2248 | true |
| 142 | 26.1 | 100.0 | 100.0 | 14718.8 | 50937.5 | 4265 | true |
| 143 | 26.3 | 100.0 | 100.0 | 14828.1 | 47875.0 | 2132 | true |
| 144 | 10.5 | 100.0 | 100.0 | 14859.4 | 39828.1 | 3073 | true |
| 145 | 0.0 | 100.0 | 100.0 | 14796.9 | 32890.6 | 2095 | true |
| 146 | 0.0 | 100.0 | 100.0 | 14750.0 | 27781.3 | 0 | true |
| 147 | 29.6 | 100.0 | 100.0 | 14812.5 | 44640.6 | 3084 | true |
| 148 | 13.4 | 100.0 | 100.0 | 14812.5 | 42390.6 | 4111 | true |
| 149 | 8.4 | 100.0 | 100.0 | 14750.0 | 41578.1 | 6204 | true |
| 150 | 9.7 | 100.0 | 100.0 | 14734.4 | 38546.9 | 2081 | true |
| 151 | 0.0 | 100.0 | 100.0 | 14625.0 | 41531.3 | 1073 | true |
| 152 | 1.1 | 100.0 | 100.0 | 14781.3 | 41296.9 | 2917 | true |
| 153 | 6.3 | 100.0 | 100.0 | 14875.0 | 37390.6 | 3114 | true |
| 154 | 22.2 | 100.0 | 100.0 | 14812.5 | 45390.6 | 1452 | true |
| 155 | 26.9 | 100.0 | 100.0 | 14781.3 | 43046.9 | 1867 | true |
| 156 | 19.4 | 100.0 | 100.0 | 14812.5 | 41671.9 | 2145 | true |
| 157 | 0.0 | 100.0 | 100.0 | 14859.4 | 29031.3 | 2052 | true |
| 158 | 0.0 | 100.0 | 100.0 | 14781.3 | 32718.8 | 2121 | true |
| 159 | 0.0 | 100.0 | 100.0 | 14734.4 | 38687.5 | 3137 | true |
| 160 | 36.7 | 100.0 | 100.0 | 14890.6 | 48687.5 | 3142 | true |
| 161 | 18.1 | 100.0 | 100.0 | 14781.3 | 48140.6 | 3342 | true |
| 162 | 25.6 | 100.0 | 100.0 | 14734.4 | 46609.4 | 1515 | true |
| 163 | 18.0 | 100.0 | 100.0 | 14828.1 | 44046.9 | 2101 | true |
| 164 | 0.0 | 100.0 | 100.0 | 12796.9 | 28734.4 | 2067 | true |
| 165 | 0.0 | 100.0 | 100.0 | 12406.3 | 27437.5 | 1037 | true |
| 166 | 27.6 | 100.0 | 100.0 | 14781.3 | 42140.6 | 3300 | true |
| 167 | 12.2 | 100.0 | 100.0 | 14859.4 | 43031.3 | 2048 | true |
| 168 | 9.0 | 100.0 | 100.0 | 14890.6 | 44203.1 | 4294 | true |
| 169 | 13.1 | 100.0 | 100.0 | 14796.9 | 40890.6 | 1068 | true |
| 170 | 1.3 | 100.0 | 100.0 | 14781.3 | 47781.3 | 4341 | true |
| 171 | 0.0 | 100.0 | 100.0 | 14718.8 | 38421.9 | 1098 | true |
| 172 | 0.0 | 100.0 | 100.0 | 14828.1 | 34515.6 | 2134 | true |
| 173 | 24.0 | 100.0 | 100.0 | 14843.8 | 59828.1 | 4442 | true |
| 174 | 18.9 | 100.0 | 100.0 | 14843.8 | 43984.4 | 2202 | true |
| 175 | 7.5 | 100.0 | 100.0 | 14859.4 | 47781.3 | 2670 | true |
| 176 | 12.1 | 100.0 | 100.0 | 14765.6 | 39375.0 | 750 | true |
| 177 | 0.0 | 100.0 | 100.0 | 14781.3 | 34984.4 | 0 | true |
| 178 | 0.0 | 92.5 | 93.1 | 14609.4 | 42468.8 | 1127 | true |
| 179 | 0.0 | 88.1 | 0.0 | 14812.5 | 32718.8 | 2068 | true |
| 180 | 33.2 | 100.0 | 70.7 | 14562.5 | 45734.4 | 5319 | true |
| 181 | 7.3 | 100.0 | 100.0 | 14578.1 | 50968.8 | 3351 | true |
| 182 | 10.1 | 100.0 | 100.0 | 14750.0 | 41625.0 | 2179 | true |
| 183 | 10.7 | 100.0 | 100.0 | 14656.3 | 46078.1 | 1857 | true |
| 184 | 0.0 | 100.0 | 100.0 | 14859.4 | 34531.3 | 2110 | true |
| 185 | 0.0 | 100.0 | 100.0 | 14765.6 | 32328.1 | 1105 | true |
| 186 | 12.7 | 100.0 | 100.0 | 14875.0 | 39750.0 | 724 | true |
| 187 | 26.7 | 100.0 | 100.0 | 14828.1 | 46890.6 | 5384 | true |
| 188 | 15.2 | 100.0 | 100.0 | 14781.3 | 40796.9 | 2253 | true |
| 189 | 9.4 | 100.0 | 100.0 | 14890.6 | 46250.0 | 3157 | true |
| 190 | 0.0 | 100.0 | 100.0 | 14859.4 | 30125.0 | 0 | true |
| 191 | 0.0 | 100.0 | 100.0 | 14718.8 | 31531.3 | 1076 | true |
| 192 | 10.6 | 100.0 | 100.0 | 14750.0 | 38546.9 | 3121 | true |
| 193 | 24.1 | 100.0 | 100.0 | 14890.6 | 49406.3 | 5427 | true |
| 194 | 9.7 | 100.0 | 100.0 | 14765.6 | 42921.9 | 5442 | true |
| 195 | 14.0 | 100.0 | 100.0 | 14734.4 | 45437.5 | 5469 | true |
| 196 | 0.3 | 100.0 | 100.0 | 14812.5 | 42187.5 | 3213 | true |
| 197 | 0.0 | 100.0 | 100.0 | 14906.3 | 37593.8 | 3160 | true |
| 198 | 0.0 | 100.0 | 100.0 | 14750.0 | 34468.8 | 2031 | true |
| 199 | 12.6 | 100.0 | 100.0 | 14765.6 | 42515.6 | 4313 | true |
| 200 | 13.4 | 100.0 | 100.0 | 14812.5 | 40453.1 | 3244 | true |
| 201 | 13.9 | 100.0 | 100.0 | 14843.8 | 45031.3 | 2251 | true |
| 202 | 10.2 | 100.0 | 100.0 | 14890.6 | 34875.0 | 1161 | true |
| 203 | 0.0 | 100.0 | 100.0 | 14687.5 | 28218.8 | 0 | true |
| 204 | 0.0 | 100.0 | 100.0 | 14781.3 | 28000.0 | 0 | true |
| 205 | 10.1 | 100.0 | 100.0 | 14812.5 | 38750.0 | 2103 | true |
| 206 | 24.0 | 100.0 | 100.0 | 14640.6 | 48468.8 | 2307 | true |
| 207 | 9.5 | 100.0 | 100.0 | 14718.8 | 40609.4 | 3310 | true |
| 208 | 8.6 | 100.0 | 100.0 | 14859.4 | 36843.8 | 1086 | true |
| 209 | 0.0 | 100.0 | 100.0 | 14859.4 | 30796.9 | 0 | true |
| 210 | 0.0 | 100.0 | 100.0 | 14546.9 | 31187.5 | 0 | true |
| 211 | 0.0 | 85.8 | 0.0 | 14656.3 | 32421.9 | 0 | true |
| 212 | 18.3 | 100.0 | 0.0 | 14312.5 | 46531.3 | 814 | true |
| 213 | 10.4 | 100.0 | 0.0 | 14593.8 | 49437.5 | 1165 | true |
| 214 | 11.3 | 100.0 | 0.0 | 14750.0 | 52031.3 | 3450 | true |
| 215 | 5.0 | 100.0 | 0.0 | 14718.8 | 45140.6 | 4465 | true |
| 216 | 0.0 | 100.0 | 0.0 | 14765.6 | 34578.1 | 2170 | true |
| 217 | 0.0 | 100.0 | 0.0 | 14781.3 | 43500.0 | 2162 | true |
| 218 | 0.0 | 100.0 | 0.0 | 14750.0 | 34546.9 | 1118 | true |
| 219 | 18.0 | 100.0 | 0.0 | 14609.4 | 42687.5 | 2003 | true |
| 220 | 21.3 | 100.0 | 0.0 | 14687.5 | 42921.9 | 780 | true |
| 221 | 21.8 | 100.0 | 0.0 | 14671.9 | 47859.4 | 4189 | true |
| 222 | 17.5 | 100.0 | 0.0 | 14687.5 | 51546.9 | 3381 | true |
| 223 | 6.5 | 100.0 | 0.0 | 14734.4 | 41203.1 | 2364 | true |
| 224 | 0.0 | 100.0 | 0.0 | 14734.4 | 34015.6 | 789 | true |
| 225 | 0.0 | 100.0 | 0.0 | 14656.3 | 37453.1 | 2223 | true |
| 226 | 2.7 | 100.0 | 0.0 | 14765.6 | 49812.5 | 7574 | true |
| 227 | 8.5 | 100.0 | 0.0 | 14625.0 | 41843.8 | 4400 | true |
| 228 | 9.6 | 100.0 | 0.0 | 14765.6 | 41234.4 | 4353 | true |
| 229 | 9.3 | 100.0 | 0.0 | 14734.4 | 37656.3 | 2171 | true |
| 230 | 13.0 | 100.0 | 0.0 | 14734.4 | 36093.8 | 3197 | true |
| 231 | 0.0 | 100.0 | 0.0 | 14734.4 | 29921.9 | 0 | true |
| 232 | 0.0 | 100.0 | 0.0 | 14828.1 | 31921.9 | 0 | true |

## Changes and warnings

- **FG=FSR/parallel=true** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR"}`
  - Trial 2: Terrain busy in 10.2% of checks; compare with adjacent controls
  - Trial 3: Terrain busy in 17.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **limitBlockEntityRendering=false** (single feature (prerequisites included); inspect settings diff): `{"limitBlockEntityRendering":false}`
  - Trial 5: GC occupied over 10% of measurement
  - Trial 6: GC occupied over 10% of measurement
- **hideAllCreateModels=true** (diagnostic removal; functionality/visual tradeoff): `{"hideAllCreateModels":true}`
  - Trial 8: Terrain busy in 12.9% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 9: Terrain busy in 15.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **cameraCulling=false** (single feature (prerequisites included); inspect settings diff): `{"cameraCulling":false}`
  - Trial 11: Terrain busy in 18.8% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **factoryConnectionDistance=16** (distance / visual tradeoff): `{"factoryConnectionDistance":16}`
  - Trial 14: GC occupied over 10% of measurement
  - Trial 15: Terrain busy in 14.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **createVisualDistance=16** (distance / visual tradeoff): `{"createVisualDistance":16}`
  - Trial 17: Terrain busy in 12.8% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
  - Trial 18: Terrain busy in 9.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **particlesPerTick=256** (particle density tradeoff): `{"particlesPerTick":256}`
  - Trial 20: GC occupied over 10% of measurement
  - Trial 21: GC occupied over 10% of measurement
- **sections=opaque** (GPU section visibility; visual tradeoff): `{"sectionsAnyBlock":false}`
  - Trial 23: Terrain busy in 9.3% of checks; compare with adjacent controls
  - Trial 24: Terrain busy in 13.2% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **factoryConnectionRange=false** (single feature (prerequisites included); inspect settings diff): `{"factoryConnectionRange":false}`
  - Trial 26: Terrain busy in 23.1% of checks; compare with adjacent controls
  - Trial 27: GC occupied over 10% of measurement
- **disableEntityShadows=true** (diagnostic removal; functionality/visual tradeoff): `{"disableEntityShadows":true}`
  - Trial 29: GC occupied over 10% of measurement
  - Trial 30: Terrain busy in 15.4% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **sableLight=false** (lighting sources / visual tradeoff): `{"sableLight":false}`
  - Trial 32: Terrain busy in 15.8% of checks; compare with adjacent controls
  - Trial 33: Terrain busy in 14.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **FG=FSR/parallel=false** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR","frameGenerationParallel":false}`
  - Trial 35: GC occupied over 10% of measurement
- **createVisualInterval=8** (update cadence tradeoff): `{"createVisualInterval":8}`
  - Trial 38: Terrain busy in 13.6% of checks; compare with adjacent controls
  - Trial 39: Terrain busy in 7.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **occludeFlywheel=false** (single feature (prerequisites included); inspect settings diff): `{"occludeFlywheel":false}`
  - Trial 41: Terrain busy in 17.5% of checks; compare with adjacent controls
- **FG=SIMPLE/parallel/x2** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE","frameGenerationMultiplier":2,"frameGenerationAmortize":false}`
  - Trial 44: Terrain busy in 0.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 45: Terrain busy in 15.6% of checks; compare with adjacent controls
- **walls=opaque + particles** (combined; visual tradeoff): `{"particleRange":true,"opaqueGlassParticles":false,"createRenderAnyBlock":false,"flywheelAnyBlock":false,"animationAnyBlock":false}`
  - Trial 47: Terrain busy in 13.1% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 48: Terrain busy in 16.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **freezeCreateAnimations=true** (diagnostic removal; functionality/visual tradeoff): `{"freezeCreateAnimations":true}`
  - Trial 50: Terrain busy in 4.0% of checks; compare with adjacent controls
- **blockEntityRenderDistance=64** (distance / visual tradeoff): `{"blockEntityRenderDistance":64}`
  - Trial 53: Terrain busy in 22.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 54: Terrain busy in 15.4% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **factoryConnectionDistance=64** (distance / visual tradeoff): `{"factoryConnectionDistance":64}`
  - Trial 56: Terrain busy in 14.4% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
  - Trial 57: Terrain busy in 14.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **itemRenderDistance=16** (distance / visual tradeoff): `{"itemRenderDistance":16}`
  - Trial 60: Terrain busy in 16.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **particleRenderDistance=64** (distance / visual tradeoff): `{"particleRange":true,"particleRenderDistance":64}`
  - Trial 62: Terrain busy in 24.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 63: Terrain busy in 10.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **indexedLighting=false** (single feature (prerequisites included); inspect settings diff): `{"indexedLighting":false}`
  - Trial 66: GC occupied over 10% of measurement
- **FG=FSR/parallel/x2** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR","frameGenerationMultiplier":2,"frameGenerationAmortize":false}`
  - Trial 68: Terrain busy in 18.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
  - Trial 69: Terrain busy in 8.1% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **FG=SIMPLE/parallel/x4** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE","frameGenerationMultiplier":4,"frameGenerationAmortize":false}`
  - Trial 72: Terrain busy in 2.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **hiddenTickInterval=8** (update cadence tradeoff): `{"throttleHiddenCreate":true,"hiddenTickInterval":8}`
  - Trial 74: Terrain busy in 9.2% of checks; compare with adjacent controls
  - Trial 75: Terrain busy in 10.2% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **disableFlywheelRendering=true** (diagnostic removal; functionality/visual tradeoff): `{"disableFlywheelRendering":true}`
  - Trial 77: Terrain busy in 10.7% of checks; compare with adjacent controls
  - Trial 78: Terrain busy in 0.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **cullBreakingOverlays=false** (single feature (prerequisites included); inspect settings diff): `{"cullBreakingOverlays":false}`
  - Trial 80: GC occupied over 10% of measurement
  - Trial 81: Terrain busy in 16.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **occludeParticles=false** (single feature (prerequisites included); inspect settings diff): `{"occludeParticles":false}`
  - Trial 83: Terrain busy in 18.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 84: Terrain busy in 20.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **hideFactoryConnections=true** (diagnostic removal; functionality/visual tradeoff): `{"hideFactoryConnections":true}`
  - Trial 86: GC occupied over 10% of measurement
  - Trial 87: GC occupied over 10% of measurement
- **factoryBulbRange=false** (single feature (prerequisites included); inspect settings diff): `{"factoryBulbRange":false}`
  - Trial 89: Terrain busy in 19.5% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 90: Terrain busy in 19.1% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **createLight=false** (lighting sources / visual tradeoff): `{"createLight":false}`
  - Trial 92: Terrain busy in 11.5% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 93: Terrain busy in 3.0% of checks; compare with adjacent controls; Real FPS drifted over 15% within this measurement
- **factoryItemDistance=64** (distance / visual tradeoff): `{"factoryItemDistance":64}`
  - Trial 96: Terrain busy in 16.9% of checks; compare with adjacent controls
- **cacheParticleLight=false** (single feature (prerequisites included); inspect settings diff): `{"cacheParticleLight":false}`
  - Trial 98: Terrain busy in 16.4% of checks; compare with adjacent controls
  - Trial 99: Terrain busy in 12.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **disabled** (Slopium disabled; FG off): `{"enabled":false}`
  - Trial 101: Terrain busy in 4.5% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
  - Trial 102: GC occupied over 10% of measurement
- **createVisualDistance=64** (distance / visual tradeoff): `{"createVisualDistance":64}`
  - Trial 104: Terrain busy in 15.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 105: Terrain busy in 14.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **itemImpostors=false** (single feature (prerequisites included); inspect settings diff): `{"itemImpostors":false}`
  - Trial 108: GC occupied over 10% of measurement
- **occludeCreateTicks=false** (client update cadence; possible visual/compatibility effects): `{"occludeCreateTicks":false}`
  - Trial 110: Terrain busy in 16.2% of checks; compare with adjacent controls
  - Trial 111: Terrain busy in 15.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **particleRenderDistance=16** (distance / visual tradeoff): `{"particleRange":true,"particleRenderDistance":16}`
  - Trial 113: Terrain busy in 12.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **disableAmbientEffects=false** (diagnostic removal; functionality/visual tradeoff): `{"disableAmbientEffects":false}`
  - Trial 116: Terrain busy in 19.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 117: Terrain busy in 16.9% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **factoryItemDistance=16** (distance / visual tradeoff): `{"factoryItemDistance":16}`
  - Trial 119: Terrain busy in 12.2% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 120: GC occupied over 10% of measurement
- **walls=opaque + items** (combined; visual tradeoff): `{"disableEntityShadows":true,"opaqueGlassParticles":false,"createRenderAnyBlock":false,"flywheelAnyBlock":false,"animationAnyBlock":false}`
  - Trial 123: Terrain busy in 28.8% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **FG=SIMPLE/parallel/x8** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE","frameGenerationMultiplier":8,"frameGenerationAmortize":false}`
  - Trial 125: Terrain busy in 20.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
  - Trial 126: Terrain busy in 6.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **disableBlockEntityRendering=true** (diagnostic removal; functionality/visual tradeoff): `{"disableBlockEntityRendering":true}`
  - Trial 128: Terrain busy in 0.2% of checks; compare with adjacent controls; Real FPS drifted over 15% within this measurement
- **disableParticles=true** (diagnostic removal; functionality/visual tradeoff): `{"disableParticles":true}`
  - Trial 131: Terrain busy in 5.1% of checks; compare with adjacent controls
  - Trial 132: Terrain busy in 23.2% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **blockEntityRenderDistance=16** (distance / visual tradeoff): `{"blockEntityRenderDistance":16}`
  - Trial 134: Terrain busy in 14.2% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
  - Trial 135: Terrain busy in 24.2% of checks; compare with adjacent controls
- **FG=FSR/parallel/x4** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR","frameGenerationMultiplier":4,"frameGenerationAmortize":false}`
  - Trial 137: GC occupied over 10% of measurement
  - Trial 138: GC occupied over 10% of measurement
- **walls=opaque** (combined visibility; visual tradeoff): `{"opaqueGlassParticles":false,"createRenderAnyBlock":false,"flywheelAnyBlock":false,"animationAnyBlock":false}`
  - Trial 140: Terrain busy in 17.0% of checks; compare with adjacent controls; Real FPS drifted over 15% within this measurement
  - Trial 141: Terrain busy in 18.4% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **throttleCreateVisuals=false** (single feature (prerequisites included); inspect settings diff): `{"throttleCreateVisuals":false}`
  - Trial 143: Terrain busy in 26.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 144: Terrain busy in 10.5% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **throttleHiddenCreate=true** (client update cadence; possible visual/compatibility effects): `{"throttleHiddenCreate":true}`
  - Trial 147: Terrain busy in 29.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **pauseXaeroBenchmark=false** (diagnostic removal; functionality/visual tradeoff): `{"pauseXaeroBenchmark":false}`
  - Trial 149: Terrain busy in 8.4% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 150: Terrain busy in 9.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **factoryBulbDistance=64** (distance / visual tradeoff): `{"factoryBulbDistance":64}`
  - Trial 152: Terrain busy in 1.1% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
  - Trial 153: Terrain busy in 6.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **factoryItemRange=false** (single feature (prerequisites included); inspect settings diff): `{"factoryItemRange":false}`
  - Trial 155: Terrain busy in 26.9% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 156: Terrain busy in 19.4% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **particleBudget=false** (single feature (prerequisites included); inspect settings diff): `{"particleBudget":false}`
  - Trial 158: GC occupied over 10% of measurement
  - Trial 159: GC occupied over 10% of measurement
- **lightQueryCache=false** (single feature (prerequisites included); inspect settings diff): `{"lightQueryCache":false}`
  - Trial 161: Terrain busy in 18.1% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 162: Terrain busy in 25.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **FG=SIMPLE/parallel=false** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE","frameGenerationParallel":false}`
  - Trial 164: GC occupied over 10% of measurement
- **gpuGlassCulling=false** (single feature (prerequisites included); inspect settings diff): `{"gpuGlassCulling":false}`
  - Trial 167: Terrain busy in 12.2% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 168: Terrain busy in 9.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **particlesPerTick=32** (particle density tradeoff): `{"particlesPerTick":32}`
  - Trial 170: Terrain busy in 1.3% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **createTickInterval=2** (update cadence tradeoff): `{"distanceCreateTicks":true,"createTickInterval":2}`
  - Trial 173: Terrain busy in 24.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 174: Terrain busy in 18.9% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **hiddenTickInterval=2** (update cadence tradeoff): `{"throttleHiddenCreate":true,"hiddenTickInterval":2}`
  - Trial 176: Terrain busy in 12.1% of checks; compare with adjacent controls
- **factoryBulbDistance=16** (distance / visual tradeoff): `{"factoryBulbDistance":16}`
  - Trial 179: GC occupied over 10% of measurement
  - Trial 180: Terrain busy in 33.2% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **visibleSections=false** (single feature (prerequisites included); inspect settings diff): `{"visibleSections":false}`
  - Trial 182: Terrain busy in 10.1% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 183: Terrain busy in 10.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **FG=SIMPLE/parallel=true** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE"}`
  - Trial 186: Terrain busy in 12.7% of checks; compare with adjacent controls
- **createTickInterval=8** (update cadence tradeoff): `{"distanceCreateTicks":true,"createTickInterval":8}`
  - Trial 188: Terrain busy in 15.2% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 189: Terrain busy in 9.4% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **particleRange=true** (single feature (prerequisites included); inspect settings diff): `{"particleRange":true}`
  - Trial 192: Terrain busy in 10.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **disableTextureInterpolation=false** (diagnostic removal; functionality/visual tradeoff): `{"disableTextureInterpolation":false}`
  - Trial 194: Terrain busy in 9.7% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 195: Terrain busy in 14.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **particles + items** (combined; visual tradeoff): `{"disableEntityShadows":true,"particleRange":true}`
  - Trial 197: GC occupied over 10% of measurement
  - Trial 198: GC occupied over 10% of measurement
- **distanceCreateTicks=true** (client update cadence; possible visual/compatibility effects): `{"distanceCreateTicks":true}`
  - Trial 200: Terrain busy in 13.4% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 201: Terrain busy in 13.9% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **original** (original settings (including FG)): `{}`
- **freezeCreateClientTicks=true** (client update cadence; possible visual/compatibility effects): `{"freezeCreateClientTicks":true}`
  - Trial 206: Terrain busy in 24.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 207: Terrain busy in 9.5% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **itemLodDistance=64** (distance / visual tradeoff): `{"itemLodDistance":64}`
- **occludeCreateRendering=false** (single feature (prerequisites included); inspect settings diff): `{"occludeCreateRendering":false}`
  - Trial 212: Terrain busy in 18.3% of checks; compare with adjacent controls
  - Trial 213: Terrain busy in 10.4% of checks; compare with adjacent controls
- **occludeCreateAnimations=false** (single feature (prerequisites included); inspect settings diff): `{"occludeCreateAnimations":false}`
  - Trial 215: Terrain busy in 5.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 216: GC occupied over 10% of measurement
- **minimumSections=2** (GPU section visibility; protected radius): `{"minimumSections":2}`
  - Trial 219: Terrain busy in 18.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **FG=FSR/parallel/x8** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR","frameGenerationMultiplier":8,"frameGenerationAmortize":false}`
  - Trial 221: Terrain busy in 21.8% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 222: Terrain busy in 17.5% of checks; compare with adjacent controls; GC occupied over 10% of measurement; Real FPS drifted over 15% within this measurement
- **itemStackLod=false** (single feature (prerequisites included); inspect settings diff): `{"itemStackLod":false}`
  - Trial 225: GC occupied over 10% of measurement
- **limitItemRendering=false** (single feature (prerequisites included); inspect settings diff): `{"limitItemRendering":false}`
  - Trial 227: Terrain busy in 8.5% of checks; compare with adjacent controls; GC occupied over 10% of measurement
  - Trial 228: Terrain busy in 9.6% of checks; compare with adjacent controls; GC occupied over 10% of measurement
- **createVisualInterval=2** (update cadence tradeoff): `{"createVisualInterval":2}`
  - Trial 230: Terrain busy in 13.0% of checks; compare with adjacent controls; GC occupied over 10% of measurement

## Spark diagnostics (excluded from FPS comparisons)

[
  {
    "trial": "control",
    "settings": {
      "enabled": true,
      "frameGenerationMode": "OFF",
      "frameGenerationTargetFps": 0,
      "frameGenerationInputFps": 0,
      "frameGenerationMultiplier": 32,
      "frameGenerationMarkFrames": false,
      "frameGenerationCounter": true,
      "frameGenerationFastPath": true,
      "frameGenerationParallel": true,
      "frameGenerationLowLatency": true,
      "frameGenerationAmortize": true,
      "frameGenerationGridStep": 4,
      "frameGenerationFlowCell": 1,
      "frameGenerationRefinement": 1,
      "frameGenerationProtect": true,
      "frameGenerationSearchPixels": 128,
      "frameGenerationRejectionPixels": 16,
      "frameGenerationFsrDebug": false,
      "cameraCulling": true,
      "visibleSections": true,
      "minimumSections": 0,
      "sectionsAnyBlock": true,
      "processedBlockMode": "BLACKLIST",
      "processedBlockIds": [
        "create:chain_conveyor",
        "create:track"
      ],
      "gpuGlassCulling": true,
      "indexedLighting": true,
      "lightQueryCache": true,
      "createLight": true,
      "sableLight": true,
      "cullBreakingOverlays": true,
      "throttleHiddenCreate": false,
      "hiddenTickInterval": 10,
      "protectedDistance": 24,
      "hiddenGraceTicks": 20,
      "diagnostics": true,
      "disableBlockEntityRendering": false,
      "disableFlywheelRendering": false,
      "disableParticles": false,
      "disableAmbientEffects": true,
      "freezeCreateClientTicks": false,
      "itemStackLod": true,
      "itemLodDistance": 16,
      "limitItemRendering": true,
      "itemRenderDistance": 64,
      "limitBlockEntityRendering": true,
      "blockEntityRenderDistance": 63,
      "pauseXaeroBenchmark": true,
      "disableEntityShadows": false,
      "throttleFlywheelDistance": false,
      "flywheelFullRateDistance": 34,
      "flywheelFrameInterval": 8,
      "itemImpostors": true,
      "itemImpostorDistance": 24,
      "throttleCreateVisuals": true,
      "createVisualDistance": 34,
      "createVisualInterval": 10,
      "particleBudget": true,
      "particlesPerTick": 16,
      "disableTextureInterpolation": true,
      "cacheParticleLight": true,
      "particleRange": false,
      "particleRenderDistance": 60,
      "factoryConnectionRange": true,
      "factoryConnectionDistance": 24,
      "factoryBulbRange": true,
      "factoryBulbDistance": 32,
      "distanceCreateTicks": false,
      "createTickDistance": 3,
      "createTickInterval": 10,
      "occludeParticles": true,
      "opaqueGlassParticles": true,
      "createRenderAnyBlock": true,
      "flywheelAnyBlock": true,
      "animationAnyBlock": true,
      "tickAnyBlock": true,
      "factoryItemRange": true,
      "factoryItemDistance": 24,
      "occludeCreateRendering": true,
      "occludeCreateTicks": true,
      "occludeFlywheel": true,
      "occludeCreateAnimations": true,
      "freezeCreateAnimations": false,
      "hideAllCreateModels": false,
      "hideFactoryConnections": false
    },
    "file": "spark/01.sparkprofile",
    "result": "saved"
  },
  {
    "trial": "cameraCulling\u003dfalse",
    "settings": {
      "enabled": true,
      "frameGenerationMode": "OFF",
      "frameGenerationTargetFps": 0,
      "frameGenerationInputFps": 0,
      "frameGenerationMultiplier": 32,
      "frameGenerationMarkFrames": false,
      "frameGenerationCounter": true,
      "frameGenerationFastPath": true,
      "frameGenerationParallel": true,
      "frameGenerationLowLatency": true,
      "frameGenerationAmortize": true,
      "frameGenerationGridStep": 4,
      "frameGenerationFlowCell": 1,
      "frameGenerationRefinement": 1,
      "frameGenerationProtect": true,
      "frameGenerationSearchPixels": 128,
      "frameGenerationRejectionPixels": 16,
      "frameGenerationFsrDebug": false,
      "cameraCulling": false,
      "visibleSections": true,
      "minimumSections": 0,
      "sectionsAnyBlock": true,
      "processedBlockMode": "BLACKLIST",
      "processedBlockIds": [
        "create:chain_conveyor",
        "create:track"
      ],
      "gpuGlassCulling": true,
      "indexedLighting": true,
      "lightQueryCache": true,
      "createLight": true,
      "sableLight": true,
      "cullBreakingOverlays": true,
      "throttleHiddenCreate": false,
      "hiddenTickInterval": 10,
      "protectedDistance": 24,
      "hiddenGraceTicks": 20,
      "diagnostics": true,
      "disableBlockEntityRendering": false,
      "disableFlywheelRendering": false,
      "disableParticles": false,
      "disableAmbientEffects": true,
      "freezeCreateClientTicks": false,
      "itemStackLod": true,
      "itemLodDistance": 16,
      "limitItemRendering": true,
      "itemRenderDistance": 64,
      "limitBlockEntityRendering": true,
      "blockEntityRenderDistance": 63,
      "pauseXaeroBenchmark": true,
      "disableEntityShadows": false,
      "throttleFlywheelDistance": false,
      "flywheelFullRateDistance": 34,
      "flywheelFrameInterval": 8,
      "itemImpostors": true,
      "itemImpostorDistance": 24,
      "throttleCreateVisuals": true,
      "createVisualDistance": 34,
      "createVisualInterval": 10,
      "particleBudget": true,
      "particlesPerTick": 16,
      "disableTextureInterpolation": true,
      "cacheParticleLight": true,
      "particleRange": false,
      "particleRenderDistance": 60,
      "factoryConnectionRange": true,
      "factoryConnectionDistance": 24,
      "factoryBulbRange": true,
      "factoryBulbDistance": 32,
      "distanceCreateTicks": false,
      "createTickDistance": 3,
      "createTickInterval": 10,
      "occludeParticles": true,
      "opaqueGlassParticles": true,
      "createRenderAnyBlock": true,
      "flywheelAnyBlock": true,
      "animationAnyBlock": true,
      "tickAnyBlock": true,
      "factoryItemRange": true,
      "factoryItemDistance": 24,
      "occludeCreateRendering": true,
      "occludeCreateTicks": true,
      "occludeFlywheel": true,
      "occludeCreateAnimations": true,
      "freezeCreateAnimations": false,
      "hideAllCreateModels": false,
      "hideFactoryConnections": false
    },
    "file": "spark/02.sparkprofile",
    "result": "saved"
  },
  {
    "trial": "occludeCreateTicks\u003dfalse",
    "settings": {
      "enabled": true,
      "frameGenerationMode": "OFF",
      "frameGenerationTargetFps": 0,
      "frameGenerationInputFps": 0,
      "frameGenerationMultiplier": 32,
      "frameGenerationMarkFrames": false,
      "frameGenerationCounter": true,
      "frameGenerationFastPath": true,
      "frameGenerationParallel": true,
      "frameGenerationLowLatency": true,
      "frameGenerationAmortize": true,
      "frameGenerationGridStep": 4,
      "frameGenerationFlowCell": 1,
      "frameGenerationRefinement": 1,
      "frameGenerationProtect": true,
      "frameGenerationSearchPixels": 128,
      "frameGenerationRejectionPixels": 16,
      "frameGenerationFsrDebug": false,
      "cameraCulling": true,
      "visibleSections": true,
      "minimumSections": 0,
      "sectionsAnyBlock": true,
      "processedBlockMode": "BLACKLIST",
      "processedBlockIds": [
        "create:chain_conveyor",
        "create:track"
      ],
      "gpuGlassCulling": true,
      "indexedLighting": true,
      "lightQueryCache": true,
      "createLight": true,
      "sableLight": true,
      "cullBreakingOverlays": true,
      "throttleHiddenCreate": false,
      "hiddenTickInterval": 10,
      "protectedDistance": 24,
      "hiddenGraceTicks": 20,
      "diagnostics": true,
      "disableBlockEntityRendering": false,
      "disableFlywheelRendering": false,
      "disableParticles": false,
      "disableAmbientEffects": true,
      "freezeCreateClientTicks": false,
      "itemStackLod": true,
      "itemLodDistance": 16,
      "limitItemRendering": true,
      "itemRenderDistance": 64,
      "limitBlockEntityRendering": true,
      "blockEntityRenderDistance": 63,
      "pauseXaeroBenchmark": true,
      "disableEntityShadows": false,
      "throttleFlywheelDistance": false,
      "flywheelFullRateDistance": 34,
      "flywheelFrameInterval": 8,
      "itemImpostors": true,
      "itemImpostorDistance": 24,
      "throttleCreateVisuals": true,
      "createVisualDistance": 34,
      "createVisualInterval": 10,
      "particleBudget": true,
      "particlesPerTick": 16,
      "disableTextureInterpolation": true,
      "cacheParticleLight": true,
      "particleRange": false,
      "particleRenderDistance": 60,
      "factoryConnectionRange": true,
      "factoryConnectionDistance": 24,
      "factoryBulbRange": true,
      "factoryBulbDistance": 32,
      "distanceCreateTicks": false,
      "createTickDistance": 3,
      "createTickInterval": 10,
      "occludeParticles": true,
      "opaqueGlassParticles": true,
      "createRenderAnyBlock": true,
      "flywheelAnyBlock": true,
      "animationAnyBlock": true,
      "tickAnyBlock": true,
      "factoryItemRange": true,
      "factoryItemDistance": 24,
      "occludeCreateRendering": true,
      "occludeCreateTicks": false,
      "occludeFlywheel": true,
      "occludeCreateAnimations": true,
      "freezeCreateAnimations": false,
      "hideAllCreateModels": false,
      "hideFactoryConnections": false
    },
    "file": "spark/03.sparkprofile",
    "result": "saved"
  },
  {
    "trial": "hideAllCreateModels\u003dtrue",
    "settings": {
      "enabled": true,
      "frameGenerationMode": "OFF",
      "frameGenerationTargetFps": 0,
      "frameGenerationInputFps": 0,
      "frameGenerationMultiplier": 32,
      "frameGenerationMarkFrames": false,
      "frameGenerationCounter": true,
      "frameGenerationFastPath": true,
      "frameGenerationParallel": true,
      "frameGenerationLowLatency": true,
      "frameGenerationAmortize": true,
      "frameGenerationGridStep": 4,
      "frameGenerationFlowCell": 1,
      "frameGenerationRefinement": 1,
      "frameGenerationProtect": true,
      "frameGenerationSearchPixels": 128,
      "frameGenerationRejectionPixels": 16,
      "frameGenerationFsrDebug": false,
      "cameraCulling": true,
      "visibleSections": true,
      "minimumSections": 0,
      "sectionsAnyBlock": true,
      "processedBlockMode": "BLACKLIST",
      "processedBlockIds": [
        "create:chain_conveyor",
        "create:track"
      ],
      "gpuGlassCulling": true,
      "indexedLighting": true,
      "lightQueryCache": true,
      "createLight": true,
      "sableLight": true,
      "cullBreakingOverlays": true,
      "throttleHiddenCreate": false,
      "hiddenTickInterval": 10,
      "protectedDistance": 24,
      "hiddenGraceTicks": 20,
      "diagnostics": true,
      "disableBlockEntityRendering": false,
      "disableFlywheelRendering": false,
      "disableParticles": false,
      "disableAmbientEffects": true,
      "freezeCreateClientTicks": false,
      "itemStackLod": true,
      "itemLodDistance": 16,
      "limitItemRendering": true,
      "itemRenderDistance": 64,
      "limitBlockEntityRendering": true,
      "blockEntityRenderDistance": 63,
      "pauseXaeroBenchmark": true,
      "disableEntityShadows": false,
      "throttleFlywheelDistance": false,
      "flywheelFullRateDistance": 34,
      "flywheelFrameInterval": 8,
      "itemImpostors": true,
      "itemImpostorDistance": 24,
      "throttleCreateVisuals": true,
      "createVisualDistance": 34,
      "createVisualInterval": 10,
      "particleBudget": true,
      "particlesPerTick": 16,
      "disableTextureInterpolation": true,
      "cacheParticleLight": true,
      "particleRange": false,
      "particleRenderDistance": 60,
      "factoryConnectionRange": true,
      "factoryConnectionDistance": 24,
      "factoryBulbRange": true,
      "factoryBulbDistance": 32,
      "distanceCreateTicks": false,
      "createTickDistance": 3,
      "createTickInterval": 10,
      "occludeParticles": true,
      "opaqueGlassParticles": true,
      "createRenderAnyBlock": true,
      "flywheelAnyBlock": true,
      "animationAnyBlock": true,
      "tickAnyBlock": true,
      "factoryItemRange": true,
      "factoryItemDistance": 24,
      "occludeCreateRendering": true,
      "occludeCreateTicks": true,
      "occludeFlywheel": true,
      "occludeCreateAnimations": true,
      "freezeCreateAnimations": false,
      "hideAllCreateModels": true,
      "hideFactoryConnections": false
    },
    "file": "spark/04.sparkprofile",
    "result": "saved"
  },
  {
    "trial": "freezeCreateClientTicks\u003dtrue",
    "settings": {
      "enabled": true,
      "frameGenerationMode": "OFF",
      "frameGenerationTargetFps": 0,
      "frameGenerationInputFps": 0,
      "frameGenerationMultiplier": 32,
      "frameGenerationMarkFrames": false,
      "frameGenerationCounter": true,
      "frameGenerationFastPath": true,
      "frameGenerationParallel": true,
      "frameGenerationLowLatency": true,
      "frameGenerationAmortize": true,
      "frameGenerationGridStep": 4,
      "frameGenerationFlowCell": 1,
      "frameGenerationRefinement": 1,
      "frameGenerationProtect": true,
      "frameGenerationSearchPixels": 128,
      "frameGenerationRejectionPixels": 16,
      "frameGenerationFsrDebug": false,
      "cameraCulling": true,
      "visibleSections": true,
      "minimumSections": 0,
      "sectionsAnyBlock": true,
      "processedBlockMode": "BLACKLIST",
      "processedBlockIds": [
        "create:chain_conveyor",
        "create:track"
      ],
      "gpuGlassCulling": true,
      "indexedLighting": true,
      "lightQueryCache": true,
      "createLight": true,
      "sableLight": true,
      "cullBreakingOverlays": true,
      "throttleHiddenCreate": false,
      "hiddenTickInterval": 10,
      "protectedDistance": 24,
      "hiddenGraceTicks": 20,
      "diagnostics": true,
      "disableBlockEntityRendering": false,
      "disableFlywheelRendering": false,
      "disableParticles": false,
      "disableAmbientEffects": true,
      "freezeCreateClientTicks": true,
      "itemStackLod": true,
      "itemLodDistance": 16,
      "limitItemRendering": true,
      "itemRenderDistance": 64,
      "limitBlockEntityRendering": true,
      "blockEntityRenderDistance": 63,
      "pauseXaeroBenchmark": true,
      "disableEntityShadows": false,
      "throttleFlywheelDistance": false,
      "flywheelFullRateDistance": 34,
      "flywheelFrameInterval": 8,
      "itemImpostors": true,
      "itemImpostorDistance": 24,
      "throttleCreateVisuals": true,
      "createVisualDistance": 34,
      "createVisualInterval": 10,
      "particleBudget": true,
      "particlesPerTick": 16,
      "disableTextureInterpolation": true,
      "cacheParticleLight": true,
      "particleRange": false,
      "particleRenderDistance": 60,
      "factoryConnectionRange": true,
      "factoryConnectionDistance": 24,
      "factoryBulbRange": true,
      "factoryBulbDistance": 32,
      "distanceCreateTicks": false,
      "createTickDistance": 3,
      "createTickInterval": 10,
      "occludeParticles": true,
      "opaqueGlassParticles": true,
      "createRenderAnyBlock": true,
      "flywheelAnyBlock": true,
      "animationAnyBlock": true,
      "tickAnyBlock": true,
      "factoryItemRange": true,
      "factoryItemDistance": 24,
      "occludeCreateRendering": true,
      "occludeCreateTicks": true,
      "occludeFlywheel": true,
      "occludeCreateAnimations": true,
      "freezeCreateAnimations": false,
      "hideAllCreateModels": false,
      "hideFactoryConnections": false
    },
    "file": "spark/05.sparkprofile",
    "result": "saved"
  },
  {
    "trial": "disableBlockEntityRendering\u003dtrue",
    "settings": {
      "enabled": true,
      "frameGenerationMode": "OFF",
      "frameGenerationTargetFps": 0,
      "frameGenerationInputFps": 0,
      "frameGenerationMultiplier": 32,
      "frameGenerationMarkFrames": false,
      "frameGenerationCounter": true,
      "frameGenerationFastPath": true,
      "frameGenerationParallel": true,
      "frameGenerationLowLatency": true,
      "frameGenerationAmortize": true,
      "frameGenerationGridStep": 4,
      "frameGenerationFlowCell": 1,
      "frameGenerationRefinement": 1,
      "frameGenerationProtect": true,
      "frameGenerationSearchPixels": 128,
      "frameGenerationRejectionPixels": 16,
      "frameGenerationFsrDebug": false,
      "cameraCulling": true,
      "visibleSections": true,
      "minimumSections": 0,
      "sectionsAnyBlock": true,
      "processedBlockMode": "BLACKLIST",
      "processedBlockIds": [
        "create:chain_conveyor",
        "create:track"
      ],
      "gpuGlassCulling": true,
      "indexedLighting": true,
      "lightQueryCache": true,
      "createLight": true,
      "sableLight": true,
      "cullBreakingOverlays": true,
      "throttleHiddenCreate": false,
      "hiddenTickInterval": 10,
      "protectedDistance": 24,
      "hiddenGraceTicks": 20,
      "diagnostics": true,
      "disableBlockEntityRendering": true,
      "disableFlywheelRendering": false,
      "disableParticles": false,
      "disableAmbientEffects": true,
      "freezeCreateClientTicks": false,
      "itemStackLod": true,
      "itemLodDistance": 16,
      "limitItemRendering": true,
      "itemRenderDistance": 64,
      "limitBlockEntityRendering": true,
      "blockEntityRenderDistance": 63,
      "pauseXaeroBenchmark": true,
      "disableEntityShadows": false,
      "throttleFlywheelDistance": false,
      "flywheelFullRateDistance": 34,
      "flywheelFrameInterval": 8,
      "itemImpostors": true,
      "itemImpostorDistance": 24,
      "throttleCreateVisuals": true,
      "createVisualDistance": 34,
      "createVisualInterval": 10,
      "particleBudget": true,
      "particlesPerTick": 16,
      "disableTextureInterpolation": true,
      "cacheParticleLight": true,
      "particleRange": false,
      "particleRenderDistance": 60,
      "factoryConnectionRange": true,
      "factoryConnectionDistance": 24,
      "factoryBulbRange": true,
      "factoryBulbDistance": 32,
      "distanceCreateTicks": false,
      "createTickDistance": 3,
      "createTickInterval": 10,
      "occludeParticles": true,
      "opaqueGlassParticles": true,
      "createRenderAnyBlock": true,
      "flywheelAnyBlock": true,
      "animationAnyBlock": true,
      "tickAnyBlock": true,
      "factoryItemRange": true,
      "factoryItemDistance": 24,
      "occludeCreateRendering": true,
      "occludeCreateTicks": true,
      "occludeFlywheel": true,
      "occludeCreateAnimations": true,
      "freezeCreateAnimations": false,
      "hideAllCreateModels": false,
      "hideFactoryConnections": false
    },
    "file": "spark/06.sparkprofile",
    "result": "saved"
  }
]
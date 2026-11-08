# Slopium performance analysis

Status: complete

Real FPS counts completed world renders. Output FPS counts window presentations (including held images); it is not a count of unique new images. Frame times are intervals between completed real frames, not isolated GPU time.

Reference: your saved settings with Slopium enabled and frame generation OFF. Changes below are relative to that reference. Caps and VSync are unchanged; capped results cannot establish available performance headroom. GPU section timings are cumulative diagnostics, not isolated per-trial timings.

Each variant is repeated twice in shuffled order. Effects use the nearest valid control before and after. 'Faster/slower in this run' requires both repeats to agree beyond max(5%, control drift), at most 10% control drift, and no chunk streaming. These are screening results, not proof for other scenes. Visual removals and client tick changes have tradeoffs. Read the full JSON settings and warnings before keeping a change.

| Variant | Real FPS effect | Assessment |
|---|---:|---|
| sections=opaque | n/a | inconclusive / incomplete |
| limitItemRendering=false | n/a | inconclusive / incomplete |
| particleBudget=false | n/a | inconclusive / incomplete |
| disableTextureInterpolation=false | n/a | inconclusive / incomplete |
| sableLight=false | n/a | inconclusive / incomplete |
| walls=opaque | -5.4% | inconclusive / incomplete |
| itemRenderDistance=16 | +1.1% | inconclusive / incomplete |
| particleRenderDistance=64 | +23.4% | inconclusive / incomplete |
| disableBlockEntityRendering=true | n/a | inconclusive / incomplete |
| FG=FSR/parallel=false | -38.5% | inconclusive / incomplete |
| createTickInterval=8 | +20.9% | inconclusive / incomplete |
| factoryBulbRange=false | +20.8% | inconclusive / incomplete |
| factoryItemRange=false | +3.3% | inconclusive / incomplete |
| factoryConnectionRange=false | +8.8% | inconclusive / incomplete |
| factoryItemDistance=64 | +8.6% | inconclusive / incomplete |
| indexedLighting=false | +3.6% | inconclusive / incomplete |
| particlesPerTick=32 | n/a | inconclusive / incomplete |
| factoryConnectionDistance=16 | n/a | inconclusive / incomplete |
| createVisualDistance=64 | -17.7% | inconclusive / incomplete |
| cameraCulling=false | +28.2% | inconclusive / incomplete |
| itemStackLod=false | n/a | inconclusive / incomplete |
| factoryBulbDistance=16 | n/a | inconclusive / incomplete |
| hideFactoryConnections=true | +35.8% | inconclusive / incomplete |
| freezeCreateClientTicks=true | +28.9% | inconclusive / incomplete |
| disableParticles=true | n/a | inconclusive / incomplete |
| FG=SIMPLE/parallel/x2 | -18.6% | inconclusive / incomplete |
| lightQueryCache=false | +4.8% | inconclusive / incomplete |
| FG=SIMPLE/parallel/x4 | +3.0% | inconclusive / incomplete |
| factoryItemDistance=16 | +2.0% | inconclusive / incomplete |
| blockEntityRenderDistance=16 | +27.9% | inconclusive / incomplete |
| createVisualInterval=8 | -2.8% | inconclusive / incomplete |
| occludeFlywheel=false | -7.2% | inconclusive / incomplete |
| FG=SIMPLE/parallel=true | n/a | inconclusive / incomplete |
| limitBlockEntityRendering=false | n/a | inconclusive / incomplete |
| createVisualDistance=16 | n/a | inconclusive / incomplete |
| blockEntityRenderDistance=64 | n/a | inconclusive / incomplete |
| disableEntityShadows=true | n/a | inconclusive / incomplete |
| disableAmbientEffects=false | n/a | inconclusive / incomplete |
| throttleCreateVisuals=false | -22.3% | inconclusive / incomplete |
| visibleSections=false | -18.8% | inconclusive / incomplete |
| walls=opaque + particles | n/a | inconclusive / incomplete |
| particleRange=true | +11.7% | inconclusive / incomplete |
| occludeCreateTicks=false | +31.2% | inconclusive / incomplete |
| createLight=false | +10.9% | inconclusive / incomplete |
| FG=FSR/parallel=true | +3.2% | no consistent benefit above noise |
| pauseXaeroBenchmark=false | -2.7% | inconclusive / incomplete |
| particleRenderDistance=16 | -1.9% | inconclusive / incomplete |
| minimumSections=2 | n/a | inconclusive / incomplete |
| FG=FSR/parallel/x8 | n/a | inconclusive / incomplete |
| throttleHiddenCreate=true | n/a | inconclusive / incomplete |
| particles + items | +34.4% | inconclusive / incomplete |
| FG=FSR/parallel/x4 | -0.1% | inconclusive / incomplete |
| particlesPerTick=256 | n/a | inconclusive / incomplete |
| occludeCreateAnimations=false | +0.5% | inconclusive / incomplete |
| distanceCreateTicks=true | +3.8% | inconclusive / incomplete |
| hiddenTickInterval=2 | -3.3% | inconclusive / incomplete |
| hideAllCreateModels=true | +40.0% | inconclusive / incomplete |
| original | -10.5% | inconclusive / incomplete |
| createTickInterval=2 | n/a | inconclusive / incomplete |
| FG=SIMPLE/parallel/x8 | n/a | inconclusive / incomplete |
| occludeParticles=false | n/a | inconclusive / incomplete |
| itemImpostors=false | n/a | inconclusive / incomplete |
| itemLodDistance=64 | -12.6% | inconclusive / incomplete |
| FG=SIMPLE/parallel=false | -42.4% | inconclusive / incomplete |
| factoryConnectionDistance=64 | n/a | inconclusive / incomplete |
| createVisualInterval=2 | n/a | inconclusive / incomplete |
| hiddenTickInterval=8 | -11.5% | inconclusive / incomplete |
| walls=opaque + items | -12.0% | inconclusive / incomplete |
| freezeCreateAnimations=true | -8.0% | inconclusive / incomplete |
| cacheParticleLight=false | -16.5% | inconclusive / incomplete |
| factoryBulbDistance=64 | n/a | inconclusive / incomplete |
| occludeCreateRendering=false | n/a | inconclusive / incomplete |
| disableFlywheelRendering=true | n/a | inconclusive / incomplete |
| gpuGlassCulling=false | n/a | inconclusive / incomplete |
| FG=FSR/parallel/x2 | n/a | inconclusive / incomplete |
| cullBreakingOverlays=false | n/a | inconclusive / incomplete |

## Measurements

| # | Variant | Real FPS | Output FPS | Generated | p50 ms | p95 ms | p99 ms | Valid |
|---:|---|---:|---:|---:|---:|---:|---:|---|
| 1 | control | 22.93 | 22.93 | 0 | 45.15 | 60.26 | 73.22 | false |
| 2 | sections=opaque | 23.69 | 23.69 | 0 | 44.85 | 56.30 | 67.29 | false |
| 3 | limitItemRendering=false | 21.56 | 21.56 | 0 | 45.81 | 71.07 | 107.89 | false |
| 4 | particleBudget=false | 25.27 | 25.27 | 0 | 42.88 | 55.04 | 77.93 | false |
| 5 | disableTextureInterpolation=false | 27.49 | 27.49 | 0 | 40.89 | 51.53 | 60.72 | true |
| 6 | control | 25.17 | 25.17 | 0 | 42.33 | 55.53 | 68.06 | true |
| 7 | disableTextureInterpolation=false | 24.85 | 24.85 | 0 | 43.26 | 55.02 | 67.22 | false |
| 8 | particleBudget=false | 16.89 | 16.89 | 0 | 53.42 | 90.26 | 98.42 | false |
| 9 | sections=opaque | 20.44 | 20.44 | 0 | 48.16 | 62.11 | 81.07 | false |
| 10 | limitItemRendering=false | 21.64 | 21.64 | 0 | 46.96 | 59.05 | 73.40 | false |
| 11 | control | 22.07 | 22.07 | 0 | 46.50 | 58.36 | 72.79 | false |
| 12 | sableLight=false | 21.97 | 21.97 | 0 | 46.44 | 57.72 | 66.00 | false |
| 13 | walls=opaque | 22.83 | 22.83 | 0 | 45.68 | 56.81 | 69.07 | true |
| 14 | itemRenderDistance=16 | 24.38 | 24.38 | 0 | 44.86 | 53.70 | 61.45 | true |
| 15 | particleRenderDistance=64 | 29.77 | 29.77 | 0 | 38.44 | 49.21 | 53.19 | true |
| 16 | control | 28.39 | 28.39 | 0 | 39.62 | 50.21 | 58.41 | false |
| 17 | particleRenderDistance=64 | 22.60 | 22.60 | 0 | 43.88 | 57.03 | 158.16 | false |
| 18 | itemRenderDistance=16 | 23.03 | 23.03 | 0 | 45.10 | 57.98 | 71.13 | false |
| 19 | sableLight=false | 26.20 | 26.20 | 0 | 41.55 | 53.07 | 61.00 | false |
| 20 | walls=opaque | 22.47 | 22.47 | 0 | 45.97 | 57.54 | 69.21 | false |
| 21 | control | 20.18 | 20.18 | 0 | 48.28 | 69.49 | 90.50 | false |
| 22 | disableBlockEntityRendering=true | 32.58 | 32.58 | 0 | 36.84 | 48.16 | 59.09 | false |
| 23 | FG=FSR/parallel=false | 14.83 | 34.44 | 186 | 63.14 | 94.10 | 99.47 | true |
| 24 | createTickInterval=8 | 29.17 | 29.17 | 0 | 38.63 | 49.04 | 53.04 | true |
| 25 | factoryBulbRange=false | 29.15 | 29.15 | 0 | 38.33 | 49.44 | 54.49 | true |
| 26 | control | 26.17 | 26.17 | 0 | 42.22 | 52.31 | 67.42 | false |
| 27 | createTickInterval=8 | 24.85 | 24.85 | 0 | 43.53 | 56.81 | 67.01 | false |
| 28 | disableBlockEntityRendering=true | 43.39 | 43.39 | 0 | 14.96 | 42.28 | 47.97 | false |
| 29 | factoryBulbRange=false | 24.07 | 24.07 | 0 | 43.22 | 56.32 | 139.51 | false |
| 30 | FG=FSR/parallel=false | 15.46 | 34.71 | 190 | 60.57 | 94.23 | 104.33 | false |
| 31 | control | 23.08 | 23.08 | 0 | 44.37 | 58.29 | 71.65 | true |
| 32 | factoryItemRange=false | 27.19 | 27.19 | 0 | 39.95 | 51.46 | 63.01 | true |
| 33 | factoryConnectionRange=false | 25.25 | 25.25 | 0 | 42.21 | 53.49 | 68.57 | false |
| 34 | factoryItemDistance=64 | 24.28 | 24.28 | 0 | 44.10 | 54.50 | 64.13 | false |
| 35 | indexedLighting=false | 23.82 | 23.82 | 0 | 44.45 | 54.82 | 67.10 | false |
| 36 | control | 28.56 | 28.56 | 0 | 38.66 | 51.65 | 62.67 | false |
| 37 | factoryItemRange=false | 24.24 | 24.24 | 0 | 44.59 | 53.34 | 61.72 | false |
| 38 | factoryItemDistance=64 | 28.58 | 28.58 | 0 | 39.75 | 49.77 | 54.29 | true |
| 39 | indexedLighting=false | 27.28 | 27.28 | 0 | 41.26 | 50.41 | 58.17 | true |
| 40 | factoryConnectionRange=false | 28.64 | 28.64 | 0 | 39.90 | 48.80 | 51.73 | true |
| 41 | control | 25.69 | 25.69 | 0 | 42.12 | 55.34 | 69.22 | false |
| 42 | particlesPerTick=32 | 27.15 | 27.15 | 0 | 41.03 | 51.83 | 59.68 | false |
| 43 | factoryConnectionDistance=16 | 27.59 | 27.59 | 0 | 39.79 | 56.11 | 70.40 | false |
| 44 | createVisualDistance=64 | 25.36 | 25.36 | 0 | 43.06 | 54.74 | 63.47 | false |
| 45 | cameraCulling=false | 33.28 | 33.28 | 0 | 33.42 | 44.96 | 53.07 | false |
| 46 | control | 29.57 | 29.57 | 0 | 38.91 | 48.84 | 53.46 | true |
| 47 | cameraCulling=false | 35.94 | 35.94 | 0 | 31.11 | 42.21 | 46.78 | true |
| 48 | createVisualDistance=64 | 23.07 | 23.07 | 0 | 45.95 | 56.78 | 78.31 | true |
| 49 | particlesPerTick=32 | 19.56 | 19.56 | 0 | 49.55 | 65.00 | 83.58 | false |
| 50 | factoryConnectionDistance=16 | 25.00 | 25.00 | 0 | 43.64 | 55.36 | 74.48 | false |
| 51 | control | 20.22 | 20.22 | 0 | 49.36 | 60.10 | 73.48 | false |
| 52 | itemStackLod=false | 20.94 | 20.94 | 0 | 48.32 | 59.69 | 76.64 | false |
| 53 | factoryBulbDistance=16 | 18.76 | 18.76 | 0 | 50.80 | 80.85 | 95.48 | false |
| 54 | hideFactoryConnections=true | 25.16 | 25.16 | 0 | 42.91 | 55.34 | 70.71 | false |
| 55 | freezeCreateClientTicks=true | 36.14 | 36.14 | 0 | 30.47 | 38.66 | 42.95 | true |
| 56 | control | 26.50 | 26.50 | 0 | 42.08 | 52.61 | 59.22 | true |
| 57 | hideFactoryConnections=true | 36.64 | 36.64 | 0 | 30.90 | 44.54 | 48.03 | true |
| 58 | freezeCreateClientTicks=true | 37.56 | 37.56 | 0 | 28.06 | 38.45 | 46.52 | false |
| 59 | itemStackLod=false | 21.97 | 21.97 | 0 | 47.04 | 58.01 | 71.12 | false |
| 60 | factoryBulbDistance=16 | 26.88 | 26.88 | 0 | 41.44 | 53.76 | 66.24 | false |
| 61 | control | 24.95 | 24.95 | 0 | 43.77 | 53.73 | 61.25 | false |
| 62 | disableParticles=true | 26.11 | 26.11 | 0 | 41.99 | 53.98 | 58.61 | false |
| 63 | FG=SIMPLE/parallel/x2 | 21.96 | 42.02 | 201 | 46.73 | 55.57 | 59.52 | true |
| 64 | lightQueryCache=false | 28.29 | 28.29 | 0 | 38.98 | 52.07 | 56.58 | true |
| 65 | FG=SIMPLE/parallel/x4 | 27.81 | 89.61 | 620 | 40.86 | 49.39 | 52.30 | true |
| 66 | control | 20.32 | 20.32 | 0 | 46.23 | 80.87 | 135.80 | false |
| 67 | FG=SIMPLE/parallel/x4 | 23.03 | 80.55 | 577 | 45.11 | 59.46 | 83.56 | false |
| 68 | lightQueryCache=false | 23.24 | 23.24 | 0 | 44.65 | 62.31 | 123.59 | false |
| 69 | disableParticles=true | 27.79 | 27.79 | 0 | 40.14 | 51.09 | 67.47 | false |
| 70 | FG=SIMPLE/parallel/x2 | 28.07 | 48.46 | 205 | 39.99 | 49.52 | 58.72 | false |
| 71 | control | 27.48 | 27.48 | 0 | 40.25 | 51.66 | 73.75 | true |
| 72 | factoryItemDistance=16 | 30.52 | 30.52 | 0 | 37.36 | 46.84 | 52.44 | true |
| 73 | blockEntityRenderDistance=16 | 33.22 | 33.22 | 0 | 33.08 | 46.51 | 99.53 | false |
| 74 | createVisualInterval=8 | 22.29 | 22.29 | 0 | 43.76 | 57.41 | 185.98 | false |
| 75 | occludeFlywheel=false | 21.38 | 21.38 | 0 | 46.75 | 71.81 | 84.92 | false |
| 76 | control | 23.58 | 23.58 | 0 | 44.73 | 58.66 | 75.66 | false |
| 77 | factoryItemDistance=16 | 24.09 | 24.09 | 0 | 43.93 | 57.76 | 65.85 | false |
| 78 | createVisualInterval=8 | 29.09 | 29.09 | 0 | 38.76 | 48.86 | 52.86 | true |
| 79 | occludeFlywheel=false | 27.78 | 27.78 | 0 | 40.53 | 52.11 | 57.43 | true |
| 80 | blockEntityRenderDistance=16 | 38.28 | 38.28 | 0 | 29.80 | 43.20 | 48.92 | true |
| 81 | control | 25.40 | 25.40 | 0 | 43.45 | 53.82 | 62.40 | false |
| 82 | FG=SIMPLE/parallel=true | 26.05 | 58.60 | 326 | 42.02 | 53.35 | 60.56 | false |
| 83 | limitBlockEntityRendering=false | 29.98 | 29.98 | 0 | 34.40 | 53.81 | 61.60 | false |
| 84 | createVisualDistance=16 | 27.08 | 27.08 | 0 | 41.49 | 51.60 | 55.46 | false |
| 85 | blockEntityRenderDistance=64 | 29.16 | 29.16 | 0 | 39.42 | 49.92 | 53.83 | false |
| 86 | control | 32.37 | 32.37 | 0 | 34.94 | 46.62 | 51.41 | true |
| 87 | createVisualDistance=16 | 27.66 | 27.66 | 0 | 39.27 | 52.56 | 69.99 | false |
| 88 | blockEntityRenderDistance=64 | 27.93 | 27.93 | 0 | 39.56 | 51.89 | 62.92 | false |
| 89 | FG=SIMPLE/parallel=true | 25.59 | 57.29 | 317 | 43.41 | 54.58 | 64.73 | false |
| 90 | limitBlockEntityRendering=false | 21.17 | 21.17 | 0 | 44.23 | 70.93 | 266.05 | false |
| 91 | control | 21.06 | 21.06 | 0 | 47.74 | 57.47 | 78.15 | false |
| 92 | disableEntityShadows=true | 23.48 | 23.48 | 0 | 45.30 | 54.65 | 63.79 | false |
| 93 | disableAmbientEffects=false | 19.32 | 19.32 | 0 | 49.46 | 77.74 | 93.40 | false |
| 94 | throttleCreateVisuals=false | 22.04 | 22.04 | 0 | 46.81 | 56.09 | 61.80 | true |
| 95 | visibleSections=false | 23.03 | 23.03 | 0 | 45.26 | 55.92 | 61.00 | true |
| 96 | control | 24.38 | 24.38 | 0 | 44.90 | 54.17 | 58.64 | true |
| 97 | throttleCreateVisuals=false | 21.74 | 21.74 | 0 | 45.86 | 61.93 | 113.44 | false |
| 98 | visibleSections=false | 26.03 | 26.03 | 0 | 42.06 | 55.98 | 70.24 | false |
| 99 | disableEntityShadows=true | 24.79 | 24.79 | 0 | 43.82 | 55.11 | 66.89 | false |
| 100 | disableAmbientEffects=false | 22.99 | 22.99 | 0 | 45.84 | 57.94 | 69.77 | false |
| 101 | control | 24.24 | 24.24 | 0 | 44.09 | 54.70 | 78.08 | false |
| 102 | walls=opaque + particles | 25.26 | 25.26 | 0 | 42.71 | 54.13 | 61.55 | false |
| 103 | particleRange=true | 27.59 | 27.59 | 0 | 41.13 | 51.54 | 58.19 | true |
| 104 | occludeCreateTicks=false | 32.40 | 32.40 | 0 | 35.18 | 43.12 | 45.66 | true |
| 105 | createLight=false | 27.39 | 27.39 | 0 | 42.47 | 51.22 | 53.51 | true |
| 106 | control | 24.11 | 24.11 | 0 | 44.53 | 53.80 | 65.75 | false |
| 107 | createLight=false | 25.58 | 25.58 | 0 | 42.37 | 54.15 | 65.65 | false |
| 108 | occludeCreateTicks=false | 35.66 | 35.66 | 0 | 30.58 | 41.15 | 47.23 | false |
| 109 | walls=opaque + particles | 31.27 | 31.27 | 0 | 36.25 | 48.66 | 53.28 | false |
| 110 | particleRange=true | 30.79 | 30.79 | 0 | 36.34 | 48.06 | 55.39 | false |
| 111 | control | 25.03 | 25.03 | 0 | 42.38 | 60.19 | 76.05 | true |
| 112 | FG=FSR/parallel=true | 30.57 | 56.54 | 259 | 36.80 | 47.51 | 52.61 | true |
| 113 | pauseXaeroBenchmark=false | 28.69 | 28.69 | 0 | 39.92 | 49.10 | 58.37 | false |
| 114 | particleRenderDistance=16 | 26.75 | 26.75 | 0 | 41.45 | 53.65 | 66.48 | false |
| 115 | minimumSections=2 | 24.00 | 24.00 | 0 | 44.36 | 56.92 | 66.67 | false |
| 116 | control | 27.55 | 27.55 | 0 | 40.47 | 50.02 | 56.32 | false |
| 117 | minimumSections=2 | 26.19 | 26.19 | 0 | 41.72 | 53.59 | 58.37 | false |
| 118 | particleRenderDistance=16 | 25.31 | 25.31 | 0 | 41.41 | 65.99 | 86.28 | true |
| 119 | FG=FSR/parallel=true | 22.71 | 57.58 | 350 | 45.18 | 60.93 | 77.66 | true |
| 120 | pauseXaeroBenchmark=false | 25.12 | 25.12 | 0 | 43.04 | 54.65 | 65.63 | true |
| 121 | control | 25.40 | 25.40 | 0 | 42.52 | 56.50 | 74.47 | false |
| 122 | FG=FSR/parallel/x8 | 25.27 | 152.30 | 1272 | 43.13 | 54.59 | 62.27 | false |
| 123 | throttleHiddenCreate=true | 26.08 | 26.08 | 0 | 42.24 | 53.25 | 67.77 | false |
| 124 | particles + items | 25.77 | 25.77 | 0 | 42.61 | 53.88 | 61.81 | false |
| 125 | FG=FSR/parallel/x4 | 25.79 | 85.87 | 601 | 43.01 | 53.43 | 61.68 | true |
| 126 | control | 26.60 | 26.60 | 0 | 42.02 | 51.80 | 58.20 | true |
| 127 | particles + items | 31.69 | 31.69 | 0 | 35.85 | 46.71 | 50.86 | true |
| 128 | FG=FSR/parallel/x4 | 26.07 | 86.75 | 610 | 42.20 | 52.45 | 66.48 | false |
| 129 | FG=FSR/parallel/x8 | 26.14 | 150.58 | 1247 | 41.41 | 54.00 | 60.67 | false |
| 130 | throttleHiddenCreate=true | 26.98 | 26.98 | 0 | 40.75 | 52.79 | 66.00 | false |
| 131 | control | 21.93 | 21.93 | 0 | 45.13 | 72.72 | 83.92 | false |
| 132 | particlesPerTick=256 | 18.53 | 18.53 | 0 | 51.36 | 80.92 | 91.66 | false |
| 133 | occludeCreateAnimations=false | 23.70 | 23.70 | 0 | 45.22 | 54.09 | 59.12 | true |
| 134 | distanceCreateTicks=true | 24.49 | 24.49 | 0 | 44.45 | 52.34 | 56.28 | true |
| 135 | hiddenTickInterval=2 | 22.80 | 22.80 | 0 | 46.00 | 55.54 | 58.91 | true |
| 136 | control | 25.05 | 25.05 | 0 | 43.66 | 52.31 | 60.44 | false |
| 137 | distanceCreateTicks=true | 18.53 | 18.53 | 0 | 47.28 | 80.24 | 315.52 | false |
| 138 | hiddenTickInterval=2 | 23.89 | 23.89 | 0 | 43.94 | 56.77 | 65.96 | false |
| 139 | particlesPerTick=256 | 20.96 | 20.96 | 0 | 48.10 | 70.86 | 78.18 | false |
| 140 | occludeCreateAnimations=false | 23.32 | 23.32 | 0 | 44.42 | 60.73 | 82.83 | false |
| 141 | control | 20.57 | 20.57 | 0 | 47.39 | 74.26 | 83.18 | true |
| 142 | hideAllCreateModels=true | 30.87 | 30.87 | 0 | 38.23 | 49.67 | 59.20 | true |
| 143 | original | 17.96 | 17.96 | 0 | 54.73 | 83.38 | 102.14 | true |
| 144 | createTickInterval=2 | 24.39 | 24.39 | 0 | 43.69 | 56.50 | 70.51 | false |
| 145 | FG=SIMPLE/parallel/x8 | 20.30 | 147.19 | 1270 | 48.90 | 63.74 | 81.97 | false |
| 146 | control | 23.09 | 23.09 | 0 | 45.85 | 56.41 | 63.59 | false |
| 147 | createTickInterval=2 | 27.30 | 27.30 | 0 | 40.69 | 51.54 | 58.84 | false |
| 148 | FG=SIMPLE/parallel/x8 | 26.07 | 154.01 | 1281 | 41.45 | 53.98 | 57.64 | false |
| 149 | hideAllCreateModels=true | 44.38 | 44.38 | 0 | 14.50 | 39.29 | 43.14 | true |
| 150 | original | 30.16 | 30.16 | 0 | 36.70 | 46.51 | 49.80 | true |
| 151 | control | 33.18 | 33.18 | 0 | 34.04 | 45.54 | 52.33 | true |
| 152 | occludeParticles=false | 24.78 | 24.78 | 0 | 43.27 | 55.14 | 60.91 | false |
| 153 | itemImpostors=false | 28.40 | 28.40 | 0 | 38.45 | 51.79 | 65.31 | false |
| 154 | itemLodDistance=64 | 21.41 | 21.41 | 0 | 46.51 | 63.85 | 85.63 | false |
| 155 | FG=SIMPLE/parallel=false | 14.99 | 29.89 | 144 | 56.67 | 114.14 | 259.99 | false |
| 156 | control | 27.10 | 27.10 | 0 | 40.74 | 51.95 | 65.43 | false |
| 157 | FG=SIMPLE/parallel=false | 18.61 | 35.82 | 163 | 53.78 | 81.32 | 83.52 | true |
| 158 | itemLodDistance=64 | 28.22 | 28.22 | 0 | 39.18 | 51.88 | 63.28 | true |
| 159 | occludeParticles=false | 25.34 | 25.34 | 0 | 42.51 | 54.26 | 62.83 | false |
| 160 | itemImpostors=false | 24.41 | 24.41 | 0 | 43.47 | 56.53 | 69.37 | false |
| 161 | control | 23.62 | 23.62 | 0 | 44.01 | 59.58 | 88.89 | false |
| 162 | factoryConnectionDistance=64 | 22.29 | 22.29 | 0 | 45.65 | 63.64 | 83.07 | false |
| 163 | createVisualInterval=2 | 26.42 | 26.42 | 0 | 41.03 | 53.67 | 71.07 | false |
| 164 | hiddenTickInterval=8 | 28.58 | 28.58 | 0 | 38.53 | 52.30 | 58.61 | true |
| 165 | walls=opaque + items | 28.40 | 28.40 | 0 | 39.70 | 50.31 | 57.49 | true |
| 166 | control | 23.98 | 23.98 | 0 | 44.13 | 55.77 | 72.00 | false |
| 167 | hiddenTickInterval=8 | 23.28 | 23.28 | 0 | 44.87 | 60.18 | 82.37 | false |
| 168 | walls=opaque + items | 26.58 | 26.58 | 0 | 41.33 | 54.23 | 65.18 | false |
| 169 | factoryConnectionDistance=64 | 26.05 | 26.05 | 0 | 40.90 | 57.62 | 65.47 | false |
| 170 | createVisualInterval=2 | 26.40 | 26.40 | 0 | 39.04 | 53.35 | 132.90 | false |
| 171 | control | 31.39 | 31.39 | 0 | 36.48 | 46.94 | 50.44 | true |
| 172 | freezeCreateAnimations=true | 28.88 | 28.88 | 0 | 39.85 | 49.77 | 54.77 | true |
| 173 | cacheParticleLight=false | 20.11 | 20.11 | 0 | 48.00 | 74.68 | 85.25 | false |
| 174 | factoryBulbDistance=64 | 31.58 | 31.58 | 0 | 33.78 | 52.25 | 55.93 | false |
| 175 | occludeCreateRendering=false | 23.77 | 23.77 | 0 | 45.06 | 52.83 | 57.57 | false |
| 176 | control | 21.82 | 21.82 | 0 | 46.26 | 56.63 | 96.35 | false |
| 177 | occludeCreateRendering=false | 22.75 | 22.75 | 0 | 46.66 | 55.14 | 62.21 | false |
| 178 | factoryBulbDistance=64 | 23.52 | 23.52 | 0 | 45.26 | 54.65 | 64.87 | false |
| 179 | freezeCreateAnimations=true | 23.83 | 23.83 | 0 | 44.88 | 55.59 | 74.73 | true |
| 180 | cacheParticleLight=false | 23.92 | 23.92 | 0 | 45.59 | 54.47 | 59.85 | true |
| 181 | control | 25.90 | 25.90 | 0 | 42.49 | 51.80 | 58.79 | true |
| 182 | disableFlywheelRendering=true | 24.53 | 24.53 | 0 | 43.45 | 56.52 | 64.10 | false |
| 183 | gpuGlassCulling=false | 21.61 | 21.61 | 0 | 46.59 | 61.75 | 78.81 | false |
| 184 | FG=FSR/parallel/x2 | 20.37 | 39.84 | 195 | 48.72 | 65.72 | 85.68 | false |
| 185 | cullBreakingOverlays=false | 25.39 | 25.39 | 0 | 42.65 | 53.98 | 61.53 | false |
| 186 | control | 27.37 | 27.37 | 0 | 41.05 | 51.65 | 63.67 | false |
| 187 | FG=FSR/parallel/x2 | 25.99 | 46.18 | 202 | 42.39 | 53.32 | 63.19 | true |
| 188 | cullBreakingOverlays=false | 23.40 | 23.40 | 0 | 45.84 | 54.02 | 59.86 | true |
| 189 | disableFlywheelRendering=true | 25.25 | 25.25 | 0 | 44.18 | 52.85 | 61.86 | true |
| 190 | gpuGlassCulling=false | 31.96 | 31.96 | 0 | 35.72 | 46.79 | 54.93 | true |
| 191 | control | 25.29 | 25.29 | 0 | 40.88 | 52.26 | 157.38 | false |

## Changes and warnings

- **sections=opaque** (GPU section visibility; visual tradeoff): `{"sectionsAnyBlock":false}`
  - Trial 2: Terrain rebuilds still pending during measurement
  - Trial 9: Terrain rebuilds still pending during measurement
- **limitItemRendering=false** (single feature (prerequisites included); inspect settings diff): `{"limitItemRendering":false}`
  - Trial 3: Terrain rebuilds still pending during measurement
  - Trial 10: Terrain rebuilds still pending during measurement
- **particleBudget=false** (single feature (prerequisites included); inspect settings diff): `{"particleBudget":false}`
  - Trial 4: Terrain rebuilds still pending during measurement
  - Trial 8: Terrain rebuilds still pending during measurement
- **disableTextureInterpolation=false** (diagnostic removal; functionality/visual tradeoff): `{"disableTextureInterpolation":false}`
  - Trial 7: Terrain rebuilds still pending during measurement
- **sableLight=false** (lighting sources / visual tradeoff): `{"sableLight":false}`
  - Trial 12: Terrain rebuilds still pending during measurement
  - Trial 19: Terrain rebuilds still pending during measurement
- **walls=opaque** (combined visibility; visual tradeoff): `{"opaqueGlassParticles":false,"createRenderAnyBlock":false,"flywheelAnyBlock":false,"animationAnyBlock":false}`
  - Trial 20: Terrain rebuilds still pending during measurement
- **itemRenderDistance=16** (distance / visual tradeoff): `{"itemRenderDistance":16}`
  - Trial 18: Terrain rebuilds still pending during measurement
- **particleRenderDistance=64** (distance / visual tradeoff): `{"particleRange":true,"particleRenderDistance":64}`
  - Trial 17: Terrain rebuilds still pending during measurement
- **disableBlockEntityRendering=true** (diagnostic removal; functionality/visual tradeoff): `{"disableBlockEntityRendering":true}`
  - Trial 22: Terrain rebuilds still pending during measurement
  - Trial 28: Terrain rebuilds still pending during measurement; Large frame-time spread
- **FG=FSR/parallel=false** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR","frameGenerationParallel":false}`
  - Trial 30: Terrain rebuilds still pending during measurement
- **createTickInterval=8** (update cadence tradeoff): `{"distanceCreateTicks":true,"createTickInterval":8}`
  - Trial 27: Terrain rebuilds still pending during measurement
- **factoryBulbRange=false** (single feature (prerequisites included); inspect settings diff): `{"factoryBulbRange":false}`
  - Trial 29: Terrain rebuilds still pending during measurement
- **factoryItemRange=false** (single feature (prerequisites included); inspect settings diff): `{"factoryItemRange":false}`
  - Trial 37: Terrain rebuilds still pending during measurement
- **factoryConnectionRange=false** (single feature (prerequisites included); inspect settings diff): `{"factoryConnectionRange":false}`
  - Trial 33: Terrain rebuilds still pending during measurement
- **factoryItemDistance=64** (distance / visual tradeoff): `{"factoryItemDistance":64}`
  - Trial 34: Terrain rebuilds still pending during measurement
- **indexedLighting=false** (single feature (prerequisites included); inspect settings diff): `{"indexedLighting":false}`
  - Trial 35: Terrain rebuilds still pending during measurement
- **particlesPerTick=32** (particle density tradeoff): `{"particlesPerTick":32}`
  - Trial 42: Terrain rebuilds still pending during measurement
  - Trial 49: Terrain rebuilds still pending during measurement
- **factoryConnectionDistance=16** (distance / visual tradeoff): `{"factoryConnectionDistance":16}`
  - Trial 43: Terrain rebuilds still pending during measurement
  - Trial 50: Terrain rebuilds still pending during measurement
- **createVisualDistance=64** (distance / visual tradeoff): `{"createVisualDistance":64}`
  - Trial 44: Terrain rebuilds still pending during measurement
- **cameraCulling=false** (single feature (prerequisites included); inspect settings diff): `{"cameraCulling":false}`
  - Trial 45: Terrain rebuilds still pending during measurement
- **itemStackLod=false** (single feature (prerequisites included); inspect settings diff): `{"itemStackLod":false}`
  - Trial 52: Terrain rebuilds still pending during measurement
  - Trial 59: Terrain rebuilds still pending during measurement
- **factoryBulbDistance=16** (distance / visual tradeoff): `{"factoryBulbDistance":16}`
  - Trial 53: Terrain rebuilds still pending during measurement
  - Trial 60: Terrain rebuilds still pending during measurement
- **hideFactoryConnections=true** (diagnostic removal; functionality/visual tradeoff): `{"hideFactoryConnections":true}`
  - Trial 54: Terrain rebuilds still pending during measurement
- **freezeCreateClientTicks=true** (client update cadence; possible visual/compatibility effects): `{"freezeCreateClientTicks":true}`
  - Trial 58: Terrain rebuilds still pending during measurement
- **disableParticles=true** (diagnostic removal; functionality/visual tradeoff): `{"disableParticles":true}`
  - Trial 62: Terrain rebuilds still pending during measurement
  - Trial 69: Terrain rebuilds still pending during measurement
- **FG=SIMPLE/parallel/x2** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE","frameGenerationMultiplier":2,"frameGenerationAmortize":false}`
  - Trial 70: Terrain rebuilds still pending during measurement
- **lightQueryCache=false** (single feature (prerequisites included); inspect settings diff): `{"lightQueryCache":false}`
  - Trial 68: Terrain rebuilds still pending during measurement
- **FG=SIMPLE/parallel/x4** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE","frameGenerationMultiplier":4,"frameGenerationAmortize":false}`
  - Trial 67: Terrain rebuilds still pending during measurement
- **factoryItemDistance=16** (distance / visual tradeoff): `{"factoryItemDistance":16}`
  - Trial 77: Terrain rebuilds still pending during measurement
- **blockEntityRenderDistance=16** (distance / visual tradeoff): `{"blockEntityRenderDistance":16}`
  - Trial 73: Terrain rebuilds still pending during measurement
- **createVisualInterval=8** (update cadence tradeoff): `{"createVisualInterval":8}`
  - Trial 74: Terrain rebuilds still pending during measurement
- **occludeFlywheel=false** (single feature (prerequisites included); inspect settings diff): `{"occludeFlywheel":false}`
  - Trial 75: Terrain rebuilds still pending during measurement
- **FG=SIMPLE/parallel=true** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE"}`
  - Trial 82: Terrain rebuilds still pending during measurement
  - Trial 89: Terrain rebuilds still pending during measurement
- **limitBlockEntityRendering=false** (single feature (prerequisites included); inspect settings diff): `{"limitBlockEntityRendering":false}`
  - Trial 83: Terrain rebuilds still pending during measurement
  - Trial 90: Terrain rebuilds still pending during measurement
- **createVisualDistance=16** (distance / visual tradeoff): `{"createVisualDistance":16}`
  - Trial 84: Terrain rebuilds still pending during measurement
  - Trial 87: Terrain rebuilds still pending during measurement
- **blockEntityRenderDistance=64** (distance / visual tradeoff): `{"blockEntityRenderDistance":64}`
  - Trial 85: Terrain rebuilds still pending during measurement
  - Trial 88: Terrain rebuilds still pending during measurement
- **disableEntityShadows=true** (diagnostic removal; functionality/visual tradeoff): `{"disableEntityShadows":true}`
  - Trial 92: Terrain rebuilds still pending during measurement
  - Trial 99: Terrain rebuilds still pending during measurement
- **disableAmbientEffects=false** (diagnostic removal; functionality/visual tradeoff): `{"disableAmbientEffects":false}`
  - Trial 93: Terrain rebuilds still pending during measurement
  - Trial 100: Terrain rebuilds still pending during measurement
- **throttleCreateVisuals=false** (single feature (prerequisites included); inspect settings diff): `{"throttleCreateVisuals":false}`
  - Trial 97: Terrain rebuilds still pending during measurement
- **visibleSections=false** (single feature (prerequisites included); inspect settings diff): `{"visibleSections":false}`
  - Trial 98: Terrain rebuilds still pending during measurement
- **walls=opaque + particles** (combined; visual tradeoff): `{"particleRange":true,"opaqueGlassParticles":false,"createRenderAnyBlock":false,"flywheelAnyBlock":false,"animationAnyBlock":false}`
  - Trial 102: Terrain rebuilds still pending during measurement
  - Trial 109: Terrain rebuilds still pending during measurement
- **particleRange=true** (single feature (prerequisites included); inspect settings diff): `{"particleRange":true}`
  - Trial 110: Terrain rebuilds still pending during measurement
- **occludeCreateTicks=false** (client update cadence; possible visual/compatibility effects): `{"occludeCreateTicks":false}`
  - Trial 108: Terrain rebuilds still pending during measurement
- **createLight=false** (lighting sources / visual tradeoff): `{"createLight":false}`
  - Trial 107: Terrain rebuilds still pending during measurement
- **FG=FSR/parallel=true** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR"}`
- **pauseXaeroBenchmark=false** (diagnostic removal; functionality/visual tradeoff): `{"pauseXaeroBenchmark":false}`
  - Trial 113: Terrain rebuilds still pending during measurement
- **particleRenderDistance=16** (distance / visual tradeoff): `{"particleRange":true,"particleRenderDistance":16}`
  - Trial 114: Terrain rebuilds still pending during measurement
- **minimumSections=2** (GPU section visibility; protected radius): `{"minimumSections":2}`
  - Trial 115: Terrain rebuilds still pending during measurement
  - Trial 117: Terrain rebuilds still pending during measurement
- **FG=FSR/parallel/x8** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR","frameGenerationMultiplier":8,"frameGenerationAmortize":false}`
  - Trial 122: Terrain rebuilds still pending during measurement
  - Trial 129: Terrain rebuilds still pending during measurement
- **throttleHiddenCreate=true** (client update cadence; possible visual/compatibility effects): `{"throttleHiddenCreate":true}`
  - Trial 123: Terrain rebuilds still pending during measurement
  - Trial 130: Terrain rebuilds still pending during measurement
- **particles + items** (combined; visual tradeoff): `{"disableEntityShadows":true,"particleRange":true}`
  - Trial 124: Terrain rebuilds still pending during measurement
- **FG=FSR/parallel/x4** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR","frameGenerationMultiplier":4,"frameGenerationAmortize":false}`
  - Trial 128: Terrain rebuilds still pending during measurement
- **particlesPerTick=256** (particle density tradeoff): `{"particlesPerTick":256}`
  - Trial 132: Terrain rebuilds still pending during measurement
  - Trial 139: Terrain rebuilds still pending during measurement
- **occludeCreateAnimations=false** (single feature (prerequisites included); inspect settings diff): `{"occludeCreateAnimations":false}`
  - Trial 140: Terrain rebuilds still pending during measurement
- **distanceCreateTicks=true** (client update cadence; possible visual/compatibility effects): `{"distanceCreateTicks":true}`
  - Trial 137: Terrain rebuilds still pending during measurement
- **hiddenTickInterval=2** (update cadence tradeoff): `{"throttleHiddenCreate":true,"hiddenTickInterval":2}`
  - Trial 138: Terrain rebuilds still pending during measurement
- **hideAllCreateModels=true** (diagnostic removal; functionality/visual tradeoff): `{"hideAllCreateModels":true}`
  - Trial 149: Large frame-time spread
- **original** (original settings (including FG)): `{"enabled":false}`
- **createTickInterval=2** (update cadence tradeoff): `{"distanceCreateTicks":true,"createTickInterval":2}`
  - Trial 144: Terrain rebuilds still pending during measurement
  - Trial 147: Terrain rebuilds still pending during measurement
- **FG=SIMPLE/parallel/x8** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE","frameGenerationMultiplier":8,"frameGenerationAmortize":false}`
  - Trial 145: Terrain rebuilds still pending during measurement
  - Trial 148: Terrain rebuilds still pending during measurement
- **occludeParticles=false** (single feature (prerequisites included); inspect settings diff): `{"occludeParticles":false}`
  - Trial 152: Terrain rebuilds still pending during measurement
  - Trial 159: Terrain rebuilds still pending during measurement
- **itemImpostors=false** (single feature (prerequisites included); inspect settings diff): `{"itemImpostors":false}`
  - Trial 153: Terrain rebuilds still pending during measurement
  - Trial 160: Terrain rebuilds still pending during measurement
- **itemLodDistance=64** (distance / visual tradeoff): `{"itemLodDistance":64}`
  - Trial 154: Terrain rebuilds still pending during measurement
- **FG=SIMPLE/parallel=false** (frame generation; compare source AND output): `{"frameGenerationMode":"SIMPLE","frameGenerationParallel":false}`
  - Trial 155: Terrain rebuilds still pending during measurement; Large frame-time spread
- **factoryConnectionDistance=64** (distance / visual tradeoff): `{"factoryConnectionDistance":64}`
  - Trial 162: Terrain rebuilds still pending during measurement
  - Trial 169: Terrain rebuilds still pending during measurement
- **createVisualInterval=2** (update cadence tradeoff): `{"createVisualInterval":2}`
  - Trial 163: Terrain rebuilds still pending during measurement
  - Trial 170: Terrain rebuilds still pending during measurement
- **hiddenTickInterval=8** (update cadence tradeoff): `{"throttleHiddenCreate":true,"hiddenTickInterval":8}`
  - Trial 167: Terrain rebuilds still pending during measurement
- **walls=opaque + items** (combined; visual tradeoff): `{"disableEntityShadows":true,"opaqueGlassParticles":false,"createRenderAnyBlock":false,"flywheelAnyBlock":false,"animationAnyBlock":false}`
  - Trial 168: Terrain rebuilds still pending during measurement
- **freezeCreateAnimations=true** (diagnostic removal; functionality/visual tradeoff): `{"freezeCreateAnimations":true}`
- **cacheParticleLight=false** (single feature (prerequisites included); inspect settings diff): `{"cacheParticleLight":false}`
  - Trial 173: Terrain rebuilds still pending during measurement
- **factoryBulbDistance=64** (distance / visual tradeoff): `{"factoryBulbDistance":64}`
  - Trial 174: Terrain rebuilds still pending during measurement
  - Trial 178: Terrain rebuilds still pending during measurement
- **occludeCreateRendering=false** (single feature (prerequisites included); inspect settings diff): `{"occludeCreateRendering":false}`
  - Trial 175: Terrain rebuilds still pending during measurement
  - Trial 177: Terrain rebuilds still pending during measurement
- **disableFlywheelRendering=true** (diagnostic removal; functionality/visual tradeoff): `{"disableFlywheelRendering":true}`
  - Trial 182: Terrain rebuilds still pending during measurement
- **gpuGlassCulling=false** (single feature (prerequisites included); inspect settings diff): `{"gpuGlassCulling":false}`
  - Trial 183: Terrain rebuilds still pending during measurement
- **FG=FSR/parallel/x2** (frame generation; compare source AND output): `{"frameGenerationMode":"FSR","frameGenerationMultiplier":2,"frameGenerationAmortize":false}`
  - Trial 184: Terrain rebuilds still pending during measurement
- **cullBreakingOverlays=false** (single feature (prerequisites included); inspect settings diff): `{"cullBreakingOverlays":false}`
  - Trial 185: Terrain rebuilds still pending during measurement

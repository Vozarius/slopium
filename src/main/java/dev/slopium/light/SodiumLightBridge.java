package dev.slopium.light;

import toni.sodiumdynamiclights.SodiumDynamicLights;

/** Loaded only when Sodium Dynamic Lights is installed. */
public final class SodiumLightBridge {
    private SodiumLightBridge() {}
    public static void clear() {
        Object instance = SodiumDynamicLights.get();
        if (instance instanceof LightIndexControl control) control.slopium$clearCaches();
    }
}


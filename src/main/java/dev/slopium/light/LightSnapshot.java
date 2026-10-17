package dev.slopium.light;

import net.minecraft.world.level.Level;

public record LightSnapshot(LightGrid grid, Level level, long frame, long revision, long sourcesRevision) {}


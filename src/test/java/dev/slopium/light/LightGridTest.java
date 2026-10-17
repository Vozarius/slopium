package dev.slopium.light;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class LightGridTest {
    @Test void matchesOriginalAcrossRandomScenesAndCellBoundaries() {
        Random random = new Random(0x51091);
        List<LightGrid.Light> lights = new ArrayList<>();
        for (int i = 0; i < 2048; i++) lights.add(new LightGrid.Light(
            random.nextDouble(-96, 96), random.nextDouble(-40, 40), random.nextDouble(-96, 96), random.nextInt(0, 16)));
        LightGrid grid = new LightGrid(lights);
        for (int i = 0; i < 10000; i++) {
            int x = random.nextInt(-110, 111), y = random.nextInt(-50, 51), z = random.nextInt(-110, 111);
            assertEquals(original(lights, x, y, z), grid.sample(x, y, z), 1e-12, x + "," + y + "," + z);
        }
    }

    @Test void preservesRadiusClampingAndNegativeCoordinates() {
        List<LightGrid.Light> lights = new ArrayList<>(List.of(
            new LightGrid.Light(-16.5, 0.5, -0.5, 15),
            new LightGrid.Light(16.5, 16.5, 16.5, 15),
            new LightGrid.Light(0.5 + 7.75, 0.5, 0.5, 15),
            new LightGrid.Light(200.5, 0.5, 0.5, 40)));
        // Keep this test on the indexed path rather than the small-set fallback.
        for (int i = 0; i < 20; i++) lights.add(new LightGrid.Light(500 + i, 500, 500, 15));
        LightGrid grid = new LightGrid(lights);
        for (int x = -32; x <= 40; x++) for (int z = -32; z <= 40; z++) {
            assertEquals(original(lights, x, 0, z), grid.sample(x, 0, z), 1e-12);
        }
        assertEquals(0, grid.sample(0, 0, 0), 1e-12);
        assertEquals(15, grid.sample(200, 0, 0), 1e-12);
    }

    @Test void fallsBackOutsidePackedCoordinateRangeWithoutAliasing() {
        List<LightGrid.Light> lights = List.of(new LightGrid.Light(100_000_000.5, 0.5, 0.5, 15));
        LightGrid grid = new LightGrid(lights);
        assertEquals(15, grid.sample(100_000_000, 0, 0));
        assertEquals(0, grid.sample(0, 0, 0));
    }

    @Test void supportsEmptyScenesAndUnlitSources() {
        assertEquals(0, new LightGrid(List.of()).sample(0, 0, 0));
        assertEquals(0, new LightGrid(List.of(new LightGrid.Light(0.5, 0.5, 0.5, 0))).sample(0, 0, 0));
    }

    @Test void isAnImmutableSnapshot() {
        var lights = new ArrayList<>(List.of(new LightGrid.Light(0.5, 0.5, 0.5, 10)));
        LightGrid grid = new LightGrid(lights);
        lights.clear();
        assertEquals(10, grid.sample(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new LightGrid.Light(Double.NaN, 0, 0, 15));
    }

    private static double original(List<LightGrid.Light> lights, int x, int y, int z) {
        double result = 0;
        for (var light : lights) {
            if (light.luminance() <= 0) continue;
            double dx = x - light.x() + 0.5, dy = y - light.y() + 0.5, dz = z - light.z() + 0.5;
            double squared = dx * dx + dy * dy + dz * dz;
            if (squared <= 60.0625) result = Math.max(result, (1 - Math.sqrt(squared) / 7.75) * light.luminance());
        }
        return Math.clamp(result, 0, 15);
    }
}


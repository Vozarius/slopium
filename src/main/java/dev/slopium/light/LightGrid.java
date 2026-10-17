package dev.slopium.light;

import java.util.Arrays;
import java.util.List;

/** Immutable, allocation-free query index. No Minecraft or mutable world objects. */
public final class LightGrid {
    public static final double RADIUS = 7.75;
    private static final double RADIUS_SQUARED = RADIUS * RADIUS;
    private static final int CELL_BITS = 4;
    private static final int XZ_LIMIT = 1 << 21;
    private static final int Y_LIMIT = 1 << 19;
    private final double[] x, y, z;
    private final int[] luminance, next, heads, counts;
    private final long[] keys;
    private final int mask;
    private final boolean indexable;

    public record Light(double x, double y, double z, int luminance) {
        public Light {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Light coordinates must be finite");
            }
        }
    }

    public LightGrid(List<Light> lights) {
        int size = lights.size();
        x = new double[size]; y = new double[size]; z = new double[size];
        luminance = new int[size]; next = new int[size];
        int capacity = 2;
        while (capacity < size * 2L) {
            if (capacity >= (1 << 27)) throw new IllegalArgumentException("Too many sources");
            capacity <<= 1;
        }
        keys = new long[capacity]; heads = new int[capacity]; counts = new int[capacity];
        Arrays.fill(heads, -1);
        mask = capacity - 1;
        boolean bounded = true;
        for (int i = 0; i < size; i++) {
            Light light = lights.get(i);
            x[i] = light.x(); y[i] = light.y(); z[i] = light.z(); luminance[i] = light.luminance();
            int cx = cell(x[i]), cy = cell(y[i]), cz = cell(z[i]);
            bounded &= inBounds(cx, cy, cz);
            long key = key(cx, cy, cz);
            int slot = slot(key);
            next[i] = heads[slot];
            heads[slot] = i;
            counts[slot]++;
            keys[slot] = key;
        }
        indexable = bounded;
    }

    /** Same query point and attenuation as Sodium Dynamic Lights 1.0.9/1.0.10. */
    public double sample(int blockX, int blockY, int blockZ) {
        if (x.length == 0) return 0;
        double px = blockX + 0.5, py = blockY + 0.5, pz = blockZ + 0.5;
        int minX = cell(px - RADIUS), minY = cell(py - RADIUS), minZ = cell(pz - RADIUS);
        int maxX = cell(px + RADIUS), maxY = cell(py + RADIUS), maxZ = cell(pz + RADIUS);
        if (x.length <= 16 || !indexable || !inBounds(minX, minY, minZ) || !inBounds(maxX, maxY, maxZ)) {
            return sampleAll(px, py, pz);
        }
        int candidates = 0;
        for (int cx = minX; cx <= maxX; cx++) for (int cy = minY; cy <= maxY; cy++) for (int cz = minZ; cz <= maxZ; cz++) {
            candidates += counts[slot(key(cx, cy, cz))];
        }
        if (candidates == 0) return 0;
        // Dense clusters have no useful spatial selectivity. A sequential array
        // scan is cheaper than following bucket chains through nearly all sources.
        if (candidates >= x.length * 3L / 4) return sampleAll(px, py, pz);
        double result = 0;
        // 2 * radius < cell width: at most eight cells intersect the query sphere.
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cy = minY; cy <= maxY; cy++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    int i = heads[slot(key(cx, cy, cz))];
                    for (; i != -1; i = next[i]) result = contribution(i, px, py, pz, result);
                }
            }
        }
        return Math.min(15, Math.max(0, result));
    }

    public int size() { return x.length; }

    private double sampleAll(double px, double py, double pz) {
        double result = 0;
        for (int i = 0; i < x.length; i++) result = contribution(i, px, py, pz, result);
        return Math.min(15, Math.max(0, result));
    }

    private double contribution(int i, double px, double py, double pz, double result) {
        if (luminance[i] <= 0) return result;
        double dx = px - x[i], dy = py - y[i], dz = pz - z[i];
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared > RADIUS_SQUARED) return result;
        return Math.max(result, (1 - Math.sqrt(distanceSquared) / RADIUS) * luminance[i]);
    }

    private int slot(long key) {
        long h = key;
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L; h ^= h >>> 33;
        int slot = (int) h & mask;
        while (heads[slot] != -1 && keys[slot] != key) slot = (slot + 1) & mask;
        return slot;
    }

    private static int cell(double value) { return (int) Math.floor(value / (1 << CELL_BITS)); }
    private static boolean inBounds(int x, int y, int z) {
        return x >= -XZ_LIMIT && x < XZ_LIMIT && z >= -XZ_LIMIT && z < XZ_LIMIT && y >= -Y_LIMIT && y < Y_LIMIT;
    }
    private static long key(int x, int y, int z) {
        return ((x & 0x3fffffL) << 42) | ((z & 0x3fffffL) << 20) | (y & 0xfffffL);
    }
}


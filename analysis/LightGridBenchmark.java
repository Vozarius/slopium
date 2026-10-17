import dev.slopium.light.LightGrid;
import java.util.*;

/** Synthetic algorithm benchmark, not a Minecraft FPS measurement. JDK 21. */
public class LightGridBenchmark {
    private static volatile double blackhole;
    private static final int QUERIES = 4096;

    public static void main(String[] args) {
        System.out.println("{\"kind\":\"synthetic: one snapshot build + 4096 queries, no cache\",\"cases\":[");
        run("empty", 0, 96, true);
        run("16 spread sources", 16, 96, true);
        run("2048 spread sources", 2048, 96, true);
        run("2048 dense sources (worst locality)", 2048, 3, false);
        System.out.println("]}");
    }

    private static void run(String name, int count, double spread, boolean comma) {
        var random = new Random(42);
        List<LightGrid.Light> lights = new ArrayList<>();
        for (int i = 0; i < count; i++) lights.add(new LightGrid.Light(
            random.nextDouble(-spread, spread), random.nextDouble(-spread / 3, spread / 3),
            random.nextDouble(-spread, spread), random.nextInt(1, 16)));
        int[][] queries = new int[QUERIES][3];
        int range = (int) spread + 1;
        for (var p : queries) {
            p[0] = random.nextInt(-range, range + 1);
            p[1] = random.nextInt(-range / 3, range / 3 + 1);
            p[2] = random.nextInt(-range, range + 1);
        }
        double expected = scan(lights, queries);
        double actual = indexed(lights, queries);
        if (Math.abs(expected - actual) > 1e-7) throw new AssertionError("Mismatch: " + name);
        for (int i = 0; i < 30; i++) {
            blackhole = scan(lights, queries);
            blackhole = indexed(lights, queries);
        }
        long[] direct = new long[15], grid = new long[15];
        for (int i = 0; i < direct.length; i++) {
            // Alternate ordering to reduce systematic first/second run effects.
            if ((i & 1) == 0) { direct[i] = time(() -> scan(lights, queries)); grid[i] = time(() -> indexed(lights, queries)); }
            else { grid[i] = time(() -> indexed(lights, queries)); direct[i] = time(() -> scan(lights, queries)); }
        }
        Arrays.sort(direct); Arrays.sort(grid);
        System.out.printf(Locale.ROOT, "{\"name\":\"%s\",\"linear_ms\":%.5f,\"index_build_and_queries_ms\":%.5f,\"ratio\":%.3f}%s%n",
            name, direct[7] / 1e6, grid[7] / 1e6, (double) direct[7] / grid[7], comma ? "," : "");
    }

    private static long time(java.util.function.DoubleSupplier action) {
        long start = System.nanoTime(); blackhole = action.getAsDouble(); return System.nanoTime() - start;
    }
    private static double indexed(List<LightGrid.Light> lights, int[][] queries) {
        var grid = new LightGrid(lights);
        double sum = 0;
        for (var p : queries) sum += grid.sample(p[0], p[1], p[2]);
        return sum;
    }
    private static double scan(List<LightGrid.Light> lights, int[][] queries) {
        double sum = 0;
        for (var p : queries) {
            double result = 0;
            for (var light : lights) {
                double dx = p[0] - light.x() + 0.5, dy = p[1] - light.y() + 0.5, dz = p[2] - light.z() + 0.5;
                double distance = dx * dx + dy * dy + dz * dz;
                if (distance <= 60.0625) result = Math.max(result, (1 - Math.sqrt(distance) / 7.75) * light.luminance());
            }
            sum += Math.clamp(result, 0, 15);
        }
        return sum;
    }
}

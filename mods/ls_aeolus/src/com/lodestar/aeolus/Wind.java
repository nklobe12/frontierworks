package com.lodestar.aeolus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * The wind model shared by turbines, the anemometer and their screens. Wind strength and direction drift smoothly with game
 * time (the same on server and client, seeded by the dimension), rain and thunder strengthen it, and it grows with height
 * above sea level. Dimensions without a sky have no wind.
 */
public final class Wind {
    private Wind() {}

    private static long seed(Level level) {
        return level.dimension().location().toString().hashCode() * 31L + 7;
    }

    private static double hash(long i, long seed) {
        long h = i * 0x9E3779B97F4A7C15L + seed * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 31);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 29);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 32);
        return ((h >>> 11) * 0x1.0p-53) * 2 - 1;
    }

    /** Smooth 1D value noise in [-1, 1]. */
    static double noise(double x, long seed) {
        long i = (long) Math.floor(x);
        double f = x - i;
        double u = f * f * (3 - 2 * f);
        double a = hash(i, seed), b = hash(i + 1, seed);
        return a + (b - a) * u;
    }

    public static boolean hasWind(Level level) {
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling();
    }

    /** Base wind strength (about 0.2 to 1.8, average 1) before weather and height. */
    public static double strength(Level level) {
        if (!hasWind(level)) return 0;
        double t = level.getGameTime();
        long s = seed(level);
        double v = 1.0 + 0.5 * noise(t / 3000.0, s) + 0.22 * noise(t / 600.0, s + 1) + 0.08 * noise(t / 60.0, s + 2);
        return Math.max(0.15, Math.min(1.85, v));
    }

    /** Rain and thunder make the wind stronger. */
    public static double weather(Level level) {
        return 1.0 + 0.35 * level.getRainLevel(1f) + 0.6 * level.getThunderLevel(1f);
    }

    /** Compass bearing the wind blows from, in radians clockwise from north. */
    public static double bearing(Level level) {
        double t = level.getGameTime();
        long s = seed(level);
        double b = Math.PI * (1.6 * noise(t / 9000.0, s + 3) + 0.35 * noise(t / 1500.0, s + 4)) + (s & 0xFF) / 40.0;
        b %= Math.PI * 2;
        return b < 0 ? b + Math.PI * 2 : b;
    }

    /** Height factor: none at or below sea level, 1 at 100 blocks above it, at most 1.5. */
    public static double height(Level level, BlockPos pos) {
        double above = pos.getY() + 0.5 - level.getSeaLevel();
        if (above <= 0) return 0;
        return Math.sqrt(Math.min(2.25, above / 100.0));
    }

    /** Overall wind factor at a position: strength x weather x height. */
    public static double local(Level level, BlockPos pos) {
        return strength(level) * weather(level) * height(level, pos);
    }

    /** Wind speed shown to players, in m/s. */
    public static double metres(double local) {
        return local * 9.0;
    }

    /** Compass bearing of a horizontal direction, radians clockwise from north. */
    public static double bearingOf(Direction d) {
        return switch (d) {
            case EAST -> Math.PI / 2;
            case SOUTH -> Math.PI;
            case WEST -> Math.PI * 1.5;
            default -> 0;
        };
    }

    /** How well a rotor on this axis catches wind from a bearing: 1 head-on or from behind, 0.55 side-on. */
    public static double alignment(Direction axis, double bearing) {
        return 0.55 + 0.45 * Math.abs(Math.cos(bearing - bearingOf(axis)));
    }

    public static String compassKey(double bearing) {
        String[] k = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        int i = (int) Math.round(bearing / (Math.PI / 4)) & 7;
        return "gui.ls_aeolus.dir." + k[i];
    }

    /** Short compass point for screens ("NW"). */
    public static Component compass(double bearing) {
        return Component.translatable(compassKey(bearing));
    }

    /** Compass point in words for messages ("north-west"). */
    public static Component compassLong(double bearing) {
        return Component.translatable(compassKey(bearing).replace("gui.", "msg."));
    }
}

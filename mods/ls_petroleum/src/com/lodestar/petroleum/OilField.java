package com.lodestar.petroleum;

import com.lodestar.core.data.WorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Oil reservoirs. Every overworld chunk has a fixed starting amount of crude oil worked out from the world seed with
 * a smooth two-octave value noise (so oil comes in fields spanning many chunks, about one chunk in five), and the
 * amount pumped out of each chunk is kept in the world save. Worldgen uses the same noise to put seeps over rich
 * fields, so what you see on the surface agrees with what the geophone hears.
 */
public final class OilField {
    private OilField() {}

    /** richness above which a chunk holds oil */
    public static final double THRESHOLD = 0.64;
    public static final int MIN_AMOUNT = 200_000, MAX_AMOUNT = 3_000_000;
    private static final String KEY = "ls_petroleum";

    private static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double lattice(long seed, int x, int z) {
        long h = mix(seed ^ (x * 0x632BE59BD9B4E019L) ^ (z * 0x8CB92BA72F3D8DD7L));
        return (h >>> 11) / (double) (1L << 53);
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double valueNoise(long seed, int cx, int cz, int cell) {
        int gx = Math.floorDiv(cx, cell), gz = Math.floorDiv(cz, cell);
        double fx = (cx - gx * cell + 0.5) / cell, fz = (cz - gz * cell + 0.5) / cell;
        double sx = smooth(fx), sz = smooth(fz);
        double a = lattice(seed, gx, gz), b = lattice(seed, gx + 1, gz), c = lattice(seed, gx, gz + 1), d = lattice(seed, gx + 1, gz + 1);
        return (a + (b - a) * sx) * (1 - sz) + (c + (d - c) * sx) * sz;
    }

    /** 0..1; chunks above {@link #THRESHOLD} hold oil. */
    public static double richness(long seed, int cx, int cz) {
        return 0.65 * valueNoise(seed, cx, cz, 7) + 0.35 * valueNoise(seed ^ 0x51EDL, cx, cz, 3);
    }

    /** Starting amount of crude oil (mB) in a chunk, from the seed alone. */
    public static int initialFromSeed(long seed, int cx, int cz) {
        double r = richness(seed, cx, cz);
        if (r < THRESHOLD) return 0;
        double frac = Math.min(1.0, (r - THRESHOLD) / (1.0 - THRESHOLD - 0.06));
        double jitter = 0.9 + 0.2 * lattice(seed ^ 0x0111L, cx * 31, cz * 17);
        int amount = (int) ((MIN_AMOUNT + Math.pow(frac, 1.4) * (MAX_AMOUNT - MIN_AMOUNT)) * jitter);
        return amount / 1000 * 1000;
    }

    private static CompoundTag data(ServerLevel level) {
        return WorldData.get(level.getServer(), KEY);
    }

    private static String key(ChunkPos c) {
        return c.x + "," + c.z;
    }

    public static boolean hasOil(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    public static int initial(ServerLevel level, ChunkPos c) {
        if (!hasOil(level)) return 0;
        CompoundTag over = data(level).getCompound("Override");
        if (over.contains(key(c))) return over.getInt(key(c));
        return initialFromSeed(level.getSeed(), c.x, c.z);
    }

    public static int extracted(ServerLevel level, ChunkPos c) {
        return data(level).getCompound("Extracted").getInt(key(c));
    }

    /** Oil left in a chunk (mB). */
    public static int remaining(ServerLevel level, ChunkPos c) {
        return Math.max(0, initial(level, c) - extracted(level, c));
    }

    /** Removes up to amount mB from the chunk's reservoir; returns what was taken. */
    public static int extract(ServerLevel level, ChunkPos c, int amount) {
        int left = remaining(level, c);
        int take = Math.min(left, amount);
        if (take <= 0) return 0;
        CompoundTag d = data(level);
        CompoundTag ex = d.getCompound("Extracted");
        ex.putInt(key(c), ex.getInt(key(c)) + take);
        d.put("Extracted", ex);
        WorldData.dirty(level.getServer());
        return take;
    }

    /** Sets a chunk's starting amount and clears what was pumped from it (self-tests and commands). */
    public static void override(ServerLevel level, ChunkPos c, int amount) {
        CompoundTag d = data(level);
        CompoundTag over = d.getCompound("Override");
        over.putInt(key(c), amount);
        d.put("Override", over);
        CompoundTag ex = d.getCompound("Extracted");
        ex.remove(key(c));
        d.put("Extracted", ex);
        WorldData.dirty(level.getServer());
    }

    /** Size word for an amount: small, fair, rich, vast. */
    public static String sizeKey(int amount) {
        if (amount < 500_000) return "small";
        if (amount < 1_200_000) return "fair";
        if (amount < 2_200_000) return "rich";
        return "vast";
    }
}

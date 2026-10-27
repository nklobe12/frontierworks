package com.lodestar.ducts;

/** Per-tier numbers for each kind of duct. */
public final class DuctTiers {
    private DuctTiers() {}

    /** FE/t through each endpoint, FE buffered per duct */
    public static final int[] ENERGY_RATE = {1_000, 4_000, 16_000, 100_000};
    public static final int[] ENERGY_BUFFER = {2_000, 8_000, 32_000, 200_000};
    /** mB/t per endpoint and servo, mB buffered per duct */
    public static final int[] FLUID_RATE = {200, 800, 4_000};
    public static final int[] FLUID_BUFFER = {500, 2_000, 8_000};
    /** items per servo pull, ticks between pulls, blocks travelled per tick */
    public static final int[] ITEM_STACK = {8, 16};
    public static final int[] ITEM_INTERVAL = {20, 10};
    public static final float[] ITEM_SPEED = {0.125f, 0.5f};

    public static int idx(int[] a, int tier) {
        return a[Math.max(0, Math.min(a.length - 1, tier))];
    }

    public static float idx(float[] a, int tier) {
        return a[Math.max(0, Math.min(a.length - 1, tier))];
    }
}

package com.lodestar.aeolus;

/**
 * The tide model: two tides a day, high water at sunrise and sunset (day time 0 and 12000), low water at noon and midnight.
 * The tidal current, and so the power, is strongest halfway between and stops at slack water. Full and new moons bring
 * spring tides (stronger), the quarter moons neap tides (weaker).
 */
public final class Tides {
    public static final int PERIOD = 12000;

    private Tides() {}

    private static double phase(long dayTime) {
        return Math.floorMod(dayTime, PERIOD) / (double) PERIOD;
    }

    /** Water height, 1 at high tide and -1 at low tide. */
    public static double height(long dayTime) {
        return Math.cos(2 * Math.PI * phase(dayTime));
    }

    /** Strength of the tidal current, 0 at slack water and 1 at mid-tide. */
    public static double current(long dayTime) {
        return Math.pow(Math.abs(Math.sin(2 * Math.PI * phase(dayTime))), 0.7);
    }

    /** True while the water is rising. */
    public static boolean rising(long dayTime) {
        return Math.sin(2 * Math.PI * phase(dayTime)) < 0;
    }

    /** Moon phase 0 = full ... 4 = new. */
    public static double moon(int moonPhase) {
        int p = Math.floorMod(moonPhase, 8);
        if (p == 0 || p == 4) return 1.25;
        if (p == 2 || p == 6) return 0.75;
        return 1.0;
    }

    public static String moonKind(int moonPhase) {
        double m = moon(moonPhase);
        return m > 1 ? "gui.ls_aeolus.moon.spring" : m < 1 ? "gui.ls_aeolus.moon.neap" : "gui.ls_aeolus.moon.normal";
    }
}

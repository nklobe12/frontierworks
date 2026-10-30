package com.lodestar.jetpacks;

/**
 * Jetpack tiers. Fuel tiers burn liquid fuel (mB per tick of full thrust, divided by the fuel's grade); electric
 * tiers draw FE. Thrust numbers are blocks/tick: accel per tick, top climb speed, sideways push and top sideways speed.
 */
public enum JetpackTier {
    //        id           fuel   capacity    use   accel  climb  side   sideMax hover  armor color
    BRONZE("bronze", true, 4_000, 3, 0.10, 0.42, 0.030, 0.42, false, 2, 0xC08040),
    STEEL("steel", true, 8_000, 4, 0.13, 0.55, 0.040, 0.55, true, 3, 0x8A9099),
    ELECTRUM("electrum", false, 400_000, 80, 0.12, 0.50, 0.040, 0.52, false, 2, 0xD8C050),
    DURALUMIN("duralumin", false, 1_600_000, 160, 0.16, 0.70, 0.055, 0.75, true, 3, 0xB8C4D6),
    PLATINUM("platinum", false, 6_000_000, 320, 0.20, 0.90, 0.070, 1.00, true, 4, 0xDCE6F0);

    public final String id;
    public final boolean fuel;
    /** mB of fuel or FE */
    public final int capacity;
    /** mB (grade 1 fuel) or FE per tick of full thrust */
    public final int use;
    public final double accel, climb, side, sideMax;
    /** hover mode built in (otherwise needs a hover stabilizer) */
    public final boolean hover;
    public final int armor;
    public final int color;

    JetpackTier(String id, boolean fuel, int capacity, int use, double accel, double climb, double side, double sideMax, boolean hover, int armor, int color) {
        this.id = id;
        this.fuel = fuel;
        this.capacity = capacity;
        this.use = use;
        this.accel = accel;
        this.climb = climb;
        this.side = side;
        this.sideMax = sideMax;
        this.hover = hover;
        this.armor = armor;
        this.color = color;
    }

    public String itemName() {
        return id + "_jetpack";
    }
}

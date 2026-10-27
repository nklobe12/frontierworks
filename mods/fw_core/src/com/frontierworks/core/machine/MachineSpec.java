package com.frontierworks.core.machine;

/** Static configuration for a generic processing machine. */
public class MachineSpec {
    public final MachineLayout layout;
    public final RecipeSource recipes;
    public final int energyCapacity;
    public final int maxReceive;
    /** multiplier applied to recipe energy/tick */
    public final float energyFactor;
    /** multiplier applied to recipe time */
    public final float timeFactor;

    public MachineSpec(MachineLayout layout, RecipeSource recipes, int energyCapacity, int maxReceive, float energyFactor, float timeFactor) {
        this.layout = layout;
        this.recipes = recipes;
        this.energyCapacity = energyCapacity;
        this.maxReceive = maxReceive;
        this.energyFactor = energyFactor;
        this.timeFactor = timeFactor;
    }

    /** Typical electric machine: N inputs, M outputs, 3 upgrade slots, battery slot. */
    public static MachineSpec simple(int inputs, int outputs, RecipeSource recipes, int capacity, int maxReceive) {
        return new MachineSpec(MachineLayout.processing(inputs, outputs, 0, 0, 0, true, 3, true), recipes, capacity, maxReceive, 1f, 1f);
    }

    public static MachineSpec withTanks(int inputs, int outputs, int tanksIn, int tanksOut, int tankCap, RecipeSource recipes, int capacity, int maxReceive) {
        return new MachineSpec(MachineLayout.processing(inputs, outputs, tanksIn, tanksOut, tankCap, true, 3, true), recipes, capacity, maxReceive, 1f, 1f);
    }
}

package com.lodestar.core.machine;

import com.lodestar.core.recipe.ChanceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/** A resolved unit of work for a processing machine: what to consume and what to produce. */
public class ProcessJob {
    public final ResourceLocation recipeId;
    /** handler slot indices and counts to consume */
    public final int[] slots;
    public final int[] counts;
    public final List<ChanceStack> outputs;
    public final FluidStack fluidIn;
    public final FluidStack fluidOut;
    public final FluidStack fluidIn2;
    public final FluidStack fluidOut2;
    public final int time;
    public final int energyPerTick;

    public ProcessJob(ResourceLocation recipeId, int[] slots, int[] counts, List<ChanceStack> outputs, FluidStack fluidIn, FluidStack fluidOut, int time, int energyPerTick) {
        this(recipeId, slots, counts, outputs, fluidIn, fluidOut, FluidStack.EMPTY, FluidStack.EMPTY, time, energyPerTick);
    }

    public ProcessJob(ResourceLocation recipeId, int[] slots, int[] counts, List<ChanceStack> outputs, FluidStack fluidIn, FluidStack fluidOut,
                      FluidStack fluidIn2, FluidStack fluidOut2, int time, int energyPerTick) {
        this.recipeId = recipeId;
        this.slots = slots;
        this.counts = counts;
        this.outputs = outputs;
        this.fluidIn = fluidIn;
        this.fluidOut = fluidOut;
        this.fluidIn2 = fluidIn2;
        this.fluidOut2 = fluidOut2;
        this.time = time;
        this.energyPerTick = energyPerTick;
    }
}

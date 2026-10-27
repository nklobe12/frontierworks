package com.frontierworks.core.machine;

import com.frontierworks.core.recipe.ChanceStack;
import com.frontierworks.core.recipe.MachineRecipe;
import com.frontierworks.core.recipe.MachineRecipeKind;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

/** Finds work for a processing machine from its current inputs. */
public interface RecipeSource {
    @Nullable
    ProcessJob find(Level level, MachineBlockEntity be, int[] inputSlots);

    /** Whether a stack could ever be used as input (slot filter). */
    boolean accepts(Level level, ItemStack stack);

    /** Whether a fluid could ever be used as input (tank filter). */
    default boolean acceptsFluid(Level level, FluidStack fluid) {
        return true;
    }

    static RecipeSource machine(Supplier<MachineRecipeKind> kind) {
        return new RecipeSource() {
            @Nullable
            @Override
            public ProcessJob find(Level level, MachineBlockEntity be, int[] inputSlots) {
                for (MachineRecipe r : kind.get().all(level)) {
                    if (!hasFluids(be, r.fluidIn, r.fluidIn2)) continue;
                    if (r.inputs.isEmpty() && r.fluidIn.isEmpty() && r.fluidIn2.isEmpty()) continue;
                    int[] assign = r.match(be.getItems(), inputSlots);
                    if (assign == null) continue;
                    int[] slots = new int[assign.length];
                    int[] counts = new int[assign.length];
                    for (int i = 0; i < assign.length; i++) {
                        slots[i] = inputSlots[assign[i]];
                        counts[i] = r.inputs.get(i).count;
                    }
                    return new ProcessJob(r.getId(), slots, counts, r.outputs, r.fluidIn, r.fluidOut, r.fluidIn2, r.fluidOut2, r.time, r.energy);
                }
                return null;
            }

            @Override
            public boolean accepts(Level level, ItemStack stack) {
                for (MachineRecipe r : kind.get().all(level)) {
                    if (r.usesItem(stack)) return true;
                }
                return false;
            }

            @Override
            public boolean acceptsFluid(Level level, FluidStack fluid) {
                for (MachineRecipe r : kind.get().all(level)) {
                    if (r.usesFluid(fluid.getFluid())) return true;
                }
                return false;
            }
        };
    }

    /** Input tank index holding at least fs (excluding skip), or -1. */
    static int inputTank(MachineBlockEntity be, FluidStack fs, int skip) {
        FluidTank[] tanks = be.getTanks();
        for (int t = 0; t < tanks.length; t++) {
            if (t == skip || !be.getLayout().tanks.get(t).input) continue;
            if (tanks[t].getFluid().isFluidEqual(fs) && tanks[t].getFluidAmount() >= fs.getAmount()) return t;
        }
        return -1;
    }

    static boolean hasFluids(MachineBlockEntity be, FluidStack a, FluidStack b) {
        int ta = -1;
        if (!a.isEmpty()) {
            ta = inputTank(be, a, -1);
            if (ta < 0) return false;
        }
        if (!b.isEmpty()) {
            return inputTank(be, b, ta) >= 0;
        }
        return true;
    }

    /** Vanilla furnace/blasting/smoking recipes, at the given speed factor and energy per tick. */
    static <T extends AbstractCookingRecipe> RecipeSource cooking(Supplier<RecipeType<T>> type, float timeFactor, int energyPerTick) {
        return new RecipeSource() {
            @Nullable
            @Override
            public ProcessJob find(Level level, MachineBlockEntity be, int[] inputSlots) {
                if (inputSlots.length == 0) return null;
                ItemStack in = be.getItems().getStackInSlot(inputSlots[0]);
                if (in.isEmpty()) return null;
                Optional<T> r = level.getRecipeManager().getRecipeFor(type.get(), new SimpleContainer(in), level);
                if (!r.isPresent()) return null;
                T recipe = r.get();
                ItemStack out = recipe.getResultItem(level.registryAccess()).copy();
                if (out.isEmpty()) return null;
                int time = Math.max(1, Math.round(recipe.getCookingTime() * timeFactor));
                return new ProcessJob(recipe.getId(), new int[]{inputSlots[0]}, new int[]{1},
                        Collections.singletonList(new ChanceStack(out, 1f)), FluidStack.EMPTY, FluidStack.EMPTY, time, energyPerTick);
            }

            @Override
            public boolean accepts(Level level, ItemStack stack) {
                return level.getRecipeManager().getRecipeFor(type.get(), new SimpleContainer(stack), level).isPresent();
            }
        };
    }
}

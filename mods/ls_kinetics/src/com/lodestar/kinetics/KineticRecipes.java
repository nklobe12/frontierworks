package com.lodestar.kinetics;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.ProcessJob;
import com.lodestar.core.machine.RecipeSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

/** Recipe sources and screen layouts for the shaft-driven machines. */
public final class KineticRecipes {
    private KineticRecipes() {}

    /** Mixing vat: heated recipes first when there is heat below, then plain mixing. */
    public static RecipeSource vat() {
        RecipeSource plain = RecipeSource.machine(() -> Content.MIXING);
        RecipeSource hot = RecipeSource.machine(() -> Content.HEATED_MIXING);
        return new RecipeSource() {
            @Nullable
            @Override
            public ProcessJob find(Level level, MachineBlockEntity be, int[] inputSlots) {
                if (be instanceof KineticMachineBE k && k.heated()) {
                    ProcessJob j = hot.find(level, be, inputSlots);
                    if (j != null) return j;
                }
                return plain.find(level, be, inputSlots);
            }

            @Override
            public boolean accepts(Level level, ItemStack stack) {
                return plain.accepts(level, stack) || hot.accepts(level, stack);
            }

            @Override
            public boolean acceptsFluid(Level level, FluidStack fluid) {
                return plain.acceptsFluid(level, fluid) || hot.acceptsFluid(level, fluid);
            }
        };
    }

    public static MachineLayout millLayout() {
        return MachineLayout.builder().slot(48, 35, MachineLayout.Role.INPUT).arrow(72, 34)
                .slot(104, 35, MachineLayout.Role.OUTPUT).slot(122, 35, MachineLayout.Role.OUTPUT)
                .slot(152, 17, MachineLayout.Role.UPGRADE).info(48, 60).extra(3).build();
    }

    public static MachineLayout pressLayout() {
        return MachineLayout.builder().slot(52, 35, MachineLayout.Role.INPUT).arrow(78, 34)
                .slot(116, 35, MachineLayout.Role.OUTPUT).info(52, 60).extra(3).build();
    }

    public static MachineLayout vatLayout() {
        return MachineLayout.builder()
                .tank(8, 17, 16, 52, 4000, true, false).tank(28, 17, 16, 52, 4000, true, false)
                .grid(52, 26, 2, 2, MachineLayout.Role.INPUT).arrow(92, 35)
                .slot(122, 26, MachineLayout.Role.OUTPUT).slot(122, 44, MachineLayout.Role.OUTPUT)
                .tank(152, 17, 16, 52, 4000, false, true).info(52, 64).extra(3).build();
    }
}

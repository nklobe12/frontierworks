package com.frontierworks.recycler;

import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.ProcessJob;
import com.frontierworks.core.machine.RecipeSource;
import com.frontierworks.core.recipe.ChanceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Collections;

/** Any item except scrap goes in; scrap comes out 1 time in 8. */
public class RecyclerSource implements RecipeSource {
    private static final ResourceLocation ID = new ResourceLocation("fw_recycler", "recycling");

    @Nullable
    @Override
    public ProcessJob find(Level level, MachineBlockEntity be, int[] inputSlots) {
        if (inputSlots.length == 0) return null;
        ItemStack in = be.getItems().getStackInSlot(inputSlots[0]);
        if (in.isEmpty() || !accepts(level, in)) return null;
        return new ProcessJob(ID, new int[]{inputSlots[0]}, new int[]{1},
                Collections.singletonList(new ChanceStack(new ItemStack(Content.SCRAP.get()), 0.125f)),
                FluidStack.EMPTY, FluidStack.EMPTY, 30, 12);
    }

    @Override
    public boolean accepts(Level level, ItemStack stack) {
        return !stack.is(Content.SCRAP.get()) && !stack.is(Content.SCRAP_BOX.get());
    }
}

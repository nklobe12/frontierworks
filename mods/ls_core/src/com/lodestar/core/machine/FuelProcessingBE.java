package com.lodestar.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

/** Processing machine powered by burning furnace fuel instead of Forge Energy (kilns, coke ovens). */
public class FuelProcessingBE extends ProcessingMachineBE {
    private final int[] fuelSlots;

    public FuelProcessingBE(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineSpec spec) {
        super(type, pos, state, spec);
        this.fuelSlots = spec.layout.indices(MachineLayout.Role.FUEL);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (layout.slots.get(slot).role == MachineLayout.Role.FUEL) {
            return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
        }
        return super.isItemValid(slot, stack);
    }

    @Override
    protected boolean consumePower(ProcessJob j) {
        if (burn <= 0 && !ignite()) {
            return false;
        }
        burn--;
        return true;
    }

    private boolean ignite() {
        for (int slot : fuelSlots) {
            ItemStack st = items.getStackInSlot(slot);
            int time = ForgeHooks.getBurnTime(st, RecipeType.SMELTING);
            if (time > 0) {
                burn = time;
                maxBurn = time;
                ItemStack remainder = st.getCraftingRemainingItem();
                ItemStack taken = items.extractItem(slot, 1, false);
                if (!remainder.isEmpty() && items.getStackInSlot(slot).isEmpty()) {
                    items.setStackInSlot(slot, remainder);
                }
                return !taken.isEmpty();
            }
        }
        return false;
    }

    @Override
    protected void idleTick(boolean active) {
        // a lit fire keeps burning down while idle, like a furnace
        if (!active && burn > 0) {
            burn--;
            setChanged();
        }
    }

    @Override
    protected void setLit(boolean active) {
        super.setLit(active || burn > 0);
    }
}

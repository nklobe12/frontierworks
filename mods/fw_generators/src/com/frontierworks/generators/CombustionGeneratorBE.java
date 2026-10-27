package com.frontierworks.generators;

import com.frontierworks.core.machine.GeneratorBE;
import com.frontierworks.core.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

/** Burns furnace fuel: produces a fixed FE/t while burning; fuel lasts burnTime * factor ticks. */
public class CombustionGeneratorBE extends GeneratorBE {
    public static final MachineLayout LAYOUT = MachineLayout.generator(true, 0, 0);
    private final int rate;
    private final float factor;

    public CombustionGeneratorBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int rate, float factor, int capacity) {
        super(type, pos, state, LAYOUT, capacity, rate * 4);
        this.rate = rate;
        this.factor = factor;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (layout.slots.get(slot).role == MachineLayout.Role.FUEL) {
            return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
        }
        return super.isItemValid(slot, stack);
    }

    @Override
    protected int generate() {
        if (burn <= 0) {
            for (int i = 0; i < layout.slots.size(); i++) {
                if (layout.slots.get(i).role != MachineLayout.Role.FUEL) continue;
                ItemStack st = items.getStackInSlot(i);
                int t = ForgeHooks.getBurnTime(st, RecipeType.SMELTING);
                if (t > 0) {
                    ItemStack rem = st.getCraftingRemainingItem();
                    items.extractItem(i, 1, false);
                    if (!rem.isEmpty() && items.getStackInSlot(i).isEmpty()) items.setStackInSlot(i, rem);
                    burn = Math.max(1, Math.round(t * factor));
                    maxBurn = burn;
                    break;
                }
            }
        }
        if (burn > 0) {
            burn--;
            setChanged();
            return rate;
        }
        return 0;
    }
}

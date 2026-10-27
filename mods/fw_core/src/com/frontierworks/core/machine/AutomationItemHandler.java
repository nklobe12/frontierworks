package com.frontierworks.core.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

/** What pipes/hoppers see: insert into input/fuel/battery/storage slots, extract from output/storage slots. */
public class AutomationItemHandler implements IItemHandler {
    private final MachineBlockEntity be;

    public AutomationItemHandler(MachineBlockEntity be) {
        this.be = be;
    }

    private MachineLayout.Role role(int slot) {
        return be.getLayout().slots.get(slot).role;
    }

    @Override
    public int getSlots() {
        return be.getItems().getSlots();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return be.getItems().getStackInSlot(slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        MachineLayout.Role r = role(slot);
        if (r == MachineLayout.Role.INPUT || r == MachineLayout.Role.FUEL || r == MachineLayout.Role.BATTERY
                || r == MachineLayout.Role.STORAGE || r == MachineLayout.Role.CHARGE) {
            if (!be.canAutomationInsert(slot, stack)) return stack;
            return be.getItems().insertItem(slot, stack, simulate);
        }
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        MachineLayout.Role r = role(slot);
        if (r == MachineLayout.Role.OUTPUT || r == MachineLayout.Role.STORAGE) {
            return be.getItems().extractItem(slot, amount, simulate);
        }
        if (r == MachineLayout.Role.CHARGE && be.isChargeDone(slot)) {
            return be.getItems().extractItem(slot, amount, simulate);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return be.getItems().getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return be.getItems().isItemValid(slot, stack);
    }
}

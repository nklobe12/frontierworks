package com.lodestar.transmutation;

import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Aether relay: stores aether from collectors and passes it to condensers. Items in its burn slot are dissolved
 * into aether, and an aether cell in its charge slot is filled.
 */
public class RelayBE extends AetherMachineBE {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .slot(44, 35, MachineLayout.Role.INPUT).slot(116, 35, MachineLayout.Role.CHARGE).info(8, 60).extra(2).build();

    public RelayBE(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity, long transfer) {
        super(type, pos, state, LAYOUT, capacity, transfer);
    }

    @Override
    public boolean providesAether() {
        return true;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        MachineLayout.Role r = layout.slots.get(slot).role;
        if (r == MachineLayout.Role.CHARGE) return stack.getItem() instanceof AetherCellItem;
        if (r == MachineLayout.Role.INPUT) return AetherValues.has(stack) && !(stack.getItem() instanceof AetherCellItem);
        return super.isItemValid(slot, stack);
    }

    @Override
    public boolean isChargeDone(int slot) {
        ItemStack st = items.getStackInSlot(slot);
        return !(st.getItem() instanceof AetherCellItem c) || AetherCellItem.stored(st) >= c.capacity;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        ItemStack burn = items.getStackInSlot(0);
        if (!burn.isEmpty() && level.getGameTime() % 4 == 0) {
            long v = AetherValues.get(burn.copyWithCount(1));
            if (v > 0 && capacity - aether >= v) {
                aether += v;
                items.extractItem(0, 1, false);
            }
        }
        ItemStack cell = items.getStackInSlot(1);
        if (cell.getItem() instanceof AetherCellItem && aether > 0) {
            ItemStack copy = cell.copy();
            aether -= AetherCellItem.charge(copy, Math.min(aether, transfer));
            items.setStackInSlot(1, copy);
        }
        pushAether(false);
        setChanged();
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_transmutation.stored", AetherValues.format(menu.extra(0)), AetherValues.format(menu.extra(1))));
    }
}

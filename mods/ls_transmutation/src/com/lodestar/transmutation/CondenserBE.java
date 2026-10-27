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
 * Aether condenser: set a target item in the top slot. It dissolves whatever is in its input grid into aether
 * (or takes aether from relays beside it) and condenses copies of the target into its output grid.
 */
public class CondenserBE extends AetherMachineBE {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .slot(80, 17, MachineLayout.Role.FILTER).grid(8, 40, 4, 4, MachineLayout.Role.INPUT).arrow(80, 66)
            .grid(108, 40, 3, 4, MachineLayout.Role.OUTPUT).info(8, 28).height(200).extra(3).build();

    private final int itemsPerTick;

    public CondenserBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int itemsPerTick) {
        super(type, pos, state, LAYOUT, 1_000_000_000L, 0);
        this.itemsPerTick = itemsPerTick;
    }

    private ItemStack target() {
        return items.getStackInSlot(0);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == 0 ? 1 : 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        MachineLayout.Role r = layout.slots.get(slot).role;
        if (r == MachineLayout.Role.FILTER) return AetherValues.has(stack);
        if (r == MachineLayout.Role.INPUT) return AetherValues.has(stack);
        return super.isItemValid(slot, stack);
    }

    @Override
    public long receiveAether(long amount, boolean simulate) {
        long v = AetherValues.get(target());
        if (v <= 0) return 0;
        long want = Math.max(0, v * 4 - aether);
        long put = Math.min(amount, want);
        if (!simulate && put > 0) aether += put;
        return put;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        ItemStack t = target();
        long v = AetherValues.get(t.copyWithCount(1));
        if (v <= 0 || !canRun()) {
            progress = 0;
            maxProgress = 1;
            return;
        }
        int budget = itemsPerTick;
        for (int s = 1; s < layout.slots.size() && budget > 0 && aether < v; s++) {
            if (layout.slots.get(s).role != MachineLayout.Role.INPUT) continue;
            ItemStack st = items.getStackInSlot(s);
            if (st.isEmpty() || ItemStack.isSameItemSameTags(st, t)) continue;
            long iv = AetherValues.get(st.copyWithCount(1));
            if (iv <= 0) continue;
            int n = (int) Math.min(budget, Math.min(st.getCount(), Math.max(1, (v - aether + iv - 1) / iv)));
            items.extractItem(s, n, false);
            aether += iv * n;
            budget -= n;
        }
        ItemStack out = t.copyWithCount(1);
        while (aether >= v && insertOutput(out, true).isEmpty()) {
            insertOutput(out, false);
            aether -= v;
        }
        maxProgress = (int) Math.min(Integer.MAX_VALUE, v);
        progress = (int) Math.min(aether, v);
        setChanged();
    }

    @Override
    public int getExtra(int i) {
        if (i == 2) return (int) Math.min(Integer.MAX_VALUE, AetherValues.get(target().copyWithCount(1)));
        return super.getExtra(i);
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_transmutation.condenser", AetherValues.format(menu.extra(0)), AetherValues.format(menu.extra(2))));
    }
}

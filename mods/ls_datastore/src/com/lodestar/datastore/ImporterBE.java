package com.lodestar.datastore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

/** Pulls items that pass its filter out of the inventory in front into the network. */
public class ImporterBE extends PortBE {
    public ImporterBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, false);
    }

    @Override
    protected void work(DataGrid g, IItemHandler h) {
        int budget = batch();
        for (int i = 0; i < h.getSlots() && budget > 0; i++) {
            ItemStack peek = h.extractItem(i, budget, true);
            if (peek.isEmpty() || !passes(peek)) continue;
            ItemKey k = ItemKey.of(peek);
            long fits = g.insert(k, peek.getCount(), true);
            if (fits <= 0) continue;
            ItemStack real = h.extractItem(i, (int) fits, false);
            if (real.isEmpty()) continue;
            ItemStack back = g.insert(real, false);
            budget -= real.getCount() - back.getCount();
            if (!back.isEmpty()) h.insertItem(i, back, false);
        }
    }
}

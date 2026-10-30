package com.lodestar.datastore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pushes items that pass its filter from the network into the inventory in front (allow list by default). */
public class ExporterBE extends PortBE {
    private int cursor;

    public ExporterBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, true);
    }

    @Override
    protected void work(DataGrid g, IItemHandler h) {
        List<ItemKey> wanted = new ArrayList<>();
        for (Map.Entry<ItemKey, Long> e : g.snapshot().entrySet()) {
            if (e.getValue() > 0 && passes(e.getKey().proto())) wanted.add(e.getKey());
        }
        if (wanted.isEmpty()) return;
        int budget = batch();
        for (int n = 0; n < wanted.size() && budget > 0; n++) {
            ItemKey k = wanted.get((cursor + n) % wanted.size());
            int want = Math.min(budget, k.maxStack());
            ItemStack rem = ItemHandlerHelper.insertItemStacked(h, k.stack(want), true);
            int fits = want - rem.getCount();
            if (fits <= 0) continue;
            ItemStack out = g.extractStack(k, fits, false);
            if (out.isEmpty()) continue;
            ItemStack back = ItemHandlerHelper.insertItemStacked(h, out, false);
            budget -= out.getCount() - back.getCount();
            if (!back.isEmpty()) g.insert(back, false);
        }
        cursor++;
    }
}

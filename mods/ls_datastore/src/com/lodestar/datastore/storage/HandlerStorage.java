package com.lodestar.datastore.storage;

import com.lodestar.datastore.ItemKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Map;
import java.util.function.Predicate;

/** An external inventory seen through an inventory link. Insertion is limited by the link's filter. */
public final class HandlerStorage implements IStorage {
    private final IItemHandler h;
    private final int priority;
    private final Predicate<ItemStack> filter;

    public HandlerStorage(IItemHandler h, int priority, Predicate<ItemStack> filter) {
        this.h = h;
        this.priority = priority;
        this.filter = filter;
    }

    @Override
    public long insert(ItemKey k, long n, boolean simulate) {
        if (n <= 0 || !filter.test(k.proto())) return 0;
        long left = n;
        // stack onto matching slots first, then empty ones
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (int i = 0; i < h.getSlots() && left > 0; i++) {
                ItemStack in = h.getStackInSlot(i);
                if (pass == 0 ? !k.matches(in) : !in.isEmpty()) continue;
                int chunk = (int) Math.min(left, k.maxStack());
                ItemStack rem = h.insertItem(i, k.stack(chunk), simulate);
                left -= chunk - rem.getCount();
            }
        }
        return n - left;
    }

    @Override
    public long extract(ItemKey k, long n, boolean simulate) {
        long got = 0;
        for (int i = h.getSlots() - 1; i >= 0 && got < n; i--) {
            if (!k.matches(h.getStackInSlot(i))) continue;
            ItemStack out = h.extractItem(i, (int) Math.min(n - got, k.maxStack()), simulate);
            got += out.getCount();
        }
        return got;
    }

    @Override
    public long count(ItemKey k) {
        long n = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (k.matches(s)) n += s.getCount();
        }
        return n;
    }

    @Override
    public void collect(Map<ItemKey, Long> into) {
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            into.merge(ItemKey.of(s), (long) s.getCount(), Long::sum);
        }
    }

    @Override
    public int priority() {
        return priority;
    }
}

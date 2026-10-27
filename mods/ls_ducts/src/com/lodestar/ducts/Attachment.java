package com.lodestar.ducts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/**
 * Something fitted to a duct side: a servo pulls from the block it faces into the duct network, a filter limits
 * what the network delivers into that block, and a retriever fetches matching items from anywhere on the network.
 * Each has nine filter slots and a whitelist/blacklist switch.
 */
public class Attachment {
    public enum Type { SERVO, FILTER, RETRIEVER }

    public final Type type;
    public final ItemStack[] filter = new ItemStack[9];
    public boolean whitelist = true;
    public int roundRobin;

    public Attachment(Type type) {
        this.type = type;
        for (int i = 0; i < 9; i++) filter[i] = ItemStack.EMPTY;
    }

    public boolean filterEmpty() {
        for (ItemStack s : filter) if (!s.isEmpty()) return false;
        return true;
    }

    /** Does an item pass this attachment's filter? An empty filter passes everything (in either mode). */
    public boolean allows(ItemStack stack) {
        if (filterEmpty()) return true;
        boolean match = false;
        for (ItemStack s : filter) if (!s.isEmpty() && ItemStack.isSameItem(s, stack)) match = true;
        return whitelist == match;
    }

    /** Fluid filter: filter items that hold a fluid (buckets) stand for that fluid. */
    public boolean allows(FluidStack fluid) {
        if (filterEmpty()) return true;
        boolean match = false;
        for (ItemStack s : filter) {
            if (s.isEmpty()) continue;
            FluidStack f = FluidUtil.getFluidContained(s).orElse(FluidStack.EMPTY);
            if (!f.isEmpty() && f.isFluidEqual(fluid)) match = true;
        }
        return whitelist == match;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Type", type.name());
        t.putBoolean("White", whitelist);
        ListTag l = new ListTag();
        for (ItemStack s : filter) l.add(s.save(new CompoundTag()));
        t.put("Filter", l);
        return t;
    }

    public static Attachment load(CompoundTag t) {
        Attachment a = new Attachment(Type.valueOf(t.getString("Type")));
        a.whitelist = t.getBoolean("White");
        ListTag l = t.getList("Filter", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(9, l.size()); i++) a.filter[i] = ItemStack.of(l.getCompound(i));
        return a;
    }
}

package com.lodestar.datastore;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Objects;

/** An item kind (item + NBT) as stored by the network, independent of any count. Immutable. */
public final class ItemKey {
    public final Item item;
    @Nullable
    private final CompoundTag tag;
    private final int hash;
    private ItemStack proto;

    private ItemKey(Item item, @Nullable CompoundTag tag) {
        this.item = item;
        this.tag = tag == null || tag.isEmpty() ? null : tag;
        this.hash = System.identityHashCode(item) * 31 + (this.tag == null ? 0 : this.tag.hashCode());
    }

    @Nullable
    public static ItemKey of(ItemStack s) {
        if (s.isEmpty()) return null;
        return new ItemKey(s.getItem(), s.getTag() == null ? null : s.getTag().copy());
    }

    public static ItemKey of(Item item) {
        return new ItemKey(item, null);
    }

    /** A new stack of this kind; count is clamped to the item's stack size by callers when needed. */
    public ItemStack stack(long count) {
        ItemStack s = new ItemStack(item, (int) Math.max(0, Math.min(Integer.MAX_VALUE, count)));
        if (tag != null) s.setTag(tag.copy());
        return s;
    }

    /** A shared count-1 stack for display and comparisons. Never modify it. */
    public ItemStack proto() {
        if (proto == null) proto = stack(1);
        return proto;
    }

    public int maxStack() {
        return proto().getMaxStackSize();
    }

    public boolean matches(ItemStack s) {
        return !s.isEmpty() && s.getItem() == item && Objects.equals(tag, s.getTag() == null || s.getTag().isEmpty() ? null : s.getTag());
    }

    public CompoundTag save() {
        return proto().save(new CompoundTag());
    }

    @Nullable
    public static ItemKey load(CompoundTag t) {
        ItemStack s = ItemStack.of(t);
        return s.isEmpty() || s.getItem() == Items.AIR ? null : of(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey k)) return false;
        return k.item == item && k.hash == hash && Objects.equals(tag, k.tag);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return item + (tag == null ? "" : tag.toString());
    }
}

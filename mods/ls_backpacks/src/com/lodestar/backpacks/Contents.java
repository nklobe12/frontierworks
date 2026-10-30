package com.lodestar.backpacks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;

/**
 * What one backpack holds, stored server side by the backpack's id (so the item stack stays small): the main
 * slots, upgrade slots, a crafting grid, a smelting slot trio and a fluid tank.
 */
public class Contents {
    public static final int TANK = 16_000;
    public final ItemStackHandler items;
    public final ItemStackHandler upgrades = new ItemStackHandler(5);
    public final ItemStackHandler craft = new ItemStackHandler(9);
    /** 0 input, 1 fuel, 2 output */
    public final ItemStackHandler smelt = new ItemStackHandler(3);
    /** bucket in / bucket out for the tank */
    public final ItemStackHandler buckets = new ItemStackHandler(2);
    public final FluidTank tank = new FluidTank(TANK);
    public int burn, burnMax, cook;
    private final Runnable dirty;

    public Contents(int size, Runnable dirty) {
        this.dirty = dirty;
        this.items = new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                dirty.run();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !(stack.getItem() instanceof BackpackItem);
            }
        };
    }

    public void changed() {
        dirty.run();
    }

    /** Grows (never shrinks) the main inventory when a backpack is upgraded to a bigger tier. */
    public Contents resized(int size) {
        if (size <= items.getSlots()) return this;
        Contents c = new Contents(size, dirty);
        for (int i = 0; i < items.getSlots(); i++) c.items.setStackInSlot(i, items.getStackInSlot(i));
        copy(upgrades, c.upgrades);
        copy(craft, c.craft);
        copy(smelt, c.smelt);
        copy(buckets, c.buckets);
        c.tank.setFluid(tank.getFluid().copy());
        c.burn = burn;
        c.burnMax = burnMax;
        c.cook = cook;
        return c;
    }

    private static void copy(ItemStackHandler a, ItemStackHandler b) {
        for (int i = 0; i < Math.min(a.getSlots(), b.getSlots()); i++) b.setStackInSlot(i, a.getStackInSlot(i));
    }

    public boolean has(UpgradeItem.Kind k) {
        for (int i = 0; i < upgrades.getSlots(); i++) if (upgrades.getStackInSlot(i).getItem() instanceof UpgradeItem u && u.kind == k) return true;
        return false;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Size", items.getSlots());
        t.put("Items", items.serializeNBT());
        t.put("Upgrades", upgrades.serializeNBT());
        t.put("Craft", craft.serializeNBT());
        t.put("Smelt", smelt.serializeNBT());
        t.put("Buckets", buckets.serializeNBT());
        t.put("Tank", tank.writeToNBT(new CompoundTag()));
        t.putInt("Burn", burn);
        t.putInt("BurnMax", burnMax);
        t.putInt("Cook", cook);
        return t;
    }

    public static Contents load(CompoundTag t, Runnable dirty) {
        Contents c = new Contents(Math.max(9, t.getInt("Size")), dirty);
        c.items.deserializeNBT(t.getCompound("Items"));
        c.upgrades.deserializeNBT(t.getCompound("Upgrades"));
        c.craft.deserializeNBT(t.getCompound("Craft"));
        c.smelt.deserializeNBT(t.getCompound("Smelt"));
        c.buckets.deserializeNBT(t.getCompound("Buckets"));
        c.tank.readFromNBT(t.getCompound("Tank"));
        c.burn = t.getInt("Burn");
        c.burnMax = t.getInt("BurnMax");
        c.cook = t.getInt("Cook");
        return c;
    }
}

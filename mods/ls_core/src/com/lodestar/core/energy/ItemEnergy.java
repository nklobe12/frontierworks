package com.lodestar.core.energy;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Static helpers for NBT-backed item energy. Items implement IEnergyItem and call {@link #provider}. */
public final class ItemEnergy {
    public static final String KEY = "LSEnergy";

    private ItemEnergy() {}

    public static int get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(KEY);
    }

    public static void set(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(KEY, Math.max(0, value));
    }

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof IEnergyItem ? ((IEnergyItem) stack.getItem()).getEnergyCapacity(stack) : 0;
    }

    /** Tries to use energy; returns true if the full amount was available and removed. */
    public static boolean use(ItemStack stack, int amount) {
        int e = get(stack);
        if (e < amount) {
            return false;
        }
        set(stack, e - amount);
        return true;
    }

    public static boolean has(ItemStack stack, int amount) {
        return get(stack) >= amount;
    }

    public static ItemStack filled(ItemStack stack) {
        set(stack, capacity(stack));
        return stack;
    }

    public static int barWidth(ItemStack stack) {
        int cap = capacity(stack);
        return cap <= 0 ? 0 : Math.round(13.0f * get(stack) / cap);
    }

    public static int barColor(ItemStack stack) {
        int cap = capacity(stack);
        float f = cap <= 0 ? 0 : (float) get(stack) / cap;
        return Mth.hsvToRgb(0.0f + f * 0.02f, 0.85f, 0.75f + f * 0.25f);
    }

    public static void tooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(EnergyUtil.energyLine(get(stack), capacity(stack)));
    }

    public static ICapabilityProvider provider(IEnergyItem item, ItemStack stack) {
        return new Provider(item, stack);
    }

    private static class Provider implements ICapabilityProvider {
        private final LazyOptional<IEnergyStorage> opt;

        Provider(IEnergyItem item, ItemStack stack) {
            this.opt = LazyOptional.of(() -> new Storage(item, stack));
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return ForgeCapabilities.ENERGY.orEmpty(cap, opt);
        }
    }

    private static class Storage implements IEnergyStorage {
        private final IEnergyItem item;
        private final ItemStack stack;

        Storage(IEnergyItem item, ItemStack stack) {
            this.item = item;
            this.stack = stack;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int cap = item.getEnergyCapacity(stack);
            int e = get(stack);
            int add = Math.min(cap - e, Math.min(item.getEnergyTransfer(stack), maxReceive));
            if (add <= 0) return 0;
            if (!simulate) set(stack, e + add);
            return add;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!item.canExtractEnergy(stack)) return 0;
            int e = get(stack);
            int rem = Math.min(e, Math.min(item.getEnergyTransfer(stack), maxExtract));
            if (rem <= 0) return 0;
            if (!simulate) set(stack, e - rem);
            return rem;
        }

        @Override
        public int getEnergyStored() {
            return get(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return item.getEnergyCapacity(stack);
        }

        @Override
        public boolean canExtract() {
            return item.canExtractEnergy(stack);
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}

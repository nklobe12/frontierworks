package com.frontierworks.core.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.EnergyStorage;

/** Forge Energy storage with change callback and internal (unrestricted) produce/consume. */
public class FWEnergyStorage extends EnergyStorage {
    private final Runnable onChange;

    public FWEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int r = super.receiveEnergy(maxReceive, simulate);
        if (r > 0 && !simulate) {
            onChange.run();
        }
        return r;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int r = super.extractEnergy(maxExtract, simulate);
        if (r > 0 && !simulate) {
            onChange.run();
        }
        return r;
    }

    /** Adds energy ignoring the receive limit. Returns amount added. */
    public int produce(int amount) {
        int add = Math.min(capacity - energy, Math.max(0, amount));
        if (add > 0) {
            energy += add;
            onChange.run();
        }
        return add;
    }

    /** Removes energy ignoring the extract limit. Returns amount removed. */
    public int consume(int amount) {
        int rem = Math.min(energy, Math.max(0, amount));
        if (rem > 0) {
            energy -= rem;
            onChange.run();
        }
        return rem;
    }

    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(capacity, value));
        onChange.run();
    }

    public void setCapacity(int value) {
        capacity = Math.max(0, value);
        if (energy > capacity) {
            energy = capacity;
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public void setMaxReceive(int v) {
        maxReceive = v;
    }

    public void setMaxExtract(int v) {
        maxExtract = v;
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public boolean isFull() {
        return energy >= capacity;
    }

    public int space() {
        return capacity - energy;
    }

    public void save(CompoundTag tag, String key) {
        tag.putInt(key, energy);
    }

    public void load(CompoundTag tag, String key) {
        energy = Math.max(0, Math.min(capacity, tag.getInt(key)));
    }
}
